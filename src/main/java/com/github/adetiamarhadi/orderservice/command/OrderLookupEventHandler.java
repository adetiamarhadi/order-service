package com.github.adetiamarhadi.orderservice.command;

import com.github.adetiamarhadi.orderservice.core.data.OrderLookupEntity;
import com.github.adetiamarhadi.orderservice.core.data.OrderLookupRepository;
import com.github.adetiamarhadi.orderservice.core.events.OrderCreatedEvent;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

@Component
@ProcessingGroup("order-group")
public class OrderLookupEventHandler {

	private final OrderLookupRepository orderLookupRepository;

	public OrderLookupEventHandler(OrderLookupRepository orderLookupRepository) {
		this.orderLookupRepository = orderLookupRepository;
	}

	@EventHandler
	public void on(OrderCreatedEvent orderCreatedEvent) {

		OrderLookupEntity orderLookupEntity = new OrderLookupEntity(orderCreatedEvent.getOrderId());

		orderLookupRepository.save(orderLookupEntity);
	}
}
