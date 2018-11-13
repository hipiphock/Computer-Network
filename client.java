import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.String;
import java.net.Socket;
import java.util.Scanner;

public class Client{

    public static final String DEFAULT_IP = "127.0.0.1";
    public static final int DEFAULT_PORTNUM = 2020;

    public static String ip;
    public static int portNumber;

    public static void main(String[] args) {

        // initial setup
        ip = new String(DEFAULT_IP);
        if(args.length == 0){
            portNumber = DEFAULT_PORTNUM;
        } else {
            portNumber = args[0];
        }

        while(true){
            // connect
            Socket clientSocket = new Socket(ip, portNumber);

            Scanner input = new Scanner(System.in);
            // System.out.println("connected");
            String userCommand = input.nextLine();
            String command = userCommand.split("\\s")[0];
            String argument = userCommand.split("\\s")[1];
            String printValue;

            DataInputStream fromServer = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream toServer = new DataOutputStream(clientSocket.getOutputStream());
            

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
                    File file = new File(argument);
                    if(file.exists()){
                        System.out.println("file already exist");
                    }
                    else {
                        toServer.writeBytes(userCommand);
                        FileOutputStream fileReceived = new FileOutputStream(argument);
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int count;
                        while((count = fromClient.read(buffer)) > 0){
                            fileReceived.write(buffer, 0, count);
                        }
                        fileReceived.close();
                    }
                    break;
                case PUT:
                    
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
}