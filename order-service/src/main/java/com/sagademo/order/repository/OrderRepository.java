package com.sagademo.order.repository;

import com.sagademo.order.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Data access for the Order aggregate. This repository can only ever be reached from
 * inside the Order Service JVM - that is what "each service owns its data" means.
 */
public interface OrderRepository extends JpaRepository<OrderEntity, String> {

    List<OrderEntity> findAllByOrderBySequenceNoDesc();

    Optional<OrderEntity> findBySagaId(String sagaId);

    @Query("select coalesce(max(o.sequenceNo), 0) from OrderEntity o")
    long findMaxSequenceNo();
}
