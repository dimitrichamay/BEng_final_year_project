package swarmModel.traders;

import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.abm.Action;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

public class NoiseTrader extends BaseTrader {


  RandomGenerator random;
  @Variable
  public double tradingThresh;

  @Override
  public void init() {
    random = this.getPrng().generator;
    tradingThresh = getPrng().uniform(0, 1).sample();
  }

  private static Action<NoiseTrader> action(SerializableConsumer<NoiseTrader> consumer) {
    return Action.create(NoiseTrader.class, consumer);
  }

  public static Action<NoiseTrader> processInformation() {
    return action(
        trader -> {
          double probToBuy = trader.getPrng().uniform(0, 1).sample();
          if (probToBuy < trader.getGlobals().noiseActivity) {
            if (Math.abs(trader.tradingThresh) > 0.5) {
              trader.buy(trader.getGlobals().stdVolume);
            } else {
              trader.sell(trader.getGlobals().stdVolume);
            }
          }
          trader.sendShares();
        });
  }

  public static Action<NoiseTrader> updateThreshold() {
    return action(
        trader -> {
          double updateFrequency = trader.getGlobals().updateFrequency;
          if (trader.random.nextDouble() <= updateFrequency) {
            trader.tradingThresh = trader.getPrng().uniform(0, 1).sample();
          }
        });
  }


}
