package ocean.RedWhale;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * チャットルームのメイン画面です。
 * メッセージの送受信と表示を行います。
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private ChatUtils chatUtils;

    private ListView listMainChat;
    private EditText edCreateMessage;
    private ImageView btnSendMessage;
    private ChatAdapter adapterMainChat;
    private ArrayList<ChatMessage> chatMessages;
    private com.google.android.material.appbar.MaterialToolbar toolbar;

    private DatabaseHelper dbHelper;
    private String remoteDeviceAddress; // 相手のBluetoothアドレス

    private static final int SELECT_DEVICE = 102;

    // メッセージの種類
    public static final int MESSAGE_STATE_CHANGED = 0; // 接続状態の変化
    public static final int MESSAGE_READ = 1;          // メッセージ受信
    public static final int MESSAGE_WRITE = 2;         // メッセージ送信
    public static final int MESSAGE_DEVICE_NAME = 3;   // 相手の名前取得
    public static final int MESSAGE_TOAST = 4;         // エラー通知

    public static final String DEVICE_NAME = "deviceName";
    public static final String TOAST = "toast";

    private String connectedDevice;
    private String localAddress;
    private byte[] localAddressHash;

    // 権限要求のハンドラ
    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean connectGranted = result.getOrDefault(Manifest.permission.BLUETOOTH_CONNECT, false);
                boolean scanGranted = result.getOrDefault(Manifest.permission.BLUETOOTH_SCAN, false);
                if (connectGranted && scanGranted) {
                    enableBluetooth();
                } else {
                    Toast.makeText(this, "Bluetooth権限が必要です", Toast.LENGTH_LONG).show();
                }
            });

    // ChatUtilsからの通知を受け取るハンドラ
    private final Handler handler = new Handler(message -> {
        if (isFinishing()) return true;
        switch (message.what) {
            case MESSAGE_STATE_CHANGED:
                switch (message.arg1) {
                    case ChatUtils.STATE_NONE:
                    case ChatUtils.STATE_LISTEN:
                        setState("未接続");
                        break;
                    case ChatUtils.STATE_CONNECTING:
                        setState("接続中...");
                        break;
                    case ChatUtils.STATE_CONNECTED:
                        setState("接続済み: " + (connectedDevice != null ? connectedDevice : ""));
                        break;
                }
                break;

            case MESSAGE_WRITE:
                // 自分がメッセージを送信した時
                byte[] writeBuf = (byte[]) message.obj;
                String writeMessage = new String(writeBuf);
                ChatMessage writeChatMessage = new ChatMessage(writeMessage, true);
                chatMessages.add(writeChatMessage);
                adapterMainChat.notifyDataSetChanged();
                // データベースへ保存（非同期）
                new Thread(() -> dbHelper.addMessage(writeChatMessage, localAddress, remoteDeviceAddress)).start();
                break;

            case MESSAGE_READ:
                // 相手からメッセージを受信した時
                byte[] readBuf = (byte[]) message.obj;
                String readMessage = new String(readBuf, 0, message.arg1);
                ChatMessage readChatMessage = new ChatMessage(readMessage, false);
                chatMessages.add(readChatMessage);
                adapterMainChat.notifyDataSetChanged();
                // データベースへ保存（非同期）
                new Thread(() -> dbHelper.addMessage(readChatMessage, remoteDeviceAddress, localAddress)).start();
                break;

            case MESSAGE_DEVICE_NAME:
                connectedDevice = message.getData().getString(DEVICE_NAME);
                Toast.makeText(context, connectedDevice + " に接続しました", Toast.LENGTH_SHORT).show();
                break;

            case MESSAGE_TOAST:
                Toast.makeText(context, message.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                break;
        }
        return true;
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;
        dbHelper = new DatabaseHelper(context);
        remoteDeviceAddress = getIntent().getStringExtra("deviceAddress");
        
        if (remoteDeviceAddress == null) {
            Toast.makeText(this, "チャット相手が選択されていません", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        
        // 重い初期化処理を別スレッドで実行
        new Thread(this::performAsyncInitialization).start();
    }

    /**
     * 非同期でアイデンティティ情報の取得やサービスの開始を行います。
     */
    private void performAsyncInitialization() {
        IdentityManager identityManager = new IdentityManager(this);
        if (identityManager.exists()) {
            localAddress = identityManager.getIdentityAddress();
            localAddressHash = IdentityManager.getAddressHash(localAddress);
        }

        runOnUiThread(() -> {
            if (isFinishing()) return;
            
            toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_24);
                
                String contactName = getIntent().getStringExtra("CHAT_NAME");
                getSupportActionBar().setTitle(contactName != null ? contactName : remoteDeviceAddress);
            }

            initBluetooth();
            startMeshService();
            loadChatHistory();
        });
    }

    /**
     * メッシュネットワークサービスを開始し、接続を試みます。
     */
    private void startMeshService() {
        Intent serviceIntent = new Intent(this, BluetoothMeshService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        BluetoothMeshService.setUiHandler(handler);
        chatUtils = BluetoothMeshService.getChatUtils();
        if (chatUtils != null) {
            chatUtils.setRemoteAddressHash(remoteDeviceAddress != null ? IdentityManager.getAddressHash(remoteDeviceAddress) : null);
            if (remoteDeviceAddress != null && BluetoothAdapter.checkBluetoothAddress(remoteDeviceAddress)) {
                try {
                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(remoteDeviceAddress);
                    chatUtils.connect(device);
                } catch (Exception e) {
                    Log.e(TAG, "接続に失敗", e);
                }
            }
        }
    }

    private void initViews() {
        listMainChat = findViewById(R.id.list);
        edCreateMessage = findViewById(R.id.editText);
        btnSendMessage = findViewById(R.id.btn);

        chatMessages = new ArrayList<>();
        adapterMainChat = new ChatAdapter(this, chatMessages);
        listMainChat.setAdapter(adapterMainChat);

        // 送信ボタンのクリックイベント
        btnSendMessage.setOnClickListener(v -> {
            String message = edCreateMessage.getText().toString().trim();
            if (!message.isEmpty() && chatUtils != null) {
                edCreateMessage.setText("");
                chatUtils.write(message.getBytes());
            }
        });
    }

    /**
     * 過去のチャット履歴をデータベースから読み込みます（非同期）。
     */
    @SuppressLint("MissingPermission")
    private void loadChatHistory() {
        if (remoteDeviceAddress != null && bluetoothAdapter != null) {
            new Thread(() -> {
                List<ChatMessage> messages = dbHelper.getMessages(localAddress != null ? localAddress : "", remoteDeviceAddress);
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    chatMessages.clear();
                    chatMessages.addAll(messages);
                    adapterMainChat.notifyDataSetChanged();
                });
            }).start();
        }
    }

    private void initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetoothがサポートされていません", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        if (item.getItemId() == R.id.menu_search_devices) {
            Intent intent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(intent, SELECT_DEVICE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_DEVICE && resultCode == RESULT_OK && data != null) {
            remoteDeviceAddress = data.getStringExtra("deviceAddress");
            if (chatUtils != null && remoteDeviceAddress != null) {
                chatUtils.connect(bluetoothAdapter.getRemoteDevice(remoteDeviceAddress));
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @SuppressLint("MissingPermission")
    private void enableBluetooth() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN});
                return;
            }
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
        }
    }

    private void setState(CharSequence subtitle) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(subtitle);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ハンドラの解除
        BluetoothMeshService.setUiHandler(null);
    }
}