package swarmModel;

import java.util.Map.Entry;
import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.Group;
import simudyne.core.abm.Split;
import simudyne.core.annotations.ModelSettings;
import simudyne.core.rng.SeededRandom;
import swarmModel.links.Links;
import swarmModel.links.Links.OpinionLink;
import swarmModel.traders.FundamentalTrader;
import swarmModel.traders.HedgeFund;
import swarmModel.traders.Initiator;
import swarmModel.traders.MarketMaker;
import swarmModel.traders.MomentumTrader;
import swarmModel.traders.NoiseTrader;
import swarmModel.traders.RetailInvestor;

//todo: correct time
@ModelSettings(macroStep = 100, timeUnit = "DAYS", start = "2021-01-01T00:00:00Z", id = "GME_squeeze")
public class TradingModel extends AgentBasedModel<Globals> {

  {
    registerAgentTypes(MarketMaker.class, NoiseTrader.class, MomentumTrader.class,
        FundamentalTrader.class, Exchange.class, HedgeFund.class, Initiator.class,
        RetailInvestor.class);
    registerLinkTypes(Links.TradeLink.class, OpinionLink.class);
    createDoubleAccumulator("buys", "Number of buy orders");
    createDoubleAccumulator("sells", "Number of sell orders");
    createDoubleAccumulator("price", "Price");
    createDoubleAccumulator("shorts", "Number of shorts");
    createDoubleAccumulator("putOptionsBought", "Number of PUT options bought");
    createDoubleAccumulator("callOptionsBought", "Number of CALL options bought");
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
        1);
    Group<Exchange> exchange = generateGroup(Exchange.class, 1);
    //TODO: change these numbers
    Group<HedgeFund> hedgeFundGroup = generateGroup(HedgeFund.class, getGlobals().nbHedgeFunds);
    Group<Initiator> initiatorGroup = generateGroup(Initiator.class, getGlobals().nbInitiators);
    Group<RetailInvestor> retailInvestorGroup = generateGroup(RetailInvestor.class,
        getGlobals().nbRetailInvestors);

    // Setup of trade links
    marketMakerGroup.fullyConnected(noiseTraderGroup, Links.TradeLink.class);
    noiseTraderGroup.fullyConnected(marketMakerGroup, Links.TradeLink.class);
    marketMakerGroup.fullyConnected(fundamentalTraderGroup, Links.TradeLink.class);
    fundamentalTraderGroup.fullyConnected(marketMakerGroup, Links.TradeLink.class);
    marketMakerGroup.fullyConnected(momentumTraderGroup, Links.TradeLink.class);
    momentumTraderGroup.fullyConnected(marketMakerGroup, Links.TradeLink.class);
    marketMakerGroup.fullyConnected(hedgeFundGroup, Links.TradeLink.class);
    hedgeFundGroup.fullyConnected(marketMakerGroup, Links.TradeLink.class);
    marketMakerGroup.fullyConnected(retailInvestorGroup, Links.TradeLink.class);
    retailInvestorGroup.fullyConnected(marketMakerGroup, Links.TradeLink.class);
    marketMakerGroup.fullyConnected(initiatorGroup, Links.TradeLink.class);
    initiatorGroup.fullyConnected(marketMakerGroup, Links.TradeLink.class);

    marketMakerGroup.fullyConnected(exchange, Links.TradeLink.class);
    momentumTraderGroup.fullyConnected(exchange, Links.TradeLink.class);
    noiseTraderGroup.fullyConnected(exchange, Links.TradeLink.class);
    fundamentalTraderGroup.fullyConnected(exchange, Links.TradeLink.class);
    //hedgeFundGroup.fullyConnected(exchange, Links.TradeLink.class);
    retailInvestorGroup.fullyConnected(exchange, Links.TradeLink.class);
    // initiatorGroup.fullyConnected(exchange, Links.TradeLink.class);

    exchange.fullyConnected(momentumTraderGroup, Links.TradeLink.class);
    exchange.fullyConnected(noiseTraderGroup, Links.TradeLink.class);
    exchange.fullyConnected(fundamentalTraderGroup, Links.TradeLink.class);
    exchange.fullyConnected(marketMakerGroup, Links.TradeLink.class);
    //exchange.fullyConnected(hedgeFundGroup, Links.TradeLink.class);
    exchange.fullyConnected(retailInvestorGroup, Links.TradeLink.class);
    //exchange.fullyConnected(initiatorGroup, Links.TradeLink.class);

    // Setup of Opinion Links

    retailInvestorGroup.gridConnected(Links.OpinionLink.class).width(2);
    initiatorGroup.partitionConnected(retailInvestorGroup, Links.OpinionLink.class).shard();

    initiatorGroup.fullyConnected(noiseTraderGroup, Links.OpinionLink.class);
    initiatorGroup.fullyConnected(momentumTraderGroup, Links.OpinionLink.class);
    initiatorGroup.fullyConnected(fundamentalTraderGroup, Links.OpinionLink.class);
    initiatorGroup.fullyConnected(marketMakerGroup, Links.OpinionLink.class);

    super.setup();
  }

  @Override
  public void step() {
    super.step();

    // We update the interest rate every 10 iterations
    if (getContext().getTick() % 5 == 0 && getContext().getTick() > 20) {
      updateInterestRate();
    }
    updateHistoricalPrices();
    run(Exchange.addNetDemand());
    run(Exchange.addTotalDemand());
    run(Exchange.updatePolynomial());
    run(
        Split.create(NoiseTrader.updateOptions(),
            FundamentalTrader.updateOptions(),
            MomentumTrader.updateOptions())
    );

    run(
        Split.create(NoiseTrader.processOptions(),
            FundamentalTrader.processOptions(),
            MomentumTrader.processOptions()),

        MarketMaker.processOptionSales()
    );

    run(
        Split.create(
            RetailInvestor.shareOpinion(),
            Initiator.shareOpinion()),

        Split.create(
            NoiseTrader.processInformation(),
            MomentumTrader.processInformation(),
            FundamentalTrader.processInformation(),
            MarketMaker.processInformation(),
            RetailInvestor.processInformation()),

        Exchange.calculateBuyAndSellPrice(),
        NoiseTrader.updateThreshold()
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
    double volatility = calculateVolatility(20);
    double changeOfRate =
        meanReversionSpeed * (longTermLevel - getGlobals().interestRate) + volatility * dWt;

    getGlobals().interestRate += changeOfRate;
  }

  /* Wiener rate has gaussian increments, ie W_t+u - W_t ~ N(0, u). We take u to be 1 here since every
   *  time we update the interest rate we want to model this as a single time step in the model*/
  private double getWienerRate() {
    SeededRandom r = getContext().getPrng();
    return r.generator.nextGaussian();
  }

  //todo: fix this volatility measure so stops giving 0
  // We use the standard deviation as a measure for the volatility of the price
  private double calculateVolatility(int timeFrame) {
    if (getGlobals().historicalPrices.isEmpty()) {
      return 0;
    }
    double mean = getGlobals().historicalPrices.entrySet().stream()
        .filter(a -> a.getKey() >= getContext().getTick() - timeFrame).mapToDouble(Entry::getValue)
        .average().getAsDouble();
    double squaredDevs = getGlobals().historicalPrices.entrySet().stream()
        .filter(a -> a.getKey() >= getContext().getTick() - timeFrame).mapToDouble(Entry::getValue)
        .map(a -> Math.pow((mean - a), 2)).sum();
    //getGlobals().volatility = Math.sqrt(squaredDevs / timeFrame);
    getGlobals().volatility = 1;
    return getGlobals().volatility;
  }
}
