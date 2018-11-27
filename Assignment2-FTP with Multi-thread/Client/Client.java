import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.Math;
import java.io.IOException;
import java.lang.String;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.Scanner;

public class Client{

    public static final String DEFAULT_IP = "127.0.0.1";
    public static final int DEFAULT_PORTNUM = 2020;
    public static final int BUFFER_SIZE = 4096;
    public static String DEFAULT_FILE_PATH = "/home/hingook/ftp/";
    // hingook can be changed to your username

    public static String host;
    public static int portNumber;

    public static Socket clientSocket;
    
    public static FileTransmitter fileTransmitter;


    public static void main(String[] args) throws UnknownHostException, IOException{

        // initial setup
        host = new String(DEFAULT_IP);
        if(args.length == 0){
            portNumber = DEFAULT_PORTNUM;
        } else {
            portNumber = Integer.parseInt(args[0]);
        }
        DEFAULT_FILE_PATH = Paths.get("").toAbsolutePath().toString();

        while(true){
        	
            // connect
        	clientSocket = new Socket(host, portNumber);
            System.out.println("Write Command: ");

            fileTransmitter= new FileTransmitter();

            Scanner input = new Scanner(System.in);
            String userCommand = input.nextLine();
            String command;
            String argument;
            try{
            	command = userCommand.split("\\s")[0];
                argument = userCommand.split("\\s")[1];
            } catch(ArrayIndexOutOfBoundsException e) {
            	command = userCommand;
            	argument = null;
            }

            switch(command){
                case "LIST":
                	if(argument == null) argument = ".";
                    fileTransmitter.sendListRequest(argument);
                    break;
                case "GET":
                    if(argument == null) break;
                    fileTransmitter.fileReceiver(argument);
                    break;
                case "PUT":
                    if(argument == null) break;
                    fileTransmitter.fileSender(argument);
                    break;
                case "CD":
                	if(argument == null) argument = ".";
                    fileTransmitter.changeDir(argument);
                    break;
                case "QUIT":
                	input.close();
                	fileTransmitter.quit();
                default:
                    System.out.println("wrong input");
                    break;
            }
        }
    }

    // inner class for transmitting files
    public static class FileTransmitter{

        int status;
        int fileSize;
        File file;
        FileInputStream fileToSend;
        FileOutputStream fileReceived;
        DataInputStream fromServer;
        DataOutputStream toServer;

        public FileTransmitter(){
            status = 0;
            fileSize = 0;
            file = null;
            fileReceived = null;
            fileToSend = null;
            fromServer = null;
            toServer = null;
        }

        // change the server's working directory
        public void changeDir(String pathname) throws IOException{
            fromServer = new DataInputStream(clientSocket.getInputStream());
            toServer = new DataOutputStream(clientSocket.getOutputStream());
            toServer.writeUTF("CD");
            toServer.writeUTF(pathname);
            status = fromServer.readInt();
            if(status < 0){
                // error
                System.out.println(fromServer.readUTF());
            }
            else{
                fileSize = fromServer.readInt();
                System.out.println(fromServer.readUTF());
            }
        }

        // get the list of files in the path
        public void sendListRequest(String pathname) throws IOException{
            fromServer = new DataInputStream(clientSocket.getInputStream());
            toServer = new DataOutputStream(clientSocket.getOutputStream());
            toServer.writeUTF("LIST");
            toServer.writeUTF(pathname);
            status = fromServer.readInt();
            if(status < 0){
                // error
                System.out.println(fromServer.readUTF());
            }
            else{
                fileSize = fromServer.readInt();
                System.out.println(fromServer.readUTF());
            }
        }

        // receive(get) file from server
        public void fileReceiver(String pathname) throws IOException{
            fromServer = new DataInputStream(clientSocket.getInputStream());
            toServer = new DataOutputStream(clientSocket.getOutputStream());
            toServer.writeUTF("GET");
            toServer.writeUTF(pathname);
            status = fromServer.readInt();
            if(status < 0){
                // error
                System.out.println(fromServer.readUTF());
            }
            else{
                fileSize = fromServer.readInt();
                fileReceived = new FileOutputStream(pathname);
                byte[] buffer = new byte[fileSize];
                fromServer.read(buffer, 0, fileSize);
                fileReceived.write(buffer, 0, fileSize);
                fileReceived.flush();
                System.out.println(fromServer.readUTF());
            }
        }

        // send(put) file to server
        public void fileSender(String filename) throws IOException{
            fromServer = new DataInputStream(clientSocket.getInputStream());
            toServer = new DataOutputStream(clientSocket.getOutputStream());
            file = new File(filename);
            if(!file.exists()){
                System.out.println("No such file exist");
                return;
            }
            toServer.writeUTF("PUT");
            toServer.writeUTF(filename);
            status = fromServer.readInt();
            if(status < 0){
                // error
                System.out.println(fromServer.readUTF());
            }
            else{
                fileSize = Math.toIntExact(file.length());
                toServer.writeInt(fileSize);
                fileToSend = new FileInputStream(filename);
                byte[] buffer = new byte[fileSize];
                fileToSend.read(buffer, 0, fileSize);
                toServer.write(buffer, 0, fileSize);
                toServer.flush();
                System.out.println(fromServer.readUTF());
            }
        }
        
        public void quit() throws IOException{
        	toServer = new DataOutputStream(clientSocket.getOutputStream());
        	toServer.writeUTF("QUIT");
        	toServer.writeUTF("QUIT");
            clientSocket.close();
            System.out.println("quit");
            System.exit(0);
        }
    }
}
