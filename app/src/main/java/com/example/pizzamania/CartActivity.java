package com.example.pizzamania;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class CartActivity extends AppCompatActivity {

    private LinearLayout cartContainer;
    private TextView tvSubtotal;
    private CartDBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        cartContainer = findViewById(R.id.cartContainer);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        Button btnCheckout = findViewById(R.id.btnCheckout);

        dbHelper = new CartDBHelper(this);

        loadCartItems();

        btnCheckout.setOnClickListener(v -> {
            // TODO: Handle checkout (navigate or confirm order)
        });
    }

    private void loadCartItems() {
        cartContainer.removeAllViews(); // clear old items
        ArrayList<CartDBHelper.CartItem> items = dbHelper.getAllItems();

        for (CartDBHelper.CartItem item : items) {
            String name = item.getName();
            String size = item.getSize();
            double price = item.getPrice();
            int quantity = item.getQuantity();

            // Row container
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowParams.setMargins(0, dpToPx(6), 0, dpToPx(6));
            row.setLayoutParams(rowParams);
            row.setMinimumHeight(dpToPx(56));
            row.setGravity(Gravity.CENTER_VERTICAL);

            // Name
            TextView tvName = new TextView(this);
            tvName.setText(name + " (" + size + ")");
            tvName.setTextSize(16f);
            tvName.setTextColor(getColor(R.color.black));
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    4f
            );
            tvName.setLayoutParams(nameParams);

            // Minus button
            Button btnMinus = new Button(this);
            btnMinus.setText("−");
            btnMinus.setTextSize(13f);
            row.setBackgroundColor(Color.parseColor("#6b8794"));
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(dpToPx(35), dpToPx(35));
            btnParams.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            btnMinus.setLayoutParams(btnParams);

            // Quantity
            TextView tvQty = new TextView(this);
            tvQty.setText(String.valueOf(quantity));
            tvQty.setTextSize(16f);
            tvQty.setTextColor(getColor(R.color.black));
            tvQty.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams qtyParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            qtyParams.setMargins(dpToPx(8), 0, dpToPx(8), 0);
            tvQty.setLayoutParams(qtyParams);

            // Plus button
            Button btnPlus = new Button(this);
            btnPlus.setText("+");
            btnPlus.setTextSize(13f);
            row.setBackgroundColor(Color.parseColor("#6b8794"));
            btnPlus.setLayoutParams(btnParams);

            // Price
            TextView tvPrice = new TextView(this);
            tvPrice.setText("Rs." + (price * quantity));
            tvPrice.setTextSize(16f);
            tvPrice.setTextColor(getColor(R.color.black));
            tvPrice.setGravity(Gravity.END);
            LinearLayout.LayoutParams priceParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    3f
            );
            tvPrice.setLayoutParams(priceParams);

            // Listeners
            btnPlus.setOnClickListener(v -> {
                int newQty = Integer.parseInt(tvQty.getText().toString()) + 1;
                tvQty.setText(String.valueOf(newQty));
                dbHelper.addOrUpdateItem(name, size, price, 1); // add 1
                tvPrice.setText("Rs." + (price * newQty));
                updateSubtotal();
            });

            btnMinus.setOnClickListener(v -> {
                int newQty = Integer.parseInt(tvQty.getText().toString()) - 1;
                if (newQty > 0) {
                    tvQty.setText(String.valueOf(newQty));
                    dbHelper.addOrUpdateItem(name, size, price, -1); // remove 1
                    tvPrice.setText("Rs." + (price * newQty));
                } else {
                    dbHelper.removeItem(name, size);
                    cartContainer.removeView(row);
                }
                updateSubtotal();
            });

            // Add views to row
            row.addView(tvName);
            row.addView(btnMinus);
            row.addView(tvQty);
            row.addView(btnPlus);
            row.addView(tvPrice);

            cartContainer.addView(row);
        }

        updateSubtotal();
    }

    private int dpToPx(int dp) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    private void updateSubtotal() {
        double subtotal = dbHelper.getSubTotal();
        tvSubtotal.setText("Rs." + String.format("%.2f", subtotal));
    }
}
