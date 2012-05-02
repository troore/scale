package scale.backend.alpha;

import scale.backend.*;
import scale.common.*;

/** 
 * This class generates Alpha assembly language from a list of Alpha
 * instructions.
 * <p>
 * $Id: AlphaAssembler.java,v 1.39 2007-10-04 19:57:50 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public final class AlphaAssembler extends scale.backend.Assembler
{
  private RegisterSet registers;
  private boolean     is;

  /**
   * @param gen is the instruction generator used to generate the
   * instructions.
   * @param source specifies the source program
   */
  public AlphaAssembler(Generator gen, String source, boolean instructionScheduling)
  {
    super(gen, source);
    this.registers = gen.getRegisterSet();
    this.is        = instructionScheduling;
  }

  /**
   * Called at the very beginning of generating assembly code.
   * Generate assembler directives needed at the beginning of the
   * assembly language file.
   */
  public void assembleProlog(Emit emit)
  {
    emit.emit("\t.set\tnoat"); // Assembly language uses no macros that use the AT register.
    emit.endLine();
    if (!is) {
      emit.emit("\t.set\tnoreorder"); // The assembler should not reorder instructions.
      emit.endLine();
    }
    emit.emit("\t.file\t1\t\""); // Debugger information
    emit.emit(source);
    emit.emit("\"");
    emit.endLine();
  }

  /**
   * Called at the very end of generating assembly code.  Generate
   * assembler directives needed at the end of the assembly language
   * file.
   */
  public void assembleEpilog(Emit emit)
  {
  }

  /**
   * Return the String representing the label.
   */
  public String getLabelString(Label label)
  {
    return "L$" + label.getLabelIndex();
  }

  /**
   * Generate a label in the assembly output.
   */
  public void assembleLabel(Label label, Emit emit)
  {
    emit.endLine();
    emit.emit(getLabelString(label));
    emit.emit(':');
  }

  /**
   * Insert the assembler representation of the comment into the
   * output stream.
   */
  public void assembleComment(String comment, Emit emit)
  {
    emit.emit(" # ");
    emit.emit(comment);
  }

  /**
   * Convert a register number into its assembly language form.
   */
  public String assembleRegister(int reg)
  {
    return registers.registerName(reg);
  }

  /**
   * Return relocation type in assembler format.
   */
  public String relocationInfo(Displacement disp, int relocType)
  {
    return ((AlphaGenerator) gen).relocationInfo(disp, relocType);
  }

  /**
   * Generate assembler directives for the start of a data area.
   * @param kind specifies the area kind
   */
  public void assembleDataAreaHeader(Emit emit, int kind)
  {
    switch (kind) {
    case AlphaGenerator.BSS:
    case AlphaGenerator.SBSS:
    case AlphaGenerator.DATA:
      emit.emit("\t.data ");
      emit.endLine();
      return;
    case AlphaGenerator.LIT4:
      emit.emit("\t.lit4 ");
      emit.endLine();
      return;
    case AlphaGenerator.LIT8:
      emit.emit("\t.lit8 ");
      emit.endLine();
      return;
    case AlphaGenerator.LITA:
      emit.emit("\t.lita ");
      emit.endLine();
      return;
    case AlphaGenerator.RCONST:
      emit.emit("\t.rconst ");
      emit.endLine();
      return;
    case AlphaGenerator.RDATA:
      emit.emit("\t.rdata ");
      emit.endLine();
      return;
    case AlphaGenerator.SDATA:
      emit.emit("\t.sdata ");
      emit.endLine();
      return;
    case AlphaGenerator.TEXT:
      emit.emit("\t.text ");
      emit.endLine();
      emit.emit("\t.align\t4 ");
      emit.endLine();
      return;
    default:
      throw new scale.common.InternalError("What area " + kind);
    }
  }

  //NONE   BYTE     SHORT    INT      LONG     FLT            DBL            ADDRESS  TEXT   LDBL
  private static final String[] directives = {
    "???", ".byte", ".word", ".long", ".quad", ".s_floating", ".t_floating", ".quad", "???", ".xxxx"};
  private static final int[]    dtsizes    = {
    0,       1,       2,       4,       8,             4,             8,       8,     0,       0};
  private static final int[]    dtalign    = {
    1,       1,       2,       4,       8,             4,             8,       8,     8,       8};

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
   * Return the number of addressable units required for one value of
   * the specified type.
   * @param dt - the data type
   * @see scale.backend.SpaceAllocation
   */
  protected int getDirectiveSize(int dt)
  {
    return dtsizes[dt];
  }

  /**
   * Generate the assembly directive required to generate a
   * zero-filled area.
   * @param emit specifies where to put the directive.
   * @param size is the number of bytes of zeros required
   */
  protected void genZeroFill(Emit emit, long size)
  {
    emit.emit("\t.space\t");
    emit.emit(size);
    emit.endLine();
  }

   /**
   * Generate the assembly directive required to generate an ASCII
   * text string.
   * @param emit specifies where to put the directive.
   * @param str is the string
   */
  protected void genAsciiText(Emit emit, String str)
  {
    emit.emit("\t.ascii\t\"");
    emit.emit(str);
    emit.emit('"');
    emit.endLine();
  }

  protected void genBytes(Emit emit, int numBytes, long data, int reps)
  {
    if (numBytes == 1) {
      emit.emit("\t.byte\t");
      emit.emit(data & 0xff);
      if (reps > 1) {
        emit.emit(" : ");
        emit.emit(reps);
      }
      emit.endLine();
      return;
    }

    // Little-endian

    for (int i = 0; i < reps; i++) {
      long d = data;
      emit.emit("\t.byte\t");
      for (int j = 0; j < numBytes; j++) {
        if (j > 0)
          emit.emit(", ");
        emit.emit(d & 0xff);
        d >>= 8;
      }
      emit.endLine();
    }
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
    int    index = lab.getLabelIndex();
    String label = "L$" + index;

    emit.emit("\t.gprel32\t");
    emit.emit(label);

    if (reps > 1) {
      emit.emit(" : ");
      emit.emit(reps);
    }

    emit.endLine();
    return getDirectiveSize(dt) * reps;
  }

  public long assembleData(Emit emit, SpaceAllocation sa, long location)
  {
    int    vis   = sa.getVisibility();
    int    dt    = sa.getType();
    long   size  = sa.getSize();
    int    reps  = sa.getReps();
    int    aln   = sa.getAlignment();
    String name  = sa.getName();
    Object value = sa.getValue();

    if (dt == SpaceAllocation.DAT_NONE) {
      switch (vis) {
      case SpaceAllocation.DAV_LOCAL:  emit.emit("\t.lcomm\t");  break;
      case SpaceAllocation.DAV_GLOBAL: emit.emit("\t.comm\t");   break;
      case SpaceAllocation.DAV_EXTERN:
        if (sa.isWeak()) {
          emit.emit("\t.weakext\t");
          emit.emit(name);
          if (sa.getValue() instanceof String) {
            emit.emit(" ");
            emit.emit((String) sa.getValue());
          }
          emit.endLine();
        }
        emit.emit("\t.extern\t");
        break;
      }
      emit.emit(name);
      emit.emit(' ');
      emit.emit(size);
      emit.endLine();
      return location;
    }

    if (dt == SpaceAllocation.DAT_TEXT) {
      assembleInstructions(emit, name, (Instruction) value);
      return location;
    }


    if (name != null) { // Generate the assembler directives to label a data area.
      long nl = Machine.alignTo(location, aln);
      if (nl > location) {
        emit.emit("\t.byte\t0 : ");
        emit.emit(nl - location);
        emit.emit("\t# align");
        emit.endLine();
        location = nl;
      }

      if (vis == SpaceAllocation.DAV_GLOBAL) {
        emit.emit("\t.globl\t");
        emit.emit(name);
        emit.endLine();
        if (sa.isWeak()) {
          emit.emit("\t.weakext\t");
          emit.emit(name);
          emit.endLine();
        }
      }

      emit.emit(name);
      emit.emit(':');
      emit.endLine();
    }

    long as = genData(emit, dt, value, reps, aln >= dtalign[dt]);
    if (size > as)
      genZeroFill(emit, size - as);

    return location + size;
  }
}
