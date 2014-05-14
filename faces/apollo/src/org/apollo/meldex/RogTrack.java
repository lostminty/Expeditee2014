package org.apollo.meldex;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import javax.swing.JOptionPane;

@SuppressWarnings("unchecked") // code in java 1.4
public class RogTrack
{
    // The number of semitones from the key centre
    final int nameOffset[] = { 0, 0, 1, 2, 2, 3, 3, 4, 4, 5, 6, 6 };

    // Data about the track (loaded from the file)
    private String databaseName = "";
    private String songName = "";
    private int wholeNoteDivisions = 16;

    // Data representing the track events
    ArrayList trackData = new ArrayList();


    // ---------------------------------------
    //   WavToRog specific functions
    // ---------------------------------------
    public RogTrack()
    {
    }


    public void addTimeSignature(int numer, int denom)
    {
	trackData.add(new RogTrackEvent(RogTrackEvent.TIME_SIG_EVENT, numer, denom));
    }


    public void addNote(int noteLength, int notePitch, int key)
    {
	int noteName = getNoteName(key, notePitch);
	trackData.add(new RogTrackEvent(noteLength, noteName, notePitch));
    }


    public void addRest(int restLength)
    {
	if (restLength > 0) {
	    trackData.add(new RogTrackEvent(restLength, 0, 0));
	}
    }


    public void addKeySignature(int numSharpsFlats, int isMinor)
    {
	trackData.add(0, new RogTrackEvent(RogTrackEvent.KEY_SIG_EVENT, isMinor, numSharpsFlats));
    }


    public int calculateBestKey()
    {
	int accidental;
	int minAccidental = trackData.size();
	int minKey = 0;

	for (int numSharpFlat = 0; numSharpFlat < 8; numSharpFlat++) {
	    accidental = checkAccidental(numSharpFlat);
	    if (accidental < minAccidental) {
		minAccidental = accidental;
		minKey = numSharpFlat;
	    }

	    accidental = checkAccidental(-numSharpFlat);
	    if (accidental < minAccidental) {
		minAccidental = accidental;
		minKey = -numSharpFlat;
	    }
	}

	return minKey;
    }


    public int checkAccidental(int key)
    {
	int sharpsFlats[] = { 0, 0, 0, 0, 0, 0, 0 };
	int nameValue[] = { 0, 2, 4, 5, 7, 9, 11 };

	// Calculate the sharps and flats for this key
	if (key < 0) {
	    for (int flatNum = 1; flatNum <= (-key); flatNum++) {
		sharpsFlats[((8 + flatNum) * 3) % 7]--;
	    }
	}
	else {
	    for (int sharpNum = 1; sharpNum <= key; sharpNum++) {
		sharpsFlats[((sharpNum * 4) + 6) % 7]++;
	    }
	}

	// Iterate through the track
	int sumAccidental = 0;
	for (int i = 0; i < trackData.size(); i++) {
	    RogTrackEvent te = (RogTrackEvent) trackData.get(i);

	    // Have we found a note??
	    if (te.type <= RogTrackEvent.LAST_NOTE_EVENT) {
		int semitone = te.param2;

		// Calculate the accidental for this key
		int noteName = getNoteName(key, semitone);
		int numSharpsFlats = (semitone % 12) - nameValue[noteName];

		if (numSharpsFlats < -6) {
		    numSharpsFlats += 12;
		}
		if (numSharpsFlats > 6) {
		    numSharpsFlats -= 12;
		}

		if (numSharpsFlats != sharpsFlats[noteName]) {
		    sumAccidental++;
		}
	    }
	}

	return sumAccidental;
    }


    public void setNoteNames(int numSharpsFlats)
    {
	for (int i = 0; i < trackData.size(); i++) {
	    RogTrackEvent te = (RogTrackEvent) trackData.get(i);
	    if (te.type <= RogTrackEvent.LAST_NOTE_EVENT && te.param2 > 0) {
		te.param1 = getNoteName(numSharpsFlats, te.param2);
	    }
	}
    }


    public int getNoteName(int key, int notePitch)
    {
	// Calculate the name of the key
	int keyName = (((7 - key) * 3) % 7);

	// Calculate the semitone start position
	int keySemitoneStart = (((12 - key) * 5) % 12);

	// Calculate the note name
	int noteName = (keyName + nameOffset[(notePitch + 12 - keySemitoneStart) % 12]) % 7;
	return noteName;
    }


    public String toString()
    {
	String str = "song \"" + databaseName + "\" \"" + songName + "\" " + wholeNoteDivisions + "\n";

	// Display each of the TrackEvents in turn
	for (int i = 0; i < trackData.size(); i++) {
	    RogTrackEvent te = (RogTrackEvent) trackData.get(i);

	    // Is this a note??
	    if (te.type <= RogTrackEvent.LAST_NOTE_EVENT) {
		char noteName;
		if (te.param1 < 5) {
		    noteName = (char) (te.param1 + 'C');
		}
		else {
		    noteName = (char) (te.param1 - 5 + 'A');
		}
		str = str + te.type + " " + noteName + " " + te.param2 + "\n";
	    }

	    // Is this a time signature??
	    else if (te.type == RogTrackEvent.TIME_SIG_EVENT) {
		str = str + "timesig " + te.param1 + " " + te.param2 + "\n";
	    }

	    // Is this a key signature??
	    else if (te.type == RogTrackEvent.KEY_SIG_EVENT) {
		str = str + "keysig " + te.param1 + " " + te.param2 + "\n";
	    }

	    // Is this a fermata event??
	    else if (te.type == RogTrackEvent.FERMATA_EVENT) {
		str = str + "fermata\n";
	    }

	    // Is this a tempo event??
	    else if (te.type == RogTrackEvent.TEMPO_EVENT) {
		str = str + "tempo " + te.param1 + "\n";
	    }
	}

	// Return the finished string
	return str;
    }

    public void fromString(String rog_text)
    {
	int rog_text_len = rog_text.length();
	byte[] rog_bytes = rog_text.getBytes();

	parseFile(rog_bytes, rog_text_len);
    }


    /**
     * 
     * @return
     * 		The converted melody. Null if was unable to convert
     */
    public Melody toMelody()
    {
	int track_len = trackData.size();

	ArrayList filtered_td = new ArrayList();

	for (int i = 0; i < track_len; i++) {
	    RogTrackEvent te = (RogTrackEvent) trackData.get(i);

	    // Is this a note??
	    if (te.type <= RogTrackEvent.LAST_NOTE_EVENT) {
		filtered_td.add(te);
	    }
	}

	int filtered_track_len = filtered_td.size();

	if (filtered_track_len<1) {
	    //System.err.println("Insufficient track data for form relative pitch melody");
	    return null;
	}

	ArrayList me_list = new ArrayList();

	for (int i = 0; i < filtered_track_len-1; i++) {
	    RogTrackEvent te = (RogTrackEvent) filtered_td.get(i);
	    RogTrackEvent next_te = (RogTrackEvent) filtered_td.get(i+1);

	    int duration = te.type;

	    if (next_te.param2 == 0) { 
		// is a rest
		MelodyEvent me = new MelodyEvent(MelodyEvent.REST_EVENT,duration);
		me_list.add(me);

		// skip ahead one further position
		i++;
		continue;
	    }

	    int rel_pitch = next_te.param2 - te.param2;

	    MelodyEvent me = new MelodyEvent(rel_pitch,duration);
	    me_list.add(me);
	}

	int me_list_len = me_list.size();

	MelodyEvent[] me_array = new MelodyEvent[me_list_len];
	for (int i=0; i<me_list_len; i++) {
	    me_array[i] = (MelodyEvent)me_list.get(i);
	}

	Melody me = new Melody(Melody.RELATIVE_PITCH,me_array);

	return me;
    }


    // ---------------------------------------
    //   RogToGif specific functions
    // ---------------------------------------
	
    // ----------------------------------------------------------------------------------
    //  Method  : loadFromFile
    //  Returns : false - if the track could not be successfully loaded
    //            true  - if the track was successfully loaded
    // ----------------------------------------------------------------------------------
    public boolean loadFromFile(File rogFile)
    {
	try {
	    // Open the file with a FileInputStream, ready for parsing
	    FileInputStream fileIn = new FileInputStream(rogFile);

	    // Get the number of bytes in the file
	    int rogLength = fileIn.available();

	    // Create a buffer to store the file, and read the file into it
	    byte[] rogData = new byte[rogLength];
	    fileIn.read(rogData, 0, rogLength);

	    // Parse the .rog file
	    parseFile(rogData, rogLength);
	}
	catch (Exception ex) {
	    JOptionPane.showMessageDialog(null, "Exception occurred reading file.\n\n" + ex);
	    return false;
	}

	// File loaded successfully
	return true;
    }


    private void parseFile(byte[] rogData, int rogLength)
    {
	// Break the file up into lines and parse each one seperately
	int lineStart = 0;
	for (int pos = 0; pos < rogLength; pos++) {
	    if (rogData[pos] == '\n') {
		parseLine(new String(rogData, lineStart, (pos - lineStart)));
		lineStart = pos + 1;
	    }
	}
	// If the last line is not newline-terminated then we have to parse it now
	if (lineStart != rogLength) {
	    parseLine(new String(rogData, lineStart, (rogLength - lineStart)));
	}
    }


    private void parseLine(String line)
    {
	String sent = null;
	String word = nextWord(line, 0);
	int parsed = (word.length() + 1);

	if (word.equals("") || word.equals("metadata")) {
	    // do nothing => ignore it!
	}
	else if (word.equals("song")) {
	    // Parse the first parameter - databaseName
	    sent = nextSentence(line, parsed + 1);
	    parsed += (sent.length() + 3);
	    databaseName = sent;

	    // Parse the second parameter - songName
	    sent = nextSentence(line, parsed + 1);
	    parsed += (sent.length() + 3);
	    songName = sent;

	    // Parse the third parameter - wholeDivision
	    word = nextWord(line, parsed);
	    parsed += (word.length() + 1);
	    wholeNoteDivisions = (int) Double.parseDouble(word);
	}

	else if (word.equals("fermata")) {
	    // Add a fermata event to the track (no parameters)
	    trackData.add(new RogTrackEvent(RogTrackEvent.FERMATA_EVENT, 0, 0));
	}

	else if (word.equals("tempo")) {
	    // Parse the first parameter - tempoBPM
	    word = nextWord(line, parsed);
	    parsed += (word.length() + 1);
	    int tempoBPM = (int) Double.parseDouble(word);

	    // Add a tempo event to the track
	    trackData.add(new RogTrackEvent(RogTrackEvent.TEMPO_EVENT, tempoBPM, 0));
	}

	else if (word.equals("keysig")) {
	    // Parse the first parameter - isMinor
	    word = nextWord(line, parsed);
	    parsed += (word.length() + 1);
	    int isMinor = (int) Double.parseDouble(word);

	    // Parse the second parameter - numSharpsFlats
	    word = nextWord(line, parsed);
	    parsed += (word.length() + 1);
	    int numSharpsFlats = (int) Double.parseDouble(word);

	    // Add this key signature event to the track
	    trackData.add(0, new RogTrackEvent(RogTrackEvent.KEY_SIG_EVENT, isMinor, numSharpsFlats));
	}

	else if (word.equals("timesig")) {
	    // Parse the first parameter - numerator
	    word = nextWord(line, parsed);
	    parsed += (word.length() + 1);
	    int numerator = (int) Double.parseDouble(word);

	    // Parse the second parameter - denominator
	    word = nextWord(line, parsed);
	    parsed += (word.length() + 1);
	    int denominator = (int) Double.parseDouble(word);

	    // Add this time signature event to the track
	    trackData.add(new RogTrackEvent(RogTrackEvent.TIME_SIG_EVENT, numerator, denominator));
	}

	else {
	    // This must be a note line, so parse the duration
	    int duration = (int) Double.parseDouble(word);

	    // Parse the first parameter - noteName
	    word = nextWord(line, parsed);
	    parsed += (word.length() + 1);
	    char noteName = word.charAt(0);

	    // Check that the noteName is valid
	    if (noteName < 'A' || noteName > 'G') {
		JOptionPane.showMessageDialog(null, "Parsing error - check that the Rog file is valid.");
		return;
	    }

	    // Convert the noteName character into it's MIDI integer equivalent
	    int noteNum;
	    if (noteName >= 'C') {
		noteNum = noteName - 'C';
	    }
	    else {
		noteNum = noteName - 'A' + 5;
	    }

	    // Parse the second parameter - pitch
	    word = nextWord(line, parsed);
	    parsed += (word.length() + 1);
	    int pitch = (int) Double.parseDouble(word);

	    // Add this note event to the track
	    trackData.add(new RogTrackEvent(duration, noteNum, pitch));
	}
    }


    private String nextSentence(String line, int sentStart)
    {
	for (int pos = sentStart; pos < line.length(); pos++) {
	    // We have found a quote mark, so return the sentence found
	    if (line.charAt(pos) == '"') {
		return (new String(line.substring(sentStart, pos)));
	    }
	}
	// We have reached the end of the string, so return the remainder
	return (new String(line.substring(sentStart, line.length())));
    }


    private String nextWord(String line, int wordStart)
    {
	for (int pos = wordStart; pos < line.length(); pos++) {
	    // We have found a space, so return the word found
	    if (line.charAt(pos) == ' ') {
		return (new String(line.substring(wordStart, pos)));
	    }
	}
	// We have reached the end of the string, so return the remainder
	return (new String(line.substring(wordStart, line.length())));
    }


    public String getDatabaseName()
    {
	return databaseName;
    }


    public String getSongName()
    {
	return songName;
    }


    public int getWholeNoteDivisions()
    {
	return wholeNoteDivisions;
    }


    // ---------------------------------------
    //   SymInput specific functions
    // ---------------------------------------
    public void setWholeNoteDivisions(int wnd)
    {
	wholeNoteDivisions = wnd;
    }


    // ----------------------------------------------------------------------------------
    //  Method  : saveToFile
    //  Returns : false - if the track could not be successfully saved
    //            true  - if the track was successfully saved
    // ----------------------------------------------------------------------------------
    public boolean saveToFile(File outFile)
    {
	// Convert the track to a string and save it to the specified file
	try {
	    FileOutputStream fileOut = new FileOutputStream(outFile);
	    fileOut.write(toString().getBytes());
	    fileOut.close();
	}
	catch (Exception ex) {
	    JOptionPane.showMessageDialog(null, "Exception occurred writing to file.\n\n" + ex);
	    return false;
	}

	// File saved successfully
	return true;
    }
}
