package org.expeditee.io;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;

import org.expeditee.gui.Frame;
import org.expeditee.items.Constraint;
import org.expeditee.items.DotType;
import org.expeditee.items.Item;
import org.expeditee.items.Justification;
import org.expeditee.items.Line;
import org.expeditee.items.PermissionPair;
import org.expeditee.items.Text;

public abstract class DefaultFrameReader implements FrameReader {
	protected static LinkedHashMap<Character, Method> _ItemTags = null;

	protected static LinkedHashMap<Character, Method> _FrameTags = null;
	
	protected static Class[] pString = { String.class };
	protected static Class[] pInt = { int.class };
	protected static Class[] pIntO = { Integer.class };
	protected static Class[] pFloat = { float.class };
	protected static Class[] pFloatO = { Float.class };
	protected static Class[] pColor = { Color.class };
	protected static Class[] pBool = { boolean.class };
	protected static Class[] pFont = { Font.class };
	protected static Class[] pPoint = { Point.class };
	protected static Class[] pArrow = { float.class, double.class, double.class };
	protected static Class[] pList = { List.class };
	protected static Class[] pIntArray = { int[].class };
	protected static Class[] pItem = { Item.class };
	protected static Class[] pJustification = { Justification.class };
	protected static Class[] pPermission = { PermissionPair.class };
	
	public DefaultFrameReader(){
		if (_ItemTags != null && _FrameTags != null)
			return;

		_ItemTags = new LinkedHashMap<Character, Method>();
		_FrameTags = new LinkedHashMap<Character, Method>();

		try {
			_FrameTags.put('A', Frame.class.getMethod("setName", pString));
			_FrameTags.put('V', Frame.class.getMethod("setVersion", pInt));
			_FrameTags
					.put('p', Frame.class.getMethod("setPermission", pPermission));
			_FrameTags.put('U', Frame.class.getMethod("setOwner", pString));
			_FrameTags.put('D', Frame.class
					.getMethod("setDateCreated", pString));
			_FrameTags.put('M', Frame.class.getMethod("setLastModifyUser",
					pString));
			_FrameTags.put('d', Frame.class.getMethod("setLastModifyDate",
					pString));
			_FrameTags
					.put('F', Frame.class.getMethod("setFrozenDate", pString));

			_FrameTags.put('O', Frame.class.getMethod("setForegroundColor",
					pColor));
			_FrameTags.put('B', Frame.class.getMethod("setBackgroundColor",
					pColor));

			_ItemTags.put('S', Item.class.getMethod("setID", pInt));
			_ItemTags.put('s', Item.class.getMethod("setDateCreated", pString));
			_ItemTags.put('d', Item.class.getMethod("setColor", pColor));
			_ItemTags.put('G', Item.class.getMethod("setBackgroundColor",
					pColor));
			_ItemTags.put('K', Item.class.getMethod("setBorderColor",
					pColor));

			_ItemTags.put('R', Item.class.getMethod("setAnchorLeft", pFloatO));
			_ItemTags.put('H', Item.class.getMethod("setAnchorRight", pFloatO));
			_ItemTags.put('N', Item.class.getMethod("setAnchorTop", pFloatO));
			_ItemTags.put('I', Item.class.getMethod("setAnchorBottom", pFloatO));

			_ItemTags.put('P', Item.class.getMethod("setPosition", pPoint));
			_ItemTags.put('F', Item.class.getMethod("setLink", pString));
			_ItemTags.put('J', Item.class.getMethod("setFormula", pString));
			
			_ItemTags.put('X', Item.class.getMethod("setActions", pList));
			_ItemTags.put('x', Item.class.getMethod("setActionMark", pBool));
			_ItemTags.put('U', Item.class.getMethod("setActionCursorEnter",
					pList));
			_ItemTags.put('V', Item.class.getMethod("setActionCursorLeave",
					pList));
			_ItemTags.put('W', Item.class.getMethod("setActionEnterFrame",
					pList));
			_ItemTags.put('Y', Item.class.getMethod("setActionLeaveFrame",
					pList));
			_ItemTags.put('D', Item.class.getMethod("addToData", pString));
			_ItemTags.put('u', Item.class.getMethod("setHighlight", pBool));
			_ItemTags.put('e', Item.class.getMethod("setFillColor", pColor));
			_ItemTags.put('E', Item.class.getMethod("setGradientColor", pColor));
			_ItemTags.put('Q', Item.class.getMethod("setGradientAngle", pInt));
			
			_ItemTags.put('i', Item.class.getMethod("setFillPattern", pString));
			_ItemTags.put('o', Item.class.getMethod("setOwner", pString));
			_ItemTags.put('n', Item.class.getMethod("setLinkMark", pBool));
			_ItemTags
					.put('q', Item.class.getMethod("setLinkFrameset", pString));
			_ItemTags
					.put('y', Item.class.getMethod("setLinkTemplate", pString));
			_ItemTags.put('g', Item.class.getMethod("setLinePattern", pIntArray));

			_ItemTags.put('j', Item.class.getMethod("setArrow", pArrow));

			_ItemTags.put('v', Item.class.getMethod("setDotType", new Class[]{DotType.class}));
			_ItemTags.put('z', Item.class.getMethod("setFilled", pBool));
			
			_ItemTags.put('f', Text.class.getMethod("setFont", pFont));
			_ItemTags.put('t', Text.class.getMethod("setSpacing", pFloat));
			_ItemTags.put('T', Text.class.getMethod("appendLine", pString));
			_ItemTags.put('a', Text.class.getMethod("setWordSpacing", pInt));
			_ItemTags.put('b', Text.class.getMethod("setLetterSpacing", pFloat));
			_ItemTags.put('m', Text.class.getMethod("setInitialSpacing", pFloat));
			_ItemTags.put('w', Text.class.getMethod("setWidth", pIntO));
			_ItemTags.put('k', Text.class.getMethod("setJustification", pJustification));
			_ItemTags.put('r', Text.class.getMethod("setAutoWrap", pBool));

			_ItemTags.put('h', Item.class.getMethod("setThickness", pFloat));
			_ItemTags.put('l', Item.class.getMethod("setLineIDs", pString));
			_ItemTags.put('c', Item.class.getMethod("setConstraintIDs", pString));
			
			_ItemTags.put('A', Item.class.getMethod("setTooltip", pString));
			_ItemTags.put('B', Item.class.getMethod("setLinkHistory", pBool));
			
			_ItemTags.put('p', Item.class.getMethod("setPermission", pPermission));
			
			// Lines and constraints are created differently
			_ItemTags.put('L', Line.class.getMethod("setStartItem", pItem));
			_ItemTags.put('C', Constraint.class.getMethod("getID", (Class[]) null));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public Frame readFrame(String fullPath) throws IOException {
		Reader in = new InputStreamReader(new FileInputStream(fullPath), "UTF-8");
		return readFrame(new BufferedReader(in));
	}
}
