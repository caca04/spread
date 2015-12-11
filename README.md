File Service
============

Hello, this is my implementation of a distributed system using Spread for the Distributed System class

How to Run:
-----

1-Run Spread

2-Compile the classes using the following command:(the Spread jar needs to be in the same directory):

 javac -cp spread-4.4.0.jar Master.java Slave.java Client.java ListenerMaster.java ListenerClient.java ListenerSlave.java FilesWork.java 
Compar.java ArquivoSlave.java Sleep.java

3-Run the program using the following commands:

3.1-Create Master:
 java -cp .:./spread-4.4.0.jar Master -u m1 -p 4803 -s localhost -r 103

3.2-Create Slave:
 java -cp .:./spread-4.4.0.jar Slave -u s1 -p 4803 -s localhost

3.3-Create Client:
 java -cp .:./spread-4.4.0.jar Client -u c1 -p 4803 -s localhost 

-u = name
-p = port 
-r = priority
-s = host
