package org.expeditee.actions;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import org.expeditee.actions.Simple.Status;
import org.expeditee.agents.Agent;
import org.expeditee.agents.DefaultAgent;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.MessageBay;
import org.expeditee.gui.FrameUtils;
import org.expeditee.items.Item;
import org.expeditee.items.Text;
import org.expeditee.items.Item.HighlightMode;
import org.expeditee.stats.AgentStats;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrappedException;

public class Javascript {

	// Field largely based on Simple.java.  Consider sharing code?

	
	/**
	 * Keeps track of how many simple programs are running. Used to check if
	 * simple should read in keyboard input. Or if the keyboard input should be
	 * handled normally by Expeditee.
	 */
	private static int _programsRunning = 0;

	/**
	 * This flag is set to true if Simple should hijack keyboard input from
	 * Expeditee
	 */
	private static boolean _consumeKeyboardInput = false;

	public static void ProgramFinished() {
		_programsRunning--;
		_stop = false;
	}

	private static LinkedList<Character> _KeyStrokes = new LinkedList<Character>();

	private static boolean _stop;

	private static Agent _agent = null;

	private static boolean _step;

	private static int _stepPause = -1;

	private static Color _stepColor;

	private static boolean _nextStatement;

	public static void KeyStroke(char c) {
		_KeyStrokes.add(c);
	}

	public static boolean isProgramRunning() {
		return _programsRunning > 0;
	}

	public static boolean consumeKeyboardInput() {
		return _consumeKeyboardInput && _programsRunning > 0;
	}

	//Have changed parameters, so it takes an Item, not just a Text item.
	private static void RunJavascriptFrame(Frame frame, Item current,
			boolean acceptKeyboardInput, boolean step, int pause, Color color) {
		try {
			if (current != null) {
				/*
				 * Changed the code from the line below because it caused
				 * problems when the "RunFrame" item was on the zero frame
				 */
				// DisplayIO.addToBack(current.getParent());
				DisplayIO.addToBack(DisplayIO.getCurrentFrame());
			} else {
				/* TODO we should not have to pass an item just to run a frame! */
				current = new Text("Dummy");
				current.setLink(frame.getName());
			}

			_stepColor = color == null ? Color.green : color;
			_stepColor = new Color(_stepColor.getRed(), _stepColor.getGreen(),
					_stepColor.getBlue(), 50);
			_stepPause = pause;
			_step = step;
			_consumeKeyboardInput = acceptKeyboardInput;
			FrameIO.SaveFrame(frame, true);

			// an item without a link signals to run the current frame
			if (current != null && current.getLink() == null) {
				// Make a copy but hide it
				current = current.copy();
				current.setLink(frame.getName());
			}

			_KeyStrokes.clear();

			Thread t = new Thread(current);
			t.setPriority(Thread.MIN_PRIORITY);
			t.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void RunJavascriptFrame(Frame frame, Text current,
			boolean acceptKeyboardInput) {
		RunJavascriptFrame(frame, current, acceptKeyboardInput, false, 0, null);
	}
	
	/**
	 * Same as RunJavascriptFrame method above, except that it takes in
	 * any Item, not just a Text item. -kgas1
	 * @param frame - frame to run
	 * @param current - item selected
	 * @param acceptKeyboardInput
	 */
	public static void RunJavascriptFrame(Frame frame, Item current,
			boolean acceptKeyboardInput){
		RunJavascriptFrame(frame, current, acceptKeyboardInput, false, 0, null);
		
	}

	public static void RunJavascriptFrame(Frame frame, Text current) {
		RunJavascriptFrame(frame, current, false);
	}
	
	/**
	 * Same as RunJavascriptFrame method above, except it takes 
	 * any Item as a parameter; not just Text Items. -kgas1
	 * @param frame
	 * @param current
	 */
	public static void RunJavascriptFrame(Frame frame, Item current){
		RunJavascriptFrame(frame, current, false);
	}

	private static void FlagError(Item item) {
		FrameUtils.DisplayFrame(item.getParent().getName(), true, true);
		item.setHighlightMode(HighlightMode.Normal);
		item.setHighlightColor(Color.CYAN);
		FrameIO.SaveFrame(item.getParent());
	}
	
	/**
	 * Runs a simple code beginning on a frame linked to by the specified item
	 * parameter.
	 * 
	 * @param current
	 *            the item that is linked to the frame to be run.
	 */
	public static Status RunFrameAndReportError(Item current, Context context, Scriptable scope)
			throws Exception {
		// the item must link to a frame
		if (current.getLink() == null) {
			throw new Exception("Could not run unlinked item: "
					+ current.toString());
		}

		Frame child = FrameIO.LoadFrame(current.getAbsoluteLink());

	

		if (_step) {
			if (child != DisplayIO.getCurrentFrame()) {
				DisplayIO.setCurrentFrame(child, true);
			}
			DisplayIO.addToBack(child);
		}

		AgentStats.FrameExecuted();

		// if the frame could not be loaded
		if (child == null) {
			throw new Exception("Could not load item link: " + current.toString());
		}

		// loop through non-title, non-name, text items
		List<Text> body = child.getBodyTextItems(false);

		// if no item was found
		if (body.size() == 0)
			throw new Exception("No code to be executed: " + current.toString());

		Status lastItemStatus = Status.OK;
		for (Text item : body) {
			AgentStats.ItemExecuted();
			try {
				Color oldColor = item.getBackgroundColor();
				if (_step) {
					pause(item);
				}
				lastItemStatus = RunItem(item, context, scope, lastItemStatus);
				if (_step) {
					if (item.getLink() == null) {
						item.setBackgroundColor(oldColor);
					} else {
						item.setHighlightMode(Item.HighlightMode.None);
					}
				}

				if (lastItemStatus != Status.OK) {
					if (lastItemStatus != Status.TrueIf
							&& lastItemStatus != Status.FalseIf) {
						if (_step) {
							DisplayIO.removeFromBack();
						}
						return lastItemStatus;
					}
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				FlagError(item);
				throw new IncorrectUseOfStatementException(
						"Too few parametres: " + item.toString(), item
								.getStatement());
			} catch (NullPointerException e) {
				FlagError(item);
				throw new Exception("Null pointer exception: "
						+ item.toString());
			} catch (RuntimeException e) {
				FlagError(item);
				throw new IncorrectUseOfStatementException(e.getMessage() + " "
						+ item.toString(), item.getStatement());
			} catch (Exception e) {
				throw new Exception(e.getMessage());
			}
		}

		if (_step) {
			DisplayIO.removeFromBack();
			if (DisplayIO.getCurrentFrame() != current.getParent())
				DisplayIO.setCurrentFrame(current.getParent(), true);
		}

		return Status.OK;
	}
	

	/**
	 * @param item
	 * @param oldColor
	 * @throws Exception
	 * @throws InterruptedException
	 */
	private static void pause(Text item) throws Exception, InterruptedException {
		if (!_step)
			return;

		Color oldColor = item.getBackgroundColor();
		item.setBackgroundColor(_stepColor);
		item.setHighlightMode(Item.HighlightMode.None);

		// Make sure we are on the frame with this item
		Frame parent = item.getParentOrCurrentFrame();
		if (!parent.equals(DisplayIO.getCurrentFrame())) {
			DisplayIO.setCurrentFrame(parent, true);
		}

		FrameGraphics.Repaint();

		int timeRemaining;
		if (_stepPause < 0)
			timeRemaining = Integer.MAX_VALUE;
		else
			timeRemaining = _stepPause;

		while (timeRemaining > 0 && !_nextStatement) {
			if (_stop) {
				item.setBackgroundColor(oldColor);
				item.setHighlightMode(HighlightMode.Normal, _stepColor);
				throw new Exception("Program terminated");
			}
			Thread.sleep(DefaultAgent.TIMER_RESOLUTION);
			timeRemaining -= DefaultAgent.TIMER_RESOLUTION;
		}
		_nextStatement = false;
		// Turn off the highlighting
		item.setBackgroundColor(oldColor);
	}

	private static void pause(double time) throws Exception {
		for (int i = 0; i < time * 10; i++) {
			if (_stop) {
				throw new Exception("Program terminated");
			}
			Thread.yield();
			Thread.sleep(100);
		}
	}
	


	/**
	 * Runs a text item on a frame as a SIMPLE statement. The statement is
	 * parsed and if it is a recognised SIMPLE keyword or procedure the code is
	 * executed.
	 * 
	 * @param code
	 *            the item containing the code to be executed.
	 * @param context
	 * @return
	 * @throws Exception
	 */
	private static Status RunItem(Text code, Context context, Scriptable scope, Status lastItemStatus) throws Exception {
		if (_stop) {
			throw new Exception("Program terminated");
		}

		if (code.getLink() != null) {
			return RunFrameAndReportError(code, context, scope);
		}
		else {
			String statement = code.getText().trim();
			
			try {
			
		      Object result = context.evaluateString(scope, statement,"<expeditee item>", 0, null);
		      if (result != org.mozilla.javascript.Context.getUndefinedValue()) {
		    	  System.err.println(org.mozilla.javascript.Context.toString(result));
		      }
			}

			catch (WrappedException we) {
				// Some form of exception was caught by JavaScript and
				// propagated up.
				System.err.println(we.getWrappedException().toString());
				we.printStackTrace();
				throw we;
			}
			catch (EvaluatorException ee) {
				// Some form of JavaScript error.
				System.err.println("js: " + ee.getMessage());
				throw ee;
			}
			catch (JavaScriptException jse) {
				// Some form of JavaScript error.
				System.err.println("js: " + jse.getMessage());
				throw jse;
			}
		
		}

	
		return Status.OK;
	}
	
	public static void stop() {
		_stop = true;
		if (_agent != null) {
			_agent.stop();
		}
	}

	public static void nextStatement() {
		_nextStatement = true;
	}

	public static void ProgramStarted() {
		_programsRunning++;
		AgentStats.reset();
		MessageBay.displayMessage("Running Javascript Program ...", Color.BLUE);
	}

	
}
