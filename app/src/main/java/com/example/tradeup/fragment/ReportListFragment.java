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

import com.example.tradeup.R;
import com.example.tradeup.model.Report; // Assuming you have a Report model
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.time.OffsetDateTime; // Import for parsing ISO 8601 string
import java.time.format.DateTimeParseException; // Import for handling parsing errors

public class ReportListFragment extends Fragment {

    private static final String TAG = "ReportListFragment";

    private RecyclerView recyclerView;
    private ReportAdapter adapter;
    private TextView textNoReports;
    private ImageView ivBackButton;

    private NavController navController;
    private List<Report> reportList;

    public ReportListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reportList = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report_list, container, false);
        initViews(view);
        setupListeners();
        setupRecyclerView();
        loadReports();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_reports);
        textNoReports = view.findViewById(R.id.text_no_reports);
        ivBackButton = view.findViewById(R.id.iv_back_button_report_list);
    }

    private void setupListeners() {
        ivBackButton.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigateUp(); // Go back to the previous fragment
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new ReportAdapter(reportList, new ReportAdapter.OnReportClickListener() {
            @Override
            public void onReportClick(Report report) {
                // Handle report item click, navigate to ReportDetailFragment
                Toast.makeText(getContext(), "Report Clicked: " + report.getReport_id(), Toast.LENGTH_SHORT).show(); // Corrected getter
                if (navController != null) {
                    // Pass report ID or object to ReportDetailFragment
                    Bundle bundle = new Bundle();
                    bundle.putString("reportId", report.getReport_id()); // Corrected getter
                    // You might also pass other details if needed, but fetching from ID is robust
                    navController.navigate(R.id.action_reportListFragment_to_reportDetailFragment, bundle);
                }
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadReports() {
        DatabaseReference reportsRef = FirebaseDatabase.getInstance().getReference("reports");
        reportsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded()) { // Ensure fragment is attached
                    reportList.clear();
                    for (DataSnapshot reportSnapshot : snapshot.getChildren()) {
                        Report report = reportSnapshot.getValue(Report.class);
                        if (report != null) {
                            // Set the reportId from the key
                            report.setReport_id(reportSnapshot.getKey()); // Corrected setter
                            reportList.add(report);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load reports: " + error.getMessage());
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to load reports: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                }
            }
        });
    }

    private void updateEmptyState() {
        if (isAdded()) {
            if (reportList.isEmpty()) {
                textNoReports.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                textNoReports.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    // Adapter for the RecyclerView
    private static class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

        private List<Report> reports;
        private OnReportClickListener listener;

        public interface OnReportClickListener {
            void onReportClick(Report report);
        }

        public ReportAdapter(List<Report> reports, OnReportClickListener listener) {
            this.reports = reports;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
            return new ReportViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
            Report report = reports.get(position);
            holder.bind(report, listener);
        }

        @Override
        public int getItemCount() {
            return reports.size();
        }

        static class ReportViewHolder extends RecyclerView.ViewHolder {
            TextView tvReportType, tvReportReason, tvReportStatus, tvReportTimestamp;
            // Add ImageView for reported user/item if needed

            public ReportViewHolder(@NonNull View itemView) {
                super(itemView);
                tvReportType = itemView.findViewById(R.id.tv_report_type);
                tvReportReason = itemView.findViewById(R.id.tv_report_reason);
                tvReportStatus = itemView.findViewById(R.id.tv_report_status);
                tvReportTimestamp = itemView.findViewById(R.id.tv_report_timestamp);
            }

            public void bind(final Report report, final OnReportClickListener listener) {
                tvReportType.setText("Loại: " + report.getReport_type()); // Corrected getter
                tvReportReason.setText("Lý do: " + report.getReason());
                tvReportStatus.setText("Trạng thái: " + report.getStatus());

                // FIX: Parse String timestamp to long for Date constructor
                String timestampString = report.getCreated_at(); // Use getCreated_at()
                long timestampMillis = 0;
                if (timestampString != null && !timestampString.isEmpty()) {
                    try {
                        // Assuming ISO 8601 format like "yyyy-MM-dd'T'HH:mm:ss'Z'"
                        timestampMillis = OffsetDateTime.parse(timestampString).toInstant().toEpochMilli();
                    } catch (DateTimeParseException e) {
                        Log.e(TAG, "Error parsing timestamp string: " + timestampString, e);
                        // Fallback to current time or 0 if parsing fails
                        timestampMillis = System.currentTimeMillis();
                    }
                } else {
                    // If timestampString is null or empty, use current time or a default
                    timestampMillis = System.currentTimeMillis();
                }

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                String formattedDate = sdf.format(new Date(timestampMillis));
                tvReportTimestamp.setText("Thời gian: " + formattedDate);

                itemView.setOnClickListener(v -> listener.onReportClick(report));
            }
        }
    }
}
