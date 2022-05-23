package swarmModel.traders;

import simudyne.core.abm.Action;
import simudyne.core.abm.Section;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;
import swarmModel.Exchange;
import swarmModel.links.Links;
import swarmModel.links.Messages;
import swarmModel.utils.Option;

public abstract class Borrower extends OptionTrader {

  @Variable
  public double amountBorrowed = 0;

  private final double forwardProjection = 5;

  @Override
  public void init() {
    capital = 500;
  }

  @Override
  public void updatePortfolioValue() {
    portfolio = shares * getGlobals().marketPrice + capital + calculateOptionPortfolioValue();
  }

  private static Action<Borrower> action(SerializableConsumer<Borrower> consumer) {
    return Action.create(Borrower.class, consumer);
  }

  public static Action<Borrower> processBorrowing() {
    return action(trader -> {
      // If we do have negative capital after our trading activity, borrow money
      if (trader.capital < 0) {
        trader.getLinks(Links.BorrowLink.class).send(Messages.BorrowRequest.class, (msg, link) -> {
          msg.borrowAmount = Math.abs(trader.capital);
        });
      }

      // If have spare capital, pay back as much as we can
      else if (trader.amountBorrowed > 0) {
        final double toPayBack = Math.min(trader.amountBorrowed, trader.capital);
        trader.getLinks(Links.BorrowLink.class).send(Messages.PayBackLoan.class, (msg, link) -> {
          msg.amountToPayBack = toPayBack;
        });
        trader.capital -= toPayBack;
        trader.amountBorrowed -= toPayBack;
      }
    });
  }

  public static Action<Borrower> actOnLoan() {
    return action(trader -> {
      double loan = trader.getMessagesOfType(Messages.BorrowOutcome.class).stream()
          .mapToDouble(m -> m.lendAmount)
          .sum();

      // Update amount borrowed to account for interest
      trader.amountBorrowed *= ((1 + trader.getLendingRate()) / 365);

      trader.amountBorrowed += loan;
      trader.capital += loan;
    });
  }

  @Override
  public void putValuesUpdate(Option option) {
    //todo
    //IF option value greater than predicted price....
    super.putValuesUpdate(option);
  }

  @Override
  public void callValuesUpdate(Option option) {
    //todo
    super.callValuesUpdate(option);
  }

  @Override
  public void buy(double volume) {
    //todo: if better to just keep money in the bank due to IR's do that (if capital >0..)
    super.buy(volume);
  }

  public double calculateProjectedPriceChange(double t){
    // Net Demand Prediction in t steps time
    double netDemand = predictNetDemand(t);
    double priceChangePrediction = (netDemand / getNumberOfTraders()) /getGlobals().lambda;
    return priceChangePrediction;
  }

  private double getLendingRate() {
    return getGlobals().interestRate + getGlobals().interestMargin;
  }

  private long getNumberOfTraders(){
    return getGlobals().nbFundamentalTraders + getGlobals().nbNoiseTraders
        + getGlobals().nbMomentumTraders + getGlobals().nbHedgeFunds
        + getGlobals().nbRetailInvestors;
  }

}
