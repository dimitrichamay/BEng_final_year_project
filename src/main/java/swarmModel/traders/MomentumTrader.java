package swarmModel.traders;

import java.util.Map;
import simudyne.core.abm.Action;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;
import swarmModel.links.Messages;


/*
   This agent is an example implementation of a moving average trading strategy
*/

public class MomentumTrader extends OptionTrader {

  @Variable(name = "Long Term Moving Average")
  public double longTermMovingAvg;

  @Variable(name = "Short Term Moving Average")
  public double shortTermMovingAvg;

  @Variable(name = "General Opinion")
  public double opinion = 0;

  // Helper function for ease of interpretation
  private static Action<MomentumTrader> action(SerializableConsumer<MomentumTrader> consumer) {
    return Action.create(MomentumTrader.class, consumer);
  }

  public static Action<MomentumTrader> updateOpinion(){
    return action(trader -> {
      trader.opinion = trader.getMessagesOfType(Messages.OpinionShared.class).stream()
          .mapToDouble(opinion -> opinion.opinion).average().orElse(0);
    });
  }

  public static Action<MomentumTrader> processInformation() {
    return action(
        trader -> {
          double probToBuy = trader.getPrng().uniform(0, 1).sample();
          if (trader.getContext().getTick() > trader.getGlobals().longTermAveragePeriod) {
            trader.longTermMovingAvg = trader
                .getTermMovingAvg(trader.getGlobals().longTermAveragePeriod,
                    trader.getGlobals().historicalPrices);
            trader.shortTermMovingAvg = trader
                .getTermMovingAvg(trader.getGlobals().shortTermAveragePeriod,
                    trader.getGlobals().historicalPrices);

            if (trader.shortTermMovingAvg > trader.longTermMovingAvg && probToBuy < trader
                .getGlobals().traderActivity) {
              trader.buy(trader.getGlobals().stdVolume);
            } else if ((trader.shortTermMovingAvg < trader.longTermMovingAvg && probToBuy < trader
                .getGlobals().traderActivity)) {
              trader.sell(trader.getGlobals().stdVolume);
            }
          }

          // Momentum buy medium-term options based on the general population
          if (trader.getContext().getTick() > trader.getGlobals().timeToStartOpinionSharing
              && probToBuy < trader.getGlobals().traderActivity) {

            if (trader.opinion > 0) {
              trader.buyCallOption(trader.optionExpiryTime,
                  trader.getGlobals().marketPrice * trader.getGlobals().callStrikeFactor);
            } else {
              trader.buyPutOption(trader.optionExpiryTime,
                  trader.getGlobals().marketPrice * trader.getGlobals().putStrikeFactor);
            }
          }
          trader.deltaHedge();
          trader.sendShares();
        });
  }


  public double getTermMovingAvg(long nbDays, Map<Long, Double> historicalPrices) {
    double totalPrice = historicalPrices.entrySet().stream()
        .filter(a -> a.getKey() >= getContext().getTick() - nbDays).mapToDouble(Map.Entry::getValue)
        .sum();
    return totalPrice / (nbDays);
  }
}
