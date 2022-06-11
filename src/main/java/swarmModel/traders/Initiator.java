package swarmModel.traders;

/* For the purpose of this model we take a single individual as the perpetrator
   of the squeeze and then opinions are dynamically propagated via other retail investors */

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;
import swarmModel.Globals;
import swarmModel.links.Links.OpinionLink;
import swarmModel.links.Messages.OpinionShared;

public class Initiator extends Agent<Globals> {

  @Variable
  public double opinion;

  @Override
  public void init() {
    this.opinion = getGlobals().maxOpinion;
  }

  private static Action<Initiator> action(SerializableConsumer<Initiator> consumer) {
    return Action.create(Initiator.class, consumer);
  }

  public static Action<Initiator> shareOpinion() {
    return action(
        trader -> {
          if (trader.getContext().getTick() <= trader.getGlobals().timeToSell) {
            trader.opinion = trader.getGlobals().maxOpinion;
            trader.getLinks(OpinionLink.class).send(OpinionShared.class, (msg, link) ->
                msg.opinion = trader.opinion);
          }
          if (trader.getContext().getTick() > trader.getGlobals().timeToSell) {
            trader.opinion = - trader.getGlobals().maxOpinion;
            trader.getLinks(OpinionLink.class).send(OpinionShared.class, (msg, link) ->
                msg.opinion = trader.opinion);
          }
        });
  }
}
