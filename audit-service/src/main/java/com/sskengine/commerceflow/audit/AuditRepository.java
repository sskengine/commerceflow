package com.sskengine.commerceflow.audit;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;


@Mapper
interface AuditRepository {
    @Insert("""
            INSERT INTO audit_entry (event_id, actor_id, action, resource_type, resource_id, correlation_id, occurred_at)
            VALUES (#{eventId}, #{actorId}, #{action}, #{resourceType}, #{resourceId}, #{correlationId}, #{occurredAt})
            """)
    void insert(AuditEntryEntity entity);

    @Select("""
            SELECT event_id, actor_id, action, resource_type, resource_id, correlation_id, occurred_at
            FROM audit_entry
            """)
    List<AuditEntryEntity> findAll();
}
