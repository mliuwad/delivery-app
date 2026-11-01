package com.example.udgh.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.udgh.repositories.SessionRepository;

public class SessionViewModel extends ViewModel {
    private final SessionRepository repository = new SessionRepository();
    private final MutableLiveData<String> role = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private int requestId;

    public LiveData<String> getRole() { return role; }
    public LiveData<String> getError() { return error; }
    public String getCurrentUserId() { return repository.getCurrentUserId(); }

    public void validateCurrentSession() {
        int currentRequest = ++requestId;
        error.setValue(null);
        repository.validateCurrentSession(new SessionRepository.Callback() {
            @Override public void onSuccess(String value) {
                if (currentRequest == requestId) role.postValue(value);
            }
            @Override public void onError(String message) {
                if (currentRequest == requestId) error.postValue(message);
            }
        });
    }

    public void signOut() {
        repository.signOut();
        role.setValue(null);
    }
}
