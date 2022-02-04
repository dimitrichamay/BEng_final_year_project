package models.swarming;

import java.util.List;
import models.swarming.Messages.SocialNetworkOpinion;
import simudyne.core.abm.Action;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

public class MomentumTrader extends OpDynTrader {

  @Variable
  public double momentum;

  float lastMarketPrice;

  @Variable
  public double mtParams_beta;

  private static Action<MomentumTrader> action(SerializableConsumer<MomentumTrader> consumer) {
    return Action.create(MomentumTrader.class, consumer);
  }

  @Override
  protected double getAlpha() {
    return getGlobals().pMomentumTrade;
  }

  private double getDemand() {

    if (getGlobals().tickCount == getGlobals().tickToStartOpDyn) {
      isOpDynOn = true;
    }

    double demand;
    if (isOpDynOn) {
      demand = Math.tanh(Math.abs(momentum + opinion) * getGlobals().mtParams_gamma);
    } else {
      demand = Math.tanh(Math.abs(momentum) * getGlobals().mtParams_gamma);
    }
    getDoubleAccumulator("MtDemand").add(demand);
    return demand;
  }

  @Override
  protected double getVolume() {
    return (mtParams_beta / getGlobals().numMomentumTrader) * getDemand();
  }

  @Override
  protected Side getSide() {
    return (momentum + opinion) > 0 ? Side.BUY : Side.SELL;
  }

  public static Action<MomentumTrader> updateMomentum =
      action(
          t -> {
            float price = t.getMarketPrice();
            double mtParams_alpha = t.getGlobals().mtParams_alpha;
            if (t.lastMarketPrice != 0) {
              t.momentum = mtParams_alpha * (price - t.lastMarketPrice)
                  + (1.0 - mtParams_alpha) * t.momentum;
            }
            t.lastMarketPrice = price;
          }
      );

  @Override
  protected void updateOpinion() {

    if (isOpDynOn) {
      double prevOpinion = opinion;
      adjustOpWithInfluencerOp();
      adjustOpinionWithTradersOpinions();
      mtParams_beta = updateSigma(prevOpinion, mtParams_beta);
    }
    getDoubleAccumulator("opinions").add(opinion);

  }

  /* consider opinions from other trader agents */
  public void adjustOpinionWithTradersOpinions() {
    List<Double> opinionsList = getMessageOfType(SocialNetworkOpinion.class).opinionList;
    for (Double o : opinionsList) {
      if (Math.abs(o - opinion) < getGlobals().vicinityRange) {
        double confidenceFactor = 1 / ((Math.abs(o - opinion) + getGlobals().weighting_cf));
        opinion += (o - opinion) * confidenceFactor;
      }
    }
  }
}


