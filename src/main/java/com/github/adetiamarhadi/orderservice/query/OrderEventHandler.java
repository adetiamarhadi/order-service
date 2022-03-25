package com.github.adetiamarhadi.orderservice.query;

import com.github.adetiamarhadi.orderservice.core.data.OrderEntity;
import com.github.adetiamarhadi.orderservice.core.data.OrderRepository;
import com.github.adetiamarhadi.orderservice.core.events.OrderCreatedEvent;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
@ProcessingGroup("order-group")
public class OrderEventHandler {

	private final OrderRepository orderRepository;

	public OrderEventHandler(OrderRepository orderRepository) {
		this.orderRepository = orderRepository;
	}

	@EventHandler
	public void on(OrderCreatedEvent orderCreatedEvent) {

		OrderEntity orderEntity = new OrderEntity();
		BeanUtils.copyProperties(orderCreatedEvent, orderEntity);

		orderRepository.save(orderEntity);
	}
}
