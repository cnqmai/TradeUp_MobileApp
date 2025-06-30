package com.example.tradeup.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup.R;
import com.example.tradeup.adapter.TransactionAdapter;
import com.example.tradeup.model.Transaction; // Your Transaction model
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class TransactionHistoryFragment extends Fragment implements
        TransactionAdapter.OnTransactionInteractionListener {

    private static final String TAG = "TransactionHistoryFragment";

    private TabLayout tabLayout;
    private RecyclerView rvTransactions;
    private TextView tvNoTransactions;
    private ImageView ivBackButton;
    private ImageView ivSortButton;

    private TransactionAdapter transactionAdapter;
    private List<Transaction> allTransactions;
    private List<Transaction> currentDisplayedTransactions;

    private DatabaseReference transactionsRef;
    private ValueEventListener transactionsValueEventListener;

    private String currentUserId;
    private NavController navController;

    private String currentTab = "active";
    private String currentSortOrder = "newest_to_oldest";

    public TransactionHistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        transactionsRef = FirebaseDatabase.getInstance().getReference("transactions");

        allTransactions = new ArrayList<>();
        currentDisplayedTransactions = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Đảm bảo NavController được khởi tạo ở đây
        navController = Navigation.findNavController(view);
        Log.d(TAG, "onViewCreated: NavController initialized.");

        initViews(view);
        setupRecyclerView();
        setupTabLayout();
        setupListeners();
        fetchAllTransactions();
    }

    private void initViews(View view) {
        tabLayout = view.findViewById(R.id.tab_layout_transaction_history);
        rvTransactions = view.findViewById(R.id.rv_transactions);
        tvNoTransactions = view.findViewById(R.id.tv_no_transactions);
        ivBackButton = view.findViewById(R.id.iv_back_button_transaction_history);
        ivSortButton = view.findViewById(R.id.iv_sort_button_transaction_history);
    }

    private void setupRecyclerView() {
        transactionAdapter = new TransactionAdapter(requireContext(), currentDisplayedTransactions, this);
        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTransactions.setAdapter(transactionAdapter);
    }

    private void setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab().setText("Active"));
        tabLayout.addTab(tabLayout.newTab().setText("Archived"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    currentTab = "active";
                } else {
                    currentTab = "archived";
                }
                filterAndDisplayTransactions();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Do nothing
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Do nothing
            }
        });
    }

    private void setupListeners() {
        ivBackButton.setOnClickListener(v -> navController.navigateUp());
        ivSortButton.setOnClickListener(v -> {
            showSortOptionsDialog();
        });
    }

    private void fetchAllTransactions() {
        if (transactionsRef != null && transactionsValueEventListener != null) {
            transactionsRef.removeEventListener(transactionsValueEventListener);
        }

        transactionsValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) {
                    Log.w(TAG, "Fragment is not added, skipping UI updates in onDataChange.");
                    return;
                }

                allTransactions.clear();
                for (DataSnapshot transactionSnapshot : snapshot.getChildren()) {
                    Transaction transaction = transactionSnapshot.getValue(Transaction.class);
                    if (transaction != null) {
                        transaction.setTransaction_id(transactionSnapshot.getKey());
                        if (currentUserId.equals(transaction.getBuyer_id()) || currentUserId.equals(transaction.getSeller_id())) {
                            allTransactions.add(transaction);
                        }
                    }
                }
                applySorting();
                filterAndDisplayTransactions();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) {
                    Log.w(TAG, "Fragment is not added, skipping error message in onCancelled.");
                    return;
                }
                Log.e(TAG, "Failed to load transactions: " + error.getMessage());
                Toast.makeText(requireContext(), "Lỗi khi tải giao dịch: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        transactionsRef.addValueEventListener(transactionsValueEventListener);
    }

    private void filterAndDisplayTransactions() {
        currentDisplayedTransactions.clear();
        for (Transaction transaction : allTransactions) {
            if (currentTab.equals("active")) {
                if (!transaction.isArchived()) {
                    currentDisplayedTransactions.add(transaction);
                }
            } else { // "archived"
                if (transaction.isArchived()) {
                    currentDisplayedTransactions.add(transaction);
                }
            }
        }

        if (currentDisplayedTransactions.isEmpty()) {
            tvNoTransactions.setVisibility(View.VISIBLE);
            rvTransactions.setVisibility(View.GONE);
        } else {
            tvNoTransactions.setVisibility(View.GONE);
            rvTransactions.setVisibility(View.VISIBLE);
        }
        transactionAdapter.setTransactions(currentDisplayedTransactions);
    }

    private void applySorting() {
        Collections.sort(allTransactions, (t1, t2) -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));

            try {
                Date date1 = sdf.parse(t1.getTransaction_date());
                Date date2 = sdf.parse(t2.getTransaction_date());

                if (date1 == null || date2 == null) {
                    return 0;
                }

                switch (currentSortOrder) {
                    case "newest_to_oldest":
                        return date2.compareTo(date1);
                    case "oldest_to_newest":
                        return date1.compareTo(date2);
                    case "price_low_to_high":
                        Long price1 = t1.getFinal_price() != null ? t1.getFinal_price() : 0L;
                        Long price2 = t2.getFinal_price() != null ? t2.getFinal_price() : 0L;
                        return price1.compareTo(price2);
                    case "price_high_to_low":
                        Long price1_h = t1.getFinal_price() != null ? t1.getFinal_price() : 0L;
                        Long price2_h = t2.getFinal_price() != null ? t2.getFinal_price() : 0L;
                        return price2_h.compareTo(price1_h);
                    default:
                        return date2.compareTo(date1);
                }
            } catch (ParseException e) {
                Log.e(TAG, "Lỗi parse timestamp khi sort transaction: " + e.getMessage());
                return 0;
            }
        });
    }

    private void showSortOptionsDialog() {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not added to context, cannot show sort dialog.");
            return;
        }

        final CharSequence[] options = {"Mới nhất đến cũ nhất", "Cũ nhất đến mới nhất", "Giá: Thấp đến cao", "Giá: Cao đến thấp"};
        int checkedItem = 0;

        switch (currentSortOrder) {
            case "oldest_to_newest":
                checkedItem = 1;
                break;
            case "price_low_to_high":
                checkedItem = 2;
                break;
            case "price_high_to_low":
                checkedItem = 3;
                break;
            default: // newest_to_oldest
                checkedItem = 0;
                break;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Sắp xếp giao dịch");
        builder.setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
            switch (which) {
                case 0:
                    currentSortOrder = "newest_to_oldest";
                    break;
                case 1:
                    currentSortOrder = "oldest_to_newest";
                    break;
                case 2:
                    currentSortOrder = "price_low_to_high";
                    break;
                case 3:
                    currentSortOrder = "price_high_to_low";
                    break;
            }
            dialog.dismiss();
            applySorting();
            filterAndDisplayTransactions();
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    public void onTransactionClick(Transaction transaction) {
        if (isAdded()) {
            Log.d(TAG, "onTransactionClick: Transaction clicked: " + transaction.getItem_id());
            Toast.makeText(requireContext(), "Đã nhấn vào chi tiết giao dịch: " + transaction.getItem_id(), Toast.LENGTH_SHORT).show();
            // Điều hướng đến màn hình chi tiết giao dịch ở đây nếu bạn có một Fragment cho nó
            // Ví dụ: Bundle bundle = new Bundle();
            // bundle.putString("transactionId", transaction.getTransaction_id());
            // navController.navigate(R.id.action_transactionHistoryFragment_to_transactionDetailFragment, bundle);
        }
    }

    @Override
    public void onArrowClickForRating(Transaction transaction, String reviewedUserId) {
        Log.d(TAG, "onArrowClickForRating: Arrow clicked for transaction: " + transaction.getTransaction_id() + ", reviewedUserId: " + reviewedUserId);
        if (isAdded()) {
            if (navController != null) {
                Bundle bundle = new Bundle();
                bundle.putString("transactionId", transaction.getTransaction_id());
                bundle.putString("itemId", transaction.getItem_id());
                bundle.putString("reviewedUserId", reviewedUserId);
                Log.d(TAG, "Navigating to RatingReviewFragment with bundle: " + bundle.toString());
                navController.navigate(R.id.action_transactionHistoryFragment_to_ratingReviewFragment, bundle);
            } else {
                Log.e(TAG, "onArrowClickForRating: NavController is null!");
                Toast.makeText(requireContext(), "Lỗi: Không thể điều hướng. NavController không khả dụng.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "onArrowClickForRating: Fragment is not added, cannot navigate.");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (transactionsRef != null && transactionsValueEventListener != null) {
            transactionsRef.removeEventListener(transactionsValueEventListener);
            Log.d(TAG, "Transaction history Firebase listener removed.");
        }
    }
}
