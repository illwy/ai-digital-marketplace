package com.aidigital.marketplace.order.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.aidigital.marketplace.order.infrastructure.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {

    @Select(
            """
            SELECT o.* FROM orders o
            JOIN order_item i ON i.order_id = o.id
            WHERE o.user_id = #{userId}
              AND o.pay_status = 'PENDING'
              AND i.product_id = #{productId}
            LIMIT 1
            """)
    OrderEntity findPendingByUserAndProduct(@Param("userId") Long userId, @Param("productId") Long productId);
}
