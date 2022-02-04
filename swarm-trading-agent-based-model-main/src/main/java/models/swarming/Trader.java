package models.swarming;

import models.swarming.Links.TradeLink;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

public abstract class Trader extends Agent<Globals> {

  public enum Side {BUY, SELL}

  public enum Type {Noise, Fundamental, Momentum, Coordinated,
    HedgeFundSL, HedgeFundSH, HedgeFundL}
   /*
   ------- Trader type -------
   Noise trader (uninformed): randomly buy or sell
   Fundamental trader (informed): force to buy and sell at prices bounded by the intrinsic value
   Momentum: buy securities that are rising and sell them when they look to have peaked
   Coordinated: Reddit WSB - buy and strong hold
   HedgeFundSL: hedge fund - going short the shares at a low price
   HedgeFundSH: hedge fund - going short the shares at a high price
   HedgeFundL: hedge fund - going long
   */

  Type type; /* Trader type */

  @Variable
  public double wealth;

  @Variable
  public double shares;

  @Variable
  public double marginAccount;

  boolean isLeftMarket;

  static double initialMarginRequirement;

  static double maintenanceMargin;

  /* ------------------- functions ------------------- */

  private static Action<Trader> action(SerializableConsumer<Trader> consumer) {
    return Action.create(Trader.class, consumer);
  }

  public static Action<Trader> submitOrders =
      action(
          t -> {

            if (t.isLeftMarket) {
              return;
            }

            double alpha = t.getAlpha();
            double p = t.getPrng().uniform(0, 1).sample();
            if (p < alpha) {
              double volume = t.getVolume();
              Side side = t.getSide();
              switch (side) {
                case BUY:
                  t.handleWhenBuyShares(volume);
                  break;
                case SELL:
                  t.handleWhenSellShares(volume);
                  break;
              }

              if (t.type == Type.Coordinated || t.type == Type.Momentum) {
                t.getDoubleAccumulator("Wealth").add(t.wealth);
                t.getDoubleAccumulator("marginAcct").add(t.marginAccount);
              }

            } else {
              t.hold();
            }
          }
      );

  /* get the probability of trading */
  protected abstract double getAlpha();

  /* either buy / cover or sell / short sell */
  protected abstract Side getSide();

  protected abstract double getVolume();

  protected void hold() { buy(0); }

  protected void handleWhenBuyShares(double volume) {

    if (hasEnoughWealth(getMarketPrice() * volume)) {
      if (shares >= 0) {
        buy(volume);

      } else {
        if (shares + volume <= 0) { // volume < current short sell positions
          closeShortPos(volume);

        } else if (shares + volume > 0) { // volume > current short sell positions
          double vol = shares + volume;
          closeShortPos(Math.abs(shares));
          buy(vol);
        }
      }

      updateTraderStatus();
      return;
    }

    noMoneyToTrade();
  }

  private void updateTraderStatus() {
    if (shares < 0) {
      handleDuringShortSelling();
    }
    if (shares < 0) {
      updateMarginAccount(Math.abs(shares));
    } else if (shares >= 0) {
      resetMarginAccount();
    }

    if (wealth < 0 && shares <= 0) {
      isLeftMarket = true;
    }
  }

  private void noMoneyToTrade() { hold(); }

  protected float getMarketPrice() {
    return getMessageOfType(Messages.MarketPrice.class).getBody();
  }

  protected void handleWhenSellShares(double volume) {
    if (hasEnoughShares(volume)) {
      sell(volume);
      return;
    }
    handleNotEnoughSharesToSell(volume);
  }

  protected void handleNotEnoughSharesToSell(double sharesToSell) {
    if (shares <= 0) {
      if (isShortSellAllowed(sharesToSell)) {
        shortSell(sharesToSell);
      }

    } else if (shares > 0) {
      double vol = sharesToSell - shares;
      sell(shares);
      if (isShortSellAllowed(vol)) {
        shortSell(vol);
      }
    }
  }

  private void initiateMarginAccount(double sharesToShort) {
    marginAccount = sharesToShort * getMarketPrice() * (1 + initialMarginRequirement);
  }

  /* check if short selling is possible */
  protected boolean isShortSellAllowed(double sharesToSell) {
    return isWealthMeetInitialMarginRequirement(sharesToSell);
  }

  /* check if a margin account can be initiated */
  protected boolean isWealthMeetInitialMarginRequirement(double sharesToSell) {
    // example: with 10K investment, you need 5K cash at start
    if (shares == 0) {
      return wealth >= (sharesToSell * getMarketPrice() * initialMarginRequirement);
    }

    // share < 0: (there are already some short positions)
    return wealth >=
        ((Math.abs(shares) + sharesToSell) * getMarketPrice() * initialMarginRequirement);

  }

  protected void handleDuringShortSelling() {
    if (isMarginCallTriggered() && !hasEnoughWealthToMaintainMarginAccount()) {
      forceCoverShortPos();
    }
  }

  /* reset the margin account */
  protected void resetMarginAccount() { marginAccount = 0; }

  /* obligated to cover the short positions */
  protected void forceCoverShortPos() { closeShortPos(Math.abs(shares)); }

  /* cover the short positions */
  protected void closeShortPos(double volume) {
    getDoubleAccumulator("closeShorts").add(volume);
    getLinks(TradeLink.class).send(Messages.CloseShortPosOrderPlaced.class, volume);

    buy(volume);

    if (shares >= 0) {
      resetMarginAccount();
    }
  }

  /* buy the shares */
  protected void buy(double volume) {
    getDoubleAccumulator("buys").add(volume);
    getLinks(TradeLink.class).send(Messages.BuyOrderPlaced.class, volume);

    wealth -= getMarketPrice() * volume;
    shares += volume;
  }

  protected boolean hasEnoughWealthToMaintainMarginAccount() {
    double totalValueOfShorts = Math.abs(shares) * getMarketPrice();
    return wealth >= totalValueOfShorts * maintenanceMargin;
  }

  /* check if margin call is triggered */
  protected boolean isMarginCallTriggered() {
    double totalValueOfShorts = Math.abs(shares) * getMarketPrice();
    return ((marginAccount - totalValueOfShorts) / totalValueOfShorts) < maintenanceMargin;
    /*
      formula: if (Trader's money / value of all short position) x 100% < maintenance margin,
      then margin call is triggered
    */
  }

  /* short the shares */
  protected void shortSell(double volume) {
    if (shares < 0) {
      updateMarginAccount(Math.abs(shares) + volume);
    } else {
      initiateMarginAccount(volume);
    }

    getDoubleAccumulator("shorts").add(volume);
    getLinks(TradeLink.class).send(Messages.ShortSellOrderPlaced.class, volume);

    sell(volume);
  }

  /* update the margin account */
  protected void updateMarginAccount(double volume) {
    marginAccount = volume * getMarketPrice() * (1 + initialMarginRequirement);
  }

  /* sell the shares */
  protected void sell(double volume) {
    getDoubleAccumulator("sells").add(volume);
    getLinks(TradeLink.class).send(Messages.SellOrderPlaced.class, volume);

    wealth += getMarketPrice() * volume;
    shares -= volume;
  }

  /* check if there are enough shares held */
  protected boolean hasEnoughShares(double volume) {
    return shares >= volume;
  }

  /* check if there are enough wealth owned*/
  protected boolean hasEnoughWealth(double totalValueOfShares) {
    return wealth >= totalValueOfShares;
  }
}
