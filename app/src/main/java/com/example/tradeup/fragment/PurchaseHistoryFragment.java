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
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradeup.R;
import com.example.tradeup.model.Item; // Import Item model
import com.example.tradeup.model.Transaction; // Import Transaction model
import com.example.tradeup.utils.FirebaseHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class PurchaseHistoryFragment extends Fragment {

    private static final String TAG = "PurchaseHistoryFragment";

    private RecyclerView recyclerView;
    private PurchaseHistoryAdapter adapter;
    private TextView textNoPurchaseHistory;
    private ImageView ivBackButton;

    private NavController navController;
    private FirebaseHelper firebaseHelper;
    private List<Transaction> purchaseTransactions; // List to hold transaction objects

    public PurchaseHistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseHelper = new FirebaseHelper(requireContext());
        purchaseTransactions = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_purchase_history, container, false);
        initViews(view);
        setupListeners();
        setupRecyclerView();
        loadPurchaseHistory();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_purchase_history);
        textNoPurchaseHistory = view.findViewById(R.id.text_no_purchase_history);
        ivBackButton = view.findViewById(R.id.iv_back_button_purchase_history);
    }

    private void setupListeners() {
        ivBackButton.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigateUp();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new PurchaseHistoryAdapter(purchaseTransactions, new PurchaseHistoryAdapter.OnTransactionClickListener() {
            @Override
            public void onTransactionClick(Transaction transaction) {
                // Navigate to ItemDetailFragment or TransactionDetailFragment
                Toast.makeText(getContext(), "Transaction Clicked for Item: " + transaction.getItem_id(), Toast.LENGTH_SHORT).show();
                if (navController != null && transaction.getItem_id() != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("itemId", transaction.getItem_id());
                    // Assuming action_purchaseHistoryFragment_to_itemDetailFragment exists in nav_graph
                    navController.navigate(R.id.action_purchaseHistoryFragment_to_itemDetailFragment, bundle);
                }
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadPurchaseHistory() {
        String currentUserId = firebaseHelper.getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Người dùng chưa đăng nhập.", Toast.LENGTH_SHORT).show();
            updateEmptyState();
            return;
        }

        // 1. Get transaction IDs from purchase_history for current user
        DatabaseReference purchaseHistoryRef = FirebaseDatabase.getInstance().getReference("purchase_history").child(currentUserId);
        purchaseHistoryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded()) {
                    purchaseTransactions.clear();
                    List<String> transactionIds = new ArrayList<>();
                    for (DataSnapshot idSnapshot : snapshot.getChildren()) {
                        transactionIds.add(idSnapshot.getKey()); // Get transaction ID from key
                    }
                    Log.d(TAG, "Found " + transactionIds.size() + " purchase history entries.");

                    if (transactionIds.isEmpty()) {
                        updateEmptyState();
                        return;
                    }

                    // 2. Fetch details for each transaction ID from 'transactions' node
                    DatabaseReference transactionsRef = FirebaseDatabase.getInstance().getReference("transactions");
                    // Use a counter to know when all offers have been fetched
                    final int[] transactionsFetchedCount = {0};
                    for (String transactionId : transactionIds) {
                        transactionsRef.child(transactionId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot transactionSnapshot) {
                                if (isAdded()) {
                                    Transaction transaction = transactionSnapshot.getValue(Transaction.class);
                                    if (transaction != null) {
                                        // Set the transaction ID from the key
                                        transaction.setTransaction_id(transactionSnapshot.getKey()); // Use setTransaction_id()
                                        // Ensure this transaction is for the current buyer
                                        if (Objects.equals(transaction.getBuyer_id(), currentUserId)) {
                                            purchaseTransactions.add(transaction);
                                        }
                                    }
                                    transactionsFetchedCount[0]++;
                                    // Check if all transactions have been fetched
                                    if (transactionsFetchedCount[0] == transactionIds.size()) {
                                        adapter.notifyDataSetChanged();
                                        updateEmptyState();
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "Failed to load transaction details for ID " + transactionId + ": " + error.getMessage());
                                if (isAdded()) {
                                    Toast.makeText(getContext(), "Lỗi khi tải chi tiết giao dịch: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                                transactionsFetchedCount[0]++;
                                if (transactionsFetchedCount[0] == transactionIds.size()) {
                                    adapter.notifyDataSetChanged();
                                    updateEmptyState();
                                }
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load purchase history IDs: " + error.getMessage());
                if (isAdded()) {
                    Toast.makeText(getContext(), "Lỗi khi tải lịch sử mua hàng: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                }
            }
        });
    }

    private void updateEmptyState() {
        if (isAdded()) {
            if (purchaseTransactions.isEmpty()) {
                textNoPurchaseHistory.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                textNoPurchaseHistory.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    // Adapter for the RecyclerView
    private static class PurchaseHistoryAdapter extends RecyclerView.Adapter<PurchaseHistoryAdapter.PurchaseViewHolder> {

        private List<Transaction> transactions;
        private OnTransactionClickListener listener;
        private DatabaseReference itemsRef; // Reference to items node to get item details

        public interface OnTransactionClickListener {
            void onTransactionClick(Transaction transaction);
        }

        public PurchaseHistoryAdapter(List<Transaction> transactions, OnTransactionClickListener listener) {
            this.transactions = transactions;
            this.listener = listener;
            this.itemsRef = FirebaseDatabase.getInstance().getReference("items");
        }

        @NonNull
        @Override
        public PurchaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_purchase_history, parent, false);
            return new PurchaseViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PurchaseViewHolder holder, int position) {
            Transaction transaction = transactions.get(position);
            holder.bind(transaction, listener, itemsRef);
        }

        @Override
        public int getItemCount() {
            return transactions.size();
        }

        static class PurchaseViewHolder extends RecyclerView.ViewHolder {
            ImageView ivPurchaseItemThumbnail;
            TextView tvPurchaseItemTitle, tvPurchaseFinalPrice, tvPurchaseDate;
            ImageView ivViewPurchaseDetails;

            public PurchaseViewHolder(@NonNull View itemView) {
                super(itemView);
                ivPurchaseItemThumbnail = itemView.findViewById(R.id.iv_purchase_item_thumbnail);
                tvPurchaseItemTitle = itemView.findViewById(R.id.tv_purchase_item_title);
                tvPurchaseFinalPrice = itemView.findViewById(R.id.tv_purchase_final_price);
                tvPurchaseDate = itemView.findViewById(R.id.tv_purchase_date);
                ivViewPurchaseDetails = itemView.findViewById(R.id.iv_view_purchase_details);
            }

            public void bind(final Transaction transaction, final OnTransactionClickListener listener, DatabaseReference itemsRef) {
                // Display final price
                NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                currencyFormatter.setMaximumFractionDigits(0);
                tvPurchaseFinalPrice.setText("Giá cuối cùng: " + currencyFormatter.format(transaction.getFinal_price()) + " VNĐ");

                // Format transaction date
                long timestampMillis = 0;
                try {
                    // Assuming transaction_date is in ISO 8601 format like "yyyy-MM-dd'T'HH:mm:ss'Z'"
                    timestampMillis = OffsetDateTime.parse(transaction.getTransaction_date()).toInstant().toEpochMilli();
                } catch (DateTimeParseException e) {
                    Log.e(TAG, "Error parsing transaction_date string: " + transaction.getTransaction_date(), e);
                    timestampMillis = System.currentTimeMillis(); // Fallback
                }
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                String formattedDate = sdf.format(new Date(timestampMillis));
                tvPurchaseDate.setText("Ngày mua: " + formattedDate);

                // Load item details (title and thumbnail)
                if (transaction.getItem_id() != null) {
                    itemsRef.child(transaction.getItem_id()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Item item = snapshot.getValue(Item.class);
                            if (item != null) {
                                tvPurchaseItemTitle.setText(item.getTitle());
                                if (item.getPhotos() != null && !item.getPhotos().isEmpty()) {
                                    Glide.with(itemView.getContext())
                                            .load(item.getPhotos().get(0))
                                            .placeholder(R.drawable.img_placeholder)
                                            .error(R.drawable.img_error)
                                            .into(ivPurchaseItemThumbnail);
                                } else {
                                    ivPurchaseItemThumbnail.setImageResource(R.drawable.img_placeholder);
                                }
                            } else {
                                tvPurchaseItemTitle.setText("Tin đăng không tìm thấy");
                                ivPurchaseItemThumbnail.setImageResource(R.drawable.img_placeholder);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Failed to load item for transaction " + transaction.getTransaction_id() + ": " + error.getMessage()); // Use getTransaction_id()
                            tvPurchaseItemTitle.setText("Lỗi tải tin đăng");
                            ivPurchaseItemThumbnail.setImageResource(R.drawable.img_error);
                        }
                    });
                } else {
                    tvPurchaseItemTitle.setText("Không có ID tin đăng");
                    ivPurchaseItemThumbnail.setImageResource(R.drawable.img_placeholder);
                }

                itemView.setOnClickListener(v -> listener.onTransactionClick(transaction));
            }
        }
    }
}
