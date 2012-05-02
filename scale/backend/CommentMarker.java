package scale.backend;

import scale.common.*;

/** 
 * This class is used to associate comments with instructions and is used for debugging
 * backend code generators.
 * <p>
 * $Id: CommentMarker.java,v 1.4 2005-06-15 04:17:41 asmith Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class CommentMarker extends Marker
{
  private String comment;  /* Other associated information. */

  /**
   * Mark the position corresponding to a source program line.
   * @param comment is the comment
   */
  public CommentMarker(String comment)
  {
    super();
    this.comment = comment;
  }

  /**
   * Return the other associated information.
   */
  public Object getComment()
  {
    return comment;
  }

  /**
   * Insert the assembler representation of the comment into the output stream. 
   */
  public void assembler(Assembler asm, Emit emit)
  {
    asm.assembleComment(comment, emit);
  }

  public String toString()
  {
    return "#\t" + comment;
  }
}


