package traders;

import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import traders.TradingModel.Globals;

public abstract class Trader extends Agent<Globals> {

  public enum Side {BUY, SELL}

  public enum Type {Noise, Fundamental, Momentum}

  @Variable
  public double capital;

  @Variable
  public double shares;

  public void buy(double volume) {
    getDoubleAccumulator("buys").add(volume);
    getLinks(Links.TradeLink.class).send(Messages.BuyOrderPlaced.class);
    shares += volume;
    capital -= volume * getGlobals().marketPrice;
  }

  public void sell(double volume) {
    getDoubleAccumulator("sells").add(volume);
    getLinks(Links.TradeLink.class).send(Messages.SellOrderPlaced.class);
    shares -= volume;
    capital += volume * getGlobals().marketPrice;
  }
}
