package Authentication_Advanced;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.comrst.R;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    // UI elements
    private EditText emailInput, passwordInput;
    private Button registerButton;
    private TextView goToLogin;
    private ProgressBar progressBar;

    // Firebase Auth
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Link UI elements
        emailInput      = findViewById(R.id.emailInput);
        passwordInput   = findViewById(R.id.passwordInput);
        registerButton  = findViewById(R.id.registerButton);
        goToLogin       = findViewById(R.id.goToLogin);
        progressBar     = findViewById(R.id.progressBar);

        // Register button click
        registerButton.setOnClickListener(v -> registerUser());

        // Go back to Login screen
        goToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String email    = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Basic validation
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Enter your email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Enter your password");
            return;
        }
        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            return;
        }

        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        registerButton.setEnabled(false);

        // Firebase create user
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                registerButton.setEnabled(true);

                if (task.isSuccessful()) {
                    Toast.makeText(this, "Account created! Please login.", Toast.LENGTH_SHORT).show();
                    // Go to Login after registration
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                } else {
                    Toast.makeText(this, "Registration failed: " + task.getException().getMessage(),
                            Toast.LENGTH_LONG).show();
                    System.out.println(task.getException().getMessage());
                }
            });
    }
}
