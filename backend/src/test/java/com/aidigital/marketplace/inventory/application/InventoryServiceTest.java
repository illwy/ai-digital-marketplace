package com.aidigital.marketplace.inventory.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.aidigital.marketplace.inventory.api.dto.InventoryImportRequest;
import com.aidigital.marketplace.inventory.infrastructure.entity.InventoryEntity;
import com.aidigital.marketplace.inventory.infrastructure.mapper.InventoryMapper;
import com.aidigital.marketplace.shared.web.ApiException;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryMapper inventoryMapper;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void invalidateOnlyAvailable() {
        InventoryEntity locked = new InventoryEntity();
        locked.setId(1L);
        locked.setStatus("LOCKED");
        when(inventoryMapper.selectById(1L)).thenReturn(locked);

        assertThatThrownBy(() -> inventoryService.invalidate(1L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void importRejectsBlankLinesOnly() {
        InventoryImportRequest request = new InventoryImportRequest(9L, List.of(" ", ""), "x");
        assertThatThrownBy(() -> inventoryService.importItems(request))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("INVENTORY_EMPTY_IMPORT");
    }

    @Test
    void lockOneFailsWhenEmpty() {
        when(inventoryMapper.selectOneAvailableForUpdate(3L)).thenReturn(null);
        assertThatThrownBy(() -> inventoryService.lockOne(3L, 10L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("INVENTORY_EMPTY");
    }

    @Test
    void lockOneUpdatesAvailableRow() {
        InventoryEntity available = new InventoryEntity();
        available.setId(8L);
        available.setProductId(3L);
        available.setStatus("AVAILABLE");
        when(inventoryMapper.selectOneAvailableForUpdate(3L)).thenReturn(available);

        InventoryEntity locked = inventoryService.lockOne(3L, 44L);

        assertThat(locked.getStatus()).isEqualTo("LOCKED");
        assertThat(locked.getOrderId()).isEqualTo(44L);
        verify(inventoryMapper).updateById(any(InventoryEntity.class));
    }
}
