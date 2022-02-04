package models.swarming;

import com.google.common.primitives.Doubles;
import java.util.List;
import models.swarming.Links.SocialNetworkLink;
import models.swarming.Messages.DvTrueValue;
import models.swarming.Messages.TraderOpinionShared;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.functions.SerializableConsumer;
import simudyne.core.graph.Message.Double;

public class SocialNetwork extends Agent<Globals> {

  private static Action<SocialNetwork> action(SerializableConsumer<SocialNetwork> consumer) {
    return Action.create(SocialNetwork.class, consumer);
  }

  /* send the change in true value and opinions to trader and hegde fund agents */
  public static Action<SocialNetwork> publishOpAndDvTrueValue =
      action(
          s -> {
            s.publishTrueValueToTraders();
            s.publishInfluencerOpinion();
            s.publishTradersOpinions();
          });

  private void publishTrueValueToTraders() {
    if (hasMessageOfType(DvTrueValue.class)) {
      double dv_trueValue = getMessageOfType(DvTrueValue.class).getBody();
      getLinks(SocialNetworkLink.class).send(DvTrueValue.class, dv_trueValue);
    }
  }

  /* handle opinions from trader agents */
  private void publishTradersOpinions() {
    double[] opinionsListRaw = getMessagesOfType(TraderOpinionShared.class).stream()
        .mapToDouble(Double::getBody).toArray();
    List<java.lang.Double> opinionsList = Doubles.asList(opinionsListRaw);

    getLinks(SocialNetworkLink.class)
        .send(Messages.SocialNetworkOpinion.class, (s, t) -> s.opinionList = opinionsList);

  }

  /* handle opinions from influencer agent */
  private void publishInfluencerOpinion() {

    if (hasMessageOfType(Messages.InfluencerOpinionShared.class)) {
      double influencerOpinion = getMessageOfType(Messages.InfluencerOpinionShared.class).getBody();

      getLinks(SocialNetworkLink.class)
          .send(Messages.InfluencerSocialNetworkOpinion.class, influencerOpinion);

    }
  }
}


