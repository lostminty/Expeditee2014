package org.expeditee.network;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.expeditee.gui.AttributeValuePair;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.MessageBay;
import org.expeditee.io.ExpReader;
import org.expeditee.io.ExpWriter;
import org.expeditee.io.FrameReader;
import org.expeditee.io.FrameWriter;
import org.expeditee.items.Item;
import org.expeditee.items.Picture;
import org.expeditee.items.Text;
import org.expeditee.settings.network.NetworkSettings;

public class FrameShare {

	public final static Charset CHARSET = Charset.forName("UTF-8");
	
	public static boolean disableNetworking = false;

	private static Collection<DefaultServer> _servers = new LinkedList<DefaultServer>();

	private static FrameShare _theSession;
	
	private static boolean headless = false;

	private Map<String, Peer> _peers;

	private int _port = Peer.DEFAULT_PORT;


        private void startServers() throws IOException
        {
	    System.err.println("Starting Expeditee Server on port " + _port);
	    _servers.add(new FrameServer(_port));
	    _servers.add(new FrameSaver(_port));
	    _servers.add(new MessageReciever(_port));
	    _servers.add(new InfServer(_port));
	    _servers.add(new InfUpdate(_port));
	    _servers.add(new ImageServer(_port));
	    _servers.add(new ImageSaver(_port));

	}

	private FrameShare() {
		_peers = new HashMap<String, Peer>();
	}

	private FrameShare(int port) {
        this();
		_port = port;

		try {
		    startServers();
		    
		} catch (Exception e) {
		    e.printStackTrace();
		    System.err.println("Could not start servers [port: "
				       + port + "]");
		}

		try {
		    for (DefaultServer server : _servers) {
			server.start();
		    }
		} catch (Exception e) {
		    System.err.println("Error in server startup");
		}
	}


 	private FrameShare(Frame settingsFrame) 
        {
	        this();

		// Set the settings
		for (Text item : settingsFrame.getBodyTextItems(false)) {

			AttributeValuePair avp = new AttributeValuePair(item.getText());
			if (avp.isAnnotation())
				continue;

			String attribute = avp.getAttributeOrValue().toLowerCase();

			if (attribute.equals("server")) {
				try {
					if (avp.hasPair()) {
						_port = avp.getIntegerValue();
					}
					MessageBay.displayMessage("Starting Expeditee Server on port: " + _port);
					
					startServers();
				} catch (Exception e) {
					e.printStackTrace();
					MessageBay.errorMessage("Could not start servers ["
							+ avp.toString() + "]");
				}
				continue;
			}

			if (!avp.hasPair())
				continue;

			try {
			    Peer peer = new Peer(avp);
			    _peers.put(attribute, peer);
			} catch (UnknownHostException e) {
				MessageBay.errorMessage("Could not locate peer ["
						+ avp.toString() + "]");
			}
		}

		try {
		    for (DefaultServer server : _servers) {
				server.start();
		    }
		} catch (Exception e) {
			MessageBay.errorMessage("Error in PeerToPeer setup");
		}
	}

	public void finalise() {
	    System.err.println("Closing servers");

		for (DefaultServer server : _servers)
			server.close();
	}

	public static FrameShare getInstance() {
		return _theSession;
	}

	/**
	 * TODO check each peer on a different thread.
	 * 
	 * @param frameName
	 * @return
	 */
	public Frame loadFrame(String frameName, String peerName) {
		String result = null;

		try {
			// get a datagram socket
			DatagramSocket socket = new DatagramSocket();
			socket.setSoTimeout(NetworkSettings.FrameShareTimeout.get() * 2);
			if (peerName == null) {
				for (Peer peer : _peers.values()) {
					try {
						result = getFrameContents(frameName, socket, peer);
						if(result == null || result.length() == 0) {
							continue;
						}
						peerName = peer.getName();
						break;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else {
				try {
					Peer peer = _peers.get(peerName.toLowerCase());
					if (peer != null) {
						result = getFrameContents(frameName, socket, peer);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			socket.close();
		} catch (Exception e) {
		    e.printStackTrace();
		}

		if (result == null || result.length() == 0)
			return null;

		// Now read the frame from the file contents
		FrameReader reader = new ExpReader(frameName);
		Frame frame = null;
		try {
			frame = reader.readFrame(new BufferedReader(
					new StringReader(result)));
			// Set the path for the frame to indicate it is NOT a local frame
			// This allows the frame to be saved in the correct location
			frame.setLocal(false);
			frame.setPath(peerName);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (frame == null) {
			MessageBay.errorMessage("Error: " + frameName
					+ " could not be successfully loaded.");
			return null;
		}

		return frame;
	}
	
	/**
	 * Downloads an image from the server (if it exists),
	 * Saves it locally in the default image folder,
	 * Then returns the filename
	 * 
	 * @param imageName
	 * @param peerName
	 * @return true if the image was successfully downloaded and saved in the default images folder
	 */
	public boolean loadImage(String imageName, String peerName) {
		boolean result = false;
		try {
			if (peerName == null) {
				for (Peer peer : _peers.values()) {
					try {
						result = getImage(imageName, peer);
						if(!result) {
							continue;
						}
						peerName = peer.getName();
						break;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else {
				try {
					Peer peer = _peers.get(peerName.toLowerCase());
					if (peer != null) {
						result = getImage(imageName, peer);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * Send a list of images that may need to be uploaded
	 * Then wait for a response (in form of a list of shorts denoting which files to keep)
	 * Then send each file in sequence
	 * @author jts21
	 *
	 */
	private class ImageSender extends Thread {
		
		private List<File> imageFiles;
		private Peer peer;
		
		public ImageSender(List<File> imageFiles, Peer peer) {
			super("ImageSender");
			this.imageFiles = imageFiles;
			this.peer = peer;
		}
		
		@Override
		public void run() {
			// System.out.println("Sending images to server");
			try(Socket socket = new Socket(peer.getAddress(), peer.getPort() + ImageSaver.OFFSET)) {
				socket.setSoTimeout(NetworkSettings.FrameShareTimeout.get() * 2);
				OutputStream os = socket.getOutputStream();
				// send the list of filenames to the server
				int numFiles = imageFiles.size();
				if(numFiles > 0xFFFF) {
					throw new Exception("Too many images on the frame");
				}
				os.write((byte) ((numFiles >> 8) & 0xFF));
            	os.write((byte) (numFiles & 0xFF));
            	// send the list of different files
				for(File f : imageFiles) {
					byte[] fileName = f.getName().getBytes();
            		int fileNameLen = fileName.length;
            		if(fileNameLen > 255) {
    					throw new Exception("Filename too long");
    				}
            		os.write((byte) ((fileNameLen) & 0xFF));
                	os.write(fileName);
				}
				// the server sends indices of files it wants, send their data
				InputStream is = socket.getInputStream();
				int i = -1;
				while((i = is.read()) != -1 && i < imageFiles.size()) {
					File f = imageFiles.get(i);
					// System.out.println("... sending " + f.getName());
					ImageServer.sendImage(f, socket);
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private boolean getImage(String imageName, Peer peer) throws IOException {
		try(Socket socket = new Socket(peer.getAddress(), peer.getPort() + ImageServer.OFFSET)) {
    		socket.setSoTimeout(NetworkSettings.FrameShareTimeout.get() * 2);
    		byte[] fileName = imageName.getBytes(FrameShare.CHARSET);
    		int fileNameLen = fileName.length;
    		OutputStream os = socket.getOutputStream();
        	os.write((byte) ((fileNameLen) & 0xFF));
        	os.write(fileName);
        	os.flush();
        	boolean ret = ImageSaver.recvImage(new File(FrameIO.IMAGES_PATH + imageName), socket);
        	socket.close();
        	return ret;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * @param frameName
	 * @param socket
	 * @param peer
	 * @return
	 * @throws IOException
	 */
	private String getFrameContents(String frameName, DatagramSocket socket, Peer peer) throws IOException {
		byte[] nameBuf = frameName.getBytes();
		byte[] buf = new byte[FrameServer.MAX_PACKET_LENGTH];
		// send request for a frame
		DatagramPacket packet = new DatagramPacket(nameBuf,
				nameBuf.length, peer.getAddress(), peer
						.getPort());
		socket.send(packet);

		// get response
		packet = new DatagramPacket(buf, buf.length);
		socket.receive(packet);

		// store frame contents
		return new String(packet.getData(), 0, packet.getLength(), FrameShare.CHARSET);
	}

	public boolean sendMessage(String message, String peerName) {
		Peer peer = _peers.get(peerName.toLowerCase());

		if (peer == null) {
			return false;
		}

		try {
			// get a datagram socket
			DatagramSocket socket = new DatagramSocket(_port - 1);
			socket.setSoTimeout(NetworkSettings.FrameShareTimeout.get());

			// message = peerName + " says " + message;
			byte[] contentsBuf = message.getBytes();

			try {
				// send save request
				DatagramPacket packet = new DatagramPacket(contentsBuf,
						contentsBuf.length, peer.getAddress(), peer.getPort()
								+ MessageReciever.OFFSET);
				socket.send(packet);
			} catch (Exception e) {
				e.printStackTrace();
			}
			socket.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	public String saveFrame(Frame toSave) {

		FrameIO.setSavedProperties(toSave);

		Peer peer = _peers.get(toSave.getPath().toLowerCase());
		
		List<File> imageFiles = new LinkedList<File>();
		for(Item i : toSave.getItems()) {
			if(i instanceof Picture) {
				((Picture) i).moveToImagesFolder();
				File f = new File(((Picture) i).getPath());
				if(f != null && f.isFile() && !imageFiles.contains(f)) {
					imageFiles.add(f);
				}
			}
		}
		new ImageSender(imageFiles, peer).start();
		
		String fileContents = "";
		// Now read the frame from the file contents
		FrameWriter writer = new ExpWriter();
		try {
			// String tempName = new Date().toString() + ".exp";
			// writer.setOutputLocation(tempName);
			// Write out the file to a StringBuffer
			StringWriter sw = new StringWriter();
			// First write out the name of the frame
			sw.write(toSave.getName() + "\n");
			// Then the version
			sw.write(toSave.getVersion() + "\n");
			// Write out the rest of the frame
			writer.writeFrame(toSave, sw);
			// Now send the packet
			fileContents = sw.getBuffer().toString();
			byte[] contentsBuf = fileContents.getBytes(FrameShare.CHARSET);

			// get a datagram socket
			DatagramSocket socket = new DatagramSocket(_port - 2);
			socket.setSoTimeout(NetworkSettings.FrameShareTimeout.get());

			try {
				// send save request
				DatagramPacket packet = new DatagramPacket(contentsBuf,
						contentsBuf.length, peer.getAddress(), peer.getPort()
								+ FrameSaver.OFFSET);
				socket.send(packet);
			} catch (Exception e) {
				e.printStackTrace();
			}
			socket.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		toSave.setSaved();
		return fileContents;
	}

	public String getPeerName(int port, InetAddress address) {
		for (Peer p : _peers.values()) {
			if (p.getPort() == port && p.getAddress().equals(address))
				return p.getName();
		}
		return null;
	}

	public int getInfNumber(String peerName, String frameset, boolean update)
			throws IOException {
		int result = -1;
		// get a datagram socket
		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket(_port - 3);
			socket.setSoTimeout(NetworkSettings.FrameShareTimeout.get());

			byte[] contentsBuf = frameset.getBytes();
			Peer peer = _peers.get(peerName.toLowerCase());
			if(peer != null) {
    			try {
    				// send inf request
    				DatagramPacket packet = new DatagramPacket(
    						contentsBuf,
    						contentsBuf.length,
    						peer.getAddress(),
    						peer.getPort()
    								+ (update ? InfUpdate.OFFSET : InfServer.OFFSET));
    				socket.send(packet);
    
    				byte[] buf = new byte[100];
    				// get response
    				packet = new DatagramPacket(buf, buf.length);
    				socket.receive(packet);
    
    				// store frame contents
    				result = Integer.parseInt(new String(packet.getData(), 0,
    						packet.getLength()));
    				peerName = peer.getName();
    
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
			}
			socket.close();
		} catch (SocketException e1) {
			e1.printStackTrace();
		}

		return result;
	}

	public static void init(Frame settingsFrame) {
		if (disableNetworking || settingsFrame == null)
			return;

		if (_theSession == null)
			_theSession = new FrameShare(settingsFrame);
	}

	public static void init(int port) {

	        if (_theSession == null) {
			_theSession = new FrameShare(port);
		}
	}
	
	public static void restart() {
		if(_theSession != null) {
			_theSession.finalise();
			_theSession = new FrameShare(_theSession._port);
		}
	}
	
	public static boolean isHeadless() {
		return headless;
	}


	/**
	 * Start a server running on the port number supplied on the command line
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

	    if (args.length != 1) {

    		// Work out the 'program name' (i.e. this class), but done in a general 
    		//   way in case class name is changed at a later date
    		StackTraceElement[] stack = Thread.currentThread ().getStackTrace ();
    		StackTraceElement main = stack[stack.length - 1];
    		String mainClass = main.getClassName ();
    
    		System.err.println("Usage: java " + mainClass + " port-number");
    		System.exit(1);
	    }
	    
	    MessageBay.suppressMessages(true);
	    
	    String port_str = args[0];
	    
	    FrameShare.headless = true;

	    try {
    		int port = Integer.parseInt(port_str);
    
    		init(port);
	    }
	    catch (Exception e) {
		e.printStackTrace();
		System.exit(2);
	    }

	}

}
