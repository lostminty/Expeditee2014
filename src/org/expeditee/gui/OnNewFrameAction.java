package org.expeditee.gui;

import org.expeditee.items.Item;

public interface OnNewFrameAction {
    public void exec(Item linker, Frame newFrame);
}
