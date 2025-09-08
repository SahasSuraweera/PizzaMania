package com.example.pizzamaniaapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class MainMenuActivity extends AppCompatActivity {

    CartDBHelper dbHelper;
    LinearLayout menuContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        dbHelper = new CartDBHelper(this);
        menuContainer = findViewById(R.id.scrollMenuLinear); // LinearLayout inside ScrollView

        // Sample menu items with descriptions
        ArrayList<MenuItem> menuItems = new ArrayList<>();
        menuItems.add(new MenuItem("Margherita Pizza", 1500, R.drawable.margerita_pizza, "Classic cheese and tomato pizza."));
        menuItems.add(new MenuItem("Pepperoni Pizza", 2000, R.drawable.margerita_pizza, "Loaded with spicy pepperoni slices."));
        menuItems.add(new MenuItem("Veggie Pizza", 1800, R.drawable.margerita_pizza, "Topped with fresh seasonal vegetables."));

        // Dynamically create menu views
        for(MenuItem item : menuItems){
            addMenuItemView(item);
        }

        // Floating Cart button
        ImageView fabCart = findViewById(R.id.fabCart);
        fabCart.setOnClickListener(v -> startActivity(new Intent(MainMenuActivity.this, CartActivity.class)));
    }

    private void addMenuItemView(MenuItem item){
        // Horizontal container for each item
        LinearLayout horizontal = new LinearLayout(this);
        horizontal.setOrientation(LinearLayout.HORIZONTAL);
        horizontal.setPadding(16,16,16,16);
        horizontal.setGravity(Gravity.CENTER_VERTICAL);

        // Item Details (Name + Description + Price)
        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView tvName = new TextView(this);
        tvName.setText(item.name);
        tvName.setTextSize(18f);
        tvName.setTextColor(Color.BLACK);

        TextView tvDescription = new TextView(this);
        tvDescription.setText(item.description);
        tvDescription.setTextSize(14f);
        tvDescription.setTextColor(Color.DKGRAY);

        TextView tvPrice = new TextView(this);
        tvPrice.setText("Rs. " + item.price);
        tvPrice.setTextColor(Color.parseColor("#FF5722"));

        details.addView(tvName);
        details.addView(tvDescription);
        details.addView(tvPrice);

        // Image + Select Button
        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.CENTER_HORIZONTAL);

        ImageView img = new ImageView(this);
        img.setImageResource(item.imageRes);
        img.setLayoutParams(new LinearLayout.LayoutParams(200,200));

        Button btnSelect = new Button(this);
        btnSelect.setText("+Select");
        btnSelect.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF5722"))); // Orange
        btnSelect.setTextColor(Color.WHITE);

        // Flag to track selection
        boolean[] isSelected = {false};

        btnSelect.setOnClickListener(v -> {
            if(!isSelected[0]){
                // Select → add to cart & change color
                btnSelect.setText("+Selected");
                btnSelect.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))); // Green
                dbHelper.addOrUpdateItem(item.name, item.price, 1);
                isSelected[0] = true;
            } else {
                // Deselect → remove from cart & restore color
                btnSelect.setText("+Select");
                btnSelect.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF5722"))); // Orange
                dbHelper.removeItem(item.name);
                isSelected[0] = false;
            }
        });

        right.addView(img);
        right.addView(btnSelect);

        horizontal.addView(details);
        horizontal.addView(right);

        menuContainer.addView(horizontal);

        // ---- Divider line after each record ----
        View divider = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2  // line thickness
        );
        params.setMargins(0, 8, 0, 8); // spacing before/after line
        divider.setLayoutParams(params);
        divider.setBackgroundColor(Color.LTGRAY);

        menuContainer.addView(divider);
    }

    // MenuItem helper class
    static class MenuItem {
        String name;
        double price;
        int imageRes;
        String description;

        MenuItem(String n, double p, int img, String d){
            name = n; price = p; imageRes = img; description = d;
        }
    }
}