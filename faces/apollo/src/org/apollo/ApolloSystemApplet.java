package org.apollo;

import java.io.File;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

import org.expeditee.gui.Browser;

public class ApolloSystemApplet extends JApplet implements ActionListener
{

    // point of entry for applet
    public void init()
    {
	
	// Check if the user has agreed to the requested security settings
	try {
	    // Work-around to reduce number of 
	    // 'Could not lock user prefs' error messages.
	    // Thanks to Walter Schatz from the java forums.
	    System.setProperty("java.util.prefs.syncInterval","2000000");
	}
	catch (Exception exception) {
	    getContentPane().add(new JLabel("Expeditee Applet deactivated", JLabel.CENTER));
	    return;
	}
	
	// Ensure platform specific LAF
	try {
	    UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
	}
	catch (Exception exception) {
	    exception.printStackTrace();
	}

	// If there is username parameter passed in, use that

	String expeditee_home = this.getParameter("expeditee.home"); 

	if (expeditee_home == null) {
	    expeditee_home = System.getProperty("user.home") + File.separator + "expeditee";
	}

	System.setProperty("expeditee.home",expeditee_home);

	// Create a button for the user to press to open the main Expeditee frame
	JButton launch_button = new JButton("Launch Musical Expeditee ...");
	launch_button.addActionListener(this);
	getContentPane().add(launch_button);
    }


    public void start() 
    {
	System.err.println("Applet start() called");
    }


    public void stop()
    {
	System.err.println("Applet stop() called");
    }


    public void destroy()
    {
	System.err.println("Applet destroy() called");

	ApolloSystem.shutdown();

	Browser._theBrowser.exit();
	Browser._theBrowser = null;
	System.err.println("Expeditee exited.");
    }


    public void actionPerformed(ActionEvent e)
    {
	if (Browser._theBrowser==null) {
	    ApolloSystem.main(null);
	}
	else {
	    // call set visible??
	}
    }

    /*
    public static void main(String[] args) {
	
    }
    */


}
