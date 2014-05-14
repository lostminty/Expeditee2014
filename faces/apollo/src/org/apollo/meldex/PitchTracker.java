package org.apollo.meldex;

import java.util.ArrayList;

@SuppressWarnings("unchecked") // code in java 1.4
public class PitchTracker
{
    // The sample rate (frequency) of the loaded sample
    int sampleRate;

    // The filtered data
    byte[] filterData;
    int filterLength;

    // The pitch data
    ArrayList pitchData;

    // Our lovely Pitch Period Estimators
    PitchEstimator[] ppe = new PitchEstimator[6];

    // Our lovely winner calculator
    WinnerCalculator winnerCalc = new WinnerCalculator();

    int lastMinHeight, lastMaxHeight;
    int lastPeriodStart;


    public ArrayList process(byte[] rawData, int rawLength, int rawSampleRate)
    {
	// Save the sample rate of the loaded sample
	sampleRate = rawSampleRate;

	// Allocate a new pitch data array to store our pitch values
	pitchData = new ArrayList();

	// Reset our 6 pitch period estimators
	for (int i = 0; i < 6; i++) {
	    ppe[i] = new PitchEstimator();
	}

	// Reset our other variables
	lastMinHeight = 0;
	lastMaxHeight = 0;
	lastPeriodStart = 0;

	// Perform a low pass filter on the raw data
	lowPassFilter(rawData, rawLength);

	// Find the peaks that exist in the filtered data
	findPeaks(filterData, filterLength);

	return pitchData;
    }


    public void lowPassFilter(byte[] rawData, int rawLength)
    {
	// I have absolutely no idea where these numbers come from - ask Rodger!!
	int filters[] = { 26968, 16384, 19494, 21549, 22266, 21549, 19494, 16384, 26968 };

	// Allocate memory for the filtered data values
	filterLength = rawLength;
	filterData = new byte[filterLength];

	// Perform a low pass filter on the note data
	for (int rawPos = 0; rawPos < rawLength; rawPos++) {

	    int value = 0;
	    for (int filterNum = 0; filterNum < 9; filterNum++) {

		// Remember to convert note data value to an unsigned value
		if ((rawPos - filterNum) >= 0) {
		    value += ((rawData[rawPos - filterNum] & 255) * filters[filterNum]);
		}
		// We are out of the array bounds, so use a zero value (unsigned 127)
		else {
		    value += (127 * filters[filterNum]);
		}
	    }

	    value = (value + 500888) / 195000;
	    filterData[rawPos] = (byte) value;
	}
    }


    public void findPeaks(byte[] data, int length)
    {
	int maxPos = 0, maxHeight = 0;
	int minPos = 0, minHeight = 0;
	int peakPos = 0, peakHeight = 0;
	boolean trackingMaximum = true;
	boolean lookingAtPeak = false;

	// Find the peaks that exist in the data
	for (int pos = 0; pos < length; pos++) {

	    int value = 0;

	    // Are we currently tracking a maximum??
	    if (trackingMaximum == true) {
		value = (data[pos] & 255) - 127;

		// Have we found our maximum??
		if (value < 0) {
		    maxPos = peakPos;
		    maxHeight = peakHeight;
		    peakPos = pos;
		    peakHeight = 0;
		    trackingMaximum = false;
		    lookingAtPeak = false;
		    value = -value;
		}
	    }
	    // No, so we must be currently tracking a minimum...
	    else {
		value = 127 - (data[pos] & 255);

		// Have we found our minimum??
		if (value < 0) {
		    minPos = peakPos;
		    minHeight = peakHeight;
		    peakPos = pos;
		    peakHeight = 0;
		    trackingMaximum = true;
		    lookingAtPeak = false;
		    value = -value;

		    // Process the Max/Min value that we have found
		    processMaxMin(maxPos, maxHeight, minPos, minHeight);
		}
	    }

	    // Deal with this sample
	    if (value > peakHeight) {
		peakHeight = value;
		peakPos = pos;
		lookingAtPeak = true;
	    }
	    else if (value < peakHeight && lookingAtPeak == true) {
		// We have found a maximum
		peakPos = (peakPos + pos - 1) / 2;
		lookingAtPeak = false;
	    }
	}
    }

    public void processMaxMin(int maxPos, int maxHeight, int minPos, int minHeight)
    {
	// int mil13 = (int) (0.0125 * sampleRate);  // !! PURE-ROG !!
	int eightieth = (int) (sampleRate / 80);
	int period;
	boolean newEstimate = false;

	// Calculate the next pitch estimate
	period = ppe[0].nextEstimate(maxPos, maxHeight);
	if (period >= 0) {
	    newEstimate = true;
	    ppe[0].setEstimate(period);
	}

	// Calculate the next pitch estimate
	period = ppe[1].nextEstimate(maxPos, maxHeight + lastMinHeight);
	if (period >= 0) {
	    newEstimate = true;
	    ppe[1].setEstimate(period);
	}

	// Calculate the next pitch estimate
	if (maxHeight > lastMaxHeight) {
	    period = ppe[2].nextEstimate(maxPos, maxHeight - lastMaxHeight);
	}
	else {
	    period = ppe[2].nextEstimate(maxPos, 0);
	}
	if (period >= 0) {
	    newEstimate = true;
	    ppe[2].setEstimate(period);
	}

	// Calculate the next pitch estimate
	period = ppe[3].nextEstimate(minPos, minHeight);
	if (period >= 0) {
	    newEstimate = true;
	    ppe[3].setEstimate(period);
	}

	// Calculate the next pitch estimate
	period = ppe[4].nextEstimate(minPos, minHeight + maxHeight);
	if (period >= 0) {
	    newEstimate = true;
	    ppe[4].setEstimate(period);
	}

	// Calculate the next pitch estimate
	if (minHeight > lastMinHeight) {
	    period = ppe[5].nextEstimate(minPos, minHeight - lastMinHeight);
	}
	else {
	    period = ppe[5].nextEstimate(minPos, 0);
	}
	if (period >= 0) {
	    newEstimate = true;
	    ppe[5].setEstimate(period);
	}

	// If we have a new estimate, calculate the winner...
	if (newEstimate == true) {
	    PitchValue winner = winnerCalc.calculateWinner();

	    // Period starts should be strictly increasing...
	    int periodStart = winner.position;
	    if (periodStart <= lastPeriodStart) {
		periodStart = lastPeriodStart + 1;
	    }

	    // If there has been a gap of more than 13 milliseconds, fill it with zero
	    // if ((lastPeriodStart + mil13) < periodStart) {  // !! PURE-ROG !!
	    if ((lastPeriodStart + eightieth) < periodStart) {
		// pitchData.add(new PitchValue(0, lastPeriodStart + mil13));  // !! PURE-ROG !!
		pitchData.add(new PitchValue(0, lastPeriodStart + eightieth));
	    }

	    // Save the pitch data
	    pitchData.add(new PitchValue(winner.period, periodStart));

	    lastPeriodStart = periodStart;
	}

	// Save the height values for next time
	lastMaxHeight = maxHeight;
	lastMinHeight = minHeight;
    }


    // ----------------------------------------
    //  Class : PitchEstimator
    // ----------------------------------------
    class PitchEstimator
    {
	int minSmoothPeriod = (int) (0.0012 * sampleRate);
	int maxSmoothPeriod = (int) (0.0012 * sampleRate * 10.0);
	int timeEndBlanking = 0;
	int timeLastEstimate = 0;
	int lnLast = 0;
	int smoothPeriod = minSmoothPeriod;
	int currentPeriod = 0;
	int currentPeriodStart = 0;
	int pitchEstimate[] = { 0, 0, 0, 0, 0, 0 };


	public int nextEstimate(int time, int height)
	{
	    // No change in the estimate
	    if (time < timeEndBlanking) {
		return -1;
	    }

	    int lnValue = 0;
	    if (height != 0) {
		lnValue = (int) (256 * Math.log((double) height));
	    }

	    // Absolutely no idea at all what this stuff is doing... :-)
	    if (lnValue >= (lnLast - ((178 * (time - timeEndBlanking)) / smoothPeriod))) {
		currentPeriod = time - timeLastEstimate;
		currentPeriodStart = timeLastEstimate;

		smoothPeriod = (smoothPeriod + currentPeriod) / 2;
		if (smoothPeriod < minSmoothPeriod) {
		    smoothPeriod = minSmoothPeriod;
		}
		if (smoothPeriod > maxSmoothPeriod) {
		    smoothPeriod = maxSmoothPeriod;
		}

		timeLastEstimate = time;
		timeEndBlanking = time + (smoothPeriod / 3);
		lnLast = lnValue;

		// Return the calculated period estimate
		return currentPeriod;
	    }

	    // No change in the estimate
	    return -1;
	}


	public void setEstimate(int estimate)
	{
	    pitchEstimate[2] = pitchEstimate[1];
	    pitchEstimate[1] = pitchEstimate[0];
	    pitchEstimate[0] = estimate;
	    pitchEstimate[3] = pitchEstimate[0] + pitchEstimate[1];
	    pitchEstimate[4] = pitchEstimate[1] + pitchEstimate[2];
	    pitchEstimate[5] = pitchEstimate[0] + pitchEstimate[1] + pitchEstimate[2]; 
	}


	public int getEstimate()
	{
	    return pitchEstimate[0];
	}


	public int getEstimatePosition()
	{
	    return currentPeriodStart;
	}


	public void compare(int period, int[] serr, int[] coin, int startNum)
	{
	    int error;
	    for (int i = startNum; i < 6; i++) {
		if (period > pitchEstimate[i]) {
		    error = period - pitchEstimate[i];
		}
		else {
		    error = pitchEstimate[i] - period;
		}

		if (error <= serr[0]) {
		    coin[0]++;
		}
		else if (error <= serr[1]) {
		    coin[1]++;
		}
		else if (error <= serr[2]) {
		    coin[2]++;
		}
		else if (error <= serr[3]) {
		    coin[3]++;
		}
	    }
	}
    }


    // ----------------------------------------
    //  Class : WinnerCalculator
    // ----------------------------------------
    class WinnerCalculator
    {    
	public PitchValue calculateWinner()
	{
	    // int mil1 = (int) (0.001 * sampleRate);  // !! PURE-ROG !!
	    int thousandth = (int) (sampleRate / 1000);
	    // int mil13 = (int) (0.0125 * sampleRate);  // !! PURE-ROG !!
	    int eightieth = (int) (sampleRate / 80);
	    int winningEstimateNum = 0;
	    int winningCoincidence = 0;
	    int winningPeriod = 0;

	    // Create a new PitchData object to store the winning pitch
	    PitchValue winner = new PitchValue(0, 0);

	    // Calculate the winner of the 6 estimates...
	    for (int estNum = 0; estNum < 6; estNum++) {

		int period = ppe[estNum].getEstimate();

		// Check that we are within the boundaries of the pitch tracker
		// if (period > mil1 && period < mil13) {  // !! PURE-ROG !!
		if (period > thousandth && period < eightieth) {

		    // Make sure we haven't calculated coincidences for this period before
		    int checkNum;
		    for (checkNum = 0; checkNum < estNum; checkNum++) {
			// Check if it has been calculated before
			if (period == ppe[checkNum].getEstimate()) {
			    break;
			}
		    }

		    // We need to calculate the coincidences for this period
		    if (checkNum == estNum) {
			int value = calculateCoincidences(estNum, period);
			if (value > winningCoincidence) {
			    winningCoincidence = value;
			    winningEstimateNum = estNum;
			    winningPeriod = period;
			}
		    }
		}
	    }

	    // Return the winning pitch
	    winner.period = winningPeriod;
	    winner.position = ppe[winningEstimateNum].getEstimatePosition();
	    return winner;
	}


	public int calculateCoincidences(int estNum, int period)
	{
	    // Allocate memory for the coincidence array
	    int[] coin = { 0, 0, 0, 0 };

	    // Allocate memory for some strange array
	    int[] serr = new int[4];
	    for (int i = 0; i < 4; i++) {
		serr[i] = ((i+1) * period) / 32;
		if (serr[i] <= 0) {
		    serr[i] = 1;
		}
	    }

	    // Do something weird...
	    for (int i = 0; i < 6; i++) {
		if (i == estNum) {
		    ppe[i].compare(period, serr, coin, 1);
		}
		else {
		    ppe[i].compare(period, serr, coin, 0);
		}
	    }

	    // More weird stuff to follow...
	    coin[1] += coin[0];
	    coin[2] += coin[1];
	    coin[3] += coin[2];

	    int maxCoin = (coin[0] - 1);
	    if ((coin[1] - 2) > maxCoin) {
		maxCoin = (coin[1] - 2);
	    }
	    if ((coin[2] - 5) > maxCoin) {
		maxCoin = (coin[2] - 5);
	    }
	    if ((coin[3] - 7) > maxCoin) {
		maxCoin = (coin[3] - 7);
	    }

	    // Return the maximum coincidence value
	    return maxCoin;
	}
    }
}
