package swarmModel.traders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;
import swarmModel.Globals;
import swarmModel.links.Links.TradeLink;
import swarmModel.links.Messages.BuyOrderPlaced;
import swarmModel.links.Messages.SellOrderPlaced;
import swarmModel.utils.Option;

public abstract class BaseTrader extends Agent<Globals> {

  @Variable
  public double capital = 0;

  @Variable
  public double shares = 0;

  @Variable
  public double portfolio = capital;

  protected final double initialMarketPrice = 15;
  private static final double nbBackStepsPrediction = 5;

  public List<Option> soldOptions = new ArrayList<>();

  @Override
  public void init() {
    capital = 10000;
    super.init();
  }

  private static Action<BaseTrader> action(SerializableConsumer<BaseTrader> consumer) {
    return Action.create(BaseTrader.class, consumer);
  }

  public static Action<BaseTrader> updatePortfolioValues() {
    return action(trader -> {
      trader.updateCapitalForInterest();
      trader.updatePortfolioValue();
    });
  }

  /* We allow buying when we have 0 capital since we are
     looking at overall portfolio value as an indicator of wealth */
  public void buy(double volume) {
    shares += volume;
    capital -= volume * getGlobals().marketPrice;
    buyValuesUpdate(volume);
  }

  public void buyValuesUpdate(double volume) {
    getDoubleAccumulator("buys").add(volume);
    getLinks(TradeLink.class).send(BuyOrderPlaced.class, (msg, link) -> {
      msg.volume = volume;
    });
  }

  public void sell(double volume) {
    double toSell = volume;
    //If doesn't have enough shares short sell
    if (shares <= 0) {
      shortStock((int) volume);
      toSell = 0;
    } else if (shares > 0 && shares < volume) {
      toSell -= shares;
      shortStock((int) (volume - shares));
    }
    // If still has shares sell these
    if (toSell > 0) {
      shares -= toSell;
      capital += toSell * getGlobals().marketPrice;
      sellValuesUpdate(toSell);
    }
  }

  public void sellValuesUpdate(double volume) {
    getDoubleAccumulator("sells").add(volume);
    getLinks(TradeLink.class).send(SellOrderPlaced.class, (msg, link) -> msg.volume = volume);
  }

  public void updatePortfolioValue() {
    portfolio = shares * getGlobals().marketPrice + capital;
  }

  public void updateCapitalForInterest() {
    double dailyInterest = getGlobals().interestRate / 365;
    capital *= (1 + dailyInterest);
  }

  /******************* Short Selling ******************/

  // Trader borrows shares and sells them, creating a margin account
  public void shortStock(int volume) {
    shares -= volume;
    capital += volume * getGlobals().marketPrice;
    getDoubleAccumulator("shorts").add(volume);

    //Update sell order numbers
    sellValuesUpdate(volume);
  }

  public boolean hasShortPosition() {
    return shares < 0;
  }

  /********* Borrowing and Price predictions **********/

  protected double predictTotalDemand() {
    if (getContext().getTick() == 0) {
      return 0;
    } else if (getContext().getTick() < nbBackStepsPrediction) {
      return getGlobals().pastTotalDemand.entrySet().stream().mapToDouble(Entry::getValue).sum()
          / getContext().getTick();
    }
    double demandPrediction = getGlobals().pastTotalDemand.entrySet().stream()
        .filter(a -> a.getKey() >= getContext().getTick() - nbBackStepsPrediction)
        .mapToDouble(Entry::getValue).sum();
    return demandPrediction / nbBackStepsPrediction;
  }

  // Predicts the net demand at the current time step using a polynomial fitted to the last 10 points
  public double predictNetDemand(double tickOffset) {
    if (getContext().getTick() <= getGlobals().derivativeTimeFrame) {
      return 0;
    }
    return new PolynomialFunction(getGlobals().coeffs)
        .value(getContext().getTick() + tickOffset);
  }

  /* If the net demand is expected to be > 0, from the price dynamics we
     therefore also expect the price to increase */
  protected boolean priceIncreasePredicted() {
    return predictNetDemand(0) > 0;
  }

}
