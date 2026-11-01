package com.example.udgh.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.udgh.models.Product;
import com.example.udgh.repositories.ProductSelectionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductSelectionViewModel extends ViewModel {
    private final ProductSelectionRepository repository = new ProductSelectionRepository();
    private final List<Product> allProducts = new ArrayList<>();
    private final MutableLiveData<List<Product>> products = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public LiveData<List<Product>> getProducts() { return products; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }

    public void loadProducts() {
        loading.setValue(true);
        error.setValue(null);
        repository.loadProductsForCurrentCreator(new ProductSelectionRepository.Callback() {
            @Override
            public void onSuccess(List<Product> result) {
                allProducts.clear();
                if (result != null) allProducts.addAll(result);
                products.postValue(new ArrayList<>(allProducts));
                loading.postValue(false);
            }

            @Override
            public void onError(Exception exception) {
                loading.postValue(false);
                error.postValue(exception != null && exception.getMessage() != null
                        ? exception.getMessage() : "Không thể tải sản phẩm");
            }
        });
    }

    public void search(String keyword) {
        String query = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<Product> filtered = new ArrayList<>();
        for (Product product : allProducts) {
            String name = product.getProductName() == null ? "" :
                    product.getProductName().toLowerCase(Locale.ROOT);
            if (query.isEmpty() || name.contains(query)) filtered.add(product);
        }
        products.setValue(filtered);
    }
}
