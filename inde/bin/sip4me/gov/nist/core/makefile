ROOT=../../../../
PACKAGE=gov.nist.core
include $(ROOT)/build-config.j2me

all:
	$(JAVAC)  -bootclasspath $(BCP) -classpath "$(CP)" *.java

copyfiles:
	cp Debug.j2me Debug.java
	cp LogWriter.j2me LogWriter.java

save:
	cp Debug.java Debug.j2me
	cp LogWriter.java LogWriter.j2me

clean: emacsclean
	
classes: all preverify



