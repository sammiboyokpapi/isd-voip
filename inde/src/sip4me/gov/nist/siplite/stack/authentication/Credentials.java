/*
 * Credential.java
 * 
 * Created on Feb 2, 2004
 *
 */
package sip4me.gov.nist.siplite.stack.authentication;

/**
 * @author Jean Deruelle
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class Credentials {
	private String userName=null;
	private String password=null;
	private String realm=null;
	/**
	 * Creates a new Credential composed of a username password and realm
	 * for later user by the API when a 401 or 407 Message will be received
	 * @param userName - the user name 
	 * @param password - the password
	 * @param realm - the realm 
	 */
	public Credentials(
					  String userName,
					  String password, 
					  String realm) {
		this.userName=userName;
		this.password=password;
		this.realm=realm;
	}
	
	/**
	 * Get the user name for this credential
	 * @return the user name for this credential
	 */
	public String getUserName(){
		return userName;
	}

	/**
	 * Set the user name for this credential
	 * @param userName - the user name for this credential 
	 */
	public void setUserName(String userName){
		this.userName=userName;
	}
	
	/**
	 * Get the password for this credential
	 * @return the password for this credential
	 */
	public String getPassword(){
		return password;
	}

	/**
	 * Set the password for this credential
	 * @param password - the password for this credential 
	 */
	public void setPassword(String password){
		this.password=password;
	}
		
	/**
	 * Get the realm for this credential
	 * @return the realm for this credential
	 */
	public String getRealm(){
		return realm;
	}

	/**
	 * Set the realm for this credential
	 * @param realm - the realm for this credential 
	 */
	public void setRealm(String realm){
		this.realm=realm;
	}
}
