package com.example.pizzamania;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignInActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private CheckBox cbRemember;
    private FirebaseAuth mAuth;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "loginPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        etEmail = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnSignIn);
        cbRemember = findViewById(R.id.cbRemember);

        btnLogin.setOnClickListener(v -> loginUser());

        // Auto-fill email/password if Remember Me was previously selected
        boolean remember = sharedPreferences.getBoolean("remember", false);
        if (remember) {
            etEmail.setText(sharedPreferences.getString("email", ""));
            etPassword.setText(sharedPreferences.getString("password", ""));
            cbRemember.setChecked(true);
        }
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
                        // Save Remember Me preference
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        if (cbRemember.isChecked()) {
                            editor.putBoolean("remember", true);
                            editor.putString("email", email);
                            editor.putString("password", password);
                        } else {
                            editor.clear();
                        }
                        editor.apply();

                        // Go to MainMenuActivity
                        startActivity(new Intent(SignInActivity.this, MainMenuActivity.class));
                        finish();
                    } else {
                        Toast.makeText(SignInActivity.this, "Incorrect Username Or Password! " ,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
