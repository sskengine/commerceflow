package com.sskengine.commerceflow.inventory;

import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;


@Mapper
interface ReservationRepository {
    @Insert("""
            INSERT INTO reservation (reservation_id, order_id, product_id, quantity, expires_at, status)
            VALUES (#{reservationId}, #{orderId}, #{productId}, #{quantity}, #{expiresAt}, #{status})
            """)
    void insert(ReservationEntity reservation);

    @Select("""
            SELECT reservation_id, order_id, product_id, quantity, expires_at, status
            FROM reservation
            WHERE order_id = #{orderId} AND status = #{status}
            """)
    List<ReservationEntity> findByOrderIdAndStatus(@Param("orderId") UUID orderId, @Param("status") String status);

    @Update("UPDATE reservation SET status = #{status} WHERE reservation_id = #{reservationId}")
    void updateStatus(ReservationEntity reservation);

    @Delete("DELETE FROM reservation")
    void deleteAll();
}
