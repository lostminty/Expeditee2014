package org.expeditee.actions;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URI;
import java.net.URLDecoder;
import java.net.JarURLConnection;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.expeditee.agents.Agent;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.FrameUtils;
import org.expeditee.gui.FreeItems;
import org.expeditee.gui.MessageBay;
import org.expeditee.io.Conversion;
import org.expeditee.items.Item;
import org.expeditee.items.ItemUtils;
import org.expeditee.items.Text;
import org.expeditee.reflection.PackageLoader;
import org.expeditee.settings.UserSettings;
import org.expeditee.simple.SString;
import org.expeditee.stats.Logger;

/**
 * The Action class is used to launch Actions and Agents.
 * 
 * This class checks all class files in the same directory, and reads in and adds all the methods from them. The methods
 * are stored in a Hashtable so that the lowercase method names can be mapped to the correctly capatilized method names
 * (to provide case-insensitivity)
 * 
 * When adding an action to a class in the actions folder the following must be considered: <li>If the first parameter
 * is of type Frame, the current frame will be passed as a parameter. <li>If the next param is of type Item the item on
 * the end of the cursor will be passed or the item that was clicked to execute the action if nothing is on the end of
 * the cursor. current frame or item.</li> <li>If there are multiple overloads for the same method they should be
 * declared in order of the methods with the most parameteres to least parameters.</li>
 */
public class Actions {

	private static final String INVALID_PARAMETERS_ERROR = "Invalid parameters for agent: "; //$NON-NLS-1$

	// the currently running agent (if there is one)
	private static Agent _Agent = null;

	// maps lower case method names to the method
	private static HashMap<String, Method> _Actions = new HashMap<String, Method>();

	// map lower case fonts to capitalized fonts
	private static HashMap<String, String> _Fonts = new HashMap<String, String>();

	// maps lower case JAG class names to capitalized JAG full class names
	private static HashMap<String, String> _JAGs = new HashMap<String, String>();

	// maps lower case IW class names to capitalized IW names
	private static HashMap<String, String> _IWs = new HashMap<String, String>();

	public static final String ROOT_PACKAGE = "org.expeditee.";

	// Package and class file locations
	public static final String ACTIONS_PACKAGE = ROOT_PACKAGE + "actions.";

	public static final String AGENTS_PACKAGE = ROOT_PACKAGE + "agents.";

	public static final String WIDGET_PACKAGE = ROOT_PACKAGE + "items.widgets.";

	public static final String CHARTS_PACKAGE = ROOT_PACKAGE + "items.widgets.charts.";

	public static final String NAVIGATIONS_CLASS = ROOT_PACKAGE + "actions.NavigationActions";

	// public static Class[] getClasses(String pckgname)
	// throws ClassNotFoundException {
	// ArrayList<Class> classes = new ArrayList<Class>();
	// // Get a File object for the package
	// File directory = null;
	// // Must be a forward slash for loading resources
	// String path = pckgname.replace('.', '/');
	// System.err.println("Get classes: " + path);
	// try {
	// ClassLoader cld = Thread.currentThread().getContextClassLoader();
	// if (cld == null) {
	// throw new ClassNotFoundException("Can't get class loader.");
	// }
	// URL resource = null;
	// try {
	// Enumeration<URL> resources = cld.getResources(path);
	// System.err.println(resources);
	// while (resources.hasMoreElements()) {
	// URL url = resources.nextElement();
	// // Ingore the classes in the test folder when we are running
	// // the program from Eclipse
	// // This doesnt apply when running directly from the jar
	// // because the test classes are not compiled into the jar.
	// // TODO change this so it is only done when running from
	// // Eclipse... if it causes problems again!!
	// // if (!url.toString().toLowerCase().contains("/tests/")) {
	// resource = url;
	// // break;
	// // }
	// }
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// if (resource == null) {
	// throw new ClassNotFoundException("No resource for " + path);
	// }
	// directory = new File(resource.getFile());
	// } catch (NullPointerException x) {
	// x.printStackTrace();
	// throw new ClassNotFoundException(pckgname + " (" + directory
	// + ") does not appear to be a valid package");
	// }
	// // System.out.println("Path:" + directory.getPath());
	// int splitPoint = directory.getPath().indexOf('!');
	// if (splitPoint > 0) {
	// try {
	// String jarName = directory.getPath().substring(
	// "file:".length(), splitPoint);
	// // Windows HACK
	// if (jarName.indexOf(":") >= 0)
	// jarName = jarName.substring(1);
	//
	// if (jarName.indexOf("%20") > 0) {
	// jarName = jarName.replace("%20", " ");
	// }
	// // System.out.println("JarName:" + jarName);
	// JarFile jarFile = new JarFile(jarName);
	//
	// Enumeration entries = jarFile.entries();
	// int classCount = 0;
	// while (entries.hasMoreElements()) {
	// ZipEntry entry = (ZipEntry) entries.nextElement();
	// String className = entry.getName();
	// if (className.startsWith(path)) {
	// if (className.endsWith(".class")
	// && !className.contains("$")) {
	// classCount++;
	// // The forward slash below is a forwards slash for
	// // both windows and linux
	// classes.add(Class.forName(className.substring(0,
	// className.length() - 6).replace('/', '.')));
	// }
	// }
	// }
	// jarFile.close();
	// // System.out.println("Loaded " + classCount + " classes from "
	// // + pckgname);
	//
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	//
	// } else {
	//
	// if (directory.exists()) {
	// // Get the list of the files contained in the package
	// String[] files = directory.list();
	// for (int i = 0; i < files.length; i++) {
	// // we are only interested in .class files
	// if (files[i].endsWith(".class") && !files[i].contains("$")
	// && !files[i].equals("Actions.class")) {
	// // removes the .class extension
	// classes
	// .add(Class.forName(pckgname
	// + files[i].substring(0, files[i]
	// .length() - 6)));
	// }
	// }
	// } else {
	// throw new ClassNotFoundException("The package '" + pckgname +
	// "' in the directory '" + directory
	// + "' does not appear to be a valid package");
	// }
	// }
	// Class[] classesA = new Class[classes.size()];
	// classes.toArray(classesA);
	// return classesA;
	// }

	/**
	 * Clears out the Action and JAG Hashtables and refills them. Normally this is only called once when the system
	 * starts.
	 * 
	 * @return a warning message if there were any problems loading agents or actions.
	 */
	public static Collection<String> Init() {

		Collection<String> warnings = new LinkedList<String>();
		List<Class<?>> classes;

		try {
			classes = PackageLoader.getClassesNew(AGENTS_PACKAGE);

			for (Class clazz : classes) {
				String name = clazz.getSimpleName();
				// maps lower case name to correct capitalised name
				_JAGs.put(name.toLowerCase(), clazz.getName());
			}

			
			classes = PackageLoader.getClassesNew(WIDGET_PACKAGE);

			for (Class clazz : classes) {
				String name = clazz.getSimpleName();
				// maps lower case name to correct capitalised name
				_IWs.put(name.toLowerCase(), WIDGET_PACKAGE + name);
			}

			
			classes = PackageLoader.getClassesNew(CHARTS_PACKAGE);

			for (Class clazz : classes) {
				String name = clazz.getSimpleName();
				// maps lower case name to correct capitalised name
				_IWs.put("charts." + name.toLowerCase(), CHARTS_PACKAGE + name);
			}
		} catch (ClassNotFoundException e) {
			System.err.println("ClassNotFoundException");
			e.printStackTrace();
		} catch (Exception e) {
			warnings.add("You must have Java 1.5 or higher to run Expeditee");
			warnings.add(e.getMessage());
			e.printStackTrace();
		}

		try {
			classes = PackageLoader.getClassesNew(ACTIONS_PACKAGE);

			for (Class clazz : classes) {
				String name = clazz.getSimpleName();
				// Ignore the test classes
				if (name.toLowerCase().contains("test"))
					continue;
				// read in all the methods from the class
				try {
					// System.out.println(name)
					LoadMethods(Class.forName(ACTIONS_PACKAGE + name));
				} catch (ClassNotFoundException e) {
					Logger.Log(e);
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			warnings.add(e.getMessage());
		}
		return warnings;
	}

	/**
	 * Temporary, if a plugin system is devised then this would porbably become redundant. For now this allows external
	 * agents to be included.
	 * 
	 * @param fullClassNames
	 *            A set of full class names, that is, the class package and name. For
	 *            example" "org.myplugin.agents.SerializedSearch"
	 * 
	 * @return A collection of classes their were omitted because either there was a name clash with existing agents or
	 *         did not exist. i.e. is completely successful this will be empty. Never null.
	 * 
	 * @throws NullPointerException
	 *             If fullClassNames is null.
	 * 
	 */
	public static Collection<String> addAgents(Set<String> fullClassNames) {
		if (fullClassNames == null)
			throw new NullPointerException("fullClassNames");

		List<String> omittedAgents = new LinkedList<String>();

		for (String fullName : fullClassNames) {

			if (fullName == null || fullName.length() == 0)
				continue;

			boolean didAdd = false;

			try {
				// Does the class even exist?
				Class<?> c = Class.forName(fullName);

				String name = c.getSimpleName().toLowerCase();

				if (!_JAGs.containsKey(name)) {

					_JAGs.put(name, fullName);
					didAdd = true;

				}

			} catch (ClassNotFoundException e) { // Nope it does not exist
				e.printStackTrace();
			}

			if (!didAdd)
				omittedAgents.add(fullName);

		}

		return omittedAgents;
	}

	/**
	 * Loads all the Methods that meet the requirements checked by MethodCheck into the hashtable.
	 * 
	 * @param c
	 *            The Class to load the Methods from.
	 */
	public static void LoadMethods(Class<?> c) {
		assert (c != null);

		// list of methods to test
		Method[] toLoad = c.getMethods();

		for (Method m : toLoad) {
			// only allow methods with the right modifiers
			if (MethodCheck(m)) {
				String lowercaseName = m.getName().toLowerCase();
				if (!(_Actions.containsKey(lowercaseName)))
					_Actions.put(lowercaseName, m);
				else {
					int i = 0;
					while (_Actions.containsKey(lowercaseName + i))
						i++;

					_Actions.put(lowercaseName + i, m);
				}

			}
		}
	}

	/**
	 * Checks if the given Method corresponds to the restrictions of Action commands, namely: Declared (not inherited),
	 * Public, and Static, with a void return type.
	 * 
	 * @param m
	 *            The Method to check
	 * @return True if the Method meets the above conditions, false otherwise.
	 */
	private static boolean MethodCheck(Method m) {
		int mods = m.getModifiers();

		// check the method is declared (not inherited)
		if ((mods & Method.DECLARED) != Method.DECLARED)
			return false;

		// check the method is public
		if ((mods & Modifier.PUBLIC) != Modifier.PUBLIC)
			return false;

		// check the method is static
		if ((mods & Modifier.STATIC) != Modifier.STATIC)
			return false;

		// if we have not returned yet, then the tests have all passed
		return true;
	}

	/**
	 * Performs the given action command. The source Frame and Item are given because they are required by some actions.
	 * Note that the source frame does not have to be the Item's parent Frame.
	 * 
	 * @param source
	 *            The Frame that the action should apply to
	 * @param launcher
	 *            The Item that has the action assigned to it
	 * @param command
	 *            The action to perform
	 */
	public static Object PerformAction(Frame source, Item launcher, String command) throws Exception {
		// if (!command.equalsIgnoreCase("Restore"))
		// FrameIO.SaveFrame(source, false);
		// TODO make restore UNDO the changes made by the last action

		// separate method name and parameter names
		String mname = getName(command);
		command = command.substring(mname.length()).trim();
		// If no params are provided get them from a text item on the cursor
		if (command.length() == 0 && launcher instanceof Text && launcher.isFloating()) {
			command = launcher.getText();
		}

		// Strip off the @ from annotation items
		if (mname.startsWith("@"))
			mname = mname.substring(1);

		mname = mname.trim();
		String lowercaseName = mname.toLowerCase();
		// check for protection on frame
		if (ItemUtils.ContainsTag(source.getItems(), "@No" + mname)) {
			throw new RuntimeException("Frame is protected by @No" + mname + " tag.");
		}

		// retrieve methods that match the name
		Method toRun = _Actions.get(lowercaseName);

		// if this is not the name of a method, it may be the name of an agent
		if (toRun == null) {
			LaunchAgent(mname, command, source, launcher);
			return null;
		}

		// Need to save the frame if we are navigating away from it so we dont
		// loose changes
		if (toRun.getDeclaringClass().getName().equals(NAVIGATIONS_CLASS)) {
			FrameIO.SaveFrame(DisplayIO.getCurrentFrame());
		}

		// if there are duplicate methods with the same name
		List<Method> possibles = new LinkedList<Method>();
		possibles.add(toRun);
		int i = 0;
		while (_Actions.containsKey(lowercaseName + i)) {
			possibles.add(_Actions.get(lowercaseName + i));
			i++;
		}

		for (Method possible : possibles) {
			// try first with the launching item as a parameter

			// run method
			try {
				// convert parameters to objects and get the method to invoke
				Object[] parameters = CreateObjects(possible, source, launcher, command);
				// Check that there are the same amount of params
				if (parameters == null) {
					continue;
				}

				return possible.invoke(null, parameters);
			} catch (Exception e) {
				Logger.Log(e);
				e.printStackTrace();
			}
		}
		// If the actions was not found... then it is run as an agent
		assert (possibles.size() > 0);
		throw new RuntimeException("Incorrect parameters for " + mname);
	}

	/**
	 * Launches an agent with the given name, and passes in the given parameters
	 * 
	 * @param name
	 *            The name of the JAG to load
	 * @param parameters
	 *            The parameters to pass to the JAG
	 * @param source
	 *            The starting Frame that the JAG is being launched on
	 */
	private static void LaunchAgent(String name, String parameters, Frame source, Item clicked) throws Exception {
		// Use the correct case version for printing error messages
		String nameWithCorrectCase = name;
		name = name.toLowerCase();

		String fullClassName = AGENTS_PACKAGE + name;

		try {
			// check for stored capitalisation
			if (_JAGs.containsKey(name)) {
				fullClassName = _JAGs.get(name);
			} else if (name.endsWith("tree")) {
				parameters = name.substring(0, name.length() - "tree".length()) + " " + parameters;
				fullClassName = AGENTS_PACKAGE + "writetree";

			} else if (name.endsWith("frame")) {
				parameters = name.substring(0, name.length() - "frame".length()) + " " + parameters;
				fullClassName = AGENTS_PACKAGE + "writeframe";
			}

			// load the JAG class
			Class<?> agentClass = Class.forName(fullClassName);

			// get the constructor for the JAG class
			Constructor<?> con = null;
			Constructor<?>[] constructors = agentClass.getConstructors();
			Object[] params = null;

			parameters = parameters.trim();
			// determine correct parameters for constructor
			for (Constructor<?> c : constructors) {
				Class<?>[] paramTypes = c.getParameterTypes();
				int paramCount = paramTypes.length;
				if (paramCount > 0 && parameters.length() > 0) {
					params = new Object[paramCount];
					String[] paramStrings = parameters.split("\\s+");
					/**
					 * Any extra parameters will be treated as the rest of the string if the last param is a string
					 */
					if (paramCount > paramStrings.length) {
						continue;
					}

					/**
					 * If there are extra parameters the last param must be a String
					 */
					int lastParam = paramTypes.length - 1;

					if (paramCount < paramStrings.length && !paramTypes[lastParam].equals(String.class)) {
						continue;
					}

					try {
						for (int i = 0; i < paramCount; i++) {
							SString nextParam = new SString(paramStrings[i]);
							params[i] = null;
							if (paramTypes[i].equals(int.class) || paramTypes[i].equals(Integer.class)) {
								params[i] = nextParam.integerValue().intValue();
							} else if (paramTypes[i].equals(long.class) || paramTypes[i].equals(Long.class)) {
								params[i] = nextParam.integerValue();
							} else if (paramTypes[i].equals(double.class) || paramTypes[i].equals(Double.class)) {
								params[i] = nextParam.doubleValue();
							} else if (paramTypes[i].equals(float.class) || paramTypes[i].equals(Float.class)) {
								params[i] = nextParam.doubleValue().floatValue();
							} else if (paramTypes[i].equals(boolean.class) || paramTypes[i].equals(Boolean.class)) {
								params[i] = nextParam.booleanValue();
							} else if (paramTypes[i].equals(String.class)) {
								params[i] = nextParam.stringValue();
							} else {
								throw new UnexpectedException("Unexpected type " + paramTypes[i].getClass().toString());
							}
						}
					} catch (Exception e) {
						continue;
					}

					if (paramCount < paramStrings.length) {

						/**
						 * Append extra params on the end of the last string param
						 */
						String s = params[lastParam].toString();
						for (int i = paramCount; i < paramStrings.length; i++) {
							s += ' ' + paramStrings[i];
						}
						params[lastParam] = s;
					}

					con = c;
					break;
				} else if (c.getParameterTypes().length == 0 && con == null) {
					con = c;
					params = null;
				}
			}

			// if there is no constructor, return
			if (con == null) {
				throw new RuntimeException(INVALID_PARAMETERS_ERROR + nameWithCorrectCase);
			}

			// create the JAG
			Agent toLaunch = (Agent) con.newInstance(params);

			LaunchAgent(toLaunch, source, clicked);

		} catch (ClassNotFoundException cnf) {
			_Agent = null;
			throw new RuntimeException("'" + nameWithCorrectCase + "' is not an action or agent.");
		}
	}

	public static void LaunchAgent(String name, String parameters, Frame source) throws Exception {
		LaunchAgent(name, parameters, source, null);
	}

	/**
	 * Launches an agent from an already instantiated object.
	 * 
	 * @param agent
	 *            The agent to launch. Must not be null.
	 * 
	 * @param source
	 *            The calling frame that launched it. Must not be null.
	 * 
	 * @param itemParam
	 *            The item parameter for the agent.
	 * 
	 * @throws NullPointerException
	 *             if any of the arguments are null.
	 */
	public static void LaunchAgent(Agent agent, Frame source, Item itemParam) {

		if (agent == null)
			throw new NullPointerException("agent");
		if (source == null)
			throw new NullPointerException("source");
		// if (itemParam == null) throw new NullPointerException("itemParam");

		String nameWithCorrectCase = agent.getClass().getSimpleName();

		try {

			// create the JAG
			_Agent = agent;

			Thread.UncaughtExceptionHandler h = new Thread.UncaughtExceptionHandler() {
			    public void uncaughtException(Thread th, Throwable ex) {
			 
			        MessageBay.errorMessage("Error occurred in Action: " + th.getName());
			        ex.printStackTrace();

			        stopAgent();
			        _Agent = null;
			    }
			};
			
			Thread t = new Thread(_Agent,nameWithCorrectCase);
		
			t.setPriority(Thread.MIN_PRIORITY);
			t.setUncaughtExceptionHandler(h);
			
			if (FreeItems.textOnlyAttachedToCursor()) {
				itemParam = FreeItems.getItemAttachedToCursor();
			}

			// check for errors during initialisation
			if (!_Agent.initialise(source, itemParam)) {
				_Agent = null;
				throw new RuntimeException("Error initialising agent: " + nameWithCorrectCase);
			}

			// save the current frame (if necesssary)
			// TODO make this nicer... ie. make Format an action rather than an
			// agent and save frames only before running agents
			if (!nameWithCorrectCase.equalsIgnoreCase("format") && !nameWithCorrectCase.equalsIgnoreCase("sort")) {
				FrameUtils.LeavingFrame(source);
			}

			if (_Agent.hasResultString()) {
				// Just run the agent on this thread... dont run it in the
				// background
				t.run();
				if (_Agent != null) {
				    String result = _Agent.toString();
				    // Attach the result to the cursor
				    if (FreeItems.textOnlyAttachedToCursor()) {
					Item resultItem = FreeItems.getItemAttachedToCursor();
					resultItem.setText(result);
				    }
				    // if there is a completion frame, then display it to the user
				}
			} else {
				t.start();
				if (_Agent != null && _Agent.hasResultFrame()) {
					// TODO We want to be able to navigate through the frames as
					// the results are loading
					Frame next = _Agent.getResultFrame();
					FrameUtils.DisplayFrame(next, true, true);
				}
			}
		} catch (Exception e) {
			_Agent = null;
			e.printStackTrace();
			throw new RuntimeException("Error creating Agent: '" + nameWithCorrectCase + "'");
		}
		FrameGraphics.refresh(false);
	}

	/**
	 * Used to determine if the previously launched agent is still executing.
	 * 
	 * @return True if the last Agent is still executing, False otherwise.
	 */
	public static boolean isAgentRunning() {
		if (_Agent != null)
			return _Agent.isRunning();

		return false;
	}

	/**
	 * Stops the currently running Agent (If there is one) by calling Agent.stop(). Note: This may not stop the Agent
	 * immediately, but the Agent should terminate as soon as it is safe to do so.
	 */
	public static void stopAgent() {
		if (_Agent != null && _Agent.isRunning()) {
			MessageBay.errorMessage("Stopping Agent...");
			_Agent.stop();
		}
	}

	public static void interruptAgent() {
		if (_Agent != null) {
			_Agent.interrupt();
		}
	}

	/**
	 * Converts the given String of values into an array of Objects
	 * 
	 * @param launcher
	 *            The Item used to launch the action, it may be required as a parameter
	 * @param values
	 *            A list of space separated String values to convert to objects
	 * @return The created array of Objects
	 */
	public static Object[] CreateObjects(Method method, Frame source, Item launcher, String values) {
		// The parameter types that should be created from the given String
		Class<?>[] paramTypes = method.getParameterTypes();

		int paramCount = paramTypes.length;
		// if the method has no parameters
		if (paramCount == 0)
			return new Object[0];

		Object[] objects = new Object[paramCount];
		int ind = 0;

		/*
		 * if the first class in the list is a frame or item, it is the source or launcher length must be at least one
		 * if we are still running
		 */
		if (paramTypes[ind] == Frame.class) {
			objects[ind] = source;
			ind++;
		}

		// Check if the second item is an item
		if (paramCount > ind && Item.class.isAssignableFrom(paramTypes[ind])) {
			objects[ind] = launcher;
			ind++;
		}// If there is stuff on the cursor use it for the rest of the params
		else if (launcher != null && launcher.isFloating()) {
			values = launcher.getText();
		}

		String param = values;
		// convert the rest of the objects
		for (; ind < objects.length; ind++) {
			// check if its the last param and combine
			if (values.length() > 0 && ind == objects.length - 1) {
				param = values.trim();
				// check if its a string
				if (param.length() > 0 && param.charAt(0) == '"') {
					int endOfString = param.indexOf('"', 1);
					if (endOfString > 0) {
						param = param.substring(1, endOfString);
					}
				}
			} else {// strip off the next value
				param = ParseValue(values);
				values = RemainingParams(values);
			}
			// convert the value to an object
			try {
				Object o = Conversion.Convert(paramTypes[ind], param);
				if (o == null)
					return null;
				objects[ind] = o;
			} catch (Exception e) {
				return null;
			}
		}

		return objects;
	}

	/**
	 * Returns a string containing the remaining params after ignoring the first one.
	 * 
	 * @param params
	 *            a space sparated list of N parameters
	 * @return the remaining N - 1 parameters
	 */
	public static String RemainingParams(String params) {
		if (params.length() == 0)
			return null;

		// remove leading and trailing spaces
		params = params.trim();

		// if there are no more parameters, we are done
		if (params.indexOf(" ") < 0) {
			return "";
		}

		// Check if we have a string parameter
		if (params.charAt(0) == '"') {
			int endOfString = params.indexOf('"', 1);
			if (endOfString > 0) {
				if (endOfString > params.length())
					return "";
				return params.substring(endOfString + 1).trim();
			}
		}

		return params.substring(params.indexOf(" ")).trim();
	}

	/**
	 * Returns the first value in the space separated String of parameters passed in. Strings are enclosed in double
	 * quotes.
	 * 
	 * @param params
	 *            The String of space separated values
	 * @return The first value in the String
	 */
	public static String ParseValue(String params) {
		if (params.length() == 0)
			return null;

		// remove leading and trailing spaces
		String param = params.trim();

		// Check if we have a string parameter
		if (param.charAt(0) == '"') {
			int endOfString = param.indexOf('"', 1);
			if (endOfString > 0)
				return param.substring(1, endOfString);
		}

		// if there are no more parameters, we are done
		if (param.indexOf(" ") < 0) {
			return param;
		}

		return param.substring(0, param.indexOf(" "));
	}

	/**
	 * Separates the name of the given command from any parameters and returns them
	 * 
	 * @param command
	 *            The String to separate out the Action or Agent name from
	 * @return The name of the Action of Agent with parameters stripped off
	 */
	private static String getName(String command) {
		if (command.indexOf(" ") < 0)
			return command;

		return command.substring(0, command.indexOf(" "));
	}

	/**
	 * Gets an uncapitalized font name and returns the capitalized font name. The capitalized form can be used with the
	 * Font.decoded method to get a corresponding Font object.
	 * 
	 * @param fontName
	 *            a font name in mixed case
	 * @return the correct capitalized form of the font name
	 */
	public static String getCapitalizedFontName(String fontName) {
		// Initialize the fonts if they have not already been loaded
		initFonts();
		return _Fonts.get(fontName.toLowerCase());
	}

	/**
	 * Initialise the fontsList if it has not been done already
	 */
	private static void initFonts() {
		if (_Fonts.size() == 0) {
			String[] availableFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
			for (String s : availableFonts) {
				_Fonts.put(s.toLowerCase(), s);
			}
		}
	}

	public static HashMap<String, String> getFonts() {
		initFonts();
		return _Fonts;
	}

	public static Object PerformActionCatchErrors(Frame current, Item launcher, String command) {
		try {
			return PerformAction(current, launcher, command);
		} catch (RuntimeException e) {
			e.printStackTrace();
			MessageBay.errorMessage("Action failed: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			MessageBay.errorMessage("Action failed: " + e.getClass().getSimpleName());
		}
		return null;
	}

	/**
	 * Gets the full class path for a widget with a given case insensitive name.
	 * 
	 * @param widgetName
	 * @return
	 */
	public static String getClassName(String widgetName) {
		return _IWs.get(widgetName.toLowerCase());
	}

	static List<String> getActions() {
		List<String> actionNames = new LinkedList<String>();
		for (Method m : _Actions.values()) {
			StringBuilder sb = new StringBuilder();
			sb.append(m.getName());
			for (Class<?> c : m.getParameterTypes()) {
				sb.append(" ").append(c.getSimpleName());
			}
			actionNames.add(sb.toString());
		}

		return actionNames;
	}

	static List<String> getAgents() {
		List<String> agentNames = new LinkedList<String>();

		for (String s : _JAGs.values()) {
			agentNames.add(s.substring(s.lastIndexOf('.') + 1));
		}

		return agentNames;
	}
}
