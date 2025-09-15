package com.example.pizzamania;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.Serializable;

import lk.payhere.androidsdk.PHConfigs;
import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.Item;

public class PaymentActivity extends AppCompatActivity {

    private static final String TAG = "PaymentActivity";

    private Button button;
    private TextView textView;

    private final ActivityResultLauncher<Intent> payHereLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();

                            if (data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
                                Serializable serializable =
                                        data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);

                                if (serializable instanceof PHResponse) {
                                    PHResponse<?> response = (PHResponse<?>) serializable;

                                    String msg = response.isSuccess()
                                            ? "Payment Success: " + response.getData()
                                            : "Payment Failed: " + response;

                                    if (response.isSuccess()) {
                                        Intent resultIntent = new Intent();
                                        resultIntent.putExtra("status", "success");
                                        setResult(RESULT_OK, resultIntent);
                                        finish();
                                    } else {
                                        Intent resultIntent = new Intent();
                                        resultIntent.putExtra("status", "failed");
                                        setResult(RESULT_OK, resultIntent);
                                        finish();
                                    }
                                    Log.d(TAG, msg);
                                    textView.setText(msg);
                                }
                            }
                        } else {
                            Log.d(TAG, "Payment cancelled or failed.");
                            textView.setText("Payment cancelled or failed.");
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        Intent intent = getIntent();
        String orderId = intent.getStringExtra("orderID");
        double totalCharge = intent.getDoubleExtra("totalCharge", 0.0);
        String userEmail = intent.getStringExtra("userEmail");

        TextView textOrderId = findViewById(R.id.textOrderID);
        TextView textTotalFee = findViewById(R.id.textTotalFee);
        TextView textEmail = findViewById(R.id.textEmail);
        button = findViewById(R.id.button);
        textView = findViewById(R.id.textView);

        textOrderId.setText("Order ID: " + orderId);
        textTotalFee.setText("Payble Amount: " + totalCharge);
        textEmail.setText("Confirmation Email: " + userEmail);

        button.setOnClickListener(v -> makePayment(orderId, totalCharge, userEmail));
    }

    private void makePayment(String orderId, Double totalCharge, String email) {
        InitRequest req = new InitRequest();
        req.setMerchantId("1232033");
        req.setCurrency("LKR");
        req.setAmount(totalCharge);
        req.setOrderId(orderId);
        req.setItemsDescription("Pizza Order");
        req.setCustom1("This is the custom message 1");
        req.setCustom2("This is the custom message 2");
        req.getCustomer().setFirstName("");
        req.getCustomer().setLastName("");
        req.getCustomer().setEmail(email);
        req.getCustomer().setPhone("");
        req.getCustomer().getAddress().setAddress("");
        req.getCustomer().getAddress().setCity("");
        req.getCustomer().getAddress().setCountry("");

//Optional Params
        req.getCustomer().getDeliveryAddress().setAddress("");
        req.getCustomer().getDeliveryAddress().setCity("");
        req.getCustomer().getDeliveryAddress().setCountry("");

        Intent intent = new Intent(this, PHMainActivity.class);
        intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);
        PHConfigs.setBaseUrl(PHConfigs.SANDBOX_URL);
        payHereLauncher.launch(intent);
    }
}
