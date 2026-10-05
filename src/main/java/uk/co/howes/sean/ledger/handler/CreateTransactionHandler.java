package uk.co.howes.sean.ledger.handler;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.openapi.validation.ValidatedRequest;
import uk.co.howes.sean.ledger.model.Ledger;

import java.math.BigInteger;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.vertx.ext.web.openapi.router.RouterBuilder.*;

public class CreateTransactionHandler implements Handler<RoutingContext> {

  private final Map<String, Ledger> ledgerMap;

  public CreateTransactionHandler(Map<String, Ledger> ledgerMap) {
    this.ledgerMap = ledgerMap;
  }

  @Override
  public void handle(RoutingContext routingContext) {

    ValidatedRequest request = routingContext.get(KEY_META_DATA_VALIDATED_REQUEST);

    JsonObject body = request.getBody().getJsonObject();
    String type = body.getString("type");


    String ledgerId = routingContext.request().getParam("ledgerId");
    Ledger ledger = ledgerMap.get(ledgerId);


    if (ledger == null) {
      ErrorResponse.sendLedgerNotFoundError(routingContext);
    } else {
      BigInteger amount = new BigInteger(body.getString("amount"));

      if (BigInteger.ZERO.equals(amount)) {
        ErrorResponse.sendInvalidAmountError(routingContext);
        return;
      }
      JsonObject response = TransactionResponse.from(ledger.addTransaction(type, amount));
      routingContext.response()
        .setStatusCode(CREATED.code())
        .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        .end(response.encode());
    }
  }
}


