package iiit.os.tracker.request;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import iiit.os.tracker.Tracker;
import iiit.os.tracker.pojo.ChunkInfo;
import iiit.os.tracker.pojo.FileInfo;
import iiit.os.tracker.pojo.GroupInfo;
import iiit.os.tracker.pojo.UserInfo;

public class TrackerRequestHandler implements Runnable {

	private Socket socket;
	private DataInputStream din;
	private DataOutputStream dos;
	
	private String requestFromUser;

	/**
	 * Request from Client for File info, sync request from other trackers
	 */
	public TrackerRequestHandler(Socket socket, DataInputStream din, DataOutputStream dos) {
		this.socket = socket;
		this.din = din;
		this.dos = dos;
	}

	@Override
	public void run() {
		try {
			/**
			 * request format
			 * 1) tracker@command...
			 * 2) client@command...
			 */
			String request = din.readUTF();
			String[] requestparts = request.split("@");
			if(requestparts.length == 2) {
				String commandFrom = requestparts[0];
				if (commandFrom.equalsIgnoreCase("Tracker")) {
					// its a sync request from another tracker
					processSyncRequest(requestparts[1]);
				} else {
					// its a request from the client
					processClientRequest(requestparts[1]);
				}
			} else {
				throw new Exception();
			}
		} catch (Exception e) {
			//System.out.println("Connection reset");
			//e.printStackTrace();
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
	
	private void processSyncRequest(String commandString) {
		String[] command = commandString.split("!");
		switch(command[0]) {
			case "activeClientAdd" : {
				try {
					addActiveUser(command[1]);
					dos.writeUTF("OK");
				} catch (Exception e) {
					writeErrorAck();
				}
				break;
			}
			case "activeClientRemove" : {
				try {
					removeActiveUser(command[1]);
					dos.writeUTF("OK");
				} catch (Exception e) {
					writeErrorAck();
				}
				break;
			}
			case "groupInfo" : {
				try {
					GroupInfo groupInfo = GroupInfo.getObjectFromString(command[1]);
					Tracker.groupInfo.put(groupInfo.getGroupName(), groupInfo);
					dos.writeUTF("OK");
				} catch (Exception e) {
					writeErrorAck();
				}
				break;
			}
			case "userInfo" : {
				try {
					UserInfo userInfo = UserInfo.getObjectFromString(command[1]);
					Tracker.userInfo.put(userInfo.getUserName(), userInfo);
					dos.writeUTF("OK");
				} catch (Exception e) {
					writeErrorAck();
				}
				break;
			}
			case "chunkInfo" : {
				try {
					ChunkInfo chunkInfo = ChunkInfo.getObjectFromString(command[1]);
					Tracker.chunkInfo.put(chunkInfo.getChunkName(), chunkInfo);
					dos.writeUTF("OK");
				} catch (Exception e) {
					writeErrorAck();
				}
				break;
			}
			case "pendingReq" : {
				try {
					updatePendingrequest(command[1]);
					dos.writeUTF("OK");
				} catch (Exception e) {
					writeErrorAck();
				}
				break;
			}
			case "fileInfo" : {
				try {
					FileInfo fileInfo = FileInfo.getObjectFromString(command[1]);
					Tracker.fileInfo.put(fileInfo.getFileName(), fileInfo);
					dos.writeUTF("OK");
				} catch (Exception e) {
					writeErrorAck();
				}
				break;
			}
			case "fileInfoRemove" : {
				try {
					Tracker.fileInfo.remove(command[1]);
					dos.writeUTF("OK");
				} catch (Exception e) {
					writeErrorAck();
				}
				break;
			}
			case "syncAll" :{
				try {
					putAllInUpdateQueue();
					dos.writeUTF("OK");
				} catch (Exception e) {
					writeErrorAck();
				}
				break;
			}
		}
	}
	
	public void putAllInUpdateQueue() {
		Iterator<UserInfo> userInfoItr = Tracker.userInfo.values().iterator();
		while (userInfoItr.hasNext()) {
			UserInfo userInfo = userInfoItr.next();
			TrackerToTrackerSyncSender.addToSyncQueue("Tracker@userInfo!" + userInfo.toString());
		}
		
		Iterator<GroupInfo> groupInfoItr = Tracker.groupInfo.values().iterator();
		while (groupInfoItr.hasNext()) {
			GroupInfo groupInfo = groupInfoItr.next();
			TrackerToTrackerSyncSender.addToSyncQueue("Tracker@groupInfo!" + groupInfo.toString());
		}
		
		Iterator<ChunkInfo> chunkInfoItr = Tracker.chunkInfo.values().iterator();
		while (chunkInfoItr.hasNext()) {
			ChunkInfo chunkInfo = chunkInfoItr.next();
			TrackerToTrackerSyncSender.addToSyncQueue("Tracker@chunkInfo!" + chunkInfo.toString());
		}
		
		Iterator<FileInfo> fileInfoItr = Tracker.fileInfo.values().iterator();
		while (fileInfoItr.hasNext()) {
			FileInfo fileInfo = fileInfoItr.next();
			TrackerToTrackerSyncSender.addToSyncQueue("Tracker@fileInfo!" + fileInfo.toString());
		}
		
		//active clients
		Iterator<Map.Entry<String, String>> activeClientsItr = Tracker.activeClients.entrySet().iterator();
		while (activeClientsItr.hasNext()) {
			Map.Entry<String, String> entry = activeClientsItr.next();
			String userName = entry.getKey();
			String address = entry.getValue();
			TrackerToTrackerSyncSender.addToSyncQueue("Tracker@activeClientAdd!" + (userName + "#" + address));
		}
		
		// All user pending request to be approved
		Iterator<String> pendingReqItr = Tracker.pendingRequest.keySet().iterator();
		while (pendingReqItr.hasNext()) {
			String groupOwner = pendingReqItr.next();
			TrackerToTrackerSyncSender.addToSyncQueue("Tracker@pendingReq!" + convertPendingRequestToString(groupOwner));
		}
	}
	
	private void writeErrorAck() {
		try {
			dos.writeUTF("ERROR");
		} catch (Exception e) {
			// Nothing
		}
	}
	
	private void updatePendingrequest(String command) {
		String[] parts = command.split("#");
		if (parts.length == 2) {
			String[] reqs = parts[1].split("%");
			List<String> reqList = new ArrayList<String>();
			if (reqs.length > 0) {
				reqList.addAll(Arrays.asList(reqs));
			}
			Tracker.pendingRequest.put(parts[0], reqList);
		}
	}
	
	public String convertPendingRequestToString(String userName) {
		StringBuilder sb = new StringBuilder();
		sb.append(userName).append("#");
		List<String> requests = Tracker.pendingRequest.get(userName);
		for (String request : requests) {
			sb.append(request).append("%");
		}
		return sb.toString();
	}
	
	private void addActiveUser(String command) {
		String[] parts = command.split("#");
		Tracker.activeClients.put(parts[0], parts[1]);
	}
	
	private void removeActiveUser(String command) {
		Tracker.activeClients.remove(command);
	}
	
	private void processClientRequest(String commandString) throws Exception{
		if (RequestHelper.isNullOrEmpty(commandString)) {
			throw new Exception();
		}
		
		/**
		 * format command#arguments...
		 */
		String[] parts = commandString.split("#");
		
		String command = parts[0].toLowerCase();
		switch (command) {
			case "create_user": {
				createUser(parts[1]);
				break;
			}
			case "login": {
				login(parts[1]);
				break;
			}
			case "create_group": {
				createGroup(parts[1]);
				break;
			}
			case "join_group": {
				joinGroup(parts[1]);
				break;
			}
			case "leave_group": {
				leaveGroup(parts[1]);
				break;
			}
			case "list_requests": {
				listRequest(parts[1]);
				break;
			}
			case "accept_request": {
				acceptRequest(parts[1]);
				break;
			}
			case "list_groups": {
				listGroup(parts[1]);
				break;
			}
			case "list_files": {
				listFiles(parts[1]);
				break;
			}
			case "upload_file": {
				uploadFile(parts[1]);
				break;
			}
			case "file_details": {
				getFileDetails(parts[1]);
				break;
			}
			case "logout": {
				logout(parts[1]);
				break;
			}
			case "stop_share": {
				stopShare(parts[1]);
				break;
			}
			case "active_clients": {
				getActiveClients(parts[1]);
				break;
			}
			case "group_users": {
				groupUsers(parts[1]);
				break;
			}
			case "file_exists": {
				fileExists(parts[1]);
				break;
			}
			default: {
				// should not happen, before sending it to the tracker, clients needs to verify it
				dos.writeUTF("ERROR");
			}
		}
	}
	
	private void fileExists(String commandString) throws Exception {
		/**
		 * Format -> username:password:fileName
		 */
		String parts[] = commandString.split(":");
		GroupInfo groupInfo = new GroupInfo(parts[2], parts[0]);
		if (authenticateUser(parts[0], parts[1]) && parts.length == 3 && groupInfo != null) {
			if (Tracker.fileInfo.containsKey(parts[2])) {
				dos.writeUTF("EXISTS");
			} else {
				StringBuilder sb = new StringBuilder();
				sb.append(getActiveClients(true));
				dos.writeUTF(sb.toString());
			}
		} else {
			dos.writeUTF("ERROR");
			throw new Exception("Exception while creating group!!");
		}	
	}
	
	
	public boolean authenticateUser(String userName, String password) {
		UserInfo userInfo = Tracker.userInfo.get(userName);
		if (userInfo != null && userInfo.getPassword().equals(password)) {
			this.requestFromUser = userName;
			return true;
		}
		return false;
	}
	
	public boolean authenticateUser(String commandString) {
		String[] parts = commandString.split(":");
		if (parts.length == 2 && authenticateUser(parts[0], parts[1])) {
			return true;
		}
		return false;
	}
	
	private void login(String commandString) throws Exception {
		// besides authenticating add the client to the active Client available
		/**
		 * Format -> clientHost:clientport:username:password
		 */
		String[] parts = commandString.split(":");
		if (parts.length == 4 && authenticateUser(parts[2], parts[3])) {
			// IP address and port of the current user needs to be put
			// Give the port from where the connect started not to which other should connect
			// needs to be passed by the client, where to connect address:port
			//SocketAddress clientAddress = socket.getRemoteSocketAddress();
			String clientAddress = parts[0] + ":" + parts[1];//socket.getInetAddress().toString(); 
			Tracker.activeClients.put(parts[2], clientAddress);
			TrackerToTrackerSyncSender.addToSyncQueue("Tracker@activeClientAdd!" + (parts[2] + "#" + clientAddress));
			dos.writeUTF("OK");
			return;
		}
		dos.writeUTF("ERROR");
	}
	
	private void logout(String commandString) throws Exception {
		if (authenticateUser(commandString)) {
			Tracker.activeClients.remove(requestFromUser);
			TrackerToTrackerSyncSender.addToSyncQueue("Tracker@activeClientRemove!" + requestFromUser);
			dos.writeUTF("OK");
			return;
		}
		dos.writeUTF("ERROR");
	}
	
	private void createUser(String commandString) throws Exception {
		UserInfo userInfo = RequestHelper.getUserInfoFromString(commandString);
		if (userInfo != null && !Tracker.userInfo.containsKey(userInfo.getUserName())) {
			Tracker.userInfo.put(userInfo.getUserName(), userInfo);
			TrackerToTrackerSyncSender.addToSyncQueue("Tracker@userInfo!" + userInfo.toString());
			dos.writeUTF("OK");
		} else {
			dos.writeUTF("ERROR");
			throw new Exception("Exception while creating user!!");
		}
	}
	
	private void createGroup(String commandString) throws Exception {
		/**
		 * Format -> username:password:groupname
		 */
		String parts[] = commandString.split(":");
		GroupInfo groupInfo = new GroupInfo(parts[2], parts[0]);
		if (authenticateUser(parts[0], parts[1]) && parts.length == 3 && groupInfo != null) {
			groupInfo.addUserToGroup(requestFromUser);
			Tracker.groupInfo.put(groupInfo.getGroupName(), groupInfo);
			TrackerToTrackerSyncSender.addToSyncQueue("Tracker@groupInfo!" + groupInfo.toString());
			dos.writeUTF("OK");
		} else {
			dos.writeUTF("ERROR");
			throw new Exception("Exception while creating group!!");
		}	
	}
	
	private void joinGroup(String commandString) throws Exception {
		/**
		 * Format -> username:password:groupname
		 */
		String parts[] = commandString.split(":");
		if (authenticateUser(parts[0], parts[1]) && parts.length == 3) {
			GroupInfo groupInfo = Tracker.groupInfo.get(parts[2]);
			if (groupInfo != null) {
				String groupOwner = groupInfo.getGroupOwner();
				if (!Tracker.pendingRequest.containsKey(groupOwner)) {
					Tracker.pendingRequest.put(groupOwner, new ArrayList<String>());
				}
				List<String> pendingList = Tracker.pendingRequest.get(groupOwner);
				// userName:requestedGroupName
				pendingList.add(requestFromUser + ":" + parts[2]);
				TrackerToTrackerSyncSender.addToSyncQueue("Tracker@pendingReq!" + convertPendingRequestToString(groupOwner));
				dos.writeUTF("OK");
				return;
			}
		}	
		dos.writeUTF("ERROR");
	}
	
	private void leaveGroup(String commandString) throws Exception {
		/**
		 * Format -> username:password:groupname
		 */
		String parts[] = commandString.split(":");
		if (authenticateUser(parts[0], parts[1]) && parts.length == 3) {
			GroupInfo groupInfo = Tracker.groupInfo.get(parts[2]);
			if (groupInfo != null) {
				groupInfo.removeUserFromGroup(requestFromUser);
				TrackerToTrackerSyncSender.addToSyncQueue("Tracker@groupInfo!" + groupInfo.toString());
				dos.writeUTF("OK");
				return;
			}
		}	
		dos.writeUTF("ERROR");
	}
	
	private void listRequest(String commandString) throws Exception {
		if (authenticateUser(commandString)) {
			List<String> pendingReqList = Tracker.pendingRequest.get(requestFromUser);
			StringBuilder sb = new StringBuilder();
			/**
			 * Format -> u1:g1@u2:g2
			 */
			if (pendingReqList != null) {
				for (String req: pendingReqList) {
					sb.append(req).append("@");
				}
			}
			dos.writeUTF(sb.toString());
		} else {
			dos.writeUTF("ERROR");
			throw new Exception("problem listing pending request");
		}
	}
	
	private void acceptRequest(String commandString) throws Exception {
		/** 
		 * Once the current user who is the owner of the group approves,
		 * add the user to the group User list
		 */
		String[] parts = commandString.split(":");
		if (parts.length == 4 && authenticateUser(parts[0], parts[1])) {
			String userName = parts[2];
			String groupName = parts[3];
			GroupInfo groupInfo = Tracker.groupInfo.get(groupName);
			groupInfo.addUserToGroup(userName);
			TrackerToTrackerSyncSender.addToSyncQueue("Tracker@groupInfo!" + groupInfo.toString());
			List<String> pendingList = Tracker.pendingRequest.get(requestFromUser);
			pendingList.remove(userName + ":" + groupName);
			TrackerToTrackerSyncSender.addToSyncQueue("Tracker@pendingReq!" + convertPendingRequestToString(requestFromUser));
			dos.writeUTF("OK");
		} else {
			dos.writeUTF("ERROR");
			throw new Exception("Accept request not correct");
		}
	}
	
	private void listGroup(String commandString) throws Exception {
		if(authenticateUser(commandString)) {
			StringBuilder sb = new StringBuilder();
			Iterator<String> it = Tracker.groupInfo.keySet().iterator();
			while(it.hasNext()) {
				String key = it.next();
				sb.append(key).append(":");
			}
			dos.writeUTF(sb.toString());
		} else {
			dos.writeUTF("ERROR");
			throw new Exception("Problem listing group");
		}
	}
	
	private void listFiles(String commandString) throws Exception {
		String parts[] = commandString.split(":");
		if (authenticateUser(parts[0], parts[1]) && parts.length == 3) {
			StringBuilder sb = new StringBuilder();
			GroupInfo groupInfo = Tracker.groupInfo.get(parts[2]);
			if (groupInfo != null && groupInfo.isUserInGroup(requestFromUser)) {
				List<String> files = groupInfo.getFilesInGroup();
				for (String file : files) {
					sb.append(file).append(":");
				}
				dos.writeUTF(sb.toString());
			} else {
				dos.writeUTF("ERROR");
				throw new Exception("Either group does not exists or User does not have access");
			}
		}
	}
	
	private void groupUsers(String commandString) throws Exception {
		String parts[] = commandString.split(":");
		if (authenticateUser(parts[0], parts[1]) && parts.length == 3) {
			StringBuilder sb = new StringBuilder();
			GroupInfo groupInfo = Tracker.groupInfo.get(parts[2]);
			List<String> users = groupInfo.getUsersInGroup();
			for (String user : users) {
				sb.append(user).append(":");
			}
			dos.writeUTF(sb.toString());
		} else {
			dos.writeUTF("ERROR");
			throw new Exception("User NOT authenticated or invalid parameters");
		}
	}
	
	private void getActiveClients(String commandString) throws Exception {
		if(authenticateUser(commandString)) {
			String response = getActiveClients();
			dos.writeUTF(response);
		} else {
			dos.writeUTF("ERROR");
			throw new Exception("User NOT authenticated");
		}
	}
	
	private String getActiveClients() {
		return getActiveClients(false);
	}
	
	private String getActiveClients(boolean excludeLoggedInUser) {
		/**
		 * Format -> u1#ip:port@u2#ip:port@
		 */
		StringBuilder sb = new StringBuilder();
		Iterator<Entry<String, String>> it = Tracker.activeClients.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String, String> entry = it.next();
			String userName = entry.getKey();
			String ipNport = entry.getValue();
			if (userName.equals(requestFromUser) && excludeLoggedInUser) { 
				continue; 
			}
			sb.append(userName).append("#");
			sb.append(ipNport).append("@");
		}
		return sb.toString();
	}
	
	/**
	 * After uploading the chunks FileInfo sent by Client 
	 * @param commandString
	 */
	private void uploadFile(String commandString) throws Exception {
		try {
			String commandparts[] = commandString.split("!");
			if (authenticateUser(commandparts[0])) {
				/**
				 * For any new File Uploaded need
				 * FileInfo, entry in GroupInfo, All ChunkInfo
				 * Format -> fileName:fileOwner:groupName:fileHash:noOfChunks&c1:c1hash$ip:port$ip:port*c2...
				 */
				String parts[] = commandparts[1].split("&");
				if (parts.length == 2) {
					String[] fileInfoParts = parts[0].split(":");
					if (fileInfoParts.length == 5) {
						String fileName = fileInfoParts[0];
						String fileOwner = fileInfoParts[1];
						String groupName = fileInfoParts[2];
						String fileHash = fileInfoParts[3];
						int noOfChunks = Integer.parseInt(fileInfoParts[4]);
						FileInfo newFile = new FileInfo(fileName, fileOwner, groupName, fileHash, noOfChunks);
						String[] chunkList = null;
						chunkList = parts[1].split("%");
							
						for(String chunk : chunkList) {
							String[] chunkParts = chunk.split("-");
							String chunkName = chunkParts[0].split(":")[0];
							String chunkHash = chunkParts[0].split(":")[1];
							ChunkInfo chunkInfo = new ChunkInfo(chunkName, fileName, chunkHash); 
							int chunkPartsSize = chunkParts.length; 
							for (int i = 1; i < chunkPartsSize; i ++) {
								String address = chunkParts[i];
								chunkInfo.addChunkLocation(address);
							}
							Tracker.chunkInfo.put(chunkName, chunkInfo);
							TrackerToTrackerSyncSender.addToSyncQueue("Tracker@chunkInfo!" + chunkInfo.toString());
						}
						GroupInfo groupInfo = Tracker.groupInfo.get(groupName);
						groupInfo.addFileToGroup(fileName);
						TrackerToTrackerSyncSender.addToSyncQueue("Tracker@groupInfo!" + groupInfo.toString());
						Tracker.fileInfo.put(fileName, newFile);
						TrackerToTrackerSyncSender.addToSyncQueue("Tracker@fileInfo!" + newFile.toString());
						dos.writeUTF("OK");
						return;
					}
				}
			} else {
				dos.writeUTF("ERROR");
				throw new Exception("User NOT authenticated");
			}
		} catch (Exception e) {
			System.out.println("Problem while processing file metadata");
		}
	}
	
	/**
	 * Before downloading the file, client calls this method to get the file details
	 * file hash, noOfChunks, hash of chunks, where the chunks are present
	 * @param fileName
	 * @return
	 */
	private void getFileDetails(String commandString) throws Exception {
		String parts[] = commandString.split(":");
		if (authenticateUser(parts[0], parts[1]) && parts.length == 3) {
			String fileName = parts[2]; 
			//Check if user has access to this file
			if (!checkIfUserHasAccessToFile(fileName)) {
				throw new Exception("Client does not have access to File");
			}
			/**
			 * fileName:fileHash:noOfChunks@c1:c1hash%ip:port%ip:port%ip2:port2@c2...
			 */
			StringBuilder sb = new StringBuilder();
			FileInfo fileInfo = Tracker.fileInfo.get(fileName);
			sb.append(fileInfo.getFileName()).append(":");
			sb.append(fileInfo.getFileHash()).append(":");
			sb.append(fileInfo.getNoOfChunks()).append("@");
			int noOfChunks = fileInfo.getNoOfChunks();
			for (int i = 1; i <= noOfChunks; i++) {
				String chunkName = fileName + "_" + i;
				ChunkInfo chunkInfo = Tracker.chunkInfo.get(chunkName);
				if (chunkInfo != null) {
					sb.append(chunkName).append(":");
					sb.append(chunkInfo.getHashOfChunk()).append("%");
					List<String> addressList = chunkInfo.getChunkLocations();
					for(String address : addressList) {
						sb.append(address).append("%");
					}
					sb.append("@");
				} else {
					dos.writeUTF("ERROR");
					throw new Exception("Problem finding chunk " + chunkName);
				}
			}
			dos.writeUTF(sb.toString());
		} else {
			dos.writeUTF("ERROR");
			throw new Exception("User NOT authenticated");
		}
	}
	
	private boolean checkIfUserHasAccessToFile(String fileName) {
		/**
		 * user can access the file 
		 * 1) Owner of the file
		 * 2) If file is shared in a group, and user is a part of that group
		 */
		
		FileInfo fileInfo = Tracker.fileInfo.get(fileName);
		if (fileInfo.getFileOwner().equals(requestFromUser)) {
			return true;
		}
		
		String fileSharedInGroup = fileInfo.getFileAvailableInGroups();
		if (fileSharedInGroup != null && Tracker.groupInfo.get(fileSharedInGroup).isUserInGroup(requestFromUser)) {
			return true;
		}
		return false;
	}
	
	private void stopShare(String commandString) throws Exception {
		/**
		 * Format -> username:password:groupName:fileName
		 */
		String parts[] = commandString.split(":");
		if (authenticateUser(parts[0], parts[1]) && parts.length == 4) {
			String groupName = parts[2];
			String fileName = parts[3];
			/**
			 * To unshare 
			 * 1) remove the file from the group
			 * 2) remove file from fileInfo map
			 */
			GroupInfo groupInfo = Tracker.groupInfo.get(groupName);
			FileInfo fileInfo = Tracker.fileInfo.get(fileName);
			if (groupInfo != null && fileInfo != null && fileInfo.getFileOwner().equals(requestFromUser)) {
				groupInfo.removeFileFromGroup(fileName);
				TrackerToTrackerSyncSender.addToSyncQueue("Tracker@groupInfo!" + groupInfo.toString());
				Tracker.fileInfo.remove(fileName);
				TrackerToTrackerSyncSender.addToSyncQueue("Tracker@fileInfoRemove!" + fileName);
				dos.writeUTF("OK");
			} else {
				dos.writeUTF("ERROR");
				throw new Exception("Problem finding file");
			}
		} else {
			dos.writeUTF("ERROR");
			throw new Exception("User NOT authenticated");
		}
	}
}
