package com.example.pizzamania;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class OrderActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private OrderAdapter orderAdapter;
    private List<OrderModel> allOrders = new ArrayList<>();
    private List<OrderModel> filteredOrders = new ArrayList<>();

    private FirebaseAuth auth;
    private Button btnOngoing, btnPast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        recyclerView = findViewById(R.id.recyclerViewOrders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        orderAdapter = new OrderAdapter(filteredOrders);
        recyclerView.setAdapter(orderAdapter);

        btnOngoing = findViewById(R.id.btnOngoing);
        btnPast = findViewById(R.id.btnPast);

        auth = FirebaseAuth.getInstance();
        String userUid = auth.getCurrentUser().getUid();

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("orders");
        dbRef.orderByChild("userUid").equalTo(userUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        allOrders.clear();
                        for (DataSnapshot orderSnap : snapshot.getChildren()) {
                            OrderModel order = orderSnap.getValue(OrderModel.class);
                            if (order != null) {
                                order.setOrderId(orderSnap.getKey()); // save Firebase key
                                allOrders.add(order);
                            }
                        }
                        // Show ongoing by default
                        filterOrders("ongoing");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(OrderActivity.this, "Failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        // Filter buttons
        btnOngoing.setOnClickListener(v -> filterOrders("ongoing"));
        btnPast.setOnClickListener(v -> filterOrders("past"));

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            Intent intent3 = new Intent(OrderActivity.this, MainMenuActivity.class);
            startActivity(intent3);
            finish();
        });
        ImageButton imgBtnHome = findViewById(R.id.imgBtnHome);
        ImageButton imgBtnOrders = findViewById(R.id.imgBtnOrders);
        ImageButton imgBtnBranches = findViewById(R.id.imgBtnBranches);
        ImageButton imgBtnProfile = findViewById(R.id.imgBtnProfile);

        imgBtnHome.setOnClickListener(v -> {
            Intent intent = new Intent(OrderActivity.this, MainMenuActivity.class);
            startActivity(intent);
            finish();
        });

        // Orders Button
        imgBtnOrders.setOnClickListener(v -> {
            Toast.makeText(this, "You are already viewing Orders", Toast.LENGTH_SHORT).show();
        });

        // Branches Button
        imgBtnBranches.setOnClickListener(v -> {
            Intent intent = new Intent(OrderActivity.this, BranchesActivity.class);
            startActivity(intent);
            finish();
        });

        // Profile Button
        imgBtnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(OrderActivity.this, UpdateProfileActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void filterOrders(String type) {
        filteredOrders.clear();

        for (OrderModel order : allOrders) {
            String status = order.getOrderStatus();

            if ("ongoing".equals(type)) {
                // Ongoing = everything that's NOT Delivered or Cancelled
                if (!"Delivered".equalsIgnoreCase(status) &&
                        !"Cancelled".equalsIgnoreCase(status)) {
                    filteredOrders.add(order);
                }
            } else if ("past".equals(type)) {
                // Past = Delivered OR Cancelled
                if ("Delivered".equalsIgnoreCase(status) ||
                        "Cancelled".equalsIgnoreCase(status)) {
                    filteredOrders.add(order);
                }
            }
        }

        orderAdapter.notifyDataSetChanged();

        if (filteredOrders.isEmpty()) {
            Toast.makeText(this, "No " + type + " orders found", Toast.LENGTH_SHORT).show();
        }

    }
}
