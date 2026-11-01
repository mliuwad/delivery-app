package com.example.udgh.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.udgh.repositories.SalaryShipperRepository;

public class SalaryShipperViewModel extends ViewModel {
    private final SalaryShipperRepository repository = new SalaryShipperRepository();
    private final MutableLiveData<SalaryShipperRepository.SalaryData> salary = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private int requestId;
    public LiveData<SalaryShipperRepository.SalaryData> getSalary() { return salary; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public void load(String shipperId, int month, int year) {
        int request = ++requestId;
        loading.setValue(true);
        error.setValue(null);
        repository.load(shipperId, month, year, new SalaryShipperRepository.Callback() {
            @Override public void onSuccess(SalaryShipperRepository.SalaryData data) {
                if (request != requestId) return;
                salary.postValue(data);
                loading.postValue(false);
            }
            @Override public void onError(Exception exception) {
                if (request != requestId) return;
                error.postValue(exception != null ? exception.getMessage() : "Không thể tải dữ liệu lương");
                loading.postValue(false);
            }
        });
    }
}
