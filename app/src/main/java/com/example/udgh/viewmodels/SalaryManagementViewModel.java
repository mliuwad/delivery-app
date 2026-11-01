package com.example.udgh.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.udgh.repositories.SalaryManagementRepository;

import java.util.ArrayList;
import java.util.List;

public class SalaryManagementViewModel extends ViewModel {
    private final SalaryManagementRepository repository = new SalaryManagementRepository();
    private final MutableLiveData<List<SalaryManagementRepository.SalaryRow>> salaries =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private int requestId = 0;

    public LiveData<List<SalaryManagementRepository.SalaryRow>> getSalaries() { return salaries; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }

    public void load(int month, int year) {
        int currentRequest = ++requestId;
        loading.setValue(true);
        error.setValue(null);
        repository.load(month, year, new SalaryManagementRepository.Callback<List<SalaryManagementRepository.SalaryRow>>() {
            @Override
            public void onSuccess(List<SalaryManagementRepository.SalaryRow> data) {
                if (currentRequest != requestId) return;
                salaries.postValue(data);
                loading.postValue(false);
            }

            @Override
            public void onError(Exception exception) {
                if (currentRequest != requestId) return;
                error.postValue(exception != null ? exception.getMessage() : "Không thể tải dữ liệu lương");
                loading.postValue(false);
            }
        });
    }
}
