package org.example.models.trading;

import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.abm.Split;
import simudyne.core.annotations.Constant;
import simudyne.core.annotations.Input;
import simudyne.core.annotations.ModelSettings;

import java.util.Random;

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

        @Input(name = "Fundamental Traders")
        public boolean fundTraders = false;

        @Input(name = "Momentum Traders")
        public boolean momTraders = false;

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



        //TODO: Swap this for seeded
        public double informationSignal = new Random().nextGaussian() * volatilityInfo;

    }

    {
        registerAgentTypes(Market.class, NoiseTrader.class, MomentumTrader.class, FundamentalTrader.class);
        registerLinkTypes(Links.TradeLink.class);
        createLongAccumulator("buys", "Number of buy orders");
        createLongAccumulator("sells", "Number of sell orders");
        createDoubleAccumulator("price", "Price");
    }

    @Override
    public void setup() {
        if (!getGlobals().fundTraders && !getGlobals().momTraders) {
            Group<NoiseTrader> noiseTraderGroup = generateGroup(NoiseTrader.class, getGlobals().nbNoiseTraders);
            Group<Market> marketGroup = generateGroup(Market.class, 1);
            noiseTraderGroup.fullyConnected(marketGroup, Links.TradeLink.class);
            marketGroup.fullyConnected(noiseTraderGroup, Links.TradeLink.class);

        } else if (getGlobals().momTraders && !getGlobals().fundTraders) {
            Group<NoiseTrader> noiseTraderGroup = generateGroup(NoiseTrader.class, getGlobals().nbNoiseTraders);
            Group<MomentumTrader> momentumTraderGroup = generateGroup(MomentumTrader.class, getGlobals().nbMomentumTraders);
            Group<Market> marketGroup = generateGroup(Market.class, 1);
            momentumTraderGroup.fullyConnected(marketGroup, Links.TradeLink.class);
            noiseTraderGroup.fullyConnected(marketGroup, Links.TradeLink.class);
            marketGroup.fullyConnected(momentumTraderGroup, Links.TradeLink.class);
            marketGroup.fullyConnected(noiseTraderGroup, Links.TradeLink.class);

        } else if (!getGlobals().momTraders && getGlobals().fundTraders) {
            Group<NoiseTrader> noiseTraderGroup = generateGroup(NoiseTrader.class, getGlobals().nbNoiseTraders);
            Group<FundamentalTrader> fundamentalTraderGroup = generateGroup(FundamentalTrader.class, getGlobals().nbFundamentalTraders);
            Group<Market> marketGroup = generateGroup(Market.class, 1);
            noiseTraderGroup.fullyConnected(marketGroup, Links.TradeLink.class);
            fundamentalTraderGroup.fullyConnected(marketGroup, Links.TradeLink.class);
            marketGroup.fullyConnected(noiseTraderGroup, Links.TradeLink.class);
            marketGroup.fullyConnected(fundamentalTraderGroup, Links.TradeLink.class);

        } else if (getGlobals().momTraders && getGlobals().fundTraders) {
            Group<NoiseTrader> noiseTraderGroup = generateGroup(NoiseTrader.class, getGlobals().nbNoiseTraders);
            Group<MomentumTrader> momentumTraderGroup = generateGroup(MomentumTrader.class, getGlobals().nbMomentumTraders);
            Group<FundamentalTrader> fundamentalTraderGroup = generateGroup(FundamentalTrader.class, getGlobals().nbFundamentalTraders);
            Group<Market> marketGroup = generateGroup(Market.class, 1);
            momentumTraderGroup.fullyConnected(marketGroup, Links.TradeLink.class);
            noiseTraderGroup.fullyConnected(marketGroup, Links.TradeLink.class);
            fundamentalTraderGroup.fullyConnected(marketGroup, Links.TradeLink.class);
            marketGroup.fullyConnected(momentumTraderGroup, Links.TradeLink.class);
            marketGroup.fullyConnected(noiseTraderGroup, Links.TradeLink.class);
            marketGroup.fullyConnected(fundamentalTraderGroup, Links.TradeLink.class);
        }

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
                Market.calcPriceImpact(),
                Split.create(
                        NoiseTrader.updateThreshold(),
                        MomentumTrader.updateMarketData(),
                        FundamentalTrader.updateMarketData())
        );
    }
}
