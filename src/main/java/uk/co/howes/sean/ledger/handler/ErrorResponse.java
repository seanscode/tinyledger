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
    routingContext.response().end(withLedgerNotFound());
  }


  static void sendInvalidAmountError(RoutingContext routingContext) {
    routingContext.response()
      .setStatusCode(UNPROCESSABLE_ENTITY.code())
      .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
      .end(withInvalidAmount());
  }

  static void sendOverdrawnError(RoutingContext routingContext) {
    routingContext.response()
      .setStatusCode(UNPROCESSABLE_ENTITY.code())
      .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
      .end(withErrorCodeAndMessage("LEDGER_OVERDRAWN", "transcation would cause ledger to be overdrawn"));
  }

  public static void sendValidationError(RoutingContext routingContext, Throwable failure) {
    String code = "VALIDATION_FAILED";
    String message;
    if (failure instanceof HttpException) {
      message = ((HttpException) failure).getPayload();
    } else {
      message = failure == null ? "no value sent " :failure.getMessage();
    }

    routingContext.response().setStatusCode(BAD_REQUEST.code());
    routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    routingContext.response().end(withErrorCodeAndMessage(code, message));
  }

  private static String withErrorCodeAndMessage(String code, String message) {
    return new JsonObject()
      .put("code", code)
      .put("message", message)
      .encode();
  }

  private static String withLedgerNotFound() {
    return withErrorCodeAndMessage("LEDGER_NOT_FOUND", "Ledger was not found.");
  }

  private static String withInvalidAmount() {
    return withErrorCodeAndMessage("INVALID_AMOUNT", "Amount must be greater than zero.");
  }
}
