import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server{

    public static final String DEFAULT_IP = "127.0.0.1";
    public static final int DEFAULT_PORTNUM = 2020;
    public static final int BUFFER_SIZE = 4096;
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

        while(true){
            Socket connSocket = welcomeSocket.accept();
            DataInputStream fromClient = new DataInputStream(connSocket.getInputStream());
            DataOutputStream toClient = new DataOutputStream(connSocket.getOutputStream());
            
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
                    File file = new File(argument);
                    if(!file.exist()){
                        toClient.writeBytes("Such file does not exist!\n");
                    }
                    else{
                        
                    }
                    break;
                case PUT:
                    File file = new File(argument);
                    if(file.exists()){
                        toClient.writeBytes("file already exist!\n");
                    }
                    else {
                        FileOutputStream fileReceived = new FileOutputStream(argument);
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int count;
                        while((count = fromClient.read(buffer)) > 0){
                            fileReceived.write(buffer, 0, count);
                        }
                        fileReceived.close();
                    }
                    break;
                case CD:
                    
                    break;
                case QUIT:
                    System.exit(0);
                default:
                    System.out.println("wrong input");
                    break;
            }
        }
    }
}