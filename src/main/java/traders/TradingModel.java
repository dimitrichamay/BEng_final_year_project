package traders;

import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.abm.Split;
import simudyne.core.annotations.Constant;
import simudyne.core.annotations.Input;
import simudyne.core.annotations.ModelSettings;

import java.util.Random;
import simudyne.core.annotations.Variable;

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

    @Input(name = "Custom Trader Activity")
    public double traderActivity = 0.1;

    @Input(name = "Market Price")
    public double marketPrice = 4.0;

    @Variable(name = "Bid Price")
    public double bidPrice = 4.04;

    @Variable(name = "Ask Price")
    public double askPrice = 3.96;

    //TODO: Swap this for seeded
    public double informationSignal = new Random().nextGaussian() * volatilityInfo;

  }

  {
    registerAgentTypes(MarketMaker.class, NoiseTrader.class, MomentumTrader.class,
        FundamentalTrader.class);
    registerLinkTypes(Links.TradeLink.class);
    createDoubleAccumulator("buys", "Number of buy orders");
    createDoubleAccumulator("sells", "Number of sell orders");
    createDoubleAccumulator("price", "Price");
  }

  @Override
  public void setup() {
    Group<NoiseTrader> noiseTraderGroup = generateGroup(NoiseTrader.class,
        getGlobals().nbNoiseTraders);
    Group<MomentumTrader> momentumTraderGroup = generateGroup(MomentumTrader.class,
        getGlobals().nbMomentumTraders);
    Group<FundamentalTrader> fundamentalTraderGroup = generateGroup(FundamentalTrader.class,
        getGlobals().nbFundamentalTraders);
    Group<MarketMaker> marketMakerGroup = generateGroup(MarketMaker.class, 1);
    momentumTraderGroup.fullyConnected(marketMakerGroup, Links.TradeLink.class);
    noiseTraderGroup.fullyConnected(marketMakerGroup, Links.TradeLink.class);
    fundamentalTraderGroup.fullyConnected(marketMakerGroup, Links.TradeLink.class);
    marketMakerGroup.fullyConnected(momentumTraderGroup, Links.TradeLink.class);
    marketMakerGroup.fullyConnected(noiseTraderGroup, Links.TradeLink.class);
    marketMakerGroup.fullyConnected(fundamentalTraderGroup, Links.TradeLink.class);

    super.setup();
  }

  @Override
  public void step() {
    super.step();

    getGlobals().informationSignal = new Random().nextGaussian() * getGlobals().volatilityInfo;
    run(
        Split.create(
            NoiseTrader.processInformation(),
            MomentumTrader.processInformation(),
            FundamentalTrader.processInformation()),
        MarketMaker.calculateBuyAndSellPrice(),
        Split.create(
            NoiseTrader.updateThreshold(),
            MomentumTrader.updateMarketData(),
            FundamentalTrader.updateMarketData())
    );
  }
}
