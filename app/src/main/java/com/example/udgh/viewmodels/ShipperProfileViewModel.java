package com.example.udgh.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.udgh.repositories.ShipperProfileRepository;

public class ShipperProfileViewModel extends ViewModel {

    private final ShipperProfileRepository repository = new ShipperProfileRepository();
    private final MutableLiveData<ShipperProfileRepository.ProfileData> profile = new MutableLiveData<>();
    private final MutableLiveData<String> operationMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operationSuccess = new MutableLiveData<>();
    private final MutableLiveData<Boolean> profileSaved = new MutableLiveData<>();
    private int loadRequestId;

    public LiveData<ShipperProfileRepository.ProfileData> getProfile() {
        return profile;
    }

    public LiveData<String> getOperationMessage() {
        return operationMessage;
    }

    public LiveData<Boolean> getOperationSuccess() {
        return operationSuccess;
    }

    public LiveData<Boolean> getProfileSaved() {
        return profileSaved;
    }

    public void loadProfile(String userId) {
        int requestId = ++loadRequestId;
        repository.loadProfile(userId, new ShipperProfileRepository.Callback<ShipperProfileRepository.ProfileData>() {
            @Override
            public void onSuccess(ShipperProfileRepository.ProfileData data) {
                if (requestId == loadRequestId) profile.postValue(data);
            }

            @Override
            public void onError(Exception error) {
                if (requestId == loadRequestId) {
                    operationSuccess.postValue(false);
                    operationMessage.postValue(error != null ? error.getMessage() : "Không thể tải hồ sơ");
                }
            }
        });
    }

    public void saveProfile(String userId, String detailId,
                            String fullName, String phone, String email,
                            String citizenId, String licensePlate, String address) {
        operationSuccess.setValue(null);
        profileSaved.setValue(null);
        repository.saveProfile(userId, detailId, fullName, phone, email,
                citizenId, licensePlate, address, new ShipperProfileRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        operationSuccess.postValue(true);
                        profileSaved.postValue(true);
                        operationMessage.postValue("Cập nhật thông tin thành công");
                    }

                    @Override
                    public void onError(Exception error) {
                        operationSuccess.postValue(false);
                        profileSaved.postValue(false);
                        operationMessage.postValue(error != null ? error.getMessage() : "Không thể cập nhật hồ sơ");
                    }
                });
    }

    public void updateAvatar(String userId, String avatarUrl) {
        operationSuccess.setValue(null);
        repository.updateAvatar(userId, avatarUrl, new ShipperProfileRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                operationSuccess.postValue(true);
                operationMessage.postValue("Đã cập nhật ảnh đại diện");
            }

            @Override
            public void onError(Exception error) {
                operationSuccess.postValue(false);
                operationMessage.postValue(error != null ? error.getMessage() : "Không thể lưu ảnh đại diện");
            }
        });
    }
}
