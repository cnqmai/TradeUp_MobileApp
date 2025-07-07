package com.example.tradeup.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout; // Import LinearLayout
import android.widget.RadioButton;    // Import RadioButton
import android.widget.RadioGroup;   // Import RadioGroup
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide; // Make sure Glide is imported if you use it for image loading
import com.example.tradeup.R;
import com.example.tradeup.model.Item;
import com.example.tradeup.model.Payment;
import com.example.tradeup.model.Transaction;
import com.example.tradeup.utils.FirebaseHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout; // Import TextInputLayout
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// NOTE: For a real Stripe integration, you would need to add Stripe SDK dependencies
// and implement a backend server to handle Payment Intents securely.
// This is a simplified simulation for demonstration purposes.

public class PaymentFragment extends Fragment {

    private static final String TAG = "PaymentFragment";

    private ImageView ivBack, ivItemImage;
    private TextView tvItemTitle, tvItemPrice;
    private TextInputEditText etCardNumber, etExpiryDate, etCvv, etCardHolderName, etUpiWalletInput; // Added etUpiWalletInput
    private CheckBox cbEscrowOption;
    private Button btnPayNow;

    private NavController navController;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    // Added for payment method selection
    private RadioGroup rgPaymentMethods;
    private RadioButton rbCreditDebitCard, rbUpi, rbWallet;
    private LinearLayout layoutCardDetails;
    private LinearLayout layoutUpiWalletDetails; // New layout for UPI/Wallet input
    private TextInputLayout tilUpiWalletInput; // New TextInputLayout for UPI/Wallet input

    private String receivedItemId;
    private String receivedItemTitle;
    private Long receivedFinalPrice;
    private String receivedBuyerId;
    private String receivedSellerId;
    private String receivedOfferId;
    private boolean isOfferPayment; // New flag to distinguish item purchase vs offer acceptance

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payment, container, false);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);

        initViews(view);
        setupListeners();
        retrieveArguments();
        setupPaymentDisplay();

        return view;
    }

    private void initViews(View view) {
        ivBack = view.findViewById(R.id.iv_back_button_payment);
        ivItemImage = view.findViewById(R.id.iv_item_image);
        tvItemTitle = view.findViewById(R.id.tv_item_title);
        tvItemPrice = view.findViewById(R.id.tv_item_price);

        // Card details
        etCardNumber = view.findViewById(R.id.et_card_number);
        etExpiryDate = view.findViewById(R.id.et_expiry_date);
        etCvv = view.findViewById(R.id.et_cvv);
        etCardHolderName = view.findViewById(R.id.et_card_holder_name);

        // Payment method selection
        rgPaymentMethods = view.findViewById(R.id.rg_payment_methods);
        rbCreditDebitCard = view.findViewById(R.id.rb_credit_debit_card);
        rbUpi = view.findViewById(R.id.rb_upi);
        rbWallet = view.findViewById(R.id.rb_wallet);
        layoutCardDetails = view.findViewById(R.id.layout_card_details);
        layoutUpiWalletDetails = view.findViewById(R.id.layout_upi_wallet_details); // Initialize new layout
        tilUpiWalletInput = view.findViewById(R.id.til_upi_wallet_input); // Initialize new TextInputLayout
        etUpiWalletInput = view.findViewById(R.id.et_upi_wallet_input); // Initialize new EditText

        cbEscrowOption = view.findViewById(R.id.cb_escrow_option);
        btnPayNow = view.findViewById(R.id.btn_pay_now);

        etExpiryDate.addTextChangedListener(new ExpiryDateTextWatcher(etExpiryDate));
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> navController.navigateUp());
        btnPayNow.setOnClickListener(v -> processPayment());

        rgPaymentMethods.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_credit_debit_card) {
                layoutCardDetails.setVisibility(View.VISIBLE);
                layoutUpiWalletDetails.setVisibility(View.GONE);
            } else if (checkedId == R.id.rb_upi) {
                layoutCardDetails.setVisibility(View.GONE);
                layoutUpiWalletDetails.setVisibility(View.VISIBLE);
                tilUpiWalletInput.setHint(getString(R.string.upi_id_hint)); // Set specific hint for UPI
                etUpiWalletInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT); // UPI IDs can be alphanumeric
            } else if (checkedId == R.id.rb_wallet) {
                layoutCardDetails.setVisibility(View.GONE);
                layoutUpiWalletDetails.setVisibility(View.VISIBLE);
                tilUpiWalletInput.setHint(getString(R.string.wallet_phone_number_hint)); // Set specific hint for Wallet
                etUpiWalletInput.setInputType(android.text.InputType.TYPE_CLASS_PHONE); // Phone number for wallet
            }
        });
    }

    private void retrieveArguments() {
        if (getArguments() != null) {
            receivedItemId = getArguments().getString("itemId");
            receivedItemTitle = getArguments().getString("itemTitle");
            receivedFinalPrice = getArguments().getLong("finalPrice", 0L);
            String itemImageUrl = getArguments().getString("itemImageUrl");
            receivedBuyerId = getArguments().getString("buyerId");
            receivedSellerId = getArguments().getString("sellerId");
            receivedOfferId = getArguments().getString("offerId");
            isOfferPayment = getArguments().getBoolean("isOfferPayment", false); // Default to false if not set

            if (itemImageUrl != null && !itemImageUrl.isEmpty()) {
                Log.d(TAG, "Attempting to load image with URL: " + itemImageUrl); // Thêm dòng này
                Glide.with(this).load(itemImageUrl).into(ivItemImage);
            } else {
                Log.d(TAG, "itemImageUrl is null or empty, loading placeholder."); // Thêm dòng này
                ivItemImage.setImageResource(R.drawable.img_placeholder);
            }
        } else {
            Toast.makeText(getContext(), getString(R.string.toast_missing_item_details), Toast.LENGTH_SHORT).show();
            navController.navigateUp();
        }
    }

    private void setupPaymentDisplay() {
        if (receivedItemTitle != null) {
            tvItemTitle.setText(receivedItemTitle);
        }
        if (receivedFinalPrice != null) {
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            currencyFormat.setMaximumFractionDigits(0);
            tvItemPrice.setText(currencyFormat.format(receivedFinalPrice));
            btnPayNow.setText(String.format(Locale.getDefault(), getString(R.string.button_pay_now_format), currencyFormat.format(receivedFinalPrice)));
        }
    }

    private void processPayment() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), getString(R.string.toast_login_required), Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = mAuth.getCurrentUser().getUid();

        if (receivedItemId == null || receivedItemTitle == null || receivedFinalPrice == null || receivedFinalPrice <= 0 || receivedBuyerId == null || receivedSellerId == null) {
            Toast.makeText(getContext(), getString(R.string.toast_invalid_payment_details), Toast.LENGTH_SHORT).show();
            return;
        }

        String paymentMethod;
        String paymentDetails = ""; // To store card last four, UPI ID, or Wallet phone number

        int selectedPaymentMethodId = rgPaymentMethods.getCheckedRadioButtonId();
        if (selectedPaymentMethodId == R.id.rb_credit_debit_card) {
            paymentMethod = "Credit/Debit Card";
            String cardNumber = Objects.requireNonNull(etCardNumber.getText()).toString().trim();
            String expiryDate = Objects.requireNonNull(etExpiryDate.getText()).toString().trim();
            String cvv = Objects.requireNonNull(etCvv.getText()).toString().trim();
            String cardHolderName = Objects.requireNonNull(etCardHolderName.getText()).toString().trim();

            if (cardNumber.isEmpty() || expiryDate.isEmpty() || cvv.isEmpty() || cardHolderName.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.toast_fill_card_details), Toast.LENGTH_SHORT).show();
                return;
            }
            if (cardNumber.length() < 16 || !isValidExpiryDate(expiryDate) || cvv.length() < 3) {
                Toast.makeText(getContext(), getString(R.string.toast_invalid_card_details), Toast.LENGTH_SHORT).show();
                return;
            }
            paymentDetails = "XXXX XXXX XXXX " + cardNumber.substring(cardNumber.length() - 4); // Store last four digits
            // In a real app, you would tokenize the card here and send to payment gateway backend.
            Toast.makeText(getContext(), getString(R.string.toast_simulating_card_payment), Toast.LENGTH_LONG).show();

        } else if (selectedPaymentMethodId == R.id.rb_upi) {
            paymentMethod = "UPI";
            String upiId = Objects.requireNonNull(etUpiWalletInput.getText()).toString().trim();
            if (upiId.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.toast_enter_upi_id), Toast.LENGTH_SHORT).show();
                return;
            }
            // Basic UPI ID validation (e.g., contains @)
            if (!upiId.contains("@") || upiId.length() < 5) { // Simple validation, could be more robust
                Toast.makeText(getContext(), getString(R.string.toast_invalid_upi_id), Toast.LENGTH_SHORT).show();
                return;
            }
            paymentDetails = upiId;
            Toast.makeText(getContext(), getString(R.string.toast_simulating_upi_payment), Toast.LENGTH_LONG).show();

        } else if (selectedPaymentMethodId == R.id.rb_wallet) {
            paymentMethod = "Wallet";
            String walletPhoneNumber = Objects.requireNonNull(etUpiWalletInput.getText()).toString().trim();
            if (walletPhoneNumber.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.toast_enter_wallet_phone), Toast.LENGTH_SHORT).show();
                return;
            }
            // Basic phone number validation
            if (walletPhoneNumber.length() < 8 || !walletPhoneNumber.matches("^[0-9]+$")) { // Simple numeric phone validation
                Toast.makeText(getContext(), getString(R.string.toast_invalid_wallet_phone), Toast.LENGTH_SHORT).show();
                return;
            }
            paymentDetails = walletPhoneNumber;
            Toast.makeText(getContext(), getString(R.string.toast_simulating_wallet_payment), Toast.LENGTH_LONG).show();

        } else {
            Toast.makeText(getContext(), getString(R.string.toast_select_payment_method), Toast.LENGTH_SHORT).show();
            return;
        }

        // Simulate payment success and record payment/transaction
        // In a real app, this would come after successful payment gateway response.
        recordPaymentAndTransaction(currentUserId, paymentMethod, paymentDetails);
    }

    private boolean isValidExpiryDate(String expiryDate) {
        if (!expiryDate.matches("\\d{2}/\\d{2}")) return false;
        try {
            int month = Integer.parseInt(expiryDate.substring(0, 2));
            int year = Integer.parseInt(expiryDate.substring(3, 5)) + 2000; // Assuming 2-digit year like YY

            if (month < 1 || month > 12) return false;

            Date now = new Date();
            SimpleDateFormat yearFormat = new SimpleDateFormat("yy", Locale.getDefault());
            SimpleDateFormat monthFormat = new SimpleDateFormat("MM", Locale.getDefault());
            int currentYear = Integer.parseInt(yearFormat.format(now)) + 2000;
            int currentMonth = Integer.parseInt(monthFormat.format(now));

            if (year < currentYear) return false;
            if (year == currentYear && month < currentMonth) return false;

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void recordPaymentAndTransaction(String userId, String paymentMethod, String paymentDetails) {
        String paymentId = mDatabase.child("payments").push().getKey();
        if (paymentId == null) {
            Toast.makeText(getContext(), getString(R.string.toast_payment_error), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Failed to generate payment ID.");
            return;
        }

        // 1. Create Payment Record
        String paymentStatus = "completed"; // Simulate success
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("payment_id", paymentId);
        paymentData.put("user_id", userId);
        paymentData.put("item_id", receivedItemId);
        paymentData.put("amount", receivedFinalPrice);
        paymentData.put("method", paymentMethod);
        paymentData.put("details", paymentDetails); // Store last 4 digits for card, or UPI ID/Wallet #
        paymentData.put("status", paymentStatus);
        paymentData.put("timestamp", ServerValue.TIMESTAMP); // Use Firebase ServerValue.TIMESTAMP for accurate server time

        mDatabase.child("payments").child(paymentId).setValue(paymentData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Payment record created: " + paymentId);
                    // 2. Create Transaction Record
                    createTransaction(userId, paymentId, cbEscrowOption.isChecked());
                    // 3. Record in payment_history for the user
                    recordPaymentInHistory(userId, paymentId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create payment record: " + e.getMessage());
                    Toast.makeText(getContext(), getString(R.string.toast_payment_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
                });
    }

    private void createTransaction(String currentUserId, String paymentId, boolean useEscrow) {
        String transactionId = mDatabase.child("transactions").push().getKey();
        if (transactionId == null) {
            Toast.makeText(getContext(), getString(R.string.toast_transaction_error), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Failed to generate transaction ID.");
            return;
        }

        Transaction transaction = new Transaction(
                transactionId,
                receivedItemId,
                receivedBuyerId,
                receivedSellerId,
                receivedFinalPrice,
                receivedOfferId, // Pass the offerId
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()),
                false, // not archived
                receivedItemTitle // item_title
        );

        transaction.setPayment_id(paymentId); // Link payment to transaction

        if (useEscrow) {
            transaction.setEscrow_status("held"); // Funds are held in escrow
            transaction.setBuyer_confirmed_receipt(false);
            transaction.setSeller_confirmed_dispatch(false);
        } else {
            transaction.setEscrow_status("n/a"); // Not applicable if escrow is not used
            transaction.setBuyer_confirmed_receipt(true); // Immediate completion if no escrow
            transaction.setSeller_confirmed_dispatch(true); // Immediate completion if no escrow
            String currentTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date());
            transaction.setCompletion_timestamp(currentTimestamp);
        }

        mDatabase.child("transactions").child(transactionId).setValue(transaction)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Transaction created: " + transactionId);

                    // Update item status to sold/inactive
                    mDatabase.child("items").child(receivedItemId).child("status").setValue("sold")
                            .addOnSuccessListener(v -> Log.d(TAG, "Item status updated to sold for " + receivedItemId))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to update item status: " + e.getMessage()));

                    // If it was an offer payment, update offer status
                    if (isOfferPayment && receivedOfferId != null) {
                        mDatabase.child("offers").child(receivedOfferId).child("status").setValue("accepted")
                                .addOnSuccessListener(v -> Log.d(TAG, "Offer status updated to accepted for " + receivedOfferId))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to update offer status: " + e.getMessage()));

                        // Decline other offers for the same item (simplified: you might need a query here)
                        // For a real app, query all offers for this item and mark others as 'declined'
                    }

                    Toast.makeText(getContext(), getString(R.string.toast_payment_success), Toast.LENGTH_LONG).show();
                    // Navigate to a confirmation screen or back to previous fragment
                    // Example:
                    // Bundle bundle = new Bundle();
                    // bundle.putString("transactionId", transactionId);
                    // navController.navigate(R.id.action_paymentFragment_to_paymentConfirmationFragment, bundle);
                    navController.navigateUp(); // Or navigate to a success fragment
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create transaction record: " + e.getMessage());
                    Toast.makeText(getContext(), getString(R.string.toast_transaction_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
                });
    }

    private void recordPaymentInHistory(String userId, String paymentId) {
        // Add the payment ID to the user's payment_history node
        mDatabase.child("payment_history").child(userId).child(paymentId).setValue(true) // Use true as placeholder value, payment details are in /payments
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Payment ID " + paymentId + " recorded in user " + userId + "'s history."))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to record payment ID in history: " + e.getMessage()));
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