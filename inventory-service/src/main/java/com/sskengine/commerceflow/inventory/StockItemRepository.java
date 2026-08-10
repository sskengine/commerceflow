package com.sskengine.commerceflow.inventory;

import java.util.UUID;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;


@Mapper
interface StockItemRepository {
    @Select("SELECT product_id, available_qty FROM stock_item WHERE product_id = #{productId} FOR UPDATE")
    StockItemEntity findForUpdate(@Param("productId") UUID productId);

    @Insert("INSERT INTO stock_item (product_id, available_qty) VALUES (#{productId}, #{availableQty})")
    void insert(StockItemEntity item);

    @Update("UPDATE stock_item SET available_qty = #{availableQty} WHERE product_id = #{productId}")
    void update(StockItemEntity item);

    @Delete("DELETE FROM stock_item")
    void deleteAll();
}
