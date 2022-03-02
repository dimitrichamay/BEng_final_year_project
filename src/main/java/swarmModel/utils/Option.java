package swarmModel.utils;

import swarmModel.Exchange;
import swarmModel.Globals;

public class Option {

  public enum type {CALL, PUT}

  private double optionPrice = 0;

  //This is represented as the number of steps until it expires
  private int timeToExpiry;

  private double exercisePrice;

  private type optionType;

  public Option(int timeToExpiry, double exercisePrice, type optionType) {
    this.timeToExpiry = timeToExpiry;
    this.exercisePrice = exercisePrice;
    this.optionType = optionType;
  }

  public boolean timeStep() {
    timeToExpiry--;
    return timeToExpiry == 0;
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

  public type getOptionType() {
    return optionType;
  }

  public boolean isCallOption() {
    return optionType == type.CALL;
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
