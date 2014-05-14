package org.expeditee.actions;

import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.expeditee.agents.SearchGreenstone;
import org.expeditee.greenstone.ResultDocument;
import org.expeditee.gui.AttributeValuePair;
import org.expeditee.gui.MessageBay;

public class GreenstoneActions {
	public static void givePositiveFeedback(String text) {
		giveFeedback(1, text);
	}

	public static void giveNegativeFeedback(String text) {
		giveFeedback(-1, text);
	}

	public static void clearGreenstoneSession() {
		SearchGreenstone.clearSession();
	}

	public static void giveFeedback(double change, String text) {
		Map<String, ResultDocument> sessionResults = SearchGreenstone
				.getConnection().getSessionResults();

		if (sessionResults.size() == 0) {
			MessageBay.errorMessage("The Greenstone Session is empty");
			return;
		}

		for (String line : text.split("\n")) {
			AttributeValuePair avp = new AttributeValuePair(line, false);

			Method getMethod = null;
			String targetValue = null;

			if (!avp.hasPair()) {
				if (!avp.hasAttributeOrValue())
					continue;
				/*
				 * If only an attribute is supplied then search for it in the
				 * entire biliographic text.
				 * 
				 */
				try {
					getMethod = ResultDocument.class.getMethod("toString",
							new Class[] {});
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				}
				targetValue = avp.getAttributeOrValue();
			} else {
				targetValue = avp.getValue().trim().toLowerCase();
				String attribute = avp.getAttribute().trim().toLowerCase();

				String methodName = "get"
						+ Character.toUpperCase(attribute.charAt(0))
						+ attribute.substring(1);

				try {
					getMethod = ResultDocument.class.getMethod(methodName,
							new Class[] {});
				} catch (SecurityException e) {
					e.printStackTrace();
					continue;
				} catch (NoSuchMethodException e) {
					// User provided an invalid attribute value pair
					MessageBay.errorMessage("Invalid feedback characteristic: "
							+ attribute);
					continue;
				}
				
				if(getMethod == null){
					MessageBay.errorMessage("Document attribute does not exist: " + methodName);
				}
			}

			for (ResultDocument srd : sessionResults.values()) {
				double sessionScore = srd.getSessionScore();

				/*
				 * You will refine this part to make more sophisticated
				 * comparisons and more sensible score modifications
				 */

				Object value = null;
				try {
					if(getMethod.getReturnType() == null){
						//Get methods should always return a value
						assert(false);
					}
					value = getMethod.invoke(srd, new Object[] {}).toString();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}catch (NullPointerException e) {
					e.printStackTrace();
				}

				if (value != null) {
					if (value.toString().trim().toLowerCase().contains(targetValue)) {
						sessionScore = sessionScore + change;
					}

					srd.setSessionScore(sessionScore);
				}
			}
		}
		MessageBay.displayMessage("Feedback complete", null, Color.green
				.darker(), true, null);
	}
}
