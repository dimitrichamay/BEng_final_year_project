package swarmModel.traders;

import simudyne.core.abm.Action;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;
import swarmModel.links.Links.OpinionLink;
import swarmModel.links.Messages;
import swarmModel.links.Messages.OpinionShared;

public class RetailInvestor extends Borrower {

  @Variable
  public double opinion;

  // How a change in opinion affects the aggressiveness of trade
  @Variable
  public double sensitivity;

  private boolean doubt = false;
  public double previousPortfolio = 500;


  @Override
  public void init() {
    super.init();
    capital = 1000;
    opinion = getPrng().uniform(-10, 10).sample();
    sensitivity = getPrng().uniform(0, 1).sample();
  }

  private static Action<RetailInvestor> action(SerializableConsumer<RetailInvestor> consumer) {
    return Action.create(RetailInvestor.class, consumer);
  }

  public static Action<RetailInvestor> shareOpinion() {
    // Opinions will be ignored by processInformation until it is time to start sharing opinion
    return action(trader -> trader.getLinks(OpinionLink.class)
        .send(OpinionShared.class, (msg, link) -> msg.opinion = trader.opinion));
  }

  public static Action<RetailInvestor> updateOpinion() {
    return action(trader -> {
      double generalOpinion = trader.getMessagesOfType(Messages.OpinionShared.class).stream()
          .mapToDouble(opinion -> opinion.opinion).average().orElse(0);
      if (generalOpinion != 0 && trader.getContext().getTick() % 3 == 0) {

        if (trader.doubt) {
          // If the trader has doubts, they reverse their opinion
          trader.opinion = - trader.opinion;
        } else {
          /* Updated trader opinion is an average between their own opinion and those around them
             every 3 time steps if they do not doubt*/
          trader.opinion = (generalOpinion + trader.opinion) / 2;
        }
        trader.doubt = trader.getPrng().uniform(0, 1).sample() > 0.9;
      }
    });
  }

  public static Action<RetailInvestor> processInformation() {
    return action(trader -> {
      // We update the sensitivity of the traders opinion trading every 5 steps
      if (trader.getContext().getTick() % 15 == 0 && trader.getContext().getTick() > 1) {
        trader.updateSensitivity();
      }
      if (trader.getContext().getTick() > trader.getGlobals().timeToStartOpinionSharing) {
        if (trader.isTrading()) {
          trader.tradeOnOpinion(trader.opinion, trader.sensitivity);
        }
      }
      trader.deltaHedge();
      trader.sendShares();
    });
  }

  // Updates sensitivity based on how well the trader has been doing recently
  public void updateSensitivity() {
    if (portfolio > previousPortfolio) {
      sensitivity += 0.025;
    } else {
      sensitivity -= 0.025;
    }
    // Cannot have sensitivity greater than 1
    if (sensitivity > 1) {
      sensitivity = 1;
    } else if (sensitivity < 0) {
      sensitivity = 0;
    }
    previousPortfolio = portfolio;
  }

  private boolean isTrading() {
    return getPrng().uniform(0, 1).sample() > 0.25;
  }
}
