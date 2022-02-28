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

  /*********** OPTION PRICING ************/

  public double calculateOptionPrice(Option option) {
    /* Black Scholes Equation: Cost = Stock price * N(d1) - Exercise price * e^(-interestRate * timeToExpiry) * N(d2)
       where N(d1) and N(d2) are cumulative distribution functions for the normal distribution */
    double stockPrice = getGlobals().marketPrice;
    double exercisePrice = option.getExercisePrice();
    double r = getGlobals().interestRate;
    double timeToExpiry = option.getTimeToExpiry();
    // Time to expiry is represented in years for these calculations, each timeStep = 1 day
    //todo: check that steps are 1 day long
    timeToExpiry = timeToExpiry / 365;

    double d1 = (1 / (getGlobals().volatility * Math.sqrt(timeToExpiry))) * (
        Math.log(stockPrice / exercisePrice)
            + (r + Math.pow(getGlobals().volatility, 2)) * timeToExpiry);
    double d2 = d1 - getGlobals().volatility * timeToExpiry;

    if (option.isCallOption()) {
      return stockPrice * getNormalDistribution(d1)
          - exercisePrice * Math.exp(-r * timeToExpiry)
          * d2;
    } else {
      return -d2 * exercisePrice
          * Math.exp(-r * timeToExpiry)
          - (-d1) * stockPrice;
    }
  }

  private double getNormalDistribution(double d) {
    NormalDistribution normalDistribution = new NormalDistribution();
    return normalDistribution.cumulativeProbability(d);
  }
}