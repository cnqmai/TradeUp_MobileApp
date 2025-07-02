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

public class SalesHistoryFragment extends Fragment {

    private static final String TAG = "SalesHistoryFragment";

    private RecyclerView recyclerView;
    private SalesHistoryAdapter adapter;
    private TextView textNoSalesHistory;
    private ImageView ivBackButton;

    private NavController navController;
    private FirebaseHelper firebaseHelper;
    private List<Transaction> salesTransactions; // List to hold transaction objects

    public SalesHistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseHelper = new FirebaseHelper(requireContext());
        salesTransactions = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sales_history, container, false);
        initViews(view);
        setupListeners();
        setupRecyclerView();
        loadSalesHistory();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_sales_history);
        textNoSalesHistory = view.findViewById(R.id.text_no_sales_history);
        ivBackButton = view.findViewById(R.id.iv_back_button_sales_history);
    }

    private void setupListeners() {
        ivBackButton.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigateUp();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new SalesHistoryAdapter(salesTransactions, new SalesHistoryAdapter.OnTransactionClickListener() {
            @Override
            public void onTransactionClick(Transaction transaction) {
                // Navigate to ItemDetailFragment or TransactionDetailFragment
                Toast.makeText(getContext(), "Transaction Clicked for Item: " + transaction.getItem_id(), Toast.LENGTH_SHORT).show();
                if (navController != null && transaction.getItem_id() != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("itemId", transaction.getItem_id());
                    // Assuming action_salesHistoryFragment_to_itemDetailFragment exists in nav_graph
                    navController.navigate(R.id.action_salesHistoryFragment_to_itemDetailFragment, bundle);
                }
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadSalesHistory() {
        String currentUserId = firebaseHelper.getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Người dùng chưa đăng nhập.", Toast.LENGTH_SHORT).show();
            updateEmptyState();
            return;
        }

        // 1. Get transaction IDs from sales_history for current user
        DatabaseReference salesHistoryRef = FirebaseDatabase.getInstance().getReference("sales_history").child(currentUserId);
        salesHistoryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded()) {
                    salesTransactions.clear();
                    List<String> transactionIds = new ArrayList<>();
                    for (DataSnapshot idSnapshot : snapshot.getChildren()) {
                        transactionIds.add(idSnapshot.getKey()); // Get transaction ID from key
                    }
                    Log.d(TAG, "Found " + transactionIds.size() + " sales history entries.");

                    if (transactionIds.isEmpty()) {
                        updateEmptyState();
                        return;
                    }

                    // 2. Fetch details for each transaction ID from 'transactions' node
                    DatabaseReference transactionsRef = FirebaseDatabase.getInstance().getReference("transactions");
                    for (String transactionId : transactionIds) {
                        transactionsRef.child(transactionId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot transactionSnapshot) {
                                if (isAdded()) {
                                    Transaction transaction = transactionSnapshot.getValue(Transaction.class);
                                    if (transaction != null) {
                                        // Set the transaction ID from the key
                                        transaction.setTransaction_id(transactionSnapshot.getKey());
                                        // Ensure this transaction is for the current seller
                                        if (Objects.equals(transaction.getSeller_id(), currentUserId)) {
                                            salesTransactions.add(transaction);
                                            adapter.notifyDataSetChanged(); // Notify after each item is added (can be optimized)
                                        }
                                    }
                                    updateEmptyState(); // Update after all transactions are potentially loaded
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "Failed to load transaction details for ID " + transactionId + ": " + error.getMessage());
                                if (isAdded()) {
                                    Toast.makeText(getContext(), "Lỗi khi tải chi tiết giao dịch: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                    updateEmptyState();
                                }
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load sales history IDs: " + error.getMessage());
                if (isAdded()) {
                    Toast.makeText(getContext(), "Lỗi khi tải lịch sử bán hàng: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                }
            }
        });
    }

    private void updateEmptyState() {
        if (isAdded()) {
            if (salesTransactions.isEmpty()) {
                textNoSalesHistory.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                textNoSalesHistory.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    // Adapter for the RecyclerView
    private static class SalesHistoryAdapter extends RecyclerView.Adapter<SalesHistoryAdapter.SalesViewHolder> {

        private List<Transaction> transactions;
        private OnTransactionClickListener listener;
        private DatabaseReference itemsRef; // Reference to items node to get item details

        public interface OnTransactionClickListener {
            void onTransactionClick(Transaction transaction);
        }

        public SalesHistoryAdapter(List<Transaction> transactions, OnTransactionClickListener listener) {
            this.transactions = transactions;
            this.listener = listener;
            this.itemsRef = FirebaseDatabase.getInstance().getReference("items");
        }

        @NonNull
        @Override
        public SalesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sales_history, parent, false);
            return new SalesViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SalesViewHolder holder, int position) {
            Transaction transaction = transactions.get(position);
            holder.bind(transaction, listener, itemsRef);
        }

        @Override
        public int getItemCount() {
            return transactions.size();
        }

        static class SalesViewHolder extends RecyclerView.ViewHolder {
            ImageView ivItemThumbnail;
            TextView tvItemTitle, tvFinalPrice, tvTransactionDate;
            ImageView ivViewDetails;

            public SalesViewHolder(@NonNull View itemView) {
                super(itemView);
                ivItemThumbnail = itemView.findViewById(R.id.iv_item_thumbnail_sales);
                tvItemTitle = itemView.findViewById(R.id.tv_item_title_sales);
                tvFinalPrice = itemView.findViewById(R.id.tv_final_price_sales);
                tvTransactionDate = itemView.findViewById(R.id.tv_transaction_date_sales);
                ivViewDetails = itemView.findViewById(R.id.iv_view_details_sales);
            }

            public void bind(final Transaction transaction, final OnTransactionClickListener listener, DatabaseReference itemsRef) {
                // Display final price
                NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                currencyFormatter.setMaximumFractionDigits(0);
                tvFinalPrice.setText("Giá cuối cùng: " + currencyFormatter.format(transaction.getFinal_price()) + " VNĐ");

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
                tvTransactionDate.setText("Ngày giao dịch: " + formattedDate);

                // Load item details (title and thumbnail)
                if (transaction.getItem_id() != null) {
                    itemsRef.child(transaction.getItem_id()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Item item = snapshot.getValue(Item.class);
                            if (item != null) {
                                tvItemTitle.setText(item.getTitle());
                                if (item.getPhotos() != null && !item.getPhotos().isEmpty()) {
                                    Glide.with(itemView.getContext())
                                            .load(item.getPhotos().get(0))
                                            .placeholder(R.drawable.img_placeholder)
                                            .error(R.drawable.img_error)
                                            .into(ivItemThumbnail);
                                } else {
                                    ivItemThumbnail.setImageResource(R.drawable.img_placeholder);
                                }
                            } else {
                                tvItemTitle.setText("Tin đăng không tìm thấy");
                                ivItemThumbnail.setImageResource(R.drawable.img_placeholder);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Failed to load item for transaction " + transaction.getTransaction_id() + ": " + error.getMessage());
                            tvItemTitle.setText("Lỗi tải tin đăng");
                            ivItemThumbnail.setImageResource(R.drawable.img_error);
                        }
                    });
                } else {
                    tvItemTitle.setText("Không có ID tin đăng");
                    ivItemThumbnail.setImageResource(R.drawable.img_placeholder);
                }

                itemView.setOnClickListener(v -> listener.onTransactionClick(transaction));
            }
        }
    }
}
