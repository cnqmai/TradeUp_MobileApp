package com.example.tradeup.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.tradeup.R;
import com.example.tradeup.model.Item;
import com.example.tradeup.model.Payment;
import com.example.tradeup.model.Transaction;
import com.example.tradeup.utils.FirebaseHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

// NOTE: For a real Stripe integration, you would need to add Stripe SDK dependencies
// and implement a backend server to handle Payment Intents securely.
// This is a simplified simulation for demonstration purposes.

public class PaymentFragment extends Fragment {

    private static final String TAG = "PaymentFragment";

    private NavController navController;
    private FirebaseHelper firebaseHelper;
    private String currentUserId;

    // UI elements
    private ImageView ivBackButton;
    private TextView tvItemTitle, tvPaymentAmountDisplay;
    private TextInputEditText etCardNumber, etExpiryDate, etCvc, etCardHolderName;
    private CheckBox cbEscrowOption;
    private Button btnPayNow;

    // Data passed from previous fragment (e.g., from ItemDetailFragment or OfferDetailFragment)
    private String itemId;
    private String transactionId; // If payment is for an existing transaction
    private String sellerId;
    private String buyerId;
    private Long finalPrice; // SỬA ĐỔI TẠI ĐÂY: Từ double thành Long
    private String itemTitle;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseHelper = new FirebaseHelper(requireContext());
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        // Retrieve arguments
        if (getArguments() != null) {
            PaymentFragmentArgs args = PaymentFragmentArgs.fromBundle(getArguments());
            itemId = args.getItemId();
            transactionId = args.getTransactionId();
            sellerId = args.getSellerId();
            buyerId = args.getBuyerId();
            finalPrice = args.getFinalPrice(); // Lấy giá trị Long
            itemTitle = args.getItemTitle();

            Log.d(TAG, "PaymentFragment received args: " +
                    "itemId=" + itemId + ", transactionId=" + transactionId +
                    ", sellerId=" + sellerId + ", buyerId=" + buyerId +
                    ", finalPrice=" + finalPrice + ", itemTitle=" + itemTitle);
        } else {
            Log.e(TAG, "No arguments passed to PaymentFragment.");
            if (isAdded()) {
                Toast.makeText(requireContext(), "Lỗi: Không có thông tin thanh toán.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payment, container, false);

        ivBackButton = view.findViewById(R.id.iv_back_button_payment);
        tvItemTitle = view.findViewById(R.id.tv_payment_item_title);
        tvPaymentAmountDisplay = view.findViewById(R.id.tv_payment_amount_display);
        etCardNumber = view.findViewById(R.id.et_card_number);
        etExpiryDate = view.findViewById(R.id.et_expiry_date);
        etCvc = view.findViewById(R.id.et_cvc);
        etCardHolderName = view.findViewById(R.id.et_card_holder_name);
        cbEscrowOption = view.findViewById(R.id.cb_escrow_option);
        btnPayNow = view.findViewById(R.id.btn_pay_now);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        // Populate UI with received data
        if (itemTitle != null) {
            tvItemTitle.setText("Thanh toán cho: " + itemTitle);
        } else {
            tvItemTitle.setText("Thanh toán cho một giao dịch");
        }

        // Định dạng giá trị Long cho hiển thị
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        currencyFormat.setMaximumFractionDigits(0); // Đảm bảo không có số thập phân
        if (finalPrice != null) {
            tvPaymentAmountDisplay.setText(String.format(Locale.getDefault(), "Số tiền: %s", currencyFormat.format(finalPrice)));
        } else {
            tvPaymentAmountDisplay.setText("Số tiền: N/A");
        }


        setupListeners();
    }

    private void setupListeners() {
        ivBackButton.setOnClickListener(v -> {
            if (navController != null) {
                navController.popBackStack();
            }
        });

        btnPayNow.setOnClickListener(v -> {
            processPayment();
        });

        // Add input formatting for expiry date (MM/YY)
        etExpiryDate.addTextChangedListener(new ExpiryDateTextWatcher(etExpiryDate));
    }

    private void processPayment() {
        String cardNumber = Objects.requireNonNull(etCardNumber.getText()).toString().trim();
        String expiryDate = Objects.requireNonNull(etExpiryDate.getText()).toString().trim();
        String cvc = Objects.requireNonNull(etCvc.getText()).toString().trim();
        String cardHolderName = Objects.requireNonNull(etCardHolderName.getText()).toString().trim();
        boolean useEscrow = cbEscrowOption.isChecked();

        // Basic validation (for simulation)
        if (cardNumber.isEmpty() || expiryDate.isEmpty() || cvc.isEmpty() || cardHolderName.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng điền đầy đủ thông tin thẻ.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (cardNumber.length() != 16 || !expiryDate.matches("(0[1-9]|1[0-2])/([0-9]{2})") || (cvc.length() != 3 && cvc.length() != 4)) {
            Toast.makeText(getContext(), "Thông tin thẻ không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- SIMULATED STRIPE PAYMENT FLOW ---
        // In a real app, you would send card details (or a token) to your backend.
        // The backend would then create a PaymentIntent with Stripe and return its client secret.
        // You would then use Stripe.confirmPayment to complete the payment on the client side.

        Toast.makeText(getContext(), "Đang xử lý thanh toán...", Toast.LENGTH_LONG).show();

        // Simulate network delay
        new android.os.Handler().postDelayed(() -> {
            // Simulate successful payment
            String paymentStatus = "completed";
            String simulatedStripePaymentIntentId = "pi_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16); // Mock ID

            // Record payment in Firebase
            recordPayment(paymentStatus, simulatedStripePaymentIntentId, useEscrow);

        }, 2000); // 2 second delay
    }

    private void recordPayment(String status, String stripePaymentIntentId, boolean escrowEnabled) {
        final String paymentId = FirebaseDatabase.getInstance().getReference("payments").push().getKey();
        if (paymentId == null) {
            Toast.makeText(getContext(), "Lỗi: Không thể tạo ID thanh toán.", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());

        Payment newPayment = new Payment(
                paymentId,
                finalPrice, // Giá trị Long
                "VND",
                "Credit Card",
                status,
                currentTimestamp,
                transactionId,
                buyerId,
                sellerId,
                escrowEnabled,
                escrowEnabled ? "pending" : null,
                stripePaymentIntentId
        );

        firebaseHelper.addPayment(newPayment, new FirebaseHelper.DbWriteCallback() {
            @Override
            public void onSuccess() {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Thanh toán thành công!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Payment recorded successfully: " + paymentId);

                    if (transactionId != null) {
                        firebaseHelper.updateTransactionPaymentId(transactionId, paymentId, new FirebaseHelper.DbWriteCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Transaction " + transactionId + " updated with payment ID " + paymentId);
                                if (escrowEnabled) {
                                    firebaseHelper.updateTransactionStatus(transactionId, "payment_held", new FirebaseHelper.DbWriteCallback() {
                                        @Override
                                        public void onSuccess() {
                                            Log.d(TAG, "Transaction " + transactionId + " status updated to 'payment_held'.");
                                        }

                                        @Override
                                        public void onFailure(String errorMessage) {
                                            Log.e(TAG, "Failed to update transaction status to 'payment_held': " + errorMessage);
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                Log.e(TAG, "Failed to update transaction with payment ID: " + errorMessage);
                            }
                        });
                    }

                    if (itemId != null && !escrowEnabled) {
                        firebaseHelper.markItemAsSold(itemId, new FirebaseHelper.DbWriteCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Item " + itemId + " marked as Sold.");
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                Log.e(TAG, "Failed to mark item as Sold: " + errorMessage);
                            }
                        });
                    } else if (itemId != null && escrowEnabled) {
                        firebaseHelper.updateItemStatus(itemId, "pending_escrow", new FirebaseHelper.DbWriteCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Item " + itemId + " status updated to 'pending_escrow'.");
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                Log.e(TAG, "Failed to update item status to 'pending_escrow': " + errorMessage);
                            }
                        });
                    }

                    if (navController != null) {
                        navController.popBackStack();
                    }
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Thanh toán thất bại: " + errorMessage, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to record payment: " + errorMessage);
                }
            }
        });
    }

    private static class ExpiryDateTextWatcher implements android.text.TextWatcher {
        private TextInputEditText editText;
        private String current = "";

        ExpiryDateTextWatcher(TextInputEditText editText) {
            this.editText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(android.text.Editable s) {
            if (s.toString().equals(current)) {
                return;
            }
            String clean = s.toString().replaceAll("[^\\d]", "");
            int cl = clean.length();
            String formatted = "";
            for (int i = 0; i < cl; ++i) {
                formatted += clean.charAt(i);
                if (i == 1 && cl > 2) {
                    formatted += "/";
                }
            }
            current = formatted;
            editText.setText(formatted);
            editText.setSelection(formatted.length());
        }
    }
}
