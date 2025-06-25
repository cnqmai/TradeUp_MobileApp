package com.example.tradeup.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.tradeup.R;
import com.example.tradeup.model.Item;
import com.example.tradeup.model.User;
import com.example.tradeup.utils.FirebaseHelper;
import com.google.firebase.database.DatabaseReference; // Import này có thể cần nếu bạn dùng addValueEventListener
import com.google.firebase.database.ValueEventListener; // Import này có thể cần nếu bạn dùng addValueEventListener


import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ItemDetailFragment extends Fragment {

    private static final String TAG = "ItemDetailFragment"; // Define a TAG for logging

    private TextView tvTitle, tvPrice, tvDescription, tvCategory, tvCondition, tvLocation,
            tvItemBehavior, tvTags, tvStatus, tvSellerName, tvSellerRating, tvViewsCount;
    private Button btnChatSeller, btnMakeOffer, btnAddToFavorites;
    private ViewPager2 vpItemImages;

    private String itemId;
    private FirebaseHelper firebaseHelper;

    private String sellerId;

    // Khai báo các biến để lưu trữ listener và reference nếu bạn sử dụng addValueEventListener
    // private ValueEventListener itemEventListener;
    // private ValueEventListener sellerEventListener;
    // private DatabaseReference itemRef;
    // private DatabaseReference userRef;


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

        if (getArguments() != null) {
            itemId = getArguments().getString("itemId");
            Log.d(TAG, "itemId received in onCreate: " + itemId);
        } else {
            Log.w(TAG, "No arguments or itemId found in onCreate.");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called.");
        View view = inflater.inflate(R.layout.fragment_item_detail, container, false);
        initViews(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called. Current itemId: " + itemId);
        if (itemId != null) {
            loadItemDetails(itemId);
            updateItemViews(itemId);
        } else {
            Toast.makeText(getContext(), "Không tìm thấy ID tin đăng.", Toast.LENGTH_SHORT).show();
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
        tvSellerRating = view.findViewById(R.id.tv_detail_seller_rating);
        tvViewsCount = view.findViewById(R.id.tv_detail_views_count);

        btnChatSeller = view.findViewById(R.id.btn_detail_chat_seller);
        btnMakeOffer = view.findViewById(R.id.btn_detail_make_offer);
        btnAddToFavorites = view.findViewById(R.id.btn_detail_add_to_favorites);
        vpItemImages = view.findViewById(R.id.vp_item_images);

        btnChatSeller.setOnClickListener(v -> Toast.makeText(getContext(), "Chức năng chat đang phát triển!", Toast.LENGTH_SHORT).show());
        btnMakeOffer.setOnClickListener(v -> Toast.makeText(getContext(), "Chức năng đề nghị đang phát triển!", Toast.LENGTH_SHORT).show());
        btnAddToFavorites.setOnClickListener(v -> Toast.makeText(getContext(), "Chức năng yêu thích đang phát triển!", Toast.LENGTH_SHORT).show());
    }

    private void loadItemDetails(String id) {
        Log.d(TAG, "loadItemDetails called for itemId: " + id);
        // Nếu bạn sử dụng addValueEventListener ở đây, bạn cần gỡ bỏ listener cũ trước khi thêm listener mới
        // if (itemEventListener != null && itemRef != null) {
        //     itemRef.removeEventListener(itemEventListener);
        // }
        // itemRef = firebaseHelper.getItemReference(id); // Cần phương thức getItemReference trong FirebaseHelper
        // itemEventListener = new ValueEventListener() { ... }; // Khởi tạo listener của bạn
        // itemRef.addValueEventListener(itemEventListener);

        // Với cách gọi hiện tại của bạn (sử dụng DbReadCallback), nó thường là single-value read,
        // nên không cần removeEventListener cho phương thức này.
        firebaseHelper.getItem(id, new FirebaseHelper.DbReadCallback<Item>() {
            @Override
            public void onSuccess(Item item) {
                if (item != null) {
                    Log.d(TAG, "Item data loaded successfully for itemId: " + id + ", title: " + item.getTitle());
                    tvTitle.setText(item.getTitle());
                    NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                    currencyFormatter.setMaximumFractionDigits(0);
                    tvPrice.setText("Giá: " + currencyFormatter.format(item.getPrice()));
                    tvDescription.setText(item.getDescription());
                    tvCategory.setText("Danh mục: " + item.getCategory());
                    tvCondition.setText("Tình trạng: " + item.getCondition());

                    if (item.getLocation() != null) {
                        tvLocation.setText("Vị trí: " + item.getLocation().getManual_address());
                    } else {
                        tvLocation.setText("Vị trí: Không xác định");
                    }

                    tvItemBehavior.setText("Cách thức giao dịch: " + (item.getItem_behavior() != null ? item.getItem_behavior() : "Chưa xác định"));
                    if (item.getTags() != null && !item.getTags().isEmpty()) {
                        tvTags.setText("Tags: " + android.text.TextUtils.join(", ", item.getTags()));
                    } else {
                        tvTags.setText("Tags: Không có");
                    }
                    tvStatus.setText("Trạng thái: " + item.getStatus());

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

                } else {
                    Log.w(TAG, "Item data is null for itemId: " + id);
                    Toast.makeText(getContext(), "Không thể tải chi tiết tin đăng: Dữ liệu trống.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(getContext(), "Lỗi tải chi tiết tin đăng: " + errorMessage, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading item details: " + errorMessage);
            }
        });
    }

    private void loadSellerInfo(String userId) {
        Log.d(TAG, "loadSellerInfo called for userId: " + userId);
        // Tương tự cho seller info, nếu dùng addValueEventListener cần gỡ bỏ.
        // if (sellerEventListener != null && userRef != null) {
        //     userRef.removeEventListener(sellerEventListener);
        // }
        // userRef = firebaseHelper.getUserReference(userId); // Cần phương thức getUserReference trong FirebaseHelper
        // sellerEventListener = new ValueEventListener() { ... }; // Khởi tạo listener của bạn
        // userRef.addValueEventListener(sellerEventListener);

        // Với cách gọi hiện tại của bạn, không cần removeEventListener.
        firebaseHelper.getUserProfile(userId, new FirebaseHelper.DbReadCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    Log.d(TAG, "Seller info loaded: " + user.getDisplay_name());
                    tvSellerName.setText("Người bán: " + user.getDisplay_name());
                    Double rating = user.getRating();
                    if (rating != null) {
                        tvSellerRating.setText(String.format(Locale.getDefault(), "Đánh giá: %.1f/5.0", rating));
                    } else {
                        tvSellerRating.setText("Đánh giá: N/A");
                    }
                } else {
                    Log.w(TAG, "Seller data is null for userId: " + userId);
                    tvSellerName.setText("Người bán: Không xác định");
                    tvSellerRating.setText("Đánh giá: N/A");
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to load seller info: " + errorMessage);
                tvSellerName.setText("Người bán: Lỗi tải");
                tvSellerRating.setText("Đánh giá: Lỗi tải");
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
                        if (analyticsData != null && analyticsData.containsKey("views")) {
                            long views = 0;
                            Object viewsObj = analyticsData.get("views");
                            if (viewsObj instanceof Long) {
                                views = (Long) viewsObj;
                            } else if (viewsObj instanceof Integer) {
                                views = ((Integer) viewsObj).longValue();
                            }
                            Log.d(TAG, "Updated views fetched: " + views);
                            tvViewsCount.setText("Lượt xem: " + views);
                        } else {
                            Log.d(TAG, "Analytics data or views not found for item: " + id);
                            tvViewsCount.setText("Lượt xem: 0");
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Failed to fetch updated views for item " + id + ": " + errorMessage);
                        tvViewsCount.setText("Lượt xem: Lỗi tải");
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to increment view count for item " + id + ": " + errorMessage);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called.");

        // Quan trọng: Gỡ bỏ các Firebase Listeners tại đây nếu bạn đang sử dụng
        // addValueEventListener hoặc các listener thời gian thực khác.
        // Với cách triển khai hiện tại của loadItemDetails và loadSellerInfo
        // (sử dụng FirebaseHelper.DbReadCallback), chúng thường là các lượt đọc một lần
        // (addListenerForSingleValueEvent hoặc get()), nên không cần phải gỡ bỏ listener
        // cho các trường hợp đó.
        // Tuy nhiên, nếu bạn có các listener khác được thêm vào bằng addValueEventListener
        // mà không được gỡ bỏ, chúng có thể gây ra lỗi "Assertion failed" và rò rỉ bộ nhớ.
        // Ví dụ:
        // if (itemEventListener != null && itemRef != null) {
        //     itemRef.removeEventListener(itemEventListener);
        //     Log.d(TAG, "itemEventListener removed.");
        // }
        // if (sellerEventListener != null && userRef != null) {
        //     userRef.removeEventListener(sellerEventListener);
        //     Log.d(TAG, "sellerEventListener removed.");
        // }

        // Đặt các tham chiếu View về null để giúp dọn dẹp bộ nhớ và tránh rò rỉ
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
        btnChatSeller = null;
        btnMakeOffer = null;
        btnAddToFavorites = null;
        vpItemImages = null; // Rất quan trọng để giải phóng ViewPager2
    }


    private class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.SliderViewHolder> {
        private List<String> imageUrls;

        public ImageSliderAdapter(List<String> imageUrls) {
            this.imageUrls = imageUrls;
        }

        @NonNull
        @Override
        public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_slider_item, parent, false);
            return new SliderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrls.get(position))
                    .placeholder(R.drawable.img_placeholder)
                    .error(R.drawable.img_error)
                    .into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return imageUrls.size();
        }

        class SliderViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            public SliderViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.image_slider_image);
            }
        }
    }
}