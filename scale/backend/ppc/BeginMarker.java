package scale.backend.ppc;

import java.util.Enumeration;
import scale.common.*;
import scale.backend.*;
import scale.clef.decl.Visibility;
import scale.clef.decl.RoutineDecl;
import scale.clef.decl.Declaration;
import scale.score.Scribble;

/** 
 * This class marks the first position in a routine.
 * <p>
 * $Id: BeginMarker.java,v 1.5 2006-12-05 21:02:02 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * An instance of this class is used to generate the assembly directives for 
 * the entry point to a routine.
 */

public class BeginMarker extends Marker
{
  private static int createdCount = 0; /* A count of all the instances of this class that were created. */
  
  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.ppc.BeginMarker", stats);
  }

  /**
   * Return the number of instances of this class created.
   */
  public static int created()
  {
    return createdCount;
  }

  private Scribble scribble; /* The associated routine. */
  private boolean  macosx; /* True of OS is mac os x. */
//  private int      adrDispReg;

  public BeginMarker(Scribble scribble, boolean macosx)
  {
    super();
    this.scribble = scribble;
    this.macosx = macosx;
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
    rs.useRegister(index, PPCRegisterSet.SP_REG, strength); // Make sure this register is always "live".
  }

  /**
   * Insert the assembler representation of the instruction into the
   * output stream.
   */
  public void assembler(Assembler asm, Emit emit)
  {
    RoutineDecl rd   = scribble.getRoutineDecl();
    int         sla  = rd.getSourceLineNumber();
    String      name = rd.getName();

    emit.endLine();

    Enumeration<String> ew = scribble.getWarnings();
    while (ew.hasMoreElements()) {
      String msg = ew.nextElement();
      emit.emit("\t! ");
      emit.emit(msg);
      emit.endLine();
    }

    if (rd.isMain() && asm.isFortran()) {
      emit.emit("\t.globl\t_MAIN__");
      emit.endLine();
      emit.emit("_MAIN__:");
    }

    if (rd.visibility() == Visibility.GLOBAL) {
      emit.emit("\t.globl\t");
          if (macosx)
            emit.emit('_');
          emit.emit(name);
      emit.endLine();
    }
        
        if (!macosx) {
      emit.emit("\t.type\t");
      emit.emit(name);
      emit.emit(",@function");
      emit.endLine();
        }
        if (macosx)
          name = "_" + name;
        emit.emit(name);
        emit.emit(':');
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(getClass().getName());
    buf.append(' ');
    buf.append(scribble);
    return buf.toString();
  }
}
