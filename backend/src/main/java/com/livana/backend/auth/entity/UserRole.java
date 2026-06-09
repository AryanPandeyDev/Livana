package com.livana.backend.auth.entity;

/**
 * User roles:
 * - USER:  default authenticated role (can donate, apply as NGO)
 * - NGO:   approved NGO after on-chain NGOApproved event
 * - ADMIN: platform admin
 */
public enum UserRole {
    USER,
    NGO,
    ADMIN
}
