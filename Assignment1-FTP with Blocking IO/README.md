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

## Servr
The server receives command from client and execute the command.
