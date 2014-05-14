package org.expeditee.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

import org.expeditee.gui.FrameIO;
import org.expeditee.gui.MessageBay;

public class InfServer extends DefaultServer {
	public final static int OFFSET = 3;

	public InfServer(int port) throws IOException {
		super("InfServer", port + OFFSET);
	}

	@Override
	protected void listenForMessages() throws IOException {
		byte[] buf = new byte[FRAMENAME_PACKET_LENGTH];

		// receive request
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		socket.receive(packet);

		String framesetName = new String(packet.getData(), 0, packet
				.getLength());
		MessageBay.displayMessage("Recieved inf request for " + framesetName);

		// figure out response
		int next = FrameIO.ReadINF(FrameIO.PUBLIC_PATH, framesetName, false);
		String dString = next + "";

		if (dString != null && dString.length() > 0) {
			buf = dString.getBytes();

			// send the response to the client at "address" and "port"
			InetAddress address = packet.getAddress();
			int port = packet.getPort();
			packet = new DatagramPacket(buf, buf.length, address, port);
			socket.send(packet);
		}
	}
}
