package com.github.adetiamarhadi.orderservice.core.data;

import com.github.adetiamarhadi.orderservice.core.model.OrderStatus;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "orders")
@Data
public class OrderEntity {

	@Id
	@Column(unique = true)
	public String orderId;

	private String productId;
	private String userId;
	private int quantity;
	private String addressId;

	@Enumerated(EnumType.STRING)
	private OrderStatus orderStatus;
}
