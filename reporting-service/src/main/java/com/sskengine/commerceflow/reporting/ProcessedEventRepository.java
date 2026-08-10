package com.sskengine.commerceflow.reporting;

import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


@Mapper
interface ProcessedEventRepository {
    @Select("SELECT COUNT(*) FROM processed_event WHERE event_id = #{eventId}")
    int countById(@Param("eventId") UUID eventId);

    @Insert("""
            INSERT INTO processed_event (event_id, event_type, processed_at)
            VALUES (#{eventId}, #{eventType}, #{processedAt})
            """)
    void insert(ProcessedEventEntity event);
}
