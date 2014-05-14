package org.expeditee.actions;

import java.util.List;

import org.expeditee.gui.DisplayIO;
import org.expeditee.items.Constraint;
import org.expeditee.items.Item;

/**
 * This class is used for temporary debugging actions that will get removed from
 * the final product.
 * 
 * @author jdm18
 * 
 */
public class Debug {

	/**
	 * Outputs the list of constraints all Items in the current Frame have.
	 */
	public static void ShowConstraints() {
		List<Item> items = DisplayIO.getCurrentFrame().getItems();

		for (Item i : items)
			if (i.isLineEnd()) {
				System.out.println(i.getID());

				for (Constraint c : i.getConstraints())
					if (c.getOppositeEnd(i) != null)
						System.out.println("\t"
								+ c.getOppositeEnd(i).getID());
					else
						System.out.println("\tNULL");

				System.out.println("------------");
			}

		System.out.println("==================");
	}

	public static void PrintLine() {
		System.out.println("Action");
	}
}
