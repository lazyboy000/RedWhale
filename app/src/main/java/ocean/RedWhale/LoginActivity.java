package ocean.RedWhale;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText etEmail = findViewById(R.id.et_email);
        EditText etPassword = findViewById(R.id.et_password);
        Button btnLogin = findViewById(R.id.btn_login);
        TextView tvSignUp = findViewById(R.id.tv_signup);
        TextView tvForgot = findViewById(R.id.tv_forgot_password);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // Basic validation
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Optional: simple email format check
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                return;
            }

            // Success path
            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();

            // Navigate to HomeActivity (your new screen with bottom navigation)
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();  // Close login so user can't return with back button
        });

        tvSignUp.setOnClickListener(v -> {
            Toast.makeText(this, "Sign Up - Coming soon!", Toast.LENGTH_LONG).show();
            // Later: startActivity(new Intent(this, SignUpActivity.class));
        });

        tvForgot.setOnClickListener(v -> {
            Toast.makeText(this, "Forgot Password? Email reset sent to your account.", Toast.LENGTH_LONG).show();
            // Later: open reset password dialog or screen
        });
    }
}