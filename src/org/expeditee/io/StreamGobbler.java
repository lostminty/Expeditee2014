package org.expeditee.io;


import java.util.*;
import java.io.*;

import javax.swing.SwingUtilities;

import org.expeditee.gui.MessageBay;

// Base on: "When When Runtime.exec() won't"
//   http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html

public class StreamGobbler extends Thread
{
	public enum MessageBayType { display, error };
	
	class UpdateMessageBay implements Runnable {
		
		  protected final MessageBayType type;
		  protected final String line;
		  
		  UpdateMessageBay(MessageBayType type, String line) 
		  {
		    this.type = type;
		    this.line = line;
		  }
		  
		  public void run() 
		  {
			  if (type == MessageBayType.display) {
            	  MessageBay.displayMessage(line);
              }
              else {
            	  MessageBay.errorMessage(line);
              }
		    
		  }
		}
		
		
		
    InputStream is;
    MessageBayType type;

    
    public StreamGobbler(String threadName, InputStream is, MessageBayType type)
    {
    	super(threadName);
    	this.is = is;
    	this.type = type;
    }

    public void run()
    {
        try
        {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            
            while ( (line = br.readLine()) != null)
            {
            	// MessageBay is on the AWT event thread, so need to use 'invokeLater' to avoid thread deadlock
            	Runnable updateMessageBayTask = new UpdateMessageBay(type,line);
                SwingUtilities.invokeLater(updateMessageBayTask);
            	
            }
           
           
        } 
        catch (IOException ioe) {
            ioe.printStackTrace();  
        }
    }
}

