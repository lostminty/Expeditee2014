package org.expeditee.actions;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import org.expeditee.agents.WriteTree;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.MessageBay;
import org.expeditee.io.StreamGobbler;


public class IDE {
	protected static int CompileClassReturnStatus() {
		
		int exitVal = 0; // default's to status OK
		
		Frame source = DisplayIO.getCurrentFrame();
		String title = source.getTitleItem().getText();
		String[] tokens = title.split("\\s+");
		String className = tokens[tokens.length - 1];
		String fileName = "expeditee/src/" + className.replaceAll("\\.", "/") + ".java";
		WriteTree wt = new WriteTree("Java", fileName);
		if (wt.initialise(source, null)) {
			wt.run();
			try {
				String class_output_dirname = FrameIO.EXPORTS_DIR + "expeditee/bin";
				File class_output_dir = new File(class_output_dirname);
				
				if (!class_output_dir.isDirectory()) {
					class_output_dir.mkdirs();
				}
				
				String javac_cmd_args[] = new String[] { "javac", "-d", class_output_dirname, FrameIO.EXPORTS_DIR + fileName };
				
				Process p = Runtime.getRuntime().exec(javac_cmd_args,null);
				
				// monitor error output
			    StreamGobbler errorGobbler = new StreamGobbler("javac errorStream", p.getErrorStream(), StreamGobbler.MessageBayType.error);            
			    // monitor standard output
			    StreamGobbler outputGobbler = new StreamGobbler("javac outputStream", p.getInputStream(), StreamGobbler.MessageBayType.display);
			    
			    errorGobbler.start();
			    outputGobbler.start();
			    
				exitVal = p.waitFor(); // need to wait to ensure the latest generated 'class' file is the one run in RunClass()
				
		
				if (exitVal != 0) {
					System.err.println("ExitValue for javac compile: " + exitVal);
				}
				else {
					MessageBay.displayMessage("Compiled " + fileName, Color.darkGray);
				}
			} catch (Exception e) {
				e.printStackTrace();
				MessageBay.errorMessage("Could not compile class!");
				MessageBay.errorMessage("Is javac on your PATH environment variable?!");
				
			}
		} else {
			MessageBay.errorMessage("Could not initialise agent!");
		}
		
		return exitVal;
	}

	public static void CompileClass() {
		CompileClassReturnStatus();
	}
	
	public static String getClassName(Frame source) {
		return source.getTitleItem().getText().trim();
		//String title = source.getTitle().getTextNoList();
		//String[] tokens = title.split(" ");
		//return tokens[tokens.length - 1];
	}

	
	
	public static void RunClass() {
		Frame source = DisplayIO.getCurrentFrame();
		String className = getClassName(source);
		try {
			String class_dirname = FrameIO.EXPORTS_DIR + "expeditee/bin";
			String java_cmd_args[] = new String[] { "java", "-cp", class_dirname, className };

			Process p = Runtime.getRuntime().exec(java_cmd_args,null);

			// monitor error output
			StreamGobbler errorGobbler = new StreamGobbler("java errorStream",p.getErrorStream(), StreamGobbler.MessageBayType.error);            
			// monitor standard output
			StreamGobbler outputGobbler = new StreamGobbler("java outputStream",p.getInputStream(), StreamGobbler.MessageBayType.display);

			// Run the two 'gobbling' monitor threads in parallel
			errorGobbler.start();
			outputGobbler.start();

			int exitVal = p.waitFor();

			if (exitVal !=0) {
				System.out.println("ExitValue: " + exitVal);
			}

		}
		catch (Exception e) {
			MessageBay.errorMessage("Could not run class!");
		}
	}

	
	public static void CompileAndRunClass() {
		int compileExitVal = CompileClassReturnStatus();
		if (compileExitVal==0) {
			RunClass();
		}
	}
}
