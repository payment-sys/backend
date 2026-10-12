package com.v_payment.pay.order.infrastructure.persistence.repository;

import com.v_payment.pay.order.domain.PaymentCreateSource;
import com.v_payment.pay.order.domain.order.Order;
import com.v_payment.pay.order.domain.order.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderCode(String orderCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    select o
    from Order o
    where o.orderCode in :orderCodes
      and o.status = :status
    order by o.orderCode asc
    """)
    List<Order> findAllByOrderCodeCreated(Collection<String> orderCodes, OrderStatus status);

    boolean existsByOrderCodeAndStatus(String orderCode, OrderStatus status);

    @Query("""
            select o.orderCode
            from Order o
            where o.status = :createdStatus
              and o.orderedAt <= :orderedBefore
            order by o.orderedAt asc
            """)
    List<String> findExpirableCreatedOrderCodes(
            @Param("createdStatus") OrderStatus createdStatus,
            @Param("orderedBefore") LocalDateTime orderedBefore,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o
            from Order o
            where o.status = :status
              and o.orderedAt <= :orderedBefore
            order by o.orderedAt asc, o.orderCode asc
            """)
    List<Order> findExpirableOrdersForUpdate(
            @Param("status") OrderStatus status,
            @Param("orderedBefore") LocalDateTime orderedBefore,
            Pageable pageable
    );

    @Modifying
    @Query("""
            update Order o
            set o.status = :failedStatus
            where o.orderCode = :orderCode
              and o.status = :createdStatus
            """)
    int markFailed(
            @Param("orderCode") String orderCode,
            @Param("createdStatus") OrderStatus createdStatus,
            @Param("failedStatus") OrderStatus failedStatus
    );

    @Modifying
    @Query("""
            update Order o
            set o.status = :failedStatus
            where o.orderCode in :orderCodes
              and o.status = :createdStatus
            """)
    int markFailedByOrderCodes(
            @Param("orderCodes") Collection<String> orderCodes,
            @Param("createdStatus") OrderStatus createdStatus,
            @Param("failedStatus") OrderStatus failedStatus
    );

    @Modifying
    @Query("""
            update Order o
            set o.status = :targetStatus
            where o.orderCode = :orderCode
              and o.status = :currentStatus
            """)
    int markStatus(
            @Param("orderCode") String orderCode,
            @Param("currentStatus") OrderStatus currentStatus,
            @Param("targetStatus") OrderStatus targetStatus
    );

    @Modifying
    @Query("""
            update Order o
            set o.status = :targetStatus
            where o.orderCode in :orderCodes
              and o.status = :currentStatus
            """)
    int markStatusByOrderCodes(
            @Param("orderCodes") Collection<String> orderCodes,
            @Param("currentStatus") OrderStatus currentStatus,
            @Param("targetStatus") OrderStatus targetStatus
    );

    @Query("""
            select new com.v_payment.pay.order.domain.PaymentCreateSource(
                o.orderCode,
                o.totalAmount,
                o.paymentMethod
            )
            from Order o
            where o.orderCode in :orderCodes
              and o.status = :status
            """)
    List<PaymentCreateSource> findPaymentCreateSourcesByOrderCodes(
            @Param("orderCodes") Collection<String> orderCodes,
            @Param("status") OrderStatus status
    );
}
