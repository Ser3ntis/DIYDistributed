Simple program demonstrating the working principle of a distributed application

Server utilizes multiple threads to serve multiple clients in a concurrent fashion
The server reads the data set file of double type values, breaks it down to smaller packets, sends those packets to multiple clients
for them to calculate the artioal sum of the packet they received, and adds each client's partial sum to the total sum.
Once all data set entries have been accounted for, the server displays the total sum.

Client connects to a server, and cyclically accepts a packet of double values, calculates their partial sum, and sends that sum to a client. 

Server-to-Client message contains a LinkedList<String> object holding dataset entries for client to add up. Server sends null to a client as a termination call.
Client-to-Server message contains a String holding partial sum.

1. Compile the code

Open a terminal window, change working folder (directory) to 
where the code is.

You can compile the package manually by typing:

	javac DIYAppController.java
	javac DIYAppWorker.java

But a more standard way is to use the make utility:

	make server
	make client

You should take a peek at the Makefile. To build both at the same time, simply:

	make


2. Run the code

Run the server such as:

	./diyappcontroller <port number> <filename> <packet size>

Alternatively (not both), run the server such as:

	./diyappcontroller <port number> <filename>

<port number> specifies the port the server will utilize .
<filename> specifies the dataset system file .
<packet size> specifies how many values each packet will contain. If not specified as an argument, defaults to 50 a packet.

Run the client such as:

	./diyappworker <dest address> <port number>

<dest address> specifies... destination address
<port number> specifies targeted server's port number




