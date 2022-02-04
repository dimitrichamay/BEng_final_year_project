package models.swarming;

import simudyne.core.abm.GlobalState;
import simudyne.core.annotations.Input;

public final class Globals extends GlobalState {

  /* ---------- Number of Agents spawned ----------- */
  @Input(name = "Number of Fundamental Traders")
  public int numFundamentalTrader = 150;

  @Input(name = "Number of Momentum Traders")
  public int numMomentumTrader = 20;

  @Input(name = "Number of Noise Traders")
  public int numNoiseTrader = 15;

  @Input(name = "Number of Coordinated Traders")
  public int numCoordinatedTrader = 20;

  @Input(name = "Number of Social Network")
  public int numSocialNetwork = 1; // expect 1

  @Input(name = "Number of Exchange")
  public int numExchange = 1; // expect 1

  @Input(name = "Number of Data Provider")
  public int numDataProvider = 1; // expect 1

  @Input(name = "Number of Influencer")
  public int numInfluencer = 0; // expect 1

  @Input(name = "Number of Hedge Fund Short Low")
  public int numHedgeFundShortLow = 0; // expect 1

  @Input(name = "Number of Hedge Fund Short High")
  public int numHedgeFundShortHigh = 0; // expect 1

  @Input(name = "Number of Hedge Fund Long")
  public int numHedgeFundLong = 0; // expect 1


  /* -------- Model Parameters ----------- */

  @Input(name = "Market price")
  public float marketPrice = 30.0F;

  @Input(name = "Market True Value")
  public double trueValue = 30.0; /* called v_0 in paper */

  @Input(name = "Tick Count")
  public int tickCount = 0;

  /* aka price elasticity. speed at which the market price converges market equilibrium */
  @Input(name = "Exchange's Lambda")
  public double lambda = 0.5;

  /* for market true value */
  @Input(name = "Standard deviation of Exogenous Effect")
  public double sigma_v = 0.075;

  @Input(name = "Std. dev. of Jump Diffusion")
  public double sigma_jd = 1;

  @Input(name = "Lambda of Jump Diffusion")
  public double lambda_jd = 1;

  /* --------- Trader Parameters --------- */
  @Input(name = "Trader's Wealth")
  public double wealth_trader = 2000;

  @Input(name = "Pareto distribution param")
  public int pareto_params = 3;

  @Input(name = "Initial Margin Requirement")
  public double initialMarginRequirement = 0.5;

  @Input(name = "Maintenance Margin")
  public double maintenanceMargin = 0.3;

  /* --------- Noise Traders Parameters --------- */
  @Input(name = "Demand from Noise Traders")
  public double sigma_n = 5;

  /* 0-1, larger number means a higher probability to trade in step */
  @Input(name = "Probability of Noise Trade")
  public double pNoiseTrade = 1;

  /* --------- Fundamental Traders Parameters --------- */
  @Input(name = "Uncertainty of True Value")
  public double sigma_u = 10;

  @Input(name = "Sensitivity to market")
  public double ftParam_kappa = 0.08; // aka. sensitivity

  /* --------- Momentum Traders Parameters --------- */
  @Input(name = "MT Params: alpha")
  public double mtParams_alpha = 0.5;

  @Input(name = "MT Params: beta")
  public double mtParams_beta = 0.5;

  @Input(name = "MT Params: gamma")
  public double mtParams_gamma = 50;

  @Input(name = "MT Demand Multiplier")
  public double mt_demandMultiplier = 1.05;

  /* 0-1, larger number means a higher probability to trade in step */
  @Input(name = "Probability of Momentum Trade")
  public double pMomentumTrade = 1;

  /* --------- Coordinated Traders Parameters --------- */
  @Input(name = "Standard Deviation of CT")
  public double sigma_ct = 2;   /* for market true value */

  @Input(name = "Opinion of Coordinated T")
  public double ctOpinion = 5;

  @Input(name = "Probability of Coordinated Trade")
  public double pCoordinatedTrade = 1;

  @Input(name = "CT Demand Multiplier")
  public double ct_demandMultiplier = 1.1;

  /* --------- Opinion Dynamics Parameters --------- */
  @Input(name = "vicinity Range")
  public double vicinityRange = 6;  /* smaller the value, more clusters formed */

  @Input(name = "Tick to Start Opinion Dynamics")
  public double tickToStartOpDyn = 15;

  @Input(name = "Weighting for True Value")
  public double weighting_dvTrueValue = 0.03;

  @Input(name = "Confidence Factor's weighting")
  public double weighting_cf = 100;

  /* --------- Influencer Parameters --------- */
  @Input(name = "Opinion of influencer")
  public double influencerOpinion = 7;

  @Input(name = "Influencer's hype point")
  public double influencerHypePoint = 8;

  @Input(name = "Influencer Weighting")
  public double weighting_influencer = 0.5;

  @Input(name = "Influencer's multiplier")
  public double multiplier_influencer = 0.01;

  /* --------- Hedge Fund Parameters --------- */
  @Input(name = "Wealth of Hedge Fund")
  public double wealth_hf = 1000000;

  /* --------- Hedge Fund Short Low Parameters --------- */
  @Input(name = "HF Short Low, Demand Multiplier")
  public double multiplier_hfShortLow = 3;

  @Input(name = "HF Short Low: Trade Volume")
  public double initialTradeVolume_hfShortLow = 10;

  @Input(name = "HF Short Low: Short Duration")
  public double ssDuration_hfShortLow = 2;

  @Input(name = "HF Short Low: Price to 1st Short")
  public double priceToFirstSS_hfShortLow = 40;

  @Input(name = "HF Short Low: Price to 2nd short")
  public double priceToSecondSS_hfShortLow = 70;

  @Input(name = "HF Short Low: Price to Reap Profit")
  public double priceToClosePos_hfShortLow = 0.5;

  @Input(name = "HF Short Low: Price to Stop Loss")
  public double priceToStopLoss_hfShortLow = 100;

  /* --------- Hedge Fund Short High Parameters --------- */
  @Input(name = "HF Short High: Demand Multiplier")
  public double multiplier_hfShortHigh = 2;

  @Input(name = "HF Short High: Short Duration")
  public double ssDuration_hfShortHigh = 2;

  @Input(name = "HF Short High: Price to Short")
  public double priceToSS_hfShortHigh = 340;

  @Input(name = "HF Short High: Price to Cover Pos")
  public double priceToClosePos_hfShortHigh = 10;

  @Input(name = "HF Short High: Price to Stop Loss")
  public double priceToStopLoss_hfShortHigh = 420;

  /* --------- Hedge Fund Long Parameters --------- */
  @Input(name = "HF Long: Price to Sell")
  public double priceToSell_hfLong = 350;

  @Input(name = "HF Long: Price to Buy")
  public double priceToBuy_hfLong = 70;

  @Input(name = "HF Long, Demand Multiplier")
  public double multiplier_hfLong = 20;

}
