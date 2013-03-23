ROOT=../../../../..
PACKAGE=gov.nist.siplite.address
include $(ROOT)/build-config.j2me

all: 	
	$(JAVAC)  -bootclasspath $(BCP) -classpath "$(CP)" *.java


classes: all preverify

clean: emacsclean
	rm $(ROOT)/classes/gov/nist/siplite/address/*.class
