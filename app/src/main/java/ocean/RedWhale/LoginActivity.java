package ocean.RedWhale;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Looper;

/**
 * アプリの開始地点（スプラッシュ画面）です。
 * ユーザーが登録済みかどうかを確認し、適切な画面へ案内します。
 */
public class LoginActivity extends AppCompatActivity {

    private boolean isNavigating = false;
    private final Handler splashHandler = new Handler(Looper.getMainLooper());
    
    // スプラッシュ表示後に実行されるナビゲーション処理
    private final Runnable navigationRunnable = () -> {
        if (isFinishing() || isNavigating) return;
        isNavigating = true;

        // バックグラウンドで登録状況をチェック
        new Thread(() -> {
            try {
                // 1. 暗号化キー（ID）が存在するか確認
                IdentityManager identityManager = new IdentityManager(LoginActivity.this);
                boolean hasIdentity = identityManager.exists();

                // 2. ユーザー名が保存されているか確認
                SharedPreferences prefs = getSharedPreferences("RedWhalePrefs", MODE_PRIVATE);
                String displayName = prefs != null ? prefs.getString("display_name", null) : null;

                // 3. 判定結果に基づいて画面を切り替え
                runOnUiThread(() -> {
                    Intent intent;
                    if (hasIdentity && displayName != null) {
                        // 登録済みならホーム画面（チャット一覧）へ
                        intent = new Intent(LoginActivity.this, HomeActivity.class);
                    } else {
                        // 未登録ならセットアップ画面へ
                        intent = new Intent(LoginActivity.this, SignUpActivity.class);
                    }
                    startActivity(intent);
                    finish();
                });
            } catch (Exception e) {
                Log.e("LoginActivity", "画面遷移に失敗", e);
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "起動エラー: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    isNavigating = false;
                });
            }
        }).start();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 2秒間ロゴを表示した後にナビゲーションを実行
        splashHandler.postDelayed(navigationRunnable, 2000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // メモリリーク防止のため、未実行のタスクを削除
        splashHandler.removeCallbacks(navigationRunnable);
    }
}