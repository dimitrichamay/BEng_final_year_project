package traders;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;
import traders.TradingModel.Globals;

/* On each time step the market maker updates the current buy and sell price based on
 *  the demand in the market. Traders can then buy or sell some number of assets.
 *  Orders influence the asset's price (and the market makers behaviour when
 *  options are added later and the market maker needs to cover their position)
 *  Pricing algorithm reference: https://econweb.ucsd.edu/~rstarr/Shen-StarrMktMaker.pdf */

public class MarketMaker extends Agent<Globals> {

  //The amount of money that the market maker makes from bid-ask spread
  //Does not yet account for short/long positions that may result of uneven demand
  @Variable(name = "Market Maker Capital")
  public double capital = 0;

  //Whether the marketmaker is long or short. eg position = 5 means they have bought 5
  //more than they sold. position = -1 means they sold 1 more than they had
  @Variable(name = "Market Maker Position")
  public double position = 0;

  //The lowest price at which a seller will buy a stock (the price which the market maker sells at)
  public double askPrice = 4.0;
  //The highest price at which a buyer will sell a stock (the price the market maker buys at)
  public double bidPrice = 4.0;
  public double price = 4.0;

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
          if (netDemand == 0) {
            marketMaker.getLinks(Links.TradeLink.class)
                .send(Messages.MarketPriceMessage.class, (msg, link) -> {
                  msg.askPrice = marketMaker.askPrice;
                  msg.bidPrice = marketMaker.bidPrice;
                  msg.price = marketMaker.price;
                  msg.priceChange = 0;
                });
            marketMaker.capital += (marketMaker.askPrice - marketMaker.bidPrice) * buys;
          } else {

            //initial way to calculate price, to be updated later
            long nbTraders = marketMaker.getNumberOfTraders();
            double lambda = marketMaker.getGlobals().lambda;
            double priceChange = (netDemand / (double) nbTraders) / lambda;
            marketMaker.price += priceChange;

            //Amount bought by market maker - amount sold by market maker
            marketMaker.position += sells - buys;
            // Market maker buys at bid price and sells at the ask price
            marketMaker.capital += buys * marketMaker.askPrice - sells * marketMaker.bidPrice;

            // This is just an initial way of creating a bid/ask spread: will be refined later
            // increases bid/ask price depending on demand to try and keep position constant
            // increase price buyers can buy at if netDemand > 0 and decrease price buyers can sell at if netDemand < 0
            if (netDemand > 0) {
              marketMaker.bidPrice = marketMaker.price - (marketMaker.price / 100);
              marketMaker.askPrice = marketMaker.price + (marketMaker.price / (100 - marketMaker
                  .normaliseDemand(netDemand)));
            } else {
              marketMaker.bidPrice = marketMaker.price - (marketMaker.price / (100 - marketMaker
                  .normaliseDemand(netDemand)));
              marketMaker.askPrice = marketMaker.price + (marketMaker.price / 100);
            }

            marketMaker.getDoubleAccumulator("price").add(marketMaker.price);

            marketMaker.getGlobals().marketPrice = marketMaker.price;
            marketMaker.getGlobals().bidPrice = marketMaker.bidPrice;
            marketMaker.getGlobals().askPrice = marketMaker.askPrice;
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

  //this normalises the values of demand to a value between 0 and 30 (This can be changed as desired)
  private double normaliseDemand(double d) {
    double netDemand = Math.abs(d);
    return netDemand / getNumberOfTraders() * 30;
  }

  private long getNumberOfTraders() {
    return getGlobals().nbFundamentalTraders + getGlobals().nbNoiseTraders
        + getGlobals().nbMomentumTraders;
  }


}
