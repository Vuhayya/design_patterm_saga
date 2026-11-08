package com.sagademo.saga.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * ONE LINE of the human-readable execution log (requirement #12).
 *
 * <p>The frontend "LIVE LOG" terminal renders these rows verbatim:
 * <pre>
 *   [10:31:02] Saga started
 *   [10:31:03] Order created successfully
 *   [10:31:04] Payment failed
 *   [10:31:04] Starting compensation
 *   [10:31:05] Saga compensated successfully
 * </pre>
 *
 * <p>They are persisted rather than streamed so that reloading the page - or opening an old
 * saga during questions - still shows the full story.
 */
@Entity
@Table(name = "saga_log_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "saga_id", nullable = false, length = 60)
    private String sagaId;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    /** INFO | SUCCESS | WARN | ERROR - chooses the colour of the log line. */
    @Column(name = "level", nullable = false, length = 10)
    private String level;

    /** EXECUTION | COMPENSATION - lets the UI group the two phases. */
    @Column(name = "phase", nullable = false, length = 20)
    private String phase;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "logged_at", nullable = false)
    private Instant loggedAt;
}
