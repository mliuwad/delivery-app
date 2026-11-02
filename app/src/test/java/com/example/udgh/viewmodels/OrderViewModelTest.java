package com.example.udgh.viewmodels;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.example.udgh.models.Order;
import com.example.udgh.repositories.OrderRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class OrderViewModelTest {
    @Rule public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();
    private OrderRepository repository;
    private OrderViewModel vm;

    @Before public void setup() {
        repository = mock(OrderRepository.class);
        vm = new OrderViewModel(repository);
    }

    private Order order(String id, String status, String sender, String receiver) {
        Order o = new Order();
        o.setOrderId(id);
        o.setStatus(status);
        o.setSenderName(sender);
        o.setReceiverName(receiver);
        return o;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private OrderRepository.ResultCallback<List<Order>> ordersCallback(int index) {
        ArgumentCaptor<OrderRepository.ResultCallback> captor =
                ArgumentCaptor.forClass(OrderRepository.ResultCallback.class);
        verify(repository, times(index + 1)).getOrders(anyString(), captor.capture());
        return captor.getAllValues().get(index);
    }

    @Test public void initialListEmpty() {
        assertTrue(vm.getOrders().getValue().isEmpty());
    }

    @Test public void initialCountZero() {
        assertEquals(Integer.valueOf(0), vm.getTotalOrdersCount().getValue());
    }

    @Test public void loadingStartsOnFetch() {
        vm.loadOrders("ALL");
        assertEquals(Boolean.TRUE, vm.getIsLoading().getValue());
    }

    @Test public void successfulFetchUpdatesCount() {
        vm.loadOrders("ALL");
        ordersCallback(0).onSuccess(Arrays.asList(order("1","PENDING","A","B"),order("2","COMPLETED","C","D")));
        assertEquals(Integer.valueOf(2), vm.getTotalOrdersCount().getValue());
    }

    @Test public void pendingFilterIncludesWaiting() {
        vm.loadOrders("ALL");
        ordersCallback(0).onSuccess(Arrays.asList(order("1","PENDING","A","B"),order("2","WAITING","C","D"),order("3","COMPLETED","E","F")));
        vm.setStatusFilter("PENDING");
        assertEquals(2, vm.getOrders().getValue().size());
    }

    @Test public void exactStatusFilter() {
        vm.loadOrders("ALL");
        ordersCallback(0).onSuccess(Arrays.asList(order("1","PENDING","A","B"),order("2","COMPLETED","C","D")));
        vm.setStatusFilter("COMPLETED");
        assertEquals("2", vm.getOrders().getValue().get(0).getOrderId());
    }

    @Test public void searchIgnoresCaseAndTrims() {
        vm.loadOrders("ALL");
        ordersCallback(0).onSuccess(Arrays.asList(order("1","PENDING","Alice","Bob"),order("2","PENDING","Carol","Dave")));
        vm.searchOrders("  aLiCe ");
        assertEquals(1, vm.getOrders().getValue().size());
    }

    @Test public void searchByReceiverName() {
        vm.loadOrders("ALL");
        ordersCallback(0).onSuccess(Collections.singletonList(order("1","PENDING","Alice","Bob")));
        vm.searchOrders("bob");
        assertEquals(1, vm.getOrders().getValue().size());
    }

    @Test public void staleSuccessIsIgnored() {
        vm.loadOrders("ALL");
        vm.loadOrders("ALL");
        ordersCallback(1).onSuccess(Collections.singletonList(order("new","PENDING","A","B")));
        ordersCallback(0).onSuccess(Collections.singletonList(order("old","PENDING","C","D")));
        assertEquals("new", vm.getOrders().getValue().get(0).getOrderId());
    }

    @Test public void staleFailureIsIgnored() {
        vm.loadOrders("ALL");
        vm.loadOrders("ALL");
        ordersCallback(1).onSuccess(Collections.emptyList());
        ordersCallback(0).onError(new RuntimeException("stale"));
        assertNull(vm.getErrorMessage().getValue());
        assertEquals(Boolean.FALSE, vm.getIsLoading().getValue());
    }
}
