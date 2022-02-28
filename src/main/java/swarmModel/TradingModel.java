package swarmModel;

import java.util.Map.Entry;
import java.util.Random;
import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.Group;
import simudyne.core.abm.Split;
import simudyne.core.annotations.ModelSettings;
import swarmModel.links.Links;
import swarmModel.traders.FundamentalTrader;
import swarmModel.traders.MarketMaker;
import swarmModel.traders.MomentumTrader;
import swarmModel.traders.NoiseTrader;

@ModelSettings(macroStep = 100)
public class TradingModel extends AgentBasedModel<Globals> {

  {
    registerAgentTypes(MarketMaker.class, NoiseTrader.class, MomentumTrader.class,
        FundamentalTrader.class, Exchange.class);
    registerLinkTypes(Links.TradeLink.class);
    createDoubleAccumulator("buys", "Number of buy orders");
    createDoubleAccumulator("sells", "Number of sell orders");
    createDoubleAccumulator("price", "Price");
    createDoubleAccumulator("shorts", "Number of shorts");
  }

  @Override
  public void setup() {
    Group<NoiseTrader> noiseTraderGroup = generateGroup(NoiseTrader.class,
        getGlobals().nbNoiseTraders);
    Group<MomentumTrader> momentumTraderGroup = generateGroup(MomentumTrader.class,
        getGlobals().nbMomentumTraders);
    Group<FundamentalTrader> fundamentalTraderGroup = generateGroup(FundamentalTrader.class,
        getGlobals().nbFundamentalTraders);
    Group<MarketMaker> marketMakerGroup = generateGroup(MarketMaker.class,
        getGlobals().nbMarketMakers);
    Group<Exchange> exchange = generateGroup(Exchange.class, 1);

    marketMakerGroup.fullyConnected(exchange, Links.TradeLink.class);
    momentumTraderGroup.fullyConnected(exchange, Links.TradeLink.class);
    noiseTraderGroup.fullyConnected(exchange, Links.TradeLink.class);
    fundamentalTraderGroup.fullyConnected(exchange, Links.TradeLink.class);

    exchange.fullyConnected(momentumTraderGroup, Links.TradeLink.class);
    exchange.fullyConnected(noiseTraderGroup, Links.TradeLink.class);
    exchange.fullyConnected(fundamentalTraderGroup, Links.TradeLink.class);
    exchange.fullyConnected(marketMakerGroup, Links.TradeLink.class);

    super.setup();
  }

  @Override
  public void step() {
    super.step();

    // We update the interest rate every 10 iterations
    if (getContext().getTick() % 10 == 0 && getContext().getTick() > 0) {
      updateInterestRate();
    }
    updateHistoricalPrices();
    run(Exchange.addNetDemand());
    run(Exchange.addTotalDemand());
    run(Exchange.updatePolynomial());
    run(
        Split.create(
            NoiseTrader.processInformation(),
            MomentumTrader.processInformation(),
            FundamentalTrader.processInformation(),
            MarketMaker.processInformation()),
        Exchange.calculateBuyAndSellPrice(),
        Split.create(
            NoiseTrader.updateThreshold(),
            MomentumTrader.updateMarketData(),
            FundamentalTrader.updateMarketData(),
            MarketMaker.updateMarketData())
    );
  }

  public void updateHistoricalPrices() {
    getGlobals().historicalPrices.put(getContext().getTick(), getGlobals().marketPrice);
  }

  // This uses the Vasicek Interest Rate Model, dr_t = a(b-r_t)dt + sigma * dW_t, we look at the UK in this model
  public void updateInterestRate() {
    double meanReversionSpeed = 0.5; // Half life of change / ln(2)
    double longTermLevel = 0.83; // Long term mean interest rate
    double dWt = getWienerRate();
    double volatility = calculateVolatility(10);
    double changeOfRate =
        meanReversionSpeed * (longTermLevel - getGlobals().interestRate) + volatility * dWt;

    getGlobals().interestRate += changeOfRate;
  }

  /* Wiener rate has gaussian increments, ie W_t+u - W_t ~ N(0, u). We take u to be 1 here since every
   *  time we update the interest rate we want to model this as a single time step in the model*/
  private double getWienerRate() {
    Random r = new Random();
    return r.nextGaussian();
  }

  // We use the standard deviation as a measure for the volatility of the price
  private double calculateVolatility(int timeFrame) {
    if (getGlobals().historicalPrices.isEmpty()){
      return 0;
    }
    double mean = getGlobals().historicalPrices.entrySet().stream()
        .filter(a -> a.getKey() >= getContext().getTick() - timeFrame).mapToDouble(Entry::getValue)
        .average().getAsDouble();
    double squaredDevs = getGlobals().historicalPrices.entrySet().stream()
        .filter(a -> a.getKey() >= getContext().getTick() - timeFrame).mapToDouble(Entry::getValue)
        .map(a -> Math.pow((mean - a), 2)).sum();
    getGlobals().volatility = Math.sqrt(squaredDevs / timeFrame);
    return getGlobals().volatility;
  }
}
