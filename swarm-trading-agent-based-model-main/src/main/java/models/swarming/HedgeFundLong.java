package models.swarming;

import models.swarming.Messages.DvTrueValue;
import simudyne.core.annotations.Variable;

public class HedgeFundLong extends HedgeFund {


  Side side = Side.BUY;

  @Variable
  public double tradeVolume;

  public double priceToBuy;

  @Variable
  public double priceToSell;

  /* --------------- functions --------------- */

  public double getAlpha() {

    curPrice = getMarketPrice();

    if (curPrice >= priceToBuy && shares == 0) {
      return 1.0d;
    }

    if (shares > 0 && curPrice >= priceToSell) {
      side = Side.SELL;
      return 1.0d;
    }

    return 0.0d;
  }

  protected Side getSide() {
    return side;
  }

  protected double getVolume() {
    if (side == Side.SELL) return Math.abs(shares);
    return tradeVolume;
  }

  @Override
  protected void updateVolume() {
    if (hasMessageOfType(DvTrueValue.class)) {
      double dv_trueValue = getMessageOfType(DvTrueValue.class).getBody();
      tradeVolume = Math.abs(dv_trueValue) * getGlobals().multiplier_hfLong;
    }
  }
}
