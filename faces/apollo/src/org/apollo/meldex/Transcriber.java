package org.apollo.meldex;

import java.io.IOException;
import java.util.ArrayList;

@SuppressWarnings("unchecked") // code in java 1.4
public class Transcriber
{
    // The values of these constants are taken directly from Rodger's code
    final double RMS_WINDOW = 10.0;
    final double RMS_STEP = (RMS_WINDOW / 2);

    // The sample rate (frequency) of the loaded sample
    int sampleRate;

    // Create instances of our classes
    PitchTracker pitchTracker = new PitchTracker();
    NoteRounder noteRounder = new NoteRounder();


    /**
     * 
     * @param sample
     * 
     * @return
     * 		The wave sample converted into a RogTrack structure if successful.
     * 		Null if unsuccessful.
     * 
     * @throws IOException
     * 		If conversion to standardized form fails
     */
    public RogTrack transcribeSample(WavSample sample) throws IOException
    {
	// This shouldn't have to be fixed...
	sampleRate = 22050;
    //	sampleRate = (int)SampledAudioManager.PLAYBACK_SAMPLE_RATE;

	// These are values that should end up being parameters
	int tempoBPM = 120;
	int minRestLength = 4, minNoteLength = 2;
	boolean addRestLeftovers = true;

	// Check that the sample is valid
	if (sample == null || sample.getFormat() == null || sample.getRawAudio() == null) {
	    return null;
	}

	// Get the standardised unsigned 8-bit mono data
	//byte[] stdData = sample.getStandardisedData(true); // THis is bad of memory - was getting lots heap errors
//	AudioFormat standardizedFormat = new AudioFormat(
//			//sample.getFormat().getSampleRate(),
//			sampleRate,
//			//sample.getFormat().getSampleRate(),
//			8, // 8-bit
//			1, // mono
//			false, // unsigned
//			true); // Big endian 
//	
//	byte[] stdData = AudioIO.convertAudioBytes(
//			sample.getRawAudio(), 
//			sample.getFormat(), 
//			standardizedFormat);
	
	byte[] stdData = MeldexConversion.toStandardizedFormat(
			sample.getRawAudio(), sample.getFormat(), sampleRate);
	
	if (stdData == null) {
	    return null;
	}

	// Create a new RogTrack to store the result of the transcription
	RogTrack track = new RogTrack();

	// We don't know the time signature
	track.addTimeSignature(0, 0);

	// Calculate the Root Mean Squared data for the loaded sample
	float[] rmsData = calculateRMS(stdData, stdData.length);

	// Check that we have some data to process
	if (rmsData.length <= 0) {
	    return null;
	}

	
	// int rmsStep = 111;  // !! PURE-ROG!!
	int rmsStep = (sampleRate / 200);
	//int rmsStep = (sampleRate / 1000);
	

	// Calculate the number of RMS steps per sixteenth note
	int rmsStepsPer16th = (int) ((60F / (float) (tempoBPM * 4F)) / (RMS_STEP / 1000F));

	// Calculate the sum of the squared RMS data
	double sumRMSDataSquared = 0;
	for (int rmsPos = 0; rmsPos < rmsData.length; rmsPos++) {
	    sumRMSDataSquared += (rmsData[rmsPos] * rmsData[rmsPos]);
	}

	// Calculate the upper and lower thresholds (asymmetrical??)
	double threshold = Math.sqrt(sumRMSDataSquared / rmsData.length);
	float upperThreshold = (float) (threshold * 0.55);
	float lowerThreshold = (float) (threshold * 0.35);

	// Now we find all of the notes in the sample...
	int lastCents = -1, lastStart = 0, lastEnd = 0;
	int restLen, noteLen;
	for (int rmsPos = 0; rmsPos < rmsData.length; rmsPos++) {

	    // Have we found the start of a note??
	    if (rmsData[rmsPos] >= upperThreshold) {

		// Yes, so find the end of the note
		int noteStart = rmsPos;
		while (rmsPos < rmsData.length) {
		    if (rmsData[rmsPos] <= lowerThreshold) {
			break;
		    }
		    rmsPos++;
		}

		int noteEnd = rmsPos;
		// If it is longer than 1/3 of the minimum note length, process it
		if ((noteEnd - noteStart) >= ((minNoteLength * rmsStepsPer16th) / 3)) {

		    // Create a new array for the interesting data
		    int noteLength = (noteEnd - noteStart) * rmsStep;
		    byte[] noteData = new byte[noteLength];
		    for (int notePos = 0; notePos < noteLength; notePos++) {
			noteData[notePos] = stdData[(noteStart * rmsStep) + notePos];
		    }

		    // Process the note
		    int cents = processNote(noteData, noteLength);
		    if (cents > 1000) {

			// If it is the first note then initialise the NoteRounder
			if (lastCents < 0) {
			    int noteRound = (noteRounder.roundCents(cents) - cents); 
			    noteRounder.setInitialValue(noteRound);
			}
			// Otherwise we calculate the note and rest values
			else {
			    int finalNote = noteRounder.roundNote(lastCents, true);

			    // Calculate the rest length
			    int rmsRestLeft = 0;
			    if (rmsStepsPer16th <= 0) {
				restLen = 0;
			    }
			    else {
				int rmsRestLen = (noteStart - lastEnd);
				int mult = (minRestLength * rmsStepsPer16th);
				int roundRest = noteRounder.round(rmsRestLen, mult);
				restLen = (roundRest / rmsStepsPer16th);
				if (addRestLeftovers == true) {
				    rmsRestLeft = rmsRestLen - (restLen * rmsStepsPer16th);
				}
			    }

			    // Calculate the note length
			    if (rmsStepsPer16th <= 0) {
				noteLen = 0;
			    }
			    else {
				int rmsNoteLen = (lastEnd - lastStart);
				int mult = (minNoteLength * rmsStepsPer16th);
				if (addRestLeftovers == true && rmsRestLeft > 0) {
				    rmsNoteLen += rmsRestLeft;
				}
				int roundNote = noteRounder.round(rmsNoteLen, mult);
				noteLen = (roundNote / rmsStepsPer16th);
			    }

			    // Add the notes and rests to the track
			    track.addNote(noteLen, (finalNote / 100), 0);
			    track.addRest(restLen);
			}

			lastCents = cents;
			lastStart = noteStart;
			lastEnd = noteEnd;
		    }
		}
	    }
	}

	// Deal with the last note (if there is one)
	if (lastCents > 0) {
	    int finalNote = noteRounder.roundNote(lastCents, true);

	    // Calculate the note length
	    if (rmsStepsPer16th <= 0) {
		noteLen = 0;
	    }
	    else {
		int rmsNoteLen = (lastEnd - lastStart);
		int mult = (minNoteLength * rmsStepsPer16th);
		int roundNote = noteRounder.round(rmsNoteLen, mult);
		noteLen = (roundNote / rmsStepsPer16th);

		// Add the last note to the track
		track.addNote(noteLen, (finalNote / 100), 0);
	    }
	}

	// Calculate the key signature
	int key = track.calculateBestKey();

	// Add a key signature event to the track
	track.addKeySignature(key, 0);

	// Set the note names for the new key signature
	track.setNoteNames(key);

	// Return the Track with the transcribed data
	return track;
    }


    private float[] calculateRMS(byte[] stdData, int stdLength)
    {
	int accMS = 0;
	int accMSPrev = 0;
	int frameStart = 0;

	// int rmsStep = 111;  // !! PURE ROG!!
	int rmsStep = (22050 / 200);  // !! SEMI-PURE ROG !!

	// Allocate memory for the RMS data values
	int rmsLength = (int) (stdLength / (float) rmsStep);
	float[] rmsData = new float[rmsLength];

	for (int stdPos = 0, rmsPos = 0; stdPos < stdLength; stdPos++) {
	    // Calculate the accumulated Mean Squared value
	    int value = (stdData[stdPos] & 255) - 127;   // !! SHOULD 127 BE 128?? !!
	    accMS += (value * value);

	    // If we have finished a frame calculate a value for the output
	    if ((stdPos - frameStart) >= rmsStep) {
		double result = Math.sqrt((double)(accMS+accMSPrev) / (double)(rmsStep*2));
		frameStart = stdPos;
		accMSPrev = accMS;
		accMS = 0;
		rmsData[rmsPos] = (float) result;
		rmsPos++;
	    }
	}

	return rmsData;
    }


    private int processNote(byte[] data, int length)
    {
	// Pitch track the note
	ArrayList pitchData = pitchTracker.process(data, length, sampleRate);

	// Average the pitch data
	int pitchLength = averagePitchData(pitchData);

	// Calculate the histogram of the pitch data
	return calculateHistogram(pitchData, pitchLength);
    }


    private int averagePitchData(ArrayList pitchData)
    {
	// Loop through the pitch values...
	int i = 0, k = 0;
	while (i < (pitchData.size() - 1)) {
	    int startPos = ((PitchValue) pitchData.get(i)).position;
	    double period = ((PitchValue) pitchData.get(i)).period;
	    double averagePeriod = period;
	    double runningPeriod = ((PitchValue) pitchData.get(i+1)).position - startPos;
	    double numPeriods = runningPeriod / period;

	    // Start from the next pitch
	    int j = 1;
	    while ((i+j) < (pitchData.size() - 1)) {
		// Get the next pitch estimate
		period = ((PitchValue) pitchData.get(i+j)).period;
		int position = ((PitchValue) pitchData.get(i+j)).position;

		// Make sure that this period is covered by the average so far
		if (period > (averagePeriod * 1.1) || period < (averagePeriod * 0.909)) {
		    break;
		}

		// Stop if we have covered more than 20 msec
		int nextPos = ((PitchValue) pitchData.get(i+j+1)).position;
		// if ((nextPos - startPos) >= 445) {  // !! PURE-ROG !!
		if ((nextPos - startPos) >= (sampleRate * 0.02)) {
		    break;
		}

		// Add to this period
		runningPeriod += (nextPos - position);
		numPeriods += ((nextPos - position) / period);
		averagePeriod = runningPeriod / numPeriods;
		j++;
	    }

	    // Write over the original pitch value with the averaged pitch value
	    pitchData.set(k, new PitchValue(averagePeriod, startPos));
	    k++;

	    // Increment i
	    i += j;
	}

	// Return the number of averaged pitch values
	return k;
    }


    private int calculateHistogram(ArrayList data, int length)
    {
	// This probably shouldn't be a constant
	final int histLength = 960;

	// Allocate memory for the histogram data and initialise it to zero
	int[] histData = new int[histLength];
	for (int i = 0; i < histLength; i++) {
	    histData[i] = 0;
	}

	// Calculate the histogram data
	for (int i = 0; i < (length - 1); i++) {
	    double period = ((PitchValue) data.get(i)).period;
	    double position = ((PitchValue) data.get(i)).position;
	    double periodLength = ((PitchValue) data.get(i+1)).position - position;

	    // We only need to do this if the pitch does not equal zero
	    if (period > 0) {
		// Do some weird stuff...
		double logValue = (Math.log(period / sampleRate)) / (Math.log(10));
		double cents = -(logValue / 0.000250858) - 3637.622659;
		int histPos = (int) (Math.floor((cents - 3600.0) / 5.0));
		if (histPos >= 0 && histPos < histLength) {
		    histData[histPos] += periodLength;
		}
	    }
	}

	// Select the cents value using the histogram data
	return selectCents(histData, histLength);
    }


    private int selectCents(int[] histData, int histLength)
    {
	// Find the maximum average histogram value
	int maxHistValue = 0, maxHistPos = 0;
	for (int i = 0; i < (histLength - 20); i++) {
	    // Sum the next 20 histogram values (1 semitone)
	    int histValue = 0;
	    for (int j = 0; j < 20; j++) {
		histValue += histData[i+j];
	    }

	    // Maintain the maximum histogram value
	    if (histValue > maxHistValue) {
		maxHistValue = histValue;
		maxHistPos = i;
	    }
	}

	// Find the average cents value of the maximum histogram
	int sumCents = 0;
	for (int j = 0; j < 20; j++) {
	    sumCents += (histData[maxHistPos+j] * (((maxHistPos+j) * 5) + 3602));
	}

	// Calculate sum of (cents * time) divided by total time
	if (maxHistValue > 0) {
	    return (sumCents / maxHistValue);
	}
	else {
	    return 0;
	}
    }
}
