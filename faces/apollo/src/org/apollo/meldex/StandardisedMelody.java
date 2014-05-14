package org.apollo.meldex;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


/**
 */
public class StandardisedMelody implements Serializable
{

	private static final long serialVersionUID = -7246438481007658500L;
	
	/** The array of MelodyEvents that form this melody. */
    private MelodyEvent[] events;


    /**
     * Creates a new StandardisedMelody with the given melody events.
     *
     * @param events The melody events that form the melody.
     */
    public StandardisedMelody(MelodyEvent[] events)
    {
	this.events = events;
    }


    /**
     * Returns the length (number of melody events) of this melody.
     *
     * @return the length (number of melody events) of this melody.
     */
    public int getLength()
    {
	return events.length;
    }


    /**
     * Returns a specified melody event from the melody.
     *
     * @param index The index of the required melody event.
     *
     * @return The melody event at the specified index.
     */
    public MelodyEvent getEvent(int index)
    {
	return events[index];
    }


    /**
     */
    public void setEvent(int index, MelodyEvent event)
    {
	events[index] = event;
    }


    /**
     */
    public static void writeMelodyToFile(String melodyFileName, StandardisedMelody melody)
    	throws FileNotFoundException, IOException
    {
	    // Open the melody file for writing serialized objects
	    FileOutputStream fos = new FileOutputStream(melodyFileName);
	    ObjectOutputStream melodyFileStream = new ObjectOutputStream(fos);

	    // Write the melody to the melody file
	    melodyFileStream.writeObject(melody);

	    // Flush and close the collection file
	    melodyFileStream.flush();
	    melodyFileStream.close();

    }


    /**
     */
    public static StandardisedMelody readMelodyFromFile(String melodyFileName) 
    	throws FileNotFoundException, IOException, ClassNotFoundException
    {
	    // Open the melody file for reading serialized objects
	    FileInputStream fis = new FileInputStream(melodyFileName);
	    ObjectInputStream melodyFileStream = new ObjectInputStream(fis);

	    // Read the melody from the melody file
	    return (StandardisedMelody) melodyFileStream.readObject();

    }
}
