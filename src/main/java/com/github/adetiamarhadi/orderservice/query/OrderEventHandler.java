package com.github.adetiamarhadi.orderservice.query;

import com.github.adetiamarhadi.orderservice.core.data.OrderEntity;
import com.github.adetiamarhadi.orderservice.core.data.OrderRepository;
import com.github.adetiamarhadi.orderservice.core.events.OrderApprovedEvent;
import com.github.adetiamarhadi.orderservice.core.events.OrderCreatedEvent;
import com.github.adetiamarhadi.orderservice.core.events.OrderRejectedEvent;
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

	@EventHandler
	public void on(OrderApprovedEvent orderApprovedEvent) {

		OrderEntity orderEntity = orderRepository.findByOrderId(orderApprovedEvent.getOrderId());

		if (null == orderEntity) {
			return;
		}

		orderEntity.setOrderStatus(orderApprovedEvent.getOrderStatus());

		orderRepository.save(orderEntity);
	}

	@EventHandler
	public void on(OrderRejectedEvent orderRejectedEvent) {

		OrderEntity orderEntity = orderRepository.findByOrderId(orderRejectedEvent.getOrderId());
		orderEntity.setOrderStatus(orderRejectedEvent.getOrderStatus());
		orderRepository.save(orderEntity);
	}
}
