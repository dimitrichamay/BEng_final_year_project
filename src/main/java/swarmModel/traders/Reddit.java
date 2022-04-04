package swarmModel.traders;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.functions.SerializableConsumer;
import swarmModel.Globals;

public class Reddit extends Agent<Globals> {

  private static Action<Reddit> action(SerializableConsumer<Reddit> consumer) {
    return Action.create(Reddit.class, consumer);
  }

  public static Action<Reddit> updateOpinions(){
    return action(s -> {});
  }
}
