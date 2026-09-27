package uk.co.howes.sean.ledger;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.openapi.router.OpenAPIRoute;
import io.vertx.ext.web.openapi.router.RouterBuilder;
import io.vertx.openapi.contract.OpenAPIContract;
import uk.co.howes.sean.ledger.handler.*;
import uk.co.howes.sean.ledger.model.Ledger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static uk.co.howes.sean.ledger.Routes.*;

public class MainVerticle extends VerticleBase {

  private static final int PORT = 8888;
  private static final String SERVER_STARTED_ON = "Server Started on %d\n";
  private final Map<String,Ledger> ledgerMap = new ConcurrentHashMap<>();

  @Override
  public Future<?> start() {
    Future<OpenAPIContract> openAPIContractFuture = OpenAPIContract.from(vertx, "ledger.yaml");
    return startHttpServer(openAPIContractFuture);
  }

  private Future<HttpServer> startHttpServer(Future<OpenAPIContract> openAPIContractFuture) {
    return openAPIContractFuture.compose(contract -> {
      Router router = buildRoutes(contract);
      return createWebServer(router);
    });
  }

  private Future<HttpServer> createWebServer(Router router) {
    return vertx.createHttpServer()
      .requestHandler(router)
      .listen(PORT)
      .timeout(10, TimeUnit.SECONDS)
      .onSuccess(server -> System.out.printf(SERVER_STARTED_ON, PORT))
      .onFailure(t -> {
        t.printStackTrace();
        vertx.close();
        System.exit(1);
      });
  }

  private Router buildRoutes(OpenAPIContract contract) {
    RouterBuilder routerBuilder = RouterBuilder.create(vertx, contract);

    routerBuilder.getRoute(CREATE_LEDGER).addHandler(new CreateLedgerHandler(ledgerMap));
    routerBuilder.getRoute(CREATE_TRANSACTION).addHandler(new CreateTransactionHandler(ledgerMap));
    routerBuilder.getRoute(GET_BALANCE).addHandler(new BalanceHandler(ledgerMap));
    routerBuilder.getRoute(GET_TRANSACTIONS).addHandler(new TransactionHandler(ledgerMap));

    ValidationFailureHandler failureHandler = new ValidationFailureHandler();
    for (OpenAPIRoute route : routerBuilder.getRoutes()) {
      route.addFailureHandler(failureHandler);
    }

    return routerBuilder.createRouter();
  }
}
