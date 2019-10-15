package iiit.os.tracker.pojo;

import java.util.ArrayList;
import java.util.List;

public class GroupInfo {
	
	private String groupName;
	private String groupOwner;
	private List<String> userInGroup;
	private List<String> filesInGroup;
	
	public GroupInfo(String groupName, String groupOwner) {
		this.groupName = groupName;
		this.groupOwner = groupOwner;
		userInGroup = new ArrayList<String>();
		filesInGroup = new ArrayList<String>();
	}
	
	public String getGroupName() {
		return groupName;
	}
	
	public String getGroupOwner() {
		return groupOwner;
	}
	
	public boolean isUserInGroup(String userName) {
		return userInGroup.contains(userName);
	}
	
	public void addUserToGroup(String userName) {
		this.userInGroup.add(userName);
	}
	
	public void removeUserFromGroup(String userName) {
		this.userInGroup.remove(userName);
	}
	
	public List<String> getUsersInGroup() {
		return userInGroup;
	}
	
	public List<String> getFilesInGroup() {
		return filesInGroup;
	}
	
	public void addFileToGroup(String fileName) {
		this.filesInGroup.add(fileName);
	}
	
	public void removeFileFromGroup(String fileName) {
		this.filesInGroup.remove(fileName);
	}

	@Override
	public String toString() {
		/**
		 * Format -> groupName:groupOwner%userInfo#u1:u2:
		 */
		StringBuilder sb = new StringBuilder();
		sb.append(groupName).append(":");
		sb.append(groupOwner).append(":");
		sb.append(convertUserListToString());
		sb.append(convertFileListToString());
		return sb.toString();
	}
	
	public String convertUserListToString() {
		StringBuilder sb = new StringBuilder();
		sb.append("%userInfo#");
		for (String user : this.userInGroup) {
			sb.append(user).append(":");
		}
		return sb.toString();
	}
	
	public String convertFileListToString() {
		StringBuilder sb = new StringBuilder();
		sb.append("%fileInfo#");
		for (String file : this.filesInGroup) {
			sb.append(file).append(":");
		}
		return sb.toString();
	}
	
	public static GroupInfo getObjectFromString(String input) {
		String[] parts = input.split("%");
		String groupName = parts[0].split(":")[0];
		String groupOwner = parts[0].split(":")[1];
		
		GroupInfo groupInfo = new GroupInfo(groupName, groupOwner);
		
		if (parts.length > 1) {
			String[] userParts = parts[1].split("#");
			if (userParts.length > 1) {
				String[] users = userParts[1].split(":");
				for (String user : users) {
					groupInfo.addUserToGroup(user);
				}
			}
		}
		
		if (parts.length > 2) {
			String[] fileParts = parts[2].split("#");
			if (fileParts.length > 1) {
				String[] files = fileParts[1].split(":");
				for (String file : files) {
					groupInfo.addFileToGroup(file);
				}
			}
		}
		return groupInfo;
	}
}
