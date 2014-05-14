package org.apollo.audio;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;

import org.apollo.io.AudioIO;
import org.apollo.mvc.AbstractSubject;
import org.apollo.mvc.SubjectChangedEvent;

/**
 * A modifiable sampled audio track.
 * 
 * @author Brook Novak
 *
 */
public class SampledTrackModel extends AbstractSubject {
	
	private AudioFormat audioFormat = null;

	private byte[] audioBytes = null;

	private int selectionStart = 0; // in frames
	
	private int selectionLength = -1; // in frames <= 1 not ranged.
	
	private boolean isAudioModified = false;
	
	private String currentFilepath = null;
	
	private String localFilename = null; // never null, immutable
	
	private String name = null;
	
	/**
	 * Constructor.
	 * 
	 * @param audioBytes
	 * 			Pure audio samples. Must not be an empty array.
	 * 
	 * @param audioFormat
	 * 			The format of the given audio samples.
	 * 
	 * @param localFilename
	 * 			The localfilename to associate this track with... imutable. Must never
	 * 			be null.
	 * 
	 * @throws IllegalArgumentException
	 * 			If audioFormat requires conversion. See SampledAudioManager.isFormatSupportedForPlayback
	 * 
	 * @throws NullPointerException
	 * 			If audioBytes or audioFormat is null
	 */
	public SampledTrackModel(byte[] audioBytes, AudioFormat audioFormat, String localFilename) {
		
		if (audioBytes == null)
			throw new NullPointerException("audioBytes");
		if (audioFormat == null)
			throw new NullPointerException("audioFormat");
		if (localFilename == null || localFilename.length() == 0)
			throw new NullPointerException("localFilename");
		
		assert(audioBytes.length > 0);
		
		// Check format
		if (!SampledAudioManager.getInstance().isFormatSupportedForPlayback(audioFormat))
			throw new IllegalArgumentException("Bad audioFormat");
		
		this.audioFormat = audioFormat;
		this.audioBytes = audioBytes;
		this.localFilename = localFilename;
		
	}
	
	/**
	 * @return True if audio bytes have been changed in some way since creation / last reset.
	 */
	public boolean isAudioModified() {
		return isAudioModified;
	}
	
	/**
	 * Sets modified flag.
	 */
	public void setAudioModifiedFlag(boolean isModified) {
		isAudioModified = isModified;
	}
	
	/**
	 * Same as fireSubjectChanged but also sets modified flag
	 */
	private void fireAudioBytesChanged(final SubjectChangedEvent event) {
		isAudioModified = true;
		fireSubjectChanged(event);
	}
	
	/**
	 * @return The audio format of the sampled bytes
	 */
	public AudioFormat getFormat() {
		return audioFormat;
	}
	
	/**
	 * @return The amount of frames contained in this audio track
	 */
	public int getFrameCount() {
		return audioBytes.length / audioFormat.getFrameSize();
	}

	
	/**
	 * @return The start of selection in frames.
	 */
	public int getSelectionStart() {
		return selectionStart;
	}
	
	/**
	 * @return The length of selection in frames. less or equal to one if selection length is one frame.
	 * 			Otherwise selection is ranged.
	 */
	public int getSelectionLength() {
		return selectionLength;
	}
	
	/**
	 * Sets the selection. Clamped
	 * 
	 * @param start 
	 * 			Must be larger or equal to zero. In frames.
	 * @param length 
	 * 			In frames. If less or equal to one, then the selection length is one frame.
	 * 			Otherwise selection is ranged.
	 */
	public void setSelection(int start, int length) {
		
		if (start < 0) start = 0;
		
		if ((start + length) > getFrameCount())
			length = getFrameCount() - start;
		
		selectionStart = start;
		selectionLength = length;
		
		fireSubjectChanged(new SubjectChangedEvent(
				ApolloSubjectChangedEvent.SELECTION_CHANGED));
	}
	
	/**
	 * @return The audio bytes of the selected frames
	 */
	public byte[] getSelectedFramesCopy() {
		
		int len = (selectionLength <= 0) ? 1 : selectionLength;
		len *= audioFormat.getFrameSize();
		
		byte[] selectedBytes = new byte[len];
		
		System.arraycopy(
				audioBytes, 
				selectionStart * audioFormat.getFrameSize(), 
				selectedBytes, 
				0, 
				len);
		
		return selectedBytes;
	}
	
	/**
	 * Removes the selected frames. If no <i>range</i> is selected then it will return immediatly.
	 * raises a {@link ApolloSubjectChangedEvent#AUDIO_REMOVED} event if removed frames.
	 * 
	 * Resets the selection length to nothing once removed the frames (selection start
	 * remains the same). Thus raises a selection changed event.
	 * 
	 * Does not allow removeing of all bytes ... must have at least frame left.
	 * 
	 * Can be playing in a playing state - as nothing "should" break.
	 */
	public void removeSelectedBytes() {
		
		if(selectionLength <= 1) throw new IllegalStateException("No range selected to remove");
		
		int len = selectionLength * audioFormat.getFrameSize();
		int start = selectionStart * audioFormat.getFrameSize();
		
		if ((audioBytes.length - len) == 0) return;
		
		byte[] newAudioBytes = new byte[audioBytes.length - len];
		
		// Copy first chunk of bytes before selection
		System.arraycopy(
				audioBytes, 0, 
				newAudioBytes, 0, 
				start);
		
		// Copy remaining bytes after selection
		System.arraycopy(
				audioBytes, start + len, 
				newAudioBytes, start, 
				audioBytes.length - (start + len));
		
		audioBytes = newAudioBytes;
		
		setSelection(selectionStart, 0); // raises the event.

		fireAudioBytesChanged(new SubjectChangedEvent(
				ApolloSubjectChangedEvent.AUDIO_REMOVED));
	}
	
	/**
	 * Inserts bytes <b>AFTER</b> a given frame position. 
	 * <b>TAKE NOTE:</b> Not zero-indexed. One-indexed.
	 * 
	 * @param bytesToAdd
	 * 			The bytes to add. Must not be null and be length larger than zero.
	 * 
	 * @param format
	 * 			The format of the audio bytes to add. Must not be null.
	 * 
	 * @param framePosition
	 * 			Zero is the lower bound case, where the bytes are added at the very beggining.
	 * 			The upper bound case is the total frame-count of this track model, where the bytes
	 * 			are added to the very end of the track.
	 * 
	 * @throws IOException
	 * 		If an error occured while converting
	 * 
	 * @throws IllegalArgumentException
	 * 		If the conversion is not supported.
	 * 
	 */
	public void insertBytes(byte[] bytesToAdd, AudioFormat format, int framePosition) 
		throws IOException {
		
		assert(format != null);
		assert(bytesToAdd != null);
		assert(bytesToAdd.length > 0);
		assert(framePosition >= 0);
		assert(framePosition <= getFrameCount());
		assert(SampledAudioManager.getInstance().isFormatSupportedForPlayback(format));
		
		// Convert format - if needs to
		byte[] normalizedBytes = AudioIO.convertAudioBytes(bytesToAdd, format, audioFormat);
		assert(normalizedBytes != null);
		
		// Insert bytes at frame position
		int insertBytePosition = framePosition * audioFormat.getFrameSize();
		byte[] newAudioBytes = new byte[audioBytes.length + normalizedBytes.length];

		// Add preceeding audio bytes from original track
		if (framePosition > 0) 
			System.arraycopy(
					audioBytes, 0, 
					newAudioBytes, 0, 
					insertBytePosition);
		
		// Add the inserted audio bytes
		System.arraycopy(
				normalizedBytes, 0, 
				newAudioBytes, insertBytePosition, 
				normalizedBytes.length);
		
		// Append the proceeding audio bytes of the original track
		if (framePosition < getFrameCount()) 
			System.arraycopy(
					audioBytes, insertBytePosition, 
					newAudioBytes, insertBytePosition +  normalizedBytes.length, 
					audioBytes.length - insertBytePosition);
		
		// Assign the new bytes
		audioBytes = newAudioBytes; 

		// Notify observers
		fireAudioBytesChanged(new SubjectChangedEvent(
				ApolloSubjectChangedEvent.AUDIO_INSERTED));
		
	}
	
	/**
	 * @see SampledTrackModel#getAllAudioBytesCopy()
	 * 
	 * @return The <b>actual</b>(same reference) audio bytes for this model. 
	 */
	public byte[] getAllAudioBytes() {
		return audioBytes;
	}
	
	/**
	 * @see SampledTrackModel#getAllAudioBytes()
	 * 
	 * @return ALl the audio bytes - copied.
	 */
	public byte[] getAllAudioBytesCopy() {
		byte[] copy = new byte[audioBytes.length];
		System.arraycopy(audioBytes, 0, copy, 0, audioBytes.length);
		return copy;
	}
	
	/**
	 * @return
	 * 		The file path associtaed to this model.
	 * 		Null if none is set.
	 */
	public String getFilepath() {
		return currentFilepath;
	}

	/**
	 * @param filepath
	 * 		The filename to associate with this model.
	 * 		Can be null.
	 */
	public void setFilepath(String filepath) {
		this.currentFilepath = filepath;
	}

	/**
	 * @return
	 * 		The name associated to this tack. Can be null.
	 * 
	 * @see #setName(String)
	 * 
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name for this track. Might be useful for identifying a track in
	 * a human readable format.
	 * 
	 * Note that the name is not used for anything inside this package - it is
	 * for the convience of the user only.
	 * 
	 * creates a {@link ApolloSubjectChangedEvent#NAME_CHANGED} event
	 * 
	 * @param name
	 * 		The name associated to this tack. Can be null.
	 */
	public void setName(String name) {
		if (name != this.name) {
			this.name = name;
			fireSubjectChanged(new SubjectChangedEvent(
					ApolloSubjectChangedEvent.NAME_CHANGED, null));
		}
	}

	/**
	 * @return
	 * 		The immutable local filename associated with this track model.
	 */
	public String getLocalFilename() {
		return localFilename;
	}

}
