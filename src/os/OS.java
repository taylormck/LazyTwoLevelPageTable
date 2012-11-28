package os;
/**
 * Title:        
 * Description:
 * Copyright:    Copyright (c) 2012
 * Company:      University of Texas at Austin
 * @author R.L. Rockhold, Ph.D.
 * @version 1.0
 */

import java.util.LinkedList;
import simulator.Debug;
import simulator.ICPU;
import simulator.IPageTableEntry;
import simulator.IProcess;
import simulator.IProcessState;
import simulator.ITLBEntry;
import simulator.Simulator;
import simulator.SystemInfo;
import simulator.CPUBase;

public class OS extends simulator.OSBase {
    final SystemInfo m_sysinfo;    
    private final FrameInfo m_frame_info;
    private final int m_l1entry_cnt;
    private final int m_l2entry_cnt;
    private final LinkedList<FrameInfo.Frame> m_free_frames = new LinkedList<FrameInfo.Frame>();
    private final LinkedList<FrameInfo.Frame> m_referenced_frames = new LinkedList<FrameInfo.Frame>();
    private final byte[] m_memory;
    private final int m_bytes_per_page;
    private final int m_page_count;
    
    private int page_number = 0;

    public OS(SystemInfo si) {
    	super(si);
        m_sysinfo = si;
        m_frame_info = new FrameInfo(si);
        m_l1entry_cnt = 1 << m_sysinfo.getPageTableLevel1EntryCountLog2();
        m_l2entry_cnt = 1 << (m_sysinfo.getPageCountLog2() - m_sysinfo.getPageTableLevel1EntryCountLog2());
        m_bytes_per_page = 1 << si.getPageSizeBytesLog2();
        m_memory = si.getSystemMemory();
        m_page_count = m_l1entry_cnt * m_l2entry_cnt;
    }

    private static class PageTableEntry implements IPageTableEntry {
        private final int m_page_number;
        private boolean m_valid = false;
        boolean m_swapped_out = false;
        long m_swapped_out_token = -1L;	// if m_swapped_out
        boolean m_in_memory = false;
        int m_frame_number = -1;	// If m_in_memory

        @Override
        public boolean isResident() {
            return m_in_memory;
        }

        @Override
        public int getFrameNumber() {
            return m_frame_number;
           
        }
        
        PageTableEntry(int i) {
            m_page_number = i;
        }
        
        boolean isValid() {
            return m_valid;
        }
        
        void setValid() {
            m_valid = true;
        }
        
        long getToken() {
            return m_swapped_out_token;
        }
        
        void setResident(int frame_number) {
            m_swapped_out_token = -1;
            m_in_memory = true;
            m_swapped_out = false;
            m_frame_number = frame_number;
        }
        
        boolean isSwapped() {
            return m_swapped_out;
        }
        
        void setSwappedOut(long token) {
            m_swapped_out_token = token;
            m_swapped_out = true;
            m_in_memory = false;
        }
        
        int getPageNumber() {
        	return m_page_number;
        }
    }

    
    private class AddressSpace {
        PageTableEntry[][] m_page_table;

        private IProcess m_p;

        AddressSpace(IProcess p) {
            m_p = p;
            // initialize the page tables
            // This will need to be changed to do lazy allocation
    		// TODO
            m_page_table = new PageTableEntry[m_l1entry_cnt][];
            for (int i=0; i<m_l1entry_cnt; ++i) {
            	m_page_table[i] = null;
            }
        }
 
        private IPageTableEntry[][] getPTBR() {
            return m_page_table;
        }
        
        boolean createMapping(int page_count, int start_page, boolean codeSpace) throws IllegalArgumentException {
            if ( (start_page + page_count) >= m_page_count) {
                Debug.log("Memory request exceeds allocation for process " +
                        m_p.getID() + " start page " + start_page + " count " + page_count);
                return false;
            }
    		// TODO
            // Unlike project3, you'll need to deal with the fact the
            // not all your 2nd level tables and the PTE references in them 
            // have been set up.
            for (int p=0; p<page_count; ++p, ++start_page) {
                
                if (m_page_table[page_to_l1index(start_page)] == null) {
                	m_page_table[page_to_l1index(start_page)] = new PageTableEntry[m_l2entry_cnt];
                }
                
                PageTableEntry pte = m_page_table[page_to_l1index(start_page)][page_to_l2index(start_page)];
                
                if (pte == null) {
                	pte = new PageTableEntry(page_number++);
                } else if (pte.isValid()) {
                    throw new IllegalArgumentException("Page " + pte.getPageNumber() + " is already allocated.");
                }
                pte.setValid();
            }
            return true;
        }
        PageTableEntry getPTEforPage(int page_num) {
        	if (m_page_table[page_to_l1index(page_num)] == null) return null;
            PageTableEntry pte = m_page_table[page_to_l1index(page_num)][page_to_l2index(page_num)];
            return pte;
        }
    }
 
    
    private class FrameInfo {
        class Frame {
            IProcess m_owning_process = null;
            int   m_page_num = -11;
            final int   m_frame_number;
            int m_reference_time = -1;

            Frame(SystemInfo osi, int frame_number) {
                m_frame_number = frame_number;
                m_reference_time = Simulator.getTime();
            }
            /**
             * @return
             */
            int getFrameNumber() {
                return m_frame_number;
            }
            /**
             * @param p
             * @param page_num
             */
            void setMapping(IProcess p, int page_num) {
                m_owning_process = p;
                m_page_num = page_num;
            }
            /**
             * @return
             */
            public IProcess getProcess() {
                return m_owning_process;
            }
            
            public int getPage() {
                return m_page_num;
            }
        }
        Frame[] m_frames;
        int m_free_frame_count;
        FrameInfo(SystemInfo osi) {
            m_free_frame_count = 1<<osi.getFrameCountLog2();
            m_frames = new Frame[m_free_frame_count];
            for (int f=0; f<m_free_frame_count; ++f) {
                m_frames[f] = new Frame(osi, f);
                m_free_frames.addLast(m_frames[f]);
            }
        }
        /**
         * @param frame
         */
        public void setReferenced(int frame, int ref_time) {
            Frame f = getFrame(frame);
            f.m_reference_time=ref_time;
            if (!m_referenced_frames.contains(f)) {
            	m_referenced_frames.addFirst(f);
            }
        }

        Frame getFrame(int ndx) {
            return m_frames[ndx];
        }

		Frame removeLRU() {
			int min=Integer.MAX_VALUE;
			Frame f, lru_frame=null;
			for(int i=0; i<m_referenced_frames.size(); i++) {
				f=(Frame)(m_referenced_frames.get(i));
				if(f.m_reference_time<=min) {
					lru_frame=f;
					min=f.m_reference_time;
				}
			}
			m_referenced_frames.remove(lru_frame);
			return lru_frame;
		}
    }


    @Override
    public boolean syscallAlloc(IProcess p, int start_page_number, int number_of_pages) {
        AddressSpace as = getAddressSpace(p);
        boolean rc = as.createMapping(number_of_pages, start_page_number, false);
        return rc;
    }
    
    @Override
    public void initProcess(IProcess p) {
        // Create an address space for p
        AddressSpace as = new AddressSpace(p);
        setAddressSpace(p, as);
    }

    @Override
    public boolean pageFaultInstruction(ICPU cpu, IProcess p, long ip_virtual) {
        // Verify the address is a valid instruction address
		int last_instruction_page_number =  bytes2pages(p.getTextByteCount()) - 1;
        int page_num = vaddr2page(ip_virtual); 
        if (page_num > last_instruction_page_number) {
            // can't execute data
            return false;
        }
        boolean ok = pageFaultGeneral(cpu, p, page_num, ip_virtual);
        return ok;
    }

    @Override
    public boolean pageFaultData(ICPU cpu, IProcess p, long d_vaddr) {
        boolean ok = pageFaultGeneral(cpu, p, vaddr2page(d_vaddr), d_vaddr);
        return ok;
    }
    
    @Override
    public IProcess schedule(ICPU cpu) {
        IProcess p = m_sysinfo.getScheduler().schedule(cpu);
        if (p != null) {
            dispatch(p, cpu);
        }
        return p;
    }

    @Override
    public void processExiting(ICPU cpu, IProcess p) {
    	checkPageTable(p, cpu.getPTBR());  /// <<< Must remain here.
        undispatch(p, cpu);
		killProcess(p);
		
        super.processExiting(cpu, p);  // Leave this at the end of the method
    }

    @Override
    public void quantumExpired(IProcess p, ICPU cpu, int ticks_given) {
        undispatch(p, cpu);
        super.quantumExpired(p, cpu, ticks_given);  // Leave this at the end of the method
    }

    /**
     * Called by CPU on a successful xlate() (iff not using TLBs).
     * Must be implemented to correctly manage LRU
     */
    @Override
    public void setFrameReferencedTime(int frame_num, int time) {
		m_frame_info.setReferenced(frame_num, time);
    }
    
    int bytes2pages(int bytes) {
        return (bytes + (1<<m_sysinfo.getPageSizeBytesLog2() -1)) >> m_sysinfo.getPageSizeBytesLog2();
    }
    
    void dispatch(IProcess p, ICPU cpu) {
        // Set IP
        IProcessState ps = p.getState();
        cpu.setIP(ps.getIP());
        cpu.setReg(0, ps.getReg(0));
        cpu.setReg(1, ps.getReg(1));
        // PTBR
        cpu.setPTBR(getAddressSpace(p).getPTBR());
		// TODO
        /*
         * If we're using TLBs, and we're switching processes, 
         * we need to invalidate the TLBs 
         */
    }
    void undispatch(IProcess p, ICPU cpu) {
        IProcessState ps = p.getState();
        ps.setIP(cpu.getIP());
        ps.setReg(0, cpu.getReg(0));
        ps.setReg(1, cpu.getReg(1));
        if (CPUBase.TLB_SUPPORTED) {
        	updateReferences(cpu, p);
        }
    }
    
    void updateReferences(ICPU icpu, IProcess p) {
    	if (CPUBase.TLB_SUPPORTED) {
    		// TODO
    		/**
    		 * When running with a TLB we need to iterate over all
    		 * the ITLBEntry's.
    		 * For those that are valid, we need to capture the
    		 * referenced time recorded in the ITLBEntry, and use
    		 * it to update our FrameInfo's reference time.
    		 */
	        // Iterate through the TLBEntries
    		 throw new IllegalStateException("Implement updateReferences"); 
    	} 
    }

    
    int page_to_l1index(int page_num) {
        return page_num >> (m_sysinfo.getPageCountLog2() - m_sysinfo.getPageTableLevel1EntryCountLog2());
    }
    
    int page_to_l2index(int page_num) {
        return page_num % m_l2entry_cnt;
    }
    
    long page_to_vaddr(int page_num) {
        return page_num << m_sysinfo.getPageSizeBytesLog2();
    }
    
    int vaddr2page(long vaddr) {
        return (int) (vaddr >> m_sysinfo.getPageSizeBytesLog2());
    }
    
    
    boolean pageFaultGeneral(ICPU cpu, IProcess p, int page_num, long vAddr) {
    	if (page_num >= m_page_count) {
            Debug.user("Segmentation fault for  " + p +
            		" on virtual address " + Simulator.hex(vAddr) +
            		". Page " + page_num + " exceeds address space size " + m_page_count);
            return false;
    	}
        AddressSpace as = (AddressSpace) p.getAddressSpace();
        PageTableEntry pte = as.getPTEforPage(page_num);
        if (! pte.isValid()) {
            // segmentation fault!
            Debug.user("Segmentation fault for  " + p + 
            		" on virtual address " + Simulator.hex(vAddr) +
            		". Page " + page_num);
            return false;
        }
        updateReferences(cpu, p);
        FrameInfo.Frame frame = allocFrame(cpu, p);	// Get a frame
        if (pte.isSwapped()) {
            Debug.user("Frame before swap in:");
            dumpFrame(frame.getFrameNumber(), m_memory, m_bytes_per_page);
            swapInPage(pte.getToken(), frame.getFrameNumber(), vaddr2page(vAddr), p.getID());
            Debug.user("Frame after swap in:");
            dumpFrame(frame.getFrameNumber(), m_memory, m_bytes_per_page);
        } else {
            // Zero the frame
            zeroFrame(frame.getFrameNumber());
        }
        pte.setResident(frame.getFrameNumber());
        frame.setMapping(p, page_num);
        return true;
    }
    
    void dumpFrame(int frameNumber, byte[] memory, int bytes_per_page)
    {
        int byte_ndx = frameNumber * bytes_per_page;
        Debug.user(String.format("Dumping frame number %d at time %d:", frameNumber, Simulator.getTime()));
        int word = 0;
        int word_count = bytes_per_page / 4;
        for (word =0; word < word_count; ++ word) {
        //for (int b=0; b< m_bytes_per_page; ++b, ++byte_ndx) {
            int byte0 = (((int)memory[byte_ndx]) & 0x0FF); 
            int byte1 = (((int)memory[byte_ndx+1]) & 0x0FF) << 8; 
            int byte2 = (((int)memory[byte_ndx+2]) & 0x0FF) << 16; 
            int byte3 = (((int)memory[byte_ndx+3]) & 0x0FF) << 24;
            int word_val = byte0 + byte1 + byte2 + byte3;
            
            Debug.user(String.format("\t%3d: %08X", word, word_val));

        	byte_ndx += 4;
        }
    }

    void zeroFrame(int frameNumber) {
        int byte_ndx = frameNumber2byteNdx(frameNumber);
        Debug.user("Zero frame before:");
        dumpFrame(frameNumber, m_memory, m_bytes_per_page);
        for (int b=0; b< m_bytes_per_page; ++b, ++byte_ndx) {
            m_memory[byte_ndx] = 0;
        }
        Debug.user("Zero frame after:");
        dumpFrame(frameNumber, m_memory, m_bytes_per_page);
    }

    /**
     * @param frameNumber
     * @return
     */
    int frameNumber2byteNdx(int frameNumber) {
        return frameNumber * m_bytes_per_page;
    }

    FrameInfo.Frame allocFrame(ICPU cpu, IProcess p) {
        FrameInfo.Frame f;
        if (m_free_frames.size() > 0) {
            f = m_free_frames.removeFirst();
            Debug.user(cpu + " Using free frame " + f.getFrameNumber() + " for " + p);
        } else {
            f = m_frame_info.removeLRU();
            Debug.user(cpu + " Stealing frame " + f.getFrameNumber() + 
                    " from " + f.getProcess() + ", page  " + f.getPage() + " for " + p);
            stealFrame(f, cpu, p);
        }
        m_frame_info.setReferenced(f.getFrameNumber(), Simulator.getMonotonicInt());
        return f;
    }

    void stealFrame(FrameInfo.Frame f, ICPU cpu, IProcess p) {
        // Update the AddressSpace's TLBEntry
        IProcess old_p = f.getProcess();
        int old_page = f.getPage();
        AddressSpace as = getAddressSpace(old_p);
        PageTableEntry pte = as.getPTEforPage(old_page);
        // Swap it out
        
        Debug.user("Frame before swap out:");
        dumpFrame(f.m_frame_number, m_memory, m_bytes_per_page);
        long token = swapOutPage(f.m_frame_number, vaddr2page(page_to_vaddr(pte.getPageNumber())), old_p.getID());
        pte.setSwappedOut(token);
		// TODO
        /**  
         * If (and only if) we're stealing a frame from ourselves,
         * we need to make sure any TLB mappings for the
         * to-be-swapped-out Page are wiped out of the TLB. 
         */
    }
    
    void setAddressSpace(IProcess p, AddressSpace as) {
        p.setAddressSpace(as);
    }
    AddressSpace getAddressSpace(IProcess p) {
        return (AddressSpace) p.getAddressSpace();
    }
    
    void killProcess(IProcess p) {
        AddressSpace as = getAddressSpace(p);
        // Release any frames, working from low to high VAs
        PageTableEntry[][] ptes = (PageTableEntry [][]) as.getPTBR();
        for (int i=0; i<ptes.length; ++i) {
            for (int j=0; j<ptes[i].length; ++j) {
            	PageTableEntry pte = null;
                pte = ptes[i][j];
                if (pte.isSwapped()) {
                    deleteSwappedPage(pte.getToken(), page_to_vaddr(pte.getPageNumber()), p.getID());
                } else if (pte.isResident()) {
                    int frame_num = pte.getFrameNumber();
                    FrameInfo.Frame f = m_frame_info.getFrame(frame_num);
                    Debug.user("Reclaiming free frame " + frame_num + " from process " + p.getID() +
                            " at vaddr " + page_to_vaddr(pte.getPageNumber()));
                    m_free_frames.addLast(f);
                    m_referenced_frames.remove(f);
                }
            }
        }
    }
}
