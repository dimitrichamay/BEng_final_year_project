package models.swarming;

import models.swarming.Links.DataProviderLink;
import models.swarming.Links.HedgeFundLink;
import models.swarming.Links.SocialNetworkLink;
import models.swarming.Links.TradeLink;
import models.swarming.Trader.Side;
import models.swarming.Trader.Type;
import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.Group;
import simudyne.core.abm.Sequence;
import simudyne.core.abm.Split;
import simudyne.core.annotations.ModelSettings;

@ModelSettings(macroStep = 200)
public class SwarmModel extends AgentBasedModel<Globals> {


  /* ------------------- model initialisation -------------------*/
  @Override
  public void init() {
    createDoubleAccumulator("buys", "Number of buy orders");
    createDoubleAccumulator("sells", "Number of sell orders");
    createDoubleAccumulator("shorts", "Number of short sell orders");           /* inclusive */
    createDoubleAccumulator("closeShorts", "Number of short position covered"); /* inclusive */
    createDoubleAccumulator("price", "Market price");
    createDoubleAccumulator("opinions", "Opinions");
    createDoubleAccumulator("equilibrium", "Market equilibrium");
    createDoubleAccumulator("MtDemand", "Momentum getDemand()");
    createDoubleAccumulator("Wealth", "Wealth of traders");
    createDoubleAccumulator("marginAcct", "Margin acct of traders");

    registerAgentTypes(
        Exchange.class, DataProvider.class,
        SocialNetwork.class, Influencer.class,
        FundamentalTrader.class, NoiseTrader.class,
        MomentumTrader.class, CoordinatedTrader.class,
        HedgeFundShortLow.class,
        HedgeFundShortHigh.class,
        HedgeFundLong.class
    );

    registerLinkTypes(Links.TradeLink.class,
        Links.SocialNetworkLink.class,
        Links.DataProviderLink.class,
        Links.HedgeFundLink.class);
  }

  @Override
  public void setup() {

    /* ---------------------- agents ---------------------- */
    Group<SocialNetwork> socialMediaGroup = null;
    if (getGlobals().numSocialNetwork > 0) {
      socialMediaGroup = generateGroup(SocialNetwork.class, getGlobals().numSocialNetwork);
    }

    Group<DataProvider> dataProviderGroup = null;
    if (getGlobals().numDataProvider > 0) {
      dataProviderGroup = generateGroup(DataProvider.class, getGlobals().numDataProvider,
          d -> d.trueValue = getGlobals().trueValue);
      dataProviderGroup.fullyConnected(socialMediaGroup, SocialNetworkLink.class);
    }

    Group<Exchange> exchangeGroup = null;
    if (getGlobals().numExchange > 0) {
      exchangeGroup = generateGroup(Exchange.class, getGlobals().numExchange,
          m -> m.price = getGlobals().marketPrice);
      exchangeGroup.fullyConnected(dataProviderGroup, DataProviderLink.class);
    }

    assert exchangeGroup != null;
    assert dataProviderGroup != null;
    assert socialMediaGroup != null;

    /* ---------------------- Individual traders ---------------------- */

    Trader.initialMarginRequirement = getGlobals().initialMarginRequirement;
    Trader.maintenanceMargin = getGlobals().maintenanceMargin;

    if (getGlobals().numFundamentalTrader > 0) {
      Group<FundamentalTrader> fundamentalTraderGroup = generateGroup(FundamentalTrader.class,
          getGlobals().numFundamentalTrader, t -> {
            t.type = Type.Fundamental;
            t.isLeftMarket = false;
            t.shares = 0;
            t.marginAccount = 0;
            t.wealth = t.getPrng().pareto(getGlobals().wealth_trader, getGlobals().pareto_params)
                .sample();

            /* --- fundamental traders specific ---- */
            t.zScore = t.getPrng().normal(0, 1).sample();
            t.intrinsicValue = getGlobals().trueValue + t.zScore * getGlobals().sigma_u;
          });

      fundamentalTraderGroup.fullyConnected(exchangeGroup, TradeLink.class);
      exchangeGroup.fullyConnected(fundamentalTraderGroup, TradeLink.class);
      dataProviderGroup.fullyConnected(fundamentalTraderGroup, DataProviderLink.class);
    }

    if (getGlobals().numNoiseTrader > 0) {
      Group<NoiseTrader> noiseTraderGroup = generateGroup(NoiseTrader.class,
          getGlobals().numNoiseTrader, t -> {
            t.type = Type.Noise;
            t.shares = 0;
            t.marginAccount = 0;
            t.isLeftMarket = false;
            t.wealth = t.getPrng().pareto(getGlobals().wealth_trader, getGlobals().pareto_params)
                .sample();
          });

      noiseTraderGroup.fullyConnected(exchangeGroup, TradeLink.class);
      exchangeGroup.fullyConnected(noiseTraderGroup, TradeLink.class);
    }

    if (getGlobals().numMomentumTrader > 0) {
      Group<MomentumTrader> momentumTraderGroup = generateGroup(MomentumTrader.class,
          getGlobals().numMomentumTrader, t -> {
            /* ---- basic ------ */
            t.isLeftMarket = false;
            t.shares = 0;
            t.marginAccount = 0;

            /* --- momentum traders specific ---- */
            t.type = Type.Momentum;
            t.wealth = t.getPrng().pareto(getGlobals().wealth_trader, getGlobals().pareto_params)
                .sample();
            t.opinion = t.getPrng().normal(0, 1).sample();
            t.momentum = 0.0;
            t.lastMarketPrice = 0.0F;
            t.mtParams_beta = getGlobals().mtParams_beta * Math.abs(t.opinion);
          });

      momentumTraderGroup.fullyConnected(exchangeGroup, TradeLink.class);
      exchangeGroup.fullyConnected(momentumTraderGroup, TradeLink.class);

      momentumTraderGroup.fullyConnected(socialMediaGroup, SocialNetworkLink.class);
      socialMediaGroup.fullyConnected(momentumTraderGroup, SocialNetworkLink.class);
    }

    if (getGlobals().numCoordinatedTrader > 0) {
      Group<CoordinatedTrader> coordinatedTraderGroup = generateGroup(CoordinatedTrader.class,
          getGlobals().numCoordinatedTrader, t -> {
            t.type = Type.Coordinated;
            t.shares = 0;
            t.marginAccount = 0;
            t.isLeftMarket = false;
            t.wealth = t.getPrng().pareto(getGlobals().wealth_trader, getGlobals().pareto_params)
                .sample();

            /* --- coordinated traders specific ---- */
            t.opinion = getGlobals().ctOpinion;
            t.sigma_ct = getGlobals().sigma_ct;
          });

      coordinatedTraderGroup.fullyConnected(exchangeGroup, TradeLink.class);
      exchangeGroup.fullyConnected(coordinatedTraderGroup, TradeLink.class);

      coordinatedTraderGroup.fullyConnected(socialMediaGroup, SocialNetworkLink.class);
      socialMediaGroup.fullyConnected(coordinatedTraderGroup, SocialNetworkLink.class);
    }

    /* ---------------- Influencer(s) ----------------- */
    if (getGlobals().numInfluencer > 0) {
      Group<Influencer> influencerGroup = generateGroup(Influencer.class,
          getGlobals().numInfluencer,
          i -> {
            i.isTweeted = false;
            i.opinion = getGlobals().influencerOpinion;
            i.hypedPoint = getGlobals().influencerHypePoint;
          });

      influencerGroup.fullyConnected(socialMediaGroup, SocialNetworkLink.class);
      socialMediaGroup.fullyConnected(influencerGroup, SocialNetworkLink.class);
    }

    /* ---------------- Hedge Fund(s) ----------------- */
    if (getGlobals().numHedgeFundShortLow > 0) {
      Group<HedgeFundShortLow> hedgeFundShortLowGroup = generateGroup(HedgeFundShortLow.class,
          getGlobals().numHedgeFundShortLow, h -> {
            /* ---- basics ----*/
            h.type = Type.HedgeFundSL;
            h.shares = 0;
            h.marginAccount = 0;
            h.isLeftMarket = false;

            /* ----- HF short low ---- */
            h.isFirstSSInitiated = false;
            h.isSecondSSInitiated = false;
            h.isSSing = false;

            h.side = Side.SELL;
            h.wealth = getGlobals().wealth_hf;
            h.tickStartSS = 0; // will update when HF starts to short sell
            h.priceToFirstSS = getGlobals().priceToFirstSS_hfShortLow;
            h.tradeVolume = getGlobals().initialTradeVolume_hfShortLow; // change dynamically
            h.ssDuration = getGlobals().ssDuration_hfShortLow;
            h.priceToSecondSS = getGlobals().priceToSecondSS_hfShortLow;
            h.priceToCoverPos = getGlobals().priceToClosePos_hfShortLow;
            h.priceToStopLoss = getGlobals().priceToStopLoss_hfShortLow;
          });

      hedgeFundShortLowGroup.fullyConnected(exchangeGroup, HedgeFundLink.class);
      exchangeGroup.fullyConnected(hedgeFundShortLowGroup, HedgeFundLink.class);

      hedgeFundShortLowGroup.fullyConnected(exchangeGroup, TradeLink.class);
      exchangeGroup.fullyConnected(hedgeFundShortLowGroup, TradeLink.class);

      socialMediaGroup
          .fullyConnected(hedgeFundShortLowGroup, SocialNetworkLink.class);
    }

    if (getGlobals().numHedgeFundShortHigh > 0) {
      Group<HedgeFundShortHigh> hedgeFundShortHighGroup = generateGroup(HedgeFundShortHigh.class,
          getGlobals().numHedgeFundShortHigh, h -> {
            /* ---- basic ---- */
            h.shares = 0;
            h.marginAccount = 0;
            h.isLeftMarket = false;
            h.type = Type.HedgeFundSH;
            h.wealth = getGlobals().wealth_hf;

            /* ----- HF short high ---- */
            h.isSSing = false;
            h.isSSInitiated = false;
            h.tradeVolume = getGlobals().initialTradeVolume_hfShortLow; // change dynamically
            h.ssDuration = getGlobals().ssDuration_hfShortHigh;
            h.priceToSS = getGlobals().priceToSS_hfShortHigh;
            h.priceToClosePos = getGlobals().priceToClosePos_hfShortHigh;
            h.priceToStopLoss = getGlobals().priceToStopLoss_hfShortHigh;
          });

      hedgeFundShortHighGroup.fullyConnected(exchangeGroup, HedgeFundLink.class);
      exchangeGroup.fullyConnected(hedgeFundShortHighGroup, HedgeFundLink.class);

      hedgeFundShortHighGroup.fullyConnected(exchangeGroup, TradeLink.class);
      exchangeGroup.fullyConnected(hedgeFundShortHighGroup, TradeLink.class);

      socialMediaGroup
          .fullyConnected(hedgeFundShortHighGroup, SocialNetworkLink.class);

    }

    if (getGlobals().numHedgeFundLong > 0) {
      Group<HedgeFundLong> hedgeFundLongGroup = generateGroup(HedgeFundLong.class,
          getGlobals().numHedgeFundLong, h -> {
            /* --- basic ----*/
            h.isLeftMarket = false;
            h.shares = 0;
            h.marginAccount = 0;
            h.type = Type.HedgeFundL;

            /* --- HF Long ----*/
            h.priceToBuy = getGlobals().priceToBuy_hfLong;
            h.priceToSell = getGlobals().priceToSell_hfLong;
            h.wealth = getGlobals().wealth_hf;
          });

      hedgeFundLongGroup.fullyConnected(exchangeGroup, HedgeFundLink.class);
      exchangeGroup.fullyConnected(hedgeFundLongGroup, HedgeFundLink.class);

      hedgeFundLongGroup.fullyConnected(exchangeGroup, TradeLink.class);
      exchangeGroup.fullyConnected(hedgeFundLongGroup, TradeLink.class);

      socialMediaGroup.fullyConnected(hedgeFundLongGroup, SocialNetworkLink.class);
    }

    super.setup();
  }

  @Override
  public void step() {
    super.step();

    Sequence subSequencePrice =
        Sequence.create(
            Exchange.sendPriceToTraders,
            Split.create(
                Trader.submitOrders, // individual traders + hedge funds
                MomentumTrader.updateMomentum
            ),
            Exchange.calcPriceImpact,
            DataProvider.updateTrueValue
        );

    /* -------------- Actual Run() --------------- */
    run(
        Sequence.create(
            Split.create(
                Influencer.shareOpinion,
                OpDynTrader.shareOpinion,
                subSequencePrice
            ),
            Split.create(
                FundamentalTrader.updateIntrinsicValue,
                Sequence.create(
                    SocialNetwork.publishOpAndDvTrueValue,
                    Split.create(
                        OpDynTrader.updateOpinion,
                        Influencer.updateOpinion,
                        HedgeFund.updateVolume
                    )
                )
            )
        )
    );

  }
}
