package com.example.udgh.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.udgh.models.Order;
import com.example.udgh.repositories.OrderRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderViewModel extends ViewModel {

    private final OrderRepository repository;

    private final MutableLiveData<Order> selectedOrder = new MutableLiveData<>();
    private int orderDetailRequestId = 0;

    public LiveData<Order> getSelectedOrder() {
        return selectedOrder;
    }

    public void loadOrderById(String orderId) {
        int currentRequest = ++orderDetailRequestId;
        isLoading.setValue(true);
        errorMessage.setValue(null);

        repository.getOrderById(orderId, new OrderRepository.ResultCallback<Order>() {
            @Override
            public void onSuccess(Order order) {
                if (currentRequest != orderDetailRequestId) return;
                selectedOrder.postValue(order);
                isLoading.postValue(false);
            }

            @Override
            public void onError(Exception e) {
                if (currentRequest != orderDetailRequestId) return;
                isLoading.postValue(false);
                errorMessage.postValue(e != null && e.getMessage() != null
                        ? e.getMessage() : "Không thể tải thông tin đơn hàng");
            }
        });
    }

    private final MutableLiveData<List<Order>> orders =
            new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<Boolean> isLoading =
            new MutableLiveData<>(false);

    private final MutableLiveData<String> errorMessage =
            new MutableLiveData<>();

    private final MutableLiveData<String> userRole = new MutableLiveData<>();
    private final MutableLiveData<OrderActionResult> actionResult = new MutableLiveData<>();

    private final MutableLiveData<Integer> totalOrdersCount =
            new MutableLiveData<>(0);

    private final List<Order> allOrders = new ArrayList<>();

    private String currentFilter = "ALL";
    private String currentKeyword = "";

    private int requestId = 0;

    public OrderViewModel() {
        repository = new OrderRepository();
    }

    public LiveData<List<Order>> getOrders() {
        return orders;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getUserRole() {
        return userRole;
    }

    public LiveData<OrderActionResult> getActionResult() {
        return actionResult;
    }

    public void loadUserRole(String userId) {
        repository.loadUserRole(userId, (role, error) -> {
            if (error == null) userRole.postValue(role);
            else actionResult.postValue(new OrderActionResult(false, null, error));
        });
    }

    public void updateOrderStatus(String orderId, String userId, String status) {
        repository.updateOrderStatus(orderId, userId, status,
                (success, newStatus, error) ->
                        actionResult.postValue(new OrderActionResult(success, newStatus, error)));
    }

    public static class OrderActionResult {
        private final boolean success;
        private final String status;
        private final Exception error;

        public OrderActionResult(boolean success, String status, Exception error) {
            this.success = success;
            this.status = status;
            this.error = error;
        }

        public boolean isSuccess() { return success; }
        public String getStatus() { return status; }
        public Exception getError() { return error; }
    }

    public LiveData<Integer> getTotalOrdersCount() {
        return totalOrdersCount;
    }

    public void loadOrders(String status) {
        currentFilter = status;

        // Mỗi lần tải tạo một mã yêu cầu mới.
        int currentRequestId = ++requestId;

        isLoading.setValue(true);
        errorMessage.setValue(null);

        repository.getOrders(status, new OrderRepository.ResultCallback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> result) {
                // Bỏ qua kết quả của lần tải cũ.
                if (currentRequestId != requestId) {
                    return;
                }

                allOrders.clear();

                if (result != null) {
                    allOrders.addAll(result);
                }

                totalOrdersCount.setValue(allOrders.size());

                applySearch();

                isLoading.setValue(false);
            }

            @Override
            public void onError(Exception e) {
                if (currentRequestId != requestId) {
                    return;
                }

                isLoading.setValue(false);

                String message = e != null
                        ? e.getMessage()
                        : "Không thể tải danh sách đơn hàng";

                errorMessage.setValue(message);
            }
        });
    }

    public void loadCreatorOrders(String creatorId) {
        currentFilter = "ALL";
        int currentRequestId = ++requestId;
        isLoading.setValue(true);
        errorMessage.setValue(null);

        repository.getOrdersByCreator(creatorId, new OrderRepository.ResultCallback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> result) {
                if (currentRequestId != requestId) return;
                allOrders.clear();
                if (result != null) allOrders.addAll(result);
                totalOrdersCount.setValue(allOrders.size());
                applySearch();
                isLoading.setValue(false);
            }

            @Override
            public void onError(Exception e) {
                if (currentRequestId != requestId) return;
                isLoading.setValue(false);
                errorMessage.setValue(e != null && e.getMessage() != null
                        ? e.getMessage() : "Không thể tải đơn hàng của Creator");
            }
        });
    }

    public void setStatusFilter(String status) {
        currentFilter = status == null ? "ALL" : status;
        applySearch();
    }

    public void searchOrders(String keyword) {
        currentKeyword = keyword != null ? keyword.trim() : "";
        applySearch();
    }

    private void applySearch() {
        List<Order> filteredOrders = new ArrayList<>();

        if (currentKeyword.isEmpty()) {
            for (Order order : allOrders) {
                if (order != null && matchesCurrentStatus(order)) {
                    filteredOrders.add(order);
                }
            }
        } else {
            String searchText = currentKeyword.toLowerCase(Locale.ROOT);

            for (Order order : allOrders) {
                if (order == null) {
                    continue;
                }

                String orderId = order.getOrderId() != null
                        ? order.getOrderId().toLowerCase(Locale.ROOT)
                        : "";

                String receiverName = order.getReceiverName() != null
                        ? order.getReceiverName().toLowerCase(Locale.ROOT)
                        : "";

                String senderName = order.getSenderName() != null
                        ? order.getSenderName().toLowerCase(Locale.ROOT)
                        : "";

                boolean keywordMatches = orderId.contains(searchText)
                        || receiverName.contains(searchText)
                        || senderName.contains(searchText);
                if (matchesCurrentStatus(order) && keywordMatches) {
                    filteredOrders.add(order);
                }
            }
        }

        orders.setValue(filteredOrders);
    }

    private boolean matchesCurrentStatus(Order order) {
        if ("ALL".equals(currentFilter)) return true;
        if ("PENDING".equals(currentFilter)) {
            return "PENDING".equals(order.getStatus()) || "WAITING".equals(order.getStatus());
        }
        return currentFilter.equals(order.getStatus());
    }

    public void refreshOrders() {
        loadOrders(currentFilter);
    }
}