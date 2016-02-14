
all: client server

client: PingClient.java
	javac PingClient.java

server: PingServer.java
	javac PingServer.java

test:
