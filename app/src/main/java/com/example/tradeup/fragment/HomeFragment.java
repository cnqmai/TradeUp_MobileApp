package com.example.tradeup.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import com.example.tradeup.model.Location;
import com.example.tradeup.utils.FirebaseHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.auth.FirebaseAuth;
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeFragment extends Fragment {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String TAG = "HomeFragment";

    private RecyclerView rvPopularItems, rvItemsNearYou, rvCategories, rvPersonalizedItems;
    private ItemAdapter popularItemsAdapter, itemsNearYouAdapter, personalizedItemsAdapter;
    private CategoryAdapter categoryAdapter;
    private FirebaseHelper firebaseHelper;
    private NavController navController;
    private ImageView ivNotificationBellHome;

    private FusedLocationProviderClient fusedLocationClient;
    private CancellationTokenSource cancellationTokenSource;
    private FirebaseAuth mAuth;

    private ExecutorService backgroundExecutor;

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
        mAuth = FirebaseAuth.getInstance();

        backgroundExecutor = Executors.newSingleThreadExecutor();

        // Initialize RecyclerViews
        rvCategories = view.findViewById(R.id.rvCategories);
        rvItemsNearYou = view.findViewById(R.id.rvItemsNearYou);
        rvPopularItems = view.findViewById(R.id.rvPopularItems);
        rvPersonalizedItems = view.findViewById(R.id.rvPersonalizedItems);

        // Setup Adapters
        categoryAdapter = new CategoryAdapter(getContext(), new ArrayList<>());
        itemsNearYouAdapter = new ItemAdapter(getContext(), new ArrayList<>());
        popularItemsAdapter = new ItemAdapter(getContext(), new ArrayList<>());
        personalizedItemsAdapter = new ItemAdapter(getContext(), new ArrayList<>());

        // Set Layout Managers and Adapters
        rvCategories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(categoryAdapter);

        rvItemsNearYou.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvItemsNearYou.setAdapter(itemsNearYouAdapter);

        rvPopularItems.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvPopularItems.setAdapter(popularItemsAdapter);

        rvPersonalizedItems.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvPersonalizedItems.setAdapter(personalizedItemsAdapter);

        // Initialize ImageView
        ivNotificationBellHome = view.findViewById(R.id.iv_notification_bell_home);

        // Set OnClickListener for ImageView
        ivNotificationBellHome.setOnClickListener(v -> {
            navController.navigate(R.id.action_homeFragment_to_notificationFragment);
        });

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

        personalizedItemsAdapter.setOnItemClickListener(itemId -> {
            Bundle bundle = new Bundle();
            bundle.putString("itemId", itemId);
            navController.navigate(R.id.action_homeFragment_to_itemDetailFragment, bundle);
        });

        // Fetch data
        fetchCategories();
        checkLocationPermissionAndFetchNearYou();
        fetchTrendingItems();
        fetchPersonalizedItems();
    }

    // Cập nhật logic để lấy sản phẩm đang được quan tâm dựa trên rating
    private void fetchTrendingItems() {
        DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("items");
        itemsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Execute heavy data processing on a background thread
                backgroundExecutor.execute(() -> {
                    List<Item> trendingItems = new ArrayList<>();
                    for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                        Item item = itemSnapshot.getValue(Item.class);
                        // Chỉ lấy các item còn "Available" và kiểm tra null cho status
                        if (item != null && item.getStatus() != null && item.getStatus().equals("Available")) {
                            item.setId(itemSnapshot.getKey());
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

                    // Update UI on the main thread
                    requireActivity().runOnUiThread(() -> {
                        popularItemsAdapter.setItems(topTrendingItems);
                    });
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load trending items by rating: " + error.getMessage());
                if (isAdded()) { // Check if fragment is still attached
                    Toast.makeText(getContext(), "Không thể tải sản phẩm đang được quan tâm.", Toast.LENGTH_SHORT).show();
                }
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
                        // Execute on background thread if processing is heavy
                        backgroundExecutor.execute(() -> {
                            List<Item> orderedItems = new ArrayList<>();
                            for (String id : itemIds) {
                                if (fetchedItemMap.containsKey(id)) {
                                    orderedItems.add(fetchedItemMap.get(id));
                                }
                            }
                            // Update UI on the main thread
                            requireActivity().runOnUiThread(() -> {
                                adapter.setItems(orderedItems);
                            });
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to load item by ID: " + error.getMessage());
                    if (fetchedCount.incrementAndGet() == itemIds.size()) { // Still increment to ensure completion check works
                        // Even if some failed, update UI on main thread with what was fetched
                        backgroundExecutor.execute(() -> {
                            List<Item> orderedItems = new ArrayList<>();
                            for (String id : itemIds) {
                                if (fetchedItemMap.containsKey(id)) {
                                    orderedItems.add(fetchedItemMap.get(id));
                                }
                            }
                            requireActivity().runOnUiThread(() -> {
                                adapter.setItems(orderedItems);
                            });
                        });
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
                backgroundExecutor.execute(() -> { // Execute on background thread
                    Set<String> categories = new HashSet<>();
                    for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                        Item item = itemSnapshot.getValue(Item.class);
                        if (item != null && item.getCategory() != null && !item.getCategory().isEmpty()) {
                            categories.add(item.getCategory());
                        }
                    }
                    List<String> categoryList = new ArrayList<>(categories);
                    Collections.sort(categoryList); // Sort categories alphabetically

                    requireActivity().runOnUiThread(() -> { // Update UI on the main thread
                        categoryAdapter.setCategories(categoryList);
                    });
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load categories: " + error.getMessage());
                if (isAdded()) {
                    Toast.makeText(getContext(), "Không thể tải danh mục.", Toast.LENGTH_SHORT).show();
                }
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
            if (isAdded()) { // Check if fragment is still attached
                Toast.makeText(getContext(), "Ứng dụng không có quyền truy cập vị trí.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        Log.d(TAG, "User location: Lat=" + location.getLatitude() + ", Lng=" + location.getLongitude());
                        fetchItemsNearYou(location);
                    } else {
                        Log.w(TAG, "Could not get last location. Trying to request new location updates.");
                        requestNewLocationUpdates();
                    }
                })
                .addOnFailureListener(requireActivity(), e -> {
                    Log.e(TAG, "Error getting location: " + e.getMessage());
                    if (isAdded()) { // Check if fragment is still attached
                        Toast.makeText(getContext(), "Không thể lấy vị trí hiện tại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
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
                for (android.location.Location location : locationResult.getLocations()) {
                    if (location != null) {
                        Log.d(TAG, "New location: Lat=" + location.getLatitude() + ", Lng=" + location.getLongitude());
                        fetchItemsNearYou(location);
                        fusedLocationClient.removeLocationUpdates(this);
                        return;
                    }
                }
            }
        };

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void fetchItemsNearYou(android.location.Location userLocation) {
        DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("items");
        itemsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                backgroundExecutor.execute(() -> { // Execute on background thread
                    List<Item> nearbyItems = new ArrayList<>();
                    float searchRadiusKm = 10.0f;

                    for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                        Item item = itemSnapshot.getValue(Item.class);
                        if (item != null && item.getLocation() != null && item.getStatus() != null && item.getStatus().equals("Available")) {
                            DataSnapshot locationSnapshot = itemSnapshot.child("location");
                            if (locationSnapshot.child("lat").exists() && locationSnapshot.child("lng").exists()) {
                                android.location.Location itemLocation = new android.location.Location("");
                                itemLocation.setLatitude(item.getLocation().getLat());
                                itemLocation.setLongitude(item.getLocation().getLng());

                                float distanceInMeters = userLocation.distanceTo(itemLocation);
                                float distanceInKm = distanceInMeters / 1000;

                                if (distanceInKm <= searchRadiusKm) {
                                    item.setId(itemSnapshot.getKey());
                                    nearbyItems.add(item);
                                }
                            } else {
                                Log.w(TAG, "Item " + itemSnapshot.getKey() + " is missing lat/lng fields in its location data.");
                            }
                        }
                    }
                    Collections.sort(nearbyItems, (item1, item2) -> {
                        android.location.Location loc1 = new android.location.Location("");
                        loc1.setLatitude(item1.getLocation().getLat());
                        loc1.setLongitude(item1.getLocation().getLng());

                        android.location.Location loc2 = new android.location.Location("");
                        loc2.setLatitude(item2.getLocation().getLat());
                        loc2.setLongitude(item2.getLocation().getLng());

                        float dist1 = userLocation.distanceTo(loc1);
                        float dist2 = userLocation.distanceTo(loc2);
                        return Float.compare(dist1, dist2);
                    });

                    requireActivity().runOnUiThread(() -> { // Update UI on the main thread
                        itemsNearYouAdapter.setItems(nearbyItems);
                    });
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load items near you: " + error.getMessage());
                if (isAdded()) {
                    Toast.makeText(getContext(), "Không thể tải sản phẩm gần bạn.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void fetchPersonalizedItems() {
        String userId = null;
        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }

        // Tạo một biến final mới để sử dụng trong lambda
        final String finalUserId = userId; // <-- Dòng sửa lỗi

        if (finalUserId == null) { // Sử dụng finalUserId
            Log.w(TAG, "User not logged in, cannot fetch personalized items.");
            fetchItemsByIds(new ArrayList<>(), personalizedItemsAdapter);
            return;
        }

        DatabaseReference userHistoryRef = FirebaseDatabase.getInstance().getReference("user_activity").child(finalUserId).child("viewed_categories"); // Sử dụng finalUserId
        userHistoryRef.limitToLast(5).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                backgroundExecutor.execute(() -> {
                    Set<String> recentCategories = new HashSet<>();
                    for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                        recentCategories.add(categorySnapshot.getKey());
                    }

                    if (recentCategories.isEmpty()) {
                        Log.d(TAG, "No recent viewing history found for user " + finalUserId); // Sử dụng finalUserId
                        requireActivity().runOnUiThread(() -> {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Chưa có lịch sử duyệt để cá nhân hóa.", Toast.LENGTH_SHORT).show();
                            }
                        });
                        fetchTrendingItemsForPersonalizationFallback();
                        return;
                    }

                    Log.d(TAG, "Recent categories for " + finalUserId + ": " + recentCategories.toString()); // Sử dụng finalUserId
                    fetchItemsByCategories(new ArrayList<>(recentCategories), personalizedItemsAdapter);
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load user history: " + error.getMessage());
                if (isAdded()) {
                    Toast.makeText(getContext(), "Lỗi khi tải lịch sử duyệt web.", Toast.LENGTH_SHORT).show();
                }
                fetchTrendingItemsForPersonalizationFallback();
            }
        });
    }

    // Phương thức fallback khi không có lịch sử duyệt
    private void fetchTrendingItemsForPersonalizationFallback() {
        DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("items");
        itemsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                backgroundExecutor.execute(() -> {
                    List<Item> trendingItems = new ArrayList<>();
                    for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                        Item item = itemSnapshot.getValue(Item.class);
                        if (item != null && item.getStatus() != null && item.getStatus().equals("Available")) {
                            item.setId(itemSnapshot.getKey());
                            trendingItems.add(item);
                        }
                    }

                    trendingItems.removeIf(item -> item.getRating_count() == null || item.getRating_count() <= 0);

                    Collections.sort(trendingItems, (item1, item2) -> {
                        int ratingComparison = Double.compare(item2.getAverage_rating(), item1.getAverage_rating());
                        if (ratingComparison != 0) {
                            return ratingComparison;
                        }
                        return Long.compare(item2.getRating_count(), item1.getRating_count());
                    });

                    List<Item> topTrendingItems = trendingItems.subList(0, Math.min(trendingItems.size(), 5));

                    requireActivity().runOnUiThread(() -> {
                        personalizedItemsAdapter.setItems(topTrendingItems);
                    });
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load fallback trending items for personalization: " + error.getMessage());
                requireActivity().runOnUiThread(() -> {
                    personalizedItemsAdapter.setItems(new ArrayList<>());
                });
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
        AtomicInteger categoriesProcessed = new AtomicInteger(0);

        for (String category : categories) {
            itemsRef.orderByChild("category").equalTo(category).limitToFirst(5).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                        Item item = itemSnapshot.getValue(Item.class);
                        if (item != null && item.getStatus() != null && item.getStatus().equals("Available")) {
                            item.setId(itemSnapshot.getKey());
                            resultItems.add(item);
                        }
                    }
                    if (categoriesProcessed.incrementAndGet() == categories.size()) {
                        backgroundExecutor.execute(() -> {
                            Set<Item> uniqueItems = new HashSet<>(resultItems);
                            List<Item> finalItems = new ArrayList<>(uniqueItems);
                            Collections.shuffle(finalItems);

                            requireActivity().runOnUiThread(() -> {
                                adapter.setItems(finalItems);
                            });
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to load items for category " + category + ": " + error.getMessage());
                    if (categoriesProcessed.incrementAndGet() == categories.size()) {
                        backgroundExecutor.execute(() -> {
                            Set<Item> uniqueItems = new HashSet<>(resultItems);
                            List<Item> finalItems = new ArrayList<>(uniqueItems);
                            Collections.shuffle(finalItems);

                            requireActivity().runOnUiThread(() -> {
                                adapter.setItems(finalItems);
                            });
                        });
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
                if (isAdded()) {
                    Toast.makeText(getContext(), "Quyền truy cập vị trí bị từ chối. Không thể hiển thị sản phẩm gần bạn.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (cancellationTokenSource != null) {
            cancellationTokenSource.cancel();
        }
        // Shut down the executor when the fragment is stopped to prevent memory leaks
        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdownNow();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Ensure executor is shut down if not already
        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdownNow();
        }
    }
}
