package org.apollo.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.LinkedList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.apollo.audio.SampledTrackModel;
import org.apollo.audio.util.TrackMixSubject;
import org.apollo.io.IconRepository;
import org.apollo.mvc.AbstractSubject;
import org.apollo.mvc.SubjectChangedEvent;
import org.apollo.util.TrackModelHandler;
import org.apollo.util.TrackModelLoadManager;
import org.expeditee.gui.Browser;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.MessageBay;
import org.expeditee.gui.Popup;
import org.expeditee.gui.PopupManager;

/**
 * All expanded tracks are contained here. Fires SubjectChangedEvent.EMPTY events whenever
 * expansion selection changes.
 * 
 * @author Brook Novak
 *
 */
public class ExpandedTrackManager extends AbstractSubject implements TrackModelHandler {
	
	private static final int MAX_EXPANDED_TRACK_SIZE = 4;
	
	/** All epxanded tracks ... either in all in view or all selected ... to be viewed */
	private LinkedList<ExpandedTrackPopup> expandedTracks = new LinkedList<ExpandedTrackPopup> (); // SHARED Model data

	private MultiTrackExpansionPopup trackSelectionPopup = new MultiTrackExpansionPopup();
	
	private static ExpandedTrackManager instance = new ExpandedTrackManager();
	private ExpandedTrackManager() {
		
		// Keep view centered to browser size
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				// Keep expanded view centered to the browser window
				if (Browser._theBrowser != null) {
					Browser._theBrowser.addComponentListener(new ComponentListener() {

						public void componentHidden(ComponentEvent e) {
						}

						public void componentMoved(ComponentEvent e) {
						}

						public void componentResized(ComponentEvent e) {
							
							if (PopupManager.getInstance().isShowing(trackSelectionPopup)) {
								// User currently selecting..
								trackSelectionPopup.setLocation(
										(Browser._theBrowser.getWidth() / 2) - (trackSelectionPopup.getWidth() / 2), 0);
								
							} else { // expanded views might be showing
								
								// Fails with Maximizing of window on some systems... swing
								// bug!! really anoying
								updateLayout();
	
							}
	
						}

						public void componentShown(ComponentEvent e) {
						}
						
					});
				}
				
			}
		});
		
		// Registewr for (indirectly) handling track models
		TrackModelLoadManager.getInstance().addTrackModelHandler(this);

	}
	
	/**
	 * 
	 * @return The singleton instance.
	 */
	public static ExpandedTrackManager getInstance() {
		return instance;
	}
	
	private void fireTrackSelectionChanged() {
		trackSelectionPopup.onTrackSelectionChanged();
		fireSubjectChanged(SubjectChangedEvent.EMPTY);
	}
	

	/**
	 * Giver/receiver design pattern for only allowing this singleton
	 * to creat expanded views.
	 * 
	 * @param requested
	 */
	void receiveExpandedTrackPopup(ExpandedTrackPopup requested) {
		synchronized(expandedTracks) {
			assert (expandedTracks.size() < MAX_EXPANDED_TRACK_SIZE);
			expandedTracks.add(requested);
		}
		// Notify observers
		fireTrackSelectionChanged();
	}
	
	/**
	 * Whenever a ExpandedTrackPopup hides, it invokes this.
	 * @param xtp
	 */
	void expandedTrackPopupHidden(ExpandedTrackPopup xtp) {
		assert(xtp != null);
		synchronized(expandedTracks) {
			expandedTracks.remove(xtp);
		}
		
		// User might have closed one but kept more visible
		updateLayout();
		
		// Notify observers
		fireTrackSelectionChanged();
	}
	
	/**
	 * Adds a SampledTrackModel ready for expanding.
	 * 
	 * @param track
	 * 		The track to expand. Must not be null.
	 * 
	 * @param trackSourceFrameName
	 * 		Where the expanded tracks track source was expanded from.
	 * 		Must not be null
	 * 
	 * @param animationSource
	 * 		If null no animation will be used.
	 * 
	 * @param mix
	 * 		The mix that the expanded view should use / manipulate. Must not be null.
	 * 
	 * @return
	 * 		True if SampledTrackModel was added.
	 * 		False if not - because the selection amount is maxed out or
	 * 		the track is already selected.
	 * 
	 * @throws NullPointerException
	 * 		If trackModel or trackMix or trackSourceFrameName or localFilename is null.
	 */
	public boolean addTrackToSelection(
			SampledTrackModel track, 
			String trackSourceFrameName,
			Rectangle animationSource,
			TrackMixSubject mix) {
		
		assert(track != null);
		assert(Browser._theBrowser != null);
		assert(mix != null);
		
		synchronized(expandedTracks) {
	
			if (expandedTracks.size() < MAX_EXPANDED_TRACK_SIZE 
					&& !isTrackInExpansionSelection(track)) {
				
				ExpandedTrackPopup.giveToExpandedTrackManager(
						track, 
						mix,
						trackSourceFrameName,
						0,
						track.getFrameCount()); /** @see #receiveExpandedTrackPopup */
	
				Rectangle trackSelectionPopupBounds = null;
				
				// Ensure that the trackSelectionPopup is not already showing / animated to show
				if (!PopupManager.getInstance().isShowing(trackSelectionPopup)) {
					
					trackSelectionPopupBounds = new Rectangle(
							(Browser._theBrowser.getWidth() / 2 ) - (trackSelectionPopup.getWidth() / 2),
							0, 
							trackSelectionPopup.getWidth(), 
							trackSelectionPopup.getHeight());
					
					// Creep from above
					PopupManager.getInstance().showPopup(
							trackSelectionPopup,
							trackSelectionPopupBounds.getLocation(),
							null,
							PopupManager.getInstance().new ExpandShrinkAnimator(
									trackSelectionPopupBounds,
									null));
					
				} else {
					trackSelectionPopup.repaint();
				}
				
				// Animate a fake popup expanding into the trackSelectionPopup from the given source if not null
				if (animationSource != null) {
					
					PopupManager.getInstance().doPureAnimation(PopupManager.getInstance().new ExpandShrinkAnimator(
							animationSource, null), 
							(trackSelectionPopupBounds == null) ?
									trackSelectionPopup.getBounds() : trackSelectionPopupBounds);
				}
				
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Expands a given SampledTrackModel into view. Hides any other 
	 * ExpandedTrackPopup currently in view.
	 * 
	 * If the given track is arleady in view. It will be hidden and reshown.
	 * 
	 * @param track
	 * 		Must not be null.
	 * 
	 * @param mix
	 * 		The mix that the expanded view should use / manipulate. Must not be null.
	 * 
	 * @param animationSource
	 * 		Must not be null.
	 * 
	 * @param trackSourceFrameName
	 * 		Where the expanded tracks track source was expanded from.
	 * 		Must not be null
	 * 
	 * @param frameStart
	 * 		the initial zoom - clamped
	 * 
	 * @param frameLength
	 * 		the initial zoom - clamped
	 * 
	 * @throws NullPointerException
	 * 		If trackModel or trackMix or trackSourceFrameName or localFilename is null.
	 * 
	 */
	public void expandSingleTrack(
			SampledTrackModel track, 
			Rectangle animationSource, 
			TrackMixSubject mix,
			String trackSourceFrameName,
			int frameStart,
			int frameLength) {
		assert(animationSource != null);
		assert(track != null);

		synchronized(expandedTracks) {
			// Hide all expandedTracks that are currently showing
			while (!expandedTracks.isEmpty())
				PopupManager.getInstance().hidePopup(expandedTracks.getFirst());
		}

		// Create a expanded tracks view
		ExpandedTrackPopup.giveToExpandedTrackManager(
				track, 
				mix,
				trackSourceFrameName,
				frameStart,
				frameLength); /** @see #receiveExpandedTrackPopup */
		
		// Show the expanded track from - expanding from the given animation source
		expandTracks(animationSource);
	}
	
	/**
	 * Removes a SampledTrackModel from being expanded...
	 * @param track
	 */
	public void removeTrackFromSelection(SampledTrackModel track) {
		
		assert(track != null);
		
		synchronized(expandedTracks) {
		
			ExpandedTrackPopup expandedView = getExpandedTrackPopup(track);
			
			// Does exist?
			if (expandedView == null) return;
			
			// Is already showing?
			else if (PopupManager.getInstance().isShowing(expandedView)) return;
			
			// Remove from selection
			expandedTracks.remove(expandedView);
			
			// Explicitly release memory since not called due to it never being in the PopupManager
			expandedView.releaseMemory(); 
			
			// Notify observers
			fireTrackSelectionChanged();
			
			// Hide the popup selection if nothing selected anymore
			if (expandedTracks.isEmpty()) {
				hideTrackSelectionPopup();
			}
		}
	}
	
	private void cancelSelection() {
		
		hideTrackSelectionPopup();
		
		// Remove from selection
		LinkedList<? extends ExpandedTrackPopup> old = (LinkedList<? extends ExpandedTrackPopup>)expandedTracks.clone();
		
		synchronized(expandedTracks) {
			expandedTracks.clear();
		}
		
		// Explicitly release memory since may have not of called due to it never being in the PopupManager
		for (ExpandedTrackPopup xtp : old) 
			xtp.releaseMemory(); 
		
		// Notify observers
		fireTrackSelectionChanged();
	}
	
	/**
	 * @param track
	 * 		The track to search for.
	 * 
	 * @return
	 * 		A tracks ExpandedTrackPopup if one exists.
	 */
	private ExpandedTrackPopup getExpandedTrackPopup(SampledTrackModel track) {
		assert(track != null);
		
		synchronized(expandedTracks) {
			for (ExpandedTrackPopup xtp : expandedTracks) {
				if (xtp.getObservedTrackModel() == track) return xtp;
			}
		}
		
		return null;
	}
	
	
	/**
	 * 
	 * @param track
	 * 		Must not be null.
	 * 
	 * @return
	 * 		True if the track is expanded - or is selected to be expanded..
	 */
	public boolean isTrackInExpansionSelection(SampledTrackModel track) {
		assert(track != null);
		return getExpandedTrackPopup(track) != null;
	}
	
	/**
	 * @return
	 * 			True if at least 1 expanded track is showing - or is selected to show.
	 */
	public boolean doesExpandedTrackExist() {
		synchronized(expandedTracks) {
			return expandedTracks.size() > 0;
		}
	}
	
	public boolean isAnyExpandedTrackVisible() {
		for (ExpandedTrackPopup xtp : expandedTracks) {
			if (PopupManager.getInstance().isShowing(xtp)) return true;
		}
		
		return false;
	}
	
	/**
	 * Shows all popups (if not already visible) to view and lays them out.
	 *
	 */
	public void showSelection() {
		expandTracks(null);
	}
	
	/**
	 * Returns immediatly if there are no tracks selected. Only expands if the popup is not already expanded.
	 * 
	 * @param directAnimSource
	 * 		Set to null to animate expanded popups from the trackSelectionPopup.
	 * 		Otherwise specify where to animate from. (Intention for latter case: a single expanded view from
	 * 		a widget).
	 */
	private void expandTracks(Rectangle directAnimSource) {

		synchronized(expandedTracks) {
			
			if (expandedTracks.isEmpty()) return;
			
			// Animate the popups from the trackSelectionPopup if there is not directAnimSource
			if (directAnimSource == null) {
				directAnimSource = trackSelectionPopup.getBounds();
			}
			
			// Update the expanded view layouts
			updateLayout();
			
			// Animate the popup(s)
			for (ExpandedTrackPopup xtp : expandedTracks) {
	
				// Note: returns immediatly if already showing
				PopupManager.getInstance().showPopup(
						xtp, 
						xtp.getLocation(), 
						null,
						PopupManager.getInstance().new ExpandShrinkAnimator(
								directAnimSource,
								null));
			
			}
		}
		
		// Hide the trackSelectionPopup
		hideTrackSelectionPopup();

	}
	
	private void hideTrackSelectionPopup() {
		PopupManager.getInstance().hidePopup(
				trackSelectionPopup, PopupManager.getInstance().new ExpandShrinkAnimator(
						new Rectangle((Browser._theBrowser.getWidth() / 2 ) - (trackSelectionPopup.getWidth() / 2),
								0, trackSelectionPopup.getWidth(), trackSelectionPopup.getHeight()), null));
	}
	
	/**
	 * Sets the bounds for all ExpandedTrackPopups, whether they are showing or not, to the
	 * current state of the window.
	 */
	private void updateLayout() {
		
		synchronized(expandedTracks) {
				
	
			if (expandedTracks.isEmpty()) return;
			
			assert (expandedTracks.size() > 0);
			assert(Browser._theBrowser != null);
		
			int cpWidth = Browser._theBrowser.getContentPane().getWidth();
			int cpHeight = Browser._theBrowser.getContentPane().getHeight();
			if (!FrameGraphics.isAudienceMode()) { // if message bay is showing - the remove the height
				
				cpHeight -= MessageBay.MESSAGE_BUFFER_HEIGHT;
				
			}
			
			if (cpHeight < 0) cpHeight = 0;
			
			// Calc widths
			final int EXPANDED_HORO_MARGINS = 100;
			int widths = cpWidth - (2 * EXPANDED_HORO_MARGINS);
			if (widths < 150) widths = 150; 
			
			// Calc heights (per track view)
			final int DESIRED_EXPANDED_HEIGHT = 340;
			final int EXPANDED_VERT_MARGINS = 20;
			int heights;
			if ((expandedTracks.size() * DESIRED_EXPANDED_HEIGHT) <= (cpHeight - (2 * EXPANDED_VERT_MARGINS))) {
				heights = DESIRED_EXPANDED_HEIGHT;
			} else {
				heights = (cpHeight - (2 * EXPANDED_VERT_MARGINS)) / expandedTracks.size();
				if (heights < 100) heights = 100;
			}
	
			// Center the popup(s) 
			int y = ((cpHeight - (1 * EXPANDED_VERT_MARGINS)) - (expandedTracks.size() * heights)) / 2;
			if (y < 0 || y > cpHeight) y = EXPANDED_VERT_MARGINS;
			
			int x = (cpWidth - widths) / 2;
			if (x < 0) x = 0;
			
			// Update bounds
			for (ExpandedTrackPopup xtp : expandedTracks) {
	
				xtp.setBounds(x, y, widths, heights);
		
				y += heights;
				
			}
		}
		
	}

	/**
	 * Global Track model re-use .... can be called from any thread
	 */
	public SampledTrackModel getSharedSampledTrackModel(String localfilename) {
		synchronized(expandedTracks) {
			for (ExpandedTrackPopup xtp : expandedTracks) {
				if (xtp.getObservedTrackModel().getLocalFilename().equals(localfilename))
					return xtp.getObservedTrackModel();
			}
		}
		
		return null;
	}

	
	/**
	 * A little popup that shows whenever there are tracks selected for expanding
	 * @author bjn8
	 *
	 */
	private class MultiTrackExpansionPopup extends Popup implements ActionListener {
		
		private JButton expandButton;
		private JButton cancelButton;
		private JLabel selected1;
		private JLabel selected2;
		private JLabel selectedMore;

		private static final long serialVersionUID = 1L;
		//private final Color BACK_COLOR = SampledTrackGraphViewPort.ZOOM_BACKING_COLOR_NORMAL;
		
		MultiTrackExpansionPopup() {
			super(new GridBagLayout());
			setSize(260, 80);
			super.setAudoHide(false);
			
			expandButton = new JButton(IconRepository.getIcon("expand.png"));
			expandButton.addActionListener(this);
			expandButton.setPreferredSize(new Dimension(40, 40));
			expandButton.setToolTipText("Expand selection");
			
			cancelButton = new JButton(IconRepository.getIcon("close.png"));
			cancelButton.addActionListener(this);
			cancelButton.setPreferredSize(new Dimension(40, 40));
			cancelButton.setToolTipText("Cancel selection");
			
			selected1 = new JLabel();
			selected1.setHorizontalTextPosition(JLabel.LEFT);
			selected1.setVerticalTextPosition(JLabel.CENTER);
			
			selected2 = new JLabel();
			selected2.setHorizontalTextPosition(JLabel.LEFT);
			selected2.setVerticalTextPosition(JLabel.CENTER);
			
			selectedMore = new JLabel();
			selectedMore.setHorizontalTextPosition(JLabel.CENTER);
			selectedMore.setVerticalTextPosition(JLabel.CENTER);
			
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = 0;
			c.fill = GridBagConstraints.CENTER;
			add(expandButton, c);
			
			c = new GridBagConstraints();
			c.gridx = 1;
			c.gridy = 0;
			c.fill = GridBagConstraints.CENTER;
			add(cancelButton, c);
			
			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = 1;
			c.gridwidth = 2;
			c.fill = GridBagConstraints.CENTER;
			add(selected1, c);
			
			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = 2;
			c.gridwidth = 2;
			c.fill = GridBagConstraints.CENTER;
			add(selected2, c);
			
			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = 1;
			c.gridwidth = 2;
			c.gridheight = 2;
			c.fill = GridBagConstraints.CENTER;
			add(selectedMore, c);

		}
		
		public void onTrackSelectionChanged() {
			
			synchronized(expandedTracks) {
					
				if (expandedTracks.isEmpty()) return;
				
				selected1.setVisible(false);
				selected2.setVisible(false);
				selectedMore.setVisible(false);
	
				if (expandedTracks.size() <= 2) {
					for (int i = 0; i < expandedTracks.size(); i++) {
						
						ExpandedTrackPopup expp = expandedTracks.get(i);
						JLabel label = (i == 0) ? selected1 : selected2;
						
						String trackName = expp.getObservedTrackModel().getName();
						
						if (trackName == null || trackName.length() == 0) trackName = "Unnamed";
						else if (trackName.length() > 16) trackName = trackName.substring(0, 14) + "...";
						
						label.setText(trackName);
						label.setVisible(true);
						
					}
					
				} else {
					selectedMore.setText(expandedTracks.size() + " tracks selected");
					selectedMore.setVisible(true);
				}
			}
		}

		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == expandButton) {
				showSelection();
			} else if (e.getSource() == cancelButton) {
				cancelSelection();
			}
		}
		
		

	}
	
	
}
