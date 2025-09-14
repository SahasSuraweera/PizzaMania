package com.example.pizzamania;

import android.os.Bundle;
import android.util.Log;
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
                        addBranchView(branch);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("BranchesActivity", "Error fetching branches: " + error.getMessage());
            }
        });
    }

    private void addBranchView(BranchDBHelper branch) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);
        layout.setBackgroundColor(0xFFEFEFEF);

        TextView tvName = new TextView(this);
        tvName.setText("Branch: " + branch.getName());
        tvName.setTextSize(16f);

        TextView tvAddress = new TextView(this);
        tvAddress.setText("Address: " + branch.getAddress());

        TextView tvLatLng = new TextView(this);
        tvLatLng.setText("Lat/Lng: " + branch.getLatitude() + " / " + branch.getLongitude());

        TextView tvOpen = new TextView(this);
        tvOpen.setText("Open Days: " + branch.getOpeningDays() + " | Hours: " + branch.getOpeningHours());

        layout.addView(tvName);
        layout.addView(tvAddress);
        layout.addView(tvLatLng);
        layout.addView(tvOpen);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        layout.setLayoutParams(params);

        branchContainer.addView(layout);
    }

}
