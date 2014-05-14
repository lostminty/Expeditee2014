package org.expeditee.network;

import java.io.IOException;
import java.net.DatagramPacket;

import org.expeditee.gui.MessageBay;

public class MessageReciever extends DefaultServer {
	public final static int OFFSET = 2;
	
	public MessageReciever(int port) throws IOException {
		super("MessageReciever", port + OFFSET);
	}

	public void listenForMessages() throws IOException {
		byte[] buf = new byte[FrameServer.MAX_PACKET_LENGTH];

		// receive request
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		socket.receive(packet);

		String packetContents = new String(packet.getData(), 0, packet
				.getLength());
		String sendersName = FrameShare.getInstance().getPeerName(
				packet.getPort() + 1, packet.getAddress());
		/**
		 * If the sender of the message is not on our peer list then use their
		 * IP addy.
		 */
		if (sendersName == null)
			sendersName = packet.getAddress().toString();

		packetContents = sendersName + " says: " + packetContents;
		MessageBay.displayMessage(packetContents);
	}
}
