package org.apollo.meldex;

public class RogTrackEvent
{
    // Special track event constants
    final static int LAST_NOTE_EVENT = 127;
    final static int FERMATA_EVENT = 128;
    final static int KEY_SIG_EVENT = 129;
    final static int TIME_SIG_EVENT = 130;
    final static int TEMPO_EVENT = 131;

    // The type of the track event and it's parameters
    int type;
    int param1;
    int param2;


    public RogTrackEvent(int t, int p1, int p2)
    {
	type = t;
	param1 = p1;
	param2 = p2;
    }
}
