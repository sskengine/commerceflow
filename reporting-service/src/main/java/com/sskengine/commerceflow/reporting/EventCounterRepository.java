package com.sskengine.commerceflow.reporting;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;


@Mapper
interface EventCounterRepository {
    @Select("SELECT event_type, event_count FROM event_counter WHERE event_type = #{eventType}")
    EventCounterEntity findById(@Param("eventType") String eventType);

    @Insert("INSERT INTO event_counter (event_type, event_count) VALUES (#{eventType}, #{eventCount})")
    void insert(EventCounterEntity counter);

    @Update("UPDATE event_counter SET event_count = #{eventCount} WHERE event_type = #{eventType}")
    void update(EventCounterEntity counter);

    @Select("SELECT event_type, event_count FROM event_counter")
    List<EventCounterEntity> findAll();
}
