package org.expeditee.agents.mail;

import java.awt.Color;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessageRemovedException;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.Flags.Flag;
import javax.mail.Message.RecipientType;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.expeditee.gui.AttributeValuePair;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameCreator;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.MessageBay;
import org.expeditee.importer.FrameDNDTransferHandler;
import org.expeditee.items.Text;

public class MailSession {
	public static final String UNREAD_MESSAGE = " unread message";

	public static boolean _autoConnect = false;

	private static MailSession _theMailSession = null;

	private Session _session;

	private Transport _transport;

	private Store _store;

	private Folder _folder;

	private String _address;

	private String _username;

	private String _password;

	private String _mailServer;

	private String _serverType;

	private Boolean _bConnecting;

	private MailSession(Frame settingsFrame) {
		_bConnecting = false;

		Properties props = System.getProperties();

		_username = null;
		_password = "";
		_mailServer = null;
		_serverType = null;

		// Set the settings
		for (Text item : settingsFrame.getBodyTextItems(false)) {
			if (item.getText().toLowerCase().trim().equals("autoconnect")) {
				_autoConnect = true;
				continue;
			}
			AttributeValuePair avp = new AttributeValuePair(item.getText());
			if (!avp.hasPair())
				continue;
			String attributeFullCase = avp.getAttribute();
			String attribute = attributeFullCase.toLowerCase();

			if (attribute.equals("user")) {
				_username = avp.getValue();
				props.setProperty("mail.user", _username);
			} else if (attribute.equals("password")) {
				_password = avp.getValue();
				props.setProperty("mail.password", _password);
			} else if (attribute.equals("address")) {
				_address = avp.getValue();
			} else if (attribute.equals("smtpserver")) {
				props.setProperty("mail.transport.protocol", "smtp");
				props.setProperty("mail.host", avp.getValue());
				props.setProperty("mail.smtp.starttls.enable", "true");
				props.setProperty("mail.smtp.host", avp.getValue());
				props.setProperty("mail.smtp.auth", "true");
			} else if (attribute.equals("popserver")) {
				_mailServer = avp.getValue();
				_serverType = "pop3s";
				props.setProperty("mail.pop3.host", _mailServer);
			} else if (attribute.equals("imapserver")) {
				_mailServer = avp.getValue();
				_serverType = "imaps";
				props.setProperty("mail.imap.host", _mailServer);
			}
		}

		// Create the authenticator
		Authenticator auth = null;
		if (_username != null) {
			auth = new SMTPAuthenticator(_username, _password);
		}
//		java.security.Security
//				.addProvider(new com.sun.net.ssl.internal.ssl.Provider());

		// -- Attaching to default Session, or we could start a new one --
		_session = Session.getDefaultInstance(props, auth);
		try {
			// Set up the mail receiver
			_store = _session.getStore(_serverType);

			// Connect the mail sender
			_transport = _session.getTransport();
			if (_autoConnect) {
				connectThreaded();
			}
		} catch (Exception e) {
			MessageBay.errorMessage("Error in ExpMail setup");
		}
	}

	/**
	 * Attempts to connect the mail transporter to the mail server.
	 * 
	 */
	public static void connect() {
		if (_theMailSession._bConnecting) {
			MessageBay.errorMessage("Already connecting to mail server");
			return;
		} else if (_theMailSession != null) {
			_theMailSession.connectThreaded();
		}
	}

	private void connectThreaded() {
		Thread t = new ConnectThread(this);
		t.start();
	}

	public synchronized void connectServers() {
		try {
			if (!_transport.isConnected()) {
				// MessageBay.displayMessage("Connecting to SMTP server...");
				_bConnecting = true;
				_transport.connect();
				// MessageBay.displayMessage("SMTP server connected",
				// Color.green);
			} else {
				MessageBay.warningMessage("SMTP server already connected");
			}
		} catch (MessagingException e) {
			MessageBay.errorMessage("Error connecting to SMTP server");
		}

		if (_mailServer != null && !_store.isConnected()) {
			try {
				// Text message = MessageBay.displayMessage("Connecting to "
				// + _mailServer + "...");
				_store.connect(_mailServer, _username, _password);

				// -- Try to get hold of the default folder --
				_folder = _store.getDefaultFolder();
				if (_folder == null)
					throw new Exception("No default folder");
				// -- ...and its INBOX --
				_folder = _folder.getFolder("INBOX");
				if (_folder == null)
					throw new Exception("No INBOX");
				// -- Open the folder for read only --
				_folder.open(Folder.READ_WRITE);
				_folder.addMessageCountListener(new MessageCountAdapter() {
					@Override
					public void messagesAdded(MessageCountEvent e) {
						try {
							MessageBay.displayMessage("New mail message!",
									null, Color.green, true, "getMailByID "
											+ _folder.getMessageCount());
							/*
							 * TODO use messageID incase mail gets deleted
							 * externally
							 */
						} catch (MessagingException e1) {
							e1.printStackTrace();
						}
						displayUnreadMailCount();
					}
				});

				new Thread() {
					public void run() {
						for (;;) {
							try {
								Thread.sleep(5000);
								/*
								 * sleep for freq milliseconds. This is to force
								 * the IMAP server to send us EXISTS
								 * notifications
								 */
								// TODO: Is synchronisation needed?
								_folder.getMessageCount();
								_folder.exists();
								// _folder.getUnreadMessageCount();
							} catch (Exception e) {
								e.printStackTrace();
								MessageBay
										.errorMessage("Mail connection unavailable");
								finalise();
								break;
							}
						}
					}
				}.start();

				MessageBay.displayMessage("Mail connection complete",
						Color.GREEN);

				displayUnreadMailCount();

			} catch (Exception e) {
				// e.printStackTrace();
				MessageBay.errorMessage("Error connecting to " + _mailServer);
			}
		}
		_bConnecting = false;
	}

	public void displayUnreadMailCount() {
		int unreadCount = getUnreadCount();
		Text text = new Text(-1, unreadCount + UNREAD_MESSAGE
				+ (unreadCount == 1 ? "" : "s"), Color.BLUE, null);
		if (unreadCount > 0)
			text.addAction("getUnreadMail " + unreadCount);
		MessageBay.displayMessage(text);
	}

	public static boolean sendTextMessage(String to, String cc, String bcc,
			String subject, String body, Object attachments) {

		if (_theMailSession == null) {
			MessageBay.errorMessage("Add mail settings to profile frame");
			return false;
		}

		if (_theMailSession._bConnecting) {
			MessageBay.errorMessage("Busy connecting to mail server...");
			return false;
		}

		return _theMailSession
				.sendText(to, cc, bcc, subject, body, attachments);
	}

	private synchronized boolean sendText(String to, String cc, String bcc,
			String subject, String body, Object attachments) {
		if (!_transport.isConnected()) {
			MessageBay
					.warningMessage("Not connected to server, attempting to reconnect...");
			try {
				_bConnecting = true;
				_transport.connect();
				_bConnecting = false;
			} catch (Exception e) {
				MessageBay.errorMessage("Could not connect to mail server");
				_bConnecting = false;
				return false;
			}
		}

		if (to == null) {
			MessageBay.errorMessage("Add tag @to:<sendToEmailAddress>");
			return false;
		}

		try {
			// -- Create a new message --
			Message msg = new MimeMessage(_session);

			// -- Set the FROM and TO fields --
			msg.setFrom(new InternetAddress(_address));
			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(
					to, false));

			// -- We could include CC recipients too --
			if (cc != null) {
				msg.setRecipients(Message.RecipientType.CC, InternetAddress
						.parse(cc, false));
			}

			if (bcc != null) {
				msg.setRecipients(Message.RecipientType.BCC, InternetAddress
						.parse(bcc, false));
			}

			// -- Set the subject and body text --
			msg.setSubject(subject);
			msg.setContent(body.toString(), "text/plain");

			// -- Set some other header information --
			msg.setHeader("ExpMail", "Expeditee");
			msg.setSentDate(new Date());

			Transport.send(msg, msg.getRecipients(Message.RecipientType.TO));
		} catch (Exception e) {
			MessageBay.errorMessage("Error sending mail: " + e.getMessage());
			return false;
		}
		MessageBay.displayMessage("Message sent OK.");
		return true;
	}

	public static void init(Frame settingsFrame) {

		if (settingsFrame == null)
			return;

		if (_theMailSession == null)
			_theMailSession = new MailSession(settingsFrame);
	}

	private class SMTPAuthenticator extends javax.mail.Authenticator {
		private String _username;

		private String _password;

		public SMTPAuthenticator(String username, String password) {
			_username = username;
			_password = password;
		}

		@Override
		public PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(_username, _password);
		}
	}

	public static MailSession getInstance() {
		return _theMailSession;
	}

	/**
	 * Closes the mail folders.
	 * 
	 * @return true if the folders needed to be closed.
	 */
	public synchronized boolean finalise() {
		boolean result = false;
		try {
			if (_transport != null && _transport.isConnected()) {
				_transport.close();
				result = true;
			}

			if (_folder != null && _folder.isOpen()) {
				_folder.close(false);
				result = true;
			}

			if (_store != null && _store.isConnected()) {
				_store.close();
				result = true;
			}
		} catch (Exception e) {

		}
		return result;
	}

	public int getUnreadCount() {
		try {
			return _folder.getUnreadMessageCount();
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * Gets mail and puts a list of mail items with links to the messages
	 * content. TODO: Put reply and forward button on the frame...
	 * 
	 * @param flag
	 * @param isPresent
	 * @param frame
	 * @param point
	 * @return
	 */
	public String getMailString(Flag flag, Boolean isPresent) {
		StringBuffer sb = new StringBuffer();
		// -- Get the message wrappers and process them --
		Message[] msgs;
		try {
			msgs = _folder.getMessages();
			for (int msgNum = 0; msgNum < msgs.length; msgNum++) {

				if (flag == null
						|| msgs[msgNum].getFlags().contains(flag) == isPresent) {
					if (sb.length() > 0) {
						sb.append('\n').append('\n').append(
								"-----------------------------").append('\n')
								.append('\n');
					}
					// Only get messages that have not been read
					sb.append(getTextMessage(msgs[msgNum]));
				}
			}
		} catch (MessagingException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	/**
	 * Gets mail and puts a list of mail items with links to the messages
	 * content. TODO: Put reply and forward button on the frame...
	 * 
	 * @param flag
	 * @param isPresent
	 * @param frame
	 * @param point
	 * @return
	 */
	public Collection<Text> getMail(Flag flag, Boolean isPresent, Frame frame,
			Point point, int numberOfMessages) {
		if (_folder == null)
			return null;

		Collection<Text> mailItems = new LinkedList<Text>();
		// -- Get the message wrappers and process them --
		Message[] msgs;
		try {
			msgs = _folder.getMessages();

			// msgs[0].get

			int messagesRead = 0;

			for (int msgNum = msgs.length - 1; messagesRead < numberOfMessages
					&& msgNum >= 0; msgNum--) {
				if (flag == null
						|| msgs[msgNum].getFlags().contains(flag) == isPresent) {
					Text newItem = readMessage(msgs[msgNum], msgNum + 1, frame,
							point);
					// TODO: May want to reverse the order of mail messages
					if (newItem != null) {
						mailItems.add(newItem);
						point.y += newItem.getBoundsHeight();
						messagesRead++;
					} else {
						newItem = null;
					}
				}

			}
		} catch (MessagingException e) {
			e.printStackTrace();
		}

		return mailItems;
	}

	public Text getMail(Frame frame, Point point, int msgNum) {
		if (_folder == null)
			return null;

		// -- Get the message wrappers and process them --
		try {
			Message[] msgs = _folder.getMessages();
			return readMessage(msgs[msgNum], msgNum + 1, frame, point);
		} catch (ArrayIndexOutOfBoundsException ae) {
			/*
			 * Just return null... error message will be displayed in the
			 * calling method
			 */
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private Text readMessage(final Message message, final int messageNo,
			final Frame frame, final Point point) {

		final Text source = FrameDNDTransferHandler.importString(
				"Reading message " + messageNo + "...", point);

		new Thread() {
			public void run() {
				try {
					String subject = message.getSubject();
					source.setText("[" + messageNo + "] " + subject);
					// Create a frameCreator
					final FrameCreator frames = new FrameCreator(frame
							.getFramesetName(), frame.getPath(), subject,
							false, false);

					frames.addText("@date: " + message.getSentDate(), null,
							null, null, false);

					// Get the header information
					String from = ((InternetAddress) message.getFrom()[0])
							.toString();
					Text fromAddressItem = frames.addText("@from: " + from,
							null, null, null, false);

					addRecipients(message, frames, _address, RecipientType.TO,
							"@to: ");
					addRecipients(message, frames, null, RecipientType.CC,
							"@cc: ");

					// Read reply to addresses
					Text reply = addAddresses(message, frames, from, message
							.getReplyTo(), "@replyTo: ");
					/*
					 * If the only person to reply to is the person who sent the
					 * mail add a tag that just says reply
					 */
					if (reply == null) {
						reply = frames.addText("@reply", null, null, null,
								false);
						reply.setPosition(10 + fromAddressItem.getX()
								+ fromAddressItem.getBoundsWidth(),
								fromAddressItem.getY());
					}
					reply.addAction("reply");
					// frames.addSpace(15);

					// -- Get the message part (i.e. the message itself) --
					Part messagePart = message;
					Object content = messagePart.getContent();
					// -- or its first body part if it is a multipart
					// message --
					if (content instanceof Multipart) {
						messagePart = ((Multipart) content).getBodyPart(0);
						// System.out.println("[ Multipart Message ]");
					}
					// -- Get the content type --
					String contentType = messagePart.getContentType()
							.toLowerCase();
					// -- If the content is plain text, we can print it --
					// System.out.println("CONTENT:" + contentType);
					if (contentType.startsWith("text/plain")
							|| contentType.startsWith("text/html")) {
						InputStream is = messagePart.getInputStream();
						BufferedReader reader = new BufferedReader(
								new InputStreamReader(is));
						String thisLine = reader.readLine();
						StringBuffer nextText = new StringBuffer();
						while (thisLine != null) {
							// A blank line is a signal to start a new text item
							if (thisLine.trim().equals("")) {
								addTextItem(frames, nextText.toString());
								nextText = new StringBuffer();
							} else {
								nextText.append(thisLine).append('\n');
							}
							thisLine = reader.readLine();
						}
						addTextItem(frames, nextText.toString());
					}
					message.setFlag(Flag.SEEN, true);

					frames.save();
					source.setLink(frames.getName());
					FrameGraphics.requestRefresh(true);
				} catch (MessageRemovedException mre) {
					source.setText("Message removed from inbox");
				} catch (MessagingException e) {
					String message = e.getMessage();
					if (message == null) {
						e.printStackTrace();
						MessageBay.errorMessage("GetMail error!");
					} else {
						MessageBay.errorMessage("GetMail error: " + message);
					}
				} catch (Exception e) {
					MessageBay.errorMessage("Error reading mail: "
							+ e.getMessage());
					e.printStackTrace();
				}
			}

			/**
			 * @param frames
			 * @param nextText
			 */
			private void addTextItem(final FrameCreator frames, String nextText) {
				nextText = nextText.trim();
				if (nextText.length() == 0)
					return;
				// Remove the last char if its a newline
				if (nextText.charAt(nextText.length() - 1) == '\n')
					nextText = nextText.substring(0, nextText.length() - 1);
				// TODO: Make the space a setting in frame creator
				frames.addSpace(10);
				frames.addText(nextText, null, null, null, false);
			}
		}.start();
		return source;
	}

	/**
	 * "getTextMessage()" method to print a message.
	 */
	public String getTextMessage(Message message) {
		StringBuffer sb = new StringBuffer();

		try {
			// Get the header information
			String from = ((InternetAddress) message.getFrom()[0])
					.getPersonal();
			if (from == null)
				from = ((InternetAddress) message.getFrom()[0]).getAddress();
			sb.append("FROM: " + from).append('\n');
			String subject = message.getSubject();
			sb.append("SUBJECT: " + subject).append('\n').append('\n');
			// -- Get the message part (i.e. the message itself) --
			Part messagePart = message;
			Object content = messagePart.getContent();
			// -- or its first body part if it is a multipart message --
			if (content instanceof Multipart) {
				messagePart = ((Multipart) content).getBodyPart(0);
				System.out.println("[ Multipart Message ]");
			}
			// -- Get the content type --
			String contentType = messagePart.getContentType();
			// -- If the content is plain text, we can print it --
			// System.out.println("CONTENT:" + contentType);
			if (contentType.startsWith("text/plain")
					|| contentType.startsWith("text/html")) {
				InputStream is = messagePart.getInputStream();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is));
				String thisLine = reader.readLine();
				while (thisLine != null) {
					sb.append(thisLine).append('\n');
					thisLine = reader.readLine();
				}
			}
			message.setFlag(Flag.SEEN, true);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	public Folder getFolder() {
		return _folder;
	}

	/**
	 * @param message
	 * @param frames
	 * @param type
	 * @throws MessagingException
	 */
	private Text addAddresses(final Message message, final FrameCreator frames,
			final String excludeAddress, final Address[] addresses,
			String typeTag) throws MessagingException {
		if (addresses == null)
			return null;

		StringBuffer sb = new StringBuffer();
		boolean foundOtherRecipients = false;
		for (Address addy : addresses) {
			// Only show the to flag if this message was sent to
			// other people
			if (excludeAddress == null
					|| !addy.toString().toLowerCase().contains(
							excludeAddress.toLowerCase())) {
				foundOtherRecipients = true;
			}
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(addy.toString());
		}
		Text reply = null;
		if (foundOtherRecipients) {
			reply = frames.addText(typeTag + sb.toString(), null, null, null,
					false);
		}
		return reply;
	}

	private Text addRecipients(final Message message,
			final FrameCreator frames, String excludeAddress,
			RecipientType type, String typeTag) throws MessagingException {
		// Read and display all the recipients of the message
		Address[] toRecipients = message.getRecipients(type);
		return addAddresses(message, frames, excludeAddress, toRecipients,
				typeTag);
	}
}
