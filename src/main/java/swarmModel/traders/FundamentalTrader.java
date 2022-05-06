package swarmModel.traders;

import simudyne.core.abm.Action;
import simudyne.core.functions.SerializableConsumer;

public class FundamentalTrader extends LiquidityProvider {

  //Helper function for ease of interpretation
  private static Action<FundamentalTrader> action(
      SerializableConsumer<FundamentalTrader> consumer) {
    return Action.create(FundamentalTrader.class, consumer);
  }

  private double rsi = 50;

  public static Action<FundamentalTrader> processInformation() {
    return action(
        trader -> {
          if (trader.getContext().getTick() > trader.getGlobals().rsiPeriod) {
            trader.rsi = trader.calculateRSI();
            if (trader.rsi > trader.getGlobals().overBuyThresh) {
              trader.sell(trader.getGlobals().stdVolume);
            } else if (trader.rsi  < trader.getGlobals().overSellThresh) {
              trader.buy(trader.getGlobals().stdVolume);
            }
          }
          trader.sendShares();
          trader.deltaHedge();
        });
  }


  public double calculateRSI() {
    double[] histPrices = new double[(int) getGlobals().rsiPeriod];
    for (int i = 0; i < getGlobals().rsiPeriod; i++) {
      histPrices[i] = getGlobals().historicalPrices
          .get(getContext().getTick() - (getGlobals().rsiPeriod + 1) + i);
    }

    double[] histReturns = new double[(int) getGlobals().rsiPeriod - 1];

    double cumulativeGain = 0;
    double cumulativeLoss = 0;

    for (int j = 1; j <= getGlobals().rsiPeriod - 1; j++) {
      histReturns[j - 1] = (histPrices[j] - histPrices[j - 1]) / histPrices[j - 1];
      if (histReturns[j - 1] > 0) {
        cumulativeGain += histReturns[j - 1];
      } else if (histReturns[j - 1] < 0) {
        cumulativeLoss += Math.abs(histReturns[j - 1]);
      }
    }

    double avgGainInitial = cumulativeGain / getGlobals().rsiPeriod;
    double avgLossInitial = cumulativeLoss / getGlobals().rsiPeriod;

    double currentPrice = getGlobals().historicalPrices.get(getContext().getTick() - 1);
    double prevPrice = getGlobals().historicalPrices.get(getContext().getTick() - 2);

    double currentReturn = (currentPrice - prevPrice) / prevPrice;

    double currentGain = 0;
    double currentLoss = 0;

    if (currentReturn > 0) {
      currentGain = currentReturn;
    } else if (currentReturn < 0) {
      currentLoss = Math.abs(currentReturn);
    }

    double avgGain =
        ((getGlobals().rsiPeriod - 1) * avgGainInitial + currentGain) / getGlobals().rsiPeriod;
    double avgLoss =
        ((getGlobals().rsiPeriod - 1) * avgLossInitial + currentLoss) / getGlobals().rsiPeriod;

    if (avgLoss == 0) {
      return 100;
    } else {
      return 100 - (100 / (1 + (avgGain / avgLoss)));
    }
  }
}
