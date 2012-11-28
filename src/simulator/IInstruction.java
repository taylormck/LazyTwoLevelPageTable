/**
 * This work is unwarrented.
 * @author Ronald L. Rockhold, Ph.D.
 *
 */

package simulator;

public interface IInstruction {
    
    public String getText();
    
    public long execute(ICPU cpu, IProcess p, long ip, long d_paddr) throws InstructionException;

    /**
     * @return
     */
    public boolean hasDataAddr();

    /**
     * @return
     */
    public long getDataVaddr(ICPU cpu, IProcess p);

    public boolean writeMemory();
    
}
