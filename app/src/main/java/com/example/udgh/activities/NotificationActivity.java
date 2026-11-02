package com.example.udgh.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.udgh.R;
import com.example.udgh.utils.RoleAccess;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class NotificationActivity extends AppCompatActivity {
    private ListenerRegistration listener;
    private RecyclerView recycler;
    private ProgressBar loading;
    private TextView empty;
    private final List<DocumentSnapshot> orders = new ArrayList<>();
    private RecyclerView.Adapter<NotificationRow> adapter;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        // Hiển thị giao diện ngay
        setContentView(R.layout.activity_notification);
        RoleAccess.requireRole(this, "SHIPPER", this::initialize);
    }

    private void initialize() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        android.widget.LinearLayout root = (android.widget.LinearLayout) toolbar.getParent();
        loading = new ProgressBar(this);
        root.addView(loading, 1);
        empty = new TextView(this);
        empty.setText("Chưa có đơn hàng chờ nhận");
        empty.setPadding(24, 24, 24, 24);
        root.addView(empty, 2);
        empty.setVisibility(View.GONE);

        recycler = findViewById(R.id.rvNotification);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecyclerView.Adapter<NotificationRow>() {
            @Override
            public NotificationRow onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
                View view = getLayoutInflater().inflate(R.layout.item_notification, parent, false);
                return new NotificationRow(view);
            }

            @Override
            public void onBindViewHolder(NotificationRow holder, int position) {
                DocumentSnapshot document = orders.get(position);
                String value = document.getString("orderId");
                final String orderId = value == null || value.isEmpty() ? document.getId() : value;
                holder.title.setText("Có đơn hàng chờ nhận");
                holder.content.setText("Đơn " + orderId + " đang chờ shipper nhận. Nhấn để xem chi tiết.");
                Timestamp timestamp = document.getTimestamp("createdAt");
                holder.time.setText(timestamp == null ? "" :
                        new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(timestamp.toDate()));
                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(NotificationActivity.this, ShipperOrderDetailActivity.class);
                    intent.putExtra("orderId", orderId);
                    startActivity(intent);
                });
            }

            @Override
            public int getItemCount() {
                return orders.size();
            }
        };
        recycler.setAdapter(adapter);

        listener = FirebaseFirestore.getInstance().collection("ORDERS")
                .whereIn("status", Arrays.asList("PENDING", "WAITING"))
                .addSnapshotListener((snapshot, error) -> {
                    loading.setVisibility(View.GONE);
                    if (error != null) {
                        Toast.makeText(this, "Không tải được thông báo: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    orders.clear();
                    if (snapshot != null) {
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            String shipperId = document.getString("shipperId");
                            if (shipperId == null || shipperId.trim().isEmpty()) {
                                orders.add(document);
                            }
                        }
                    }
                    orders.sort((a, b) -> {
                        Timestamp first = a.getTimestamp("createdAt");
                        Timestamp second = b.getTimestamp("createdAt");
                        if (first == null) return second == null ? 0 : 1;
                        if (second == null) return -1;
                        return second.compareTo(first);
                    });
                    adapter.notifyDataSetChanged();
                    empty.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    static class NotificationRow extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView content;
        final TextView time;

        NotificationRow(View item) {
            super(item);
            title = item.findViewById(R.id.txtTitle);
            content = item.findViewById(R.id.txtContent);
            time = item.findViewById(R.id.txtTime);
        }
    }

    @Override
    protected void onDestroy() {
        if (listener != null) listener.remove();
        super.onDestroy();
    }
}
