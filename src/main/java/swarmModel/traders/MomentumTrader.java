package swarmModel.traders;

import java.util.Map;
import simudyne.core.abm.Action;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;
import swarmModel.links.Messages;


/*
   This agent is an example implementation of a moving average trading strategy
*/

public class MomentumTrader extends Borrower {

  @Variable(name = "Long Term Moving Average")
  public double longTermMovingAvg;

  @Variable(name = "Short Term Moving Average")
  public double shortTermMovingAvg;

  private double generalOpinion = 0;

  // Helper function for ease of interpretation
  private static Action<MomentumTrader> action(SerializableConsumer<MomentumTrader> consumer) {
    return Action.create(MomentumTrader.class, consumer);
  }

  public static Action<MomentumTrader> updateOpinion(){
    return action(trader -> {
      trader.generalOpinion = trader.getMessagesOfType(Messages.OpinionShared.class).stream()
          .mapToDouble(opinion -> opinion.opinion).average().orElse(0);
    });
  }

  public static Action<MomentumTrader> processInformation() {
    return action(
        trader -> {
          if (trader.getContext().getTick() > trader.getGlobals().longTermAverage) {
            trader.longTermMovingAvg = trader
                .getTermMovingAvg(trader.getGlobals().longTermAverage,
                    trader.getGlobals().historicalPrices);
            trader.shortTermMovingAvg = trader
                .getTermMovingAvg(trader.getGlobals().shortTermAverage,
                    trader.getGlobals().historicalPrices);
            double probToBuy = trader.getPrng().uniform(0, 1).sample();

            if (trader.shortTermMovingAvg > trader.longTermMovingAvg && probToBuy < trader
                .getGlobals().traderActivity) {
              trader.buy(trader.getGlobals().stdVolume);
            } else if ((trader.shortTermMovingAvg < trader.longTermMovingAvg && probToBuy < trader
                .getGlobals().traderActivity)) {
              trader.sell(trader.getGlobals().stdVolume);
            }
          }

          // Momentum buy medium-term options based on the general population opinion every 5 steps
          if (trader.getContext().getTick() > trader.getGlobals().timeToStartOpinionSharing
              && trader.getContext().getTick() % 5 == 0) {

            if (trader.generalOpinion > 0) {
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
