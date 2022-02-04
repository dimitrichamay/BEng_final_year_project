package models.swarming;

import models.swarming.Messages.CloseShortPosOrderPlaced;
import models.swarming.Messages.SellOrderPlaced;
import models.swarming.Messages.ShortSellOrderPlaced;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;
import simudyne.core.graph.Message.Double;

/* market only knows the market prices, its demand and supply */
public class Exchange extends Agent<Globals> {

  @Variable
  public float price;

  /* --------- function definitions --------- */
  private static Action<Exchange> action(SerializableConsumer<Exchange> consumer) {
    return Action.create(Exchange.class, consumer);
  }

  /* send the market price to the trader agents */
  public static Action<Exchange> sendPriceToTraders =
      action(e -> {
        e.getGlobals().tickCount++;
        e.getLinks(Links.TradeLink.class).send(Messages.MarketPrice.class, e.price);
        e.getLinks(Links.HedgeFundLink.class).send(Messages.MarketPrice.class, e.price);

      });

  /* process the orders and calculate the new price */
  public static Action<Exchange> calcPriceImpact =
      action(
          e -> {

            double buys = e.getMessagesOfType(Messages.BuyOrderPlaced.class)
                .stream().mapToDouble(Double::getBody).sum();
            double sells = e.getMessagesOfType(SellOrderPlaced.class)
                .stream().mapToDouble(Double::getBody).sum();
            double shorts = e.getMessagesOfType(ShortSellOrderPlaced.class)
                .stream().mapToDouble(Double::getBody).sum();
            double covers = e.getMessagesOfType(CloseShortPosOrderPlaced.class)
                .stream().mapToDouble(Double::getBody).sum();

            double netDemand = buys - sells;

            if (netDemand != 0) {
              double lambda = e.getGlobals().lambda;
              float priceChange = (float) (netDemand * lambda);
              e.price = (e.price + priceChange) < 0 ? 0 : (e.price + priceChange);
            }

            e.getDoubleAccumulator("price").add(e.price);
            e.getLinks(Links.DataProviderLink.class).send(Messages.NetDemand.class, netDemand);
            e.getLinks(Links.HedgeFundLink.class).send(Messages.MarketPrice.class, e.price);

          });
}
