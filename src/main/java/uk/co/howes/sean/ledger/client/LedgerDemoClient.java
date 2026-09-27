package uk.co.howes.sean.ledger.client;

import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Demo the ledger but make sure you have the server running first
 * see the readme.md for details
 */
public final class LedgerDemoClient {

  private static final URI BASE_URI = URI.create("http://localhost:8888");
  private final HttpClient client;

  private LedgerDemoClient() {
    this.client = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .build();
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    new LedgerDemoClient().run();
  }

  private void run() throws IOException, InterruptedException {
    System.out.println("Ledger demo against ");

    String ledger = post("/ledger", "{\"currency\":\"GBP\"}");
    String ledgerId = getLedgerId(ledger);
    printResponse("Create ledger", ledger);

    printResponse("Deposit 1000", postTransaction(ledgerId, "deposit", "1000"));
    printResponse("Withdraw 250", postTransaction(ledgerId, "withdrawal", "250"));
    printResponse("Deposit 200", postTransaction(ledgerId, "deposit", "200"));
    printResponse("Get balance", get("/ledger/" + ledgerId + "/balance"));
    printResponse("List first deposit", get("/ledger/" + ledgerId + "/transactions?type=deposit&limit=1"));
    printResponse("List all transactions", get("/ledger/" + ledgerId + "/transactions"));

    String ledger2 = post("/ledger", "{\"currency\":\"GBP\"}");
    printResponse("Create second ledger", ledger2);
    printResponse("List all transactions", get("/ledger/" + getLedgerId(ledger2) + "/transactions"));

  }

  private String postTransaction(String ledgerId, String type, String amount) throws IOException, InterruptedException {
    return post("/ledger/" + ledgerId + "/transactions", "{\"type\":\"" + type + "\",\"amount\":\"" + amount + "\"}");
  }

  private String post(String path, String body) throws IOException, InterruptedException {

    HttpRequest request = HttpRequest.newBuilder(BASE_URI.resolve(path))
      .timeout(Duration.ofSeconds(10))
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(body))
      .build();

    return send(request);
  }

  private String get(String path) throws IOException, InterruptedException {

    HttpRequest request = HttpRequest.newBuilder(BASE_URI.resolve(path))
      .timeout(Duration.ofSeconds(10))
      .header("Accept", "application/json")
      .GET()
      .build();

    return send(request);
  }

  private String send(HttpRequest request) throws IOException, InterruptedException {
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    return response.body();
  }

  private String getLedgerId(String response) {
    return new JsonObject(response).getString("id");
  }

  private void printResponse(String label, String body) {
    System.out.println();
    System.out.println(label + ":");
    System.out.println(body);
  }
}
