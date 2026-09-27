package uk.co.howes.sean.ledger.handler;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.openapi.validation.RequestParameter;
import io.vertx.openapi.validation.ValidatedRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.co.howes.sean.ledger.model.Ledger;

import java.util.HashMap;
import java.util.Map;

import static io.vertx.ext.web.openapi.router.RouterBuilder.KEY_META_DATA_VALIDATED_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateLedgerHandlerTest {

  @Test
  void createsALedgerAndReturnsItsDetails() {
    Map<String, Ledger> ledgers = new HashMap<>();
    RoutingContext context = mock(RoutingContext.class);
    ValidatedRequest request = mock(ValidatedRequest.class);
    RequestParameter body = mock(RequestParameter.class);
    HttpServerResponse response = mock(HttpServerResponse.class);

    when(context.get(KEY_META_DATA_VALIDATED_REQUEST)).thenReturn(request);
    when(request.getBody()).thenReturn(body);
    when(body.getJsonObject()).thenReturn(new JsonObject().put("currency", "GBP"));
    when(context.response()).thenReturn(response);
    when(response.setStatusCode(201)).thenReturn(response);

    new CreateLedgerHandler(ledgers).handle(context);

    ArgumentCaptor<String> bodyCaptor = forClass(String.class);
    verify(response).end(bodyCaptor.capture());

    JsonObject responseBody = new JsonObject(bodyCaptor.getValue());

    String ledgerId = responseBody.getString("id");
    Ledger foundLedger = ledgers.get(ledgerId);

    assertEquals(1, ledgers.size());
    assertEquals("GBP", foundLedger.getCurrency());
    assertEquals(foundLedger.getLedgerId(), ledgerId);
    assertEquals("0", responseBody.getString("balance"));
    assertEquals("GBP", responseBody.getString("currency"));
    assertEquals(foundLedger.getCreatedAt().toString(), responseBody.getString("createdAt"));
  }
}

