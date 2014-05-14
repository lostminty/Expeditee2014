package org.expeditee.items.widgets;

import junit.framework.TestCase;

public class InteractiveWidgetTest extends TestCase {

	public void testParseArgs1() {
		
		assertNull(InteractiveWidget.parseArgs(null));
		assertNull(InteractiveWidget.parseArgs(""));
		assertNull(InteractiveWidget.parseArgs("         "));
		
		String[] args = InteractiveWidget.parseArgs("arg1");
		assertEquals(1, args.length);
		assertEquals("arg1", args[0]);
		
		args = InteractiveWidget.parseArgs("arg1 arg2");
		assertEquals(2, args.length);
		assertEquals("arg1", args[0]);
		assertEquals("arg2", args[1]);
		
		args = InteractiveWidget.parseArgs("  )*^*$^   24224 5775    13 :  ");
		assertEquals(5, args.length);
		assertEquals(")*^*$^", args[0]);
		assertEquals("24224", args[1]);
		assertEquals("5775", args[2]);
		assertEquals("13", args[3]);
		assertEquals(":", args[4]);
	}
	
	public void testParseArgs2() {
		
		String[] args = InteractiveWidget.parseArgs("\"");
		assertNull(args);
		
		args = InteractiveWidget.parseArgs("\"\"");
		assertEquals(1, args.length);
		assertEquals("\"", args[0]);
		
		args = InteractiveWidget.parseArgs("\"\"\"\"\"\"\"");
		assertEquals(1, args.length);
		assertEquals("\"\"\"", args[0]);
		
		args = InteractiveWidget.parseArgs("  \"         \"   ");
		assertEquals(1, args.length);
		assertEquals("         ", args[0]);
		
		args = InteractiveWidget.parseArgs("arg1\" \"");
		assertEquals(1, args.length);
		assertEquals("arg1 ", args[0]);
		
		args = InteractiveWidget.parseArgs("arg1\" \"+8");
		assertEquals(1, args.length);
		assertEquals("arg1 +8", args[0]);
		
		args = InteractiveWidget.parseArgs("abc\" \"de 123\" \"45");
		assertEquals(2, args.length);
		assertEquals("abc de", args[0]);
		assertEquals("123 45", args[1]);
		
		args = InteractiveWidget.parseArgs("adgdag\" adgdgadag");
		assertEquals(1, args.length);
		assertEquals("adgdag adgdgadag", args[0]);
		
		args = InteractiveWidget.parseArgs("abc\"\"\" s \"   ggg");
		assertEquals(2, args.length);
		assertEquals("abc\" s ", args[0]);
		assertEquals("ggg", args[1]);
		
		args = InteractiveWidget.parseArgs("abc\"\"123");
		assertEquals(1, args.length);
		assertEquals("abc\"123", args[0]);
		
		args = InteractiveWidget.parseArgs("file=\"a URL with spaces\" title=\"A title with spaces and \"\"quotes\"\"\"");
		assertEquals(2, args.length);
		assertEquals("file=a URL with spaces", args[0]);
		assertEquals("title=A title with spaces and \"quotes\"", args[1]);
		
		
	}
	
	public void testFormatParseArgs() {
		
		AssertArgParsingAndFormatting(null);
		AssertArgParsingAndFormatting("");
		AssertArgParsingAndFormatting(" ");
		AssertArgParsingAndFormatting(" \"");
		AssertArgParsingAndFormatting("   \"   ");
		AssertArgParsingAndFormatting("arg1");
		AssertArgParsingAndFormatting("arg1 arg2");
		AssertArgParsingAndFormatting("%%#%# 13  fgsfa sf SA sa\" f sa\" S sa\" ");
		AssertArgParsingAndFormatting("\"\"\"\"\"\"\"");
		AssertArgParsingAndFormatting("\"\\\\ file \"\"  \"\"\" ad \"");
		AssertArgParsingAndFormatting(" add\"\"ad\"aad ");
		AssertArgParsingAndFormatting("abc\"\"\" s \"   ggg");
		AssertArgParsingAndFormatting("1 2 3 4 5 6 7 8 \"9    10 \"\"\" \"1");
		AssertArgParsingAndFormatting("file=\"a URL with spaces\" title=\"A title with spaces and \"\"quotes\"\"\"");
		
	}
	
	private void AssertArgParsingAndFormatting(String str) {
		
		String[] args1 = InteractiveWidget.parseArgs(str);
		
		String formatted = InteractiveWidget.formatArgs(args1);
		
		String[] args2 = InteractiveWidget.parseArgs(formatted);
		
		if (args1 == null || args2 == null) assertEquals(args1, args2);
		else {
			assertEquals(args1.length, args2.length);
			for (int i = 0; i < args1.length; i++) {
				assertEquals(args1[i], args2[i]);
			}
		}
		
	}
		
}
