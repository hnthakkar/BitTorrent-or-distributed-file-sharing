package iiit.os.client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import iiit.os.client.file.FileHelper;
import iiit.os.client.request.ClientToClientHelper;
import iiit.os.client.request.ClientToClientRequestSender;
import iiit.os.client.request.GroupRequestHandler;
import iiit.os.client.request.UserRequestHandler;
import iiit.os.client.socket.ClientServerSocket;

/**
 * Created by hnthakka on 02-Oct-19.
 */
public class Client {
	private static ClientServerSocket clientServerSocket;

	private static final String CONFIG_FILE = "./config.properties";  
	public static int CHUNK_SIZE;
	public static String clientIP;
	public static int PORT;
	public static String chunkDir;
	public static String downloadDir;
	public static int MULTIPLICITY_FACTOR;
	
	public static List<String> trackerAddress = new ArrayList<String>();
	
	public static String userName = null;
	public static String password = null;

	public static void main(String[] arg) {

		try {
			Client client = new Client();
			client.initailize();
			Client.startClientServerSocket();
			client.serverClient();
		} catch (Exception e) {

		}

	}

	public void initailize() {
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
		       			clientIP = address[0];
		       			break;
		       		}
		       		case "TRACKERADDRESS": {
		       			trackerAddress.add(parts[1]);
		       			break;
		       		}
		       		case "CHUNKSIZE": {
		       			int sizeInKB = Integer.parseInt(parts[1]);
		       			CHUNK_SIZE = sizeInKB * 1024;
		       			break;
		       		}
		       		case "CHUNKDIR": {
		       			chunkDir = parts[1];
		       			break;
		       		}
		       		case "DOWNLOADDIR": {
		       			downloadDir = parts[1];
		       			break;
		       		}
		       		case "MULTIPLICITYFACTOR": {
		       			MULTIPLICITY_FACTOR = Integer.parseInt(parts[1]);
		       			break;
		       		}
		       }
		    }
		} catch (Exception e) {
			System.out.println("Config file does not exists, program will terminate");
			System.exit(0);
		}
		
		createDir(chunkDir);
		createDir(downloadDir);

	}
	
	private void createDir(String path) {
		File dir = new File(path);
		if (!dir.exists()) {
			dir.mkdir();
		}
	}

	public void serverClient() throws Exception {
		Scanner scan = new Scanner(System.in);
		boolean stopSignal = false;
		 
		while (true) {
			try {
				String[] userInput = scan.nextLine().split(" ");
				if (userInput.length == 0) {
					System.out.println("Something wrong");
				}
				String command = userInput[0].toLowerCase();

				switch (command) {
					case "create_user": {
						// user Input pattern Followed "create_user userName password"
						// System.out.println("Received create user request ");
						createUser(userInput[1], userInput[2]);
						break;
					}
					case "login": {
						// user Input pattern Followed "login userName password"
						// System.out.println("Received create user request ");
						login(userInput[1], userInput[2]);
						break;
					}
					case "logout": {
						logout();
						break;
					}
					case "create_group": {
						// user Input pattern Followed "create_group groupName"
						if (Client.userName == null) {
							System.out.println("Please Login first");
							return;
						}
						createGroup(userInput[1]);
						break;
					}
					case "join_group": {
						// user Input pattern Followed "create_group groupName"
						if (Client.userName == null) {
							System.out.println("Please Login first");
							return;
						}
						joinGroup(userInput[1]);
						break;
					}
					case "leave_group": {
						// user Input pattern Followed "create_group groupName"
						if (Client.userName == null) {
							System.out.println("Please Login first");
							return;
						}
						leaveGroup(userInput[1]);
						break;
					}
					case "list_groups": {
						// user Input pattern Followed "create_group groupName"
						if (Client.userName == null) {
							System.out.println("Please Login first");
							return;
						}
						listGroup();;
						break;
					}
					case "list_requests": {
						// user Input pattern Followed "create_group groupName"
						if (Client.userName == null) {
							System.out.println("Please Login first");
							return;
						}
						listPendingRequest();
						break;
					}
					case "accept_request": {
						// user Input pattern Followed "accept_request groupName userName"
						if (Client.userName == null) {
							System.out.println("Please Login first");
							return;
						}
						if (userInput.length ==3) {
							acceptpendingRequest(userInput[2], userInput[1]);
						} else {
							System.out.println("Please follow the format: 'accept_request groupName userName'");
						}
						
						break;
					}
					case "active_clients": {
						// user Input pattern Followed "upload fileName"
						if (Client.userName == null) {
							System.out.println("Please Login first");
							return;
						}
						activeClient();
						break;
					}
					case "group_users": {
						// user Input pattern Followed "upload fileName"
						if (Client.userName == null) {
							System.out.println("Please Login first");
							return;
						}
						groupUsers(userInput[1]);
						break;
					}
					case "download_file": {
						// user Input pattern Followed "download fileName"
						if (Client.userName == null) {
							System.out.println("Please Login first");
							return;
						}
						//System.out.println("Received Download request from client :" + userInput[1]);
						downloadFile(userInput[1]);
						break;
					}
					case "upload_file": {
						// user Input pattern Followed "upload_file completePath"
						if (Client.userName == null) {
							System.out.println("Please Login first");
							return;
						}
						//System.out.println("Received upload request from client :" + userInput[1]);
						uploadFile(userInput[1], userInput[2]);
						break;
					}
					case "file_details": {
						// user Input pattern Followed "file_details fileName"
						if (Client.userName == null) {
							System.out.println("Please Login first");
							return;
						}
						fileDetails(userInput[1], new HashMap<String, List<String>>());
						break;
					}
					case "list_files": {
						// user Input pattern Followed "file_details fileName"
						if (Client.userName == null) {
							System.out.println("Please Login first");
							return;
						}
						listFileOfGroup(userInput[1]);
						break;
					}
					case "stop_share": {
						// user Input pattern Followed "file_details fileName"
						if (Client.userName == null) {
							System.out.println("Please Login first");
							return;
						}
						stopSharing(userInput[1], userInput[2]);
						break;
					}
					case "show_downloads": {
						// user Input pattern Followed "show_downloads"
						if (Client.userName == null) {
							System.out.println("Please Login first");
							return;
						}
						showDownloads();
						break;
					}
					case "exit": {
						//stopClientServerSocket();
						stopSignal = true;
						break;
					}
					default: {
						System.out.println("Please Enter a valid Command");
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
	
	private void showDownloads() throws Exception {
		File downloadDir = new File(Client.downloadDir);
		if(downloadDir.exists() && downloadDir.isDirectory()) { 
            File[] files = downloadDir.listFiles(); 
            for (File file : files) {
            	System.out.println(file.getName());
            }
       }  
	}

	private void createUser(String userName, String password) throws Exception {
		if (ClientToClientHelper.isNullOrEmpty(userName) || ClientToClientHelper.isNullOrEmpty(password)) {
			System.out.println("Username or password cannot be null");
			return;
		}
		
		new Thread(new UserRequestHandler(UserRequestHandler.CREATE_USER_REQ, userName, password, trackerAddress)).start();
	}
	
	private void activeClient() throws Exception {
		new Thread(new UserRequestHandler(UserRequestHandler.ACTIVE_CLIENTS, userName, password, trackerAddress)).start();
	}
	
	private void groupUsers(String groupName) throws Exception {
		if (ClientToClientHelper.isNullOrEmpty(groupName)) {
			System.out.println("Group Name cannot be null");
			return;
		}
		new Thread(new GroupRequestHandler(GroupRequestHandler.GROUP_USERS, groupName, userName, password, trackerAddress)).start();
	}
	
	private void listFileOfGroup(String groupName) throws Exception {
		if (ClientToClientHelper.isNullOrEmpty(groupName)) {
			System.out.println("Group Name cannot be null");
			return;
		}
		new Thread(new GroupRequestHandler(GroupRequestHandler.LIST_FILES_IN_GROUP, groupName, userName, password, trackerAddress)).start();
	}
	
	private void stopSharing(String groupName, String fileName) throws Exception {
		if (ClientToClientHelper.isNullOrEmpty(groupName) || ClientToClientHelper.isNullOrEmpty(fileName)) {
			System.out.println("Group Name or the File name cannot be null");
			return;
		}
		new Thread(new GroupRequestHandler(GroupRequestHandler.STOP_SHARING, (groupName + ":" + fileName), userName, password, trackerAddress)).start();
	}
	
	private void createGroup(String groupName) throws Exception {
		if (ClientToClientHelper.isNullOrEmpty(groupName)) {
			System.out.println("Group Name cannot be null");
			return;
		}
		new Thread(new GroupRequestHandler(GroupRequestHandler.CREATE_GROUP_REQ, groupName, userName, password, trackerAddress)).start();
	}
	
	private void joinGroup(String groupName) throws Exception {
		if (ClientToClientHelper.isNullOrEmpty(groupName)) {
			System.out.println("Group Name cannot be null");
			return;
		}
		new Thread(new GroupRequestHandler(GroupRequestHandler.JOIN_GROUP_REQ, groupName, userName, password, trackerAddress)).start();
	}
	
	private void leaveGroup(String groupName) throws Exception {
		if (ClientToClientHelper.isNullOrEmpty(groupName)) {
			System.out.println("Group Name cannot be null");
			return;
		}
		new Thread(new GroupRequestHandler(GroupRequestHandler.LEAVE_GROUP_REQ, groupName, userName, password, trackerAddress)).start();
	}
	
	private void acceptpendingRequest(String userToBeAdded, String groupName) throws Exception {
		if (ClientToClientHelper.isNullOrEmpty(groupName) || ClientToClientHelper.isNullOrEmpty(userToBeAdded)) {
			System.out.println("User to be added or Group request, cannot be null");
			return;
		}
		new Thread(new GroupRequestHandler(GroupRequestHandler.ACCEPT_PENDING_REQ, (userToBeAdded + ":" + groupName), userName, password, trackerAddress)).start();
	}
	
	private void listPendingRequest() throws Exception {
		new Thread(new GroupRequestHandler(GroupRequestHandler.LIST_PENDING_REQ, "", userName, password, trackerAddress)).start();
	}
	
	private void listGroup() throws Exception {
		new Thread(new GroupRequestHandler(GroupRequestHandler.LIST_GROUP_REQ, "", userName, password, trackerAddress)).start();
	}
	
	private void login(String userName, String password) throws Exception {
		if (ClientToClientHelper.isNullOrEmpty(userName) || ClientToClientHelper.isNullOrEmpty(password)) {
			System.out.println("Username or password cannot be null");
			return;
		}
		new Thread(new UserRequestHandler(UserRequestHandler.LOGIN_REQ, userName, password, trackerAddress)).start();
	}
	
	private void logout() throws Exception {
		if (ClientToClientHelper.isNullOrEmpty(userName) || ClientToClientHelper.isNullOrEmpty(password)) {
			System.out.println("User is already logged out");
			return;
		}
		
		new Thread(new UserRequestHandler(UserRequestHandler.LOGOUT_REQ, userName, password, trackerAddress)).start();
	}
	
	public static void startClientServerSocket() {
		clientServerSocket = new ClientServerSocket(PORT);
		new Thread(clientServerSocket).start();
	}

	public static void stopClientServerSocket() {
		clientServerSocket.closeServerSocket();
	}
	
	private boolean mergeChunks(String fileName, int noOfChunks) {
		try {
			FileHelper.mergeChunks(fileName, noOfChunks);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private void downloadFile(String fileName) throws Exception {
		// Step 1: get the file metadata from the tracker
		HashMap<String, List<String>> chunkMap = new HashMap<String, List<String>>();
		HashMap<String, String> fileMetadata = fileDetails(fileName, chunkMap);
		
		// Step 2: download the chunks
		int noOfChunks = Integer.parseInt(fileMetadata.get("noOfChunks"));
		ExecutorService service = Executors.newFixedThreadPool(10);
		List<Future<String>> list = new ArrayList<Future<String>>();
        
		for (int i = 1; i <= noOfChunks; i++) {
			String chunkName = fileName + "_" + i;
			// check if chunks exists locally
			File chunkFile = new File(chunkDir + chunkName);
			if (chunkFile.exists()) {
				continue;
			}
			List<String> addresses = chunkMap.get(chunkName);
			//Currently using the first address
			String address = addresses.get(0);
			Future<String> future = service.submit(new ClientToClientRequestSender(chunkName, address,
					ClientToClientHelper.downloadRequestType, addresses));
			list.add(future);
		}
		
		StringBuilder hashOfChunksCombined = new StringBuilder();
		for(Future<String> future : list){
            try {
            	hashOfChunksCombined.append(future.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
		System.out.println("****** All Chunks downloaded *********");
		service.shutdown();
		
		// Merge the chunks
		if (mergeChunks(fileName, noOfChunks)) {
			System.out.println("File downloaded");
			// Validate the file hash
			try {
				System.out.println("Valication the file with the original Hash");
				FileHelper.validateFile(hashOfChunksCombined.toString().getBytes(), fileMetadata.get("fileHash"));
				System.out.println("File Successfully validated!!");
			} catch(Exception e) {
				System.out.println("Hash did not match");
			}
			
		} else {
			System.out.println("Problem merging the file");
		}
	}

	private HashMap<String, String> fileDetails(String fileName, HashMap<String, List<String>> chunkClientMap) throws Exception {
		if (ClientToClientHelper.isNullOrEmpty(fileName)) {
			System.out.println("File name cannot be null");
			return null;
		}
		
		Socket socket = null;
		DataOutputStream dos = null;
		DataInputStream din = null;
		HashMap<String, String> fileMetadata = null;
		try {
			socket = getActiveTrackerSocket();
			dos = new DataOutputStream(socket.getOutputStream());
			din = new DataInputStream(socket.getInputStream());
			StringBuilder sb = new StringBuilder();
			sb.append("Client").append("@");
			sb.append("file_details").append("#");
			sb.append(userName).append(":").append(password).append(":").append(fileName);
			dos.writeUTF(sb.toString());
			String response = din.readUTF();
			if ("ERROR".equals(response)) {
				System.out.println("Either File does not exists or User does not have rights to access this File");
				throw new Exception();
			}
			
			if (response == null || "".equals(response)) {
				System.out.println("Unable to fetch File details");
				throw new Exception();
			}
			
			fileMetadata = new HashMap<String, String>(); 
			/**
			 * fileName:fileHash:noOfChunks@c1:c1hash%ip:port%ip:port%ip2:port2@c2...
			 */
			String[] parts = response.split("@");
			String[] fileparts = parts[0].split(":");
			String fileNamep = fileparts[0];
			String fileHash = fileparts[1];
			int noOfChunks = Integer.parseInt(fileparts[2]);
			System.out.println("File details from the tracker:");
			System.out.println("Filename : " + fileNamep);
			System.out.println("Filehash : " + fileHash);
			System.out.println("noOfChunks : " + noOfChunks);
			fileMetadata.put("fileName", fileNamep);
			fileMetadata.put("fileHash", fileHash);
			fileMetadata.put("noOfChunks", noOfChunks + "");
			
			for (int i = 1; i < parts.length; i++) {
				String[] chunkparts = parts[i].split("%");
				String chunkName = chunkparts[0].split(":")[0];
				String chunkHash = chunkparts[0].split(":")[1];
				fileMetadata.put(chunkName, chunkHash);
				List<String> mapping = new ArrayList<String>();
				chunkClientMap.put(chunkName, mapping);
				for (int j = 1; j < chunkparts.length; j++) {
					String address = chunkparts[j];
					mapping.add(address);
					System.out.println(chunkName + ":" + chunkHash + ":" + address);
				}
			}
		} catch (Exception e) {
			System.out.println("Problem while establishing socket with the tracker");
		} finally {
			try {
				din.close();
				dos.close();
				socket.close();
			} catch (Exception e) {
				System.out.println("Problem while closing the Client to Tracker resources");
			}
		}
		return fileMetadata;
	}
		
	private void uploadFile(String completePath, String groupName) throws Exception {
		if (ClientToClientHelper.isNullOrEmpty(completePath) || ClientToClientHelper.isNullOrEmpty(groupName)) {
			System.out.println("File name and group name cannot be null");
			return;
		}
		
		// Step 1 : Check if file by locally exists and accessible
		File file = new File(completePath);
		if (!file.exists()) {
			System.out.println("File Not Found!!");
			return;
		}
		
		// Step 2 : Check if file by this name already uploaded (also get the active users)
		String fileName = file.getName();
		Socket socket = null;
		DataOutputStream dos = null;
		DataInputStream din = null;
		String[] activeUsers = null;
		try {
			socket = getActiveTrackerSocket();
			dos = new DataOutputStream(socket.getOutputStream());
			din = new DataInputStream(socket.getInputStream());
			StringBuilder sb = new StringBuilder();
			sb.append("Client").append("@");
			sb.append("file_exists").append("#");
			sb.append(userName).append(":").append(password).append(":").append(fileName);
			dos.writeUTF(sb.toString());
			String response = din.readUTF();
			if ("ERROR".equals(response)) {
				System.out.println("File by this name already exists");
				return;
			}
			
			if (response == null || "".equals(response)) {
				System.out.println("File cannot be uploaded currently no other active clients");
				return;
			}
			activeUsers = response.split("@");
			for (String user : activeUsers) {
				System.out.println(user);
			}
		} catch (Exception e) {
			System.out.println("Problem while establishing socket with the tracker");
		} finally {
			try {
				din.close();
				dos.close();
				socket.close();
			} catch (Exception e) {
				System.out.println("Problem while closing the Client to Tracker resources");
			}
		}
		
		// Step 3 : locally Split the file first and get and its metadata
		Map<String, String> fileMetadata = null;
		try {
			fileMetadata = FileHelper.splitFile(file);
		} catch (Exception e) {
			System.out.println("Error while splitting the file");
		}
		
		// Step 4 : upload the chunks
		/**
		 * MULTIPLICITY_FACTOR = 2, meaning each chunk is present with two client,
		 * so even if one client is down, chunk can be reached/download from another client
		 * while uploading chunks are already created locally, hence reducing factor by one 
		 */
		int noOfChunks = Integer.parseInt(fileMetadata.get("noOfChunks"));
		ExecutorService service = Executors.newFixedThreadPool(10);
		List<String> taskList = listAllTheTasks(fileName, noOfChunks, 0, activeUsers.length -1, MULTIPLICITY_FACTOR - 1);
		
		List<Future<String>> list = new ArrayList<Future<String>>();
		for (String task: taskList) {
			String[] parts = task.split(":");
			String chunkName = parts[0];
			String client = activeUsers[Integer.parseInt(parts[1])];
			String[] hostNport = (client.split("#"))[1].split(":");
			String hostaddress = hostNport[0];
			int port = Integer.parseInt(hostNport[1]);
			List<String> activeUserList = new ArrayList<String>();
			activeUserList.addAll(Arrays.asList(activeUsers));
			Future<String> future = service.submit(new ClientToClientRequestSender(chunkName, (hostaddress + ":" + port),
					ClientToClientHelper.uploadRequestType, activeUserList));
			list.add(future);
		}
		HashMap<String,List<String>> chunkMap = new HashMap<>();
		for(Future<String> future : list){
            try {
            	String chunkUploadInfo = future.get();
            	String[] chunkInfoparts = chunkUploadInfo.split(":");
            	String chunkName = chunkInfoparts[0];
            	String uploadedHost = chunkInfoparts[1];
            	String uploadedPort = chunkInfoparts[2];
            	if (!chunkMap.containsKey(chunkName)) {
    				chunkMap.put(chunkName, new ArrayList<String>());
    			}
    			List<String> chunkToClient = chunkMap.get(chunkName);
    			chunkToClient.add(uploadedHost + ":" + uploadedPort);
    		} catch (Exception e) {
                e.printStackTrace();
            }
        }
		service.shutdown();
		System.out.println("****** File Uploaded Successfully *********");
		
		// Step 5 : upload the chunks
		/**
		 * Once chunks are uploaded to other client's
		 * update the tracker with the Metadata about the file, So that any client can download it
		 * 1) FileName: Owner of the file: HashOfTheFile
		 * 2) group in which it should be available
		 * 3) No of chunks the file has
		 * 4) Details about the chunk i.e. chunk name, hash of chunk, clients with whom the chunk is available
		 */
		updateFileInfoToTracker(fileMetadata, groupName, chunkMap);
		
	}
	
	private void updateFileInfoToTracker (Map<String, String> fileMetadata, String groupName, HashMap<String, List<String>> chunkMap) {
		String fileName = fileMetadata.get("fileName");
		int noOfChunks= Integer.parseInt(fileMetadata.get("noOfChunks"));
		String fileHash = fileMetadata.get("fileHash");
		String fileOwner = userName;
		/**
		 * * Format -> fileName:fileOwner:groupName:fileHash:noOfChunks&c1:c1hash$ip:port$ip:port*c2...
		 */
		
		Socket socket = null;
		DataOutputStream dos = null;
		DataInputStream din = null;
		try {
			socket = getActiveTrackerSocket();
			dos = new DataOutputStream(socket.getOutputStream());
			din = new DataInputStream(socket.getInputStream());
			StringBuilder sb = new StringBuilder();
			sb.append("Client").append("@");
			sb.append("upload_file").append("#");
			sb.append(userName).append(":").append(password).append("!");
			sb.append(fileName).append(":").append(fileOwner).append(":").append(groupName).append(":");
			sb.append(fileHash).append(":").append(noOfChunks).append("&");
			 
			for (int i = 1; i <= noOfChunks; i++) {
				String currentChunk = fileName + "_" + i;
				String currentChunkHash = fileMetadata.get(currentChunk);
				sb.append(currentChunk).append(":").append(currentChunkHash).append("-");
				//while uploading one chunk copy is kept locally, hence
				sb.append(clientIP + ":" + PORT).append("-");
				List<String> chunkUploadedTo = chunkMap.get(currentChunk);
				for (String address : chunkUploadedTo) {
					sb.append(address).append("-");
				}
				sb.append("%");
			}
			dos.writeUTF(sb.toString());
			String response = din.readUTF();
			if ("ERROR".equals(response)) {
				System.out.println("Error updating file Metadata to the tracker..");
				return;
			} else {
				System.out.println("Successfully updated File Metadata to Tracker");
			}
		} catch (Exception e) {
			System.out.println("Problem while establishing socket with the tracker");
		} finally {
			try {
				din.close();
				dos.close();
				socket.close();
			} catch (Exception e) {
				System.out.println("Problem while closing the Client to Tracker resources");
			}
		}
	}
	
	private Socket getActiveTrackerSocket() throws Exception {
		Socket socket = null; 
		for (String trackerAddress : trackerAddress) {
			String[] parts = trackerAddress.split(":");
			String ip = parts[0];
			int port = Integer.parseInt(parts[1]);
			try {
				socket = new Socket(ip, port);
				if (socket != null) {
					break;
				}
			} catch (Exception e) {
				// do nothing continue;
			}
		}
		
		if (socket == null) {
			System.out.println("No Active tracker");
			throw new Exception();
		} 
		return socket;
	}
	
	private List<String> listAllTheTasks(String fileName, int noOfChunks, int minNumber, int maxNumber, int multiplicityFactor) {
		List<String> tasks = new ArrayList<>();
		for (int i = 1; i <= noOfChunks; i++) {
			String currentChunkName = fileName + "_" + i;
			List<String> chunkAllocation = allocatedChunkToClients(currentChunkName, minNumber, maxNumber, multiplicityFactor);
			tasks.addAll(chunkAllocation);
		}
		return tasks;
	}
	
	private List<String> allocatedChunkToClients(String chunkName, int minNumber, int maxNumber, int multiplicityFactor) { 
		List<String> clientIndex = new ArrayList<>();
		if (multiplicityFactor >= (maxNumber - minNumber + 1)) {
			// multiplicity factor is more than the clients available, adding all the available client
			for (int i = minNumber; i <= maxNumber; i++) {
				clientIndex.add(chunkName + ":" + i);
			}
			return clientIndex;
		}
		
		Random r = new Random();
		while (clientIndex.size() < multiplicityFactor) {
			int random = r.nextInt((maxNumber - minNumber) + 1) + minNumber;
			if (clientIndex.contains(random)) {
				continue;
			}
			clientIndex.add(chunkName + ":" +random);
		}
		return clientIndex;
	}
}
