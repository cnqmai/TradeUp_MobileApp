package com.example.tradeup.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.tradeup.R;
import com.example.tradeup.model.Offer;
import com.example.tradeup.model.Transaction;
import com.example.tradeup.model.Notification;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.Query;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class OfferDetailFragment extends Fragment {

    private static final String TAG = "OfferDetailFragment";

    private TextView tvOfferAmount, tvBuyerName, tvOfferStatus, tvItemInfo;
    private Button btnAccept, btnReject, btnCounter;
    private Button btnBuyerRespondOffer;

    private String offerId;
    private DatabaseReference offerRef;
    private ValueEventListener offerValueEventListener;
    private Offer currentOffer;
    private String currentUserId;
    private NavController navController;

    private DatabaseReference notificationsRef;
    private DatabaseReference itemsRef;

    public OfferDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        notificationsRef = FirebaseDatabase.getInstance().getReference("notifications");
        itemsRef = FirebaseDatabase.getInstance().getReference("items");

        if (getArguments() != null) {
            offerId = getArguments().getString("offerId");
            if (offerId != null) {
                offerRef = FirebaseDatabase.getInstance().getReference("offers").child(offerId);
            } else {
                Log.e(TAG, "Offer ID is null in arguments.");
            }
        } else {
            Log.e(TAG, "Arguments are null.");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_offer_detail, container, false);
        tvOfferAmount = view.findViewById(R.id.tv_offer_amount);
        tvBuyerName = view.findViewById(R.id.tv_buyer_name);
        tvOfferStatus = view.findViewById(R.id.tv_offer_status);
        tvItemInfo = view.findViewById(R.id.tv_item_info);
        btnAccept = view.findViewById(R.id.btn_accept_offer);
        btnReject = view.findViewById(R.id.btn_reject_offer);
        btnCounter = view.findViewById(R.id.btn_counter_offer);
        btnBuyerRespondOffer = view.findViewById(R.id.btn_buyer_respond_offer);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        if (offerRef != null) {
            fetchOfferDetails();
        } else {
            Toast.makeText(requireContext(), "Lỗi: Không thể tải chi tiết đề nghị.", Toast.LENGTH_SHORT).show();
        }

        btnAccept.setOnClickListener(v -> {
            if (currentOffer != null && currentOffer.getStatus().equals("pending") && currentOffer.getSeller_id().equals(currentUserId)) {
                handleOfferAction("accepted");
            } else {
                if (isAdded()) Toast.makeText(requireContext(), "Không thể chấp nhận đề nghị này.", Toast.LENGTH_SHORT).show();
            }
        });
        btnReject.setOnClickListener(v -> {
            if (currentOffer != null && currentOffer.getStatus().equals("pending") && currentOffer.getSeller_id().equals(currentUserId)) {
                handleOfferAction("rejected");
            } else {
                if (isAdded()) Toast.makeText(requireContext(), "Không thể từ chối đề nghị này.", Toast.LENGTH_SHORT).show();
            }
        });
        btnCounter.setOnClickListener(v -> {
            if (currentOffer != null && currentOffer.getStatus().equals("pending") && currentOffer.getSeller_id().equals(currentUserId)) {
                showCounterOfferDialog();
            } else {
                if (isAdded()) Toast.makeText(requireContext(), "Không thể trả giá đề nghị này.", Toast.LENGTH_SHORT).show();
            }
        });

        btnBuyerRespondOffer.setOnClickListener(v -> {
            if (currentOffer != null && currentOffer.getStatus().equals("countered") && currentOffer.getBuyer_id().equals(currentUserId)) {
                showBuyerRespondDialog();
            } else {
                if (isAdded()) Toast.makeText(requireContext(), "Không thể phản hồi đề nghị này.", Toast.LENGTH_SHORT).show();
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
                    if (currentOffer.getStatus() != null && currentOffer.getStatus().equals("countered") && currentOffer.getCounter_price() != null) {
                        tvOfferAmount.setText("Giá trả lại: " + String.valueOf(currentOffer.getCounter_price()));
                    } else if (currentOffer.getOffer_price() != null) {
                        tvOfferAmount.setText("Số tiền đề nghị: " + String.valueOf(currentOffer.getOffer_price()));
                    } else {
                        tvOfferAmount.setText("Số tiền đề nghị: N/A");
                    }

                    tvOfferStatus.setText("Trạng thái: " + (currentOffer.getStatus() != null ? currentOffer.getStatus() : "N/A"));
                    tvItemInfo.setText("ID Tin đăng: " + (currentOffer.getItem_id() != null ? currentOffer.getItem_id() : "N/A"));

                    tvBuyerName.setText("ID Người mua: " + (currentOffer.getBuyer_id() != null ? currentOffer.getBuyer_id() : "N/A"));

                    updateButtonState(currentOffer);
                } else {
                    Toast.makeText(requireContext(), "Không tìm thấy chi tiết đề nghị.", Toast.LENGTH_SHORT).show();
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
                Log.e(TAG, "Lỗi tải đề nghị: " + error.getMessage());
                Toast.makeText(requireContext(), "Lỗi tải đề nghị: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
    }


    private void updateButtonState(Offer offer) {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not added to context, cannot update UI or show toast.");
            return;
        }

        boolean isSeller = currentUserId.equals(offer.getSeller_id());
        boolean isBuyer = currentUserId.equals(offer.getBuyer_id());
        String status = offer.getStatus();

        btnAccept.setEnabled(false);
        btnReject.setEnabled(false);
        btnCounter.setEnabled(false);
        btnBuyerRespondOffer.setEnabled(false);

        btnAccept.setVisibility(View.GONE);
        btnReject.setVisibility(View.GONE);
        btnCounter.setVisibility(View.GONE);
        btnBuyerRespondOffer.setVisibility(View.GONE);


        if (status != null) {
            switch (status) {
                case "pending":
                    if (isSeller) {
                        btnAccept.setVisibility(View.VISIBLE);
                        btnReject.setVisibility(View.VISIBLE);
                        btnCounter.setVisibility(View.VISIBLE);

                        btnAccept.setEnabled(true);
                        btnReject.setEnabled(true);
                        btnCounter.setEnabled(true);
                    } else if (isBuyer) {
                        Toast.makeText(requireContext(), "Đang chờ người bán phản hồi đề nghị của bạn.", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case "countered":
                    if (isBuyer) {
                        btnBuyerRespondOffer.setVisibility(View.VISIBLE);
                        btnBuyerRespondOffer.setEnabled(true);
                        Toast.makeText(requireContext(), "Người bán đã đưa ra đề nghị ngược lại.", Toast.LENGTH_SHORT).show();
                    } else if (isSeller) {
                        Toast.makeText(requireContext(), "Đã gửi đề nghị ngược lại. Đang chờ người mua phản hồi.", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case "accepted":
                    Toast.makeText(requireContext(), "Đề nghị đã được chấp nhận.", Toast.LENGTH_SHORT).show();
                    break;
                case "rejected":
                    Toast.makeText(requireContext(), "Đề nghị đã bị từ chối.", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    private void handleOfferAction(String action) {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not added to context, cannot perform action or show toast.");
            return;
        }

        if (offerRef == null || currentOffer == null) {
            Toast.makeText(requireContext(), "Lỗi: Không thể thực hiện hành động.", Toast.LENGTH_SHORT).show();
            return;
        }

        String updatedTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", action);
        updates.put("updated_at", updatedTimestamp);

        if ("accepted".equals(action) || "rejected".equals(action)) {
            updates.put("counter_price", null);
        }

        offerRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) Toast.makeText(requireContext(), "Đề nghị đã được " + action + ".", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Offer " + offerId + " status updated to " + action);

                    String recipientId = null;
                    if (currentUserId.equals(currentOffer.getBuyer_id())) {
                        recipientId = currentOffer.getSeller_id();
                    } else if (currentUserId.equals(currentOffer.getSeller_id())) {
                        recipientId = currentOffer.getBuyer_id();
                    }

                    if ("accepted".equals(action)) {
                        markItemAsSold(currentOffer.getItem_id());
                        createOrUpdateTransactionRecord(currentOffer);
                        upsertOfferNotification(recipientId, currentOffer.getItem_id(), offerId,
                                "Đề nghị đã được chấp nhận!",
                                "Đề nghị của bạn cho sản phẩm \"%s\" đã được chấp nhận.",
                                "offer_accepted", true);
                    } else if ("rejected".equals(action)) {
                        upsertOfferNotification(recipientId, currentOffer.getItem_id(), offerId,
                                "Đề nghị đã bị từ chối!",
                                "Đề nghị của bạn cho sản phẩm \"%s\" đã bị từ chối.",
                                "offer_rejected", true);
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) Toast.makeText(requireContext(), "Lỗi khi cập nhật đề nghị: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Lỗi khi cập nhật đề nghị " + offerId + ": " + e.getMessage());
                });
    }

    private void showCounterOfferDialog() {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not added to context, cannot show dialog.");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Đưa ra đề nghị ngược lại");

        final EditText input = new EditText(requireContext());
        String currentOfferPrice = (currentOffer.getOffer_price() != null) ? String.valueOf(currentOffer.getOffer_price()) : "N/A";
        input.setHint("Nhập giá bạn muốn trả giá (đề nghị hiện tại: " + currentOfferPrice + ")");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("Gửi Trả Giá", (dialog, which) -> {
            String counterPriceStr = input.getText().toString().trim();
            if (counterPriceStr.isEmpty()) {
                if (isAdded()) Toast.makeText(requireContext(), "Vui lòng nhập giá trả giá.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                long counterPrice = Long.parseLong(counterPriceStr);
                if (counterPrice <= 0) {
                    if (isAdded()) Toast.makeText(requireContext(), "Giá trả giá phải lớn hơn 0.", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendCounterOffer(counterPrice);
            } catch (NumberFormatException e) {
                if (isAdded()) Toast.makeText(requireContext(), "Giá trả giá không hợp lệ.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void sendCounterOffer(long counterPrice) {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not added to context, cannot perform action or show toast.");
            return;
        }

        if (offerRef == null || currentOffer == null) {
            Toast.makeText(requireContext(), "Lỗi: Không thể trả giá.", Toast.LENGTH_SHORT).show();
            return;
        }

        String updatedTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "countered");
        updates.put("counter_price", counterPrice);
        updates.put("updated_at", updatedTimestamp);

        offerRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) Toast.makeText(requireContext(), "Đã gửi đề nghị ngược lại: " + counterPrice, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Offer " + offerId + " countered with price: " + counterPrice);

                    upsertOfferNotification(currentOffer.getBuyer_id(), currentOffer.getItem_id(), offerId,
                            "Đề nghị của bạn đã được trả giá!",
                            "Người bán đã đưa ra giá trả lại " + counterPrice + " cho sản phẩm \"%s\".",
                            "counter_offer", false);
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) Toast.makeText(requireContext(), "Lỗi khi gửi trả giá đề nghị: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Lỗi khi gửi trả giá đề nghị " + offerId + ": " + e.getMessage());
                });
    }

    private void showBuyerRespondDialog() {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not added to context, cannot show dialog.");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Đưa ra đề nghị mới");

        final EditText input = new EditText(requireContext());
        String counterPrice = (currentOffer.getCounter_price() != null) ? String.valueOf(currentOffer.getCounter_price()) : "N/A";
        input.setHint("Nhập giá đề nghị mới của bạn (giá trả lại: " + counterPrice + ")");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("Gửi Đề nghị", (dialog, which) -> {
            String newOfferPriceStr = input.getText().toString().trim();
            if (newOfferPriceStr.isEmpty()) {
                if (isAdded()) Toast.makeText(requireContext(), "Vui lòng nhập giá đề nghị mới.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                long newOfferPrice = Long.parseLong(newOfferPriceStr);
                if (newOfferPrice <= 0) {
                    if (isAdded()) Toast.makeText(requireContext(), "Giá đề nghị mới phải lớn hơn 0.", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendBuyerResponseOffer(newOfferPrice);
            } catch (NumberFormatException e) {
                if (isAdded()) Toast.makeText(requireContext(), "Giá đề nghị mới không hợp lệ.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void sendBuyerResponseOffer(long newOfferPrice) {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not added to context, cannot perform action or show toast.");
            return;
        }

        if (offerRef == null || currentOffer == null) {
            Toast.makeText(requireContext(), "Lỗi: Không thể gửi đề nghị mới.", Toast.LENGTH_SHORT).show();
            return;
        }

        String updatedTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "pending");
        updates.put("offer_price", newOfferPrice);
        updates.put("counter_price", null);
        updates.put("updated_at", updatedTimestamp);

        offerRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) Toast.makeText(requireContext(), "Đã gửi đề nghị mới: " + newOfferPrice, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Buyer responded to offer " + offerId + " with new price: " + newOfferPrice);

                    upsertOfferNotification(currentOffer.getSeller_id(), currentOffer.getItem_id(), offerId,
                            "Người mua đã phản hồi đề nghị của bạn!",
                            "Người mua đã đưa ra đề nghị mới " + newOfferPrice + " cho sản phẩm \"%s\".",
                            "buyer_responded_offer", false);
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) Toast.makeText(requireContext(), "Lỗi khi gửi đề nghị mới: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Lỗi khi gửi đề nghị mới từ người mua " + offerId + ": " + e.getMessage());
                });
    }


    private void markItemAsSold(String itemId) {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not added to context, cannot perform action or show toast.");
            return;
        }

        if (itemId == null || itemId.isEmpty()) {
            Log.e(TAG, "Cannot mark item as sold: item ID is null or empty.");
            return;
        }

        DatabaseReference itemRef = FirebaseDatabase.getInstance().getReference("items").child(itemId);
        itemRef.child("status").setValue("sold")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Item " + itemId + " marked as sold.");
                    if (isAdded()) Toast.makeText(requireContext(), "Tin đăng đã được đánh dấu là 'Đã bán'.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to mark item " + itemId + " as sold: " + e.getMessage());
                    if (isAdded()) Toast.makeText(requireContext(), "Lỗi khi đánh dấu tin đăng là 'Đã bán'.", Toast.LENGTH_SHORT).show();
                });
    }

    private void createOrUpdateTransactionRecord(Offer offer) {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not added to context, cannot perform action or show toast.");
            return;
        }

        if (offer == null || offerId == null) {
            Log.e(TAG, "Cannot create transaction record: offer or offerId is null.");
            return;
        }

        DatabaseReference transactionsRef = FirebaseDatabase.getInstance().getReference("transactions");
        String transactionId = transactionsRef.push().getKey();

        Transaction transaction = new Transaction();
        transaction.setItem_id(offer.getItem_id());
        transaction.setBuyer_id(offer.getBuyer_id());
        transaction.setSeller_id(offer.getSeller_id());
        transaction.setFinal_price(offer.getOffer_price());
        transaction.setOffer_id(offerId);
        transaction.setTransaction_date(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));
        transaction.setArchived(false);

        if (transactionId != null) {
            transactionsRef.child(transactionId).setValue(transaction)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Transaction record created: " + transactionId);
                        if (isAdded()) Toast.makeText(requireContext(), "Giao dịch đã được ghi nhận.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to create transaction record: " + e.getMessage());
                        if (isAdded()) Toast.makeText(requireContext(), "Lỗi khi ghi nhận giao dịch.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // --- Phương thức chung để Gửi/Cập nhật thông báo liên quan đến Đề nghị ---
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
                String fullBody = String.format(Locale.getDefault(), bodyFormat, (itemTitle != null ? itemTitle : "sản phẩm của bạn"));

                // SỬA LỖI Ở ĐÂY: Chỉ orderByChild một trường, sau đó lọc trong code
                Query query = notificationsRef
                        .orderByChild("user_id").equalTo(recipientId); // CHỈ SẮP XẾP THEO user_id

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
                                // Lọc theo related_id TẠI ĐÂY trong code Java
                                if (offerId.equals(existingNotif.getRelated_id())) { // <-- THÊM ĐIỀU KIỆN LỌC NÀY
                                    // Chỉ cập nhật các loại thông báo liên quan đến thương lượng
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

    // Các phương thức send...Notification cũ đã được thay thế bằng upsertOfferNotification
    // Giờ đây chúng ta có thể xóa các phương thức cũ này:
    // private void sendCounterOfferNotification(...)
    // private void sendBuyerRespondedNotification(...)
    // private void sendOfferAcceptedOrRejectedNotification(...)
}
