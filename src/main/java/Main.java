import swarmModel.TradingModel;
import simudyne.nexus.Server;

public class Main {

  // Opens console window to allow for dynamic running of the model
  public static void main(String[] args) {
    Server.register("Trading Model", TradingModel.class);
    Server.run();
  }
}
