package com.example.pizzamania;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignInActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin; //btnRegister;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // 0 instance
        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnSignIn);
        //btnRegister = findViewById(R.id.btnSignup);

        // Login button click
        btnLogin.setOnClickListener(v -> loginUser());

        // Go to registration screen
        //btnRegister.setOnClickListener(v -> {
            //Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
            //startActivity(intent);
        //});
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password required");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(SignInActivity.this, "Login successful", Toast.LENGTH_SHORT).show();

                        // Go to Home/Dashboard screen
                        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(SignInActivity.this, "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
