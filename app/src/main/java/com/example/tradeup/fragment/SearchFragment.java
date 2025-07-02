// SearchFragment.java (đã cập nhật định vị + nhập địa chỉ thủ công + sắp xếp theo khoảng cách)

package com.example.tradeup.fragment;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location; // Ensure this is android.location.Location
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup.R;
import com.example.tradeup.adapter.ItemAdapter;
import com.example.tradeup.model.Item;
import com.google.android.gms.location.*; // For FusedLocationProviderClient, CurrentLocationRequest, Priority
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.*;

import java.io.IOException;
import java.util.*;

public class SearchFragment extends Fragment {

    private TextInputEditText etSearchKeyword, etMinPrice, etMaxPrice, etSearchDistance, etLocation;
    private Spinner spinnerSearchCategory, spinnerSearchCondition, spinnerSortOption;
    private RecyclerView rvSearchResults;
    private TextView tvCurrentLocation;
    private View advancedFilterSection;
    private ImageView ivSearchIcon, ivToggleAdvanced;
    private View btnPerformSearch;
    private Button btnGetLocationGps;

    private NavController navController;

    private double currentLat = 0.0;
    private double currentLng = 0.0;

    private final List<Item> itemList = new ArrayList<>();
    private ItemAdapter itemAdapter;
    private final Handler searchHandler = new Handler();
    private Runnable searchRunnable;
    private DatabaseReference itemsRef;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        itemsRef = FirebaseDatabase.getInstance().getReference("items");
        if (getActivity() != null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        initViews(view);
        setupSpinners();
        setupRecyclerView();
        setupListeners();

        // Initial location request, which will trigger performSearch() on success
        requestLocationPermission();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called. Performing search.");
        // Ensure search is performed when fragment becomes active,
        // in case initial location request failed or user navigated back.
        performSearch();
    }

    private void initViews(View view) {
        etSearchKeyword = view.findViewById(R.id.et_search_keyword);
        spinnerSearchCategory = view.findViewById(R.id.spinner_search_category);
        spinnerSearchCondition = view.findViewById(R.id.spinner_search_condition);
        spinnerSortOption = view.findViewById(R.id.spinner_sort_option);
        etMinPrice = view.findViewById(R.id.et_min_price);
        etMaxPrice = view.findViewById(R.id.et_max_price);
        etSearchDistance = view.findViewById(R.id.et_search_distance);
        etLocation = view.findViewById(R.id.et_location);
        rvSearchResults = view.findViewById(R.id.rv_search_results);
        tvCurrentLocation = view.findViewById(R.id.tv_current_location);
        advancedFilterSection = view.findViewById(R.id.advanced_filter_section);
        ivSearchIcon = view.findViewById(R.id.iv_search_icon);
        ivToggleAdvanced = view.findViewById(R.id.iv_toggle_advanced);
        btnPerformSearch = view.findViewById(R.id.btn_perform_search);
        btnGetLocationGps = view.findViewById(R.id.btn_get_location_gps);
    }

    private void setupSpinners() {
        if (!isAdded()) return;
        spinnerSearchCategory.setAdapter(ArrayAdapter.createFromResource(requireContext(), R.array.search_categories, android.R.layout.simple_spinner_item));
        spinnerSearchCondition.setAdapter(ArrayAdapter.createFromResource(requireContext(), R.array.search_conditions, android.R.layout.simple_spinner_item));
        spinnerSortOption.setAdapter(ArrayAdapter.createFromResource(requireContext(), R.array.search_sort_options, android.R.layout.simple_spinner_item));

        spinnerSortOption.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                performSearch();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupRecyclerView() {
        if (!isAdded()) return;
        rvSearchResults.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        itemAdapter = new ItemAdapter(requireContext(), itemList, R.layout.item_list_vertical);
        rvSearchResults.setAdapter(itemAdapter);
        rvSearchResults.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.bottom = 24;
                outRect.right = 16;
            }
        });
        itemAdapter.setOnItemClickListener(itemId -> {
            Bundle bundle = new Bundle();
            bundle.putString("itemId", itemId);
            navController.navigate(R.id.action_searchFragment_to_itemDetailFragment, bundle);
        });
    }

    private void setupListeners() {
        ivToggleAdvanced.setVisibility(View.VISIBLE);

        ivToggleAdvanced.setOnClickListener(v -> {
            boolean expanded = advancedFilterSection.getVisibility() == View.VISIBLE;
            advancedFilterSection.setVisibility(expanded ? View.GONE : View.VISIBLE);
            ivToggleAdvanced.setImageResource(expanded ? R.drawable.ic_expand_more : R.drawable.ic_expand_less);
        });

        btnPerformSearch.setOnClickListener(v -> performSearch());
        btnGetLocationGps.setOnClickListener(v -> requestLocationPermission());

        etSearchKeyword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> performSearch();
                searchHandler.postDelayed(searchRunnable, 200);
            }
        });

        etLocation.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {
                updateLocationFromAddressInput(s.toString());
                searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> performSearch();
                searchHandler.postDelayed(searchRunnable, 500);
            }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    private void requestLocationPermission() {
        if (!isAdded()) return;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentAccurateLocation();
        } else {
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isAdded()) {
                    if (isGranted) getCurrentAccurateLocation();
                    else Toast.makeText(requireContext(), getString(R.string.permission_location_required), Toast.LENGTH_SHORT).show();
                }
            }).launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void getCurrentAccurateLocation() {
        if (!isAdded()) return;
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), getString(R.string.permission_location_missing), Toast.LENGTH_SHORT).show();
            return;
        }

        CurrentLocationRequest currentLocationRequest = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setDurationMillis(30000)
                .build();

        tvCurrentLocation.setText(getString(R.string.current_location_loading));
        fusedLocationClient.getCurrentLocation(currentLocationRequest, new CancellationTokenSource().getToken())
                .addOnSuccessListener(requireActivity(), location -> {
                    if (isAdded()) {
                        if (location != null) {
                            currentLat = location.getLatitude();
                            currentLng = location.getLongitude();
                            getAddressFromLocation(location);
                            Log.d("SearchFragment", "getCurrentLocation successful: Lat=" + location.getLatitude() + ", Lng=" + location.getLongitude());
                            performSearch();
                        } else {
                            Log.w("SearchFragment", "getCurrentLocation returned null location.");
                            tvCurrentLocation.setText(getString(R.string.current_location_unknown));
                            Toast.makeText(requireContext(), getString(R.string.cannot_get_current_location), Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Log.e("SearchFragment", "Failed to get current location: " + e.getMessage());
                        tvCurrentLocation.setText(getString(R.string.current_location_gps_error));
                        Toast.makeText(requireContext(), getString(R.string.error_getting_current_location, e.getMessage()), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateLocationFromAddressInput(String addressText) {
        if (!isAdded()) return;
        if (addressText.isEmpty()) {
            currentLat = 0.0;
            currentLng = 0.0;
            tvCurrentLocation.setText(getString(R.string.current_location_not_set));
            return;
        }

        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(addressText, 1);
            if (isAdded()) {
                if (addresses != null && !addresses.isEmpty()) {
                    Address firstAddress = addresses.get(0);
                    currentLat = firstAddress.getLatitude();
                    currentLng = firstAddress.getLongitude();
                    tvCurrentLocation.setText(getString(R.string.search_location, addressText));
                    Log.d(TAG, "Location updated from manual input: Lat=" + currentLat + ", Lng=" + currentLng);
                } else {
                    tvCurrentLocation.setText(getString(R.string.search_location_not_found));
                    currentLat = 0.0;
                    currentLng = 0.0;
                }
            }
        } catch (IOException e) {
            if (isAdded()) {
                Log.e(TAG, "Error getting coordinates from manual address: " + e.getMessage());
                tvCurrentLocation.setText(getString(R.string.search_location_geocoder_error));
                currentLat = 0.0;
                currentLng = 0.0;
            }
        }
    }

    private void getAddressFromLocation(Location location) {
        if (!isAdded()) return;
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (isAdded()) {
                if (addresses != null && !addresses.isEmpty()) {
                    String address = addresses.get(0).getAddressLine(0);
                    etLocation.setText(address);
                    tvCurrentLocation.setText(getString(R.string.current_location, address));
                } else {
                    String coords = String.format(Locale.getDefault(), "Lat: %.6f, Lng: %.6f", location.getLatitude(), location.getLongitude());
                    etLocation.setText(coords);
                    tvCurrentLocation.setText(getString(R.string.current_location_coords_not_found, coords));
                    Toast.makeText(requireContext(), getString(R.string.address_not_found_for_location), Toast.LENGTH_SHORT).show();
                }
            }
        } catch (IOException e) {
            if (isAdded()) {
                Log.e("Geocoder", "Error getting address from GPS", e);
                String coords = String.format(Locale.getDefault(), "Lat: %.6f, Lng: %.6f", location.getLatitude(), location.getLongitude());
                etLocation.setText(coords);
                tvCurrentLocation.setText(getString(R.string.current_location_coords_geocoder_error, coords));
                Toast.makeText(requireContext(), getString(R.string.error_getting_address_from_gps), Toast.LENGTH_LONG).show();
            }
        }
    }

    // Helper method to translate Vietnamese conditions to English for comparison
    private String getEnglishCondition(String firebaseCondition) {
        if (firebaseCondition == null) return null;
        String lowerCaseCondition = firebaseCondition.toLowerCase(Locale.ROOT);
        switch (lowerCaseCondition) {
            case "mới": return "new";
            case "đã sử dụng": return "used";
            case "như mới": return "like new";
            case "tốt": return "good"; // Added mapping for "Tốt"
            case "khá": return "fair"; // Added mapping for "Khá"
            // Add more mappings if your Firebase has other Vietnamese conditions
            default: return lowerCaseCondition; // Return as is if no specific mapping
        }
    }

    private void performSearch() {
        if (!isAdded()) return;

        Log.d(TAG, "performSearch: starting search...");

        String keyword = Objects.requireNonNull(etSearchKeyword.getText()).toString().trim().toLowerCase();
        String selectedCategory = spinnerSearchCategory.getSelectedItem().toString();
        String selectedCondition = spinnerSearchCondition.getSelectedItem().toString().trim(); // Trim selected condition
        String selectedSort = spinnerSortOption.getSelectedItem().toString();

        long min = -1, max = -1;
        double dist = -1;

        try {
            if (!etMinPrice.getText().toString().trim().isEmpty()) min = Long.parseLong(etMinPrice.getText().toString().trim());
            if (!etMaxPrice.getText().toString().trim().isEmpty()) max = Long.parseLong(etMaxPrice.getText().toString().trim());
            if (!etSearchDistance.getText().toString().trim().isEmpty()) dist = Double.parseDouble(etSearchDistance.getText().toString().trim());
        } catch (NumberFormatException e) {
            if (isAdded()) {
                Toast.makeText(requireContext(), getString(R.string.invalid_price_distance), Toast.LENGTH_SHORT).show();
            }
            return;
        }

        final long finalMin = min;
        final long finalMax = max;
        final double finalDist = dist;
        final double searchLat = currentLat;
        final double searchLng = currentLng;

        String allCategories = getString(R.string.all_categories);
        String allConditions = getString(R.string.all_conditions).trim(); // Get and trim the string resource for "All Conditions"
        Log.d(TAG, "performSearch: allConditions string from resources (trimmed) = '" + allConditions + "'");
        Log.d(TAG, "performSearch: selectedCondition from spinner (trimmed) = '" + selectedCondition + "'");


        boolean hasSearchOrigin = (searchLat != 0.0 || searchLng != 0.0);
        if (finalDist != -1 && !hasSearchOrigin) {
            if (isAdded()) {
                Toast.makeText(requireContext(), getString(R.string.toast_location_required_for_distance_filter), Toast.LENGTH_LONG).show();
            }
            itemList.clear();
            itemAdapter.setItems(itemList);
            return;
        }

        Query query = selectedCategory.equals(allCategories) ? itemsRef : itemsRef.orderByChild("category").equalTo(selectedCategory);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                Log.d(TAG, "onDataChange: snapshot size = " + snapshot.getChildrenCount());
                itemList.clear();
                List<Item> filtered = new ArrayList<>();

                for (DataSnapshot snap : snapshot.getChildren()) {
                    Item item = snap.getValue(Item.class);
                    if (item == null) continue;

                    item.setId(snap.getKey());
                    Log.d(TAG, "onDataChange: processing item " + item.getTitle());

                    boolean matchKeyword = keyword.isEmpty() ||
                            (item.getTitle() != null && item.getTitle().toLowerCase().contains(keyword)) ||
                            (item.getDescription() != null && item.getDescription().toLowerCase().contains(keyword));

                    boolean matchCondition;
                    // Use equalsIgnoreCase and ensure both are trimmed for robust comparison
                    if (selectedCondition.equalsIgnoreCase(allConditions)) {
                        matchCondition = true; // If "All Conditions" is selected, always match
                    } else {
                        // Translate item's condition to English for comparison
                        String itemConditionEnglish = getEnglishCondition(item.getCondition());
                        // Compare translated item condition with selected spinner condition (both lowercased)
                        matchCondition = (itemConditionEnglish != null && itemConditionEnglish.equals(selectedCondition.toLowerCase(Locale.ROOT)));
                    }

                    // Log the values for debugging condition filter
                    Log.d(TAG, "Item: " + item.getTitle() + " | Item Condition (Firebase): '" + (item.getCondition() != null ? item.getCondition() : "null") +
                            "' | Item Condition (English): '" + (getEnglishCondition(item.getCondition()) != null ? getEnglishCondition(item.getCondition()) : "null") +
                            "' | Selected Condition (Spinner): '" + selectedCondition + "' | Match Condition: " + matchCondition);


                    boolean matchPrice = item.getPrice() != null &&
                            (finalMin == -1 || item.getPrice() >= finalMin) &&
                            (finalMax == -1 || item.getPrice() <= finalMax);

                    boolean matchDistance = true;
                    item.setDistanceToUser(-1);

                    boolean itemHasLocation = (item.getLocation() != null && item.getLocation().getLat() != null && item.getLocation().getLng() != null);

                    if (itemHasLocation && hasSearchOrigin) {
                        float[] result = new float[1];
                        Location.distanceBetween(searchLat, searchLng, item.getLocation().getLat(), item.getLocation().getLng(), result);
                        double km = result[0] / 1000.0;
                        item.setDistanceToUser(km);
                        if (finalDist != -1) {
                            matchDistance = km <= finalDist;
                        }
                    } else if (finalDist != -1) {
                        matchDistance = false;
                    }

                    Log.d(TAG, "Item: " + item.getTitle() + ", Keyword: " + matchKeyword + ", Condition: " + matchCondition +
                            ", Price: " + matchPrice + ", Distance: " + matchDistance + " (ItemHasLoc: " + itemHasLocation +
                            ", HasSearchOrigin: " + hasSearchOrigin + ", FinalDist: " + finalDist + ", ItemDist: " + item.getDistanceToUser() + ")");


                    if (matchKeyword && matchCondition && matchPrice && matchDistance) {
                        filtered.add(item);
                        Log.d(TAG, "Item ADDED to filtered list: " + item.getTitle()); // New log for added items
                    } else {
                        Log.d(TAG, "Item NOT ADDED to filtered list: " + item.getTitle() +
                                " (Keyword: " + matchKeyword + ", Condition: " + matchCondition +
                                ", Price: " + matchPrice + ", Distance: " + matchDistance + ")"); // New log for not added items
                    }
                }

                // Sort by selected option
                switch (selectedSort) {
                    case "Newest":
                        filtered.sort((a, b) -> {
                            if (a.getCreated_at() == null || b.getCreated_at() == null) return 0;
                            return b.getCreated_at().compareTo(a.getCreated_at());
                        });
                        break;
                    case "Price: Low to High":
                        filtered.sort(Comparator.comparingLong(item -> item.getPrice() != null ? item.getPrice() : 0L));
                        break;
                    case "Price: High to Low":
                        filtered.sort((a, b) -> {
                            Long priceA = a.getPrice() != null ? a.getPrice() : 0L;
                            Long priceB = b.getPrice() != null ? b.getPrice() : 0L;
                            return Long.compare(priceB, priceA);
                        });
                        break;
                    case "Distance (Nearest)":
                        filtered.sort(Comparator.comparingDouble(item -> {
                            double d = item.getDistanceToUser();
                            return d >= 0 ? d : Double.MAX_VALUE;
                        }));
                        break;
                }

                itemList.addAll(filtered);
                itemAdapter.setItems(itemList);
                Log.d(TAG, "onDataChange: final itemList size = " + itemList.size());
                Log.d(TAG, "onDataChange: adapter notified of changes");
                Toast.makeText(requireContext(), getString(R.string.found_items, itemList.size()), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), getString(R.string.error_firebase_data_cancelled, error.getMessage()), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
