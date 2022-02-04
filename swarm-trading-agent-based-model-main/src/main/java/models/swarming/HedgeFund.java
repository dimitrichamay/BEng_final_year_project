package models.swarming;

import simudyne.core.abm.Action;
import simudyne.core.functions.SerializableConsumer;

public abstract class HedgeFund extends Trader {

  public float curPrice; // record the current price

  /* ---------- functions ---------- */

  private static Action<HedgeFund> action(SerializableConsumer<HedgeFund> consumer) {
    return Action.create(HedgeFund.class, consumer);
  }

  public static Action<HedgeFund> updateVolume =
      action(HedgeFund::updateVolume);

  protected abstract void updateVolume();

  /* ---------- from Trader class ---------- */
  @Override
  protected abstract double getAlpha();

  @Override
  protected abstract Side getSide();

  // get the information of change in true value and update the volume
  protected abstract double getVolume();

}
