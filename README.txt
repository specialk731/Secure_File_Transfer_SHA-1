Group: Kevin Greenwald (ktg150130) and Matthew Valencia (mlv140130)
Platform: Java

How to Compile:
To comile use command "javac Server.java" or "javac Client.java" to compile the Server and Client respectively.
All Files can be found on our github - https://github.com/specialk731/Secure_File_Transfer_SHA-1.

How to Run:
From a directory  
java Server
java Client [args]
4 Required args are: (S for sending or R for receiving)   IP_of_Server  Port  FileName

Details:
Server assumes port 14014 and will run until ctrl+c is used to kill the server.

Client will assume sending of a file if first args is not R.