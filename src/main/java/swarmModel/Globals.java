package swarmModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import simudyne.core.abm.GlobalState;
import simudyne.core.annotations.Constant;
import simudyne.core.annotations.Input;

public final class Globals extends GlobalState {

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

  @Input(name = "Volatility")
  public double volatility = 0;

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

  //This can be changed if desired but is set to the interest rate level in Jan 2021
  @Input(name = "Interest Rate")
  public double interestRate = 0.028;

  //The number of ticks over which the derivative polynomial is fitted for netDemand
  @Input(name = "Derivative time frame")
  public double derivativeTimeFrame = 10;

  public Map<Long, Double> historicalPrices = new HashMap<>();

  public Map<Long, Double> pastNetDemand = new HashMap<>();
  public Map<Long, Double> pastTotalDemand = new HashMap<>();
  public double[] coeffs = new double[10];

}

