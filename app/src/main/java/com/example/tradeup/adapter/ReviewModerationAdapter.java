package com.example.tradeup.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup.R;
import com.example.tradeup.model.Review;
import com.example.tradeup.model.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ReviewModerationAdapter extends RecyclerView.Adapter<ReviewModerationAdapter.ReviewModerationViewHolder> {

    private static final String TAG = "ReviewModerationAdapter";

    private Context context;
    private List<Review> reviewList;
    private OnModerationActionListener listener;

    // FIXED: Moved UserNamesCallback interface here, to the top-level class
    private interface UserNamesCallback {
        void onUserNamesFetched(String reviewerName, String revieweeName);
    }

    public interface OnModerationActionListener {
        void onApproveClick(Review review);
        void onRejectClick(Review review);
        // Có thể thêm onReviewClick(Review review) nếu bạn muốn click vào review để xem chi tiết
    }

    public ReviewModerationAdapter(Context context, List<Review> reviewList, OnModerationActionListener listener) {
        this.context = context;
        this.reviewList = reviewList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReviewModerationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_moderation_review, parent, false);
        return new ReviewModerationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewModerationViewHolder holder, int position) {
        Review review = reviewList.get(position);
        holder.bind(review);
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    public void setReviewList(List<Review> newReviewList) {
        this.reviewList = newReviewList;
        notifyDataSetChanged();
    }

    class ReviewModerationViewHolder extends RecyclerView.ViewHolder {
        TextView tvReviewerInfo;
        TextView tvReviewStatusTag;
        RatingBar ratingBar;
        TextView tvTimeAgo;
        TextView tvReviewComment;
        Button btnApproveReview;
        Button btnRejectReview;
        LinearLayout llModerationButtons; // Ánh xạ LinearLayout chứa các nút

        public ReviewModerationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReviewerInfo = itemView.findViewById(R.id.tv_reviewer_info);
            tvReviewStatusTag = itemView.findViewById(R.id.tv_review_status_tag);
            ratingBar = itemView.findViewById(R.id.rating_bar_moderation_item);
            tvTimeAgo = itemView.findViewById(R.id.tv_time_ago);
            tvReviewComment = itemView.findViewById(R.id.tv_review_comment);
            btnApproveReview = itemView.findViewById(R.id.btn_approve_review);
            btnRejectReview = itemView.findViewById(R.id.btn_reject_review);
            llModerationButtons = itemView.findViewById(R.id.ll_moderation_buttons); // Ánh xạ
        }

        public void bind(Review review) {
            fetchUserNames(review.getReviewer_id(), review.getReviewee_id(), new UserNamesCallback() {
                @Override
                public void onUserNamesFetched(String reviewerName, String revieweeName) {
                    if (reviewerName != null && revieweeName != null) {
                        tvReviewerInfo.setText(String.format("%s đã đánh giá %s", reviewerName, revieweeName));
                    } else {
                        tvReviewerInfo.setText("Người dùng không xác định");
                    }
                }
            });

            ratingBar.setRating(review.getRating() != null ? review.getRating() : 0);
            tvReviewComment.setText(review.getComment() != null && !review.getComment().isEmpty() ? review.getComment() : "Không có nhận xét");
            tvTimeAgo.setText(getTimeAgo(review.getCreated_at()));

            // Set status tag styling
            String status = review.getStatus();
            tvReviewStatusTag.setText(status);
            switch (status) {
                case "pending":
                    tvReviewStatusTag.setBackgroundResource(R.drawable.tag_pending);
                    tvReviewStatusTag.setTextColor(context.getResources().getColor(android.R.color.white));
                    llModerationButtons.setVisibility(View.VISIBLE); // Hiển thị nút nếu pending
                    break;
                case "approved":
                    tvReviewStatusTag.setBackgroundResource(R.drawable.tag_approved);
                    tvReviewStatusTag.setTextColor(context.getResources().getColor(android.R.color.white));
                    llModerationButtons.setVisibility(View.GONE); // Ẩn nút nếu đã approve
                    break;
                case "rejected":
                    tvReviewStatusTag.setBackgroundResource(R.drawable.tag_rejected);
                    tvReviewStatusTag.setTextColor(context.getResources().getColor(android.R.color.white));
                    llModerationButtons.setVisibility(View.GONE); // Ẩn nút nếu đã reject
                    break;
                default:
                    tvReviewStatusTag.setBackgroundResource(R.drawable.tag_default);
                    tvReviewStatusTag.setTextColor(context.getResources().getColor(android.R.color.black));
                    llModerationButtons.setVisibility(View.VISIBLE); // Mặc định hiển thị nút
                    break;
            }

            btnApproveReview.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onApproveClick(review);
                }
            });

            btnRejectReview.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRejectClick(review);
                }
            });
        }

        private void fetchUserNames(String reviewerId, String revieweeId, UserNamesCallback callback) {
            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
            final String[] reviewerName = {null};
            final String[] revieweeName = {null};

            usersRef.child(reviewerId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User reviewer = snapshot.getValue(User.class);
                    if (reviewer != null) {
                        reviewerName[0] = reviewer.getDisplay_name();
                    }
                    // Check if both names are fetched
                    if (reviewerName[0] != null && revieweeName[0] != null) {
                        callback.onUserNamesFetched(reviewerName[0], revieweeName[0]);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to fetch reviewer name: " + error.getMessage());
                }
            });

            usersRef.child(revieweeId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User reviewee = snapshot.getValue(User.class);
                    if (reviewee != null) {
                        revieweeName[0] = reviewee.getDisplay_name();
                    }
                    // Check if both names are fetched
                    if (reviewerName[0] != null && revieweeName[0] != null) {
                        callback.onUserNamesFetched(reviewerName[0], revieweeName[0]);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to fetch reviewee name: " + error.getMessage());
                }
            });
        }

        // REMOVED: private interface UserNamesCallback { ... } from here
        // It's now at the top-level class

        private String getTimeAgo(String dateTimeString) {
            if (dateTimeString == null || dateTimeString.isEmpty()) {
                return "N/A";
            }
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                Date date = sdf.parse(dateTimeString);
                if (date == null) return "N/A";

                long diff = System.currentTimeMillis() - date.getTime();

                long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
                long hours = TimeUnit.MILLISECONDS.toHours(diff);
                long days = TimeUnit.MILLISECONDS.toDays(diff);
                long weeks = days / 7;
                long months = days / 30;
                long years = days / 365;

                if (seconds < 60) {
                    return seconds + " giây trước";
                } else if (minutes < 60) {
                    return minutes + " phút trước";
                } else if (hours < 24) {
                    return hours + " giờ trước";
                } else if (days < 7) {
                    return days + " ngày trước";
                } else if (weeks < 4) { // Less than 4 weeks implies within a month
                    return weeks + " tuần trước";
                } else if (months < 12) {
                    return months + " tháng trước";
                } else {
                    return years + " năm trước";
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date for getTimeAgo: " + e.getMessage());
                return "N/A";
            }
        }
    }
}
