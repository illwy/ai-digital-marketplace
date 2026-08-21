package com.aidigital.marketplace.wallet.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.aidigital.marketplace.wallet.infrastructure.entity.WalletEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface WalletMapper extends BaseMapper<WalletEntity> {

    @Update(
            """
            UPDATE wallet
            SET balance_fen = balance_fen - #{amountFen}, updated_at = NOW(3)
            WHERE user_id = #{userId} AND balance_fen >= #{amountFen}
            """)
    int debit(@Param("userId") Long userId, @Param("amountFen") int amountFen);

    @Update(
            """
            UPDATE wallet
            SET balance_fen = balance_fen + #{amountFen}, updated_at = NOW(3)
            WHERE user_id = #{userId}
            """)
    int credit(@Param("userId") Long userId, @Param("amountFen") int amountFen);
}
