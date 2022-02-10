package traders;

import java.util.HashMap;
import java.util.Map;
import simudyne.core.abm.Action;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;
import traders.Messages.MarketPriceMessage;


/*
This agent is an example implementation of a moving average trading strategy
*/


public class MomentumTrader extends Trader {

  @Variable(name = "Long Term Bid Moving Average")
  public double longTermBidMovingAvg;

  @Variable(name = "Short Term Bid Moving Average")
  public double shortTermBidMovingAvg;

  @Variable(name = "Long Term Ask Moving Average")
  public double longTermAskMovingAvg;

  @Variable(name = "Short Term Ask Moving Average")
  public double shortTermAskMovingAvg;

  public Map<Long, Double> historicalBidPrices = new HashMap<>();
  public Map<Long, Double> historicalAskPrices = new HashMap<>();

  //Helper function for ease of interpretation
  private static Action<MomentumTrader> action(SerializableConsumer<MomentumTrader> consumer) {
    return Action.create(MomentumTrader.class, consumer);
  }

  public static Action<MomentumTrader> processInformation() {
    return action(
        trader -> {
          if (trader.getContext().getTick() > trader.getGlobals().longTermAverage) {
            trader.longTermAskMovingAvg = trader
                .getTermMovingAvg(trader.getGlobals().longTermAverage, trader.historicalAskPrices);
            trader.shortTermAskMovingAvg = trader
                .getTermMovingAvg(trader.getGlobals().shortTermAverage, trader.historicalAskPrices);
            trader.longTermBidMovingAvg = trader
                .getTermMovingAvg(trader.getGlobals().longTermAverage, trader.historicalBidPrices);
            trader.shortTermBidMovingAvg = trader
                .getTermMovingAvg(trader.getGlobals().shortTermAverage,trader.historicalBidPrices);
            double probToBuy = trader.getPrng().uniform(0, 1).sample();

            int volume = trader.getRandomInRange(1, (int) trader.shares);
            if (trader.capital > volume * trader.getGlobals().askPrice) {
              if (trader.shortTermAskMovingAvg > trader.longTermAskMovingAvg && probToBuy < trader
                  .getGlobals().traderActivity) {
                trader.buy(volume);
              }
            } else if ((trader.shortTermBidMovingAvg < trader.longTermBidMovingAvg && probToBuy < trader
                .getGlobals().traderActivity)) {
              trader.sell(volume);
            }
          }
        });
  }

  public static Action<MomentumTrader> updateMarketData() {
    return action(
        trader -> {
          trader.historicalAskPrices
              .put(trader.getContext().getTick(), trader.getMessageOfType(
                  MarketPriceMessage.class).askPrice);
          trader.historicalBidPrices
              .put(trader.getContext().getTick(), trader.getMessageOfType(
                  MarketPriceMessage.class).bidPrice);
        });
  }

  public double getTermMovingAvg(long nbDays, Map<Long, Double> historicalPrices) {
    double totalPrice = historicalPrices.entrySet().stream()
        .filter(a -> a.getKey() >= getContext().getTick() - nbDays).mapToDouble(Map.Entry::getValue)
        .sum();
    return totalPrice / (nbDays);
  }
}
