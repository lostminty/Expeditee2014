package org.expeditee.gui;

import java.util.Collection;

import org.expeditee.items.UserAppliedPermission;

public class Overlay {
	public Frame Frame;

	public UserAppliedPermission permission;

	public Overlay(Frame overlay, UserAppliedPermission level) {
		Frame = overlay;
		permission = level;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || o.getClass() != Overlay.class)
			return false;

		return ((Overlay) o).Frame == Frame;
	}

	@Override
	public int hashCode() {
		return 0;
	}
	
	public static Overlay getOverlay(Collection<Overlay>overlays, Frame frame){
		// Check the frame is in the list of overlays
		for (Overlay o : overlays) {
			if (o.Frame.equals(frame)) {
				return o;
			}
		}
		return null;
	}
}
