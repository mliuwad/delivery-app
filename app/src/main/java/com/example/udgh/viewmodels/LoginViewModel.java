package com.example.udgh.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.udgh.repositories.AuthRepository;

public class LoginViewModel extends ViewModel {

    private final AuthRepository repository = new AuthRepository();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> role = new MutableLiveData<>();
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<Boolean> resetEmailSent = new MutableLiveData<>();

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getRole() { return role; }
    public LiveData<String> getMessage() { return message; }
    public LiveData<Boolean> getResetEmailSent() { return resetEmailSent; }

    public void login(String email, String password) {
        loading.setValue(true);
        role.setValue(null);
        message.setValue(null);

        repository.signIn(email, password, new AuthRepository.Callback<String>() {
            @Override
            public void onSuccess(String value) {
                loading.postValue(false);
                role.postValue(value);
            }

            @Override
            public void onError(String error) {
                loading.postValue(false);
                message.postValue(error);
            }
        });
    }

    public void sendPasswordReset(String email) {
        resetEmailSent.setValue(null);
        repository.sendPasswordReset(email, new AuthRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void ignored) {
                resetEmailSent.postValue(true);
                message.postValue("Đã gửi liên kết khôi phục vào Email: " + email
                        + "\nVui lòng kiểm tra hộp thư!");
            }

            @Override
            public void onError(String error) {
                resetEmailSent.postValue(false);
                message.postValue("Không thể gửi email khôi phục: " + error);
            }
        });
    }
}
