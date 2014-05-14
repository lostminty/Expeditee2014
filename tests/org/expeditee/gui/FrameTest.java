package org.expeditee.gui;

import java.awt.Dimension;
import java.util.Collection;
import java.util.LinkedList;

import junit.framework.TestCase;

import org.expeditee.items.UserAppliedPermission;

public class FrameTest extends TestCase {
	Frame f1, f2, f3,f4, root, f5;
	public void setUp(){
		 f1 = new Frame(); f1.setName("dgad3");
		 f2 = new Frame(); f2.setName("f4df5");
		 f3 = new Frame(); f3.setName("iu5yf4");
		 f4 = new Frame(); f4.setName("r6s3");
		 f5 = new Frame(); f4.setName("r6ssdf456");
		
		 root = new Frame(); root.setName("roottest1");
	}
	
	public void testConstructor() { // the most trivial test in the world
		Frame f = new Frame();
		assertNotNull(f);
	}
	
	public void testEquals1() {
		Frame f1 = new Frame();
		Frame f2 = new Frame();
		assertEquals(f1, f2);
		assertEquals(f2, f1);
	}
	
	public void testEquals2() {
		
		Frame f1 = new Frame();
		f1.setFrameset("safhsfah");
		
		Frame f2 = new Frame();
		
		assertFalse(f1.equals(f2));
		assertFalse(f2.equals(f1));
	}
	
	public void testAddGetSingleOverlay() {
		Frame f = new Frame();
		Collection<Overlay> overlays = f.getOverlays();
		assertNotNull(overlays);
		assertEquals(0, overlays.size());
		
		Overlay o = new Overlay(new Frame(), UserAppliedPermission.none);
		f.addOverlay(o);
		assertEquals(0, overlays.size()); // getOverlays should be immutable
		overlays = f.getOverlays();
		assertNotNull(overlays);
		assertEquals(1, overlays.size());
		assertEquals(o, overlays.iterator().next());
	}
	
	public void testAddGetMultipleOverlays() {
		
		// TODO: REMOVE THIS LINE - THIS SHOULD NOT BE NEEDED - OTHER TEST CAPTURE THIS ERROR
		FrameGraphics.setMaxSize(new Dimension(100,100));
		
		Overlay o1 = new Overlay(f1, UserAppliedPermission.none);
		Overlay o2 = new Overlay(f2, UserAppliedPermission.followLinks);
		Overlay o3 = new Overlay(f3, UserAppliedPermission.copy);
		Overlay o4 = new Overlay(f4, UserAppliedPermission.createFrames);
		
		root.addOverlay(o1);
		
		Collection<Overlay> overlays = root.getOverlays();
		assertNotNull(overlays);
		assertEquals(1, overlays.size());
		assertEquals(o1, overlays.iterator().next());
		
		f3.addOverlay(o2);
		root.addOverlay(o3);
		
		overlays = root.getOverlays();
		assertNotNull(overlays);
		assertEquals(3, overlays.size());
		assertTrue(overlays.contains(o1));
		assertTrue(overlays.contains(o3));
		
		overlays.clear();
		overlays.add(new Overlay(new Frame(), UserAppliedPermission.none));
		overlays.add(new Overlay(new Frame(), UserAppliedPermission.followLinks));
		overlays.add(new Overlay(new Frame(), UserAppliedPermission.copy));
		overlays.add(o4);
		overlays.add(new Overlay(new Frame(), UserAppliedPermission.createFrames));
		overlays.add(new Overlay(new Frame(), UserAppliedPermission.full));
		String framesetname = "X";
		for (Overlay o : overlays) {
			if (o.Frame.getFramesetName() == null) {
				o.Frame.setName(framesetname+"1");
				o.Frame.setName(framesetname + "name"+"1");
				framesetname = framesetname + "X";
				
			}
		}
		root.addAllOverlays(overlays);

		overlays = root.getOverlays();
		assertNotNull(overlays);
		assertEquals(9, overlays.size());
		assertTrue(overlays.contains(o1));
		assertTrue(overlays.contains(o3));
		assertTrue(overlays.contains(o4));
		
	}
	
	public void testClearOverlays() {
		
		// TODO: REMOVE THIS LINE - THIS SHOULD NOT BE NEEDED - OTHER TEST CAPTURE THIS ERROR
		FrameGraphics.setMaxSize(new Dimension(100,100));
		
		Collection<Overlay> overlays = new LinkedList<Overlay>();
		overlays.add(new Overlay(new Frame(), UserAppliedPermission.none));
		overlays.add(new Overlay(new Frame(), UserAppliedPermission.followLinks));
		overlays.add(new Overlay(new Frame(), UserAppliedPermission.copy));
		overlays.add(new Overlay(new Frame(), UserAppliedPermission.createFrames));
		overlays.add(new Overlay(new Frame(), UserAppliedPermission.full));
		String framesetname = "X";
		for (Overlay o : overlays) {
			o.Frame.setName(framesetname + "1");
			o.Frame.setName(framesetname + "name"+ "1");
			framesetname = framesetname + "X"+ "1";
		}
		Frame root = new Frame(); root.setName("roottest1");
		
		root.addAllOverlays(overlays);
		
		root.clearOverlays();
		overlays = root.getOverlays();
		assertNotNull(overlays);
		assertEquals(0, overlays.size());
	}
	
	public void testRemoveOverlay() {
		
		// TODO: REMOVE THIS LINE - THIS SHOULD NOT BE NEEDED - OTHER TEST CAPTURE THIS ERROR
		FrameGraphics.setMaxSize(new Dimension(100,100));
		
		Collection<Overlay> overlays = new LinkedList<Overlay>();
		Overlay toRemove = new Overlay(new Frame(), UserAppliedPermission.followLinks);
		overlays.add(new Overlay(new Frame(), UserAppliedPermission.none));
		overlays.add(toRemove);
		overlays.add(new Overlay(new Frame(), UserAppliedPermission.copy));
		overlays.add(new Overlay(new Frame(), UserAppliedPermission.createFrames));
		overlays.add(new Overlay(new Frame(), UserAppliedPermission.full));
		String framesetname = "X";
		for (Overlay o : overlays) {
			o.Frame.setName(framesetname+ "1");
			o.Frame.setName(framesetname + "name"+ "1");
			framesetname = framesetname + "X";
		}
		
		root.addAllOverlays(overlays);
		
		root.removeOverlay(toRemove.Frame);
		overlays = root.getOverlays();
		assertNotNull(overlays);
		assertEquals(4, overlays.size());
		assertFalse(overlays.contains(toRemove));
	}
	
		
	
	public void testGetOverlaysDeep() {
		
		// TODO: REMOVE THIS LINE - THIS SHOULD NOT BE NEEDED - OTHER TEST CAPTURE THIS ERROR
		FrameGraphics.setMaxSize(new Dimension(100,100));
		
		Overlay o0 = new Overlay(f1, UserAppliedPermission.none);
		Overlay o1 = new Overlay(f1, UserAppliedPermission.full);
		Overlay o2 = new Overlay(f2, UserAppliedPermission.followLinks);
		Overlay o3 = new Overlay(f3, UserAppliedPermission.copy);
		Overlay o4 = new Overlay(f4, UserAppliedPermission.createFrames);
		Overlay o5 = new Overlay(f5, UserAppliedPermission.copy);
		
		f1.addOverlay(o2);
		f2.addOverlay(o3);
		f3.addOverlay(o4);
		
		
		root.addOverlay(o0);
		Collection<Overlay> overlays = root.getOverlays();
		assertNotNull(overlays);
		assertEquals(4, overlays.size());
		assertTrue(overlays.contains(o0));
		assertTrue(overlays.contains(o2));
		assertTrue(overlays.contains(o3));
		assertTrue(overlays.contains(o4));
		
		
		f3.addOverlay(o5);
		root.clearOverlays();
		root.addOverlay(o0);
		overlays = root.getOverlays();
		assertNotNull(overlays);
		assertEquals(5, overlays.size());
		assertTrue(overlays.contains(o0));
		assertTrue(overlays.contains(o2));
		assertTrue(overlays.contains(o3));
		assertTrue(overlays.contains(o4));
		assertTrue(overlays.contains(o5));
		
		root.clearOverlays();
		overlays = root.getOverlays();
		assertNotNull(overlays);
		assertEquals(0, overlays.size());
		
		overlays.clear();
		overlays.add(new Overlay(new Frame(), UserAppliedPermission.none));
		overlays.add(new Overlay(new Frame(), UserAppliedPermission.followLinks));
		overlays.add(new Overlay(new Frame(), UserAppliedPermission.copy));
		overlays.add(new Overlay(new Frame(), UserAppliedPermission.createFrames));
		overlays.add(new Overlay(new Frame(), UserAppliedPermission.full));
		String framesetname = "X";
		for (Overlay o : overlays) {
			if (o.Frame.getFramesetName() == null) {
				o.Frame.setName(framesetname + "1");
				o.Frame.setName(framesetname + "name" + "1");
				framesetname = framesetname + "X" + "1";
				
			}
		}
		root.addAllOverlays(overlays);
		root.addOverlay(o1);

		Collection<Overlay> overlays2 = root.getOverlays();
		assertNotNull(overlays2);
		assertEquals(10, overlays2.size());
		
		
		f2.removeOverlay(f3);
		root.clearOverlays();
		root.addAllOverlays(overlays);
		root.addOverlay(o1);
		
		overlays = root.getOverlays();
		assertNotNull(overlays);
		assertEquals(7, overlays.size());
	}
	
}
