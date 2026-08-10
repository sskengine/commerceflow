package com.sskengine.commerceflow.payment;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


@Mapper
interface RefundRepository {
    @Select("""
            SELECT refund_id, payment_id, amount, currency, status, idempotency_key, correlation_id, created_at
            FROM refund
            WHERE idempotency_key = #{idempotencyKey}
            """)
    RefundEntity findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    @Insert("""
            INSERT INTO refund (refund_id, payment_id, amount, currency, status, idempotency_key,
                                correlation_id, created_at)
            VALUES (#{refundId}, #{paymentId}, #{amount}, #{currency}, #{status}, #{idempotencyKey},
                    #{correlationId}, #{createdAt})
            """)
    void insert(RefundEntity refund);
}
