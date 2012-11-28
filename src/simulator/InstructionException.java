/**
 * Title:        
 * Description:
 * Copyright:    Copyright (c) 2012
 * Company:      University of Texas at Austin
 * @author R.L. Rockhold, Ph.D.
 * @version 1.0
 */

package simulator;

public class InstructionException extends Exception {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private IInstruction m_instr;
    InstructionException(IInstruction instr, String msg) {
        m_instr = instr;
    }

    /**
     * @return
     */
    public IInstruction getInstruction() {
        return m_instr;
    }
}
