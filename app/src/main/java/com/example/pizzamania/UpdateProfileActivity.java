package com.example.pizzamania;

import android.Manifest;
import android.app.AlertDialog;
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

import com.bumptech.glide.Glide;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class UpdateProfileActivity extends AppCompatActivity {

    private EditText etName, etPhone, etAddress, etEmail, etPassword, etConfirmPassword;
    private Button btnUpdate, btnDelete, tvUploadPhoto;
    private ImageView imgProfile, btnSelectAddress;
    private Uri imageUri;

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    private double userLat = 0.0, userLng = 0.0;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> mapLauncher;

    private static final int CAMERA_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) finish();
        String uid = user.getUid();
        dbRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        // Views
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        imgProfile = findViewById(R.id.imgProfile);
        tvUploadPhoto = findViewById(R.id.tvUploadPhoto);
        btnSelectAddress = findViewById(R.id.btnSelectAddress);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnDelete = findViewById(R.id.btnDelete);

        setupLaunchers();
        loadUserDetails();

        tvUploadPhoto.setOnClickListener(v -> showImagePickerOptions());
        btnSelectAddress.setOnClickListener(v -> openMapPicker());
        etAddress.setOnClickListener(v -> openMapPicker());

        btnUpdate.setOnClickListener(v -> updateProfile());
        btnDelete.setOnClickListener(v -> confirmDeleteAccount());
    }

    private void setupLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null &&
                            result.getData().getExtras() != null) {
                        Bitmap bitmap = (Bitmap) result.getData().getExtras().get("data");
                        if (bitmap != null) {
                            imgProfile.setImageBitmap(bitmap);
                            imageUri = getImageUriFromBitmap(bitmap);
                        }
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        if (imageUri != null) imgProfile.setImageURI(imageUri);
                    }
                });

        mapLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        String address = data.getStringExtra("address");
                        userLat = data.getDoubleExtra("latitude", 0.0);
                        userLng = data.getDoubleExtra("longitude", 0.0);
                        etAddress.setText(address);
                    }
                });
    }

    private void loadUserDetails() {
        dbRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) return;

            etName.setText(snapshot.child("fullName").getValue(String.class));
            etPhone.setText(snapshot.child("phone").getValue(String.class));
            etAddress.setText(snapshot.child("address").getValue(String.class));
            etEmail.setText(mAuth.getCurrentUser().getEmail());

            Double lat = snapshot.child("latitude").getValue(Double.class);
            Double lng = snapshot.child("longitude").getValue(Double.class);
            if (lat != null) userLat = lat;
            if (lng != null) userLng = lng;

            String profileUrl = snapshot.child("profilePhoto").getValue(String.class);
            if (profileUrl != null && !profileUrl.isEmpty()) {
                Glide.with(this).load(profileUrl).into(imgProfile);
            }
        });
    }

    private void showImagePickerOptions() {
        String[] options = {"Camera", "Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Select Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED) openCamera();
                        else requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                    } else openGallery();
                }).show();
    }

    private void openCamera() {
        cameraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
    }

    private void openGallery() {
        galleryLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
    }

    private void openMapPicker() {
        mapLauncher.launch(new Intent(this, MapActivity.class));
    }

    private void updateProfile() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Password confirmation
        if (!password.isEmpty() || !confirmPassword.isEmpty()) {
            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Upload profile image first if exists
        if (imageUri != null) {
            StorageReference storageRef = FirebaseStorage.getInstance()
                    .getReference("profile_images/" + mAuth.getUid() + ".jpg");
            storageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> saveProfileToDB(name, phone, address, uri.toString(), password))
                            .addOnFailureListener(e -> Log.e("UpdateProfile", "Download URL failed", e)))
                    .addOnFailureListener(e -> Log.e("UpdateProfile", "Image upload failed", e));
        } else {
            saveProfileToDB(name, phone, address, null, password);
        }
    }

    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bytes); // compress the bitmap
            String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "Profile", null);
            if (path != null) {
                return Uri.parse(path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void saveProfileToDB(String name, String phone, String address, String profileUrl, String password) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("fullName", name);
        userMap.put("phone", phone);
        userMap.put("address", address);
        userMap.put("latitude", userLat);
        userMap.put("longitude", userLng);
        if (profileUrl != null) userMap.put("profilePhoto", profileUrl);

        dbRef.updateChildren(userMap)
                .addOnSuccessListener(aVoid -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null && !password.isEmpty()) {
                        user.updatePassword(password)
                                .addOnSuccessListener(aVoid1 -> Toast.makeText(this, "Profile & Password updated!", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(this, "Password update failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    } else {
                        Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Log.e("UpdateProfile", "Update failed", e));
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // Re-authenticate user before deletion
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), "USER_PASSWORD_HERE"); // Replace with actual password

        user.reauthenticate(credential).addOnCompleteListener(reauthTask -> {
            if (reauthTask.isSuccessful()) {
                dbRef.removeValue().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        user.delete().addOnCompleteListener(deleteTask -> {
                            if (deleteTask.isSuccessful()) {
                                Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(UpdateProfileActivity.this, MainMenuActivity.class);
                                startActivity(intent);
                            } else {
                                Toast.makeText(this, "Failed to delete user: " + deleteTask.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        Toast.makeText(this, "Failed to remove data: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Toast.makeText(this, "Re-authentication failed: " + reauthTask.getException().getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) openCamera();
    }
}
