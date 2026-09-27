package uk.co.howes.sean.ledger.handler;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

final class ErrorResponse {
  private ErrorResponse() {
  }

  static void sendLedgerNotFoundError(RoutingContext routingContext) {
    routingContext.response().setStatusCode(NOT_FOUND.code());
    routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    routingContext.response().end(new JsonObject()
      .put("code", "LEDGER_NOT_FOUND" )
      .put("message", "Ledger was not found.")
      .encode());
  }

  static void sendInvalidAmountError(RoutingContext routingContext) {
    routingContext.response().setStatusCode(UNPROCESSABLE_ENTITY.code());
    routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    routingContext.response().end(new JsonObject()
      .put("code", "INVALID_AMOUNT" )
      .put("message", "Amount must be greater than zero.")
      .encode());
  }

  public static void sendValidationError(RoutingContext routingContext, Throwable failure) {
    String code = "VALIDATION_FAILED";
    String message;
    if(failure instanceof HttpException) {
       message = ((HttpException) failure).getPayload();
    } else{
      message = failure.getMessage();
    }

    routingContext.response().setStatusCode(BAD_REQUEST.code());
    routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    routingContext.response().end(new JsonObject()
      .put("code", code)
      .put("message", message)
      .encode());
  }
}
