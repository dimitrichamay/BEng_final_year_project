package swarmModel.traders;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import swarmModel.Globals;
import swarmModel.links.Links.TradeLink;
import swarmModel.links.Messages.BuyOrderPlaced;
import swarmModel.links.Messages.SellOrderPlaced;
import swarmModel.utils.Option;

public abstract class BaseTrader extends Agent<Globals> {

  @Variable
  public double capital = 100;

  @Variable
  public double shares = 0;

  //todo: update this 4 to be the initial price in the model
  @Variable
  public double portfolio = shares * 4 + capital;

  private double minCapitalToShort = 1;

  public List<Option> options = new ArrayList<>();

  public void buy(double volume) {
    if (capital > volume * getGlobals().marketPrice) {
      getDoubleAccumulator("buys").add(volume);
      getLinks(TradeLink.class).send(BuyOrderPlaced.class);
      shares += volume;
      capital -= volume * getGlobals().marketPrice;
      updatePortfolio();
    }
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
      getDoubleAccumulator("sells").add(toSell);
      getLinks(TradeLink.class).send(SellOrderPlaced.class);
      shares -= toSell;
      capital += toSell * getGlobals().marketPrice;
      updatePortfolio();
    }
  }


  public void updatePortfolio() {
    portfolio = shares * getGlobals().marketPrice + capital;
  }

  private Random generator = new Random();

  protected int getRandomInRange(int start, int end) {
    if (end > 1) {
      return start + generator.nextInt(end - start + 1);
    }
    return 0;
  }

  /******************* Short Selling ******************/

  // Trader borrows shares and sells them, creating a margin account
  public void shortStock(int volume) {
    if (canAffordToShortStock(volume)) {
      //Update sell order numbers
      getDoubleAccumulator("sells").add(volume);
      getLinks(TradeLink.class).send(SellOrderPlaced.class);

      shares -= volume;
      capital += volume * getGlobals().marketPrice;
      getDoubleAccumulator("shorts").add(volume);

      updatePortfolio();
    }
  }

  protected boolean canAffordToShortStock(int volume) {
    if (shares == 0) {
      return capital >= minCapitalToShort * (volume * getGlobals().marketPrice);
    } else {
      // Shares < 0 since cannot short sell if have shares
      return capital >= minCapitalToShort * (Math.abs(shares) + volume) * getGlobals().marketPrice;
    }
  }

  protected boolean hasShortPositions() {
    return shares < 0;
  }

  /******************* Options Selling ******************/

  public void buyPutOptions(int volume, int expiryTime, double exercisePrice) {

  }

  public void buyCallOptions(int volume, int expiryTime, double exercisePrice) {

  }

  public void actOnOption(Option option, boolean exerciseOption) {
    if (exerciseOption) {

    } else {
      options.remove(option);
    }
  }

  public List<Option> getOptions() {
    return options;
  }

}
