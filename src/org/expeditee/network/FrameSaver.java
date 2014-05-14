package org.expeditee.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.DatagramPacket;

import org.expeditee.gui.FrameIO;
import org.expeditee.gui.MessageBay;

public class FrameSaver extends DefaultServer {
	public final static int OFFSET = 1;

	public FrameSaver(int port) throws IOException {
		super("FrameSaver", port + OFFSET);
	}

	@Override
	protected void listenForMessages() throws IOException {
		byte[] buf = new byte[MAX_PACKET_LENGTH];

		// receive request
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		socket.receive(packet);

		BufferedReader packetContents = new BufferedReader(new StringReader(
				new String(packet.getData(), 0, packet.getLength())));

		// Get the name of the frame
		String frameName = packetContents.readLine();
		int version = Integer.parseInt(packetContents.readLine());

		MessageBay.displayMessage("Recieved request to save " + frameName);

		// TODO: find out what's causing a NullPointerException here
		try {
			FrameIO.SavePublicFrame(FrameShare.getInstance().getPeerName(
				packet.getPort() + 2, packet.getAddress()), frameName, version,
				packetContents);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}
