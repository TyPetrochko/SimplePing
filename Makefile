
all: client server

client: PingClient.java
	javac PingClient.java

server: PingServer.java
	javac PingServer.java

test: all
	timeout 10s java PingServer 1025 pass -delay 100 -loss 0 &
	java PingClient localhost 1025 pass
