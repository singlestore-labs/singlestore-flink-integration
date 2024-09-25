CREATE TABLE `stockTrans` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `stock` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `qty` int(11) DEFAULT NULL,
  `avgValue` double DEFAULT NULL,
  PRIMARY KEY (`id`)
) 