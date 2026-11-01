package com.example.udgh;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.Locale;

public class CreatorHomeFragment extends Fragment {
    private FirebaseFirestore db;
    private String userId;
    private TextView txtCreatorName, txtTotalOrders, txtTotalProductValue;
    private TextView txtWaiting, txtDelivering, txtCompleted;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_creator_home, container, false);
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        txtCreatorName = view.findViewById(R.id.txtCreatorName);
        txtTotalOrders = view.findViewById(R.id.txtTotalOrders);
        txtTotalProductValue = view.findViewById(R.id.txtTotalProductValue);
        txtWaiting = view.findViewById(R.id.txtWaiting);
        txtDelivering = view.findViewById(R.id.txtDelivering);
        txtCompleted = view.findViewById(R.id.txtCompleted);

        view.findViewById(R.id.btnCreateOrder).setOnClickListener(v ->
                startActivity(new android.content.Intent(requireContext(), CreateOrderActivity.class)));
        view.findViewById(R.id.btnViewOrders).setOnClickListener(v -> {
            if (getActivity() instanceof HomeCreatorActivity) {
                ((HomeCreatorActivity) getActivity()).openOrdersTab();
            }
        });

        loadCreatorInfo();
        loadStatistics();
        return view;
    }

    private void loadCreatorInfo() {
        if (TextUtils.isEmpty(userId)) return;
        db.collection("USERS").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (isAdded() && doc.exists()) {
                        String name = doc.getString("fullName");
                        txtCreatorName.setText(TextUtils.isEmpty(name) ? "Cửa hàng của bạn" : name);
                    }
                });
    }

    private void loadStatistics() {
        if (TextUtils.isEmpty(userId)) return;
        db.collection("ORDERS").whereEqualTo("creatorId", userId).get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;
                    int waiting = 0, delivering = 0, completed = 0;
                    double productValue = 0;
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String status = doc.getString("status");
                        if ("PENDING".equals(status) || "WAITING".equals(status)) waiting++;
                        else if ("DELIVERING".equals(status)) delivering++;
                        else if ("COMPLETED".equals(status)) completed++;

                        Number amount = (Number) doc.get("totalProductValue");
                        if (amount != null) productValue += amount.doubleValue();
                    }
                    txtTotalOrders.setText(String.valueOf(snapshot.size()));
                    txtTotalProductValue.setText(formatCurrency(productValue));
                    txtWaiting.setText(String.valueOf(waiting));
                    txtDelivering.setText(String.valueOf(delivering));
                    txtCompleted.setText(String.valueOf(completed));
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) Toast.makeText(requireContext(),
                            "Không thể tải thống kê: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String formatCurrency(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return format.format(amount);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStatistics();
    }
}