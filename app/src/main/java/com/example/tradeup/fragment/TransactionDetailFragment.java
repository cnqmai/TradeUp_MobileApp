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
import java.util.Locale;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView; // Assuming you use CircleImageView

public class TransactionDetailFragment extends Fragment {

    private static final String TAG = "TransactionDetailFragment";

    // UI Elements
    private ImageView ivBackButton;
    private TextView tvTransactionDetailTitle;
    private ImageView ivTransactionItemImage;
    private TextView tvTransactionItemTitle;
    private TextView tvTransactionFinalPrice;
    private CircleImageView ivBuyerProfilePic;
    private TextView tvBuyerNameTransaction;
    private CircleImageView ivSellerProfilePic;
    private TextView tvSellerNameTransaction;
    private TextView tvTransactionDate;
    private Button btnMarkAsComplete;

    // Data
    private String transactionId;
    private Transaction currentTransaction;
    private String currentUserId;
    private NavController navController;

    // Firebase References
    private DatabaseReference transactionsRef;
    private DatabaseReference itemsRef;
    private DatabaseReference usersRef;
    private ValueEventListener transactionValueEventListener;

    public TransactionDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        if (getArguments() != null) {
            transactionId = getArguments().getString("transactionId");
        }

        transactionsRef = FirebaseDatabase.getInstance().getReference("transactions");
        itemsRef = FirebaseDatabase.getInstance().getReference("items");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transaction_detail, container, false);
        initViews(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        setupListeners();
        fetchTransactionDetails();
    }

    private void initViews(View view) {
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
        btnMarkAsComplete = view.findViewById(R.id.btn_mark_as_complete);
    }

    private void setupListeners() {
        ivBackButton.setOnClickListener(v -> navController.navigateUp());
        btnMarkAsComplete.setOnClickListener(v -> showMarkAsCompleteConfirmationDialog());
    }

    private void fetchTransactionDetails() {
        if (transactionId == null) {
            if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_error_transaction_id_missing), Toast.LENGTH_SHORT).show();
            navController.navigateUp();
            return;
        }

        transactionValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                currentTransaction = snapshot.getValue(Transaction.class);
                if (currentTransaction != null) {
                    currentTransaction.setTransaction_id(snapshot.getKey());
                    updateUI(currentTransaction);
                    fetchRelatedItemAndUserDetails(currentTransaction);
                } else {
                    if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_transaction_not_found), Toast.LENGTH_SHORT).show();
                    navController.navigateUp();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                Log.e(TAG, "Failed to load transaction details: " + error.getMessage());
                if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_error_loading_transaction, error.getMessage()), Toast.LENGTH_SHORT).show();
                navController.navigateUp();
            }
        };
        transactionsRef.child(transactionId).addValueEventListener(transactionValueEventListener);
    }

    private void fetchRelatedItemAndUserDetails(Transaction transaction) {
        // Fetch Item details
        itemsRef.child(transaction.getItem_id()).addListenerForSingleValueEvent(new ValueEventListener() {
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
                    tvTransactionItemTitle.setText(getString(R.string.item_not_found));
                    ivTransactionItemImage.setImageResource(R.drawable.img_placeholder);
                    Log.e(TAG, "Item not found for transaction: " + transaction.getItem_id());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                Log.e(TAG, "Failed to load item for transaction: " + error.getMessage());
                tvTransactionItemTitle.setText(getString(R.string.error_loading_item));
                ivTransactionItemImage.setImageResource(R.drawable.img_error);
            }
        });

        // Fetch Buyer details
        usersRef.child(transaction.getBuyer_id()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                User buyer = snapshot.getValue(User.class);
                if (buyer != null) {
                    tvBuyerNameTransaction.setText(buyer.getDisplay_name());
                    if (buyer.getProfile_picture_url() != null && !buyer.getProfile_picture_url().isEmpty()) {
                        Glide.with(requireContext())
                                .load(buyer.getProfile_picture_url())
                                .placeholder(R.drawable.img_placeholder)
                                .error(R.drawable.img_error)
                                .into(ivBuyerProfilePic);
                    } else {
                        ivBuyerProfilePic.setImageResource(R.drawable.img_placeholder);
                    }
                } else {
                    tvBuyerNameTransaction.setText(getString(R.string.unknown_user));
                    ivBuyerProfilePic.setImageResource(R.drawable.img_placeholder);
                    Log.e(TAG, "Buyer not found for ID: " + transaction.getBuyer_id());
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
        usersRef.child(transaction.getSeller_id()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                User seller = snapshot.getValue(User.class);
                if (seller != null) {
                    tvSellerNameTransaction.setText(seller.getDisplay_name());
                    if (seller.getProfile_picture_url() != null && !seller.getProfile_picture_url().isEmpty()) {
                        Glide.with(requireContext())
                                .load(seller.getProfile_picture_url())
                                .placeholder(R.drawable.img_placeholder)
                                .error(R.drawable.img_error)
                                .into(ivSellerProfilePic);
                    } else {
                        ivSellerProfilePic.setImageResource(R.drawable.img_placeholder);
                    }
                } else {
                    tvSellerNameTransaction.setText(getString(R.string.unknown_user));
                    ivSellerProfilePic.setImageResource(R.drawable.img_placeholder);
                    Log.e(TAG, "Seller not found for ID: " + transaction.getSeller_id());
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

    private void updateUI(Transaction transaction) {
        if (!isAdded()) return;

        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        currencyFormatter.setMaximumFractionDigits(2);

        // Update final price
        if (transaction.getFinal_price() != null) {
            tvTransactionFinalPrice.setText(getString(R.string.final_price_display, currencyFormatter.format(transaction.getFinal_price())));
        } else {
            tvTransactionFinalPrice.setText(getString(R.string.final_price_display, getString(R.string.price_not_available_short)));
        }

        // Update transaction date
        tvTransactionDate.setText(formatDate(transaction.getTransaction_date()));

        // Show/hide "Mark as Complete" button
        // Only show if the transaction is NOT archived and the current user is either the buyer or seller
        if (!transaction.isArchived() && (currentUserId.equals(transaction.getBuyer_id()) || currentUserId.equals(transaction.getSeller_id()))) {
            btnMarkAsComplete.setVisibility(View.VISIBLE);
        } else {
            btnMarkAsComplete.setVisibility(View.GONE);
        }
    }

    private String formatDate(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return "";
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            inputFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            Date date = inputFormat.parse(timestamp);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            return outputFormat.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing transaction date: " + timestamp, e);
            return timestamp;
        }
    }

    private void showMarkAsCompleteConfirmationDialog() {
        if (!isAdded()) return;
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_title_confirm_complete)
                .setMessage(R.string.dialog_message_confirm_complete)
                .setPositiveButton(R.string.button_yes, (dialog, which) -> markTransactionAsComplete())
                .setNegativeButton(R.string.button_no, null)
                .show();
    }

    private void markTransactionAsComplete() {
        if (currentTransaction == null || currentTransaction.getTransaction_id() == null) {
            if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_error_transaction_data_missing), Toast.LENGTH_SHORT).show();
            return;
        }

        transactionsRef.child(currentTransaction.getTransaction_id()).child("archived").setValue(true)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_transaction_marked_complete), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Transaction " + currentTransaction.getTransaction_id() + " marked as complete.");
                    // Optionally navigate back after marking as complete
                    navController.navigateUp();
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_error_marking_complete, e.getMessage()), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to mark transaction as complete: " + e.getMessage());
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
        btnMarkAsComplete = null;
    }
}
