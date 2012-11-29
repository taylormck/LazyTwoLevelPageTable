package cpu;
/**
 * Title:        
 * Description:
 * Copyright:    Copyright (c) 2012
 * Company:      University of Texas at Austin
 * @author R.L. Rockhold, Ph.D.
 * @version 1.0
 */

import simulator.CPUBase;
import simulator.Debug;
import simulator.ICPU;
import simulator.IOS;
import simulator.IPageTableEntry;
import simulator.ITLBEntry;
import simulator.Simulator;
import simulator.SystemInfo;

public class CPU extends CPUBase { 

	private ICPU m_cpu;

	public class TLB  {
		// This is the class that constructs/contains all
		// of the TLBEntry objects
		// Here's the constructor I would use. But feel free to change it.
		TLBEntry[] TLBEntryArray;
		TLB(final int tlb_entry_count) {
			TLBEntryArray = new TLBEntry[tlb_entry_count];
			for (int i = 0; i < tlb_entry_count; ++i)
				TLBEntryArray[i] = new TLBEntry();
		}

		/**
		 * @return a the array of TLBEntries
		 */
		TLBEntry[] getTLBEntries() {
			return TLBEntryArray;
		}
		
		/**
		 * Searches through the TLB to see if our desired page is stored there.
		 * Returns the corrosponding frame number if it is, and -1 if not.
		 * @param search_page
		 * @return
		 */
		TLBEntry searchTLB(int search_page) {
			for(TLBEntry e : TLBEntryArray) {
				if (e.valid && e.page_number == search_page)
					return e;
			}
			return null;
		}
		
		/**
		 * Invalidates all the entries in our array
		 */
		void invalidate() {
			for(TLBEntry e : TLBEntryArray) {
				e.valid = false;
			}
		}
		
		void updateTLBEntryReferencedTime(int search_page, int time) {
			for (TLBEntry e : TLBEntryArray) {
				if (e.page_number == search_page) {
					e.referenced_time = time;
					break;
				}
			}
		}

		public void updateTLB(int page_num, int frame_num) {
			boolean found_one = false;
			for (TLBEntry e : TLBEntryArray) {
				if (!e.valid) {
					e.frame_number = frame_num;
					e.page_number = page_num;
					e.valid = true;
					e.referenced_time = Simulator.getMonotonicInt();
					found_one = true;
					logTLBLoadClean(page_num, frame_num);
					return;
				}
			}
			// All were valid, so we grab the least recently used instead
			if (!found_one) {
				TLBEntry e = TLBEntryArray[0];
				int current_time = e.getReferencedTime();
				for (int i = 1; i < TLBEntryArray.length; ++i) {
					if (TLBEntryArray[i].referenced_time < current_time) {
						e = TLBEntryArray[i];
						current_time = e.referenced_time;
					}
				}
				logTLBLoadSteal(page_num, frame_num, e.page_number, e.frame_number);
				e.frame_number = frame_num;
				e.page_number = page_num;
				e.valid = true;
				e.referenced_time = Simulator.getMonotonicInt();
			}
		}
	}

	/*
	 * This your TLBEntry class that you need to implement
	 */
	static class TLBEntry implements ITLBEntry {
		int page_number;
		int frame_number;
		int referenced_time;
		boolean valid = false;

		@Override
		public int getFrame() {
			return frame_number;
		}

		@Override
		public int getReferencedTime() {
			return referenced_time;
		}

		@Override
		public boolean isValid() {
			return valid;
		}

		public void setReferencedTime(int time) {
			referenced_time = time;
		}
	}

	private final IOS my_os;
	private final long m_pageoffset_mask;
	private final SystemInfo m_sysinfo;
	private final TLB tlb;

	/**
	 * Constructor used by Simulator for each CPU
	 * @param id  This CPU's ID
	 * @param os  The IOS reference
	 * @param info The SystemInfo
	 */
	public CPU(final int id, final IOS os, SystemInfo info) {
		super(id);	// Leave this as the first statement 
		m_sysinfo = info;
		my_os = os;
		m_pageoffset_mask = -1 + (1 << m_sysinfo.getPageSizeBytesLog2());
		tlb = new TLB(m_sysinfo.getTLBEntryCount());
	}

	/**
	 * Called by CPUBase to convert a (vaddr, frame_number)
	 * to a physical address.
	 * Does not require lookup, just shifting/masking based
	 * on the SystemInfo.
	 */
	@Override
	public long vaddr2paddr(final long vaddr, final int fnum) {
		long paddr;
		paddr = fnum << m_sysinfo.getPageSizeBytesLog2();
		long paddr_o = vaddr2pageoffset(vaddr);
		paddr = paddr | paddr_o;
		return paddr;
	}

	@Override
	public void invalidateTLB() {
		logTLBInvalidate();
		tlb.invalidate();
	}

	@Override
	public ITLBEntry[] getTLBEntries() {
		return tlb.getTLBEntries();
	}

	@Override
	public int xlatePageTable(long vaddr) {
		return xlatePageTable(vaddr, true);
	}
	
	public int xlatePageTable(long vaddr, boolean log) {
		IPageTableEntry pte = null;
		int page_num = vaddr2page(vaddr);
		int l1_ndx =   page_to_l1index(page_num);
		int l2_ndx =   page_to_l2index(page_num);
		try {
			pte = getPTBR()[l1_ndx][l2_ndx];
		} catch (ArrayIndexOutOfBoundsException e) {
			logXlateFault(page_num);
			return -1;
		} catch (NullPointerException e) {
			if (getPTBR()[l1_ndx] == null)
				logPTEL1IndexNull(l1_ndx);
			logXlateFault(page_num);
			return -1;
		}

		if (pte == null) {
			logPTEIsNull(l1_ndx, l2_ndx);
			logXlateFault(page_num);
			return -1;
		}

		if (pte.isResident()) {
			int fnum = pte.getFrameNumber();
			if (log)
				logXlateHit(page_num, fnum);
			my_os.setFrameReferencedTime(fnum, Simulator.getMonotonicInt());
			return fnum;
		}
		logXlateFault(page_num);
		return -1;
	}

	int page_to_l1index(int page_num) {
		return page_num >> (m_sysinfo.getPageCountLog2() - m_sysinfo.getPageTableLevel1EntryCountLog2());
	}

	int page_to_l2index(int page_num) {
		return page_num % (1 << (m_sysinfo.getPageCountLog2() - m_sysinfo.getPageTableLevel1EntryCountLog2()));
	}

	int vaddr2page(long vaddr) {
		return (int) (vaddr >> m_sysinfo.getPageSizeBytesLog2());
	}

	/**
	 * Called by CPUBase.
	 * If the virtual address is mapped, returns the frame number
	 * of the frame that "holds" the page.
	 * Otherwise, returns -1.
	 * NOTE!!!!!!!!!!!!!!!!!!!!!!! TO PRODUCE THE CORRECT OUTPUT!!!
	 * When xlate() fails to find a valid mapping with a resident page,
	 * xlate() must call:  logXlateFault(page_number).
	 * Otherwise, xlate() must call: logXlateHit(page_number, frame_number). 
	 * Note that on a "hit", xlate() must call IOS.setFrameReferencedTime()
	 * to ensure it knows how to track LRU.
	 */
	public int xlate(final long virtual_address) {
		// TLB_SUPPORTED is set to true iff the simulator
		// is run with the -t flag (makefile targets config*_tlb)
		// in which case the xlatePageTable() method is never called.
		// Likewise, if -t is not specified (makefile targets config*_pt)
		// only xlatePageTable() is called (and never xlateTLB())
		if (TLB_SUPPORTED) 
			return xlateTLB(virtual_address);
		else 
			return xlatePageTable(virtual_address);
	}

	/**
	 * Called when running the simulator with -t (for TLB), which
	 * is done with all the config*_tlb tests in the Makefile.
	 * Your solution needs to first look for a mapping in your TLB
	 * If found, call logXlateHit() and update the referenced time
	 * in the TLBEntry.
	 * If NOT found, search the PageTable
	 * (almost exactly like the code in your xlatePageTable())
	 * If NOT found in the PageTable, call LogPTEL1IndexNull or LogPTEIsNull()
	 * (if appropriate), and logXlateFault().
	 * If FOUND, select a TLBEnry to use for the mapping, and call 
	 * tlbe.setReferenceTime(). << Unlike without TLB where we'd call IOS.setFrameReferencedTime
	 */
	public int xlateTLB(final long virtual_address) {
		int page_num = vaddr2page(virtual_address);
		TLBEntry result = tlb.searchTLB(page_num);
		if (result != null) {
			result.setReferencedTime(Simulator.getMonotonicInt());
			logXlateHit(page_num, result.frame_number);
			return result.frame_number;
		}
		else {
			int frame_num = xlatePageTable(virtual_address, false);
			if (frame_num >= 0 ) {
				tlb.updateTLB(page_num, frame_num);
			}
			return frame_num;
		}
	}

	long vaddr2pageoffset(final long vaddr) {
		return vaddr & m_pageoffset_mask;
	}

}
