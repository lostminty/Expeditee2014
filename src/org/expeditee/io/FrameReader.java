package org.expeditee.io;

import java.io.BufferedReader;
import java.io.IOException;

import org.expeditee.gui.Frame;

public interface FrameReader {
	public Frame readFrame(String fullPath) throws IOException;
	public Frame readFrame(BufferedReader frameContents) throws IOException;
}
