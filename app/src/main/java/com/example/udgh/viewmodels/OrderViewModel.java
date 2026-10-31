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

    private final MutableLiveData<List<Order>> orders =
            new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<Boolean> isLoading =
            new MutableLiveData<>(false);

    private final MutableLiveData<String> errorMessage =
            new MutableLiveData<>();

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

    public void searchOrders(String keyword) {
        currentKeyword = keyword != null ? keyword.trim() : "";
        applySearch();
    }

    private void applySearch() {
        List<Order> filteredOrders = new ArrayList<>();

        if (currentKeyword.isEmpty()) {
            filteredOrders.addAll(allOrders);
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

                if (orderId.contains(searchText)
                        || receiverName.contains(searchText)
                        || senderName.contains(searchText)) {
                    filteredOrders.add(order);
                }
            }
        }

        orders.setValue(filteredOrders);
    }

    public void refreshOrders() {
        loadOrders(currentFilter);
    }
}