package org.expeditee.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

import org.expeditee.gui.FrameIO;
import org.expeditee.gui.MessageBay;

public class FrameServer extends DefaultServer {
	public final static int OFFSET = 0;

	public FrameServer(int port) throws IOException {
		super("FrameServer", port);
	}

	protected String getFrame(String frameName) {
		StringBuffer sb = new StringBuffer();
		BufferedReader br = FrameIO.LoadPublicFrame(frameName);
		if (br == null)
			return null;

		String s = null;
		try {
			while ((s = br.readLine()) != null) {
				// Check if there is space for the next line in the packet
				if (sb.length() + s.length() > MAX_PACKET_LENGTH) {
					MessageBay.errorMessage(frameName
							+ " is too large to be sent in a single packet");
					break;
				}
				sb.append(s).append('\n');
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return sb.toString();
	}

	@Override
	protected void listenForMessages() throws IOException {
		byte[] buf = new byte[FRAMENAME_PACKET_LENGTH];

		// receive request
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		socket.receive(packet);

		String frameName = new String(packet.getData(), 0, packet.getLength());
		MessageBay.displayMessage("Recieved request for " + frameName);

		// figure out response
		String dString = getFrame(frameName);
		if (dString == null) {
			dString = "";
		}

		buf = dString.getBytes(FrameShare.CHARSET);

		// send the response to the client at "address" and "port"
		InetAddress address = packet.getAddress();
		int port = packet.getPort();
		packet = new DatagramPacket(buf, buf.length, address, port);
		socket.send(packet);
	}
}
