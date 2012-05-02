package scale.backend.sparc;

import scale.common.*;
import scale.backend.*;
import scale.clef.decl.RoutineDecl;

/** 
 * This class marks the last position in a routine.
 * <p>
 * $Id: EndMarker.java,v 1.17 2006-12-05 21:02:02 burrill Exp $
 * <p>
 * Copyright 2006 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class EndMarker extends Marker
{
  private static int createdCount = 0; /* A count of all the instances of this class that were created. */

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.sparc.EndMarker", stats);
  }

  /**
   * Return the number of instances of this class created.
   */
  public static int created()
  {
    return createdCount;
  }

  private RoutineDecl rd;

  public EndMarker(RoutineDecl rd)
  {
    super();
    this.rd = rd;
    createdCount++;
  }

  /**
   * Specify the registers used by this instruction.
   * @param rs is the register set in use
   * @param index is an index associated with the instruction
   * @param strength is the importance of the instruction
   * @see scale.backend.RegisterAllocator#useRegister(int,int,int)
   * @see scale.backend.RegisterAllocator#defRegister(int,int)
   */
  public void specifyRegisterUsage(RegisterAllocator rs, int index, int strength)
  {
//      rs.useRegister(index, SparcRegisterSet.GP_REG, strength); // Make sure these two registers are always "live".
//      rs.useRegister(index, SparcRegisterSet.SP_REG, strength);
  }

  /**
   * Insert the assembler representation of the instruction into the
   * output stream.
   */
  public void assembler(Assembler asm, Emit emit)
  {
    String name    =  rd.getName();
    String comment = null;

    if (rd.isMain() && asm.isFortran()) {
      comment = name;
      name = "MAIN__";
    }

    emit.emit("nop");
    emit.endLine();
    emit.emit("\tnop");
    emit.endLine();

    emit.emit(".size\t");
    emit.emit(name);
    emit.emit(",(.-");
    emit.emit(name);
    emit.emit(")");
    if (comment != null) {
      emit.emit("\t! ");
      emit.emit(comment);
    }
  }
}


