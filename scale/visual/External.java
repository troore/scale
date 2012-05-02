package scale.visual;

import java.io.*;
import java.lang.Exception;
import scale.common.*;

/**
 * Wrapper for external process creation and communication.
 * <p>
 * $Id: External.java,v 1.19 2007-03-22 13:43:17 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This wrapper simplifies the creation of external processes and
 * communicating with them through pipes.  
 */

public class External 
{
  private Process        foreign;
  private BufferedReader fromForeign = null;
  private BufferedWriter toForeign   = null;

  /**
   * This method starts an exteral program in a separate process and
   * establishes communication with it. 
   */
  public External(String s) throws java.io.IOException
  {
    foreign = Runtime.getRuntime().exec(s); // Start process.

    // Find the streams we use to communicate with the process.

    toForeign   = new BufferedWriter(new OutputStreamWriter(foreign.getOutputStream()));
    fromForeign = new BufferedReader(new InputStreamReader(foreign.getInputStream()));
    if ((toForeign == null) || (fromForeign == null))
      Msg.reportError(Msg.MSG_Broken_pipe_s, null, 0, 0, s);
  }

  /**
   * Checks if the foreign has been initialized.
   * <p>
   * This method may no longer have a purpose.
   */
  private boolean isInitialized()
  {
    return ((toForeign != null) && (fromForeign != null));
  }

  /**
   * Primitive send routine.
   * @param s String to be sent.
   */
  public void send(String s) throws java.io.IOException
  {
    // System.out.println("Send: " + s);
    toForeign.write(s, 0, s.length());
    toForeign.newLine(); // Send always appends a newline.
    toForeign.flush();
  }

  /**
   * Primitive read routine.  Returns input it has read.
   * @return string of characters read from pipe
   */
  public String read()
  {
    try {
      String line = fromForeign.readLine();
      if (line != null)
        return line;

      Msg.reportError(Msg.MSG_Unrecognized_visulaizer_response_s, null, 0, 0, "null");
      return null;

    } catch(java.io.IOException e) {
      e.printStackTrace();
      return null;
    }
  }
}
