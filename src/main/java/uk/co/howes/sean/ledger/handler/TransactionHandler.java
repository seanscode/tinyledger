package uk.co.howes.sean.ledger.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import uk.co.howes.sean.ledger.model.Ledger;
import uk.co.howes.sean.ledger.model.Transaction;

import java.util.Locale;
import java.util.Map;

public class TransactionHandler implements Handler<RoutingContext> {

  private final Map<String,Ledger> ledgerMap;

  public TransactionHandler(Map<String,Ledger> ledgerMap) {
    this.ledgerMap = ledgerMap;
  }

  @Override
  public void handle(RoutingContext routingContext) {
   String ledgerId = routingContext.request().getParam("ledgerId");
   Ledger ledger = ledgerMap.get(ledgerId);

    if (ledger == null) {
      ErrorResponse.sendLedgerNotFoundError(routingContext);
      return;
    }

    String type = routingContext.request().getParam("type");
    String limit = routingContext.request().getParam("limit");
    String offset = routingContext.request().getParam("offset");

    int transactionLimit = limit == null ? 50 : Integer.parseInt(limit);
    int transactionOffset = offset == null ? 0 : Integer.parseInt(offset);

    JsonObject reponse = buildResponse(ledger, type, transactionOffset, transactionLimit);
    routingContext.json(reponse);
  }

  private static JsonObject buildResponse(Ledger ledger, String type, int offset, int limit) {
    JsonArray transactions = new JsonArray();

    ledger.getTransactionsList().stream()
      .filter(t -> type == null || isMatchingType(type, t))
      .skip(offset)
      .limit(limit)
      .map(TransactionResponse::from)
      .forEach(transactions::add);

    return new JsonObject().put("transactions", transactions);
  }

  private static boolean isMatchingType(String type, Transaction transaction) {
    return transaction.getType().name().toLowerCase(Locale.ROOT).equals(type);
  }
}


