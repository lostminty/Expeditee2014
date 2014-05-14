package org.expeditee.agents;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.MessageBay;
import org.expeditee.gui.TimeKeeper;
import org.expeditee.items.Item;
import org.expeditee.stats.SessionStats;

/**
 * The framework for loading of agents accounts for two possible constructors.
 * The first takes no parametres and is called when the agent is run without
 * parametres. The second takes a single string parametre which is the
 * constructor called when an agent is run with a parametre.
 * 
 * @author johnathon, mike
 * 
 */
public abstract class DefaultAgent implements Agent {
	public static int AGENTS_RUNNING = 0;
	
	public static final String CLIPBOARD = "Clipboard";

	protected long _timeRemaining = 0;

	// The shortest delay between frames
	public static final long TIMER_RESOLUTION = 10;

	protected Item _clicked = null;

	protected Frame _start = null;

	protected Frame _end = null;

	protected boolean _running = true;

	protected boolean _stop = false;

	protected int _frameCount = 0;

	protected int _itemCount = 0;

	protected TimeKeeper _timer;

	// delay between frames, in ms
	protected long _delay = 0;

	public DefaultAgent(String delay) {
		this();
		try {
			_delay = (int) (Double.parseDouble(delay) * 1000);
		} catch (Exception e) {
		}
	}

	public DefaultAgent() {
		super();
		AGENTS_RUNNING++;
	}
	
	public static boolean isAgentRunning() {
		return AGENTS_RUNNING > 0;
	}

	/**
	 * Performs any post-processing actions, such as displaying a completion
	 * message to the user
	 * 
	 * @param frame
	 *            The starting Frame this Agent was executed on
	 */
	protected void finalise(Frame start) {
		if (_frameCount == 0)
			_frameCount++;

		int framesPerSecond = (int) Math.round(_frameCount
				/ (_timer.getElapsedMillis() / 1000.0));
		String stats = (_itemCount > 0 ? ("Items: " + _itemCount + ", ") : "")
				+ (_frameCount > 1 ? ("Frames: " + _frameCount + ", ") : "")
				+ "Time: " + _timer.getElapsedStringSeconds()
				+ (framesPerSecond > 1 ? (", FPS: " + framesPerSecond) : "");
		String msg = this.getClass().getSimpleName() + " stats- ";

		message(msg + stats);
	}

	/**
	 * Performs any pre-processing of the starting frame, which may include
	 * searching the Frame for tags that determine the Agent behaviour
	 * 
	 * @param start
	 */
	public boolean initialise(Frame init, Item launcher) {
		_start = init;
		_clicked = launcher;
		message("Starting " + this.getClass().getSimpleName() + "...");
		_timer = new TimeKeeper();
		return true;
	}

	public void run() {
		SessionStats.setEnabled(false);
		if (_start != null)
			_start.change();
		_end = process(_start);

		finalise(_start);

		_running = false;
		AGENTS_RUNNING--;

		FrameGraphics.requestRefresh(true);
		SessionStats.setEnabled(true);
	}

	public boolean hasResultFrame() {
		return getResultFrame() != null;
	}
	
	public boolean hasResultString() {
		return false;
	}

	public Frame getResultFrame() {
		return _end;
	}

	/**
	 * Processes the given Frame, behaviour depends on individual
	 * implementation. If this JAG displays any kind of completion Frame to the
	 * user it should be returned from this method, otherwise null can be
	 * returned.
	 * 
	 * @param frame
	 *            the Frame to process
	 * @return The completion Frame to show to the user, or null
	 */
	protected abstract Frame process(Frame frame);

	/**
	 * Displays a message to the user
	 * 
	 * @param message
	 *            The message to display to the user
	 */
	protected void message(String message) {
		MessageBay.displayMessageAlways(message);
	}

	protected void overwriteMessage(String message) {
		MessageBay.overwriteMessage(message);
	}

	public boolean isRunning() {
		return _running;
	}

	public void stop() {
		_timeRemaining = 0;
		_stop = true;
	}

	public void interrupt() {
		if (_timeRemaining > 0)
			_timeRemaining = 0;
		else
			stop();
	}

	/**
	 * Pauses the execution of this Agent for the given time period (in ms.)
	 * This is used for the delay between displaying frames.
	 * 
	 * @param time
	 */
	protected void pause(long time) {
		try {
			if (time < 0)
				_timeRemaining = Long.MAX_VALUE;
			else
				_timeRemaining = time;

			while (_timeRemaining > 0) {
				Thread.yield();
				Thread.sleep(TIMER_RESOLUTION);
				_timeRemaining -= TIMER_RESOLUTION;
			}
		} catch (InterruptedException e) {
		}
	}
}
