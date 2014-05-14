package org.expeditee.settings.templates;

import java.awt.Color;

import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameUtils;
import org.expeditee.gui.FreeItems;
import org.expeditee.items.Item;
import org.expeditee.items.ItemUtils;
import org.expeditee.items.Text;
import org.expeditee.setting.ArraySetting;
import org.expeditee.setting.FrameSetting;
import org.expeditee.setting.StringSetting;
import org.expeditee.setting.TextSetting;

public class TemplateSettings {

	public static final StringSetting DefaultFrame = new StringSetting("The default frame", null) {
		@Override
		public boolean setSetting(Text text) {
			_value = FrameUtils.getLink(text, _value);
			return true;
		}
	};
	
	public static final FrameSetting CursorFrame = new FrameSetting("Items on this frame will be used as the cursor (clearing the frame or removing the link will default back to a normal cursor)") {
		@Override
		public void run(Frame frame) {
			FreeItems.getCursor().addAll(ItemUtils.CopyItems(frame.getAllItems()));
			for (Item i : FreeItems.getCursor()) {
				i.setParent(null);
			}
			DisplayIO.setCursor(Item.HIDDEN_CURSOR);
			DisplayIO.setCursor(Item.DEFAULT_CURSOR);
		}
	};
	
	public static final ArraySetting<Color> ColorWheel = new ArraySetting<Color>("The colours of items in the child frame are used to populate the colour wheel",
			new Color[] { Color.BLACK, Color.RED, Color.BLUE, Item.GREEN, Color.MAGENTA, Color.YELLOW.darker(), Color.WHITE }) {
		@Override
		public boolean setSetting(Text text) {
			Frame child = text.getChild();
    		if (child == null) {
    			return false;
    		}
    		_value = FrameUtils.getColorWheel(child);
    		return true;
		}
	};
	
	public static final ArraySetting<Color> FillColorWheel = new ArraySetting<Color>("The colours of items in the child frame are used to populate the colour wheel",
			new Color[] { new Color(255, 150, 150), new Color(150, 150, 255), new Color(150, 255, 150),
			new Color(255, 150, 255), new Color(255, 255, 100), Color.WHITE, Color.BLACK }) {
		@Override
		public boolean setSetting(Text text) {
			Frame child = text.getChild();
    		if (child == null) {
    			return false;
    		}
    		_value = FrameUtils.getColorWheel(child);
    		return true;
		}
	};
	
	public static final ArraySetting<Color> BackgroundColorWheel = new ArraySetting<Color>("The colours of items in the child frame are used to populate the colour wheel",
			new Color[] { new Color(235, 235, 235), new Color(225, 225, 255), new Color(195, 255, 255),
			new Color(225, 255, 225), new Color(255, 255, 195), new Color(255, 225, 225),
			new Color(255, 195, 255), Color.WHITE, Color.GRAY, Color.DARK_GRAY, Color.BLACK, null }) {
		@Override
		public boolean setSetting(Text text) {
			Frame child = text.getChild();
    		if (child == null) {
    			return false;
    		}
    		_value = FrameUtils.getColorWheel(child);
    		return true;
		}
	};
	
	public static final TextSetting ItemTemplate = new TextSetting("Template for normal text items") {
		@Override
		public Text generateText() {
			return new Text("ItemTemplate");
		}
	};
	public static final TextSetting AnnotationTemplate = new TextSetting("Template for annotation text items") {
		@Override
		public Text generateText() {
			Text t = new Text("AnnotationTemplate");
    		t.setColor(Color.gray);
    		return t;
		}
	};

	public static final TextSetting CommentTemplate = new TextSetting("Template for code comment text items") {
		@Override
		public Text generateText() {
			Text t = new Text("CommentTemplate");
    		t.setColor(Color.green.darker());
    		return t;
		}
	};

	public static final TextSetting StatTemplate = new TextSetting("Template for statistics (e.g. extracted attributes) text items") {
		@Override
		public Text generateText() {
			Text t = new Text("StatsTemplate");
    		t.setColor(Color.BLACK);
    		t.setBackgroundColor(new Color(0.9F, 0.9F, 0.9F));
    		t.setFamily(Text.MONOSPACED_FONT);
    		t.setSize(14);
    		return t;
		}
	};
	
	public static final TextSetting TitleTemplate = new TextSetting("Template for Title text item") {
		@Override
		public Text generateText() {
			Text t = new Text("TitleTemplate");
			t.setSize(30);
    		t.setFontStyle("Bold");
    		t.setFamily("SansSerif");
    		t.setColor(Color.BLUE);
    		t.setPosition(25, 50);
    		return t;
		}
	};
	
	public static final TextSetting DotTemplate = new TextSetting("Template for dot items") {
		@Override
		public Text generateText() {
			return new Text("DotTemplate");
		}
	};
	
	public static final TextSetting TooltipTemplate = new TextSetting("Template for tooltips") {
		@Override
		public Text generateText() {
			Text t = new Text("TooltipTemplate");
    		t.setColor(Color.BLACK);
    		t.setBackgroundColor(new Color(0.7F, 0.7F, 0.9F));
    		// t.setFamily(Text.MONOSPACED_FONT);
    		t.setSize(14);
    		return t;
		}
	};
}
