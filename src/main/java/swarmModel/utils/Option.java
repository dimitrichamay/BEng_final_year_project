package swarmModel.utils;

import swarmModel.Exchange;
import swarmModel.Globals;

public class Option {

  public enum type {CALL, PUT}

  private double optionPrice = 0;

  //This is represented as the number of steps until it expires
  private int timeToExpiry;

  private double exercisePrice;
  private double initialStockPrice;
  private type optionType;



  public Option(int timeToExpiry, double exercisePrice, type optionType, double initialStockPrice) {
    this.timeToExpiry = timeToExpiry;
    this.exercisePrice = exercisePrice;
    this.optionType = optionType;
    this.initialStockPrice = initialStockPrice;
  }

  public boolean timeStep() {
    timeToExpiry--;
    return timeToExpiry == 0;
  }

  public double getInitialStockPrice() {
    return initialStockPrice;
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

  public boolean isCallOption() {
    return optionType == type.CALL;
  }

  public double getExercisePrice() {
    return exercisePrice;
  }

}
