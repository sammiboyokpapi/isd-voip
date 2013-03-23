package sip4me.gov.nist.siplite.parser;

import sip4me.gov.nist.core.HostNameParser;
import sip4me.gov.nist.core.HostPort;
import sip4me.gov.nist.core.NameValue;
import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.core.Token;
import sip4me.gov.nist.siplite.header.Header;
import sip4me.gov.nist.siplite.header.Protocol;
import sip4me.gov.nist.siplite.header.ViaHeader;
import sip4me.gov.nist.siplite.header.ViaList;

/** Parser for via headers.
*
*@version  JAIN-SIP-1.1
*
*@author Olivier Deruelle 
*@author M. Ranganathan <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/
public class ViaParser extends HeaderParser {
	ViaParser() {}

  	public ViaParser(String via) {
		super(via);
	}

        public ViaParser(Lexer lexer) {
		super(lexer);
	}
	
	/**  a parser for the essential part of the via header.
	*/
        private void parseVia(ViaHeader v) throws ParseException {

        	// The protocol   
        	lexer.match(TokenTypes.ID);
        	Token protocolName = lexer.getNextToken();

        	this.lexer.SPorHT();
        	// consume the "/"
        	lexer.match('/');
        	this.lexer.SPorHT();
        	lexer.match(TokenTypes.ID);
        	this.lexer.SPorHT();
        	Token protocolVersion = lexer.getNextToken();

        	this.lexer.SPorHT();

        	// We consume the "/"
        	lexer.match('/');
        	this.lexer.SPorHT();
        	lexer.match(TokenTypes.ID);
        	this.lexer.SPorHT();

        	Token transport = lexer.getNextToken();
        	this.lexer.SPorHT();

        	Protocol protocol=new Protocol();
        	protocol.setProtocolName(protocolName.getTokenValue());
        	protocol.setProtocolVersion(protocolVersion.getTokenValue());
        	protocol.setTransport(transport.getTokenValue());
        	v.setSentProtocol(protocol);

        	// sent-By  
        	HostNameParser hnp = new HostNameParser(this.getLexer());
        	HostPort hostPort= hnp.hostPort();
        	v.setSentBy(hostPort);

        	// Ignore blanks
        	this.lexer.SPorHT();

        	// parameters
        	while (lexer.lookAhead(0) == ';') {
        		this.lexer.match(';');
        		this.lexer.SPorHT();
        		NameValue nameValue=this.nameValue();
        		String name=nameValue.getName();
        		nameValue.setName( name.toLowerCase() );
        		v.setParameter(nameValue);
        		this.lexer.SPorHT();
        	}

        	if (lexer.lookAhead(0) == '(') {
        		this.lexer.selectLexer("charLexer");
        		lexer.consume(1);
        		StringBuffer comment=new StringBuffer();
        		boolean cond=true;
        		while( true ) {
        			char ch = lexer.lookAhead(0);
        			if ( ch == ')' ) {
        				lexer.consume(1);
        				break;
        			} else if ( ch == '\\') {
        				// Escaped character
        				Token tok = lexer.getNextToken();
        				comment.append(tok.getTokenValue());
        				lexer.consume(1);
        				tok = lexer.getNextToken();
        				comment.append(tok.getTokenValue());
        				lexer.consume(1);
        			} else if ( ch == '\n') {
        				break;
        			} else  {
        				comment.append(ch);
        				lexer.consume(1);
        			}
        		}
        		v.setComment(comment.toString());
        	}

        }
                    
	
	public Header  parse() throws ParseException {
	     if (debug) dbg_enter("parse");
	     try {
		ViaList viaList= new ViaList();
		// The first via header.
		this.lexer.match (TokenTypes.VIA);
		this.lexer.SPorHT(); 	// ignore blanks
		this.lexer.match(':');  // expect a colon.
		this.lexer.SPorHT(); 	// ingore blanks.
                
		while(true) {
                	ViaHeader v=new ViaHeader();
			parseVia(v);
			viaList.add(v);
			this.lexer.SPorHT();  // eat whitespace.
			if (this.lexer.lookAhead(0) == ',') {
				this.lexer.consume(1); // Consume the comma
				this.lexer.SPorHT();   // Ignore space after.
			} 
			if (this.lexer.lookAhead(0) == '\n') break;
		}
                this.lexer.match('\n');
                return viaList;
	      } finally {
		 if (debug) dbg_leave("parse");
	      }
          
	}

/*	public static void main(String args[]) throws ParseException {
		String via[] = {
				"Via: SIP/2.0/UDP 135.180.130.133\n",
				"Via: SIP/2.0/UDP 166.34.120.100;branch=0000045d-00000001"+
				",SIP/2.0/UDP 166.35.224.216:5000\n",
				"Via: SIP/2.0/UDP sip33.example.com,"+
				" SIP/2.0/UDP sip32.example.com (oli),"+
				"SIP/2.0/UDP sip31.example.com\n",
				"Via: SIP/2.0/UDP host.example.com;received=135.180.130.133;"+
				" branch=C1C3344E2710000000E299E568E7potato10potato0potato0\n",
				"Via: SIP/2.0/UDP company.com:5604 ( Hello )"+
				", SIP /  2.0  /  UDP 135.180.130.133\n",
				"Via: SIP/2.0/UDP 129.6.55.9:7060;received=stinkbug.antd.nist.gov\n",

				"Via: SIP/2.0/UDP ss2.wcom.com:5060;branch=721e418c4.1"+
				", SIP/2.0/UDP ss1.wcom.com:5060;branch=2d4790.1"+
				" , SIP/2.0/UDP here.com:5060( Hello the big world) \n"


				,"Via: SIP/2.0/UDP ss1.wcom.com:5060;branch=2d4790.1\n",
				"Via: SIP/2.0/UDP first.example.com:4000;ttl=16"+
				";maddr=224.2.0.1 ;branch=a7c6a8dlze.1 (Acme server)\n"
		};

		for (int i = 0; i < via.length; i++ ) {
			ViaParser vp = 
				new ViaParser(via[i]);
			ViaList vl = (ViaList) vp.parse();

			System.out.println("encoded = " + vl.encode());
			System.out.println("first = " + vl.getFirst());
			System.out.println();
		}

	}*/

}
