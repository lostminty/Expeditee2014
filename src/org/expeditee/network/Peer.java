package org.expeditee.network;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.expeditee.gui.AttributeValuePair;
import org.expeditee.gui.MessageBay;

public class Peer {
	public static final int DEFAULT_PORT = 3000;

	private String name_ = null;

	private int port_ = DEFAULT_PORT;

	private InetAddress ip_ = null;

	public Peer(AttributeValuePair avp) throws UnknownHostException {
		name_ = avp.getAttribute();
		String[] address = avp.getValue().split("\\s+");
		
		ip_ = InetAddress.getByName(address[0]);
		
		if (address.length > 1) {
			try {
				port_ = Integer.parseInt(address[1]);
			} catch (Exception e) {
				MessageBay.errorMessage("Could not parse port in ["
						+ avp.toString() + "]");
			}
		}
	}

	public int getPort() {
		return port_;
	}

	public InetAddress getAddress() {
		return 	ip_;
	}

	public String getName() {
		return name_;
	}
}
