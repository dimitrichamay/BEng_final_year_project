import swarmModel.TradingModel;
import simudyne.nexus.Server;

public class Main {
  public static void main(String[] args) {
    Server.register("Trading Model", TradingModel.class);
    Server.run();
  }
}
