package com.github.adetiamarhadi.orderservice.saga;

import com.github.adetiamarhadi.orderservice.command.ApproveOrderCommand;
import com.github.adetiamarhadi.orderservice.command.RejectOrderCommand;
import com.github.adetiamarhadi.orderservice.core.events.OrderApprovedEvent;
import com.github.adetiamarhadi.orderservice.core.events.OrderCreatedEvent;
import com.github.adetiamarhadi.orderservice.core.events.OrderRejectedEvent;
import com.github.adetiamarhadi.orderservice.core.model.OrderSummary;
import com.github.adetiamarhadi.orderservice.query.FindOrderQuery;
import com.github.adetiamarhadi.sagacore.commands.CancelProductReservationCommand;
import com.github.adetiamarhadi.sagacore.commands.ProcessPaymentCommand;
import com.github.adetiamarhadi.sagacore.commands.ReserveProductCommand;
import com.github.adetiamarhadi.sagacore.events.PaymentProcessedEvent;
import com.github.adetiamarhadi.sagacore.events.ProductReservationCancelledEvent;
import com.github.adetiamarhadi.sagacore.events.ProductReservedEvent;
import com.github.adetiamarhadi.sagacore.model.User;
import com.github.adetiamarhadi.sagacore.query.FetchUserPaymentDetailsQuery;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.axonframework.spring.stereotype.Saga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Saga
public class OrderSaga {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrderSaga.class);

	public static final String PAYMENT_PROCESSING_DEADLINE = "payment-processing-deadline";

	private transient CommandGateway commandGateway;
	private transient QueryGateway queryGateway;
	private transient DeadlineManager deadlineManager;
	private transient QueryUpdateEmitter queryUpdateEmitter;

	private String scheduleId;

	@Autowired
	public void setCommandGateway(CommandGateway commandGateway) {
		this.commandGateway = commandGateway;
	}

	@Autowired
	public void setQueryGateway(QueryGateway queryGateway) {
		this.queryGateway = queryGateway;
	}

	@Autowired
	public void setDeadlineManager(DeadlineManager deadlineManager) {
	    this.deadlineManager = deadlineManager;
    }

    @Autowired
	public void setQueryUpdateEmitter(QueryUpdateEmitter queryUpdateEmitter) {
		this.queryUpdateEmitter = queryUpdateEmitter;
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

				RejectOrderCommand rejectOrderCommand = new RejectOrderCommand(orderCreatedEvent.getOrderId(),
						commandResultMessage.exceptionResult().getMessage());

				commandGateway.send(rejectOrderCommand);
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

			cancelProductReservation(productReservedEvent, e.getMessage());

			return;
		}

		if (null == user) {
			LOGGER.warn("user id not found.");

			cancelProductReservation(productReservedEvent, "could not fetch user payment details");

			return;
		}

		LOGGER.info("successfully fetched user payment details for user " + user.getFirstName());

		scheduleId = deadlineManager.schedule(Duration.of(120, ChronoUnit.SECONDS), PAYMENT_PROCESSING_DEADLINE,
                productReservedEvent);

		ProcessPaymentCommand processPaymentCommand = ProcessPaymentCommand.builder()
				.orderId(productReservedEvent.getOrderId())
				.paymentDetails(user.getPaymentDetails())
				.paymentId(UUID.randomUUID().toString())
				.build();

		String result = null;

		try {
			result = commandGateway.sendAndWait(processPaymentCommand);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);

			cancelProductReservation(productReservedEvent, e.getMessage());

			return;
		}

		if (null == result) {
			LOGGER.warn("the result of process payment command is null.");

			cancelProductReservation(productReservedEvent, "could not process user payment with provided payment details");
		}
	}

	@SagaEventHandler(associationProperty = "orderId")
	public void handle(PaymentProcessedEvent paymentProcessedEvent) {

	    cancelDeadline();

		ApproveOrderCommand approveOrderCommand = new ApproveOrderCommand(paymentProcessedEvent.getOrderId());

		commandGateway.send(approveOrderCommand);
	}

	@EndSaga
	@SagaEventHandler(associationProperty = "orderId")
	public void handle(OrderApprovedEvent orderApprovedEvent) {

		LOGGER.info("order is approved. order saga is complete for order id: " + orderApprovedEvent.getOrderId());

		queryUpdateEmitter.emit(FindOrderQuery.class, query -> true, new OrderSummary(orderApprovedEvent.getOrderId(),
				orderApprovedEvent.getOrderStatus(), ""));
	}

	private void cancelProductReservation(ProductReservedEvent productReservedEvent, String reason) {

		cancelDeadline();

		CancelProductReservationCommand cancelProductReservationCommand = CancelProductReservationCommand.builder()
				.orderId(productReservedEvent.getOrderId())
				.productId(productReservedEvent.getProductId())
				.quantity(productReservedEvent.getQuantity())
				.userId(productReservedEvent.getUserId())
				.reason(reason)
				.build();

		commandGateway.send(cancelProductReservationCommand);
	}

	@SagaEventHandler(associationProperty = "orderId")
	public void handle(ProductReservationCancelledEvent productReservationCancelledEvent) {

		RejectOrderCommand rejectOrderCommand = new RejectOrderCommand(productReservationCancelledEvent.getOrderId(),
				productReservationCancelledEvent.getReason());

		commandGateway.send(rejectOrderCommand);
	}

	@EndSaga
	@SagaEventHandler(associationProperty = "orderId")
	public void handle(OrderRejectedEvent orderRejectedEvent) {

		LOGGER.info("successfully rejected order id " + orderRejectedEvent.getOrderId());

		queryUpdateEmitter.emit(FindOrderQuery.class, query -> true, new OrderSummary(orderRejectedEvent.getOrderId(),
				orderRejectedEvent.getOrderStatus(), orderRejectedEvent.getReason()));
	}

	@DeadlineHandler(deadlineName = PAYMENT_PROCESSING_DEADLINE)
	public void handlePaymentDeadline(ProductReservedEvent productReservedEvent) {

	    LOGGER.info("payment processing deadline took place. sending a compensating command to cancel the product reservation");

	    cancelProductReservation(productReservedEvent, "payment timeout");
    }

	private void cancelDeadline() {
		if (null != scheduleId) {
			deadlineManager.cancelSchedule(PAYMENT_PROCESSING_DEADLINE, scheduleId);
			scheduleId = null;
		}
	}
}
