package org.apollo.actions;

import org.apollo.items.FramePlaybackLauncher;
import org.apollo.widgets.LinkedTrack;
import org.apollo.widgets.SampleRecorder;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.items.Text;

/**
 * Apollo's action set
 * 
 * @author Brook Novak
 *
 */
public class ApolloActions {

	private ApolloActions() {} // Util construction
	
	/**
	 * Attatches a new recorder to the cursor
	 */
	public static void spawnRecorder() {
		
		Frame target = DisplayIO.getCurrentFrame();
		if (target  == null) return;

		// Create the widget
		Text source = new Text(target.getNextItemID());
		source.setParent(target);
		source.setPosition(FrameMouseActions.MouseX, FrameMouseActions.MouseY);
		SampleRecorder recWidget = new SampleRecorder(source, null);
		
		// Pick it up
		FrameMouseActions.pickup(recWidget.getItems());
	}
	
	/**
	 * Attatches a new linked track to the cursor
	 */
	public static void spawnLinkedTrack() {
		
		Frame target = DisplayIO.getCurrentFrame();
		if (target  == null) return;

		// Create the widget
		Text source = new Text(target.getNextItemID());
		source.setParent(target);
		source.setPosition(FrameMouseActions.MouseX, FrameMouseActions.MouseY);
		
		LinkedTrack ltWidget = new LinkedTrack(source, null);
		
		// Pick it up
		FrameMouseActions.pickup(ltWidget.getItems());
	}
	

	/**
	 * Attatches a new FramePlaybackLauncher to the cursor
	 */
	public static void spawnFramePlayLauncher() {
		
		// Create the launcher
		FramePlaybackLauncher launcher = new FramePlaybackLauncher(null);
		launcher.setPosition(FrameMouseActions.MouseX, FrameMouseActions.MouseY);

		// Pick it up
		FrameMouseActions.pickup(launcher);
	}
	
}
