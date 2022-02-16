package swarmModel.utils;

public class Option {

  private enum type {CALL, PUT}

  private double optionPrice;

  //This is represented as the number of steps until it expires
  private int timeToExpiry;

  private double exercisePrice;

  public Option(int timeToExpiry, double exercisePrice) {
    optionPrice = calculateOptionPrice();
    this.timeToExpiry = timeToExpiry;
    this.exercisePrice = exercisePrice;
  }

  private double calculateOptionPrice() {
    /* Black Scholes Equation: Cost = Stock price * N(d1) - Exercise price * e^(-interestRate * timeToExpiry) * N(d2)
       where N(d1) and N(d2) are cumulative distribution functions for the normal distribution */
    double nd1 = getNormalDistribution(1);
    double nd2 = getNormalDistribution(2);
    return 0;
  }

  private double getNormalDistribution(int d1ord2) {
    //double d = Math.log()
    return 0;
  }

  public void timeStep() {
    timeToExpiry--;
  }

  public double getOptionPrice() {
    return optionPrice;
  }

  public void setOptionPrice(double optionPrice) {
    this.optionPrice = optionPrice;
  }

  public int getTimeToExpiry() {
    return timeToExpiry;
  }

  public void setTimeToExpiry(int timeToExpiry) {
    this.timeToExpiry = timeToExpiry;
  }

  public double getExercisePrice() {
    return exercisePrice;
  }

  public void setExercisePrice(double exercisePrice) {
    this.exercisePrice = exercisePrice;
  }
}
