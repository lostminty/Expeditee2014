package org.expeditee.math;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

import org.expeditee.gui.AttributeValuePair;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameUtils;
import org.expeditee.items.Item;
import org.expeditee.items.Text;
import org.lsmp.djep.vectorJep.VectorJep;
import org.lsmp.djep.vectorJep.values.MVector;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.Variable;

public class ExpediteeJEP extends VectorJep {
	Observer observer = null;

	public ExpediteeJEP() {
		super();
		addStandardFunctions();
		addStandardConstants();
		setImplicitMul(true);
		setAllowAssignment(true);
		setAllowUndeclared(true);
		resetObserver();
	}

	public void resetObserver() {
		observer = new Observer() {
			private String _attribute = "";

			public void update(Observable ob, Object o) {
				assert (o instanceof Variable);
				_attribute = ((Variable) o).getName();
			}

			@Override
			public String toString() {
				return _attribute;
			}
		};
		getSymbolTable().addObserver(observer);
		getSymbolTable().addObserverToExistingVariables(observer);
	}

	public String evaluate(Node node) throws ParseException {
		return evaluate(node, true);
	}

	public String evaluate(Node node, boolean prependVarName)
			throws ParseException {
		Object rawResult = rawResult = super.evaluate(node);

		if (rawResult instanceof Double) {
			Double result = (Double) rawResult;
			if (result.isNaN()) {
				return null;
			}
			NumberFormat nf = NumberFormat.getInstance();
			// TODO see if the parser can handle commas if a flag is switched
			nf.setGroupingUsed(false);
			nf.setMinimumFractionDigits(0);
			nf.setMaximumFractionDigits(15);
			String varName = observer.toString();
			if (varName.length() > 0)
				return (prependVarName ? (varName + ": ") : "")
						+ nf.format(result);

			return nf.format(result);
		}
		return rawResult.toString();
	}

	public void addVariables(Frame frame) {
		if (frame == null)
			return;
		// Check for variables
		for (Text t : frame.getTextItems()) {
			if (t.isAnnotation())
				continue;

			AttributeValuePair avp = t.getAttributeValuePair();
			if (avp.hasPair()) {
				try {
					if (getVar(avp.getAttribute()) == null) {
						Double d = avp.getDoubleValue();
						if (!d.equals(Double.NaN))
							addVariable(avp.getAttribute(), avp
									.getDoubleValue());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (t.isLineEnd()) {
				Collection<Item> enclosed = FrameUtils.getItemsEnclosedBy(
						frame, t.getEnclosedShape());
				String variableName = t.getText();
				if (!variableName.contains(" "))
					addVectorVariable(enclosed, variableName);
			}
		}
	}

	/**
	 * @param textItems
	 * @param variableName
	 */
	public void addVectorVariable(Collection<Item> items, String variableName) {
		Collection<Double> vector = new LinkedList<Double>();
		for (Item i : items) {
			if (i instanceof Text && !i.isAnnotation() && !i.isLineEnd()) {
				try {
					Double value = i.getAttributeValuePair().getDoubleValue();
					if (!value.equals(Double.NaN)) {
						vector.add(value);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		// At the moment the VSum method will not work with empty vectors.
		if (vector.size() > 0)
			addVariable(variableName, MVector.getInstance(vector.toArray()));
	}

	public String getNewVariable() {
		return observer.toString();
	}
}
