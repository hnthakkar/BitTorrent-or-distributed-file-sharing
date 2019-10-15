package iiit.os.tracker.pojo;

public class FileInfo {
	
	private String fileOwner;
	private String fileName;
	private int noOfChunks;
	private String fileHash;
	private String fileAvailableInGroups;
	
	public FileInfo(String fileName, String fileOwner, String groupName, String fileHash, int noOfChunks) {
		this.fileOwner = fileOwner;
		this.fileName = fileName;
		this.fileAvailableInGroups = groupName;
		this.fileHash = fileHash;
		this.noOfChunks = noOfChunks;
	}

	public String getFileOwner() {
		return fileOwner;
	}

	public String getFileName() {
		return fileName;
	}

	public int getNoOfChunks() {
		return noOfChunks;
	}

	public String getFileHash() {
		return fileHash;
	}

	public String getFileAvailableInGroups() {
		return fileAvailableInGroups;
	}
	
	@Override
	public String toString() {
		/**
		 * Format -> fileName:fileOwner:fileAvailableInGroup:fileHash:noOfChunks
		 */
		StringBuilder sb = new StringBuilder();
		sb.append(fileName).append(":");
		sb.append(fileOwner).append(":");
		sb.append(fileAvailableInGroups).append(":");
		sb.append(fileHash).append(":");
		sb.append(noOfChunks);
		return sb.toString();
	}
	
	public static FileInfo getObjectFromString(String input) {
		String[] parts = input.split(":");
		FileInfo fileInfo = null;
		if (parts.length == 5) {
			fileInfo = new FileInfo(parts[0], parts[1], parts[2], parts[3], Integer.parseInt(parts[4]));
		}
		return fileInfo;
	}
}
