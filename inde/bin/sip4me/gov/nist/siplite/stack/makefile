PACKAGE=gov.nist.siplite.stack
ROOT=../../../../../
include $(ROOT)/build-config.j2me

copyfiles:
	cp TCPMessageChannel.j2me TCPMessageChannel.java
	cp UDPMessageChannel.j2me UDPMessageChannel.java
	cp UDPMessageProcessor.j2me UDPMessageProcessor.java
	cp TCPMessageProcessor.j2me TCPMessageProcessor.java
	rm  -f IOHandler.java

save:
	cp TCPMessageChannel.java TCPMessageChannel.j2me
	cp UDPMessageChannel.java UDPMessageChannel.j2me
	cp UDPMessageProcessor.java UDPMessageProcessor.j2me
	cp TCPMessageProcessor.java TCPMessageProcessor.j2me


all:
	$(JAVAC) -bootclasspath $(BCP) -classpath "$(CP)" *.java

classes: all preverify

clean: emacsclean
	\rm -f *.class
	\rm $(ROOT)/classes/gov/nist/siplite/stack/*.class


