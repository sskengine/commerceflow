package com.sskengine.commerceflow.notification;

import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


@Mapper
interface NotificationRepository {
    @Select("SELECT COUNT(*) FROM notification_message WHERE event_id = #{eventId}")
    int countById(@Param("eventId") UUID eventId);

    @Insert("""
            INSERT INTO notification_message (event_id, event_type, correlation_id, status, processed_at)
            VALUES (#{eventId}, #{eventType}, #{correlationId}, #{status}, #{processedAt})
            """)
    void insert(NotificationEntity entity);

    @Select("""
            SELECT event_id, event_type, correlation_id, status, processed_at
            FROM notification_message
            """)
    List<NotificationEntity> findAll();
}
