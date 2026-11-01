package com.example.udgh.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.udgh.models.User;
import com.example.udgh.repositories.ShipperManagementRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ShipperManagementViewModel extends ViewModel {

    private final ShipperManagementRepository repository = new ShipperManagementRepository();
    private final MutableLiveData<List<User>> shippers = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operationSuccess = new MutableLiveData<>();
    private final List<User> allShippers = new ArrayList<>();
    private String keyword = "";
    private int requestId;

    public LiveData<List<User>> getShippers() { return shippers; }
    public LiveData<String> getMessage() { return message; }
    public LiveData<Boolean> getOperationSuccess() { return operationSuccess; }

    public void loadShippers() {
        int currentRequest = ++requestId;
        repository.getShippers(new ShipperManagementRepository.Callback<List<User>>() {
            @Override
            public void onSuccess(List<User> result) {
                if (currentRequest != requestId) return;
                allShippers.clear();
                if (result != null) allShippers.addAll(result);
                applyFilter();
            }

            @Override
            public void onError(Exception error) {
                if (currentRequest == requestId) {
                    message.postValue(error != null ? error.getMessage() : "Không thể tải danh sách Shipper");
                }
            }
        });
    }

    public void search(String value) {
        keyword = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        applyFilter();
    }

    private void applyFilter() {
        List<User> filtered = new ArrayList<>();
        for (User user : allShippers) {
            if (keyword.isEmpty()
                    || contains(user.getFullName(), keyword)
                    || contains(user.getPhone(), keyword)
                    || contains(user.getEmail(), keyword)) {
                filtered.add(user);
            }
        }
        shippers.postValue(filtered);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    public void toggleStatus(User shipper) {
        if (shipper == null) return;
        String newStatus = "ACTIVE".equals(shipper.getStatus()) ? "INACTIVE" : "ACTIVE";
        operationSuccess.setValue(null);
        repository.updateShipperStatus(shipper.getUserId(), newStatus,
                new ShipperManagementRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        shipper.setStatus(newStatus);
                        applyFilter();
                        operationSuccess.postValue(true);
                        message.postValue("Cập nhật trạng thái thành công");
                    }

                    @Override
                    public void onError(Exception error) {
                        operationSuccess.postValue(false);
                        message.postValue(error != null ? error.getMessage() : "Không thể cập nhật trạng thái");
                    }
                });
    }
}
