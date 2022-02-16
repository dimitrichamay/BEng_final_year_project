package swarmModel.links;

import simudyne.core.graph.Message;

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
}
