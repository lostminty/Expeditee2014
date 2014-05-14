package org.expeditee.io;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

public class ProxyAuth extends Authenticator {

	private String httpUser = null, httpPass = null, httpsUser = null, httpsPass = null;
	private int attempts = 0;
	
	public ProxyAuth() {
	}
	
	@Override
	public PasswordAuthentication getPasswordAuthentication() {
		// TODO: differentiate between HTTP and HTTPS proxies somehow
		// currently just chooses whichever one is set, preferentially http
		System.out.println("Authenticating");
		// stop it from breaking from a redirect loop
		attempts++;
		if(attempts > 5) {
			return null;
		}
		String user = httpUser != null ? httpUser : httpsUser;
		char[] pass = httpPass != null ? httpPass.toCharArray() : (httpsPass != null ? httpsPass.toCharArray() : null);
		if(user == null || user.length() == 0 || pass == null || pass.length == 0) {
			return null;
		}
		return new PasswordAuthentication(user, pass);
	}
	
	public void setup(String user, String pass) {
		// System.out.println("setup proxy");
		this.httpUser = user;
		this.httpPass = pass;
		this.httpsUser = user;
		this.httpsPass = pass;
		attempts = 0;
	}
}
