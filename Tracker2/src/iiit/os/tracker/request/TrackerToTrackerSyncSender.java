package iiit.os.tracker.request;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TrackerToTrackerSyncSender implements Runnable {

	private List<String> otherTrackers;
	private static ConcurrentLinkedQueue<String> pendingSyncReq = new ConcurrentLinkedQueue<String>();
	
	public TrackerToTrackerSyncSender(List<String> otherTrackers) {
		this.otherTrackers = otherTrackers;
	}
	
	@Override
	public void run() {
		String currentReq = null;
		try {
			while (true) {
				if (pendingSyncReq.isEmpty()) {
					Thread.sleep(500);
					continue;
				}
				currentReq = pendingSyncReq.poll();
				sendSynUpdate(otherTrackers, currentReq);
			}
		} catch (Exception e) {
			
		}
	}
	
	public static void addToSyncQueue(String request) {
		pendingSyncReq.add(request);
	}
	
	public boolean sendSynUpdate(List<String> trackerList, String update) {
		Socket socket = null;
		DataOutputStream dos = null;
		DataInputStream din = null;
		boolean allInSync = true;
		for (String tracker : trackerList) {
			try {
				String[] parts = tracker.split(":");
				String trackerIP = parts[0];
				int trackerPort = Integer.parseInt(parts[1]);
				socket = new Socket(trackerIP, trackerPort);
				System.out.println("Socket Connection established");
				dos = new DataOutputStream(socket.getOutputStream());
				din = new DataInputStream(socket.getInputStream());
				StringBuilder sb = new StringBuilder();
				//sb.append("Tracker").append("@");
				sb.append(update);
				dos.writeUTF(sb.toString());
				String response = din.readUTF();
				if ("ERROR".equals(response)) {
					System.out.println("Sync Error");
					allInSync = false;
				} else {
					System.out.println("Sync successful");
				}
			} catch (Exception e) {
				System.out.println("Other Tracker NOT alive");
				//e.printStackTrace();
				allInSync = false;
			} finally {
				try {
					din.close();
					dos.close();
					socket.close();
				} catch (Exception e) {
					//System.out.println("Problem while closing the resources");
				}
			}
		}
		return allInSync;
	}
}
