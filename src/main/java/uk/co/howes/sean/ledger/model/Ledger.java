package uk.co.howes.sean.ledger.model;

import uk.co.howes.sean.ledger.exceptions.OverDrawnException;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Ledger {
  private final String ledgerId;
  private final String currency;
  private final List<Transaction> transactionsList;
  private final Instant createdAt;

  public Ledger(String ledgerId, String currency) {
    this.ledgerId = ledgerId;
    this.currency = currency;
    this.transactionsList = new ArrayList<>();
    this.createdAt = Instant.now();
  }

  public String getLedgerId() {
    return ledgerId;
  }

  public String getCurrency() {
    return currency;
  }

  public List<Transaction> getTransactionsList() {
    return List.copyOf(transactionsList);
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public BigInteger getCurrentBalance() {
    return transactionsList.stream()
      .map(transaction -> transaction.getType() == TransactionType.DEPOSIT
        ? transaction.getAmount()
        : transaction.getAmount().negate())
      .reduce(BigInteger.ZERO, BigInteger::add);
  }

  public Transaction addTransaction(String type, BigInteger amount) throws OverDrawnException {
    BigInteger balanceAfter = getCurrentBalance();
    TransactionType transactionType = TransactionType.valueOf(type.toUpperCase(Locale.ROOT));
    if (transactionType == TransactionType.DEPOSIT) {
      balanceAfter = balanceAfter.add(amount);
    } else {

      BigInteger result = balanceAfter.subtract(amount);
      if (result.compareTo(BigInteger.ZERO) < 0) {
        throw new OverDrawnException();
      }
      balanceAfter = result;
    }
    Transaction transaction = new Transaction(transactionType, amount, balanceAfter);
    transactionsList.add(transaction);
    return transaction;
  }

  public BigInteger getBalanceAt(Instant atTime) {
    Transaction lastBeforeTime = null;

    for (Transaction transaction : transactionsList) {
      if (transaction.getCreatedAt().compareTo(atTime) <= 0) {
        lastBeforeTime = transaction;
      }
    }
    BigInteger balance = BigInteger.ZERO;
    if (lastBeforeTime != null) {
      balance = lastBeforeTime.getBalanceAfter();
    }
    return balance;
  }
}
