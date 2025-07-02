package com.example.tradeup.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup.R;
import com.example.tradeup.adapter.RecentTransactionAdapter;
import com.example.tradeup.adapter.SavedCardAdapter;
import com.example.tradeup.model.Payment;
import com.example.tradeup.model.SavedCard;
import com.example.tradeup.model.Transaction; // Import Transaction model
import com.example.tradeup.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

// NEW: Import the generated Directions class
import com.example.tradeup.fragment.PaymentMethodsFragmentDirections; // Dòng này cần được thêm vào

public class PaymentMethodsFragment extends Fragment {

    private static final String TAG = "PaymentMethodsFragment";

    private NavController navController;
    private FirebaseHelper firebaseHelper;
    private FirebaseUser currentUser;

    // UI Elements
    private ImageView ivBackButton;
    private Button btnAddCard, btnLinkUpi;
    private RecyclerView recyclerSavedCards, recyclerRecentTransactions;
    private TextView textNoSavedCards, textNoRecentTransactions;
    private TextView tvSecurePaymentItemTitle, tvSecurePaymentAmount;
    private Button btnProceedToPaymentSecure;

    private SavedCardAdapter savedCardAdapter;
    private RecentTransactionAdapter recentTransactionAdapter;

    // Data for Secure Payment section (mock data for now, would come from arguments in a real scenario)
    private String securePaymentItemId = "item_007"; // Example item ID
    private String securePaymentTransactionId = "transaction_007"; // Example transaction ID
    private String securePaymentSellerId = "NTv5DwTFwUZK3lSo94U66lDG8r83";
    private String securePaymentBuyerId = "SArdKr2bOLaZLR1HzOrPq1FcZXV2";
    private double securePaymentFinalPrice = 599.99; // Example price
    private String securePaymentItemTitle = "Guitar Cổ điển"; // Example item title

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseHelper = new FirebaseHelper(requireContext());
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Log.e(TAG, "User not logged in.");
            if (isAdded()) {
                Toast.makeText(requireContext(), "Vui lòng đăng nhập để xem phương thức thanh toán.", Toast.LENGTH_SHORT).show();
                // Optionally navigate to login
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payment_methods, container, false);

        ivBackButton = view.findViewById(R.id.iv_back_button_payment_methods);
        btnAddCard = view.findViewById(R.id.btn_add_card);
        btnLinkUpi = view.findViewById(R.id.btn_link_upi);
        recyclerSavedCards = view.findViewById(R.id.recycler_saved_cards);
        recyclerRecentTransactions = view.findViewById(R.id.recycler_recent_transactions);
        textNoSavedCards = view.findViewById(R.id.text_no_saved_cards);
        textNoRecentTransactions = view.findViewById(R.id.text_no_recent_transactions);
        tvSecurePaymentItemTitle = view.findViewById(R.id.tv_secure_payment_item_title);
        tvSecurePaymentAmount = view.findViewById(R.id.tv_secure_payment_amount);
        btnProceedToPaymentSecure = view.findViewById(R.id.btn_proceed_to_payment_secure);

        // Setup RecyclerViews
        recyclerSavedCards.setLayoutManager(new LinearLayoutManager(getContext()));
        savedCardAdapter = new SavedCardAdapter(requireContext(), new ArrayList<>());
        recyclerSavedCards.setAdapter(savedCardAdapter);

        recyclerRecentTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        recentTransactionAdapter = new RecentTransactionAdapter(requireContext(), new ArrayList<>(), Objects.requireNonNull(currentUser).getUid());
        recyclerRecentTransactions.setAdapter(recentTransactionAdapter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        setupListeners();
        loadSavedCards();
        loadRecentTransactions();
        setupSecurePaymentSection();
    }

    private void setupListeners() {
        ivBackButton.setOnClickListener(v -> {
            if (navController != null) {
                navController.popBackStack();
            }
        });

        btnAddCard.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Chức năng thêm thẻ đang được phát triển.", Toast.LENGTH_SHORT).show();
        });

        btnLinkUpi.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Chức năng liên kết UPI đang được phát triển.", Toast.LENGTH_SHORT).show();
        });

        btnProceedToPaymentSecure.setOnClickListener(v -> {
            if (currentUser == null) {
                Toast.makeText(getContext(), "Vui lòng đăng nhập để thanh toán.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Navigate to PaymentFragment with secure payment details
            // FIX: Use PaymentMethodsFragmentDirections instead of PaymentFragmentDirections
            PaymentMethodsFragmentDirections.ActionPaymentMethodsFragmentToPaymentFragment action =
                    PaymentMethodsFragmentDirections.actionPaymentMethodsFragmentToPaymentFragment(
                            securePaymentItemId,
                            securePaymentTransactionId,
                            securePaymentSellerId,
                            securePaymentBuyerId,
                            (float) securePaymentFinalPrice,
                            securePaymentItemTitle
                    );
            navController.navigate(action);
        });
    }

    private void loadSavedCards() {
        if (currentUser == null) {
            textNoSavedCards.setVisibility(View.VISIBLE);
            recyclerSavedCards.setVisibility(View.GONE);
            return;
        }

        firebaseHelper.getSavedCardsForUser(currentUser.getUid(), new FirebaseHelper.DbReadCallback<List<SavedCard>>() {
            @Override
            public void onSuccess(List<SavedCard> savedCards) {
                if (!isAdded()) return;
                if (savedCards != null && !savedCards.isEmpty()) {
                    savedCardAdapter.updateSavedCardList(savedCards);
                    textNoSavedCards.setVisibility(View.GONE);
                    recyclerSavedCards.setVisibility(View.VISIBLE);
                } else {
                    textNoSavedCards.setVisibility(View.VISIBLE);
                    recyclerSavedCards.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (!isAdded()) return;
                Log.e(TAG, "Failed to load saved cards: " + errorMessage);
                Toast.makeText(getContext(), "Lỗi tải thẻ đã lưu: " + errorMessage, Toast.LENGTH_SHORT).show();
                textNoSavedCards.setVisibility(View.VISIBLE);
                recyclerSavedCards.setVisibility(View.GONE);
            }
        });
    }

    private void loadRecentTransactions() {
        if (currentUser == null) {
            textNoRecentTransactions.setVisibility(View.VISIBLE);
            recyclerRecentTransactions.setVisibility(View.GONE);
            return;
        }

        firebaseHelper.getPaymentHistoryForUser(currentUser.getUid(), new FirebaseHelper.DbReadCallback<List<Payment>>() {
            @Override
            public void onSuccess(List<Payment> payments) {
                if (!isAdded()) return;
                if (payments != null && !payments.isEmpty()) {
                    Collections.sort(payments, (p1, p2) -> p2.getTimestamp().compareTo(p1.getTimestamp()));
                    List<Payment> recent = payments.subList(0, Math.min(payments.size(), 3));
                    recentTransactionAdapter.updateRecentTransactionList(recent);
                    textNoRecentTransactions.setVisibility(View.GONE);
                    recyclerRecentTransactions.setVisibility(View.VISIBLE);
                } else {
                    textNoRecentTransactions.setVisibility(View.VISIBLE);
                    recyclerRecentTransactions.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (!isAdded()) return;
                Log.e(TAG, "Failed to load recent transactions: " + errorMessage);
                Toast.makeText(getContext(), "Lỗi tải giao dịch gần đây: " + errorMessage, Toast.LENGTH_SHORT).show();
                textNoRecentTransactions.setVisibility(View.VISIBLE);
                recyclerRecentTransactions.setVisibility(View.GONE);
            }
        });
    }

    private void setupSecurePaymentSection() {
        tvSecurePaymentItemTitle.setText(securePaymentItemTitle);
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        tvSecurePaymentAmount.setText(currencyFormat.format(securePaymentFinalPrice));
        btnProceedToPaymentSecure.setText(String.format(Locale.getDefault(), "Tiến hành Thanh toán - %s", currencyFormat.format(securePaymentFinalPrice)));
    }
}
