package uk.co.howes.sean.ledger.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import uk.co.howes.sean.ledger.model.Ledger;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

public class BalanceHandler implements Handler<RoutingContext> {

  private final Map<String,Ledger> ledgerMap;

  public BalanceHandler(Map<String,Ledger> ledgerMap) {
    this.ledgerMap = ledgerMap;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    String ledgerId = routingContext.request().getParam("ledgerId");
    String atTime = routingContext.request().getParam("atTime");

    Ledger ledger = ledgerMap.get(ledgerId);

    if (ledger == null) {
      ErrorResponse.sendLedgerNotFoundError(routingContext);
      return;
    }

    BigInteger balance;
    if (atTime == null) {
      balance = ledger.getCurrentBalance();
    } else {
      try {
        balance = ledger.getBalanceAt(OffsetDateTime.parse(atTime).toInstant());
      } catch (DateTimeParseException e) {
        ErrorResponse.sendValidationError(routingContext, e);
        return;
      }
    }

    JsonObject response = buildBalanceResponse(ledger, balance);
    routingContext.json(response);
  }

  private static JsonObject buildBalanceResponse(Ledger ledger, BigInteger balance) {
    JsonObject response = new JsonObject();
    response.put("balance", balance.toString());
    response.put("currency", ledger.getCurrency());

    return response;
  }

}
