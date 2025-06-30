package com.example.tradeup.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView; // Import ImageView
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide; // Import Glide for image loading
import com.example.tradeup.R;
import com.example.tradeup.model.Item;
import com.example.tradeup.model.Review;
import com.example.tradeup.model.Transaction; // Your Transaction model
import com.example.tradeup.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class RatingReviewFragment extends Fragment {

    private static final String TAG = "RatingReviewFragment";

    // New UI elements
    private ImageView ivBackButtonReview; // Nút quay lại trên toolbar
    private ImageView ivReviewedUserProfilePicture; // Ảnh đại diện người được đánh giá
    private TextView tvReviewedUserNameFull; // Tên đầy đủ người được đánh giá (như Sarah Johnson)
    private TextView tvTransactionProductInfo; // Thông tin sản phẩm/giao dịch (như iPhone 13 Pro • 2 days ago)
    private RatingBar ratingBarReview; // RatingBar mới
    private EditText etCommentReview; // EditText nhận xét mới
    private TextView tvCharCount; // TextView đếm ký tự
    private Button btnSubmitReviewBottom; // Nút gửi đánh giá mới

    private NavController navController;

    private String transactionId;
    private String itemId;
    private String reviewedUserId;
    private String currentUserId;
    private String reviewedUserDisplayName;
    private String reviewedUserProfilePictureUrl;
    private String transactionDate; // Để hiển thị ngày giao dịch

    private DatabaseReference reviewsRef;
    private DatabaseReference usersRef;
    private DatabaseReference itemsRef;
    private DatabaseReference transactionsRef;

    public RatingReviewFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        reviewsRef = FirebaseDatabase.getInstance().getReference("reviews");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        itemsRef = FirebaseDatabase.getInstance().getReference("items");
        transactionsRef = FirebaseDatabase.getInstance().getReference("transactions");

        if (getArguments() != null) {
            transactionId = getArguments().getString("transactionId");
            itemId = getArguments().getString("itemId");
            reviewedUserId = getArguments().getString("reviewedUserId");
        } else {
            Log.e(TAG, "No arguments passed to RatingReviewFragment!");
            if (isAdded() && navController != null) {
                Toast.makeText(requireContext(), "Lỗi: Không có thông tin giao dịch.", Toast.LENGTH_SHORT).show();
                navController.navigateUp();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rating_review, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        initViews(view);
        loadDetailsAndCheckExistingReview();
        setupListeners();
    }

    private void initViews(View view) {
        ivBackButtonReview = view.findViewById(R.id.iv_back_button_review);
        ivReviewedUserProfilePicture = view.findViewById(R.id.iv_reviewed_user_profile_picture);
        tvReviewedUserNameFull = view.findViewById(R.id.tv_reviewed_user_name_full);
        tvTransactionProductInfo = view.findViewById(R.id.tv_transaction_product_info);
        ratingBarReview = view.findViewById(R.id.rating_bar_review);
        etCommentReview = view.findViewById(R.id.et_comment_review);
        tvCharCount = view.findViewById(R.id.tv_char_count);
        btnSubmitReviewBottom = view.findViewById(R.id.btn_submit_review_bottom);
    }

    private void setupListeners() {
        ivBackButtonReview.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigateUp();
            }
        });

        btnSubmitReviewBottom.setOnClickListener(v -> submitReview());

        etCommentReview.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                int currentLength = s.length();
                tvCharCount.setText(String.format(Locale.getDefault(), "%d/500", currentLength));
            }
        });
    }

    private void loadDetailsAndCheckExistingReview() {
        if (!isAdded()) return;

        if (transactionId == null || itemId == null || reviewedUserId == null) {
            Log.e(TAG, "Missing transactionId, itemId, or reviewedUserId. Navigating up.");
            if (navController != null) navController.navigateUp();
            return;
        }

        // 1. Get item name and transaction date
        itemsRef.child(itemId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded()) {
                    Item item = snapshot.getValue(Item.class);
                    String itemTitle = (item != null && item.getTitle() != null) ? item.getTitle() : "Không xác định";

                    // Fetch transaction date
                    transactionsRef.child(transactionId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot transactionSnapshot) {
                            if (isAdded()) {
                                Transaction transaction = transactionSnapshot.getValue(Transaction.class);
                                String dateInfo = "";
                                if (transaction != null && transaction.getTransaction_date() != null) {
                                    try {
                                        SimpleDateFormat inputSdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                                        inputSdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                                        Date date = inputSdf.parse(transaction.getTransaction_date());
                                        SimpleDateFormat outputSdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                                        dateInfo = " • " + outputSdf.format(date);
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error parsing transaction date: " + e.getMessage());
                                    }
                                }
                                tvTransactionProductInfo.setText(String.format("Sản phẩm: %s%s", itemTitle, dateInfo));
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Failed to load transaction date: " + error.getMessage());
                            if (isAdded()) {
                                tvTransactionProductInfo.setText(String.format("Sản phẩm: %s • Lỗi tải ngày", itemTitle));
                            }
                        }
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load item title: " + error.getMessage());
                if (isAdded()) {
                    tvTransactionProductInfo.setText("Sản phẩm: Lỗi tải");
                }
            }
        });

        // 2. Get reviewed user's display name and profile picture
        usersRef.child(reviewedUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        reviewedUserDisplayName = user.getDisplay_name() != null ? user.getDisplay_name() : "Người dùng không xác định";
                        reviewedUserProfilePictureUrl = user.getProfile_picture_url();

                        tvReviewedUserNameFull.setText(reviewedUserDisplayName);
                        if (reviewedUserProfilePictureUrl != null && !reviewedUserProfilePictureUrl.isEmpty()) {
                            Glide.with(requireContext())
                                    .load(reviewedUserProfilePictureUrl)
                                    .placeholder(R.drawable.img_profile_placeholder)
                                    .error(R.drawable.img_profile_placeholder)
                                    .into(ivReviewedUserProfilePicture);
                        } else {
                            ivReviewedUserProfilePicture.setImageResource(R.drawable.img_profile_placeholder);
                        }
                    } else {
                        tvReviewedUserNameFull.setText("Người dùng không xác định");
                        ivReviewedUserProfilePicture.setImageResource(R.drawable.img_profile_placeholder);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load reviewed user info: " + error.getMessage());
                if (isAdded()) {
                    tvReviewedUserNameFull.setText("Lỗi tải thông tin người dùng");
                    ivReviewedUserProfilePicture.setImageResource(R.drawable.img_profile_placeholder);
                }
            }
        });

        // 3. Check if the current user has already reviewed this transaction for this specific reviewedUser
        reviewsRef.orderByChild("transaction_id").equalTo(transactionId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (isAdded()) {
                            boolean hasReviewed = false;
                            for (DataSnapshot reviewSnap : snapshot.getChildren()) {
                                Review existingReview = reviewSnap.getValue(Review.class);
                                if (existingReview != null && Objects.equals(existingReview.getReviewer_id(), currentUserId)
                                        && Objects.equals(existingReview.getReviewee_id(), reviewedUserId)) {
                                    Toast.makeText(requireContext(), "Bạn đã đánh giá giao dịch này rồi.", Toast.LENGTH_LONG).show();
                                    ratingBarReview.setRating(existingReview.getRating() != null ? existingReview.getRating() : 0);
                                    etCommentReview.setText(existingReview.getComment());
                                    etCommentReview.setEnabled(false);
                                    ratingBarReview.setIsIndicator(true);
                                    btnSubmitReviewBottom.setVisibility(View.GONE);
                                    hasReviewed = true;
                                    break;
                                }
                            }
                            if (!hasReviewed) {
                                etCommentReview.setEnabled(true);
                                ratingBarReview.setIsIndicator(false);
                                btnSubmitReviewBottom.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to check existing reviews: " + error.getMessage());
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Lỗi kiểm tra đánh giá cũ: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void submitReview() {
        if (!isAdded()) return;

        int rating = (int) ratingBarReview.getRating();
        String comment = etCommentReview.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(requireContext(), "Vui lòng chọn số sao để đánh giá.", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        String timestamp = sdf.format(new Date());

        String newReviewId = reviewsRef.push().getKey();
        if (newReviewId == null) {
            Toast.makeText(requireContext(), "Lỗi tạo ID đánh giá.", Toast.LENGTH_SHORT).show();
            return;
        }

        Review newReview = new Review(
                newReviewId,
                currentUserId,
                reviewedUserId,
                transactionId,
                rating,
                comment.isEmpty() ? null : comment,
                "pending",
                timestamp
        );

        reviewsRef.child(newReviewId).setValue(newReview)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Đánh giá của bạn đã được gửi và đang chờ kiểm duyệt!", Toast.LENGTH_LONG).show();
                        updateUserRating(reviewedUserId, rating);
                        if (navController != null) {
                            navController.navigateUp();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to submit review: " + e.getMessage());
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Lỗi khi gửi đánh giá: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUserRating(String userIdToUpdate, int newRating) {
        if (!isAdded()) return;

        usersRef.child(userIdToUpdate).runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @NonNull
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(@NonNull MutableData currentData) {
                User user = currentData.getValue(User.class);
                if (user == null) {
                    return com.google.firebase.database.Transaction.success(currentData);
                }

                Long currentRatingSum = user.getRating_sum() != null ? user.getRating_sum() : 0L;
                Long currentRatingCount = user.getRating_count() != null ? user.getRating_count() : 0L;

                user.setRating_sum(currentRatingSum + newRating);
                user.setRating_count(currentRatingCount + 1);
                if (user.getRating_count() > 0) {
                    user.setAverage_rating((double) user.getRating_sum() / user.getRating_count());
                } else {
                    user.setAverage_rating(0.0);
                }

                currentData.setValue(user);
                return com.google.firebase.database.Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Log.e(TAG, "Firebase transaction failed for user rating: " + error.getMessage());
                } else if (committed) {
                    Log.d(TAG, "User rating updated successfully for user: " + userIdToUpdate);
                }
            }
        });
    }
}
