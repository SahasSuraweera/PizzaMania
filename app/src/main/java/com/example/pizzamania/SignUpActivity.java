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
import android.view.View;
import android.widget.ArrayAdapter;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText etName, etPhone, etEmail, etAddress, etPassword, etConfirmPassword;
    private Spinner spinnerCountryCode;
    private Button btnRegister, tvUploadPhoto;
    private ImageView imgProfile, btnSelectAddress;

    private Uri imageUri;
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> mapLauncher;

    private double userLat = 0.0;
    private double userLng = 0.0;

    private static final int CAMERA_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance("https://pizzamania-d2775-default-rtdb.firebaseio.com/")
                .getReference("users");

        // Views
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

        setupLaunchers();

        btnRegister.setOnClickListener(v -> registerUser());
        tvUploadPhoto.setOnClickListener(v -> showImagePickerOptions());
        btnSelectAddress.setOnClickListener(v -> openMapPicker());
        etAddress.setOnClickListener(v -> openMapPicker());

        Spinner spinner = findViewById(R.id.spinnerCountryCode);

// Load array from resources
        String[] countryCodes = getResources().getStringArray(R.array.country_codes);

// Create adapter with custom layout
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,    // custom layout
                countryCodes
        );

// Optional: for dropdown view (can use same layout or a separate one)
        adapter.setDropDownViewResource(R.layout.spinner_item);

        spinner.setAdapter(adapter);

    }

    private void setupLaunchers() {
        // Camera
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null &&
                            result.getData().getExtras() != null) {
                        Bitmap bitmap = (Bitmap) result.getData().getExtras().get("data");
                        if (bitmap != null) {
                            bitmap = getResizedBitmap(bitmap, 500);
                            imgProfile.setImageBitmap(bitmap);
                            imageUri = getImageUriFromBitmap(bitmap);
                        }
                    }
                });

        // Gallery
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        if (imageUri != null) {
                            try {
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                                bitmap = getResizedBitmap(bitmap, 500);
                                imgProfile.setImageBitmap(bitmap);
                                imageUri = getImageUriFromBitmap(bitmap);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

        // Map Picker
        mapLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        String address = data.getStringExtra("address");
                        userLat = data.getDoubleExtra("latitude", 0.0);
                        userLng = data.getDoubleExtra("longitude", 0.0);
                        etAddress.setText(address);
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
                            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
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

    private void openMapPicker() {
        Intent intent = new Intent(this, MapActivity.class);
        mapLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        }
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String phone = (spinnerCountryCode.getSelectedItem() != null ? spinnerCountryCode.getSelectedItem().toString().split(" ")[0] : "") + etPhone.getText().toString().trim();
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

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null) return;
                        String uid = user.getUid();

                        if (imageUri != null) {
                            StorageReference storageRef = FirebaseStorage.getInstance()
                                    .getReference("profile_images/" + uid + ".jpg");
                            storageRef.putFile(imageUri)
                                    .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                                            .addOnSuccessListener(uri -> saveUserToDB(uid, name, phone, email, address, uri.toString()))
                                            .addOnFailureListener(e -> Log.e("SignUp", "Download URL failed", e)))
                                    .addOnFailureListener(e -> Log.e("SignUp", "Image upload failed", e));
                        } else {
                            saveUserToDB(uid, name, phone, email, address, null);
                        }

                    } else {
                        Log.e("SignUp", "Auth failed", task.getException());
                        Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToDB(String uid, String name, String phone, String email, String address, String profileUrl) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("fullName", name);
        userMap.put("phone", phone);
        userMap.put("email", email);
        userMap.put("address", address);
        userMap.put("latitude", userLat);
        userMap.put("longitude", userLng);
        if (profileUrl != null) userMap.put("profilePhoto", profileUrl);

        dbRef.child(uid).setValue(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> Log.e("SignUp", "System Error! Please Try Again Later.", e));
    }

    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bytes);
            String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "Profile", null);
            if (path != null) return Uri.parse(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    public void goToSignIn(View view) {
        Intent intent = new Intent(this, SignInActivity.class);
        startActivity(intent);
    }
}
