package swarmModel.traders;

import java.util.Map;
import simudyne.core.abm.Action;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;


/*
This agent is an example implementation of a moving average trading strategy
*/


public class MomentumTrader extends BaseTrader {

  @Variable(name = "Long Term Moving Average")
  public double longTermMovingAvg;

  @Variable(name = "Short Term Moving Average")
  public double shortTermMovingAvg;

  //Helper function for ease of interpretation
  private static Action<MomentumTrader> action(SerializableConsumer<MomentumTrader> consumer) {
    return Action.create(MomentumTrader.class, consumer);
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

            int volume = 1; //trader.getRandomInRange(1, (int) trader.shares);
            if (trader.shortTermMovingAvg > trader.longTermMovingAvg && probToBuy < trader
                .getGlobals().traderActivity) {
              trader.buy(volume);
            } else if ((trader.shortTermMovingAvg < trader.longTermMovingAvg && probToBuy < trader
                .getGlobals().traderActivity)) {
              trader.sell(volume);
            }
          }
          trader.sendShares();
        });
  }

  public static Action<MomentumTrader> processOptions(){
    return action(trader -> {
      if (trader.shortTermMovingAvg > trader.longTermMovingAvg) {
        //trader.buyCallOption(20, trader.getGlobals().marketPrice + 2);
      } else if (Math.abs(trader.shortTermMovingAvg) < 0.2) {
        //trader.buyPutOption(20, trader.getGlobals().marketPrice);
      }
    });
  }

  public static Action<MomentumTrader> updateMarketData() {
    return action(
        trader -> {

        });
  }

  public double getTermMovingAvg(long nbDays, Map<Long, Double> historicalPrices) {
    double totalPrice = historicalPrices.entrySet().stream()
        .filter(a -> a.getKey() >= getContext().getTick() - nbDays).mapToDouble(Map.Entry::getValue)
        .sum();
    return totalPrice / (nbDays);
  }
}
