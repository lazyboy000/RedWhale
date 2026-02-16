package ocean.RedWhale;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
 * メインのチャット画面を管理するアクティビティです。
 * Bluetooth接続、メッセージの送受信、UIの更新を担当します。
 */
public class MainActivity extends AppCompatActivity {

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
    private String remoteDeviceAddress; // 接続相手のデバイスアドレス

    private static final int SELECT_DEVICE = 102; // デバイス選択画面からの結果を識別するコード

    // Handlerで処理するメッセージの種類を定義
    public static final int MESSAGE_STATE_CHANGED = 0; // 接続状態の変化
    public static final int MESSAGE_READ = 1;          // メッセージの受信
    public static final int MESSAGE_WRITE = 2;         // メッセージの送信
    public static final int MESSAGE_DEVICE_NAME = 3;   // 接続先デバイス名の受信
    public static final int MESSAGE_TOAST = 4;         // トーストメッセージの表示

    // メッセージに含めるキー
    public static final String DEVICE_NAME = "deviceName";
    public static final String TOAST = "toast";

    private String connectedDevice; // 接続中のデバイス名

    /**
     * Bluetooth関連の権限リクエストの結果を処理するためのランチャー。
     */
    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean connectGranted = result.getOrDefault(Manifest.permission.BLUETOOTH_CONNECT, false);
                boolean scanGranted = result.getOrDefault(Manifest.permission.BLUETOOTH_SCAN, false);

                if (connectGranted && scanGranted) {
                    enableBluetooth(); // 権限が許可されたらBluetooth有効化処理を呼び出す
                } else {
                    Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_LONG).show();
                }
            });

    /**
     * バックグラウンドのスレッド（ChatUtils）からUIの更新やトースト表示などの操作を行うためのハンドラです。
     */
    private final Handler handler = new Handler(message -> {
        switch (message.what) {
            case MESSAGE_STATE_CHANGED:
                // 接続状態が変化したときの処理
                switch (message.arg1) {
                    case ChatUtils.STATE_NONE:
                    case ChatUtils.STATE_LISTEN:
                        setState("Not Connected");
                        break;
                    case ChatUtils.STATE_CONNECTING:
                        setState("Connecting...");
                        break;
                    case ChatUtils.STATE_CONNECTED:
                        setState("Connected: " + connectedDevice);
                        break;
                }
                break;

            case MESSAGE_WRITE:
                // メッセージを送信したときの処理
                byte[] writeBuf = (byte[]) message.obj;
                String writeMessage = new String(writeBuf);
                ChatMessage writeChatMessage = new ChatMessage(writeMessage, true);
                chatMessages.add(writeChatMessage);
                adapterMainChat.notifyDataSetChanged();
                dbHelper.addMessage(writeChatMessage, bluetoothAdapter.getAddress(), remoteDeviceAddress);
                break;

            case MESSAGE_READ:
                // メッセージを受信したときの処理
                byte[] readBuf = (byte[]) message.obj;
                String readMessage = new String(readBuf, 0, message.arg1);
                ChatMessage readChatMessage = new ChatMessage(readMessage, false);
                chatMessages.add(readChatMessage);
                adapterMainChat.notifyDataSetChanged();
                dbHelper.addMessage(readChatMessage, remoteDeviceAddress, bluetoothAdapter.getAddress());
                break;

            case MESSAGE_DEVICE_NAME:
                // 接続先デバイス名を受信したときの処理
                connectedDevice = message.getData().getString(DEVICE_NAME);
                Toast.makeText(context, connectedDevice, Toast.LENGTH_SHORT).show();
                break;

            case MESSAGE_TOAST:
                // トーストメッセージを表示するときの処理
                Toast.makeText(context,
                        message.getData().getString(TOAST),
                        Toast.LENGTH_SHORT).show();
                break;
        }
        return true;
    });

    /**
     * アクションバーのサブタイトル（接続状態）を更新します。
     * @param subtitle 表示するテキスト
     */
    private void setState(CharSequence subtitle) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(subtitle);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;
        dbHelper = new DatabaseHelper(context);
        remoteDeviceAddress = getIntent().getStringExtra("deviceAddress");

        // ツールバーの設定
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initViews();
        initBluetooth();
        chatUtils = new ChatUtils(handler);

        // 接続先アドレスが渡されていれば、接続を試みる
        if (remoteDeviceAddress != null) {
            chatUtils.connect(bluetoothAdapter.getRemoteDevice(remoteDeviceAddress));
        }
    }

    /**
     * UIコンポーネント（リストビュー、編集テキスト、送信ボタン）を初期化します。
     */
    private void initViews() {
        listMainChat = findViewById(R.id.list);
        edCreateMessage = findViewById(R.id.editText);
        btnSendMessage = findViewById(R.id.btn);

        chatMessages = new ArrayList<>();
        adapterMainChat = new ChatAdapter(this, chatMessages);
        listMainChat.setAdapter(adapterMainChat);

        loadChatHistory(); // チャット履歴を読み込む

        // 送信ボタンのクリックリスナー
        btnSendMessage.setOnClickListener(v -> {
            String message = edCreateMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                edCreateMessage.setText("");
                chatUtils.write(message.getBytes());
            }
        });
    }

    /**
     * データベースからチャット履歴を読み込み、リストビューに表示します。
     */
    @SuppressLint("MissingPermission")
    private void loadChatHistory() {
        if (remoteDeviceAddress != null) {
            chatMessages.clear();
            List<ChatMessage> messages = dbHelper.getMessages(bluetoothAdapter.getAddress(), remoteDeviceAddress);
            chatMessages.addAll(messages);
            adapterMainChat.notifyDataSetChanged();
        }
    }

    /**
     * Bluetoothアダプターを初期化し、サポートされていない場合はアクティビティを終了します。
     */
    private void initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
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
        // 左上のBluetoothアイコン
        if (item.getItemId() == android.R.id.home) {
            enableBluetooth();
            return true;
        }

        // 右上の検索アイコン
        if (item.getItemId() == R.id.menu_search_devices) {
            Intent intent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(intent, SELECT_DEVICE);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // デバイス選択画面から戻ってきたときの結果を処理
        if (requestCode == SELECT_DEVICE && resultCode == RESULT_OK) {
            remoteDeviceAddress = data.getStringExtra("deviceAddress");
            chatUtils.connect(bluetoothAdapter.getRemoteDevice(remoteDeviceAddress));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Bluetoothが有効でない場合に有効にするようユーザーに要求します。
     * Android 12以上では必要な権限も要求します。
     */
    @SuppressLint("MissingPermission")
    private void enableBluetooth() {
        // Android 12 (API 31) 以上の場合
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

                // 権限をリクエスト
                permissionLauncher.launch(new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                });
                return; // 権限リクエストの結果を待つ
            }
        }

        // 権限が許可されている（または古いAndroid）場合
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent); // システムの有効化ダイアログを表示
        }

        // デバイスが検出可能モードでない場合
        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // アクティビティが破棄されるときに、ChatUtilsを停止
        if (chatUtils != null) {
            chatUtils.stop();
        }
    }
}