package org.expeditee.network;

import java.io.IOException;
import java.net.DatagramSocket;

public abstract class DefaultServer extends Thread {


	protected static final int MAX_PACKET_LENGTH = 64000;
	protected static final int FRAMENAME_PACKET_LENGTH = 1000;
	
	protected boolean _stop = false;
	protected DatagramSocket socket = null;

	public DefaultServer() {
		super();
	}

	public DefaultServer(Runnable target) {
		super(target);
	}
	
	protected DefaultServer(String name) {
		super(name);
	}

	public DefaultServer(String name, int port) throws IOException {
		super(name);
		socket = new DatagramSocket(port);
	}

	public DefaultServer(ThreadGroup group, Runnable target) {
		super(group, target);
	}

	public DefaultServer(ThreadGroup group, String name) {
		super(group, name);
	}

	public DefaultServer(Runnable target, String name) {
		super(target, name);
	}

	public DefaultServer(ThreadGroup group, Runnable target, String name) {
		super(group, target, name);
	}

	public DefaultServer(ThreadGroup group, Runnable target, String name,
			long stackSize) {
		super(group, target, name, stackSize);
	}

	public void close() {
		_stop = true;
	}

	public void run() {
		//MessageBay.displayMessage(this.getName() + " started on port "
		//		+ socket.getLocalPort());
		int fail = 0;

		while (!_stop) {
			try {
				listenForMessages();
				fail = 0;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				/**
				 * If we hit an Exception we don't want to crash,
				 * but we also don't want to get into an infinite loop of ignoring fatal exceptions.
				 * so try again but count the failures to make sure we don't get into an infinite loop.
				 * (don't just use a boolean because the next message we parse could also cause another
				 *  non-fatal exception, and we don't want to exit unless absolutely necessary)
				 */
				e.printStackTrace();
				if(fail > 10) {
					System.out.println("*****\nIt appears that we've failed to continue from an uncaught exception\n*****");
					// close all the servers
					FrameShare.getInstance().finalise();
					// attempt to restart
					// (don't do this yet since there's currently no way to know if the restart fails,
					//  so we could get stuck in a loop) 
					// FrameShare.restart();
					
					// if we're only running the frameshare server, exit
					if(FrameShare.isHeadless()) {
						System.exit(1);
					}
				} else {
					fail++;
					System.out.println("*****\nEncountered an uncaught exception, attempting to continue\n*****");
				}
			}
		}
		
		closeSocket();
	}
	
	protected void closeSocket() {
		socket.close();
	}

	protected abstract void listenForMessages() throws IOException;

}