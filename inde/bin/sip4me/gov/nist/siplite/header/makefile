ROOT=../../../../../
PACKAGE=gov.nist.siplite.header
include $(ROOT)/build-config.j2me

all:
	$(JAVAC)  -bootclasspath $(BCP) -classpath "$(CP)" *.java

classes: clean all preverify

clean: emacsclean
	\rm -f $(ROOT)/classes/gov/nist/siplite/header/*.class


