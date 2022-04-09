package com.github.adetiamarhadi.orderservice.core.events;

import com.github.adetiamarhadi.orderservice.core.model.OrderStatus;
import lombok.Value;

@Value
public class OrderRejectedEvent {

    private final String orderId;
    private final String reason;
    private final OrderStatus orderStatus = OrderStatus.REJECTED;
}
