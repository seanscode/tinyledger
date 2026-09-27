package uk.co.howes.sean.ledger.handler;

import io.vertx.core.json.JsonObject;
import uk.co.howes.sean.ledger.model.Transaction;

import java.util.Locale;

final class TransactionResponse {
  private TransactionResponse() {
  }

  static JsonObject from(Transaction transaction) {
    return new JsonObject()
      .put("id", transaction.getId())
      .put("type", transaction.getType().name().toLowerCase(Locale.ROOT))
      .put("amount", transaction.getAmount().toString())
      .put("balanceAfter", transaction.getBalanceAfter().toString())
      .put("createdAt", transaction.getCreatedAt().toString());
  }
}
