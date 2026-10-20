package utd.aos.p1.huang;

import java.io.Serializable;
import java.math.BigInteger;

public class Rational implements Serializable {
    public BigInteger numerator;
    public BigInteger denominator;

    public Rational(int numerator, int denominator) {
        this.numerator = BigInteger.valueOf(numerator);
        this.denominator = BigInteger.valueOf(denominator);
    }

    public Rational(BigInteger numerator, BigInteger denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public Rational add(Rational b) {
        if(b.denominator != this.denominator) {
            var thisCopy = new Rational(numerator.multiply(b.denominator), denominator.multiply(b.denominator));
            var bCopy = new Rational(b.numerator.multiply(denominator), b.denominator.multiply(denominator));
            return simplify(new Rational(thisCopy.numerator.add(bCopy.numerator), thisCopy.denominator));
        }

        return simplify(new Rational(numerator.add(b.numerator), denominator));
    }

    public Rational multiply(Rational b) {
        return simplify(new Rational(numerator.multiply(b.numerator), denominator.multiply(b.denominator)));
    }

    public double inexact() {
        return this.numerator.doubleValue() / this.denominator.doubleValue();
    }

    private Rational simplify(Rational r) {
        var divBy = r.numerator.gcd(r.denominator);
        return new Rational(r.numerator.divide(divBy), r.denominator.divide(divBy));
    }

    public String toString() {
        return "{\"numerator\": " + numerator.toString() + ", \"denominator\": " + denominator.toString() + "}";
    }
}
