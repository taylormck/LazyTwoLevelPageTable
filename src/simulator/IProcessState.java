package simulator;
/**
 * Title:        
 * Description:
 * Copyright:    Copyright (c) 2012
 * Company:      University of Texas at Austin
 * @author R.L. Rockhold, Ph.D.
 * @version 1.0
 */


public interface IProcessState {
    long getIP();
    void setIP(long ip);
    /**
     * @param i
     * @param reg
     */
    void setReg(int regnum, int val);
    int getReg(int regnum);
}
