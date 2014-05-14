package org.expeditee.agents;

import java.text.NumberFormat;

import org.expeditee.gui.AttributeUtils;
import org.expeditee.gui.AttributeValuePair;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.items.Item;
import org.expeditee.items.Text;

public class ComputeTree extends DefaultAgent {

	private static final String COMPUTE_TAG = "@compute:";

	private static final NumberFormat _format = NumberFormat.getInstance();

	private static final String DECIMAL_PLACES_TAG = "@dp:";

	@Override
	protected Frame process(Frame frame) {
		_format.setMinimumFractionDigits(0);
		if (computeFrame(frame) == null && !_stop) {
			message("Nothing to compute!");
		}

		return null;
	}

	/**
	 * Computes the value of a frame.
	 * 
	 * @param frame
	 * @return the value of the frame or null if the frame does not have the
	 *         compute tag or any values to compute;
	 */
	private Double computeFrame(Frame frame) {
		// Search for @Compute frame tag
		String computeTag = null;
		
		//TODO can speed this up by using frame.hasAnnotations
		for (Item i : frame.getItems()) {
			if (_stop)
				return null;
			if (i.isAnnotation()) {
				String s = ((Text) i).getFirstLine().toLowerCase().trim();
				if (s.startsWith(COMPUTE_TAG)) {
					computeTag = s.substring(COMPUTE_TAG.length()).trim();
					// break;
				} else if (s.startsWith(DECIMAL_PLACES_TAG)) {
					try {
						String[] values = s.substring(
								DECIMAL_PLACES_TAG.length()).trim().split(
								"\\s+");
						int min = 0;
						int max = 0;
						if (values.length == 1) {
							min = Integer.parseInt(values[0]);
							max = min;
						} else if (values.length == 2) {
							min = Integer.parseInt(values[0]);
							max = Integer.parseInt(values[1]);
						}
						_format.setMinimumFractionDigits(min);
						_format.setMaximumFractionDigits(max);
					} catch (Exception e) {
					}
				}
			}
		}

		// Check that the compute tag exists
		if (computeTag == null)
			computeTag = "+";

		// check for text versions of the operators
		if (computeTag.length() > 1) {
			if (computeTag.equalsIgnoreCase("add"))
				computeTag = "+";
			else if (computeTag.equalsIgnoreCase("subtract"))
				computeTag = "-";
			else if (computeTag.equalsIgnoreCase("multiply"))
				computeTag = "*";
			else if (computeTag.equalsIgnoreCase("divide"))
				computeTag = "/";
		}
		char operator = computeTag.charAt(computeTag.length() - 1);
		// double defaultValue = (operator == '*' ? 1.0 : 0.0);
		Double result = null;
		// Iterate through all valid items on the frame performing the correct
		// operation
		for (Item i : frame.getBodyTextItems(false)) {
			if (_stop)
				return null;
			Double value = null;
			// Process the frame that each item is linked to
			if (i.getLink() != null) {
				value = computeFrame(FrameIO.LoadFrame(i.getAbsoluteLink()));
				// Set the items text
				if (value != null)
					AttributeUtils
							.replaceValue((Text) i, _format.format(value));
			}

			if (value == null) {
				try {
					value = new AttributeValuePair(i.getText())
							.getDoubleValue();
				} catch (NumberFormatException e) {
					continue;
				}
			}
			
			if(value.equals(Double.NaN))
				continue;

			if (value != null) {
				_itemCount++;

				// the first time we find an item init result
				if (result == null) {
					result = value;
				} else {
					switch (operator) {
					case '*':
						result *= value;
						break;
					case '+':
						result += value;
						break;
					case '-':
						result -= value;
						break;
					case '/':
						result /= value;
						break;
					}
				}
			}
		}

		if (result != null) {
			AttributeUtils.replaceValue(frame.getTitleItem(), _format
					.format(result));
		}
		_frameCount++;
		FrameIO.ForceSaveFrame(frame);

		return result;
	}
}
