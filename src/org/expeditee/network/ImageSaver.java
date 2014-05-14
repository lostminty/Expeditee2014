package org.expeditee.network;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.expeditee.gui.FrameIO;

public class ImageSaver extends DefaultServer {
	
	public static boolean recvImage(File file, Socket socket) throws IOException {
		InputStream is = socket.getInputStream();
        // file length is the first 4 bytes
        int fileLen = (is.read() << 24) | (is.read() << 16) | (is.read() << 8) | is.read();
        // if the above reads failed, fileLen will be -1
        if(fileLen <= 0) {
        	return false;
        }
        if(file.exists()) {
        	System.out.println("Ignoring " + file.getName() + " (already exists on filesystem)");
        	is.skip(fileLen);
        	return false;
        }
        System.out.println("Receiving " + file.getName() + " (" + fileLen + " bytes)");
        file.getParentFile().mkdirs();
        FileOutputStream out = new FileOutputStream(file);
        // file is the remaining bytes of the stream
        byte[] buf = new byte[1024];
        int r = -1;
        int len = 0;
        while((r = is.read(buf)) > 0) {
        	out.write(buf, 0, r);
        	len += r;
        	if(fileLen - len <= 0) {
        		break;
        	}
        	if(fileLen - len < 1024) {
        		buf = new byte[fileLen - len];
        	}
        }
		out.flush();
        out.close();
    	return true;
	}
	
	/**
	 * Reads an image storage request, and saves the image on the server
	 * 
	 * @author jts21
	 *
	 */
	private static class ImageReceiver extends Thread {
		
		private Socket socket;
		
		public ImageReceiver(Socket client) {
			super("ImageReceiver");
			this.socket = client;
		}
		
		@Override
		public void run() {
			try {
				InputStream is = this.socket.getInputStream();
				OutputStream os = this.socket.getOutputStream();
				// number of files is the first byte
				int numFiles = is.read() << 8 | is.read();
				if(numFiles == -1)
					return;
				// read the filenames
				File[] files = new File[numFiles];
				List<Integer> wantFiles = new ArrayList<Integer>();
				for(int i = 0; i < numFiles; i++) {
					int fileNameLen = is.read();
					byte[] fileName = new byte[fileNameLen];
					is.read(fileName);
					files[i] = new File(FrameIO.IMAGES_PATH + new String(fileName, FrameShare.CHARSET));
					// pick out which files we want
					if(!files[i].exists()) {
						wantFiles.add(i);
						os.write(i);
					}
				}
				// close the output stream since we won't be sending anything more
				this.socket.shutdownOutput();
				// get the files
				for(Integer i : wantFiles) {
					recvImage(files[i], socket);
				}
	            this.socket.close();
            } catch (IOException e) {
	            e.printStackTrace();
            }
		}
	}
	
	public final static int OFFSET = 6;
	
	private ServerSocket socket;

	public ImageSaver(int port) throws IOException {
		super("ImageSaver");
		this.socket = new ServerSocket(port + OFFSET);
	}

	@Override
	protected void listenForMessages() throws IOException {
		Socket client = socket.accept();
		// should probably only allow a limited number of threads here
		new ImageReceiver(client).start();
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
