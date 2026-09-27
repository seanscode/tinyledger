package uk.co.howes.sean.ledger.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import uk.co.howes.sean.ledger.model.Ledger;
import uk.co.howes.sean.ledger.model.Transaction;

import java.util.List;
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

  private static JsonObject buildResponse(Ledger ledger, String type, int transactionOffset, int transactionLimit) {
    JsonArray jsonArray = new JsonArray();

    int matched = 0;
    List<Transaction> transactionsList = ledger.getTransactionsList();
    for(int i = transactionOffset; i <  transactionsList.size() && matched < transactionLimit; i++) {
      Transaction transaction = transactionsList.get(i);
      if (type == null || transaction.getType().name().toLowerCase(Locale.ROOT).equals(type)) {
        jsonArray.add(TransactionResponse.from(transaction));
        matched++;
      }
    }
    return new JsonObject().put("transactions", jsonArray);
  }
}


