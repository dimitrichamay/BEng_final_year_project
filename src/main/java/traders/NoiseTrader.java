package traders;

import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

public class NoiseTrader extends Agent<TradingModel.Globals> {

  RandomGenerator random;
  @Variable public double tradingThresh;

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

          if (Math.abs(informationSignal) > trader.tradingThresh) {
            if (informationSignal > 0) {
              trader.buy();
            } else {
              trader.sell();
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

  private void buy() {
    getLongAccumulator("buys").add(1);
    getLinks(Links.TradeLink.class).send(Messages.BuyOrderPlaced.class);
  }

  private void sell() {
    getLongAccumulator("sells").add(1);
    getLinks(Links.TradeLink.class).send(Messages.SellOrderPlaced.class);
  }
}
