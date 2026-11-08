package com.sagademo.payment.repository;

import com.sagademo.payment.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/** Data access for payments. Private to the Payment Service. */
public interface PaymentRepository extends JpaRepository<PaymentEntity, String> {

    List<PaymentEntity> findAllByOrderBySequenceNoDesc();

    /** Lets the compensation find what to refund from the correlation id alone. */
    Optional<PaymentEntity> findFirstBySagaIdOrderBySequenceNoDesc(String sagaId);

    @Query("select coalesce(max(p.sequenceNo), 0) from PaymentEntity p")
    long findMaxSequenceNo();
}
