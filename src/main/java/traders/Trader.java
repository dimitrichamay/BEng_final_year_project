package traders;

import java.util.Random;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Input;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;
import traders.TradingModel.Globals;

public abstract class Trader extends Agent<Globals> {

  @Variable
  public double capital = 100;

  @Variable
  public double shares = 20;

  //todo: update this 4 to be the initial price in the model
  @Variable
  public double portfolio = shares * 4 + capital;

  public void buy(double volume) {
    getDoubleAccumulator("buys").add(volume);
    getLinks(Links.TradeLink.class).send(Messages.BuyOrderPlaced.class);
    shares += volume;
    capital -= volume * getGlobals().askPrice;
    updatePortfolio();
  }

  public void sell(double volume) {
    getDoubleAccumulator("sells").add(volume);
    getLinks(Links.TradeLink.class).send(Messages.SellOrderPlaced.class);
    shares -= volume;
    capital += volume * getGlobals().bidPrice;
    updatePortfolio();
  }

  public void updatePortfolio() {
    portfolio = shares * getGlobals().bidPrice + capital;
  }

  private Random generator = new Random();

  protected int getRandomInRange(int start, int end) {
    if (end > 1) {
      return start + generator.nextInt(end - start + 1);
    }
    return 0;
  }
}
