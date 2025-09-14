package com.example.pizzamania;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
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
    }

    private void filterOrders(String type) {
        filteredOrders.clear();

        for (OrderModel order : allOrders) {
            if ("ongoing".equals(type) && "pending".equalsIgnoreCase(order.getOrderStatus())) {
                filteredOrders.add(order);
            } else if ("past".equals(type) && !"pending".equalsIgnoreCase(order.getOrderStatus())) {
                filteredOrders.add(order);
            }
        }

        orderAdapter.notifyDataSetChanged();

        if (filteredOrders.isEmpty()) {
            Toast.makeText(this, "No " + type + " orders found", Toast.LENGTH_SHORT).show();
        }

    }
}
