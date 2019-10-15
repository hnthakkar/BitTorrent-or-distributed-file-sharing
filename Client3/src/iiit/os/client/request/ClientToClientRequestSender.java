package iiit.os.client.request;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Callable;

import iiit.os.client.Client;

/**
 * Created by hnthakka on 05-Oct-19.
 */
public class ClientToClientRequestSender implements Callable<String> {

	private String chunkName;
	private String preferredAddress;
	private List<String> alternateClients;
	private String requestType;
	
	// In case of upload, its the Client to which it connect
	// for download, its the Hash of the chunk
	private String connectClientAddress;
	
	public ClientToClientRequestSender(String chunkName, String preferredAddress, String requestType, List<String> alternateClients) {
		this.chunkName = chunkName;
		this.preferredAddress = preferredAddress;
		this.alternateClients = alternateClients;
		this.requestType = requestType;
	}
	
	private Socket getActiveClientSocket() throws Exception {
		Socket socket = null; 
		// first try with the preferred address
		try {
			String[] parts = preferredAddress.split(":");
			String ip = parts[0];
			int port = Integer.parseInt(parts[1]);
			socket = new Socket(ip, port);
			connectClientAddress = preferredAddress;
			return socket;
		} catch (Exception e) {
			System.out.println("Preferred Address socket failed");
		}
		
		for (String alternateClient : alternateClients) {
			String[] parts = alternateClient.split(":");
			String ip = parts[0];
			int port = Integer.parseInt(parts[1]);
			try {
				socket = new Socket(ip, port);
				if (socket != null) {
					connectClientAddress = alternateClient;
					break;
				}
			} catch (Exception e) {
				// do nothing continue;
			}
		}
		
		if (socket == null) {
			System.out.println("No Active Clients");
			throw new Exception();
		} 
		return socket;
	}

	@Override
	public String call() {
		Socket socket = null;
		DataOutputStream dos = null;
		DataInputStream din = null;
		String retString = null;
		try {
			socket = getActiveClientSocket();
			System.out.println("Socket Connection established");
			dos = new DataOutputStream(socket.getOutputStream());
			din = new DataInputStream(socket.getInputStream());
			if (requestType.equals(ClientToClientHelper.downloadRequestType)) {
				retString = fetchChunk(chunkName, dos, din);
			} else {
				retString = uploadChunk(Client.chunkDir, chunkName, dos);
			}
		} catch (Exception e) {
			System.out.println("Problem while sending Chunk Request");
			e.printStackTrace();
		} finally {
			try {
				din.close();
				dos.close();
				socket.close();
			} catch (Exception e) {
				System.out.println("Problem while closing the resources");
			}
		}
		return retString;
	}
	
	private String fetchChunk(String chunkName, DataOutputStream dos, DataInputStream din) throws Exception {
		dos.writeUTF(ClientToClientHelper.downloadRequestType + ":" + chunkName);
		System.out.println("Request for chunk " + chunkName + " sent, waiting for response");
		return ClientToClientHelper.readStreamWriteToFile(Client.chunkDir, chunkName, din);
	}
	
	private String uploadChunk(String path, String chunkName, DataOutputStream dos) throws Exception {
		dos.writeUTF(ClientToClientHelper.uploadRequestType + ":" + chunkName);
		System.out.println("Uploading chunk " + chunkName + " to other client");
		ClientToClientHelper.readFileWriteToStream(path, chunkName, dos);
		return chunkName + ":" + connectClientAddress;
	}
}
