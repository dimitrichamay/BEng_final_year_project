package models.swarming;

import java.util.List;
import simudyne.core.graph.Message;

public class Messages {

  public static class BuyOrderPlaced extends Message.Double {}

  public static class SellOrderPlaced extends Message.Double {}

  public static class ShortSellOrderPlaced extends Message.Double {}

  public static class CloseShortPosOrderPlaced extends Message.Double {}

  public static class MarketPrice extends Message.Float {}

  public static class TraderOpinionShared extends Message.Double {}

  public static class InfluencerOpinionShared extends Message.Double {}

  public static class SocialNetworkOpinion extends Message { List<java.lang.Double> opinionList; }

  public static class InfluencerSocialNetworkOpinion extends Message.Double {}

  public static class TrueValue extends Message.Double {}

  public static class DvTrueValue extends Message.Double {}

  public static class NetDemand extends Message.Double {}
}
