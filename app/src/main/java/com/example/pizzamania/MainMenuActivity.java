package com.example.pizzamania;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class MainMenuActivity extends AppCompatActivity {

    private CartDBHelper dbHelper;
    private LinearLayout menuContainer;
    private DatabaseReference dbRef;
    private ImageView fabCart;
    private TextView tvUsername, tvDeliveryLocation, tvNearestBranch;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private FirebaseDatabase database;
    private String deliveryAddress, nearestBranch;
    private double currentLat, currentLng;
    private double nearestBranchLat, nearestBranchLog;

    private SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        sharedPrefs = getSharedPreferences("SelectedItems", MODE_PRIVATE);
        dbHelper = new CartDBHelper(this);
        menuContainer = findViewById(R.id.scrollMenuLinear);
        fabCart = findViewById(R.id.fabCart);

        tvUsername = findViewById(R.id.tvUserName);
        tvDeliveryLocation = findViewById(R.id.tvDeliveryLocation);
        tvNearestBranch = findViewById(R.id.tvNearestBranch);

        dbRef = FirebaseDatabase.getInstance().getReference("Pizzas");
        loadPizzasFromRealtimeDB();

        // Sign out button
        ImageButton btnSignOut = findViewById(R.id.btnSignOut);
        btnSignOut.setOnClickListener(v -> showSignOutDialog());

        Button btnEditLocation = findViewById(R.id.btnEditLocation);
        btnEditLocation.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, MapActivity.class);
            startActivityForResult(intent, 200);
        });
        ImageButton btnHome = findViewById(R.id.imgBtnHome);
        btnHome.setOnClickListener(v -> {
            startActivity(new Intent(MainMenuActivity.this, MainMenuActivity.class));
            finish();
        });
        // Orders button
        ImageButton btnOrders = findViewById(R.id.imgBtnOrders);
        btnOrders.setOnClickListener(v -> {
            startActivity(new Intent(MainMenuActivity.this, OrderActivity.class));
            finish();
        });

        // Branches button
        ImageButton btnBranches = findViewById(R.id.imgBtnBranches);
        btnBranches.setOnClickListener(v -> {
            startActivity(new Intent(MainMenuActivity.this, BranchesActivity.class));
            finish();
        });
        ImageButton btnProfile = findViewById(R.id.imgBtnProfile);
        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(MainMenuActivity.this, UpdateProfileActivity.class));
            finish();
        });

        // Fetch user info
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            database = FirebaseDatabase.getInstance("https://pizzamania-d2775-default-rtdb.firebaseio.com/");
            userRef = database.getReference("users").child(uid);

            // Name
            userRef.child("fullName").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String name = snapshot.getValue(String.class);
                    if (name != null) tvUsername.setText("Hello, " + name + " !");
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("MainMenu", "Failed to fetch user name: " + error.getMessage());
                }
            });

            // Address
            userRef.child("address").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String address = snapshot.getValue(String.class);
                    if (address != null) {
                        tvDeliveryLocation.setText("Delivery Address: " + address);
                        deliveryAddress = address;
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("MainMenu", "Failed to fetch address: " + error.getMessage());
                }
            });

            // Coordinates → nearest branch
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Double userLat = snapshot.child("latitude").getValue(Double.class);
                    Double userLng = snapshot.child("longitude").getValue(Double.class);

                    if (userLat != null && userLng != null) {
                        currentLat = userLat;
                        currentLng = userLng;
                        checkNearestBranch(userLat, userLng);
                    } else {
                        tvNearestBranch.setText("User coordinates not available");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("MainMenu", "Failed to fetch user coordinates: " + error.getMessage());
                }
            });

            fabCart.setOnClickListener(v -> {
                if(nearestBranchLat == 0 && nearestBranchLog == 0){
                    Toast.makeText(MainMenuActivity.this,
                            "No delivery option here. Call our hotline to place your order.",
                            Toast.LENGTH_SHORT).show();
                }
                else {
                    Intent intent = new Intent(MainMenuActivity.this, CartActivity.class);
                    intent.putExtra("userUid", uid);
                    intent.putExtra("deliveryAddress", deliveryAddress);
                    intent.putExtra("latitude", currentLat);
                    intent.putExtra("longitude", currentLng);
                    intent.putExtra("nearestBranch", nearestBranch);
                    intent.putExtra("nearestBranchLat", nearestBranchLat);
                    intent.putExtra("nearestBranchLog", nearestBranchLog);
                    startActivity(intent);
                }
            });
        }
    }

    // Method to reset deselected items after order
    private void clearSelectedFlags() {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.clear();
        editor.apply();
    }

    // Call this in onResume to update buttons
    @Override
    protected void onResume() {
        super.onResume();

        // Check if coming from CartActivity with resetCartSelections
        Intent intent = getIntent();
        boolean resetCartSelections = intent.getBooleanExtra("resetCartSelections", false);
        String passedDeliveryAddress = intent.getStringExtra("deliveryAddress");
        String passedNearestBranch = intent.getStringExtra("nearestBranch");

        // Only update delivery info if passed from CartActivity
        if (passedDeliveryAddress != null) {
            deliveryAddress = passedDeliveryAddress;
            tvDeliveryLocation.setText("Delivery Address: " + deliveryAddress);
        }

        if (passedNearestBranch != null) {
            nearestBranch = passedNearestBranch;
            tvNearestBranch.setText("Nearest branch: " + nearestBranch);
        }

        // Reset cart selections if requested
        if (resetCartSelections) {
            clearSelectedFlags(); // reset pizza selections only
        }

        // Load pizzas and reflect selection states
        loadPizzasFromRealtimeDB();
    }


    private void showSignOutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(MainMenuActivity.this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Clear any saved login info
                    getSharedPreferences("loginPrefs", MODE_PRIVATE).edit().clear().apply();
                    // Clear selected items flags
                    getSharedPreferences("SelectedItems", MODE_PRIVATE).edit().clear().apply();

                    // Sign out from Firebase
                    FirebaseAuth.getInstance().signOut();

                    // Go back to main login screen
                    Intent intent = new Intent(MainMenuActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            String address = data.getStringExtra("address");
            currentLat = data.getDoubleExtra("latitude", 0);
            currentLng = data.getDoubleExtra("longitude", 0);

            if (address != null) {
                tvDeliveryLocation.setText("Delivery Address: " + address);
                deliveryAddress = address;
                checkNearestBranch(currentLat, currentLng);
            }
        }
    }


// ==================== PIZZAS ====================

    private void loadPizzasFromRealtimeDB() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                menuContainer.removeAllViews();
                if (snapshot.exists()) {
                    for (DataSnapshot pizzaSnap : snapshot.getChildren()) {
                        String name = pizzaSnap.child("name").getValue(String.class);
                        String size = pizzaSnap.child("size").getValue(String.class);
                        Double price = pizzaSnap.child("price").getValue(Double.class);
                        String description = pizzaSnap.child("description").getValue(String.class);
                        String imageUrl = pizzaSnap.child("imageUrl").getValue(String.class);

                        if (name != null && size != null && price != null) {
                            Pizza pizza = new Pizza(name, size, price, description, imageUrl);
                            addMenuItemView(pizza);
                        }
                    }
                } else {
                    loadPizzasFromSQLite(); // fallback
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("REALTIME_DB", "Error: " + error.getMessage());
                loadPizzasFromSQLite();
            }
        });
    }

    private void loadPizzasFromSQLite() {
        MenuDBHelper menuDBHelper = new MenuDBHelper();
        List<Pizza> defaultPizzas = menuDBHelper.getDefaultPizzas(6);
        for (Pizza pizza : defaultPizzas) {
            addMenuItemView(pizza);
        }
    }

    private void addMenuItemView(Pizza pizza) {
        LinearLayout horizontal = new LinearLayout(this);
        horizontal.setOrientation(LinearLayout.HORIZONTAL);
        horizontal.setPadding(16, 16, 16, 16);
        horizontal.setGravity(Gravity.CENTER_VERTICAL);

        // Left: details
        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView tvName = new TextView(this);
        tvName.setText(pizza.getName() + " (" + pizza.getSize() + ")");
        tvName.setTextSize(18f);
        tvName.setTextColor(Color.BLACK);

        TextView tvDescription = new TextView(this);
        tvDescription.setText(pizza.getDescription());
        tvDescription.setTextSize(14f);
        tvDescription.setTextColor(Color.DKGRAY);

        TextView tvPrice = new TextView(this);
        tvPrice.setText("Rs. " + pizza.getPrice());
        tvPrice.setTextColor(Color.parseColor("#FF5722"));

        details.addView(tvName);
        details.addView(tvDescription);
        details.addView(tvPrice);

        // Right: image + select button
        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.CENTER_HORIZONTAL);

        ImageView img = new ImageView(this);
        img.setLayoutParams(new LinearLayout.LayoutParams(200, 200));

        if (pizza.getImageUrl() != null && !pizza.getImageUrl().isEmpty()) {
            new DownloadImageTask(img).execute(pizza.getImageUrl());
        } else {
            img.setImageResource(R.drawable.pizza);
        }

        Button btnSelect = new Button(this);
        btnSelect.setText("+Select");
        btnSelect.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF5722")));
        btnSelect.setTextColor(Color.WHITE);

        boolean[] isSelected = {false};
        btnSelect.setOnClickListener(v -> {
            if (!isSelected[0]) {
                btnSelect.setText("+Selected");
                btnSelect.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                dbHelper.addOrUpdateItem(pizza.getName(), pizza.getSize(), pizza.getPrice(), 1);
                isSelected[0] = true;
            } else {
                btnSelect.setText("+Select");
                btnSelect.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF5722")));
                dbHelper.removeItem(pizza.getName(), pizza.getSize());
                isSelected[0] = false;
            }
        });

        right.addView(img);
        right.addView(btnSelect);

        horizontal.addView(details);
        horizontal.addView(right);
        menuContainer.addView(horizontal);

        // Divider
        View divider = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2);
        params.setMargins(0, 8, 0, 8);
        divider.setLayoutParams(params);
        divider.setBackgroundColor(Color.LTGRAY);
        menuContainer.addView(divider);
    }

    // Async load image
    private static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView imageView;

        public DownloadImageTask(ImageView imageView) { this.imageView = imageView; }

        @Override
        protected Bitmap doInBackground(String... urls) {
            String urlStr = urls[0];
            Bitmap bitmap = null;
            try {
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                bitmap = BitmapFactory.decodeStream(input);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) imageView.setImageBitmap(bitmap);
            else imageView.setImageResource(R.drawable.pizza);
        }
    }

    // ==================== NEAREST BRANCH ====================

    private void checkNearestBranch(double userLat, double userLng) {
        DatabaseReference branchesRef = FirebaseDatabase.getInstance().getReference("branches");

        branchesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String nearestBranchName = null;
                double minDistance = Double.MAX_VALUE;
                double branchLat = 0.0, branchLng = 0.0;

                for (DataSnapshot branchSnap : snapshot.getChildren()) {
                    String name = branchSnap.child("name").getValue(String.class);
                    Double lat = branchSnap.child("latitude").getValue(Double.class);
                    Double lng = branchSnap.child("longitude").getValue(Double.class);

                    if (name != null && lat != null && lng != null) {
                        double dist = distanceInKm(userLat, userLng, lat, lng);
                        if (dist < minDistance) {
                            minDistance = dist;
                            nearestBranchName = name;
                            branchLat = lat;
                            branchLng = lng;
                        }
                    }
                }

                if (nearestBranchName != null && minDistance <= 5.0) {
                    tvNearestBranch.setText("Nearest branch: " + nearestBranchName);
                    nearestBranch = nearestBranchName;
                    nearestBranchLat = branchLat;
                    nearestBranchLog = branchLng;
                } else {
                    tvNearestBranch.setText("No delivery options in 5 km radius");
                    nearestBranch = "No Delivery Options";
                    nearestBranchLat = 0;
                    nearestBranchLog = 0;

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvNearestBranch.setText("Failed to fetch branches");
            }
        });
    }


    public double distanceInKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }


    // Pizza model
    public static class Pizza {
        private String name;
        private String size;
        private double price;
        private String description;
        private String imageUrl;

        public Pizza() {}
        public Pizza(String name, String size, double price, String description, String imageUrl) {
            this.name = name; this.size = size; this.price = price;
            this.description = description; this.imageUrl = imageUrl;
        }

        public String getName() { return name; }
        public String getSize() { return size; }
        public double getPrice() { return price; }
        public String getDescription() { return description; }
        public String getImageUrl() { return imageUrl; }
    }
}
