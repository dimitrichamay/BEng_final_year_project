package swarmModel;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.functions.SerializableConsumer;
import swarmModel.links.Links;
import swarmModel.links.Messages;
import swarmModel.utils.Option;

public class Exchange extends Agent<Globals> {

  public double price = 4.0;

  //Helper function for ease of interpretation
  private static Action<Exchange> action(SerializableConsumer<Exchange> consumer) {
    return Action.create(Exchange.class, consumer);
  }

  private int lastNetDemand = 0;

  private int totalDemand = 0;

  public static Action<Exchange> calculateBuyAndSellPrice() {
    return action(
        exchange -> {
          int buys = exchange.getMessagesOfType(Messages.BuyOrderPlaced.class).size();
          int sells = exchange.getMessagesOfType(Messages.SellOrderPlaced.class).size();
          exchange.totalDemand = buys + sells;

          int netDemand = buys - sells;
          exchange.lastNetDemand = netDemand;
          if (netDemand == 0) {
            exchange.getLinks(Links.TradeLink.class)
                .send(Messages.MarketPriceMessage.class, (msg, link) -> {
                  msg.price = exchange.price;
                  msg.priceChange = 0;
                });
          } else {

            //initial way to calculate price, to be updated later
            long nbTraders = exchange.getNumberOfTraders();
            double lambda = exchange.getGlobals().lambda;
            double priceChange = (netDemand / (double) nbTraders) / lambda;
            exchange.price += priceChange;

            exchange.getDoubleAccumulator("price").add(exchange.price);

            exchange.getGlobals().marketPrice = exchange.price;

            //Send latest price change and price to traders
            exchange.getLinks(Links.TradeLink.class)
                .send(Messages.MarketPriceMessage.class, (msg, link) -> {
                  msg.priceChange = priceChange;
                  msg.price = exchange.price;
                });
          }
        });
  }

  public static Action<Exchange> addNetDemand() {
    return action(exchange -> {
      exchange.getGlobals().pastNetDemand
          .put(exchange.getContext().getTick(), (double) exchange.lastNetDemand);
    });
  }

  public static Action<Exchange> addTotalDemand() {
    return action(exchange -> {
      exchange.getGlobals().pastTotalDemand
          .put(exchange.getContext().getTick(), (double) exchange.totalDemand);
    });
  }

  public static Action<Exchange> updatePolynomial() {
    final WeightedObservedPoints obs = new WeightedObservedPoints();
    return action(exchange -> {
      if (exchange.getContext().getTick() < exchange.getGlobals().derivativeTimeFrame) {
        return;
      }
      exchange.getGlobals().pastNetDemand.entrySet().stream()
          .filter(a -> a.getKey() >= exchange.getContext().getTick() - exchange
              .getGlobals().derivativeTimeFrame)
          .forEach(a -> obs.add(a.getKey(), a.getValue()));
      final PolynomialCurveFitter fitter = PolynomialCurveFitter
          .create((int) exchange.getGlobals().derivativeTimeFrame);
      exchange.getGlobals().coeffs = fitter.fit(obs.toList());
    });
  }

  private long getNumberOfTraders() {
    return getGlobals().nbFundamentalTraders + getGlobals().nbNoiseTraders
        + getGlobals().nbMomentumTraders;
  }


}