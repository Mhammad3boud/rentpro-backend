package com.rentpro.backend.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.rentpro.backend.payment.RentPayment.PaymentStatus;

public interface RentPaymentRepository extends JpaRepository<RentPayment, UUID> {

    List<RentPayment> findByLease_LeaseId(UUID leaseId);

    Optional<RentPayment> findByLease_LeaseIdAndMonthYear(UUID leaseId, String monthYear);

    List<RentPayment> findByLease_Tenant_User_UserId(UUID tenantUserId);

    List<RentPayment> findByLease_Property_Owner_UserId(UUID ownerId);


    // ✅ OWNER: total expected
    @Query("""
        select coalesce(sum(r.amountExpected), 0)
        from RentPayment r
        where r.lease.property.owner.userId = :ownerId
    """)
    BigDecimal sumExpectedByOwner(@Param("ownerId") UUID ownerId);

    // ✅ OWNER: total collected
    @Query("""
        select coalesce(sum(r.amountPaid), 0)
        from RentPayment r
        where r.lease.property.owner.userId = :ownerId
    """)
    BigDecimal sumPaidByOwner(@Param("ownerId") UUID ownerId);

    // ✅ OWNER: count by status
    @Query("""
        select count(r)
        from RentPayment r
        where r.lease.property.owner.userId = :ownerId
          and r.paymentStatus = :status
    """)
    long countByOwnerAndStatus(@Param("ownerId") UUID ownerId,
                               @Param("status") PaymentStatus status);

    // ✅ TENANT: total expected
    @Query("""
        select coalesce(sum(r.amountExpected), 0)
        from RentPayment r
        where r.lease.tenant.user.userId = :tenantUserId
    """)
    BigDecimal sumExpectedByTenant(@Param("tenantUserId") UUID tenantUserId);

    // ✅ TENANT: total paid
    @Query("""
        select coalesce(sum(r.amountPaid), 0)
        from RentPayment r
        where r.lease.tenant.user.userId = :tenantUserId
    """)
    BigDecimal sumPaidByTenant(@Param("tenantUserId") UUID tenantUserId);

    // ✅ TENANT: overdue count
    @Query("""
        select count(r)
        from RentPayment r
        where r.lease.tenant.user.userId = :tenantUserId
          and r.paymentStatus = 'OVERDUE'
    """)
    long countOverdueByTenant(@Param("tenantUserId") UUID tenantUserId);

    // ✅ TENANT: next due date (min due date >= today and not paid)
    @Query("""
        select min(r.dueDate)
        from RentPayment r
        where r.lease.tenant.user.userId = :tenantUserId
          and r.dueDate is not null
          and r.dueDate >= :today
          and r.paymentStatus <> 'PAID'
    """)
    LocalDate nextDueDate(@Param("tenantUserId") UUID tenantUserId,
                          @Param("today") LocalDate today);

    // ✅ OWNER: current month revenue
    @Query("""
        select coalesce(sum(r.amountPaid), 0)
        from RentPayment r
        where r.lease.property.owner.userId = :ownerId
          and r.paidDate is not null
          and year(r.paidDate) = year(current_date)
          and month(r.paidDate) = month(current_date)
    """)
    BigDecimal sumCurrentMonthRevenueByOwner(@Param("ownerId") UUID ownerId);
}
