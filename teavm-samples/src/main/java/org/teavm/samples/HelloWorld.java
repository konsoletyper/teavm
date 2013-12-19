package org.teavm.samples;

/**
 *
 * @author Alexey Andreev
 */
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, world!");
        System.out.println("Here is the Fibonacci sequence:");
        long a = 0;
        long b = 1;
        for (int i = 0; i < 70; ++i) {
            System.out.println(a);
            long c = a + b;
            a = b;
            b = c;
        }
        System.out.println("And so on...");
    }
}
