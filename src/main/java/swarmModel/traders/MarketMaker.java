package swarmModel.traders;

import java.util.Map.Entry;
import simudyne.core.abm.Action;
import simudyne.core.functions.SerializableConsumer;
import swarmModel.utils.Option;

/* On each time step the market maker updates the current buy and sell price based on
 *  the demand in the market. Traders can then buy or sell some number of assets.
 *  Orders influence the asset's price (and the market makers behaviour when
 *  options are added later and the market maker needs to cover their position)
 *  Pricing algorithm reference: https://econweb.ucsd.edu/~rstarr/Shen-StarrMktMaker.pdf */

public class MarketMaker extends BaseTrader {

  private static final double maxThreshold = 0.05;

  private static final double nbStepsPrediction = 5;

  //Helper function for ease of interpretation
  private static Action<MarketMaker> action(SerializableConsumer<MarketMaker> consumer) {
    return Action.create(MarketMaker.class, consumer);
  }

  public static Action<MarketMaker> processInformation() {
    return action(marketMaker -> {
      double predictNetDemand = marketMaker.predictNetDemand();
      if (Math.abs(predictNetDemand / marketMaker.predictTotalDemand()) > maxThreshold) {
        if (predictNetDemand > 0) {
          marketMaker.sell(Math.round(predictNetDemand * 0.5));
        } else {
          marketMaker.buy(Math.round((-predictNetDemand) * 0.5));
        }
      }
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

  //todo: refine this to use derivatives
  private double predictNetDemand() {
    if (getContext().getTick() == 0) {
      return 1000000;
    } else if (getContext().getTick() < nbStepsPrediction) {
      return getGlobals().pastNetDemand.entrySet().stream().mapToDouble(Entry::getValue).sum()
          / getContext().getTick();
    }
    double demandTotal = getGlobals().pastNetDemand.entrySet().stream()
        .filter(a -> a.getKey() >= getContext().getTick() - nbStepsPrediction)
        .mapToDouble(Entry::getValue).sum();
    return demandTotal / nbStepsPrediction;
  }

  public static Action<MarketMaker> updateMarketData() {
    return action(marketMaker -> {
    });
  }

  private long getNumberOfTraders() {
    return getGlobals().nbFundamentalTraders + getGlobals().nbNoiseTraders
        + getGlobals().nbMomentumTraders;
  }

  /***************** Option Pricing ****************/

  public void sellOption(Option option) {
    getOptions().add(option);
  }

  public void coverPosition(Option option) {

  }

}
