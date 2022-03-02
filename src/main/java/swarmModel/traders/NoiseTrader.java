package swarmModel.traders;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.abm.Action;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;
import swarmModel.utils.Option;

public class NoiseTrader extends BaseTrader {


  RandomGenerator random;
  @Variable
  public double tradingThresh;

  public double sharesToSend = 0;

  // 1 / maxBuyOrSellProportion is the proportion of shares that can be bought or sold at one time
  private double maxBuyOrSellProportion = 3;

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
          int volume = 1; //trader.getRandomInRange(1, (int) Math.floor(trader.shares / trader.maxBuyOrSellProportion));
          double probToBuy = trader.getPrng().uniform(0, 1).sample();
          if (probToBuy < trader.getGlobals().noiseActivity) {
            if (Math.abs(trader.tradingThresh) > 0.5) {
              trader.buy(volume);
            } else {
              trader.sell(volume);
            }
          }
          trader.buyValuesUpdate(trader.sharesToSend);
          trader.sellValuesUpdate(trader.sharesToSend);
        });
  }

  public static Action<NoiseTrader> processOptions(){
    return action(trader -> {
      if (Math.abs(trader.tradingThresh) > 0.8) {
        trader.buyCallOption(20, trader.getGlobals().marketPrice - 0.01);
      } else if (Math.abs(trader.tradingThresh) < 0.2) {
        trader.buyPutOption(20, trader.getGlobals().marketPrice + 0.01);
      }
    });
  }

  // Update each Option on every time step
  public static Action<NoiseTrader> updateOptions() {
    return action(noiseTrader -> {
      double total = 0;
      if (noiseTrader.getContext().getTick() > 0) {
        List<Option> expiredOptions = new ArrayList<>();
        for (Option option : noiseTrader.boughtOptions) {
          boolean expired = option.timeStep();
          if (expired) {
            double toSell = noiseTrader.actOnOption(option);
            total += toSell;
            expiredOptions.add(option);
            if (option.isCallOption()) {
              noiseTrader.callOptions--;
            } else {
              noiseTrader.putOptions--;
            }
            //System.out.println("Removed option with price " + option.getOptionPrice() + (option.isCallOption() ? "Call Option" : "Put option"));
          }
        }
        noiseTrader.boughtOptions.removeAll(expiredOptions);
      }
      noiseTrader.sharesToSend = total;
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
