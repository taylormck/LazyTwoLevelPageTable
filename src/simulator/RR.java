package simulator;
import java.util.ArrayList;
import java.util.List;


/**
 * Title:        
 * Description:
 * Copyright:    Copyright (c) 2012
 * Company:      University of Texas at Austin
 * @author R.L. Rockhold, Ph.D.
 * @version 1.0
 */


public class RR implements IScheduler {

	private List<IProcess> m_runQueue = new ArrayList<IProcess>();

	/**
	 * This is called when a new process enters the system
	 * and is ready to be scheduled.
	 * @param p  The new process.
	 */
	public void processStarted(IProcess p) {
		m_runQueue.add(p);
	}

	/**
	 * This is called when the process's
	 * assigned quantum has expired (i.e. when
	 * the process didn't block voluntarily).
	 * The process has been "undispatched" and should
	 * be added back to the scheduler's ready-queue.
	 * The scheduler's scheduler() method will be called very soon 
	 *   after this call.
	 * @param p The process whose quantum expired
	 * @param cpu  The cpu that had been running the process.
	 */
	public void quantumExpired(IProcess p, ICPU cpu, int ticks_used) {
		m_runQueue.add(p);
	}
	
	/**
	 * Called when a CPU is looking for work.
	 * Remember to change the quantum for this process if
	 * the default (4) is not appropriate.
	 * @param cpu
	 * @return The next process to run, or null if none are ready.
	 */
	public IProcess schedule(ICPU cpu) {
		if (m_runQueue.size() > 0) {
			IProcess p = m_runQueue.remove(0);
			// Uncomment the next line to see the burst history, which
			//  could be a good mechanism for predicting priority and future burst sizes.
			// Simulator.info("Dispatching " + p + " with burst history " + p.getBurstHistory());
			return p;
		}
		return null;
	} 

	/**
	 * Informational.  This process has left-the-building.
	 * @param p
	 */
	public void processExited(IProcess p) {
		return;
	}

	/**
	 * Informational.
	 * This process was execution, but has blocked.
	 * The scheudler's schedule() method will soon be called.
	 * @param p
	 * @param cpu
	 */
	public void processBlocked(IProcess p, ICPU rlrcpu, int ticks_used) {
		return;
	}

	/**
	 * This process is no longer blocked and should be added
	 * to the scheduler's list of ready processes.
	 * @param p
	 */
	public void unblocked(IProcess p) {
		m_runQueue.add(p);
	}  
	
	public void quantumInterrupted(IProcess p, ICPU cpu, int ticks_used) {
	    return;
	}
	
}
