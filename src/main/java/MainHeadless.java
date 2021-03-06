import java.util.logging.Logger;
import org.slf4j.LoggerFactory;
import simudyne.core.exec.runner.ModelRunner;
import simudyne.core.exec.runner.MultirunController;
import simudyne.core.exec.runner.RunnerBackend;
import simudyne.core.exec.runner.definition.BatchDefinitionsBuilder;
import simudyne.nexus.Server;
import swarmModel.TradingModel;

public class MainHeadless {

  // Runs model with given parameters and puts result in outputs folder for use with python script
  public static void main(String[] args) {
    try {
      RunnerBackend runnerBackend = RunnerBackend.create();
      ModelRunner modelRunner = runnerBackend.forModel(TradingModel.class);

      long seed = 1234;   //((LocalModelRunner) modelRunner).rootBuiltConfig().getLong("seed");
      int n_runs = 1;     //((LocalModelRunner) modelRunner).rootBuiltConfig().getInt("runs");
      long n_ticks = 250;  //((LocalModelRunner) modelRunner).rootBuiltConfig().getLong("ticks");

      BatchDefinitionsBuilder runDefinitionBuilder =
          BatchDefinitionsBuilder.create()
              .forRuns(n_runs)
              .forTicks(n_ticks)
              .forSeeds(seed);


      modelRunner.forRunDefinitionBuilder(runDefinitionBuilder);
      modelRunner.run();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }



}
