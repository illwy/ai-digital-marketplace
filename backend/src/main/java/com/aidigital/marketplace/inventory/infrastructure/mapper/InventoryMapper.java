package com.aidigital.marketplace.inventory.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.aidigital.marketplace.inventory.infrastructure.entity.InventoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface InventoryMapper extends BaseMapper<InventoryEntity> {

    @Select(
            """
            SELECT * FROM virtual_inventory
            WHERE product_id = #{productId} AND status = 'AVAILABLE'
            ORDER BY id ASC
            LIMIT 1
            FOR UPDATE
            """)
    InventoryEntity selectOneAvailableForUpdate(@Param("productId") Long productId);

    @Update(
            """
            UPDATE virtual_inventory
            SET status = 'AVAILABLE', order_id = NULL, locked_at = NULL, updated_at = NOW(3)
            WHERE id = #{id} AND status = 'LOCKED' AND order_id = #{orderId}
            """)
    int releaseLock(@Param("id") Long id, @Param("orderId") Long orderId);

    @Update(
            """
            UPDATE virtual_inventory
            SET status = 'SOLD', sold_at = NOW(3), updated_at = NOW(3)
            WHERE id = #{id} AND status = 'LOCKED' AND order_id = #{orderId}
            """)
    int markSold(@Param("id") Long id, @Param("orderId") Long orderId);
}
