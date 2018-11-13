import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server{

    public static final String DEFAULT_IP = "127.0.0.1";
    public static final int DEFAULT_PORTNUM = 2020;
    public static final String DEFAULT_FILE_PATH = "";
    public static final int BUFFER_SIZE = 4096;

    public static DataInputStream fromClient;
    public static DataOutputStream toClient;
    public static void main(String[] args) {
        
        // initial setup
        ip = new String(DEFAULT_IP);
        if(args.length == 0){
            portNumber = DEFAULT_PORTNUM;
        } else {
            portNumber = args[0];
        }
        
        // connect
        ServerSocket welcomeSocket = new ServerSocket();

        FileTransmitter fileTransmitter;

        while(true){
            Socket connSocket = welcomeSocket.accept();
            
            fromClient = new DataInputStream(connSocket.getInputStream());
            toClient = new DataOutputStream(connSocket.getOutputStream());
            
            String clientCommand = fromClient.readLine();
            String command = clientCommand.split("\\s")[0];
            String argument = clientCommand.split("\\s")[1];

            switch(command){
                case null:
                    System.out.println("wrong input");
                    break;
                case LIST:
                    File dir = new File(argument);
                    String[] fileList = dir.list();
                    File[] files = dir.listFiles();
                    String strToSend;
                    for(File file: files){
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
                    toClient.writeBytes(strToSend);
                    break;
                case GET:
                    fileTransmitter.fileSender(argument);
                    break;
                case PUT:
                    fileTransmitter.fileReceiver(argument);
                    break;
                case CD:
                    if(argument == null){

                    }
                    else{

                    }
                    break;
                case QUIT:
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
        FileInputStream fileReceived;
        FileOutputStream fileToSend;

        public FileTransmitter(){

        }

        public void listSender(String pathname){

        }

        public void fileReceiver(String pathname){
            file = File(pathname);
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
                fileSize = file.length();
                toClient.writeInt(fileSize);
                fileToSend = new FileInputStream(filename);
                byte[] buffer = new byte[BUFFER_SIZE];
                int count;
                while((count = fileToSend.read(buffer)) > 0){
                    toClient.write(bytes, 0, count);
                }
                toClient.writeBytes("Received " + filename + "/ " + fileSize + "bytes\n");
            }
        }
    }
}