package com.sagademo.saga.entity;

/** Distinguishes the "do" phase from the "undo" phase, so the UI can draw them differently. */
public enum StepType {

    /** A normal local transaction that moves the business transaction forward. */
    FORWARD,

    /** A compensating transaction that semantically undoes an earlier FORWARD step. */
    COMPENSATION
}
