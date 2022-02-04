package models.swarming;

import models.swarming.Messages.DvTrueValue;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

/* eg. Bloomberg */
public class DataProvider extends Agent<Globals> {

  @Variable
  public double trueValue;

  @Variable
  public double dv_exo;

  /* --------- function definitions --------- */
  private static Action<DataProvider> action(SerializableConsumer<DataProvider> consumer) {
    return Action.create(DataProvider.class, consumer);
  }

  /* update true value V(t) - random walk: */
  public static Action<DataProvider> updateTrueValue =
      action(
          d -> {
            d.getDoubleAccumulator("equilibrium").add(d.trueValue);

            double netDemand = d.getMessageOfType(Messages.NetDemand.class).getBody();

            /* sum of jump size = Y_i * N_t = N(0,s_j) * P(lambda)   Note. s_j maybe == s_v. */
            double sigma_jd = d.getGlobals().sigma_jd;
            double lambda_jd = d.getGlobals().lambda_jd;
            double jumpDiffusionProcess =
                d.getPrng().normal(0, sigma_jd).sample() * d.getPrng().poisson(lambda_jd).sample();

            // how the random walk changes
            double sigma_v = d.getGlobals().sigma_v;
            d.dv_exo = d.getPrng().normal(0, 1).sample() * sigma_v + jumpDiffusionProcess;

            double kyle_lambda = d.getGlobals().lambda * 2/3;
            double dv_endo = kyle_lambda * netDemand;

           /*
               new True value = prev true value + random walk (dv_exo) + market impact (dv_endo)
               V(t) = V(t – 1) + N(0,sd_v) + jump diffusion process + market impact
                    = V(t – 1) + N(0,sd_v) + summation[i, N_t](Y_i) + market impact
            */
            double dv_trueValue = d.dv_exo + dv_endo;
            d.trueValue = d.trueValue + d.dv_exo + dv_endo;
            d.trueValue = d.trueValue < 0 ? 0 : d.trueValue;

            d.getLinks(Links.DataProviderLink.class).send(Messages.TrueValue.class, d.trueValue);
            d.getLinks(Links.SocialNetworkLink.class).send(DvTrueValue.class, dv_trueValue);

          });
}
