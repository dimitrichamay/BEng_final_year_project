package swarmModel.traders;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.distribution.NormalDistribution;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import swarmModel.Globals;
import swarmModel.links.Links;
import swarmModel.links.Links.TradeLink;
import swarmModel.links.Messages;
import swarmModel.links.Messages.BuyOrderPlaced;
import swarmModel.links.Messages.SellOrderPlaced;
import swarmModel.utils.Option;
import swarmModel.utils.Option.type;

public abstract class BaseTrader extends Agent<Globals> {

  @Variable
  public double capital = 100;

  @Variable
  public double shares = 0;

  @Variable
  public double putOptions = 0;

  @Variable
  public double callOptions = 0;

  //todo: update this 4 to be the initial price in the model
  @Variable
  public double portfolio = shares * 4 + capital;

  private double minCapitalToShort = 1;

  public List<Option> boughtOptions = new ArrayList<>();
  public List<Option> soldOptions = new ArrayList<>();

  public void buy(double volume) {
    if (capital > volume * getGlobals().marketPrice) {
      shares += volume;
      capital -= volume * getGlobals().marketPrice;
      buyValuesUpdate(volume);
      updatePortfolioValue();
    }
  }

  public void buyValuesUpdate(double volume) {
    getDoubleAccumulator("buys").add(volume);
    getLinks(TradeLink.class).send(BuyOrderPlaced.class);
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
      updatePortfolioValue();
    }
  }

  public void sellValuesUpdate(double volume) {
    getDoubleAccumulator("sells").add(volume);
    getLinks(TradeLink.class).send(SellOrderPlaced.class);
  }

  public void updatePortfolioValue() {
    portfolio = shares * getGlobals().marketPrice + capital + calculateOptionPortfolioValue();
  }

  /******************* Short Selling ******************/

  // Trader borrows shares and sells them, creating a margin account
  public void shortStock(int volume) {
    if (canAffordToShortStock(volume)) {
      shares -= volume;
      capital += volume * getGlobals().marketPrice;
      getDoubleAccumulator("shorts").add(volume);

      //Update sell order numbers
      sellValuesUpdate(volume);
      updatePortfolioValue();
    }
  }

  //todo: check whether this affects option covering!
  protected boolean canAffordToShortStock(int volume) {
    if (shares == 0) {
      return capital >= minCapitalToShort * (volume * getGlobals().marketPrice);
    } else {
      // Shares < 0 since cannot short sell if have shares
      return capital >= minCapitalToShort * (Math.abs(shares) + volume) * getGlobals().marketPrice;
    }
  }

  protected boolean hasShortPositions() {
    return shares < 0;
  }

  /******************* Options Trading ******************/

  // Each option is valid for 100 shares of the stock
  public void buyPutOption(int expiryTime, double exercisePrice) {
    Option option = new Option(expiryTime, exercisePrice, type.PUT);
    initiateOptionPrice(option);
    getDoubleAccumulator("putOptionsBought").add(1);
    putOptions += 1;
    getLinks(Links.TradeLink.class)
        .send(Messages.PutOptionBought.class, (msg, link) -> msg.option = option);
    boughtOptions.add(option);
    capital -= option.getOptionPrice();
    updatePortfolioValue();
  }

  public void buyCallOption(int expiryTime, double exercisePrice) {
    Option option = new Option(expiryTime, exercisePrice, type.CALL);
    initiateOptionPrice(option);
    getDoubleAccumulator("callOptionsBought").add(1);
    callOptions += 1;
    getLinks(Links.TradeLink.class)
        .send(Messages.CallOptionBought.class, (msg, link) -> msg.option = option);
    boughtOptions.add(option);
    capital -= option.getOptionPrice();
    updatePortfolioValue();
  }

  public double actOnOption(Option option) {
    if (option.isCallOption() && getGlobals().marketPrice > option.getExercisePrice()) {
      capital += (getGlobals().marketPrice - option.getExercisePrice()) * 100;
      updatePortfolioValue();
      return 100;
    }
    return 0;
  }

  //Todo: account for time decay in option valuation
  public double calculateOptionPortfolioValue() {
    double value = 0;
    for (Option option : boughtOptions) {
      if (option.isCallOption()) {
        value += Math.max(
            (getGlobals().marketPrice - option.getExercisePrice()) * 100,
            0);
      } else {
        value += Math.max(
            (option.getExercisePrice() - getGlobals().marketPrice) * 100,
            0);
      }
    }
    for (Option option : soldOptions) {
      if (option.isCallOption()) {
        value -= Math.max(
            (getGlobals().marketPrice - option.getExercisePrice()) * 100,
            0);
      } else {
        value -= Math.max(
            (option.getExercisePrice() - getGlobals().marketPrice) * 100,
            0);
      }
    }
    return value;
  }

  public List<Option> getBoughtOptions() {
    return boughtOptions;
  }

  public List<Option> getSoldOptions() {
    return soldOptions;
  }

  public void initiateOptionPrice(Option option) {
    /* Black Scholes Equation: Cost = Stock price * N(d1) - Exercise price * e^(-interestRate * timeToExpiry) * N(d2)
       where N(d1) and N(d2) are cumulative distribution functions for the normal distribution */
    double stockPrice = getGlobals().marketPrice;
    double exercisePrice = option.getExercisePrice();
    double r = getGlobals().interestRate;
    double timeToExpiry = option.getTimeToExpiry();
    // Time to expiry is represented in years for these calculations, each timeStep = 1 day
    //todo: check that steps are 1 month long
    timeToExpiry = timeToExpiry / 12;
    double d1 = (1 / (getGlobals().volatility * Math.sqrt(timeToExpiry))) * (
        Math.log(stockPrice / exercisePrice)
            + (r + Math.pow(getGlobals().volatility, 2)) * timeToExpiry);
    double d2 = d1 - getGlobals().volatility * timeToExpiry;
    if (option.isCallOption()) {
      option.setOptionPrice(stockPrice * getNormalDistribution(d1)
          - exercisePrice * Math.exp(-r * timeToExpiry)
          * getNormalDistribution(d2));
    } else {
      /*option.setOptionPrice(getNormalDistribution(-d2) * exercisePrice
          * Math.exp(-r * timeToExpiry)
          - getNormalDistribution(-d1) * stockPrice);*/
      option.setOptionPrice(stockPrice * getNormalDistribution(d1)
          - exercisePrice * Math.exp(-r * timeToExpiry)
          * getNormalDistribution(d2));
    }
  }

  private double getNormalDistribution(double d) {
    NormalDistribution normalDistribution = new NormalDistribution();
    return normalDistribution.cumulativeProbability(d);
  }
}
