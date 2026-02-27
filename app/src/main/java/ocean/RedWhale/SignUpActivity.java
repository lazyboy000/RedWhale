package ocean.RedWhale;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 初回起動時のセットアップ（ユーザー登録）画面です。
 * ユーザー名を入力し、暗号化用の鍵ペアを生成します。
 */
public class SignUpActivity extends AppCompatActivity {

    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // 画面の部品を取得
        EditText etDisplayName = findViewById(R.id.et_display_name);
        Button btnGetStarted = findViewById(R.id.btn_signup_page);

        if (btnGetStarted == null || etDisplayName == null) return;

        // 「はじめる」ボタンが押された時の処理
        btnGetStarted.setOnClickListener(v -> {
            if (isProcessing) return;
            isProcessing = true;
            
            // 入力された名前を取得し、前後の空白を削除
            String displayName = etDisplayName.getText().toString().trim();

            // 名前が空の場合はエラー表示
            if (displayName.isEmpty()) {
                Toast.makeText(this, "表示名（ニックネーム）を入力してください", Toast.LENGTH_SHORT).show();
                isProcessing = false;
                return;
            }

            // アイデンティティ（鍵）の生成と保存をバックグラウンドで実行
            new Thread(() -> {
                try {
                    // 暗号化に使用する鍵ペアを生成
                    IdentityManager identityManager = new IdentityManager(SignUpActivity.this);
                    identityManager.ensureIdentityExists();
                    String address = identityManager.getIdentityAddress();

                    // UIスレッドで画面遷移を実行
                    runOnUiThread(() -> {
                        if (address == null) {
                            Toast.makeText(SignUpActivity.this, "鍵の生成に失敗しました", Toast.LENGTH_SHORT).show();
                            isProcessing = false;
                            return;
                        }

                        // 表示名をローカルに保存
                        SharedPreferences prefs = getSharedPreferences("RedWhalePrefs", MODE_PRIVATE);
                        if (prefs != null) {
                            prefs.edit().putString("display_name", displayName).apply();
                        }

                        // ホーム画面（チャット一覧）へ直接移動
                        Intent intent = new Intent(SignUpActivity.this, HomeActivity.class);
                        // 戻るボタンでこの画面に戻れないようにスタックをクリア
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(SignUpActivity.this, "エラー: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        isProcessing = false;
                    });
                }
            }).start();
        });
    }
}