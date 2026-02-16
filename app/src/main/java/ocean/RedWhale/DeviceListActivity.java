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

import androidx.appcompat.app.AppCompatActivity;

import java.util.Set;

/**
 * Bluetoothデバイスをスキャンし、一覧表示するためのアクティビティです。
 * ユーザーはデバイスを選択してチャットを開始できます。
 */
public class DeviceListActivity extends AppCompatActivity {
    private ListView listPairedDevices, listAvailableDevices;
    private ProgressBar progressScanDevices;

    private ArrayAdapter<String> adapterPairedDevices, adapterAvailableDevices;
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        context = this;
        dbHelper = new DatabaseHelper(context);
        init();
    }

    /**
     * UIコンポーネントの初期化、アダプターの設定、ペアリング済みデバイスのリストアップを行います。
     * また、デバイス発見のためのBroadcastReceiverを登録します。
     */
    @SuppressLint("MissingPermission")
    private void init() {
        listPairedDevices = findViewById(R.id.list_paired_devices);
        listAvailableDevices = findViewById(R.id.list_available_devices);
        progressScanDevices = findViewById(R.id.progress_scan_devices);

        adapterPairedDevices = new ArrayAdapter<>(context, R.layout.device_list_item);
        adapterAvailableDevices = new ArrayAdapter<>(context, R.layout.device_list_item);

        listPairedDevices.setAdapter(adapterPairedDevices);
        listAvailableDevices.setAdapter(adapterAvailableDevices);

        // 利用可能なデバイスリストのクリックリスナー
        listAvailableDevices.setOnItemClickListener((adapterView, view, i, l) -> {
            String info = ((TextView) view).getText().toString();
            String address = info.substring(info.length() - 17);
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            showAddFriendDialog(device);
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        // ペアリング済みのデバイスがあればリストに追加
        if (pairedDevices != null && pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                adapterPairedDevices.add(device.getName() + "\n" + device.getAddress());
            }
        }

        // デバイス発見のためのインテントフィルターとBroadcastReceiverを登録
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetoothDeviceListener, intentFilter);
        IntentFilter intentFilter1 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothDeviceListener, intentFilter1);

        // ペアリング済みデバイスリストのクリックリスナー
        listPairedDevices.setOnItemClickListener((adapterView, view, i, l) -> {
            bluetoothAdapter.cancelDiscovery(); // スキャンを停止

            String info = ((TextView) view).getText().toString();
            String address = info.substring(info.length() - 17);
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            showAddFriendDialog(device);
        });
    }

    /**
     * 新しい友達を連絡先に追加するためのダイアログを表示します。
     * @param device 追加するBluetoothデバイス
     */
    @SuppressLint("MissingPermission")
    private void showAddFriendDialog(BluetoothDevice device) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Add Friend");
        builder.setMessage("Enter a name for " + device.getName());

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // 「追加」ボタンの処理
        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = input.getText().toString();
            Friend friend = new Friend(0, name, device.getAddress());
            dbHelper.addFriend(friend);
            Toast.makeText(context, "Friend added", Toast.LENGTH_SHORT).show();
            startChat(device.getAddress());
        });
        // 「キャンセル」ボタンの処理
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.cancel();
            startChat(device.getAddress());
        });

        builder.show();
    }

    /**
     * チャット画面を開始するために、選択したデバイスのアドレスを結果として返します。
     * @param address 開始するチャット相手のデバイスアドレス
     */
    private void startChat(String address) {
        Intent intent = new Intent();
        intent.putExtra("deviceAddress", address);
        setResult(Activity.RESULT_OK, intent);
        finish(); // このアクティビティを終了
    }

    /**
     * Bluetoothデバイスの発見やスキャンの終了をリッスンするBroadcastReceiverです。
     */
    private final BroadcastReceiver bluetoothDeviceListener = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // デバイスが見つかった場合
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && device.getName() != null && device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    adapterAvailableDevices.add(device.getName() + "\n" + device.getAddress());
                }
            // スキャンが終了した場合
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                progressScanDevices.setVisibility(View.GONE);
                if (adapterAvailableDevices.getCount() == 0) {
                    Toast.makeText(context, "No new devices found", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Click on the device to start the chat", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        // 「デバイスをスキャン」メニューが選択された場合
        if (itemId == R.id.menu_scan_device) {
            scanDevices();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Bluetoothデバイスのスキャンを開始します。
     */
    @SuppressLint("MissingPermission")
    private void scanDevices() {
        progressScanDevices.setVisibility(View.VISIBLE);
        adapterAvailableDevices.clear();
        Toast.makeText(context, "Scan started", Toast.LENGTH_SHORT).show();

        // 既にスキャン中であればキャンセル
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        // 新しいスキャンを開始
        bluetoothAdapter.startDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // BroadcastReceiverの登録を解除
        if (bluetoothDeviceListener != null) {
            unregisterReceiver(bluetoothDeviceListener);
        }
    }
}

