package scale.backend.ppc;

import scale.backend.*;
import scale.common.*;
import scale.callGraph.*;
import scale.clef.expr.*;
import scale.clef.decl.*;
import scale.score.*;
import scale.clef.type.*;
import scale.score.chords.*;
import scale.score.expr.*;

/** 
 * This class generates PPC assembly language from a list of PPC instructions.
 * <p>
 * $Id: PPCAssembler.java,v 1.27 2007-10-04 19:57:55 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public final class PPCAssembler extends scale.backend.Assembler
{
  private PPCMachine machine;
  private RegisterSet registers;

  // NONE   BYTE     SHORT     INT      LONG     FLT        DBL        ADDRESS  TEXT   LDBL
  private static final String[] directives = {
    "???", ".byte", ".short", ".long", ".long", ".single", ".double", ".long", "???", "???"};
  private static final int[]    dtsizes    = {
    0,       1,        2,       4,       8,         4,         8,       4,     0,     0};
  private static final int[]    dtalign    = {
    1,       1,        2,       4,       4,         4,         4,       4,     4,     4};

  /**
   * @param gen is the instruction generator used to generate the instructions.
   * @param source specifies the source program
   */
  public PPCAssembler(Generator gen, String source)
  {
    super(gen, source);
    registers = gen.getRegisterSet();
    this.machine = (PPCMachine)gen.getMachine();
    this.repsAllowedInAL = false;
  }

  /**
   * Called at the very beginning of generating assembly code.
   * Generate assembler directives needed at the beginning of the
   * assembly language file.
   */
  public void assembleProlog(Emit emit)
  {
//    throw new scale.common.NotImplementedError("assembleProlog");
  }

  /**
   * Called at the very end of generating assembly code.  Generate
   * assembler directives needed at the end of the assembly language
   * file.
   */
  public void assembleEpilog(Emit emit)
  {
//    throw new scale.common.NotImplementedError("assembleEpilog");
  }

  /**
   * Return the String representing the label.
   */
  public String getLabelString(Label label)
  {
        return ".L" + label.getLabelIndex();
//    throw new scale.common.NotImplementedError("getLabelString");
  }

  /**
   * Insert the assembler representation of the comment into the
   * output stream.
   */
  public void assembleComment(String comment, Emit emit)
  {
    emit.emit("# ");
    emit.emit(comment);
  }

  /**
   * Generate a label in the assembly output.
   */
  public void assembleLabel(Label label, Emit emit)
  {
    emit.endLine();
    emit.emit(getLabelString(label));
    emit.emit(':');
//    throw new scale.common.NotImplementedError("assembleLabel");
  }

  /**
   * Convert a register number into its assembly language form.
   */
  public String assembleRegister(int reg)
  {
    return registers.registerName(reg);
//    throw new scale.common.NotImplementedError("assembleRegister");
  }

  /**
   * Generate assembler directives for the start of a data area.
   * @param kind specifies the area kind
   */
  public void assembleDataAreaHeader(Emit emit, int kind)
  {
    switch (kind) {
    case PPCGenerator.BSS:
      if (machine.getOS() == PPCMachine.LINUX) {
        emit.emit("\t.section\t\".bss\" ");
        emit.endLine();
      } else {
        // emit.emit("\t.section __BSS");
      }
      return;
    case PPCGenerator.SBSS:
      emit.emit("\t.section\t\".sbss\" ");
      emit.endLine();
      return;
    case PPCGenerator.DATA:
      if (machine.getOS() == PPCMachine.LINUX) {
        emit.emit("\t.section\t\".data\" ");
      } else {
        emit.emit(".data");
      }
      emit.endLine();
      return;
    case PPCGenerator.RDATA:
    case PPCGenerator.RCONST:
      if (machine.getOS() == PPCMachine.LINUX) {
        emit.emit("\t.section\t\".rodata\" ");
      } else {
        emit.emit("\t.section __TEXT,__text,regular,pure_instructions");
        emit.endLine();
        emit.emit(".data");
      }
      emit.endLine();
      return;   
    case PPCGenerator.SDATA:
      emit.emit("\t.section\t\".sdata\" ");
      emit.endLine();
      return;
    case PPCGenerator.TEXT:
      if (machine.getOS() == PPCMachine.LINUX) {
        emit.emit("\t.section\t\".text\" ");
        emit.endLine();
        emit.emit("\t.align\t2 ");
        emit.endLine();
      } else {
        emit.emit("\t.section __TEXT,__text,regular,pure_instructions");
        emit.endLine();
        emit.emit("\t.align\t2 ");
      }
      return;
    case PPCGenerator.LIT4:
      if (machine.getOS() == PPCMachine.LINUX) {
        emit.emit("\t.section\t.rodata");
        emit.endLine();
        emit.emit("\t.align 3");
        emit.endLine();
      } else {
        emit.emit(".data");
        emit.endLine();
        emit.emit(".literal4");
        emit.endLine();
      }
      return;
    case PPCGenerator.LIT8:
      if (machine.getOS() == PPCMachine.LINUX) {
        emit.emit("\t.section\t.rodata");
        emit.endLine();
        emit.emit("\t.align 3");
        emit.endLine();
      } else {
        emit.emit(".data");
        emit.endLine();
        emit.emit(".literal8");
        emit.endLine();
      }
      return;
    case PPCGenerator.LITA:
    default:
      throw new scale.common.InternalError("What area " + kind);
    }
  }

  public long assembleData(Emit emit, SpaceAllocation sa, long location)
  {
    int    vis   = sa.getVisibility();
    int    dt    = sa.getType();
    int    size  = (int) sa.getSize();
    int    type  = sa.getType();
    int    reps  = sa.getReps();
    int    aln   = sa.getAlignment();
    String name  = sa.getName();
    Object value = sa.getValue();

    if (dt == SpaceAllocation.DAT_NONE) {
      assert (value == null) : "DAT_NONE with value " + sa;

      if (machine.getOS() == PPCMachine.LINUX) {
        switch (vis) {
        case SpaceAllocation.DAV_LOCAL:
          break;
        case SpaceAllocation.DAV_GLOBAL:
          emit.emit("\t.globl\t");
          emit.emit(name);
          emit.endLine();
          break;
        case SpaceAllocation.DAV_EXTERN:
          emit.emit("\t.extern\t");
          emit.emit(name);
          emit.endLine();
          break;
        }
        emit.emit(name);
        emit.emit(':');
        emit.endLine();
        if(size > 0)
          genZeroFill(emit, size);
      } else {
        switch (vis) {
        case SpaceAllocation.DAV_LOCAL:  
          emit.emit(".lcomm ");
          emit.emit(name);
          emit.emit(",");
          emit.emit(size);
          emit.emit(",");
          if (value == null) {
            emit.emit("0");
          }     else {
            emit.emit(value.toString());
          }
          emit.endLine();
          break;
        case SpaceAllocation.DAV_GLOBAL: 
          String namesub = name.substring(2);
          namesub = namesub.substring(0, namesub.length() - 13);
          emit.emit(".comm _");
          emit.emit(namesub);
          emit.emit(",");
          emit.emit(size);
          emit.endLine();
          emit.emit(".data");
          emit.endLine();
          emit.emit(".non_lazy_symbol_pointer");
          emit.endLine();
          emit.emit(name);
          emit.emit(":");
          emit.endLine();
          emit.emit("\t.indirect_symbol _");
          emit.emit(namesub);
          emit.endLine();                       
          emit.emit("\t.long\t0");
          emit.endLine();
          break;
        case SpaceAllocation.DAV_EXTERN:
          break;
        }
      }
      return location; 
    }

    if (dt == SpaceAllocation.DAT_TEXT) {
      assembleInstructions(emit, name, (Instruction) value);
      return location;
    }
    
    if (name != null) { // Generate the assembler directives to label a data area.
      int nl = (int) Machine.alignTo(location, aln);
      if (nl > location) {
        emit.emit("\t.align\t");
        emit.emit(aln);
        emit.endLine();
        location = nl;
      }

      if (vis == SpaceAllocation.DAV_GLOBAL) {
        emit.emit("\t.globl\t");
        emit.emit(name);
        emit.endLine();
      }

      emit.emit(name);
      emit.emit(':');
      emit.endLine();
    }

    long as = genData(emit, dt, value, reps, aln >= dtalign[dt]);
    if (size > as) {
      genZeroFill(emit, size - as);
    }

    return location + size;
  }
  
  /**
   * Generate the assembly directive required for the type.
   * @param dt - the data type
   * @param emit specifies where to put the directive.
   * @see scale.backend.SpaceAllocation
   */
  protected void genDirective(Emit emit, int dt)
  {
    emit.emit('\t');
    emit.emit(directives[dt]);
    emit.emit('\t');
  }

  /**
   * Return the number of addressable units required for one value of the
   * specified type.
   * @param dt - the data type
   * @see scale.backend.SpaceAllocation
   */
  protected int getDirectiveSize(int dt)
  {
    return dtsizes[dt];
  }

  /**
   * Generate the data representation for address of the label.
   * @param emit specifies where to generate the data
   * @param dt specifies the data type
   * @param lab is the label
   * @param reps specifies how many times to generate the representation
   * @param aligned specifies whether the data will be aligned
   * @return the number of bytes generated
   * @see scale.backend.SpaceAllocation
   */
  protected long genData(Emit emit, int dt, Label lab, int reps, boolean aligned)
  {
    int    dts   = getDirectiveSize(dt);
    int    index = lab.getLabelIndex();
    String label = ".L" + index;

    for (int i = 0; i < reps; i++) {
      emit.emit("\t.long\t");
      emit.emit(label);
      emit.endLine();
    }

    return dts * reps;
  }

  /**
   * Generate the assembly representation of <code>long</code> integer value.
   * @param emit specifies where to generate the data
   * @param dt specifies the data type
   * @param data specifies the value
   * @param reps specifies how many times to generate the representation
   * @param aligned specifies whether the data will be aligned
   * @return the number of bytes generated
   * @see scale.backend.SpaceAllocation
   */
  protected long genData(Emit emit, int dt, long data, int reps, boolean aligned)
  {
    /* This is for the 64 bit integers.
     * Smaller integers are processed by Assembler.genData()
     */
    int dts = getDirectiveSize(dt);
    if(8 == dts) {
      int lowerbits = (int)(data & 0xffffffffL);
      int upperbits = (int)((data >> 32) & 0x00000000ffffffff);
      emit.emit('\t');
      assembleComment("64 bit long integer `0x" + Long.toHexString(data) +
          "' written using 2 .long directives\n", emit);
      for(int i = 0; i < reps; i++) {
        genDirective(emit, dt);
        emit.emit("0x" + Integer.toHexString(upperbits));
        emit.endLine();
        genDirective(emit, dt);
        emit.emit("0x" + Integer.toHexString(lowerbits));
        emit.endLine();
      }
      return reps * dts;
    } else {
      return super.genData(emit, dt, data, reps, aligned);
    }
  }

  /**
   * Generate the assembly representation of an array of long values.
   * @param emit specifies where to generate the data
   * @param dt specifies the data type
   * @param data specifies the values
   * @param reps specifies how many times to generate the representation
   * @param aligned specifies whether the data will be aligned
   * @return the number of bytes generated
   * @see scale.backend.SpaceAllocation
   */
  protected long genData(Emit emit, int dt, long[] data, int reps, boolean aligned)
  {
    /*
     * This is for arrays of 64 bit integers. 
     * Arrays of smaller integers are processed by Assembler.genData()
     */
    int dts = getDirectiveSize(dt);
    if (8 == dts) {
      int bytes_generated = 0;
      for (int r = 0; r < reps; r++) {
        for (int i = 0; i < data.length; i++) {
          bytes_generated += genData(emit, dt, data[i], 1, aligned);
        }
      }
      return bytes_generated;
    } else {
      if ((data.length == 1) && (data[0] == 0)) {
        long size = reps * dts * data.length;
        genZeroFill(emit, size);
        return size;
      }

      if (!aligned) {
        for (int r = 0; r < reps; r++)
          for (int i = 0; i < data.length; i++)
            genBytes(emit, dts, data[i], 1);
        return reps * data.length * dts;
      }

      for (int r = 0; r < reps; r++) {
        for (int i = 0; i < data.length; i++) {
          if (oneItemPerLine) {
            genDirective(emit, dt);
            emit.emit("0x");
            emit.emit(Integer.toHexString((int)(data[i])));
            emit.endLine();
            continue;
          }

          if ((i % 4) == 0) {
            if (i > 0)
              emit.endLine();
            genDirective(emit, dt);
          } else
            emit.emit(", ");
          emit.emit("0x");
          emit.emit(Integer.toHexString((int)(data[i])));
        }
        if (!oneItemPerLine)
          emit.endLine();
      }
      return reps * dts * data.length;
    }
  }

  /**
   * Convert a string to a form suitable for display.
   * For example, a double quote (") is converted to "\"" and "\0" is converted to "\\000".
   * @param v the original string
   * @return the converted string
   */
  public String getDisplayString(char[] v, int start, int end)
  {
    StringBuffer buf = new StringBuffer("");

    for (int i = start; i < end; i++) {
      char ch = v[i];
      switch (ch) {
      case '\0': buf.append("\\000"); break;
      case '\b': buf.append("\\b"); break;
      case '\t': buf.append("\\t"); break;
      case '\n': buf.append("\\n"); break;
      case '\f': buf.append("\\f"); break;
      case '\r': buf.append("\\r"); break;
      case '"' : buf.append("\\\""); break;
      case '\\': buf.append("\\\\"); break;
      default:
        if (((ch >= '\u0001') && (ch <= '\u001F')) || ((ch > '\u007F') && (ch <= '\u00FF'))) {
          // Not all assemblers seem to handle "\xhh" correctly.
          String oct = Long.toOctalString(ch);
          int    ol  = oct.length();
          buf.append('\\');
          while (ol++ < 3)
            buf.append('0');
          buf.append(oct);
        } else {
          buf.append(ch);
        }
        break;
      }
    }

    return buf.toString();
  }
}
