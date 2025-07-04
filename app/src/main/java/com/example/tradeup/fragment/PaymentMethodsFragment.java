package com.example.tradeup.fragment;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup.R;
import com.example.tradeup.adapter.RecentTransactionAdapter;
import com.example.tradeup.adapter.SavedCardAdapter;
import com.example.tradeup.model.Payment;
import com.example.tradeup.model.SavedCard;
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

import com.example.tradeup.fragment.PaymentMethodsFragmentDirections;

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
    private LinearLayout layoutSecurePaymentSection;

    private SavedCardAdapter savedCardAdapter;
    private RecentTransactionAdapter recentTransactionAdapter;

    // Data for Secure Payment section (now from arguments)
    private String receivedItemId;
    private String receivedTransactionId;
    private String receivedSellerId;
    private String receivedBuyerId;
    private Long receivedFinalPrice = 0L; // SỬA ĐỔI TẠI ĐÂY: Từ double thành Long
    private String receivedItemTitle;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseHelper = new FirebaseHelper(requireContext());
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Log.e(TAG, "User not logged in.");
            if (isAdded()) {
                Toast.makeText(requireContext(), "Vui lòng đăng nhập để xem phương thức thanh toán.", Toast.LENGTH_SHORT).show();
            }
        }

        // Retrieve arguments for secure payment section
        if (getArguments() != null) {
            PaymentMethodsFragmentArgs args = PaymentMethodsFragmentArgs.fromBundle(getArguments());
            receivedItemId = args.getItemId();
            receivedTransactionId = args.getTransactionId();
            receivedSellerId = args.getSellerId();
            receivedBuyerId = args.getBuyerId();
            receivedFinalPrice = args.getFinalPrice(); // Lấy giá trị Long
            receivedItemTitle = args.getItemTitle();

            Log.d(TAG, "Received arguments for secure payment: " +
                    "itemId=" + receivedItemId + ", transactionId=" + receivedTransactionId +
                    ", sellerId=" + receivedSellerId + ", buyerId=" + receivedBuyerId +
                    ", finalPrice=" + receivedFinalPrice + ", itemTitle=" + receivedItemTitle);
        } else {
            Log.d(TAG, "No arguments received for secure payment section.");
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
        layoutSecurePaymentSection = view.findViewById(R.id.layout_secure_payment_section);

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
            PaymentMethodsFragmentDirections.ActionPaymentMethodsFragmentToPaymentFragment action =
                    PaymentMethodsFragmentDirections.actionPaymentMethodsFragmentToPaymentFragment(
                            receivedItemId,
                            receivedTransactionId,
                            receivedSellerId,
                            receivedBuyerId,
                            receivedFinalPrice, // Truyền giá trị Long trực tiếp
                            receivedItemTitle
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
        // Only show this section if relevant arguments are passed
        if (receivedItemId != null && receivedFinalPrice != null && receivedFinalPrice > 0 && receivedItemTitle != null) {
            layoutSecurePaymentSection.setVisibility(View.VISIBLE);
            tvSecurePaymentItemTitle.setText(receivedItemTitle);
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            currencyFormat.setMaximumFractionDigits(0);
            tvSecurePaymentAmount.setText(currencyFormat.format(receivedFinalPrice)); // Định dạng Long
            btnProceedToPaymentSecure.setText(String.format(Locale.getDefault(), getString(R.string.button_proceed_to_payment_secure_format), currencyFormat.format(receivedFinalPrice)));
        } else {
            layoutSecurePaymentSection.setVisibility(View.GONE);
            Log.d(TAG, "Hiding secure payment section as no specific item/transaction data was passed.");
        }
    }
}
