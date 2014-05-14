package org.apollo.meldex;


public class PitchValue
{
    // A pitch data value consists of a period and a position
    double period;
    int position;


    public PitchValue(double per, int pos)
    {
	period = per;
	position = pos;
    }
}
