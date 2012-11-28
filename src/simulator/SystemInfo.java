/**
 * Title:        
 * Description:
 * Copyright:    Copyright (c) 2012
 * Company:      University of Texas at Austin
 * @author R.L. Rockhold, Ph.D.
 * @version 1.0
 */

package simulator;

public class SystemInfo {
    private final int pagesize_bytes_log2;   // bytes in a page/frame = 2**pagesize_bytes_log2
    private final int frame_count_log2;		// number of frames = 2**frame_count_log2
    private final int page_count_log2;		// number of pages in a virtual address space = 2**page_count_log2
    private final int level_1_pt_entries_log2;	// # Level1 Entries = 2 ** level_1_pt_entries_log2
    private final byte [] system_memory;
    private final IScheduler scheduler;
    private final int tlb_entry_count;
    
    public int getPageSizeBytesLog2() {
    	return pagesize_bytes_log2;
    }
    
    public int getFrameCountLog2() {
    	return frame_count_log2;
    }
    
    public int getPageCountLog2() {
    	return page_count_log2;
    }
    
    public int getPageTableLevel1EntryCountLog2() {
    	return level_1_pt_entries_log2;
    }
    
    public byte[] getSystemMemory() {
    	return system_memory;
    }
    
    public IScheduler getScheduler() {
    	return scheduler;
    }
    
    public int getTLBEntryCount() {
    	return tlb_entry_count;
    }
    
    public String toString() {
        return "SystemInfo:"
          + " pagesize_bytes_log2: " + pagesize_bytes_log2 
          + ". frame_count_log2: " + frame_count_log2 
          + ". page_count_log2: " + page_count_log2
          + ". level_1_pt_entries_log2: " + level_1_pt_entries_log2
          + ". tlb_entry_count: " + tlb_entry_count 
          + ".";
          
    }
    
    SystemInfo(int pg_sz_log2, 
            int frm_cnt_log2, 
            int page_cnt_log2, 
            int lev1_entry_count_log2,
            int tlb_count_log2,
            IScheduler s) {
        pagesize_bytes_log2 = pg_sz_log2;
        frame_count_log2 = frm_cnt_log2;
        page_count_log2 = page_cnt_log2; 
        level_1_pt_entries_log2 = lev1_entry_count_log2;
        system_memory = new byte[(1<<frame_count_log2) * (1<<pagesize_bytes_log2)];
        tlb_entry_count = 1<<tlb_count_log2;
        scheduler = s;
    }
}
