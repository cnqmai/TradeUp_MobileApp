package com.example.tradeup.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup.R;
import com.example.tradeup.adapter.ChatAdapter;
import com.example.tradeup.model.Chat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ChatListFragment extends Fragment implements ChatAdapter.OnChatClickListener {

    private static final String TAG = "ChatListFragment";

    private RecyclerView rvChatList;
    private TextView tvNoChats;
    private ChatAdapter chatAdapter;
    private List<Chat> chatList;

    private DatabaseReference chatsRef;
    private String currentUserId;
    private NavController navController;

    public ChatListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        chatsRef = FirebaseDatabase.getInstance().getReference("chats");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        initViews(view);
        setupRecyclerView();
        fetchChats();
    }

    private void initViews(View view) {
        rvChatList = view.findViewById(R.id.rv_chat_list);
        tvNoChats = view.findViewById(R.id.tv_no_chats);
    }

    private void setupRecyclerView() {
        chatList = new ArrayList<>();
        chatAdapter = new ChatAdapter(requireContext(), chatList, currentUserId, this);
        rvChatList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvChatList.setAdapter(chatAdapter);
    }

    private void fetchChats() {
        Query query = chatsRef.orderByChild("lastMessageTimestamp"); // Sort by last message timestamp

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Chat> fetchedChats = new ArrayList<>();
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    Chat chat = chatSnapshot.getValue(Chat.class);
                    if (chat != null) {
                        // Only add chats the current user is part of
                        if (chat.getUser_1().equals(currentUserId) || chat.getUser_2().equals(currentUserId)) {
                            // Firebase does not automatically set ID when getValue, need to set manually
                            chat.setChatId(chatSnapshot.getKey()); // Ensure Chat model has setId
                            fetchedChats.add(chat);
                        }
                    }
                }

                // Re-sort by lastMessageTimestamp in descending order (newest first)
                Collections.sort(fetchedChats, (c1, c2) -> {
                    String ts1 = c1.getLastMessageTimestamp();
                    String ts2 = c2.getLastMessageTimestamp();
                    if (ts1 == null && ts2 == null) return 0;
                    if (ts1 == null) return 1; // Put chats without timestamp at the end
                    if (ts2 == null) return -1;
                    return ts2.compareTo(ts1); // Reverse comparison for newest first
                });

                chatList.clear();
                chatList.addAll(fetchedChats);

                if (chatList.isEmpty()) {
                    tvNoChats.setVisibility(View.VISIBLE);
                    rvChatList.setVisibility(View.GONE);
                } else {
                    tvNoChats.setVisibility(View.GONE);
                    rvChatList.setVisibility(View.VISIBLE);
                }
                chatAdapter.setChats(chatList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load chats: " + error.getMessage());
                // Consider moving this string to strings.xml
                Toast.makeText(requireContext(), "Error loading chats: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onChatClick(Chat chat, String otherUserId, String otherUserName) {
        // Navigate to ChatDetailFragment when a chat is clicked
        Bundle bundle = new Bundle();
        bundle.putString("chatId", chat.getChatId()); // Get chat ID
        bundle.putString("otherUserId", otherUserId); // Get other user ID
        bundle.putString("otherUserName", otherUserName); // Get other user name
        navController.navigate(R.id.chatDetailFragment, bundle);
    }
}