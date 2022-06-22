package swarmModel.links;

import simudyne.core.graph.Message;
import swarmModel.utils.Option;

public class Messages {

  public static class BuyOrderPlaced extends Message {
    public double volume;
  }

  public static class SellOrderPlaced extends Message {
    public double volume;
  }

  public static class MarketPriceMessage extends Message {

    public double price;
    public double priceChange;
  }

  public static class PutOptionBought extends Message {

    public Option option;
  }

  public static class CallOptionBought extends Message {

    public Option option;
  }

  public static class OpinionShared extends Message {

    public double opinion;
  }

  public static class BorrowRequest extends Message {
    public double borrowAmount;
  }

  public static class BorrowOutcome extends Message {
    public double lendAmount;
    public boolean isLending;
  }

  public static class PayBackLoan extends Message {
    public double originalLoanRepayment;
    public double interestPaidBack;
  }
}
