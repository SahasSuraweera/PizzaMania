package com.example.pizzamania;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class MainMenuActivity extends AppCompatActivity {

    CartDBHelper dbHelper;
    LinearLayout menuContainer;
    FirebaseFirestore db;
    FirebaseStorage storage;

    ImageView fabCart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        dbHelper = new CartDBHelper(this);
        menuContainer = findViewById(R.id.scrollMenuLinear);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            loadPizzasFromFirestore();
        } else {
            // Anonymous sign-in for testing
            auth.signInAnonymously().addOnSuccessListener(authResult -> loadPizzasFromFirestore());
        }

        fabCart = findViewById(R.id.fabCart);

        fabCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open CartActivity
                Intent intent = new Intent(MainMenuActivity.this, CartActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loadPizzasFromFirestore() {
        db.collection("Pizzas").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String description = document.getString("description");
                    String imageUrl = document.getString("imageUrl");
                    String name = document.getString("name");
                    Double price = document.getDouble("price");
                    String size = document.getString("size");

                    if (name != null && size != null && price != null) {
                        Pizza pizza = new Pizza(name, size, price, description, imageUrl);
                        addMenuItemView(pizza);
                    } else {
                        Log.e("FIRESTORE", "Missing fields in document: " + document.getId());
                    }
                }
            } else {
                Log.e("FIRESTORE", "Error getting documents, loading from SQLite", task.getException());
                loadPizzasFromSQLite();
            }
        }).addOnFailureListener(e -> {
            Log.e("FIRESTORE", "Failed to get documents, loading from SQLite", e);
            loadPizzasFromSQLite();
        });
    }

    private void loadPizzasFromSQLite() {
        MenuDBHelper menuDBHelper = new MenuDBHelper();
        List<Pizza> defaultPizzas =  menuDBHelper.getDefaultPizzas(6); // Get 6 default pizzas
        for (Pizza pizza : defaultPizzas) {
            addMenuItemView(pizza);
        }
    }

    private void addMenuItemView(Pizza pizza) {
        LinearLayout horizontal = new LinearLayout(this);
        horizontal.setOrientation(LinearLayout.HORIZONTAL);
        horizontal.setPadding(16,16,16,16);
        horizontal.setGravity(Gravity.CENTER_VERTICAL);

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

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.CENTER_HORIZONTAL);

        ImageView img = new ImageView(this);
        img.setLayoutParams(new LinearLayout.LayoutParams(200,200));

        if (pizza.getImageUrl() != null && !pizza.getImageUrl().isEmpty()) {
            StorageReference imgRef = storage.getReference().child(pizza.getImageUrl());
            imgRef.getDownloadUrl().addOnSuccessListener(uri -> {
                Glide.with(this)
                        .load(uri.toString())
                        .placeholder(R.drawable.pizza)
                        .into(img);
            }).addOnFailureListener(e -> img.setImageResource(R.drawable.pizza));
        } else {
            img.setImageResource(R.drawable.pizza);
        }

        Button btnSelect = new Button(this);
        btnSelect.setText("+Select");
        btnSelect.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF5722")));
        btnSelect.setTextColor(Color.WHITE);

        boolean[] isSelected = {false};

        btnSelect.setOnClickListener(v -> {
            if(!isSelected[0]){
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

        View divider = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2);
        params.setMargins(0, 8, 0, 8);
        divider.setLayoutParams(params);
        divider.setBackgroundColor(Color.LTGRAY);

        menuContainer.addView(divider);
    }

    // Pizza class
    public static class Pizza {
        private String name;
        private String size;
        private double price;
        private String description;
        private String imageUrl;

        public Pizza() {}

        public Pizza(String name, String size, double price, String description, String imageUrl) {
            this.name = name;
            this.size = size;
            this.price = price;
            this.description = description;
            this.imageUrl = imageUrl;
        }

        public String getName() { return name; }
        public String getSize() { return size; }
        public double getPrice() { return price; }
        public String getDescription() { return description; }
        public String getImageUrl() { return imageUrl; }
    }
}
