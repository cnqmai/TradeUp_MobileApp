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
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradeup.R;
import com.example.tradeup.model.Item; // Import Item model
import com.example.tradeup.model.Offer; // Import Offer model
import com.example.tradeup.utils.FirebaseHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class OfferHistoryFragment extends Fragment {

    private static final String TAG = "OfferHistoryFragment";

    private RecyclerView recyclerView;
    private OfferHistoryAdapter adapter;
    private TextView textNoOfferHistory;
    private ImageView ivBackButton;

    private NavController navController;
    private FirebaseHelper firebaseHelper;
    private List<Offer> offerList; // List to hold offer objects

    public OfferHistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseHelper = new FirebaseHelper(requireContext());
        offerList = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_offer_history, container, false);
        initViews(view);
        setupListeners();
        setupRecyclerView();
        loadOfferHistory();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
    }

    private void initViews(View view) {
        ivBackButton = view.findViewById(R.id.iv_back_button_offer_history);
        recyclerView = view.findViewById(R.id.recycler_offer_history);
        textNoOfferHistory = view.findViewById(R.id.text_no_offer_history);
    }

    private void setupListeners() {
        ivBackButton.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigateUp();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new OfferHistoryAdapter(offerList, new OfferHistoryAdapter.OnOfferClickListener() {
            @Override
            public void onOfferClick(Offer offer) {
                // Navigate to ItemDetailFragment or OfferDetailFragment
                Toast.makeText(getContext(), "Offer Clicked for Item: " + offer.getItem_id(), Toast.LENGTH_SHORT).show();
                if (navController != null && offer.getItem_id() != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("itemId", offer.getItem_id());
                    // Assuming action_offerHistoryFragment_to_itemDetailFragment exists in nav_graph
                    navController.navigate(R.id.action_offerHistoryFragment_to_itemDetailFragment, bundle);
                }
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadOfferHistory() {
        String currentUserId = firebaseHelper.getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Người dùng chưa đăng nhập.", Toast.LENGTH_SHORT).show();
            updateEmptyState();
            return;
        }

        // 1. Get offer IDs from offer_history for current user
        DatabaseReference offerHistoryRef = FirebaseDatabase.getInstance().getReference("offer_history").child(currentUserId);
        offerHistoryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded()) {
                    offerList.clear();
                    List<String> offerIds = new ArrayList<>();
                    for (DataSnapshot idSnapshot : snapshot.getChildren()) {
                        if (Boolean.TRUE.equals(idSnapshot.getValue(Boolean.class))) { // Ensure it's marked as true
                            offerIds.add(idSnapshot.getKey()); // Get offer ID from key
                        }
                    }
                    Log.d(TAG, "Found " + offerIds.size() + " offer history entries.");

                    if (offerIds.isEmpty()) {
                        updateEmptyState();
                        return;
                    }

                    // 2. Fetch details for each offer ID from 'offers' node
                    DatabaseReference offersRef = FirebaseDatabase.getInstance().getReference("offers");
                    // Use a counter to know when all offers have been fetched
                    final int[] offersFetchedCount = {0};
                    for (String offerId : offerIds) {
                        offersRef.child(offerId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot offerSnapshot) {
                                if (isAdded()) {
                                    Offer offer = offerSnapshot.getValue(Offer.class);
                                    if (offer != null) {
                                        // Set the offer ID from the key
                                        offer.setOffer_id(offerSnapshot.getKey()); // FIX: Use setOffer_id()
                                        // Ensure this offer is either made by or received by the current user
                                        if (Objects.equals(offer.getBuyer_id(), currentUserId) || Objects.equals(offer.getSeller_id(), currentUserId)) {
                                            offerList.add(offer);
                                        }
                                    }
                                    offersFetchedCount[0]++;
                                    // Check if all offers have been fetched
                                    if (offersFetchedCount[0] == offerIds.size()) {
                                        adapter.notifyDataSetChanged();
                                        updateEmptyState();
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "Failed to load offer details for ID " + offerId + ": " + error.getMessage());
                                if (isAdded()) {
                                    Toast.makeText(getContext(), "Lỗi khi tải chi tiết đề nghị: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                                offersFetchedCount[0]++;
                                if (offersFetchedCount[0] == offerIds.size()) {
                                    adapter.notifyDataSetChanged();
                                    updateEmptyState();
                                }
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load offer history IDs: " + error.getMessage());
                if (isAdded()) {
                    Toast.makeText(getContext(), "Lỗi khi tải lịch sử đề nghị: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                }
            }
        });
    }

    private void updateEmptyState() {
        if (isAdded()) {
            if (offerList.isEmpty()) {
                textNoOfferHistory.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                textNoOfferHistory.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    // Adapter for the RecyclerView
    private static class OfferHistoryAdapter extends RecyclerView.Adapter<OfferHistoryAdapter.OfferViewHolder> {

        private List<Offer> offers;
        private OnOfferClickListener listener;
        private DatabaseReference itemsRef; // Reference to items node to get item details

        public interface OnOfferClickListener {
            void onOfferClick(Offer offer);
        }

        public OfferHistoryAdapter(List<Offer> offers, OnOfferClickListener listener) {
            this.offers = offers;
            this.listener = listener;
            this.itemsRef = FirebaseDatabase.getInstance().getReference("items");
        }

        @NonNull
        @Override
        public OfferViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_offer_history, parent, false);
            return new OfferViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull OfferViewHolder holder, int position) {
            Offer offer = offers.get(position);
            holder.bind(offer, listener, itemsRef);
        }

        @Override
        public int getItemCount() {
            return offers.size();
        }

        static class OfferViewHolder extends RecyclerView.ViewHolder {
            ImageView ivOfferItemThumbnail;
            TextView tvOfferItemTitle, tvOfferPrice, tvOfferStatus, tvOfferDate;
            ImageView ivViewOfferDetails;

            public OfferViewHolder(@NonNull View itemView) {
                super(itemView);
                ivOfferItemThumbnail = itemView.findViewById(R.id.iv_offer_item_thumbnail);
                tvOfferItemTitle = itemView.findViewById(R.id.tv_offer_item_title);
                tvOfferPrice = itemView.findViewById(R.id.tv_offer_price);
                tvOfferStatus = itemView.findViewById(R.id.tv_offer_status);
                tvOfferDate = itemView.findViewById(R.id.tv_offer_date);
                ivViewOfferDetails = itemView.findViewById(R.id.iv_view_offer_details);
            }

            public void bind(final Offer offer, final OnOfferClickListener listener, DatabaseReference itemsRef) {
                // Display offer price
                NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                currencyFormatter.setMaximumFractionDigits(0);
                // FIX: Check for counter_price and display it if available, otherwise offer_price
                Long priceToDisplay = offer.getCounter_price() != null ? offer.getCounter_price() : offer.getOffer_price();
                tvOfferPrice.setText("Giá đề nghị: " + currencyFormatter.format(priceToDisplay) + " VNĐ");

                // Display offer status
                tvOfferStatus.setText("Trạng thái: " + offer.getStatus());

                // Format offer date
                long timestampMillis = 0;
                try {
                    // Assuming created_at is in ISO 8601 format like "yyyy-MM-dd'T'HH:mm:ss'Z'"
                    timestampMillis = OffsetDateTime.parse(offer.getCreated_at()).toInstant().toEpochMilli();
                } catch (DateTimeParseException e) {
                    Log.e(TAG, "Error parsing offer created_at string: " + offer.getCreated_at(), e);
                    timestampMillis = System.currentTimeMillis(); // Fallback
                }
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                String formattedDate = sdf.format(new Date(timestampMillis));
                tvOfferDate.setText("Ngày đề nghị: " + formattedDate);

                // Load item details (title and thumbnail)
                if (offer.getItem_id() != null) {
                    itemsRef.child(offer.getItem_id()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Item item = snapshot.getValue(Item.class);
                            if (item != null) {
                                tvOfferItemTitle.setText(item.getTitle());
                                if (item.getPhotos() != null && !item.getPhotos().isEmpty()) {
                                    Glide.with(itemView.getContext())
                                            .load(item.getPhotos().get(0))
                                            .placeholder(R.drawable.img_placeholder)
                                            .error(R.drawable.img_error)
                                            .into(ivOfferItemThumbnail);
                                } else {
                                    ivOfferItemThumbnail.setImageResource(R.drawable.img_placeholder);
                                }
                            } else {
                                tvOfferItemTitle.setText("Tin đăng không tìm thấy");
                                ivOfferItemThumbnail.setImageResource(R.drawable.img_placeholder);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Failed to load item for offer " + offer.getOffer_id() + ": " + error.getMessage()); // FIX: Use getOffer_id()
                            tvOfferItemTitle.setText("Lỗi tải tin đăng");
                            ivOfferItemThumbnail.setImageResource(R.drawable.img_error);
                        }
                    });
                } else {
                    tvOfferItemTitle.setText("Không có ID tin đăng");
                    ivOfferItemThumbnail.setImageResource(R.drawable.img_placeholder);
                }

                itemView.setOnClickListener(v -> listener.onOfferClick(offer));
            }
        }
    }
}
