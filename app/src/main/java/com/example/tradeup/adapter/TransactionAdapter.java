package com.example.tradeup.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradeup.R;
import com.example.tradeup.model.Item;
import com.example.tradeup.model.Review;
import com.example.tradeup.model.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.Query;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private static final String TAG = "TransactionAdapter";
    private Context context;
    private List<Transaction> transactionList;
    private OnTransactionInteractionListener listener;
    private SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
    private SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd,yyyy", Locale.getDefault());
    private DecimalFormat currencyFormat;
    private String currentUserId;

    public interface OnTransactionInteractionListener {
        void onTransactionClick(Transaction transaction);
        void onArrowClickForRating(Transaction transaction, String reviewedUserId);
    }

    private interface OnReviewedCheckCallback {
        void onResult(boolean alreadyReviewed);
    }

    public TransactionAdapter(Context context, List<Transaction> transactionList, OnTransactionInteractionListener listener) {
        this.context = context;
        this.transactionList = transactionList;
        this.listener = listener;
        // Ensure currentUserId is initialized safely
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            this.currentUserId = null; // Handle case where user is not logged in
            Log.e(TAG, "Current user is null in TransactionAdapter constructor.");
        }


        inputFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        currencyFormat = new DecimalFormat("#,###");
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        // FIX: Add null check for transactionList before accessing elements
        if (transactionList == null || position >= transactionList.size()) {
            Log.e(TAG, "transactionList is null or position is out of bounds in onBindViewHolder.");
            return;
        }
        Transaction transaction = transactionList.get(position);
        holder.bind(transaction);
    }

    @Override
    public int getItemCount() {
        // FIX: Ensure transactionList is not null before calling size()
        return transactionList != null ? transactionList.size() : 0;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactionList = transactions;
        notifyDataSetChanged();
    }

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        ImageView ivItemImage;
        TextView tvItemTitle, tvItemPrice, tvSoldDate, tvStatusSold;
        ImageView ivArrowRight;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemImage = itemView.findViewById(R.id.iv_item_image);
            tvItemTitle = itemView.findViewById(R.id.tv_item_title);
            tvItemPrice = itemView.findViewById(R.id.tv_item_price);
            tvSoldDate = itemView.findViewById(R.id.tv_sold_date);
            tvStatusSold = itemView.findViewById(R.id.tv_status_sold);
            ivArrowRight = itemView.findViewById(R.id.iv_arrow_right);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        // FIX: Add null check for transactionList here as well
                        if (transactionList != null && position < transactionList.size()) {
                            listener.onTransactionClick(transactionList.get(position));
                        } else {
                            Log.e(TAG, "transactionList is null or position is out of bounds on item click.");
                        }
                    }
                }
            });
        }

        public void bind(Transaction transaction) {
            if (transaction.getFinal_price() != null) {
                tvItemPrice.setText(currencyFormat.format(transaction.getFinal_price()) + " VNĐ"); // Keep VNĐ for currency
            } else {
                tvItemPrice.setText(context.getString(R.string.price_not_available));
            }

            if (transaction.getTransaction_date() != null) {
                try {
                    Date date = inputFormat.parse(transaction.getTransaction_date());
                    if (date != null) {
                        tvSoldDate.setText(context.getString(R.string.sold_on_date, outputFormat.format(date)));
                    } else {
                        tvSoldDate.setText(context.getString(R.string.date_not_available));
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "Failed to parse transaction date: " + transaction.getTransaction_date(), e);
                    tvSoldDate.setText(context.getString(R.string.date_not_available));
                }
            } else {
                tvSoldDate.setText(context.getString(R.string.date_not_available));
            }

            // Display status based on isArchived
            if (transaction.isArchived()) {
                tvStatusSold.setText(context.getString(R.string.status_completed));
                tvStatusSold.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
            } else {
                tvStatusSold.setText(context.getString(R.string.status_active));
                tvStatusSold.setTextColor(context.getResources().getColor(android.R.color.holo_orange_dark));
            }


            if (transaction.getItem_id() != null) {
                DatabaseReference itemRef = FirebaseDatabase.getInstance().getReference("items").child(transaction.getItem_id());
                itemRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Item item = snapshot.getValue(Item.class);
                        if (item != null) {
                            tvItemTitle.setText(item.getTitle());
                            if (item.getPhotos() != null && !item.getPhotos().isEmpty()) {
                                Glide.with(context)
                                        .load(item.getPhotos().get(0))
                                        .placeholder(R.drawable.img_placeholder)
                                        .error(R.drawable.img_error)
                                        .into(ivItemImage);
                            } else {
                                ivItemImage.setImageResource(R.drawable.img_placeholder);
                            }
                        } else {
                            tvItemTitle.setText(context.getString(R.string.item_not_found));
                            ivItemImage.setImageResource(R.drawable.img_placeholder);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load item details for transaction: " + error.getMessage());
                        tvItemTitle.setText(context.getString(R.string.error_loading_item));
                        ivItemImage.setImageResource(R.drawable.img_error);
                    }
                });
            } else {
                tvItemTitle.setText(context.getString(R.string.invalid_item_id));
                ivItemImage.setImageResource(R.drawable.img_placeholder);
            }

            // Logic to handle arrow click for rating or transaction details
            ivArrowRight.setOnClickListener(v -> {
                if (listener != null) {
                    // Check if the transaction is archived (completed)
                    if (transaction.isArchived()) {
                        // Determine who needs to be reviewed
                        String reviewedUserId = null;
                        if (currentUserId != null) { // Ensure currentUserId is not null
                            if (currentUserId.equals(transaction.getBuyer_id())) {
                                reviewedUserId = transaction.getSeller_id();
                            } else if (currentUserId.equals(transaction.getSeller_id())) {
                                reviewedUserId = transaction.getBuyer_id();
                            }
                        }

                        if (reviewedUserId != null) {
                            // Always navigate to the rating screen if transaction is archived and reviewedUserId is valid
                            listener.onArrowClickForRating(transaction, reviewedUserId);
                        } else {
                            Log.e(TAG, "Reviewed user ID is null for transaction: " + transaction.getTransaction_id());
                            Toast.makeText(context, context.getString(R.string.toast_cannot_rate_transaction), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // If the transaction is not completed (not archived), inform and navigate to transaction details
                        Toast.makeText(context, context.getString(R.string.toast_transaction_not_completed_cannot_rate), Toast.LENGTH_SHORT).show();
                        listener.onTransactionClick(transaction); // Still allow viewing details
                    }
                }
            });
        }
    }

    private void checkIfAlreadyReviewed(Transaction transaction, String reviewedUserId, OnReviewedCheckCallback callback) {
        // FIX: Add null check for currentUserId
        if (currentUserId == null) {
            Log.e(TAG, "Current user ID is null, cannot check if reviewed.");
            callback.onResult(true); // Assume reviewed to prevent further action
            return;
        }

        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("reviews");
        Query query = reviewsRef.orderByChild("reviewer_id").equalTo(currentUserId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean alreadyReviewed = false;
                for (DataSnapshot reviewSnap : snapshot.getChildren()) {
                    Review review = reviewSnap.getValue(Review.class);
                    if (review != null && Objects.equals(review.getTransaction_id(), transaction.getTransaction_id()) &&
                            Objects.equals(review.getReviewee_id(), reviewedUserId)) {
                        alreadyReviewed = true;
                        break;
                    }
                }
                callback.onResult(alreadyReviewed);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to check if reviewed: " + error.getMessage());
                callback.onResult(true); // Assume reviewed on error to prevent issues
            }
        });
    }
}
