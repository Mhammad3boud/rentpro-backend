package com.rentpro.backend.activity;

public enum ActivityType {
    // Maintenance
    MAINTENANCE_CREATED,
    MAINTENANCE_UPDATED,
    MAINTENANCE_STATUS_CHANGED,
    MAINTENANCE_DELETED,
    
    // Payments
    PAYMENT_RECEIVED,
    PAYMENT_UPDATED,
    PAYMENT_OVERDUE,
    
    // Leases
    LEASE_CREATED,
    LEASE_UPDATED,
    LEASE_TERMINATED,
    
    // Properties
    PROPERTY_CREATED,
    PROPERTY_UPDATED,
    PROPERTY_DELETED,
    
    // Tenants
    TENANT_CREATED,
    TENANT_UPDATED,
    TENANT_ASSIGNED,
    
    // General
    SYSTEM,
    OTHER
}
