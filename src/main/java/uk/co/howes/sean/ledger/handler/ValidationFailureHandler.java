package uk.co.howes.sean.ledger.handler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class ValidationFailureHandler implements Handler<RoutingContext> {
  @Override
  public void handle(RoutingContext routingContext) {
    ErrorResponse.sendValidationError(routingContext, routingContext.failure());
  }
}
