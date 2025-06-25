package com.example.tradeup.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tradeup.R;
import com.example.tradeup.model.User;
import com.example.tradeup.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText editFirstName, editLastName, editBio, editContactInfo;
    private Button buttonSaveProfile;
    private FirebaseHelper firebaseHelper;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile); // Ensure layout is activity_edit_profile

        firebaseHelper = new FirebaseHelper();
        currentUser = firebaseHelper.getCurrentUser();

        editFirstName = findViewById(R.id.edit_first_name);
        editLastName = findViewById(R.id.edit_last_name);
        editBio = findViewById(R.id.edit_bio);
        editContactInfo = findViewById(R.id.edit_contact_info);
        buttonSaveProfile = findViewById(R.id.button_save_profile);

        if (currentUser == null) {
            Toast.makeText(this, "You need to log in to edit your profile.", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if no user
            return;
        }

        // Get data from Intent
        Intent intent = getIntent();
        if (intent != null) {
            String firstName = intent.getStringExtra("firstName");
            String lastName = intent.getStringExtra("lastName");
            String bio = intent.getStringExtra("bio");
            String contactInfo = intent.getStringExtra("contactInfo"); // Get contact info

            // Populate data into EditText fields
            if (firstName != null) editFirstName.setText(firstName);
            if (lastName != null) editLastName.setText(lastName);
            if (bio != null) editBio.setText(bio);
            if (contactInfo != null) editContactInfo.setText(contactInfo); // Populate email/contact info field
        }

        // Load existing profile data
        loadExistingProfileData();

        buttonSaveProfile.setOnClickListener(v -> saveProfileChanges());
    }

    // Handle back button in ActionBar
    @Override
    public boolean onSupportNavigateUp() {
        finish(); // Close this activity and go back to the previous one
        return true;
    }

    private void loadExistingProfileData() {
        firebaseHelper.getUserProfile(currentUser.getUid(), new FirebaseHelper.DbReadCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    editFirstName.setText(user.getFirst_name());
                    editLastName.setText(user.getLast_name());
                    editBio.setText(user.getBio());
                    editContactInfo.setText(user.getContact_info());
                } else {
                    Toast.makeText(EditProfileActivity.this, "Could not load existing profile data.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(EditProfileActivity.this, "Error loading profile data: " + errorMessage, Toast.LENGTH_SHORT).show();
                // Fallback to FirebaseUser display name if any
                String displayName = currentUser.getDisplayName();
                if (displayName != null && !displayName.isEmpty()) {
                    String[] nameParts = displayName.split(" ", 2);
                    if (nameParts.length > 0) {
                        editFirstName.setText(nameParts[0]);
                    }
                    if (nameParts.length > 1) {
                        editLastName.setText(nameParts[1]);
                    }
                }
            }
        });
    }

    private void saveProfileChanges() {
        String newFirstName = editFirstName.getText().toString().trim();
        String newLastName = editLastName.getText().toString().trim();
        String newBio = editBio.getText().toString().trim();
        String newContactInfo = editContactInfo.getText().toString().trim();

        // Basic validation
        if (newFirstName.isEmpty()) {
            editFirstName.setError("First name cannot be empty");
            editFirstName.requestFocus();
            return;
        }
        if (newLastName.isEmpty()) {
            editLastName.setError("Last name cannot be empty");
            editLastName.requestFocus();
            return;
        }

        // Prepare data for update.
        Map<String, Object> updates = new HashMap<>();
        updates.put("first_name", newFirstName);
        updates.put("last_name", newLastName);
        updates.put("display_name", newFirstName + " " + newLastName); // Update display name too
        updates.put("bio", newBio);
        updates.put("contact_info", newContactInfo);

        firebaseHelper.updateUserProfileFields(currentUser.getUid(), updates, new FirebaseHelper.DbWriteCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(EditProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                finish(); // Go back to ProfileFragment
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(EditProfileActivity.this, "Error updating profile: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}