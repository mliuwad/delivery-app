package com.example.udgh.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.udgh.repositories.CreateShipperRepository;

public class CreateShipperViewModel extends AndroidViewModel {
    private final CreateShipperRepository repository = new CreateShipperRepository();
    private final MutableLiveData<Boolean> success = new MutableLiveData<>();
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public CreateShipperViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Boolean> getSuccess() { return success; }
    public LiveData<String> getMessage() { return message; }

    public void create(String fullName, String phone, String email, String password, String status,
                       String avatarUrl, String citizenId, String licensePlate, String address) {
        success.setValue(null);
        repository.create(getApplication(), fullName, phone, email, password, status,
                avatarUrl, citizenId, licensePlate, address, new CreateShipperRepository.Callback() {
                    @Override
                    public void onSuccess() {
                        success.postValue(true);
                        message.postValue("Thêm nhân viên thành công!");
                    }

                    @Override
                    public void onError(Exception error) {
                        success.postValue(false);
                        message.postValue(error != null ? error.getMessage() : "Không thể tạo tài khoản");
                    }
                });
    }
}
