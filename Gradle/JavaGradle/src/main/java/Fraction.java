import java.io.*;

public class Fraction {

  private int numerator, denominator;

  public Fraction() {
    numerator = denominator = 0;
  }

  public void print() {
    System.out.print(numerator + "/" + denominator);
  }

  public void setNumerator(int n) {
    numerator = n;
  }

  public void setDenominator(int d) {
    denominator = d;
  }

  public int getDenominator() {
    return denominator;
  }

  public int getNumerator() {
    return numerator;
  }

  public static void main(String args[]) {
    try {
      // create a new instance
      Fraction frac = new Fraction();

      // set the values with arguments or defaults
      int numerator = args.length > 0 ? Integer.parseInt(args[0]) : 1;
      int denominator = args.length > 1 ? Integer.parseInt(args[1]) : 3;

      frac.setNumerator(numerator);
      frac.setDenominator(denominator);

      // print it
      System.out.print("The fraction is: ");
      frac.print();
      System.out.println("");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
