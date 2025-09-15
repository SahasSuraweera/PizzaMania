package com.example.pizzamania;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CartActivity extends AppCompatActivity {

    private LinearLayout cartContainer;
    private TextView tvSubtotal, tvTotalCharge, tvDeliveryFee;
    private RadioGroup radioGroupPayment;
    private RadioButton radioBtnCash, radioBtnCard;
    private CartDBHelper dbHelper;

    private String userUid, deliveryAddress, nearestBranch, paymentMethod, orderStatus;
    private double latitude, longitude, subtotal, deliveryFee, totalWithDelivery;
    String orderId = "ORD-" + new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());

    private final ActivityResultLauncher<Intent> paymentLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String status = result.getData().getStringExtra("status");
                    if ("success".equals(status)) {
                        saveOrderToFirebase();
                    } else {
                        Toast.makeText(this, "Payment Failed!", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        cartContainer = findViewById(R.id.cartContainer);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvTotalCharge = findViewById(R.id.tvTotalCharge);
        tvDeliveryFee = findViewById(R.id.tvDeliveryFee);
        Button btnCheckout = findViewById(R.id.btnCheckout);

        dbHelper = new CartDBHelper(this);

        // Get intent data
        Intent intent = getIntent();
        userUid = intent.getStringExtra("userUid");
        deliveryAddress = intent.getStringExtra("deliveryAddress");
        latitude = intent.getDoubleExtra("latitude", 0);
        longitude = intent.getDoubleExtra("longitude", 0);
        nearestBranch = intent.getStringExtra("nearestBranch");
        double nearestBranchLat = intent.getDoubleExtra("nearestBranchLat", 0);
        double nearestBranchLog = intent.getDoubleExtra("nearestBranchLog", 0);

        // Show delivery info
        TextView tvCheckoutAddress = findViewById(R.id.tvCheckoutAddress);
        TextView tvCheckoutBranch = findViewById(R.id.tvCheckoutBranch);
        tvCheckoutAddress.setText("Delivery Address    : " + deliveryAddress);
         tvCheckoutBranch.setText("Delivering Branch   : " + nearestBranch);

        deliveryFee = calculateDeliveryFee(latitude, longitude, nearestBranchLat, nearestBranchLog);
        tvDeliveryFee.setText("Delivery Fee              Rs. " + deliveryFee);

        // Payment method
        radioGroupPayment = findViewById(R.id.radioGroupPayment);
        radioBtnCash = findViewById(R.id.radioBtnCash);
        radioBtnCard = findViewById(R.id.radioBtnCard);

        radioBtnCash.setChecked(true);
        paymentMethod = "Cash On Delivery";
        orderStatus = "Pending";

        radioGroupPayment.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioBtnCash) {
                paymentMethod = "Cash On Delivery";
                orderStatus = "Pending";
            } else if (checkedId == R.id.radioBtnCard) {
                paymentMethod = "Card Payment";
                orderStatus = "Pending";
            }
        });

        loadCartItems();

        // Checkout button
        btnCheckout.setOnClickListener(v -> {
            if (radioBtnCard.isChecked()) {
                new AlertDialog.Builder(this)
                        .setTitle("Confirm Checkout")
                        .setMessage("Order will be placed after payment is successful. Are you sure?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            Intent intent1 = new Intent(CartActivity.this, PaymentActivity.class);

                            intent1.putExtra("orderID", orderId);
                            intent1.putExtra("totalCharge", updateSubtotal()); // double or int
                            intent1.putExtra("userEmail", FirebaseAuth.getInstance().getCurrentUser().getEmail());

                            paymentLauncher.launch(intent1);
                        })
                        .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                        .show();

            } else if (radioBtnCash.isChecked()) { // Cash on Delivery option
                new AlertDialog.Builder(this)
                        .setTitle("Confirm COD Order")
                        .setMessage("Are you sure you want to place this order with Cash on Delivery?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            saveOrderToFirebase();
                        })
                        .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                        .show();

            } else {
                Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show();
            }
        });

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            Intent intent3 = new Intent(CartActivity.this, MainMenuActivity.class);
            startActivity(intent3);
            finish();
        });
    }


        private void loadCartItems() {
        cartContainer.removeAllViews();
        List<CartDBHelper.CartItem> items = dbHelper.getAllItems();

        if (items.isEmpty()) {
            goBackToMainMenu();
            return;
        }

        for (CartDBHelper.CartItem item : items) {
            String name = item.getName();
            String size = item.getSize();
            double price = item.getPrice();
            int quantity = item.getQuantity();

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(12, 8, 12, 8);
            row.setGravity(Gravity.CENTER_VERTICAL);

            // Name
            TextView tvName = new TextView(this);
            tvName.setText(name + " (" + size + ")");
            tvName.setTextSize(18f);
            tvName.setTextColor(Color.BLACK);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 4f);
            tvName.setLayoutParams(nameParams);

            // Minus button
            Button btnMinus = new Button(this);
            btnMinus.setText("−");
            btnMinus.setTextSize(16f);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(70, 70);
            btnMinus.setLayoutParams(btnParams);
            // Quantity
            TextView tvQty = new TextView(this);
            tvQty.setText(String.valueOf(quantity));
            tvQty.setTextSize(16f);
            tvQty.setTextColor(Color.BLACK);
            LinearLayout.LayoutParams qtyParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            tvQty.setLayoutParams(qtyParams);

            // Plus button
            Button btnPlus = new Button(this);
            btnPlus.setText("+");
            btnMinus.setTextSize(16f);
            btnPlus.setLayoutParams(btnParams);

            // Price
            TextView tvPrice = new TextView(this);
            tvPrice.setText("Rs." + (price * quantity));
            tvPrice.setTextSize(18f);
            tvPrice.setGravity(Gravity.END);
            tvPrice.setTextColor(Color.BLACK);
            LinearLayout.LayoutParams priceParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3f);
            tvPrice.setLayoutParams(priceParams);

            // Listeners
            btnPlus.setOnClickListener(v -> {
                int newQty = Integer.parseInt(tvQty.getText().toString()) + 1;
                tvQty.setText(String.valueOf(newQty));
                dbHelper.addOrUpdateItem(name, size, price, 1);
                tvPrice.setText("Rs." + (price * newQty));
                updateSubtotal();
            });

            btnMinus.setOnClickListener(v -> {
                int newQty = Integer.parseInt(tvQty.getText().toString()) - 1;
                if (newQty > 0) {
                    tvQty.setText(String.valueOf(newQty));
                    dbHelper.addOrUpdateItem(name, size, price, -1);
                    tvPrice.setText("Rs." + (price * newQty));
                } else {
                    dbHelper.removeItem(name, size);
                    cartContainer.removeView(row);
                    notifyItemDeselected(name, size); // inform MainMenuActivity
                }
                updateSubtotal();

                // If cart is empty, go back to main menu
                if (dbHelper.getAllItems().isEmpty()) goBackToMainMenu();
            });

            row.addView(tvName);
            row.addView(btnMinus);
            row.addView(tvQty);
            row.addView(btnPlus);
            row.addView(tvPrice);

            cartContainer.addView(row);
        }

        updateSubtotal();
    }

    private void notifyItemDeselected(String name, String size) {
        SharedPreferences prefs = getSharedPreferences("cartPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String key = name + "_" + size;
        editor.putBoolean(key, false);
        editor.apply();
    }

    private double updateSubtotal() {
        subtotal = dbHelper.getSubTotal();
        if (subtotal == 0) deliveryFee = 0;
        totalWithDelivery = subtotal + deliveryFee;

        tvTotalCharge.setText("Sub Total       :                                   Rs." + subtotal);
        tvSubtotal.setText(String.format("Rs.%.1f", totalWithDelivery));
        tvDeliveryFee.setText("Delivery Fee  :                                   Rs." + deliveryFee);

        return totalWithDelivery;
    }

    private double calculateDeliveryFee(double userLat, double userLng, double branchLat, double branchLng) {
        double distance = distanceInKm(userLat, userLng, branchLat, branchLng);
        if (distance <= 1.0) return 0.0;
        int extraKm = (int) Math.ceil(distance - 1);
        return extraKm * 50.0;
    }

    private double distanceInKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    private void saveOrderToFirebase() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("orders");
        String orderDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String orderTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        OrderModel order = new OrderModel(
                orderId,
                FirebaseAuth.getInstance().getCurrentUser().getUid(),
                deliveryAddress,
                latitude,
                longitude,
                nearestBranch,
                subtotal,
                deliveryFee,
                totalWithDelivery,
                paymentMethod,
                orderStatus,
                orderDate,
                orderTime
        );

        dbRef.child(orderId).setValue(order).addOnCompleteListener(task -> {
            if (task.isSuccessful()) saveOrderItemsToFirebase(orderId);
            else Toast.makeText(this, "Failed to save order", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveOrderItemsToFirebase(String orderId) {
        DatabaseReference itemsRef = FirebaseDatabase.getInstance()
                .getReference("order_items")
                .child(orderId);

        List<CartDBHelper.CartItem> cartItems = dbHelper.getAllItems();
        for (CartDBHelper.CartItem item : cartItems) {
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("name", item.getName());
            itemData.put("size", item.getSize());
            itemData.put("price", item.getPrice());
            itemData.put("quantity", item.getQuantity());
            itemsRef.push().setValue(itemData);
        }

        dbHelper.clearCart();
        Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, OrderActivity.class);
        startActivity(intent);
        finish();


    }

    // Updated: pass deliveryAddress & nearestBranch
    private void goBackToMainMenu() {
        Intent intent = new Intent(this, MainMenuActivity.class);

        // Only tell MainMenuActivity to reset cart selections
        intent.putExtra("resetCartSelections", true);

        // Pass delivery info so it stays intact
        intent.putExtra("deliveryAddress", deliveryAddress);
        intent.putExtra("nearestBranch", nearestBranch);

        startActivity(intent);
        finish();
    }

}
