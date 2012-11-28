/**
 * Title:        
 * Description:
 * Copyright:    Copyright (c) 2012
 * Company:      University of Texas at Austin
 * @author R.L. Rockhold, Ph.D.
 * @version 1.0
 */

package simulator;

public class ProcessState implements IProcessState {

    long m_ip = 0;
    int [] m_regs = new int[2];
    /* (non-Javadoc)
     * @see simulator.IProcessState#getIP()
     */
    public long getIP() { 
        return m_ip;
    }

    /* (non-Javadoc)
     * @see simulator.IProcessState#setIP()
     */
    public void setIP(long ip) {
        m_ip = ip;
    }

    /* (non-Javadoc)
     * @see simulator.IProcessState#setReg(int, int)
     */
    public void setReg(int regnum, int val) {
        m_regs[regnum] = val;
    }

    /* (non-Javadoc)
     * @see simulator.IProcessState#getReg(int)
     */
    public int getReg(int regnum) {
        return m_regs[regnum];
    }

}
