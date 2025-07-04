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
    private List<Transaction> allTransactions; // Keep these initialized
    private List<Transaction> currentDisplayedTransactions; // Keep these initialized

    private DatabaseReference transactionsRef;
    private ValueEventListener transactionsValueEventListener;

    private String currentUserId;
    private NavController navController;

    private String currentTab = "active"; // Default tab
    private String currentSortOrder = "newest_to_oldest"; // Default sort order

    public TransactionHistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Fragment created.");

        // Ensure current user is not null before accessing UID
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            Log.e(TAG, "No current user found. Cannot load transactions.");
            // Consider navigating to a login screen or showing an error state
            // if (navController != null) { // NavController might not be available yet in onCreate
            //     navController.navigate(R.id.action_global_loginFragment);
            // }
        }
        transactionsRef = FirebaseDatabase.getInstance().getReference("transactions");

        // Initialize lists here to ensure they are never null
        allTransactions = new ArrayList<>();
        currentDisplayedTransactions = new ArrayList<>();
        Log.d(TAG, "onCreate: allTransactions and currentDisplayedTransactions initialized.");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: View created.");
        return inflater.inflate(R.layout.fragment_transaction_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: View created and ready.");

        navController = Navigation.findNavController(view);
        Log.d(TAG, "onViewCreated: NavController initialized.");

        initViews(view);
        setupRecyclerView();
        setupTabLayout();
        setupListeners();
        fetchAllTransactions();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Fragment resumed. Re-fetching transactions.");
        // Re-fetch transactions to ensure data is up-to-date, especially after returning from another fragment
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
        if (!isAdded()) return;
        transactionAdapter = new TransactionAdapter(requireContext(), currentDisplayedTransactions, this);
        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTransactions.setAdapter(transactionAdapter);
        Log.d(TAG, "setupRecyclerView: Adapter set.");
    }

    private void setupTabLayout() {
        if (!isAdded()) return;
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_active)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_archived)));
        Log.d(TAG, "setupTabLayout: Tabs added.");

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    currentTab = "active";
                } else {
                    currentTab = "archived";
                }
                Log.d(TAG, "onTabSelected: Current tab changed to " + currentTab);
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
        ivBackButton.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigateUp();
                Log.d(TAG, "setupListeners: Back button clicked, navigating up.");
            } else {
                Log.e(TAG, "setupListeners: NavController is null, cannot navigate up.");
            }
        });
        ivSortButton.setOnClickListener(v -> {
            showSortOptionsDialog();
            Log.d(TAG, "setupListeners: Sort button clicked, showing dialog.");
        });
    }

    private void fetchAllTransactions() {
        if (currentUserId == null) {
            Log.e(TAG, "Current user ID is null. Cannot fetch transactions.");
            if (isAdded()) {
                Toast.makeText(requireContext(), getString(R.string.toast_error_no_user_id), Toast.LENGTH_LONG).show();
            }
            tvNoTransactions.setText(getString(R.string.no_transactions_message));
            tvNoTransactions.setVisibility(View.VISIBLE);
            rvTransactions.setVisibility(View.GONE);
            return;
        }

        // Remove previous listener to prevent duplicate calls if fetchAllTransactions is called multiple times
        if (transactionsRef != null && transactionsValueEventListener != null) {
            transactionsRef.removeEventListener(transactionsValueEventListener);
            Log.d(TAG, "fetchAllTransactions: Removed previous Firebase listener.");
        }

        transactionsValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) {
                    Log.w(TAG, "onDataChange: Fragment is not added, skipping UI updates.");
                    return;
                }

                // Ensure allTransactions is not null before clearing
                if (allTransactions == null) {
                    allTransactions = new ArrayList<>();
                    Log.w(TAG, "onDataChange: allTransactions was null, re-initialized.");
                }
                allTransactions.clear(); // Clear existing data before adding new
                Log.d(TAG, "onDataChange: allTransactions cleared.");

                for (DataSnapshot transactionSnapshot : snapshot.getChildren()) {
                    Transaction transaction = transactionSnapshot.getValue(Transaction.class);
                    if (transaction != null) {
                        transaction.setTransaction_id(transactionSnapshot.getKey());
                        if (currentUserId.equals(transaction.getBuyer_id()) || currentUserId.equals(transaction.getSeller_id())) {
                            allTransactions.add(transaction);
                        }
                    }
                }
                Log.d(TAG, "onDataChange: Fetched " + allTransactions.size() + " transactions for current user.");
                applySorting();
                filterAndDisplayTransactions();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) {
                    Log.w(TAG, "onCancelled: Fragment is not added, skipping error message.");
                    return;
                }
                Log.e(TAG, "Failed to load transactions: " + error.getMessage());
                Toast.makeText(requireContext(), getString(R.string.toast_error_loading_transactions, error.getMessage()), Toast.LENGTH_SHORT).show();
                tvNoTransactions.setText(getString(R.string.no_transactions_message));
                tvNoTransactions.setVisibility(View.VISIBLE);
                rvTransactions.setVisibility(View.GONE);
            }
        };

        transactionsRef.addValueEventListener(transactionsValueEventListener);
        Log.d(TAG, "fetchAllTransactions: Firebase listener added.");
    }

    private void filterAndDisplayTransactions() {
        // Ensure currentDisplayedTransactions is not null before clearing
        if (currentDisplayedTransactions == null) {
            currentDisplayedTransactions = new ArrayList<>();
            Log.w(TAG, "filterAndDisplayTransactions: currentDisplayedTransactions was null, re-initialized.");
        }
        currentDisplayedTransactions.clear();
        Log.d(TAG, "filterAndDisplayTransactions: currentDisplayedTransactions cleared.");

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
        Log.d(TAG, "filterAndDisplayTransactions: Filtered " + currentDisplayedTransactions.size() + " transactions for tab " + currentTab);


        if (currentDisplayedTransactions.isEmpty()) {
            tvNoTransactions.setVisibility(View.VISIBLE);
            rvTransactions.setVisibility(View.GONE);
            Log.d(TAG, "filterAndDisplayTransactions: No transactions, showing message.");
        } else {
            tvNoTransactions.setVisibility(View.GONE);
            rvTransactions.setVisibility(View.VISIBLE);
            Log.d(TAG, "filterAndDisplayTransactions: Transactions found, showing RecyclerView.");
        }
        transactionAdapter.setTransactions(currentDisplayedTransactions);
        Log.d(TAG, "filterAndDisplayTransactions: Adapter updated.");
    }

    private void applySorting() {
        // Ensure allTransactions is not null before sorting
        if (allTransactions == null) {
            Log.e(TAG, "applySorting: allTransactions is null, cannot sort.");
            return;
        }
        Log.d(TAG, "applySorting: Applying sort order: " + currentSortOrder);

        Collections.sort(allTransactions, (t1, t2) -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));

            try {
                Date date1 = sdf.parse(t1.getTransaction_date());
                Date date2 = sdf.parse(t2.getTransaction_date());

                if (date1 == null || date2 == null) {
                    return 0; // Cannot compare if dates are null
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
                Log.e(TAG, "Error parsing timestamp when sorting transaction: " + e.getMessage());
                return 0;
            }
        });
    }

    private void showSortOptionsDialog() {
        if (!isAdded()) {
            Log.w(TAG, "showSortOptionsDialog: Fragment not added to context, cannot show sort dialog.");
            return;
        }

        final CharSequence[] options = {
                getString(R.string.sort_newest_to_oldest),
                getString(R.string.sort_oldest_to_newest),
                getString(R.string.sort_price_low_to_high),
                getString(R.string.sort_price_high_to_low)
        };
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
        builder.setTitle(R.string.dialog_title_sort_transactions);
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
            Log.d(TAG, "showSortOptionsDialog: Sort option selected: " + currentSortOrder);
            applySorting();
            filterAndDisplayTransactions();
        });
        builder.setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    public void onTransactionClick(Transaction transaction) {
        if (isAdded()) {
            Log.d(TAG, "onTransactionClick: Transaction clicked: " + transaction.getTransaction_id());
            // Navigate to TransactionDetailFragment
            Bundle bundle = new Bundle();
            bundle.putString("transactionId", transaction.getTransaction_id());
            navController.navigate(R.id.action_transactionHistoryFragment_to_transactionDetailFragment, bundle);
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
                Toast.makeText(requireContext(), getString(R.string.toast_error_cannot_navigate_nav_controller_null), Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "onArrowClickForRating: Fragment is not added, cannot navigate.");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: Fragment view destroyed.");
        if (transactionsRef != null && transactionsValueEventListener != null) {
            transactionsRef.removeEventListener(transactionsValueEventListener);
            Log.d(TAG, "onDestroyView: Transaction history Firebase listener removed.");
        }
        // Nullify view references to prevent memory leaks
        tabLayout = null;
        rvTransactions = null;
        tvNoTransactions = null;
        ivBackButton = null;
        ivSortButton = null;
        // Do NOT nullify transactionAdapter, allTransactions, currentDisplayedTransactions here.
        // They are data/adapter objects and should persist across view lifecycle or be re-initialized in onCreate.
        // If you nullify them here, they might be null when onDataChange is triggered later.
        Log.d(TAG, "onDestroyView: View references nullified.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Fragment destroyed.");
        // If you want to completely clear data when fragment is truly destroyed (e.g., app killed)
        // you can do it here, but typically not needed for navigation.
        // allTransactions = null;
        // currentDisplayedTransactions = null;
    }
}
