package com.github.adetiamarhadi.orderservice.core.events;

import com.github.adetiamarhadi.orderservice.command.OrderStatus;
import lombok.Data;

@Data
public class OrderCreatedEvent {

	private String orderId;
	private String productId;
	private String userId;
	private int quantity;
	private String addressId;
	private OrderStatus orderStatus;
}
