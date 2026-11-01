package com.example.udgh;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.udgh.activities.AddEditProductActivity;
import com.example.udgh.models.Order;
import com.example.udgh.models.OrderDetail;
import com.example.udgh.viewmodels.OrderViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.example.udgh.viewmodels.SessionViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreateOrderActivity extends AppCompatActivity {

    // ================== KHAI BÁO ==================

    private MaterialToolbar toolbar;
    private MaterialButton btnAction;

    // Sender
    private TextInputEditText edtSenderName, edtSenderPhone, edtSenderAddress;
    private MaterialButton btnChooseSenderLocation;

    // Receiver
    private TextInputEditText edtReceiverName, edtReceiverPhone, edtReceiverAddress;
    private MaterialButton btnChooseReceiverLocation;

    // Product
    private RecyclerView rvSelectedProducts;
    private SelectedProductAdapter selectedProductAdapter;
    private List<OrderDetail> selectedProductList;
    private MaterialButton btnChooseProduct;
    private TextView txtAddNewProduct;
    private TextView txtNoProduct;

    // Note
    private TextInputEditText edtNote;

    // Payment
    private TextView txtProductPrice, txtShippingFee, txtTotalPrice;
    private CheckBox cbCOD;
    private TextInputLayout layoutCOD;
    private TextInputEditText edtCOD;

    // Order info (chi tiết) - được include từ create_order_info.xml
    private View orderInfoLayout;
    private TextView tvOrderId, tvStatus, tvCreatedAt, tvDeliveredAt;
    private LinearLayout layoutDeliveredAt;

    // Firebase
    private SessionViewModel sessionViewModel;
    private String userId;
    private String userRole;
    private OrderViewModel orderViewModel;

    // Data
    private double deliveryFee = 0;
    private double senderLat = 0, senderLng = 0;
    private double receiverLat = 0, receiverLng = 0;
    private boolean hasSenderLocation = false;
    private boolean hasReceiverLocation = false;

    // Chế độ xem chi tiết
    private boolean isViewMode = false;
    private boolean isCreating = false;
    private String orderId = null;
    private Order currentOrder = null;
    private List<OrderDetail> orderDetailList = new ArrayList<>();

    // ================== LAUNCHER CHỌN VỊ TRÍ ==================

    private final ActivityResultLauncher<Intent> locationPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            String address = result.getData().getStringExtra("address");
                            double lat = result.getData().getDoubleExtra("lat", 0);
                            double lng = result.getData().getDoubleExtra("lng", 0);
                            String type = result.getData().getStringExtra("type");

                            if ("sender".equals(type)) {
                                edtSenderAddress.setText(address);
                                senderLat = lat;
                                senderLng = lng;
                                hasSenderLocation = true;
                                if (!isViewMode) calculateShippingFee();
                            } else if ("receiver".equals(type)) {
                                edtReceiverAddress.setText(address);
                                receiverLat = lat;
                                receiverLng = lng;
                                hasReceiverLocation = true;
                                if (!isViewMode) calculateShippingFee();
                            }
                        }
                    });

    // ================== ONCREATE ==================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_order);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sessionViewModel = new ViewModelProvider(this).get(SessionViewModel.class);

        // Lấy userId từ SharedPreferences hoặc SessionViewModel
        SharedPreferences sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        userId = sharedPref.getString("userId", "");
        if (TextUtils.isEmpty(userId)) userId = sessionViewModel.getCurrentUserId();

        // Kiểm tra nếu có orderId -> chế độ xem chi tiết
        orderId = getIntent().getStringExtra("orderId");
        isViewMode = !TextUtils.isEmpty(orderId);

        // ================== ÁNH XẠ VIEW ==================

        toolbar = findViewById(R.id.toolbar);
        btnAction = findViewById(R.id.btnCreateOrder);

        edtSenderName = findViewById(R.id.edtSenderName);
        edtSenderPhone = findViewById(R.id.edtSenderPhone);
        edtSenderAddress = findViewById(R.id.edtSenderAddress);
        btnChooseSenderLocation = findViewById(R.id.btnChooseSenderLocation);

        edtReceiverName = findViewById(R.id.edtReceiverName);
        edtReceiverPhone = findViewById(R.id.edtReceiverPhone);
        edtReceiverAddress = findViewById(R.id.edtReceiverAddress);
        btnChooseReceiverLocation = findViewById(R.id.btnChooseReceiverLocation);

        rvSelectedProducts = findViewById(R.id.rvSelectedProducts);
        btnChooseProduct = findViewById(R.id.btnChooseProduct);
        txtAddNewProduct = findViewById(R.id.txtAddNewProduct);
        txtNoProduct = findViewById(R.id.txtNoProduct);

        edtNote = findViewById(R.id.edtNote);

        txtProductPrice = findViewById(R.id.txtProductPrice);
        txtShippingFee = findViewById(R.id.txtShippingFee);
        txtTotalPrice = findViewById(R.id.txtTotalPrice);
        cbCOD = findViewById(R.id.cbCOD);
        layoutCOD = findViewById(R.id.layoutCOD);
        edtCOD = findViewById(R.id.edtCOD);

        // Ánh xạ view thông tin đơn hàng - các view này nằm trong include
        orderInfoLayout = findViewById(R.id.orderInfoLayout);
        tvOrderId = findViewById(R.id.tvOrderId);
        tvStatus = findViewById(R.id.tvStatus);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        tvDeliveredAt = findViewById(R.id.tvDeliveredAt);
        layoutDeliveredAt = findViewById(R.id.layoutDeliveredAt);

        // ================== SET TOOLBAR ==================

        setSupportActionBar(toolbar);
        if (isViewMode) {
            getSupportActionBar().setTitle("Chi tiết đơn hàng");
        } else {
            getSupportActionBar().setTitle("Tạo đơn hàng");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // ================== SETUP RECYCLERVIEW ==================

        selectedProductList = new ArrayList<>();
        selectedProductAdapter = new SelectedProductAdapter(selectedProductList, this::removeProduct, isViewMode);
        rvSelectedProducts.setLayoutManager(new LinearLayoutManager(this));
        rvSelectedProducts.setAdapter(selectedProductAdapter);

        orderViewModel = new ViewModelProvider(this).get(OrderViewModel.class);
        observeOrderActions();
        orderViewModel.loadUserRole(userId);

        // ================== SỰ KIỆN ==================

        btnChooseProduct.setOnClickListener(v -> {
            if (!isViewMode) showProductSelectionDialog();
        });

        txtAddNewProduct.setOnClickListener(v -> {
            if (!isViewMode) {
                Intent intent = new Intent(CreateOrderActivity.this, AddEditProductActivity.class);
                startActivity(intent);
            }
        });

        cbCOD.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutCOD.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) edtCOD.setText("");
            if (!isViewMode) updateTotalPrice();
        });

        btnAction.setOnClickListener(v -> {
            if (isViewMode) {
                handleActionButton();
            } else {
                createOrder();
            }
        });

        btnChooseSenderLocation.setOnClickListener(v -> {
            if (!isViewMode) {
                Intent intent = new Intent(CreateOrderActivity.this, LocationPickerActivity.class);
                intent.putExtra("type", "sender");
                locationPickerLauncher.launch(intent);
            }
        });

        btnChooseReceiverLocation.setOnClickListener(v -> {
            if (!isViewMode) {
                Intent intent = new Intent(CreateOrderActivity.this, LocationPickerActivity.class);
                intent.putExtra("type", "receiver");
                locationPickerLauncher.launch(intent);
            }
        });

        // Nếu là xem chi tiết -> load dữ liệu, ngược lại chế độ tạo mới
        if (isViewMode) {
            loadOrderDetails();
        } else {
            setupForCreateMode();
        }
    }

    // ================== LẤY ROLE TỪ FIRESTORE (LUÔN CẬP NHẬT) ==================

    private void observeOrderActions() {
        orderViewModel.getSelectedOrder().observe(this, order -> {
            if (order == null) return;
            currentOrder = order;
            displayOrderInfo();
            updateActionButton();

            selectedProductList.clear();
            if (order.getOrderDetails() != null) {
                selectedProductList.addAll(order.getOrderDetails());
            }
            selectedProductAdapter.notifyDataSetChanged();
            updateProductUI();
            updateTotalPriceViewMode();
        });

        orderViewModel.getErrorMessage().observe(this, message -> {
            if (message != null && !message.trim().isEmpty()) {
                Toast.makeText(this, "Lỗi tải đơn hàng: " + message, Toast.LENGTH_LONG).show();
                finish();
            }
        });

        orderViewModel.getCreateOrderResult().observe(this, result -> {
            if (result == null) return;
            isCreating = false;
            if (result.isSuccess()) {
                Toast.makeText(this, "Tạo đơn hàng " + orderId + " thành công!",
                        Toast.LENGTH_LONG).show();
                finish();
            } else {
                btnAction.setEnabled(true);
                btnAction.setText("XÁC NHẬN TẠO ĐƠN");
                String message = result.getError() != null
                        ? result.getError().getMessage()
                        : "Không thể tạo đơn hàng";
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                Log.e("CreateOrderActivity", "Create order failed", result.getError());
            }
        });

        orderViewModel.getUserRole().observe(this, role -> {
            userRole = role;
            Log.d("OrderDebug", "USER ROLE LOADED: [" + role + "]");
            if (currentOrder != null) updateActionButton();
            else Log.d("OrderDebug", "ROLE loaded before order data; waiting for currentOrder");
        });

        orderViewModel.getActionResult().observe(this, result -> {
            if (result == null) return;

            if (result.isSuccess()) {
                loadOrderDetails();
            } else {
                btnAction.setEnabled(true);
                updateActionButton();
                // Lỗi được giữ trong ViewModel; không bắt buộc hiển thị Toast.
                Log.e("CreateOrderActivity", "Order action failed",
                        result.getError());
            }
        });
    }

    // ================== CÀI ĐẶT CHẾ ĐỘ TẠO MỚI ==================

    private void setupForCreateMode() {
        orderInfoLayout.setVisibility(View.GONE);
        btnAction.setText("XÁC NHẬN TẠO ĐƠN");
        btnAction.setVisibility(View.VISIBLE);
        setAllInputsEnabled(true);
        btnChooseProduct.setVisibility(View.VISIBLE);
        txtAddNewProduct.setVisibility(View.VISIBLE);
        btnChooseSenderLocation.setVisibility(View.VISIBLE);
        btnChooseReceiverLocation.setVisibility(View.VISIBLE);
    }

    // ================== LOAD CHI TIẾT ĐƠN HÀNG ==================

    private void loadOrderDetails() {
        orderInfoLayout.setVisibility(View.VISIBLE);
        btnChooseProduct.setVisibility(View.GONE);
        txtAddNewProduct.setVisibility(View.GONE);
        btnChooseSenderLocation.setVisibility(View.GONE);
        btnChooseReceiverLocation.setVisibility(View.GONE);
        setAllInputsEnabled(false);
        orderViewModel.loadOrderById(orderId);
    }

    private void displayOrderInfo() {
        if (currentOrder == null) return;

        tvOrderId.setText("Đơn hàng: " + currentOrder.getOrderId());

        String status = currentOrder.getStatus();
        tvStatus.setText(getStatusText(status));
        tvStatus.setTextColor(getStatusColor(status));

        if (currentOrder.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            tvCreatedAt.setText(sdf.format(currentOrder.getCreatedAt().toDate()));
        } else {
            tvCreatedAt.setText("--");
        }

        if (currentOrder.getDeliveredAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            tvDeliveredAt.setText(sdf.format(currentOrder.getDeliveredAt().toDate()));
            layoutDeliveredAt.setVisibility(View.VISIBLE);
        } else {
            layoutDeliveredAt.setVisibility(View.GONE);
        }

        edtSenderName.setText(currentOrder.getSenderName());
        edtSenderPhone.setText(currentOrder.getSenderPhone());
        edtSenderAddress.setText(currentOrder.getSenderAddress());

        edtReceiverName.setText(currentOrder.getReceiverName());
        edtReceiverPhone.setText(currentOrder.getReceiverPhone());
        edtReceiverAddress.setText(currentOrder.getDeliveryAddress());

        edtNote.setText(currentOrder.getNote());

        deliveryFee = currentOrder.getDeliveryFee();

        boolean hasCOD = currentOrder.isCOD();
        cbCOD.setChecked(hasCOD);
        cbCOD.setEnabled(false);
        layoutCOD.setVisibility(hasCOD ? View.VISIBLE : View.GONE);
        edtCOD.setText(hasCOD
                ? String.valueOf((long) currentOrder.getCodAmount())
                : "");
        edtCOD.setEnabled(false);
        edtCOD.setFocusable(false);
    }

    private void updateTotalPriceViewMode() {
        if (currentOrder == null) return;
        double totalProductPrice = 0;
        for (OrderDetail detail : orderDetailList) {
            totalProductPrice += detail.getPrice() * detail.getQuantity();
        }
        txtProductPrice.setText(formatCurrency((int) totalProductPrice));
        txtShippingFee.setText(formatCurrency((int) deliveryFee));
        double total = totalProductPrice + deliveryFee;
        txtTotalPrice.setText(formatCurrency((int) total));
    }

    // ================== XỬ LÝ NÚT HÀNH ĐỘNG THEO ROLE ==================

    private void updateActionButton() {
        Log.d("OrderDebug", "========== UPDATE ACTION BUTTON ==========");
        Log.d("OrderDebug", "currentOrder is null: " + (currentOrder == null));
        Log.d("OrderDebug", "userId is empty: " + TextUtils.isEmpty(userId));
        Log.d("OrderDebug", "userRole: [" + userRole + "]");

        if (currentOrder == null) {
            Log.d("OrderDebug", "RESULT: currentOrder is NULL -> HIDE BUTTON");
            btnAction.setVisibility(View.GONE);
            return;
        }

        String status = currentOrder.getStatus();
        String role = userRole == null ? "" : userRole.trim().toUpperCase(Locale.ROOT);
        String shipperId = currentOrder.getShipperId();
        boolean shipperIdEmpty = TextUtils.isEmpty(shipperId)
                || shipperId.trim().isEmpty();

        Log.d("OrderDebug", "ORDER STATUS: [" + status + "]");
        Log.d("OrderDebug", "ROLE AFTER NORMALIZE: [" + role + "]");
        Log.d("OrderDebug", "ORDER HAS SHIPPER: " + !shipperIdEmpty);
        Log.d("OrderDebug", "CURRENT USER IS ASSIGNED SHIPPER: "
                + (userId != null && userId.equals(shipperId)));

        btnAction.setEnabled(true);

        if (TextUtils.isEmpty(role)) {
            Log.d("OrderDebug", "RESULT: ROLE EMPTY -> HIDE BUTTON");
            btnAction.setVisibility(View.GONE);
            return;
        }

        // Mặc định chỉ xem chi tiết, không có thao tác.
        btnAction.setVisibility(View.GONE);

        if ("CREATOR".equals(role)) {
            Log.d("OrderDebug", "ROLE = CREATOR");
            if ("PENDING".equals(status) || "WAITING".equals(status)) {
                Log.d("OrderDebug", "RESULT: SHOW CANCEL BUTTON");
                btnAction.setText("HỦY ĐƠN");
                btnAction.setVisibility(View.VISIBLE);
            } else {
                Log.d("OrderDebug", "RESULT: CREATOR STATUS NOT ALLOWED");
            }
            return;
        }

        if ("SHIPPER".equals(role)) {
            Log.d("OrderDebug", "ROLE = SHIPPER");
            boolean isPending = "PENDING".equals(status) || "WAITING".equals(status);
            boolean isMyOrder = userId != null && userId.equals(shipperId);

            Log.d("OrderDebug", "isPending: " + isPending);
            Log.d("OrderDebug", "isUnassigned: " + shipperIdEmpty);
            Log.d("OrderDebug", "isMyOrder: " + isMyOrder);

            if (isPending && shipperIdEmpty) {
                Log.d("OrderDebug", "RESULT: SHOW ACCEPT BUTTON");
                btnAction.setText("NHẬN ĐƠN");
                btnAction.setVisibility(View.VISIBLE);
                return;
            }

            if ("DELIVERING".equals(status) && isMyOrder) {
                Log.d("OrderDebug", "RESULT: SHOW COMPLETE BUTTON");
                btnAction.setText("HOÀN THÀNH");
                btnAction.setVisibility(View.VISIBLE);
                return;
            }

            Log.d("OrderDebug", "RESULT: SHIPPER CONDITIONS NOT MATCHED -> HIDE BUTTON");
            return;
        }

        Log.d("OrderDebug", "RESULT: UNKNOWN ROLE -> HIDE BUTTON");
    }

    private void handleActionButton() {
        if (currentOrder == null) return;
        String status = currentOrder.getStatus();
        String role = userRole != null ? userRole.toUpperCase() : "";

        if ("CREATOR".equals(role)) {
            updateOrderStatus("CANCELLED");
        } else if ("SHIPPER".equals(role)) {
            if ("PENDING".equals(status) || "WAITING".equals(status)) {
                updateOrderStatus("DELIVERING");
            } else if ("DELIVERING".equals(status)) {
                updateOrderStatus("COMPLETED");
            }
        }
    }

    // ================== CẬP NHẬT TRẠNG THÁI + LƯƠNG (THEO THÁNG NĂM) ==================

    private void updateOrderStatus(String newStatus) {
        if (TextUtils.isEmpty(orderId) || TextUtils.isEmpty(userId)
                || orderViewModel == null) {
            btnAction.setEnabled(true);
            updateActionButton();
            return;
        }

        btnAction.setEnabled(false);
        btnAction.setText("Đang cập nhật...");
        orderViewModel.updateOrderStatus(orderId, userId, newStatus);
    }

    // ================== TIỆN ÍCH ==================

    private String getStatusText(String status) {
        if (status == null) return "Không xác định";
        switch (status) {
            case "PENDING": return "Chờ nhận";
            case "WAITING": return "Chờ nhận";
            case "DELIVERING": return "Đang giao";
            case "COMPLETED": return "Hoàn thành";
            case "CANCELLED": return "Đã hủy";
            default: return status;
        }
    }

    private int getStatusColor(String status) {
        if (status == null) return getColor(R.color.gray);
        switch (status) {
            case "PENDING":
            case "WAITING":
                return getColor(R.color.warning);
            case "DELIVERING":
                return getColor(R.color.secondary);
            case "COMPLETED":
                return getColor(R.color.success);
            case "CANCELLED":
                return getColor(R.color.error);
            default:
                return getColor(R.color.gray);
        }
    }

    private void setAllInputsEnabled(boolean enabled) {
        edtSenderName.setEnabled(enabled);
        edtSenderPhone.setEnabled(enabled);
        edtSenderAddress.setEnabled(enabled);
        edtReceiverName.setEnabled(enabled);
        edtReceiverPhone.setEnabled(enabled);
        edtReceiverAddress.setEnabled(enabled);
        edtNote.setEnabled(enabled);
        cbCOD.setEnabled(enabled);
        edtCOD.setEnabled(enabled);
    }

    private void updateProductUI() {
        if (selectedProductList.isEmpty()) {
            txtNoProduct.setVisibility(View.VISIBLE);
            rvSelectedProducts.setVisibility(View.GONE);
        } else {
            txtNoProduct.setVisibility(View.GONE);
            rvSelectedProducts.setVisibility(View.VISIBLE);
        }
    }

    // ================== PHẦN TẠO ĐƠN ==================

    private void showProductSelectionDialog() {
        AddProductToOrderDialog dialog = new AddProductToOrderDialog(this, orderDetail -> {
            boolean exists = false;
            for (OrderDetail detail : selectedProductList) {
                if (detail.getProductId().equals(orderDetail.getProductId())) {
                    exists = true;
                    detail.setQuantity(detail.getQuantity() + orderDetail.getQuantity());
                    break;
                }
            }
            if (!exists) {
                selectedProductList.add(orderDetail);
            }
            selectedProductAdapter.notifyDataSetChanged();
            updateProductUI();
            updateTotalPrice();
            Toast.makeText(this, "Đã thêm " + orderDetail.getProductName() + " (x" + orderDetail.getQuantity() + ")", Toast.LENGTH_SHORT).show();
        });
        dialog.show();
    }

    private void removeProduct(OrderDetail product) {
        selectedProductList.remove(product);
        selectedProductAdapter.notifyDataSetChanged();
        updateProductUI();
        updateTotalPrice();
        Toast.makeText(this, "Đã xóa " + product.getProductName(), Toast.LENGTH_SHORT).show();
    }

    private void calculateShippingFee() {
        if (hasSenderLocation && hasReceiverLocation) {
            double distance = calculateDistance(senderLat, senderLng, receiverLat, receiverLng);
            if (distance <= 2) {
                deliveryFee = 15000;
            } else {
                deliveryFee = 15000 + (distance - 2) * 5000;
            }
            deliveryFee = Math.round(deliveryFee / 1000) * 1000;
            updateTotalPrice();
            Toast.makeText(this, "Khoảng cách: " + String.format("%.2f", distance) + " km\nPhí ship: " + formatCurrency((int) deliveryFee), Toast.LENGTH_LONG).show();
        }
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private void updateTotalPrice() {
        double totalProductPrice = 0;
        for (OrderDetail detail : selectedProductList) {
            totalProductPrice += detail.getPrice() * detail.getQuantity();
        }

        // COD là số tiền shipper thu hộ, bằng tiền hàng + phí vận chuyển.
        // COD không phải khoản phí cộng thêm vào tổng tiền đơn hàng.
        double totalToPay = totalProductPrice + deliveryFee;

        txtProductPrice.setText(formatCurrency((int) totalProductPrice));
        txtShippingFee.setText(formatCurrency((int) deliveryFee));
        txtTotalPrice.setText(formatCurrency((int) totalToPay));

        if (!isViewMode) {
            edtCOD.setText(cbCOD.isChecked()
                    ? String.valueOf((long) Math.round(totalToPay))
                    : "");
        }
    }

    private boolean isValidPhone(String phone) {
        if (TextUtils.isEmpty(phone)) return false;
        String phonePattern = "^0[0-9]{9}$";
        return phone.matches(phonePattern);
    }

    // Tạo mã đơn duy nhất theo thời điểm, không cần đọc toàn bộ danh sách ORDERS.
    private void generateOrderId() {
        String newOrderId = "OD" + System.currentTimeMillis();
        createOrderWithId(newOrderId);
    }

    private void createOrderWithId(String orderId) {
        String senderName = edtSenderName.getText().toString().trim();
        String senderPhone = edtSenderPhone.getText().toString().trim();
        String senderAddress = edtSenderAddress.getText().toString().trim();
        String receiverName = edtReceiverName.getText().toString().trim();
        String receiverPhone = edtReceiverPhone.getText().toString().trim();
        String receiverAddress = edtReceiverAddress.getText().toString().trim();
        String note = edtNote.getText().toString().trim();

        if (TextUtils.isEmpty(senderName)) {
            edtSenderName.setError("Vui lòng nhập tên người gửi");
            edtSenderName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(senderPhone) || !isValidPhone(senderPhone)) {
            edtSenderPhone.setError("Số điện thoại không hợp lệ (10 số, bắt đầu bằng 0)");
            edtSenderPhone.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(senderAddress)) {
            edtSenderAddress.setError("Vui lòng nhập địa chỉ người gửi");
            edtSenderAddress.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(receiverName)) {
            edtReceiverName.setError("Vui lòng nhập tên người nhận");
            edtReceiverName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(receiverPhone) || !isValidPhone(receiverPhone)) {
            edtReceiverPhone.setError("Số điện thoại không hợp lệ (10 số, bắt đầu bằng 0)");
            edtReceiverPhone.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(receiverAddress)) {
            edtReceiverAddress.setError("Vui lòng nhập địa chỉ nhận hàng");
            edtReceiverAddress.requestFocus();
            return;
        }
        if (!hasSenderLocation || !hasReceiverLocation) {
            Toast.makeText(this, "Vui lòng chọn vị trí trên bản đồ cho người gửi và người nhận", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedProductList.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 sản phẩm", Toast.LENGTH_SHORT).show();
            return;
        }

        for (OrderDetail detail : selectedProductList) {
            if (detail == null || TextUtils.isEmpty(detail.getProductId())
                    || detail.getQuantity() <= 0
                    || !Double.isFinite(detail.getPrice()) || detail.getPrice() < 0
                    || !Double.isFinite(detail.getWeight()) || detail.getWeight() < 0) {
                Toast.makeText(this, "Thông tin sản phẩm không hợp lệ. Vui lòng kiểm tra lại.",
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        double totalProductPrice = 0;
        for (OrderDetail detail : selectedProductList) {
            totalProductPrice += detail.getPrice() * detail.getQuantity();
        }
        if (!Double.isFinite(totalProductPrice) || !Double.isFinite(deliveryFee) || deliveryFee < 0) {
            Toast.makeText(this, "Giá trị đơn hàng không hợp lệ.", Toast.LENGTH_LONG).show();
            return;
        }
        double totalToPay = totalProductPrice + deliveryFee;
        boolean isCOD = cbCOD.isChecked();
        double codAmount = isCOD ? totalToPay : 0;

        btnAction.setEnabled(false);
        btnAction.setText("Đang tạo...");

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("creatorId", userId);
        orderData.put("shipperId", "");
        orderData.put("senderName", senderName);
        orderData.put("senderPhone", senderPhone);
        orderData.put("senderAddress", senderAddress);
        orderData.put("senderLat", senderLat);
        orderData.put("senderLng", senderLng);
        orderData.put("receiverName", receiverName);
        orderData.put("receiverPhone", receiverPhone);
        orderData.put("deliveryAddress", receiverAddress);
        orderData.put("receiverLat", receiverLat);
        orderData.put("receiverLng", receiverLng);
        orderData.put("deliveryFee", deliveryFee);
        orderData.put("isCOD", isCOD);
        orderData.put("codAmount", codAmount);
        orderData.put("totalProductValue", totalProductPrice);
        orderData.put("status", "PENDING");
        orderData.put("note", note);
        orderData.put("createdAt", Timestamp.now());
        orderData.put("deliveredAt", null);
        orderData.put("salaryAdded", false);

        isCreating = true;
        btnAction.setEnabled(false);
        btnAction.setText("Đang tạo...");

        // ViewModel điều phối; Repository ghi đơn và các dòng sản phẩm trong cùng batch.
        orderViewModel.createOrder(orderId, orderData, new ArrayList<>(selectedProductList));
    }

    private void createOrder() {
        if (isCreating) return;
        generateOrderId();
    }

    private String formatCurrency(int price) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return format.format(price);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isViewMode) {
            loadOrderDetails();
        }
    }
}