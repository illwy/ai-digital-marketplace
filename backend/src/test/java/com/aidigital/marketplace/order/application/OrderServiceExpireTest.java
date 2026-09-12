package com.aidigital.marketplace.order.application;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aidigital.marketplace.catalog.application.CatalogService;
import com.aidigital.marketplace.inventory.application.InventoryService;
import com.aidigital.marketplace.order.infrastructure.entity.OrderEntity;
import com.aidigital.marketplace.order.infrastructure.mapper.OrderItemMapper;
import com.aidigital.marketplace.order.infrastructure.mapper.OrderMapper;

@ExtendWith(MockitoExtension.class)
class OrderServiceExpireTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private CatalogService catalogService;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void expireDueOrdersReleasesLockedInventory() {
        OrderEntity due = new OrderEntity();
        due.setId(5L);
        due.setPayStatus("PENDING");
        due.setInventoryId(9L);
        due.setExpireAt(LocalDateTime.now().minusMinutes(1));
        when(orderMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(due));

        orderService.expireDueOrders();

        verify(orderMapper).updateById(due);
        verify(inventoryService).release(9L, 5L);
    }

    @Test
    void expireDueOrdersSkipsReleaseWhenNoInventory() {
        OrderEntity due = new OrderEntity();
        due.setId(6L);
        due.setPayStatus("PENDING");
        due.setInventoryId(null);
        due.setExpireAt(LocalDateTime.now().minusMinutes(1));
        when(orderMapper.selectList(any())).thenReturn(List.of(due));

        orderService.expireDueOrders();

        verify(orderMapper).updateById(due);
        verify(inventoryService, never()).release(any(), any());
    }
}
