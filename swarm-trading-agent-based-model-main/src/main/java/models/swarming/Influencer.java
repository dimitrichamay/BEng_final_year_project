package models.swarming;

import java.util.List;
import models.swarming.Links.SocialNetworkLink;
import models.swarming.Messages.DvTrueValue;
import models.swarming.Messages.InfluencerOpinionShared;
import models.swarming.Messages.SocialNetworkOpinion;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

/* eg. Elon Musk */
public class Influencer extends Agent<Globals> {

  @Variable
  public double hypedPoint;

  @Variable
  public double opinion; /* expect to be highly positive */

  @Variable
  public boolean isTweeted;

  boolean isOpDynOn = false;

  private static Action<Influencer> action(SerializableConsumer<Influencer> consumer) {
    return Action.create(Influencer.class, consumer);
  }

  public static Action<Influencer> shareOpinion =
      action(
          i -> {
            i.checkIfOpDynIsOn();
            if (i.opinion >= i.hypedPoint && !i.isTweeted) {
              i.getLinks(SocialNetworkLink.class).send(InfluencerOpinionShared.class, i.opinion);
              i.isTweeted = true;
            }
          });


  private void checkIfOpDynIsOn() {
    if (getGlobals().tickCount == getGlobals().tickToStartOpDyn) {
      isOpDynOn = true;
    }
  }

  public static Action<Influencer> updateOpinion =
      action(
          i -> {
            if (i.isOpDynOn) {
              i.adjustOpWithDvTrueValue();
              i.adjustOpWithTradersOp();
            }
          });

  private void adjustOpWithTradersOp() {
    if (hasMessageOfType(Messages.SocialNetworkOpinion.class)) {
      List<Double> opinionsList =
          getMessageOfType(SocialNetworkOpinion.class).opinionList;
      double avgOpList = opinionsList.stream().mapToDouble(o -> o).average().orElse(0.0);
      opinion += getGlobals().multiplier_influencer * (avgOpList - opinion);
    }
  }

  private void adjustOpWithDvTrueValue() {
    if (getGlobals().tickCount >= getGlobals().tickToStartOpDyn) {
      if (hasMessageOfType(DvTrueValue.class)) {
        double dv_trueValue = getMessageOfType(DvTrueValue.class).getBody();
        opinion += getGlobals().multiplier_influencer * dv_trueValue;
      }
    }
  }

}
