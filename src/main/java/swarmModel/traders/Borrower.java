package swarmModel.traders;

import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.abm.Action;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;
import swarmModel.links.Links;
import swarmModel.links.Messages;
import swarmModel.utils.Option;

public abstract class Borrower extends OptionTrader {

  @Variable
  public double amountBorrowed = 0;

  @Variable
  public double accruedInterest = 0;

  RandomGenerator random;
  public double amountToBorrow = 0;
  private int interestRepaymentStep = 5;
  public boolean isTrading = true;
  public boolean canBorrow = true;

  @Override
  public void init() {
    super.init();
    random = this.getPrng().generator;
    interestRepaymentStep = getPrng().generator.nextInt(8) + 5;
  }

  private static Action<Borrower> action(SerializableConsumer<Borrower> consumer) {
    return Action.create(Borrower.class, consumer);
  }

  public static Action<Borrower> processBorrowing() {
    return action(trader -> {
      // If we do have negative capital after our trading activity, borrow money
      trader.getLinks(Links.BorrowLink.class).send(Messages.BorrowRequest.class, (msg, link) -> {
        msg.borrowAmount = Math.abs(trader.amountToBorrow);
      });

      // We pay back part of our loan every few time steps
      if (trader.getContext().getTick() % trader.interestRepaymentStep == 0) {
        trader.payBackLoan();
      }
    });
  }

  public static Action<Borrower> actOnLoan() {
    return action(trader -> {
      double loan = trader.getMessagesOfType(Messages.BorrowOutcome.class).stream()
          .mapToDouble(m -> m.lendAmount)
          .sum();

      boolean bankLending = trader.getMessagesOfType(Messages.BorrowOutcome.class).stream()
          .allMatch(
              m -> m.isLending
          );

      // Update amount borrowed to account for interest
      trader.accruedInterest += (trader.amountBorrowed * (trader.getLendingRate() / 365));
      trader.amountBorrowed += loan;
      trader.capital += loan;
      trader.canBorrow = bankLending;

      trader.amountToBorrow = 0;
    });
  }

  private void payBackLoan() {

    // The traders first priority is to pay back the interest on the loan every few steps
    double interestPaid = payBackInterest();

    // Then we pay back what we can of the loan itself using a part of the profit made
    double paidBackLoan = payBackProportionOfLoan();

    getLinks(Links.BorrowLink.class).send(Messages.PayBackLoan.class, (msg, link) -> {
      msg.originalLoanRepayment = paidBackLoan;
      msg.interestPaidBack = interestPaid;
    });
  }

  private double payBackInterest() {
    double interestPaidBack = 0;
    if (accruedInterest > 0) {
      if (capital > accruedInterest) {
        capital -= accruedInterest;
        interestPaidBack = accruedInterest;
        accruedInterest = 0;
      } else if (capital < accruedInterest) {
        capital = 0;
        accruedInterest -= capital;
        interestPaidBack += capital;
        if (shares * getGlobals().marketPrice > accruedInterest) {
          sharesToSell += (Math.floor(accruedInterest / getGlobals().marketPrice));
          accruedInterest = 0;
          interestPaidBack += accruedInterest;
        }
      }
    }
    return interestPaidBack;
  }

  private double payBackProportionOfLoan() {
    double paidBack = 0;

    // Is in insurmountable debt, stop trading completely
    if (!canBorrow && portfolio < 0) {
      isTrading = false;
    }

    if (amountBorrowed > 0) {
      double priceChangePrediction = getGlobals().projectedPrice / getGlobals().marketPrice;

      // Price is predicted to increase
      if (priceChangePrediction > 1) {

        if (capital >= 0) {
          double increaseFromInterest = 1 + getGlobals().interestRate;

          if (increaseFromInterest < priceChangePrediction) {
            // Price is expected to rise more than interestRates so use capital to pay back
            if (shares > 0) {
              paidBack += Math.min(amountBorrowed / 2, capital / 2);
              capital -= Math.min(amountBorrowed / 2, capital / 2);
            }
            // We have a short position so we choose to cover this first with our capital
            else {
              double s = Math.floor(capital / getGlobals().marketPrice);
              sharesToBuy += s;
              capital -= s;
            }
          }
          // Interest rates are predicted to be more profitable so sell shares and keep capital
          else {
            if (shares > 0) {
              if (shares * getGlobals().marketPrice > amountBorrowed) {
                sharesToSell += Math.floor(amountBorrowed / getGlobals().marketPrice);
                paidBack += Math.floor(amountBorrowed / getGlobals().marketPrice);
              } else {
                sharesToSell += shares;
                paidBack += Math.floor(shares * getGlobals().marketPrice);
              }
            }
          }
        } else {
          // The trader is already in debt (capital < 0) so do nothing and hold out
        }
      }
      // Price is predicted to decrease so want to hold capital and sell stocks we have
      else {
        // If we have shares we want to sell these to pay back to loan
        if (shares > 0) {
          double s = Math.min(Math.floor(amountBorrowed / getGlobals().marketPrice), shares);
          sharesToSell += s;
          paidBack += s;
        }
        // If we have no shares, do nothing since will gain interest on any capital we may have
      }
    }
    amountBorrowed -= paidBack;
    return paidBack;
  }


  @Override
  public void putValuesUpdate(Option option) {
    if ((option.getExercisePrice() - getGlobals().projectedPrice) * getGlobals().profitFactor
        > (getOptionPremium(option) * getGlobals().interestRate)) {
      if (capital < option.getOptionPrice()) {
        if (!canBorrow) {
          return;
        }
        amountToBorrow += option.getOptionPrice() - capital;
      }
      super.putValuesUpdate(option);
    }
  }

  @Override
  public void callValuesUpdate(Option option) {
    if ((getGlobals().projectedPrice - option.getExercisePrice()) * getGlobals().profitFactor
        > (getOptionPremium(option) * getGlobals().interestRate)) {
      if (capital < option.getOptionPrice()) {
        if (!canBorrow) {
          return;
        }
        amountToBorrow += option.getOptionPrice() - capital;
      }
      super.callValuesUpdate(option);
    }
  }

  @Override
  public void buy(double volume) {
    if (isTrading) {
      if (capital < 0) {
        if (!canBorrow) {
          return;
        }
        amountToBorrow += volume * getGlobals().marketPrice;
      }
      super.buy(volume);
    }
  }

  @Override
  public void sell(double volume) {
    if (isTrading) {
      super.sell(volume);
    }
  }

  private double getLendingRate() {
    return getGlobals().interestRate + getGlobals().interestMargin;
  }

  private double getOptionPremium(Option option) {
    if (option.isCallOption()) {
      // The option is out of the money so its price does not include intrinsic value
      if (option.getExercisePrice() > getGlobals().marketPrice) {
        return option.getOptionPrice();
      }
      return option.getOptionPrice() - getGlobals().optionShareNumber * (getGlobals().marketPrice
          - option
          .getExercisePrice());
    } else {
      //  The option is out of the money so its price does not include intrinsic value
      if (option.getExercisePrice() < getGlobals().marketPrice) {
        return option.getOptionPrice();
      }
      return option.getOptionPrice() - getGlobals().optionShareNumber * (option.getExercisePrice()
          - getGlobals().marketPrice);
    }
  }

}
