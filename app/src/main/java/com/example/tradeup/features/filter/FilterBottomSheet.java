package com.example.tradeup.features.filter;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import com.example.tradeup.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

public class FilterBottomSheet extends BottomSheetDialogFragment {

    public static FilterBottomSheet newInstance() {
        return new FilterBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout bộ lọc mới
        return inflater.inflate(R.layout.bottom_sheet_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        CheckBox checkboxUseGPS = view.findViewById(R.id.checkbox_use_gps);
        SeekBar seekbarDistance = view.findViewById(R.id.seekbar_distance);
        TextView tvDistance = view.findViewById(R.id.tv_distance_value);
        Button btnApply = view.findViewById(R.id.btn_apply_filter);

        seekbarDistance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvDistance.setText("Khoảng cách: " + progress + " km");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnApply.setOnClickListener(v -> {
            // TODO: xử lý dữ liệu lọc ở đây (nếu cần)
            dismiss();
        });
    }
}
