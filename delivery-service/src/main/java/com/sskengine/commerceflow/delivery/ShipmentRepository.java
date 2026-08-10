package com.sskengine.commerceflow.delivery;

import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;


@Mapper
interface ShipmentRepository {
    @Select("""
            SELECT shipment_id, order_id, address_ref, tracking_number, status, provider_reference,
                   correlation_id, created_at
            FROM shipment
            WHERE order_id = #{orderId}
            """)
    ShipmentEntity findByOrderId(@Param("orderId") UUID orderId);

    @Select("""
            SELECT shipment_id, order_id, address_ref, tracking_number, status, provider_reference,
                   correlation_id, created_at
            FROM shipment
            WHERE shipment_id = #{id}
            """)
    ShipmentEntity findById(@Param("id") UUID id);

    @Insert("""
            INSERT INTO shipment (shipment_id, order_id, address_ref, tracking_number, status,
                                  provider_reference, correlation_id, created_at)
            VALUES (#{shipmentId}, #{orderId}, #{addressRef}, #{trackingNumber}, #{status},
                    #{providerReference}, #{correlationId}, #{createdAt})
            """)
    void insert(ShipmentEntity shipment);

    @Update("UPDATE shipment SET status = #{status} WHERE shipment_id = #{shipmentId}")
    void updateStatus(ShipmentEntity shipment);
}
