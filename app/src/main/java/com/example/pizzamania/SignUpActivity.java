package com.example.pizzamania;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private static final int AUTOCOMPLETE_REQUEST_CODE = 200;

    private EditText etName, etPhone, etEmail, etAddress, etPassword, etConfirmPassword;
    private Spinner spinnerCountryCode;
    private Button btnRegister, tvUploadPhoto;
    private ImageView imgProfile, btnSelectAddress;

    private Uri imageUri;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize Places SDK
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyBw0C78mfaDpspwyJtdfm8Ubg5mp3QAiLc");
        }

        // Initialize views
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        spinnerCountryCode = findViewById(R.id.spinnerCountryCode);
        etEmail = findViewById(R.id.etEmail);
        etAddress = findViewById(R.id.etAddress);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        imgProfile = findViewById(R.id.imgProfile);
        tvUploadPhoto = findViewById(R.id.tvUploadPhoto);
        btnSelectAddress = findViewById(R.id.btnSelectAddress);

        btnRegister.setOnClickListener(v -> registerUser());
        tvUploadPhoto.setOnClickListener(v -> showImagePickerOptions());
        etAddress.setOnClickListener(v -> openPlacePicker());
        btnSelectAddress.setOnClickListener(v -> openPlacePicker());

        setupImagePickers();
    }

    private void setupImagePickers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Bitmap bitmap = (Bitmap) result.getData().getExtras().get("data");
                        imgProfile.setImageBitmap(bitmap);
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                            imgProfile.setImageBitmap(bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void showImagePickerOptions() {
        String[] options = {"Camera", "Gallery"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("Select Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED) {
                            openCamera();
                        } else {
                            requestPermissions(new String[]{Manifest.permission.CAMERA}, 101);
                        }
                    } else {
                        openGallery();
                    }
                }).show();
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void openPlacePicker() {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.ADDRESS, Place.Field.LAT_LNG);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                .build(this);
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                etAddress.setText(place.getAddress());
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Toast.makeText(this, "Error selecting place", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String phone = spinnerCountryCode.getSelectedItem().toString().split(" ")[0] + etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || address.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Registering user...", Toast.LENGTH_SHORT).show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser == null) {
                            Toast.makeText(this, "Error: User not found after signup", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String uid = firebaseUser.getUid();

                        if (imageUri != null) {
                            // Upload profile image
                            FirebaseStorage.getInstance().getReference()
                                    .child("profile_images")
                                    .child(uid + ".jpg")
                                    .putFile(imageUri)
                                    .addOnSuccessListener(taskSnapshot ->
                                            taskSnapshot.getStorage().getDownloadUrl()
                                                    .addOnSuccessListener(uri ->
                                                            saveUserToFirestore(uid, name, phone, email, address, uri.toString())
                                                    )
                                                    .addOnFailureListener(e ->
                                                            Toast.makeText(this, "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                                    )
                                    )
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                    );
                        } else {
                            saveUserToFirestore(uid, name, phone, email, address, null);
                        }

                    } else {
                        Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("SIGNUP", "Error creating user", task.getException());
                    }
                });
    }

    private void saveUserToFirestore(String uid, String name, String phone, String email, String address, String profileImageUrl) {
        Map<String, Object> user = new HashMap<>();
        user.put("fullName", name);
        user.put("phone", phone);
        user.put("email", email);
        user.put("address", address);
        if (profileImageUrl != null) user.put("profilePhoto", profileImageUrl);

        db.collection("users").document(uid)
                .set(user, SetOptions.merge()) // Merge to avoid overwriting
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    // Redirect to MainActivity
                    startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("SIGNUP", "Firestore save failed", e);
                });
    }
}
