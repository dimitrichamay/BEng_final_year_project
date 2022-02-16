package swarmModel;

import java.util.HashMap;
import java.util.Map;
import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.abm.Split;
import simudyne.core.annotations.Constant;
import simudyne.core.annotations.Input;
import simudyne.core.annotations.ModelSettings;
import swarmModel.links.Links;
import swarmModel.traders.FundamentalTrader;
import swarmModel.traders.MarketMaker;
import swarmModel.traders.MomentumTrader;
import swarmModel.traders.NoiseTrader;

@ModelSettings(macroStep = 100)
public class TradingModel extends AgentBasedModel<TradingModel.Globals> {

  public static final class Globals extends GlobalState {

    @Input(name = "Update Frequency")
    public double updateFrequency = 0.01;

    @Constant(name = "Number of Noise Traders")
    public long nbNoiseTraders = 1000;

    @Constant(name = "Number of Momentum Traders")
    public long nbMomentumTraders = 100;

    @Constant(name = "Number of Fundamental Traders")
    public long nbFundamentalTraders = 100;

    @Constant(name = "Number of Market Makers")
    public long nbMarketMakers = 5;

    @Input(name = "Lambda")
    public double lambda = 10;

    @Input(name = "Volatility of Information Signal")
    public double volatilityInfo = 0.001;

    @Input(name = "Momentum: Short Term Average")
    public long shortTermAverage = 7;

    @Input(name = "Momentum: Long Term Average")
    public long longTermAverage = 21;

    @Input(name = "RSI Look Back Period")
    public long rsiPeriod = 14;

    @Input(name = "Overbought Threshold")
    public double overBuyThresh = 70.0;

    @Input(name = "Oversold Threshold")
    public double overSellThresh = 30.0;

    @Input(name = "Custom Momentum Trader Activity")
    public double traderActivity = 0.1;

    @Input(name = "Custom noise trader activity")
    public double noiseActivity = 0.4;

    @Input(name = "Market Price")
    public double marketPrice = 4.0;

    //This can be changed if desired
    @Input(name = "Interest Rate")
    public double interestRate = 0.05;

    public Map<Long, Double> historicalPrices = new HashMap<>();

    public Map<Long, Double> pastNetDemand = new HashMap<>();
    public Map<Long, Double> pastTotalDemand = new HashMap<>();
  }

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

    updateHistoricalPrices();
    run(Exchange.addNetDemand());
    run(Exchange.addTotalDemand());
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
}
