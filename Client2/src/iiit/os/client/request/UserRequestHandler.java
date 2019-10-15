package iiit.os.client.request;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.List;

import iiit.os.client.Client;

public class UserRequestHandler implements Runnable {

	private int requestType;
	private String userName;
	private String password;
	private List<String> trackerList;
	private Socket socket = null;
	private DataOutputStream dos = null;
	private DataInputStream din = null;
	
	/**
	 * All User specific related request handled here
	 * 1) create_user
	 * 2) login
	 * 3) logout	
	 * @param command
	 * @param userName
	 * @param password
	 */
	
	public static final int CREATE_USER_REQ = 1;
	public static final int LOGIN_REQ = 2;
	public static final int LOGOUT_REQ = 3;
	public static final int ACTIVE_CLIENTS = 4;
	
	public UserRequestHandler(int requestType, String userName, String password, List<String> trackerList) {
		this.requestType = requestType;
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
				case CREATE_USER_REQ: {
					sb.append("create_user").append("#");
					break;
				}
				case LOGIN_REQ: {
					sb.append("login").append("#");
					sb.append(Client.clientIP).append(":").append(Client.PORT).append(":");
					break;
				}
				case LOGOUT_REQ: {
					sb.append("logout").append("#");
					break;
				}
				case ACTIVE_CLIENTS: {
					sb.append("active_clients").append("#");
					break;
				}
			}
			
			sb.append(userName).append(":").append(password);
			dos.writeUTF(sb.toString());
			//System.out.println("Create User request sent");
			String response = din.readUTF();
			if ("ERROR".equalsIgnoreCase(response)) {
				switch (requestType) {
					case CREATE_USER_REQ: {
						System.out.println("Problem while creating user");
						break;
					}
					case LOGIN_REQ: {
						System.out.println("Login failed");
						break;
					}
					case LOGOUT_REQ: {
						System.out.println("Logout failed");
						break;
					}
					case ACTIVE_CLIENTS: {
						System.out.println("Unable to fetch Active Clients");
						break;
					}
				}
			} else {
				switch (requestType) {
					case CREATE_USER_REQ: {
						System.out.println("User sucessfully created");
						break;
					}
					case LOGIN_REQ: {
						System.out.println("Successfully Logged In");
						Client.userName = userName;
						Client.password = password;
						//Client.startClientServerSocket();
						break;
					}
					case LOGOUT_REQ: {
						System.out.println("Successfully logged Out");
						Client.userName = null;
						Client.password = null;
						break;
					}
					case ACTIVE_CLIENTS: {
						/**
						 * Format -> u1#ip:port@u2#ip:port@
						 */
						if (response == null || "".equals(response)) {
							System.out.println("No Active user found");
							break;
						}
						String[] users = response.split("@");
						for (String user : users) {
							System.out.println(user);
						}
						break;
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Problem while establishing socket with the tracker");
			//e.printStackTrace();
		} finally {
			try {
				din.close();
				dos.close();
				socket.close();
				/*
				 * if (LOGOUT_REQ == requestType) { Client.stopClientServerSocket(); }
				 */
			} catch (Exception e) {
				System.out.println("Problem while closing the Client to Tracker resources");
			}
		}
	}
}
