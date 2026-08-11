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

import com.example.comrst.MatchmakingActivity;
import com.example.comrst.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    // UI elements
    private EditText emailInput, passwordInput;
    private Button loginButton;
    private TextView goToRegister;
    private ProgressBar progressBar;

    // Firebase Auth instance
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Link UI elements
        emailInput    = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton   = findViewById(R.id.loginButton);
        goToRegister  = findViewById(R.id.goToRegister);
        progressBar   = findViewById(R.id.progressBar);

        // Login button click
        loginButton.setOnClickListener(v -> loginUser());

        // Go to Register screen
        goToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // If user is already logged in, go straight to Matchmaking
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            goToMatchmaking();
        }
    }

    private void loginUser() {
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

        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);

        // Firebase sign in
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                loginButton.setEnabled(true);

                if (task.isSuccessful()) {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                    goToMatchmaking();
                } else {
                    Toast.makeText(this, "Login failed: " + task.getException().getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });
    }

    private void goToMatchmaking() {
        startActivity(new Intent(LoginActivity.this, MatchmakingActivity.class));
        finish(); // Close login screen so user can't go back
    }
}
