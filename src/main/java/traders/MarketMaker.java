package traders;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;
import traders.TradingModel.Globals;

/* On each time step the market maker updates the current buy and sell price based on
*  the demand in the market. Traders can then buy or sell some number of assets.
*  Orders influence the asset's price (and the market makers behaviour when
*  options are added later and the market maker needs to cover their position) */

public class MarketMaker extends Agent<Globals> {

  @Variable
  public double askPrice = 4.0;
  @Variable
  public double bidPrice = 4.0;

  //The amount of money that the market maker makes from bid-ask spread
  //Does not yet account for short/long positions that may result of uneven demand
  private double capital = 0;
  public double price = 4.0;

  //Whether the marketmaker is long or short. eg position = 5 means they have bought 5
  //more than they sold. position = -1 means they sold 1 more than they had
  private double position = 0;

  //Helper function for ease of interpretation
  private static Action<MarketMaker> action(SerializableConsumer<MarketMaker> consumer) {
    return Action.create(MarketMaker.class, consumer);
  }

  public static Action<MarketMaker> calculateBuyAndSellPrice() {
    return action(
        marketMaker -> {
          int buys = marketMaker.getMessagesOfType(Messages.BuyOrderPlaced.class).size();
          int sells = marketMaker.getMessagesOfType(Messages.SellOrderPlaced.class).size();

          int netDemand = buys - sells;
          if (netDemand == 0){
            marketMaker.getLinks(Links.TradeLink.class).send(Messages.MarketPriceMessage.class, (msg, link) ->{
                  msg.askPrice = marketMaker.askPrice;
                  msg.bidPrice = marketMaker.bidPrice;
                  msg.price = marketMaker.price;
                  msg.priceChange = 0;
            });
            marketMaker.capital += (marketMaker.bidPrice - marketMaker.askPrice) * buys;
          } else {

            //initial way to calculate price, to be updated later
            long nbTraders = marketMaker.getNumberOfTraders();
            double lambda = marketMaker.getGlobals().lambda;
            double priceChange = (netDemand / (double) nbTraders) / lambda;
            marketMaker.price += priceChange;

            marketMaker.position += buys > sells ? (buys - sells) : -(sells - buys);
            marketMaker.capital += Math.min(buys, sells);

            // This is just an initial way of creating a bid/ask spread: will be refined later
            marketMaker.askPrice = marketMaker.price - (marketMaker.price / 1000);
            marketMaker.bidPrice = marketMaker.price + (marketMaker.price / 1000);

            marketMaker.getDoubleAccumulator("price").add(marketMaker.price);
            marketMaker.getDoubleAccumulator("askPrice").add(marketMaker.askPrice);
            marketMaker.getDoubleAccumulator("bidPrice").add(marketMaker.bidPrice);

            //Send latest price change and price to traders
            marketMaker.getLinks(Links.TradeLink.class)
                .send(Messages.MarketPriceMessage.class, (msg, link) -> {
                  msg.priceChange = priceChange;
                  msg.price = marketMaker.price;
                  msg.bidPrice = marketMaker.bidPrice;
                  msg.askPrice = marketMaker.askPrice;
                });
          }
        });
  }

  public static Action<MarketMaker> processOrders() {
    return action(
        marketMaker -> {

        });
  }

  private long getNumberOfTraders(){
    return getGlobals().nbFundamentalTraders + getGlobals().nbNoiseTraders + getGlobals().nbMomentumTraders;
  }


}
