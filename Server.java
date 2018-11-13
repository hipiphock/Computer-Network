import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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

    public static DataInputStream fromClient;
    public static DataOutputStream toClient;

    public static void main(String[] args) {
        
        // initial setup
        host = new String(DEFAULT_IP);
        if(args.length == 0){
            portNumber = DEFAULT_PORTNUM;
        } else {
            portNumber = Integer.parseInt(args[0]);
        }
        
        // connect
        ServerSocket welcomeSocket = new ServerSocket();

        while(true){
            Socket connSocket = welcomeSocket.accept();
            FileTransmitter fileTransmitter = new FileTransmitter();

            fromClient = new DataInputStream(connSocket.getInputStream());
            toClient = new DataOutputStream(connSocket.getOutputStream());
            
            String clientCommand = fromClient.readLine();
            String command = clientCommand.split("\\s")[0];
            String argument = clientCommand.split("\\s")[1];

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
                    System.exit(0);
                default:
                    System.out.println("wrong input");
                    break;
            }
        }
    }
    
    public class FileTransmitter{

        int status;
        int fileSize;
        File file;
        FileInputStream fileToSend;
        FileOutputStream fileReceived;

        public FileTransmitter(){
            status = 0;
            fileSize = 0;
            file = null;
        }

        public void changeDir(String pathname){
            if(pathname == null){
                status = 1;
                toClient.writeInt(status);
                toClient.writeInt(DEFAULT_FILE_PATH.length());
                toClient.writeBytes(DEFAULT_FILE_PATH);
            }
            else if(pathname.equals(".")){
                status = 1;
                toClient.writeInt(status);
                toClient.writeInt(DEFAULT_FILE_PATH.length());
                toClient.writeBytes(DEFAULT_FILE_PATH);
            }
            else if(pathname.equals("..")){
                status = 1;
                toClient.writeInt(status);
                file = new File(DEFAULT_FILE_PATH);
                String parent = file.getParent();
                DEFAULT_FILE_PATH = parent;
                toClient.writeInt(DEFAULT_FILE_PATH.length());
                toClient.writeBytes(DEFAULT_FILE_PATH);
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
                    toClient.writeBytes(DEFAULT_FILE_PATH);
                }

            }
        }

        public void sendFileList(String pathname){
            file = new File(pathname);
            if(!file.exists()){
                status = -1;
                toClient.writeInt(status);
                toClient.writeBytes("Failed - directory name is invalid\n");
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
                toClient.writeBytes(strToSend);
            }
        }

        public void fileReceiver(String pathname){
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

        public void fileSender(String filename){
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
