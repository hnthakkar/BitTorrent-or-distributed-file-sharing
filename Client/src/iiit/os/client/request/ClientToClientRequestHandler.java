package iiit.os.client.request;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

import iiit.os.client.Client;

/**
 * Created by hnthakka on 05-Oct-19.
 */
public class ClientToClientRequestHandler implements Runnable {

	private Socket socket;
	private DataInputStream din;
	private DataOutputStream dos;
	
	/**
	 * Client can only get request from other client for Chunks of file present with
	 * the current client.
	 */
	public ClientToClientRequestHandler(Socket socket, DataInputStream din, DataOutputStream dos) {
		this.socket = socket;
		this.din = din;
		this.dos = dos;
	}

	@Override
	public void run() {
		try {
			String request = din.readUTF();
			String[] requestparts = request.split(":");
			if (requestparts.length == 2) {
				if (requestparts[0].equals(ClientToClientHelper.downloadRequestType)) {
					processFetchChunk(requestparts[1]);
				} else {
					processUploadChunk(requestparts[1]);
				}
			} else {
				throw new Exception();
			}
		} catch (Exception e) {
			System.out.println("problem while catering chunk request");
			e.printStackTrace();
		} finally {
			try {
				din.close();
				dos.close();
				socket.close();
			} catch (Exception e) {
				System.out.println("Problem closing the resources");
			}
		}
	}
	
	private void processUploadChunk(String chunkName) throws Exception {
		ClientToClientHelper.readStreamWriteToFile(Client.chunkDir, chunkName, din);
		System.out.println("upload Request received chunk " + chunkName);
	}

	private void processFetchChunk(String chunkRequested) throws Exception {
		ClientToClientHelper.readFileWriteToStream(Client.chunkDir, chunkRequested, dos);
	}
}
