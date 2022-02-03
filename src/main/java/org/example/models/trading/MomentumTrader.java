package org.example.models.trading;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

import java.util.HashMap;
import java.util.Map;


/*
This agent is an example implementation of a moving average trading strategy
*/


public class MomentumTrader extends Agent<TradingModel.Globals> {

    @Variable(name = "Long Term Moving Average")
    public double longTermMovingAvg;

    @Variable(name = "Short Term Moving Average")
    public double shortTermMovingAvg;

    public Map<Long, Double> historicalPrices = new HashMap<>();

    //Helper function for ease of interpretation
    private static Action<MomentumTrader> action(SerializableConsumer<MomentumTrader> consumer) {
        return Action.create(MomentumTrader.class, consumer);
    }


    public static Action<MomentumTrader> processInformation() {
        return action(
                trader -> {
                    if (trader.getContext().getTick() > trader.getGlobals().longTermAverage) {
                        trader.longTermMovingAvg = trader.getTermMovingAvg(trader.getGlobals().longTermAverage);
                        trader.shortTermMovingAvg = trader.getTermMovingAvg(trader.getGlobals().shortTermAverage);
                        double probToBuy = trader.getPrng().uniform(0, 1).sample();

                        if (trader.shortTermMovingAvg > trader.longTermMovingAvg && probToBuy < trader.getGlobals().traderActivity) {
                            trader.buy();
                        } else if ((trader.shortTermMovingAvg < trader.longTermMovingAvg && probToBuy < trader.getGlobals().traderActivity)) {
                            trader.sell();
                        }
                    }
                });
    }

    public static Action<MomentumTrader> updateMarketData() {
        return action(
                trader -> {
                    trader.historicalPrices.put(trader.getContext().getTick(), trader.getMessageOfType(Messages.MarketPriceMessage.class).price);
                });
    }

    public double getTermMovingAvg(long nbDays) {
        double totalPrice = historicalPrices.entrySet().stream().filter(a -> a.getKey() > getContext().getTick() - nbDays).mapToDouble(Map.Entry::getValue).sum();
        return totalPrice / nbDays;
    }

    private void buy() {
        getLongAccumulator("buys").add(1);
        getLinks(Links.TradeLink.class).send(Messages.BuyOrderPlaced.class);
    }

    private void sell() {
        getLongAccumulator("sells").add(1);
        getLinks(Links.TradeLink.class).send(Messages.SellOrderPlaced.class);
    }

}
