package ocean.RedWhale;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SignUpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        EditText etEmail = findViewById(R.id.et_email_signup);
        EditText etPassword = findViewById(R.id.et_password_signup);
        EditText etConfirmPassword = findViewById(R.id.et_confirm_password_signup);
        Button btnSignUp = findViewById(R.id.btn_signup_page);
        TextView tvLoginLink = findViewById(R.id.tv_login_link);

        btnSignUp.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                return;
            }

            // For now, just show a success message and go to LoginActivity
            Toast.makeText(this, "Sign up successful!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        tvLoginLink.setOnClickListener(v -> {
            // Go back to login
            finish();
        });
    }
}
