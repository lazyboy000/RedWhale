package ocean.RedWhale;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResult;

import android.os.Build;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;
import android.Manifest;

public class MainActivity extends AppCompatActivity {

    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private ChatUtils chatUtils;

    private ListView listMainChat;
    private EditText edCreateMessage;
    private Button btnSendMessage;
    private ArrayAdapter<String> adapterMainChat;

    private static final int LOCATION_PERMISSION_REQUEST = 101;
    private static final int SELECT_DEVICE = 102;

    public static final int MESSAGE_STATE_CHANGED = 0;
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE = 2;
    public static final int MESSAGE_DEVICE_NAME = 3;
    public static final int MESSAGE_TOAST = 4;

    public static final String DEVICE_NAME = "deviceName";
    public static final String TOAST = "toast";

    private String connectedDevice;

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                Boolean connectGranted = result.getOrDefault(Manifest.permission.BLUETOOTH_CONNECT, false);
                Boolean scanGranted = result.getOrDefault(Manifest.permission.BLUETOOTH_SCAN, false);

                if (connectGranted && scanGranted) {
                    enableBluetooth();
                } else {
                    Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_LONG).show();
                }
            });
    private final Handler handler = new Handler(message -> {
        switch (message.what) {
            case MESSAGE_STATE_CHANGED:
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
                adapterMainChat.add("Me: " + new String((byte[]) message.obj));
                break;

            case MESSAGE_READ:
                adapterMainChat.add(
                        connectedDevice + ": " +
                                new String((byte[]) message.obj, 0, message.arg1)
                );
                break;

            case MESSAGE_DEVICE_NAME:
                connectedDevice = message.getData().getString(DEVICE_NAME);
                Toast.makeText(context, connectedDevice, Toast.LENGTH_SHORT).show();
                break;

            case MESSAGE_TOAST:
                Toast.makeText(context,
                        message.getData().getString(TOAST),
                        Toast.LENGTH_SHORT).show();
                break;
        }
        return true;
    });

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

        // Toolbar setup
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("RedWhale");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_bluetooth);
        }

        initViews();
        initBluetooth();
        chatUtils = new ChatUtils(handler);
    }

    private void initViews() {
        listMainChat = findViewById(R.id.list);
        edCreateMessage = findViewById(R.id.editText);
        btnSendMessage = findViewById(R.id.btn);

        adapterMainChat = new ArrayAdapter<>(
                this,
                R.layout.message_layout,
                R.id.tvMessage
        );
        listMainChat.setAdapter(adapterMainChat);

        btnSendMessage.setOnClickListener(v -> {
            String message = edCreateMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                edCreateMessage.setText("");
                chatUtils.write(message.getBytes());
            }
        });
    }

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

        // Top-left Bluetooth icon
        if (item.getItemId() == android.R.id.home) {
            enableBluetooth();
            return true;
        }

        // Top-right search icon
        if (item.getItemId() == R.id.menu_search_devices) {
            checkPermissions();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST
            );
        } else {
            startActivityForResult(
                    new Intent(this, DeviceListActivity.class),
                    SELECT_DEVICE
            );
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_DEVICE && resultCode == RESULT_OK) {
            String address = data.getStringExtra("deviceAddress");
            chatUtils.connect(bluetoothAdapter.getRemoteDevice(address));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                startActivityForResult(
                        new Intent(this, DeviceListActivity.class),
                        SELECT_DEVICE
                );

            } else {
                new AlertDialog.Builder(this)
                        .setMessage("Location permission is required")
                        .setPositiveButton("Grant",
                                (d, i) -> checkPermissions())
                        .setNegativeButton("Exit",
                                (d, i) -> finish())
                        .setCancelable(false)
                        .show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void enableBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

                // Request permissions
                permissionLauncher.launch(new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                });
                return; // Wait for permission result before proceeding
            }
        }

        // Permissions are OK (or on older Android) → proceed
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent); // This shows the system enable dialog
        }

        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatUtils != null) {
            chatUtils.stop();
        }
    }
}
