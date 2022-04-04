package swarmModel.traders;

import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.abm.Action;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;
import swarmModel.links.Links.OpinionLink;
import swarmModel.links.Messages;
import swarmModel.links.Messages.OpinionShared;

public class RetailInvestor extends BaseTrader {

  @Variable
  public double opinion;

  RandomGenerator random;

  @Override
  public void init() {
    random = this.getPrng().generator;
    opinion = getPrng().uniform(-2, 2).sample();
  }

  private static Action<RetailInvestor> action(SerializableConsumer<RetailInvestor> consumer) {
    return Action.create(RetailInvestor.class, consumer);
  }

  public static Action<RetailInvestor> processInformation() {
    return action(trader -> {
      if (trader.getContext().getTick() > trader.getGlobals().timeToStartOpinionSharing) {
        if (trader.isTrading()) {
          trader.tradeOnOpinion(trader.opinion);
        }
        trader.getLinks(OpinionLink.class).send(OpinionShared.class, (msg, link) -> {
          msg.opinion = trader.opinion;
        });
      }
    });
  }

  public static Action<RetailInvestor> updateOpinions() {
    return action(trader -> {
      if (trader.getContext().getTick() > trader.getGlobals().timeToStartOpinionSharing) {
        int size = trader.getMessagesOfType(Messages.OpinionShared.class).size();
        int affecting = (int) Math
            .min(trader.getContext().getTick() - trader.getGlobals().timeToStartOpinionSharing + 1,
                size);
        double generalOpinion = trader.getMessagesOfType(Messages.OpinionShared.class).stream()
            .mapToDouble(opinion -> opinion.opinion).limit(affecting).average().orElse(0);
        if (generalOpinion != 0){
          trader.opinion = generalOpinion;
        }
      }
    });
  }

  private boolean isTrading() {
    return getPrng().uniform(0, 1).sample() > 0.85;
  }

  public static Action<RetailInvestor> processOptions() {
    return action(trader -> {
    });
  }
}
