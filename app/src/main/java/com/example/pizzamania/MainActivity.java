package com.example.pizzamania;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "loginPrefs";
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        if (isUserLoggedIn()) {
            startActivity(new Intent(this, MainMenuActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        TextView text = findViewById(R.id.Textbtn);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            text.setText(Html.fromHtml("Not Registered Yet? <u>Sign up</u>", Html.FROM_HTML_MODE_LEGACY));
        } else {
            text.setText(Html.fromHtml("Not Registered Yet? <u>Sign up</u>"));
        }
    }

    private boolean isUserLoggedIn() {
        FirebaseUser user = mAuth.getCurrentUser();
        boolean remember = sharedPreferences.getBoolean("remember", false);
        String email = sharedPreferences.getString("email", "");
        String password = sharedPreferences.getString("password", "");

        // Only consider logged-in if user is registered & Remember Me is true
        return (user != null) || (remember && !email.isEmpty() && !password.isEmpty());
    }

    public void goToSignUp(View view) {
        startActivity(new Intent(this, SignUpActivity.class));
    }

    public void goToSignIn(View view) {
        startActivity(new Intent(this, SignInActivity.class));
    }
}
