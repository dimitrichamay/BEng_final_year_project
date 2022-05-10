package swarmModel.traders;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.math3.distribution.NormalDistribution;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;
import swarmModel.Globals;
import swarmModel.links.Links;
import swarmModel.links.Links.TradeLink;
import swarmModel.links.Messages;
import swarmModel.links.Messages.BuyOrderPlaced;
import swarmModel.links.Messages.SellOrderPlaced;
import swarmModel.utils.Option;
import swarmModel.utils.Option.type;

public abstract class BaseTrader extends Agent<Globals> {

  @Variable
  public double capital = 10000;

  @Variable
  public double shares = 0;

  @Variable
  public double portfolio = capital;

  protected final double initialMarketPrice = 15;

  public List<Option> soldOptions = new ArrayList<>();

  private static Action<BaseTrader> action(SerializableConsumer<BaseTrader> consumer) {
    return Action.create(BaseTrader.class, consumer);
  }

  public static Action<BaseTrader> updatePortfolioValues() {
    return action(BaseTrader::updatePortfolioValue);
  }

  /* We allow buying when we have 0 capital since we are
     looking at overall portfolio value as an indicator of wealth */
  public void buy(double volume) {
    shares += volume;
    capital -= volume * getGlobals().marketPrice;
    buyValuesUpdate(volume);
  }

  public void buyValuesUpdate(double volume) {
    getDoubleAccumulator("buys").add(volume);
    getLinks(TradeLink.class).send(BuyOrderPlaced.class, (msg, link) -> {
      msg.volume = volume;
    });
  }

  public void sell(double volume) {
    double toSell = volume;
    //If doesn't have enough shares short sell
    if (shares <= 0) {
      shortStock((int) volume);
      toSell = 0;
    } else if (shares > 0 && shares < volume) {
      toSell -= shares;
      shortStock((int) (volume - shares));
    }
    // If still has shares sell these
    if (toSell > 0) {
      shares -= toSell;
      capital += toSell * getGlobals().marketPrice;
      sellValuesUpdate(toSell);
    }
  }

  public void sellValuesUpdate(double volume) {
    getDoubleAccumulator("sells").add(volume);
    getLinks(TradeLink.class).send(SellOrderPlaced.class, (msg, link) -> msg.volume = volume);
  }

  public void updatePortfolioValue() {
    portfolio = shares * getGlobals().marketPrice + capital;
  }

  /******************* Short Selling ******************/

  // Trader borrows shares and sells them, creating a margin account
  public void shortStock(int volume) {
    shares -= volume;
    capital += volume * getGlobals().marketPrice;
    getDoubleAccumulator("shorts").add(volume);

    //Update sell order numbers
    sellValuesUpdate(volume);
  }

  public boolean hasShortPosition() {
    return shares < 0;
  }
}
