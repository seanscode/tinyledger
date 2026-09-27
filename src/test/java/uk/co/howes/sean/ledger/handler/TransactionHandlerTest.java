package uk.co.howes.sean.ledger.handler;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.co.howes.sean.ledger.model.Ledger;

import java.math.BigInteger;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionHandlerTest {

  private static final String LEDGER_ID = "ledger-1";

  @Test
  void returnsTransactionsForTheLedgerInEntryOrder() {
    Ledger ledger = new Ledger(LEDGER_ID, "GBP");
    ledger.addTransaction("deposit", BigInteger.valueOf(100));
    ledger.addTransaction("withdrawal", BigInteger.valueOf(25));
    RoutingContext context = contextFor(LEDGER_ID);

    new TransactionHandler(Map.of(LEDGER_ID,ledger)).handle(context);

    ArgumentCaptor<JsonObject> response = forClass(JsonObject.class);
    verify(context).json(response.capture());

    String transactionsKey = "transactions";
    JsonObject transactions = response.getValue();
    JsonArray transaction = transactions.getJsonArray(transactionsKey);

    assertEquals(2, transaction.size());
    assertEquals("deposit", transaction.getJsonObject(0).getString("type"));
    assertEquals("25", transaction.getJsonObject(1).getString("amount"));
    assertEquals("100", transaction.getJsonObject(0).getString("balanceAfter"));
    assertEquals(ledger.getTransactionsList().get(0).getId(),
      transaction.getJsonObject(0).getString("id"));
  }

  @Test
  void returnsNotFoundForAnUnknownLedger() {
    RoutingContext context = contextFor("missing");
    HttpServerResponse response = context.response();

    when(response.setStatusCode(404)).thenReturn(response);

    new TransactionHandler(Map.of()).handle(context);

    ArgumentCaptor<String> bodyCaptor = forClass(String.class);
    verify(response).setStatusCode(404);
    verify(response).end(bodyCaptor.capture());
    JsonObject body = new JsonObject(bodyCaptor.getValue());
    assertEquals("LEDGER_NOT_FOUND", body.getString("code"));
    assertNotNull(body.getString("message"));
  }

  @Test
  void filtersByTypeAndLimit() {
    Ledger ledger = new Ledger(LEDGER_ID, "GBP");
    ledger.addTransaction("deposit", BigInteger.valueOf(100));
    ledger.addTransaction("withdrawal", BigInteger.valueOf(25));
    ledger.addTransaction("deposit", BigInteger.valueOf(50));
    RoutingContext context = contextFor(LEDGER_ID);
    when(context.request().getParam("type")).thenReturn("deposit");
    when(context.request().getParam("limit")).thenReturn("1");

    new TransactionHandler(Map.of(LEDGER_ID,ledger)).handle(context);

    ArgumentCaptor<JsonObject> response = forClass(JsonObject.class);
    verify(context).json(response.capture());
    assertEquals(1, response.getValue().getJsonArray("transactions").size());
    assertEquals("deposit", response.getValue().getJsonArray("transactions").getJsonObject(0).getString("type"));
  }

  @Test
  void filtersByTypeWhenAnEarlierTransactionDoesNotMatch() {
    Ledger ledger = new Ledger(LEDGER_ID, "GBP");
    ledger.addTransaction("withdrawal", BigInteger.valueOf(25));
    ledger.addTransaction("deposit", BigInteger.valueOf(100));
    RoutingContext context = contextFor(LEDGER_ID);
    when(context.request().getParam("type")).thenReturn("deposit");

    new TransactionHandler(Map.of(LEDGER_ID, ledger)).handle(context);

    ArgumentCaptor<JsonObject> response = forClass(JsonObject.class);
    verify(context).json(response.capture());
    assertEquals(1, response.getValue().getJsonArray("transactions").size());
  }

  @Test
  void offsetApplies() {
    Ledger ledger = new Ledger(LEDGER_ID, "GBP");
    ledger.addTransaction("deposit", BigInteger.valueOf(100));
    ledger.addTransaction("deposit", BigInteger.valueOf(200));
    ledger.addTransaction("deposit", BigInteger.valueOf(300));
    RoutingContext context = contextFor(LEDGER_ID);
    when(context.request().getParam("offset")).thenReturn("1");
    when(context.request().getParam("limit")).thenReturn("1");

    new TransactionHandler(Map.of(LEDGER_ID, ledger)).handle(context);

    ArgumentCaptor<JsonObject> response = forClass(JsonObject.class);
    verify(context).json(response.capture());
    JsonArray transactions = response.getValue().getJsonArray("transactions");
    assertEquals(1, transactions.size());
    assertEquals("200", transactions.getJsonObject(0).getString("amount"));
  }

  private RoutingContext contextFor(String ledgerId) {
    RoutingContext context = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    when(context.request()).thenReturn(request);
    when(request.getParam("ledgerId")).thenReturn(ledgerId);
    when(context.response()).thenReturn(response);
    return context;
  }
}
