package com.example.tradeup.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup.R;
import com.example.tradeup.adapter.CategoryAdapter;
import com.example.tradeup.adapter.ItemAdapter;
import com.example.tradeup.model.Item;
import com.example.tradeup.model.Location; // Giữ import này cho class Location của bạn
import com.example.tradeup.utils.FirebaseHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.auth.FirebaseAuth; // Thêm import này để lấy userId
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects; // Thêm import này
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger; // Thêm import này

public class HomeFragment extends Fragment {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private RecyclerView rvPopularItems, rvItemsNearYou, rvCategories, rvPersonalizedItems;
    private ItemAdapter popularItemsAdapter, itemsNearYouAdapter, personalizedItemsAdapter;
    private CategoryAdapter categoryAdapter;
    private FirebaseHelper firebaseHelper;
    private NavController navController;

    private FusedLocationProviderClient fusedLocationClient;
    private CancellationTokenSource cancellationTokenSource;
    private FirebaseAuth mAuth; // Khai báo FirebaseAuth

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);
        firebaseHelper = new FirebaseHelper();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        mAuth = FirebaseAuth.getInstance(); // Khởi tạo FirebaseAuth

        // Initialize RecyclerViews
        rvCategories = view.findViewById(R.id.rvCategories);
        rvItemsNearYou = view.findViewById(R.id.rvItemsNearYou);
        rvPopularItems = view.findViewById(R.id.rvPopularItems);
        rvPersonalizedItems = view.findViewById(R.id.rvPersonalizedItems); // Khởi tạo RecyclerView mới

        // Setup Adapters
        categoryAdapter = new CategoryAdapter(getContext(), new ArrayList<>());
        itemsNearYouAdapter = new ItemAdapter(getContext(), new ArrayList<>());
        popularItemsAdapter = new ItemAdapter(getContext(), new ArrayList<>());
        personalizedItemsAdapter = new ItemAdapter(getContext(), new ArrayList<>()); // Khởi tạo Adapter mới

        // Set Layout Managers and Adapters (theo thứ tự hiển thị trong XML)
        rvCategories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(categoryAdapter);

        rvItemsNearYou.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvItemsNearYou.setAdapter(itemsNearYouAdapter);

        rvPopularItems.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvPopularItems.setAdapter(popularItemsAdapter);

        rvPersonalizedItems.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvPersonalizedItems.setAdapter(personalizedItemsAdapter); // Thiết lập cho RecyclerView mới

        // Set click listeners for items
        categoryAdapter.setOnCategoryClickListener(category -> {
            Bundle bundle = new Bundle();
            bundle.putString("searchCategory", category);
            navController.navigate(R.id.action_homeFragment_to_searchFragment, bundle);
        });

        itemsNearYouAdapter.setOnItemClickListener(itemId -> {
            Bundle bundle = new Bundle();
            bundle.putString("itemId", itemId);
            navController.navigate(R.id.action_homeFragment_to_itemDetailFragment, bundle);
        });

        popularItemsAdapter.setOnItemClickListener(itemId -> {
            Bundle bundle = new Bundle();
            bundle.putString("itemId", itemId);
            navController.navigate(R.id.action_homeFragment_to_itemDetailFragment, bundle);
        });

        personalizedItemsAdapter.setOnItemClickListener(itemId -> { // Thiết lập listener cho adapter mới
            Bundle bundle = new Bundle();
            bundle.putString("itemId", itemId);
            navController.navigate(R.id.action_homeFragment_to_itemDetailFragment, bundle);
        });


        // Fetch data
        fetchCategories(); // Duyệt theo danh mục
        checkLocationPermissionAndFetchNearYou(); // Sản phẩm gần bạn
        fetchTrendingItems(); // Đang được quan tâm (dựa vào rating)
        fetchPersonalizedItems(); // Dành riêng cho bạn (dựa vào lịch sử duyệt)
    }

    // Cập nhật logic để lấy sản phẩm đang được quan tâm dựa trên rating
    private void fetchTrendingItems() {
        DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("items");
        itemsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Item> trendingItems = new ArrayList<>();
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    Item item = itemSnapshot.getValue(Item.class);
                    // Chỉ lấy các item còn "Available"
                    if (item != null && item.getStatus().equals("Available")) {
                        item.setId(itemSnapshot.getKey()); // Set the ID from the snapshot key
                        trendingItems.add(item);
                    }
                }

                // Lọc và sắp xếp các item theo rating
                // Tiêu chí: average_rating (giảm dần), sau đó rating_count (giảm dần)
                // Loại bỏ các item không có đánh giá (rating_count <= 0) để đảm bảo độ tin cậy
                trendingItems.removeIf(item -> item.getRating_count() == null || item.getRating_count() <= 0);

                Collections.sort(trendingItems, (item1, item2) -> {
                    // Sắp xếp chính: average_rating giảm dần
                    int ratingComparison = Double.compare(item2.getAverage_rating(), item1.getAverage_rating());
                    if (ratingComparison != 0) {
                        return ratingComparison;
                    }
                    // Sắp xếp phụ: rating_count giảm dần (dùng khi average_rating bằng nhau)
                    return Long.compare(item2.getRating_count(), item1.getRating_count());
                });

                // Giới hạn số lượng item hiển thị (ví dụ: top 10)
                List<Item> topTrendingItems = trendingItems.subList(0, Math.min(trendingItems.size(), 10));
                popularItemsAdapter.setItems(topTrendingItems);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomeFragment", "Failed to load trending items by rating: " + error.getMessage());
                Toast.makeText(getContext(), "Không thể tải sản phẩm đang được quan tâm.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void fetchItemsByIds(List<String> itemIds, ItemAdapter adapter) {
        if (itemIds.isEmpty()) {
            adapter.setItems(new ArrayList<>());
            return;
        }

        DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("items");
        // Using a Map to ensure items are added in the original order of itemIds, and to handle async fetches
        Map<String, Item> fetchedItemMap = new HashMap<>();
        final AtomicInteger fetchedCount = new AtomicInteger(0); // Use AtomicInteger for thread safety

        for (String itemId : itemIds) {
            itemsRef.child(itemId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Item item = snapshot.getValue(Item.class);
                    if (item != null) {
                        item.setId(snapshot.getKey()); // Set the ID from the snapshot key
                        fetchedItemMap.put(snapshot.getKey(), item);
                    }
                    if (fetchedCount.incrementAndGet() == itemIds.size()) { // Increment and check if all fetched
                        List<Item> orderedItems = new ArrayList<>();
                        for (String id : itemIds) {
                            if (fetchedItemMap.containsKey(id)) {
                                orderedItems.add(fetchedItemMap.get(id));
                            }
                        }
                        adapter.setItems(orderedItems);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("HomeFragment", "Failed to load item by ID: " + error.getMessage());
                    if (fetchedCount.incrementAndGet() == itemIds.size()) { // Still increment to ensure completion check works
                        List<Item> orderedItems = new ArrayList<>();
                        for (String id : itemIds) {
                            if (fetchedItemMap.containsKey(id)) {
                                orderedItems.add(fetchedItemMap.get(id));
                            }
                        }
                        adapter.setItems(orderedItems);
                    }
                }
            });
        }
    }


    private void fetchCategories() {
        DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("items");
        itemsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> categories = new HashSet<>();
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    Item item = itemSnapshot.getValue(Item.class);
                    if (item != null && item.getCategory() != null && !item.getCategory().isEmpty()) {
                        categories.add(item.getCategory());
                    }
                }
                List<String> categoryList = new ArrayList<>(categories);
                Collections.sort(categoryList); // Sort categories alphabetically
                categoryAdapter.setCategories(categoryList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomeFragment", "Failed to load categories: " + error.getMessage());
                Toast.makeText(getContext(), "Không thể tải danh mục.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkLocationPermissionAndFetchNearYou() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchUserLocationAndItemsNearYou();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void fetchUserLocationAndItemsNearYou() {
        cancellationTokenSource = new CancellationTokenSource();

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "Ứng dụng không có quyền truy cập vị trí.", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        Log.d("HomeFragment", "Vị trí người dùng: Lat=" + location.getLatitude() + ", Lng=" + location.getLongitude());
                        fetchItemsNearYou(location); // location ở đây là android.location.Location
                    } else {
                        Log.w("HomeFragment", "Không thể lấy vị trí cuối cùng. Thử yêu cầu vị trí mới.");
                        requestNewLocationUpdates();
                    }
                })
                .addOnFailureListener(requireActivity(), e -> {
                    Log.e("HomeFragment", "Lỗi khi lấy vị trí: " + e.getMessage());
                    Toast.makeText(getContext(), "Không thể lấy vị trí hiện tại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void requestNewLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000); // 10 seconds
        locationRequest.setFastestInterval(5000); // 5 seconds
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (android.location.Location location : locationResult.getLocations()) { // Sử dụng tên đầy đủ
                    if (location != null) {
                        Log.d("HomeFragment", "Vị trí mới: Lat=" + location.getLatitude() + ", Lng=" + location.getLongitude());
                        fetchItemsNearYou(location); // location ở đây là android.location.Location
                        fusedLocationClient.removeLocationUpdates(this); // Stop updates after getting one
                        return;
                    }
                }
            }
        };

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void fetchItemsNearYou(android.location.Location userLocation) { // Cập nhật tham số thành android.location.Location
        DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("items");
        itemsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Item> nearbyItems = new ArrayList<>();
                float searchRadiusKm = 10.0f; // Example search radius in kilometers

                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    Item item = itemSnapshot.getValue(Item.class);
                    if (item != null && item.getLocation() != null && item.getStatus().equals("Available")) {
                        // Kiểm tra sự tồn tại của 'lat' và 'lng' trực tiếp trên DataSnapshot
                        DataSnapshot locationSnapshot = itemSnapshot.child("location");
                        if (locationSnapshot.child("lat").exists() && locationSnapshot.child("lng").exists()) {
                            // Tạo đối tượng android.location.Location để tính khoảng cách
                            android.location.Location itemLocation = new android.location.Location("");
                            // Chuyển đổi Double (từ model của bạn) sang double (cho Android Location)
                            itemLocation.setLatitude(item.getLocation().getLat());
                            itemLocation.setLongitude(item.getLocation().getLng());

                            float distanceInMeters = userLocation.distanceTo(itemLocation);
                            float distanceInKm = distanceInMeters / 1000;

                            if (distanceInKm <= searchRadiusKm) {
                                item.setId(itemSnapshot.getKey());
                                nearbyItems.add(item);
                            }
                        } else {
                            Log.w("HomeFragment", "Item " + itemSnapshot.getKey() + " is missing lat/lng fields in its location data.");
                        }
                    }
                }
                // Sort by distance (closest first)
                Collections.sort(nearbyItems, (item1, item2) -> {
                    // Tạo đối tượng android.location.Location để tính khoảng cách
                    android.location.Location loc1 = new android.location.Location("");
                    // Chuyển đổi Double (từ model của bạn) sang double (cho Android Location)
                    loc1.setLatitude(item1.getLocation().getLat());
                    loc1.setLongitude(item1.getLocation().getLng());

                    android.location.Location loc2 = new android.location.Location("");
                    // Chuyển đổi Double (từ model của bạn) sang double (cho Android Location)
                    loc2.setLatitude(item2.getLocation().getLat());
                    loc2.setLongitude(item2.getLocation().getLng());

                    float dist1 = userLocation.distanceTo(loc1);
                    float dist2 = userLocation.distanceTo(loc2);
                    return Float.compare(dist1, dist2);
                });
                itemsNearYouAdapter.setItems(nearbyItems);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomeFragment", "Failed to load items near you: " + error.getMessage());
                Toast.makeText(getContext(), "Không thể tải sản phẩm gần bạn.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchPersonalizedItems() {
        String userId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        if (userId == null) {
            // Người dùng chưa đăng nhập, không thể cá nhân hóa. Có thể hiển thị mục phổ biến chung
            Log.w("HomeFragment", "User not logged in, cannot fetch personalized items.");
            // Ví dụ: hiển thị các item phổ biến chung nếu không có user ID
            fetchItemsByIds(new ArrayList<>(), personalizedItemsAdapter); // Hoặc một danh sách rỗng
            return;
        }

        // Lấy 5 danh mục xem gần nhất của người dùng
        DatabaseReference userHistoryRef = FirebaseDatabase.getInstance().getReference("user_activity").child(userId).child("viewed_categories");
        userHistoryRef.limitToLast(5).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> recentCategories = new HashSet<>();
                for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                    // Giả sử bạn lưu category là key và timestamp là value trong node viewed_categories
                    recentCategories.add(categorySnapshot.getKey());
                }

                if (recentCategories.isEmpty()) {
                    Log.d("HomeFragment", "No recent viewing history found for user " + userId);
                    Toast.makeText(getContext(), "Chưa có lịch sử duyệt để cá nhân hóa.", Toast.LENGTH_SHORT).show();
                    // Fallback: Nếu không có lịch sử, có thể hiển thị các item phổ biến hoặc ngẫu nhiên
                    fetchTrendingItemsForPersonalizationFallback();
                    return;
                }

                Log.d("HomeFragment", "Recent categories for " + userId + ": " + recentCategories.toString());
                fetchItemsByCategories(new ArrayList<>(recentCategories), personalizedItemsAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomeFragment", "Failed to load user history: " + error.getMessage());
                Toast.makeText(getContext(), "Lỗi khi tải lịch sử duyệt web.", Toast.LENGTH_SHORT).show();
                fetchTrendingItemsForPersonalizationFallback(); // Fallback nếu lỗi
            }
        });
    }

    // Phương thức fallback khi không có lịch sử duyệt
    private void fetchTrendingItemsForPersonalizationFallback() {
        // Lấy một số item phổ biến nhất để hiển thị trong mục "Dành riêng cho bạn" nếu không có lịch sử
        DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("items");
        itemsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Item> trendingItems = new ArrayList<>();
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    Item item = itemSnapshot.getValue(Item.class);
                    // Chỉ lấy các item còn "Available"
                    if (item != null && item.getStatus().equals("Available")) {
                        item.setId(itemSnapshot.getKey()); // Set the ID from the snapshot key
                        trendingItems.add(item);
                    }
                }

                // Lọc và sắp xếp các item theo rating (tương tự fetchTrendingItems)
                trendingItems.removeIf(item -> item.getRating_count() == null || item.getRating_count() <= 0);

                Collections.sort(trendingItems, (item1, item2) -> {
                    int ratingComparison = Double.compare(item2.getAverage_rating(), item1.getAverage_rating());
                    if (ratingComparison != 0) {
                        return ratingComparison;
                    }
                    return Long.compare(item2.getRating_count(), item1.getRating_count());
                });

                List<Item> topTrendingItems = trendingItems.subList(0, Math.min(trendingItems.size(), 5)); // Lấy top 5 cho fallback
                personalizedItemsAdapter.setItems(topTrendingItems);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomeFragment", "Failed to load fallback trending items for personalization: " + error.getMessage());
                personalizedItemsAdapter.setItems(new ArrayList<>()); // Đặt rỗng nếu lỗi
            }
        });
    }


    private void fetchItemsByCategories(List<String> categories, ItemAdapter adapter) {
        if (categories.isEmpty()) {
            adapter.setItems(new ArrayList<>());
            return;
        }

        DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("items");
        List<Item> resultItems = new ArrayList<>();
        // Sử dụng AtomicInteger để đếm số lượng category đã xử lý
        AtomicInteger categoriesProcessed = new AtomicInteger(0);

        for (String category : categories) {
            itemsRef.orderByChild("category").equalTo(category).limitToFirst(5).addListenerForSingleValueEvent(new ValueEventListener() { // Lấy 5 item mỗi category
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                        Item item = itemSnapshot.getValue(Item.class);
                        if (item != null && item.getStatus().equals("Available")) {
                            item.setId(itemSnapshot.getKey());
                            resultItems.add(item);
                        }
                    }
                    if (categoriesProcessed.incrementAndGet() == categories.size()) {
                        // Trộn các item từ các danh mục khác nhau và loại bỏ trùng lặp nếu có
                        // Có thể thêm logic sắp xếp nếu cần
                        Set<Item> uniqueItems = new HashSet<>(resultItems); // Loại bỏ trùng lặp
                        List<Item> finalItems = new ArrayList<>(uniqueItems);
                        Collections.shuffle(finalItems); // Xáo trộn để có sự đa dạng
                        adapter.setItems(finalItems);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("HomeFragment", "Failed to load items for category " + category + ": " + error.getMessage());
                    if (categoriesProcessed.incrementAndGet() == categories.size()) {
                        Set<Item> uniqueItems = new HashSet<>(resultItems);
                        List<Item> finalItems = new ArrayList<>(uniqueItems);
                        Collections.shuffle(finalItems);
                        adapter.setItems(finalItems);
                    }
                }
            });
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchUserLocationAndItemsNearYou();
            } else {
                Toast.makeText(getContext(), "Quyền truy cập vị trí bị từ chối. Không thể hiển thị sản phẩm gần bạn.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (cancellationTokenSource != null) {
            cancellationTokenSource.cancel();
        }
    }
}