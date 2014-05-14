package org.expeditee.agents;

import org.expeditee.gui.Frame;
import org.expeditee.items.Item;

/**
 * The Interface that all Agents must implement. This interface allows the JAG
 * classes\ to be loaded dynamically at run-time.
 * 
 * @author jdm18
 * 
 */
public interface Agent extends Runnable {
	
	/**
	 * This method should always be called before calling process(). The Frame
	 * passed in should be scanned for any paramters that should be set before
	 * process() is called. If this method returns false then process() should
	 * not be called.
	 * 
	 * @param toInit
	 *            The Frame that has any parameters that need to be set before
	 *            processing begins. It is also the first frame for the agent.
	 * @param launcher
	 *            The item that was clicked on to launch this agent.
	 * @return True if the initialisation executed correctly, False if an error
	 *         occured.
	 */
	public boolean initialise(Frame toInit, Item launcher);

	/**
	 * Specifies whether this Agents produces a Frame of results to be shown to
	 * the user after execution has completed, or if the program should return
	 * to the Frame the user was on when they invoked this Agent.
	 * 
	 * @return True if this Agent produces a results Frame, false otherwise.
	 */
	public boolean hasResultFrame();
	
	public boolean hasResultString();

	/**
	 * Returns the results Frame produced by this Agent. This method is only
	 * called if hasResultFrame() returns true. If hasResultFrame() returns
	 * false, it is expected that this method returns null.
	 * 
	 * @return The result Frame produced by this Agent if there is one, or null
	 *         otherwise.
	 */
	public Frame getResultFrame();

	public abstract boolean isRunning();

	public abstract void stop();

	public abstract void interrupt();
}
