package com.github.adetiamarhadi.orderservice.query;

import com.github.adetiamarhadi.orderservice.core.data.OrderEntity;
import com.github.adetiamarhadi.orderservice.core.data.OrderRepository;
import com.github.adetiamarhadi.orderservice.core.model.OrderSummary;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public class OrderQueriesHandlerr {

	private final OrderRepository orderRepository;

	public OrderQueriesHandlerr(OrderRepository orderRepository) {
		this.orderRepository = orderRepository;
	}

	@QueryHandler
	public OrderSummary findOrder(FindOrderQuery findOrderQuery) {

		OrderEntity orderEntity = orderRepository.findByOrderId(findOrderQuery.getOrderId());

		return new OrderSummary(orderEntity.getOrderId(), orderEntity.getOrderStatus(), "");
	}
}
