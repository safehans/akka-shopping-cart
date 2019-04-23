package com.example.shoppingcart.shoppingCart.impl;

import java.util.Optional;

import com.example.shoppingcart.shoppingCart.impl.ShoppingCartCommand.Checkout;
import com.example.shoppingcart.shoppingCart.impl.ShoppingCartCommand.Get;
import com.example.shoppingcart.shoppingCart.impl.ShoppingCartCommand.UpdateItem;
import com.example.shoppingcart.shoppingCart.impl.ShoppingCartEvent.CheckedOut;
import com.example.shoppingcart.shoppingCart.impl.ShoppingCartEvent.ItemUpdated;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;

import akka.Done;

public class ShoppingCartEntity extends PersistentEntity<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState> {

	@Override
	public Behavior initialBehavior(Optional<ShoppingCartState> snapshotState) {

		ShoppingCartState state = snapshotState.orElse(ShoppingCartState.EMPTY);
		BehaviorBuilder b = newBehaviorBuilder(state);

		if (state.isCheckedOut()) {
			return checkedOut(b);
		} else {
			return openShoppingCart(b);
		}
	}

	/**
	 * Create a behavior for the open shopping cart state.
	 */
	private Behavior openShoppingCart(BehaviorBuilder b) {
		// Command handler for the UpdateItem command
		b.setCommandHandler(UpdateItem.class, (cmd, ctx) -> {
			if (cmd.getQuantity() < 0) {
				ctx.commandFailed(new ShoppingCartException("Quantity must be greater than zero"));
				return ctx.done();
			} else if (cmd.getQuantity() == 0 && !state().getItems().containsKey(cmd.getProductId())) {
				ctx.commandFailed(new ShoppingCartException("Cannot delete item that is not already in cart"));
				return ctx.done();
			} else {
				return ctx.thenPersist(new ItemUpdated(entityId(), cmd.getProductId(), cmd.getQuantity()),
						e -> ctx.reply(Done.getInstance()));
			}
		});

		// Command handler for the Checkout command
		b.setCommandHandler(Checkout.class, (cmd, ctx) -> {
			if (state().getItems().isEmpty()) {
				ctx.commandFailed(new ShoppingCartException("Cannot checkout empty cart"));
				return ctx.done();
			} else {
				return ctx.thenPersist(new CheckedOut(entityId()), e -> ctx.reply(Done.getInstance()));
			}
		});
		commonHandlers(b);
		return b.build();
	}

	/**
	 * Create a behavior for the checked out state.
	 */
	private Behavior checkedOut(BehaviorBuilder b) {
		b.setReadOnlyCommandHandler(UpdateItem.class, (cmd, ctx) -> ctx
				.commandFailed(new ShoppingCartException("Can't update item on already checked out shopping cart")));
		b.setReadOnlyCommandHandler(Checkout.class, (cmd, ctx) -> ctx
				.commandFailed(new ShoppingCartException("Can't checkout on already checked out shopping cart")));
		commonHandlers(b);
		return b.build();
	}

	/**
	 * Add all the handlers that are shared across all states to the behavior
	 * builder.
	 */
	private void commonHandlers(BehaviorBuilder b) {
		b.setReadOnlyCommandHandler(Get.class, (cmd, ctx) -> ctx.reply(state()));

		b.setEventHandler(ItemUpdated.class,
				itemUpdated -> state().updateItem(itemUpdated.getProductId(), itemUpdated.getQuantity()));

		b.setEventHandlerChangingBehavior(CheckedOut.class, e -> checkedOut(newBehaviorBuilder(state().checkout())));
	}

}
