package org.expeditee.agents;

import java.util.HashMap;
import java.util.LinkedList;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.FrameUtils;
import org.expeditee.gui.MessageBay;
import org.expeditee.items.Item;

public class CopyTree extends TreeProcessor {

	private String _nameTo;

	private String _nameFrom;

	private String _framePath;

	private int _lastNumber = -1;

	private int _firstNumber = 1;

	private HashMap<String, String> _nameMap = new HashMap<String, String>();

	private LinkedList<String> _toReparse = new LinkedList<String>();

	public CopyTree(String framesetTo) {
		_nameTo = framesetTo.trim();
	}

	@Override
	public boolean initialise(Frame init, Item launcher) {
		_nameFrom = init.getFramesetName().toLowerCase();

		// create the new frameset
		try {
			// get the last used frame in the destination frameset
			_lastNumber = FrameIO.getLastNumber(_nameTo);
			Frame one = FrameIO.CreateFrameset(_nameTo, init.getPath());

			_framePath = one.getPath();
			_lastNumber = -1;
			_firstNumber = 1;

			// copy the original .0 frame
			Frame zero = FrameIO
					.LoadFrame(init.getFramesetName() + "0");
			processFrame(zero);
		} catch (ExistingFramesetException efe) {
			MessageBay.errorMessage("A frameset called " + _nameTo
					+ " already exists.");
			return false;
		} catch (Exception e) {
			return false;
		}

		return super.initialise(init, launcher);
	}

	@Override
	protected void processFrame(Frame toProcess) {
		// load a fresh copy of the frame that bypasses the cache
		FrameIO.SuspendCache();

		Frame fresh = FrameIO.LoadFrame(toProcess.getName());
		if (_nameMap.containsKey(fresh.getName().toLowerCase())) {
			fresh
					.setName(_nameMap.get(fresh.getName()
							.toLowerCase()));
		} else {
			fresh.setFrameset(_nameTo);
			fresh.setFrameNumber(++_lastNumber);

			_nameMap.put(toProcess.getName().toLowerCase(), fresh
					.getName().toLowerCase());
		}

		boolean added = false;
		for (Item i : fresh.getItems())
			if (i.getLink() != null && !i.isAnnotation() && i.isLinkValid()) {
				String link = i.getLink().toLowerCase();
				//convert to absolute link with the old framesetName
				if (FrameIO.isPositiveInteger(link)){
					link = _nameFrom + link;
				}
				//check if we already have this in our map
				if (_nameMap.containsKey(link))
					link = _nameMap.get(link);
				//otherwise add it to our map
				else if (link.startsWith(_nameFrom)) {
					_nameMap.put(link, _nameTo + (++_lastNumber));
					link = "" + _lastNumber;
				}
				i.setLink(link);
			} else if (!added && i.getLink() != null && i.isAnnotation()
					&& i.isLinkValid()) {
				// annotation links need to be parsed at the end
				if (i.getAbsoluteLink().toLowerCase().startsWith(_nameFrom)) {
					_toReparse.add(fresh.getName());
					added = true;
				}

			}
		_frameCount++;
		fresh.setPath(_framePath);
		FrameIO.ForceSaveFrame(fresh);
		FrameIO.ResumeCache();
	}

	@Override
	protected void finalise(Frame frame) {
		// reparse all frames that have annotation links that may need updating
		for (String name : _toReparse) {
			Frame toParse = FrameIO.LoadFrame(name);
			boolean changed = false;
			for (Item i : toParse.getItems()) {
				if (i.getLink() != null && i.isAnnotation() && i.isLinkValid()) {
					String link = i.getLink();
					link = link.toLowerCase();
					// link = link.replace(_nameFrom, _nameTo);
					if (_nameMap.containsKey(link)) {
						link = _nameMap.get(link);
						i.setLink(link);
						changed = true;
					}
				}
			}
			if (changed) {
				FrameIO.SaveFrame(toParse);
			}
		}

		message("Tree successfully copied to " + _nameTo);
		FrameUtils.DisplayFrame(_nameTo + _firstNumber);
		
		super.finalise(frame);
	}

}
