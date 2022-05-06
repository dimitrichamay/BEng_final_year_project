package swarmModel.traders;

import simudyne.core.abm.Action;
import simudyne.core.functions.SerializableConsumer;

public class LiquidityProvider extends BaseTrader{

  private static Action<LiquidityProvider> action(SerializableConsumer<LiquidityProvider> consumer) {
    return Action.create(LiquidityProvider.class, consumer);
  }

  // Adds option liquidity into the market
  public static Action<LiquidityProvider> addOptionLiquidity() {
    return action(trader -> {
      double tradingThresh = trader.getPrng().uniform(0, 1).sample();
      double probToBuy = trader.getPrng().uniform(0, 1).sample();
      if (probToBuy < trader.getGlobals().noiseActivity) {
        if (Math.abs(tradingThresh) > 0.95) {
          trader.buyCallOption(trader.optionExpiryTime,
              trader.getGlobals().marketPrice * trader.getGlobals().callStrikeFactor);
        } else if (Math.abs(tradingThresh) < 0.05) {
          trader.buyPutOption(trader.optionExpiryTime,
              trader.getGlobals().marketPrice * trader.getGlobals().putStrikeFactor);
        }
      }
    });
  }
}
