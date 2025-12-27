-- MySQL dump 10.13  Distrib 8.0.36, for Linux (x86_64)
--
-- Host: localhost    Database: parking
-- ------------------------------------------------------
-- Server version	8.0.36

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `payment_orders`
--

DROP TABLE IF EXISTS `payment_orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` bigint DEFAULT NULL,
  `application_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `approved_amount` bigint DEFAULT NULL,
  `authorized_at` datetime(6) DEFAULT NULL,
  `avs_status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `captured_at` datetime(6) DEFAULT NULL,
  `card_bin` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `card_brand` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `card_exp_month` int DEFAULT NULL,
  `card_exp_year` int DEFAULT NULL,
  `card_fingerprint` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `card_status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `card_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `checkout_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `currency` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `cvv_status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `delay_action` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `delay_duration` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `delayed_until` datetime(6) DEFAULT NULL,
  `device_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `entry_method` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `last4` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `local_created_at` datetime(6) DEFAULT NULL,
  `local_updated_at` datetime(6) DEFAULT NULL,
  `location_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `note` text,
  `order_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `payment_source` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `prepaid_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `receipt_number` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `receipt_url` text,
  `reference_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `risk_evaluation_created_at` datetime(6) DEFAULT NULL,
  `risk_level` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `source_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `square_payment_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `square_product` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `statement_description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `total_amount` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `version_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `vehicle_record_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `UK4xg1eywgkvdhvn0xtol5prt4r` (`square_payment_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=48 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment_orders`
--

LOCK TABLES `payment_orders` WRITE;
/*!40000 ALTER TABLE `payment_orders` DISABLE KEYS */;
INSERT INTO `payment_orders` VALUES (46,9,'sq0idp-w46nJ_NCNDMSOywaCY0mwA',9,NULL,'AVS_REJECTED',NULL,'559994','MASTERCARD',1,2029,'sq-1-hhgM7nbHe8R6MRS2MLqWPJJOz7GA0yBoQHNeDiVx6cisXq5OSpgXCPiyHtA9hbw2ig','CAPTURED','DEBIT',NULL,'2025-12-22 14:19:17.871000',NULL,'CAD','CVV_ACCEPTED','CANCEL','PT168H','2025-12-29 14:19:17.871000',NULL,'KEYED','3935','2025-12-22 14:15:21.909264','2025-12-22 14:23:33.854994','LYZ4X83G13CQK','在线支付 - 车牌: AB6234','R3LIMs2u0HSSyjPgYCUjumG5EVeZY','ONLINE','PREPAID','v3EA','https://squareup.com/receipt/preview/v3EAlnA0Z7162gfr69rWQ1swBOTZY',NULL,'2025-12-22 14:21:21.211000','NORMAL','CARD','v3EAlnA0Z7162gfr69rWQ1swBOTZY',NULL,'SQ *TAOTIE TRADING CORP.','COMPLETED',9,'2025-12-22 14:19:21.211000',NULL,95),(47,9,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'5wCoLyoVLidqO',NULL,NULL,'USD',NULL,NULL,NULL,NULL,'533CS145C3000603',NULL,NULL,'2025-12-22 14:15:21.919200',NULL,NULL,'停车费支付 - 车牌: AB6234',NULL,'TERMINAL',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'PENDING',NULL,NULL,NULL,95);
/*!40000 ALTER TABLE `payment_orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vehicle_records`
--

DROP TABLE IF EXISTS `vehicle_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vehicle_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `duration_seconds` int DEFAULT NULL,
  `entry_camera_id` int DEFAULT NULL,
  `entry_camera_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `entry_camera_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `entry_detection_count` int DEFAULT NULL,
  `entry_event_id` int DEFAULT NULL,
  `entry_plate_number` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `entry_snapshot` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `entry_time` datetime(6) DEFAULT NULL,
  `entry_weight` decimal(10,2) DEFAULT NULL,
  `exit_camera_id` int DEFAULT NULL,
  `exit_camera_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `exit_camera_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `exit_detection_count` int DEFAULT NULL,
  `exit_event_id` int DEFAULT NULL,
  `exit_plate_number` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `exit_snapshot` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `exit_time` datetime(6) DEFAULT NULL,
  `exit_weight` decimal(10,2) DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `parking_lot_code` varchar(50) DEFAULT NULL,
  `parking_duration_minutes` int DEFAULT NULL,
  `parking_fee_cents` int DEFAULT NULL,
  `payment_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `payment_time` datetime(6) DEFAULT NULL,
  `square_payment_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `exit_barrier_gate_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `exit_led_screen_config` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `exit_payment_device_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `online_payment_link_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `online_payment_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `entry_backup_channel_id` varchar(50) DEFAULT NULL,
  `entry_barrier_gate_id` varchar(50) DEFAULT NULL,
  `entry_led_screen_config` varchar(255) DEFAULT NULL,
  `entry_payment_device_id` varchar(50) DEFAULT NULL,
  `backup_channel_id` varchar(50) DEFAULT NULL,
  `barrier_gate_id` varchar(50) DEFAULT NULL,
  `led_screen_config` varchar(255) DEFAULT NULL,
  `payment_device_id` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=96 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vehicle_records`
--

LOCK TABLES `vehicle_records` WRITE;
/*!40000 ALTER TABLE `vehicle_records` DISABLE KEYS */;
INSERT INTO `vehicle_records` VALUES (95,'2025-12-22 22:05:33.804503',586,8,'192.168.99.102','tom的平台',3,8,'AB6234','cam8_motion_8_20251222_220533_100.jpg','2025-12-22 22:05:33.804241',10.93,8,'192.168.99.102','tom的平台',3,10,'AB6234','cam8_motion_10_20251222_221519_173.jpg','2025-12-22 22:15:19.862513',9.12,'exited','2025-12-22 22:19:22.452624','LYZ4X83G13CQK',10,9,'paid','2025-12-22 22:19:22.449539','v3EAlnA0Z7162gfr69rWQ1swBOTZY',NULL,NULL,NULL,'4ARFLQMGWPXFBC6I','https://square.link/u/lfCfKx2n',NULL,NULL,NULL,NULL,'1','01','96:6E:6D:27:DC:9D','533CS145C3000603');
/*!40000 ALTER TABLE `vehicle_records` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-12-28  7:36:06
