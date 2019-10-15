package iiit.os.tracker.pojo;

import java.util.ArrayList;
import java.util.List;

public class UserInfo {
	
	private String userName;
	private String password;
	private List<String> ownerOfGroups;
	private List<String> ownerOfFile;
	
	public UserInfo(String userName, String password) {
		this.userName = userName;
		this.password = password;
		this.ownerOfGroups = new ArrayList<String>();
		this.ownerOfFile = new ArrayList<String>();
	}
	
	public String getUserName() {
		return userName;
	}
	
	public String getPassword() {
		return password;
	}
	
	public List<String> getOwnerOfGroups() {
		return ownerOfGroups;
	}
	
	public void addToUserGroups(String group) {
		this.ownerOfGroups.add(group);
	}
	
	public List<String> getOwnerOfFile() {
		return ownerOfFile;
	}
	
	public void addToUserFile(String fileName) {
		this.ownerOfFile.add(fileName);
	}
	
	public boolean isUserOwnerofGroup(String group) {
		return this.ownerOfGroups.contains(group);
	}
	
	public boolean isUserOwnerofFile(String fileName) {
		return this.ownerOfFile.contains(fileName);
	}
	
	@Override
	public String toString() {
		/**
		 * Format -> username:password%groupInfo#g1:g2:%fileInfo#f1:f2:
		 */
		StringBuilder sb = new StringBuilder();
		sb.append(userName).append(":").append(password);
		sb.append("%").append(convertFileListToString());
		sb.append("%").append(convertGroupListToString());
		return sb.toString();
	}
	
	private String convertGroupListToString() {
		StringBuilder sb = new StringBuilder();
		sb.append("groupInfo#");
		for (String group : this.ownerOfGroups) {
			sb.append(group).append(":");
		}
		return sb.toString();
	}
	
	private String convertFileListToString() {
		StringBuilder sb = new StringBuilder();
		sb.append("fileInfo#");
		for (String file : this.ownerOfFile) {
			sb.append(file).append(":");
		}
		return sb.toString();
	}
	
	public static UserInfo getObjectFromString(String input) {
		String[] parts = input.split("%");
		String userName = parts[0].split(":")[0];
		String password = parts[0].split(":")[1];
		
		UserInfo userInfo = new UserInfo(userName, password);
		
		if (parts.length > 1) {
			String[] groupParts = parts[1].split("#");
			if (groupParts.length > 1) {
				String[] groups = groupParts[1].split(":");
				for (String group : groups) {
					userInfo.addToUserGroups(group);
				}
			}
		}
		
		if (parts.length > 2) {
			String[] fileParts = parts[1].split("#");
			if (fileParts.length > 1) {
				String[] files = fileParts[1].split(":");
				for (String file : files) {
					userInfo.addToUserFile(file);
				}
			}
		}
		return userInfo;
	}
}
