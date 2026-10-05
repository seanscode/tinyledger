package uk.co.howes.sean.ledger.model;

import org.junit.jupiter.api.Test;
import uk.co.howes.sean.ledger.exceptions.OverDrawnException;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LedgerTest {

  private static final String LEDGER_ID = "ledger-1";

  @Test
  void createsAnEmptyLedgerWithZeroBalance() {
    Ledger ledger = new Ledger(LEDGER_ID, "GBP");

    assertEquals(LEDGER_ID, ledger.getLedgerId());
    assertEquals("GBP", ledger.getCurrency());
    assertEquals(BigInteger.ZERO, ledger.getCurrentBalance());
    assertEquals(List.of(), ledger.getTransactionsList());

    assertTrue(ledger.getCreatedAt().toString().endsWith("Z"));
  }

  @Test
  void recordsDepositsAndWithdrawalsWithCorrectBalance() {
    Ledger ledger = new Ledger(LEDGER_ID, "GBP");

    ledger.addTransaction("deposit", BigInteger.valueOf(1000));
    ledger.addTransaction("withdrawal", BigInteger.valueOf(250));

    assertEquals(BigInteger.valueOf(750), ledger.getCurrentBalance());
    assertEquals(TransactionType.DEPOSIT, ledger.getTransactionsList().get(0).getType());
    assertEquals(TransactionType.WITHDRAWAL, ledger.getTransactionsList().get(1).getType());
  }

  @Test
  void balanceCalcuatedForGivenTime() throws InterruptedException {
    Ledger ledger = new Ledger("over-time", "GBP");

    ledger.addTransaction("deposit", BigInteger.valueOf(100));
    Thread.sleep(500);
    Transaction midPoint = ledger.addTransaction("deposit", BigInteger.valueOf(100));
    Thread.sleep(500);
    ledger.addTransaction("deposit", BigInteger.valueOf(100));

    BigInteger bal = ledger.getBalanceAt(midPoint.getCreatedAt());
    assertEquals(BigInteger.valueOf(200), bal);
  }



  @Test
  void transactionBalanceIsImmutable() {
    Ledger ledger = new Ledger(LEDGER_ID, "GBP");
    ledger.addTransaction("deposit", BigInteger.TEN);

    assertThrows(UnsupportedOperationException.class,
      () -> ledger.getTransactionsList().clear());
    assertEquals(BigInteger.TEN, ledger.getCurrentBalance());
  }

  @Test
  void testNegativeBalanceThrowsOverdrawnException() {
    Ledger ledger = new Ledger("overdrawn", "EUR");
    assertThrows(OverDrawnException.class, () -> ledger.addTransaction("withdrawal", BigInteger.valueOf(100L)));

  }
}
