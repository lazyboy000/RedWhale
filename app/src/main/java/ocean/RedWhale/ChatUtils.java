package ocean.RedWhale;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Bluetoothチャットの接続とデータ送受信を管理するユーティリティクラスです。
 * 接続の待受、接続の試行、データ送受信のスレッドを管理します。
 */
public class ChatUtils {
    private final Handler handler;
    private BluetoothAdapter bluetoothAdapter;
    private ConnectThread connectThread;
    private AcceptThread acceptThread;
    private ConnectedThread connectedThread;

    // Bluetooth SPP (Serial Port Profile)のための標準UUID
    private final UUID APP_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private final String APP_NAME = "BluetoothChatApp";

    // 接続状態を示す定数
    public static final int STATE_NONE = 0;       // 何もしていない状態
    public static final int STATE_LISTEN = 1;     // 接続を待ち受けている状態
    public static final int STATE_CONNECTING = 2; // 接続を試みている状態
    public static final int STATE_CONNECTED = 3;  // 接続済みの状態

    private int state;

    /**
     * コンストラクタ
     * @param handler UIスレッドと通信するためのハンドラ
     */
    public ChatUtils(Handler handler) {
        this.handler = handler;
        state = STATE_NONE;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * 現在の接続状態を取得します。
     * @return 接続状態
     */
    public int getState() {
        return state;
    }

    /**
     * 接続状態を同期的に設定し、UIスレッドに通知します。
     * @param state 設定する新しい状態
     */
    public synchronized void setState(int state) {
        this.state = state;
        handler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGED, state, -1).sendToTarget();
    }

    /**
     * チャットサービスを同期的に開始します。
     * 接続待受用のAcceptThreadを開始します。
     */
    private synchronized void start() {
        // 既存の接続試行スレッドがあればキャンセル
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        // 既存の接続待受スレッドがなければ新規作成して開始
        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }

        // 既存の接続済みスレッドがあればキャンセル
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_LISTEN);
    }

    /**
     * すべてのスレッドを同期的に停止します。
     */
    public synchronized void stop() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * 指定されたデバイスへの接続を開始します。
     * @param device 接続するBluetoothDevice
     */
    public void connect(BluetoothDevice device) {
        // 接続中のスレッドがあればキャンセル
        if (state == STATE_CONNECTING) {
            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
        }

        // 新しい接続試行スレッドを作成して開始
        connectThread = new ConnectThread(device);
        connectThread.start();

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_CONNECTING);
    }

    /**
     * 接続中のデバイスにデータを書き込みます。
     * @param buffer 書き込むデータ
     */
    public void write(byte[] buffer) {
        ConnectedThread connThread;
        synchronized (this) {
            if (state != STATE_CONNECTED) {
                return;
            }
            connThread = connectedThread;
        }

        try {
            // メッセージを暗号化してから書き込む
            String message = new String(buffer);
            String encryptedMessage = Security.encrypt(message);
            connThread.write(encryptedMessage.getBytes());
        } catch (Exception e) {
            Log.e("ChatUtils->write", e.toString());
        }
    }

    /**
     * サーバーソケットとして着信接続を待ち受けるスレッドです。
     */
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;

        @SuppressLint("MissingPermission")
        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                // RFCOMMチャネルでリッスンするサーバーソケットを作成
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID);
            } catch (IOException e) {
                Log.e("Accept->Constructor", e.toString());
            }
            serverSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            try {
                // 接続があるまでブロック
                socket = serverSocket.accept();
            } catch (IOException e) {
                Log.e("Accept->Run", e.toString());
                try {
                    serverSocket.close();
                } catch (IOException e1) {
                    Log.e("Accept->Close", e1.toString());
                }
            }

            if (socket != null) {
                // 状態に応じて接続を処理
                switch (state) {
                    case STATE_LISTEN:
                    case STATE_CONNECTING:
                        connected(socket, socket.getRemoteDevice());
                        break;
                    case STATE_NONE:
                    case STATE_CONNECTED:
                        try {
                            socket.close();
                        } catch (IOException e) {
                            Log.e("Accept->CloseSocket", e.toString());
                        }
                        break;
                }
            }
        }

        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e("Accept->CloseServer", e.toString());
            }
        }
    }

    /**
     * リモートデバイスへの接続を試みるクライアント側のスレッドです。
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        @SuppressLint("MissingPermission")
        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket tmp = null;
            try {
                // リモートデバイスに接続するためのソケットを作成
                tmp = device.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                Log.e("Connect->Constructor", e.toString());
            }
            socket = tmp;
        }

        @SuppressLint("MissingPermission")
        public void run() {
            try {
                // 接続を試みる（ブロックされる可能性あり）
                socket.connect();
            } catch (IOException e) {
                Log.e("Connect->Run", e.toString());
                try {
                    socket.close();
                } catch (IOException e1) {
                    Log.e("Connect->CloseSocket", e1.toString());
                }
                connectionFailed(); // 接続失敗を通知
                return;
            }

            synchronized (ChatUtils.this) {
                connectThread = null;
            }

            // 接続成功
            connected(socket, device);
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("Connect->Cancel", e.toString());
            }
        }
    }

    /**
     * 確立された接続を介してデータの送受信を処理するスレッドです。
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e("Connected->Streams", e.toString());
            }
            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            // データ受信を常に待機
            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    // 受信したメッセージを復号
                    String encryptedMessage = new String(buffer, 0, bytes);
                    String decryptedMessage = Security.decrypt(encryptedMessage);
                    // UIスレッドにメッセージを送信
                    handler.obtainMessage(MainActivity.MESSAGE_READ, decryptedMessage.length(), -1, decryptedMessage.getBytes()).sendToTarget();
                } catch (Exception e) {
                    connectionLost(); // 接続が失われた
                    break;
                }
            }
        }

        /**
         * データをリモートデバイスに書き込みます。
         * @param buffer 書き込むデータ
         */
        public void write(byte[] buffer) {
            try {
                outputStream.write(buffer);
                // UIスレッドに書き込んだメッセージを送信
                handler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                Log.e("Connected->Write", e.toString());
            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("Connected->Cancel", e.toString());
            }
        }
    }

    /**
     * 接続が失われたことをUIに通知し、待受状態に戻ります。
     */
    private void connectionLost() {
        Message message = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Connection Lost");
        message.setData(bundle);
        handler.sendMessage(message);

        // 待受モードに戻る
        ChatUtils.this.start();
    }

    /**
     * 接続に失敗したことをUIに通知し、待受状態に戻ります。
     */
    private synchronized void connectionFailed() {
        Message message = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Cant connect to the device");
        message.setData(bundle);
        handler.sendMessage(message);

        // 待受モードに戻る
        ChatUtils.this.start();
    }

    /**
     * 接続が成功したときの処理を同期的に行います。
     * @param socket 接続されたBluetoothSocket
     * @param device 接続されたBluetoothDevice
     */
    @SuppressLint("MissingPermission")
    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        // 既存のスレッドをキャンセル
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        // 新しいConnectedThreadを開始
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        // 接続先デバイス名をUIに通知
        Message message = handler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.DEVICE_NAME, device.getName());
        message.setData(bundle);
        handler.sendMessage(message);

        setState(STATE_CONNECTED);
    }
}
