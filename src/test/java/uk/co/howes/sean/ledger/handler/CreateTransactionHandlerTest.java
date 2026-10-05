package uk.co.howes.sean.ledger.handler;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.openapi.validation.RequestParameter;
import io.vertx.openapi.validation.ValidatedRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.co.howes.sean.ledger.exceptions.OverDrawnException;
import uk.co.howes.sean.ledger.model.Ledger;
import uk.co.howes.sean.ledger.model.Transaction;
import uk.co.howes.sean.ledger.model.TransactionType;

import java.math.BigInteger;
import java.util.Map;

import static io.vertx.ext.web.openapi.router.RouterBuilder.KEY_META_DATA_VALIDATED_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateTransactionHandlerTest {

  private static final String LEDGER_ID = "ledger-1";

  @Test
  void addsValidTransactionToTheLedger() {
    Ledger ledger = new Ledger(LEDGER_ID, "GBP");
    RoutingContext context = transactionContext(LEDGER_ID, "deposit", "500");
    HttpServerResponse response = context.response();
    when(response.setStatusCode(201)).thenReturn(response);
    when(response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")).thenReturn(response);

    new CreateTransactionHandler(Map.of(LEDGER_ID, ledger)).handle(context);

    assertEquals(BigInteger.valueOf(500), ledger.getCurrentBalance());
    Transaction transaction = ledger.getTransactionsList().get(0);

    assertEquals(TransactionType.DEPOSIT, transaction.getType());

    ArgumentCaptor<String> bodyCaptor = forClass(String.class);
    verify(response).end(bodyCaptor.capture());
    JsonObject responseBody = new JsonObject(bodyCaptor.getValue());
    assertEquals(transaction.getId(), responseBody.getString("id"));
    assertEquals("deposit", responseBody.getString("type"));
    assertEquals("500", responseBody.getString("amount"));
    assertEquals("500", responseBody.getString("balanceAfter"));
    assertEquals(transaction.getCreatedAt().toString(), responseBody.getString("createdAt"));
  }


  @Test
  void returnsNotFoundWhenLedgerDoesNotExist() {
    RoutingContext context = transactionContext("missing", "withdrawal", "10");
    HttpServerResponse response = context.response();
    when(response.setStatusCode(404)).thenReturn(response);

    new CreateTransactionHandler(Map.of()).handle(context);

    verify(response).end(anyString());
  }

  @Test
  void negativeBalanceResultsInInsufficentFundsError() {
    Ledger ledger = new Ledger("overdrawn", "GBP");
    RoutingContext context = transactionContext("overdrawn", "withdrawal", "500");
    HttpServerResponse response = context.response();
    when(response.setStatusCode(422)).thenReturn(response);

    assertThrows(OverDrawnException.class, () ->
      new CreateTransactionHandler(Map.of("overdrawn", ledger)).handle(context));
  }

  private RoutingContext transactionContext(String ledgerId, String type, String amount) {
    RoutingContext context = mock(RoutingContext.class);
    HttpServerRequest httpRequest = mock(HttpServerRequest.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    ValidatedRequest request = mock(ValidatedRequest.class);
    RequestParameter body = mock(RequestParameter.class);

    when(context.request()).thenReturn(httpRequest);
    when(httpRequest.getParam("ledgerId")).thenReturn(ledgerId);
    when(context.response()).thenReturn(response);
    when(context.get(KEY_META_DATA_VALIDATED_REQUEST)).thenReturn(request);
    when(request.getBody()).thenReturn(body);
    when(body.getJsonObject()).thenReturn(new JsonObject().put("type", type).put("amount", amount));
    return context;
  }
}
