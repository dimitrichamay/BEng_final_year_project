package swarmModel.traders;

import java.util.Map.Entry;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import simudyne.core.abm.Action;
import simudyne.core.functions.SerializableConsumer;
import swarmModel.links.Messages;
import swarmModel.utils.Option;

/* On each time step the market maker updates the current buy and sell price based on
 *  the demand in the market. Traders can then buy or sell some number of assets.
 *  Orders influence the asset's price (and the market makers behaviour when
 *  options are added later and the market maker needs to cover their position)
 *  Pricing algorithm reference: https://econweb.ucsd.edu/~rstarr/Shen-StarrMktMaker.pdf */

public class MarketMaker extends BaseTrader {

  private static final double maxThreshold = 0.05;

  private static final double nbStepsPrediction = 5;

  private int sharesToBuy = 0;
  private int sharesToSell = 0;

  //Helper function for ease of interpretation
  private static Action<MarketMaker> action(SerializableConsumer<MarketMaker> consumer) {
    return Action.create(MarketMaker.class, consumer);
  }

  public static Action<MarketMaker> processInformation() {
    return action(marketMaker -> {
      double predictNetDemand = marketMaker.predictNetDemand();
      if (Math.abs(predictNetDemand / marketMaker.predictTotalDemand()) > maxThreshold) {
        if (predictNetDemand > 0) {
          marketMaker.sell(Math.round(predictNetDemand * 0.05));
        } else {
          marketMaker.buy(Math.round((-predictNetDemand) * 0.05));
        }
      }
      //todo: fix these since not covering position
      marketMaker.sell(marketMaker.sharesToSell);
      marketMaker.buy(marketMaker.sharesToBuy);
      marketMaker.sharesToBuy = 0;
      marketMaker.sharesToSell = 0;
    });
  }

  public static Action<MarketMaker> processOptionSales() {
    return action(marketMaker -> {
      marketMaker.sellCallOptions();
      marketMaker.sellPutOptions();
    });
  }

  private double predictTotalDemand() {
    if (getContext().getTick() == 0) {
      return 1000000; //Return large integer to prevent trading before we have information
    } else if (getContext().getTick() < nbStepsPrediction) {
      return getGlobals().pastTotalDemand.entrySet().stream().mapToDouble(Entry::getValue).sum()
          / getContext().getTick();
    }
    double demandPrediction = getGlobals().pastTotalDemand.entrySet().stream()
        .filter(a -> a.getKey() >= getContext().getTick() - nbStepsPrediction)
        .mapToDouble(Entry::getValue).sum();
    if (demandPrediction == 0) {
      return 1000000; //Return large integer to prevent trading before we have information
    }
    return demandPrediction / nbStepsPrediction;
  }

  // Predicts the net demand at the current time step using a polynomial fitted to the last 10 points
  private double predictNetDemand() {
    if (getContext().getTick() <= getGlobals().derivativeTimeFrame) {
      return 0;
    }
    return new PolynomialFunction(getGlobals().coeffs)
        .value(getContext().getTick());
  }

  public static Action<MarketMaker> updateMarketData() {
    return action(marketMaker -> {
    });
  }

  private long getNumberOfTraders() {
    return getGlobals().nbFundamentalTraders + getGlobals().nbNoiseTraders
        + getGlobals().nbMomentumTraders;
  }

  /*********** OPTION SELLING **********/

  public void sellPutOptions() {
    getMessagesOfType(Messages.PutOptionBought.class).stream().forEach(putOptionBought -> {
      Option option = putOptionBought.option;
      soldOptions.add(option);
      capital += option.getOptionPrice();
      //Todo: can change covering option every single time
      sharesToSell += 100;
    });
  }

  public void sellCallOptions() {
    getMessagesOfType(Messages.CallOptionBought.class).stream().forEach(callOptionBought -> {
      Option option = callOptionBought.option;
      soldOptions.add(option);
      capital += option.getOptionPrice();
      //Todo: can change covering option every single time
      sharesToBuy += 100;
    });
  }

}
