package ocean.RedWhale;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.Nullable;

/**
 * メッシュネットワークをバックグラウンドで維持するためのサービスです。
 * アプリが閉じられてもパケットの中継（リレー）を継続します。
 */
public class BluetoothMeshService extends Service {
    private static final String CHANNEL_ID = "MeshServiceChannel";
    
    // 全体で共有するChatUtilsのインスタンス（メッシュノードとして機能）
    private static ChatUtils meshChatUtils;
    // UIからのコールバックを受け取るためのハンドラ
    private static Handler uiHandler;

    private static final Handler serviceHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (uiHandler != null) {
                // UIが接続されていれば、メッセージを転送します
                Message newMsg = Message.obtain();
                newMsg.copyFrom(msg);
                uiHandler.sendMessage(newMsg);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        
        // 永続的な通知を表示して、サービスをフォアグラウンドで実行します
        Notification.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        Notification notification = builder
                .setContentTitle("RedWhale Mesh Network")
                .setContentText("バックグラウンドでメッシュネットワークに接続中...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        
        startForeground(1, notification);

        if (meshChatUtils == null) {
            meshChatUtils = new ChatUtils(serviceHandler, this);
            meshChatUtils.start(); // サーバーとして待受開始
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // メッシュ機能を持続させるため、再起動を許可します
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Bound Serviceとしては使用しません
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (meshChatUtils != null) {
            meshChatUtils.stop();
            meshChatUtils = null;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Mesh Network Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    public static void setUiHandler(Handler handler) {
        uiHandler = handler;
        // 状態の同期
        if (uiHandler != null && meshChatUtils != null) {
            uiHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGED, meshChatUtils.getState(), -1).sendToTarget();
        }
    }

    public static ChatUtils getChatUtils() {
        return meshChatUtils;
    }
}
