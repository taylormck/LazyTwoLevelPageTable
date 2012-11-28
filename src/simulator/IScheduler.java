package simulator;
/**
 * Title:        
 * Description:
 * Copyright:    Copyright (c) 2012
 * Company:      University of Texas at Austin
 * @author R.L. Rockhold, Ph.D.
 * @version 1.0
 */
public interface IScheduler {
	
	/**
	 * This is called when a new process enters the system
	 * and is ready to be scheduled.
	 * @param p  The new process.
	 */
	void processStarted(IProcess p);
	
	
	/**
	 * This is called when the process's
	 * assigned quantum has expired (i.e. when
	 * the process didn't block voluntarily).
	 * The process has been "undispatched" and should
	 * be added back to the scheduler's ready-queue.
	 * The scheduler's schedule() method will be called very soon 
	 *   after this call.
	 * @param p The process whose quantum expired
	 * @param cpu  The cpu that had been running the process.
	 * @param ticks_given Number of ticks consumed during last dispatch
	 */
	void quantumExpired(IProcess p, ICPU cpu, int ticks_given);
	
	/**
	 * Called when a CPU is looking for work.
	 * Remember to change the quantum for this process if
	 * the default (4) is not appropriate.
	 * @param cpu
	 * @return The next process to run, or null if none are ready.
	 */
	IProcess schedule(ICPU cpu);


	/**
	 * Informational.  This process has left-the-building.
	 * @param p
	 */
	void processExited(IProcess p);


	/**
	 * Informational.
	 * This process was executing, but has blocked for I/O.
	 * The scheudler's schedule() method will soon be called.
	 * @param p
	 * @param cpu
	 * @param ticks_given Number of ticks consumed during last dispatch before blocking
	 */
	void processBlocked(IProcess p, ICPU cpu, int burst_given);


	/**
	 * This process is no longer blocked (I/O complete) and should be added
	 * to the scheduler's list of ready processes.
	 * @param p
	 */
	void unblocked(IProcess p);


    /**
     * @param p
     * @param cpu
     * @param ticks_given Number of ticks consumed during last dispatch
     */
    void quantumInterrupted(IProcess p, ICPU cpu, int ticks_given);
}
