package com.example.tradeup.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup.R;
import com.example.tradeup.model.Payment;
import com.example.tradeup.model.Transaction;
import com.example.tradeup.model.Item;
import com.example.tradeup.model.User;
import com.example.tradeup.utils.FirebaseHelper;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentTransactionAdapter extends RecyclerView.Adapter<RecentTransactionAdapter.RecentTransactionViewHolder> {

    private static final String TAG = "RecentTransactionAdapter";
    private List<Payment> recentPaymentList;
    private Context context;
    private String currentUserId;
    private FirebaseHelper firebaseHelper;

    public RecentTransactionAdapter(Context context, List<Payment> recentPaymentList, String currentUserId) {
        this.context = context;
        this.recentPaymentList = recentPaymentList;
        this.currentUserId = currentUserId;
        this.firebaseHelper = new FirebaseHelper(context);
    }

    @NonNull
    @Override
    public RecentTransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_transaction, parent, false);
        return new RecentTransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentTransactionViewHolder holder, int position) {
        Payment payment = recentPaymentList.get(position);

        fetchTransactionDetails(payment, holder);

        // Format currency for display (e.g., $1,234.56 or 1.234,56 VNĐ)
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault()); // Use default locale for currency
        String formattedAmount = currencyFormat.format(payment.getAmount());

        // Set amount and color based on payer/payee
        if (currentUserId != null && currentUserId.equals(payment.getBuyer_id())) {
            holder.tvTransactionAmount.setText("-" + formattedAmount);
            holder.tvTransactionAmount.setTextColor(Color.RED);
        } else if (currentUserId != null && currentUserId.equals(payment.getSeller_id())) {
            holder.tvTransactionAmount.setText("+" + formattedAmount);
            holder.tvTransactionAmount.setTextColor(context.getResources().getColor(R.color.green_bold, null));
        } else {
            holder.tvTransactionAmount.setText(formattedAmount);
            holder.tvTransactionAmount.setTextColor(context.getResources().getColor(R.color.black, null));
        }

        // Set status icon and color
        if ("completed".equalsIgnoreCase(payment.getStatus())) {
            holder.ivTransactionStatusIcon.setImageResource(R.drawable.ic_check_circle);
        } else {
            holder.ivTransactionStatusIcon.setImageResource(R.drawable.ic_cancel_circle);
        }

        // Format and display transaction date
        try {
            SimpleDateFormat firebaseDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            Date date = firebaseDateFormat.parse(payment.getTimestamp());
            // Revert to English date format for display
            SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()); // Changed to English format
            holder.tvTransactionDate.setText(displayDateFormat.format(date));
        } catch (ParseException e) {
            holder.tvTransactionDate.setText("Date: N/A"); // English fallback
            Log.e(TAG, "Error parsing transaction date: " + e.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return recentPaymentList.size();
    }

    public void updateRecentTransactionList(List<Payment> newList) {
        this.recentPaymentList.clear();
        this.recentPaymentList.addAll(newList);
        notifyDataSetChanged();
    }

    private void fetchTransactionDetails(Payment payment, RecentTransactionViewHolder holder) {
        if (payment.getTransaction_id() != null) {
            firebaseHelper.getTransactionById(payment.getTransaction_id(), new FirebaseHelper.DbReadCallback<Transaction>() {
                @Override
                public void onSuccess(Transaction transaction) {
                    if (transaction != null && transaction.getItem_id() != null) {
                        firebaseHelper.getItem(transaction.getItem_id(), new FirebaseHelper.DbReadCallback<Item>() {
                            @Override
                            public void onSuccess(Item item) {
                                if (item != null) {
                                    holder.tvTransactionItemTitle.setText(item.getTitle());
                                } else {
                                    holder.tvTransactionItemTitle.setText("Item not found"); // English string
                                }
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                holder.tvTransactionItemTitle.setText("Error loading item name"); // English string
                                Log.e(TAG, "Failed to load item title: " + errorMessage);
                            }
                        });
                    } else {
                        holder.tvTransactionItemTitle.setText("Transaction not found"); // English string
                    }
                }

                @Override
                public void onFailure(String errorMessage) {
                    holder.tvTransactionItemTitle.setText("Error loading transaction"); // English string
                    Log.e(TAG, "Failed to load transaction details: " + errorMessage);
                }
            });
        } else {
            holder.tvTransactionItemTitle.setText("No Transaction ID"); // English string
        }

        String otherUserId = null;
        String relation = "";
        if (currentUserId != null) {
            if (currentUserId.equals(payment.getBuyer_id())) {
                otherUserId = payment.getSeller_id();
                relation = "Paid to "; // English string
            } else if (currentUserId != null && currentUserId.equals(payment.getSeller_id())) {
                otherUserId = payment.getBuyer_id();
                relation = "Received from "; // English string
            }
        }

        if (otherUserId != null) {
            String finalRelation = relation;
            firebaseHelper.getUserProfile(otherUserId, new FirebaseHelper.DbReadCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    if (user != null) {
                        holder.tvTransactionDescription.setText(finalRelation + user.getDisplay_name());
                    } else {
                        holder.tvTransactionDescription.setText(finalRelation + "Unknown User"); // English string
                    }
                }

                @Override
                public void onFailure(String errorMessage) {
                    holder.tvTransactionDescription.setText(finalRelation + "Error loading user"); // English string
                    Log.e(TAG, "Failed to load other user profile: " + errorMessage);
                }
            });
        } else {
            holder.tvTransactionDescription.setText("No user information"); // English string
        }
    }

    static class RecentTransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvTransactionItemTitle, tvTransactionDescription, tvTransactionDate, tvTransactionAmount;
        ImageView ivTransactionStatusIcon;

        public RecentTransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTransactionItemTitle = itemView.findViewById(R.id.tv_transaction_item_title);
            tvTransactionDescription = itemView.findViewById(R.id.tv_transaction_description);
            tvTransactionDate = itemView.findViewById(R.id.tv_transaction_date);
            tvTransactionAmount = itemView.findViewById(R.id.tv_transaction_amount);
            ivTransactionStatusIcon = itemView.findViewById(R.id.iv_transaction_status_icon);
        }
    }
}
