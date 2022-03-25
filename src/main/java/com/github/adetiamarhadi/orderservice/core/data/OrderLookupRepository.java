package com.github.adetiamarhadi.orderservice.core.data;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderLookupRepository extends JpaRepository<OrderLookupEntity, String> {
	OrderLookupEntity findByOrderId(String orderId);
}
