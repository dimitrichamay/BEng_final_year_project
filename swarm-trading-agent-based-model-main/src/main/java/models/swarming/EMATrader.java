//package models.trading;
//
//import java.util.HashMap;
//import models.trading.Messages.HistoricalPrices;
//import simudyne.core.abm.Action;
//import simudyne.core.annotations.Variable;
//import simudyne.core.functions.SerializableConsumer;
//
//public class EMATrader extends Trader {
//
//  static long tick; /* equal to the global tick. just to avoid ticks being passed around */
//  static long shortTermMALookBackPeriod = 7;
//  static long longTermMALookBackPeriod = 21;
//  static double smoothing = 2;
//  static int crossoverDirection = 0;   /* 1: upward(↗)(buy) | 0: hold | -1: downward(↘)(sell) */
//
//  @Variable
//  public double shortTermSMA;
//
//  @Variable
//  public double longTermSMA;
//
//  @Variable
//  public double shortTermEMA;
//
//  @Variable
//  public double longTermEMA;
//
//  private static Action<EMATrader> action(SerializableConsumer<EMATrader> consumer) {
//    return Action.create(EMATrader.class, consumer);
//  }
//
//  @Override
//  protected double getVolume() {
//    return 0;
//  }
//
//  @Override
//  protected Side getSide() {
//    return null;
//  }
//
//  @Override
//  protected double getAlpha() {
//    System.out.println("EMA Trader no implementation yet");
//    return 0;
//  }
//
////  @Override
////  protected void tradeStrategy() {
////    System.out.println("********* momentum trader strategy *********");
////    System.out.println("Trader id: " + getID());
////    System.out.println("tick: " + tick);
////
////    if (tick > longTermMALookBackPeriod) {
////      System.out.println("can trade");
////      if (getPrng().uniform(0, 1).sample() < getGlobals().probabilityMomentumTrade) {
////
////        int volume = (int) Math.abs(getPrng().normal(0, getGlobals().stdDev).sample());
////        System.out.println("Volume: " + volume);
////
////        switch (crossoverDirection) {
////          case 1:
////            System.out.println("Time to buy");
////            handleWhenBuyShares(volume);
////            break;
////          case -1:
////            System.out.println("Time to sell");
////            handleWhenSellShares(volume);
////            break;
////          case 0:
////            System.out.println("momentum trader holds");
////            hold();
////            break;
////        }
////
////      }
////    }
////  }
//
//  /* ----------- SMA --------------- */
//  private double getShortTermSMA() {
//    return getSMA(shortTermMALookBackPeriod);
//  }
//
//  private double getLongTermSMA() {
//    return getSMA(longTermMALookBackPeriod);
//  }
//
//  private double getSMA(long lookBackPeriod) {
//
//    HashMap<Long, Double> historicalPrices = getMessageOfType(
//        HistoricalPrices.class).historicalPrices;
//
//    double SMA = 0;
//    int count = 0;
//    for (long i = EMATrader.tick; i > EMATrader.tick - lookBackPeriod; i--) {
//      SMA += historicalPrices.get(i);
//      count++;
//    }
//
//    System.out.println("Days counted: " + count);
//    return SMA / lookBackPeriod;
//  }
//
//  /* ----------- EMA --------------- */
//  private double getShortTermEMA() {
//    return getEMA(shortTermMALookBackPeriod, shortTermEMA);
//  }
//
//  private double getLongTermEMA() {
//    return getEMA(longTermMALookBackPeriod, longTermEMA);
//  }
//
//  private double getEMA(long lookBackPeriod, double yesterdayEMA) {
//    double closingPrice = getMessageOfType(Messages.MarketPrice.class).getBody();
//    double multiplier = smoothing / (1 + lookBackPeriod);
//
//    /* EMA = Closing price x multiplier + EMA (previous day) x (1-multiplier) */
//    return closingPrice * multiplier + yesterdayEMA * (1 - multiplier);
//  }
//
//  public static Action<EMATrader> calcMA =
//      action(t -> {
//
//        System.out.println("tick at momentum trader calMA action: " + tick);
//
//        if (EMATrader.tick == longTermMALookBackPeriod) { // first SMA / EMA
//          System.out.println("get first SMA and LMA at tick " + tick);
//          t.shortTermEMA = t.shortTermSMA = t.getShortTermSMA();
//          t.longTermEMA = t.longTermSMA = t.getLongTermSMA();
//        }
//
//        if (tick > longTermMALookBackPeriod) {
//          double curShortTermSMA = t.getShortTermSMA();
//          double curLongTermSMA = t.getLongTermSMA();
////
//////          System.out.println("prev short-term SMA: " + t.shortTermSMA);
//////          System.out.println("prev long-term SMA: " + t.longTermSMA);
//////          System.out.println("cur short-term SMA: " + curShortTermSMA);
//////          System.out.println("cur long-term SMA: " + curLongTermSMA);
////
////          if (t.shortTermSMA < t.longTermSMA && curShortTermSMA >= curLongTermSMA) {
////            t.crossover = 1;
////          } else if (t.shortTermSMA > t.longTermSMA && curShortTermSMA <= curLongTermSMA) {
////            t.crossover = -1;
////          } else {
////            t.crossover = 0;
////          }
////
//          // cur moving averages becomes yesterday moving averages
//          t.shortTermSMA = curShortTermSMA;
//          t.longTermSMA = curLongTermSMA;
//
//          /* --------------------------------------------------- */
//
//          /* EMA version */
//          double curShortTermEMA = t.getShortTermEMA();
//          double curLongTermEMA = t.getLongTermEMA();
//
//          if (t.shortTermEMA < t.longTermEMA && curShortTermEMA >= curLongTermEMA) {
//            crossoverDirection = 1;
//          } else if (t.shortTermEMA > t.longTermEMA && curShortTermEMA <= curLongTermEMA) {
//            crossoverDirection = -1;
//          } else {
//            crossoverDirection = 0;
//          }
//
//          // cur moving averages becomes yesterday moving averages
//          t.shortTermEMA = curShortTermEMA;
//          t.longTermEMA = curLongTermEMA;
//
//        }
//      });
//}
