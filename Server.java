import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.Math;
import java.net.ServerSocket;
import java.net.Socket;

public class Server{

    public static final String DEFAULT_IP = "127.0.0.1";
    public static final int DEFAULT_PORTNUM = 2020;
    public static final int BUFFER_SIZE = 4096;
    public static String DEFAULT_FILE_PATH = "/home/hingook/ftp/";
    // hingook can be changed to your username

    public static String host;
    public static int portNumber;
    
    public static ServerSocket welcomeSocket;
    public static Socket connSocket;

    public static DataInputStream fromClient;
    public static DataOutputStream toClient;
    public static FileTransmitter fileTransmitter;

    public static void main(String[] args) throws IOException {
        
        // initial setup
        host = new String(DEFAULT_IP);
        if(args.length == 0){
            portNumber = DEFAULT_PORTNUM;
        } else {
            portNumber = Integer.parseInt(args[0]);
        }
        welcomeSocket = new ServerSocket(portNumber);

        while(true){
        	// connect
            connSocket = welcomeSocket.accept();
            System.out.println("Connected with client");
            
            fileTransmitter = new FileTransmitter();
            
            // 아니 생각해보니까 클라에서 파싱해서 주면 알아서 받으면 되는건데 하
            fromClient = new DataInputStream(connSocket.getInputStream());
            toClient = new DataOutputStream(connSocket.getOutputStream());

        	String command = fromClient.readUTF(); 
            String argument = fromClient.readUTF();
            System.out.println(command);
            System.out.println(argument);

            switch(command){
                case "LIST":
                    fileTransmitter.sendFileList(argument);
                    break;
                case "GET":
                    fileTransmitter.fileSender(argument);
                    break;
                case "PUT":
                    fileTransmitter.fileReceiver(argument);
                    break;
                case "CD":
                    fileTransmitter.changeDir(argument);
                    break;
                case "QUIT":
                    welcomeSocket.close();
                    connSocket.close();
                    System.exit(0);
                default:
                    System.out.println("wrong input");
                    break;
            }
        }
    }
    
    public static class FileTransmitter{

        int status;
        int fileSize;
        File file;
        FileInputStream fileToSend;
        FileOutputStream fileReceived;

        public FileTransmitter(){
            status = 0;
            fileSize = 0;
            file = null;
            fileToSend = null;
            fileReceived = null;
        }

        public void changeDir(String pathname) throws IOException{
            if(pathname == null){
                status = 1;
                toClient.writeInt(status);
                toClient.writeInt(DEFAULT_FILE_PATH.length());
                toClient.writeUTF(DEFAULT_FILE_PATH);
            }
            else if(pathname.equals(".")){
                status = 1;
                toClient.writeInt(status);
                toClient.writeInt(DEFAULT_FILE_PATH.length());
                toClient.writeUTF(DEFAULT_FILE_PATH);
            }
            else if(pathname.equals("..")){
                status = 1;
                toClient.writeInt(status);
                file = new File(DEFAULT_FILE_PATH);
                String parent = file.getParent();
                DEFAULT_FILE_PATH = parent;
                toClient.writeInt(DEFAULT_FILE_PATH.length());
                toClient.writeUTF(DEFAULT_FILE_PATH);
            }
            else{
                file = new File(pathname);
                if(!file.isDirectory()){
                    status = -1;
                    toClient.writeBytes("Failed - directory name is invalid\n");
                }
                else{
                    String path = file.getAbsolutePath();
                    DEFAULT_FILE_PATH = path;
                    toClient.writeInt(DEFAULT_FILE_PATH.length());
                    toClient.writeUTF(DEFAULT_FILE_PATH);
                }

            }
        }

        public void sendFileList(String pathname) throws IOException{
            file = new File(pathname);
            if(!file.exists()){
                status = -1;
                toClient.writeInt(status);
                toClient.writeUTF("Failed - directory name is invalid\n");
            }
            else{
                status = 1;
                toClient.writeInt(status);
                String[] fileList = file.list();
                File[] files = file.listFiles();
                String strToSend = new String();
                for(File fileidx: files){
                    String str = file.getName();
                    if(file.isDirectory()){
                        strToSend += str;
                        strToSend += ", - \n";
                    } else {
                        strToSend += str;
                        strToSend += ", ";
                        strToSend += file.length();
                        strToSend += "\n";
                    }
                }
                toClient.writeInt(strToSend.length());
                toClient.writeUTF(strToSend);
            }
        }

        public void fileReceiver(String pathname) throws IOException{
            pathname = DEFAULT_FILE_PATH + pathname;
            file = new File(pathname);
            if(file.exists()){
                status = -1;
                toClient.writeInt(status);
                toClient.writeBytes("File already exist\n.");
            }
            else{
                status = 1;
                toClient.writeInt(status);
                fileSize = fromClient.readInt();
                fileReceived = new FileOutputStream(pathname);
                byte[] buffer = new byte[BUFFER_SIZE];
                int count;
                while((count = fromClient.read(buffer)) > 0){
                    fileReceived.write(buffer, 0, count);
                }
                toClient.writeBytes(pathname + " transferred/ " + fileSize + "bytes\n");
            }
        }

        public void fileSender(String filename) throws IOException{
            file = new File(filename);
            if(!file.exists()){
                status = -1;
                toClient.writeInt(status);
                toClient.writeBytes("Such file does not exist!\n");
            }
            else{
                status = 1;
                toClient.writeInt(status);
                fileSize = Math.toIntExact(file.length());
                toClient.writeInt(fileSize);
                fileToSend = new FileInputStream(filename);
                byte[] buffer = new byte[BUFFER_SIZE];
                int count;
                while((count = fileToSend.read(buffer)) > 0){
                    toClient.write(buffer, 0, count);
                }
                toClient.writeBytes("Received " + filename + "/ " + fileSize + "bytes\n");
            }
        }
    }
}
