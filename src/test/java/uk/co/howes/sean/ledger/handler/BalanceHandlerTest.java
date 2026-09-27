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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
