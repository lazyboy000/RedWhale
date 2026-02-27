package ocean.RedWhale;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.StrictMode;

/**
 * アプリ全体の初期設定を行うクラスです。
 */
public class RedWhaleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // デバッグモードの判定
        boolean isDebuggable = (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));

        // デバッグ時のみ、メインスレッドでの不適切な処理（重い通信など）を検知する設定を有効化
        if (isDebuggable) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }
    }
}