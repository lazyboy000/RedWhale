package ocean.RedWhale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Set;

/**
 * 周辺のBluetoothデバイスをスキャン（検索）し、一覧表示するための画面です。
 * ユーザーが新しいデバイスを見つけて接続し、チャットを開始できるようにします。
 */
public class DeviceListActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1001;
    // 画面に表示するリスト（ペアリング済みデバイス用と、新しく見つけたデバイス用）
    private ListView listPairedDevices, listAvailableDevices;
    
    // スキャン中にくるくる回るローディング表示
    private ProgressBar progressScanDevices;

    // リストにデータを表示するためのアダプター
    private ArrayAdapter<String> adapterPairedDevices, adapterAvailableDevices;
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private DatabaseHelper dbHelper;

    /**
     * 画面が作られたときに最初に呼ばれる処理です。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        context = this;
        dbHelper = new DatabaseHelper(context);
        init();
    }

    /**
     * 画面の部品やBluetoothの設定を初期化し、過去にペアリングしたデバイスを一覧表示します。
     * また、新しいデバイスを見つけるための「耳（BroadcastReceiver）」を登録します。
     */
    @SuppressLint("MissingPermission")
    private void init() {
        listPairedDevices = findViewById(R.id.list_paired_devices);
        listAvailableDevices = findViewById(R.id.list_available_devices);
        progressScanDevices = findViewById(R.id.progress_scan_devices);
        TextView tvEmptyPaired = findViewById(R.id.tv_empty_paired);
        TextView tvEmptyAvailable = findViewById(R.id.tv_empty_available);

        // 各リスト用のアダプターを作成します。
        adapterPairedDevices = new ArrayAdapter<>(context, R.layout.device_list_item);
        adapterAvailableDevices = new ArrayAdapter<>(context, R.layout.device_list_item);

        listPairedDevices.setAdapter(adapterPairedDevices);
        listAvailableDevices.setAdapter(adapterAvailableDevices);

        // 新しく見つけたデバイスのリストがタップされたときの動作です。
        listAvailableDevices.setOnItemClickListener((adapterView, view, i, l) -> {
            // タップされた項目のテキスト（文字列）から、末尾の17文字（MACアドレス）を切り出します。
            String info = ((TextView) view).getText().toString();
            String address = info.substring(info.length() - 17);
            
            // アドレスを使ってBluetoothDeviceオブジェクトを取得し、友達追加ダイアログを表示します。
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            showAddFriendDialog(device);
        });

        // 端末のBluetooth機能にアクセスします。
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        // 過去にペアリング（接続設定）したことがあるデバイスの一覧を取得します。
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        // ペアリング済みのデバイスがあれば、それをリスト（上半分）に追加します。
        if (pairedDevices != null && pairedDevices.size() > 0) {
            tvEmptyPaired.setVisibility(View.GONE);
            for (BluetoothDevice device : pairedDevices) {
                // デバイスの名前とアドレスを改行でつなげて表示します。
                adapterPairedDevices.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            tvEmptyPaired.setVisibility(View.VISIBLE);
        }

        // Androidシステムから「新しいデバイスを見つけたよ」という通知を受け取るための設定をします。
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetoothDeviceListener, intentFilter);
        
        // 「スキャンが終わったよ」という通知を受け取るための設定をします。
        IntentFilter intentFilter1 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothDeviceListener, intentFilter1);

        // ペアリング済みデバイスのリストがタップされたときの動作です。
        listPairedDevices.setOnItemClickListener((adapterView, view, i, l) -> {
            // スキャン中であれば、スキャンをすぐに止めます（無駄な電池消費を防ぐため）。
            bluetoothAdapter.cancelDiscovery();

            String info = ((TextView) view).getText().toString();
            String address = info.substring(info.length() - 17);
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            showAddFriendDialog(device);
        });
    }

    /**
     * デバイスに接続する前に、「表示名」を決めて連絡先に追加するためのダイアログ（ポップアップ）を表示します。
     *
     * @param device 追加しようとしているBluetoothデバイス
     */
    @SuppressLint("MissingPermission")
    private void showAddFriendDialog(BluetoothDevice device) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("友達を追加");
        builder.setMessage(device.getName() + " の表示名を入力してください");

        // 名前を入力するテキストボックスを用意します。
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // 「追加」ボタンが押された時の処理
        builder.setPositiveButton("追加", (dialog, which) -> {
            String name = input.getText().toString();
            
            // 本当のシステムでは、最初の接続時に相手の「公開鍵」を受け取りますが、
            // ここでは仮のID（プレースホルダー）を入れておきます。
            String placeholderIdentity = "ID_" + device.getAddress();
            
            // Friendオブジェクトを作り、データベースに保存します。
            Friend friend = new Friend(0, name, device.getAddress(), placeholderIdentity);
            dbHelper.addFriend(friend);
            
            Toast.makeText(context, "友達を追加しました", Toast.LENGTH_SHORT).show();
            
            // チャット画面を開きます。
            startChat(device.getAddress(), name);
        });
        
        // 「キャンセル」ボタンが押された時の処理
        builder.setNegativeButton("キャンセル", (dialog, which) -> {
            dialog.cancel();
            // キャンセルしても、とりあえず一時的なチャットとして開きます。
            startChat(device.getAddress(), device.getName());
        });

        // ダイアログを画面に表示します。
        builder.show();
    }

    /**
     * 選ばれたデバイスのアドレスを、前の画面（HomeActivity）に「結果」として返します。
     * HomeActivityはこれを受け取って、実際のチャット画面（MainActivity）を開きます。
     *
     * @param address チャット相手のデバイスアドレス
     * @param name    相手の表示名
     */
    private void startChat(String address, String name) {
        Intent intent = new Intent();
        intent.putExtra("deviceAddress", address); // 結果のデータにアドレスを含めます
        intent.putExtra("CHAT_NAME", name);       // 名前も渡します
        setResult(Activity.RESULT_OK, intent);     // 「成功」として結果をセットします
        finish(); // この検索画面を閉じて、元の画面に戻ります
    }

    /**
     * AndroidシステムからのBluetooth関連の通知（Broadcast）を受け取るための「耳（リスナー）」です。
     * デバイスが見つかった時や、スキャンが終わった時に動きます。
     */
    private final BroadcastReceiver bluetoothDeviceListener = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // 新しいデバイスが見つかった場合
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // 通知の中からデバイス情報を取り出します。
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                
                // デバイスの名前があり、まだペアリングされていないものだけを「利用可能なデバイス」リストに追加します。
                if (device != null && device.getName() != null && device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    adapterAvailableDevices.add(device.getName() + "\n" + device.getAddress());
                    findViewById(R.id.tv_empty_available).setVisibility(View.GONE);
                }
                
            // スキャン（検索）が完了した場合
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // くるくる回るローディング表示を消します。
                progressScanDevices.setVisibility(View.GONE);
                
                // ひとつも見つからなかった場合
                if (adapterAvailableDevices.getCount() == 0) {
                    findViewById(R.id.tv_empty_available).setVisibility(View.VISIBLE);
                    Toast.makeText(context, "新しいデバイスが見つかりませんでした", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "デバイスをタップしてチャットを開始してください", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    /**
     * 画面の右上にメニュー項目（スキャンボタン）を表示します。
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * メニュー項目が選ばれたときの動作です。
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        // スキャンボタンが押された場合
        if (itemId == R.id.menu_scan_device) {
            scanDevices();
            return true;
        } else if (itemId == R.id.menu_manual_entry) {
            showManualEntryDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * MACアドレスを手動で入力するダイアログを表示します。
     */
    private void showManualEntryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("手動でアドレスを入力");
        builder.setMessage("相手のBluetooth MACアドレス（例: 00:11:22:33:44:55）を入力してください");

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("次へ", (dialog, which) -> {
            String address = input.getText().toString().trim().toUpperCase();
            if (BluetoothAdapter.checkBluetoothAddress(address)) {
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
                showAddFriendDialog(device);
            } else {
                Toast.makeText(context, "無効なMACアドレス形式です", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("キャンセル", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private android.bluetooth.le.BluetoothLeScanner bleScanner;
    private final android.bluetooth.le.ScanCallback bleScanCallback = new android.bluetooth.le.ScanCallback() {
        @Override
        public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device != null && device.getName() != null && device.getBondState() != BluetoothDevice.BOND_BONDED) {
                String deviceInfo = device.getName() + "\n" + device.getAddress();
                boolean exists = false;
                for (int i = 0; i < adapterAvailableDevices.getCount(); i++) {
                    if (adapterAvailableDevices.getItem(i).equals(deviceInfo)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    adapterAvailableDevices.add(deviceInfo);
                    findViewById(R.id.tv_empty_available).setVisibility(View.GONE);
                }
            }
        }
    };

    /**
     * 周辺のBluetoothデバイスを探す「スキャン」処理を開始します。
     */
    @SuppressLint("MissingPermission")
    private void scanDevices() {
        if (!checkPermissions()) {
            requestPermissions();
            return;
        }

        progressScanDevices.setVisibility(View.VISIBLE);
        findViewById(R.id.tv_empty_available).setVisibility(View.GONE);
        adapterAvailableDevices.clear();
        Toast.makeText(context, "BLEスキャンを開始しました", Toast.LENGTH_SHORT).show();

        if (bleScanner == null) {
            bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
        if (bleScanner != null) {
            bleScanner.startScan(bleScanCallback);
            // Stop scan after 10 seconds
            new android.os.Handler().postDelayed(() -> {
                if (bleScanner != null) {
                    try {
                        bleScanner.stopScan(bleScanCallback);
                    } catch (Exception e) {
                        // Scan already stopped or adapter disabled
                    }
                }
                progressScanDevices.setVisibility(View.GONE);
                if (adapterAvailableDevices.getCount() == 0) {
                    findViewById(R.id.tv_empty_available).setVisibility(View.VISIBLE);
                    Toast.makeText(context, "新しいデバイスが見つかりませんでした", Toast.LENGTH_SHORT).show();
                }
            }, 10000);
        }
    }

    private boolean checkPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                scanDevices();
            } else {
                Toast.makeText(this, "Bluetoothスキャンには権限が必要です", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 画面が閉じられるときに呼ばれる処理です。
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 登録していたシステム通知の「耳（BroadcastReceiver）」を解除します。
        // これを忘れると、メモリリーク（アプリが重くなる原因）になります。
        if (bluetoothDeviceListener != null) {
            unregisterReceiver(bluetoothDeviceListener);
        }
    }
}