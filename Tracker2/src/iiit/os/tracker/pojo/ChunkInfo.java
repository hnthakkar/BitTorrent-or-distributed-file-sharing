package iiit.os.tracker.pojo;

import java.util.ArrayList;
import java.util.List;

public class ChunkInfo {
	
	private String chunkName;
	private String fileName;
	private String hashOfChunk;
	private List<String> chunkLocation;
	
	public ChunkInfo(String chunkName, String fileName, String hashOfChunk) {
		this.chunkName = chunkName;
		this.fileName = fileName;
		this.hashOfChunk = hashOfChunk;
		this.chunkLocation = new ArrayList<String>();
	}

	public String getChunkName() {
		return chunkName;
	}

	public String getFileName() {
		return fileName;
	}

	public String getHashOfChunk() {
		return hashOfChunk;
	}
	
	public void addChunkLocation(String addressNport) {
		this.chunkLocation.add(addressNport);
	}
	
	public List<String> getChunkLocations() {
		//ip:port
		return chunkLocation;
	}
	
	@Override
	public String toString() {
		/**
		 * Format -> chunkName:fileName:hashOfChunk%ip:port%ip:port
		 */
		StringBuilder sb = new StringBuilder();
		sb.append(chunkName).append(":");
		sb.append(fileName).append(":");
		sb.append(hashOfChunk).append("%");
		
		for (String chunkaddress : chunkLocation) {
			sb.append(chunkaddress).append("%");
		}
		return sb.toString();
	}
	
	public static ChunkInfo getObjectFromString(String input) {
		String[] parts = input.split("%");
		String chunkName = parts[0].split(":")[0];
		String fileName = parts[0].split(":")[1];
		String hashOfChunk = parts[0].split(":")[2];
		
		ChunkInfo chunkInfo = new ChunkInfo(chunkName, fileName, hashOfChunk);
		
		for (int i = 1; i < parts.length; i++) {
			chunkInfo.addChunkLocation(parts[i]);
		}
		return chunkInfo;
	}
}
