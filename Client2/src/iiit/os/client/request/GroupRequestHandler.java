package iiit.os.client.request;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.List;

public class GroupRequestHandler implements Runnable {
	
	private int requestType;
	private String userName;
	private String groupName;
	private String password;
	private List<String> trackerList;
	private Socket socket = null;
	private DataOutputStream dos = null;
	private DataInputStream din = null;
	
	public static final int CREATE_GROUP_REQ = 1;
	public static final int JOIN_GROUP_REQ = 2;
	public static final int LEAVE_GROUP_REQ = 3;
	public static final int LIST_GROUP_REQ = 4;
	public static final int LIST_PENDING_REQ = 5;
	public static final int ACCEPT_PENDING_REQ = 6;
	public static final int GROUP_USERS = 7;
	public static final int LIST_FILES_IN_GROUP = 8;
	public static final int STOP_SHARING = 9;
	
	public GroupRequestHandler(int requestType, String groupName, String userName, String password, List<String> trackerList) {
		this.requestType = requestType;
		this.groupName = groupName;
		this.userName = userName;
		this.password = password;
		this.trackerList = trackerList;
	}
	
	private Socket getActiveTrackerSocket() throws Exception {
		Socket socket = null; 
		for (String trackerAddress : trackerList) {
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
	
	@Override
	public void run() {
		try {
			socket = getActiveTrackerSocket();
			//System.out.println("Socket Connection established with the tracker");
			dos = new DataOutputStream(socket.getOutputStream());
			din = new DataInputStream(socket.getInputStream());
			StringBuilder sb = new StringBuilder();
			sb.append("Client").append("@");
			switch (requestType) {
				case CREATE_GROUP_REQ: {
					sb.append("create_group").append("#");
					break;
				}
				case JOIN_GROUP_REQ: {
					sb.append("join_group").append("#");
					break;
				}
				case LEAVE_GROUP_REQ: {
					sb.append("leave_group").append("#");
					break;
				}
				case LIST_PENDING_REQ: {
					sb.append("list_requests").append("#");
					break;
				}
				case ACCEPT_PENDING_REQ: {
					sb.append("accept_request").append("#");
					break;
				}
				case LIST_GROUP_REQ: {
					sb.append("list_groups").append("#");
					break;
				}
				case GROUP_USERS: {
					sb.append("group_users").append("#");
					break;
				}
				case LIST_FILES_IN_GROUP: {
					sb.append("list_files").append("#");
					break;
				}
				case STOP_SHARING: {
					sb.append("stop_share").append("#");
					break;
				}
			}
			sb.append(userName).append(":").append(password).append(":").append(groupName);
			//sb.append(groupName).append(":").append(userName);
			dos.writeUTF(sb.toString());
			
			String response = din.readUTF();
			if ("ERROR".equalsIgnoreCase(response)) {
				switch (requestType) {
					case CREATE_GROUP_REQ: {
						System.out.println("Problem while creating group");
						break;
					}
					case JOIN_GROUP_REQ: {
						System.out.println("Failed to submit join group request");
						break;
					}
					case LEAVE_GROUP_REQ: {
						System.out.println("Failed to leave group");
						break;
					}
					case LIST_PENDING_REQ: {
						System.out.println("Unable to fetch Pending requests");
						break;
					}
					case ACCEPT_PENDING_REQ: {
						System.out.println("Unable to add user to the group");
						break;
					}
					case LIST_GROUP_REQ: {
						System.out.println("Unable to fetch the groups");
						break;
					}
					case GROUP_USERS: {
						System.out.println("Unable to fetch users in the group");
						break;
					}
					case LIST_FILES_IN_GROUP: {
						System.out.println("Unable to fetch files in the group");
						break;
					}
					case STOP_SHARING: {
						System.out.println("Either fileName or Group Name does not exists or you are Not the owner of file");
						break;
					}
				}
			} else {
				switch (requestType) {
					case CREATE_GROUP_REQ: {
						System.out.println("Group sucessfully created");
						break;
					}
					case JOIN_GROUP_REQ: {
						System.out.println("Sumbitted request to group owner");
						break;
					}
					case LEAVE_GROUP_REQ: {
						System.out.println("Successfully left group");
						break;
					}
					case LIST_PENDING_REQ: {
						// sout all the pending request to the user 
						/**
						 * Format -> u1:g1@u2:g2
						 */
						if (response == null || "".equals(response)) {
							System.out.println("No pending request found");
							break;
						}
						
						String[] requests = response.split("@");
						for (String req : requests) {
							System.out.println(req);
						}
						break;
					}
					case ACCEPT_PENDING_REQ: {
						System.out.println("Successfully added User to the group");
						break;
					}
					case LIST_GROUP_REQ: {
						if (response == null || "".equals(response)) {
							System.out.println("No Groups found");
							break;
						}
						String[] groups = response.split(":");
						for (String group : groups) {
							System.out.println(group);
						}
						break;
					}
					case GROUP_USERS: {
						if (response == null || "".equals(response)) {
							System.out.println("No Users parts of the group");
							break;
						}
						String[] users = response.split(":");
						for (String user : users) {
							System.out.println(user);
						}
						break;
					}
					case LIST_FILES_IN_GROUP: {
						if (response == null || "".equals(response)) {
							System.out.println("No file shared in the group");
							break;
						}
						String[] files = response.split(":");
						for (String file : files) {
							System.out.println(file);
						}
						break;
					}
					case STOP_SHARING: {
						System.out.println("File successfully unshared");
						break;
					}
				}
			}
			
		} catch (Exception e) {
			System.out.println("Problem serving the request");
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
}
