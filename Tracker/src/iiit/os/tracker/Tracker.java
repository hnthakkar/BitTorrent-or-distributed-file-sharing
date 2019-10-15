package iiit.os.tracker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import iiit.os.tracker.pojo.ChunkInfo;
import iiit.os.tracker.pojo.FileInfo;
import iiit.os.tracker.pojo.GroupInfo;
import iiit.os.tracker.pojo.UserInfo;
import iiit.os.tracker.request.TrackerToTrackerSyncSender;
import iiit.os.tracker.socket.TrackerServerSocket;

public class Tracker {
	
	private static final String CONFIG_FILE = "./config.properties";  
	
	private TrackerServerSocket trackerServerSocket;
	
	public static int PORT;
	public static String trackerIP;
	public static List<String> otherTrackerAddress = new ArrayList<String>();
	
	
	public static ConcurrentHashMap<String, UserInfo> userInfo = new ConcurrentHashMap<>();
	public static ConcurrentHashMap<String, FileInfo> fileInfo = new ConcurrentHashMap<>();
	public static ConcurrentHashMap<String, GroupInfo> groupInfo = new ConcurrentHashMap<>();
	public static ConcurrentHashMap<String, ChunkInfo> chunkInfo = new ConcurrentHashMap<>();
	public static ConcurrentHashMap<String, String> activeClients = new ConcurrentHashMap<>();
	public static ConcurrentHashMap<String, List<String>> pendingRequest = new ConcurrentHashMap<>();

	public static void main(String[] arg) {
		try {
			Tracker tracker = new Tracker();
			tracker.initailize();
			tracker.startTrackerServerSocket(PORT);
			tracker.startSyncSignalThread();
			tracker.syncAllRequest();
			tracker.serverUserInput();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void initailize() throws Exception {
		File config = new File(CONFIG_FILE);
		if (!config.exists()) {
			System.out.println("Config file does not exists, program will terminate");
			System.exit(0);
		}
		
		try (BufferedReader br = new BufferedReader(new FileReader(CONFIG_FILE))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		       String[] parts = line.split("#");
		       switch(parts[0]) {
		       		case "MYADDRESS": {
		       			String[] address = parts[1].split(":");
		       			PORT = Integer.parseInt(address[1]);
		       			trackerIP = address[0];
		       			break;
		       		}
		       		case "OTHERTRACKERADDRESS": {
		       			otherTrackerAddress.add(parts[1]);
		       			break;
		       		}
		       }
		    }
		} catch (Exception e) {
			System.out.println("Config file does not exists, program will terminate");
			System.exit(0);
		}
	}
	
	public void serverUserInput() throws Exception {
		Scanner scan = new Scanner(System.in);
		boolean stopSignal = false;
		 
		while (true) {
			try {
				String command = scan.nextLine().toLowerCase();
				switch (command) {
					case "quit": {
						stopSignal = true;
						break;
					}
					case "syncAll": {
						// Manually initiating sync from 
						// TODO
						break;
					}
				}
				if (stopSignal) {
					scan.close();
					break;
				}
			} catch (Exception e) {
				System.out.println("Invalid command. Please try again");
			}	
		}
	}
	
	public void syncAllRequest() {
		TrackerToTrackerSyncSender.addToSyncQueue("Tracker@syncAll");
	}
	
	public void startSyncSignalThread() {
		(new Thread(new TrackerToTrackerSyncSender(otherTrackerAddress))).start();
	}
	
	public void startTrackerServerSocket(int port) {
		trackerServerSocket = new TrackerServerSocket(port);
		(new Thread(trackerServerSocket)).start();
	}
	
	public void stopTrackerServerSocket() {
		trackerServerSocket.closeServerSocket();
	}
}
