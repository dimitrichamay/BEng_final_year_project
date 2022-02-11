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

  // Number of shares owed - todo: add lender potentially
  @Variable(name = "Number of shares shorted")
  public double shortedStocks = 0;

  //todo: update this 4 to be the initial price in the model
  @Variable
  public double portfolio = shares * 4 + capital;

  private double minCapitalToShort = 0.5;

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
    portfolio = shares * getGlobals().bidPrice + capital - shortedStocks * getGlobals().askPrice;
  }

  private Random generator = new Random();

  protected int getRandomInRange(int start, int end) {
    if (end > 1) {
      return start + generator.nextInt(end - start + 1);
    }
    return 0;
  }

  /******************* SHORT SELLING ******************/

  // Trader borrows shares and sells them, creating a margin account
  public void shortStock(int volume) {
    if (canAffordToShortStock(volume)) {
      //Update sell order numbers
      getDoubleAccumulator("sells").add(volume);
      getLinks(Links.TradeLink.class).send(Messages.SellOrderPlaced.class);

      //Short stock
      shortedStocks += volume;
      capital += volume * getGlobals().bidPrice;
      getDoubleAccumulator("shorts").add(volume);

      updatePortfolio();
    }
  }

  protected boolean canAffordToShortStock(int volume) {
    return capital + shares * getGlobals().bidPrice
        > minCapitalToShort * (volume * getGlobals().askPrice
        + shortedStocks * getGlobals().askPrice);
  }

  public void coverPosition(double volume) {
    if (shortedStocks > 0) {
      volume = Math.min(volume, shortedStocks);
      getDoubleAccumulator("buys").add(volume);
      getLinks(Links.TradeLink.class).send(Messages.BuyOrderPlaced.class);

      shortedStocks -= volume;
      capital -= volume * getGlobals().askPrice;

      updatePortfolio();
    }
  }

  protected boolean hasShortPositions() {
    return shortedStocks > 0;
  }

}
