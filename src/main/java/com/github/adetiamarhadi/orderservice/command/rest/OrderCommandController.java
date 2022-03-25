package com.github.adetiamarhadi.orderservice.command.rest;

import com.github.adetiamarhadi.orderservice.command.CreateOrderCommand;
import com.github.adetiamarhadi.orderservice.core.model.OrderStatus;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderCommandController {

	private final CommandGateway commandGateway;

	public OrderCommandController(CommandGateway commandGateway) {
		this.commandGateway = commandGateway;
	}

	@PostMapping
	public String createOrder(@Valid @RequestBody CreateOrderRestModel createOrderRestModel) {

		CreateOrderCommand createOrderCommand = CreateOrderCommand.builder()
				.orderId(UUID.randomUUID().toString())
				.userId("27b95829-4f3f-4ddf-8983-151ba010e35b")
				.productId(createOrderRestModel.getProductId())
				.quantity(createOrderRestModel.getQuantity())
				.addressId(createOrderRestModel.getAddressId())
				.orderStatus(OrderStatus.CREATED)
				.build();

		return commandGateway.sendAndWait(createOrderCommand);
	}
}
