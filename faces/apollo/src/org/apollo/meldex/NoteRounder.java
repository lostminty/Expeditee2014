package org.apollo.meldex;

public class NoteRounder
{
    private int prevValue;


    public void setInitialValue(int value)
    {
	prevValue = value;
    }


    public int round(int value, int mult)
    {
	return (((value + (mult/2)) / mult) * mult);
    }


    public int roundCents(int value)
    {
	return (round(value, 100));
    }


    public int roundNote(int value, boolean useAdaptiveTuning)
    {
	// If we aren't using adaptive tuning, ignore the previous value
	if (useAdaptiveTuning == false) {
	    return (roundCents(value));
	}

	// Otherwise calculate the new adaptive value
	else {
	    int finalValue = roundCents(value + prevValue);
	    prevValue = (prevValue + (finalValue - value)) / 2;
	    return finalValue;
	}
    }
}
