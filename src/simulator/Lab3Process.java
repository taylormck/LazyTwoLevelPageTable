package simulator;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Title:        
 * Description:
 * Copyright:    Copyright (c) 2012
 * Company:      University of Texas at Austin
 * @author R.L. Rockhold, Ph.D.
 * @version 1.0
 */

class Lab3Process implements IProcess {
	private int m_context_switches = 0;
	private int m_executed_ticks = 0;

	private int m_id;
	private String m_infile_name;
	boolean m_is_started = false;

	private String m_name;

	private Properties m_props = new Properties();

	private int m_quantum = 4;
	private int m_started_time;
	private ArrayList<String> m_trace = new ArrayList<String>();
    private Object m_object = null;
    private int m_instr_faults;
    private IProcessState m_proc_state;
    private long m_data_vaddr = -1L;

	public Lab3Process(int id, String proc_name, String infile_name) 
		throws IOException {
		this.m_id = id;
		this.m_name = proc_name;
		this.m_infile_name = infile_name;
		m_proc_state = new ProcessState();
		buildTrace();
		if (m_trace.size() < 100) {
			Debug.log(
				"Trace for process " + m_name + "(" + id + "): " + m_trace.toString());
		}
	}

	private void addInstr(String instr) {
		m_trace.add(instr);
	}

	private void addTraceForSequence(String s) {
		StringTokenizer stk = new StringTokenizer(s, ";");
		while (stk.hasMoreTokens()) {
			String elem = stk.nextToken().trim();
			if (elem.startsWith("#")) {
				break;
			}
			if (elem.startsWith("_")) {
				addTraceForTag(elem);
			} else {
			    // It's an instruction
			    addInstr(elem);
			}
		}
	}
	private void addTraceForTag(String tag) {
		String base = m_props.getProperty(tag + ".TR").trim();
		if (base == null) {
			throw new IllegalArgumentException(
				"Trace input file "
					+ m_infile_name
					+ " is missing the "
					+ tag
					+ ".TR property.");
		}
		String reps_s = m_props.getProperty(tag + ".REPS", "1").trim();
		int reps = Integer.parseInt(reps_s);
		for (int rep = 0; rep < reps; ++rep) {
			addTraceForSequence(base);
		}
	}

	private void buildTrace() throws IOException {
		FileInputStream in = new FileInputStream(m_infile_name + ".properties");
		m_props.load(in);

		addTraceForTag("ROOT");
	}
	
	public int getTextByteCount() {
	    return Simulator.BYTES_PER_INSTRUCTION * m_trace.size();
	}
	

	/* (non-Javadoc)
	 * @see IProcess#completed(int)
	 */
	public void completed(int current_time) {
		Simulator.finishedData(
			this,
			m_started_time,
			current_time,
			m_executed_ticks,
			m_context_switches);
	}

	/* (non-Javadoc)
	 * @see IProcess#getBurstHistory()
	 */

	public int getID() {
		return m_id;
	}

	public String getName() {
		return m_name;
	}

	public int getQuantum() {
		return m_quantum;
	}

	public void incrCPUTime(int ticks) {
		m_executed_ticks += ticks;
	}

	/* (non-Javadoc)
	 * @see IProcess#isStarted()
	 */
	public boolean isStarted() {
		return m_is_started;
	}

	public void setQuantum(int ticks) {
		m_quantum = ticks;
	}

	/* (non-Javadoc)
	 * @see IProcess#started(int)
	 */
	public void started(int current_time) {
		m_started_time = current_time;
		Debug.log(this +" started at time " + current_time);
		m_is_started = true;
	}

	public String toString() {
		return "Process " + m_name + "(" + m_id + ")";
	}

    /* (non-Javadoc)
     * @see simulator.IProcess#setObject(java.lang.Object)
     */
    public void setAddressSpace(Object o) {
        m_object = o;
        
    }

    /* (non-Javadoc)
     * @see simulator.IProcess#getObject()
     */
    public Object getAddressSpace() {
        return m_object;
    }

    /* (non-Javadoc)
     * @see simulator.IInternalProcess#incrDispatches()
     */
    public void incrDispatches() {
		++m_context_switches;
        
    }

    /* (non-Javadoc)
     * @see simulator.IInternalProcess#getInstructionCount()
     */
    public int getInstructionCount() {
        return m_trace.size();
    }

    /* (non-Javadoc)
     * @see simulator.IProcess#incrInstrFaults()
     */
    public void incrInstrFaults() {
        ++ m_instr_faults;
    }

    /* (non-Javadoc)
     * @see simulator.IProcess#getProcessState()
     */
    public IProcessState getState() {
        return m_proc_state;
    }

    /* (non-Javadoc)
     * @see simulator.IProcess#getInstruction(int)
     */
    public String getInstruction(int trace_ndx) {
        if (trace_ndx >= m_trace.size()) {
            return Simulator.INVALID_INSTRUCTION + " at index " + trace_ndx;
        }
        String rc = (String) m_trace.get(trace_ndx);
        return rc;
    }

    /* (non-Javadoc)
     * @see simulator.IProcess#setStartingDataVaddr(long)
     */
    public void setStartingDataVaddr(long vaddr) {
        m_data_vaddr = vaddr;
    }

    /* (non-Javadoc)
     * @see simulator.IProcess#getStartingDataVaddr()
     */
    public long getStartingDataVaddr() {
        return m_data_vaddr;
    }

}
