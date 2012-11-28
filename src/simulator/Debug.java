package simulator;

import java.io.PrintStream;

/**
 * Title:        
 * Description:
 * Copyright:    Copyright (c) 2012
 * Company:      University of Texas at Austin
 * @author R.L. Rockhold, Ph.D.
 * @version 1.0
 */

public class Debug {
	
	public static int verbose = 0;
	static PrintStream output = System.out;

	
	public static void user(final String msg) {
		if (Debug.verbose > 1) {
			synchronized(output) {
				output.println("(User" + /* (" + Thread.currentThread().getName() + */ "): " + msg);
				output.flush();
			}
		}
	}

	/**
	 * Unconditionally output a message.
	 * @param msg
	 */
	public static void log(String msg) {
		synchronized(output) {
			output.println("(Log" + /* (" + Thread.currentThread().getName() + */ "):  " + msg);
			//output.println(msg);
			output.flush();
		}
	}

	/**
	 * Call this method to conditionally display a message.
	 * The configuration file's "Verbose" property controls the setting.
	 * @param msg
	 */
	public static void info(String msg) {
		if (Debug.verbose > 0) {
			synchronized(output) {
				output.println("(Info" + /* Thread.currentThread().getName() + */ "): " + msg);
				output.flush();
			}
		}
	}

		

}
