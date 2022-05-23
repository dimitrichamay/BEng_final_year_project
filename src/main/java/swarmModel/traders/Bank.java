package swarmModel.traders;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;
import swarmModel.Globals;
import swarmModel.links.Messages;
import swarmModel.links.Messages.BorrowRequest;
import swarmModel.links.Messages.PayBackLoan;

public class Bank extends Agent<Globals> {

  @Variable
  public double moneyLent = 0;

  @Variable
  public double capitalToLend = 10000000;

  private static Action<Bank> action(SerializableConsumer<Bank> consumer) {
    return Action.create(Bank.class, consumer);
  }

  public static Action<Bank> lendMoney() {
    return action(bank -> {
      // Process money paid back to bank
      double paidBack = bank.getMessagesOfType(PayBackLoan.class).stream()
          .mapToDouble(m -> m.amountToPayBack).sum();
      bank.capitalToLend += paidBack;

      // Process borrow requests
      bank.getMessagesOfType(BorrowRequest.class).stream().forEach(m ->
      {
        double amountLent = 0;
        if (m.borrowAmount < (bank).capitalToLend) {
          amountLent = m.borrowAmount;
        } else {
          amountLent = bank.capitalToLend;
        }
        double finalAmountLent = amountLent;
        bank.moneyLent += finalAmountLent;

        bank.send(Messages.BorrowOutcome.class, (msg) -> {
          msg.lendAmount = finalAmountLent;
        }).to(m.getSender());
      });
    });
  }

}
