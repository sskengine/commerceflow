package com.sskengine.commerceflow.order;

import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;


@Mapper
interface OrderRepository {
    @Insert("""
            INSERT INTO customer_order (order_id, customer_id, currency, total_amount, status, correlation_id, created_at)
            VALUES (#{orderId}, #{customerId}, #{currency}, #{totalAmount}, #{status}, #{correlationId}, #{createdAt})
            """)
    void insert(OrderEntity order);

    @Insert("""
            INSERT INTO order_item (id, order_id, product_id, quantity, unit_price)
            VALUES (#{id}, #{orderId}, #{productId}, #{quantity}, #{unitPrice})
            """)
    void insertItem(OrderItemEntity item);

    @Select("""
            SELECT order_id, customer_id, currency, total_amount, status, correlation_id, created_at
            FROM customer_order
            WHERE order_id = #{id}
            """)
    OrderEntity findById(@Param("id") UUID id);

    @Select("""
            SELECT id, order_id, product_id, quantity, unit_price
            FROM order_item
            WHERE order_id = #{orderId}
            """)
    List<OrderItemEntity> findItemsByOrderId(@Param("orderId") UUID orderId);

    @Update("UPDATE customer_order SET status = #{status} WHERE order_id = #{orderId}")
    void updateStatus(OrderEntity order);
}
