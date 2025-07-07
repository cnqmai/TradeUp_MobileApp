package com.example.tradeup.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat; // Added for color tinting
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.tradeup.R;
import com.example.tradeup.model.Notification;
import com.example.tradeup.model.Offer;
import com.example.tradeup.model.Item; // Assuming your Item model
import com.example.tradeup.model.Transaction; // Assuming your Transaction model
import com.example.tradeup.model.User; // Assuming your User model
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.Query; // Added for notification query
import com.google.firebase.database.ServerValue; // Add this import

import java.text.NumberFormat;
import java.text.ParseException; // Added for time formatting
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap; // Add this import
import java.util.Locale;
import java.util.Map; // Add this import
import java.util.Objects;
import java.util.concurrent.TimeUnit; // Added for time formatting

import de.hdodenhof.circleimageview.CircleImageView; // Assuming you use CircleImageView

// Import the generated Directions class for this fragment
import com.example.tradeup.fragment.OfferDetailFragmentDirections;


public class OfferDetailFragment extends Fragment {

    private static final String TAG = "OfferDetailFragment";

    // UI elements (updated to match new XML structure and IDs)
    private ImageView ivBackButton;
    private ImageView ivFilterButton; // Renamed from ivMoreOptions
    private TextView tvOfferDetailTitleToolbar; // New ID for toolbar title

    private ImageView ivItemImageOfferDetail; // New ID
    private TextView tvItemTitleOfferDetail; // New ID
    private TextView tvItemOriginalPriceOfferDetail; // New ID
    private TextView tvOfferAmount; // Existing ID, repurposed for prominent offer price
    private TextView tvOfferStatus; // Existing ID, repurposed as status tag

    private CircleImageView ivSenderProfilePicOfferDetail; // New ID, assuming CircleImageView
    private TextView tvBuyerName; // Existing ID, repurposed for sender name
    private TextView tvOfferTimeOfferDetail; // New ID

    private LinearLayout layoutSellerActions; // Existing ID
    private Button btnAcceptOffer; // Existing ID
    private Button btnRejectOffer; // Existing ID
    private Button btnCounterOffer; // Existing ID
    private Button btnBuyerRespondOffer; // Existing ID

    // Data
    private String offerId;
    private DatabaseReference offerRef;
    private ValueEventListener offerValueEventListener;
    private Offer currentOffer;
    private Item offeredItem; // To get item details for display
    private String currentUserId;
    private NavController navController;

    private DatabaseReference notificationsRef;
    private DatabaseReference itemsRef;
    private DatabaseReference usersRef; // To fetch buyer/seller names
    private DatabaseReference transactionsRef; // For creating transactions

    public OfferDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        notificationsRef = FirebaseDatabase.getInstance().getReference("notifications");
        itemsRef = FirebaseDatabase.getInstance().getReference("items");
        usersRef = FirebaseDatabase.getInstance().getReference("users"); // Initialize usersRef
        transactionsRef = FirebaseDatabase.getInstance().getReference("transactions"); // Initialize transactionsRef

        if (getArguments() != null) {
            offerId = getArguments().getString("offerId");
            if (offerId != null) {
                offerRef = FirebaseDatabase.getInstance().getReference("offers").child(offerId);
            } else {
                Log.e(TAG, "Offer ID is null in arguments.");
                if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_error_offer_id_missing), Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "Arguments are null.");
            if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_error_offer_id_missing), Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_offer_detail, container, false);
        initViews(view); // Initialize views here
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        if (offerRef != null) {
            fetchOfferDetails();
        } else {
            if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_error_loading_offer_details), Toast.LENGTH_SHORT).show();
        }

        setupListeners(); // Setup listeners after views are initialized
    }

    private void initViews(View view) {
        // Toolbar elements
        ivBackButton = view.findViewById(R.id.iv_back_button_offer_detail);
        ivFilterButton = view.findViewById(R.id.iv_filter_button_offers_list); // Changed from iv_more_options_offer_detail
        tvOfferDetailTitleToolbar = view.findViewById(R.id.tv_offer_detail_title_toolbar); // New ID

        // Main Offer Card elements
        ivItemImageOfferDetail = view.findViewById(R.id.iv_item_image_offer_detail);
        tvItemTitleOfferDetail = view.findViewById(R.id.tv_item_title_offer_detail);
        tvItemOriginalPriceOfferDetail = view.findViewById(R.id.tv_item_original_price_offer_detail);
        tvOfferAmount = view.findViewById(R.id.tv_offer_amount); // Existing ID, repurposed for prominent offer price
        tvOfferStatus = view.findViewById(R.id.tv_offer_status); // Existing ID, repurposed as status tag

        ivSenderProfilePicOfferDetail = view.findViewById(R.id.iv_sender_profile_pic_offer_detail);
        tvBuyerName = view.findViewById(R.id.tv_buyer_name); // Existing ID, repurposed for sender name (was tv_buyer_name)
        tvOfferTimeOfferDetail = view.findViewById(R.id.tv_offer_time_offer_detail); // New ID

        // Action Buttons
        layoutSellerActions = view.findViewById(R.id.layout_seller_actions);
        btnAcceptOffer = view.findViewById(R.id.btn_accept_offer);
        btnRejectOffer = view.findViewById(R.id.btn_reject_offer);
        btnCounterOffer = view.findViewById(R.id.btn_counter_offer);
        btnBuyerRespondOffer = view.findViewById(R.id.btn_buyer_respond_offer);
    }

    private void setupListeners() {
        ivBackButton.setOnClickListener(v -> navController.navigateUp());
        ivFilterButton.setOnClickListener(v -> {
            if (isAdded()) Toast.makeText(requireContext(), getString(R.string.filter_button_clicked), Toast.LENGTH_SHORT).show();
        });

        btnAcceptOffer.setOnClickListener(v -> {
            if (currentOffer != null && currentOffer.getStatus().equals("pending") && currentOffer.getSeller_id().equals(currentUserId)) {
                handleOfferAction("accepted");
            } else {
                if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_cannot_accept_offer), Toast.LENGTH_SHORT).show();
            }
        });
        btnRejectOffer.setOnClickListener(v -> {
            if (currentOffer != null && currentOffer.getStatus().equals("pending") && currentOffer.getSeller_id().equals(currentUserId)) {
                handleOfferAction("rejected");
            } else {
                if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_cannot_reject_offer), Toast.LENGTH_SHORT).show();
            }
        });
        btnCounterOffer.setOnClickListener(v -> {
            if (currentOffer != null && currentOffer.getStatus().equals("pending") && currentOffer.getSeller_id().equals(currentUserId)) {
                showCounterOfferDialog();
            } else {
                if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_cannot_counter_offer), Toast.LENGTH_SHORT).show();
            }
        });

        btnBuyerRespondOffer.setOnClickListener(v -> {
            if (currentOffer != null && currentOffer.getStatus().equals("countered") && currentOffer.getBuyer_id().equals(currentUserId)) {
                showBuyerRespondDialog();
            } else {
                if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_cannot_respond_offer), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchOfferDetails() {
        offerValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) {
                    Log.w(TAG, "Fragment is not added, skipping UI updates in onDataChange.");
                    return;
                }

                currentOffer = snapshot.getValue(Offer.class);
                if (currentOffer != null) {
                    // Fetch item and sender details based on the current offer
                    fetchItemAndSenderDetails(currentOffer.getItem_id(), currentOffer.getBuyer_id());
                    updateUI(currentOffer); // Update UI with offer-specific data
                } else {
                    if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_offer_not_found), Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Offer data is null for ID: " + offerId);
                    navController.navigateUp();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) {
                    Log.w(TAG, "Fragment is not added, skipping error message in onCancelled.");
                    return;
                }
                Log.e(TAG, "Error loading offer: " + error.getMessage());
                if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_error_loading_offer, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        };
        offerRef.addValueEventListener(offerValueEventListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (offerRef != null && offerValueEventListener != null) {
            offerRef.removeEventListener(offerValueEventListener);
            Log.d(TAG, "Firebase listener removed.");
        }
        // Nullify view references to prevent memory leaks
        ivBackButton = null;
        ivFilterButton = null;
        tvOfferDetailTitleToolbar = null;
        ivItemImageOfferDetail = null;
        tvItemTitleOfferDetail = null;
        tvItemOriginalPriceOfferDetail = null;
        tvOfferAmount = null;
        tvOfferStatus = null;
        ivSenderProfilePicOfferDetail = null;
        tvBuyerName = null;
        tvOfferTimeOfferDetail = null;
        layoutSellerActions = null;
        btnAcceptOffer = null;
        btnRejectOffer = null;
        btnCounterOffer = null;
        btnBuyerRespondOffer = null;
    }

    private void fetchItemAndSenderDetails(String itemId, String senderId) {
        // Fetch Item details
        itemsRef.child(itemId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                offeredItem = snapshot.getValue(Item.class);
                if (offeredItem != null) {
                    tvItemTitleOfferDetail.setText(offeredItem.getTitle());
                    NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
                    currencyFormatter.setMaximumFractionDigits(2);
                    // FIX: Add null check for offeredItem.getPrice()
                    if (offeredItem.getPrice() != null) {
                        tvItemOriginalPriceOfferDetail.setText(getString(R.string.original_price_short, currencyFormatter.format(offeredItem.getPrice())));
                    } else {
                        tvItemOriginalPriceOfferDetail.setText(getString(R.string.original_price_short, getString(R.string.price_not_available_short)));
                    }

                    if (offeredItem.getPhotos() != null && !offeredItem.getPhotos().isEmpty()) {
                        Glide.with(requireContext())
                                .load(offeredItem.getPhotos().get(0))
                                .placeholder(R.drawable.img_placeholder)
                                .error(R.drawable.img_error)
                                .into(ivItemImageOfferDetail);
                    } else {
                        ivItemImageOfferDetail.setImageResource(R.drawable.img_placeholder);
                    }
                } else {
                    Log.e(TAG, "Item not found for offer: " + itemId);
                    tvItemTitleOfferDetail.setText(getString(R.string.item_not_found));
                    tvItemOriginalPriceOfferDetail.setText(getString(R.string.price_not_available));
                    ivItemImageOfferDetail.setImageResource(R.drawable.img_placeholder);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                Log.e(TAG, "Failed to load item details for offer: " + error.getMessage());
                tvItemTitleOfferDetail.setText(getString(R.string.error_loading_item));
                tvItemOriginalPriceOfferDetail.setText(getString(R.string.error_loading_data));
                ivItemImageOfferDetail.setImageResource(R.drawable.img_error);
            }
        });

        // Fetch Sender details (assuming Buyer ID is the sender of the initial offer)
        usersRef.child(senderId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                User sender = snapshot.getValue(User.class);
                if (sender != null) {
                    tvBuyerName.setText(sender.getDisplay_name()); // Repurposed tv_buyer_name for sender's display name
                    if (sender.getProfile_picture_url() != null && !sender.getProfile_picture_url().isEmpty()) {
                        Glide.with(requireContext())
                                .load(sender.getProfile_picture_url())
                                .placeholder(R.drawable.img_profile_placeholder)
                                .error(R.drawable.img_profile_placeholder)
                                .into(ivSenderProfilePicOfferDetail);
                    } else {
                        ivSenderProfilePicOfferDetail.setImageResource(R.drawable.img_placeholder);
                    }
                } else {
                    Log.e(TAG, "Sender details not found for ID: " + senderId);
                    tvBuyerName.setText(getString(R.string.unknown_user));
                    ivSenderProfilePicOfferDetail.setImageResource(R.drawable.img_placeholder);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                Log.e(TAG, "Failed to load sender details: " + error.getMessage());
                tvBuyerName.setText(getString(R.string.error_loading_user));
                ivSenderProfilePicOfferDetail.setImageResource(R.drawable.img_error);
            }
        });
    }

    private void updateUI(Offer offer) {
        if (!isAdded()) return;

        // Update offer price (tv_offer_amount)
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        currencyFormatter.setMaximumFractionDigits(2); // Match image format

        // Display current offer price or counter price
        if (offer.getStatus() != null && offer.getStatus().equals("countered") && offer.getCounter_price() != null) {
            tvOfferAmount.setText(getString(R.string.offer_price_display, currencyFormatter.format(offer.getCounter_price())));
        } else if (offer.getOffer_price() != null) {
            tvOfferAmount.setText(getString(R.string.offer_price_display, currencyFormatter.format(offer.getOffer_price())));
        } else {
            tvOfferAmount.setText(getString(R.string.offer_price_display, getString(R.string.price_not_available_short)));
        }


        // Update offer status tag (tv_offer_status)
        String statusText = offer.getStatus();
        int statusBgDrawable;
        int statusTextColor;

        switch (statusText) {
            case "pending":
                statusBgDrawable = R.drawable.tag_pending; // Use new drawable for pending
                statusTextColor = ContextCompat.getColor(requireContext(), R.color.white);
                break;
            case "accepted":
                statusBgDrawable = R.drawable.tag_approved;
                statusTextColor = ContextCompat.getColor(requireContext(), R.color.white);
                break;
            case "rejected":
                statusBgDrawable = R.drawable.tag_rejected;
                statusTextColor = ContextCompat.getColor(requireContext(), R.color.white);
                break;
            case "countered":
                statusBgDrawable = R.drawable.tag_pending; // Orange for countered
                statusTextColor = ContextCompat.getColor(requireContext(), R.color.white);
                break;
            case "superseded": // Offers that have been superseded by a new counter-offer
                statusBgDrawable = R.drawable.tag_default;
                statusTextColor = ContextCompat.getColor(requireContext(), R.color.black);
                break;
            case "cancelled": // Assuming you might have a cancelled status
                statusBgDrawable = R.drawable.tag_default; // Or a specific cancelled tag
                statusTextColor = ContextCompat.getColor(requireContext(), R.color.black);
                break;
            default:
                statusBgDrawable = R.drawable.tag_default;
                statusTextColor = ContextCompat.getColor(requireContext(), android.R.color.black);
                break;
        }
        tvOfferStatus.setText(statusText.substring(0, 1).toUpperCase() + statusText.substring(1)); // Capitalize first letter
        tvOfferStatus.setBackgroundResource(statusBgDrawable);
        tvOfferStatus.setTextColor(statusTextColor);

        // Update offer time
        tvOfferTimeOfferDetail.setText(formatTimestamp(offer.getUpdated_at()));

        // Determine button visibility based on current user role and offer status
        boolean isSeller = currentUserId.equals(offer.getSeller_id());
        boolean isBuyer = currentUserId.equals(offer.getBuyer_id());
        String status = offer.getStatus(); // Get status again for clarity in this block

        // Reset visibility for all action buttons
        layoutSellerActions.setVisibility(View.GONE);
        btnBuyerRespondOffer.setVisibility(View.GONE);

        if (status != null) {
            switch (status) {
                case "pending":
                    if (isSeller) {
                        layoutSellerActions.setVisibility(View.VISIBLE); // Show Accept, Counter, Reject
                        btnAcceptOffer.setEnabled(true);
                        btnRejectOffer.setEnabled(true);
                        btnCounterOffer.setEnabled(true);
                    } else if (isBuyer) {
                        if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_waiting_seller_response), Toast.LENGTH_SHORT).show();
                    }
                    break;
                case "countered":
                    if (isBuyer) {
                        btnBuyerRespondOffer.setVisibility(View.VISIBLE);
                        btnBuyerRespondOffer.setEnabled(true);
                        if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_seller_countered_offer), Toast.LENGTH_SHORT).show();
                    } else if (isSeller) {
                        if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_toast_counter_offer_sent_waiting_buyer), Toast.LENGTH_SHORT).show();
                    }
                    break;
                case "accepted":
                    // If offer is accepted, and current user is buyer, navigate to payment methods
                    // This navigation is now triggered by createTransactionRecord after successful transaction creation
                    if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_offer_accepted_status), Toast.LENGTH_SHORT).show();
                    break;
                case "rejected":
                    if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_offer_rejected_status), Toast.LENGTH_SHORT).show();
                    break;
                case "superseded":
                    if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_offer_superseded_status), Toast.LENGTH_SHORT).show();
                    break;
                case "cancelled":
                    if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_offer_cancelled_status), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    private String formatTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return "";

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            inputFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));

            Date offerDate = inputFormat.parse(timestamp);
            Date now = new Date();

            long diffMillis = now.getTime() - offerDate.getTime();

            long seconds = TimeUnit.MILLISECONDS.toSeconds(diffMillis);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis);
            long hours = TimeUnit.MILLISECONDS.toHours(diffMillis);
            long days = TimeUnit.MILLISECONDS.toDays(diffMillis);

            if (seconds < 60) {
                return getString(R.string.time_just_now);
            } else if (minutes < 60) {
                return getString(R.string.time_minutes_ago, minutes);
            } else if (hours < 24) {
                return getString(R.string.time_hours_ago, hours);
            } else if (days < 7) {
                return getString(R.string.time_days_ago, days);
            } else {
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                return outputFormat.format(offerDate);
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error formatting timestamp: " + timestamp, e);
            return timestamp;
        }
    }


    private void handleOfferAction(String action) {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not added to context, cannot perform action or show toast.");
            return;
        }

        if (offerRef == null || currentOffer == null) {
            if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_error_cannot_perform_action), Toast.LENGTH_SHORT).show();
            return;
        }

        String updatedTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", action);
        updates.put("updated_at", updatedTimestamp);

        if ("accepted".equals(action) || "rejected".equals(action)) {
            updates.put("counter_price", null); // Clear counter price if accepted or rejected
        }

        offerRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_offer_updated_to, action), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Offer " + offerId + " status updated to " + action);

                    String recipientId = null;
                    // Determine recipient for notification
                    if (currentUserId.equals(currentOffer.getBuyer_id())) { // If current user is buyer, recipient is seller
                        recipientId = currentOffer.getSeller_id();
                    } else if (currentUserId.equals(currentOffer.getSeller_id())) { // If current user is seller, recipient is buyer
                        recipientId = currentOffer.getBuyer_id();
                    }

                    if ("accepted".equals(action)) {
                        // When offer is accepted, we create a transaction record
                        // and then the buyer proceeds to payment.
                        // The item status will be updated to "pending_escrow" or "Sold" after payment.
                        createTransactionRecord(currentOffer); // Create transaction record here
                        upsertOfferNotification(recipientId, currentOffer.getItem_id(), offerId,
                                getString(R.string.notification_title_offer_accepted),
                                getString(R.string.notification_body_offer_accepted),
                                "offer_accepted", true);
                        // Navigation to payment will happen in createTransactionRecord
                    } else if ("rejected".equals(action)) {
                        upsertOfferNotification(recipientId, currentOffer.getItem_id(), offerId,
                                getString(R.string.notification_title_offer_rejected),
                                getString(R.string.notification_body_offer_rejected),
                                "offer_rejected", true);
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_error_updating_offer, e.getMessage()), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating offer " + offerId + ": " + e.getMessage());
                });
    }

    private void showCounterOfferDialog() {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not added to context, cannot show dialog.");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.dialog_title_counter_offer));

        final EditText input = new EditText(requireContext());
        String currentOfferPrice = (currentOffer.getOffer_price() != null) ? String.valueOf(currentOffer.getOffer_price()) : getString(R.string.price_not_available_short);
        input.setHint(getString(R.string.hint_enter_counter_offer_price_with_current, currentOfferPrice));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton(getString(R.string.button_send_counter_offer), (dialog, which) -> {
            String counterPriceStr = input.getText().toString().trim();
            if (counterPriceStr.isEmpty()) {
                if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_please_enter_counter_price), Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                long counterPrice = Long.parseLong(counterPriceStr);
                if (counterPrice <= 0) {
                    if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_counter_price_must_be_greater_than_zero), Toast.LENGTH_SHORT).show();
                    return;
                }
                sendCounterOffer(counterPrice);
            } catch (NumberFormatException e) {
                if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_invalid_counter_price), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(getString(R.string.button_cancel), (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void sendCounterOffer(long counterPrice) {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not added to context, cannot perform action or show toast.");
            return;
        }

        if (offerRef == null || currentOffer == null) {
            if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_error_cannot_counter_offer_generic), Toast.LENGTH_SHORT).show();
            return;
        }

        String updatedTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "countered");
        updates.put("counter_price", counterPrice);
        updates.put("updated_at", updatedTimestamp);

        offerRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Offer " + offerId + " countered with price: " + counterPrice);

                    upsertOfferNotification(currentOffer.getBuyer_id(), currentOffer.getItem_id(), offerId,
                            getString(R.string.notification_title_counter_offer),
                            getString(R.string.notification_body_counter_offer, NumberFormat.getCurrencyInstance(Locale.US).format(counterPrice)),
                            "counter_offer", false);
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_error_sending_counter_offer, e.getMessage()), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error sending counter offer " + offerId + ": " + e.getMessage());
                });
    }

    private void showBuyerRespondDialog() {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not added to context, cannot show dialog.");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.dialog_title_respond_to_counter_offer));

        final EditText input = new EditText(requireContext());
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        currencyFormatter.setMaximumFractionDigits(0);
        String counterPrice = (currentOffer.getCounter_price() != null) ? currencyFormatter.format(currentOffer.getCounter_price()) : getString(R.string.price_not_available_short);
        input.setHint(getString(R.string.hint_enter_new_offer_price_with_counter, counterPrice));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton(getString(R.string.button_send_new_offer), (dialog, which) -> {
            String newOfferPriceStr = input.getText().toString().trim();
            if (newOfferPriceStr.isEmpty()) {
                if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_please_enter_new_offer_price), Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                long newOfferPrice = Long.parseLong(newOfferPriceStr);
                if (newOfferPrice <= 0) {
                    if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_new_offer_price_must_be_greater_than_zero), Toast.LENGTH_SHORT).show();
                    return;
                }
                sendBuyerResponseOffer(newOfferPrice);
            } catch (NumberFormatException e) {
                if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_invalid_new_offer_price), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(getString(R.string.button_cancel), (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void sendBuyerResponseOffer(long newOfferPrice) {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not added to context, cannot perform action or show toast.");
            return;
        }

        if (offerRef == null || currentOffer == null) {
            if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_error_cannot_send_new_offer), Toast.LENGTH_SHORT).show();
            return;
        }

        String updatedTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "pending"); // New offer is pending for seller to respond
        updates.put("offer_price", newOfferPrice);
        updates.put("counter_price", null); // Clear counter price
        updates.put("updated_at", updatedTimestamp);

        offerRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    // FIX: Correct string formatting
                    if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_new_offer_sent_value, NumberFormat.getCurrencyInstance(Locale.US).format(newOfferPrice)), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Buyer responded to offer " + offerId + " with new price: " + newOfferPrice);

                    // Đã sửa lỗi ở đây: Loại bỏ "%s" và đảm bảo chỉ có một tham số String
                    upsertOfferNotification(currentOffer.getSeller_id(), currentOffer.getItem_id(), offerId,
                            getString(R.string.notification_title_buyer_responded),
                            getString(R.string.notification_body_buyer_responded, NumberFormat.getCurrencyInstance(Locale.US).format(newOfferPrice)),
                            "buyer_responded_offer", false);
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_error_sending_new_offer, e.getMessage()), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error sending new offer from buyer " + offerId + ": " + e.getMessage());
                });
    }


    private void createTransactionRecord(Offer offer) {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not added to context, cannot create transaction record.");
            return;
        }

        if (offer == null || offerId == null) {
            Log.e(TAG, "Cannot create transaction record: offer or offerId is null.");
            return;
        }

        DatabaseReference transactionsRef = FirebaseDatabase.getInstance().getReference("transactions");
        String transactionId = transactionsRef.push().getKey();

        // Initialize transaction with escrow_status as "pending"
        Transaction transaction = new Transaction(
                transactionId, // Pass the generated transactionId
                offer.getItem_id(),
                offer.getBuyer_id(),
                offer.getSeller_id(),
                offer.getOffer_price(), // Final price is the accepted offer price
                offerId,
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()),
                false, // Not archived initially
                offeredItem != null ? offeredItem.getTitle() : getString(R.string.default_item_title_for_transaction)
        );

        // Set initial escrow status for the transaction
        transaction.setEscrow_status("pending"); // Will be updated to "held" after payment
        transaction.setBuyer_confirmed_receipt(false);
        transaction.setSeller_confirmed_dispatch(false);
        transaction.setCompletion_timestamp(null);


        if (transactionId != null) {
            transactionsRef.child(transactionId).setValue(transaction)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Transaction record created: " + transactionId);
                        if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_transaction_recorded), Toast.LENGTH_SHORT).show();

                        // Now, update the offer to link to this new transaction
                        Map<String, Object> offerUpdates = new HashMap<>();
                        offerUpdates.put("transaction_id", transactionId); // Assuming Offer model has transaction_id
                        offerRef.updateChildren(offerUpdates)
                                .addOnSuccessListener(v -> Log.d(TAG, "Offer " + offerId + " linked to transaction " + transactionId))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to link offer to transaction: " + e.getMessage()));

                        // --- START FIX for Seller Acceptance Flow ---

                        // THAY VÀO ĐÓ: Gửi thông báo đến người mua để họ tiến hành thanh toán
                        upsertOfferNotification(
                                currentOffer.getBuyer_id(), // Người nhận là người mua
                                currentOffer.getItem_id(),  // ID của mặt hàng
                                transactionId,              // related_id sẽ là transactionId
                                getString(R.string.notification_title_payment_required), // Tiêu đề thông báo
                                getString(R.string.notification_body_payment_required, offeredItem != null ? offeredItem.getTitle() : "the item", NumberFormat.getCurrencyInstance(Locale.US).format(currentOffer.getOffer_price())), // Nội dung thông báo
                                "payment_required",         // Loại thông báo mới: yêu cầu thanh toán
                                false // true nếu bạn muốn nó tự mở màn hình khi click, nhưng chúng ta sẽ xử lý chi tiết trong NotificationFragment
                        );
                        // --- END FIX for Seller Acceptance Flow ---

                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) Toast.makeText(requireContext(), getString(R.string.toast_error_creating_transaction, e.getMessage()), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error creating transaction: " + e.getMessage());
                    });
        }
    }

    // --- Common method to Send/Update Offer-related Notifications ---
    private void upsertOfferNotification(String recipientId, String itemId, String offerId,
                                         String title, String bodyFormat, String type, boolean isFinal) {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not added to context, skipping upsertOfferNotification.");
            return;
        }

        if (recipientId == null || recipientId.isEmpty() || itemId == null || itemId.isEmpty() || offerId == null || offerId.isEmpty()) {
            Log.w(TAG, "Cannot upsert offer notification: Missing required IDs.");
            return;
        }

        itemsRef.child(itemId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) {
                    Log.w(TAG, "Fragment detached during item title fetch for upsert, skipping notification.");
                    return;
                }

                String itemTitle = snapshot.child("title").getValue(String.class);
                String fullBody = String.format(Locale.getDefault(), bodyFormat, (itemTitle != null ? itemTitle : getString(R.string.your_item)));

                // FIX HERE: Order by one field, then filter in code
                Query query = notificationsRef
                        .orderByChild("user_id").equalTo(recipientId); // ONLY ORDER BY user_id

                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) {
                            Log.w(TAG, "Fragment detached after notification query for upsert, skipping notification.");
                            return;
                        }

                        final String[] notificationToUpdateId = {null};
                        for (DataSnapshot notifSnapshot : snapshot.getChildren()) {
                            Notification existingNotif = notifSnapshot.getValue(Notification.class);
                            if (existingNotif != null && existingNotif.getType() != null) {
                                // Filter by related_id HERE in Java code
                                if (offerId.equals(existingNotif.getRelated_id())) { // <-- ADD THIS FILTER CONDITION
                                    // Only update negotiation-related notification types
                                    if (!isFinal && (existingNotif.getType().equals("new_offer") ||
                                            existingNotif.getType().equals("counter_offer") ||
                                            existingNotif.getType().equals("buyer_responded_offer"))) {
                                        notificationToUpdateId[0] = notifSnapshot.getKey();
                                        break;
                                    }
                                }
                            }
                        }

                        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());

                        Map<String, Object> notificationContent = new HashMap<>();
                        notificationContent.put("user_id", recipientId);
                        notificationContent.put("title", title);
                        notificationContent.put("body", fullBody);
                        notificationContent.put("type", type);
                        notificationContent.put("related_id", offerId);
                        notificationContent.put("timestamp", timestamp);
                        notificationContent.put("read", false);

                        if (notificationToUpdateId[0] != null) {
                            notificationsRef.child(notificationToUpdateId[0]).updateChildren(notificationContent)
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Updated existing offer notification: " + notificationToUpdateId[0]))
                                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update existing offer notification: " + e.getMessage()));
                        } else {
                            String newNotificationId = notificationsRef.push().getKey();
                            if (newNotificationId != null) {
                                notificationsRef.child(newNotificationId).setValue(notificationContent)
                                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Created new offer notification: " + newNotificationId))
                                        .addOnFailureListener(e -> Log.e(TAG, "Failed to create new offer notification: " + e.getMessage()));
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to query existing notification for upsert: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch item title for upsert notification: " + error.getMessage());
            }
        });
    }
}
