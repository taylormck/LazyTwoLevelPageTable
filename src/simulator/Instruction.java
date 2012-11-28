/**
 * Title:        
 * Description:
 * Copyright:    Copyright (c) 2012
 * Company:      University of Texas at Austin
 * @author R.L. Rockhold, Ph.D.
 * @version 1.0
 */
package simulator;


abstract public class Instruction implements IInstruction {
    public static final String syscall_exit = "Syscall_Exit";
    public static final String noop = "NoOp";
    public static final String branch = "branch_uc";	// "branch_uc 3" --> Branch 3 (base 10) instructions beyond IP
    public static final String brancheq = "branch_eq";  // "branch_eq -2" --> if reg0==reg1, branch 2 (base 10) instrctions previous 
    public static final String print0 = "printReg0";	// "printReg0"  --> prints contents of reg0
    public static final String print1 = "printReg1";
    public static final String printStr = "printStr";	// "printStr Hello World!"
    public static final String incr0 = "incrReg0";		// "incrReg0 -4"  --> reduce reg0 by 0x4
    public static final String incr1 = "incrReg1";
    public static final String load0  = "loadReg0Indirect";	// "loadReg0Indirect 1" --> loads reg0 from where (data) reg1 "points" 	
    public static final String load1  = "loadReg1Indirect";	// "loadReg1Indirect 1" --> loads reg1 from where (data) reg1 "points"
    public static final String loadi0  = "loadReg0Immediate";	// "loadReg0Immediate 4c" --> loads reg0 with 0x4c 	
    public static final String loadi1  = "loadReg1Immediate";	// "loadReg1Immediate -24c" --> loads reg0 with -0x24c
    public static final String loadp0  = "loadReg0PageAddr";	// "loadReg0PageAddr d" --> loads reg0 with the virtual address of data page 0xd  	
    public static final String loadp1  = "loadReg1PageAddr";	  	
    public static final String syscall_alloc  = "Syscall_Alloc";	// Allocates contiguous virtual memory from page_at(reg0) to (page_at(reg1)-1)
    	// With reg0==4  and reg1==5, you'd get one page (page 4)
    public static final String store0  = "storeReg0Indirect";	// "storeReg0Indirect 1" --> stores reg0 from where (data) reg1 "points" 	
    public static final String store1  = "storeReg1Indirect";	// "storeReg1Indirect 1" --> stores reg1 from where (data) reg1 "points"
    
    void ExecuteTrace(String string, ICPU cpu, IProcess p, long ip) {
		Debug.info(String.format("*** (%s) %s", m_text, string));
		
	}

    void ExecuteTrace(ICPU cpu, IProcess p, long ip) {
    	ExecuteTrace("", cpu, p, ip);
    }
    
    long m_ip;

    protected String m_text;
    /**
     * @param instr
     * @param m_ip
     */
    public Instruction(String text) {
        m_text = text;
    }
    public static Instruction buildInstruction(String instr, long ip) {
        if (syscall_exit.compareToIgnoreCase(instr) == 0) {
            return new Syscall_Exit_Instruction(instr); 
        }
        if (noop.compareToIgnoreCase(instr) == 0) {
            return new NoOp_Instruction(instr); 
        }
        if (instr.startsWith(branch)) {
            return new Branch_Instruction(instr, parseIntArg(instr,10)); 
        }
        if (instr.startsWith(brancheq)) {
            return new BranchEq_Instruction(instr, parseIntArg(instr,10)); 
        }
        if (print0.compareToIgnoreCase(instr) == 0) {
            return new PrintReg_Instruction(instr, 0); 
        }
        if (print1.compareToIgnoreCase(instr) == 0) {
            return new PrintReg_Instruction(instr, 1); 
        }
        if (instr.startsWith(printStr)) {
            int ndx = instr.indexOf(" ");
            String arg = instr.substring(ndx+1);
            return new PrintString_Instruction(instr, arg); 
        }
        if (instr.startsWith(incr0)) {
            return new IncrReg_Instruction(instr, 0, parseIntArg(instr, 16)); 
        }
        if (instr.startsWith(incr1)) {
            return new IncrReg_Instruction(instr, 1, parseIntArg(instr, 16)); 
        }
        if (instr.startsWith(loadi0)) {
            return new LoadRegImmediate_Instruction(instr, 0, parseIntArg(instr, 16)); 
        }
        if (instr.startsWith(loadi1)) {
            return new LoadRegImmediate_Instruction(instr, 1, parseIntArg(instr, 16)); 
        }
        if (instr.startsWith(load0)) {
            return new LoadRegIndirect_Instruction(instr, 0, parseIntArg(instr, 16)); 
        }
        if (instr.startsWith(load1)) {
            return new LoadRegIndirect_Instruction(instr, 1, parseIntArg(instr, 16)); 
        }
        if (instr.startsWith(loadp0)) {
            return new LoadRegImmediate_Instruction(instr, 0, Simulator.page2vaddr(parseIntArg(instr, 16))); 
        }
        if (instr.startsWith(loadp1)) {
            return new LoadRegImmediate_Instruction(instr, 1, Simulator.page2vaddr(parseIntArg(instr, 16))); 
        }
        if (syscall_alloc.compareToIgnoreCase(instr) == 0) {
            return new SyscallAlloc_Instruction(instr); 
        }
        if (instr.startsWith(store0)) {
            return new StoreRegIndirect_Instruction(instr, 0, parseIntArg(instr, 16)); 
        }
        if (instr.startsWith(store1)) {
            return new StoreRegIndirect_Instruction(instr, 1, parseIntArg(instr, 16)); 
        }
        return new InvalidInstruction(instr);
    }
    
    public String getText() {
        return m_text;
    }

    static int parseIntArg(String instr, int radix) {
        String val = instr.split(" ")[1];
        return Integer.parseInt(val, radix);
    }

}
abstract class Data_Instruction extends Instruction {
    Data_Instruction(String text) {
        super(text);
    }
    public boolean hasDataAddr() { return true; }
    public boolean writeMemory() { return false; }
    abstract public long getDataVaddr(ICPU cpu, IProcess p);
    
}

class LoadRegIndirect_Instruction extends Data_Instruction {
    private int m_reg_to_load;
    private int m_reg_with_address;
    LoadRegIndirect_Instruction(String instr, int reg_to_load_num, int reg_with_address) {
        super(instr);
        m_reg_to_load = reg_to_load_num;
        m_reg_with_address = reg_with_address;
    }
    public long execute(ICPU cpu, IProcess p, long ip, long d_paddr) throws InstructionException {
        int data = Simulator.loadWord(d_paddr);
        int old_value = cpu.getReg(m_reg_to_load);
        cpu.setReg(m_reg_to_load, data);
        ExecuteTrace(String.format("'Reg%d = *(pa)0x%X' Reg%d was 0x%X, now 0x%X",
        		m_reg_to_load, d_paddr, m_reg_to_load, old_value, data), cpu, p, ip);

        return ip+Simulator.BYTES_PER_INSTRUCTION;
    }
    public long getDataVaddr(ICPU cpu, IProcess p) {
        int raw_vaddr = cpu.getReg(m_reg_with_address);
        long adjusted_vaddr = raw_vaddr + p.getStartingDataVaddr();
        return adjusted_vaddr;
    }
}

class StoreRegIndirect_Instruction extends Data_Instruction {
    private int m_reg_to_store;
    private int m_reg_with_address;
    StoreRegIndirect_Instruction(String instr, int reg_to_store_num, int reg_with_address) {
        super(instr);
        m_reg_to_store = reg_to_store_num;
        m_reg_with_address = reg_with_address;
    }
    public long execute(ICPU cpu, IProcess p, long ip, long d_paddr) throws InstructionException {
        int old_mem = Simulator.loadWord(d_paddr);
        int data = cpu.getReg(m_reg_to_store);
        Simulator.storeWord(d_paddr, data);
        ExecuteTrace(String.format("'*(pa)0x%X = Reg%d' *(pa)0x%X was 0x%X, now 0x%X",
        		d_paddr, m_reg_to_store, d_paddr, old_mem, data), cpu, p, ip);
        return ip+Simulator.BYTES_PER_INSTRUCTION;
    }
    public long getDataVaddr(ICPU cpu, IProcess p) {
        int raw_vaddr = cpu.getReg(m_reg_with_address);
        long adjusted_vaddr = raw_vaddr + p.getStartingDataVaddr();
        return adjusted_vaddr;
    }
    public boolean writeMemory(){ return true; }
}

abstract class DataLess_Instruction extends Instruction {
    DataLess_Instruction(String text) {
        super(text);
    }
    public boolean hasDataAddr() { return false; }
    public long getDataVaddr(ICPU cpu, IProcess p) {
        throw new IllegalStateException("Implement getDataVaddr!");
    }
    public boolean writeMemory() { return false; }
}

class InvalidInstruction extends DataLess_Instruction {

    public InvalidInstruction(String text) {
        super(text);
    }
    public long execute(ICPU cpu, IProcess p, long ip, long d_paddr) throws InstructionException {
        throw new InstructionException(this, "Invalid Instruction");
    }
}

class Syscall_Exit_Instruction extends DataLess_Instruction {
    Syscall_Exit_Instruction(String text) {
        super(text);
    }
    public long execute(ICPU cpu, IProcess p, long ip, long d_paddr) throws InstructionException {
    	ExecuteTrace(cpu, p, ip);
        throw new InstructionException(this, "Exit System Call");
    }
}

class SyscallAlloc_Instruction extends DataLess_Instruction {
    SyscallAlloc_Instruction(String text) {
        super(text);
    } 
    public long execute(ICPU cpu, IProcess p, long ip, long d_paddr) throws InstructionException {
        // Will throw an InstructionException if fails
        int start_page=cpu.getReg(0);
        int until_page=cpu.getReg(1);
        ExecuteTrace(String.format("SyscallAlloc start page 0x%X for 0x%X pages", start_page,  until_page - start_page), cpu, p, ip);
        Simulator.syscallAlloc(this, cpu, p, ip, start_page, until_page);
        return ip+Simulator.BYTES_PER_INSTRUCTION;
    }
}



class NoOp_Instruction extends DataLess_Instruction {
    NoOp_Instruction(String text) {
        super(text);
    }
    public long execute(ICPU cpu, IProcess p, long ip, long d_paddr) throws InstructionException {
        ExecuteTrace(cpu, p, ip);
        return ip+Simulator.BYTES_PER_INSTRUCTION;
    }
}

class PrintReg_Instruction extends DataLess_Instruction {
    private int m_regnum;
    PrintReg_Instruction(String text, int regnum) {
        super(text);
        m_regnum = regnum;
    }
    public long execute(ICPU cpu, IProcess p, long ip, long d_paddr)  {
        ExecuteTrace(cpu, p, ip);
        Debug.log("[PrintReg "+ m_regnum + "]  " + Simulator.hex(cpu.getReg(m_regnum)));
        return ip+Simulator.BYTES_PER_INSTRUCTION;
    }
}

class PrintString_Instruction extends DataLess_Instruction {
    private String m_arg;
    PrintString_Instruction(String text, String arg) {
        super(text);
        m_arg = arg;
    }
    public long execute(ICPU cpu, IProcess p, long ip, long d_paddr)  {
        ExecuteTrace(cpu, p, ip);
        Debug.log("[PrintString] " + m_arg);
        return ip+Simulator.BYTES_PER_INSTRUCTION;
    }
}

class IncrReg_Instruction extends DataLess_Instruction {
    private int m_regnum;
    private int m_incr;
    IncrReg_Instruction(String text, int regnum, int incr) {
        super(text);
        m_regnum = regnum;
        m_incr = incr;
    }
    
    public long execute(ICPU cpu, IProcess p, long ip, long d_paddr)  {
    	int old_val = cpu.getReg(m_regnum);
    	int new_val = old_val + m_incr;
        cpu.setReg(m_regnum, new_val);
    	ExecuteTrace(String.format("'Reg%d += 0x%X' Reg%d was 0x%X, now 0x%X", 
    			m_regnum, m_incr, m_regnum, old_val, new_val), cpu, p, ip);
        return ip+Simulator.BYTES_PER_INSTRUCTION;
    }
}

class LoadRegImmediate_Instruction extends DataLess_Instruction {
    private int m_regnum;
    private int m_val;
    LoadRegImmediate_Instruction(String text, int regnum, int val) {
        super(text);
        m_regnum = regnum;
        m_val = val;
    }
    public long execute(ICPU cpu, IProcess p, long ip, long d_paddr)  {
        cpu.setReg(m_regnum, m_val);
        ExecuteTrace(String.format("'Reg%d = 0x%X' Reg%d now 0x%x",
        		m_regnum, m_val, m_regnum, m_val), cpu, p, ip);
        return ip+Simulator.BYTES_PER_INSTRUCTION;
    }
}



class Branch_Instruction extends DataLess_Instruction {
    private int m_offset;
    Branch_Instruction(String text, int offset) {
        super(text);
        m_offset = offset;
    }
    public long execute(ICPU cpu, IProcess p, long ip, long d_paddr) {
        long new_ip = ip + m_offset*Simulator.BYTES_PER_INSTRUCTION;
        ExecuteTrace(String.format("Branch to instruction offset 0x%X. IP was 0x%X, now 0x%X", m_offset, ip, new_ip),cpu, p, ip);
        return new_ip;
    }
}

class BranchEq_Instruction extends DataLess_Instruction {
    private int m_offset;
    BranchEq_Instruction(String text, int offset) {
        super(text);
        m_offset = offset;
    }
    public long execute(ICPU cpu, IProcess p, long ip, long d_paddr) {
    	long new_ip;
    	int r0 = cpu.getReg(0);
    	int r1 = cpu.getReg(1);
        if (r0 == r1) {
            new_ip =  ip + m_offset*Simulator.BYTES_PER_INSTRUCTION; 
        } else {
        	new_ip = ip + Simulator.BYTES_PER_INSTRUCTION; 
        }
        ExecuteTrace(String.format("BranchEq Reg0=0x%X Reg1=0x%X instruction offset 0x%X. New ip 0x%X", 
        		r0, r1, m_offset, new_ip),cpu, p, ip);
        return new_ip;
    }
}

/*
class LoadRegPageAddr_Instruction extends DataLess_Instruction {
    private int m_regnum;
    private int m_val;
    LoadRegPageAddr_Instruction(String text, int regnum, int val) {
        super(text);
        m_regnum = regnum;
        m_val = Simulator.page2vaddr(val);
    }
    public long execute(ICPU cpu, IProcess p, long ip, long d_paddr)  {
        cpu.setReg(m_regnum, m_val);
        return ip+Simulator.BYTES_PER_INSTRUCTION;
    }
}
*/


