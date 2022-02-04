package models.swarming;

import models.swarming.Messages.DvTrueValue;
import simudyne.core.annotations.Variable;

/* Hedge fund short selling at a high price */
public class HedgeFundShortHigh extends HedgeFund {

  Side side = Side.SELL;

  public double tradeVolume; // (change dynamically)

  public double tickStartSS;

  @Variable
  public boolean isSSing;

  @Variable
  public boolean isSSInitiated;

  public double ssDuration;

  public double priceToSS;  // price at which HF will short sell

  public double priceToClosePos; // price to cover pos

  public double priceToStopLoss; // cover all the pos (triggered by short squeeze)

  /* ---------- functions ---------- */

  public double getAlpha() {

    curPrice = getMarketPrice();

    if (isSSing && getGlobals().tickCount - tickStartSS < ssDuration) {
      return 1.0d;
    }

    if (!isSSing && curPrice >= priceToSS && !isSSInitiated) {
      isSSing = true;
      isSSInitiated = true;
      tickStartSS = getGlobals().tickCount;
      return 1.0d;
    }

    if (isSSInitiated && (curPrice <= priceToClosePos || curPrice >= priceToStopLoss)) {
      side = Side.BUY;
      return 1.0d;
    }

    isSSing = false; // re-set
    return 0.0d;
  }

  public Side getSide() { return side; }

  protected double getVolume() {
    if (side == Side.BUY) return Math.abs(shares);
    return tradeVolume;
  }

  @Override
  protected void updateVolume() {
    if (hasMessageOfType(DvTrueValue.class)) {
      double dv_trueValue = getMessageOfType(DvTrueValue.class).getBody();
      tradeVolume = Math.abs(dv_trueValue) * getGlobals().multiplier_hfShortHigh;
    }
  }

}
