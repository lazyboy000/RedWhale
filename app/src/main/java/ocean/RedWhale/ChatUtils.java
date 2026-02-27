package ocean.RedWhale;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Base64;
import android.util.Log;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bluetooth Low Energy (BLE) を使用したメッシュネットワークエンジンです。
 * メッセージの暗号化、パケット分割、ルーティングを担当します。
 */
@SuppressLint("MissingPermission")
public class ChatUtils {
    private static final String TAG = "ChatUtils";

    private final Handler handler;
    private final Context context;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;

    private BluetoothLeAdvertiser advertiser;
    private BluetoothLeScanner scanner;
    private BluetoothGattServer gattServer;

    private final List<BluetoothGatt> connectedClients = new CopyOnWriteArrayList<>();
    private final List<BluetoothDevice> connectedServerDevices = new CopyOnWriteArrayList<>();

    private final MessageRouter messageRouter;

    // サービスのUUID
    public static final UUID SERVICE_UUID = UUID.fromString("0000B81D-0000-1000-8000-00805F9B34FB");
    public static final UUID CHAR_RX_UUID = UUID.fromString("0000B81E-0000-1000-8000-00805F9B34FB");
    public static final UUID CHAR_TX_UUID = UUID.fromString("0000B81F-0000-1000-8000-00805F9B34FB");

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private int state;
    private byte[] localAddressHash;
    private String localIdentityAddress;
    private byte[] remoteAddressHash;
    private PrivateKey privateKey;
    private boolean isReady = false;

    public ChatUtils(Handler handler, Context context) {
        this.handler = handler;
        this.context = context;
        this.state = STATE_NONE;

        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }

        messageRouter = new MessageRouter();

        // 重い初期化処理をバックグラウンドで実行
        new Thread(() -> {
            IdentityManager identityManager = new IdentityManager(context);
            if (identityManager.exists()) {
                localIdentityAddress = identityManager.getIdentityAddress();
                localAddressHash = IdentityManager.getAddressHash(localIdentityAddress);
                privateKey = identityManager.getPrivateKey();
                messageRouter.setLocalAddressHash(localAddressHash);
            }
            setupMessageRouter();
            isReady = true;
            Log.i(TAG, "ChatUtilsの初期化が完了しました");
        }).start();
    }

    /**
     * メッセージルーターのリスナーを設定します。
     */
    private void setupMessageRouter() {
        messageRouter.setMessageListener(new MessageRouter.MessageListener() {
            @Override
            public void onMessageReceived(String encryptedMessage, byte[] senderHash) {
                // メッセージ受信時の復号処理
                String decrypted = performE2EDecryption(encryptedMessage, senderHash);
                if (decrypted != null) {
                    if (handler != null) {
                        handler.obtainMessage(MainActivity.MESSAGE_READ, decrypted.length(), -1, decrypted.getBytes())
                                .sendToTarget();
                    }
                    sendAck(senderHash); // 受信確認を送信
                }
            }

            @Override
            public void onRelayRequest(MessagePacket packet) {
                // 他のノードへのメッセージ転送（リレー）
                Log.i(TAG, "メッセージを転送します: msgId " + packet.msgId);
                routePacket(packet);
            }
        });
    }

    /**
     * 受信したメッセージをRSA/AESを使用して復号します。
     */
    private String performE2EDecryption(String packagedData, byte[] senderHash) {
        if (!isReady || privateKey == null) return null;
        try {
            byte[] rawData = Base64.decode(packagedData, Base64.NO_WRAP);
            if (rawData.length < 512) return null;

            byte[] encryptedAesKey = new byte[256];
            byte[] signatureBytes = new byte[256];
            byte[] encryptedPayload = new byte[rawData.length - 512];

            System.arraycopy(rawData, 0, encryptedAesKey, 0, 256);
            System.arraycopy(rawData, 256, signatureBytes, 0, 256);
            System.arraycopy(rawData, 512, encryptedPayload, 0, encryptedPayload.length);

            // 共通鍵をRSAで復号
            byte[] aesKey = EncryptionManager.decryptAESKeyWithRSA(encryptedAesKey, privateKey);
            if (aesKey == null) return null;

            // 本文をAESで復号
            String decryptedString = EncryptionManager.decrypt(Base64.encodeToString(encryptedPayload, Base64.NO_WRAP), aesKey);
            if (decryptedString == null) return null;

            // 署名の検証
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            List<Friend> friends = dbHelper.getAllFriends();
            PublicKey senderPublicKey = null;
            for (Friend f : friends) {
                if (Arrays.equals(IdentityManager.getAddressHash(f.getIdentityAddress()), senderHash)) {
                    senderPublicKey = IdentityManager.getPublicKeyFromAddress(f.getIdentityAddress());
                    break;
                }
            }

            if (senderPublicKey != null) {
                boolean isValid = EncryptionManager.verifySignature(decryptedString.getBytes(), signatureBytes, senderPublicKey);
                if (!isValid) return null;
            }
            return decryptedString;
        } catch (Exception e) {
            Log.e(TAG, "復号エラー", e);
            return null;
        }
    }

    /**
     * 送信メッセージを暗号化します。
     */
    private String performE2EEncryption(String message, byte[] destHash) {
        if (!isReady || privateKey == null) return null;
        try {
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            List<Friend> friends = dbHelper.getAllFriends();
            PublicKey destPublicKey = null;
            for (Friend f : friends) {
                if (Arrays.equals(IdentityManager.getAddressHash(f.getIdentityAddress()), destHash)) {
                    destPublicKey = IdentityManager.getPublicKeyFromAddress(f.getIdentityAddress());
                    break;
                }
            }
            if (destPublicKey == null) return null;

            byte[] aesKey = EncryptionManager.generateRandomKey();
            String encryptedPayloadString = EncryptionManager.encrypt(message, aesKey);
            byte[] encryptedPayload = Base64.decode(encryptedPayloadString, Base64.NO_WRAP);
            byte[] encryptedAesKey = EncryptionManager.encryptAESKeyWithRSA(aesKey, destPublicKey);
            byte[] signatureBytes = EncryptionManager.signData(message.getBytes(), privateKey);

            byte[] packagedData = new byte[512 + encryptedPayload.length];
            System.arraycopy(encryptedAesKey, 0, packagedData, 0, 256);
            System.arraycopy(signatureBytes, 0, packagedData, 256, 256);
            System.arraycopy(encryptedPayload, 0, packagedData, 512, encryptedPayload.length);

            return Base64.encodeToString(packagedData, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "暗号化エラー", e);
            return null;
        }
    }

    /**
     * 受信確認（ACK）パケットを送信します。
     */
    private void sendAck(byte[] destHash) {
        if (localAddressHash == null) return;
        MessagePacket ackPacket = new MessagePacket(MessagePacket.TYPE_ACK, 0, (short)0, (short)1, destHash, localAddressHash, (byte)10, System.currentTimeMillis(), new byte[0]);
        routePacket(ackPacket);
    }

    /**
     * パケットを接続中の全ノードに転送します。
     */
    private void routePacket(MessagePacket packet) {
        byte[] payload = packet.toBytes();
        // クライアントとして接続しているデバイスへ送信
        for (BluetoothGatt gatt : connectedClients) {
            BluetoothGattService service = gatt.getService(SERVICE_UUID);
            if (service != null) {
                BluetoothGattCharacteristic rx = service.getCharacteristic(CHAR_RX_UUID);
                if (rx != null) {
                    rx.setValue(payload);
                    gatt.writeCharacteristic(rx);
                }
            }
        }
        // サーバーとして接続されているデバイスへ通知
        if (gattServer != null) {
            BluetoothGattService service = gattServer.getService(SERVICE_UUID);
            if (service != null) {
                BluetoothGattCharacteristic tx = service.getCharacteristic(CHAR_TX_UUID);
                if (tx != null) {
                    tx.setValue(payload);
                    for (BluetoothDevice device : connectedServerDevices) {
                        gattServer.notifyCharacteristicChanged(device, tx, false);
                    }
                }
            }
        }
    }

    public synchronized int getState() {
        return state;
    }

    public synchronized void setState(int state) {
        this.state = state;
        if (handler != null) {
            handler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGED, state, -1).sendToTarget();
        }
    }

    /**
     * メッシュ機能を開始します。
     */
    public synchronized void start() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) return;
        setState(STATE_LISTEN);

        startGattServer();
        startAdvertising();
        startScanning();
    }

    /**
     * メッシュ機能を停止します。
     */
    public synchronized void stop() {
        stopScanning();
        stopAdvertising();
        stopGattServer();

        for (BluetoothGatt gatt : connectedClients) {
            gatt.disconnect();
            gatt.close();
        }
        connectedClients.clear();
        connectedServerDevices.clear();
        setState(STATE_NONE);
    }

    /**
     * 特定のデバイスへ直接接続を試みます。
     */
    public synchronized void connect(BluetoothDevice device) {
        device.connectGatt(context, false, gattClientCallback);
        setState(STATE_CONNECTING);
    }

    public void setRemoteAddressHash(byte[] hash) {
        this.remoteAddressHash = hash;
    }

    /**
     * メッセージをパケットに分割してネットワークへ送信します。
     */
    public void write(byte[] out) {
        if (!isReady) {
            Log.w(TAG, "ChatUtilsの準備ができていません");
            return;
        }
        byte[] dest = (remoteAddressHash != null) ? remoteAddressHash : new byte[32];
        String message = new String(out);
        String encrypted = performE2EEncryption(message, dest);

        if (encrypted != null) {
            List<MessagePacket> packets = messageRouter.preparePackets(encrypted, dest, localAddressHash);
            for (MessagePacket p : packets) {
                routePacket(p);
            }
            if (handler != null) {
                handler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, out).sendToTarget();
            }
        } else {
            if (handler != null) {
                Message msg = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString(MainActivity.TOAST, "送信に失敗しました（鍵が見つかりません）");
                msg.setData(bundle);
                handler.sendMessage(msg);
            }
        }
    }

    // GATTサーバー・アドバタイズ・スキャン関係の処理は省略（内部動作のため）
    private void startGattServer() {
        if (gattServer != null || bluetoothManager == null) return;
        gattServer = bluetoothManager.openGattServer(context, gattServerCallback);
        if (gattServer == null) return;

        BluetoothGattService service = new BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic rxChar = new BluetoothGattCharacteristic(CHAR_RX_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        BluetoothGattCharacteristic txChar = new BluetoothGattCharacteristic(CHAR_TX_UUID,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);
        
        service.addCharacteristic(rxChar);
        service.addCharacteristic(txChar);
        gattServer.addService(service);
    }

    private void stopGattServer() {
        if (gattServer != null) {
            gattServer.close();
            gattServer = null;
        }
    }

    private void startAdvertising() {
        if (bluetoothAdapter == null) return;
        advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (advertiser == null) return;

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(new ParcelUuid(SERVICE_UUID))
                .build();

        advertiser.startAdvertising(settings, data, advertiseCallback);
    }

    private void stopAdvertising() {
        if (advertiser != null) {
            advertiser.stopAdvertising(advertiseCallback);
        }
    }

    private void startScanning() {
        if (bluetoothAdapter == null) return;
        scanner = bluetoothAdapter.getBluetoothLeScanner();
        if (scanner == null) return;

        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(SERVICE_UUID))
                .build();

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .build();

        scanner.startScan(Arrays.asList(filter), settings, scanCallback);
    }

    private void stopScanning() {
        if (scanner != null) {
            scanner.stopScan(scanCallback);
        }
    }

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "アドバタイズ開始");
        }
        @Override
        public void onStartFailure(int errorCode) {
            Log.e(TAG, "アドバタイズ失敗: " + errorCode);
        }
    };

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            // スキャン結果の処理
        }
    };

    private final BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectedServerDevices.add(device);
                setState(STATE_CONNECTED);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectedServerDevices.remove(device);
                if (connectedServerDevices.isEmpty() && connectedClients.isEmpty()) {
                    setState(STATE_LISTEN);
                }
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            if (CHAR_RX_UUID.equals(characteristic.getUuid())) {
                messageRouter.handleIncomingPacket(value);
                if (responseNeeded) {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                }
            }
        }
    };

    private final BluetoothGattCallback gattClientCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectedClients.add(gatt);
                gatt.discoverServices();
                setState(STATE_CONNECTED);
                if (handler != null) {
                    Message msg = handler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
                    Bundle bundle = new Bundle();
                    bundle.putString(MainActivity.DEVICE_NAME, gatt.getDevice().getName() != null ? gatt.getDevice().getName() : "Unknown");
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectedClients.remove(gatt);
                gatt.close();
                if (connectedServerDevices.isEmpty() && connectedClients.isEmpty()) {
                    setState(STATE_LISTEN);
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service != null) {
                    BluetoothGattCharacteristic txChar = service.getCharacteristic(CHAR_TX_UUID);
                    if (txChar != null) {
                        gatt.setCharacteristicNotification(txChar, true);
                    }
                    gatt.requestMtu(512);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (CHAR_TX_UUID.equals(characteristic.getUuid())) {
                messageRouter.handleIncomingPacket(characteristic.getValue());
            }
        }
    };
}