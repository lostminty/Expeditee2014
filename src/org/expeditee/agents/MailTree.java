package org.expeditee.agents;

import java.util.Collection;
import java.util.Map;

import org.expeditee.actions.Misc;
import org.expeditee.agents.mail.MailSession;
import org.expeditee.gui.AttributeValuePair;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.FreeItems;
import org.expeditee.items.Item;
import org.expeditee.items.Text;
import org.expeditee.stats.Formatter;

public class MailTree extends DefaultAgent {

	@Override
	public boolean initialise(Frame frame, Item item) {
		return super.initialise(frame, item);
	}

	private String getFrameText(Frame frame, String prefix) {
		StringBuffer sb = new StringBuffer();
		// Get the text to mail
		for (Text t : frame.getBodyTextItems(false)) {
			if (t.hasLink()) {
				Frame linkedFrame = FrameIO.LoadFrame(t.getAbsoluteLink());
				if (linkedFrame == null)
					continue;

				String frameTitle = linkedFrame.getTitle();
				sb.append('\n').append(prefix).append(frameTitle).append('\n')
						.append(prefix);
				// for (int i = 0; i < frameTitle.length(); i++) {
				// sb.append('-');
				// }
				sb.append("----------------");

				sb.append("\n");
				sb.append(getFrameText(linkedFrame, prefix));
			} else {
				sb.append(prefix).append(t.getText()).append("\n\n");
			}
		}

		Text original = frame.getAnnotation("original");
		if (original != null) {
			Frame linkedFrame = FrameIO.LoadFrame(original.getAbsoluteLink());
			if (linkedFrame != null) {
				sb.append("\n\n");
				String date = linkedFrame.getAnnotationValue("date");
				if (date != null) {
					sb.append("On ").append(date);
				}
				String from = linkedFrame.getAnnotationValue("from");
				if (from != null) {
					if (sb.length() > 0)
						sb.append(", ");
					//else
					//	sb.append("> ");
					sb.append(from);
					sb.append(" wrote:");
				}
				sb.append("\n\n");
				sb.append(getFrameText(linkedFrame, ">"));
			}
		}

		return sb.toString();
	}

	/**
	 * Email's a frame. The email addy's the mail will be sent to are taken from
	 * the frame tags of the form \@to:<AddressList>, \@cc:<AddressList> or
	 * \@bcc:<AddressList>. If such frame tags do not exist the agent looks for
	 * items on the end of the cursor. Different types of addresses can be
	 * grouped in boxes with a corner labelled, cc, bc or to. If a box does not
	 * have a label it is assumed to contain SendTo addresses.
	 */
	@Override
	protected Frame process(Frame frame) {
		String subject = frame.getTitle();

		// Get the text to mail
		String body = getFrameText(frame, "");

		String to = frame.getAnnotationValue("to");
		String cc = frame.getAnnotationValue("cc");
		String bcc = frame.getAnnotationValue("bcc");

		// Check for the address on the end of the cursor
		if (to == null) {
			StringBuffer ccString = new StringBuffer();
			StringBuffer toString = new StringBuffer();
			StringBuffer bccString = new StringBuffer();
			// Check for a group of text items attached to the cursor
			Map<String, Collection<String>> groupedText = FreeItems
					.getGroupedText();
			Collection<String> ccStrings = groupedText.get("cc");
			Collection<String> toStrings = groupedText.get("to");
			Collection<String> bccStrings = groupedText.get("bcc");

			if (cc == null && ccStrings != null) {
				for (String address : ccStrings) {
					if (address.charAt(0) == (AttributeValuePair.ANNOTATION_CHAR))
						address = address.substring(1);
					ccString.append(address);
					ccString.append(',');
				}
			}

			if (bcc == null && bccStrings != null) {
				for (String address : bccStrings) {
					if (address.charAt(0) == (AttributeValuePair.ANNOTATION_CHAR))
						address = address.substring(1);
					bccString.append(address);
					bccString.append(',');
				}
			}

			if (toStrings != null) {
				for (String address : toStrings) {
					if (address.charAt(0) == (AttributeValuePair.ANNOTATION_CHAR))
						address = address.substring(1);
					toString.append(address);
					toString.append(',');
				}
			} else {
				Collection<String> otherStrings = groupedText.get("");
				for (String address : otherStrings) {
					if (address.charAt(0) == (AttributeValuePair.ANNOTATION_CHAR))
						address = address.substring(1);
					if (address.startsWith("cc:") && address.length() > 5) {
						ccString.append(address.substring(3).trim());
						ccString.append(',');
					} else if (address.startsWith("bcc:")
							&& address.length() > 6) {
						bccString.append(address.substring(4).trim());
						bccString.append(',');
					} else {
						toString.append(address);
						toString.append(',');
					}
				}
			}
			if (toString.length() > 0) {
				toString.deleteCharAt(toString.length() - 1);
				to = toString.toString();
			}

			if (ccString.length() > 0) {
				ccString.deleteCharAt(ccString.length() - 1);
				cc = ccString.toString();
			}

			if (bccString.length() > 0) {
				bccString.deleteCharAt(ccString.length() - 1);
				bcc = bccString.toString();
			}
		}

		FreeItems.getInstance().clear();
		
		//Produce output for the user to put down on their frame
		StringBuffer sb = new StringBuffer();
		sb.append("@Sent: ").append(Formatter.getDateTime());
		
		if(to != null){
			sb.append("\nTo: " + to);
		}
		
		if(cc != null){
			sb.append("\nCc: " + cc);
		}
		
		if(bcc != null){
			sb.append("\nBcc: " + bcc);
		}
		
		Misc.attachStatsToCursor(sb.toString());
		
		// Last chance for the user to stop
		if (_stop)
			return null;
		// Allow the user to do other stuff while the message gets sent
		_running = false;
		MailSession.sendTextMessage(to, cc, bcc, subject, body, null);

		return null;
	}
}
