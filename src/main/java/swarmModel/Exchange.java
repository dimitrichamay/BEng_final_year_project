package swarmModel;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;
import swarmModel.links.Links;
import swarmModel.links.Messages;
import swarmModel.links.Messages.BuyOrderPlaced;
import swarmModel.links.Messages.SellOrderPlaced;

public class Exchange extends Agent<Globals> {

  @Variable
  public double price = 15;

  private static Action<Exchange> action(SerializableConsumer<Exchange> consumer) {
    return Action.create(Exchange.class, consumer);
  }

  private int lastNetDemand = 0;

  private int totalDemand = 0;

  public static Action<Exchange> calculateBuyAndSellPrice() {
    return action(
        exchange -> {
          int buys = exchange.getMessagesOfType(BuyOrderPlaced.class).stream()
              .mapToInt(order -> (int) order.volume).sum();
          int sells = exchange.getMessagesOfType(SellOrderPlaced.class).stream()
              .mapToInt(order -> (int) order.volume).sum();
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
            // Initial way to calculate price, to be updated later
            long nbTraders = exchange.getNumberOfTraders();
            double lambda = exchange.getGlobals().lambda;
            double priceChange = (netDemand / (double) nbTraders) / lambda;
            if (exchange.price + priceChange > 0) {
              exchange.price += priceChange;
            } else {
              // Price cannot be negative so we set it as zero and throw an error
              exchange.price = 0;
              System.err.println("Price cannot be negative, company has gone bankrupt");
            }

            exchange.getDoubleAccumulator("price").add(exchange.price);

            exchange.getGlobals().marketPrice = exchange.price;

            // Send latest price change and price to traders
            exchange.getLinks(Links.TradeLink.class)
                .send(Messages.MarketPriceMessage.class, (msg, link) -> {
                  msg.priceChange = priceChange;
                  msg.price = exchange.price;
                });
          }
        });
  }

  public static Action<Exchange> updateDemandPrediction() {
    final WeightedObservedPoints obs = new WeightedObservedPoints();
    final WeightedObservedPoints priceObs = new WeightedObservedPoints();
    return action(exchange -> {
      exchange.getGlobals().pastNetDemand
          .put(exchange.getContext().getTick(), (double) exchange.lastNetDemand);
      exchange.getGlobals().pastTotalDemand
          .put(exchange.getContext().getTick(), (double) exchange.totalDemand);
      if (exchange.getContext().getTick() < exchange.getGlobals().derivativeTimeFrame) {
        return;
      }
      exchange.getGlobals().pastNetDemand.entrySet().stream()
          .filter(a -> a.getKey() >= exchange.getContext().getTick() - exchange
              .getGlobals().derivativeTimeFrame)
          .forEach(a -> obs.add(a.getKey(), a.getValue()));
      final PolynomialCurveFitter fitter = PolynomialCurveFitter
          .create(exchange.getGlobals().orderOfPoly);
      exchange.getGlobals().coeffs = fitter.fit(obs.toList());

      exchange.getGlobals().historicalPrices.entrySet().stream()
          .filter(a -> a.getKey() >= exchange.getContext().getTick() - exchange
              .getGlobals().derivativeTimeFrame)
          .forEach(a -> priceObs.add(a.getKey(), a.getValue()));
      final PolynomialCurveFitter priceFitter = PolynomialCurveFitter
          .create(exchange.getGlobals().orderOfPoly);
      exchange.getGlobals().priceCoeffs = priceFitter.fit(priceObs.toList());
    });
  }

  public long getNumberOfTraders() {
    return getGlobals().nbFundamentalTraders + getGlobals().nbNoiseTraders
        + getGlobals().nbMomentumTraders + getGlobals().nbHedgeFunds
        + getGlobals().nbRetailInvestors;
  }


}