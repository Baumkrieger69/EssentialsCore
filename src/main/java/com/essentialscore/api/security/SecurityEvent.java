package com.essentialscore.api.security;

/**
 * Security events for audit logging.
 */
public enum SecurityEvent {
    INVALID_SESSION,
    AUTH_SUCCESS,
    AUTH_FAILURE,
    IP_BLOCKED,
    PERMISSION_DENIED,
    UNAUTHORIZED_ACCESS,
    SECURITY_BREACH,
    COMPLIANCE_VIOLATION
}
