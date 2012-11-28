package simulator;
/**
 * Title:        
 * Description:
 * Copyright:    Copyright (c) 2012
 * Company:      University of Texas at Austin
 * @author R.L. Rockhold, Ph.D.
 * @version 1.0
 */

import java.util.HashMap;
import java.util.Map;

public abstract class OSBase implements IOS {
    protected final SystemInfo m_sysinfo;

    public OSBase(SystemInfo osi) {
		m_sysinfo = osi;
	}
	Map<Long, SwappedFrameInfo> token2swapped_page = new HashMap<Long, SwappedFrameInfo>();
    long next_token = 0;
    class SwappedFrameInfo {
        private long m_paddr;
        private long m_page;
        private int m_pid;
        private long m_token;
        private long m_time;
        private byte[] m_data;
        SwappedFrameInfo(long token, long paddr, int page, int pid) {
            m_paddr = paddr;
            m_page = page;
            m_pid = pid;
            m_token = token;
            m_time = Simulator.getTime();
            int bytes = 1<<m_sysinfo.getPageSizeBytesLog2();
            m_data = new byte[bytes];
            for (int i=0; i<bytes; ++i, ++paddr) {
                m_data[i] = m_sysinfo.getSystemMemory()[(int)paddr];
            }
            Debug.info("Swapping out frame. " + toString());
        }
        void copyOut(long token, long paddr, int page, int pid) {
            if (m_page != page  ||  m_pid != pid) {
                Debug.log("ERROR - swapIn for page with missmatch pid or vaddr. Swapped info: " + toString() + 
                         "Target info: " +
                    " target paddr: " + Long.toString(paddr, 16) + 
                    " page " + Simulator.hex(page) + 
                    " pid " + pid);
            }
            Debug.info("Swapping in frame from info: " + toString() +
                    " target paddr: " + Long.toString(paddr, 16) + 
                    " page " + Simulator.hex(page) + 
                    " pid " + pid);
            
            int bytes = 1<<m_sysinfo.getPageSizeBytesLog2();
            for (int i=0; i<bytes; ++i, ++paddr) {
                m_sysinfo.getSystemMemory()[(int)paddr] = m_data[i];
}
        }
        public String toString() {
            return "Token: " +
            m_token + " paddr: " + Long.toString(m_paddr, 16) + 
            " page " + Simulator.hex(m_page) + 
            " pid " + m_pid + " time " + m_time;
        }
    }

    public void processStarted(IProcess p) {
        m_sysinfo.getScheduler().processStarted(p);
    }

    /**
     * 
     * @param token  Token returned by swapOutPage
     * @param frame_num  Number of the frame into which the page's 
     * 				 data (in simulated memory) will be copied to.
     * @param page   Info.  For validation.
     * @param pid	 For validation
     */
    public void swapInPage(long token, int frame_num, int page, int pid) {
    	long paddr = frame_num << m_sysinfo.getPageSizeBytesLog2();
        Long tok = new Long(token);
        SwappedFrameInfo fi = token2swapped_page.remove(tok);
        if (fi == null) {
            throw new IllegalStateException("swapInPage called for token that doesn't exist.  Token: " +
                    token + " target paddr: " + Long.toString(paddr, 16) + 
                    " vaddr " + Simulator.hex(page) + 
                    " pid " + pid);
	    } else {
	        /*
	        info("Swapping in page for Token: " +
	                token +  " target paddr: " + Long.toString(paddr, 16) + 
	                " vaddr " + Long.toString(vaddr, 16) + 
	                " pid " + pid);
	        */
	    }
        
        fi.copyOut(token, paddr, page, pid);
       
    }

    public void processExiting(ICPU cpu, IProcess p) {
    	m_sysinfo.getScheduler().processExited(p);
		p.completed(Simulator.getTime());
    }

    public void quantumExpired(IProcess p, ICPU cpu, int ticks_given) {
        m_sysinfo.getScheduler().quantumExpired(p, cpu, ticks_given);
    }


    /**
     * Swaps out a page from "simulated memory", returning
     *    a token (long) that can later be used to retrieve 
     *    the swapped out data (or delete the info).
     * @param frame_num The frame number where
     *               the data for the page will be copied out.
     * @param page   Info.  Saved for comparison on swapInPage().
     * @param pid    Info.  Saved for comparison on swapInPage().
     * @return  the "token" which should be stored in the page
     *          table entry for retrieval later.
     */
    public long swapOutPage(int frame_num, int page, int pid) {
    	long paddr = frame_num << m_sysinfo.getPageSizeBytesLog2();
        while (token2swapped_page.containsKey(new Long(next_token))) {
            ++next_token;
        }
        Long token = new Long(next_token);
        token2swapped_page.put(token, new SwappedFrameInfo(next_token, paddr, page, pid));
        /*
        info("Swapped out frame for paddr " + Long.toString(paddr, 16) + 
                " vaddr " + Long.toString(vaddr, 16) + 
                " for pid " + pid + " under token " + token);
        */
        return next_token;
    }
	/**
     * Delete a swapped page.  Called, for example, when a process
     * exits and it had pages that were swapped out.
     * @param token  The swapped page's token (from swapOutPage)
     * @param vaddr  Info.  The vaddr associated with the original page
     * @param pid   Info.  The pid of the process that had owned the data in the swapped page
     */
    public void deleteSwappedPage(long token, long vaddr, int pid) {
        Long tok = new Long(token);
        SwappedFrameInfo fi = token2swapped_page.remove(tok);
        if (fi == null) {
            throw new IllegalStateException("deleteSwappedPage called for token that doesn't exist.  Token: " +
                    token +  
                    " vaddr " + Long.toString(vaddr, 16) + 
                    " pid " + pid);
        } else {
            Debug.info("Deleting swapped page for Token: " +
                    token +  
                    " vaddr " + Long.toString(vaddr, 16) + 
                    " pid " + pid);
        }
    }
	void checkSwaps() {
	    if (token2swapped_page.size() > 0) {
	        Debug.log("Error.  Exiting with swapped pages!");
	        for (OSBase.SwappedFrameInfo sfi: token2swapped_page.values()) {
	        //for (Iterator it=token2swapped_page.values().iterator(); it.hasNext();) {
	        //    OSBase.SwappedFrameInfo sfi = (OSBase.SwappedFrameInfo) it.next();
	            Debug.log(sfi.toString());
	        }
	    }
	}

    protected void checkPageTable(IProcess p, IPageTableEntry[][] pt) {
    	for (int i=0; i<pt.length; ++i) {
    		IPageTableEntry[] l2 = pt[i];
    		if (l2 == null) {
    			Debug.log("OS Process " + p.getName() + ": No 2nd Level Page Table at index [" + i + "][]");
    			continue;
    		}
    		// l2 is our 2nd level page table.
    		// Report on which slots are null
    		for (int j=0; j<l2.length; ++j) {
    			IPageTableEntry pte = l2[j];
    			if (pte == null) {
    				Debug.log("OS Process " + p.getName() + ": No PTE at index [" + i + "][" + j + "]");
    			} 
    		}
    	}
    			
    }
    

	// Added here to allow OS to compile without it
    @Override
    public void setFrameReferencedTime(int frame_num, int time) {
		throw new IllegalStateException("Implement OS.setFrameReferencedTime()");
    }
    

}
