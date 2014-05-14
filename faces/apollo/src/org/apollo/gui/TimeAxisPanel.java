package org.apollo.gui;

import java.awt.Event;
import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.sound.sampled.AudioFormat;
import javax.swing.JPanel;

import org.apollo.util.AudioMath;

/**
 * A time scale along the X axis that can be rendered.
 * 
 * Observes a SampledTrackGraphView - keeps time axis consistent with
 * current time scale on the observed SampledTrackGraphView.
 * 
 * @author Brook Novak
 *
 */
public class TimeAxisPanel extends JPanel implements TimelineChangeListener, MouseListener {
	
	private static final long serialVersionUID = 1L;
	
	private SampledTrackGraphView observedSubject;
	
	private TimeAxis timeAxis = new TimeAxis();
	
	public TimeAxisPanel(SampledTrackGraphView graph) {
		assert (graph != null);
		
		observedSubject = graph;
		graph.addTimeScaleChangeListener(this); // observe
		
		// Auto-adjust scale when component resizes.
		this.addComponentListener(new ComponentListener() {

			public void componentHidden(ComponentEvent e) {
			}

			public void componentMoved(ComponentEvent e) {
			}

			public void componentResized(ComponentEvent e) {
				updateAxis();
			}

			public void componentShown(ComponentEvent e) {
			}
		
		});
		
		updateAxis();
	}
	

	public void timelineChanged(Event e) {
		 updateAxis();
	}
	

	private void updateAxis() {
		
		// Is track model even set yet?
		if (observedSubject.getSampledTrackModel() == null ||
				getWidth() == 0) return;
		
		AudioFormat format = observedSubject.getSampledTrackModel().getFormat();
		
		long startTimeMS = AudioMath.framesToMilliseconds(
				observedSubject.getTimeScaleStart(), format);
		
		long timeLengthMS = AudioMath.framesToMilliseconds(
				observedSubject.getTimeScaleLength(), format);
		
		long totalTrackMSLength = AudioMath.framesToMilliseconds(
				observedSubject.getSampledTrackModel().getFrameCount(), format);
		
		timeAxis.setAxis(
				startTimeMS, 
				timeLengthMS, 
				observedSubject.getTimeScaleLength(), 
				totalTrackMSLength, 
				getWidth());
		
		repaint();
		
	}
	
	@Override
	public void paint(Graphics g) {
		timeAxis.paint(g, 0, 0, getHeight(), getBackground());
	}

	/** BELOW: TRAPS MOUSE EVENTS TO GIVE FOCUS */
	public void mouseClicked(MouseEvent e) {
	}
	public void mouseEntered(MouseEvent e) {
	}
	public void mouseExited(MouseEvent e) {
	}
	public void mousePressed(MouseEvent e) {
	}
	public void mouseReleased(MouseEvent e) {
	}
}
