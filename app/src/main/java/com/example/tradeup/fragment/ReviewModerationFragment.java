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
import com.example.tradeup.adapter.ReviewModerationAdapter;
import com.example.tradeup.model.Review;
import com.example.tradeup.model.User;
import com.example.tradeup.utils.FirebaseHelper; // Ensure this is imported and has updateReviewStatus method
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Comparator;

public class ReviewModerationFragment extends Fragment implements ReviewModerationAdapter.OnModerationActionListener {

    private static final String TAG = "ReviewModerationFrag";

    private ImageView ivBackButton, ivFilterButton;
    private TextView tvTotalReviews, tvPendingReviews, tvApprovedReviews, tvRejectedReviews;
    private RecyclerView rvReviewModeration;
    private TextView tvNoReviewsModeration;

    private ReviewModerationAdapter adapter;
    private List<Review> allReviews;
    private List<Review> displayedReviews;

    private DatabaseReference reviewsRef;
    private DatabaseReference usersRef;
    private FirebaseHelper firebaseHelper; // Instance of FirebaseHelper
    private String currentFilter = "all"; // "all", "pending", "approved", "rejected"

    public ReviewModerationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reviewsRef = FirebaseDatabase.getInstance().getReference("reviews");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        firebaseHelper = new FirebaseHelper(); // Initialize FirebaseHelper
        allReviews = new ArrayList<>();
        displayedReviews = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_review_moderation, container, false);
        initViews(view);
        setupRecyclerView();
        setupListeners();
        fetchReviews(); // Start fetching reviews
        return view;
    }

    private void initViews(View view) {
        ivBackButton = view.findViewById(R.id.iv_back_button_moderation);
        ivFilterButton = view.findViewById(R.id.iv_filter_button_moderation);
        tvTotalReviews = view.findViewById(R.id.tv_total_reviews);
        tvPendingReviews = view.findViewById(R.id.tv_pending_reviews);
        tvApprovedReviews = view.findViewById(R.id.tv_approved_reviews);
        tvRejectedReviews = view.findViewById(R.id.tv_rejected_reviews);
        rvReviewModeration = view.findViewById(R.id.rv_review_moderation);
        tvNoReviewsModeration = view.findViewById(R.id.tv_no_reviews_moderation);
    }

    private void setupRecyclerView() {
        adapter = new ReviewModerationAdapter(requireContext(), displayedReviews, this);
        rvReviewModeration.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvReviewModeration.setAdapter(adapter);
    }

    private void setupListeners() {
        ivBackButton.setOnClickListener(v -> {
            if (isAdded()) {
                Navigation.findNavController(v).navigateUp();
            }
        });

        ivFilterButton.setOnClickListener(v -> showFilterDialog());
    }

    private void fetchReviews() {
        reviewsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return; // Avoid updating UI if fragment is detached

                allReviews.clear();
                long total = 0, pending = 0, approved = 0, rejected = 0;

                for (DataSnapshot reviewSnapshot : snapshot.getChildren()) {
                    Review review = reviewSnapshot.getValue(Review.class);
                    if (review != null) {
                        review.setReview_id(reviewSnapshot.getKey()); // Set the review ID
                        allReviews.add(review);
                        total++;

                        switch (Objects.requireNonNull(review.getStatus())) {
                            case "pending":
                                pending++;
                                break;
                            case "approved":
                                approved++;
                                break;
                            case "rejected":
                                rejected++;
                                break;
                        }
                    }
                }
                updateStats(total, pending, approved, rejected);
                applyFilter();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load reviews: " + error.getMessage());
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Lỗi khi tải đánh giá: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateStats(long total, long pending, long approved, long rejected) {
        tvTotalReviews.setText(String.valueOf(total));
        tvPendingReviews.setText(String.valueOf(pending));
        tvApprovedReviews.setText(String.valueOf(approved));
        tvRejectedReviews.setText(String.valueOf(rejected));
    }

    private void applyFilter() {
        displayedReviews.clear();
        for (Review review : allReviews) {
            if (currentFilter.equals("all") || review.getStatus().equals(currentFilter)) {
                displayedReviews.add(review);
            }
        }
        // Sort reviews by creation date (newest first)
        Collections.sort(displayedReviews, (r1, r2) -> {
            if (r1.getCreated_at() == null || r2.getCreated_at() == null) {
                return 0;
            }
            return r2.getCreated_at().compareTo(r1.getCreated_at());
        });

        adapter.setReviewList(displayedReviews);
        if (displayedReviews.isEmpty()) {
            tvNoReviewsModeration.setVisibility(View.VISIBLE);
            rvReviewModeration.setVisibility(View.GONE);
        } else {
            tvNoReviewsModeration.setVisibility(View.GONE);
            rvReviewModeration.setVisibility(View.VISIBLE);
        }
    }

    private void showFilterDialog() {
        if (!isAdded()) return;

        final CharSequence[] options = {"Tất cả", "Chờ duyệt", "Đã duyệt", "Từ chối"};
        int checkedItem = 0;
        switch (currentFilter) {
            case "all": checkedItem = 0; break;
            case "pending": checkedItem = 1; break;
            case "approved": checkedItem = 2; break;
            case "rejected": checkedItem = 3; break;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Lọc đánh giá");
        builder.setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
            switch (which) {
                case 0: currentFilter = "all"; break;
                case 1: currentFilter = "pending"; break;
                case 2: currentFilter = "approved"; break;
                case 3: currentFilter = "rejected"; break;
            }
            dialog.dismiss();
            applyFilter();
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    public void onApproveClick(Review review) {
        if (!isAdded()) return;
        updateReviewStatus(review, "approved");
    }

    @Override
    public void onRejectClick(Review review) {
        if (!isAdded()) return;
        updateReviewStatus(review, "rejected");
    }

    private void updateReviewStatus(Review review, String newStatus) {
        if (review.getReview_id() == null) {
            Toast.makeText(requireContext(), "Lỗi: ID đánh giá không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo Map để cập nhật status và updated_at
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        // Có thể thêm updated_at nếu cần
        // updates.put("updated_at", System.currentTimeMillis());

        reviewsRef.child(review.getReview_id()).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Đánh giá đã được " + (newStatus.equals("approved") ? "chấp thuận" : "từ chối") + ".", Toast.LENGTH_SHORT).show();
                        // Nếu đánh giá được chấp thuận, cập nhật điểm đánh giá trung bình của người được đánh giá
                        if (newStatus.equals("approved")) {
                            updateUserRatingForApprovedReview(review.getReviewee_id(), review.getRating());
                        } else if (newStatus.equals("rejected") && review.getStatus().equals("approved")) {
                            // If a previously approved review is now rejected, revert its rating contribution
                            // This is complex for a simple example, assuming approved status won't be easily reverted.
                            // For a full solution, you'd need to re-calculate average or subtract the old rating.
                            // For now, if moving from approved to rejected, we won't revert the score automatically.
                            // Consider only allowing pending reviews to be approved/rejected.
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update review status: " + e.getMessage());
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Lỗi khi cập nhật trạng thái đánh giá: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUserRatingForApprovedReview(String revieweeId, Integer rating) {
        if (rating == null || revieweeId == null || !isAdded()) return;

        usersRef.child(revieweeId).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                User user = currentData.getValue(User.class);
                if (user == null) {
                    return Transaction.success(currentData); // User not found, do nothing
                }

                Long currentRatingSum = user.getRating_sum() != null ? user.getRating_sum() : 0L;
                Long currentRatingCount = user.getRating_count() != null ? user.getRating_count() : 0L;

                // Only add rating if it's a new approval or if rating changes (more complex)
                // For simplicity, we assume this is called for initial approval.
                user.setRating_sum(currentRatingSum + rating);
                user.setRating_count(currentRatingCount + 1);
                // Recalculate average_rating
                if (user.getRating_count() > 0) {
                    user.setAverage_rating((double) user.getRating_sum() / user.getRating_count());
                } else {
                    user.setAverage_rating(0.0);
                }

                currentData.setValue(user);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Log.e(TAG, "Firebase transaction failed for user rating update on review approval: " + error.getMessage());
                } else if (committed) {
                    Log.d(TAG, "User rating updated successfully for user: " + revieweeId);
                }
            }
        });
    }
}
