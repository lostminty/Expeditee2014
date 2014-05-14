package org.apollo.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apollo.audio.util.Timeline;
import org.apollo.gui.FrameLayoutDaemon;
import org.expeditee.gui.DisplayIO;

public class ProbeDaemon extends Thread {
	public ProbeDaemon() {
		super("ProbeDaemon");
		super.setDaemon(true);
	}
	
	public void run() {
		
		System.err.println("APOLLO PROBE DAEMON RUNNING - THIS IS NOT AN ERROR BUT DON'T FORGET TO REMOVE ME :)");
		
		BufferedReader rd = new BufferedReader(new InputStreamReader(System.in));
		
		
		while (true) {
			
			try {
				String line = rd.readLine();
				if (line != null) {
					procccessMessage(line);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}

	}

	private void procccessMessage(String message) {
		
		assert(message != null);
		
		String output = "";
		
		if (message.equalsIgnoreCase("tl") || message.equalsIgnoreCase("timeline")) {
			String timeline = "none";
			Timeline tl = FrameLayoutDaemon.getInstance().getLastComputedTimeline();
			if (tl != null && DisplayIO.getCurrentFrame() == FrameLayoutDaemon.getInstance().getTimelineOwner()) {
				timeline = tl.toString();
			}


			output = "Current timeline: " + timeline;

			
		} else {
			output = "I don't understand \"" + message + "\"";
		}
		
		System.out.println("ProbeDaemon: " + output);

	}
}
