package com.quaer_api.repository;

import com.quaer_api.entity.PaymentOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long>, JpaSpecificationExecutor<PaymentOrder> {

    // 基本查询方法
    Optional<PaymentOrder> findBySquarePaymentId(String squarePaymentId);
    Optional<PaymentOrder> findByCheckoutId(String checkoutId);
    Optional<PaymentOrder> findByReferenceId(String referenceId);
    Optional<PaymentOrder> findByReceiptNumber(String receiptNumber);
    Optional<PaymentOrder> findByOrderId(String orderId);
    Optional<PaymentOrder> findByVehicleRecordId(Long vehicleRecordId);

    // 根据车辆记录ID查询，优先返回已完成的支付记录
    @Query("SELECT p FROM PaymentOrder p WHERE p.vehicleRecordId = :vehicleRecordId " +
           "ORDER BY CASE WHEN p.status = 'COMPLETED' THEN 0 ELSE 1 END, p.id DESC")
    List<PaymentOrder> findAllByVehicleRecordIdOrderByStatusAndId(@Param("vehicleRecordId") Long vehicleRecordId);

    // 分页查询方法
    Page<PaymentOrder> findByStatus(String status, Pageable pageable);
    Page<PaymentOrder> findByPaymentSource(String paymentSource, Pageable pageable);
    Page<PaymentOrder> findByStatusAndPaymentSource(String status, String paymentSource, Pageable pageable);

    // ==================== 统计查询方法 ====================

    // 查询所有停车场的统计
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentOrder p WHERE p.status = :status")
    Long sumAmountByStatus(@Param("status") String status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentOrder p WHERE p.status = :status AND p.localCreatedAt >= :startTime")
    Long sumAmountByStatusAfterDate(@Param("status") String status, @Param("startTime") OffsetDateTime startTime);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentOrder p WHERE p.status = :status AND p.localCreatedAt >= :startTime AND p.localCreatedAt < :endTime")
    Long sumAmountByStatusAndDateRange(@Param("status") String status, @Param("startTime") OffsetDateTime startTime, @Param("endTime") OffsetDateTime endTime);

    @Query("SELECT COUNT(p) FROM PaymentOrder p WHERE p.status = :status")
    Long countByStatus(@Param("status") String status);

    @Query("SELECT COUNT(p) FROM PaymentOrder p WHERE p.status = :status AND p.localCreatedAt >= :startTime")
    Long countByStatusAfterDate(@Param("status") String status, @Param("startTime") OffsetDateTime startTime);

    @Query("SELECT COUNT(p) FROM PaymentOrder p WHERE p.status = :status AND p.localCreatedAt >= :startTime AND p.localCreatedAt < :endTime")
    Long countByStatusAndDateRange(@Param("status") String status, @Param("startTime") OffsetDateTime startTime, @Param("endTime") OffsetDateTime endTime);

    // 查询指定停车场的统计
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentOrder p WHERE p.locationId IN :locationIds AND p.status = :status")
    Long sumAmountByLocationIdsAndStatus(@Param("locationIds") List<String> locationIds, @Param("status") String status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentOrder p WHERE p.locationId IN :locationIds AND p.status = :status AND p.localCreatedAt >= :startTime")
    Long sumAmountByLocationIdsAndStatusAfterDate(@Param("locationIds") List<String> locationIds, @Param("status") String status, @Param("startTime") OffsetDateTime startTime);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentOrder p WHERE p.locationId IN :locationIds AND p.status = :status AND p.localCreatedAt >= :startTime AND p.localCreatedAt < :endTime")
    Long sumAmountByLocationIdsAndStatusAndDateRange(@Param("locationIds") List<String> locationIds, @Param("status") String status, @Param("startTime") OffsetDateTime startTime, @Param("endTime") OffsetDateTime endTime);

    @Query("SELECT COUNT(p) FROM PaymentOrder p WHERE p.locationId IN :locationIds AND p.status = :status")
    Long countByLocationIdsAndStatus(@Param("locationIds") List<String> locationIds, @Param("status") String status);

    @Query("SELECT COUNT(p) FROM PaymentOrder p WHERE p.locationId IN :locationIds AND p.status = :status AND p.localCreatedAt >= :startTime")
    Long countByLocationIdsAndStatusAfterDate(@Param("locationIds") List<String> locationIds, @Param("status") String status, @Param("startTime") OffsetDateTime startTime);

    @Query("SELECT COUNT(p) FROM PaymentOrder p WHERE p.locationId IN :locationIds AND p.status = :status AND p.localCreatedAt >= :startTime AND p.localCreatedAt < :endTime")
    Long countByLocationIdsAndStatusAndDateRange(@Param("locationIds") List<String> locationIds, @Param("status") String status, @Param("startTime") OffsetDateTime startTime, @Param("endTime") OffsetDateTime endTime);
}
