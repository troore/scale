package scale.backend;

import scale.common.*;
import scale.frontend.SourceLanguage;
import scale.clef.decl.*;
import scale.clef.expr.*;

/** 
 * This class is the base class for classes that translate
 * instructions into assembly language.
 * <p>
 * $Id: Assembler.java,v 1.50 2007-10-04 19:57:48 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public abstract class Assembler
{
  /**
   * Convert an integer from 0 to 15 to a hex digit.
   */
  public static final char[] hex = {'0', '1', '2', '3',
                                    '4', '5', '6', '7',
                                    '8', '9', 'A', 'B',
                                    'C', 'D', 'E', 'F'};

  /**
   * Specifies whether or not individual data items should be output
   * with their own assembly statement.  Otherwise, several will share
   * the same assembly statement.  For example,
   * <pre>
   *  .long 1, 2, 3
   * </pre>
   * or
   * <pre>
   *  .long 1
   *  .long 2
   *  .long 3
   * </pre>
   */
  public static boolean oneItemPerLine = true;

  /**
   * The Generator used to generate the instructions.
   * @see Generator
   */
  protected Generator gen;
  /**
   * The source file name.
   */
  protected String source;

  /**
   * Set true if the assembly language allows repititions to be specified by
   * a ':' and a repitition count.
   */
  protected boolean repsAllowedInAL = true;

  /**
   * Convert the Scribble CFG to instructions.
   * @param gen is the generator used to generate the instructions
   * @param source is the source file name
   */
  public Assembler(Generator gen, String source)
  {
    this.gen    = gen;
    this.source = source;
  }

  /**
   * Return the name associated with an area handle.
   */
  public final String getName(int handle)
  {
    return gen.getName(handle);
  }

  /**
   * Return the SpaceAllocation associated with the specified handle.
   */
  public final SpaceAllocation getSpaceAllocation(int handle)
  {
    return gen.getSpaceAllocation(handle);
  }

  /**
   * Generate assembly code for the data areas.
   */
  public void assemble(Emit emit, SpaceAllocation[] dataAreas)
  {
    assembleProlog(emit);
    assembleAreas(emit, dataAreas);
    assembleEpilog(emit);
  }

  /**
   * Generate assembly instructions for the list of instructions.
   * @param name is the associated routine name
   * @param firstInstruction is the head of the list of instructions
   */
  public void assembleInstructions(Emit        emit,
                                   String      name,
                                   Instruction firstInstruction)
  {
    boolean     debug = Debug.debug(1);
    Instruction inst  = firstInstruction;
    while (inst != null) {
      emit.emit('\t');
      inst.assembler(this, emit);
      if (debug && inst.isSpillInstruction())
        assembleComment("spill", emit);
      emit.endLine();
      inst = inst.getNext();
    }
  }
  
  /**
   * Return the source language of the original program.
   * @return the source language of the original program.
   */
  public final SourceLanguage getSourceLanguage()
  {
    return gen.getSourceLanguage();
  }

  /**
   * Return true if the source language is Fortran.
   */
  public final boolean isFortran()
  {
    return gen.isFortran();
  }

  /**
   * Return the String representing the label.
   */
  public abstract String getLabelString(Label label);

  /**
   * Called at the very beginning of generating assembly code.
   */
  protected abstract void assembleProlog(Emit emit);

  /**
   * Called at the very end of generating assembly code.
   */
  protected abstract void assembleEpilog(Emit emit);

  /**
   * Generate a label in the assembly output.
   */
  public abstract void assembleLabel(Label label, Emit emit);

  /**
   * Insert the assembler representation of the comment into the
   * output stream.
   */
  protected abstract void assembleComment(String comment, Emit emit);

  /**
   * Convert a register number into its assembly language form.
   */
  public abstract String assembleRegister(int reg);
  
  /**
   * Generate the assembly directive required for the type.
   * @param emit specifies where to put the directive.
   * @param dt - the data type
   * @see scale.backend.SpaceAllocation
   */
  protected abstract void genDirective(Emit emit, int dt);

  /**
   * Return the number of addressable units required for one value of
   * the specified type.
   * @param dt - the data type
   * @see scale.backend.SpaceAllocation
   */
  protected abstract int getDirectiveSize(int dt);

  /**
   * Generate the assembly directive required to generate a
   * zero-filled area.
   * @param emit specifies where to put the directive.
   * @param size is the number of bytes of zeros required
   * @see scale.backend.SpaceAllocation
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

  /**
   * Generate the assembly directives for each different kind of data
   * area.  The different kindss would be data, text, read-only data,
   * etc.  All areas of the same kind are processed together.
   * @param emit is the output sink
   * @param kind specifies the area kind
   */
  protected abstract void assembleDataAreaHeader(Emit emit, int kind);

  /**
   * Called when a named area is started.
   * @param emit is the output sink
   * @param sa is the data space
   * @param location is the current location
   */
  public void assembleDataBegin(Emit emit, SpaceAllocation sa, long location)
  {
  }

  /**
   * Called for each allocation in a named area.
   * @param emit is the output sink
   * @param sa is the data space
   * @param location is the current location
   */
  public abstract long assembleData(Emit emit, SpaceAllocation sa, long location);

  /**
   * Called when a named area is ended.
   * @param emit is the output sink
   * @param sa is the data space
   * @param location is the current location
   */
  public void assembleDataEnd(Emit emit, SpaceAllocation sa, long location)
  {
  }

  /**
   * Generate the assembly directives for each data area.
   * All areas of the same kind are processed together.
   * @param emit is the output sink
   * @param location is the current location
   */
  private void assembleAreas(Emit emit, SpaceAllocation[] dataAreas)
  {
    int na = dataAreas.length;
    int ma = gen.getMaxAreaIndex();

    for (int area = 0; area <= ma; area++) {
      boolean         flg      = true; // True if first space of this area
      long            location = 0;
      SpaceAllocation last     = null;

      for (int handle = 1; handle < na; handle++) {
        SpaceAllocation sa = dataAreas[handle];

        if (sa == null)
          continue;

        if (sa.getArea() != area)
          continue;

        if (flg) {
          assembleDataAreaHeader(emit, area);
          flg = false;
        }

        if (sa.getName() != null) {
          if (last != null)
            assembleDataEnd(emit, last, location);
          assembleDataBegin(emit, sa, location);
          last = sa;
        }
        location = assembleData(emit, sa, location);
      }
      if (last != null)
        assembleDataEnd(emit, last, location);
    }
  }

  /**
   * Generate the assembly representation of the specified data value.
   * @param emit specifies where to generate the data
   * @param dt specifies the data type
   * @param value is the data
   * @param reps specifies how many times to generate the representation
   * @param aligned specifies whether the data will be aligned
   * @return the number of bytes generated
   * @see scale.backend.SpaceAllocation
   */
  protected long genData(Emit emit, int dt, Object value, int reps, boolean aligned)
  {
    if (value instanceof Literal) {
      Literal lit = (Literal) value;

      if (lit instanceof IntLiteral)
        return genData(emit, dt, ((IntLiteral) lit).getLongValue(), reps, aligned);

      if (lit instanceof CharLiteral)
        return genData(emit, dt, ((CharLiteral) lit).getCharacterValue(), reps, aligned);

      if (lit instanceof FloatLiteral)
        return genData(emit, dt, ((FloatLiteral) lit).getDoubleValue(), reps, aligned);

      if (lit instanceof ComplexLiteral) {
        ComplexLiteral cl = (ComplexLiteral) lit;
        double         rv = cl.getReal();
        double         iv = cl.getImaginary();
        long           nb = 0;
        for (int i = 0; i < reps; i++) {
          nb +=  genData(emit, dt, rv, 1, aligned);
          nb +=  genData(emit, dt, iv, 1, aligned);
        }
        return nb;
      }

      if (lit instanceof IntArrayLiteral) {
        long[] arr = ((IntArrayLiteral) lit).getArrayValue();
        return genData(emit, dt, arr, reps, aligned);
      }

      if (lit instanceof FloatArrayLiteral) {
        double[] arr = ((FloatArrayLiteral) lit).getArrayValue();
        return genData(emit, dt, arr, reps, aligned);
      }
      if (lit instanceof AddressLiteral) {
        AddressLiteral al = (AddressLiteral) lit;
        return genData(emit, dt, al, reps, aligned);
      }
    }

    if (value instanceof String)
      return genData(emit, dt, (String) value, reps, aligned);

    if (value instanceof Long)
      return genData(emit, dt, ((Long) value).longValue(), reps, aligned);

    if (value instanceof Double)
      return genData(emit, dt, ((Double) value).doubleValue(), reps, aligned);

    if (value instanceof Declaration)
      return genData(emit, dt, (Declaration) value, reps, aligned);

    if (value instanceof Label[])
      return genData(emit, dt, (Label[]) value, reps, aligned);

    if (value instanceof Label)
      return genData(emit, dt, (Label) value, reps, aligned);

    if (value instanceof byte[])
      return genData(emit, dt, (byte[]) value, reps, aligned);

    throw new scale.common.InternalError("What data " + value);
  }

  /**
   * Generate the assembly representation of an array of label addresses.
   * @param emit specifies where to generate the data
   * @param dt specifies the data type
   * @param labels is the array of labels
   * @param reps specifies how many times to generate the representation
   * @param aligned specifies whether the data will be aligned
   * @return the number of bytes generated
   * @see scale.backend.SpaceAllocation
   */
  protected long genData(Emit    emit,
                         int     dt,
                         Label[] labels,
                         int     reps,
                         boolean aligned)
  {
    int size = 0;
    for (int j = 0; j < reps; j++) {
      for (int i = 0; i < labels.length; i++) {
        size += genData(emit, dt, labels[i], 1, aligned);
      }
    }
    return size;
  }

  /**
   * Generate the assembly representation of a Declaration.
   * @param emit specifies where to generate the data
   * @param dt specifies the data type
   * @param decl is the Declaration
   * @param reps specifies how many times to generate the
   * representation
   * @param aligned specifies whether the data will be aligned
   * @return the number of bytes generated
   * @see scale.backend.SpaceAllocation
   */
  protected long genData(Emit        emit,
                         int         dt,
                         Declaration decl,
                         int         reps,
                         boolean     aligned)
  {
    genDirective(emit, dt);
    emit.emit(decl.getName());
    if (reps > 1) {
      if (repsAllowedInAL) {
        emit.emit("\t : ");
        emit.emit(reps);
      } else {
        int repx = reps;
        while (--repx > 0) {
          emit.endLine();
          genDirective(emit, dt);
          emit.emit(decl.getName());
        }
      }
    }
    emit.endLine();
    return getDirectiveSize(dt) * reps;
  }

  /**
   * Generate the assembly representation of an address.
   * @param emit specifies where to generate the data
   * @param dt specifies the data type
   * @param lit specifies the address
   * @param reps specifies how many times to generate the representation
   * @param aligned specifies whether the data will be aligned
   * @return the number of bytes generated
   * @see scale.backend.SpaceAllocation
   */
  protected long genData(Emit           emit,
                         int            dt,
                         AddressLiteral lit,
                         int            reps,
                         boolean        aligned)
  {
    Declaration decl = lit.getDecl();
    long        off  = lit.getOffset();

    genDirective(emit, dt);
    emit.emit(decl.getName());
    if (off > 0) {
      emit.emit('+');
      emit.emit(off);
    } else if (off < 0)
      emit.emit(off);

    if (reps > 1) {
      if (repsAllowedInAL) {
        emit.emit("\t : ");
        emit.emit(reps);
      } else {
        int repx = reps;
        while (--repx > 0) {
          emit.endLine();
          genDirective(emit, dt);
          emit.emit(decl.getName());
          if (off > 0) {
            emit.emit('+');
            emit.emit(off);
          } else if (off < 0)
            emit.emit(off);
        }
      }
    }
    emit.endLine();
    return getDirectiveSize(dt) * reps;
  }

  /**
   * Generate the assembly representation of <code>long</code> integer
   * value.
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
      return dts * dts;
    }

    String str = Long.toHexString(data);
    genDirective(emit, dt);
    emit.emit("0x");
    emit.emit(str);
    if (reps > 1) {
      if (repsAllowedInAL) {
        emit.emit(" : ");
        emit.emit(reps);
      } else {
        int repx = reps;
        while (--repx > 0) {
          emit.endLine();
          genDirective(emit, dt);
          emit.emit("0x");
          emit.emit(str);
        }
      }
    }
    emit.endLine();
    return reps * dts;
  }

  /**
   * Generate the assembly representation of <code>double</code>
   * floating point value.
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
    if ((data == 0.0) && (Double.doubleToLongBits(data) == 0)) {
      long size = reps * dts;
      genZeroFill(emit, size);
      return size;
    }

    if (!aligned) {
      long bits = ((dts == 4) ?
                   Float.floatToIntBits((float) data) :
                   Double.doubleToLongBits(data));
      genBytes(emit, dts, bits, reps);
      return 8 * reps;
    }

    String str = Double.toString(data);
    boolean sp = (dt == SpaceAllocation.DAT_FLT);

    if (str.equals("NaN")) {
      str = sp ? "0x7f180000" : "0x7ff8000000000000";
      dt = sp ? SpaceAllocation.DAT_INT : SpaceAllocation.DAT_LONG;
    } else if (str.equals("Infinity")) {
      str = sp ? "0x7f100000" : "0x7ff0000000000000";
      dt = sp ? SpaceAllocation.DAT_INT : SpaceAllocation.DAT_LONG;
    } else if (str.equals("-Infinity")) {
      str = sp ? "0xff100000" : "0xfff0000000000000";
      dt = sp ? SpaceAllocation.DAT_INT : SpaceAllocation.DAT_LONG;
    }

    genDirective(emit, dt);

    emit.emit(str);

    if (reps > 1) {
      if (repsAllowedInAL) {
        emit.emit(" : ");
        emit.emit(reps);
      } else {
        int repx = reps;
        while (--repx > 0) {
          emit.endLine();
          genDirective(emit, dt);
          emit.emit(str);
        }
      }
    }
    emit.endLine();
    return reps * dts;
  }

  /**
   * Generate the assembly representation of an array of byte values.
   * @param emit specifies where to generate the data
   * @param dt specifies the data type
   * @param data specifies the values
   * @param reps specifies how many times to generate the representation
   * @param aligned specifies whether the data will be aligned
   * @return the number of bytes generated
   * @see scale.backend.SpaceAllocation
   */
  protected long genData(Emit emit, int dt, byte[] data, int reps, boolean aligned)
  {
    if ((data.length == 1) && (data[0] == 0)) {
      long size = reps * getDirectiveSize(dt) * data.length;
      genZeroFill(emit, size);
      return size;
    }

    for (int r = 0; r < reps; r++) {
      for (int i = 0; i < data.length; i++) {
        if (oneItemPerLine) {
          genDirective(emit, dt);
          emit.emit("0x");
          emit.emit(hex[(data[i] >> 4) & 0xf]);
          emit.emit(hex[(data[i]) & 0xf]);
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
        emit.emit(hex[(data[i] >> 4) & 0xf]);
        emit.emit(hex[(data[i]) & 0xf]);
      }
      emit.endLine();
    }
    return reps * getDirectiveSize(dt) * data.length;
  }

  /**
   * Generate the assembly representation of an array of long values.
   * @param emit specifies where to generate the data
   * @param dt specifies the data type
   * @param data specifies the values
   * @param reps specifies how many times to generate the
   * representation
   * @param aligned specifies whether the data will be aligned
   * @return the number of bytes generated
   * @see scale.backend.SpaceAllocation
   */
  protected long genData(Emit emit, int dt, long[] data, int reps, boolean aligned)
  {
    int dts = getDirectiveSize(dt);
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
          emit.emit(Long.toHexString(data[i]));
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
        emit.emit(Long.toHexString(data[i]));
      }
      if (!oneItemPerLine)
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
  protected long genData(Emit     emit,
                         int      dt,
                         double[] data,
                         int      reps,
                         boolean  aligned)
  {
    int dts = getDirectiveSize(dt);
    if ((data.length == 1) && (data[0] == 0.0)) {
      long size = reps * dts * data.length;
      genZeroFill(emit, size);
      return size;
    }

    if (!aligned) {
      for (int r = 0; r < reps; r++)
        for (int i = 0; i < data.length; i++) {
          long bits = ((dts == 4) ?
                       Float.floatToIntBits((float) data[i]) :
                       Double.doubleToLongBits(data[i]));
          genBytes(emit, dts, bits, reps);
        }
      return reps * data.length * 8;
    }

    for (int r = 0; r < reps; r++) {
      for (int i = 0; i < data.length; i++) {
        if (oneItemPerLine) {
          genDirective(emit, dt);
          emit.emit(Double.toString(data[i]));
          emit.endLine();
          continue;
        }

        if ((i % 4) == 0) {
          if (i > 0)
            emit.endLine();
          genDirective(emit, dt);
        } else
          emit.emit(", ");

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
  protected abstract long genData(Emit    emit,
                                  int     dt,
                                  Label   lab,
                                  int     reps,
                                  boolean aligned);

  /**
   * Generate the data representation for the string.
   * @param emit specifies where to generate the data
   * @param dt specifies the data type
   * @param value is the string
   * @param reps specifies how many times to generate the representation
   * @param aligned specifies whether the data will be aligned
   * @return the number of bytes generated
   * @see scale.backend.SpaceAllocation
   */
  protected long genData(Emit emit, int dt, String value, int reps, boolean aligned)
  {
    if (dt == SpaceAllocation.DAT_BYTE) {
      char[] data = value.toCharArray();
      int j;
      for (j = 0; j < data.length; j++)
        if (data[j] > 127)
          break;

      if (j < data.length) {
        for (int r = 0; r < reps; r++) {
          for (int i = 0; i < data.length; i++) {
            if ((i % 4) == 0) {
              if (i > 0)
                emit.endLine();
              genDirective(emit, dt);
            } else
              emit.emit(", ");

            emit.emit(Integer.toString(data[i]));
          }
        }
        emit.endLine();
      } else {
        for (int r = 0; r < reps; r++) {
          int start = 0;
          int end   = 60;
          while (end < data.length) {
            genAsciiText(emit, getDisplayString(data, start, end));
            start += 60;
            end += 60;
          }
          if (start < data.length)
            genAsciiText(emit, getDisplayString(data, start, data.length));
        }
      }
      return reps * getDirectiveSize(dt) * data.length;
    }

    int  l      = value.length();
    char[] data = new char[l];
    value.getChars(0, l, data, 0);

    for (int r = 0; r < reps; r++) {
      for (int i = 0; i < data.length; i++) {
        if ((i % 4) == 0) {
          if (i > 0)
            emit.endLine();
          genDirective(emit, dt);
        } else
          emit.emit(", ");

        emit.emit(Integer.toString(data[i]));
      }
    }

    emit.endLine();
    return reps * getDirectiveSize(dt) * data.length;
  }

  /**
   * Output an integer data item as a string of bytes.  This method is
   * used when the data is not on the correct boundary for a normal
   * assembly language directive.
   * @param emit specifies where to generate the data
   * @param numBytes is the number of bytes of data
   * @param data is the data to be output
   * @param reps is the number of times to repeat the data
   */
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

    // Big-endian

    int nb = numBytes;
    if (nb > 8) 
      nb = 8;

    for (int i = 0; i < reps; i++) {
      emit.emit("\t.byte\t");
      int k = (nb - 1) * 8;
      for (int j = 0; j < nb; j++) {
        if (j > 0)
          emit.emit(", ");
        emit.emit((data >> k) & 0xff);
        k -= 8;
      }
      emit.endLine();
    }
    if (numBytes > 8)
      genZeroFill(emit, numBytes - 8);
  }

  /**
   * Convert a string to a form suitable for display.  For example, a
   * double quote (") is converted to "\"" and "\0" is converted to
   * "\\000".
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
      case '\'': buf.append("\\\'"); break;
      case '\\': buf.append("\\\\"); break;
      default:
        if (((ch >= '\u0001') && (ch <= '\u001F')) ||
            ((ch > '\u007F') && (ch <= '\u00FF'))) {
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
