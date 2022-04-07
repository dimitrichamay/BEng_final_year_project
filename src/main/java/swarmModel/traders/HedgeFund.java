package swarmModel.traders;

import simudyne.core.abm.Action;
import simudyne.core.functions.SerializableConsumer;

/* Holds a very large initial short position which leads to
   having to cover this position as the price increases */
public class HedgeFund extends BaseTrader{

  private final double initialShortTime = 5;

  private static Action<HedgeFund> action(SerializableConsumer<HedgeFund> consumer) {
    return Action.create(HedgeFund.class, consumer);
  }

  public static Action<HedgeFund> processInformation(){
    return action(trader ->{
      if (trader.getContext().getTick() == 5){
        trader.shortStock(100);
      }
    });
  }

  public static Action<HedgeFund> processOptions(){
    return action(trader->{});
  }
}
