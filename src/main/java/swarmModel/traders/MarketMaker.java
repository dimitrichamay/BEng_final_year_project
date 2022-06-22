package swarmModel.traders;

import simudyne.core.abm.Action;
import simudyne.core.functions.SerializableConsumer;
import swarmModel.links.Messages;
import swarmModel.utils.Option;

public class MarketMaker extends BaseTrader {

  private static final double maxThreshold = 0.5;

  private final double compensationFactor = 0.001;

  private int sharesToBuy = 0;
  private int sharesToSell = 0;

  // Helper function for ease of interpretation
  private static Action<MarketMaker> action(SerializableConsumer<MarketMaker> consumer) {
    return Action.create(MarketMaker.class, consumer);
  }

  public static Action<MarketMaker> processInformation() {
    return action(marketMaker -> {
      double predictNetDemand = marketMaker.predictNetDemand(0);
      double predictTotalDemand = marketMaker.predictTotalDemand();
      if (predictTotalDemand > 0) {

        // Adds liquidity on other side of the market if large disparity in demand
        if (Math.abs(predictNetDemand / predictTotalDemand) > maxThreshold) {
          long compensation = Math
              .round(Math.min(Math.abs(predictNetDemand), marketMaker.getNumberOfTraders()) * marketMaker.compensationFactor);
          if (predictNetDemand > 0) {
            marketMaker.sell(compensation);
          } else {
            marketMaker.buy(compensation);
          }
        }
      }

      marketMaker.sellCallOptions();
      marketMaker.sellPutOptions();

      marketMaker.sell(marketMaker.sharesToSell);
      marketMaker.buy(marketMaker.sharesToBuy);

      marketMaker.sharesToBuy = 0;
      marketMaker.sharesToSell = 0;

    });
  }


  /*********** OPTION SELLING **********/

  public void sellPutOptions() {
    getMessagesOfType(Messages.PutOptionBought.class).stream().forEach(putOptionBought -> {
      Option option = putOptionBought.option;
      soldOptions.add(option);
      capital += option.getOptionPrice();
      sharesToSell += getGlobals().optionShareNumber;
    });
  }

  public void sellCallOptions() {
    getMessagesOfType(Messages.CallOptionBought.class).stream().forEach(callOptionBought -> {
      Option option = callOptionBought.option;
      soldOptions.add(option);
      capital += option.getOptionPrice();
      sharesToBuy += getGlobals().optionShareNumber;
    });
  }

  public long getNumberOfTraders() {
    return getGlobals().nbFundamentalTraders + getGlobals().nbNoiseTraders
        + getGlobals().nbMomentumTraders + getGlobals().nbHedgeFunds
        + getGlobals().nbRetailInvestors;
  }

}
