package org.apollo.items;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Stroke;

import org.apollo.ApolloSystem;
import org.apollo.gui.Strokes;
import org.apollo.io.IconRepository;
import org.apollo.widgets.SampleRecorder;
import org.expeditee.gui.Browser;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.items.Item;
import org.expeditee.items.ItemParentStateChangedEvent;
import org.expeditee.items.Text;

public class RecordOverdubLauncher extends Item {
		
		private static int WIDTH = 80;
		private static int HEIGHT = 80;
		private static Stroke BORDER_STOKE = Strokes.SOLID_1;
		private int countdown;
		
		private static final Color BASE_COLOR = new Color(255, 100, 100);
		private static final Color HIGHLIGHT_COLOR = new Color(253, 255, 201);
		
		public RecordOverdubLauncher(int countdown) {
			this.setID(getParentOrCurrentFrame().getNextItemID());
			this.countdown = countdown;
		}

		@Override
		public Item copy() {
			RecordOverdubLauncher copy = new RecordOverdubLauncher(countdown);
			
			DuplicateItem(this, copy);
			
			return copy;
		}

		@Override
		public Item merge(Item merger, int mouseX, int mouseY) {
			return null;
		}

		@Override
		public void paint(Graphics2D g) {
			if (Browser._theBrowser == null) return;
			
			Paint restore = g.getPaint();
			
			if (ApolloSystem.useQualityGraphics) {
				GradientPaint gp = new GradientPaint(
						_x + (WIDTH / 2), _y, HIGHLIGHT_COLOR,
						_x + (WIDTH / 2), _y + HEIGHT - (HEIGHT / 5), BASE_COLOR);
				g.setPaint(gp);
			} else {
				g.setColor(BASE_COLOR);
			}
			
			g.fillRect((int)_x, (int)_y, WIDTH, HEIGHT);
			
			g.setPaint(restore);
			
			g.setColor(Color.BLACK);
			g.setStroke(BORDER_STOKE);
			g.drawRect((int)_x, (int)_y, WIDTH, HEIGHT);
			
			IconRepository.getIcon("recordplay.png").paintIcon(
					Browser._theBrowser.getContentPane(), g, getX() + 25, getY() + 25);
			
		}

		@Override
		public void setAnnotation(boolean val) {
		}

		
		@Override
		public int getHeight() {
			return HEIGHT;
		}

		@Override
		public Integer getWidth() {
			return WIDTH;
		}

		@Override
		public void updatePolygon() {
			
			_poly = new Polygon();

			int x = (int)_x;
			int y = (int)_y;
			
			_poly.addPoint(x, y);
			_poly.addPoint(x + WIDTH, y);
			_poly.addPoint(x + WIDTH, y + HEIGHT);
			_poly.addPoint(x, y + HEIGHT);

		}

		@Override
		public void onParentStateChanged(ItemParentStateChangedEvent e) {
			super.onParentStateChanged(e);

			switch (e.getEventType()) {

			case ItemParentStateChangedEvent.EVENT_TYPE_ADDED:
			case ItemParentStateChangedEvent.EVENT_TYPE_ADDED_VIA_OVERLAY:
			case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN:
			case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN_VIA_OVERLAY:
				// TODO: Invoke later for concurrent modification possibility?
				selfDestruct();
				break;

			}
		}
		
		private void selfDestruct() {
			
			Frame parent = getParent();
			parent.removeItem(this);
			
			Frame currentFrame = DisplayIO.getCurrentFrame();
			
			if (currentFrame != null) {
				
				Text source = new Text(currentFrame.getNextItemID());
				source.setParent(currentFrame);
				SampleRecorder destructableRecorder = new SampleRecorder(source, null, true);
				destructableRecorder.setCountdown(countdown);
				destructableRecorder.setPosition((int)_x, (int)_y);
				currentFrame.addAllItems(destructableRecorder.getItems());
				destructableRecorder.commenceOverdubRecording();
				
				
			}
		}
		
		
		
	}
