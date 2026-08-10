package com.sskengine.commerceflow.payment;

import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


@Mapper
interface PaymentRepository {
    @Select("""
            SELECT payment_id, order_id, amount, currency, status, idempotency_key, provider_reference,
                   correlation_id, created_at
            FROM payment
            WHERE idempotency_key = #{idempotencyKey}
            """)
    PaymentEntity findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    @Select("""
            SELECT payment_id, order_id, amount, currency, status, idempotency_key, provider_reference,
                   correlation_id, created_at
            FROM payment
            WHERE payment_id = #{paymentId}
            """)
    PaymentEntity findById(@Param("paymentId") UUID paymentId);

    @Insert("""
            INSERT INTO payment (payment_id, order_id, amount, currency, status, idempotency_key,
                                 provider_reference, correlation_id, created_at)
            VALUES (#{paymentId}, #{orderId}, #{amount}, #{currency}, #{status}, #{idempotencyKey},
                    #{providerReference}, #{correlationId}, #{createdAt})
            """)
    void insert(PaymentEntity payment);
}
