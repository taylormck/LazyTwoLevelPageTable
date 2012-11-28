package simulator;


/**
 * Title:        
 * Description:
 * Copyright:    Copyright (c) 2012
 * Company:      University of Texas at Austin
 * @author R.L. Rockhold, Ph.D.
 * @version 1.0
 */

public interface ICPU extends Runnable {

	// Student implemented:
	
	/**
	 * Use the TLB or the PageTable to translate a virtual address
	 * @param virtual_address The virtual address to translate.
	 * @return the frame number backing the page
	 *         -1 if none found
	 */
	public int xlate(long virtual_address);
	
	/**
	 * Use the TLB or the PageTable to translate a virtual address
	 * @param virtual_address The virtual address to translate.
	 * @return the frame number backing the page
	 *         -1 if none found
	 */
	public int xlateTLB(long virtual_address);
	
	/**
	 * Invalidate the TLB contents.
	 *
	 */
    public void invalidateTLB();

	/**
	 * Use the the PageTable to translate a virtual address
	 * @param virtual_address The virtual address to translate.
	 * @return the frame number backing the page
	 *         -1 if none found
	 */
    public int xlatePageTable(long vaddr);
    
    
    
    /**
     * Consructs a Physical Address for virtual address vaddr, 
     * given that it's backed by frame number fnum.
     * @param vaddr
     * @param fnum
     * @return
     */
    public long vaddr2paddr(long vaddr, int fnum);
    
    
    
    
	// These are implemented in OSBase
	int getID();
	void setIP(long ip);
	long getIP();
    void setPTBR(IPageTableEntry[][] ptbr);
    
    /* Must be used by xlate */
    IPageTableEntry[][] getPTBR();
    int getReg(int regnum);
    void setReg(int regnum, int val);
    /**
     * @return
     */
    int getXlateHits();

     IProcess getLastDispatchedProcess();

    // TLB Stuff (If we have a TLB)
    /**
     * @return
     */
    int getTLBXlateLoadSteals();
    
    int getTLBXlateLoadCleans();

    /**
     * @return
     */
    int getXlatefaults();

    /**
     * @return
     */
    int getTLBInvalidates();
    
    /**
     * 
     */
    void logTLBInvalidate();
    
    ITLBEntry[] getTLBEntries();
	
}
