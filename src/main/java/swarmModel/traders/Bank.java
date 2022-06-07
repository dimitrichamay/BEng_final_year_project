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

  @Variable
  public double profitFromInterest = 0;

  private static Action<Bank> action(SerializableConsumer<Bank> consumer) {
    return Action.create(Bank.class, consumer);
  }

  public static Action<Bank> lendMoney() {
    return action(bank -> {
      // Process money paid back to bank
      double loanPaidBack = bank.getMessagesOfType(PayBackLoan.class).stream()
          .mapToDouble(m -> m.originalLoanRepayment).sum();
      double interestPaid = bank.getMessagesOfType(PayBackLoan.class).stream()
          .mapToDouble(m -> m.interestPaidBack).sum();

      bank.capitalToLend += loanPaidBack + interestPaid;
      bank.profitFromInterest += interestPaid;
      bank.moneyLent -= loanPaidBack;

      // Process borrow requests
      bank.getMessagesOfType(BorrowRequest.class).forEach(m ->
      {
        if (bank.capitalToLend <= 0) {
          bank.send(Messages.BorrowOutcome.class, (msg) -> msg.lendAmount = 0)
              .to(m.getSender());
        } else {
          double amountLent = Math.min(m.borrowAmount, bank.capitalToLend);
          bank.moneyLent += amountLent;
          bank.capitalToLend -= amountLent;

          bank.send(Messages.BorrowOutcome.class, (msg) -> msg.lendAmount = amountLent)
              .to(m.getSender());
        }
      });
    });
  }

}
