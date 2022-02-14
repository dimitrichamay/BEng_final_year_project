package traders;

import java.util.Random;
import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.abm.Action;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

public class NoiseTrader extends Trader {

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

  private static Action<NoiseTrader> action(SerializableConsumer<NoiseTrader> consumer) {
    return Action.create(NoiseTrader.class, consumer);
  }

  public static Action<NoiseTrader> processInformation() {
    return action(
        trader -> {
          double informationSignal = trader.getGlobals().informationSignal;
          int volume = trader
              .getRandomInRange(1, (int) Math.floor(trader.shares / trader.maxBuyOrSellProportion));
          if (Math.abs(informationSignal) > trader.tradingThresh) {
            if (informationSignal > 0 && trader.capital > volume * trader
                .getGlobals().askPrice) {
              trader.buy(volume);
            } else {
              trader.sell(volume);
              //Short stock if info signal is very negative
              if (informationSignal < -0.2){
                trader.shortStock(volume);
              }
            }
          }
        });
  }

  public static Action<NoiseTrader> updateThreshold() {
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
