package uk.co.howes.sean.ledger;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(VertxExtension.class)
class MainVerticleTest {

  private static final URI BASE_URI = URI.create("http://localhost:8888");

  private final HttpClient httpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(5))
    .build();

  private Vertx vertx;
  private String deploymentId;

  @BeforeEach
  void deployVerticle(Vertx vertx) throws Exception {
    this.vertx = vertx;
    this.deploymentId = vertx.deployVerticle(new MainVerticle())
      .toCompletionStage().toCompletableFuture()
      .get(10, TimeUnit.SECONDS);
  }

  @AfterEach
  void undeployVerticle() throws Exception {
    if (deploymentId != null) {
      vertx.undeploy(deploymentId)
        .toCompletionStage().toCompletableFuture()
        .get(10, TimeUnit.SECONDS);
    }
  }

  @Test
  void createsALedger() throws Exception {
    HttpResponse<String> response = post("/ledger", new JsonObject().put("currency", "GBP").encode());

    assertEquals(201, response.statusCode());
    JsonObject body = new JsonObject(response.body());
    assertNotNull(body.getString("id"));
    assertEquals("0", body.getString("balance"));
    assertEquals("GBP", body.getString("currency"));
    assertNotNull(body.getString("createdAt"));
  }

  @Test
  void acceptsDepositAndWithdrwalBalanceReflectsThemCorrectly() throws Exception {
    String ledgerId = createLedger();

    HttpResponse<String> deposit = postTransaction(ledgerId, "deposit", "1000");
    assertEquals(201, deposit.statusCode(), "deposit request body: " + deposit.body());
    JsonObject depositBody = new JsonObject(deposit.body());
    assertEquals("deposit", depositBody.getString("type"));
    assertEquals("1000", depositBody.getString("balanceAfter"));
    assertNotNull(depositBody.getString("id"));
    assertNotNull(depositBody.getString("createdAt"));

    HttpResponse<String> withdrawal = postTransaction(ledgerId, "withdrawal", "250");
    assertEquals(201, withdrawal.statusCode(), "withdrawal request body: " + withdrawal.body());

    JsonObject withdrawalBody = new JsonObject(withdrawal.body());
    assertEquals("withdrawal", withdrawalBody.getString("type"));
    assertEquals("750", withdrawalBody.getString("balanceAfter"));

    HttpResponse<String> balance = get("/ledger/" + ledgerId + "/balance");
    assertEquals(200, balance.statusCode());
    JsonObject balanceBody = new JsonObject(balance.body());
    assertEquals("750", balanceBody.getString("balance"));
    assertEquals("GBP", balanceBody.getString("currency"));
  }

  @Test
  void listsTransactionsFilteredByTypeAndLimit() throws Exception {
    String ledgerId = createLedger();
    postTransaction(ledgerId, "deposit", "1000");
    postTransaction(ledgerId, "withdrawal", "250");
    postTransaction(ledgerId, "deposit", "50");

    HttpResponse<String> all = get("/ledger/" + ledgerId + "/transactions");
    assertEquals(200, all.statusCode());
    assertEquals(3, new JsonObject(all.body()).getJsonArray("transactions").size());

    HttpResponse<String> depositsOnly = get("/ledger/" + ledgerId + "/transactions?type=deposit");
    assertEquals(200, depositsOnly.statusCode());
    assertEquals(2, new JsonObject(depositsOnly.body()).getJsonArray("transactions").size());

    HttpResponse<String> limited = get("/ledger/" + ledgerId + "/transactions?type=deposit&limit=1");
    assertEquals(200, limited.statusCode());
    JsonObject limitedBody = new JsonObject(limited.body());
    assertEquals(1, limitedBody.getJsonArray("transactions").size());
    assertEquals("deposit", limitedBody.getJsonArray("transactions").getJsonObject(0).getString("type"));
  }

  @Test
  void returnErrorForNotFoundLedger() throws Exception {
    HttpResponse<String> balance = get("/ledger/does-not-exist/balance");
    assertEquals(404, balance.statusCode());
    assertLedgerNotFoundBody(balance.body());

    HttpResponse<String> transactions = get("/ledger/does-not-exist/transactions");
    assertEquals(404, transactions.statusCode());
    assertLedgerNotFoundBody(transactions.body());

    HttpResponse<String> createTransaction = postTransaction("does-not-exist", "deposit", "100");
    assertEquals(404, createTransaction.statusCode());
    assertLedgerNotFoundBody(createTransaction.body());
  }

  @Test
  void zeroTransactionIsRejected() throws Exception {
    String ledgerId = createLedger();

    HttpResponse<String> response = postTransaction(ledgerId, "deposit", "0");

    assertEquals(422, response.statusCode(), "response body: " + response.body());
  }

  @Test
  void rejectsInvalidTransactionType() throws Exception {
    String ledgerId = createLedger();

    HttpResponse<String> response = post("/ledger/" + ledgerId + "/transactions",
      new JsonObject().put("type", "invalid").put("amount", "100").encode());

    assertEquals(400, response.statusCode(), "response body: " + response.body());
    JsonObject body = new JsonObject(response.body());
    assertEquals("VALIDATION_FAILED", body.getString("code"));
    assertEquals("The value of the request body is invalid. Reason: Property \"type\" does not match additional properties schema at #/type", body.getString("message"));
  }

  @Test
  void rejectsNegativeAmount() throws Exception {
    String ledgerId = createLedger();

    HttpResponse<String> response = post("/ledger/" + ledgerId + "/transactions",
      new JsonObject().put("type", "deposit").put("amount", "-100").encode());

    assertEquals(400, response.statusCode(), "response body: " + response.body());
    JsonObject body = new JsonObject(response.body());
    assertEquals("VALIDATION_FAILED", body.getString("code"));
    assertEquals("The value of the request body is invalid. Reason: Property \"amount\" does not match additional properties schema at #/amount", body.getString("message"));
  }


  @Test
  void skipBasedOnOffset() throws Exception {
    String ledgerId = createLedger();
    postTransaction(ledgerId, "deposit", "100");
    postTransaction(ledgerId, "deposit", "200");
    postTransaction(ledgerId, "deposit", "300");

    HttpResponse<String> response = get("/ledger/" + ledgerId + "/transactions?offset=1&limit=1");

    assertEquals(200, response.statusCode(), "response body: " + response.body());
    JsonObject body = new JsonObject(response.body());
    assertEquals(1, body.getJsonArray("transactions").size());
    assertEquals("200", body.getJsonArray("transactions").getJsonObject(0).getString("amount"));
  }

  private String createLedger() throws Exception {
    HttpResponse<String> response = post("/ledger", new JsonObject().put("currency", "GBP").encode());
    return new JsonObject(response.body()).getString("id");
  }

  private HttpResponse<String> postTransaction(String ledgerId, String type, String amount) throws Exception {
    return post("/ledger/" + ledgerId + "/transactions",
      new JsonObject().put("type", type).put("amount", amount).encode());
  }

  private HttpResponse<String> post(String path, String body) throws Exception {
    HttpRequest request = HttpRequest.newBuilder(BASE_URI.resolve(path))
      .timeout(Duration.ofSeconds(10))
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(body))
      .build();
    return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private void assertLedgerNotFoundBody(String body) {
    JsonObject json = new JsonObject(body);
    assertEquals("LEDGER_NOT_FOUND", json.getString("code"));
    assertNotNull(json.getString("message"));
  }

  private HttpResponse<String> get(String path) throws Exception {
    HttpRequest request = HttpRequest.newBuilder(BASE_URI.resolve(path))
      .timeout(Duration.ofSeconds(10))
      .GET()
      .build();
    return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
