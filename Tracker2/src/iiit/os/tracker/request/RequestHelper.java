package iiit.os.tracker.request;

import iiit.os.tracker.pojo.GroupInfo;
import iiit.os.tracker.pojo.UserInfo;

public class RequestHelper {
	
	public static UserInfo getUserInfoFromString(String input) {
		if (isNullOrEmpty(input)) {
			return null;
		}
		/**
		 * Format -> username:password
		 */
		String[] parts = input.split(":");
		if (parts.length != 2) {
			return null;
		}
		
		return new UserInfo(parts[0], parts[1]);
	}
	
	public static GroupInfo getGroupInfoFromString(String input) {
		if (isNullOrEmpty(input)) {
			return null;
		}
		/**
		 * Format -> username:password
		 */
		String[] parts = input.split(":");
		if (parts.length != 2) {
			return null;
		}
		
		GroupInfo grpInfo = new GroupInfo(parts[0], parts[1]);
		// adding the group owner as well to the group user List
		grpInfo.addUserToGroup(parts[1]);
				
		return grpInfo;
	}
	
	public static boolean isNullOrEmpty(String str) {
		if (str != null && !str.isEmpty())
			return false;
		return true;
	}

}
