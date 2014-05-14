package org.expeditee.actions;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.expeditee.actions.Misc;

import org.expeditee.agents.Agent;
import org.expeditee.agents.DefaultAgent;
import org.expeditee.agents.DisplayTree;
import org.expeditee.agents.SearchAgent;
import org.expeditee.agents.SearchFramesetAndReplace;
import org.expeditee.agents.SearchFrameset;
import org.expeditee.agents.SearchTreeAndReplace;
import org.expeditee.agents.SearchTree;
import org.expeditee.agents.WriteTree;
import org.expeditee.gui.AttributeUtils;
import org.expeditee.gui.AttributeValuePair;
import org.expeditee.gui.Browser;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.gui.FrameUtils;
import org.expeditee.gui.FreeItems;
import org.expeditee.gui.MessageBay;
import org.expeditee.io.Conversion;
import org.expeditee.items.Dot;
import org.expeditee.items.Item;
import org.expeditee.items.ItemUtils;
import org.expeditee.items.Line;
import org.expeditee.items.Text;
import org.expeditee.items.Item.HighlightMode;
import org.expeditee.math.ExpediteeJEP;
import org.expeditee.simple.AboveMaxParametreCountException;
import org.expeditee.simple.BelowMinParametreCountException;
import org.expeditee.simple.Context;
import org.expeditee.simple.IncorrectParametreCountException;
import org.expeditee.simple.IncorrectTypeException;
import org.expeditee.simple.Pointers;
import org.expeditee.simple.Primitives;
import org.expeditee.simple.SBoolean;
import org.expeditee.simple.SCharacter;
import org.expeditee.simple.SInteger;
import org.expeditee.simple.SPointer;
import org.expeditee.simple.SPrimitive;
import org.expeditee.simple.SReal;
import org.expeditee.simple.SString;
import org.expeditee.simple.SVariable;
import org.expeditee.simple.UnitTestFailedException;
import org.expeditee.stats.AgentStats;
import org.expeditee.stats.SessionStats;
import org.nfunk.jep.Node;

public class Simple implements Runnable {

	private static final String DEFAULT_STRING = "$s.";

	private static final String DEFAULT_BOOLEAN = "$b.";

	private static final String DEFAULT_CHAR = "$c.";

	private static final String DEFAULT_INTEGER = "$i.";

	private static final String DEFAULT_REAL = "$r.";

	private static final String DEFAULT_ITEM = "$ip.";

	private static final String DEFAULT_FRAME = "$fp.";

	private static final String DEFAULT_ASSOCIATION = "$ap.";

	private static final String EXIT_TEXT = "exitall";

	public static enum Status {
		Exit, OK, Break, Continue, Return, TrueIf, FalseIf;
	};

	private static final String BREAK2_TEXT = "exitrepeat";

	private static final String BREAK_TEXT = "break";

	private static final String CONTINUE2_TEXT = "nextrepeat";

	private static final String CONTINUE_TEXT = "continue";

	private static final String RETURN_TEXT = "return";

	private static final String LOOP_TEXT = "repeat";

	private static final String TOKEN_SEPARATOR = " +";

	public static final String RUN_FRAME_ACTION = "runframe";

	public static final String DEBUG_FRAME_ACTION = "debugframe";

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

	public static void NewSimpleTest() {
		Frame newSimpleTest = FrameIO.CreateFrame(DisplayIO.getCurrentFrame()
				.getFramesetName(), "Test", null);
		List<String> actions = new ArrayList<String>();
		actions.add(RUN_FRAME_ACTION);
		newSimpleTest.getTitleItem().setActions(actions);
		FrameUtils.DisplayFrame(newSimpleTest, true, true);
		MessageBay.displayMessage("New test created");
	}

	public static void NextTest() {
		Frame next = DisplayIO.getCurrentFrame();
		do {
			next = FrameIO.LoadNext(next);
		} while (next != null && !next.isTestFrame());
		FrameUtils.DisplayFrame(next, true, true);
	}

	public static void PreviousTest() {
		Frame prev = DisplayIO.getCurrentFrame();
		do {
			prev = FrameIO.LoadPrevious(prev);
		} while (prev != null && !prev.isTestFrame());

		FrameUtils.DisplayFrame(prev, true, true);
	}

	public static void LastTest() {
		Frame next = FrameIO.LoadLast();
		Frame lastTest = null;
		do {
			// check if its a test frame
			if (next != null && next.isTestFrame()) {
				lastTest = next;
				break;
			}

			next = FrameIO.LoadPrevious(next);
		} while (next != null);

		FrameUtils.DisplayFrame(lastTest, true, true);
	}

	public static void RunSimpleTests(String frameset) {
		RunSimpleTests(frameset, false, true);
	}

	public static void RunSimpleTestsVerbose(String frameset) {
		RunSimpleTests(frameset, true, true);
	}

	private String _frameset;

	private static boolean _verbose = false;

	public Simple(String frameset, boolean verbose) {
		_frameset = frameset;
		_verbose = verbose;
	}

	public void run() {
		runSuite();
	}

	public boolean runSuite() {
		int testsPassed = 0;
		int testsFailed = 0;

		FrameIO.SaveFrame(DisplayIO.getCurrentFrame(), false);
		MessageBay.displayMessage("Starting test suite: " + _frameset,
				Color.CYAN);

		// Get the next number in the inf file for the _frameset
		int lastFrameNo = FrameIO.getLastNumber(_frameset);

		// Loop through all the valid frames in the _frameset
		for (int i = 1; i <= lastFrameNo; i++) {
			String nextFrameName = _frameset + i;
			Frame nextFrame = FrameIO.LoadFrame(nextFrameName);
			if (nextFrame == null)
				continue;
			Text frameTitle = nextFrame.getTitleItem();
			if (frameTitle == null)
				continue;
			// Run the frames with the RunFrame action on the title
			Text title = frameTitle.copy();
			List<String> actions = title.getAction();
			if (actions == null || title.isAnnotation())
				continue;
			if (actions.get(0).toLowerCase().equals("runframe")) {
				boolean passed = true;
				String errorMessage = null;
				try {
					title.setLink(nextFrameName);
					// TODO add the ability to run a setup frame
					// which sets up variables to be used in all
					// tests
					AgentStats.reset();
					_KeyStrokes.clear();
					_programsRunning++;
					Context context = new Context();
					RunFrameAndReportError(title, context);
					_programsRunning--;
					// if the throws exception annotation is on the frame then
					// it passes only if an exception is thrown
					assert (title.getParent() != null);
					if (title.getParent().hasAnnotation("ThrowsException")) {
						errorMessage = "Expected exception " + title.toString();
						passed = false;
					}
				} catch (Exception e) {
					_programsRunning--;
					if (e instanceof UnitTestFailedException
							|| !title.getParentOrCurrentFrame().hasAnnotation(
									"ThrowsException")) {
						errorMessage = e.getMessage();
						passed = false;
					}
				}
				if (passed) {
					if (_verbose)
						MessageBay.displayMessage("Test passed: "
								+ title.toString(), Item.GREEN);
					testsPassed++;
				} else {
					testsFailed++;
					// Print out the reason for failed tests
					MessageBay.linkedErrorMessage(errorMessage);
					if (Simple._stop) {
						Simple._stop = false;
						return false;
					}
				}
			}
		}
		// The statement below assumes there are no other programs running at
		// the same time
		assert (_programsRunning == 0);
		// Report the number of test passed and failed
		MessageBay.displayMessage(
				"Total tests: " + (testsPassed + testsFailed), Color.CYAN);
		if (testsPassed > 0)
			MessageBay.displayMessage("Passed: " + testsPassed, Item.GREEN);
		if (testsFailed > 0)
			MessageBay.displayMessage("Failed: " + testsFailed, Color.RED);
		// Remove items from the cursor...
		FreeItems.getInstance().clear();

		return testsFailed == 0;
	}

	/**
	 * Runs a suite of tests stored in a given frameset.
	 * 
	 * @param frameset
	 * @param verbose
	 * @param newThread
	 *            false if tests should be run on the current frame
	 */
	public static void RunSimpleTests(String frameset, boolean verbose,
			boolean newThread) {
		_stop = false;
		Simple testSuite = new Simple(frameset, verbose);
		if (newThread) {
			Thread t = new Thread(testSuite);
			t.setPriority(Thread.MIN_PRIORITY);
			t.start();
		} else {
			if (!testSuite.runSuite()) {
				throw new RuntimeException(frameset + " failed");
			}
			
		}

	}

	//Have changed parameters, so it takes an Item, not just a Text item.
	private static void RunFrame(Frame frame, Item current,
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

	public static void RunFrame(Frame frame, Text current,
			boolean acceptKeyboardInput) {
		RunFrame(frame, current, acceptKeyboardInput, false, 0, null);
	}
	
	/**
	 * Same as RunFrame method above, except that it takes in
	 * any Item, not just a Text item. -kgas1
	 * @param frame - frame to run
	 * @param current - item selected
	 * @param acceptKeyboardInput
	 */
	public static void RunFrame(Frame frame, Item current,
			boolean acceptKeyboardInput){
		RunFrame(frame, current, acceptKeyboardInput, false, 0, null);
		
	}

	public static void RunFrame(Frame frame, Text current) {
		RunFrame(frame, current, false);
	}
	
	/**
	 * Same as runframe method above, except it takes 
	 * any Item as a parameter; not just Text Items. -kgas1
	 * @param frame
	 * @param current
	 */
	public static void RunFrame(Frame frame, Item current){
		RunFrame(frame, current, false);
	}

	/**
	 * At present programs which accept keyboard input can not be debugged.
	 * 
	 * @param current
	 * @param pause
	 */
	public static void DebugFrame(Frame frame, Text current, float pause,
			Color color) {
		if (isProgramRunning()) {
			stop();
		}
		RunFrame(frame, current, false, true, Math.round(pause * 1000), color);
	}

	/**
	 * At present programs which accept keyboard input can not be debugged.
	 * 
	 * @param current
	 * @param pause
	 *            the time to pause between
	 */
	public static void DebugFrame(Frame frame, Text current, float pause) {
		DebugFrame(frame, current, pause, null);
	}

	public static void DebugFrame(Frame frame, Text current) {
		DebugFrame(frame, current, -1.0F, null);
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
	public static Status RunFrameAndReportError(Item current, Context context)
			throws Exception {
		
		
		// the item must link to a frame
		if (current.getLink() == null) {
			throw new Exception("Could not run unlinked item: "
					+ current.toString());
		}

		Frame child = FrameIO.LoadFrame(current.getAbsoluteLink());

		// Create frame variables for each linked annotation item on the frame
		// which has a single word of text corresponding to the variable name
		for (Text text : child.getAnnotationItems()) {
			String link = text.getAbsoluteLink();
			if (link == null)
				continue;
			Frame frame = FrameIO.LoadFrame(link);
			if (frame == null)
				continue;
			// Now save the frame as a variable
			String varName = text.getText().substring(1).trim();
			if (varName.indexOf(' ') > 0)
				continue;
			context.getPointers().setObject(SPointer.framePrefix + varName,
					frame);
			context.getPointers().setObject(SPointer.itemPrefix + varName,
					frame.getTitleItem());
			context.getPrimitives().add(SString.prefix + varName,
					new SString(frame.getName()));
		}

		if (_step) {
			if (child != DisplayIO.getCurrentFrame()) {
				DisplayIO.setCurrentFrame(child, true);
			}
			DisplayIO.addToBack(child);
		}

		AgentStats.FrameExecuted();

		// if the frame could not be loaded
		if (child == null) {
			throw new Exception("Could not load item link: "
					+ current.toString());
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
				lastItemStatus = RunItem(item, context, lastItemStatus);
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
	 * This method should be modified to parse strings correctly
	 * 
	 * @param statement
	 * @return
	 */
	private static String[] parseStatement(Text code) throws Exception {
		String statement = code.getText().trim();
		ArrayList<String> tokens = new ArrayList<String>();

		// At the moment annotation items are ignored by the interpreter
		// // Check special annotation tags for programs
		// if (statement.length() > 0 && statement.charAt(0) == '@') {
		// statement = statement.toLowerCase();
		// // Flags the next unit test as being required to throw an exception
		// if (statement.equals("@throwsexception")) {
		// code.getParentOrCurrentFrame().setThrowsException(true);
		// }
		// return null;
		// }

		for (int i = 0; i < statement.length(); i++) {
			char c = statement.charAt(i);
			// ignore spaces
			if (c != ' ') {
				int startOfToken = i;
				if (c == '\"') {
					int endOfToken = statement.length() - 1;
					// find the end of the string literal
					while (statement.charAt(endOfToken) != '\"')
						endOfToken--;
					if (endOfToken > startOfToken) {
						tokens.add(statement.substring(startOfToken,
								endOfToken + 1));
					} else {
						throw new RuntimeException("Expected matching \" ");
					}
					break;
				} else if (c == '#' || c == '/') {
					break;
				} else {
					// read in a normal token
					while (++i < statement.length()
							&& statement.charAt(i) != ' ')
						;
					tokens.add(statement.substring(startOfToken, i)
							.toLowerCase());
				}
			}
		}

		String[] a = new String[tokens.size()];
		a = tokens.toArray(a);
		code.setProcessedText(a);
		return a;
	}

	/**
	 * Runs a SIMPLE procedure statement.
	 * 
	 * @param tokens
	 *            the parsed Call statement.
	 * @param code
	 *            the expeditee item containing the procedure call and the link
	 *            to the frame with the procedure code.
	 * @param context
	 *            the current context from which the procedure call is being
	 *            made.
	 * @throws Exception
	 *             when errors occur in running the procedure.
	 */
	private static Status Call(String[] tokens, Item code, Context context)
			throws Exception {
		// Check that the call statement is linked
		if (code.getLink() == null)
			throw new Exception("Unlinked call statement: " + code.toString());

		Frame procedure = FrameIO.LoadFrame(code.getAbsoluteLink());

		// Add call to the start of the title if it doesnt exist
		// This makes the call and signature tokens counts match
		String procedureTitle = procedure.getTitleItem().getFirstLine();
		
		if (!procedureTitle.toLowerCase().startsWith("call "))
			procedureTitle = "call " + procedureTitle;

		// Check that the calling statement matches the procedure
		// signature
		String[] procedureSignature = procedureTitle.split(TOKEN_SEPARATOR);

		// Check for the right amount of parametres
		if (procedureSignature.length < tokens.length)
			throw new Exception("Call statement has too many parametres: "
					+ code.toString());
		else if (procedureSignature.length > tokens.length)
			throw new Exception("Call statement has too few parametres: "
					+ code.toString());
		// else if (procedureSignature[1].equals(tokens[1]))
		// throw new Exception("Call statement and procedure name dont match: "
		// + code.toString());

		// create the new context for the sub procedure
		Context newContext = new Context();
		// Check that the types/prefixes match
		for (int i = 2; i < tokens.length; i++) {
			// TODO allow for auto casting of primitives
			if (tokens[i].substring(1, 2).equals(
					procedureSignature[i].substring(1, 2))) {
				// Add the variables to the new context
				if (Primitives.isPrimitive(tokens[i])) {
					try {
						// try and get the value for the variable
						SPrimitive p = context.getPrimitives().getVariable(
								tokens[i]);
						newContext.getPrimitives()
								.add(procedureSignature[i], p);
					} catch (Exception e) {
						// If an exception occurs the variable doesnt
						// exist in the current context
						// So the variable must be added to both context
						context.getPrimitives().add(tokens[i], new SString(""));
						newContext.getPrimitives().add(procedureSignature[i],
								new SString(""));
					}
				} else if (Pointers.isPointer(tokens[i])) {
					try {
						// try and get the value for the variable
						SPointer p = context.getPointers().getVariable(
								tokens[i]);
						newContext.getPointers().add(procedureSignature[i], p);
					} catch (Exception e) {
						// If an exception occurs the variable doesnt
						// exist in the current context
						// So the variable must be added to both context
						context.getPointers().setObject(tokens[i], null);
						newContext.getPointers().setObject(
								procedureSignature[i], null);
					}
				} else
					throw new Exception("Unrecognised variable type: "
							+ tokens[i] + " in " + code.toString());
			} else
				throw new IncorrectTypeException(procedureSignature[i], i);
		}

		// Follow the link and Run the code for the procedure
		Status result = RunFrameAndReportError(code, newContext);
		// If a return statement ends the procedure then we accept this as
		// normal execution
		switch (result) {
		case Return:
			result = Status.OK;
			break;
		case Break:
			throw new Exception(BREAK_TEXT + " statement without matching "
					+ LOOP_TEXT + " in " + code.toString());
		case Continue:
			throw new Exception("");
		}

		// Now copy the values from the procedure context into the
		// current context
		for (int i = 2; i < tokens.length; i++) {
			try {
				if (Primitives.isPrimitive(tokens[i])) {
					// try and get the value for the variable
					SVariable p = context.getPrimitives()
							.getVariable(tokens[i]);
					SVariable newP = newContext.getPrimitives().getVariable(
							procedureSignature[i]);
					p.setValue(newP);
				} else {
					// try and get the value for the variable
					SVariable p = context.getPointers().getVariable(tokens[i]);
					SVariable newP = newContext.getPointers().getVariable(
							procedureSignature[i]);
					p.setValue(newP);
				}
			} catch (Exception e) {
				assert (false);
			}
		}

		return result;
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
	private static Status RunItem(Text code, Context context,
			Status lastItemStatus) throws Exception {
		if (_stop) {
			throw new Exception("Program terminated");
		}

		String[] tokens = code.getProcessedText();

		if (tokens == null) {
			tokens = parseStatement(code);
		}

		// Annotation items are ignored after parsing running options
		if (tokens == null) {
			return Status.OK;
			// Comments without links are ignored
		} else if (tokens.length == 0) {
			if (code.getLink() == null)
				return Status.OK;
			else
				return RunFrameAndReportError(code, context);
		}

		// At present only set statements can have literals so they
		// are the only statements that we have to worry about string
		// literals
		if (tokens[0].equals("call") && tokens.length >= 2) {
			return Call(tokens, code, context);
			// Check if the user wants to display a message
			// Check for set statements
		} 
		else if (tokens[0].startsWith("calculatestring")) {
			assertMinParametreCount(tokens, 1);
			String toCalculate = context.getPrimitives().getStringValue(
					tokens[1]);
			boolean result = true;
			String error = "";
			ExpediteeJEP myParser = new ExpediteeJEP();
			// Add the variables in the system
			context.getPrimitives().addToParser(myParser);
			try {
				Node equation = myParser.parse(toCalculate);
				String value = myParser.evaluate(equation, false);
				
				// Add a new variable if its an assignment statement
				String newVar = SReal.prefix + myParser.getNewVariable();
				if (newVar.length() > 0) {
					try {
						context.getPrimitives().setValue(newVar,
								new SString(value));
					} catch (IncorrectTypeException e) {
						result = false;
						error = e.getMessage();
					}
				}
				if (tokens.length > 2) {
					context.getPrimitives().setValue(tokens[2],
							new SString(value));
				}
				// TODO get the answer
			} catch (Throwable e) {
				result = false;
				if (myParser.getErrorInfo() == null) {
					error = e.getMessage();
				} else {
					error = myParser.getErrorInfo().replace("\n", "");
				}
			}
			// Set the result variable if there is one
			if (tokens.length > 3) {
				context.getPrimitives().setValue(tokens[3],
						new SBoolean(result));
				// Set the result variable if there is one
				if (tokens.length > 4 && !result) {
					context.getPrimitives().setValue(tokens[4], error);
				}
			}
		} else if (tokens[0].startsWith("issearchpatternvalid")) {
			assertExactParametreCount(tokens, 2);
			boolean result = true;
			try {
				Pattern.compile(context.getPrimitives().getStringValue(
						tokens[1]));
			} catch (Exception e) {
				result = false;
			}
			context.getPrimitives().setValue(tokens[2], new SBoolean(result));
		} else if (tokens[0].startsWith("search")) {
			if (tokens[0].equals("searchstr")) {
				assertExactParametreCount(tokens, 5);
				String searchStr = context.getPrimitives().getStringValue(
						tokens[1]);
				String pattern = context.getPrimitives().getStringValue(
						tokens[2]);
				String[] result = searchStr.split(pattern, 2);
				boolean bFound = result.length > 1;
				context.getPrimitives().setValue(tokens[3],
						new SBoolean(bFound));
				if (bFound) {
					context.getPrimitives().setValue(tokens[4],
							new SInteger(result[0].length() + 1));
					context.getPrimitives().setValue(
							tokens[5],
							new SInteger(searchStr.length()
									- result[1].length()));
				}
			} else if (tokens[0].equals("searchitem")) {
				assertExactParametreCount(tokens, 5);
				assertVariableType(tokens[1], 1, SPointer.itemPrefix);
				boolean bFound = false;
				Item searchItem = (Item) context.getPointers().getVariable(
						tokens[1]).getValue();
				if (searchItem instanceof Text) {
					bFound = context.searchItem((Text) searchItem, context
							.getPrimitives().getStringValue(tokens[2]), null,
							tokens[4], tokens[5], null);
				}
				context.getPrimitives().setValue(tokens[3],
						new SBoolean(bFound));
			} else if (tokens[0].equals("searchframe")) {
				assertExactParametreCount(tokens, 6);
				assertVariableType(tokens[1], 1, SPointer.framePrefix);
				assertVariableType(tokens[4], 4, SPointer.itemPrefix);
				Frame frameToSearch = (Frame) context.getPointers()
						.getVariable(tokens[1]).getValue();
				boolean bFound = false;
				for (Text itemToSearch : frameToSearch.getTextItems()) {
					bFound = context.searchItem(itemToSearch, context
							.getPrimitives().getStringValue(tokens[2]),
							tokens[4], tokens[5], tokens[6], null);
					if (bFound)
						break;
				}
				context.getPrimitives().setValue(tokens[3],
						new SBoolean(bFound));
			} else if (tokens[0].equals("searchframeset")) {
				assertMinParametreCount(tokens, 3);

				String frameset = context.getPrimitives().getStringValue(
						tokens[1]);
				boolean bReplace = false;
				if (tokens.length > 4) {
					bReplace = context.getPrimitives().getBooleanValue(
							tokens[4]);
				}
				String replacementString = null;
				// If in replacement mode get the string to replace the
				// searchPattern with
				if (bReplace) {
					if (tokens.length > 5) {
						replacementString = context.getPrimitives()
								.getStringValue(tokens[5]);
					} else {
						replacementString = "";
					}
				}
				String resultsFrameset = context.getPrimitives()
						.getStringValue(tokens[3]);
				String pattern = context.getPrimitives().getStringValue(
						tokens[2]);
				long firstFrame = 1;
				long maxFrame = Long.MAX_VALUE;
				if (tokens.length > 7) {
					firstFrame = context.getPrimitives().getIntegerValue(
							tokens[7]);
					if (tokens.length > 8) {
						maxFrame = context.getPrimitives().getIntegerValue(
								tokens[8]);
					}
				}

				SearchAgent searchAgent = null;

				// If replacement needs to be done... use the slow search for
				// now
				// The fast search does not do replacement
				if (bReplace) {
					searchAgent = new SearchFramesetAndReplace(firstFrame,
							maxFrame, null);
				} else {
					searchAgent = new SearchFrameset(firstFrame, maxFrame, null);
				}
				searchAgent.initialise(null, null, frameset, resultsFrameset,
						replacementString, pattern);
				_agent = searchAgent;
				searchAgent.run();
				_agent = null;

				if (tokens.length > 6) {
					context.getPrimitives().setValue(tokens[6],
							searchAgent.getResultsFrameName());
				}
			} else if (tokens[0].equals("searchtree")) {
				assertMinParametreCount(tokens, 3);

				String topFrameName = context.getPrimitives().getStringValue(
						tokens[1]);
				boolean bReplace = false;
				if (tokens.length > 4) {
					bReplace = context.getPrimitives().getBooleanValue(
							tokens[4]);
				}
				String replacementString = null;
				// If in replacement mode get the string to replace the
				// searchPattern with
				if (bReplace) {
					if (tokens.length > 5) {
						replacementString = context.getPrimitives()
								.getStringValue(tokens[5]);
					} else {
						replacementString = "";
					}
				}
				String resultsFrameset = context.getPrimitives()
						.getStringValue(tokens[3]);
				String pattern = context.getPrimitives().getStringValue(
						tokens[2]);
				SearchAgent searchAgent = null;
				// If replacement needs to be done... use the slow search for
				// now The fast search does not do replacement
				if (bReplace) {
					searchAgent = new SearchTreeAndReplace(null);
				} else {
					searchAgent = new SearchTree(null);
				}
				_agent = searchAgent;
				searchAgent.initialise(null, null, topFrameName,
						resultsFrameset, replacementString, pattern);
				searchAgent.run();
				_agent = null;
				if (tokens.length > 6) {
					context.getPrimitives().setValue(tokens[6],
							searchAgent.getResultsFrameName());
				}
			} else {
				throw new Exception("Unsupported search command: "
						+ code.toString());
			}
		} else if (tokens[0].startsWith("set")) {
			if (tokens[0].equals("set")) {
				assertExactParametreCount(tokens, 2);
				try {
					// Check if we are setting variable to variable
					if (tokens[2].startsWith(SVariable.prefix)
							&& tokens.length == 3) {
						context.getPrimitives().set(tokens[1], tokens[2]);
					}
					// Otherwise we are setting a variable with a literal
					else {
						// check for strings enclosed in quotes
						if (tokens[2].startsWith("\"")) {
							context.getPrimitives().setValue(
									tokens[1],
									new SString(tokens[2].substring(1,
											tokens[2].length() - 1)));
							// set a literal
						} else if (tokens.length == 3) {
							context.getPrimitives().setValue(tokens[1],
									tokens[2]);
						}
					}
				} catch (Exception e) {
					throw new RuntimeException(e.getMessage());
				}
			} else if (tokens[0].equals("setassociation")) {
				assertExactParametreCount(tokens, 3);

				Map<String, String> map = (Map<String, String>) context
						.getPointers().getVariable(tokens[1]).getValue();
				String attribute = context.getPrimitives().getStringValue(
						tokens[2]);
				String value = context.getPrimitives()
						.getStringValue(tokens[3]);
				map.put(attribute, value);
			} else if (tokens[0].equals("setframevalue")) {
				assertMinParametreCount(tokens, 3);
				assertVariableType(tokens[1], 1, SPointer.framePrefix);

				// Get the attribute to be searched for on the target frame
				Frame targetFrame = (Frame) context.getPointers().getVariable(
						tokens[1]).getValue();
				String targetAttribute = context.getPrimitives()
						.getStringValue(tokens[2]).toLowerCase()
						+ ":";
				String value = context.getPrimitives()
						.getStringValue(tokens[3]);
				Boolean found = false;
				Text attributeItem = null;
				Item valueItem = null;
				// Begin the search
				for (Text text : targetFrame.getTextItems()) {
					String s = text.getText().toLowerCase();

					if (s.startsWith(targetAttribute)) {
						attributeItem = text;
						AttributeUtils.replaceValue(attributeItem, value);
						found = true;
						break;
					}
				}
				// Keep looking for a matching value nearby if we found an
				// attribute without the value in the same item
				if (!found && attributeItem != null) {
					Point2D.Float endPoint = attributeItem
							.getParagraphEndPosition();

					for (Text text : targetFrame.getTextItems()) {
						Point startPoint = text.getPosition();
						if (Math.abs(startPoint.y - endPoint.y) < 10
								&& Math.abs(startPoint.x - endPoint.x) < 20) {
							found = true;
							valueItem = text;
							text.setText(value);
							break;
						}
					}
				}

				// Set the values of the output parametres
				if (tokens.length > 4) {
					context.getPrimitives().setValue(tokens[4],
							new SBoolean(found));
					if (tokens.length > 5) {
						context.getPointers().setObject(tokens[5],
								attributeItem);
						if (tokens.length > 6) {
							context.getPointers().setObject(tokens[6],
									valueItem);
						}
					}
				}
			} else if (tokens[0].startsWith("setitem")) {
				if (tokens[0].equals("setitemposition")) {
					assertVariableType(tokens[1], 1, SPointer.itemPrefix);
					// assertPrimitiveType(tokens[2], 2);
					// assertPrimitiveType(tokens[3], 3);
					Item item = (Item) context.getPointers().getVariable(
							tokens[1]).getValue();
					item.setPosition(context.getPrimitives().getVariable(
							tokens[2]).integerValue().intValue(), context
							.getPrimitives().getVariable(tokens[3])
							.integerValue().intValue());
				} else if (tokens[0].equals("setitemthickness")) {
					assertVariableType(tokens[1], 1, SPointer.itemPrefix);
					// assertPrimitiveType(tokens[2], 2);
					Item item = (Item) context.getPointers().getVariable(
							tokens[1]).getValue();
					item.setThickness(context.getPrimitives().getVariable(
							tokens[2]).integerValue().intValue());
				} else if (tokens[0].equals("setitemwidth")) {
					assertVariableType(tokens[1], 1, SPointer.itemPrefix);
					// assertPrimitiveType(tokens[2], 2);
					Item item = (Item) context.getPointers().getVariable(
							tokens[1]).getValue();
					item.setWidth(context.getPrimitives()
							.getVariable(tokens[2]).integerValue().intValue());
				} else if (tokens[0].equals("setitemsize")) {
					assertVariableType(tokens[1], 1, SPointer.itemPrefix);
					// assertPrimitiveType(tokens[2], 2);
					Item item = (Item) context.getPointers().getVariable(
							tokens[1]).getValue();
					item.setSize(context.getPrimitives().getVariable(tokens[2])
							.integerValue().intValue());
				} else if (tokens[0].equals("setitemlink")) {
					assertVariableType(tokens[1], 1, SPointer.itemPrefix);
					// assertPrimitiveType(tokens[2], 2);
					Item item = (Item) context.getPointers().getVariable(
							tokens[1]).getValue();
					item.setLink(context.getPrimitives().getVariable(tokens[2])
							.stringValue());
				} else if (tokens[0].equals("setitemdata")) {
					
					assertVariableType(tokens[1], 1, SPointer.itemPrefix);
					// assertPrimitiveType(tokens[2], 2);
					Item item = (Item) context.getPointers().getVariable(
							tokens[1]).getValue();
					
					item.setData(context.getPrimitives().getVariable(tokens[2]).stringValue());
					//TODO: should there be a difference between setting and appending??
					//item.addToData(context.getPrimitives().getVariable(tokens[2]).stringValue());
					
				} else if (tokens[0].equals("setitemaction")) {
					assertVariableType(tokens[1], 1, SPointer.itemPrefix);
					// assertPrimitiveType(tokens[2], 2);
					Item item = (Item) context.getPointers().getVariable(
							tokens[1]).getValue();
					item.setAction(context.getPrimitives().getVariable(
							tokens[2]).stringValue());
				} else if (tokens[0].equals("setitemfillcolor")) {
					assertVariableType(tokens[1], 1, SPointer.itemPrefix);
					// assertPrimitiveType(tokens[2], 2);
					Item item = (Item) context.getPointers().getVariable(
							tokens[1]).getValue();
					String stringColor = context.getPrimitives().getVariable(
							tokens[2]).stringValue();
					item.setBackgroundColor((Color) Conversion.Convert(
							Color.class, stringColor));
				} else if (tokens[0].equals("setitemcolor")) {
					assertVariableType(tokens[1], 1, SPointer.itemPrefix);
					// assertPrimitiveType(tokens[2], 2);
					Item item = (Item) context.getPointers().getVariable(
							tokens[1]).getValue();
					String stringColor = context.getPrimitives().getVariable(
							tokens[2]).stringValue();
					item.setColor((Color) Conversion.Convert(Color.class,
							stringColor, item.getColor()));
					
				} else if (tokens[0].equals("setitemtext")) {
					assertVariableType(tokens[1], 1, SPointer.itemPrefix);
					// assertPrimitiveType(tokens[2], 2);
					String newText = context.getPrimitives().getVariable(
							tokens[2]).stringValue();
					Text textItem = (Text) context.getPointers().getVariable(
							tokens[1]).getValue();
					textItem.setText(newText, true);
				} 
				else
					throw new Exception("Unsupported setItem command: "
							+ code.toString());
			} else if (tokens[0].equals("setstrchar")) {
				assertExactParametreCount(tokens, 3);
				StringBuffer s = new StringBuffer(context.getPrimitives()
						.getStringValue(tokens[1]));
				int pos = (int) context.getPrimitives().getIntegerValue(
						tokens[2]);
				char newChar = context.getPrimitives().getCharacterValue(
						tokens[3]);
				while (pos > s.length()) {
					s.append(newChar);
				}
				s.setCharAt(pos - 1, newChar);

				context.getPrimitives().setValue(tokens[1],
						new SString(s.toString()));
			} else if (tokens[0].equals("setcharinitem")) {
				assertExactParametreCount(tokens, 4);
				assertVariableType(tokens[1], 1, SPointer.itemPrefix);

				int row = (int) context.getPrimitives().getIntegerValue(
						tokens[3]) - 1;
				int col = (int) context.getPrimitives().getIntegerValue(
						tokens[4]) - 1;
				char newChar = context.getPrimitives().getCharacterValue(
						tokens[2]);
				Text item = (Text) context.getPointers().getVariable(tokens[1])
						.getValue();
				List<String> itemText = item.getTextList();

				while (row >= itemText.size()) {
					StringBuffer sb = new StringBuffer();
					for (int i = 0; i <= col; i++)
						sb.append(newChar);
					itemText.add(sb.toString());
				}
				StringBuffer sb = new StringBuffer(itemText.get(row));
				while (sb.length() <= col)
					sb.append(newChar);
				sb.setCharAt(col, newChar);

				// set the modified string
				itemText.set(row, sb.toString());
				item.setTextList(itemText);
			}
		} else if (tokens[0].startsWith("assert")) {
			if (tokens[0].equals("asserttrue")) {
				assertExactParametreCount(tokens, 1);
				if (!context.getPrimitives().getBooleanValue(tokens[1]))
					throw new UnitTestFailedException("true", "false");
			} else if (tokens[0].equals("assertfalse")) {
				assertExactParametreCount(tokens, 1);
				if (context.getPrimitives().getBooleanValue(tokens[1]))
					throw new UnitTestFailedException("false", "true");
			} else if (tokens[0].equals("assertfail")) {
				assertExactParametreCount(tokens, 0);
				throw new UnitTestFailedException("pass", "fail");
			} else if (tokens[0].equals("assertnull")) {
				assertExactParametreCount(tokens, 1);
				if (!context.isNull(tokens[1]))
					throw new UnitTestFailedException("null", "not null");
			} else if (tokens[0].equals("assertnotnull")) {
				assertExactParametreCount(tokens, 1);
				if (context.isNull(tokens[1]))
					throw new UnitTestFailedException("not null", "null");
			} else if (tokens[0].equals("assertdefined")) {
				assertExactParametreCount(tokens, 1);
				if (!context.isDefined(tokens[1]))
					throw new UnitTestFailedException("defined", "not defined");
			} else if (tokens[0].equals("assertnotdefined")) {
				assertExactParametreCount(tokens, 1);
				if (context.isDefined(tokens[1]))
					throw new UnitTestFailedException("defined", "not defined");
			} else if (tokens[0].equals("assertequals")) {
				assertExactParametreCount(tokens, 2);
				if (!context.getPrimitives().equalValues(tokens[1], tokens[2]))
					throw new UnitTestFailedException(context.getPrimitives()
							.getStringValue(tokens[1]), context.getPrimitives()
							.getStringValue(tokens[2]));
			} else if (tokens[0].equals("assertequalframes")) {
				assertExactParametreCount(tokens, 2);
				assertVariableType(tokens[1], 1, SPointer.framePrefix);
				assertVariableType(tokens[2], 2, SPointer.framePrefix);
				Frame frame1 = (Frame) context.getPointers().getVariable(
						tokens[1]).getValue();
				Frame frame2 = (Frame) context.getPointers().getVariable(
						tokens[2]).getValue();
				frame1.assertEquals(frame2);
			} else if (tokens[0].equals("assertdefined")) {
				assertExactParametreCount(tokens, 1);
				if (!context.isDefined(tokens[1])) {
					throw new UnitTestFailedException(tokens[1] + " exists",
							"not defined");
				}
			} else {
				throw new RuntimeException("Invalid Assert statement");
			}
		} else if (tokens[0].startsWith("goto")) {
			String frameNameVar = DEFAULT_STRING;
			if (tokens.length > 1) {
				assertExactParametreCount(tokens, 1);
				frameNameVar = tokens[1];
			}
			String frameName = context.getPrimitives().getStringValue(
					frameNameVar);
			Navigation.Goto(frameName);
		} else if (tokens[0].startsWith("get")) {
				if (tokens[0].startsWith("getframe")) {
				if (tokens[0].equals("getframevalue")) {
					assertMinParametreCount(tokens, 3);
					assertVariableType(tokens[1], 1, SPointer.framePrefix);

					// Get the attribute to be searched for on the target frame
					Frame targetFrame = (Frame) context.getPointers()
							.getVariable(tokens[1]).getValue();
					String targetAttribute = context.getPrimitives()
							.getStringValue(tokens[2]).toLowerCase()
							+ ":";
					Boolean found = false;
					String value = "";
					Text attributeItem = null;
					Item valueItem = null;
					// Begin the search
					for (Text text : targetFrame.getTextItems()) {
						String s = text.getText().toLowerCase();
						if (s.startsWith(targetAttribute)) {
							attributeItem = text;
							value = new AttributeValuePair(s).getValue();
							if (value.length() > 0) {
								found = true;
							}
							break;
						}
					}
					// Keep looking for a matching value nearby if we found an
					// attribute without the value in the same item
					if (!found && attributeItem != null) {
						Point2D.Float endPoint = attributeItem
								.getParagraphEndPosition();

						for (Text text : targetFrame.getTextItems()) {
							Point startPoint = text.getPosition();
							if (Math.abs(startPoint.y - endPoint.y) < 10
									&& Math.abs(startPoint.x - endPoint.x) < 20) {
								found = true;
								valueItem = text;
								value = text.getText();
								break;
							}
						}
					}

					// Set the values of the output parametres
					context.getPrimitives().setValue(tokens[3],
							new SString(value));
					if (tokens.length > 4) {
						context.getPrimitives().setValue(tokens[4],
								new SBoolean(found));
						if (tokens.length > 5) {
							context.getPointers().setObject(tokens[5],
									attributeItem);
							if (tokens.length > 6) {
								context.getPointers().setObject(tokens[6],
										valueItem);
							}
						}
					}
				} else if (tokens[0].startsWith("getframename")) {
					String frameNameVar = DEFAULT_STRING;
					String frameVar = DEFAULT_FRAME;

					if (tokens.length > 1) {
						assertExactParametreCount(tokens, 2);
						assertVariableType(tokens[1], 1, SPointer.framePrefix);
						frameNameVar = tokens[2];
						frameVar = tokens[1];
					}
					Frame frame = (Frame) context.getPointers().getVariable(
							frameVar).getValue();
					context.getPrimitives().setValue(frameNameVar,
							frame.getName());
				} else if (tokens[0].startsWith("getframetitle")) {
					String frameTitleVar = DEFAULT_ITEM;
					String frameVar = DEFAULT_FRAME;

					if (tokens.length > 1) {
						assertExactParametreCount(tokens, 2);
						assertVariableType(tokens[1], 1, SPointer.framePrefix);
						assertVariableType(tokens[2], 2, SPointer.itemPrefix);
						frameTitleVar = tokens[2];
						frameVar = tokens[1];
					}
					Frame frame = (Frame) context.getPointers().getVariable(
							frameVar).getValue();
					context.getPointers().setObject(frameTitleVar,
							frame.getTitleItem());
				} else if (tokens[0].startsWith("getframefilepath")) {
					assertExactParametreCount(tokens, 2);
					String frameName = context.getPrimitives().getStringValue(
							tokens[1]);
					String path = FrameIO.LoadFrame(frameName).getPath();
					String filePath = FrameIO.getFrameFullPathName(path,
							frameName);
					context.getPrimitives().setValue(tokens[2], filePath);
				} else if (tokens[0].equals("getframelog")) {
					assertExactParametreCount(tokens, 1);
					assertVariableType(tokens[1], 1, SPointer.itemPrefix);

					String log = SessionStats.getFrameEventList();
					Text t;

					t = (Text) context.getPointers().getVariable(tokens[1])
							.getValue();
					t.setText(log, true);
				} else if (tokens[0].equals("getframeitemcount")) {
					String frameVar = DEFAULT_FRAME;
					String countVar = DEFAULT_INTEGER;
					if (tokens.length > 1) {
						assertExactParametreCount(tokens, 2);
						assertVariableType(tokens[1], 1, SPointer.framePrefix);
						frameVar = tokens[1];
						countVar = tokens[2];
					}
					Frame frame = (Frame) context.getPointers().getVariable(
							frameVar).getValue();
					Integer count = frame.getItems(true).size();
					context.getPrimitives().setValue(countVar,
							new SInteger(count));
				} else {
					executeAction(tokens, context);
				}
			} else if (tokens[0].equals("getassociation")) {
				assertMinParametreCount(tokens, 3);
				Map map = (Map) context.getPointers().getVariable(tokens[1])
						.getValue();
				String attribute = context.getPrimitives().getStringValue(
						tokens[2]);
				String newValue = map.get(attribute).toString();
				context.getPrimitives().setValue(tokens[3], newValue);
			} else if (tokens[0].startsWith("getcurrent")) {
				if (tokens[0].equals("getcurrentframe")) {
					assertMinParametreCount(tokens, 1);
					assertVariableType(tokens[1], 1, SPointer.framePrefix);

					Frame currentFrame = DisplayIO.getCurrentFrame();
					context.getPointers().setObject(tokens[1], currentFrame);

					// check if the user is also after the frameName
					if (tokens.length > 2) {
						context.getPrimitives().setValue(tokens[2],
								new SString(currentFrame.getName()));
					}
				} else if (tokens[0].equals("getcurrentitem")) {
					assertMinParametreCount(tokens, 1);
					assertVariableType(tokens[1], 1, SPointer.itemPrefix);

					Item currentItem = FrameUtils.getCurrentItem();

					context.getPointers().setObject(tokens[1], currentItem);

					// check if the user is also after line position
					if (currentItem != null && currentItem instanceof Text
							&& tokens.length > 2) {
						Text text = (Text) currentItem;
						int cursorLinePos = text
								.getLinePosition(FrameMouseActions.getY());
						context.getPrimitives().setValue(tokens[2],
								new SInteger(cursorLinePos + 1));
						if (tokens.length > 3) {
							int cursorCharPos = text.getCharPosition(
									cursorLinePos, DisplayIO.getMouseX())
									.getCharIndex();
							context.getPrimitives().setValue(tokens[3],
									new SInteger(cursorCharPos + 1));
						}
					}
				}
			} else if (tokens[0].startsWith("getitem")) {
				
				//used to return an item containing a particular piece of data.
			if(tokens[0].equals("getitemcontainingdata")) {
				
				assertVariableType(tokens[1],1,SPointer.itemPrefix);
                
                String data;
                Item getItem;
                String getFrameName;
               
                if(tokens.length == 3) {
                    data = context.getPrimitives().getVariable(tokens[2]).stringValue();
                   
                    //no frame specified by user so use current frame.
                    getFrameName = DisplayIO.getCurrentFrame().getName();
                    getItem = Misc.getItemContainingData(data, FrameUtils.getFrame(getFrameName));
                }
                else if(tokens.length == 4) {
                    
                	getFrameName = context.getPrimitives().getStringValue(tokens[2]);
                    data = context.getPrimitives().getVariable(tokens[3]).stringValue();
                    getItem = Misc.getItemContainingData(data, FrameUtils.getFrame(getFrameName));
                }
                else {
                    getItem = null;
                }
                
                context.getPointers().setObject(tokens[1], getItem);
               
                // System.out.println(getItem.getText());
				
			} 	//used to get an item at a specified position,
			//will lessen the amount of looping and if statements
			//I have to do when writing SIMPLE code - kgas1 23/01/2012 
			else if(tokens[0].equals("getitematposition")) {
					assertVariableType(tokens[1], 1, SPointer.itemPrefix);
					
					Integer x;
					Integer y;
					Item getItem;
					String getFrameName;
	
					//use 3 parameter version.
					if(tokens.length == 4)
					{
						x = context.getPrimitives()
								.getVariable(tokens[2]).integerValue().intValue();
						y = context.getPrimitives()
								.getVariable(tokens[3]).integerValue().intValue();
						
						//no frame specified by user so use current frame.
						getFrameName = DisplayIO.getCurrentFrame().getName();
						getItem = Misc.getItemAtPosition(x, y, FrameUtils.getFrame(getFrameName));
					}
					else if(tokens.length == 5)	//use 4 parameter version.
					{
						getFrameName = context.getPrimitives()
								.getStringValue(tokens[2]);
						x = context.getPrimitives()
								.getVariable(tokens[3]).integerValue().intValue();
						y = context.getPrimitives()
								.getVariable(tokens[4]).integerValue().intValue();
						
						getItem = Misc.getItemAtPosition(x, y, FrameUtils.getFrame(getFrameName));
					}
					else {
						getItem = null;
					}
					
					context.getPointers().setObject(tokens[1], getItem);
				}				
				else if (tokens[0].equals("getitemposition")) {
					assertVariableType(tokens[1], 1, SPointer.itemPrefix);

						Point pos = ((Item) context.getPointers().getVariable(
								tokens[1]).getValue()).getPosition();
						Integer x = pos.x;
						Integer y = pos.y;
						context.getPrimitives()
								.setValue(tokens[2], new SInteger(x));
						context.getPrimitives()
								.setValue(tokens[3], new SInteger(y));
								
					
				} else if (tokens[0].equals("getitemthickness")) {
					assertVariableType(tokens[1], 1, SPointer.itemPrefix);
					// assertPrimitiveType(tokens[2], 2);
					Float thickness = ((Item) context.getPointers()
							.getVariable(tokens[1]).getValue()).getThickness();
					context.getPrimitives().setValue(tokens[2],
							new SReal(thickness));
				} else if (tokens[0].equals("getitemwidth")) {
					assertVariableType(tokens[1], 1, SPointer.itemPrefix);
					// assertPrimitiveType(tokens[2], 2);
					Integer width = ((Item) context.getPointers().getVariable(
							tokens[1]).getValue()).getWidth();
					context.getPrimitives().setValue(tokens[2],
							new SInteger(width));
				} else if(tokens[0].equals("getitemheight")) {
					//added in by kgas1
					
					assertVariableType(tokens[1], 1, SPointer.itemPrefix);
					
					Integer height = ((Item) context.getPointers().getVariable(
							tokens[1]).getValue()).getHeight();
					context.getPrimitives().setValue(tokens[2],
							new SInteger(height));
				}				
				else if (tokens[0].equals("getitemsize")) {
					assertVariableType(tokens[1], 1, SPointer.itemPrefix);
					// assertPrimitiveType(tokens[2], 2);
					Integer size = (int) ((Item) context.getPointers()
							.getVariable(tokens[1]).getValue()).getSize();
					context.getPrimitives().setValue(tokens[2],
							new SInteger(size));
				} else if (tokens[0].equals("getitemlink")) {
					assertVariableType(tokens[1], 1, SPointer.itemPrefix);
					// assertPrimitiveType(tokens[2], 2);
					String link = ((Item) context.getPointers().getVariable(
							tokens[1]).getValue()).getAbsoluteLink();
					context.getPrimitives().setValue(tokens[2],
							new SString(link));
				} else if (tokens[0].equals("getitemdata")) {
					assertVariableType(tokens[1], 1, SPointer.itemPrefix);
					Collection<String> dataList = ((Item) context.getPointers()
							.getVariable(tokens[1]).getValue()).getData();
					String data = "";
					if (dataList.size() > 0)
						data = dataList.iterator().next();
					context.getPrimitives().setValue(tokens[2],
							new SString(data));
				} else if (tokens[0].equals("getitemaction")) {
					assertVariableType(tokens[1], 1, SPointer.itemPrefix);
					Collection<String> dataList = ((Item) context.getPointers()
							.getVariable(tokens[1]).getValue()).getAction();
					String action = "";
					if (dataList.size() > 0)
						action = dataList.iterator().next();
					context.getPrimitives().setValue(tokens[2],
							new SString(action));
				} 
				else if (tokens[0].equals("getitemfillcolor")) {
					assertVariableType(tokens[1], 1, SPointer.itemPrefix);
					// assertPrimitiveType(tokens[2], 2);
					Color itemColor = ((Item) context.getPointers()
							.getVariable(tokens[1]).getValue())
							.getPaintBackgroundColor();
					String color = itemColor.getRed() + " "
							+ itemColor.getGreen() + " " + itemColor.getBlue();
					context.getPrimitives().setValue(tokens[2],
							new SString(color));
				} else if (tokens[0].equals("getitemcolor")) {
					assertVariableType(tokens[1], 1, SPointer.itemPrefix);
					// assertPrimitiveType(tokens[2], 2);
					Color itemColor = ((Item) context.getPointers()
							.getVariable(tokens[1]).getValue()).getPaintColor();
					String color = itemColor.getRed() + " "
							+ itemColor.getGreen() + " " + itemColor.getBlue();
					context.getPrimitives().setValue(tokens[2],
							new SString(color));
				} else if (tokens[0].equals("getitemtext")) {
					assertVariableType(tokens[1], 1, SPointer.itemPrefix);
					Item item = ((Item) context.getPointers().getVariable(
							tokens[1]).getValue());
					context.getPrimitives().setValue(tokens[2],
							new SString(item.getText()));
				} else if(tokens[0].equals("getitemid")) {
					assertVariableType(tokens[1], 1, SPointer.itemPrefix);
					Item item = ((Item) context.getPointers().getVariable(
							tokens[1]).getValue());
					context.getPrimitives().setValue(tokens[2], new SInteger(item.getID()));							
					
				} else
					throw new Exception("Unsupported getItem command: "
							+ code.toString());
			} else if (tokens[0].equals("getstrchar")) {
				assertExactParametreCount(tokens, 3);
				String s = context.getPrimitives().getStringValue(tokens[1]);
				int pos = (int) context.getPrimitives().getIntegerValue(
						tokens[2]);

				context.getPrimitives().setValue(tokens[3],
						new SCharacter(s.charAt(pos - 1)));
			} else if (tokens[0].equals("getstrlength")) {
				assertExactParametreCount(tokens, 2);
				String s = context.getPrimitives().getStringValue(tokens[1]);

				context.getPrimitives().setValue(tokens[2],
						new SInteger(s.length()));
			} else if (tokens[0].equals("getelapsedtime")) {
				assertExactParametreCount(tokens, 1);
				context.getPrimitives().setValue(tokens[1],
						new SInteger(AgentStats.getMilliSecondsElapsed()));
			} else if (tokens[0].equals("getlastnumberinframeset")) {
				String framesetNameVar = DEFAULT_STRING;
				String countVar = DEFAULT_INTEGER;
				if (tokens.length > 1) {
					assertMinParametreCount(tokens, 2);
					framesetNameVar = tokens[1];
					countVar = tokens[2];
				}
				String frameset = context.getPrimitives().getStringValue(
						framesetNameVar);
				long count = FrameIO.getLastNumber(frameset);
				context.getPrimitives().setValue(countVar, new SInteger(count));
			} else if (tokens[0].equals("getlistofframesets")) {
				String stringVar = DEFAULT_ITEM;
				if (tokens.length > 1) {
					assertMinParametreCount(tokens, 1);
					stringVar = tokens[1];
				}
				context.getPrimitives().setValue(stringVar,
						FrameIO.getFramesetList());
			} else if (tokens[0].equals("getsessionstats")) {
				assertExactParametreCount(tokens, 1);
				assertVariableType(tokens[1], 1, SPointer.itemPrefix);

				String stats = SessionStats.getCurrentStats();
				Text t = (Text) context.getPointers().getVariable(tokens[1])
						.getValue();
				t.setText(stats);
			} else if (tokens[0].equals("getrandominteger")) {
				assertExactParametreCount(tokens, 3);
				int lowerBound = (int) context.getPrimitives().getIntegerValue(
						tokens[1]);
				int upperBound = (int) context.getPrimitives().getIntegerValue(
						tokens[2]);
				Random random = new Random();
				long result = Math.abs(random.nextInt())
						% (upperBound - lowerBound) + lowerBound;
				context.getPrimitives().setValue(tokens[3],
						new SInteger(result));
			} else if (tokens[0].equals("getrandomreal")) {
				assertExactParametreCount(tokens, 3);
				double lowerBound = context.getPrimitives().getDoubleValue(
						tokens[1]);
				double upperBound = context.getPrimitives().getDoubleValue(
						tokens[2]);
				Random random = new Random();
				double result = random.nextDouble() * (upperBound - lowerBound)
						+ lowerBound;
				context.getPrimitives().setValue(tokens[3], new SReal(result));
			} else if (tokens[0].equals("getrandomtextitem")) {
				assertExactParametreCount(tokens, 2);
				assertVariableType(tokens[1], 1, SPointer.framePrefix);
				assertVariableType(tokens[2], 2, SPointer.itemPrefix);
				List<Text> items = ((Frame) context.getPointers().getVariable(
						tokens[1]).getValue()).getBodyTextItems(false);
				Random random = new Random();
				int itemIndex = random.nextInt(items.size());
				context.getPointers()
						.setObject(tokens[2], items.get(itemIndex));
			} else {
				executeAction(tokens, context);
			}
		} else if (tokens[0].equals("or")) {
			for (int i = 1; i < tokens.length - 1; i++) {
				if (Primitives.isPrimitive(tokens[i])) {
					if (context.getPrimitives().getBooleanValue(tokens[i])) {
						context.getPrimitives().setValue(
								tokens[tokens.length - 1], new SBoolean(true));
						return Status.OK;
					}
				}
			}
			context.getPrimitives().setValue(tokens[tokens.length - 1],
					new SBoolean(false));
		} else if (tokens[0].equals("and")) {
			for (int i = 1; i < tokens.length - 1; i++) {
				if (Primitives.isPrimitive(tokens[i])) {
					if (!context.getPrimitives().getBooleanValue(tokens[i])) {
						context.getPrimitives().setValue(
								tokens[tokens.length - 1], new SBoolean(false));
						return Status.OK;
					}
				}
			}
			context.getPrimitives().setValue(tokens[tokens.length - 1],
					new SBoolean(true));
		} else if (tokens[0].equals("messagelnitem")
				|| tokens[0].equals("messagelineitem")) {
			String itemVar = DEFAULT_ITEM;

			if (tokens.length > 1) {
				assertExactParametreCount(tokens, 1);
				itemVar = tokens[1];
				assertVariableType(itemVar, 1, SPointer.itemPrefix);
			}
			Item message = (Item) context.getPointers().getVariable(itemVar)
					.getValue();
			try {
				MessageBay.displayMessage(((Text) message).copy());
			} catch (NullPointerException e) {
				MessageBay.displayMessage("null");
			} catch (ClassCastException e) {
				// Just ignore not text items!
				MessageBay.displayMessage(message.toString());
			} catch (Exception e) {
				// Just ignore other errors
			}
		} else if (tokens[0].equals("messageln")
				|| tokens[0].equals("messageline")
				|| tokens[0].equals("messagelnnospaces")
				|| tokens[0].equals("messagelinenospaces")
				|| tokens[0].equals("errorln") || tokens[0].equals("errorline")) {
			String message = getMessage(tokens, context, code.toString(),
					tokens[0].endsWith("nospaces") ? "" : " ", 1);

			if (tokens[0].equals("errorln") || tokens[0].equals("errorline"))
				MessageBay.errorMessage(message);
			else
				MessageBay.displayMessageAlways(message);
		} else if (tokens[0].equals("typeatrate")) {
			assertMinParametreCount(tokens, 1);
			double delay = context.getPrimitives().getDoubleValue(tokens[1]);
			String s = getMessage(tokens, context, code.toString(), " ", 2);
			DisplayIO.typeStringDirect(delay, s);
		} else if (tokens[0].equals("type") || tokens[0].equals("typenospaces")) {

			String s = getMessage(tokens, context, code.toString(), tokens[0]
					.equals("type") ? " " : "", 1);

			DisplayIO.typeStringDirect(0.025, s);
		} else if (tokens[0].equals("runstring")) {
			String codeText = getMessage(tokens, context, code.toString(), " ",
					1);
			Text dynamicCode = new Text(codeText);
			RunItem(dynamicCode, context, Status.OK);
		} else if (tokens[0].equals("runoscommand")) {
			String command = getMessage(tokens, context, code.toString(), " ",
					1);
			Runtime.getRuntime().exec(command);
		} else if (tokens[0].equals("executeoscommand")
				|| tokens[0].equals("runoscommandwithoutput")) {
			String command = getMessage(tokens, context, code.toString(), " ",
					1);
			try {
				Process p = Runtime.getRuntime().exec(command);
				// Process p = Runtime.getRuntime().exec(new String[]{"date",
				// ">", "test.date"});
				MessageBay.displayMessage(command, Color.darkGray);

				BufferedReader stdInput = new BufferedReader(
						new InputStreamReader(p.getInputStream()));
				BufferedReader stdError = new BufferedReader(
						new InputStreamReader(p.getErrorStream()));
				String message = "";
				while ((message = stdInput.readLine()) != null) {
					MessageBay.displayMessage(message);
				}
				while ((message = stdError.readLine()) != null) {
					MessageBay.errorMessage(message);
				}
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage());
			}
		} else if (tokens[0].startsWith("else")) {
			// if the if statement was false then run the else statement
			if (lastItemStatus == Status.FalseIf) {
				// check if it is a one line if statment
				if (tokens.length > 1) {
					// put together the one line statement
					StringBuilder statement = new StringBuilder();
					for (int i = 1; i < tokens.length; i++)
						statement.append(tokens[i]).append(' ');
					// create a copy of the code item to run
					Text copiedCode = code.copy();
					copiedCode.setText(statement.toString());
					return RunItem(copiedCode, context, Status.OK);
				} else {
					return RunFrameAndReportError(code, context);
				}
			} else if (lastItemStatus == Status.TrueIf) {
				return Status.OK;
			}
			throw new RuntimeException("Else without matching If statement");
		} else if (tokens[0].startsWith("if")) {
			Boolean result = null;
			int parametres = 1;
			String variable = DEFAULT_ITEM;
			String ifStatement = tokens[0];
			// Set the default variable
			if (tokens.length == 1) {
				if (ifStatement.equals("if") || ifStatement.equals("ifnot"))
					variable = DEFAULT_STRING;
				else if (ifStatement.equals("ifzero")
						|| ifStatement.equals("ifnotzero"))
					variable = DEFAULT_INTEGER;
			} else {
				variable = tokens[1];
			}

			if (ifStatement.equals("if")) {
				result = context.getPrimitives().getBooleanValue(variable);
			} else if (ifStatement.equals("ifnot")) {
				result = !context.getPrimitives().getBooleanValue(variable);
			} else if (ifStatement.equals("ifdefined")) {
				result = context.isDefined(tokens[1]);
			} else if (ifStatement.equals("ifnotdefined")) {
				result = !context.isDefined(tokens[1]);
			} else if (ifStatement.equals("ifzero")) {
				result = context.getPrimitives().getIntegerValue(variable) == 0;
			} else if (ifStatement.equals("ifnotzero")) {
				result = context.getPrimitives().getIntegerValue(variable) != 0;
			} else if (tokens[0].equals("ifeq")) {
				result = context.equalValues(tokens[1], tokens[2]);
				parametres = 2;
			} else if (tokens[0].equals("ifeqnocase")) {
				result = context.getPrimitives().equalValuesNoCase(tokens[1],
						tokens[2]);
				parametres = 2;
			} else if (tokens[0].equals("ifnoteqnocase")) {
				result = !context.getPrimitives().equalValuesNoCase(tokens[1],
						tokens[2]);
				parametres = 2;
			} else if (tokens[0].equals("ifnoteq")) {
				result = !context.equalValues(tokens[1], tokens[2]);
				parametres = 2;
			} else if (tokens[0].equals("ifless")) {
				result = context.getPrimitives().compareValues(tokens[1],
						tokens[2]) < 0;
				parametres = 2;
			} else if (tokens[0].equals("ifgtr")) {
				result = context.getPrimitives().compareValues(tokens[1],
						tokens[2]) > 0;
				parametres = 2;
			} else if (tokens[0].equals("ifgeq")) {
				result = context.getPrimitives().compareValues(tokens[1],
						tokens[2]) >= 0;
				parametres = 2;
			} else if (tokens[0].equals("ifleq")) {
				result = context.getPrimitives().compareValues(tokens[1],
						tokens[2]) <= 0;
				parametres = 2;
			} else if (tokens[0].equals("ifexistingframe")) {
				result = FrameIO.canAccessFrame(context.getPrimitives()
						.getStringValue(tokens[1]));
			} else if (tokens[0].equals("ifexistingframeset")) {
				String framesetName = context.getPrimitives().getStringValue(
						tokens[1]);
				result = FrameIO.canAccessFrameset(framesetName);
			} else {
				// assertVariableType(variable, 1, SPointer.itemPrefix);
				if (ifStatement.equals("ifannotation")) {
					result = ((Item) context.getPointers()
							.getVariable(variable).getValue()).isAnnotation();
				} else if (ifStatement.equals("iflinked")) {
					result = ((Item) context.getPointers()
							.getVariable(variable).getValue()).getLink() != null;
				} else if (ifStatement.equals("ifactioned")) {
					result = ((Item) context.getPointers()
							.getVariable(variable).getValue()).hasAction();
				} else if (ifStatement.equals("ifnotactioned")) {
					result = !((Item) context.getPointers().getVariable(
							variable).getValue()).hasAction();
				} else if (ifStatement.equals("ifbodytext")) {
					Item i = (Item) context.getPointers().getVariable(variable)
							.getValue();
					result = i instanceof Text && !i.isFrameName()
							&& !i.isFrameTitle();
				} else if (ifStatement.equals("ifnotannotation")) {
					result = !((Item) context.getPointers().getVariable(
							variable).getValue()).isAnnotation();
				} else if (ifStatement.equals("ifnotlinked")) {
					result = ((Item) context.getPointers()
							.getVariable(variable).getValue()).getLink() == null;
				} else if (ifStatement.equals("ifnotbodytext")) {
					Item i = (Item) context.getPointers().getVariable(variable)
							.getValue();
					result = !(i instanceof Text) || i.isFrameName()
							|| i.isFrameTitle();
				}
			}

			if (result == null)
				throw new RuntimeException("Invalid If statement");
			// Now check if we need to run the code
			else if (result) {
				Status status;
				// check if it is a one line if statement
				if (tokens.length > parametres + 1) {
					// put together the one line statement
					StringBuilder statement = new StringBuilder();
					for (int i = parametres + 1; i < tokens.length; i++)
						statement.append(tokens[i]).append(' ');
					// create a copy of the code item to run
					Text copiedCode = code.copy();
					copiedCode.setText(statement.toString());
					status = RunItem(copiedCode, context, Status.OK);
				} else {
					status = RunFrameAndReportError(code, context);
				}
				if (status == Status.OK) {
					return Status.TrueIf;
				} else {
					return status;
				}
			}
			return Status.FalseIf;
		} // Look for variable length methods
		else if (tokens[0].equals("attachitemtocursor")) {
			String itemVar = DEFAULT_ITEM;

			if (tokens.length > 1) {
				assertExactParametreCount(tokens, 1);
				assertVariableType(tokens[1], 1, SPointer.itemPrefix);
				itemVar = tokens[1];
			}
			Item item = (Item) context.getPointers().getVariable(itemVar)
					.getValue();
			item
					.setPosition(FrameMouseActions.MouseX,
							FrameMouseActions.MouseY);
			FrameMouseActions.pickup(item);
		} else if (tokens[0].equals("attachstrtocursor")) {
			String stringVar = DEFAULT_STRING;

			if (tokens.length > 1) {
				assertExactParametreCount(tokens, 1);
				stringVar = tokens[1];
			}
			String s = context.getPrimitives().getStringValue(stringVar);
			Frame frame = DisplayIO.getCurrentFrame();
			Item item = frame.createNewText(s);
			item
					.setPosition(FrameMouseActions.MouseX,
							FrameMouseActions.MouseY);
			FrameMouseActions.pickup(item);
		} else if (tokens[0].equals("additemtoframe")) {
			String itemVar = DEFAULT_ITEM;
			String frameVar = DEFAULT_FRAME;

			if (tokens.length > 1) {
				assertExactParametreCount(tokens, 2);
				assertVariableType(tokens[1], 1, SPointer.framePrefix);
				assertVariableType(tokens[2], 2, SPointer.itemPrefix);
				itemVar = tokens[2];
				frameVar = tokens[1];
			}
			Frame frame = (Frame) context.getPointers().getVariable(frameVar)
					.getValue();
			Item item = (Item) context.getPointers().getVariable(itemVar)
					.getValue();
			frame.addItem(item);
		} else if (tokens[0].equals("connectdots")) {
			assertMinParametreCount(tokens, 2);
			assertVariableType(tokens[1], 1, SPointer.itemPrefix);
			assertVariableType(tokens[2], 2, SPointer.itemPrefix);
			Item dot1 = null;
			Item dot2 = null;
			try {
				dot1 = (Item) context.getPointers().getVariable(tokens[1])
						.getValue();
			} catch (Exception e) {
				throw new IncorrectTypeException("Dot", 1);
			}
			try {
				dot2 = (Item) context.getPointers().getVariable(tokens[2])
						.getValue();
			} catch (Exception e) {
				throw new IncorrectTypeException("Dot", 2);
			}
			Frame frame = dot1.getParent();
			frame.addItem(new Line(dot1, dot2, frame.getNextItemID()));
		} else if (tokens[0].equals("createitem")
				|| tokens[0].equals("createtext")) {
			assertMinParametreCount(tokens, 4);
			assertVariableType(tokens[1], 1, SPointer.framePrefix);
			assertVariableType(tokens[4], 4, SPointer.itemPrefix);
			Frame frame = (Frame) context.getPointers().getVariable(tokens[1])
					.getValue();
			Item newItem;
			int x = (int) context.getPrimitives().getIntegerValue(tokens[2]);
			int y = (int) context.getPrimitives().getIntegerValue(tokens[3]);
			// check for the option text and action for the new item
			if (tokens.length > 5) {
				String newText = context.getPrimitives().getStringValue(
						tokens[5]);
				String newAction = null;
				if (tokens.length > 6) {
					newAction = context.getPrimitives().getStringValue(
							tokens[6]);
				}
				newItem = frame.addText(x, y, newText, newAction);
			} else {
				if (tokens[0].equals("createtext")) {
					newItem = frame.createNewText();
					newItem.setPosition(x, y);
				} else {
					// create a point if the optional params are not provided
					newItem = frame.addDot(x, y);
				}
			}
			context.getPointers().setObject(tokens[4], newItem);
		} else if (tokens[0].equals("deleteitem")) {
			assertMinParametreCount(tokens, 1);
			assertVariableType(tokens[1], 1, SPointer.itemPrefix);
			Item item = (Item) context.getPointers().getVariable(tokens[1])
					.getValue();
			item.delete();
		} else if (tokens[0].equals("deleteframe")) {
			assertMinParametreCount(tokens, 1);
			assertVariableType(tokens[1], 1, SPointer.framePrefix);
			Frame frame = (Frame) context.getPointers().getVariable(tokens[1])
					.getValue();
			String errorMessage = "Error deleting " + frame.getName();
			boolean success = false;
			try {
				success = FrameIO.DeleteFrame(frame) != null;
				if (!success && _verbose)
					MessageBay.warningMessage(errorMessage);
			} catch (Exception e) {
				// If an exception is thrown then success is false
				if (_verbose) {
					MessageBay.warningMessage(errorMessage
							+ (e.getMessage() != null ? ". " + e.getMessage()
									: ""));
				}
			}
			if (tokens.length > 2) {
				context.getPrimitives().setValue(tokens[2],
						new SBoolean(success));
			}
		} else if (tokens[0].equals("deleteframeset")) {
			assertMinParametreCount(tokens, 1);
			String framesetName = context.getPrimitives().getStringValue(
					tokens[1]);
			boolean success = FrameIO.deleteFrameset(framesetName);
			if (!success && _verbose) {
				MessageBay.warningMessage("Error deleting " + framesetName);
			}
			if (tokens.length > 2) {
				context.getPrimitives().setValue(tokens[2],
						new SBoolean(success));
			}
		} else if (tokens[0].equals("copyitem")) {
			assertMinParametreCount(tokens, 2);
			assertVariableType(tokens[1], 1, SPointer.itemPrefix);
			assertVariableType(tokens[2], 2, SPointer.itemPrefix);
			Item item = (Item) context.getPointers().getVariable(tokens[1])
					.getValue();
			Item copy = item.copy();
			context.getPointers().setObject(tokens[2], copy);
		} else if (tokens[0].equals("copyframeset")) {
			assertMinParametreCount(tokens, 2);
			String framesetToCopy = context.getPrimitives().getStringValue(
					tokens[1]);
			String copiedFrameset = context.getPrimitives().getStringValue(
					tokens[2]);
			boolean success = FrameIO.CopyFrameset(framesetToCopy,
					copiedFrameset);
			if (!success && _verbose)
				MessageBay.warningMessage("Error copying " + framesetToCopy);
			if (tokens.length > 3) {
				context.getPrimitives().setValue(tokens[3],
						new SBoolean(success));
			}
		} else if (tokens[0].equals("copyframe")) {
			assertMinParametreCount(tokens, 2);
			assertVariableType(tokens[1], 1, SPointer.framePrefix);
			assertVariableType(tokens[2], 2, SPointer.framePrefix);
			Frame frameToCopy = (Frame) context.getPointers().getVariable(
					tokens[1]).getValue();
			FrameIO.SuspendCache();
			Frame freshCopy = FrameIO.LoadFrame(frameToCopy.getName());
			// Change the frameset if one was provided
			if (tokens.length > 3) {
				String destinationFrameset = context.getPrimitives()
						.getStringValue(tokens[3]);
				freshCopy.setFrameset(destinationFrameset);
			}// Otherwise add it to the end of this frameset
			int nextNumber = FrameIO.getLastNumber(freshCopy.getFramesetName()) + 1;
			// if the frameset doesnt already exist then create it
			if (nextNumber <= 0) {
				try {
					FrameIO.CreateFrameset(freshCopy.getFramesetName(),
							frameToCopy.getPath());
					nextNumber = 1;
				} catch (Exception e) {
				}
			} else {
				Frame zero = FrameIO.LoadFrame(freshCopy.getFramesetName()
						+ "0");
				freshCopy.setPath(zero.getPath());
			}
			boolean success = false;
			if (nextNumber > 0) {
				freshCopy.setFrameNumber(nextNumber);
				context.getPointers().setObject(tokens[2], freshCopy);
				String fileContents = FrameIO.ForceSaveFrame(freshCopy);
				success = fileContents != null;
			}
			FrameIO.ResumeCache();
			if (success) {
				// Need to add the new copy to the cache in case it is edited by
				// other simple statements
				FrameIO.addToCache(freshCopy);
			}
			if (!success && _verbose)
				MessageBay.warningMessage("Error copying "
						+ frameToCopy.getName());
			if (tokens.length > 4) {
				context.getPrimitives().setValue(tokens[4],
						new SBoolean(success));
			}
		} else if (tokens[0].equals("createframe")) {

			String framesetName = DEFAULT_STRING;
			String frameVar = DEFAULT_FRAME;

			if (tokens.length > 1) {
				assertMinParametreCount(tokens, 2);
				assertVariableType(tokens[2], 2, SPointer.framePrefix);
				framesetName = tokens[1];
				frameVar = tokens[2];
			}

			if (tokens.length > 3) {
				context.createFrame(framesetName, frameVar, tokens[3]);
			} else
				context.createFrame(framesetName, frameVar, null);
		} else if (tokens[0].equals("closeframe")) {
			String frameVar = DEFAULT_FRAME;

			if (tokens.length > 1) {
				assertVariableType(tokens[1], 1, SPointer.framePrefix);
				frameVar = tokens[1];
			}

			if (tokens.length > 2) {
				// assertPrimitiveType(tokens[3], 3);
				context.closeFrame(frameVar, tokens[3]);
			} else
				context.closeFrame(frameVar, null);
		} else if (tokens[0].equals("readframe")
				|| tokens[0].equals("openframe")) {

			String frameName = DEFAULT_STRING;
			String frameVar = DEFAULT_FRAME;

			if (tokens.length > 1) {
				assertMinParametreCount(tokens, 2);
				assertVariableType(tokens[2], 2, SPointer.framePrefix);
				frameName = tokens[1];
				frameVar = tokens[2];
				// assertPrimitiveType(frameName, 1);
			}

			if (tokens.length > 3) {
				// assertPrimitiveType(tokens[3], 3);
				context.readFrame(frameName, frameVar, tokens[3]);
			} else
				context.readFrame(frameName, frameVar, null);
		} else if (tokens[0].equals("exitexpeditee")) {
			Browser._theBrowser.exit();
		} else if (tokens[0].equals("readkbdcond")) {

			String nextCharVarName = DEFAULT_CHAR;
			String wasCharVarName = DEFAULT_BOOLEAN;

			if (tokens.length > 1) {
				assertMinParametreCount(tokens, 2);
				assertVariableType(tokens[2], 2, SPointer.framePrefix);
				nextCharVarName = tokens[1];
				wasCharVarName = tokens[2];
			}

			Character nextChar = _KeyStrokes.poll();
			boolean hasChar = nextChar != null;
			context.getPrimitives().setValue(wasCharVarName,
					new SBoolean(hasChar));
			if (hasChar)
				context.getPrimitives().setValue(nextCharVarName,
						new SCharacter(nextChar));
		} else if (tokens[0].equals("createassociation")) {

			String associationVar = DEFAULT_ASSOCIATION;

			if (tokens.length > 0) {
				assertVariableType(tokens[1], 2, SPointer.associationPrefix);
				associationVar = tokens[1];
			}
			Map<String, String> newMap = new HashMap<String, String>();
			context.getPointers().setObject(associationVar, newMap);
		} else if (tokens[0].equals("deleteassociation")) {

			String associationVar = DEFAULT_ASSOCIATION;

			if (tokens.length > 0) {
				assertVariableType(tokens[1], 2, SPointer.associationPrefix);
				associationVar = tokens[1];
			}
			context.getPointers().delete(associationVar);
		} else if (tokens[0].equals("openreadfile")) {
			assertVariableType(tokens[1], 1, SString.prefix);
			assertVariableType(tokens[2], 2, SPointer.filePrefix);

			if (tokens.length > 3) {
				assertVariableType(tokens[3], 3, SBoolean.prefix);
				context.openReadFile(tokens[1], tokens[2], tokens[3]);
			} else
				context.openReadFile(tokens[1], tokens[2]);
		} else if (tokens[0].equals("readlinefile")
				|| tokens[0].equals("readlnfile")) {
			assertVariableType(tokens[1], 1, SPointer.filePrefix);

			if (tokens.length > 3) {
				assertVariableType(tokens[3], 3, SBoolean.prefix);
				context.readLineFile(tokens[1], tokens[2], tokens[3]);
			} else
				context.readLineFile(tokens[1], tokens[2]);
		} else if (tokens[0].equals("readitemfile")) {
			assertVariableType(tokens[1], 1, SPointer.filePrefix);
			assertVariableType(tokens[2], 1, SPointer.itemPrefix);

			if (tokens.length > 3) {
				assertVariableType(tokens[3], 3, SBoolean.prefix);
				context.readItemFile(tokens[1], tokens[2], tokens[3]);
			} else
				context.readItemFile(tokens[1], tokens[2]);
		} else if (tokens[0].equals("openwritefile")) {
			assertVariableType(tokens[1], 1, SString.prefix);
			assertVariableType(tokens[2], 2, SPointer.filePrefix);

			if (tokens.length > 3) {
				assertVariableType(tokens[3], 3, SBoolean.prefix);
				context.openWriteFile(tokens[1], tokens[2], tokens[3]);
			} else
				context.openWriteFile(tokens[1], tokens[2]);
		} else if (tokens[0].equals("writefile")
				|| tokens[0].equals("writelinefile")
				|| tokens[0].equals("writelnfile")) {
			assertVariableType(tokens[1], 1, SPointer.filePrefix);

			StringBuffer textToWrite = new StringBuffer();
			if (tokens.length == 1) {
				textToWrite.append(context.getPrimitives().getVariable(
						DEFAULT_STRING).stringValue()
						+ " ");
			} else {
				for (int i = 2; i < tokens.length; i++) {
					if (Primitives.isPrimitive(tokens[i])) {
						textToWrite.append(context.getPrimitives().getVariable(
								tokens[i]).stringValue());
					} else
						throw new Exception("Illegal parametre: " + tokens[i]
								+ " in " + code.toString());
				}
			}

			if (!tokens[0].equals("writefile"))
				textToWrite.append(Text.LINE_SEPARATOR);
			context.writeFile(tokens[1], textToWrite.toString());
		} else if (tokens[0].equals("displayframeset")) {
			assertMinParametreCount(tokens, 1);
			String framesetName = context.getPrimitives().getStringValue(
					tokens[1]);
			int lastFrameNo = FrameIO.getLastNumber(framesetName);
			int firstFrameNo = 0;
			double pause = 0.0;
			// get the first and last frames to display if they were proided
			if (tokens.length > 2) {
				firstFrameNo = (int) context.getPrimitives().getIntegerValue(
						tokens[2]);
				if (tokens.length > 3) {
					lastFrameNo = (int) context.getPrimitives()
							.getIntegerValue(tokens[3]);
					if (tokens.length > 4) {
						pause = context.getPrimitives().getDoubleValue(
								tokens[4]);
					}
				}
			}
			Runtime runtime = Runtime.getRuntime();
			// Display the frames
			for (int i = firstFrameNo; i <= lastFrameNo; i++) {
				Frame frame = FrameIO.LoadFrame(framesetName + i);
				if (frame != null) {
					double thisFramesPause = pause;
					// check for change in delay for this frame only
					Item pauseItem = ItemUtils.FindTag(frame.getItems(),
							"@DisplayFramePause:");
					if (pauseItem != null) {
						try {
							// attempt to read in the delay value
							thisFramesPause = Double.parseDouble(ItemUtils
									.StripTag(
											((Text) pauseItem).getFirstLine(),
											"@DisplayFramePause"));
						} catch (NumberFormatException nfe) {
						}
					}
					DisplayIO.setCurrentFrame(frame, false);
					pause(thisFramesPause);

					long freeMemory = runtime.freeMemory();
					if (freeMemory < DisplayTree.GARBAGE_COLLECTION_THRESHOLD) {
						runtime.gc();
						MessageBay.displayMessage("Force Garbage Collection!");
					}
				}
			}
		} else if (tokens[0].equals("createframeset")) {
			String framesetName = DEFAULT_STRING;
			String successVar = null;
			if (tokens.length > 1) {
				framesetName = tokens[1];
				if (tokens.length > 2)
					successVar = tokens[2];
			}
			context.createFrameset(framesetName, successVar);
		} else if (tokens[0].equals("writetree")) {
			assertMinParametreCount(tokens, 3);
			assertVariableType(tokens[1], 1, SPointer.framePrefix);
			Frame source = (Frame) context.getPointers().getVariable(tokens[1])
					.getValue();
			String format = context.getPrimitives().getStringValue(tokens[2]);
			String fileName = context.getPrimitives().getStringValue(tokens[3]);
			WriteTree wt = new WriteTree(format, fileName);
			if (wt.initialise(source, null)) {
				_agent = wt;
				wt.run();
				_agent = null;
			}
		} else if (tokens[0].equals("concatstr")) {
			assertMinParametreCount(tokens, 3);
			String resultVar = tokens[tokens.length - 1];

			StringBuilder sb = new StringBuilder();
			// loop through all the strings concatenating them
			for (int i = 1; i < tokens.length - 1; i++) {
				// assertPrimitiveType(tokens[i], i);
				sb.append(context.getPrimitives().getStringValue(tokens[i]));
			}
			context.getPrimitives().setValue(resultVar,
					new SString(sb.toString()));
		} else if (tokens[0].equals("convstrlower")) {
			assertExactParametreCount(tokens, 1);
			// assertPrimitiveType(tokens[1], 1);
			context.getPrimitives().setValue(
					tokens[1],
					new SString(context.getPrimitives().getStringValue(
							tokens[1]).toLowerCase()));
		} else if (tokens[0].equals("convstrupper")) {
			assertExactParametreCount(tokens, 1);
			// assertPrimitiveType(tokens[1], 1);
			context.getPrimitives().setValue(
					tokens[1],
					new SString(context.getPrimitives().getStringValue(
							tokens[1]).toUpperCase()));
		} else if (tokens[0].equals("countcharsinstr")) {
			assertExactParametreCount(tokens, 3);
			String s = context.getPrimitives().getStringValue(tokens[1]);
			String pattern = context.getPrimitives().getStringValue(tokens[2]);
			int count = countCharsInString(s, pattern);
			context.getPrimitives().setValue(tokens[3], new SInteger(count));
		} else if (tokens[0].equals("countcharsinitem")) {
			assertExactParametreCount(tokens, 3);
			assertVariableType(tokens[1], 1, SPointer.itemPrefix);
			Item item = (Item) context.getPointers().getVariable(tokens[1])
					.getValue();
			String pattern = context.getPrimitives().getStringValue(tokens[2]);
			int count = 0;
			if (item instanceof Text)
				count = countCharsInString(((Text) item).getText(), pattern);
			context.getPrimitives().setValue(tokens[3], new SInteger(count));
		} else if (tokens[0].equals("clearframe")) {
			String frameVar = DEFAULT_FRAME;
			if (tokens.length > 1) {
				assertMinParametreCount(tokens, 1);
				assertVariableType(tokens[1], 1, SPointer.framePrefix);
				frameVar = tokens[1];
			}
			boolean success = true;
			try {
				Frame frameToClear = (Frame) context.getPointers().getVariable(
						frameVar).getValue();
				frameToClear.clear(false);
				assert (frameToClear.getItems().size() <= 1);
			} catch (Exception e) {
				success = false;
			}
			if (tokens.length > 2) {
				assertExactParametreCount(tokens, 2);
				context.getPrimitives().setValue(tokens[2],
						new SBoolean(success));
			}
		} else if (tokens[0].equals("parseframename")) {
			assertExactParametreCount(tokens, 4);
			String frameName = context.getPrimitives()
					.getStringValue(tokens[1]);
			String frameSet = "";
			int frameNo = -1;
			boolean success = true;
			try {
				frameSet = Conversion.getFramesetName(frameName, false);
				frameNo = Conversion.getFrameNumber(frameName);
			} catch (Exception e) {
				success = false;
				if (_verbose)
					MessageBay.warningMessage("Error parsing " + frameName);
			}
			// assertPrimitiveType(tokens[2], 2);
			context.getPrimitives().setValue(tokens[2], new SBoolean(success));

			// assertPrimitiveType(tokens[3], 3);
			context.getPrimitives().setValue(tokens[3], new SString(frameSet));

			// assertPrimitiveType(tokens[4], 4);
			context.getPrimitives().setValue(tokens[4], new SInteger(frameNo));
		} else if (tokens[0].equals("parsestr")) {
			assertMinParametreCount(tokens, 2);
			// assertPrimitiveType(tokens[1], 1);
			// assertPrimitiveType(tokens[2], 2);

			String s = context.getPrimitives().getStringValue(tokens[1]);

			String separator = context.getPrimitives()
					.getStringValue(tokens[2]);

			String[] split = s.split(separator, tokens.length - 4);

			if (tokens.length > 3) {
				// assertPrimitiveType(tokens[3], 3);
				int count = split.length;
				// if the string is not blank and its got a remainder then
				// decrease the count by 1 to account for the remainder
				if (split.length != 0 && split.length > tokens.length - 5)
					count--;

				context.getPrimitives()
						.setValue(tokens[3], new SInteger(count));

				if (tokens.length > 4) {
					// Set the remainder string
					// assertPrimitiveType(tokens[4], 4);
					if (split.length < tokens.length - 4)
						context.getPrimitives().setValue(tokens[4],
								new SString());
					else
						context.getPrimitives().setValue(tokens[4],
								new SString(split[split.length - 1]));

					// Set the strings for each of the vars
					if (tokens.length > 5) {
						for (int i = 5; i < tokens.length; i++) {
							// assertPrimitiveType(tokens[i], i);
							if (split.length < i - 4)
								context.getPrimitives().setValue(tokens[i],
										new SString());
							else
								context.getPrimitives().setValue(tokens[i],
										new SString(split[i - 5]));
						}
					}
				}
			}
		} else if (tokens[0].equals("stripstr")) {
			assertExactParametreCount(tokens, 2);
			String s = context.getPrimitives().getStringValue(tokens[1]);
			String charsToStrip = context.getPrimitives().getStringValue(
					tokens[2]);
			for (int i = 0; i < charsToStrip.length(); i++)
				s = s.replaceAll(charsToStrip.substring(i, i + 1), "");
			context.getPrimitives().setValue(tokens[1], new SString(s));
		} else if (tokens[0].equals("subststr")) {
			assertExactParametreCount(tokens, 3);
			String oldString = context.getPrimitives()
					.getStringValue(tokens[2]);
			String newString = context.getPrimitives()
					.getStringValue(tokens[3]);
			String result = context.getPrimitives().getStringValue(tokens[1]);
			result = result.replaceAll(oldString, newString);
			context.getPrimitives().setValue(tokens[1], new SString(result));
		} else if (tokens[0].equals("substr")) {
			assertExactParametreCount(tokens, 4);
			int startPos = (int) context.getPrimitives().getIntegerValue(
					tokens[2]) - 1;
			int length = (int) context.getPrimitives().getIntegerValue(
					tokens[3]);
			String s = context.getPrimitives().getStringValue(tokens[1]);
			String result;
			if (startPos + length < s.length())
				result = s.substring(startPos, startPos + length);
			else
				result = s.substring(startPos);
			context.getPrimitives().setValue(tokens[4], new SString(result));
		} else if (tokens[0].equals("pause")) {
			String lengthVar = DEFAULT_REAL;

			if (tokens.length > 1) {
				assertExactParametreCount(tokens, 1);
				lengthVar = tokens[1];
			}

			pause(context.getPrimitives().getDoubleValue(lengthVar));
		} else if (tokens[0].equals("waitforagent")) {
			while (DefaultAgent.isAgentRunning()) {
				Thread.sleep(100);
			}
		} else if (tokens[0].equals("glidecursorto")) {
			assertMinParametreCount(tokens, 2);
			int finalX = (int) context.getPrimitives().getIntegerValue(
					tokens[1]);
			int finalY = (int) context.getPrimitives().getIntegerValue(
					tokens[2]);
			int milliseconds = 1000;
			if (tokens.length > 3)
				milliseconds = (int) (context.getPrimitives().getDoubleValue(
						tokens[3]) * 1000);

			int initialX = DisplayIO.getMouseX();
			int initialY = FrameMouseActions.getY();

			final int timeInterval = 40;

			int deltaX = (int) (finalX - initialX);
			int deltaY = (int) (finalY - initialY);

			int intervals = milliseconds / timeInterval;
			for (double i = 0; i < intervals; i++) {
				int newX = initialX + (int) (deltaX * i / intervals);
				int newY = initialY + (int) (deltaY * i / intervals);
				Thread.yield();
				Thread.sleep(timeInterval);
				DisplayIO.setCursorPosition(newX, newY);
				// DisplayIO.repaint();
			}
			// Thread.yield();
			Thread.sleep(milliseconds % timeInterval);
			DisplayIO.setCursorPosition(finalX, finalY);
		} else if (tokens[0].equals("glideitemto")) {
			assertMinParametreCount(tokens, 3);
			assertVariableType(tokens[1], 1, SPointer.itemPrefix);
			Item item = (Item) context.getPointers().getVariable(tokens[1])
					.getValue();
			int finalX = (int) context.getPrimitives().getIntegerValue(
					tokens[2]);
			int finalY = (int) context.getPrimitives().getIntegerValue(
					tokens[3]);

			// DisplayIO.setCursorPosition(item.getX(), item.getY());
			// FrameMouseActions.pickup(item);

			int milliseconds = 1000;
			if (tokens.length > 4)
				milliseconds = (int) (context.getPrimitives().getDoubleValue(
						tokens[4]) * 1000);

			int initialX = item.getX();
			int initialY = item.getY();
			// int initialX = DisplayIO.getMouseX();
			// int initialY = DisplayIO.getMouseY();

			final int timeInterval = 40;

			int deltaX = (int) (finalX - initialX);
			int deltaY = (int) (finalY - initialY);

			int intervals = milliseconds / timeInterval;
			for (double i = 0; i < intervals; i++) {
				int newX = initialX + (int) (deltaX * i / intervals);
				int newY = initialY + (int) (deltaY * i / intervals);
				Thread.yield();
				Thread.sleep(timeInterval);
				// DisplayIO.setCursorPosition(newX, newY);

				item.setPosition(newX, newY);
				FrameGraphics.Repaint();
			}
			// Thread.yield();
			Thread.sleep(milliseconds % timeInterval);
			item.setPosition(finalX, finalY);
			// DisplayIO.setCursorPosition(finalX, finalY);
			FrameMouseActions.anchor(item);
			FreeItems.getInstance().clear();
			FrameGraphics.Repaint();
			// FrameMouseActions.updateCursor();
		}
		// Now look for fixed parametre statements
		else if (tokens[0].equals(EXIT_TEXT)) {
			return Status.Exit;
		} else if (tokens[0].equals(LOOP_TEXT)) {
			Status status = Status.OK;
			// Check if its a counter loop
			if (tokens.length > 1) {
				// Get the number of times to repeat the loop
				long finalCount = context.getPrimitives().getIntegerValue(
						tokens[1]);
				String counterVar = tokens.length > 2 ? tokens[2] : null;
				long count = 0;
				while ((status == Status.OK || status == Status.Continue)
						&& (count < finalCount)) {
					count++;
					// Update the counter variable
					if (counterVar != null) {
						context.getPrimitives().setValue(counterVar,
								new SInteger(count));
					}
					status = RunFrameAndReportError(code, context);
					pause(code);
				}
			} else {
				// Keep looping until break or exit occurs
				while (status == Status.OK || status == Status.Continue) {
					status = RunFrameAndReportError(code, context);
					pause(code);
				}
			}
			if (status == Status.Continue || status == Status.Break)
				status = Status.OK;
			return status;
		} else if (tokens[0].equals(CONTINUE_TEXT)
				|| tokens[0].equals(CONTINUE2_TEXT)) {
			return Status.Continue;
		} else if (tokens[0].equals(BREAK_TEXT)
				|| tokens[0].equals(BREAK2_TEXT)) {
			return Status.Break;
		} else if (tokens[0].equals(RETURN_TEXT)) {
			return Status.Return;
		} else if (tokens[0].equals("pressleftbutton")) {
			assertExactParametreCount(tokens, 0);
			DisplayIO.pressMouse(InputEvent.BUTTON1_MASK);
		} else if (tokens[0].equals("pressmiddlebutton")) {
			assertExactParametreCount(tokens, 0);
			DisplayIO.pressMouse(InputEvent.BUTTON2_MASK);
		} else if (tokens[0].equals("pressrightbutton")) {
			assertExactParametreCount(tokens, 0);
			DisplayIO.pressMouse(InputEvent.BUTTON3_MASK);
		} else if (tokens[0].equals("releaseleftbutton")) {
			assertExactParametreCount(tokens, 0);
			DisplayIO.releaseMouse(InputEvent.BUTTON1_MASK);
		} else if (tokens[0].equals("releasemiddlebutton")) {
			assertExactParametreCount(tokens, 0);
			DisplayIO.releaseMouse(InputEvent.BUTTON2_MASK);
		} else if (tokens[0].equals("releaserightbutton")) {
			assertExactParametreCount(tokens, 0);
			DisplayIO.releaseMouse(InputEvent.BUTTON3_MASK);
		} else if (tokens[0].equals("clickleftbutton")) {
			assertExactParametreCount(tokens, 0);
			FrameMouseActions.leftButton();
			// DisplayIO.clickMouse(InputEvent.BUTTON1_MASK);
		} else if (tokens[0].equals("clickmiddlebutton")) {
			assertExactParametreCount(tokens, 0);
			FrameMouseActions.middleButton();
			// DisplayIO.clickMouse(InputEvent.BUTTON2_MASK);
		} else if (tokens[0].equals("clickrightbutton")) {
			assertExactParametreCount(tokens, 0);
			FrameMouseActions.rightButton();
			// DisplayIO.clickMouse(InputEvent.BUTTON3_MASK);
		} else if (tokens[0].equals("repaint")) {
			assertExactParametreCount(tokens, 0);
			// FrameGraphics.Repaint();
			FrameGraphics.requestRefresh(true);
		} else if (tokens[0].equals("add")) {
			assertMaxParametreCount(tokens, 3);
			switch (tokens.length) {
			case 1:
				context.getPrimitives().add(DEFAULT_INTEGER);
				break;
			case 2:
				context.getPrimitives().add(tokens[1]);
				break;
			case 3:
				context.getPrimitives().add(tokens[2], tokens[1]);
				break;
			case 4:
				context.getPrimitives().add(tokens[1], tokens[2], tokens[3]);
				break;
			default:
				assert (false);
			}
		} else if (tokens[0].equals("subtract")) {
			assertMaxParametreCount(tokens, 3);
			switch (tokens.length) {
			case 1:
				context.getPrimitives().subtract(DEFAULT_INTEGER);
				break;
			case 2:
				context.getPrimitives().subtract(tokens[1]);
				break;
			case 3:
				context.getPrimitives().subtract(tokens[2], tokens[1]);
				break;
			case 4:
				context.getPrimitives().subtract(tokens[1], tokens[2],
						tokens[3]);
				break;
			default:
				assert (false);
			}
		} else if (tokens[0].equals("multiply")) {
			assertMinParametreCount(tokens, 2);
			assertMaxParametreCount(tokens, 3);
			switch (tokens.length) {
			case 3:
				context.getPrimitives().multiply(tokens[2], tokens[1]);
				break;
			case 4:
				context.getPrimitives().multiply(tokens[1], tokens[2],
						tokens[3]);
				break;
			default:
				assert (false);
			}
		} else if (tokens[0].equals("divide")) {
			assertMinParametreCount(tokens, 2);
			assertMaxParametreCount(tokens, 3);
			switch (tokens.length) {
			case 3:
				context.getPrimitives().divide(tokens[2], tokens[1]);
				break;
			case 4:
				context.getPrimitives().divide(tokens[1], tokens[2], tokens[3]);
				break;
			default:
				assert (false);
			}
		} else if (tokens[0].equals("modulo")) {
			assertExactParametreCount(tokens, 3);
			context.getPrimitives().modulo(tokens[1], tokens[2], tokens[3]);
		} else if (tokens[0].equals("power")) {
			assertExactParametreCount(tokens, 3);
			context.getPrimitives().power(tokens[1], tokens[2], tokens[3]);
		} else if (tokens[0].equals("not")) {
			assertExactParametreCount(tokens, 2);
			context.getPrimitives().not(tokens[1], tokens[2]);
		} else if (tokens[0].equals("exp")) {
			assertExactParametreCount(tokens, 2);
			context.getPrimitives().exp(tokens[1], tokens[2]);
		} else if (tokens[0].equals("log")) {
			assertExactParametreCount(tokens, 2);
			context.getPrimitives().log(tokens[2], tokens[2]);
		} else if (tokens[0].equals("log10")) {
			assertExactParametreCount(tokens, 2);
			context.getPrimitives().log10(tokens[1], tokens[2]);
		} else if (tokens[0].equals("sqrt")) {
			assertExactParametreCount(tokens, 2);
			context.getPrimitives().sqrt(tokens[1], tokens[2]);
		} else if (tokens[0].equals("closewritefile")) {
			assertVariableType(tokens[1], 1, SPointer.filePrefix);
			context.closeWriteFile(tokens[1]);
		} else if (tokens[0].equals("closereadfile")) {
			assertVariableType(tokens[1], 1, SPointer.filePrefix);
			context.closeReadFile(tokens[1]);
		} 

		else if (tokens[0].startsWith("foreach")) {
			if (tokens[0].equals("foreachassociation")) {
				assertExactParametreCount(tokens, 3);
				assertVariableType(tokens[1], 1, SPointer.associationPrefix);
				Map<String, String> map = (Map<String, String>) context
						.getPointers().getVariable(tokens[1]).getValue();
				for (Map.Entry entry : map.entrySet()) {
					String value = entry.getValue().toString();
					String key = entry.getKey().toString();
					context.getPrimitives().setValue(tokens[2], key);
					context.getPrimitives().setValue(tokens[3], value);
					Status status = RunFrameAndReportError(code, context);
					pause(code);
					// check if we need to exit this loop because of
					// statements in the code that was run
					if (status == Status.Exit || status == Status.Return)
						return status;
					else if (status == Status.Break)
						break;
				}
			} 
			else {
				Class itemType = Object.class;
				String type = tokens[0].substring("foreach".length());
				// Check the type of foreach loop
				// and set the item type to iterate over
				if (type.equals("dot")) {
					itemType = Dot.class;
				} else if (type.equals("text")) {
					itemType = Text.class;
				} else if (type.equals("line")) {
					itemType = Line.class;
				} else if (type.equals("item") || type.equals("")) {
					itemType = Object.class;
				} else {
					throw new RuntimeException("Invalid ForEach loop type");
				}

				assertVariableType(tokens[2], 2, SPointer.itemPrefix);
				assertVariableType(tokens[1], 1, SPointer.framePrefix);
				Frame currFrame = (Frame) context.getPointers().getVariable(
						tokens[1]).getValue();
				// Create the ip variable
				Item frameTitle = currFrame.getTitleItem();

				for (Item i : currFrame.getVisibleItems()) {
					if (i == frameTitle)
						continue;
					if (!(itemType.isInstance(i)))
						continue;

					context.getPointers().setObject(tokens[2], i);
					Status status = RunFrameAndReportError(code, context);
					pause(code);
					// check if we need to exit this loop because of
					// statements in the code that was run
					if (status == Status.Exit || status == Status.Return)
						return status;
					else if (status == Status.Break)
						return Status.OK;
				}
			}
		} else if (tokens[0].equals("movecursorto")) {
			assertExactParametreCount(tokens, 2);
			int x = (int) context.getPrimitives().getIntegerValue(tokens[1]);
			int y = (int) context.getPrimitives().getIntegerValue(tokens[2]);
			DisplayIO.setCursorPosition(x, y);
			
			
		} else {
			// Check the available actions
			if (!executeAction(tokens, context)) {
				throw new RuntimeException("Unknown statement");
			}
		}
		return Status.OK;
	}

	/**
	 * This method is the backstop if the main SIMPLE parser did not recognise.
	 * the statement TODO make it so it passes the vars rather than a string
	 * with all the vars in order to improve efficiency.
	 * 
	 * @param tokens
	 * @param context
	 * @return
	 * @throws Exception
	 */
	private static boolean executeAction(String[] tokens, Context context)
			throws Exception {
		StringBuffer command = new StringBuffer();
		command.append(tokens[0]);
		int param = 1;

		Frame source = null;
		// Check if the first param is a frame
		if (param < tokens.length
				&& tokens[param].startsWith(SPointer.framePrefix)) {
			source = (Frame) context.getPointers().getVariable(tokens[param])
					.getValue();
			param++;
		}
		// Check if the next param is an item
		Item launcher = null;
		if (param < tokens.length
				&& tokens[param].startsWith(SPointer.itemPrefix)) {
			try {
				launcher = (Item) context.getPointers().getVariable(
						tokens[param]).getValue();
				param++;
			} catch (Exception e) {
				// If the variable does not exist it could be for a return value
			}
		}

		if (source == null)
			source = DisplayIO.getCurrentFrame();
		int lastParam = tokens.length - 1;
		String resultVarName = null;
		if (tokens[lastParam].startsWith(SPointer.itemPrefix)) {
			resultVarName = tokens[lastParam];
			lastParam--;
		}

		// Finally add the rest of the params as Strings
		for (int i = param; i <= lastParam; i++) {
			command.append(' ').append(
					context.getPrimitives().getStringValue(tokens[i]));
		}

		Object returnValue = Actions.PerformAction(source, launcher, command
				.toString());
		if (returnValue != null) {
			if (resultVarName != null) {
				if (!(returnValue instanceof Item)) {
					try {
						Item item = ((Item) context.getPointers().getVariable(
								resultVarName).getValue());
						item.setText(returnValue.toString());
						returnValue = item;
					} catch (Exception e) {
						// If the itemVariable does not exist then create one
						returnValue = source.getStatsTextItem(returnValue
								.toString());
					}
				}
				context.getPointers().setObject(resultVarName, returnValue);
			} else {
				FreeItems.getInstance().clear();
				if (returnValue instanceof Item) {
					Misc.attachToCursor((Item) returnValue);
				} else {
					Misc.attachStatsToCursor(returnValue.toString());
				}
			}
		}

		return true;
	}

	public static int countCharsInString(String s, String pattern) {
		String newString = s;
		int count = -1;
		do {
			count++;
			s = newString;
			newString = s.replaceFirst(pattern, "");
		} while (s.length() != newString.length());

		return count;
	}

	public static void assertVariableType(String varName, int no, String type)
			throws Exception {
		if (!varName.startsWith(type))
			throw new IncorrectTypeException(type, no);
	}

	/*
	 * public static void assertPrimitiveType(String varName, int no) throws
	 * Exception { if (!Primitives.isPrimitive(varName)) throw new
	 * IncorrectTypeException("primitive", no); }
	 */

	public static void assertMinParametreCount(String[] tokens,
			int minParametres) throws Exception {
		if (tokens.length - 1 < minParametres)
			throw new BelowMinParametreCountException(minParametres);
	}

	public static void assertExactParametreCount(String[] tokens,
			int parametreCount) throws Exception {
		if (tokens.length - 1 != parametreCount)
			throw new IncorrectParametreCountException(parametreCount);
	}

	public static void assertMaxParametreCount(String[] tokens,
			int parametreCount) throws Exception {
		if (tokens.length - 1 > parametreCount)
			throw new AboveMaxParametreCountException(parametreCount);
	}

	private static String getMessage(String[] tokens, Context context,
			String code, String separator, int firstStringIndex)
			throws Exception {
		StringBuilder message = new StringBuilder();
		if (tokens.length == firstStringIndex) {
			message.append(context.getPrimitives().getVariable(DEFAULT_STRING)
					.stringValue());
		} else {
			for (int i = firstStringIndex; i < tokens.length; i++) {
				if (Primitives.isPrimitive(tokens[i])) {
					message.append(context.getPrimitives().getVariable(
							tokens[i]).stringValue()
							+ separator);
				} else
					throw new Exception("Illegal parametre: [" + tokens[i]
							+ "] in line " + code);
			}
		}
		return message.toString();
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
		MessageBay.displayMessage("Running SimpleProgram...", Color.BLUE);
	}

	public static boolean isVerbose() {
		return _verbose;
	}
}