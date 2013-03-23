ROOT=../../../../../
PACKAGE=gov.nist.siplite.parser
include $(ROOT)/build-config.j2me

all:
	$(JAVAC)  -bootclasspath $(BCP) -classpath "$(CP)" *.java

clean: emacsclean
	\rm -f $(ROOT)/classes/gov/nist/siplite/parser/*.class


contactparse:
	$(JAVA) $(PACKAGE).ContactParser

authparse: AuthorizationParser.class
	$(JAVA) $(PACKAGE).AuthorizationParser

proxyauthparse: ProxyAuthenticateParser.class
	$(JAVA) $(PACKAGE).ProxyAuthenticateParser
