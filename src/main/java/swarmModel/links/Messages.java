package swarmModel.links;

import simudyne.core.graph.Message;
import swarmModel.utils.Option;

public class Messages {
  public static class BuyOrderPlaced extends Message {
    public double volume;
    public double buyPrice;
  }

  public static class SellOrderPlaced extends Message {
    public double volume;
    public double sellPrice;
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

  public static class OptionUpdated extends Message{}
}
