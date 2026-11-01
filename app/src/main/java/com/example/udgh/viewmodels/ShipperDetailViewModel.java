package com.example.udgh.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.udgh.repositories.ShipperDetailRepository;

public class ShipperDetailViewModel extends ViewModel {
    private final ShipperDetailRepository repository = new ShipperDetailRepository();
    private final MutableLiveData<ShipperDetailRepository.DetailData> detail = new MutableLiveData<>();
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operationSuccess = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleted = new MutableLiveData<>();
    private int loadRequest;

    public LiveData<ShipperDetailRepository.DetailData> getDetail() { return detail; }
    public LiveData<String> getMessage() { return message; }
    public LiveData<Boolean> getOperationSuccess() { return operationSuccess; }
    public LiveData<Boolean> getDeleted() { return deleted; }

    public void load(String shipperId) {
        int request = ++loadRequest;
        repository.load(shipperId, new ShipperDetailRepository.Callback<ShipperDetailRepository.DetailData>() {
            @Override public void onSuccess(ShipperDetailRepository.DetailData value) {
                if (request == loadRequest) detail.postValue(value);
            }
            @Override public void onError(Exception error) {
                if (request == loadRequest) message.postValue(error != null ? error.getMessage() : "Không thể tải hồ sơ");
            }
        });
    }

    public void save(String shipperId, String detailId, String fullName, String phone, String email,
                     String citizenId, String licensePlate, String address) {
        operationSuccess.setValue(null);
        repository.save(shipperId, detailId, fullName, phone, email, citizenId, licensePlate, address,
                new ShipperDetailRepository.Callback<Void>() {
                    @Override public void onSuccess(Void ignored) {
                        operationSuccess.postValue(true);
                        message.postValue("Cập nhật thành công");
                        load(shipperId);
                    }
                    @Override public void onError(Exception error) {
                        operationSuccess.postValue(false);
                        message.postValue(error != null ? error.getMessage() : "Không thể cập nhật");
                    }
                });
    }

    public void updateAvatar(String shipperId, String url) {
        repository.updateAvatar(shipperId, url, new ShipperDetailRepository.Callback<Void>() {
            @Override public void onSuccess(Void ignored) { message.postValue("Cập nhật ảnh đại diện thành công"); }
            @Override public void onError(Exception error) { message.postValue(error != null ? error.getMessage() : "Không thể cập nhật ảnh"); }
        });
    }

    public void toggleStatus(String shipperId, String currentStatus) {
        operationSuccess.setValue(null);
        String next = "ACTIVE".equals(currentStatus) ? "INACTIVE" : "ACTIVE";
        repository.updateStatus(shipperId, next, new ShipperDetailRepository.Callback<Void>() {
            @Override public void onSuccess(Void ignored) {
                operationSuccess.postValue(true);
                message.postValue("Đã cập nhật trạng thái");
                load(shipperId);
            }
            @Override public void onError(Exception error) {
                operationSuccess.postValue(false);
                message.postValue(error != null ? error.getMessage() : "Không thể cập nhật trạng thái");
            }
        });
    }

    public void delete(String shipperId, String detailId) {
        repository.deleteProfile(shipperId, detailId, new ShipperDetailRepository.Callback<Void>() {
            @Override public void onSuccess(Void ignored) {
                deleted.postValue(true);
                message.postValue("Đã xóa dữ liệu nhân viên");
            }
            @Override public void onError(Exception error) {
                deleted.postValue(false);
                message.postValue(error != null ? error.getMessage() : "Không thể xóa nhân viên");
            }
        });
    }
}
