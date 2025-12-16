package com.quaer_api.repository;

import com.quaer_api.entity.VehicleRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 车辆记录Repository
 */
@Repository
public interface VehicleRecordRepository extends JpaRepository<VehicleRecord, Long>, JpaSpecificationExecutor<VehicleRecord> {

    /**
     * 查询指定停车场+车牌的未出场记录（标准化车牌号，去掉连字符）
     *
     * @param parkingLotCode 停车场编号
     * @param normalizedPlate 标准化的车牌号（已去除连字符）
     * @return 未出场的记录
     */
    @Query("SELECT v FROM VehicleRecord v " +
           "WHERE v.parkingLotCode = :parkingLotCode " +
           "AND REPLACE(v.entryPlateNumber, '-', '') = :normalizedPlate " +
           "AND v.status = 'entered' " +
           "ORDER BY v.entryTime DESC")
    Optional<VehicleRecord> findUnexitedRecordByParkingLotAndPlate(
            @Param("parkingLotCode") String parkingLotCode,
            @Param("normalizedPlate") String normalizedPlate);

    /**
     * 查询指定停车场+车牌的异常出口记录（标准化车牌号，去掉连字符）
     *
     * @param parkingLotCode 停车场编号
     * @param normalizedPlate 标准化的车牌号（已去除连字符）
     * @return 异常出口记录
     */
    @Query("SELECT v FROM VehicleRecord v " +
           "WHERE v.parkingLotCode = :parkingLotCode " +
           "AND REPLACE(v.exitPlateNumber, '-', '') = :normalizedPlate " +
           "AND v.status = 'exit_only' " +
           "ORDER BY v.exitTime DESC")
    Optional<VehicleRecord> findExitOnlyRecordByParkingLotAndPlate(
            @Param("parkingLotCode") String parkingLotCode,
            @Param("normalizedPlate") String normalizedPlate);

    /**
     * 查询指定停车场+车牌的最新已出场记录（用于LED显示费用）
     *
     * @param parkingLotCode 停车场编号
     * @param normalizedPlate 标准化的车牌号（已去除连字符）
     * @return 最新的已出场记录
     */
    @Query("SELECT v FROM VehicleRecord v " +
           "WHERE v.parkingLotCode = :parkingLotCode " +
           "AND (REPLACE(v.exitPlateNumber, '-', '') = :normalizedPlate " +
           "     OR REPLACE(v.entryPlateNumber, '-', '') = :normalizedPlate) " +
           "AND v.status = 'exited' " +
           "AND v.exitTime IS NOT NULL " +
           "ORDER BY v.exitTime DESC")
    Optional<VehicleRecord> findLatestExitedRecordByParkingLotAndPlate(
            @Param("parkingLotCode") String parkingLotCode,
            @Param("normalizedPlate") String normalizedPlate);
}
