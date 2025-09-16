package com.example.pizzamania;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class BranchesActivity extends AppCompatActivity {

    private LinearLayout branchContainer;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_branches);

        branchContainer = findViewById(R.id.branchContainer);

        dbRef = FirebaseDatabase.getInstance("https://pizzamania-d2775-default-rtdb.firebaseio.com/")
                .getReference("branches");

        loadBranches();

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(BranchesActivity.this, MainMenuActivity.class));
            finish();
        });

        ImageButton imgBtnHome = findViewById(R.id.imgBtnHome);
        ImageButton imgBtnOrders = findViewById(R.id.imgBtnOrders);
        ImageButton imgBtnBranches = findViewById(R.id.imgBtnBranches);
        ImageButton imgBtnProfile = findViewById(R.id.imgBtnProfile);

        imgBtnHome.setOnClickListener(v -> {
            startActivity(new Intent(BranchesActivity.this, MainMenuActivity.class));
            finish();
        });

        imgBtnOrders.setOnClickListener(v -> {
            startActivity(new Intent(BranchesActivity.this, OrderActivity.class));
            finish();
        });

        imgBtnBranches.setOnClickListener(v -> {
            Toast.makeText(this, "You are already viewing branches", Toast.LENGTH_SHORT).show();
        });

        imgBtnProfile.setOnClickListener(v -> {
            startActivity(new Intent(BranchesActivity.this, UpdateProfileActivity.class));
            finish();
        });
    }

    private void loadBranches() {
        branchContainer.removeAllViews();

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(BranchesActivity.this, "No branches found!", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (DataSnapshot branchSnap : snapshot.getChildren()) {
                    BranchDBHelper branch = branchSnap.getValue(BranchDBHelper.class);
                    if (branch != null) {
                        addBranchCard(branch);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("BranchesActivity", "Error fetching branches: " + error.getMessage());
            }
        });
    }

    private void addBranchCard(BranchDBHelper branch) {

        androidx.cardview.widget.CardView card = (androidx.cardview.widget.CardView)
                getLayoutInflater().inflate(R.layout.branch_item, branchContainer, false);

        TextView tvName = card.findViewById(R.id.tvBranchName);
        TextView tvAddress = card.findViewById(R.id.tvBranchAddress);
        TextView tvOpen = card.findViewById(R.id.tvBranchOpen);
        TextView tvClick = card.findViewById(R.id.tvBranchClick);

        tvName.setText(branch.getName() + " Branch");
        tvAddress.setText("Address: " + branch.getAddress());
        tvOpen.setText("Open Days: " + branch.getOpeningDays() + " | Hours: " + branch.getOpeningHours());
        tvClick.setText("Tap to view location >>>");

        card.setOnClickListener(v -> {
            Intent intent = new Intent(BranchesActivity.this, MapActivity.class);
            intent.putExtra("latitude", branch.getLatitude());
            intent.putExtra("longitude", branch.getLongitude());
            startActivity(intent);
        });

        branchContainer.addView(card);
    }
}
