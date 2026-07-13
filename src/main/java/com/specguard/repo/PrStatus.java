package com.specguard.repo;

/** PR lifecyle, based on github webhook action semantics */
public enum PrStatus {
    OPEN,
    CLOSED,
    MERGED
}