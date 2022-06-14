package swarmModel;

import java.util.HashMap;
import java.util.Map;
import simudyne.core.abm.GlobalState;
import simudyne.core.annotations.Constant;
import simudyne.core.annotations.Input;

public final class Globals extends GlobalState {

  @Constant(name = "Number of Noise Traders")
  public long nbNoiseTraders = 1000;

  @Constant(name = "Number of Momentum Traders")
  public long nbMomentumTraders = 500;

  @Constant(name = "Number of Fundamental Traders")
  public long nbFundamentalTraders = 500;

  @Constant(name = "Number of Retail Investors")
  public long nbRetailInvestors = 1000;

  @Constant(name = "Number of intitiators")
  public long nbInitiators = 10;

  @Constant(name = "Number of Hedge Funds")
  public long nbHedgeFunds = 50;

  @Input(name = "Lambda")
  public double lambda = 10;

  @Input(name = "Volatility")
  public double volatility = 0;

  @Input(name = "Momentum: Short Term Average")
  public long shortTermAveragePeriod = 7;

  @Input(name = "Momentum: Long Term Average")
  public long longTermAveragePeriod = 21;

  @Input(name = "RSI Look Back Period")
  public long rsiPeriod = 14;

  @Input(name = "Overbought Threshold")
  public double overBuyThresh = 75.0;

  @Input(name = "Oversold Threshold")
  public double overSellThresh = 25.0;

  @Input(name = "Custom Momentum Trader Activity")
  public double traderActivity = 0.15;

  @Input(name = "Custom noise trader activity")
  public double noiseActivity = 0.4;

  @Input(name = "Market Price")
  public double marketPrice = 15;

  // The number of shares which an option gives the right to buy/sell
  @Input(name = "Option share number")
  public double optionShareNumber = 100;

  // This can be changed if desired but is set to the interest rate level in Jan 2021
  @Input(name = "Interest Rate")
  public double interestRate = 0.028;

  @Input(name = "Net interest margin")
  public double interestMargin = 0.03;

  // The number of ticks over which the derivative polynomial is fitted for netDemand
  @Input(name = "Derivative time frame")
  public double derivativeTimeFrame = 10;

  @Input(name = "Variable Interest Rates")
  public boolean variableInterestRates = true;

  @Constant(name = "Call option strike price factor")
  public double callStrikeFactor = 0.95;

  @Constant(name = "Put option strike price factor")
  public double putStrikeFactor = 1.05;

  @Constant(name = "Number of shares bought/sold in normal purchase")
  public double stdVolume = 1;

  @Constant(name = "Max number of shares traded on an opinion")
  public double maxSharesTradedOnOpinion = 100;

  @Constant(name = "Max opinion")
  public double maxOpinion = 20;

  @Input(name = "Time to start opinion sharing")
  public double timeToStartOpinionSharing = 20;

  @Input(name = "Time to start crash")
  public double timeToSell = 150;

  @Input(name = "Max capital in the market to lend")
  public double marketMaxCapital = 10000000;

  public Map<Long, Double> historicalPrices = new HashMap<>();
  public double projectedPrice = 15;
  public Map<Long, Double> pastNetDemand = new HashMap<>();
  public Map<Long, Double> pastTotalDemand = new HashMap<>();
  public double[] coeffs = new double[10];

}

