package swarmModel.traders;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.distribution.NormalDistribution;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;
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
  public double capital = 10000;

  @Variable
  public double shares = 0;

  @Variable
  public double putOptions = 0;

  @Variable
  public double callOptions = 0;

  @Variable
  public double portfolio = capital;

  public double sharesToSend = 0;
  protected final double initialMarketPrice = 15;
  private double optionOpinionThreshold = 0.6;
  public int optionExpiryTime;


  public List<Option> boughtOptions = new ArrayList<>();
  public List<Option> soldOptions = new ArrayList<>();

  @Override
  public void init() {
    optionExpiryTime = (int) Math.floor(getPrng().uniform(10, 25).sample());
  }

  private static Action<BaseTrader> action(SerializableConsumer<BaseTrader> consumer) {
    return Action.create(BaseTrader.class, consumer);
  }

  /* We allow buying when we have 0 capital since we are
     looking at overall portfolio value as an indicator of wealth */
  public void buy(double volume) {
    shares += volume;
    capital -= volume * getGlobals().marketPrice;
    buyValuesUpdate(volume);
    updatePortfolioValue();

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
      updatePortfolioValue();
    }
  }

  public void sellValuesUpdate(double volume) {
    getDoubleAccumulator("sells").add(volume);
    getLinks(TradeLink.class).send(SellOrderPlaced.class, (msg, link) -> msg.volume = volume);
  }

  public void updatePortfolioValue() {
    portfolio = shares * getGlobals().marketPrice + capital + calculateOptionPortfolioValue();
  }

  /******************* Short Selling ******************/

  // Trader borrows shares and sells them, creating a margin account
  public void shortStock(int volume) {
    shares -= volume;
    capital += volume * getGlobals().marketPrice;
    getDoubleAccumulator("shorts").add(volume);

    //Update sell order numbers
    sellValuesUpdate(volume);
    updatePortfolioValue();
  }

  public boolean hasShortPosition() {
    return shares < 0;
  }

  /******************* Options Trading ******************/

  // Each option is valid for 10 shares of the stock (used to simplify values instead of 100)
  public void buyPutOption(int expiryTime, double exercisePrice) {
    Option option = new Option(expiryTime, exercisePrice, type.PUT, getGlobals().marketPrice);
    option.setOptionPrice(calculateOptionPrice(option));
    getDoubleAccumulator("putOptionsBought").add(1);
    putOptions += 1;
    getLinks(Links.TradeLink.class)
        .send(Messages.PutOptionBought.class, (msg, link) -> msg.option = option);
    boughtOptions.add(option);
    capital -= option.getOptionPrice();
    updatePortfolioValue();
  }

  public void buyCallOption(int expiryTime, double exercisePrice) {
    Option option = new Option(expiryTime, exercisePrice, type.CALL, getGlobals().marketPrice);
    option.setOptionPrice(calculateOptionPrice(option));
    getDoubleAccumulator("callOptionsBought").add(1);
    callOptions += 1;
    getLinks(Links.TradeLink.class)
        .send(Messages.CallOptionBought.class, (msg, link) -> msg.option = option);
    boughtOptions.add(option);
    capital -= option.getOptionPrice();
    updatePortfolioValue();
  }

  // Update each Option on every time step
  public static Action<BaseTrader> updateOptions() {
    return action(trader -> {
      double total = 0;
      if (trader.getContext().getTick() > 0) {
        List<Option> expiredOptions = new ArrayList<>();
        for (Option option : trader.boughtOptions) {
          boolean expired = option.timeStep();
          if (expired) {
            double toSend = trader.actOnOption(option);
            total += toSend;
            expiredOptions.add(option);
            if (option.isCallOption()) {
              trader.callOptions--;
            } else {
              trader.putOptions--;
            }
          }
        }
        trader.boughtOptions.removeAll(expiredOptions);
      }
      trader.sharesToSend = total;
    });
  }

  public double actOnOption(Option option) {
    if (option.isCallOption() && getGlobals().marketPrice > option.getExercisePrice()) {
      capital +=
          (getGlobals().marketPrice - option.getExercisePrice()) * getGlobals().optionShareNumber;
      updatePortfolioValue();
      return getGlobals().optionShareNumber;
    } else if (!option.isCallOption() && getGlobals().marketPrice < option.getExercisePrice()) {
      capital +=
          (option.getExercisePrice() - getGlobals().marketPrice) * getGlobals().optionShareNumber;
      updatePortfolioValue();
      return -1 * getGlobals().optionShareNumber;
    }
    return 0;
  }

  public void sendShares() {
    if (sharesToSend == 0) {
      return;
    }
    if (sharesToSend > 0) {
      buyValuesUpdate(Math.abs(sharesToSend));
    } else {
      sellValuesUpdate(Math.abs(sharesToSend));
    }
    sharesToSend = 0;
  }

  public double calculateOptionPortfolioValue() {
    double value = 0;
    for (Option option : boughtOptions) {
      if (option.isCallOption()) {
        value += Math.max(
            (getGlobals().marketPrice - option.getExercisePrice()) * getGlobals().optionShareNumber,
            0);
      } else {
        value += Math.max(
            (option.getExercisePrice() - getGlobals().marketPrice) * getGlobals().optionShareNumber,
            0);
      }
    }
    for (Option option : soldOptions) {
      if (option.isCallOption()) {
        value -= Math.max(
            (getGlobals().marketPrice - option.getExercisePrice()) * getGlobals().optionShareNumber,
            0);
      } else {
        value -= Math.max(
            (option.getExercisePrice() - getGlobals().marketPrice) * getGlobals().optionShareNumber,
            0);
      }
    }
    return value;
  }

  public double calculateD1(Option option){
    double stockPrice = getGlobals().marketPrice;
    double exercisePrice = option.getExercisePrice();
    double r = getGlobals().interestRate;
    double timeToExpiry = option.getTimeToExpiry();
    // Time to expiry is represented in years for these calculations, each timeStep = 1 day
    timeToExpiry = timeToExpiry / 365;
    return (1 / (getGlobals().volatility * Math.sqrt(timeToExpiry))) * (
        Math.log(stockPrice / exercisePrice)
            + (r + Math.pow(getGlobals().volatility, 2)) * timeToExpiry);
  }

  public double calculateOptionPrice(Option option) {
    /* Black Scholes Equation: Cost = Stock price * N(d1) - Exercise price * e^(-interestRate * timeToExpiry) * N(d2)
       where N(d1) and N(d2) are cumulative distribution functions for the normal distribution */
    double stockPrice = getGlobals().marketPrice;
    double exercisePrice = option.getExercisePrice();
    double r = getGlobals().interestRate;
    double timeToExpiry = option.getTimeToExpiry();
    // Time to expiry is represented in years for these calculations, each timeStep = 1 day
    timeToExpiry = timeToExpiry / 365;
    double d1 = calculateD1(option);
    double d2 = d1 - getGlobals().volatility * timeToExpiry;
    if (option.isCallOption()) {
      double optionPrice = (stockPrice * getNormalDistribution(d1)
          - exercisePrice * Math.exp(-r * timeToExpiry)
          * getNormalDistribution(d2)) * getGlobals().optionShareNumber;
      if (optionPrice > 0) {
        return optionPrice;
      }
      return 0;
    } else {
      double optionPrice = (getNormalDistribution(-d2) * exercisePrice
          * Math.exp(-r * timeToExpiry)
          - getNormalDistribution(-d1) * stockPrice) * getGlobals().optionShareNumber;
      if (optionPrice > 0) {
        return optionPrice;
      }
      return 0;
    }
  }

  private double getNormalDistribution(double d) {
    NormalDistribution normalDistribution = new NormalDistribution();
    return normalDistribution.cumulativeProbability(d);
  }

  /*********************** Hedging  ***********************/

  public double calcualteDelta(Option option) {
    double delta = 0;
    double currentOptionPrice = calculateOptionPrice(option);
    double initialOptionPrice = option.getOptionPrice();

    delta = (currentOptionPrice - initialOptionPrice) / (getGlobals().marketPrice / option
        .getInitialStockPrice() * getGlobals().optionShareNumber);
    double d1 = calculateD1(option);
    double alternativeDelta = Math.exp(-option.getTimeToExpiry()) * getNormalDistribution(d1);
    //todo: test this out see if they give the same values for call options
    // check that absolute value of call and put delta sum approx to 1
    // remember delta is for 10 shares, can multiply by 10 here or in deltahedge()
    return delta;
  }

  public void deltaHedge() {
    // Calculates the total delta of the portfolio
    double totalDelta = Math.round(boughtOptions.stream().mapToDouble(this::calcualteDelta).sum());
    if (totalDelta == 0) {
      return;
    } else if (totalDelta > 0) {
      shortStock((int) totalDelta);
    } else {
      buy((int) totalDelta);
    }
  }


  /******************* Opinion Dynamics ******************/

  // Uses an exponential model based on trader aggressiveness
  protected void tradeOnOpinion(double generalOpinion, double sensitivity) {
    double scaledOpinion = Math.abs(generalOpinion / 20);
    double sensitiveOpinion =
        (Math.exp(scaledOpinion * sensitivity) - 1) / (Math.exp(sensitivity) - 1);
    double sharesTraded = Math.floor(sensitiveOpinion * getGlobals().maxSharesTradedOnOpinion);

    if (generalOpinion > 0) {
      buy(sharesTraded);
      if (sensitiveOpinion > optionOpinionThreshold) {
        buyCallOption(optionExpiryTime, getGlobals().marketPrice * getGlobals().callStrikeFactor);
      }
    } else {
      sell(sharesTraded);
      if (sensitiveOpinion > optionOpinionThreshold) {
        buyPutOption(optionExpiryTime, getGlobals().marketPrice * getGlobals().putStrikeFactor);
      }
    }
  }
}
