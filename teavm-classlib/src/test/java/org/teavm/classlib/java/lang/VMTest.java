package org.teavm.classlib.java.lang;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev
 */
public class VMTest {
    @Test
    public void multiArrayCreated() {
        int[][] array = new int[2][3];
        assertEquals(2, array.length);
        assertEquals(3, array[0].length);
        assertEquals(int[][].class, array.getClass());
        assertEquals(int[].class, array[0].getClass());
    }

    @Test
    public void longIntegersMultipied() {
        long a = 1199747L;
        long b = 1062911L;
        assertEquals(1275224283517L, a * b);
        assertEquals(-1275224283517L, a * -b);
        a = 229767376164L;
        b = 907271478890L;
        assertEquals(-5267604004427634456L, a * b);
        assertEquals(5267604004427634456L, a * -b);
    }

    @Test
    public void longIntegersDivided() {
        long a = 12752242835177213L;
        long b = 1062912L;
        assertEquals(11997458712L, a / b);
        assertEquals(-11997458712L, a / -b);
    }

    @Test
    public void longAdditionWorks() {
        long a = 1134903170;
        long b = 1836311903;
        assertEquals(2971215073L, a + b);
    }
}
