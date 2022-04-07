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

  // The number of opinions used to determine new opinion of each person
  private final double initialNumberOfAffecting = 5;

  @Override
  public void init() {
    random = this.getPrng().generator;
    opinion = getPrng().uniform(-2, 4).sample();
  }

  private static Action<RetailInvestor> action(SerializableConsumer<RetailInvestor> consumer) {
    return Action.create(RetailInvestor.class, consumer);
  }

  public static Action<RetailInvestor> shareOpinion() {
    // Opinions will be ignored by processInformation until it is time to start sharing opinion
    return action(trader -> trader.getLinks(OpinionLink.class)
        .send(OpinionShared.class, (msg, link) -> msg.opinion = trader.opinion));
  }

  public static Action<RetailInvestor> processInformation() {
    return action(trader -> {
      if (trader.getContext().getTick() > trader.getGlobals().timeToStartOpinionSharing) {
        if (trader.isTrading()) {
          trader.tradeOnOpinion(trader.opinion);
        }
        double generalOpinion = trader.getMessagesOfType(Messages.OpinionShared.class).stream()
            .mapToDouble(opinion -> opinion.opinion).average().orElse(0);
        if (generalOpinion != 0 && trader.getContext().getTick() % 3 == 0) {
          /* Updated trader opinion is an average between their own opinion and those around them
             every 3 time steps */
          trader.opinion = (generalOpinion + trader.opinion) / 2;
        }
      }
    });
  }

  private boolean isTrading() {
    // TODO: update this as wanted
    return getPrng().uniform(0, 1).sample() > 0;
  }

  public static Action<RetailInvestor> processOptions() {
    return action(trader -> {
    });
  }
}
