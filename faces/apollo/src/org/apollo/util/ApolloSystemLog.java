package org.apollo.util;

public class ApolloSystemLog {
	
	public static void println(Object o) {
		println(o.toString());
	}
	
	public static void print(Object o) {
		print(o.toString());
	}
	
	public static void println(String message) {
		print("Apollo: " + message + "\n");
	}
	
	public static void print(String message) {
		System.out.print(message);
	}

	public static void printError(String errorMessage) {
		System.err.print("*Apollo-Error*: " + errorMessage);
	}
	
	public static void printException(Exception e) {
		e.printStackTrace();
	}
	
	public static void printException(String errorMessage, Exception e) {
		System.err.println("*Apollo-Error*: " + errorMessage + ". Exception stacktrace to follow:");
		e.printStackTrace();
	}
}
