package com.example.tradeup.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.tradeup.R;

public class SellFragment extends Fragment {

    public SellFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sell, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Không cần gọi Navigation.findNavController(view) ở đây
        // NavController cho SellFragment sẽ là NavController của Main Activity.
        // NavController cho các fragment con bên trong sell_nav_host_fragment sẽ khác.
    }
}