package org.expeditee.items;

import junit.framework.TestCase;

public class LineTest extends TestCase {

	public final void testGetPath() {
		// Test a single line
		Dot[] d = new Dot[] { new Dot(10, 30, 1), new Dot(30, 10, 2),
				new Dot(40, 20, 3), new Dot(20, 40, 4) };

		Line line = new Line(d[0], d[1], 24);
		int[][][] paths = line.getPaths();
		assertEquals(1, paths.length);
		int[][] path = paths[0];
		assertEquals(2, path[0].length);
		if (path[0][0] == 10) {
			assertEquals(30, path[0][1]);
			assertEquals(30, path[1][0]);
			assertEquals(10, path[1][1]);
		} else {
			assertEquals(30, path[0][0]);
			assertEquals(10, path[0][1]);
			assertEquals(10, path[1][0]);
			assertEquals(30, path[1][1]);
		}
		
		Line line2 = new Line(d[1], d[2], 20);
		paths = line2.getPaths();
		assertEquals(1, paths.length);
		path = paths[0];
		assertEquals(3, path[0].length);
		if (path[0][0] == 10) {
			assertEquals(30, path[0][1]);
			assertEquals(40, path[0][2]);
			assertEquals(30, path[1][0]);
			assertEquals(10, path[1][1]);
			assertEquals(20, path[1][2]);
		} else {
			assertEquals(40, path[0][0]);
			assertEquals(30, path[0][1]);
			assertEquals(10, path[0][2]);
			assertEquals(20, path[1][0]);
			assertEquals(10, path[1][1]);
			assertEquals(30, path[1][2]);
		}

		paths = line2.getPaths();
		assertEquals(1, paths.length);
		path = paths[0];
		assertEquals(3, path[0].length);
		if (path[0][0] == 10) {
			assertEquals(30, path[0][1]);
			assertEquals(40, path[0][2]);
			assertEquals(30, path[1][0]);
			assertEquals(10, path[1][1]);
			assertEquals(20, path[1][2]);
		} else {
			assertEquals(40, path[0][0]);
			assertEquals(30, path[0][1]);
			assertEquals(10, path[0][2]);
			assertEquals(20, path[1][0]);
			assertEquals(10, path[1][1]);
			assertEquals(30, path[1][2]);
		}
		// Test a square
		new Line(d[2], d[3], 21);
		new Line(d[3], d[0], 23);
		paths = line.getPaths();
		assertEquals(1, paths.length);
		path = paths[0];
		assertEquals(5, path[0].length);
		
		//Add a connected to one of the corners
		new Line(d[2], new Dot(100,100, 8), 25);
		paths = line.getPaths();
		assertEquals(2, paths.length);
		
	}

}
