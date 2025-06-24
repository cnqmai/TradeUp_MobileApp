package com.example.tradeup.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.tradeup.model.User; // Import the User model
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;


import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class FirebaseHelper {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private static final String TAG = "FirebaseHelper";
    private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

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
    public interface DbReadCallback<T> { // <T> makes it generic
        void onSuccess(T data);
        void onFailure(String errorMessage);
    }

    public FirebaseHelper() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    // New method to create or update user profile in Realtime Database
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
                    // ... (phần code cập nhật người dùng đã tồn tại không đổi)
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("updated_at", currentTime);
                    // Also update first_name and last_name if provided and different
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
                                    Log.e(TAG, "Failed to update user profile: " + task.getException().getMessage());
                                    callback.onFailure(task.getException().getMessage());
                                }
                            });
                } else {
                    // User profile does not exist, create a new one
                    String email = firebaseUser.getEmail();
                    // Set default display name if first/last name not provided
                    String displayName = (firstName != null && !firstName.isEmpty()) ? firstName : (email != null ? email.split("@")[0] : "New User");
                    if (lastName != null && !lastName.isEmpty()) {
                        displayName += " " + lastName;
                    } else if (email != null && displayName.isEmpty()) {
                        displayName = email.split("@")[0]; // Fallback to email prefix if display name is still empty
                    }
                    if(displayName.isEmpty()) { // Final fallback
                        displayName = "New User";
                    }

                    User newUser = new User(
                            userId, // THÊM userId VÀO ĐÂY
                            email,
                            firebaseUser.isEmailVerified(),
                            displayName,
                            "", // bio mặc định
                            "", // contact_info mặc định
                            (firebaseUser.getPhotoUrl() != null) ? firebaseUser.getPhotoUrl().toString() : "", // profile_picture_url mặc định
                            0.0,  // rating
                            0,    // total_transactions
                            "user", // role
                            "active", // account_status
                            null, // location
                            currentTime, // created_at
                            currentTime, // updated_at
                            firstName,
                            lastName
                    );

                    userRef.setValue(newUser)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "New user profile created successfully: " + userId);
                                    callback.onSuccess();
                                } else {
                                    Log.e(TAG, "Failed to create new user profile: " + task.getException().getMessage());
                                    callback.onFailure(task.getException().getMessage());
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
    // Đã thay đổi AuthCallback thành DbWriteCallback
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
    // Vẫn dùng AuthCallback vì nó liên quan đến FirebaseUser và Auth
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

    // Method to sign out
    public void signOut() {
        mAuth.signOut();
    }

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
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
}