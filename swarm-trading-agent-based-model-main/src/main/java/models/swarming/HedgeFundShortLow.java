package models.swarming;


import models.swarming.Messages.DvTrueValue;
import simudyne.core.annotations.Variable;

/* Hedge fund: simplified strategy in this model. Assume HF only trades 3 times.
   1. at specified time step: short sell x amount of shares

   2. if the current market price is raised to double of the price at which HF short sold,
   short sell 2x amount of shares to suppress the market price

   3. if the current market price is raised to triple of the price at which HF short sold,
   close all the short sell positions to prevent from suffer even bigger lost

   4. HF is out of market
 */
public class HedgeFundShortLow extends HedgeFund {

  Side side;

  @Variable
  public double tradeVolume; // change dynamically

  public double tickStartSS;

  public boolean isSSing;

  public boolean isFirstSSInitiated;

  public boolean isSecondSSInitiated;

  public double ssDuration;

  public double priceToFirstSS;   // price that trigger 1st short selling

  public double priceToSecondSS;   // price that triggers 2nd short selling

  public double priceToCoverPos;  // price to cover pos (expect to never happen)

  public double priceToStopLoss; // cover all the pos (triggered by short squeeze)

  /* ---------------------- functions ----------------------*/
  public double getAlpha() {

    curPrice = getMarketPrice();

    if (isSSing && getGlobals().tickCount - tickStartSS < ssDuration) {

      return 1.0d;
    }

    if (!isSSing && curPrice <= priceToFirstSS && !isFirstSSInitiated) {
      isSSing = true;
      isFirstSSInitiated = true;
      tickStartSS = getGlobals().tickCount;
      return 1.0d; // expect to further sell
    }
    if (!isSSing && curPrice >= priceToSecondSS && !isSecondSSInitiated) {
      isSSing = true;
      isSecondSSInitiated = true;
      tickStartSS = getGlobals().tickCount;
      return 1.0d; // expect to further sell
    }

    if (curPrice <= priceToCoverPos || curPrice >= priceToStopLoss) {
      side = Side.BUY;
      return 1.0d; // buy
    }

    isSSing = false; // re-set
    return 0.0d;
  }

  public Side getSide() {
    return side;
  }

  protected double getVolume() {
    if (side == Side.BUY) {
      return Math.abs(shares);
    }
    return tradeVolume;
  }

  @Override
  protected void updateVolume() {
    if (hasMessageOfType(DvTrueValue.class)) {
      double dv_trueValue = getMessageOfType(DvTrueValue.class).getBody();
      if (isFirstSSInitiated) {
        tradeVolume = Math.abs(dv_trueValue) * getGlobals().multiplier_hfShortLow
            + Math.abs(shares) / ssDuration;
      } else {
        tradeVolume = Math.abs(dv_trueValue) * getGlobals().multiplier_hfShortLow;
      }
    }
  }

}
