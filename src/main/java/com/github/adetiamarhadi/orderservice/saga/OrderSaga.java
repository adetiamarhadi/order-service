package com.github.adetiamarhadi.orderservice.saga;

import com.github.adetiamarhadi.orderservice.command.ApproveOrderCommand;
import com.github.adetiamarhadi.orderservice.core.events.OrderApprovedEvent;
import com.github.adetiamarhadi.orderservice.core.events.OrderCreatedEvent;
import com.github.adetiamarhadi.sagacore.commands.ProcessPaymentCommand;
import com.github.adetiamarhadi.sagacore.commands.ReserveProductCommand;
import com.github.adetiamarhadi.sagacore.events.PaymentProcessedEvent;
import com.github.adetiamarhadi.sagacore.events.ProductReservedEvent;
import com.github.adetiamarhadi.sagacore.model.User;
import com.github.adetiamarhadi.sagacore.query.FetchUserPaymentDetailsQuery;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.spring.stereotype.Saga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Saga
public class OrderSaga {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrderSaga.class);

	private transient CommandGateway commandGateway;
	private transient QueryGateway queryGateway;

	@Autowired
	public void setCommandGateway(CommandGateway commandGateway) {
		this.commandGateway = commandGateway;
	}

	@Autowired
	public void setQueryGateway(QueryGateway queryGateway) {
		this.queryGateway = queryGateway;
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

		FetchUserPaymentDetailsQuery query = FetchUserPaymentDetailsQuery.builder()
				.userId(productReservedEvent.getUserId())
				.build();

		User user = null;

		try {
			user = queryGateway.query(query, ResponseTypes.instanceOf(User.class)).join();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return;
		}

		if (null == user) {
			LOGGER.warn("user id not found.");
			return;
		}

		LOGGER.info("successfully fetched user payment details for user " + user.getFirstName());

		ProcessPaymentCommand processPaymentCommand = ProcessPaymentCommand.builder()
				.orderId(productReservedEvent.getOrderId())
				.paymentDetails(user.getPaymentDetails())
				.paymentId(UUID.randomUUID().toString())
				.build();

		String result = null;

		try {
			result = commandGateway.sendAndWait(processPaymentCommand, 10, TimeUnit.SECONDS);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		if (null == result) {
			LOGGER.warn("the result of process payment command is null.");
		}
	}

	@SagaEventHandler(associationProperty = "orderId")
	public void handle(PaymentProcessedEvent paymentProcessedEvent) {

		ApproveOrderCommand approveOrderCommand = new ApproveOrderCommand(paymentProcessedEvent.getOrderId());

		commandGateway.send(approveOrderCommand);
	}

	@EndSaga
	@SagaEventHandler(associationProperty = "orderId")
	public void handle(OrderApprovedEvent orderApprovedEvent) {

		LOGGER.info("order is approved. order saga is complete for order id: " + orderApprovedEvent.getOrderId());
	}
}
