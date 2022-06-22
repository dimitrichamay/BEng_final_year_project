package swarmModel.traders;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.distribution.NormalDistribution;
import simudyne.core.abm.Action;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;
import swarmModel.links.Links;
import swarmModel.links.Messages;
import swarmModel.utils.Option;
import swarmModel.utils.Option.type;

public class OptionTrader extends BaseTrader {

  @Variable
  public double hedgePosition = 0;

  @Variable
  public double putOptions = 0;

  @Variable
  public double callOptions = 0;

  private final double optionOpinionThreshold = 0.2;
  public int optionExpiryTime;
  public double sharesToSell = 0;
  public double sharesToBuy = 0;
  private double hedgeProportion;

  public List<Option> boughtOptions = new ArrayList<>();

  @Override
  public void init() {
    super.init();
    optionExpiryTime = (int) Math.floor(getPrng().uniform(10, 25).sample());
    hedgeProportion = getPrng().uniform(0.4, 1).sample();
  }

  private static Action<OptionTrader> action(SerializableConsumer<OptionTrader> consumer) {
    return Action.create(OptionTrader.class, consumer);
  }

  @Override
  public void updatePortfolioValue() {
    portfolio = shares * getGlobals().marketPrice + capital + calculateOptionPortfolioValue();
  }

  /******************* Options Trading ******************/

  // Each option is valid for 10 shares of the stock (used to simplify values instead of 100)
  public void buyPutOption(int expiryTime, double exercisePrice) {
    Option option = new Option(expiryTime, exercisePrice, type.PUT, getGlobals().marketPrice);
    option.setOptionPrice(calculateOptionPrice(option));
    putValuesUpdate(option);
  }

  public void putValuesUpdate(Option option) {
    getDoubleAccumulator("putOptionsBought").add(1);
    putOptions += 1;
    getLinks(Links.TradeLink.class)
        .send(Messages.PutOptionBought.class, (msg, link) -> msg.option = option);
    boughtOptions.add(option);
    capital -= option.getOptionPrice();
  }

  public void buyCallOption(int expiryTime, double exercisePrice) {
    Option option = new Option(expiryTime, exercisePrice, type.CALL, getGlobals().marketPrice);
    option.setOptionPrice(calculateOptionPrice(option));
    callValuesUpdate(option);
  }

  public void callValuesUpdate(Option option) {
    getDoubleAccumulator("callOptionsBought").add(1);
    callOptions += 1;
    getLinks(Links.TradeLink.class)
        .send(Messages.CallOptionBought.class, (msg, link) -> msg.option = option);
    boughtOptions.add(option);
    capital -= option.getOptionPrice();
  }

  // Update each Option on every time step
  public static Action<OptionTrader> updateOptions() {
    return action(trader -> {
      if (trader.getContext().getTick() > 0) {
        List<Option> expiredOptions = new ArrayList<>();
        for (Option option : trader.boughtOptions) {
          boolean expired = option.timeStep();
          if (expired) {
            double toSend = trader.actOnOption(option);
            if (toSend >= 0) {
              trader.sharesToBuy += toSend;
            } else {
              trader.sharesToSell += Math.abs(toSend);
            }
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
    });
  }

  public double actOnOption(Option option) {
    if (option.isCallOption() && getGlobals().marketPrice > option.getExercisePrice()) {
      capital +=
          (getGlobals().marketPrice - option.getExercisePrice()) * getGlobals().optionShareNumber;
      return getGlobals().optionShareNumber;
    } else if (!option.isCallOption() && getGlobals().marketPrice < option.getExercisePrice()) {
      capital +=
          (option.getExercisePrice() - getGlobals().marketPrice) * getGlobals().optionShareNumber;
      return -1 * getGlobals().optionShareNumber;
    }
    return 0;
  }

  public void sendShares() {
    if (sharesToSell > 0) {
      shares -= sharesToSell;
      sellValuesUpdate(sharesToSell);
    }
    if (sharesToBuy > 0) {
      shares += sharesToBuy;
      buyValuesUpdate(sharesToBuy);
    }
    sharesToBuy = 0;
    sharesToSell = 0;
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

  public double calculateD1(Option option) {
    double stockPrice = getGlobals().marketPrice;
    double exercisePrice = option.getExercisePrice();
    double r = getGlobals().interestRate;
    double timeToExpiry = option.getTimeToExpiry();
    // Time to expiry is represented in years for these calculations, each timeStep = 1 day
    timeToExpiry = timeToExpiry / 365;
    return (1 / (getGlobals().volatility * Math.sqrt(timeToExpiry))) * (
        Math.log(stockPrice / exercisePrice)
            + (r + Math.pow(getGlobals().volatility, 2) / 2) * timeToExpiry);
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
    // We do not want to update the hedge if the option is close to expiry
    if (option.getTimeToExpiry() < 5){
      return 0;
    }

    double currentOptionPrice = calculateOptionPrice(option);
    double initialOptionPrice = option.getOptionPrice();

    // This is the value of delta for 100 shares since this is what an option represents
    delta = ((currentOptionPrice - initialOptionPrice) / (getGlobals().marketPrice - option
        .getInitialStockPrice()));

    // The value of delta for each individual option contract is between -1 and 1
    if (option.isCallOption()){
      if (delta > getGlobals().optionShareNumber){
        delta = getGlobals().optionShareNumber;
      }
      if (delta < 0){
        delta = 0;
      }
    } else {
      if (delta < - getGlobals().optionShareNumber){
        delta = - getGlobals().optionShareNumber;
      }
      if (delta > 0){
        delta = 0;
      }
    }
    return delta;
  }

  public void deltaHedge() {
    // Calculates the total delta of the portfolio
    double totalDelta = Math.round(boughtOptions.stream().mapToDouble(this::calcualteDelta).sum());
    totalDelta *= hedgeProportion;
    double changeInHedge = 0;
    double absoluteHedge = Math.abs(hedgePosition);
    double absoluteDelta = Math.abs(totalDelta);
    if (totalDelta == 0) {
      return;
    } else if (totalDelta >= 0) {
      // Hedge position is positive so we need to short even more of the stock
      if (hedgePosition >= 0) {
        changeInHedge = -(absoluteDelta + absoluteHedge);
        sell(Math.abs(changeInHedge));
      }
      // Hedge is already negative so less of the stock is shorted/some is bought back
      else if (hedgePosition < 0) {
        // Current hedge position less than required so short
        if (absoluteHedge < absoluteDelta) {
          changeInHedge = -(absoluteDelta - absoluteHedge);
          sell(Math.abs(changeInHedge));
        }
        // Current hedge position more than required so buy back stock
        else if (absoluteHedge > absoluteDelta) {
          changeInHedge = (absoluteHedge - absoluteDelta);
          buy(Math.abs(changeInHedge));
        }
      }
    } else {
      // Hedge position negative so need to buy even more of the stock
      if (hedgePosition <= 0) {
        changeInHedge = (absoluteHedge + absoluteDelta);
        buy(Math.abs(changeInHedge));
      }
      // Hedge position already positive so less of the stock is bought/some is shorted
      else if (hedgePosition > 0) {
        // Current hedge position more than required so sell stock
        if (absoluteHedge > absoluteDelta) {
          changeInHedge = -(absoluteHedge - absoluteDelta);
          sell(Math.abs(changeInHedge));
        }
        // Current hedge position less than required so buy stock
        else if (absoluteHedge < absoluteDelta) {
          changeInHedge = (absoluteDelta - absoluteHedge);
          buy(Math.abs(changeInHedge));
        }
      }
    }
    hedgePosition += changeInHedge;
  }


  /******************* Opinion Dynamics ******************/

  // Uses an exponential model based on trader aggressiveness
  protected void tradeOnOpinion(double generalOpinion, double sensitivity) {
    double scaledOpinion = Math.abs(generalOpinion / 20);
    double sensitiveOpinion =
        (Math.exp(scaledOpinion * 10 * sensitivity) - 1) / (Math.exp(10 * sensitivity) - 1);
    if (sensitiveOpinion <= 0) {
      sensitiveOpinion = 0;
    } else if (sensitiveOpinion > 1) {
      sensitiveOpinion = 1;
    }
    double sharesTraded = Math.floor(sensitiveOpinion * getGlobals().maxSharesTradedOnOpinion);

    if (generalOpinion > 0) {
      buy(Math.abs(sharesTraded));
      if (sensitiveOpinion > optionOpinionThreshold) {
        buyCallOption(optionExpiryTime, getGlobals().marketPrice * getGlobals().callStrikeFactor);
      }
    } else {
      sell(Math.abs(sharesTraded));
      if (sensitiveOpinion > optionOpinionThreshold) {
        buyPutOption(optionExpiryTime, getGlobals().marketPrice * getGlobals().putStrikeFactor);
      }
    }
  }
}
