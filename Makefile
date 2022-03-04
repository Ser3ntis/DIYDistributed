# A very dumb Makefile

all:	server client

server:
	javac DIYAppController.java

client:
	javac DIYAppWorker.java

clean:
	rm -f *.class DIYAppController DIYAppWorker Receptionist Waiter

