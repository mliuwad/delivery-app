package com.example.udgh.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.udgh.repositories.UserAvatarRepository;

public class UserAvatarViewModel extends ViewModel {
    private final UserAvatarRepository repository = new UserAvatarRepository();
    private final MutableLiveData<String> avatarUrl = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private int requestId;

    public LiveData<String> getAvatarUrl() { return avatarUrl; }
    public LiveData<String> getError() { return error; }

    public void loadAvatar(String userId) {
        int currentRequest = ++requestId;
        repository.loadAvatar(userId, new UserAvatarRepository.Callback() {
            @Override public void onSuccess(String url) {
                if (currentRequest == requestId) avatarUrl.postValue(url);
            }
            @Override public void onError(Exception exception) {
                if (currentRequest == requestId) {
                    error.postValue(exception != null ? exception.getMessage() : "Không thể tải ảnh đại diện");
                }
            }
        });
    }
}
