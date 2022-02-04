package models.swarming;

import simudyne.core.annotations.Variable;

/* group of traders that keep buying and strong holding */
public class CoordinatedTrader extends OpDynTrader {

  @Variable
  public double sigma_ct;

  @Override
  protected double getAlpha() {
    return getGlobals().pCoordinatedTrade;
  }

  @Override
  protected double getVolume() {
    if (getGlobals().tickCount == getGlobals().tickToStartOpDyn) {
      isOpDynOn = true;
    }

    return sigma_ct / getGlobals().numCoordinatedTrader;
  }



  @Override
  protected Side getSide() {
    return opinion > 0 ? Side.BUY : Side.SELL;
  }

  @Override
  protected void updateOpinion() {
    if (isOpDynOn) {
      double prevOpinion = opinion;
      adjustOpWithInfluencerOp();
      adjustOpWithDvTrueValue();
      sigma_ct = updateSigma(prevOpinion, sigma_ct);
    }
    getDoubleAccumulator("opinions").add(opinion);
  }

  @Override
  protected void adjustOpinionWithTradersOpinions() {}

}