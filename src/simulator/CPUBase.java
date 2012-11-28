package simulator;



/**
 * Title:        
 * Description:
 * Copyright:    Copyright (c) 2012
 * Company:      University of Texas at Austin
 * @author R.L. Rockhold, Ph.D.
 * @version 1.0
 */


public abstract class CPUBase implements ICPU {

    protected int m_id;
    // private boolean m_executing_instructions = false;
    protected long m_ip = -1L;
    protected  IPageTableEntry[][] m_PTBR = null;
    private int[] m_regs = new int[2];

    private static int m_xlate_hits;
    private static int m_xlate_faults;
    private static int m_xlate_load_steals;
    private static int m_xlate_load_cleans;
    private static int m_tlb_invalidates;
    protected IProcess m_last_proc;
    protected IProcess m_curr_proc = null;
    
   
    
	/* (non-Javadoc)
	 * @see ICPU#getID()
	 */
	public int getID() {
		return m_id; 
	}
	
	public CPUBase(int id) {
	    m_id = id;
	}
	
    public void setIP(long ip) {
        m_ip = ip;
    }
    
    public long getIP() {
        return m_ip;
    }

	public void run() {
	    while (Simulator.isRunning()) {
        	// We block here if there's nothing to schedule
	        m_last_proc = m_curr_proc;
		    IProcess p = Simulator.schedule(this);
		    m_curr_proc = p;
		    p.incrDispatches();
			
			int quantum = p.getQuantum();
			int burst_given = 0;
			long i_paddr;
			Debug.info("Dispatching " + p + " at time " + Simulator.getTime() + 
			        " with quantum " + quantum + ".");
			
			while (true) {
			    boolean ok = true;
				if (quantum <= burst_given) {
				    Simulator.quantumExpired(this, p, burst_given);	// XXX  OS or Simulator?
				    break;
				}
				Debug.info("Translating (instruction) virtual address " + Simulator.hex(m_ip));
				
				
				int frame = xlate(m_ip);
				if (frame==-1) {
				    ok = Simulator.pageFaultInstruction(p, m_ip, this);
				    if (!ok) {
				        Simulator.processExited(this, p);
				        break;	// Try another process
				    }
				    frame = xlate(m_ip);
				    if(frame == -1)
	 					throw new AssertionError("Recursive IP xlateTLB failure for " + p + " at " + m_ip);				    
				    i_paddr = vaddr2paddr(m_ip, frame);	
				    
				    Debug.log("Instruction fault for " + p + " at ip " + Simulator.hex(m_ip) + 
					" resolved to physical address " + Simulator.hex(i_paddr));

				    continue;
				}
				i_paddr = vaddr2paddr(m_ip, frame);
				
				Debug.info("Fetching instruction from physical address " + Simulator.hex(i_paddr));
				IInstruction instr;
			    instr = Simulator.decode(p, m_ip, i_paddr);	// get the Instruction
				long d_paddr = -1;
				if (instr.hasDataAddr()) {	// have memory address?
				    long d_vaddr = instr.getDataVaddr(this, p);
					Debug.info("Translating (data) virtual address " + Simulator.hex(d_vaddr));
					
					frame = xlate(d_vaddr);

				    if (frame == -1) {
				        ok = Simulator.pageFaultData(p, m_ip, i_paddr, d_vaddr, this);
					    if (!ok) {
					        Simulator.processExited(this, p);
					        break;	// Try another process
					    }

					    frame = xlate(d_vaddr);
					    if(frame == -1) 
							throw new AssertionError("Recursive DP xlateTLB failure for " + p + " at " + m_ip);
					    d_paddr = vaddr2paddr(d_vaddr, frame);
						    
					    Debug.log("Data fault for " + p + " at ip " + Simulator.hex(m_ip) + " at address " + 
					    			Simulator.hex(d_vaddr) + " resolved to physical address " + Simulator.hex(d_paddr) +
					    			".  Refecthing instruction.");
				        continue;	/* Start the instruction over */
				    } 
				    d_paddr = vaddr2paddr(d_vaddr, frame);
					Debug.info("Data physical address " + Simulator.hex(d_paddr));
				}
			    Simulator.executeProcess(this, p, 1);	// XXX Use ticks from instruction?
			    ++ burst_given;	// XXX Use data from instruction?
			    Debug.info("CPU " + m_id + " executing instruction '" +
			            instr.getText() + "' for process " + p.getID() + " at ip va/pa " + Simulator.hex(m_ip) + "/" + Simulator.hex(i_paddr));
				try {
				    m_ip = instr.execute(this, p, m_ip, d_paddr);	// Won't use paddr if not needed
				} catch (InstructionException ie) {
				    // Same as Simulator.processExited, but with more info.
				    Simulator.instructionException(this, p, m_ip, ie);
				    break;	// Switch processes
				}
				
			}	// end while(true)
		}	// end while Simulator.isRunning
    }  // end run


    public void setPTBR(IPageTableEntry[][] ptbr) {
        m_PTBR = ptbr;
    }
    
    public IPageTableEntry[][] getPTBR() {
        return m_PTBR;
    }

    public String toString() {
	    return "CPU " + m_id;
	}
	
	public int getReg(int regnum) {
        return m_regs[regnum];
    }
    
	public void setReg(int regnum, int val) {
	    m_regs[regnum] = val;
	}
	
    /**
     * @param cpu
     * @param page_num
     * @param fnum
     */
    public void logXlateHit(int page_num, int fnum) {
        ++m_xlate_hits;
        Debug.log(this + " Xlation Hit: page/frame " + page_num + "->" + fnum);
    }

     /**
     * @param cpu
     * @param page_num
     */
    public void logXlateFault(int page_num) {
        ++ m_xlate_faults;
        Debug.log(this + " Xlation Fault: page " + page_num);
    }

    public void logPTBRNull() {
    	Debug.log(this + " PTBR is Null");
    }
    /**
     * @param cpu
     * @param page_num
     */
    public void logPTEL1IndexNull(int ndx1) {
        Debug.log(this + " PTE L1 Index Null at index [" + ndx1 + "]");
    }

    public void logPTEIsNull(int ndx1, int ndx2) {
        Debug.log(this + " PTE is Null at index [" + ndx1 + "][" + ndx2 + "]");    	
    }
    
    /**
     * @param tlb
     * @param page
     * @param frame
     * @param page2
     * @param frame2
     */
    public void logTLBLoadSteal(int page, int frame, int page2, int frame2) {
        ++ m_xlate_load_steals;
        Debug.log(this + " TLB Load Steal: page/frame " + page + "->" + frame + " stolen from " + page2 + "->" + frame2);
    }

    /**
     * @param cpu
     * @param page
     * @param frame
     */
    public void logTLBLoadClean(int page, int frame) {
        ++ m_xlate_load_cleans;
        Debug.log(this + " TLB Load Clean: page/frame " + page + "->" + frame);
    }

    /**
     * @param m_cpu
     */
    public void logTLBInvalidate() {
        ++ m_tlb_invalidates;
        Debug.log(this + " TLB Invalidate.");
    }
	
    /* (non-Javadoc)
     * @see simulator.ICPU#getTLBXlateHits()
     */
    public int getXlateHits() {
        return m_xlate_hits;
    }

    /* (non-Javadoc)
     * @see simulator.ICPU#getTLBXlateLoadSteals()
     */
    public int getTLBXlateLoadSteals() {
        // TODO Auto-generated method stub
        return m_xlate_load_steals;
    }

    /* (non-Javadoc)
     * @see simulator.ICPU#getTLBXlateLoadCleans()
     */
    public int getTLBXlateLoadCleans() {
        return m_xlate_load_cleans;
    }

    /* (non-Javadoc)
     * @see simulator.ICPU#getTLBXlatefaults()
     */
    public int getXlatefaults() {
        return m_xlate_faults;
    }

    /* (non-Javadoc)
     * @see simulator.ICPU#getTLBInvalidates()
     */
    public int getTLBInvalidates() {
        return m_tlb_invalidates;
    }

    public IProcess getLastDispatchedProcess() {
        return m_last_proc;
    }
    
	public static boolean TLB_SUPPORTED = false;	// Do Not Change.  Needed to compile.



}
