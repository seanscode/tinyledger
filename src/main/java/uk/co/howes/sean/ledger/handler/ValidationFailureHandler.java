package uk.co.howes.sean.ledger.handler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import uk.co.howes.sean.ledger.exceptions.OverDrawnException;

public class ValidationFailureHandler implements Handler<RoutingContext> {
  @Override
  public void handle(RoutingContext routingContext) {
    if (routingContext.failure() instanceof OverDrawnException) {
      ErrorResponse.sendOverdrawnError(routingContext);
    } else {
      ErrorResponse.sendValidationError(routingContext, routingContext.failure());
    }
  }
}

