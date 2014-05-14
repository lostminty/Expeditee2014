package org.expeditee.reflection;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;


public class PackageLoader {

    // The following is adapted from:
    // http://stackoverflow.com/questions/1456930/how-do-i-read-all-classes-from-a-java-package-in-the-classpath

    public static List<Class<?>> getClassesNew(String packageName)
	    throws ClassNotFoundException {

	ClassLoader classLoader = Thread.currentThread()
		.getContextClassLoader();

	ArrayList<String> names = new ArrayList<String>();
	;
	final ArrayList<Class<?>> classes = new ArrayList<Class<?>>();

	String realPackageName = packageName;
	packageName = packageName.replace(".", "/");
	URL packageURL = classLoader.getResource(packageName);

	if (packageURL.getProtocol().equals("jar")) {

	    // build jar file name, then loop through zipped entries

	    String jarFileNameUndecoded = packageURL.getFile();

	    try {
		JarURLConnection ju_connection = (JarURLConnection) packageURL
			.openConnection();
		JarFile jf = ju_connection.getJarFile();

		Enumeration<JarEntry> jarEntries = jf.entries();
		while (jarEntries.hasMoreElements()) {
		    String entryName = jarEntries.nextElement().getName();

		    if (entryName.startsWith(packageName)) {

			if (entryName.endsWith(".class")
				&& !entryName.contains("$")) {

			    // Deal with situation where the class found might
			    // be a further sub-package
			    // e.g. for "org.expeditee.action"
			    // there is:
			    // org/expeditee/action/widgets/Chart.class
			    // which would produce a value of 'entryName' as
			    // widgets/Chart.class

			    entryName = entryName.substring(0,
				    entryName.length() - 6); // 6 = '.class'
			    entryName = entryName.replace('/', '.');

			    names.add(entryName);
			    classes.add(Class.forName(entryName));
			}
		    }
		}
	    } catch (Exception e) {
		System.err.println("Failed to decode jar file: "
			+ jarFileNameUndecoded);
		e.printStackTrace();
	    }
	} else if (packageURL.getProtocol().equals("bundleresource")) {
	    try {
		final URLConnection urlConnection = packageURL.openConnection();
		final Class<?> c = urlConnection.getClass();
		final java.lang.reflect.Method toInvoke = c.getMethod("getFileURL");
		final URL fileURL = (URL)toInvoke.invoke(urlConnection);
		final File folder = new File(fileURL.getFile());
		final List<String> files = findFiles(folder);
		for (final String s : files) {
		    String entryName = realPackageName + s;
		    if (entryName.endsWith(".class") && !entryName.contains("$")) {
			entryName = entryName.substring(0, entryName.lastIndexOf('.'));
			entryName = entryName.replace('/', '.');
			names.add(entryName);
			try {
			    final Class<?> tmpc = Class.forName(entryName);
			    classes.add(tmpc);
			} catch (NoClassDefFoundError e) {
			    System.err.println("Unable to instantiate class " + entryName);
			    System.err.println(e.getMessage());
			}
		    }
		}
	    } catch (final Exception e) {
		System.err.println("Failed to process file: " + packageName);
		e.printStackTrace();
	    }
	} else {
	    // loop through files in classpath

	    String packageURLString = packageURL.toString();
	    try {
		URI uri = new URI(packageURLString);
		File folder = new File(uri.getPath());

		List<String> files = findFiles(folder);
		for (String s : files) {
		    String entryName = realPackageName + s;

		    if (entryName.endsWith(".class")
			    && !entryName.contains("$")) {
			entryName = entryName.substring(0,
				entryName.lastIndexOf('.'));
			entryName = entryName.replace('/', '.');

			names.add(entryName);
			classes.add(Class.forName(entryName));
		    }
		}
	    } catch (Exception e) {
		System.err.println("Failed to process file: "
			+ packageURLString);
		e.printStackTrace();
	    }
	}

	return classes;
    }

    /**
     * Finds all files in a directory and it's subdirectories
     * 
     * @param directory
     *            The folder to start in
     * 
     * @return A list of Strings containing file paths relative to the starting
     *         directory
     */
    private static List<String> findFiles(File directory) {
	List<String> files = new LinkedList<String>();
	for (File f : directory.listFiles()) {
	    if (f.isDirectory()) {
		for (String s : findFiles(f)) {
		    files.add(f.getName() + "/" + s);
		}
	    } else {
		files.add(f.getName());
	    }
	}
	return files;
    }

    public static List<Class<?>> getClasses(final String pckgname)
	    throws ClassNotFoundException {

	final ArrayList<Class<?>> classes = new ArrayList<Class<?>>();

	// Must be a forward slash for loading resources
	final String packagePath = pckgname.replace('.', '/');

	if (System.getProperty("eclipse.expeditee.home") == null) {
	    final ClassLoader cld = Thread.currentThread()
		    .getContextClassLoader();
	    if (cld == null) {
		throw new ClassNotFoundException("Can't get class loader.");
	    }
	    URL resource = null;
	    try {
		final Enumeration<URL> resources = cld
			.getResources(packagePath);
		while (resources.hasMoreElements()) {
		    URL url = resources.nextElement();
		    resource = url;
		}
	    } catch (IOException e) {
		System.err
			.println("A IO Error has occured when trying to use the ContextClassLoader"
				+ System.getProperty("line.separator")
				+ "Are you running from within Eclipse? (or just not with Jar)  Then make sure your"
				+ " 'eclipse.expeditee.home' property is set correctly.  It is currently: '"
				+ System.getProperty("eclipse.expeditee.home")
				+ "'"
				+ System.getProperty("line.separator")
				+ "You can set it by adding a VM argument.  "
				+ "Example: -Declipse.expeditee.home=D:\\Desktop\\Research\\expeditee-svn");
		e.printStackTrace();
	    }
	    if (resource == null) {
		throw new ClassNotFoundException("No resource for "
			+ packagePath);
	    }
	    final File directory = new File(resource.getFile());

	    final int splitPoint = directory.getPath().indexOf('!');
	    if (splitPoint > 0) {
		String jarName = directory.getPath().substring(
			"file:".length(), splitPoint);
		// Windows HACK
		if (jarName.indexOf(":") >= 0)
		    jarName = jarName.substring(1);

		if (jarName.indexOf("%20") > 0) {
		    jarName = jarName.replace("%20", " ");
		}
		// System.out.println("JarName:" + jarName);
		try {
		    final JarFile jarFile = new JarFile(jarName);
		    final Enumeration<?> entries = jarFile.entries();
		    while (entries.hasMoreElements()) {
			final ZipEntry entry = (ZipEntry) entries.nextElement();
			final String className = entry.getName();
			if (className.startsWith(packagePath)) {
			    if (className.endsWith(".class")
				    && !className.contains("$")) {
				// The forward slash below is a forwards slash
				// for
				// both windows and linux

				String class_forname = className.substring(0,
					className.length() - 6);
				class_forname = class_forname.replace('/', '.');

				classes.add(Class.forName(class_forname));
			    }
			}
		    }
		    try {
			jarFile.close();
		    } catch (IOException e) {
			System.err
				.println("Error attempting to close Jar file");
			e.printStackTrace();
		    }
		} catch (IOException e) {
		    System.err.println("Error Instantiating Jar File Object");
		    e.printStackTrace();
		}
	    } else {

		System.err
			.println("A Error has occured when trying to use a Jar file to find actions or agents."
				+ System.getProperty("line.separator")
				+ "Are you running from within Eclipse? (or just not with Jar)  Then make sure your"
				+ " 'eclipse.expeditee.home' property is set correctly.  It is currently: '"
				+ System.getProperty("eclipse.expeditee.home")
				+ "'"
				+ System.getProperty("line.separator")
				+ "You can set it by adding a VM argument.  "
				+ "Example: -Declipse.expeditee.home=D:\\Desktop\\Research\\expeditee-svn");
	    }
	} else {
	    String eclipse_expeditee_home = System.getProperty(
		    "eclipse.expeditee.home", "");
	    String full_package_path = eclipse_expeditee_home + File.separator
		    + "bin" + File.separator + packagePath;

	    final File directory = new File(full_package_path);

	    if (directory.exists()) {
		// Get the list of the files contained in the package
		String[] files = directory.list();
		for (int i = 0; i < files.length; i++) {
		    // we are only interested in .class files
		    if (files[i].endsWith(".class") && !files[i].contains("$")
			    && !files[i].equals("Actions.class")) {
			// removes the .class extension
			classes.add(Class.forName(pckgname
				+ files[i].substring(0, files[i].length() - 6)));
		    }
		}
	    } else {
		throw new ClassNotFoundException("The package '" + pckgname
			+ "' in the directory '" + directory
			+ "' does not appear to be a valid package");
	    }
	}
	return classes;
    }

}
