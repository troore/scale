package scale.backend;

import java.util.Iterator;
import java.util.Enumeration;

import scale.common.*;
import scale.clef.TypePredicate;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.type.*;

/** 
 * This class represents "stabs" debugging information.
 * <p>
 * $Id: Stabs.java,v 1.42 2007-10-04 19:57:49 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * <b>Notes</b>
 * <ul>
 * <li>Do not call the <code>visit...Type</code> methods from other classes.
 * <li>This class has not been enhanced yet to handle Fortran programs correctly.
 * <li>This version is based upon Solaris stabs which differ from GCC stabs
 * and AIX stabs in important details.
 * </ol>
 */
public class Stabs implements TypePredicate
{
  /**
   * Output stabs in Solaris format.
   */
  public static final int FMT_SOLARIS = 0;

  /**
   * Output stabs in GCC format.
   */
  public static final int FMT_GCC = 1;

  /**
   * Global symbol.
   */
  public static final short N_GSYM = 0x20; // 32

  /**
   * Function name (for BSD Fortran).
   */
  public static final short N_FNAME = 0x22; // 34

  /**
   * Function name or text segment variable.
   */
  public static final short N_FUN = 0x24; // 36

  /**
   * Data segment file-scope variable.
   */
  public static final short N_STSYM = 0x26; // 38

  /**
   * BSS segment file-scope variable.
   */
  public static final short N_LCSYM = 0x28; // 40

  /**
   * Name of main routine.
   */
  public static final short N_MAIN = 0x2a; // 42

  /**
   * Variable in <CODE>.rodata</CODE> section.
   */
  public static final short N_ROSYM = 0x2c; // 44

  /**
   * Global symbol (for Pascal).
   */
  public static final short N_PC = 0x30; // 48

  /**
   * Number of symbols (according to Ultrix V4.0).
   */
  public static final short N_NSYMS = 0x32; // 50

  /**
   * No DST map.
   */
  public static final short N_NOMAP = 0x34; // 52

  /**
   * Displacement file (Solaris2).
   */
  public static final short N_OBJ = 0x38; // 56

  /**
   * Debugger options (Solaris2).
   */
  public static final short N_OPT = 0x3c; // 60

  /**
   * Register variable.
   */
  public static final short N_RSYM = 0x40; // 64

  /**
   * Modula-2 compilation unit.
   */
  public static final short N_M2C = 0x42; // 66

  /**
   * Line number in text segment.
   */
  public static final short N_SLINE = 0x44; // 68

  /**
   * Line number in data segment.
   */
  public static final short N_DSLINE = 0x46; // 70

  /**
   * Line number in bss segment.
   */
  public static final short N_BSLINE = 0x48; // 72

  /**
   * Sun source code browser, path to <TT>`.cb'</TT> file.
   */
  public static final short N_BROWS = 0x48; // 72

  /**
   * GNU Modula2 definition module dependency.
   */
  public static final short N_DEFD = 0x4a; // 74

  /**
   * Function start/body/end line numbers (Solaris2).
   */
  public static final short N_FLINE = 0x4c; // 76

  /**
   * GNU C++ exception variable.
   */
  public static final short N_EHDECL = 0x50; // 80

  /**
   * Modula2 info "for imc" (according to Ultrix V4.0).
   */
  public static final short N_MOD2 = 0x50; // 80

  /**
   * GNU C++ <CODE>catch</CODE> clause.
   */
  public static final short N_CATCH = 0x54; // 84

  /**
   * Structure of union element.
   */
  public static final short N_SSYM = 0x60; // 96

  /**
   * Last stab for module (Solaris2).
   */
  public static final short N_ENDM = 0x62; // 98

  /**
   * Path and name of source file.
   */
  public static final short N_SO = 0x64; // 100

  /**
   * Stack variable.
   */
  public static final short N_LSYM = 0x80; // 128

  /**
   * Beginning of an include file (Sun only).
   */
  public static final short N_BINCL = 0x82; // 130

  /**
   * Name of include file.
   */
  public static final short N_SOL = 0x84; // 132

  /**
   * Parameter variable.
   */
  public static final short N_PSYM = 0xa0; // 160

  /**
   * End of an include file.
   */
  public static final short N_EINCL = 0xa2; // 162

  /**
   * Alternate entry point.
   */
  public static final short N_ENTRY = 0xa4; // 164

  /**
   * Beginning of a lexical block.
   */
  public static final short N_LBRAC = 0xc0; // 192

  /**
   * Place holder for a deleted include file.
   */
  public static final short N_EXCL = 0xc2; // 194

  /**
   * Modula2 scope information (Sun linker).
   */
  public static final short N_SCOPE = 0xc4; // 196

  /**
   * End of a lexical block.
   */
  public static final short N_RBRAC = 0xe0; // 224

  /**
   * Begin named common block.
   */
  public static final short N_BCOMM = 0xe2; // 226

  /**
   * End named common block.
   */
  public static final short N_ECOMM = 0xe4; // 228

  /**
   * Member of a common block.
   */
  public static final short N_ECOML = 0xe8; // 232

  /**
   * Pascal <CODE>with</CODE> statement: type,,0,0,offset (Solaris2).
   */
  public static final short N_WITH = 0xea; // 234

  /**
   * Gould non-base registers.
   */
  public static final short N_NBTEXT = 0xf0; // 240

  /**
   * Gould non-base registers.
   */
  public static final short N_NBDATA = 0xf2; // 242

  /**
   * Gould non-base registers.
   */
  public static final short N_NBBSS = 0xf4; // 244

  /**
   * Gould non-base registers.
   */
  public static final short N_NBSTS = 0xf6; // 246

  /**
   * Gould non-base registers.
   */
  public static final short N_NBLCS = 0xf8; // 248

  /**
   * The source program is written in assembly.
   */
  public static final byte N_SO_AS = 1;
  /**
   * The source program is written in C.
   */
  public static final byte N_SO_C  = 2;
  /**
   * The source program is written in ANSI C.
   */
  public static final byte N_SO_ANSI_C = 3;
  /**
   * The source program is written in C++.
   */
  public static final byte N_SO_CC = 4;
  /**
   * The source program is written in Fortran 77.
   */
  public static final byte N_SO_FORTRAN = 5;
  /**
   * The source program is written in Pascal.
   */
  public static final byte N_SO_PASCAL = 6;
  /**
   * The source program is written in Fortran 90.
   */
  public static final byte N_SO_FORTRAN90 = 7;

  private static final byte NONE   = 0;
  private static final byte STABS  = 1;
  private static final byte STABN  = 2;
  private static final byte STABD  = 3;
  private static final byte STABX  = 4;
  private static final byte XSTABS = 5;

  // The stab entries are stored using the following arrays.

  private byte[]         format; // Type of stab.
  private String[]       string; // String description.
  private short[]        type;   // Stab type - e.g., N_LSYM, etc.
  private byte[]         other;  // other field
  private int[]          desc;   // desc field
  private Displacement[] value;  // value field if symbolic or stack offset.
  private int[]          value2; // value field if constant integer or register.
  private int            number; // Index of next available entry in the arrays.

  private boolean gcc;
  private boolean isFortran;
  private int     fmt;
  private int     lang;

  private HashMap<Type, String> typeMap; // Map from a type to it's number - e.g., (0,14).
  private Machine machine;
  private int     typeNumber; // The next type number to use.
  private int     fileNum;    // File number for declaration.
  private String  typeString; // String generated by a visit to a type.

  /**
   * @param machine specifies the target architecture
   * @param fmt specifies the stabs format: FMT_SOLARIS, FMT_GCC, etc.
   * @param lang specifies the source language: N_SO_C, N_SO_FORTRAN, etc
   */
  public Stabs(Machine machine, int fmt, int lang)
  {
    this.machine    = machine;
    this.fmt        = fmt;
    this.lang       = lang;
    this.format     = new byte[256];
    this.string     = new String[256];
    this.type       = new short[256];
    this.other      = new byte[256];
    this.desc       = new int[256];
    this.value      = new Displacement[256];
    this.value2     = new int[256];
    this.number     = 0;
    this.typeMap    = new HashMap<Type, String>(203);
    this.typeNumber = 1;
    this.fileNum    = 0;
    this.typeString = null;

    this.gcc        = (fmt == FMT_GCC);
    this.isFortran  = (lang == N_SO_FORTRAN) || (lang == N_SO_FORTRAN90);
  }

  /**
   * Expand the array of stab information.
   */
  private void expand()
  {
    if ((number + 1) < string.length)
      return;

    byte[] nf = new byte[number + 256];
    System.arraycopy(format, 0, nf, 0, format.length);
    format = nf;

    String[] ns = new String[number + 256];
    System.arraycopy(string, 0, ns, 0, string.length);
    string = ns;

    short[] nt = new short[number + 256];
    System.arraycopy(type, 0, nt, 0, type.length);
    type = nt;

    byte[] no = new byte[number + 256];
    System.arraycopy(other, 0, no, 0, other.length);
    other = no;

    int[] nd = new int[number + 256];
    System.arraycopy(desc, 0, nd, 0, desc.length);
    desc = nd;

    Displacement[] nv = new Displacement[number + 256];
    System.arraycopy(value, 0, nv, 0, value.length);
    value = nv;

    int[] nv2 = new int[number + 256];
    System.arraycopy(value2, 0, nv2, 0, value2.length);
    value2 = nv2;
  }

  /**
   * Add a new "stabs".
   * @return the handle for this new stabs
   */
  public int addStabs(String string, short type, int other, int desc, Displacement value)
  {
    expand();
    this.format[number] = STABS;
    this.string[number] = string;
    this.type[number]   = type;
    this.other[number]  = (byte) other;
    this.desc[number]   = desc;
    this.value[number]  = value;
    return this.number++;
  }

  /**
   * Add a new "stabs".
   * @return the handle for this new stabs
   */
  public int addStabs(String string, short type, int other, int desc, int value)
  {
    expand();
    this.format[number] = STABS;
    this.string[number] = string;
    this.type[number]   = type;
    this.other[number]  = (byte) other;
    this.desc[number]   = desc;
    this.value[number]  = null;
    this.value2[number] = value;
    return this.number++;
  }

  /**
   * Add a new "xstabs".
   * @return the handle for this new stabs
   */
  public int addXStabs(String string, short type, int other, int desc, Displacement value)
  {
    expand();
    this.format[number] = XSTABS;
    this.string[number] = string;
    this.type[number]   = type;
    this.other[number]  = (byte) other;
    this.desc[number]   = desc;
    this.value[number]  = value;
    this.value2[number] = 0;
    return this.number++;
  }

  /**
   * Add a new "xstabs".
   * @return the handle for this new stabs
   */
  public int addXStabs(String string, short type, int other, int desc, int value)
  {
    expand();
    this.format[number] = XSTABS;
    this.string[number] = string;
    this.type[number]   = type;
    this.other[number]  = (byte) other;
    this.desc[number]   = desc;
    this.value[number]  = null;
    this.value2[number] = value;
    return this.number++;
  }

  /**
   * Add a new "stabn".
   * @return the handle for this new stabn
   */
  public int addStabn(short type, int other, int desc, Displacement value)
  {
    expand();
    this.format[number] = STABN;
    this.string[number] = null;
    this.type[number]   = type;
    this.other[number]  = (byte) other;
    this.desc[number]   = desc;
    this.value[number]  = value;
    this.value2[number] = 0;
    return this.number++;
  }

  /**
   * Add a new "stabn".
   * @return the handle for this new stabn
   */
  public int addStabn(short type, int other, int desc, int value)
  {
    expand();
    this.format[number] = STABN;
    this.string[number] = null;
    this.type[number]   = type;
    this.other[number]  = (byte) other;
    this.desc[number]   = desc;
    this.value[number]  = null;
    this.value2[number] = value;
    return this.number++;
  }

  /**
   * Add a new "stabd".
   * @return the handle for this new stabd
   */
  public int addStabd(short type, int other, int desc)
  {
    expand();
    this.format[number] = STABD;
    this.string[number] = null;
    this.type[number]   = type;
    this.other[number]  = (byte) other;
    this.desc[number]   = desc;
    this.value[number]  = null;
    this.value2[number] = 0;
    return this.number++;
  }

  /**
   * Add a new "stabx".
   * @return the handle for this new stabx
   */
  public int addStabx(String string, Displacement value, short type, short std_type)
  {
    expand();
    this.format[number] = STABX;
    this.string[number] = string;
    this.type[number]   = type;
    this.other[number]  = 0;
    this.desc[number]   = std_type;
    this.value[number]  = value;
    this.value2[number] = 0;
    return this.number++;
  }

  /**
   * Add a new "stabx".
   * @return the handle for this new stabx
   */
  public int addStabx(String string, int value, short type, short std_type)
  {
    expand();
    this.format[number] = STABX;
    this.string[number] = string;
    this.type[number]   = type;
    this.other[number]  = 0;
    this.desc[number]   = std_type;
    this.value[number]  = null;
    this.value2[number] = value;
    return this.number++;
  }

  /**
   * Return the number of stab entries.
   */
  public int numberStabs()
  {
    return number;
  }

  /**
   * Renumber the registers in N_RSYM entries after register
   * allocation.  If the map entry for a register is greater than or
   * equal to <code>nrr</code>, the corresponding stabs entry is
   * eliminated.
   * @param first is the handle of the first stab entry to process
   * @param end is the handle of the stab entry after the last entry to process
   * @param map specifies the register mapping: virtual->real
   * @param nrr is the number of real registers
   */
  public void renumberRegisters(int first, int end, int[] map, int nrr)
  {
    for (int i = first; i < end; i++) {
      if (type[i] != N_RSYM)
        continue;

      assert (value[i] == null) : "An int value should be used.";

      if (value2[i] < map.length)
        value2[i] = map[value2[i]];

      if (value2[i] >= nrr)
        format[i] = NONE;
    }
  }

  /**
   * Generate the assembly statement for all of the stabs.
   */
  public void assemble(Assembler asm, Emit emit)
  {
    for (int i = 0; i < number; i++) {
      switch (format[i]) {
      case NONE: continue;
      case XSTABS:
        emit.emit("\t.xstabs\t\".stab.index\",");
        emit.emit('"');
        emit.emit(string[i]);
        emit.emit('"');
        emit.emit(',');
        emit.emit(type[i]);
        emit.emit(',');
        emit.emit(other[i]);
        emit.emit(',');
        emit.emit(desc[i]);
        emit.emit(',');
        if (value[i] != null)
          emit.emit(value[i].assembler(asm));
        else
          emit.emit(value2[i]);
        break;
      case STABS:
        emit.emit("\t.stabs\t");
        emit.emit('"');
        emit.emit(string[i]);
        emit.emit('"');
        emit.emit(',');
        emit.emit(type[i]);
        emit.emit(',');
        emit.emit(other[i]);
        emit.emit(',');
        emit.emit(desc[i]);
        emit.emit(',');
        if (value[i] != null) {
          if (value[i].isZero())
            emit.emit('0');
          else
            emit.emit(value[i].assembler(asm));
        } else
          emit.emit(value2[i]);
        break;
      case STABN:
        emit.emit("\t.stabn\t");
        emit.emit(type[i]);
        emit.emit(',');
        emit.emit(other[i]);
        emit.emit(',');
        emit.emit(desc[i]);
        emit.emit(',');
        if (value[i] != null)
          emit.emit(value[i].assembler(asm));
        else
          emit.emit(value2[i]);
        emit.emit("+0");
        break;
      case STABD:
        emit.emit("\t.stabd\t");
        emit.emit(type[i]);
        emit.emit(',');
        emit.emit(other[i]);
        emit.emit(',');
        emit.emit(desc[i]);
        break;
      case STABX:
        emit.emit("\t.stabx\t");
        emit.emit('"');
        emit.emit(string[i]);
        emit.emit('"');
        emit.emit(',');
        if (value[i] != null)
          emit.emit(value[i].assembler(asm));
        else
          emit.emit(value2[i]);
        emit.emit(',');
        emit.emit(desc[i]);
        break;
      }
      emit.endLine();
    }
  }

  /**
   * Associate a number with a type.  Return the string to use in the
   * stab description for the type.
   */
  private String addType(Type type)
  {
    String ts = typeMap.get(type);
    if (ts != null)
      return ts;

    StringBuffer buf = new StringBuffer("(");
    buf.append(fileNum);
    buf.append(',');
    buf.append(typeNumber++);
    buf.append(')');

    ts = buf.toString();
    typeMap.put(type, ts);

    return ts;
  }


  /**
   * Generate the stabs for this {$link scale.clef.type.Type Type}
   * and all the other types it depends upon.
   */
  public void genTypeDecl(TypeDecl decl)
  {
    RefType rt = (RefType) decl.getType();
    typeString = typeMap.get(rt);
    if (typeString != null)
      return;

    Type   to  = rt.getRefTo();
    String ts  = addType(rt);

    to.visit(this);
    String tos = typeString;

    StringBuffer buf = new StringBuffer(decl.getName());
    buf.append(":");

    Type toc = to.getCoreType();
    if (!gcc && (toc.isAggregateType() || toc.isEnumerationType()))
      buf.append("T");
    else
      buf.append("t");

    buf.append(ts);
    buf.append('=');
    buf.append(tos);

    addStabs(buf.toString(), N_LSYM, 0, 0, 0);
  }

  /**
   * Generate the stabs for this {$link scale.clef.type.Type Type}
   * and all the other types it depends upon.
   */
  public void genTypeName(TypeName tn)
  {
    RefType rt = (RefType) tn.getType();
    typeString = typeMap.get(rt);
    if (typeString != null)
      return;

    Type   to = rt.getRefTo();
    String ts = addType(rt);

    StringBuffer buf = new StringBuffer(tn.getName());
    buf.append(":");

    Type toc = to.getCoreType();
    if (!gcc && (toc.isAggregateType() || toc.isEnumerationType()))
      buf.append("T");
    else
      buf.append("t");

    buf.append(ts);
    buf.append('=');

    String tos = typeMap.get(to);
    if (tos == null) {
      Declaration decl = to.getDecl();
      if (false && toc.isUnionType()) {
        buf.append("xu");
        buf.append(decl.getName());
        buf.append(":0,");
        buf.append(to.memorySize(machine));
        buf.append(';');
      } else if (false && toc.isAggregateType()) {
        buf.append("xs");
        buf.append(decl.getName());
        buf.append(":0,");
        buf.append(to.memorySize(machine));
        buf.append(';');
      } else if (false && toc.isEnumerationType()) {
        buf.append("xe");
        buf.append(decl.getName());
        buf.append(":0,");
        buf.append(to.memorySize(machine));
        buf.append(';');
      } else {
        to.visit(this);
        buf.append(typeString);
      }
    } else {
      buf.append(tos);
    }

    addStabs(buf.toString(), N_LSYM, 0, 0, 0);
  }

  /**
   * Generate the stab for a {$link scale.clef.decl.RoutineDecl RoutineDecl}.
   * Return the stab handle.
   */
  public int genFtnDescriptor(RoutineDecl rd)
  {
    ProcedureType pt = (ProcedureType) rd.getCoreType();

    pt.getReturnType().visit(this);

    String        rs = typeString;
    String        fs = processFormals(pt, true);

    StringBuffer  buf = new StringBuffer(rd.getName());
    buf.append(':');
    if ((rd.getScribbleCFG() == null) && !gcc)
      buf.append('P');
    else
      buf.append((rd.visibility() == Visibility.GLOBAL) ? 'F' : 'f');

    buf.append(rs);

    if (fs.length() > 0) {
      buf.append(';');
      buf.append(fs);
    }

    if (gcc && (rd.visibility() != Visibility.EXTERN))
      return addStabs(buf.toString(), N_FUN, 0, 0, new SymbolDisplacement(rd.getName(), 0));

    return addStabs(buf.toString(), N_FUN, 0, 0, 0);
  }

  /**
   * Don't define types for stabs entries that may be eliminated
   * later.
   */
  private String handleVarType(Type type, String name)
  {
    String ts = typeMap.get(type);
    if (ts != null)
      return ts;

    type.visit(this);
    String ss = typeString;

    StringBuffer buf = new StringBuffer("__");
    buf.append(name);
    buf.append("_t:t");
    buf.append(ss);
    addStabs(buf.toString(), N_LSYM, 0, 0, 0);

    return typeMap.get(type);
  }

  /**
   * Generate the stab for a {$link scale.clef.decl.VariableDecl VariableDecl}.
   * Return the stab handle.
   */
  public int genVarDescriptor(VariableDecl decl)
  {
    Type       type = decl.getType();
    int        bs   = type.memorySizeAsInt(machine);
    Visibility vis  = decl.visibility();

    if (vis == Visibility.EXTERN)
      return -1;

    String       ts  = handleVarType(type, decl.getName());
    StringBuffer buf = new StringBuffer(decl.getName());
    buf.append(':');

    switch (vis) {
    case LOCAL:
      buf.append(ts);
      return addStabs(buf.toString(), N_LSYM, 0, bs, 0);
    case FILE:
      buf.append('S');
      buf.append(ts);
      if (decl.getInitialValue() == null)
        return addStabs(buf.toString(), N_LCSYM, 0, bs, 0);
      if (decl.isConst())
        return addStabs(buf.toString(), N_ROSYM, 0, bs, 0);
      return addStabs(buf.toString(), N_STSYM, 0, bs, decl.getDisplacement());
    case GLOBAL:
      if (decl.isEquivalenceDecl()) {
        buf.append('V');
        buf.append(ts);
        int offset = (int) ((EquivalenceDecl) decl).getBaseOffset();
        return addStabs(buf.toString(), N_GSYM, 0, 0, offset);
      }

      buf.append('G');
      buf.append(ts);
      if (fmt == FMT_SOLARIS)
        addXStabs(decl.getName(), N_GSYM, 0, bs, 0);
      return addStabs(buf.toString(), N_GSYM, 0, bs, 0);
    default:
      return -1;
    }
  }

  /**
   * Associate information with a variable Declaration kept in a register.
   * @param decl is the variable
   * @param register is the register allocated for the variable
   */
  public void defineDeclInRegister(Declaration decl, int register)
  {
    if (decl.isTemporary())
      return;

    Type         type = decl.getType();
    int          bs   = type.memorySizeAsInt(machine);
    String       ts   = handleVarType(type, decl.getName());
    StringBuffer buf  = new StringBuffer(decl.getName());
    buf.append(":r");
    buf.append(ts);

    addStabs(buf.toString(), N_RSYM, 0, bs, register);
  }

  /**
   * Associate information with a variable Declaration kept in a register.
   * @param decl is the variable
   * @param register is the register allocated for the variable
   */
  public void defineParameterInRegister(Declaration decl, int register)
  {
    Type         type = decl.getType();
    int          bs   = type.memorySizeAsInt(machine);
    String       ts   = handleVarType(type, decl.getName());
    StringBuffer buf  = new StringBuffer(decl.getName());
    buf.append(":p");
    buf.append(ts);
    addStabs(buf.toString(), N_RSYM, 0, bs, register);
  }

  /**
   * Associate information with a Declaration kept on the stack.
   * @param decl is the variable
   * @param disp - displacement associated with declaration
   */
  public void defineDeclOnStack(Declaration decl, Displacement disp)
  {
    Type         type = decl.getType();
    int          bs   = type.memorySizeAsInt(machine);
    String       ts   = handleVarType(type, decl.getName());
    StringBuffer buf  = new StringBuffer(decl.getName());
    buf.append(":");
    buf.append(ts);
    addStabs(buf.toString(), N_LSYM, 0, bs, disp);
  }

  /**
   * Associate information with a Declaration kept on the stack.
   * @param decl is the variable
   * @param disp - displacement associated with declaration
   */
  public void defineParameterOnStack(Declaration decl, Displacement disp)
  {
    Type         type = decl.getType();
    int          bs   = type.memorySizeAsInt(machine);
    String       ts   = handleVarType(type, decl.getName());
    StringBuffer buf  = new StringBuffer(decl.getName());
    buf.append(":p");
    buf.append(ts);
    addStabs(buf.toString(), N_PSYM, 0, bs, disp);
  }

  /**
   * Associate information with a Declaration kept in memory.
   * @param decl is the variable
   * @param disp - displacement associated with declaration
   */
  public void defineDeclInMemory(Declaration decl, Displacement disp)
  {
    VariableDecl vd = (VariableDecl) decl;
    if (vd.isCommonBaseVariable())
      return;
    genVarDescriptor(vd);
  }

  private Table<VariableDecl, EquivalenceDecl> commonTable;

  /**
   * Associate information with a Declaration in COMMON.
   * @param decl is the variable
   * @param disp - displacement associated with declaration
   */
  public void defineDeclInCommon(Declaration decl, Displacement disp)
  {
    EquivalenceDecl vd   = (EquivalenceDecl) decl;
    VariableDecl    base = vd.getBaseVariable();

    if (commonTable == null)
      commonTable = new Table<VariableDecl, EquivalenceDecl>();

    commonTable.add(base, vd);
  }

  /**
   * Associate information with a routine.
   * @param rd is the routine
   * @param disp - displacement associated with declaration
   */
  public void defineRoutineInfo(RoutineDecl rd, Displacement disp)
  {
    if (!rd.isReferenced())
      return;

    genFtnDescriptor(rd);
  }

  /**
   * Prepare for processing COMMON variables.
   */
  public void startCommon()
  {
  }

  /**
   * Finish processing COMMON variables.
   */
  public void endCommon()
  {
    if (commonTable == null)
      return;

    Enumeration<VariableDecl> baseEnum = commonTable.keys();
    while (baseEnum.hasMoreElements()) {
      VariableDecl base = baseEnum.nextElement();
      addStabs(base.getName(), N_BCOMM, 0,0,0);
      Iterator<EquivalenceDecl> it = commonTable.getRowEnumeration(base);
      while (it.hasNext())
        genVarDescriptor(it.next());
      addStabs(base.getName(), N_ECOMM, 0,0,0);
    }
    commonTable = null;
  }

  public void visitAggregateType(AggregateType type)
  {
    throw new scale.common.InternalError("AggregateType");
  }

  public void visitArrayType(ArrayType type)
  {
    throw new scale.common.InternalError("ArrayType");
  }

  public void visitAtomicType(AtomicType type)
  {
    throw new scale.common.InternalError("AtomicType");
  }

  public void visitBooleanType(BooleanType type)
  {
    typeString = typeMap.get(type);
    if (typeString != null)
      return;

    StringBuffer buf = new StringBuffer("bool:t");
    typeString = addType((Type) type);
    buf.append(typeString);
    buf.append("=bub1;0;8");
    addStabs(buf.toString(), N_LSYM, 0, 0, 0);
  }

  public void visitCharacterType(CharacterType type)
  {
    typeString = typeMap.get(type);
    if (typeString != null)
      return;

    throw new scale.common.NotImplementedError("CharacterType");
  }

  public void visitComplexType(ComplexType ct)
  {
    typeString = typeMap.get(ct);
    if (typeString != null)
      return;

    int bs = ct.getRealMinbitSize();

    typeString = addType(ct);

    String       cts = isFortran ? ct.mapTypeToF77String() : ct.mapTypeToCString();
    StringBuffer buf = new StringBuffer(cts);
    buf.append(':');
    buf.append("t");
    buf.append(typeString);
    buf.append("=R");
    buf.append((bs == 32) ? '3' : ((bs == 64) ? '4' : '5'));
    buf.append(";");
    buf.append(bs / 8);

    addStabs(buf.toString(), N_LSYM, 0, 0, 0);
  }

  public void visitCompositeType(CompositeType type)
  {
    throw new scale.common.InternalError("CompositeType");
  }

  public void visitEnumerationType(EnumerationType et)
  {
    typeString = typeMap.get(et);
    if (typeString != null)
      return;

    int             l   = et.getNumEnums();
    EnumElementDecl ed  = et.getEnum(0);
    String          ts  = addType(et);
    StringBuffer    buf = new StringBuffer(ts);

    buf.append("=e");

    for (int i = 0; i < l; i++) {
      EnumElementDecl rt = et.getEnum(i);
      buf.append(rt.getName());
      buf.append(':');
      Expression exp = rt.getValue().getConstantValue();
      assert (exp instanceof Literal) : "Not an integer - " + exp;
      buf.append(((Literal) exp).getGenericValue());
      buf.append(',');
    }

    buf.append(';');

    typeString = buf.toString();
  }

  private String processArrayDimensions(ArrayType at)
  {
    int          l   = at.getRank();
    StringBuffer buf = new StringBuffer("");

    for (int i = 0; i < l; i++) {
      if (i > 0)
        buf.append(";a");
      Bound bd = at.getIndex(i);
      buf.append(processBound(bd));
    }

    return buf.toString();
  }

  public void visitAllocArrayType(AllocArrayType at)
  {
    at.getStruct().visit(this);
  }

  public void visitFixedArrayType(FixedArrayType at)
  {
    typeString = typeMap.get(type);
    if (typeString != null)
      return;

    Type   et = at.getElementType();
    String ts = addType(at);

    et.visit(this);

    String es = typeString;
    String is = processArrayDimensions(at);

    StringBuffer buf = new StringBuffer(ts);
    buf.append("=a");
    buf.append(is);
    buf.append(';');
    buf.append(es);
    typeString = buf.toString();
  }

  public void visitFloatType(FloatType ft)
  {
    typeString = typeMap.get(ft);
    if (typeString != null)
      return;

    int bs = ft.bitSize();

    typeString = addType(ft);

    String       fts = isFortran ? ft.mapTypeToF77String() : ft.mapTypeToCString();
    StringBuffer buf = new StringBuffer(fts);
    buf.append(':');
    buf.append("t");
    buf.append(typeString);
    buf.append("=R");
    buf.append((bs == 32) ? '1' : ((bs == 64) ? '2' : '6'));
    buf.append(";");
    buf.append(bs / 8);

    addStabs(buf.toString(), N_LSYM, 0, 0, 0);
  }

  public void visitIncompleteType(IncompleteType type)
  {
    type.getCompleteType().visit(this);
  }

  public void visitIntegerType(IntegerType it)
  {
    typeString = typeMap.get(it);
    if (typeString != null)
      return;

    int bs = it.bitSize();

    typeString = addType(it);

    String       its = isFortran ? it.mapTypeToF77String() : it.mapTypeToCString();
    StringBuffer buf = new StringBuffer(its);
    buf.append(":t");
    buf.append(typeString);
    buf.append("=b");
    buf.append(it.isSigned() ? 's' : 'u');

    if (bs == 8)
      buf.append('c');

    buf.append(bs / 8);
    buf.append(";0;");
    buf.append(bs);

    addStabs(buf.toString(), N_LSYM, 0, 0, 0);
  }

  public void visitSignedIntegerType(SignedIntegerType it)
  {
    visitIntegerType(it);
  }

  public void visitUnsignedIntegerType(UnsignedIntegerType it)
  {
    visitIntegerType(it);
  }

  public void visitFortranCharType(FortranCharType it)
  {
    typeString = typeMap.get(type);
    if (typeString != null)
      return;

    String ts = addType(it);
    String es = "bsc1";

    StringBuffer buf = new StringBuffer(ts);
    buf.append("=a");
    buf.append(it.getLength());
    buf.append(';');
    buf.append(es);
    typeString = buf.toString();
  }

  public void visitNumericType(NumericType type)
  {
    throw new scale.common.InternalError("NumericType");
  }

  public void visitPointerType(PointerType pt)
  {
    typeString = typeMap.get(pt);
    if (typeString != null)
      return;

    String ts = addType(pt);
    pt.getPointedTo().visit(this);
    String ps = typeString;

    StringBuffer buf = new StringBuffer(ts);
    buf.append("=*");
    buf.append(ps);
    typeString = buf.toString();
  }

  private String processFormals(ProcedureType pt, boolean addSemi)
  {
    StringBuffer buf = new StringBuffer("");
    int          l = pt.numFormals();
    for (int i = 0; i < l; i++) {
      if (addSemi && (i > 0))
        buf.append(';');

      FormalDecl fd = pt.getFormal(i);
      fd.getType().visit(this);
      buf.append(typeString);
    }
    return buf.toString();
  }

  public void visitProcedureType(ProcedureType pt)
  {
    typeString = typeMap.get(pt);
    if (typeString != null)
      return;

    String ts = addType(pt);

    pt.getReturnType().visit(this);
    String rs = typeString;
    String fs = processFormals(pt, false);

    StringBuffer buf = new StringBuffer(ts);
    buf.append(pt.isOldStyle() ? "=f" : "=g");
    buf.append(rs);
    buf.append(fs);

    if (!pt.isOldStyle())
      buf.append('#');

    typeString = buf.toString();
  }

  public void visitRaise(Raise type)
  {
    throw new scale.common.InternalError("Raise");
  }

  public void visitRaiseWithObject(RaiseWithObject type)
  {
    throw new scale.common.InternalError("RaiseWithObject");
  }

  public void visitRaiseWithType(RaiseWithType type)
  {
    throw new scale.common.InternalError("RaiseWithType");
  }

  public void visitBound(Bound bd)
  {
    typeString = processBound(bd);
  }

  private String processBound(Bound bd)
  {
    machine.getIntegerCalcType().visit(this);

    StringBuffer buf = new StringBuffer("r");
    buf.append(';');
    long min = 0;
    try {
      min = bd.getConstMin();
    } catch (java.lang.Throwable ex) {
    }
    long max = 0;
    try {
      max = bd.getConstMax();
    } catch (java.lang.Throwable ex) {
    }
    buf.append(min);
    buf.append(';');
    buf.append(max);
    return buf.toString();
  }

  public void visitRealType(RealType type)
  {
    throw new scale.common.InternalError("RealType");
  }

  private String processFields(AggregateType at)
  {
    int          l   = at.numFields();
    StringBuffer buf = new StringBuffer("");

    for (int i = 0; i < l; i++) {
      FieldDecl fd   = at.getField(i);
      Type      ft   = fd.getType();
      int       bits = fd.getBits();
      buf.append(fd.getName());
      buf.append(':');
      ft.visit(this);
      buf.append(typeString);
      buf.append(',');
      if (bits == 0) {
        buf.append(fd.getFieldOffset() * 8);
        buf.append(',');
        buf.append(ft.memorySize(machine) * 8);
      } else {
        buf.append(fd.getBitOffset());
        buf.append(',');
        buf.append(bits);
      }
      buf.append(';');
    }

    buf.append(';');
    return buf.toString();
  }

  public void visitRecordType(RecordType rt)
  {
    typeString = typeMap.get(rt);
    if (typeString != null)
      return;

    String ts = addType(rt);
    String fs = processFields(rt);

    StringBuffer buf = new StringBuffer(ts);
    buf.append("=s");
    buf.append(rt.memorySize(machine));
    buf.append(fs);
    typeString = buf.toString();
  }

  public void visitRefType(RefType rt)
  {
    typeString = typeMap.get(rt);
    if (typeString != null)
      return;

    Type to = rt.getRefTo();

    RefAttr attribute = rt.getAttribute();
    if (attribute == RefAttr.Const) {
      String ts  = addType(rt);

      to.visit(this);
      String tos = typeString;

      StringBuffer buf = new StringBuffer(ts);
      buf.append("=k");
      buf.append(tos);
      typeString = buf.toString();
      return;
    }

    if (attribute == RefAttr.Volatile) {
      String ts  = addType(rt);

      to.visit(this);
      String tos = typeString;

      StringBuffer buf = new StringBuffer(ts);
      buf.append("=B");
      buf.append(tos);
      typeString = buf.toString();
      return;
    }

    if (gcc && (attribute == RefAttr.Aligned)) {
      String ts  = addType(rt);

      to.visit(this);
      String tos = typeString;

      StringBuffer buf = new StringBuffer(ts);
      buf.append("=@a");
      buf.append(rt.alignment(machine));
      buf.append(';');
      buf.append(tos);
      typeString = buf.toString();
      return;
    }

    Declaration decl = rt.getDecl();
    if (decl instanceof TypeName) {
      genTypeName((TypeName) decl);
      typeString = typeMap.get(decl.getType());
      return;
    }

    to.visit(this);
  }

  public void visitType(Type type)
  {
    throw new scale.common.InternalError("Type");
  }

  public void visitUnionType(UnionType ut)
  {
    typeString = typeMap.get(ut);
    if (typeString != null)
      return;

    String       ts  = addType(ut);
    String       fs  = processFields(ut);
    StringBuffer buf = new StringBuffer(ts);

    buf.append("=u");
    buf.append(ut.memorySize(machine));
    buf.append(fs);
    typeString = buf.toString();
  }

  public void visitVoidType(VoidType vt)
  {
    typeString = typeMap.get(vt);
    if (typeString != null)
      return;

    typeString = addType(vt);

    StringBuffer buf = new StringBuffer("void:t");
    buf.append(typeString);
    buf.append("=bs0;0;0");

    addStabs(buf.toString(), N_LSYM, 0, 0, 0);
  }
}
