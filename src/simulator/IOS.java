/**
 * Title:        
 * Description:
 * Copyright:    Copyright (c) 2012
 * Company:      University of Texas at Austin
 * @author R.L. Rockhold, Ph.D.
 * @version 1.0
 */

package simulator;

public interface IOS {

	/**
     * Called by Simulator when a process begins.
     * Perform any setup required to allow the process to
     * run.
     * This should include creating an "Address Space" for
     * the process, which can be saved in the process object
     * using IProcess.setAddressSpace(Object), and retrieved
     * using getAddressSpace().
     * @param p
     */
	void initProcess(IProcess p);
	
    /**
     * Should result in the process being put on the ready-queue.
     * @param p
     */
    void processStarted(IProcess p);

    /**
     * Called when an insruction fetch (at ip_vaddr)
     * results in a page fault (not in tlb or page-table).
     *
     * If the vaddr is not valid (hasn't been allocated or
     *   beyond the processes address space size), return false.
     * If the vaddr is not an instruction page, return false.
     * (see IProcess.getTextByteCount())
     * 
     * Otherwise, allocate a frame of memory to back the page,
     * update p's page table to reflect the new mapping, 
     * and return true.
     * @param cpu
     * @param p
     * @param ip_virtual
     */
    boolean pageFaultInstruction(ICPU cpu, IProcess p, long ip_vaddr);

    /**
     * Called when an insruction's attempt to load/store
     * to memory at virutal address d_vaddr
     * results in a page fault (not in tlb or page-table).
     *
     * If the vaddr is not valid (hasn't been allocated or
     *   beyond the processes address space size), return false.
     * Otherwise, allocate a frame of memory to back the page,
     * update p's page table to reflect the new mapping, 
     * and return true.
     * @param cpu
     * @param p
     * @param ip_virtual
     */
    boolean pageFaultData(ICPU cpu, IProcess p, long d_vaddr);
    
    /**
     * Call sequence:  CPU -> Simulator.schedule() -> OS.schedule()
     * Don't forget to call sched.schedule()
     * If a process is found, then restore its state before returning.
     * @param cpu
     * @return
     */
    IProcess schedule(ICPU cpu);

    /**
     * Called by Simulator whenever the currently running proccess
     * is exiting.
     * This can be the result of a "Syscall_Exit" instruction, or
     *   because of a non-recoverable fault (e.g. invalid instruction,
     *   invalid virtual address, etc.)
     *  
     * @param cpu
     * @param p
     */
    void processExiting(ICPU cpu, IProcess p);

    /**
     * Call Sequence:  CPU->Simulator.quantumExpired()->OS.quantumExpired()
     * @param p
     * @param cpu
     * @param ticks_given
     */
    void quantumExpired(IProcess p, ICPU cpu, int ticks_given);

    /**
     * Indicates that the specified range of (virtual memory) pages should now be
     * considered as "allocated".
     * (Only allocated pages can be referenced - instruction fetching
     *  and data loads/stores).
     * Call Sequence:
     * 	CPU->instr.execute() on a Syscall_Alloc instruction.
     *    instr.execute() -> Simulator.syscallAlloc() -> OS.syscallAlloc()
     * @param p
     * @param start_page_number
     * @param number_of_pages
     * @return indicator of success (true), failure (false)
     */
    boolean syscallAlloc(IProcess p, int start_page_number, int number_of_pages);
    
    void setFrameReferencedTime(int frame_ndx, int time);
}
