ROOT=../../../..
PACKAGE=gov.nist.siplite
include $(ROOT)/build-config.j2me

all:
	$(JAVAC)  -bootclasspath $(BCP) -classpath "$(CP)" *.java parser/*.java message/*.java

clean: emacsclean
	\rm $(ROOT)/classes/gov/nist/siplite/*.class



