package com.example.shoppingcart.shoppingCart.api;

import static com.lightbend.lagom.javadsl.api.Service.named;
import static com.lightbend.lagom.javadsl.api.Service.restCall;
import static com.lightbend.lagom.javadsl.api.Service.topic;

import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.api.broker.kafka.KafkaProperties;
import com.lightbend.lagom.javadsl.api.transport.Method;

import akka.Done;
import akka.NotUsed;


public interface ShoppingCartService extends Service {

	String TOPIC_NAME = "shopping-cart";

	/**
	 * Get a shopping cart.
	 * <p>
	 * Example: curl http://localhost:9000/shoppingcart/123
	 */
	ServiceCall<NotUsed, ShoppingCart> get(String id);

	/**
	 * Update an items quantity in the shopping cart.
	 * <p>
	 * Example: curl -H "Content-Type: application/json" -X POST -d '{"productId":
	 * 456, "quantity": 2}' http://localhost:9000/shoppingcart/123
	 */
	ServiceCall<ShoppingCartItem, Done> updateItem(String id);

	/**
	 * Checkout the shopping cart.
	 * <p>
	 * Example: curl -X POST http://localhost:9000/shoppingcart/123/checkout
	 */
	ServiceCall<NotUsed, Done> checkout(String id);

	/**
	 * This gets published to Kafka.
	 */
	Topic<ShoppingCart> shoppingCartTopic();

	@Override
	default Descriptor descriptor() {		
		return named("shoppingCart")
				.withCalls(
						 	restCall(Method.GET, "/shoppingcart/:id", this::get),
			                restCall(Method.POST, "/shoppingcart/:id", this::updateItem),
			                restCall(Method.POST, "/shoppingcart/:id/checkout", this::checkout)
						)
				.withTopics(
						topic(TOPIC_NAME, this::shoppingCartTopic)						
						.withProperty(KafkaProperties.partitionKeyStrategy(), ShoppingCart::getId))
				.withAutoAcl(true);	
	}
}
