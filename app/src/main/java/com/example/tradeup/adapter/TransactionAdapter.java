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
        this.currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

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
        Transaction transaction = transactionList.get(position);
        holder.bind(transaction);
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
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
                        listener.onTransactionClick(transactionList.get(position));
                    }
                }
            });
        }

        public void bind(Transaction transaction) {
            if (transaction.getFinal_price() != null) {
                tvItemPrice.setText(currencyFormat.format(transaction.getFinal_price()) + " VNĐ");
            } else {
                tvItemPrice.setText("N/A");
            }

            if (transaction.getTransaction_date() != null) {
                try {
                    Date date = inputFormat.parse(transaction.getTransaction_date());
                    if (date != null) {
                        tvSoldDate.setText("Sold on " + outputFormat.format(date));
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "Failed to parse transaction date: " + transaction.getTransaction_date(), e);
                    tvSoldDate.setText("Sold on N/A");
                }
            } else {
                tvSoldDate.setText("Sold on N/A");
            }

            // Hiển thị trạng thái dựa trên isArchived
            if (transaction.isArchived()) {
                tvStatusSold.setText("Completed");
                tvStatusSold.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
            } else {
                tvStatusSold.setText("Active");
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
                            tvItemTitle.setText("Sản phẩm không tồn tại");
                            ivItemImage.setImageResource(R.drawable.img_placeholder);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load item details for transaction: " + error.getMessage());
                        tvItemTitle.setText("Lỗi tải sản phẩm");
                        ivItemImage.setImageResource(R.drawable.img_error);
                    }
                });
            } else {
                tvItemTitle.setText("ID Sản phẩm không hợp lệ");
                ivItemImage.setImageResource(R.drawable.img_placeholder);
            }

            // Logic xử lý khi nhấn vào mũi tên
            ivArrowRight.setOnClickListener(v -> {
                if (listener != null) {
                    // Kiểm tra xem giao dịch đã được archived (hoàn tất) chưa
                    if (transaction.isArchived()) {
                        // Xác định người cần được đánh giá
                        String reviewedUserId;
                        if (currentUserId.equals(transaction.getBuyer_id())) {
                            reviewedUserId = transaction.getSeller_id();
                        } else if (currentUserId.equals(transaction.getSeller_id())) {
                            reviewedUserId = transaction.getBuyer_id();
                        } else {
                            Log.e(TAG, "Current user is neither buyer nor seller for transaction: " + transaction.getTransaction_id());
                            Toast.makeText(context, "Không thể đánh giá giao dịch này.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // LUÔN LUÔN điều hướng đến màn hình đánh giá nếu giao dịch đã archived
                        listener.onArrowClickForRating(transaction, reviewedUserId);
                    } else {
                        // Nếu giao dịch chưa hoàn tất (chưa archived), thông báo và điều hướng đến màn hình chi tiết giao dịch
                        Toast.makeText(context, "Giao dịch chưa hoàn tất. Không thể đánh giá.", Toast.LENGTH_SHORT).show();
                        listener.onTransactionClick(transaction); // Vẫn cho phép xem chi tiết
                    }
                }
            });
        }
    }

    private void checkIfAlreadyReviewed(Transaction transaction, String reviewedUserId, OnReviewedCheckCallback callback) {
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
                callback.onResult(true);
            }
        });
    }
}
