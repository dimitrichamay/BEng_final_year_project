package models.swarming;

/* Noise trader (uninformed): Randomly buy or sell. Only contribute to noise */
public class NoiseTrader extends Trader {

  @Override
  protected double getAlpha() {
    return getGlobals().pNoiseTrade;
  }

  @Override
  protected Side getSide() { // 50% sell, 50% buy
    double p = getPrng().uniform(0, 1).sample();
    return p > 0.5 ? Side.BUY : Side.SELL;
  }

  @Override
  protected double getVolume() { // change it to double
    return 1 * getGlobals().sigma_n;
  }

}

