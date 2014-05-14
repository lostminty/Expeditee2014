package org.expeditee.agents.mail;


public class ConnectThread extends Thread {
	private MailSession _mailSession;

	public ConnectThread(MailSession mailSession) {
		_mailSession = mailSession;
	}

	@Override
	public void run() {
		_mailSession.connectServers();
	}
}
