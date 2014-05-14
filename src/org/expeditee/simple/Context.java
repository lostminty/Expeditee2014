package org.expeditee.simple;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.expeditee.actions.Simple;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.MessageBay;
import org.expeditee.items.Text;

public class Context {
	private Primitives primitives_ = new Primitives();

	private Pointers pointers_ = new Pointers();

	public Primitives getPrimitives() {
		return primitives_;
	}

	public Pointers getPointers() {
		return pointers_;
	}

	public Context() {
	}

	public int size() {
		return primitives_.size() + pointers_.size();
	}

	public void clear() {
		primitives_.clear();
		pointers_.clear();
	}

	public void display() {
		if (size() == 0) {
			MessageBay.errorMessage("No variables exist!");
			return;
		}

		primitives_.display();
		pointers_.display();
	}

	/**
	 * 
	 * @param var1
	 * @param var2
	 * @return
	 * @throws Exception
	 *             if the variables could not be converted to the same type.
	 */
	public boolean equalValues(String var1, String var2) throws Exception {
		if (Primitives.isPrimitive(var1) && Primitives.isPrimitive(var2)) {
			return primitives_.equalValues(var1, var2);
		} else if (Pointers.isPointer(var1) && Pointers.isPointer(var2)) {
			throw new UnsupportedOperationException(
					"Pointer comparison not yet implemented");
		}
		return false;
	}

	public void readFrame(String frameNameVar, String frameVar,
			String successVar) throws Exception {
		// Get the values to be set
		String frameName = primitives_.getStringValue(frameNameVar);
		Frame currentFrame = FrameIO.LoadFrame(frameName);

		pointers_.setObject(frameVar, currentFrame);

		if (successVar != null) {
			Boolean success = currentFrame != null;
			primitives_.setValue(successVar, new SBoolean(success));
		}
	}

	public void createFrame(String framesetNameVar, String frameVar,
			String successVar) throws Exception {
		// Get the values to be set
		String framesetName = primitives_.getStringValue(framesetNameVar);
		Frame currentFrame = FrameIO.CreateFrame(framesetName, "", null);
		pointers_.setObject(frameVar, currentFrame);

		if (successVar != null) {
			Boolean success = currentFrame != null;
			primitives_.setValue(successVar, new SBoolean(success));
		}
	}

	public void closeFrame(String frameVar, String successVar) throws Exception {
		// Get the values to be set
		Frame frame = (Frame) pointers_.getVariable(frameVar).getValue();
		frame.change();
		String contents = FrameIO.SaveFrame(frame, false);

		if (successVar != null) {
			Boolean success = contents != null;
			primitives_.setValue(successVar, new SBoolean(success));
		}
	}

	public void closeWriteFile(String fileVar) throws Exception {
		BufferedWriter bw = (BufferedWriter) pointers_.getVariable(fileVar)
				.getValue();
		bw.close();
	}

	public void closeReadFile(String fileVar) throws Exception {
		BufferedReader br = (BufferedReader) pointers_.getVariable(fileVar)
				.getValue();
		br.close();
	}

	public void writeFile(String fileVar, String text) throws Exception {
		BufferedWriter bw = (BufferedWriter) pointers_.getVariable(fileVar)
				.getValue();
		bw.write(text);
	}

	public void openWriteFile(String fileNameVar, String fileVar)
			throws Exception {
		openWriteFile(fileNameVar, fileVar, null);
	}

	public void openWriteFile(String fileNameVar, String fileVar,
			String successVar) throws Exception {
		// Get the values to be set
		File filePath = new File(primitives_.getStringValue(fileNameVar));
		
//		 Open an Output Stream Writer to set encoding
		OutputStream fout = new FileOutputStream(filePath);
		Writer out = new OutputStreamWriter(fout, "UTF-8");
		BufferedWriter currentFile = new BufferedWriter(
				out);

		pointers_.setObject(fileVar, currentFile);

		if (successVar != null) {
			Boolean success = currentFile != null;
			primitives_.setValue(successVar, new SBoolean(success));
		}
	}

	public void readLineFile(String fileVar, String textVar) throws Exception {
		readLineFile(fileVar, fileVar, null);
	}

	public void readLineFile(String fileVar, String textVar, String successVar)
			throws Exception {

		BufferedReader br = (BufferedReader) pointers_.getVariable(fileVar)
				.getValue();

		String text = br.readLine();
		boolean success = text != null;
		primitives_.setValue(textVar, text);

		if (successVar != null) {
			primitives_.setValue(successVar, new SBoolean(success));
		}
	}

	public void readItemFile(String fileVar, String itemVar) throws Exception {
		readItemFile(fileVar, fileVar, null);
	}

	public void readItemFile(String fileVar, String itemVar, String successVar)
			throws Exception {

		BufferedReader br = (BufferedReader) pointers_.getVariable(fileVar)
				.getValue();
		boolean success = true;
		try {
			Text item = (Text) pointers_.getVariable(itemVar).getValue();
			String nextLine = br.readLine();
			String text = nextLine;
			success = (text != null);
			while ((nextLine = br.readLine()) != null)
				text += '\n' + nextLine;
			item.setText(text);
		} catch (Exception e) {
			success = false;
		}

		if (successVar != null) {
			primitives_.setValue(successVar, new SBoolean(success));
		}
	}

	public void openReadFile(String fileNameVar, String fileVar)
			throws Exception {
		openReadFile(fileNameVar, fileVar, null);
	}

	public void openReadFile(String fileNameVar, String fileVar,
			String successVar) throws Exception {
		// Get the values to be set
		File filePath = new File(primitives_.getStringValue(fileNameVar));
		Boolean success = true;
		BufferedReader currentFile = null;
		try {
			currentFile = new BufferedReader(new FileReader(filePath));
		} catch (Exception e) {
			success = false;
		}
		pointers_.setObject(fileVar, currentFile);

		if (successVar != null) {
			primitives_.setValue(successVar, new SBoolean(success));
		}
	}

	public boolean isNull(String varName) {
		try {
			Object o = null;
			if (Primitives.isPrimitive(varName)) {
				SPrimitive sp = getPrimitives().getVariable(varName);
				if(sp == null)
					o = sp;
				else
					o = sp.getValue();
			} else {
				o = getPointers().getVariable(varName);
			}
			return o == null;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean isDefined(String varName) {
		try {
			if (Primitives.isPrimitive(varName)) {
				getPrimitives().getVariable(varName);
			} else {
				getPointers().getVariable(varName);
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public void createFrameset(String framesetNameVar, String successVar)
			throws IncorrectTypeException {
		boolean success = true;
		String framesetName = "frameset";
		try {
			// Get the values to be set
			framesetName = primitives_.getStringValue(framesetNameVar);
			Frame firstFrame = FrameIO.CreateNewFrameset(framesetName);
			success = firstFrame != null;
		} catch (Exception e) {
			success = false;
		}
		if (!success && Simple.isVerbose())
			MessageBay.warningMessage("Error creating " + framesetName);
		if (successVar != null) {
			primitives_.setValue(successVar, new SBoolean(success));
		}

	}

	public boolean searchItem(Text itemToSearch, String pattern,
			String itemVar, String startVar, String endVar,
			String replacementString) throws Exception {
		String searchStr = itemToSearch.getText();
		String[] result = searchStr.split(pattern, 2);
		boolean bFound = result.length > 1;
		if (bFound) {
			if (itemVar != null)
				getPointers().setObject(itemVar, itemToSearch);
			if (startVar != null) {
				getPrimitives().setValue(startVar,
						new SInteger(result[0].length() + 1));
				assert (endVar != null);
				getPrimitives().setValue(endVar,
						new SInteger(searchStr.length() - result[1].length()));
			}
			// If it is a find and replace... then replace with the replacement
			// string
			if (replacementString != null) {
				itemToSearch.setText(searchStr.replaceAll(pattern,
						replacementString));
			}
		}
		return bFound;
	}
}