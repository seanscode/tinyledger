package uk.co.howes.sean.ledger.handler;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
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
import static org.mockito.Mockito.*;

class BalanceHandlerTest {

  @Test
  void correctBalanceAfterDepositsAndWithdrawals() {
    Ledger ledger = new Ledger("ledger-1", "GBP");
    ledger.addTransaction("deposit", BigInteger.valueOf(100));
    ledger.addTransaction("withdrawal", BigInteger.valueOf(25));
    RoutingContext context = contextFor("ledger-1");

    ArgumentCaptor<JsonObject> response = forClass(JsonObject.class);

    new BalanceHandler(Map.of("ledger-1", ledger)).handle(context);

    verify(context).json(response.capture());
    assertEquals("75", response.getValue().getString("balance"));
    assertEquals("GBP", response.getValue().getString("currency"));
  }

  @Test
  void returnsNotFoundForUnknownLedger() {
    RoutingContext context = contextFor("missing");
    HttpServerResponse response = context.response();
    when(response.setStatusCode(404)).thenReturn(response);

    new BalanceHandler(Map.of()).handle(context);

    ArgumentCaptor<String> bodyCaptor = forClass(String.class);
    verify(response).setStatusCode(404);
    verify(response).end(bodyCaptor.capture());
    JsonObject body = new JsonObject(bodyCaptor.getValue());
    assertEquals("LEDGER_NOT_FOUND", body.getString("code"));
    assertNotNull(body.getString("message"));
  }


  @Test
  void atTimeBeforeAnyTransactionReturnsZero() {
    Ledger ledger = ledgerWithDepositThenWithdrawal();

    JsonObject body = balanceAt(ledger, "2000-01-01T00:00:00Z");

    assertEquals("0", body.getString("balance"));
  }

  @Test
  void atTimeAfterAllTransactionsReturnsCurrentBalance() {
    Ledger ledger = ledgerWithDepositThenWithdrawal();

    JsonObject body = balanceAt(ledger, "2999-01-01T00:00:00Z");

    assertEquals("75", body.getString("balance"));
    assertEquals("GBP", body.getString("currency"));
  }

  @Test
  void atTimeOfFirstTransactionReturnsBalanceAtThatPoint() {
    Ledger ledger = ledgerWithDepositThenWithdrawal();
    String firstCreatedAt = ledger.getTransactionsList().get(0).getCreatedAt().toString();

    JsonObject body = balanceAt(ledger, firstCreatedAt);

    assertEquals("100", body.getString("balance"));
  }

  @Test
  void unparseableAtTimeIsAValidationError() {
    RoutingContext context = contextFor("ledger-1", "not-a-time");
    HttpServerResponse response = context.response();

    new BalanceHandler(Map.of("ledger-1", new Ledger("ledger-1", "GBP"))).handle(context);

    verify(response).setStatusCode(400);
  }

  private Ledger ledgerWithDepositThenWithdrawal() {
    Ledger ledger = new Ledger("ledger-1", "GBP");
    ledger.addTransaction("deposit", BigInteger.valueOf(100));
    sleepBriefly(); // keeps the two createdAt timestamps distinct
    ledger.addTransaction("withdrawal", BigInteger.valueOf(25));
    return ledger;
  }

  private JsonObject balanceAt(Ledger ledger, String atTime) {
    RoutingContext context = contextFor("ledger-1", atTime);
    new BalanceHandler(Map.of("ledger-1", ledger)).handle(context);
    ArgumentCaptor<JsonObject> response = forClass(JsonObject.class);
    verify(context).json(response.capture());
    return response.getValue();
  }

  private RoutingContext contextFor(String ledgerId) {
    return contextFor(ledgerId, null);
  }

  private RoutingContext contextFor(String ledgerId, String atTime) {
    RoutingContext context = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    when(context.request()).thenReturn(request);
    when(request.getParam("ledgerId")).thenReturn(ledgerId);
    when(request.getParam("atTime")).thenReturn(atTime);
    when(context.response()).thenReturn(response);
    return context;
  }

  private static void sleepBriefly() {
    try {
      Thread.sleep(5);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
