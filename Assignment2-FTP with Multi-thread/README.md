# FTP Protocol with Blocking IO

This is the FTP Protocol server & client program.

# Feature

* upload / download feature
  + `GET (filename)` gets the file in server.
  + `PUT (filename)` puts the file in client.
* listing current files(ls)
  + `LIST (path)` gets the file list of current working directory.
  + If `(path)` is blank, it gets the list of current working directory.
* changing directory(cd)
  + `CD (path)` changes the current working directory.
  + If `(path)` is blank, it changes to the current working directory, which is no change.
  + It supports `.` and `..`, just like bash.
  
# Limitation

* It may not transfer files with big size

* There may be problem with shutdown

# Building

It can be easily compiled, just like other java files.

``` console
javac Server.java
java Server
```

``` console
javac Client.java
java Client
```

**Make sure that the Server's working directory is different from Client**

# Documentation

## Client
The client sends order to the server, and wait for response.
It has basically 4 command options(excluding QUIT), which is GET, PUT, LIST, and CD.
The client performs those 4 acts by inner class: `FileTransmitter`

``` java
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
```
This is the inner class that performs 4 acts.

``` java
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
```
This method handles the `CD` command.
When the input is like `CD`, it handle as `CD .`.

``` java
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
```
This method gets the list of files in the working directory of the server.
Like `CD`, `LIST` works like `LIST .`.

``` java
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
```
One of the key operation of FTP is sending and receiving file.
The `fileReceiver()` GET the file from server.

``` java
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
    }
```
This method sends the file to the server.
If there's no exact file, it terminates the method.

## Server
The server receives command from client and execute the command.
It handles the input given by client with the inner class: `FileTransmitter`

``` java
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
```
This is the variables of FileTransmitter.

``` java
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
```
This is the method which handles the `CD` command.
It even handles "." and "..".

``` java
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
```
This method sends the list of files in current directory.
It sends it with the form of String.

``` java
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
```
This method receives the file from client.
If the file already exist, it sends negative status and error message.
Else, it gets the file and sends the confirm message.


``` java
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
```
This method sends the file from server to client.
If there's no such file, it sends negative status and error message.
Else, it sends the file to the client.
