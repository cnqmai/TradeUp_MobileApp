package com.example.tradeup.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import android.widget.RatingBar;

import com.bumptech.glide.Glide;
import com.example.tradeup.R;
import com.example.tradeup.model.Item;
import com.example.tradeup.model.Offer;
import com.example.tradeup.model.Report;
import com.example.tradeup.model.User;
import com.example.tradeup.utils.FirebaseHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.text.DecimalFormat;

import de.hdodenhof.circleimageview.CircleImageView;

public class ItemDetailFragment extends Fragment {

    private static final String TAG = "ItemDetailFragment";

    private TextView tvTitle, tvPrice, tvDescription, tvCategory, tvCondition, tvLocation,
            tvItemBehavior, tvTags, tvStatus, tvSellerName, tvSellerRating, tvViewsCount,
            tvSellerRatingCount;

    private TextView tvItemAverageRating;
    private TextView tvItemRatingCount;

    private Button btnChatSeller, btnMakeOffer, btnAddToFavorites;
    private ViewPager2 vpItemImages;

    private ImageView ivBackButton;
    private ImageView ivReportButton;
    private RatingBar sellerRatingBar;
    private CircleImageView ivSellerProfilePicture;

    private String itemId;
    private FirebaseHelper firebaseHelper;
    private String sellerId;
    private String currentUserId;
    private Item currentItem;

    // Khai báo NavController ở cấp độ lớp
    private NavController navController; // Đảm bảo dòng này tồn tại

    public ItemDetailFragment() {
        // Required empty public constructor
    }

    public static ItemDetailFragment newInstance(String itemId) {
        ItemDetailFragment fragment = new ItemDetailFragment();
        Bundle args = new Bundle();
        args.putString("itemId", itemId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called.");
        firebaseHelper = new FirebaseHelper();
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = firebaseUser != null ? firebaseUser.getUid() : null;

        if (getArguments() != null) {
            itemId = getArguments().getString("itemId");
            Log.d(TAG, "itemId received in onCreate: " + itemId);
        } else {
            Log.w(TAG, "No arguments or itemId found in onCreate.");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item_detail, container, false);
        initViews(view);
        setupListeners();

        // FIXED: Khởi tạo NavController ở đây, sau khi view đã được inflate
        //navController = Navigation.findNavController(view);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // FIX: Lấy NavController ở đây, sau khi view đã được tạo và sẵn sàng
        navController = Navigation.findNavController(view);
        Log.d(TAG, "onViewCreated called. NavController initialized.");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called. Current itemId: " + itemId);
        if (itemId != null) {
            loadItemDetails(itemId);
            updateItemViews(itemId);
        } else {
            Toast.makeText(getContext(), getString(R.string.toast_item_id_not_found), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "onResume: itemId is null, popping back stack.");
            if (getParentFragmentManager() != null && getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        }
    }

    private void initViews(View view) {
        tvTitle = view.findViewById(R.id.tv_detail_title);
        tvPrice = view.findViewById(R.id.tv_detail_price);
        tvDescription = view.findViewById(R.id.tv_detail_description);
        tvCategory = view.findViewById(R.id.tv_detail_category);
        tvCondition = view.findViewById(R.id.tv_detail_condition);
        tvLocation = view.findViewById(R.id.tv_detail_location);
        tvItemBehavior = view.findViewById(R.id.tv_detail_item_behavior);
        tvTags = view.findViewById(R.id.tv_detail_tags);
        tvStatus = view.findViewById(R.id.tv_detail_status);
        tvSellerName = view.findViewById(R.id.tv_detail_seller_name);
        ivSellerProfilePicture = view.findViewById(R.id.iv_seller_profile_picture);
        tvSellerRatingCount = view.findViewById(R.id.tv_detail_seller_rating_count);
        sellerRatingBar = view.findViewById(R.id.sellerRatingBar);
        tvSellerRating = view.findViewById(R.id.tv_detail_seller_rating);
        tvViewsCount = view.findViewById(R.id.tv_detail_views_count);

        tvItemAverageRating = view.findViewById(R.id.tv_detail_item_average_rating);
        tvItemRatingCount = view.findViewById(R.id.tv_detail_item_rating_count);

        btnChatSeller = view.findViewById(R.id.btn_detail_chat_seller);
        btnMakeOffer = view.findViewById(R.id.btn_detail_make_offer);
        btnAddToFavorites = view.findViewById(R.id.btn_detail_add_to_favorites);
        vpItemImages = view.findViewById(R.id.vp_item_images);

        ivBackButton = view.findViewById(R.id.iv_back_button_item_detail);
        ivReportButton = view.findViewById(R.id.iv_report_button_item_detail);
    }

    private void setupListeners() {
        btnChatSeller.setOnClickListener(v -> {
            if (currentUserId == null) {
                Toast.makeText(getContext(), getString(R.string.toast_login_to_chat), Toast.LENGTH_SHORT).show();
                return;
            }
            if (sellerId != null && !sellerId.isEmpty()) {
                startChatWithSeller(sellerId);
            } else {
                Toast.makeText(getContext(), getString(R.string.toast_seller_info_not_found), Toast.LENGTH_SHORT).show();
            }
        });

        btnMakeOffer.setOnClickListener(v -> {
            if (currentUserId == null) {
                Toast.makeText(getContext(), getString(R.string.toast_login_to_make_offer), Toast.LENGTH_SHORT).show();
                return;
            }
            if (itemId == null || sellerId == null || sellerId.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.toast_make_offer_missing_info), Toast.LENGTH_SHORT).show();
                return;
            }
            showMakeOfferDialog();
        });

        btnAddToFavorites.setOnClickListener(v -> {
            if (currentUserId == null) {
                Toast.makeText(getContext(), getString(R.string.toast_login_to_favorite), Toast.LENGTH_SHORT).show();
                return;
            }
            if (itemId != null && !itemId.isEmpty()) {
                toggleFavorite(itemId);
            } else {
                Toast.makeText(getContext(), getString(R.string.toast_favorite_missing_item_id), Toast.LENGTH_SHORT).show();
            }
        });

        ivBackButton.setOnClickListener(v -> {
            // FIXED: Kiểm tra navController trước khi sử dụng
            if (navController != null) {
                navController.navigateUp();
            }
        });

        ivReportButton.setOnClickListener(v -> {
            if (isAdded() && currentItem != null && currentUserId != null) {
                if (currentUserId.equals(currentItem.getUser_id())) {
                    Toast.makeText(getContext(), getString(R.string.toast_cannot_report_own_item), Toast.LENGTH_SHORT).show();
                } else {
                    showReportDialog("item", currentItem.getId());
                }
            } else {
                Log.w(TAG, "Report button clicked but fragment not added, currentItem is null, or currentUserId is null.");
                if (isAdded()) {
                    Toast.makeText(getContext(), getString(R.string.toast_cannot_report_now), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void startChatWithSeller(String otherUserId) {
        if (currentUserId == null || otherUserId == null || currentUserId.equals(otherUserId)) {
            Log.e(TAG, "Invalid chat participants: currentUserId or otherUserId is null/same.");
            Toast.makeText(getContext(), getString(R.string.toast_cannot_start_chat), Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");

        Query query1 = chatsRef.orderByChild("user_1").equalTo(currentUserId);
        Query query2 = chatsRef.orderByChild("user_1").equalTo(otherUserId);

        query1.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String chatId = findExistingChat(dataSnapshot, otherUserId);

                if (chatId != null) {
                    navigateToChatDetail(chatId, otherUserId);
                } else {
                    query2.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot2) {
                            String chatId2 = findExistingChat(dataSnapshot2, otherUserId);
                            if (chatId2 != null) {
                                navigateToChatDetail(chatId2, otherUserId);
                            } else {
                                createNewChat(otherUserId);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e(TAG, "Error checking chat (query2): " + databaseError.getMessage());
                            if (isAdded()) {
                                Toast.makeText(getContext(), getString(R.string.error_checking_chat), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error checking chat (query1): " + databaseError.getMessage());
                if (isAdded()) {
                    Toast.makeText(getContext(), getString(R.string.error_checking_chat), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private String findExistingChat(DataSnapshot dataSnapshot, String targetUserId) {
        for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
            String user1 = chatSnapshot.child("user_1").getValue(String.class);
            String user2 = chatSnapshot.child("user_2").getValue(String.class);
            if ((Objects.equals(user1, currentUserId) && Objects.equals(user2, targetUserId)) ||
                    (Objects.equals(user1, targetUserId) && Objects.equals(user2, currentUserId))) {
                return chatSnapshot.getKey();
            }
        }
        return null;
    }

    private void createNewChat(String otherUserId) {
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        String newChatId = chatsRef.push().getKey();

        if (newChatId == null) {
            if (isAdded()) {
                Toast.makeText(getContext(), getString(R.string.error_creating_chat_id), Toast.LENGTH_SHORT).show();
            }
            return;
        }

        Map<String, Object> chatData = new HashMap<>();
        chatData.put("user_1", currentUserId);
        chatData.put("user_2", otherUserId);
        chatData.put("lastMessage", getString(R.string.new_chat_message)); // New string resource
        chatData.put("lastMessageTimestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));
        chatData.put("blocked", false);
        chatData.put("reported", false);

        chatsRef.child(newChatId).setValue(chatData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "New chat created: " + newChatId);
                    navigateToChatDetail(newChatId, otherUserId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create new chat: " + e.getMessage());
                    if (isAdded()) {
                        Toast.makeText(getContext(), getString(R.string.error_creating_new_chat), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToChatDetail(String chatId, String otherUserId) {
        Bundle bundle = new Bundle();
        bundle.putString("chatId", chatId);
        bundle.putString("otherUserId", otherUserId);
        // FIXED: Kiểm tra navController trước khi sử dụng
        if (navController != null) {
            navController.navigate(R.id.action_itemDetailFragment_to_chatDetailFragment, bundle);
        } else {
            Log.e(TAG, "NavController is null, cannot navigate to chat detail.");
            if (isAdded()) {
                Toast.makeText(getContext(), getString(R.string.error_opening_chat), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showMakeOfferDialog() {
        if (!isAdded()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.dialog_title_make_offer));

        final EditText input = new EditText(getContext());
        input.setHint(getString(R.string.hint_enter_offer_price));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton(getString(R.string.button_send_offer), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String offerPriceStr = input.getText().toString().trim();
                if (offerPriceStr.isEmpty()) {
                    Toast.makeText(getContext(), getString(R.string.toast_enter_offer_price), Toast.LENGTH_SHORT).show();
                    return;
                }
                long offerPrice = Long.parseLong(offerPriceStr);
                createOffer(offerPrice);
            }
        });
        builder.setNegativeButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void createOffer(long offerPrice) {
        DatabaseReference offersRef = FirebaseDatabase.getInstance().getReference("offers");
        String offerId = offersRef.push().getKey();

        if (offerId == null) {
            if (isAdded()) {
                Toast.makeText(getContext(), getString(R.string.error_creating_offer_id), Toast.LENGTH_SHORT).show();
            }
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        String timestamp = sdf.format(new Date());

        Offer offer = new Offer(
                offerId,
                itemId,
                currentUserId,
                sellerId,
                offerPrice,
                "pending", // This is a status, might be better as an enum or string resource if localized
                null,
                timestamp,
                timestamp
        );

        offersRef.child(offerId).setValue(offer)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), getString(R.string.toast_offer_sent_success), Toast.LENGTH_SHORT).show();
                    }
                    Log.d(TAG, "Offer created: " + offerId);

                    if (sellerId != null && !sellerId.isEmpty() && currentItem != null) {
                        DatabaseReference notificationsRef = FirebaseDatabase.getInstance().getReference("notifications");
                        String notificationId = notificationsRef.push().getKey();

                        if (notificationId != null) {
                            Map<String, Object> notificationContent = new HashMap<>();
                            notificationContent.put("user_id", sellerId);
                            notificationContent.put("title", getString(R.string.notification_new_offer_title));
                            notificationContent.put("body", getString(R.string.notification_new_offer_body, currentItem.getTitle()));
                            notificationContent.put("type", "new_offer"); // This is a type, might be better as an enum or string resource if localized
                            notificationContent.put("related_id", offerId);
                            String isoTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());
                            notificationContent.put("timestamp", isoTimestamp);
                            notificationContent.put("read", false);

                            notificationsRef.child(notificationId).setValue(notificationContent)
                                    .addOnSuccessListener(aVoid1 -> Log.d(TAG, "Notification created for seller: " + sellerId))
                                    .addOnFailureListener(e -> Log.e(TAG, "Failed to create notification for seller: " + e.getMessage()));
                        }
                    } else {
                        Log.w(TAG, getString(R.string.cannot_create_notification_missing_data));
                    }

                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), getString(R.string.toast_offer_send_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
                    }
                    Log.e(TAG, "Failed to create offer: " + e.getMessage());
                });
    }

    private void toggleFavorite(String itemId) {
        if (currentUserId == null) {
            if (isAdded()) {
                Toast.makeText(getContext(), getString(R.string.toast_login_to_favorite), Toast.LENGTH_SHORT).show();
            }
            return;
        }

        DatabaseReference favoritesRef = FirebaseDatabase.getInstance().getReference("users")
                .child(currentUserId)
                .child("favorites")
                .child(itemId);

        favoritesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                if (snapshot.exists()) {
                    favoritesRef.removeValue()
                            .addOnSuccessListener(aVoid -> {
                                if (isAdded()) {
                                    Toast.makeText(getContext(), getString(R.string.toast_removed_from_favorites), Toast.LENGTH_SHORT).show();
                                }
                                Log.d(TAG, "Item " + itemId + " removed from favorites.");
                            })
                            .addOnFailureListener(e -> {
                                if (isAdded()) {
                                    Toast.makeText(getContext(), getString(R.string.toast_error_removing_favorite, e.getMessage()), Toast.LENGTH_SHORT).show();
                                }
                                Log.e(TAG, "Failed to remove item " + itemId + " from favorites: " + e.getMessage());
                            });
                } else {
                    favoritesRef.setValue(true)
                            .addOnSuccessListener(aVoid -> {
                                if (isAdded()) {
                                    Toast.makeText(getContext(), getString(R.string.toast_added_to_favorites), Toast.LENGTH_SHORT).show();
                                }
                                Log.d(TAG, "Item " + itemId + " added to favorites.");
                            })
                            .addOnFailureListener(e -> {
                                if (isAdded()) {
                                    Toast.makeText(getContext(), getString(R.string.toast_error_adding_favorite, e.getMessage()), Toast.LENGTH_SHORT).show();
                                }
                                Log.e(TAG, "Failed to add item " + itemId + " to favorites: " + e.getMessage());
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), getString(R.string.toast_error_checking_favorite, error.getMessage()), Toast.LENGTH_SHORT).show();
                }
                Log.e(TAG, "Failed to check favorite status: " + error.getMessage());
            }
        });
    }

    private void loadItemDetails(String id) {
        Log.d(TAG, "loadItemDetails called for itemId: " + id);
        firebaseHelper.getItem(id, new FirebaseHelper.DbReadCallback<Item>() {
            @Override
            public void onSuccess(Item item) {
                if (!isAdded()) return;

                if (item != null) {
                    currentItem = item;
                    Log.d(TAG, "Item data loaded successfully for itemId: " + id + ", title: " + item.getTitle());
                    tvTitle.setText(item.getTitle());
                    NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                    currencyFormatter.setMaximumFractionDigits(0);
                    tvPrice.setText(getString(R.string.price_format, currencyFormatter.format(item.getPrice())));
                    tvDescription.setText(item.getDescription());
                    tvCategory.setText(getString(R.string.category_format, item.getCategory()));
                    tvCondition.setText(getString(R.string.condition_format, item.getCondition()));

                    if (item.getLocation() != null) {
                        tvLocation.setText(getString(R.string.location_format, item.getLocation().getManual_address()));
                    } else {
                        tvLocation.setText(R.string.location_unknown);
                    }

                    tvItemBehavior.setText(item.getItem_behavior() != null ? item.getItem_behavior() : getString(R.string.item_behavior_unspecified));
                    if (item.getTags() != null && !item.getTags().isEmpty()) {
                        tvTags.setText(getString(R.string.tags_format, android.text.TextUtils.join(", ", item.getTags())));
                    } else {
                        tvTags.setText(R.string.tags_none);
                    }

                    // START OF ADDED/MODIFIED CODE
                    // Cập nhật trạng thái sản phẩm và áp dụng styling nếu cần
                    String status = item.getStatus();
                    if (status != null) {
                        // Chuyển đổi trạng thái từ database sang chuỗi hiển thị thân thiện
                        String displayStatus;
                        int statusBgDrawable; // Drawable cho nền
                        int statusTextColor;  // Màu chữ

                        switch (status) {
                            case "Available":
                                displayStatus = getString(R.string.status_available);
                                statusBgDrawable = R.drawable.tag_approved; // Ví dụ: drawable màu xanh lá cây
                                statusTextColor = getResources().getColor(R.color.white); // Màu trắng
                                break;
                            case "Sold":
                                displayStatus = getString(R.string.status_sold);
                                statusBgDrawable = R.drawable.tag_rejected; // Ví dụ: drawable màu đỏ
                                statusTextColor = getResources().getColor(R.color.white); // Màu trắng
                                break;
                            case "Pending": // Có thể có trạng thái "đang chờ giao dịch"
                                displayStatus = getString(R.string.status_pending);
                                statusBgDrawable = R.drawable.tag_pending; // Ví dụ: drawable màu cam
                                statusTextColor = getResources().getColor(R.color.white); // Màu trắng
                                break;
                            default:
                                displayStatus = status; // Hiển thị nguyên trạng nếu không khớp
                                statusBgDrawable = R.drawable.tag_default; // Drawable mặc định (ví dụ: xám)
                                statusTextColor = getResources().getColor(R.color.black); // Màu đen
                                break;
                        }
                        tvStatus.setText(displayStatus);
                        tvStatus.setBackgroundResource(statusBgDrawable);
                        tvStatus.setTextColor(statusTextColor);
                        // END OF ADDED/MODIFIED CODE
                    } else {
                        tvStatus.setText(getString(R.string.status_unknown)); // Trường hợp status là null
                        tvStatus.setBackgroundResource(R.drawable.tag_default);
                        tvStatus.setTextColor(getResources().getColor(R.color.black));
                    }

                    if (item.getAverage_rating() != null && item.getRating_count() != null) {
                        tvItemAverageRating.setText(getString(R.string.item_rating_format, item.getAverage_rating()));
                        tvItemRatingCount.setText(getString(R.string.rating_count_format, item.getRating_count())); // New string resource
                    } else {
                        tvItemAverageRating.setText(R.string.item_rating_na);
                        tvItemRatingCount.setText(getString(R.string.rating_count_format, 0));
                    }

                    sellerId = item.getUser_id();
                    loadSellerInfo(sellerId);

                    if (item.getPhotos() != null && !item.getPhotos().isEmpty()) {
                        Log.d(TAG, "Loading " + item.getPhotos().size() + " images.");
                        ImageSliderAdapter adapter = new ImageSliderAdapter(item.getPhotos());
                        vpItemImages.setAdapter(adapter);
                    } else {
                        Log.d(TAG, "No images found, using placeholder.");
                        List<String> placeholders = new ArrayList<>();
                        placeholders.add("android.resource://" + requireContext().getPackageName() + "/" + R.drawable.img_placeholder);
                        ImageSliderAdapter adapter = new ImageSliderAdapter(placeholders);
                        vpItemImages.setAdapter(adapter);
                    }

                    if (currentUserId != null && item.getCategory() != null) {
                        recordUserView(currentUserId, id, item.getCategory());
                    } else {
                        Log.w(TAG, getString(R.string.cannot_record_user_view));
                    }

                    if (currentUserId != null && currentUserId.equals(sellerId)) {
                        btnChatSeller.setVisibility(View.GONE);
                        btnMakeOffer.setVisibility(View.GONE);
                        btnAddToFavorites.setVisibility(View.GONE);
                        Log.d(TAG, "Current user is seller. Hiding chat, offer, and favorite buttons.");
                    } else {
                        btnChatSeller.setVisibility(View.VISIBLE);
                        btnMakeOffer.setVisibility(View.VISIBLE);
                        btnAddToFavorites.setVisibility(View.VISIBLE);
                        Log.d(TAG, "Current user is buyer. Showing chat, offer, and favorite buttons.");
                    }

                } else {
                    Log.w(TAG, "Item data is null for itemId: " + id);
                    if (isAdded()) {
                        Toast.makeText(getContext(), getString(R.string.error_loading_item_details_empty_data), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (isAdded()) {
                    Toast.makeText(getContext(), getString(R.string.error_loading_item_details, errorMessage), Toast.LENGTH_SHORT).show();
                }
                Log.e(TAG, "Error loading item details: " + errorMessage);
            }
        });
    }

    private void loadSellerInfo(String userId) {
        Log.d(TAG, "loadSellerInfo called for sellerUid: " + userId);
        firebaseHelper.getUserProfile(userId, new FirebaseHelper.DbReadCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (isAdded()) {
                    if (user != null) {
                        Log.d(TAG, "Seller info loaded: " + user.getDisplay_name());
                        tvSellerName.setText(user.getDisplay_name());

                        String profilePictureUrl = user.getProfile_picture_url();
                        Log.d(TAG, "Seller Profile Picture URL: " + (profilePictureUrl != null ? profilePictureUrl : "NULL"));

                        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                            Glide.with(requireContext())
                                    .load(profilePictureUrl)
                                    .placeholder(R.drawable.img_profile_placeholder)
                                    .error(R.drawable.img_error)
                                    .into(ivSellerProfilePicture);
                            Log.d(TAG, "Attempting to load image with Glide: " + profilePictureUrl);
                        } else {
                            ivSellerProfilePicture.setImageResource(R.drawable.img_profile_placeholder);
                            Log.d(TAG, "Profile picture URL is null or empty, setting placeholder.");
                        }

                        if (user.getAverage_rating() != null) {
                            float averageRating = user.getAverage_rating().floatValue();
                            sellerRatingBar.setRating(averageRating);
                            DecimalFormat df = new DecimalFormat("#.#");
                            tvSellerRating.setText(df.format(averageRating) + "/5.0");

                            String ratingCountText = (user.getRating_count() != null ? user.getRating_count() : 0) + " lượt";
                            tvSellerRatingCount.setText("(" + ratingCountText + ")");
                        } else {
                            sellerRatingBar.setRating(0);
                            tvSellerRating.setText("N/A");
                            tvSellerRatingCount.setText("(0 lượt)");
                        }
                    } else {
                        Log.w(TAG, "Seller with ID " + userId + " not found.");
                        tvSellerName.setText("Người bán không tồn tại");
                        tvSellerRating.setText("N/A");
                        tvSellerRatingCount.setText("(0 lượt)");
                        sellerRatingBar.setRating(0);
                        ivSellerProfilePicture.setImageResource(R.drawable.img_profile_placeholder);
                    }
                } else {
                    Log.w(TAG, "Fragment is not added when seller info loaded.");
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Error loading seller details: " + error);
                if (isAdded()) {
                    Toast.makeText(getContext(), "Lỗi khi tải thông tin người bán: " + error, Toast.LENGTH_SHORT).show();
                }
                tvSellerName.setText("Lỗi tải thông tin");
                tvSellerRating.setText("N/A");
                tvSellerRatingCount.setText("(0 lượt)");
                sellerRatingBar.setRating(0);
                ivSellerProfilePicture.setImageResource(R.drawable.img_profile_placeholder);
            }
        });
    }

    private void updateItemViews(String id) {
        Log.d(TAG, "updateItemViews called for itemId: " + id);
        firebaseHelper.incrementItemView(id, new FirebaseHelper.DbWriteCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "View count incremented successfully for item: " + id);
                firebaseHelper.getItemAnalytics(id, new FirebaseHelper.DbReadCallback<Map<String, Object>>() {
                    @Override
                    public void onSuccess(Map<String, Object> analyticsData) {
                        if (!isAdded()) return;

                        if (analyticsData != null && analyticsData.containsKey("views")) {
                            long views = 0;
                            Object viewsObj = analyticsData.get("views");
                            if (viewsObj instanceof Long) {
                                views = (Long) viewsObj;
                            } else if (viewsObj instanceof Integer) {
                                views = ((Integer) viewsObj).longValue();
                            }
                            Log.d(TAG, "Updated views fetched: " + views);
                            tvViewsCount.setText(getString(R.string.item_views, views));
                        } else {
                            Log.d(TAG, "Analytics data or views not found for item: " + id);
                            tvViewsCount.setText(R.string.item_views_zero); // New string resource
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        if (!isAdded()) return;
                        Log.e(TAG, "Failed to fetch updated views for item " + id + ": " + errorMessage);
                        tvViewsCount.setText(R.string.item_views_error);
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                if (!isAdded()) return;
                Log.e(TAG, "Failed to increment view count for item " + id + ": " + errorMessage);
            }
        });
    }

    private void recordUserView(String userId, String itemId, String category) {
        if (userId == null || itemId == null || category == null || category.isEmpty()) {
            Log.w(TAG, getString(R.string.cannot_record_user_view));
            return;
        }

        FirebaseDatabase.getInstance().getReference("user_activity")
                .child(userId)
                .child("viewed_categories")
                .child(category)
                .child("last_viewed")
                .setValue(ServerValue.TIMESTAMP)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User category view recorded for: " + category))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to record user category view: " + e.getMessage()));
    }

    private void showReportDialog(String reportType, String reportedObjectId) {
        if (!isAdded()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_report_reason, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.tv_report_dialog_title);
        RadioGroup rgReasons = dialogView.findViewById(R.id.rg_report_reasons);
        TextInputEditText etComment = dialogView.findViewById(R.id.et_report_comment);
        ImageView ivCloseDialog = dialogView.findViewById(R.id.iv_close_report_dialog);
        Button btnSubmit = dialogView.findViewById(R.id.btn_submit_report);

        String title = "";
        switch (reportType) {
            case "item":
                title = getString(R.string.report_dialog_title_item);
                break;
            case "user":
                title = getString(R.string.report_dialog_title_user);
                break;
            case "chat":
                title = getString(R.string.report_dialog_title_chat);
                break;
            default:
                title = getString(R.string.report_dialog_title_default);
                break;
        }
        tvTitle.setText(title);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        ivCloseDialog.setOnClickListener(v -> {
            etComment.setText(""); // Vẫn xóa nội dung của etComment khi đóng/hủy
            dialog.dismiss();
        });

        btnSubmit.setOnClickListener(v -> {
            int selectedId = rgReasons.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(getContext(), getString(R.string.toast_select_report_reason), Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton selectedRadioButton = dialogView.findViewById(selectedId);
            String reason = selectedRadioButton.getText().toString();
            String comment = etComment.getText().toString().trim();

            if (reason.equals(getString(R.string.report_reason_other)) && comment.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.toast_describe_other_reason), Toast.LENGTH_SHORT).show();
                return;
            }

            String reporterId = currentUserId != null ? currentUserId : "anonymous"; // "anonymous" could also be a string resource
            Report report = new Report(reporterId, reportedObjectId, reportType, reason, comment);

            saveReportToFirebase(report);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveReportToFirebase(Report report) {
        if (!isAdded()) return;

        DatabaseReference reportsRef = FirebaseDatabase.getInstance().getReference("reports");
        String reportId = reportsRef.push().getKey();

        if (reportId != null) {
            report.setReport_id(reportId);
            reportsRef.child(reportId).setValue(report)
                    .addOnSuccessListener(aVoid -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), getString(R.string.toast_report_sent_success), Toast.LENGTH_LONG).show();
                            Log.d(TAG, "Report submitted: " + reportId + " for " + report.getReported_object_id());
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), getString(R.string.toast_error_sending_report, e.getMessage()), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Failed to submit report: " + e.getMessage());
                        }
                    });
        } else {
            if (isAdded()) {
                Toast.makeText(getContext(), getString(R.string.error_creating_report_id), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called.");

        tvTitle = null;
        tvPrice = null;
        tvDescription = null;
        tvCategory = null;
        tvCondition = null;
        tvLocation = null;
        tvItemBehavior = null;
        tvTags = null;
        tvStatus = null;
        tvSellerName = null;
        tvSellerRating = null;
        tvViewsCount = null;
        tvItemAverageRating = null;
        tvItemRatingCount = null;
        btnChatSeller = null;
        btnMakeOffer = null;
        btnAddToFavorites = null;
        vpItemImages = null;
        ivBackButton = null;
        ivReportButton = null;
        currentItem = null;
        ivSellerProfilePicture = null;
        tvSellerRatingCount = null;
        sellerRatingBar = null;
    }

    private class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.SliderViewHolder> {
        private List<String> imageUrls;
        private Context adapterContext;

        public ImageSliderAdapter(List<String> imageUrls) {
            this.imageUrls = imageUrls;
            // Ensure context is retrieved safely
            if (ItemDetailFragment.this.isAdded()) {
                this.adapterContext = ItemDetailFragment.this.requireContext();
            }
        }

        @NonNull
        @Override
        public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_slider_item, parent, false);
            return new SliderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
            String imageUrl = imageUrls.get(position);
            if (adapterContext != null) {
                Glide.with(adapterContext)
                        .load(imageUrl)
                        .placeholder(R.drawable.img_placeholder)
                        .error(R.drawable.img_error)
                        .into(holder.imageView);
            }
        }

        @Override
        public int getItemCount() {
            return imageUrls.size();
        }

        public class SliderViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            public SliderViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.image_slider_image);
            }
        }
    }
}