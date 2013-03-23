package org.linphone.jortp;

import java.util.Vector;

public class Logger {
	static public interface Handler {
		public void log(String loggerName, int level, String levelName, String msg, Throwable e);
	}
	static Vector sLoggers=new Vector();
	private String mDomain;
	private int mMask;
	
	
	public static final int Fatal=1<<4;
	public static final int Error=1<<3|Fatal;
	public static final int Warn=1<<2|Error;
	public static final int Info=1<<1|Warn;
	public static final int Debug=1|Info;
	static int sDefaultLevel=Warn;
	static Handler sExternalHandler;
	
	public Logger(String domain){
		if (domain==null) domain="";
		mDomain=domain;
		mMask=sDefaultLevel;
		registerLogger(this);
	}
	public void setLogMask(int mask){
		mMask=mask|Fatal;
	}
	public int getLogMask(){
		return mMask;
	}
	public boolean isLevelEnabled(int level){
		return (level & mMask)!=0;
	}
	public void enableLogLevel(int level){
		mMask=mMask|level;
	}
	public void setLogLevel(int level){
		mMask=level;
	}
	public void disableLogLevel(int level){
		mMask&=~mMask;
	}
	public void log(int level, String msg, Throwable e){
		if ((level > mMask)) return;
		if (sExternalHandler !=null) {
			sExternalHandler.log(mDomain, level, getLevelName(level), msg, e);
		} 
		StringBuffer sb;

		sb=new StringBuffer();
		sb.append(mDomain);
		sb.append("-");
		sb.append(getLevelName(level));
		sb.append(":");
		sb.append(msg);
		if (e!=null) {
			sb.append(" ["+e.getMessage()+"]");
		}
		System.out.println(sb.toString());
		if (e!=null) {
			e.printStackTrace();
		}

	}
	public void debug(String msg, Throwable e){
		log(Debug,msg,e);
	}
	public void debug(String msg){
		log(Debug,msg,null);
	}
	public void info(String msg){
		log(Info,msg,null);
	}
	public void info(String msg, Throwable e){
		log(Info,msg,e);
	}
	public void warn(String msg, Throwable e){
		log(Warn,msg,e);
	}
	public void error(String msg, Throwable e){
		log(Error,msg,e);
	}
	public void fatal(String msg, Throwable e){
		log(Fatal,msg,e);
	}
	public String getLevelName(int level){
		switch(level){
		case Debug:
			return "Debug";
		case Info:
			return "Info";
		case Warn:
			return "Warn";
		case Error:
			return "Error";
		case Fatal:
			return "Fatal";
		default:
			return "undef";
		}
	}
	public synchronized static Logger getLogger(String domain){
		int i;
		for (i=0;i<sLoggers.size();++i){
			Logger l=(Logger)sLoggers.elementAt(i);
			if (l!=null && l.getDomain().equals(domain))
				return l;
		}
		return new Logger(domain);
	}
	static synchronized public void setGlobalLogLevel(int level) {
		for (int i=0;i<sLoggers.size();++i){
			Logger l=(Logger)sLoggers.elementAt(i);
			l.setLogLevel(level);
		}
		sDefaultLevel = level;
		
	}
	public String getDomain() {
		return mDomain;
	}
	private synchronized static void registerLogger(Logger logger){
		sLoggers.addElement(logger);
	}
	public void error(String string) {
		error(string,null);
		
	}
	public void warn(String string) {
		warn(string,null);
	}
	public static void setLogHandler(Handler anHandler) {
		sExternalHandler = anHandler;
	}

}
