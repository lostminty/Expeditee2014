package org.expeditee.actions;

import junit.framework.TestCase;

import org.expeditee.gui.Browser;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.items.Text;
import org.expeditee.simple.Context;

public class SimpleTest extends TestCase {
	static Browser b;

	Context context;
	
	protected void setUp() throws Exception {
		super.setUp();
		b = Browser.initializeForTesting();
		context = new Context();
	}

	public void runSimpleTest(String testFrameName) {
		String frameName = testFrameName;
		String frameTitle = null;
		try {
			Frame testFrame = FrameIO.LoadFrame(frameName);
			Text runFrameItem = testFrame.getTitleItem();
			runFrameItem.setLink(testFrameName);
			frameTitle = runFrameItem.getFirstLine();
			Simple.RunFrameAndReportError(runFrameItem, context);
			System.out.println(frameName + ": " + frameTitle + " ran OK");
		} catch (Exception e) {
			System.out.println("FAILED TEST: " + e.getMessage());
			fail(frameName + ": " + (frameTitle != null ? frameTitle : "")
					+ " threw unexpected exception");
		}
	}

	public void runExceptionTest(String testFrameName) {
		String frameName = testFrameName;
		String frameTitle = null;
		try {
			Frame testFrame = FrameIO.LoadFrame(frameName);
			Text runFrameItem = testFrame.getTitleItem();
			frameTitle = runFrameItem.getFirstLine();
			Simple.RunFrameAndReportError(runFrameItem, context);

			String message = frameName + ": " + frameTitle
					+ " unexpectedly ran OK";
			System.out.println("FAILED TEST! " + message);
			fail(message);
		} catch (Exception e) {
			System.out.println(frameName + ": "
					+ (frameTitle != null ? frameTitle : "")
					+ " threw expected exception");
		}
	}

	public void testRunFrame() {
		try {
			runExceptionTest("SimpleTest1");
			runExceptionTest("SimpleTest6");
			runSimpleTest("SimpleTest7");
			context.clear();
			runSimpleTest("SimpleTest9");
			//Variables include the automatically created ones
			assertEquals(6, context.size());

		} catch (Exception e) {
			System.out.println("EVALUATION ERROR: " + e.getMessage());
			fail(e.getMessage());
		}
	}

	public void testCallStatement() {
		try {
			runSimpleTest("SimpleTest10");
			assertEquals(24,context.getPrimitives().getIntegerValue("$i.1"));
			assertEquals(42,context.getPrimitives().getIntegerValue("$i.2"));

			runSimpleTest("SimpleTest11");
			assertEquals(context.getPrimitives().getStringValue("$s.1"),
					"String2");
			assertEquals(context.getPrimitives().getStringValue("$s.2"),
					"String1");

			runSimpleTest("SimpleTest13");
			assertEquals(context.getPrimitives().getStringValue("$s.1"),
					"String2");
			assertEquals(context.getPrimitives().getStringValue("$s.2"),
					"String1");

		} catch (Exception e) {
			System.out.println("EVALUATION ERROR: " + e.getMessage());
			fail(e.getMessage());
		}
	}

	public void testExitAllStatement() {
		try {
			runSimpleTest("SimpleTest15");
		} catch (Exception e) {
			System.out.println("EVALUATION ERROR: " + e.getMessage());
			fail(e.getMessage());
		}
	}

	public void testReturnStatement() {
		try {
			runSimpleTest("SimpleTest17");
			assertEquals(context.getPrimitives().getBooleanValue("$b.Flag"),
					true);
		} catch (Exception e) {
			System.out.println("EVALUATION ERROR: " + e.getMessage());
			fail(e.getMessage());
		}
	}

	public void testBreakStatement() {
		try {
			runSimpleTest("SimpleTest23");
			assertEquals(243,context.getPrimitives().getIntegerValue("$i.Result"));
			assertEquals(0,context.getPrimitives().getIntegerValue("$i.Exponent"));

			runSimpleTest("SimpleTest30");
			assertEquals(243,context.getPrimitives().getIntegerValue("$i.Result"));
			assertEquals(0,context.getPrimitives().getIntegerValue("$i.Exponent"));
		} catch (Exception e) {
			System.out.println("EVALUATION ERROR: " + e.getMessage());
			fail(e.getMessage());
		}
	}

	public void testIfStatements() {
		try {
			runExceptionTest("SimpleTest36");
			runSimpleTest("SimpleTest37");
			assertEquals(13, context.getPrimitives().getIntegerValue("$i.Counter"));
		} catch (Exception e) {
			System.out.println("EVALUATION ERROR: " + e.getMessage());
			fail(e.getMessage());
		}
	}

	public void testReadFrame() {
		try {
			runSimpleTest("SimpleTest42");
			assertEquals(25, context.getPrimitives().getIntegerValue(
					"$i.ItemCount"));
			assertEquals(9, context.getPrimitives().getIntegerValue(
					"$i.BodyTextCount"));
			assertEquals(5, context.getPrimitives().getIntegerValue(
					"$i.AnnotationCount"));
			assertEquals(3, context.getPrimitives().getIntegerValue(
					"$i.LinkedCount"));
		} catch (Exception e) {
			System.out.println("EVALUATION ERROR: " + e.getMessage());
			fail(e.getMessage());
		}
	}

	public void testBooleans() {
		try {
			runSimpleTest("SimpleTest42");
			assertEquals(25, context.getPrimitives().getIntegerValue(
					"$i.ItemCount"));
			assertEquals(context.getPrimitives().getIntegerValue(
					"$i.BodyTextCount"), 9);
			assertEquals(context.getPrimitives().getIntegerValue(
					"$i.AnnotationCount"), 5);
			assertEquals(context.getPrimitives().getIntegerValue(
					"$i.LinkedCount"), 3);
		} catch (Exception e) {
			System.out.println("EVALUATION ERROR: " + e.getMessage());
			fail(e.getMessage());
		}
	}

	public void testDefaultParametres() {
		try {
			runSimpleTest("SimpleTest64");
			assertEquals(context.getPrimitives().getIntegerValue("$i."), 7);
		} catch (Exception e) {
			System.out.println("EVALUATION ERROR: " + e.getMessage());
			fail(e.getMessage());
		}
	}
	
	public void testSimpleTestSuite() {
		try {
			Simple.RunSimpleTests("SimpleTestSuite",true,false);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
