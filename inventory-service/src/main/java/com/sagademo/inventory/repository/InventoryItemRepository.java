package com.sagademo.inventory.repository;

import com.sagademo.inventory.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data access for stock levels. Private to the Inventory Service. */
public interface InventoryItemRepository extends JpaRepository<InventoryItem, String> {

    List<InventoryItem> findAllByOrderByProductIdAsc();
}
