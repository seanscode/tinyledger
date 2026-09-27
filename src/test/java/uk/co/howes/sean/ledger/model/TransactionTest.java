package uk.co.howes.sean.ledger.model;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionTest {

  @Test
  void constructsValidTransaction() {
    Transaction transaction = new Transaction(TransactionType.DEPOSIT, BigInteger.valueOf(50), BigInteger.valueOf(90));

    assertEquals(TransactionType.DEPOSIT, transaction.getType());
    assertEquals(BigInteger.valueOf(50), transaction.getAmount());
    assertEquals(BigInteger.valueOf(90), transaction.getBalanceAfter());
    assertTrue(transaction.getId().matches("[0-9a-f-]{36}"));
    assertTrue(transaction.getCreatedAt().toString().endsWith("Z"));
  }
}
