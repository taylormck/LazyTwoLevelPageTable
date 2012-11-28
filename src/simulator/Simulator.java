package simulator;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import os.OS;
import cpu.CPU;



/**
 * Title:        
 * Description:
 * Copyright:    Copyright (c) 2012
 * Company:      University of Texas at Austin
 * @author R.L. Rockhold, Ph.D.
 * @version 1.0
 */


public class Simulator { 
	
// Public stuff.  Student code can/must use theses.
    
    /**
     * For converting a long to a hex string
     * @param ip_virtual
     * @return
     */
    public static String hex(long value) {
        return "0x" + Long.toHexString(value);
    }

    /**
     * Get the current Simulated Time.
     * Repeated calls will get the same value if time
     * has not advanced.
     * @return
     */
	public static int getTime() {
		return m_current_time;
	}
	
	private static int mono_int = 0;
	/**
	 * Always returns a unique, monotonically increasing value.
	 * @return
	 */
	public static synchronized int getMonotonicInt() {
		int rc = ++mono_int;
		return rc;
	}
    
// Stuff below here is either package or private

	static final int BYTES_PER_INSTRUCTION = 4;
	
	/**
	 * CPUs call this when they're looking for work.
	 * This method will ask the Scheduler for the next "ready"
	 * process, and "peek" at what the process will do next.
	 * This code handles the cases where the process has asked to:
	 *    exit
	 *    do I/O
	 * Only when a process is found that wants a CPU burst will this
	 * call return.
	 * If there are no processes ready, this code will call the "idle loop".
	 * @param cpu
	 * @return The next process that has a CPU burst
	 */
	
	public static IProcess schedule(ICPU cpu) {
		do {
			IProcess p = m_os.schedule(cpu);
			if (p == null) {
				// No ready processes.  Call the "idle loop".
				idleLoop(cpu);
				continue;
			}
			return p;
		} while (true);
	}
	
	
	
	/**
	 * A CPU thread uses this when a process has been dispatched.
	 * The caller will be blocked for the number of ticks specified.
	 * Once that much "time" has passed, the method will return to the caller,
	 *   with the guarantee that exactly "ticks" time has passed.
	 * @param cpu  The calling CPU
	 * @param p The process being executed
	 * @param ticks The number of ticks the CPU will "execute".
	 */
	
	public static void executeProcess(ICPU cpu, IProcess p, int ticks) {
		if (ticks <= 0) {
			throw new IllegalArgumentException("ticks must be > 0: " + ticks);
		}
		//info("CPU " + cpu.getID() + " running " + p + " [quantum=" + p.getQuantum() + "] at time " + m_current_time + " for " + ticks + " ticks.");
		int wake_tick = m_current_time + ticks;
		WaitingElement we = new WaitingElement(wake_tick, cpu);
		synchronized(we) {
			synchronized (Simulator.class) {
				++ m_waiting_count;
				m_to_dos.add(we);
			}
			// directorLoop could try to awaken us, but
			//  we still hold the Integer's monitor
			try {
				do {
					we.wait();
				} while (wake_tick != m_current_time);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		p.incrCPUTime(ticks);
		//info("CPU " + cpu.getID() + " clock interrupt for  " + p + " at time " + m_current_time + " after " + ticks + " ticks.");

	}
	

	/**
	 * A CPU calls this when a process' CPU Burst has complete,
	 * either before the quantum expired, at exactly as the quantum expired.
	 * 
	 * @param cpu  The calling CPU
	 * @param p  The process whose burst was completed
	 * @param ticks_given
	 */
	static void processBlock(ICPU cpu, IProcess p, int ticks_given, int block_time) {
		Debug.info("Burst completed for " + p + " at time " + m_current_time + " after " + ticks_given + " ticks.");
		blockProcess(cpu, p, block_time);
		m_scheduler.processBlocked(p, cpu, ticks_given);
	}
	
	/**
	 * A CPU calls this when a quantum has expired the process blocked.
	 * 
	 * @param p
	 * @param cpu
	 * @param ticks_given
	 */
	public static void quantumExpired(ICPU cpu, IProcess p, int ticks_given) {
		Debug.info("Quantum expired for " + p + " at time " + m_current_time + " after " + ticks_given + " ticks.");
		m_os.quantumExpired(p, cpu, ticks_given);
	}
	
	
// Stuff below here is interesting, but not required reading, and
//    should not be called by Student code

	public static final String INVALID_INSTRUCTION = "Invalid Instruction";
	
	static class WaitingElement {
		private Object waiter;
		private int wake_tick;
		private boolean waiting = false;
		WaitingElement(int wake_tick, Object waiter) {
			this.wake_tick = wake_tick;
			this.waiter = waiter;
		}
		int getTick() {
			return wake_tick;
		}
		Object getWaiter() {
			return waiter;
		}
		WaitingElement setTick(int tick) {
			wake_tick = tick;
			return this;
		}
		boolean isWaiting() { return waiting; }
		void setWaiting(boolean t) { waiting = t; }
	}
	
	
	private static int m_cpu_count = 0;
	private static Thread[] m_cpu_threads;
	private static int m_cpu_ticks = 0;
	private static ICPU[] m_cpus;
	
	private static int m_current_time = 0;
	private static int m_dispatches = 0;
	private static int m_duration = 0;
	private static int m_idle_count = 0;
	private static int m_process_count = 0;
	private static final String CONFIG_DIR = "configs" + java.io.File.separator;

	private static IScheduler m_scheduler;
	private static List<WaitingElement> m_to_dos = Collections.synchronizedList(
							new ArrayList<WaitingElement>());
		
	private static int m_total_process_count = 0;

	private static int m_waiting_count = 0;
    private static OS m_os;
    private static SystemInfo m_osinfo;
    private static boolean m_exited;
	
	static synchronized void blockProcess(ICPU cpu, IProcess p, int ticks) {
		if (ticks <= 0) {
			throw new IllegalArgumentException("ticks must be > 0: " + ticks);
		}
		Debug.info("blocking " + p + " at time " + m_current_time + " for " + ticks + " ticks.");
		m_to_dos.add(new WaitingElement(m_current_time + ticks, p));
	}

	private static void directorLoop() {
		/*   lower our priority -- we can't be starved since
		 *   we don't do useful work until all other threads are blocked.
		 */
		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
		boolean exit = false;
		do {
			Thread.yield();
			synchronized (Simulator.class) {
				if (m_waiting_count == m_cpu_count) {
			    
					if (m_process_count == 0) {
						exit = true;
					} else {
						/* Time to make the donuts */
						// info("Everyone's blocked at time " + getTime());
						/* See what happens next .... */
						// There must be something in our to_do list
						int cnt = m_to_dos.size();
						if (cnt == 0) {
							// Fault!
							Debug.log("**** Error **** All CPUs are blocked in idle and no processes are waiting!");
							throw new IllegalStateException("All CPUs are blocked in idle and no processes are waiting!");
						}
						// Find the next tick where something happens
						int next_tick = getNextWaitingTick();	
						Debug.info("Advancing time to " + next_tick);
						//  Wake up anyone with wake_time < next_tick
						m_current_time = next_tick;
						//for (WaitingElement we: m_to_dos) {
						for (final Iterator<WaitingElement> it=m_to_dos.iterator(); it.hasNext();) {
							final WaitingElement we = it.next();
							synchronized(we) {
								int wake_tick = we.getTick();
								if (wake_tick < m_current_time) {
									Debug.log("Error. Time warp. Now: " + m_current_time + " but we on to_dos has time " + wake_tick + "for we " + we);
									throw new IllegalStateException("Error. Time warp. Now: " + m_current_time + " but we on to_dos has time " + wake_tick + "for we " + we);
								}
								if (wake_tick == m_current_time) {
									it.remove();
									we.setWaiting(false);
									// See what type the value is
									Object val = we.getWaiter();
									if (val instanceof IProcess) {
										// Start it up!
									    IProcess p = ((IProcess) val);
										if (p.isStarted()) {
											// Now it's unblocked
											Debug.info("Unblocking process " + p + " at time " + m_current_time + ".");
											m_scheduler.unblocked(p);	// TODO change to OS?
											Debug.info("Unblocking " + p + " at time " + m_current_time + ".");
										} else {
										    // tell the OS
											//boolean rc = m_os.processStarted(p);
											boolean rc = startProcess(p);
											if (!rc) {
											    Simulator.processKill(p);
											    Debug.info("Unable to start process " + p);
											} else {
												Debug.info("Starting " + p + " at time " + m_current_time + ".");
											}
										}
									} else if (val instanceof ICPU) {
										we.notify();
										-- m_waiting_count;
									} else {
										throw new IllegalStateException("Unknown we object: " + val);
									}
								}
							}
						}	// end for
						
						// Now we need to wake all the CPUs that were idle
						if (m_idle_count != 0) {
							Simulator.class.notifyAll();
							m_waiting_count -= m_idle_count;
						}
						m_idle_count = 0;
						
					} // End if/else process-count
				} // End if wait_count == cpu_count
			}  // End synchronized block
			
		} while (!exit);
		/* No more work! */
		m_exited = true;
	}
	
    private static int bytes2pages(int bytes) {
        return (bytes + (1<<m_osinfo.getPageSizeBytesLog2() -1)) >> m_osinfo.getPageSizeBytesLog2();
    }

    private static long page_to_vaddr(int page_num) {
        return page_num << m_osinfo.getPageSizeBytesLog2();
    }
    
	private static boolean startProcess(IProcess p) {
		m_os.initProcess(p);
		int num_instruction_pages =  bytes2pages(p.getTextByteCount());
		boolean rc = m_os.syscallAlloc(p, 0, num_instruction_pages);
        //boolean rc = as.createMapping(m_last_instruction_page_number + 1, 0, true);
		if (rc) {
			p.setStartingDataVaddr(page_to_vaddr(num_instruction_pages));
			m_os.processStarted(p);
		}
        return rc;
		
	}

	private static void do_CPUs(Properties props, OS os) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		//m_cpu_count = Integer.parseInt(props.getProperty("CPUs").trim());
	    m_cpu_count = 1;	// For this project
		//log("CPUs: " + m_cpu_count);
		m_cpus = new ICPU[m_cpu_count];
		m_cpu_threads = new Thread[m_cpu_count];
		for (int c=0; c< m_cpu_count; ++c) {
			m_cpus[c] = new CPU(c, os, m_osinfo);
			m_cpu_threads[c] = new Thread(m_cpus[c], "CPU " + c);
			m_cpu_threads[c].setDaemon(true);
			m_cpu_threads[c].start();
			
		}
	}

	private static void do_Processes(Properties props) 
		throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException {
		//log("Using process constructor " + proc_constructor);
		String p_names = props.getProperty("Processes").trim();
		Debug.log("Processes: " + p_names);
		StringTokenizer stk = new StringTokenizer(p_names);
		// int p_num = 0;
		while (stk.hasMoreTokens()) {
			String p_name = stk.nextToken();
			/* Get the specific info about this process */
			String p_trace = CONFIG_DIR + props.getProperty("Process." + p_name + ".trace").trim();
			String s_start = props.getProperty("Process." + p_name + ".start", "0").trim();
			Integer i_start = Integer.decode(s_start);
			IProcess proc = new Lab3Process(m_process_count++, p_name, p_trace);
			/* Add it to our list of to-dos */
			m_to_dos.add(new WaitingElement(i_start.intValue() , proc));	
			++ m_total_process_count;									
		}
	}

	
	private static void do_Scheduler(final IScheduler scheduler) {
		Simulator.m_scheduler = scheduler;
	}

	private static OS do_OS(Properties props) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		int p_b_log2 = Integer.parseInt(props.getProperty("PageBytesLog2"));
		Debug.log("PageBytesLog2: " + p_b_log2);
		int f_c_log2 = Integer.parseInt(props.getProperty("FrameCountLog2"));
		Debug.log("FrameCountLog2: " + f_c_log2);
		int p_c_log2 = Integer.parseInt(props.getProperty("PageCountLog2"));
		Debug.log("PageCountLog2: " + p_c_log2);
		int l1_log2 = Integer.parseInt(props.getProperty("LevelOnePTEntryCountLog2"));
		Debug.log("LevelOnePTEntryCountLog2: " + l1_log2);
		int tlb_entries_log2 = Integer.parseInt(props.getProperty("TLBEntryCountLog2"));
		if (CPUBase.TLB_SUPPORTED) Debug.log("TLBEntryCountLog2: " + tlb_entries_log2);
		m_osinfo = new SystemInfo(p_b_log2, f_c_log2, p_c_log2, l1_log2, tlb_entries_log2, m_scheduler);
		OS os = new OS(m_osinfo);
		return os;
		
	}
	
	static void finishedData(
				IProcess p, 
				int started_time, 
				int current_time, 
				int executed_ticks, 
				int context_switches) {
		Debug.info(p + " completed at time " + current_time + 
				", used " + executed_ticks + " ticks, and required " +
				context_switches + " dispatches.");
		Debug.log(p + " completed at time " + current_time);
		m_duration += current_time -started_time;
		m_dispatches += context_switches;
		m_cpu_ticks += executed_ticks;
		
	}
	
	private static int getNextWaitingTick() {
		int low_tick = Integer.MAX_VALUE;
		for (Iterator<WaitingElement> it=m_to_dos.iterator(); it.hasNext();) {
			int t = ((WaitingElement) it.next()).getTick();
			if (t < low_tick) {
				low_tick = t;
			}
		}
		return low_tick;
	}
	//  Note that idle CPU threads block on Scheduler.class monitor.
	//    No need for a queue since they will all be awakened at once
	static synchronized void idleLoop(ICPU cpu) {
		++ m_waiting_count;
		++ m_idle_count;
		Debug.info("CPU " + cpu.getID() + " going idle at time " + m_current_time + ".");
		
		try {
			Simulator.class.wait();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//info("CPU " + cpu.getID() + " re-thinking going idle at time " + m_current_time + ".");
		return;
	}
	
	public static void init(Properties props) throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException {
	}
	
	private static void do_sim() {
		/* Pass control to our director */
		directorLoop();
	}
	
	public static boolean isRunning() {
	    return ! m_exited;
	}
	
	private static void usage() {
		System.err.println("Usage:  java <package-name>/Simulator [-v[v]] config-properties-file<.properties>  ");
		System.err.println("For example:  java simulator/Simulator -v simple_config ");
	}
	
	public static void main(String[] args) throws IOException, SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
		
		if (args.length < 1) {
			Simulator.usage();
			return;
		}
		int arg_next = 0;
		String arg0 = args[0].trim();
		if (arg0.startsWith("-v")) {
			Debug.verbose = 1;
			if (arg0.startsWith("-vv")) {
				Debug.verbose = 2;
			}
			++arg_next;
			if (args.length - arg_next < 1) {
				Simulator.usage();
				return;
			}
			arg0 = args[arg_next].trim();
			if (arg0.startsWith("-t")) {
				CPU.TLB_SUPPORTED = true;
				++arg_next;
				if (args.length - arg_next < 1) {
					Simulator.usage();
					return;
				}
			}
			
			
		} else if (arg0.startsWith("-t")) {
			CPU.TLB_SUPPORTED = false;
			++arg_next;
			if (args.length - arg_next < 1) {
				Simulator.usage();
				return;
			}
			arg0 = args[arg_next].trim();
			if (arg0.startsWith("-v")) {
				Debug.verbose = 1;
				if (arg0.startsWith("-vv")) {
					Debug.verbose = 2;
				}
				++arg_next;
				if (args.length - arg_next < 1) {
					Simulator.usage();
					return;
				}
			}
		}	
			
		 		
		final String pfile_name = CONFIG_DIR + args[arg_next] + ".properties";
		
		final Properties props = new Properties();
		Debug.log("Simulator using config file " + pfile_name);
		final FileInputStream in = new FileInputStream(pfile_name);
		props.load(in);
		/* Create the Scheduler */
		do_Scheduler(new RR());
		/* Create each user process and add it to the incoming queue */
		do_Processes(props);
		/* Create OS */
		m_os = do_OS(props);
		/* Create each CPU -- passing them their Scheduler instance */
		do_CPUs(props, m_os);	    
		do_sim();
		showStatistics();
	}

	/**
     * 
     */
    private static void showStatistics() {
		((OSBase)m_os).checkSwaps();
        Debug.log("Finished at time " + m_current_time);
		for (int c=0; c< m_cpus.length; ++c) {
		    Debug.log("Statistics for " + m_cpus[c]);
		    if (!CPU.TLB_SUPPORTED) {
			    Debug.log("     Hits: " + m_cpus[c].getXlateHits() + 
			            ". Faults: " + m_cpus[c].getXlatefaults() + 
			            ". Invalidates: " + m_cpus[c].getTLBInvalidates() + ".");
		    } else {
		    Debug.log("     Hits: " + m_cpus[c].getXlateHits() + 
		            ". Load Cleans: " + m_cpus[c].getTLBXlateLoadCleans() + 
		            ". Load Steals: " + m_cpus[c].getTLBXlateLoadSteals() + 
		            ". Faults: " + m_cpus[c].getXlatefaults() + 
		            ". Invalidates: " + m_cpus[c].getTLBInvalidates() + ".");
		    }
		}
    }

    public static void processExited(ICPU cpu, IProcess p) {
		// Tell the scheduler
        m_os.processExiting(cpu, p);
		--m_process_count;
	}

    /**
     * @param cpu
     * @param p
     * @param ticks_given
     */
    static void quantumInterrupted(CPUBase cpu, IProcess p, int ticks_given) {
		Debug.info("Execution interrupted for " + p + " at time " + m_current_time + " after " + ticks_given + " ticks.");
		m_scheduler.quantumInterrupted(p, cpu, ticks_given);
        
    }

    public static boolean pageFaultInstruction(IProcess p, long ip_virtual, ICPU cpu) {
        Debug.info("Instruction page fault at " + 
        		hex(ip_virtual) + " in process " + p.getID() + " (page number " + (ip_virtual >> m_osinfo.getPageSizeBytesLog2()) + ")" );
        p.incrInstrFaults();
        boolean ok = m_os.pageFaultInstruction(cpu, p, ip_virtual);
        if (!ok) {
            Debug.log("Invalid instruction address for process " + p.getID() +
            		" at ip: " + hex(ip_virtual) + ". page number " + (ip_virtual >> m_osinfo.getPageSizeBytesLog2()));
        }
        return ok;
    }

    public static boolean pageFaultData(IProcess p, long ip, long i_paddr, long d_vaddr, ICPU cpu) {
        boolean ok = false;
        Debug.info("Data page fault at " + 
        		hex(ip) + " in process " + p.getID() + " for data address " + hex(d_vaddr) + " (page number " + (d_vaddr >> m_osinfo.getPageSizeBytesLog2()) + ")");
        
        p.incrInstrFaults();
        ok = m_os.pageFaultData(cpu, p, d_vaddr);
        if (!ok) {
            Debug.log("Invalid Data address for process " + p.getID() + 
            	" at ip: " + hex(ip) + " for data at virtual address " + hex(d_vaddr) + " (page number " + (d_vaddr >> m_osinfo.getPageSizeBytesLog2()) + ")");
        }
        return ok;
    }

    
    private static void processKill(IProcess p) {
        // Same as process Exited, except the OS already knows
		p.completed(m_current_time);
		--m_process_count;
    }

    private static final int INSTRUCTION_LENGTH = 4;
    /**
     * @param m_p
     * @param m_ip
     * @param i_paddr
     * @return
     */
    public static Instruction decode(IProcess m_p, long m_ip, long i_paddr) {
        // m_ip divided by instr_len is the index
        int trace_ndx = (int) m_ip / INSTRUCTION_LENGTH;
        String instr = m_p.getInstruction(trace_ndx);
        Instruction in = Instruction.buildInstruction(instr, m_ip);
        return in;
    }

    /**
     * @param e
     */

    /**
     * @param p
     * @param ip
     * @param ie
     */
    public static void instructionException(ICPU cpu, IProcess p, long ip, InstructionException ie) {
        IInstruction inst = ie.getInstruction();
        if (inst instanceof Syscall_Exit_Instruction) {
            processExited(cpu, p);
            return;
        }
        if (inst instanceof InvalidInstruction) {
            Debug.info("Invalid instruction: '" +
                    inst.getText() + "' in process " + p.getID() +
                    " at vaddr " + hex(ip));
            processExited(cpu, p);
            return;
        }
        if (inst instanceof SyscallAlloc_Instruction) {
            Debug.info("Allocation failed: '" +
                    inst.getText() + "' in process " + p.getID() +
                    " at vaddr " + hex(ip));
            processExited(cpu, p);
            return;
        }
        throw new IllegalStateException("Unrecognized state!");
    }

    /**
     * @param d_paddr
     * @return
     */
    static int loadWord(long d_paddr) {
    	byte [] memory = m_osinfo.getSystemMemory(); 
        int byte0 = (((int)memory[(int)d_paddr]) & 0x0FF); 
        int byte1 = (((int)memory[(int)d_paddr+1]) & 0x0FF) << 8; 
        int byte2 = (((int)memory[(int)d_paddr+2]) & 0x0FF) << 16; 
        int byte3 = (((int)memory[(int)d_paddr+3]) & 0x0FF) << 24;
        int rc = byte0 + byte1 + byte2 + byte3;
        return rc;
    }

    /**
     * @param d_paddr
     * @param data
     */
    static void storeWord(long d_paddr, int data) {
        byte byte0 = (byte) (data & 0x0FF);
        data = data >> 8;
        byte byte1 = (byte) (data & 0x0FF);
        data = data >> 8;
        byte byte2 = (byte) (data & 0x0FF);
        data = data >> 8;
        byte byte3 = (byte) (data & 0x0FF);
    	byte [] memory = m_osinfo.getSystemMemory(); 
        memory[((int)d_paddr)+0] = byte0;
        memory[((int)d_paddr)+1] = byte1;
        memory[((int)d_paddr)+2] = byte2;
        memory[((int)d_paddr)+3] = byte3;
    }

    /**
     * @param val
     * @return
     */
    static int page2vaddr(int page_num) {
        return (int) page_num << m_osinfo.getPageSizeBytesLog2();
    }

    private static int vaddr2page(long vaddr) {
        return (int) vaddr >> m_osinfo.getPageSizeBytesLog2();
    }

    /**
     * @param cpu
     * @param p
     * @param ip
     * @param reg
     * @param reg2
     */
    static void syscallAlloc(Instruction instr, ICPU cpu, IProcess p, long ip, int from_page, int until_page) throws InstructionException {
        from_page += vaddr2page(p.getStartingDataVaddr());
        until_page += vaddr2page(p.getStartingDataVaddr());
        boolean rc = m_os.syscallAlloc(p, from_page, until_page - from_page);
        if (!rc) {
            throw new InstructionException(instr, "Unable to allocate virtual for " +p + " from_page " + from_page + " until page " + until_page);
        }
        return;
    }

}
