package uk.co.howes.sean.ledger.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.openapi.validation.ValidatedRequest;
import uk.co.howes.sean.ledger.model.Ledger;

import java.util.Map;
import java.util.UUID;

import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.vertx.ext.web.openapi.router.RouterBuilder.KEY_META_DATA_VALIDATED_REQUEST;

public class CreateLedgerHandler implements Handler<RoutingContext> {
  private final Map<String,Ledger> ledgerMap;

  public CreateLedgerHandler(Map<String,Ledger> ledgerMap) {
      this.ledgerMap = ledgerMap;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    Ledger ledger = buildNewLedger(routingContext);
    ledgerMap.put(ledger.getLedgerId(),ledger);

    JsonObject response = buildCreateLedgerResponse(ledger);
    routingContext.response().setStatusCode(CREATED.code()).end(response.encode());
  }

  private static JsonObject buildCreateLedgerResponse(Ledger ledger) {
    return new JsonObject()
      .put("id", ledger.getLedgerId())
      .put("balance", ledger.getCurrentBalance().toString())
      .put("currency", ledger.getCurrency())
      .put("createdAt", ledger.getCreatedAt().toString());
  }

  private static Ledger buildNewLedger(RoutingContext routingContext) {
    ValidatedRequest request = routingContext.get(KEY_META_DATA_VALIDATED_REQUEST);
    String currency = request.getBody().getJsonObject().getString("currency");
    return new Ledger(UUID.randomUUID().toString(), currency);
  }
}
