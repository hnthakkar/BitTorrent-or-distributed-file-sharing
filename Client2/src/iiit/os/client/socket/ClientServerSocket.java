package iiit.os.client.socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import iiit.os.client.request.ClientToClientRequestHandler;

/**
 * Created by hnthakka on 03-Oct-19.
 */
public class ClientServerSocket implements Runnable{

    private int port;
    private ServerSocket serverSocket = null;

    /**
     * Other Clients requesting for chunks this client has
     * @param port
     */
    public ClientServerSocket(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            //System.out.println("port open for other Clients to request chunks!!");
        } catch (IOException e) {
            System.out.println("Problem with opening the ServerSocket!!");
            e.printStackTrace();
            return;
        }

        while (serverSocket.isBound()) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("Connection Req from : " + socket);
                DataInputStream din = new DataInputStream(socket.getInputStream());
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                (new Thread(new ClientToClientRequestHandler(socket, din, dos))).start();
            } catch (IOException e) {
                System.out.println("Exception while accepting Socket Connection!");
                e.printStackTrace();
            }
        }
    }

    public boolean closeServerSocket() {
        try {
            if (serverSocket.isBound()) {
                serverSocket.close();
            }
        } catch (Exception e) {
            System.out.println("Problem while closing the Socket");
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
