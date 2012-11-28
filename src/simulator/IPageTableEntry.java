/**
 * Title:        
 * Description:
 * Copyright:    Copyright (c) 2012
 * Company:      University of Texas at Austin
 * @author R.L. Rockhold, Ph.D.
 * @version 1.0
 */

package simulator;

public interface IPageTableEntry {
    public boolean isResident();
    public int getFrameNumber();
}
