package org.expeditee.network;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.expeditee.gui.FrameIO;

public class ImageServer extends DefaultServer {
	
	public static void sendImage(File file, Socket socket) throws IOException {
		if(!file.isFile()) {
			System.out.println("Could not find " + file.getName());
			return;
		}
		OutputStream os = socket.getOutputStream();
       	// send the file over the socket
    	int fileLen = (int) file.length();
    	System.out.println("Sending " + file.getName() + " (" + fileLen + " bytes)");
    	byte[] fileLenBytes = new byte[4];
    	fileLenBytes[0] = (byte) ((fileLen >> 24) & 0xFF);
    	fileLenBytes[1] = (byte) ((fileLen >> 16) & 0xFF);
    	fileLenBytes[2] = (byte) ((fileLen >> 8) & 0xFF);
    	fileLenBytes[3] = (byte) ((fileLen) & 0xFF);
    	os.write(fileLenBytes);
    	FileInputStream in = new FileInputStream(file);
    	byte[] buf = null;
    	if(in.available() > 1024) {
    		buf = new byte[1024];
    		while(in.available() > 1024) {
    			in.read(buf);
    			os.write(buf);
    		}
    	}
    	buf = new byte[in.available()];
    	in.read(buf);
		os.write(buf);
		os.flush();
		in.close();
	}
	
	/**
	 * Reads an image request, and sends the image back if it was found
	 * 
	 * @author jts21
	 *
	 */
	private static class ImageSender extends Thread {
		
		private Socket socket;
		
		public ImageSender(Socket client) {
			super("ImageSender");
			this.socket = client;
		}
		
		@Override
		public void run() {
			try {
				InputStream is = this.socket.getInputStream();
				// file name length is the first byte
	            int fileNameLen = is.read();
	            if(fileNameLen <= 0)
	            	return;
	            // file name is the remaining bytes of the stream, encoded in UTF-8
	            byte[] fileName = new byte[fileNameLen];
	            is.read(fileName);
	            sendImage(new File(FrameIO.IMAGES_PATH + new String(fileName, FrameShare.CHARSET)), socket);
            	this.socket.close();
            } catch (IOException e) {
	            e.printStackTrace();
            }
		}
	}
	
	public final static int OFFSET = 5;
	
	private ServerSocket socket;

	public ImageServer(int port) throws IOException {
		super("ImageServer");
		this.socket = new ServerSocket(port + OFFSET);
	}

	@Override
	protected void listenForMessages() throws IOException {
		Socket client = socket.accept();
		// should probably only allow a limited number of threads here
		new ImageSender(client).start();
	}
	
	@Override
	protected void closeSocket() {
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
