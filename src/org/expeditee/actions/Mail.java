package org.expeditee.actions;

import java.awt.Color;
import java.util.Collection;

import javax.mail.Flags.Flag;

import org.expeditee.agents.mail.MailSession;
import org.expeditee.gui.AttributeValuePair;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameKeyboardActions;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.gui.MessageBay;
import org.expeditee.items.Item;
import org.expeditee.items.Text;
import org.expeditee.network.FrameShare;

public class Mail {
	/**
	 * Attempts to connect to mail servers
	 */
	public static void connectMail() {
		MailSession.connect();
	}

	/**
	 * Gets any unread mail and attaches it to the cursor for now
	 */
	public static String getAllMailString() {
		String allMail = MailSession.getInstance().getMailString(null, null);
		if (allMail.length() == 0) {
			return "Mailbox empty";
		}
		return allMail;
	}

	public static String getNewMailString() {
		String mail = MailSession.getInstance()
				.getMailString(Flag.RECENT, true);
		if (mail.length() == 0) {
			return "No new mail";
		}
		return mail;
	}

	public static String getUnreadMailString() {
		String mail = MailSession.getInstance().getMailString(Flag.SEEN, false);
		if (mail.length() == 0) {
			return "No unread mail";
		}
		return mail;
	}

	public static Collection<Text> getRecentMail(int number) {
		return getMail(null, null, number);
	}

	public static Collection<Text> getUnreadMail(Item clicked, int number) {
		if (clicked instanceof Text) {
			AttributeValuePair avp = new AttributeValuePair(clicked.getText());
			if (avp.hasPair()
					&& avp.getValue().contains(MailSession.UNREAD_MESSAGE)) {
				avp.setValue("0" + MailSession.UNREAD_MESSAGE + "s");
				clicked.setText(avp.toString());
				clicked.setActions(null);
			}
		}

		return getMail(Flag.SEEN, false, number);
	}

	public static Collection<Text> getUnreadMail() {
		return getMail(Flag.SEEN, false, Integer.MAX_VALUE);
	}

	public static Collection<Text> getAllMail() {
		return getMail(null, null, Integer.MAX_VALUE);
	}

	public static Text getMail(int firstMessage, int lastMessage) {
		// Swap message numbers if they are around the wrong way
		if (firstMessage > lastMessage) {
			int temp = firstMessage;
			firstMessage = lastMessage;
			lastMessage = temp;
		}

		MessageBay.errorMessage("Not yet supported");

		return null;
	}
	
	public static Collection<Text> getMail(int count) {
		return getMail(null, null, count);
	}

	public static Text getMailByID(int messageNo) {
		Text mailItem = MailSession.getInstance().getMail(
				DisplayIO.getCurrentFrame(), FrameMouseActions.getPosition(),
				messageNo - 1);
		// MessageBay.displayMessage(mailItems.size() + " messages read",
		// Color.green);
		if (mailItem == null) {
			MessageBay
					.errorMessage("Mail message does not exist: " + messageNo);
		}

		return mailItem;
	}

	public static Collection<Text> getMail() {
		return getAllMail();
	}

	public static Collection<Text> getNewMail() {
		return getMail(Flag.RECENT, true, Integer.MAX_VALUE);
	}

	private static Collection<Text> getMail(Flag flag, Boolean isPresent,
			int noOfMessages) {
		Collection<Text> mailItems = MailSession.getInstance().getMail(flag,
				isPresent, DisplayIO.getCurrentFrame(),
				FrameMouseActions.getPosition(), noOfMessages);
		// MessageBay.displayMessage(mailItems.size() + " messages read",
		// Color.green);

		return mailItems;
	}

	public static String getUnreadMailCount() {
		try {
			int count = MailSession.getInstance().getFolder()
					.getUnreadMessageCount();
			return count + " unread message" + (count == 1 ? "" : "s");
		} catch (Exception e) {
			return "Mail Error";
		}
	}

	public static String getAllMailCount() {
		try {
			int count = MailSession.getInstance().getFolder().getMessageCount();
			return count + " message" + (count == 1 ? "" : "s");
		} catch (Exception e) {
			return "Mail Error";
		}
	}

	public static String getMailCount() {
		return getAllMailCount();
	}

	public static String getNewMailCount() {
		try {
			int count = MailSession.getInstance().getFolder()
					.getNewMessageCount();
			return count + " new messages" + (count == 1 ? "" : "s");
		} catch (Exception e) {
			return "Mail Error";
		}
	}

	public static void reply(Frame frame, Item reply) {
		String fromAddress = frame.getAnnotationValue("from");
		if (fromAddress == null) {
			return;
		}

		reply.setActions(null);
		FrameMouseActions.tdfc(reply);

		Frame replyFrame = DisplayIO.getCurrentFrame();
		String titleText = frame.getTitle();
		// Add Re on the end if it is not already there
		if (titleText.length() >= 3
				&& !"re:".equals(titleText.substring(0, 3).toLowerCase())) {
			titleText = "Re: " + titleText;
		}
		replyFrame.setTitle(titleText);
		FrameKeyboardActions.Drop(null, false);

		// Add a link to the original message
		Text original = replyFrame.createNewText("@original");
		original.setPosition(FrameMouseActions.getPosition());
		original.setLink(frame.getName());
		FrameKeyboardActions.Drop(original, false);

		Text to = replyFrame.createNewText("@to: " + fromAddress);
		to.setPosition(FrameMouseActions.getPosition());
		to.addAction("MailTree");
		FrameKeyboardActions.Drop(to, false);
		DisplayIO.setCursorPosition(FrameMouseActions.MouseX,
				FrameMouseActions.MouseY + 15);
	}

	public static void sendMessage(String peerName, String message) {
		if (FrameShare.getInstance().sendMessage(message, peerName)) {
			MessageBay.displayMessage("Sent message to " + peerName,
					Color.green.darker());
		} else {
			MessageBay.errorMessage("Could not find " + peerName);
		}
	}
}
