package com.sskengine.commerceflow.catalog;

import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


@Mapper
interface ProductRepository {
    @Select("""
            SELECT id, name, amount, currency, published
            FROM product
            WHERE published = TRUE AND LOWER(name) LIKE LOWER(CONCAT('%', #{name}, '%'))
            """)
    List<ProductEntity> findByPublishedTrueAndNameContainingIgnoreCase(@Param("name") String name);

    @Select("SELECT id, name, amount, currency, published FROM product WHERE id = #{id}")
    ProductEntity findById(@Param("id") UUID id);

    @Insert("""
            INSERT INTO product (id, name, amount, currency, published)
            VALUES (#{id}, #{name}, #{amount}, #{currency}, #{published})
            """)
    void insert(ProductEntity entity);
}
