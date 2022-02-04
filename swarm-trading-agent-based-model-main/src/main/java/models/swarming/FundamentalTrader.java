package models.swarming;

import simudyne.core.abm.Action;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

/* Fundamental(informed) trader: force to buy and sell at prices bounded by the intrinsic value */
public class FundamentalTrader extends Trader {

  @Variable
  public double intrinsicValue;

  private double priceDistortion;

  public double zScore;

  /* ------------------ functions definition -------------------------*/

  private static Action<FundamentalTrader> action(
      SerializableConsumer<FundamentalTrader> consumer) {
    return Action.create(FundamentalTrader.class, consumer);
  }

  @Override
  protected double getAlpha() {
    return 1;
  }

  @Override
  protected double getVolume() {
    double ftParam_kappa = getGlobals().ftParam_kappa;
    int numFundamentalTrader = getGlobals().numFundamentalTrader;
    return (ftParam_kappa / numFundamentalTrader) * Math.abs(getPriceDistortion());
  }

  @Override
  protected Side getSide() {
    return priceDistortion > 0 ? Side.BUY : Side.SELL;
  }

  private double getPriceDistortion() {
    priceDistortion = intrinsicValue - getMarketPrice();
    return priceDistortion;
  }

  public static Action<FundamentalTrader> updateIntrinsicValue =
      action(
          t -> {
            double trueValue = t.getMessageOfType(Messages.TrueValue.class).getBody();
            t.intrinsicValue = t.zScore * t.getGlobals().sigma_u + trueValue;
            t.intrinsicValue = t.intrinsicValue <= 0 ? 0 : t.intrinsicValue;
          });
}