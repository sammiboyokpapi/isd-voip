/*
 * RefreshManager.java
 * 
 * Created on Apr 8, 2004
 *
 */
package sip4me.gov.nist.microedition.sip;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Timer;

import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.siplite.message.Request;
import sip4me.nist.javax.microedition.sip.SipClientConnection;
import sip4me.nist.javax.microedition.sip.SipConnectionNotifier;
import sip4me.nist.javax.microedition.sip.SipRefreshListener;

/**
 * @author Jean Deruelle <jean.deruelle@nist.gov>
 * 
 *         <a href="{@docRoot} /uncopyright.html">This code is in the public
 *         domain.</a>
 */
public class RefreshManager {
	/**
	 * The unique instance of this class
	 */
	private static RefreshManager instance = null;
	/**
	 * The hashtable keeping a mapping between the refreshID and the refresh
	 * Tasks
	 */
	private Hashtable refreshTable = null;
	/**
	 * generator of task id
	 */
	private int idGenerator = 1;

	/**
	 * Creates a new instance of the RefreshManager
	 */
	private RefreshManager() {
		refreshTable = new Hashtable();
	}

	/**
	 * Returns the instance of RefreshManager
	 * 
	 * @return the instance of RefreshManager singleton
	 */
	public static synchronized RefreshManager getInstance() {
		if (instance == null)
			instance = new RefreshManager();
		return instance;
	}

	/**
	 * Creates a new RefreshTask with a specific id, schedules it and keep the
	 * mapping between this newly created task and the id
	 * 
	 * @param request
	 *            - the request for which a refresh task must be created
	 * @param sipConnectionNotifier
	 *            - the sipConnectionNotifier used to send the request
	 * @param sipRefreshListener
	 *            - the callback interface used listening for refresh event on
	 *            this task
	 * @return the id of the newly created task
	 */
	public int createRefreshTask(Request request,
			SipConnectionNotifier sipConnectionNotifier,
			SipRefreshListener sipRefreshListener,
			SipClientConnection sipClientConnection) {
		int taskId = idGenerator++;
		RefreshTask refreshTask = new RefreshTask(request,
				sipConnectionNotifier, sipRefreshListener, sipClientConnection);
		refreshTable.put(String.valueOf(taskId), refreshTask);
		return taskId;
	}

	/**
	 * Schedules the task whose id is given in parameter for the expires
	 * 
	 * @param taskId
	 *            - the id of the task to schedule
	 * @param expires
	 *            - the expires time,so the delay until when the stack must
	 *            schedule the task. If it is -1, it means that the expires has
	 *            already been given when the task was created.
	 */
	public void scheduleTask(String taskId, int expires) {
		RefreshTask refreshTask = (RefreshTask) refreshTable.get(taskId);
		if (refreshTask == null)
			return;
		if (expires == -1)
			return;
		if (expires >= 0) {
			
			Timer timer;
			try {
				timer = StackConnector.getInstance().getStackTimer();
				
				// Once the task has been processed, it can be scheduled again
				// so a new one is created
				refreshTask = new RefreshTask(refreshTask.getRequest(), refreshTask
						.getSipConnectionNotifier(), refreshTask
						.getSipRefreshListener(), refreshTask
						.getSipClientConnection());
				refreshTable.put(taskId, refreshTask);
				// Send the refresh BEFORE the timer expires on the server
				timer.schedule(refreshTask, expires * 850);
				
				if (LogWriter.needsLogging)
					LogWriter.logMessage(LogWriter.TRACE_MESSAGES, "Scheduling refresh in " + (expires*850) + " ms for taskID " + taskId + " RefreshTask " + refreshTask);
				
			} catch (IOException e) {
				if (LogWriter.needsLogging)
					LogWriter.logMessage(LogWriter.TRACE_EXCEPTION, "Can't get stackTimer to schedule refresh for taskID " + taskId);
			}
		}
	}

	/**
	 * Return the task mapping the taskId
	 * 
	 * @param taskId
	 *            - the id of the task to retrieve
	 * @return the Refresh task mapping the taskId
	 */
	public RefreshTask getTask(String taskId) {
		if (taskId == null)
			return null;
		return (RefreshTask) refreshTable.get(taskId);
	}

	/**
	 * Removes a task
	 * 
	 * @param taskId
	 *            - the task id of the task to remove
	 */
	public void removeTask(String taskId) {
		refreshTable.remove(taskId);
	}
}
