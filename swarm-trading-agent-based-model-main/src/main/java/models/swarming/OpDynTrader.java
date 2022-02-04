package models.swarming;

import models.swarming.Links.SocialNetworkLink;
import models.swarming.Messages.DvTrueValue;
import models.swarming.Messages.InfluencerSocialNetworkOpinion;
import simudyne.core.abm.Action;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

public abstract class OpDynTrader extends Trader {

  @Variable
  public double opinion;

  boolean isOpDynOn = false;

  private static Action<OpDynTrader> action(SerializableConsumer<OpDynTrader> consumer) {
    return Action.create(OpDynTrader.class, consumer);
  }

  /* ------ from Trader class -------------- */
  protected abstract double getAlpha();

  protected abstract Side getSide();

  protected abstract double getVolume();

  /* ------- from OpDynTrader class ------------- */

  /* update the self opinion */
  protected abstract void updateOpinion();

  protected abstract void adjustOpinionWithTradersOpinions();

  /* share opinion to the social network */
  public static Action<OpDynTrader> shareOpinion =
      action(
          t -> {
            t.getLinks(SocialNetworkLink.class).send(Messages.TraderOpinionShared.class, t.opinion);
          });

  /* fetch the opinion from social network and update the self opinion accordingly */
  public static Action<OpDynTrader> updateOpinion =
      action(OpDynTrader::updateOpinion);

  /* take opinion from influencer */
  protected void adjustOpWithInfluencerOp() {
    if (hasMessageOfType(InfluencerSocialNetworkOpinion.class)) {
      double influencerOpinion = getMessageOfType(InfluencerSocialNetworkOpinion.class).getBody();
      opinion += influencerOpinion * getGlobals().weighting_influencer; // take influencer opinion
    }
  }

  /* update sigma_ct (coordinated traders) or mtParams_beta (momentum traders) */
  protected double updateSigma(double prevOpinion, double sigma) {

    double prevOpinionCopy = Math.abs(prevOpinion);
    double opinionCopy = Math.abs(opinion);

    double demandMultiplier =
        type == Type.Momentum ? getGlobals().mt_demandMultiplier : getGlobals().ct_demandMultiplier;

    if (prevOpinionCopy < opinionCopy) {
      sigma = (opinionCopy * sigma / prevOpinionCopy) * demandMultiplier;
    } else {
      sigma = (opinionCopy * sigma / prevOpinionCopy) / demandMultiplier;
    }
    return sigma;
  }

  /* consider change in market true value */
  protected void adjustOpWithDvTrueValue() {
    if (hasMessageOfType(DvTrueValue.class)) {
      double dv_trueValue = getMessageOfType(DvTrueValue.class).getBody();
      double dv_opinion = getGlobals().weighting_dvTrueValue * dv_trueValue;
      opinion += dv_opinion;
    }
  }

}
