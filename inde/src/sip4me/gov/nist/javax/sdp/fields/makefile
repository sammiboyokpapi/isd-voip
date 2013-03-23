# this works with the gnu make tool. If you are working with windows, please install cygwin to get
# gnumake (goto http://www.sourceware.cygnus.com).

ROOT=../../../../../../

include  $(ROOT)/build-config.j2me

all: 
	$(JAVAC)  -bootclasspath $(BCP) -classpath "$(CP)" *.java 

clean:  emacsclean
	$(RM) *.class
	$(RM) $(DESTINATION)/gov/nist/javax/sdp/fields/*.class

