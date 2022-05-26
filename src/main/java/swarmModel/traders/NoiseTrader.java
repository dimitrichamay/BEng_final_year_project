package swarmModel.traders;

import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.abm.Action;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

public class NoiseTrader extends OptionTrader {

  @Variable
  public double tradingThresh;

  RandomGenerator random;

  @Override
  public void init() {
    capital = 5000;
    random = this.getPrng().generator;
    tradingThresh = getPrng().uniform(0, 1).sample();
  }

  private static Action<NoiseTrader> action(SerializableConsumer<NoiseTrader> consumer) {
    return Action.create(NoiseTrader.class, consumer);
  }

  public static Action<NoiseTrader> processInformation() {
    return action(
        trader -> {
          trader.updateThreshold();
          double probToBuy = trader.getPrng().uniform(0, 1).sample();
          if (probToBuy < trader.getGlobals().noiseActivity) {
            // Random stock liquidity adding
            if (Math.abs(trader.tradingThresh) > 0.5) {
              trader.buy(trader.getGlobals().stdVolume);
            } else {
              trader.sell(trader.getGlobals().stdVolume);
            }
            // Random option liquidity adding
            if (Math.abs(trader.tradingThresh) > 0.95) {
              trader.buyCallOption(trader.optionExpiryTime,
                  trader.getGlobals().marketPrice * trader.getGlobals().callStrikeFactor);
            } else if (Math.abs(trader.tradingThresh) < 0.05) {
              trader.buyPutOption(trader.optionExpiryTime,
                  trader.getGlobals().marketPrice * trader.getGlobals().putStrikeFactor);
            }
          }
          trader.sendShares();
          trader.deltaHedge();
        });
  }

  public void updateThreshold() {
    double updateFrequency = getGlobals().updateFrequency;
    if (random.nextDouble() <= updateFrequency) {
      tradingThresh = getPrng().uniform(0, 1).sample();
    }
  }
}
