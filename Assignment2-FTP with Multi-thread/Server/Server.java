import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.Math;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server{

    private static final String DEFAULT_IP = "127.0.0.1";
    private static final int DEFAULT_PORTNUM = 2020;
    private static final int THREAD_NUM = 5;
    private static String DEFAULT_FILE_PATH;

    public static String host;
    public static int portNumber;

    public static Socket connSocket;

    public static DataInputStream fromClient;
    public static DataOutputStream toClient;
    public static FileTransmitter fileTransmitter;

    public static ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_NUM);

    public static void main(String[] args) throws IOException {
        
        // initial setup
        host = new String(DEFAULT_IP);
        if(args.length == 0){
            portNumber = DEFAULT_PORTNUM;
        } else {
            portNumber = Integer.parseInt(args[0]);
        }
        DEFAULT_FILE_PATH = Paths.get("").toAbsolutePath().toString();
        
        // starting point
        ServerSocket welcomeSocket = new ServerSocket(portNumber);

        while(true){
            // connect
            connSocket = welcomeSocket.accept();
            // thread
            threadPool.execute(new TransferThread(connSocket));
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
            if(pathname.equals(".")){
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
                    toClient.writeInt(status);
                    toClient.writeUTF("Failed - directory name is invalid\n");
                }
                else{
                	status = 1;
                	toClient.writeInt(status);
                    String path = file.getAbsolutePath();
                    DEFAULT_FILE_PATH = path;
                    toClient.writeInt(DEFAULT_FILE_PATH.length());
                    toClient.writeUTF(DEFAULT_FILE_PATH);
                }

            }
        }

        public void sendFileList(String pathname) throws IOException{
        	if(pathname == ".") pathname = DEFAULT_FILE_PATH;
            file = new File(pathname);
            if(!file.exists()){
                status = -1;
                toClient.writeInt(status);
                toClient.writeUTF("Failed - directory name is invalid\n");
            }
            else{
                status = 1;
                toClient.writeInt(status);
                File[] files = file.listFiles();
                String strToSend = new String();
                for(File fileidx: files){
                    String str = fileidx.getName();
                    if(fileidx.isDirectory()){
                        strToSend += str;
                        strToSend += ", - \n";
                    } else {
                        strToSend += str;
                        strToSend += ", ";
                        strToSend += fileidx.length();
                        strToSend += "\n";
                    }
                }
                toClient.writeInt(strToSend.length());
                toClient.writeUTF(strToSend);
            }
        }

        public void fileReceiver(String filename) throws IOException{
        	filename = DEFAULT_FILE_PATH + File.separator + filename;
            file = new File(filename);
            if(file.exists()){
                status = -1;
                toClient.writeInt(status);
                toClient.writeUTF("File already exist\n.");
            }
            else{
                status = 1;
                toClient.writeInt(status);
                fileSize = fromClient.readInt();
                fileReceived = new FileOutputStream(filename);
                byte[] buffer = new byte[fileSize];
                fromClient.read(buffer, 0, fileSize);
                fileReceived.write(buffer, 0, fileSize);
                fileReceived.flush();
                toClient.writeUTF(filename + " transferred/ " + fileSize + "bytes\n");
            }
        }

        public void fileSender(String filename) throws IOException{
            file = new File(filename);
            if(!file.exists()){
                status = -1;
                toClient.writeInt(status);
                toClient.writeUTF("Such file does not exist!\n");
            }
            else{
                status = 1;
                toClient.writeInt(status);
                fileSize = Math.toIntExact(file.length());
                toClient.writeInt(fileSize);
                fileToSend = new FileInputStream(filename);
                byte[] buffer = new byte[fileSize];
                fileToSend.read(buffer, 0, fileSize);
                toClient.write(buffer, 0, fileSize);
                toClient.writeUTF("Received " + filename + "/ " + fileSize + "bytes\n");
            }
        }
    }

    public static class TransferThread implements Runnable{
    	
    	private Socket threadSocket; 
    	public TransferThread(Socket socket){
    		threadSocket = socket;
    	}
        public void run(){
            
            System.out.println("Receiving...");
            
            fileTransmitter = new FileTransmitter();
            
            try {
				fromClient = new DataInputStream(connSocket.getInputStream());
	            toClient = new DataOutputStream(connSocket.getOutputStream());
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

            String command = null;
			try {
				command = fromClient.readUTF();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
            String argument = null;
            try {
            	argument = fromClient.readUTF();
            } catch (IOException e) {
            	// TODO Auto-generated catch block
            	e.printStackTrace();
            }
            System.out.println(command);
            System.out.println(argument);

            switch(command){
                case "LIST":
				try {
					fileTransmitter.sendFileList(argument);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                    break;
                case "GET":
				try {
					fileTransmitter.fileSender(argument);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                    break;
                case "PUT":
				try {
					fileTransmitter.fileReceiver(argument);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                    break;
                case "CD":
				try {
					fileTransmitter.changeDir(argument);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                    break;
                case "QUIT":
				try {
					connSocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                    System.exit(0);
                default:
                    System.out.println("wrong input");
                    break;
            }
        }
    }
}