package scale.backend.trips2;

import scale.common.*;
import scale.backend.*;
import scale.clef.decl.Visibility;
import scale.clef.decl.RoutineDecl;
import scale.clef.decl.Declaration;
import scale.score.Scribble;

/**
 * This class marks the first position in a routine.
 * <p>
 * $Id: BeginMarker.java,v 1.12 2007-08-27 18:30:13 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
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
  private Scribble scribble;   // The associated routine.
  private int      lowTempReg; // LOwest numered temp register.

  public BeginMarker(Scribble scribble)
  {
    super();
    this.scribble = scribble;
  }

  /**
   * Specifiy the lowest numbered temp register for this routine.
   */
  protected void setLowTmpReg(int lowTempReg)
  {
    this.lowTempReg = lowTempReg;
  }
  
  /**
   * Return the lowest numbered temp register.  Should be called just
   * prior to generating code for this routine.
   */
  protected int getLowTmpReg()
  {
    return lowTempReg;
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
    rs.useRegister(index, Trips2RegisterSet.SP_REG, strength);
  }

  /**
   * Insert the assembler representation of the instruction into the
   * output stream.
   */
  public void assembler(Assembler asm, Emit emit)
  {
    RoutineDecl rd   = scribble.getRoutineDecl();
    String      name = rd.getName();

    if (rd.visibility() == Visibility.GLOBAL) {
      emit.emit(rd.isWeak() ? "\t.weak\t" : "\t.global\t");
      emit.emit(name);
    } // No label implies .local.

    // Fortran needs to support refrences to "main" by both main and
    // its ptogram name
    if (rd.isMain() && asm.isFortran()) {
      emit.endLine();
      emit.emit("\t.global\t");
      emit.emit("main");
      emit.endLine();
      // output .equiv specifying that main=prognam_
      
      emit.emit("\t.equ\t");
      emit.emit("main=");
      emit.emit(name);
    }
  }

  /**
   * Return the name of the routine.
   */
  public String getName()
  {
    RoutineDecl rd = scribble.getRoutineDecl();
    return rd.getName();
  }

  public RoutineDecl getRoutine()
  {
    RoutineDecl rd = scribble.getRoutineDecl();
    return rd;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(getClass().getName());
    buf.append(' ');
    buf.append(scribble);
    return buf.toString();
  }
}
