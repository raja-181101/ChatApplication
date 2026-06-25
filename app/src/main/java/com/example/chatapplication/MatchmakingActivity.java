package com.example.chatapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import Authentication_Advanced.LoginActivity;

public class MatchmakingActivity extends AppCompatActivity {

    private Button logoutButton;
    private Button findChatButton;
    private ProgressBar progressBar;
    private TextView statusText;

    private FirebaseAuth mAuth;
    private DatabaseReference waitingRoomRef;
    private DatabaseReference chatsRef;
    private DatabaseReference myMatchRef;

    private String currentUserId;
//    private String currentUserEmail;

    private ValueEventListener matchListener;
    private boolean isMatched = false;
    private boolean isSearching = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matchmaking);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            startActivity(new Intent(this, LauncherActivity.class));
            finish();
            return;
        }

        currentUserId    = user.getUid();
//        currentUserEmail = user.getEmail();

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        waitingRoomRef = database.getReference("waiting_room");
        chatsRef       = database.getReference("chats");

        findChatButton = findViewById(R.id.findChatButton);
        logoutButton   = findViewById(R.id.logoutButton);
        progressBar    = findViewById(R.id.progressBar);
        statusText     = findViewById(R.id.statusText);

        findChatButton.setOnClickListener(v -> startMatchmaking());
        logoutButton.setEnabled(false);
        logoutButton.setVisibility(View.INVISIBLE);
//        logoutButton.setOnClickListener(v -> logout());
    }

    private void startMatchmaking() {
        isMatched   = false;
        isSearching = true;

        findChatButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Looking for someone...");

        // First clean up any old entry for ourselves
        waitingRoomRef.child(currentUserId).removeValue()
                .addOnCompleteListener(task -> scanAndMatch());
    }

    // Scan the waiting room and try to match using a transaction
    private void scanAndMatch() {
        waitingRoomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!isSearching) return;

                String foundUserId = null;

                // Find someone else waiting (not ourselves)
                for (DataSnapshot child : snapshot.getChildren()) {
                    String uid = child.getKey();
                    if (uid != null && !uid.equals(currentUserId)) {
                        foundUserId = uid;
                        break;
                    }
                }

                if (foundUserId != null) {
                    // Try to "claim" this waiting user using a transaction
                    // Transaction prevents two people matching the same person simultaneously
                    tryClaimUser(foundUserId);
                } else {
                    // Nobody waiting — add ourselves and listen for a match
                    addSelfAndWait();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                showError("Connection error. Try again.");
                resetUI();
            }
        });
    }

    // Use a Firebase Transaction to atomically claim a waiting user
    private void tryClaimUser(String otherUserId) {
        DatabaseReference otherUserRef = waitingRoomRef.child(otherUserId);

        otherUserRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData currentData) {
                // If the user is still there, remove them (claim them)
                if (currentData.getValue() != null) {
                    currentData.setValue(null); // Remove from waiting room
                    return Transaction.success(currentData);
                } else {
                    // Someone else already claimed them — abort
                    return Transaction.abort();
                }
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (committed && isSearching) {
                    // Successfully claimed! Create chat room
                    isMatched = true;
                    createChatRoom(otherUserId);
                } else {
                    // Claim failed — that user was taken. Try again.
                    scanAndMatch();
                }
            }
        });
    }

    // Create a chat room for the two matched users
    private void createChatRoom(String otherUserId) {
        // Consistent room ID regardless of who matched whom
        String chatRoomId;
        if (currentUserId.compareTo(otherUserId) < 0) {
            chatRoomId = currentUserId + "_" + otherUserId;
        } else {
            chatRoomId = otherUserId + "_" + currentUserId;
        }

        DatabaseReference roomRef = chatsRef.child(chatRoomId);

        // Write both users into the chat room
        roomRef.child("users").child("user1").setValue(currentUserId);
        roomRef.child("users").child("user2").setValue(otherUserId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && isSearching) {
                        openChatRoom(chatRoomId);
                    } else {
                        showError("Failed to create chat room. Try again.");
                        resetUI();
                    }
                });
    }

    // Add ourselves to waiting room and listen for someone to match us
    private void addSelfAndWait() {
        DatabaseReference myRef = waitingRoomRef.child(currentUserId);

        // Add to waiting room
//        myRef.setValue(currentUserEmail);
        myRef.setValue(currentUserId);

        // Remove ourselves if we disconnect unexpectedly
        myRef.onDisconnect().removeValue();

        statusText.setText("Waiting for someone to join...");

        // Listen on our specific user node in chats
        // The other user will create a chat room with our UID in it
        myMatchRef = chatsRef;
        matchListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (isMatched || !isSearching) return;

                for (DataSnapshot room : snapshot.getChildren()) {
                    // Check if this room has us as a user
                    for (DataSnapshot userEntry : room.child("users").getChildren()) {
                        if (currentUserId.equals(userEntry.getValue(String.class))) {
                            // We've been matched!
                            isMatched = true;
                            isSearching = false;
                            waitingRoomRef.child(currentUserId).removeValue();
                            openChatRoom(room.getKey());
                            return;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                showError("Connection error. Try again.");
                resetUI();
            }
        };

        chatsRef.addValueEventListener(matchListener);
    }

    private void openChatRoom(String chatRoomId) {
        removeListeners();

        Intent intent = new Intent(MatchmakingActivity.this, ChatActivity.class);
        intent.putExtra("chatRoomId", chatRoomId);
        intent.putExtra("currentUserId", currentUserId);
//        intent.putExtra("currentUserEmail", currentUserEmail);
        startActivity(intent);
        finish();

        resetUI();
    }

    private void removeListeners() {
        if (matchListener != null && myMatchRef != null) {
            chatsRef.removeEventListener(matchListener);
            matchListener = null;
        }
    }

    private void resetUI() {
        isSearching = false;
        runOnUiThread(() -> {
            findChatButton.setEnabled(true);
            progressBar.setVisibility(View.GONE);
            statusText.setText("Press the button to find someone to chat with!");
        });
    }

    private void showError(String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }

   /* private void logout() {

        waitingRoomRef.child(currentUserId).removeValue();
        removeListeners();
        mAuth.signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }*/

    @Override
    protected void onStop() {
        super.onStop();
        isSearching = false;
        removeListeners();
        waitingRoomRef.child(currentUserId).removeValue();
        resetUI();
    }
}
