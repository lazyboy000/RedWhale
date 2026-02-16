package ocean.RedWhale;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * ログイン画面を処理するアクティビティです。
 * ユーザー認証（現在はプレースホルダー）を管理します。
 */
public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // UI要素への参照を取得します。
        EditText etEmail = findViewById(R.id.et_email);
        EditText etPassword = findViewById(R.id.et_password);
        Button btnLogin = findViewById(R.id.btn_login);
        TextView tvSignUp = findViewById(R.id.tv_signup);
        TextView tvForgot = findViewById(R.id.tv_forgot_password);

        // ログインボタンのクリックリスナー
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // 基本的な入力検証
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // 簡単なメール形式のチェック（任意）
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                return;
            }

            // ログイン成功の処理
            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();

            // HomeActivityに画面遷移します。
            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();  // 戻るボタンでログイン画面に戻れないように終了させます。
        });

        // 新規登録テキストのクリックリスナー
        tvSignUp.setOnClickListener(v -> {
            Toast.makeText(this, "Sign Up - Coming soon!", Toast.LENGTH_LONG).show();
            // 後で実装: startActivity(new Intent(this, SignUpActivity.class));
        });

        // パスワード忘れテキストのクリックリスナー
        tvForgot.setOnClickListener(v -> {
            Toast.makeText(this, "Forgot Password? Email reset sent to your account.", Toast.LENGTH_LONG).show();
            // 後で実装: パスワードリセット用のダイアログか画面を開く
        });
    }
}