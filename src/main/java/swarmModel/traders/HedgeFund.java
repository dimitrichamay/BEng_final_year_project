package swarmModel.traders;

import simudyne.core.abm.Action;
import simudyne.core.functions.SerializableConsumer;

/* Holds a very large initial short position which leads to
   having to cover this position as the price increases */
public class HedgeFund extends BaseTrader{

  private final double shortingPhase = 3;
  private boolean secondShort = false;

  private static Action<HedgeFund> action(SerializableConsumer<HedgeFund> consumer) {
    return Action.create(HedgeFund.class, consumer);
  }

  public static Action<HedgeFund> processInformation(){
    return action(trader ->{
      if (trader.getContext().getTick() < trader.shortingPhase){
        trader.sell(500);
      }
      // Second short selling to try and make the market fall
      if (trader.getGlobals().marketPrice > 2 * trader.initialMarketPrice && !trader.secondShort){
        trader.sell(1000);
        trader.secondShort = true;
      }
      // Cover position if the stock carries on increasing to minimise loss
      if (trader.getGlobals().marketPrice > 3 * trader.initialMarketPrice){
        trader.buy(Math.abs(trader.shares));
      }
    });
  }

  public static Action<HedgeFund> processOptions(){
    return action(trader->{});
  }
}
