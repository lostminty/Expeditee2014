package org.expeditee.simple;

import junit.framework.TestCase;

import org.expeditee.items.Text;

public class ContextTest extends TestCase {

	private Context context;

	protected void setUp() throws Exception {
		super.setUp();
		context = new Context();
		try {
			context.getPrimitives().setValue("$i.test", "5");
			context.getPrimitives().set("$r.test", "$i.test");
			context.getPointers().setObject("$ip.test", new Text(1, "test"));
			context.getPointers().set("$ip.test2", "$ip.test");
		} catch (Exception e) {
		}
	}

	public void testSize() {
		assertEquals(context.size(), 4);
	}

	public void testClear() {
		context.clear();
		assertEquals(context.size(), 0);
	}

	public void testEqualValues() {
		try {
			assertTrue(context.equalValues("$i.test", "$r.Test"));
		} catch (Exception e) {

		}
	}
}
