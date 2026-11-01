package com.example.udgh.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.udgh.repositories.RegistrationRepository;

public class RegistrationViewModel extends ViewModel {
    private final RegistrationRepository repository = new RegistrationRepository();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> registered = new MutableLiveData<>();

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getRegistered() { return registered; }

    public void register(String fullName, String email, String password, String role) {
        loading.setValue(true);
        error.setValue(null);
        registered.setValue(null);
        repository.register(fullName, email, password, role, new RegistrationRepository.Callback() {
            @Override public void onSuccess() {
                loading.postValue(false);
                registered.postValue(true);
            }
            @Override public void onError(String message) {
                loading.postValue(false);
                error.postValue(message);
            }
        });
    }
}
