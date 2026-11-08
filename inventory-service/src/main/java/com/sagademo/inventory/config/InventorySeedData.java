package com.sagademo.inventory.config;

import com.sagademo.inventory.entity.InventoryItem;
import com.sagademo.inventory.repository.InventoryItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Seeds the menu with 100 units of every dish, but only when the Inventory database is empty.
 *
 * <p>100 is chosen because the presentation says "Inventory: 100 units" in the
 * STATE COMPARISON panel - after a successful saga it becomes 99 available, and after a
 * compensated saga it must be exactly 100 again.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class InventorySeedData {

    private final InventoryItemRepository itemRepository;

    @Bean
    ApplicationRunner seedInventory() {
        return args -> {
            if (itemRepository.count() > 0) {
                log.info("Inventory database already populated ({} products) - skipping seed",
                        itemRepository.count());
                return;
            }
            List<InventoryItem> menu = List.of(
                    item("PIZZA-01", "Pepperoni Pizza", "120000"),
                    item("BURGER-01", "Double Cheese Burger", "85000"),
                    item("PHO-01", "Beef Pho", "65000"),
                    item("SUSHI-01", "Salmon Sushi Set", "180000"));
            itemRepository.saveAll(menu);
            log.info("Seeded {} products with 100 units each", menu.size());
        };
    }

    private InventoryItem item(String productId, String name, String price) {
        return InventoryItem.builder()
                .productId(productId)
                .productName(name)
                .totalUnits(100)
                .reservedUnits(0)
                .unitPrice(new BigDecimal(price))
                .updatedAt(Instant.now())
                .build();
    }
}
