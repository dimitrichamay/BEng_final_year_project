package traders;

import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.abm.Action;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

//Noise trader which shorts stocks used to implement short selling
public class ShortTrader extends Trader {

  RandomGenerator random;
  @Variable
  public double tradingThresh;
  private double maxBuyOrSellProportion = 3;
  // 1 / maxBuyOrSellProportion is the proportion of shares that can be bought or sold at one time

  @Override
  public void init() {
    random = this.getPrng().generator;
    tradingThresh = random.nextGaussian();
  }

  private static Action<ShortTrader> action(SerializableConsumer<ShortTrader> consumer) {
    return Action.create(ShortTrader.class, consumer);
  }

  public static Action<ShortTrader> processInformation() {
    return action(
        trader -> {
          double informationSignal = trader.getGlobals().informationSignal;
          int volume = trader
              .getRandomInRange(1, (int) Math.floor(trader.shares / trader.maxBuyOrSellProportion));
          if (Math.abs(informationSignal) > trader.tradingThresh) {
            if (informationSignal > 0) {
              trader.shortStock(volume);
            } else {
              trader.coverPosition(volume);
            }
          }
        });
  }


  public static Action<ShortTrader> updateThreshold() {
    return action(
        trader -> {
          double updateFrequency = trader.getGlobals().updateFrequency;
          if (trader.random.nextDouble() <= updateFrequency) {
            trader.tradingThresh =
                trader.getMessageOfType(Messages.MarketPriceMessage.class).priceChange;
          }
        });
  }
}
