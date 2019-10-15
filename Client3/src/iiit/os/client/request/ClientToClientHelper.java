package iiit.os.client.request;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.logging.FileHandler;

import iiit.os.client.Client;
import iiit.os.client.file.FileHelper;

public class ClientToClientHelper {
	
	public static final String uploadRequestType = "upload";
	public static final String downloadRequestType = "download";

	public static boolean isNullOrEmpty(String str) {
		if (str != null && !str.isEmpty())
			return false;
		return true;
	}

	public static void readFileWriteToStream(String path, String chunkName, DataOutputStream dos) throws Exception {
		File file = new File(path + chunkName);
		if (!file.exists()) {
			System.out.println("Chunk " + chunkName + " not found");
		}

		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			byte[] bytes = new byte[Client.CHUNK_SIZE];
			int count;
			while ((count = fis.read(bytes)) > 0) {
				dos.write(bytes, 0, count);
			}
		} catch (Exception e) {
			System.out.println("Problem while transfer the Chunk");
			e.printStackTrace();
			throw e;
		} finally {
			fis.close();
		}
		System.out.println(chunkName + " Transferred!");
	}

	public static String readStreamWriteToFile(String path, String fileName, DataInputStream din) {
		FileOutputStream fos = null;
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		byte[] bytes = new byte[Client.CHUNK_SIZE];
		int count = 0;
		String hash = null;
		try {
			bis = new BufferedInputStream(din);
			fos = new FileOutputStream(path + fileName);
			bos = new BufferedOutputStream(fos);

			while ((count = bis.read(bytes)) > 0) {
				bos.write(bytes, 0, count);
			}
			hash = FileHelper.getChunkHash(bytes);
			bos.flush();
		} catch (Exception e) {
			System.out.println("Problem writing bytes to chunk file");
			// If there is an error writing the chunk, than delete if chunk is created
			try {
				File file = new File(path + fileName);
				if (file.exists()) {
					file.delete();
				}
			} catch (Exception ex) {
				// DO nothing
			}
		} finally {
			try {
				bis.close();
				bos.close();
				fos.close();
			} catch (Exception e) {
				System.out.println("Problem Closing the chunk file");
			}
		}
		return hash;
	}
}
