package scale.backend.sparc;

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
 * This class generates Sparc assembly language from a list of Sparc
 * instructions.
 * <p>
 * $Id: SparcAssembler.java,v 1.41 2007-10-04 19:57:57 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public final class SparcAssembler extends scale.backend.Assembler
{
  // NONE   BYTE     SHORT    INT      LONG     FLT        DBL        ADDRESS  TEXT   LDBL
  private static final String[] directives = {
    "???", ".byte", ".half", ".word", ".word", ".single", ".double", ".word", "???", ".quad"};
  private static final int[]    dtsizes    = {
    0,       1,       2,       4,       8,         4,         8,       4,     0,      16};
  private static final int[]    dtalign    = {
    1,       1,       2,       4,       4,         4,         4,       4,     4,       4};

  private Machine     machine;
  private RegisterSet registers;

  /**
   * @param gen is the instruction generator used to generate the
   * instructions.
   * @param source specifies the source program
   */
  public SparcAssembler(Generator gen, String source)
  {
    super(gen, source);
    machine   = gen.getMachine();
    registers = gen.getRegisterSet();
    repsAllowedInAL = false;
  }

  /**
   * Called at the very beginning of generating assembly code.
   * Generate assembler directives needed at the beginning of the
   * assembly language file.
   */
  public void assembleProlog(Emit emit)
  {
    emit.emit("\t.file\t\""); // Debugger information
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
    return ".L" + label.getLabelIndex();
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
    emit.emit(" ! ");
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
   * Generate assembler directives for the start of a data area.
   * @param emit is the sink for the generated assembly code
   * @param kind specifies the area kind
   */
  public void assembleDataAreaHeader(Emit emit, int kind)
  {
    switch (kind) {
    case SparcGenerator.BSS:
      emit.emit("\t.section\t\".bss\",#alloc,#write");
      emit.endLine();
      return;
    case SparcGenerator.DATA:
      emit.emit("\t.section\t\".data\",#alloc,#write");
      emit.endLine();
      emit.emit("\t.align\t8");
      emit.endLine();
      return;
    case SparcGenerator.DATA1:
      emit.emit("\t.section\t\".data1\",#alloc,#write");
      emit.endLine();
      emit.emit("\t.align\t8");
      emit.endLine();
      return;
    case SparcGenerator.RODATA:
      emit.emit("\t.section\t\".rodata\",#alloc");
      emit.endLine();
      emit.emit("\t.align\t8");
      emit.endLine();
      return;
     case SparcGenerator.RODATA1:
      emit.emit("\t.section\t\".rodata1\",#alloc");
      emit.endLine();
      emit.emit("\t.align\t8");
      emit.endLine();
      return;
    case SparcGenerator.TEXT:
      emit.emit("\t.section\t\".text\",#alloc,#execinstr");
      emit.endLine();
      return;
    default:
      throw new scale.common.InternalError("What area " + kind);
    }
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
   * Generate the assembly directive required to generate a zero-filled area.
   * @param emit specifies where to put the directive.
   * @param size is the number of bytes of zeros required
   * @see scale.backend.SpaceAllocation
   */
  protected void genZeroFill(Emit emit, long size)
  {
    emit.emit("\t.skip\t");
    emit.emit(size);
    emit.endLine();
  }

   /**
   * Generate the assembly directive required to generate an ASCII text string.
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

  private static int[] alignments = {1, 1, 2, 4, 4, 8, 8, 8};

  /**
   * Called for each allocation in a named area.
   * @param emit is the output sink
   * @param sa is the data space
   * @param location is the current location
   */
  @SuppressWarnings("fallthrough")
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
      switch (vis) {
      case SpaceAllocation.DAV_LOCAL:
        emit.emit("\t.local\t");
        emit.emit(name);
        emit.endLine(); // Fall through
      case SpaceAllocation.DAV_GLOBAL:
        emit.emit("\t.common\t");
        emit.emit(name);
        emit.emit(',');
        emit.emit(size);
        emit.emit(',');
        emit.emit(aln);
        emit.endLine();
        if (sa.isWeak()) {
          emit.emit("\t.weak\t");
          emit.emit(name);
          emit.endLine();
        }
        break;
      case SpaceAllocation.DAV_EXTERN:
        emit.emit(sa.isWeak() ? "\t.weak\t" : "\t.global\t");
        emit.emit(name);
        emit.endLine();
        if (sa.getValue() instanceof String) {
          emit.emit("\t");
          emit.emit(name);
          emit.emit(" = ");
          emit.emit((String) sa.getValue());
          emit.endLine();
        }
        break;
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
        emit.emit(sa.isWeak() ? "\t.weak\t" : "\t.global\t");
        emit.emit(name);
        emit.endLine();
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

  /**
   * Called when a named area is ended.
   * @param emit is the output sink
   * @param sa is the data space
   * @param location is the current location
   */
  public void assembleDataEnd(Emit emit, SpaceAllocation sa, long location)
  {
    int dt = sa.getType();
    if (dt == SpaceAllocation.DAT_NONE)
      return;

    if (dt == SpaceAllocation.DAT_TEXT)
      return;

    String name = sa.getName();
    if (name == null)
      return;

    emit.emit("\t.type\t");
    emit.emit(name);
    emit.emit(",#object");
    emit.endLine();
    emit.emit("\t.size\t");
    emit.emit(name);
    emit.emit(",(.-");
    emit.emit(name);
    emit.emit(")");
    emit.endLine();
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
    int dts = getDirectiveSize(dt);
    if (data == 0) {
      long size = reps * dts;
      genZeroFill(emit, size);
      return size;
    }

    if (dts < 8)
      data &= ((1L << (dts * 8)) - 1);

    if (!aligned) {
      genBytes(emit, dts, data, reps);
      return dts * reps;
    }

    int k = 0;
    for (int i = 0; i < reps; i++) {
      if (k == 0)
        genDirective(emit, dt);
      else
        emit.emit(',');

      if (dts > 4) {
        emit.emit("0x");
        emit.emit(Long.toHexString((data >> 32) & 0xffffffff));
        data = data & 0xffffffffL;
        emit.emit(", ");
      }

      emit.emit("0x");
      emit.emit(Long.toHexString(data));

      k++;
      if (k > 5) {
        emit.endLine();
        k = 0;
      }
    }
    if (k > 0)
      emit.endLine();
    return reps * dts;
  }

  /**
   * Generate the assembly representation of <code>double</code> floating point value.
   * @param emit specifies where to generate the data
   * @param dt specifies the data type
   * @param data specifies the value
   * @param reps specifies how many times to generate the representation
   * @param aligned specifies whether the data will be aligned
   * @return the number of bytes generated
   * @see scale.backend.SpaceAllocation
   */
  protected long genData(Emit emit, int dt, double data, int reps, boolean aligned)
  {
    int dts = getDirectiveSize(dt);
    if (data == 0.0) {
      long size = reps * dts;
      genZeroFill(emit, size);
      return size;
    }

    if (!aligned) {
      genBytes(emit,
               dts,
               (dts == 4) ? Float.floatToIntBits((float) data) : Double.doubleToLongBits(data),
               reps);
      return dts * reps;
    }

    int k = 0;
    for (int i = 0; i < reps; i++) {
      if (k == 0)
        genDirective(emit, dt);
      else
        emit.emit(',');

      emit.emit("0r");
      if (Double.isInfinite(data)) {
        if (data < 0)
          emit.emit('-');
        emit.emit("inf");
      } else if (Double.isNaN(data)) {
        if (data < 0)
          emit.emit('-');
        emit.emit("nan");
      } else {
        emit.emit(Double.toString(data));
      }
      k++;
      if (k > 5) {
        emit.endLine();
        k = 0;
      }
    }
    if (k > 0)
      emit.endLine();
    return reps * dts;
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
    int dts = getDirectiveSize(dt);
    if ((data.length == 1) && (data[0] == 0)) {
      long size = reps * getDirectiveSize(dt) * data.length;
      genZeroFill(emit, size);
      return size;
    }
      
    if (!aligned) {
      for (int r = 0; r < reps; r++)
        for (int i = 0; i < data.length; i++)
          genBytes(emit, dts, data[i], 1);
      return reps * data.length * dts;
    }

    long mask = 0xffffffffffffffffL;
    if (dts < 8)
      mask = ((1L << (dts * 8)) - 1);

    for (int r = 0; r < reps; r++) {
      for (int i = 0; i < data.length; i++) {
        if ((i % 4) == 0) {
          if (i > 0)
            emit.endLine();
          genDirective(emit, dt);
        } else
          emit.emit(", ");

        long d = mask & data[i];
        if (dts > 4) {
          emit.emit("0x");
          emit.emit(Long.toHexString((d >> 32) & 0xffffffffL));
          d = d & 0xffffffffL;
          emit.emit(", ");
        }

        emit.emit("0x");
        emit.emit(Long.toHexString(d));
      }
      emit.endLine();
    }
    return reps * dts * data.length;
  }

  /**
   * Generate the assembly representation of an array of double values.
   * @param emit specifies where to generate the data
   * @param dt specifies the data type
   * @param data specifies the values
   * @param reps specifies how many times to generate the representation
   * @param aligned specifies whether the data will be aligned
   * @return the number of bytes generated
   * @see scale.backend.SpaceAllocation
   */
  protected long genData(Emit emit, int dt, double[] data, int reps, boolean aligned)
  {
    int dts = getDirectiveSize(dt);
    if ((data.length == 1) && (data[0] == 0.0)) {
      long size = reps * dts * data.length;
      genZeroFill(emit, size);
      return size;
    }
      
    if (!aligned) {
      for (int r = 0; r < reps; r++)
        for (int i = 0; i < data.length; i++)
          genBytes(emit,
                   dts,
                   ((dts == 4) ?
                    Float.floatToIntBits((float) data[i]) :
                    Double.doubleToLongBits(data[i])),
                   reps);
      return (reps * data.length * dts);
    }

    for (int r = 0; r < reps; r++) {
      for (int i = 0; i < data.length; i++) {
        if ((i % 4) == 0) {
          if (i > 0)
            emit.endLine();
          genDirective(emit, dt);
        } else
          emit.emit(", ");

        emit.emit("0r");
        emit.emit(Double.toString(data[i]));
      }
      emit.endLine();
    }
    return reps * dts * data.length;
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
    String label = ".L" + index;

    for (int i = 0; i < reps; i++) {
      emit.emit("\t.word\t");
      emit.emit(label);
      emit.endLine();
    }

    return dtsizes[dt] * reps;
  }
}
