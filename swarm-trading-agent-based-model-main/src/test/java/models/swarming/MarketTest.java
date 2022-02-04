//package models.trading;
//
//import models.trading.Messages.MarketPrice;
//import org.junit.Before;
//import org.junit.Test;
//
//import simudyne.core.abm.testkit.TestKit;
//import simudyne.core.abm.testkit.TestResult;
//
//import static org.junit.Assert.assertEquals;
//
//public class MarketTest {
//
//  private TestKit<TradingModel.Globals> testKit;
//  private Market market;
//  public static final int TARGET_LINK_ID = 1;
//
//  @Before
//  public void init() {
//    testKit = TestKit.create(TradingModel.Globals.class);
//    testKit.registerLinkTypes(Links.TradeLink.class);
//
//    testKit.createDoubleAccumulator("price");
//
//    market = testKit.addAgent(Market.class);
//
//    market.addLink(TARGET_LINK_ID, Links.TradeLink.class);
//  }
//
//  @Test
//  public void shouldHandleZeroNetDemand() {
//    testKit.send(Messages.BuyOrderPlaced.class).to(market);
//    testKit.send(Messages.SellOrderPlaced.class).to(market);
//
//    TestResult testResult = testKit.testAction(market, Market.calcPriceImpact());
//    MarketPrice expectedMessage =
//        testResult.getMessagesOfType(MarketPrice.class).get(0);
//
//    assertEquals(0, expectedMessage.getBody(), 0);
//  }
//
//  @Test
//  public void shouldHandlePriceChange() {
//    // Two buys and one sell makes a netDemand of 1
//    testKit.send(Messages.BuyOrderPlaced.class).to(market);
//    testKit.send(Messages.BuyOrderPlaced.class).to(market);
//    testKit.send(Messages.SellOrderPlaced.class).to(market);
//
//    double startingPrice = market.getGlobals().marketPrice;
//
//    TestResult testResult = testKit.testAction(market, Market.calcPriceImpact());
//    MarketPrice expectedMessage =
//        testResult.getMessagesOfType(MarketPrice.class).get(0);
//
//    double expectedPriceChange =
//        (1 / (double) market.numTraders) / testKit.getGlobals().lambda;
//
//    assertEquals(0, expectedMessage.getBody(), startingPrice + expectedPriceChange);
//  }
//}
