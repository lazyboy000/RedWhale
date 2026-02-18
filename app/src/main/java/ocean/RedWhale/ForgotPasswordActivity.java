package ocean.RedWhale;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ForgotPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        EditText etEmail = findViewById(R.id.et_email_forgot);
        Button btnReset = findViewById(R.id.btn_reset_password);
        TextView tvBackToLogin = findViewById(R.id.tv_back_to_login);

        btnReset.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                return;
            }

            // For now, just show a success message
            Toast.makeText(this, "Password reset link sent to " + email, Toast.LENGTH_LONG).show();
            finish();
        });

        tvBackToLogin.setOnClickListener(v -> {
            // Go back to login
            finish();
        });
    }
}
