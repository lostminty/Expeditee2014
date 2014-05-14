package org.apollo.util;

/**
 * Need mutable primitives? e.g. want call-by-reference in java? 
 * 
 * Privades mutable primitive classes.
 * 
 * @author Brook Novak
 *
 */
public class Mutable {
	private static Mutable instance = new Mutable();
	private Mutable() {}
	
	public class Integer {
		public Integer(int val) {
			this.value = val;
		}
		public int value;
		
		public String toString() {
			return java.lang.Integer.toString(value);
		}
	}
	
	
	public class Long {
		public Long(long val) {
			this.value = val;
		}
		public long value;
		
		public String toString() {
			return java.lang.Long.toString(value);
		}
	}
	
	public static Integer createMutableInteger(int val) {
		return instance.new Integer(val);
	}
	
	public static Long createMutableLong(long val) {
		return instance.new Long(val);
	}
}
