package org.apollo.meldex;


import java.io.File;
import java.util.ArrayList;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.JOptionPane;

@SuppressWarnings("unchecked") // code in java 1.4
public class WavSample
{
    // Data representing the currently loaded file at a high level of abstraction
    private File file = null;
    private AudioFormat format = null;
    AudioInputStream audioData = null;

    // The actual raw audio data (without header). This shouldn't need to be used often.
    byte[] rawData = null;

    // The standardised unsigned 8-bit mono data. This should be used instead of the raw data.
    private byte[] stdData = null;


    public WavSample(byte[] rawData, AudioFormat format)
    {
    	this.rawData = rawData;
    	this.format = format;
    }
    
    public WavSample()
    {
    }


    public WavSample(AudioInputStream data)
    {
	audioData = data;

	buildSampleData();
    }


    // ----------------------------------------------------------------------------------
    //  Method  : buildSampleData
    //  Returns : true  - if the sample data was successfully built
    //            false - if the sample data was not successfully built
    // ----------------------------------------------------------------------------------
    private boolean buildSampleData()
    {
	// Reset the sample to the beginning
	if (reset() == false) {
	    return false;
	}

	// Get the format of the loaded file
	format = audioData.getFormat();

	try {
	    // Get the length of the loaded sample
	    int rawLength = audioData.available();

	    // Load the sample data into an array ready for processing
	    rawData = new byte[rawLength];
	    audioData.read(rawData, 0, rawLength);
	}
	catch (Exception ex) {
	    JOptionPane.showMessageDialog(null, "Exception occurred creating sample.\n\n" + ex);
	    return false;
	}

	// Sample data built successfully
	return true;
    }


    // ----------------------------------------------------------------------------------
    //  Method  : reset
    //  Returns : true  - if the sample was successfully reset
    //            false - if the sample was not successfully reset
    // ----------------------------------------------------------------------------------
    public boolean reset()
    {
	// For some bizarre reason we cannot reset a loaded file, so it is necessary to reload it
	if (file != null) {
	    try {
		audioData = AudioSystem.getAudioInputStream(file);
	    }
	    catch (Exception ex) {
		JOptionPane.showMessageDialog(null, "Exception occurred reloading sample.\n\n" + ex);
		return false;
	    }
	}

	// Otherwise we just reset the stream to the start, ready for playback
	else if (audioData != null) {
	    try {
		audioData.reset();
	    }
	    catch (Exception ex) {
		JOptionPane.showMessageDialog(null, "Exception occurred resetting audio stream.\n\n" + ex);
		return false;
	    }
	}

	// Reset performed successfully
	return true;
    }


    // ----------------------------------------------------------------------------------
    //  Method  : getFormat
    //  Returns : null - if the sample does not have a specified format
    //            else - the AudioFormat of the audio sample
    // ----------------------------------------------------------------------------------
    public AudioFormat getFormat()
    {
	if (format == null) {
	    if (audioData != null) {
		format = audioData.getFormat();
	    }
	}

	return format;
    }


    // ----------------------------------------------------------------------------------
    //  Method  : getBytesPerSecond
    //  Returns : -1   - if the value could not be successfully calculated
    //            else - the number of bytes of audio data per second
    // ----------------------------------------------------------------------------------
    public int getBytesPerSecond()
    {
	if (format == null) {
	    return -1;
	}

	int frameSize = (format.getChannels() * (format.getSampleSizeInBits() / 8));
	return (int) (format.getSampleRate() * frameSize);
    }


    // ----------------------------------------------------------------------------------
    //  Method  : getStandardisedData
    //  Returns : null - if the sample could not be successfully standardised
    //            else - byte[] containing the standardised unsigned 8-bit mono data
    // ----------------------------------------------------------------------------------
    public byte[] getStandardisedData(boolean createIfNotComputed)
    {
	if (stdData == null && createIfNotComputed) {
	    // Create the standardised unsigned 8-bit mono data
	    // stdData = toStandardForm(format, rawData, rawData.length, (int) format.getSampleRate());
	    stdData = toStandardForm(format, rawData, (int) format.getSampleRate());
	}

	return stdData;
    }


    // ----------------------------------------------------------------------------------
    //  Method  : toStandardForm
    //  Returns : null - if the sample could not be successfully standardised
    //            else - byte[] containing the standardised unsigned 8-bit mono data
    // ----------------------------------------------------------------------------------
    // public static byte[] toStandardForm(AudioFormat format, byte[] inData, int amount, int frequency)
    public static byte[] toStandardForm(AudioFormat format, byte[] inData, int frequency)
    {
	// Check for (supposedly) invalid sample formats
	if (format.getSampleSizeInBits() == 8) {
	    if (format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
		JOptionPane.showMessageDialog(null, "Oops! Internal Error: Unexpected audio format (signed 8-bit).");
		return null;
	    }
	}
	if (format.getSampleSizeInBits() == 16) {
	    if (format.getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED)) {
		JOptionPane.showMessageDialog(null, "Oops! Internal Error: Unexpected audio format (unsigned 16-bit).");
		return null;
	    }
	}

	int baseSampleSize = (int) (format.getSampleRate() / frequency);
	int frameSize = (format.getChannels() * (format.getSampleSizeInBits() / 8));
	int sampleSize = (baseSampleSize * frameSize);

	// Create a new list to store the standardised sample values
	ArrayList stdTemp = new ArrayList();

	// Simply pick values from the sample at the frequency specified
	// for (int i = 0; i < amount; i += sampleSize) {
	for (int i = 0; i < inData.length; i += sampleSize) {
	    for (int j = 0; j < frameSize; j++) {
		int val = inData[i+j];
		stdTemp.add(new Integer(val));
	    }
	}

	// Convert the sample to standard unsigned, 8-bit, mono values
	stdTemp = toStandardEndian(format, stdTemp);
	stdTemp = toUnsignedValues(format, stdTemp);
	stdTemp = to8bit(format, stdTemp);
	stdTemp = toMono(format, stdTemp);

	// Allocate memory for the standardised data
	byte[] tmpData = new byte[stdTemp.size()];

	// Convert the ArrayList back to an array of bytes
	for (int i = 0; i < stdTemp.size(); i++) {
	    int val = ((Integer) stdTemp.get(i)).intValue();
	    tmpData[i] = (byte) val;
	}

	return tmpData;
    }


    private static ArrayList toStandardEndian(AudioFormat format, ArrayList inData)
    {
	// Endian details do not apply to 8-bit samples, so no conversion needed
	if (format.getSampleSizeInBits() == 8) {
	    return inData;
	}

	// Create a new list to store the results
	ArrayList outData = new ArrayList();

	// Simply convert each number in the sample to the endian form for the current system
	for (int i = 0; i < inData.size(); i += 2) {

	    int MSB, LSB;
	    if (format.isBigEndian()) {
		MSB = ((Integer) inData.get(i)).intValue();    // First byte is MSB (high order)
		LSB = ((Integer) inData.get(i+1)).intValue();  // Second byte is LSB (low order)
	    }
	    else {
		LSB = ((Integer) inData.get(i)).intValue();    // First byte is LSB (low order)
		MSB = ((Integer) inData.get(i+1)).intValue();  // Second byte is MSB (high order)
	    }

	    int val = (MSB << 8) + (LSB & 255);
	    outData.add(new Integer(val));
	}

	return outData;
    }


    private static ArrayList toUnsignedValues(AudioFormat format, ArrayList inData)
    {
	// PCM_UNSIGNED samples are what we want, so no conversion needed
	if (format.getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED)) {
	    return inData;
	}

	// Create a new list to store the results
	ArrayList outData = new ArrayList();

	// Simply convert each signed number in the sample to an unsigned value
	for (int i = 0; i < inData.size(); i++) {

	    int val = ((Integer) inData.get(i)).intValue();
	    val = val - 32768;
	    if (val < -32768) {
		val += 65536;
	    }

	    outData.add(new Integer(val));
	}

	return outData;
    }


    private static ArrayList to8bit(AudioFormat format, ArrayList inData)
    {
	// 8-bit data is what we want, so no conversion necessary
	if (format.getSampleSizeInBits() == 8) {
	    return inData;
	}

	// Create a new list to store the results
	ArrayList outData = new ArrayList();

	// Simply reduce each value in the sample to an 8-bit value
	for (int i = 0; i < inData.size(); i++) {

	    int val = ((Integer) inData.get(i)).intValue();
	    val = val / 256;

	    outData.add(new Integer(val));
	}

	return outData;
    }


    private static ArrayList toMono(AudioFormat format, ArrayList inData)
    {
	// Mono-channel data is what we want, so no conversion necessary
	if (format.getChannels() == 1) {
	    return inData;
	}

	// Create a new list to store the results
	ArrayList outData = new ArrayList();

	// Simply average all of the channels for each frame
	for (int i = 0; i < inData.size(); i += format.getChannels()) {

	    int sum = 0;
	    for (int j = 0; j < format.getChannels(); j++) {
		int val = ((Integer) inData.get(i+j)).intValue();
		// We need to use the true unsigned value
		if (val < 0) {
		    val = val + 256;
		}
		sum += val;
	    }

	    int val = sum / format.getChannels();
	    // Revert back to signed representation
	    if (val > 127) {
		val = val - 256;
	    }

	    outData.add(new Integer(val));
	}

	return outData;
    }


    // ----------------------------------------------------------------------------------
    //  Method  : loadFromFile
    //  Returns : false - if the track could not be successfully loaded
    //            true  - if the track was successfully loaded
    // ----------------------------------------------------------------------------------
    public boolean loadFromFile(File wavFile)
    {
	file = wavFile;

	return buildSampleData();
    }


    // ----------------------------------------------------------------------------------
    //  Method  : saveToFile
    //  Returns : false - if the sample could not be successfully saved
    //            true  - if the sample was successfully saved
    // ----------------------------------------------------------------------------------
    public boolean saveToFile(File outFile)
    {
	// Reset the sample to the beginning
	if (reset() == false) {
	    return false;
	}

	// Write the audio data to the selected file in the format specified
	try {
	    if (AudioSystem.write(audioData, AudioFileFormat.Type.WAVE, outFile) == -1) {
		JOptionPane.showMessageDialog(null, "Problem occurred writing to file.");
		return false;
	    }
	}
	catch (Exception ex) {
	    JOptionPane.showMessageDialog(null, "Exception occurred saving file.\n\n" + ex);
	    return false;
	}

	// File saved successfully
	return true;
    }
    
    public byte[] getRawAudio() {
    	return rawData;
    }
}
