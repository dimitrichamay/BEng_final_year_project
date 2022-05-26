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

  private static final double maxThreshold = 0.5;


  private final double priceToStartCoverPos = initialMarketPrice * 1.5;
  private final double priceToCoverHalfPos = initialMarketPrice * 3;
  private final double priceToCoverPos = initialMarketPrice * 5;
  private final double compensationFactor = 0.01;

  private int sharesToBuy = 0;
  private int sharesToSell = 0;

  //Helper function for ease of interpretation
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
              .round(Math.abs(predictNetDemand) * marketMaker.compensationFactor);

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

      marketMaker.coverShortIfNecessary();
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

  private void coverShortIfNecessary() {
    if (shares < 0) {
      if (getGlobals().marketPrice > priceToCoverPos && priceIncreasePredicted()) {
        coverPosition(1);
      } else if (getGlobals().marketPrice > priceToCoverHalfPos && priceIncreasePredicted()) {
        coverPosition(0.5);
      } else if (getGlobals().marketPrice > priceToStartCoverPos && priceIncreasePredicted()) {
        coverPosition(0.25);
      }
    }
  }

  private void coverPosition(double proportionToCover) {
    buy(Math.abs(shares) * proportionToCover);
  }
}
