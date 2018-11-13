import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.String;
import java.net.Socket;
import java.net.UnknownHostException;
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
    public static DataInputStream fromServer;
    public static DataOutputStream toServer;


    public static void main(String[] args){

        // initial setup
        host = new String(DEFAULT_IP);
        if(args.length == 0){
            portNumber = DEFAULT_PORTNUM;
        } else {
            portNumber = Integer.parseInt(args[0]);
        }

        while(true){
            // connect
            clientSocket = new Socket(host, portNumber);

            FileTransmitter fileTransmitter;
            fileTransmitter= new FileTransmitter();

            fromServer = new DataInputStream(clientSocket.getInputStream());
            toServer = new DataOutputStream(clientSocket.getOutputStream());

            Scanner input = new Scanner(System.in);
            String userCommand = input.nextLine();
            String command = userCommand.split("\\s")[0];
            String argument = userCommand.split("\\s")[1];
            String printValue;

            switch(command){
                case "LIST":
                    fileTransmitter.sendListRequest(argument);
                    break;
                case "GET":
                    fileTransmitter.fileReceiver(argument);
                    break;
                case "PUT":
                    fileTransmitter.fileSender(argument);
                    break;
                case "CD":
                    fileTransmitter.changeDir(argument);
                    break;
                case "QUIT":
                    clientSocket.close();
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
            status = 0;
            fileSize = 0;
            file = null;
            fileReceived = null;
            fileToSend = null;
        }

        
        public void changeDir(String pathname){
            toServer.writeBytes("CD " + pathname);
            status = fromServer.readInt();
            if(status < 0){
                // error
                System.out.println(fromServer.readLine());
            }
            else{
                fileSize = fromServer.readInt();
                DEFAULT_FILE_PATH = fromServer.readLine();
                System.out.println(DEFAULT_FILE_PATH);
            }
        }

        public void sendListRequest(String pathname){
            toServer.writeBytes("LIST " + pathname);
            status = fromServer.readInt();
            if(status < 0){
                // error
                System.out.println(fromServer.readLine());
            }
            else{
                fileSize = fromServer.readInt();
                System.out.println(fromServer.readLine());
            }
        }

        public void fileReceiver(String pathname){
            toServer.writeBytes("GET " + pathname);
            status = fromServer.readInt();
            if(status < 0){
                // error
                System.out.println(fromServer.readLine());
            }
            else{
                fileSize = fromServer.readInt();
                fileReceived = new FileOutputStream(pathname);
                byte[] buffer = new byte[BUFFER_SIZE];
                int count;
                while((count = fromServer.read(buffer)) > 0){
                    fileReceived.write(buffer, 0, count);
                }
            }
        }

        public void fileSender(String filename){
            file = new File(filename);
            if(!file.exists()){
                System.out.println("No such file exist");
                return;
            }
            toServer.writeBytes("PUT " + filename);
            status = fromServer.readInt();
            if(status < 0){
                // error
                System.out.println(fromServer.readLine());
            }
            else{
                fileSize = file.length();
                fileToSend = new FileInputStream(filename);
                byte[] buffer = new byte[BUFFER_SIZE];
                int count;
                while((count = fileToSend.read(buffer)) > 0){
                    toServer.write(bytes, 0, count);
                }
                System.out.println(fromServer.readLine());
            }
        }
    }
}