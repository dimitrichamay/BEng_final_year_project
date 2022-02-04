package traders;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.functions.SerializableConsumer;

public class Market extends Agent<TradingModel.Globals> {

    double price = 4.0;

    //Helper function for ease of interpretation
    private static Action<Market> action(SerializableConsumer<Market> consumer) {
        return Action.create(Market.class, consumer);
    }

    public static Action<Market> calcPriceImpact() {
        return action(
                market -> {

                    //Aggregate all the buys and sells
                    int buys = market.getMessagesOfType(Messages.BuyOrderPlaced.class).size();
                    int sells = market.getMessagesOfType(Messages.SellOrderPlaced.class).size();

                    //calculate net demand
                    int netDemand = buys - sells;

                    //if there is no demand nothing happens in the step
                    if (netDemand == 0) {
                        market.getLinks(Links.TradeLink.class).send(Messages.MarketPriceMessage.class, (msg, link) -> {
                            msg.price = market.price;
                            msg.priceChange = 0;
                        });

                        //if there is demand then we calculate the price change based on this net demand
                    } else {

                        //constants in our model
                        long nbTraders = market.getGlobals().nbNoiseTraders;
                        double lambda = market.getGlobals().lambda;

                        //calculate price change
                        double priceChange = (netDemand / (double) nbTraders) / lambda;

                        //adjust market price by price change
                        market.price += priceChange;

                        //update accumulator for price
                        market.getDoubleAccumulator("price").add(market.price);

                        //Send latest price change and price to traders
                        market.getLinks(Links.TradeLink.class)
                                .send(Messages.MarketPriceMessage.class, (msg, link) -> {
                                    msg.priceChange = priceChange;
                                    msg.price = market.price;
                                });
                    }
                });
    }
}
