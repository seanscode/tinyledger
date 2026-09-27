package uk.co.howes.sean.ledger.model;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

public class Transaction {
  private final String id;
  private final TransactionType type;
  private final BigInteger amount;
  private final BigInteger balanceAfter;
  private final Instant createdAt;

  public Transaction(TransactionType type, BigInteger amount, BigInteger balanceAfter) {
    this.id = UUID.randomUUID().toString();
    this.type = type;
    this.amount = amount;
    this.balanceAfter = balanceAfter;
    this.createdAt = Instant.now();
  }

  public String getId() {
    return id;
  }

  public BigInteger getAmount() {
    return amount;
  }

  public TransactionType getType() {
    return type;
  }

  public BigInteger getBalanceAfter() {
    return balanceAfter;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}

