package com.example.tradeup.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.tradeup.R;
import com.example.tradeup.model.Item;
import com.example.tradeup.model.Transaction;
import com.example.tradeup.model.User;
import com.example.tradeup.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap; // Add this import
import java.util.Locale;
import java.util.Map; // Add this import
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class TransactionDetailFragment extends Fragment {

    private static final String TAG = "TransactionDetailFragment";

    // UI elements
    private ImageView ivBackButton;
    private TextView tvTransactionDetailTitle;
    private ImageView ivTransactionItemImage;
    private TextView tvTransactionItemTitle, tvTransactionFinalPrice;
    private CircleImageView ivBuyerProfilePic, ivSellerProfilePic;
    private TextView tvBuyerNameTransaction, tvSellerNameTransaction;
    private TextView tvTransactionDate;
    private TextView tvTransactionStatus; // NEW: Display transaction status
    private TextView tvEscrowStatus; // NEW: Display escrow status
    private LinearLayout layoutConfirmationButtons; // NEW: Layout for confirmation buttons

    private Button btnBuyerConfirmReceipt; // NEW: Buyer's confirmation button
    private Button btnSellerConfirmDispatch; // NEW: Seller's confirmation button
    private Button btnLeaveReview; // Existing, but logic will change

    private NavController navController;
    private FirebaseHelper firebaseHelper;
    private String transactionId;
    private Transaction currentTransaction;
    private String currentUserId;
    private DatabaseReference transactionsRef;
    private ValueEventListener transactionValueEventListener;

    public TransactionDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseHelper = new FirebaseHelper(requireContext());
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        if (getArguments() != null) {
            transactionId = getArguments().getString("transactionId");
            if (transactionId != null) {
                transactionsRef = FirebaseDatabase.getInstance().getReference("transactions");
            } else {
                Log.e(TAG, "Transaction ID is null in arguments.");
                if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_error_transaction_id_missing), Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "Arguments are null.");
            if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_error_transaction_id_missing), Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transaction_detail, container, false);

        ivBackButton = view.findViewById(R.id.iv_back_button_transaction_detail);
        tvTransactionDetailTitle = view.findViewById(R.id.tv_transaction_detail_title);
        ivTransactionItemImage = view.findViewById(R.id.iv_transaction_item_image);
        tvTransactionItemTitle = view.findViewById(R.id.tv_transaction_item_title);
        tvTransactionFinalPrice = view.findViewById(R.id.tv_transaction_final_price);
        ivBuyerProfilePic = view.findViewById(R.id.iv_buyer_profile_pic);
        tvBuyerNameTransaction = view.findViewById(R.id.tv_buyer_name_transaction);
        ivSellerProfilePic = view.findViewById(R.id.iv_seller_profile_pic);
        tvSellerNameTransaction = view.findViewById(R.id.tv_seller_name_transaction);
        tvTransactionDate = view.findViewById(R.id.tv_transaction_date);
        tvTransactionStatus = view.findViewById(R.id.tv_transaction_status); // NEW
        tvEscrowStatus = view.findViewById(R.id.tv_escrow_status); // NEW
        layoutConfirmationButtons = view.findViewById(R.id.layout_confirmation_buttons); // NEW

        btnBuyerConfirmReceipt = view.findViewById(R.id.btn_buyer_confirm_receipt); // NEW
        btnSellerConfirmDispatch = view.findViewById(R.id.btn_seller_confirm_dispatch); // NEW
        btnLeaveReview = view.findViewById(R.id.btn_leave_review); // Existing

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        setupListeners();
        fetchTransactionDetails();
    }

    private void setupListeners() {
        ivBackButton.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigateUp();
            }
        });

        btnBuyerConfirmReceipt.setOnClickListener(v -> showConfirmationDialog(true)); // True for buyer
        btnSellerConfirmDispatch.setOnClickListener(v -> showConfirmationDialog(false)); // False for seller

        btnLeaveReview.setOnClickListener(v -> {
            if (currentTransaction != null && currentTransaction.isArchived()) { // Only allow review if archived
                String reviewedUserId = "";
                if (currentUserId.equals(currentTransaction.getBuyer_id())) {
                    reviewedUserId = currentTransaction.getSeller_id();
                } else if (currentUserId.equals(currentTransaction.getSeller_id())) {
                    reviewedUserId = currentTransaction.getBuyer_id();
                }

                if (!reviewedUserId.isEmpty()) {
                    Bundle bundle = new Bundle();
                    bundle.putString("transactionId", currentTransaction.getTransaction_id());
                    bundle.putString("itemId", currentTransaction.getItem_id());
                    bundle.putString("reviewedUserId", reviewedUserId);
                    navController.navigate(R.id.action_transactionDetailFragment_to_ratingReviewFragment, bundle);
                } else {
                    Toast.makeText(getContext(), getString(R.string.toast_error_finding_user_to_review), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), getString(R.string.toast_transaction_not_completed_yet), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchTransactionDetails() {
        if (transactionId == null) {
            Log.e(TAG, "Transaction ID is null, cannot fetch details.");
            return;
        }

        transactionValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) {
                    Log.w(TAG, "Fragment not added, skipping UI update.");
                    return;
                }
                currentTransaction = snapshot.getValue(Transaction.class);
                if (currentTransaction != null) {
                    updateUI(currentTransaction);
                    fetchItemAndUserNames(currentTransaction.getItem_id(), currentTransaction.getBuyer_id(), currentTransaction.getSeller_id());
                } else {
                    if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_transaction_not_found), Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Transaction data is null for ID: " + transactionId);
                    navController.navigateUp();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                Log.e(TAG, "Error loading transaction: " + error.getMessage());
                if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_error_loading_transaction, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        };
        transactionsRef.child(transactionId).addValueEventListener(transactionValueEventListener);
    }

    private void updateUI(Transaction transaction) {
        if (!isAdded()) return;

        tvTransactionDetailTitle.setText(getString(R.string.transaction_detail_title));

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        currencyFormat.setMaximumFractionDigits(0);
        tvTransactionFinalPrice.setText(currencyFormat.format(transaction.getFinal_price()));

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            inputFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            Date date = inputFormat.parse(transaction.getTransaction_date());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            tvTransactionDate.setText(outputFormat.format(date));
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing transaction date: " + transaction.getTransaction_date(), e);
            tvTransactionDate.setText(transaction.getTransaction_date());
        }

        // NEW: Update transaction status and escrow status
        tvTransactionStatus.setText(getString(R.string.transaction_status_display, transaction.isArchived() ? "Hoàn thành" : "Đang chờ"));
        if (transaction.getEscrow_status() != null && !transaction.getEscrow_status().isEmpty()) {
            tvEscrowStatus.setText(getString(R.string.escrow_status_display, transaction.getEscrow_status()));
            tvEscrowStatus.setVisibility(View.VISIBLE);
        } else {
            tvEscrowStatus.setVisibility(View.GONE);
        }

        // Manage button visibility based on roles and escrow status
        boolean isBuyer = currentUserId.equals(transaction.getBuyer_id());
        boolean isSeller = currentUserId.equals(transaction.getSeller_id());

        // Reset button visibility
        layoutConfirmationButtons.setVisibility(View.GONE);
        btnBuyerConfirmReceipt.setVisibility(View.GONE);
        btnSellerConfirmDispatch.setVisibility(View.GONE);
        btnLeaveReview.setVisibility(View.GONE);

        if ("held".equals(transaction.getEscrow_status())) {
            layoutConfirmationButtons.setVisibility(View.VISIBLE); // Show the confirmation button layout

            // Money is held in escrow
            if (isBuyer) {
                if (!transaction.getBuyer_confirmed_receipt()) {
                    btnBuyerConfirmReceipt.setVisibility(View.VISIBLE);
                    btnBuyerConfirmReceipt.setText(getString(R.string.button_confirm_receipt));
                    btnBuyerConfirmReceipt.setEnabled(true);
                } else {
                    btnBuyerConfirmReceipt.setVisibility(View.VISIBLE);
                    btnBuyerConfirmReceipt.setText(getString(R.string.button_receipt_confirmed));
                    btnBuyerConfirmReceipt.setEnabled(false);
                }
            }

            if (isSeller) {
                if (!transaction.getSeller_confirmed_dispatch()) {
                    btnSellerConfirmDispatch.setVisibility(View.VISIBLE);
                    btnSellerConfirmDispatch.setText(getString(R.string.button_confirm_dispatch));
                    btnSellerConfirmDispatch.setEnabled(true);
                } else {
                    btnSellerConfirmDispatch.setVisibility(View.VISIBLE);
                    btnSellerConfirmDispatch.setText(getString(R.string.button_dispatch_confirmed));
                    btnSellerConfirmDispatch.setEnabled(false);
                }
            }

            // If both confirmed, allow review (and money should be released by backend)
            if (transaction.getBuyer_confirmed_receipt() && transaction.getSeller_confirmed_dispatch()) {
                btnLeaveReview.setVisibility(View.VISIBLE);
                layoutConfirmationButtons.setVisibility(View.GONE); // Hide confirmation buttons if completed
            }

        } else if ("released".equals(transaction.getEscrow_status()) || transaction.isArchived()) {
            // Money released or transaction archived (completed directly)
            btnLeaveReview.setVisibility(View.VISIBLE);
            layoutConfirmationButtons.setVisibility(View.GONE);
        } else if ("not_applicable".equals(transaction.getEscrow_status())) {
            // No escrow, assume completed directly
            btnLeaveReview.setVisibility(View.VISIBLE);
            layoutConfirmationButtons.setVisibility(View.GONE);
        }
        // Handle other escrow statuses like "refunded", "disputed" if you implement them
    }


    private void fetchItemAndUserNames(String itemId, String buyerId, String sellerId) {
        DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("items");
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Fetch Item details
        itemsRef.child(itemId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                Item item = snapshot.getValue(Item.class);
                if (item != null) {
                    tvTransactionItemTitle.setText(item.getTitle());
                    if (item.getPhotos() != null && !item.getPhotos().isEmpty()) {
                        Glide.with(requireContext())
                                .load(item.getPhotos().get(0))
                                .placeholder(R.drawable.img_placeholder)
                                .error(R.drawable.img_error)
                                .into(ivTransactionItemImage);
                    } else {
                        ivTransactionItemImage.setImageResource(R.drawable.img_placeholder);
                    }
                } else {
                    Log.e(TAG, "Item not found for transaction: " + itemId);
                    tvTransactionItemTitle.setText(getString(R.string.item_not_found));
                    ivTransactionItemImage.setImageResource(R.drawable.img_placeholder);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                Log.e(TAG, "Failed to load item details: " + error.getMessage());
                tvTransactionItemTitle.setText(getString(R.string.error_loading_item));
                ivTransactionItemImage.setImageResource(R.drawable.img_error);
            }
        });

        // Fetch Buyer details
        usersRef.child(buyerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                User buyer = snapshot.getValue(User.class);
                if (buyer != null) {
                    tvBuyerNameTransaction.setText(buyer.getDisplay_name());
                    if (buyer.getProfile_picture_url() != null && !buyer.getProfile_picture_url().isEmpty()) {
                        Glide.with(requireContext())
                                .load(buyer.getProfile_picture_url())
                                .placeholder(R.drawable.img_profile_placeholder)
                                .error(R.drawable.img_profile_placeholder)
                                .into(ivBuyerProfilePic);
                    } else {
                        ivBuyerProfilePic.setImageResource(R.drawable.img_profile_placeholder);
                    }
                } else {
                    Log.e(TAG, "Buyer not found for ID: " + buyerId);
                    tvBuyerNameTransaction.setText(getString(R.string.unknown_user));
                    ivBuyerProfilePic.setImageResource(R.drawable.img_profile_placeholder);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                Log.e(TAG, "Failed to load buyer details: " + error.getMessage());
                tvBuyerNameTransaction.setText(getString(R.string.error_loading_user));
                ivBuyerProfilePic.setImageResource(R.drawable.img_error);
            }
        });

        // Fetch Seller details
        usersRef.child(sellerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                User seller = snapshot.getValue(User.class);
                if (seller != null) {
                    tvSellerNameTransaction.setText(seller.getDisplay_name());
                    if (seller.getProfile_picture_url() != null && !seller.getProfile_picture_url().isEmpty()) {
                        Glide.with(requireContext())
                                .load(seller.getProfile_picture_url())
                                .placeholder(R.drawable.img_profile_placeholder)
                                .error(R.drawable.img_profile_placeholder)
                                .into(ivSellerProfilePic);
                    } else {
                        ivSellerProfilePic.setImageResource(R.drawable.img_profile_placeholder);
                    }
                } else {
                    Log.e(TAG, "Seller not found for ID: " + sellerId);
                    tvSellerNameTransaction.setText(getString(R.string.unknown_user));
                    ivSellerProfilePic.setImageResource(R.drawable.img_profile_placeholder);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                Log.e(TAG, "Failed to load seller details: " + error.getMessage());
                tvSellerNameTransaction.setText(getString(R.string.error_loading_user));
                ivSellerProfilePic.setImageResource(R.drawable.img_error);
            }
        });
    }

    private void showConfirmationDialog(boolean isBuyerConfirming) {
        if (currentTransaction == null || transactionId == null) {
            Toast.makeText(getContext(), getString(R.string.toast_error_transaction_data_missing), Toast.LENGTH_SHORT).show();
            return;
        }

        String title, message;
        if (isBuyerConfirming) {
            title = getString(R.string.dialog_title_confirm_receipt);
            message = getString(R.string.dialog_message_confirm_receipt);
        } else {
            title = getString(R.string.dialog_title_confirm_dispatch);
            message = getString(R.string.dialog_message_confirm_dispatch);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.button_confirm), (dialog, which) -> {
                    performConfirmation(isBuyerConfirming);
                })
                .setNegativeButton(getString(R.string.button_cancel), (dialog, which) -> dialog.cancel())
                .show();
    }

    private void performConfirmation(boolean isBuyerConfirming) {
        if (currentTransaction == null || transactionId == null) {
            Toast.makeText(getContext(), getString(R.string.toast_error_transaction_data_missing), Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        if (isBuyerConfirming) {
            updates.put("buyer_confirmed_receipt", true);
        } else {
            updates.put("seller_confirmed_dispatch", true);
        }

        firebaseHelper.updateTransactionEscrowStatusAndConfirmations(transactionId, updates, new FirebaseHelper.DbWriteCallback() {
            @Override
            public void onSuccess() {
                if (isAdded()) {
                    Toast.makeText(getContext(), isBuyerConfirming ? getString(R.string.toast_receipt_confirmed) : getString(R.string.toast_dispatch_confirmed), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, (isBuyerConfirming ? "Buyer" : "Seller") + " confirmed for transaction " + transactionId);

                    // Re-fetch transaction to get updated state and check for completion
                    firebaseHelper.getTransactionById(transactionId, new FirebaseHelper.DbReadCallback<Transaction>() {
                        @Override
                        public void onSuccess(Transaction updatedTransaction) {
                            if (updatedTransaction != null && updatedTransaction.getBuyer_confirmed_receipt() && updatedTransaction.getSeller_confirmed_dispatch()) {
                                // Both parties have confirmed, now release escrow and mark as archived
                                releaseEscrowAndArchiveTransaction(updatedTransaction);
                            } else {
                                // Update UI to reflect current confirmation status
                                updateUI(updatedTransaction);
                            }
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Log.e(TAG, "Failed to re-fetch transaction after confirmation: " + errorMessage);
                            if (isAdded()) Toast.makeText(getContext(), getString(R.string.toast_error_reloading_transaction_status), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (isAdded()) Toast.makeText(getContext(), getString(R.string.toast_error_confirming_transaction, errorMessage), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to confirm transaction: " + errorMessage);
            }
        });
    }

    private void releaseEscrowAndArchiveTransaction(Transaction transaction) {
        if (transaction == null || transaction.getTransaction_id() == null) {
            Log.e(TAG, "Cannot release escrow: transaction data missing.");
            return;
        }

        // 1. Update Transaction status to "released" and archived=true
        Map<String, Object> transactionUpdates = new HashMap<>();
        transactionUpdates.put("escrow_status", "released");
        transactionUpdates.put("archived", true);
        transactionUpdates.put("completion_timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date()));

        firebaseHelper.updateTransactionEscrowStatusAndConfirmations(transaction.getTransaction_id(), transactionUpdates, new FirebaseHelper.DbWriteCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Transaction " + transaction.getTransaction_id() + " escrow released and archived.");
                if (isAdded()) Toast.makeText(getContext(), getString(R.string.toast_transaction_completed_and_escrow_released), Toast.LENGTH_LONG).show();

                // 2. Update associated Payment's escrow status to "released"
                if (transaction.getPayment_id() != null) { // Assuming Transaction model now has payment_id
                    firebaseHelper.updatePaymentEscrowStatus(transaction.getPayment_id(), "released", new FirebaseHelper.DbWriteCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Payment " + transaction.getPayment_id() + " escrow status updated to 'released'.");
                        }
                        @Override
                        public void onFailure(String errorMessage) {
                            Log.e(TAG, "Failed to update payment escrow status: " + errorMessage);
                        }
                    });
                }

                // 3. Mark the item as "Sold"
                firebaseHelper.markItemAsSold(transaction.getItem_id(), new FirebaseHelper.DbWriteCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Item " + transaction.getItem_id() + " marked as Sold.");
                        // No toast here, as the main transaction completion toast is sufficient
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Failed to mark item as Sold during escrow release: " + errorMessage);
                        if (isAdded()) Toast.makeText(getContext(), getString(R.string.toast_error_marking_item_sold_escrow, errorMessage), Toast.LENGTH_SHORT).show();
                    }
                });

                // Update UI after completion
                updateUI(transaction); // Re-render UI based on the updated transaction state
                btnLeaveReview.setVisibility(View.VISIBLE); // Show review button
                layoutConfirmationButtons.setVisibility(View.GONE); // Hide confirmation buttons layout
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to release escrow and archive transaction: " + errorMessage);
                if (isAdded()) Toast.makeText(getContext(), getString(R.string.toast_error_completing_transaction_escrow, errorMessage), Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (transactionsRef != null && transactionValueEventListener != null) {
            transactionsRef.child(transactionId).removeEventListener(transactionValueEventListener);
            Log.d(TAG, "Firebase listener removed for transaction detail.");
        }
        // Nullify view references to prevent memory leaks
        ivBackButton = null;
        tvTransactionDetailTitle = null;
        ivTransactionItemImage = null;
        tvTransactionItemTitle = null;
        tvTransactionFinalPrice = null;
        ivBuyerProfilePic = null;
        tvBuyerNameTransaction = null;
        ivSellerProfilePic = null;
        tvSellerNameTransaction = null;
        tvTransactionDate = null;
        tvTransactionStatus = null;
        tvEscrowStatus = null;
        layoutConfirmationButtons = null;
        btnBuyerConfirmReceipt = null;
        btnSellerConfirmDispatch = null;
        btnLeaveReview = null;
    }
}
