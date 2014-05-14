package org.expeditee.simple;

import junit.framework.TestCase;

public class PrimitivesTest extends TestCase {

    private Primitives primitives;

    protected void setUp() throws Exception {
	super.setUp();
	primitives = new Primitives();
    }

    public void testSet() {
	// Variable doesnt exist
	try {
	    primitives.set("$i.Test", "$i.Testing");
	    fail("Exception not thrown for variable that doesnt exist!");
	} catch (Exception e) {
	}
	try {
	    primitives.set("$i.Test", "Test");
	    fail("Exception not thrown for invalid variable!");
	} catch (Exception e) {
	}
	try {
	    primitives.set("$i.Test", "t.Test");
	    fail("Exception not thrown for invalid variable!");
	} catch (Exception e) {
	}
	try {
	    primitives.set("$i.Test", null);
	    fail("Exception not thrown for invalid variable!");
	} catch (Exception e) {
	}
	try {
	    primitives.set("$i.Test", "i.Test");
	    fail("Exception not thrown for invalid variable!");
	} catch (Exception e) {
	}
	try {
	    primitives.set("$i.test", "10");
	    fail("Exception not thrown for invalid variable!");
	} catch (Exception e) {
	}

	// Now test adding some valid variables
	try {
	    primitives.setValue("$i.Test", "10");
	    primitives.set("$i.Test2", "$i.Test");
	    long iTest = primitives.getIntegerValue("$i.Test");
	    assertEquals(iTest, primitives.getIntegerValue("$i.Test2"));
	    primitives.set("$i.Test", "$i.Test");
	    long newITest = primitives.getIntegerValue("$i.Test");
	    assertEquals(iTest, newITest);

	    primitives.set("$r.Test", "$i.Test");
	    assertEquals(primitives.getIntegerValue("$r.Test"), newITest);
	} catch (Exception e) {
	    fail("Exception thrown for valid variables");
	}
    }

    public void testsetValue() {
	// First add a few bad variables
	try {
	    primitives.setValue("$i.Test", "Test");
	    fail("Exception not thrown for invalid variable!");
	} catch (Exception e) {
	}
	try {
	    primitives.setValue("$t.Test", "Test");
	    fail("Exception not thrown for invalid variable!");
	} catch (Exception e) {
	}
	try {
	    primitives.setValue("$i.", "Test");
	    fail("Exception not thrown for invalid variable!");
	} catch (Exception e) {
	}
	try {
	    primitives.setValue("%i.Test", "Test");
	    fail("Exception not thrown for invalid variable!");
	} catch (Exception e) {
	}
	try {
	    primitives.setValue("i.Test", "Test");
	    fail("Exception not thrown for invalid variable!");
	} catch (Exception e) {
	}
	try {
	    primitives.setValue("Test", "Test");
	    fail("Exception not thrown for invalid variable!");
	} catch (Exception e) {
	}

	// Now test adding some valid variables
	try {
	    primitives.setValue("$i.Test", "10");
	    primitives.setValue("$i.Test", "15");
	    assertEquals(primitives.getIntegerValue("$i.Test"), 15);
	    
	    primitives.setValue("$i.Test", "0xff");
	    assertEquals(primitives.getIntegerValue("$i.Test"), 255);

	    primitives.setValue("$r.Test", "10.0");
	    primitives.setValue("$r.Test", "15.0");
	    assertEquals(primitives.getDoubleValue("$r.Test"), 15.0);
	    
	    primitives.setValue("$r.Test", "0xff");
	    assertEquals(primitives.getDoubleValue("$r.Test"), 255.0);

	    primitives.setValue("$b.Test", "True");
	    primitives.setValue("$s.Test", "Testing 1, 2, 3");
	    assertEquals(primitives.size(), 4);
	} catch (Exception e) {
	    fail("Exception thrown for valid variables");
	}
    }

    public void testEqualValues() {
	// Do the basic cases
	try {
	    primitives.setValue("$i.1", "5");
	    primitives.setValue("$i.2", "5");
	    primitives.setValue("$i.3", "-10");
	    assertTrue(primitives.equalValues("$i.1", "$i.1"));
	    assertTrue(primitives.equalValues("$i.3", "$i.3"));
	    assertTrue(primitives.equalValues("$i.1", "$i.2"));
	    assertFalse(primitives.equalValues("$i.1", "$i.3"));
	} catch (Exception e) {
	    fail("1. " + e.getMessage());
	}

	// Different vars and rounding checking
	try {
	    primitives.setValue("$i.4", "-5");
	    primitives.setValue("$r.1", "-5.1");
	    primitives.setValue("$r.5", "-5.0");
	    primitives.setValue("$s.1", "-4.9");
	    primitives.setValue("$s.2", "-5.1");
	    primitives.setValue("$s.3", "-5.6");
	    assertFalse(primitives.equalValues("$i.4", "$r.1"));
	    assertTrue(primitives.equalValues("$i.4", "$r.5"));
	    assertFalse(primitives.equalValues("$r.1", "$s.3"));
	    assertTrue(primitives.equalValues("$r.1", "$s.2"));
	    assertFalse(primitives.equalValues("$r.1", "$s.1"));
	} catch (Exception e) {
	    fail("2. " + e.getMessage());
	}

	// Strings
	try {
	    primitives.setValue("$s.1", "Testing");
	    primitives.setValue("$s.2", "Test");
	    primitives.setValue("$s.3", "Test");
	    assertTrue(primitives.equalValues("$s.1", "$s.1"));
	    assertTrue(primitives.equalValues("$s.2", "$s.3"));
	    assertFalse(primitives.equalValues("$s.1", "$s.3"));
	} catch (Exception e) {
	    fail("2. " + e.getMessage());
	}

	// Adding to variable that doesnt exist
	try {
	    primitives.equalValues("$i.4", "$i.7");
	    fail("7. Variable did not exist but did not throw an exception");
	} catch (Exception e) {
	}

	// Adding a variable that doesnt exist
	try {
	    primitives.equalValues("$i.7", "$i.4");
	    fail("8. Variable did not exist but did not throw an exception");
	} catch (Exception e) {
	}
    }

    public void testAdd() {
	// Do a simple addition with answer var not yet created
	try {
	    primitives.setValue("$i.1", "5");
	    primitives.setValue("$i.2", "15");
	    primitives.add("$i.1", "$i.2", "$i.3");
	    assertEquals(primitives.getIntegerValue("$i.3"), 20);
	} catch (Exception e) {
	    fail("1. " + e.getMessage());
	}

	// With negatives and existing variable for answer
	try {
	    primitives.setValue("$i.4", "-5");
	    primitives.setValue("$i.2", "15");
	    primitives.add("$i.4", "$i.2", "$i.3");
	    assertEquals(primitives.getIntegerValue("$i.3"), 10);
	} catch (Exception e) {
	    fail("2. " + e.getMessage());
	}

	// Make sure it can handle 0 and reuse of variable as answer
	try {
	    primitives.setValue("$i.5", "0");
	    primitives.setValue("$i.2", "15");
	    primitives.add("$i.5", "$i.2", "$i.2");
	    assertEquals(primitives.getIntegerValue("$i.2"), 15);
	} catch (Exception e) {
	    fail("3. " + e.getMessage());
	}

	// Adding a variable to itself
	try {
	    primitives.setValue("$i.6", "22222");
	    primitives.add("$i.6", "$i.6", "$i.6");
	    assertEquals(primitives.getIntegerValue("$i.6"), 44444);
	} catch (Exception e) {
	    fail("4. " + e.getMessage());
	}

	// Adding a reals
	try {
	    primitives.setValue("$r.1", "24.7");
	    primitives.setValue("$r.2", "5");
	    primitives.add("$r.1", "$r.2", "$r.3");
	    assertEquals(primitives.getDoubleValue("$r.3"), 29.7);
	} catch (Exception e) {
	    fail("5. " + e.getMessage());
	}

	// Adding a reals and ints
	try {
	    primitives.setValue("$r.4", "20.7");
	    primitives.setValue("$i.1", "7");
	    primitives.add("$r.4", "$i.1", "$r.5");
	    assertEquals(primitives.getDoubleValue("$r.5"), 27.7);
	} catch (Exception e) {
	    fail("6. " + e.getMessage());
	}

	// Adding to variable that doesnt exist
	try {
	    primitives.add("$i.4", "$i.7", "$i.3");
	    fail("7. Variable did not exist but did not throw an exception");
	} catch (Exception e) {
	}

	// Adding a variable that doesnt exist
	try {
	    primitives.add("$i.7", "$i.4", "$i.3");
	    fail("8. Variable did not exist but did not throw an exception");
	} catch (Exception e) {
	}
    }

    public void testSubtract() {
	// Do a simple addition with answer var not yet created
	try {
	    primitives.setValue("$i.1", "5");
	    primitives.setValue("$i.2", "15");
	    primitives.subtract("$i.2", "$i.1", "$i.3");
	    assertEquals(primitives.getIntegerValue("$i.3"), 10);
	} catch (Exception e) {
	    fail("1. " + e.getMessage());
	}

	// With negatives and existing variable for answer
	try {
	    primitives.setValue("$i.4", "-5");
	    primitives.setValue("$i.2", "15");
	    primitives.subtract("$i.4", "$i.2", "$i.3");
	    assertEquals(primitives.getIntegerValue("$i.3"), -20);
	} catch (Exception e) {
	    fail("2. " + e.getMessage());
	}

	// Make sure it can handle 0 and reuse of variable as answer
	try {
	    primitives.setValue("$i.5", "0");
	    primitives.setValue("$i.2", "15");
	    primitives.subtract("$i.5", "$i.2", "$i.2");
	    assertEquals(primitives.getIntegerValue("$i.2"), -15);
	} catch (Exception e) {
	    fail("3. " + e.getMessage());
	}

	// Subtracting a variable from itself
	try {
	    primitives.setValue("$i.6", "22222");
	    primitives.subtract("$i.6", "$i.6", "$i.6");
	    assertEquals(primitives.getIntegerValue("$i.6"), 0);
	} catch (Exception e) {
	    fail("4. " + e.getMessage());
	}

	// Subtracting from variable that doesnt exist
	try {
	    primitives.subtract("$i.4", "$i.7", "$i.3");
	    fail("5. Variable did not exist but did not throw an exception");
	} catch (Exception e) {
	}

	// Subtracting a variable that doesnt exist
	try {
	    primitives.subtract("$i.7", "$i.4", "$i.3");
	    fail("6. Variable did not exist but did not throw an exception");
	} catch (Exception e) {
	}

	assertEquals(primitives.size(), 6);
    }

    public void testDivide() {
	// Do a simple addition with answer var not yet created
	try {
	    primitives.setValue("$i.1", "15");
	    primitives.setValue("$i.2", "5");
	    primitives.divide("$i.1", "$i.2", "$i.3");
	    assertEquals(primitives.getIntegerValue("$i.3"), 3);
	} catch (Exception e) {
	    fail("1. " + e.getMessage());
	}

	// With negatives and existing variable for answer
	try {
	    primitives.setValue("$i.4", "-100");
	    primitives.setValue("$i.2", "10");
	    primitives.divide("$i.4", "$i.2", "$i.3");
	    assertEquals(primitives.getIntegerValue("$i.3"), -10);
	} catch (Exception e) {
	    fail("2. " + e.getMessage());
	}

	// Make sure it can handle 0 and reuse of variable as answer
	try {
	    primitives.setValue("$i.5", "0");
	    primitives.setValue("$i.2", "15");
	    primitives.divide("$i.5", "$i.2", "$i.2");
	    assertEquals(primitives.getIntegerValue("$i.2"), 0);
	} catch (Exception e) {
	    fail("3. " + e.getMessage());
	}

	// Make sure it can handle 0 and reuse of variable as answer
	try {
	    primitives.setValue("$i.5", "15");
	    primitives.setValue("$i.2", "0");
	    primitives.divide("$i.5", "$i.2", "$i.2");
	    // $i.2 now equals infinity
	    assertEquals(Long.MAX_VALUE, primitives.getIntegerValue("$i.2"));
	} catch (Exception e) {
	    fail("4. " + e.getMessage());
	}

	// divideing a variable from itself
	try {
	    primitives.setValue("$i.6", "22222");
	    primitives.divide("$i.6", "$i.6", "$i.6");
	    assertEquals(primitives.getIntegerValue("$i.6"), 1);
	} catch (Exception e) {
	    fail("5. " + e.getMessage());
	}

	// divideing by variable that doesnt exist
	try {
	    primitives.divide("$i.4", "$i.7", "$i.3");
	    fail("6. Variable did not exist but did not throw an exception");
	} catch (Exception e) {
	}

	// divideing a variable that doesnt exist
	try {
	    primitives.divide("$i.7", "$i.4", "$i.3");
	    fail("7. Variable did not exist but did not throw an exception");
	} catch (Exception e) {
	}

	// Reals and ints
	try {
	    primitives.setValue("$r.1", "71");
	    primitives.setValue("$i.2", "10");
	    primitives.divide("$r.1", "$i.2", "$r.2");
	    assertEquals(primitives.getDoubleValue("$r.2"), 7.1);
	} catch (Exception e) {
	    fail("8. " + e.getMessage());
	}
    }

    public void testMultiply() {
	// Do a simple multiplication with answer var not yet created
	try {
	    primitives.setValue("$i.1", "15");
	    primitives.setValue("$i.2", "5");
	    primitives.multiply("$i.1", "$i.2", "$i.3");
	    assertEquals(primitives.getIntegerValue("$i.3"), 75);
	} catch (Exception e) {
	    fail("1. " + e.getMessage());
	}

	// With negatives and existing variable for answer
	try {
	    primitives.setValue("$i.4", "-100");
	    primitives.setValue("$i.2", "10");
	    primitives.multiply("$i.4", "$i.2", "$i.3");
	    assertEquals(primitives.getIntegerValue("$i.3"), -1000);
	} catch (Exception e) {
	    fail("2. " + e.getMessage());
	}

	// Make sure it can handle 0 and reuse of variable as answer
	try {
	    primitives.setValue("$i.5", "0");
	    primitives.setValue("$i.2", "15");
	    primitives.multiply("$i.5", "$i.2", "$i.2");
	    assertEquals(primitives.getIntegerValue("$i.2"), 0);
	} catch (Exception e) {
	    fail("3. " + e.getMessage());
	}

	// Make sure it can handle 0 and reuse of variable as answer
	try {
	    primitives.setValue("$i.5", "15");
	    primitives.setValue("$i.2", "0");
	    primitives.multiply("$i.2", "$i.5", "$i.5");
	    assertEquals(primitives.getIntegerValue("$i.5"), 0);
	} catch (Exception e) {
	    fail("4. " + e.getMessage());
	}

	// multiplying a variable by itself
	try {
	    primitives.setValue("$i.6", "12");
	    primitives.multiply("$i.6", "$i.6", "$i.6");
	    assertEquals(primitives.getIntegerValue("$i.6"), 144);
	} catch (Exception e) {
	    fail("5. " + e.getMessage());
	}

	// multiplying by variable that doesnt exist
	try {
	    primitives.multiply("$i.4", "$i.7", "$i.3");
	    fail("6. Variable did not exist but did not throw an exception");
	} catch (Exception e) {
	}

	// multiplying a variable that doesnt exist
	try {
	    primitives.multiply("$i.7", "$i.4", "$i.3");
	    fail("7. Variable did not exist but did not throw an exception");
	} catch (Exception e) {
	}

	// Reals and ints
	try {
	    primitives.setValue("$r.1", "2.55");
	    primitives.setValue("$i.2", "100");
	    primitives.multiply("$r.1", "$i.2", "$r.2");
	    assertEquals(2.55 * 100, primitives.getDoubleValue("$r.2"));
	} catch (Exception e) {
	    fail("8. " + e.getMessage());
	}

	// Reals
	try {
	    primitives.setValue("$r.1", "2.5");
	    primitives.setValue("$r.2", "1.5");
	    primitives.multiply("$r.1", "$r.2", "$r.3");
	    assertEquals(2.5 * 1.5, primitives.getDoubleValue("$r.3"));
	} catch (Exception e) {
	    fail("9. " + e.getMessage());
	}
    }

    public void testCompareValues() {
	try {
	    // Do integer tests
	    primitives.setValue("$i.Test1", "10");
	    primitives.setValue("$i.Test2", "15");
	    assertTrue(primitives.compareValues("$i.Test1", "$i.Test2") < 0);
	    primitives.setValue("$i.Test1", "3024");
	    primitives.setValue("$i.Test2", "3024");
	    assertTrue(primitives.compareValues("$i.Test1", "$i.Test2") == 0);
	    primitives.setValue("$i.Test1", "3");
	    primitives.setValue("$i.Test2", "-3");
	    assertTrue(primitives.compareValues("$i.Test1", "$i.Test2") > 0);
	    primitives.setValue("$i.Test1", "-0");
	    primitives.setValue("$i.Test2", "0");
	    assertTrue(primitives.compareValues("$i.Test1", "$i.Test2") == 0);
	    // Do real tests
	    primitives.setValue("$r.Test1", "-0.0");
	    primitives.setValue("$r.Test2", "0");
	    assertTrue(primitives.compareValues("$r.Test1", "$r.Test2") == 0);
	    primitives.setValue("$r.Test1", "-4.5");
	    primitives.setValue("$r.Test2", "-4.49");
	    assertTrue(primitives.compareValues("$r.Test1", "$r.Test2") < 0);
	    primitives.setValue("$r.Test1", "100025.01");
	    primitives.setValue("$r.Test2", "100025");
	    assertTrue(primitives.compareValues("$r.Test1", "$r.Test2") > 0);
	    // Do real vs integer
	    primitives.setValue("$r.Test", "55.5");
	    primitives.setValue("$i.Test", "55.1");
	    assertFalse(primitives.compareValues("$r.Test", "$i.Test") == 0);
	    primitives.setValue("$r.Test", "-55.5");
	    primitives.setValue("$i.Test", "-55");
	    assertFalse(primitives.compareValues("$i.Test", "$r.Test") == 0);
	    primitives.setValue("$r.Test", "-0.1");
	    primitives.setValue("$i.Test", "0");
	    assertFalse(primitives.compareValues("$r.Test", "$i.Test") == 0);
	    primitives.setValue("$r.Test", "-55.0");
	    primitives.setValue("$i.Test", "-55");
	    assertTrue(primitives.compareValues("$i.Test", "$r.Test") == 0);
	    primitives.setValue("$r.Test", "-0.0");
	    primitives.setValue("$i.Test", "0");
	    assertTrue(primitives.compareValues("$r.Test", "$i.Test") == 0);
	    primitives.setValue("$r.Test", "2.1");
	    primitives.setValue("$i.Test", "1.9");
	    assertTrue(primitives.compareValues("$r.Test", "$i.Test") > 0);
	    primitives.setValue("$r.Test", "-4444.999999");
	    primitives.setValue("$i.Test", "-4445");
	    assertTrue(primitives.compareValues("$i.Test", "$r.Test") < 0);
	    // Do some string tests
	    primitives.setValue("$s.Test1", "Testing... 1, 2, 3");
	    primitives.setValue("$s.Test2", "Testing... 1, 2, 3");
	    assertTrue(primitives.compareValues("$s.Test1", "$s.Test2") == 0);
	    primitives.setValue("$s.Test2", "tESTING... 1, 2, 3");
	    assertTrue(primitives.compareValues("$s.Test1", "$s.Test2") < 0);
	    primitives.setValue("$s.Test1", "rob");
	    primitives.setValue("$s.Test2", "michael");
	    assertTrue(primitives.compareValues("$s.Test1", "$s.Test2") > 0);
	} catch (Exception e) {
	    fail(e.getMessage());
	}

	// Try comparing variables that wont compare
	try {
	    primitives.setValue("$s.Test1", "Zero");
	    primitives.setValue("$r.Test2", "0.0");
	    primitives.compareValues("$s.Test1", "$r.Test2");
	    fail("Attempted comparison of text and real but did not throw an exception");
	} catch (Exception e) {
	}
    }
    
    public void testNot() {
	try {
	    // Do integer tests
	    primitives.setValue("$b.Test", "F");
	    primitives.not("$b.Test", "$b.Result");
	    assertTrue(primitives.getBooleanValue("$b.Result"));
	    primitives.setValue("$b.Test", "True");
	    primitives.not("$b.Test", "$b.Result");
	    assertFalse(primitives.getBooleanValue("$b.Result"));
	} catch (Exception e) {
	    fail(e.getMessage());
	}
    }

}
