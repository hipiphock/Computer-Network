import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.String;
import java.net.Socket;
import java.util.Scanner;

public class Client{

    public static final String DEFAULT_IP = "127.0.0.1";
    public static final int DEFAULT_PORTNUM = 2020;
    public static final String DEFAULT_FILE_PATH = "";
    public static final int BUFFER_SIZE = 4096;

    public static String host;
    public static int portNumber;

    public static Socket clientSocket;
    public static DataInputStream fromServer;
    public static DataOutputStream toServer;


    public static void main(String[] args) {

        // initial setup
        host = new String(DEFAULT_IP);
        if(args.length == 0){
            portNumber = DEFAULT_PORTNUM;
        } else {
            portNumber = Integer.parseInt(args[0]);
        }

        FileTransmitter fileTransmitter;

        while(true){
            // connect
            clientSocket = new Socket(host, portNumber);

            fromServer = new DataInputStream(clientSocket.getInputStream());
            toServer = new DataOutputStream(clientSocket.getOutputStream());

            Scanner input = new Scanner(System.in);
            String userCommand = input.nextLine();
            String command = userCommand.split("\\s")[0];
            String argument = userCommand.split("\\s")[1];
            String printValue;

            switch(command){
                case null:
                    System.out.println("wrong input");
                    break;
                case LIST:
                    toServer.writeBytes(userCommand);
                    printValue = fromServer.readLine();
                    System.out.println(printValue);
                    break;
                case GET:
                    fileTransmitter.fileReceiver(argument);
                    break;
                case PUT:
                    fileTransmitter.fileSender(argument);
                    break;
                case CD:
                    toServer.writeBytes(userCommand);
                    break;
                case QUIT:
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

        }

        public void fileReceiver(String pathname){
            toServer.writeBytes("GET " + pathname);
            status = fromServer.readInt();
            if(status < 0){
                // error
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