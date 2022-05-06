package swarmModel.traders;

import simudyne.core.abm.Action;
import simudyne.core.functions.SerializableConsumer;

/* Holds a very large initial short position which leads to
   having to cover this position as the price increases */
public class HedgeFund extends BaseTrader{

  private final double shortingPhase = 5;
  private final double shortAmount = 500;
  private final double secondShortSellIncrease = 1.5;
  private final double coverPosition = 2.5;
  private boolean secondShort = false;

  private static Action<HedgeFund> action(SerializableConsumer<HedgeFund> consumer) {
    return Action.create(HedgeFund.class, consumer);
  }

  public static Action<HedgeFund> processInformation(){
    return action(trader ->{
      if (trader.getContext().getTick() < trader.shortingPhase){
        trader.sell(trader.shortAmount);
      }
      double increaseProportion = trader.getGlobals().marketPrice / trader.initialMarketPrice;
      // Second short selling phase to try and make the market fall
      if (increaseProportion > trader.secondShortSellIncrease && !trader.secondShort){
        trader.sell(trader.shortAmount * increaseProportion);
        if (trader.getContext().getTick() % trader.shortingPhase == 0) {
          trader.secondShort = true;
        }
      }
      // Cover position if the stock carries on increasing to minimise loss
      if (trader.getGlobals().marketPrice > trader.coverPosition){
        if (trader.hasShortPosition()) {
          trader.buy(Math.abs(trader.shares));
        }
      }
    });
  }
}
