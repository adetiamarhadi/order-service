package com.github.adetiamarhadi.orderservice.saga;

import com.github.adetiamarhadi.orderservice.core.events.OrderCreatedEvent;
import com.github.adetiamarhadi.sagacore.commands.ReserveProductCommand;
import com.github.adetiamarhadi.sagacore.events.ProductReservedEvent;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Saga
public class OrderSaga {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrderSaga.class);

	private transient CommandGateway commandGateway;

	@Autowired
	public void setCommandGateway(CommandGateway commandGateway) {
		this.commandGateway = commandGateway;
	}

	@StartSaga
	@SagaEventHandler(associationProperty = "orderId")
	public void handle(OrderCreatedEvent orderCreatedEvent) {

		ReserveProductCommand reserveProductCommand = ReserveProductCommand.builder()
				.productId(orderCreatedEvent.getProductId())
				.quantity(orderCreatedEvent.getQuantity())
				.orderId(orderCreatedEvent.getOrderId())
				.userId(orderCreatedEvent.getUserId())
				.build();

		LOGGER.info("OrderCreatedEvent handled for orderId: " + reserveProductCommand.getOrderId() + " and productId: " + reserveProductCommand.getProductId());

		commandGateway.send(reserveProductCommand, (commandMessage, commandResultMessage) -> {
			if (commandResultMessage.isExceptional()) {
				LOGGER.info("error: " + commandResultMessage.toString());
			}
		});
	}

	@SagaEventHandler(associationProperty = "orderId")
	public void handle(ProductReservedEvent productReservedEvent) {
		LOGGER.info("ProductReservedEvent is called for productId: " + productReservedEvent.getProductId() + " and orderId: " + productReservedEvent.getOrderId());
	}
}
