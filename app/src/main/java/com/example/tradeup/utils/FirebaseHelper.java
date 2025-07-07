package com.example.tradeup.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tradeup.model.Location;
import com.example.tradeup.model.Payment;
import com.example.tradeup.model.SavedCard;
import com.example.tradeup.model.User;
import com.example.tradeup.model.Transaction; // Ensure this is the correct Transaction model
import com.example.tradeup.model.Item; // Ensure this is the correct Item model
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
// import com.google.firebase.database.Transaction; // Removed ambiguity, use fully qualified name if needed for Handler
import java.util.concurrent.atomic.AtomicInteger;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

public class FirebaseHelper {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase; // Using mDatabase consistently
    private static final String TAG = "FirebaseHelper";
    private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private Context context;

    // Interface for Authentication Callbacks
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(String errorMessage);
    }

    // Interface for Database Write Callbacks
    public interface DbWriteCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    // Interface for Database Read Callbacks (generic to handle different data types)
    public interface DbReadCallback<T> {
        void onSuccess(T data);
        void onFailure(String errorMessage);
    }

    public FirebaseHelper() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public FirebaseHelper(Context context) {
        this.context = context;
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public FirebaseUser getCurrentUser() { return mAuth.getCurrentUser(); }

    public String getCurrentUserId() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public void signOut() { mAuth.signOut(); }


    // New method to create or update user profile in Realtime Database
    public void createOrUpdateUserProfile(FirebaseUser firebaseUser, String firstName, String lastName, DbWriteCallback callback) {
        if (firebaseUser == null) {
            callback.onFailure("FirebaseUser is null.");
            return;
        }

        String userId = firebaseUser.getUid();
        DatabaseReference userRef = mDatabase.child("users").child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String currentTime = OffsetDateTime.now(ZoneOffset.UTC).format(ISO_DATE_TIME_FORMATTER);

                if (snapshot.exists()) {
                    // User profile exists, update it
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("updated_at", currentTime);
                    String existingFirstName = snapshot.child("first_name").getValue(String.class);
                    String existingLastName = snapshot.child("last_name").getValue(String.class);

                    if (firstName != null && !firstName.isEmpty() && (existingFirstName == null || !firstName.equals(existingFirstName))) {
                        updates.put("first_name", firstName);
                    }
                    if (lastName != null && !lastName.isEmpty() && (existingLastName == null || !lastName.equals(existingLastName))) {
                        updates.put("last_name", lastName);
                    }

                    userRef.updateChildren(updates)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "User profile updated successfully: " + userId);
                                    callback.onSuccess();
                                } else {
                                    Log.e(TAG, "Failed to update user profile: " + Objects.requireNonNull(task.getException()).getMessage());
                                    callback.onFailure(Objects.requireNonNull(task.getException()).getMessage());
                                }
                            });
                } else {
                    // User profile does not exist, create a new one
                    String email = firebaseUser.getEmail();
                    String displayName = (firstName != null && !firstName.isEmpty()) ? firstName : (email != null ? email.split("@")[0] : "New User");
                    if (lastName != null && !lastName.isEmpty()) {
                        displayName += " " + lastName;
                    } else if (email != null && displayName.isEmpty()) {
                        displayName = email.split("@")[0];
                    }
                    if(displayName.isEmpty()) {
                        displayName = "New User";
                    }

                    // FIX: Updated constructor to match the 20-parameter User constructor
                    User newUser = new User(
                            userId, // uid
                            email,
                            firebaseUser.isEmailVerified(),
                            displayName,
                            "", // bio mặc định
                            "", // contact_info mặc định
                            (firebaseUser.getPhotoUrl() != null) ? firebaseUser.getPhotoUrl().toString() : "", // profile_picture_url mặc định
                            0,    // total_transactions (Integer)
                            "user", // role
                            "active", // account_status
                            new Location(0.0, 0.0, "Unknown"), // location
                            currentTime, // created_at
                            currentTime, // updated_at
                            firstName,
                            lastName,
                            0L,  // rating_sum (Long)
                            0L,  // rating_count (Long)
                            0.0, // average_rating (Double)
                            false // NEW: is_banned (Boolean), default to false
                    );

                    userRef.setValue(newUser)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "New user profile created successfully: " + userId);
                                    callback.onSuccess();
                                } else {
                                    Log.e(TAG, "Failed to create new user profile: " + Objects.requireNonNull(task.getException()).getMessage());
                                    callback.onFailure(Objects.requireNonNull(task.getException()).getMessage());
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database read for user profile cancelled: " + error.getMessage());
                callback.onFailure(error.getMessage());
            }
        });
    }

    // --- User Management ---
    // Existing method for user registration with email and password
    public void registerUser(String email, String password, AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        Log.d(TAG, "User registered successfully: " + firebaseUser.getUid());
                        sendEmailVerification(firebaseUser, new AuthCallback() {
                            @Override
                            public void onSuccess(FirebaseUser user) {
                                Log.d(TAG, "Email verification sent.");
                                callback.onSuccess(firebaseUser); // Pass the FirebaseUser back
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                Log.e(TAG, "Failed to send email verification: " + errorMessage);
                                // Still call onSuccess for auth, but log the email verification failure
                                callback.onSuccess(firebaseUser);
                            }
                        });
                    } else {
                        Log.e(TAG, "Registration failed: " + task.getException().getMessage());
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    // Method to update email verification status in Realtime Database
    public void updateEmailVerificationStatus(String userId, boolean isVerified, DbWriteCallback callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onFailure("User ID is null or empty, cannot update verification status.");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("is_email_verified", isVerified);
        updates.put("updated_at", OffsetDateTime.now(ZoneOffset.UTC).format(ISO_DATE_TIME_FORMATTER));

        mDatabase.child("users").child(userId).updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Email verification status updated in Realtime Database for UID: " + userId);
                        callback.onSuccess();
                    } else {
                        Log.e(TAG, "Error updating email verification status in Realtime Database for UID: " + userId + ": " + task.getException().getMessage());
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }


    // Existing method for sending email verification
    public void sendEmailVerification(FirebaseUser user, AuthCallback callback) {
        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            callback.onSuccess(user);
                        } else {
                            callback.onFailure(task.getException().getMessage());
                        }
                    });
        } else {
            callback.onFailure("No current user to send verification email.");
        }
    }

    // Existing method for Firebase Authentication with Google
    public void firebaseAuthWithGoogle(String idToken, AuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        Log.d(TAG, "Google sign-in successful for user: " + firebaseUser.getUid());
                        callback.onSuccess(firebaseUser);
                    } else {
                        Log.e(TAG, "Google sign-in failed: " + task.getException().getMessage());
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    // Existing method for sending password reset email
    public void sendPasswordResetEmail(String email, AuthCallback callback) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Password reset email sent to " + email);
                        callback.onSuccess(null); // No FirebaseUser is typically returned for reset email
                    } else {
                        Log.e(TAG, "Failed to send password reset email: " + task.getException().getMessage());
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    // Existing method for logging in with email and password
    public void loginUser(String email, String password, AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        Log.d(TAG, "User logged in successfully: " + firebaseUser.getUid());
                        callback.onSuccess(firebaseUser);
                    } else {
                        Log.e(TAG, "Login failed: " + task.getException().getMessage());
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    // New method to get user profile from Realtime Database
    public void getUserProfile(String uid, DbReadCallback<User> callback) {
        mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    callback.onSuccess(user);
                } else {
                    callback.onFailure("User data not found.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure(error.getMessage());
            }
        });
    }

    // New method to deactivate user account by setting status to "inactive"
    public void deactivateAccount(String uid, DbWriteCallback callback) {
        mDatabase.child("users").child(uid).child("account_status").setValue("inactive")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User " + uid + " account status set to inactive.");
                        callback.onSuccess();
                    } else {
                        Log.e(TAG, "Failed to update account status for " + uid + ": " + task.getException().getMessage());
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    public void deleteUserData(String uid, DbWriteCallback callback) {
        mDatabase.child("users").child(uid).removeValue()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Method to update specific user profile fields in Realtime Database
    public void updateUserProfileFields(String uid, Map<String, Object> updates, DbWriteCallback callback) {
        if (uid == null || updates == null || updates.isEmpty()) {
            callback.onFailure("Invalid UID or empty updates map.");
            return;
        }

        String currentTime = OffsetDateTime.now(ZoneOffset.UTC).format(ISO_DATE_TIME_FORMATTER);
        updates.put("updated_at", currentTime); // Always update updated_at

        mDatabase.child("users").child(uid).updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User profile fields updated successfully for UID: " + uid);
                        callback.onSuccess();
                    } else {
                        Log.e(TAG, "Failed to update user profile fields for UID: " + uid + ": " + task.getException().getMessage());
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    // --- Item Management ---

    // Add item to Realtime Database (FR-2.1)
    public void addItem(String itemId, Item item, DbWriteCallback callback) {
        if (mAuth.getCurrentUser() == null) {
            callback.onFailure("User not authenticated.");
            return;
        }

        // Save item
        mDatabase.child("items").child(itemId).setValue(item)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Initialize analytics for the new item (FR-2.2.3)
                        Map<String, Object> analyticsData = new HashMap<>();
                        analyticsData.put("views", 0);
                        analyticsData.put("chats_started", 0);
                        analyticsData.put("offers_made", 0);

                        mDatabase.child("analytics").child(itemId).setValue(analyticsData)
                                .addOnCompleteListener(analyticsTask -> {
                                    if (analyticsTask.isSuccessful()) {
                                        callback.onSuccess();
                                    } else {
                                        callback.onFailure("Failed to initialize analytics for item: " + analyticsTask.getException().getMessage());
                                        // Consider rolling back item creation if analytics fails critically
                                    }
                                });
                    } else {
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    // Update item in Realtime Database (for editing and status changes) (FR-2.2.1, FR-2.2.2)
    public void updateItem(String itemId, Map<String, Object> updates, DbWriteCallback callback) {
        if (mAuth.getCurrentUser() == null) {
            callback.onFailure("User not authenticated.");
            return;
        }
        mDatabase.child("items").child(itemId).updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    // Delete item from Realtime Database (FR-2.2.1)
    public void deleteItem(String itemId, DbWriteCallback callback) {
        if (mAuth.getCurrentUser() == null) {
            callback.onFailure("User not authenticated.");
            return;
        }
        // Remove from main items node
        mDatabase.child("items").child(itemId).removeValue()
                .addOnCompleteListener(itemRemovalTask -> {
                    if (itemRemovalTask.isSuccessful()) {
                        // Also remove its analytics data
                        mDatabase.child("analytics").child(itemId).removeValue()
                                .addOnCompleteListener(analyticsRemovalTask -> {
                                    if (analyticsRemovalTask.isSuccessful()) {
                                        callback.onSuccess();
                                    } else {
                                        callback.onFailure("Failed to remove item analytics: " + analyticsRemovalTask.getException().getMessage());
                                        // Deleting analytics is less critical than item, but still good to handle
                                    }
                                });
                    } else {
                        callback.onFailure(itemRemovalTask.getException().getMessage());
                    }
                });
    }

    // Get item by ID (for editing or viewing details)
    public void getItem(String itemId, DbReadCallback<Item> callback) {
        mDatabase.child("items").child(itemId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Item item = task.getResult().getValue(Item.class);
                        if (item != null) {
                            callback.onSuccess(item);
                        } else {
                            callback.onFailure("Item not found.");
                        }
                    } else {
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    // Get user's items (for "My Listings") (FR-2.2.1)
    public void getUserItems(String userId, DbReadCallback<List<Item>> callback) {
        mDatabase.child("items").orderByChild("user_id").equalTo(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Item> userItems = new ArrayList<>();
                        for (DataSnapshot itemSnapshot : task.getResult().getChildren()) {
                            Item item = itemSnapshot.getValue(Item.class);
                            if (item != null) {
                                // Lấy key của snapshot và đặt nó làm ID của item
                                item.setId(itemSnapshot.getKey());
                                userItems.add(item);
                            }
                        }
                        callback.onSuccess(userItems);
                    } else {
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    // Update views for an item (FR-2.2.3)
    public void incrementItemView(String itemId, DbWriteCallback callback) {
        mDatabase.child("analytics").child(itemId).child("views").runTransaction(new com.google.firebase.database.Transaction.Handler() { // Use fully qualified name
            @NonNull
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(@NonNull MutableData currentData) { // Use fully qualified name
                Integer views = currentData.getValue(Integer.class);
                if (views == null) {
                    currentData.setValue(1); // First view
                } else {
                    currentData.setValue(views + 1);
                }
                return com.google.firebase.database.Transaction.success(currentData); // Use fully qualified name
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    callback.onFailure(error.getMessage());
                } else if (committed) {
                    callback.onSuccess();
                } else {
                    callback.onFailure("Transaction not committed.");
                }
            }
        });
    }

    public DatabaseReference getItemReference(String itemId) {
        return mDatabase.child("items").child(itemId);
    }

    public DatabaseReference getUserReference(String userId) {
        return mDatabase.child("users").child(userId);
    }

    // Get analytics for an item (FR-2.2.3)
    public void getItemAnalytics(String itemId, DbReadCallback<Map<String, Object>> callback) {
        mDatabase.child("analytics").child(itemId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, Object> analytics = (Map<String, Object>) task.getResult().getValue();
                        if (analytics != null) {
                            callback.onSuccess(analytics);
                        } else {
                            callback.onFailure("Analytics data not found for item: " + itemId);
                        }
                    } else {
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    // You can add more methods here for other interactions like incrementing chats_started, offers_made
    public void incrementChatsStarted(String itemId, DbWriteCallback callback) {
        mDatabase.child("analytics").child(itemId).child("chats_started").runTransaction(new com.google.firebase.database.Transaction.Handler() { // Use fully qualified name
            @NonNull
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(@NonNull MutableData currentData) { // Use fully qualified name
                Integer chats = currentData.getValue(Integer.class);
                if (chats == null) {
                    currentData.setValue(1);
                } else {
                    currentData.setValue(chats + 1);
                }
                return com.google.firebase.database.Transaction.success(currentData); // Use fully qualified name
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    callback.onFailure(error.getMessage());
                } else if (committed) {
                    callback.onSuccess();
                } else {
                    callback.onFailure("Transaction not committed.");
                }
            }
        });
    }

    // Add Payment to 'payments' node and update 'payment_history'
    public void addPayment(Payment payment, DbWriteCallback callback) {
        String generatedPaymentId = payment.getPayment_id();
        if (generatedPaymentId == null) {
            generatedPaymentId = mDatabase.child("payments").push().getKey();
            payment.setPayment_id(generatedPaymentId);
        }
        // Make variables effectively final
        final String finalPaymentId = generatedPaymentId;
        // FIX: Use getBuyer_id() and getSeller_id() instead of getPayer_id() and getPayee_id()
        final String buyerId = payment.getBuyer_id();
        final String sellerId = payment.getSeller_id();

        mDatabase.child("payments").child(finalPaymentId).setValue(payment)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        mDatabase.child("payment_history").child(buyerId).child(finalPaymentId).setValue(true)
                                .addOnCompleteListener(buyerTask -> {
                                    if (buyerTask.isSuccessful()) {
                                        mDatabase.child("payment_history").child(sellerId).child(finalPaymentId).setValue(true)
                                                .addOnCompleteListener(sellerTask -> {
                                                    if (sellerTask.isSuccessful()) {
                                                        callback.onSuccess();
                                                    } else {
                                                        callback.onFailure("Failed to update seller's payment history: " + sellerTask.getException().getMessage());
                                                    }
                                                });
                                    } else {
                                        callback.onFailure("Failed to update buyer's payment history: " + buyerTask.getException().getMessage());
                                    }
                                });
                    } else {
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    // Get Payment by ID
    public void getPaymentById(String paymentId, DbReadCallback<Payment> callback) {
        mDatabase.child("payments").child(paymentId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Payment payment = snapshot.getValue(Payment.class);
                if (payment != null) {
                    payment.setPayment_id(snapshot.getKey());
                }
                callback.onSuccess(payment);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure(error.getMessage());
            }
        });
    }

    // Update transaction with payment_id
    public void updateTransactionPaymentId(String transactionId, String paymentId, DbWriteCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("payment_id", paymentId);
        mDatabase.child("transactions").child(transactionId).updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    // Get Transaction by ID (needed by RecentTransactionAdapter and TransactionDetailFragment)
    public void getTransactionById(String transactionId, DbReadCallback<Transaction> callback) {
        mDatabase.child("transactions").child(transactionId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Transaction transaction = snapshot.getValue(Transaction.class);
                callback.onSuccess(transaction);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure(error.getMessage());
            }
        });
    }

    // Add SavedCard to 'saved_cards' node and update 'user_saved_cards'
    public void addSavedCard(SavedCard card, DbWriteCallback callback) {
        String generatedCardId = card.getCard_id();
        if (generatedCardId == null) {
            generatedCardId = mDatabase.child("saved_cards").push().getKey();
            card.setCard_id(generatedCardId);
        }
        // Make variables effectively final
        final String finalCardId = generatedCardId;
        final String userId = card.getUser_id();

        mDatabase.child("saved_cards").child(finalCardId).setValue(card)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        mDatabase.child("user_saved_cards").child(userId).child(finalCardId).setValue(true)
                                .addOnCompleteListener(userCardTask -> {
                                    if (userCardTask.isSuccessful()) {
                                        callback.onSuccess();
                                    } else {
                                        callback.onFailure("Failed to update user's saved cards list: " + userCardTask.getException().getMessage());
                                    }
                                });
                    } else {
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    // Get SavedCards for a specific user
    public void getSavedCardsForUser(String userId, DbReadCallback<List<SavedCard>> callback) {
        mDatabase.child("user_saved_cards").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> cardIds = new ArrayList<>();
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    cardIds.add(childSnapshot.getKey());
                }
                fetchSavedCardDetails(cardIds, callback);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure(error.getMessage());
            }
        });
    }

    private void fetchSavedCardDetails(List<String> cardIds, DbReadCallback<List<SavedCard>> callback) {
        if (cardIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        List<SavedCard> fetchedCards = new ArrayList<>();
        DatabaseReference savedCardsRef = mDatabase.child("saved_cards");
        final AtomicInteger cardsToFetch = new AtomicInteger(cardIds.size());

        for (String cardId : cardIds) {
            savedCardsRef.child(cardId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        SavedCard card = snapshot.getValue(SavedCard.class);
                        if (card != null) {
                            card.setCard_id(snapshot.getKey());
                            fetchedCards.add(card);
                        }
                    }
                    if (cardsToFetch.decrementAndGet() == 0) {
                        callback.onSuccess(fetchedCards);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to fetch saved card details for ID " + cardId + ": " + error.getMessage());
                    if (cardsToFetch.decrementAndGet() == 0) {
                        callback.onSuccess(fetchedCards);
                    }
                }
            });
        }
    }

    public void getPaymentHistoryForUser(String userId, DbReadCallback<List<Payment>> callback) {
        mDatabase.child("payment_history").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> paymentIds = new ArrayList<>();
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    paymentIds.add(childSnapshot.getKey());
                }
                fetchPaymentDetailsForHistory(paymentIds, callback);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure(error.getMessage());
            }
        });
    }

    private void fetchPaymentDetailsForHistory(List<String> paymentIds, DbReadCallback<List<Payment>> callback) {
        if (paymentIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        List<Payment> fetchedPayments = new ArrayList<>();
        DatabaseReference paymentsRef = mDatabase.child("payments");
        final AtomicInteger paymentsToFetch = new AtomicInteger(paymentIds.size());

        for (String paymentId : paymentIds) {
            paymentsRef.child(paymentId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Payment payment = snapshot.getValue(Payment.class);
                        if (payment != null) {
                            payment.setPayment_id(snapshot.getKey());
                            fetchedPayments.add(payment);
                        }
                    }
                    if (paymentsToFetch.decrementAndGet() == 0) {
                        callback.onSuccess(fetchedPayments);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to fetch payment details for ID " + paymentId + ": " + error.getMessage());
                    if (paymentsToFetch.decrementAndGet() == 0) {
                        callback.onSuccess(fetchedPayments);
                    }
                }
            });
        }
    }

    // Method to update item status to "Sold"
    public void markItemAsSold(String itemId, DbWriteCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "Sold");
        mDatabase.child("items").child(itemId).updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    // Method to update transaction status (general purpose, might not be used directly for escrow flow)
    public void updateTransactionStatus(String transactionId, String newStatus, DbWriteCallback callback) {
        DatabaseReference transactionRef = mDatabase.child("transactions").child(transactionId);
        transactionRef.child("status").setValue(newStatus)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Method to update item status (general purpose, used for "pending_escrow")
    public void updateItemStatus(String itemId, String newStatus, DbWriteCallback callback) {
        DatabaseReference itemRef = mDatabase.child("items").child(itemId);
        itemRef.child("status").setValue(newStatus)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Method to update transaction escrow status and confirmations
    public void updateTransactionEscrowStatusAndConfirmations(String transactionId, Map<String, Object> updates, DbWriteCallback callback) {
        if (transactionId == null || transactionId.isEmpty()) {
            callback.onFailure("Transaction ID cannot be null or empty.");
            return;
        }
        mDatabase.child("transactions").child(transactionId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Method to update payment escrow status
    public void updatePaymentEscrowStatus(String paymentId, String newEscrowStatus, DbWriteCallback callback) {
        if (paymentId == null || paymentId.isEmpty() || newEscrowStatus == null || newEscrowStatus.isEmpty()) {
            callback.onFailure("Payment ID or new escrow status cannot be null or empty.");
            return;
        }
        mDatabase.child("payments").child(paymentId).child("escrow_status").setValue(newEscrowStatus)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Ensure you have addTransaction method (if not already present)
    public void addTransaction(Transaction transaction, DbWriteCallback callback) {
        if (transaction.getTransaction_id() == null || transaction.getTransaction_id().isEmpty()) {
            callback.onFailure("Transaction ID cannot be null or empty.");
            return;
        }
        mDatabase.child("transactions").child(transaction.getTransaction_id()).setValue(transaction)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Thêm interface cho Item Fetch Listener nếu chưa có
    public interface OnItemsFetchListener {
        void onItemsFetched(List<Item> items);
        void onFailure(String errorMessage);
    }

    // Phương thức mới để lấy các mặt hàng theo danh mục
    public void getItemsByCategory(String category, final OnItemsFetchListener listener) {
        Query query = mDatabase.child("items").orderByChild("category").equalTo(category);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Item> items = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Item item = snapshot.getValue(Item.class);
                    if (item != null) {
                        item.setId(snapshot.getKey()); // Đặt ID từ key của Firebase
                        items.add(item);
                    }
                }
                listener.onItemsFetched(items);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load items by category: " + databaseError.getMessage());
                listener.onFailure(databaseError.getMessage());
            }
        });
    }
}
