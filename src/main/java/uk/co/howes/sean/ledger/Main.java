package uk.co.howes.sean.ledger;

import io.vertx.core.Vertx;

/*
 this is included only to run the application in the ide.
 see the readme.md for instuctions on how to run the app
 */
public class Main {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    vertx.deployVerticle(new MainVerticle())
      .onFailure(Throwable::printStackTrace);
  }
}
