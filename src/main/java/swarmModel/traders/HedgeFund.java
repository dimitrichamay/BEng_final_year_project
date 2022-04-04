package swarmModel.traders;

import simudyne.core.abm.Action;
import simudyne.core.functions.SerializableConsumer;

public class HedgeFund extends BaseTrader{

  private static Action<HedgeFund> action(SerializableConsumer<HedgeFund> consumer) {
    return Action.create(HedgeFund.class, consumer);
  }

  public static Action<HedgeFund> processInformation(){
    return action(trader ->{});
  }

  public static Action<HedgeFund> processOptions(){
    return action(trader->{});
  }
}
