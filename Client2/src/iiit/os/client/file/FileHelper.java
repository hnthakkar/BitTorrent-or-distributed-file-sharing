package iiit.os.client.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import iiit.os.client.Client;

/**
 * Created by hnthakka on 02-Oct-19.
 */
public class FileHelper {

    public static final String HashingAlgo = "SHA-1";

    /**
     * Locally splits the file before upload, creating hash for each chunk and also the Hash of the file
     * @param userName
     * @param f
     * @return a HashMap containing the metadata of the file,
     * i.e. no of chunks, Hash of each chunk, Hash of the file, Owner
     * @throws IOException
     */
    public static Map<String, String> splitFile(File f) throws IOException, NoSuchAlgorithmException {
        int chunkStartIndex = 1;
        Map<String, String> fileMetaData = new HashMap<>();
        String fileName = f.getName();
        byte[] buffer = new byte[Client.CHUNK_SIZE];

        try (FileInputStream fis = new FileInputStream(f);
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            int fileCounter = 0;
            int bytesAmount = 0;
            while ((bytesAmount = bis.read(buffer)) > 0) {
                String chunkName = fileName + "_" + chunkStartIndex++;
                Path path = Paths.get(Client.chunkDir + chunkName);
                Files.createDirectories(path.getParent());

                File newFile = new File(path.toString());
                try (FileOutputStream out = new FileOutputStream(newFile)) {
                    out.write(buffer, 0, bytesAmount);
                }
                String hexSHA = getChunkHash(buffer);
                fileMetaData.put(chunkName, hexSHA);
            }
        }
        fileMetaData.put("fileName", fileName);
        fileMetaData.put("noOfChunks", (chunkStartIndex - 1) + "");
        fileMetaData.put("fileHash", getFileHash(fileName, chunkStartIndex - 1, fileMetaData));
        return fileMetaData;
    }

    /**
     * @param fileChar
     * @param size
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static String getChunkHash(byte[] fileChar) throws NoSuchAlgorithmException {
        MessageDigest shaDigest = MessageDigest.getInstance(HashingAlgo);
        byte[] bytes = shaDigest.digest(fileChar);

        return convertByteToHexFormat(bytes);
    }

    public static String getFileHash(String fileName, int noOfChunks, Map<String, String> FileMetaData) throws NoSuchAlgorithmException{
        StringBuilder compositeHash = new StringBuilder();
        int chunkIndexCounter = 1;
        while (chunkIndexCounter <= noOfChunks) {
            if (FileMetaData.containsKey(fileName + "_" + chunkIndexCounter)) {
                compositeHash.append(FileMetaData.get(fileName + "_" + chunkIndexCounter++));
            } else {
                System.out.println("Valid Hash Entry NOT found");
                throw new NoSuchAlgorithmException();
            }
        }

        MessageDigest shaDigest = MessageDigest.getInstance(HashingAlgo);
        byte[] bytes = shaDigest.digest(compositeHash.toString().getBytes());
        return convertByteToHexFormat(bytes);
    }

    public static boolean validateFile(byte[] hashOfChunksCombined, String expectedHash) throws Exception {
        String fileHash = getChunkHash(hashOfChunksCombined);
        return expectedHash.equals(fileHash);
    }

    private static String convertByteToHexFormat(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    public static boolean mergeChunks(String fileName, int noOfChunks) throws IOException {
        int chunkStartIndex = 1;
        Path outputPath = Paths.get(Client.downloadDir + fileName);
        Files.createDirectories(outputPath.getParent());

        while (chunkStartIndex <= noOfChunks) {
            String chunkName = fileName + "_" + chunkStartIndex++;
            Path chunkPath = Paths.get(Client.chunkDir + chunkName);
            if(!appendToFile(outputPath.toString(), chunkPath.toString())) {
                System.out.println("Error appending chunk");
                return false;
            }
        }
        return true;
    }

    private static boolean appendToFile(String outputFile, String chunkName) {
        try (FileInputStream fis = new FileInputStream(chunkName);
             FileOutputStream out = new FileOutputStream(outputFile, true)) {
            byte[] buffer = new byte[Client.CHUNK_SIZE];
            int bytesRead = -1;

            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
            fis.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error appending to file");
            return false;
        }
        return true;
    }
}
