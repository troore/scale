package scale.backend.trips2;

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
 * This class generates Trips assembly language from a list of Trips
 * instructions.
 * <p>
 * $Id: Trips2Assembler.java,v 1.44 2007-10-31 23:47:51 bmaher Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public final class Trips2Assembler extends scale.backend.Assembler
{
  public static boolean useConsecutiveLabels = true;

  private Machine     machine;
  private RegisterSet registers;

  /**
   * @param gen is the instruction generator used to generate the
   * instructions.
   * @param source specifies the source program
   */
  public Trips2Assembler(Generator gen, String source)
  {
    super(gen, source);
    this.machine   = gen.getMachine();
    this.registers = gen.getRegisterSet();
    repsAllowedInAL = false;
  }

  /**
   * Generate assembly code for the data areas.
   */
  public void assemble(Emit emit, SpaceAllocation[] dataAreas)
  {
    assembleProlog(emit);

    int ma = gen.getMaxAreaIndex();

    // Relabel labels in consecutive order starting at 0 for each
    // function.

    if (useConsecutiveLabels) {
      int na  = dataAreas.length;
      for (int handle = 1; handle < na; handle++) {
        SpaceAllocation sa = dataAreas[handle];

        if (sa == null)
          continue;

        if (sa.getType() != SpaceAllocation.DAT_TEXT)
          continue;

        int labelIndex = 1;
        for (Instruction fi = (Instruction) sa.getValue(); fi != null; fi = fi.getNext()) {
          if (!fi.isLabel())
            continue;
          Label lab = (Label) fi;
          lab.setLabelIndex(labelIndex++);
        }
      }
    }

    for (int area = 0; area <= ma; area++)
      assembleArea(emit, area, dataAreas);

    emit.endLine();
    emit.endLine();
  }
  
  private void assembleArea(Emit emit, int area, SpaceAllocation[] dataAreas)
  {
    boolean flg = true;
    int     na  = dataAreas.length;

    for (int handle = 1; handle < na; handle++) {
      SpaceAllocation sa = dataAreas[handle];

      if (sa == null)
        continue;

      if (sa.getArea() != area)
        continue;

      flg = false;
      break;
    }

    if (flg)
      return;

    assembleDataAreaHeader(emit, area);
    emit.endLine();

    for (int handle = 1; handle < na; handle++) {
      SpaceAllocation sa = dataAreas[handle];

      if (sa == null)
        continue;

      if (sa.getArea() != area)
        continue;

      int dt = sa.getType();

      if (dt == SpaceAllocation.DAT_NONE) {
        assembleBSS(emit, handle, dataAreas);
        continue;
      }

      if (dt == SpaceAllocation.DAT_TEXT) {
        assembleText(emit, handle, dataAreas);
        continue;
      }

      handle = assembleVariable(emit, handle, dataAreas);
    }

    emit.endLine();
  }

  private void assembleBSS(Emit emit, int handle, SpaceAllocation[] dataAreas)
  {
    SpaceAllocation sa = dataAreas[handle];
    String name  = sa.getName();
    int    vis   = sa.getVisibility();
    long   size  = sa.getSize();
    int alignment = sa.getAlignment();

    switch (vis) {
      case SpaceAllocation.DAV_EXTERN:   // Externs
        emit.emit(";\t.extern\t");
        emit.emit(name);
        emit.endLine();
        if (sa.isWeak() && (sa.getValue() instanceof String)) {
          emit.emit("\t.weak\t");
          emit.emit(name);
          emit.endLine();
          emit.emit("\t.equ\t" + name + "=" + sa.getValue());
          emit.endLine();
        }
        break;
      case SpaceAllocation.DAV_GLOBAL:   // Globals
        emit.emit(sa.isWeak() ? "\t.weak " : "\t.global ");
        emit.emit(name);
        emit.endLine();
        // Bugzilla #848 -- we should be using .lcomm for BSS
        //
        // Fri Feb 18 09:15:24 CST 2005:
        // Roll back change to .comm because of link errors in crafty
        // caused by two variables of the same name ("pawn_score")
        // in two separate modules (evaluate.c and option.c) 
        // referring to the same memory address.  With .lcomm
        // linker thinks they are duplicate definitions of the
        // same global variable.  With .lcomm, linker arranges
        // for them to share space in BSS.
        //
        emit.emit("\t.comm\t");
        emit.emit(name);
        emit.emit(", ");
        emit.emit(size);
        emit.emit(", ");
        emit.emit(alignment);
        emit.endLine();
        break;
      case SpaceAllocation.DAV_LOCAL:   // Locals
        emit.emit("\t.lcomm\t");
        emit.emit(name);
        emit.emit(", ");
        emit.emit(size);
        emit.emit(", ");
        emit.emit(alignment); //note: alignment in bytes
        emit.endLine();
    }
  }

  private void assembleText(Emit emit, int handle, SpaceAllocation[] dataAreas)
  {
    SpaceAllocation sa   = dataAreas[handle];
    Instruction     inst = (Instruction) sa.getValue();

    if (inst == null)
      return;
    
    BeginMarker       bm          = (BeginMarker) inst;
    String            routineName = bm.getName();
    Trips2Generator   tgen        = (Trips2Generator) gen;
    Trips2RegisterSet rset        = (Trips2RegisterSet) tgen.getRegisterSet();
    
    inst.assembler(this, emit);       // Output the BeginMarker.
    emit.endLine();
    assembleVars(routineName, emit);  // Output the list of variables in this routine.
    rset.setLowTmpReg(bm.getLowTmpReg()); // The lowest used temporary register in this routine.
   
    inst = inst.getNext();
    
    // The routine entry point.
    
    emit.emit(".bbegin ");
    emit.emit(routineName);
    emit.endLine();

    boolean debug = Debug.debug(1);
    for (Instruction next = null; inst != null; inst = next) {
      next = inst.getNext();
    
      if (inst.isLabel()) {
        Label lab    = (Label) inst;
        int   lastbn = lab.getLabelIndex();
        
        emit.emit(".bend");
        emit.endLine();
        if (next == null)
          emit.emit("\t.set ");
        else
          emit.emit(".bbegin ");
        emit.emit(routineName);
        emit.emit('$');
        emit.emit(lastbn);
        if (next == null) {
          emit.emit("=.-1");
          emit.endLine();
          emit.endLine();
          return;
        }
        emit.endLine();

        continue;
      }

      if (inst.isMarker() &&
          (inst instanceof Trips2LineMarker) &&
          (next != null) && next.isLabel()) {
        Label       lab    = (Label) next;
        int         lastbn = lab.getLabelIndex();
        Instruction nxt    = lab.getNext();

        emit.emit(".bend");
        emit.endLine();
        if (nxt == null)
          emit.emit("\t.set ");
        else
          emit.emit(".bbegin ");
        emit.emit(routineName);
        emit.emit('$');
        emit.emit(lastbn);
        if (nxt == null) {
          emit.emit("=.");
          emit.endLine();
          emit.endLine();
          return;
        }
        emit.endLine();
        
        next = next.getNext();
      }

      if (inst.nullified())
        emit.emit(";\t nop - ");
      else
        emit.emit("\t");
      
      inst.assembler(this, emit);

      int loopNumber = inst.getLoopNumber();
      if (loopNumber > 0)
        emit.emit("\t\t; loop " + loopNumber);
      int bbid = inst.getBBID();
      if (bbid > -1)
        emit.emit("\t\t; BB " + bbid);
      if (debug)
        assembleComment("spill", emit);
      emit.endLine();
    }

    emit.emit(".bend");
    emit.endLine();
    emit.endLine();
  }

  private int assembleVariable(Emit emit, int handle, SpaceAllocation[] dataAreas)
  {
    SpaceAllocation sa    = dataAreas[handle];
    int             first = handle;
    int             last  = handle;
    
    for (int i = first + 1; i < dataAreas.length; i++) {
      SpaceAllocation sac = dataAreas[i];
      if (sac == null)
        continue;
      if (sac.getName() != null)
        break;
      last = i;
    }

    int    vis  = sa.getVisibility();
    String name = sa.getName();
    int    aln  = sa.getAlignment();

    // For now output, align for every variable instead of as needed.

    emit.emit("\t.align\t");
    emit.emit(aln);
    emit.endLine();
    
    // .local is default, .extern are all in BSS
    if (vis == SpaceAllocation.DAV_GLOBAL) {
      emit.emit(sa.isWeak() ? "\t.weak\t" : "\t.global\t");
      emit.emit(name);
      emit.endLine();
    }

    emit.emit(name);
    emit.emit(':');
    emit.endLine();

    for (int i = first; i <= last; i++) {
      SpaceAllocation sac   = dataAreas[i];
      Object          value = sac.getValue();
      int             dtc   = sac.getType();
      int             dt    = sac.getType();
      int             reps  = sac.getReps();
      long            size  = sac.getSize();
      int             aln2  = sac.getAlignment();
      long            as    = genData(emit, dtc, value, reps, aln2 >= dtalign[dt]);

      if (size > as)
        genZeroFill(emit, size - as);
    }

    return last;
  }

  /**
   * Called at the very beginning of generating assembly code.
   * Generate assembler directives needed at the beginning of the
   * assembly language file.
   */
  public void assembleProlog(Emit emit)
  {
    if (gen instanceof TripsPGenerator) {
      Vector<String> passThroughItems = ((TripsPGenerator) gen).getPassThroughItems();
      int            pl               = passThroughItems.size();
      
      for (int i = 0; i < pl; i++) {
        String item = passThroughItems.get(i);
        emit.emit(item);
        emit.endLine();
      }
    } else {
      emit.emit(".app-file \"");
      emit.emit(source);
      emit.emit("\"");
      emit.endLine();
    }
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
    TripsLabel   lab = (TripsLabel) label;
    StringBuffer buf = new StringBuffer(lab.getRoutine().getName());
    
    buf.append('$');
    buf.append(label.getLabelIndex());
    
    return buf.toString();
  }

  /**
   * Generate a label in the assembly output.
   */
  public void assembleLabel(Label label, Emit emit)
  {
    emit.emit(getLabelString(label));
  }

  /**
   * Insert the assembler representation of the comment into the
   * output stream.
   */
  public void assembleComment(String comment, Emit emit)
  {
    emit.emit(";# ");
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
   * Convert a predicate register number into its assembly language
   * form.
   */
  public String assemblePredicateRegister(int reg)
  {
    return ((Trips2RegisterSet) registers).predicateRegisterName(reg);
  }

  /**
   * Generate assembler directives for the start of a data area.
   * @param kind specifies the area kind
   */
  public void assembleDataAreaHeader(Emit emit, int kind)
  {
    // Options still in ALL-CAPS (except BSS) are not currently used.
    switch (kind) {
      case Trips2Generator.BSS:    emit.emit("; BSS ");  break;
      case Trips2Generator.SBSS:   emit.emit("SBSS ");   break;
      case Trips2Generator.DATA:   emit.emit("\t.data\t");  break;
      case Trips2Generator.LIT4:   emit.emit("LIT4 ");   break;
      case Trips2Generator.LIT8:   emit.emit("LIT8 ");   break;
      case Trips2Generator.LITA:   emit.emit("LITA ");   break;
      case Trips2Generator.RCONST: emit.emit("RCONST "); break;
      case Trips2Generator.RDATA:  emit.emit("\t.rdata\t"); break;
      case Trips2Generator.SDATA:  emit.emit("SDATA ");  break;
      case Trips2Generator.TEXT:   emit.emit("\t.text\t");  break;
      default: 
        throw new scale.common.InternalError("What area " + kind);
    }
  }

  private void assembleVars(String func, Emit emit)
  {
    Vector<VariableDecl> v = ((Trips2Generator) gen).getVariables(func);
    if (v == null)
      return;

    int vl = v.size();
    int nr = registers.numRealRegisters();
    for (int i = 0; i < vl; i++) {
      VariableDecl vd  = v.elementAt(i);
      Assigned     loc = vd.getStorageLoc();

      if (loc == Assigned.IN_COMMON)
        continue;
      if (loc == Assigned.IN_MEMORY)
        continue;
      if ((loc == Assigned.IN_REGISTER) && 
          (registers.virtualRegister(vd.getValueRegister()) ||
           (vd.getValueRegister() < 0)))
        continue;

      // Block variable declarations are only for debugging.

      emit.emit(";VARIABLE \"");
      emit.emit(vd.getName());
      emit.emit("\" ");

      Type ty = ((Trips2Generator) gen).processType(vd);

      emit.emit("size:");
      emit.emit(ty.memorySizeAsInt(machine));
      emit.emit(' ');

      switch (loc) {
        case IN_REGISTER:
          int reg = vd.getValueRegister();
          emit.emit(assembleRegister(reg));
          break;
        case ON_STACK:  emit.emit("(stack " + vd.getDisplacement() + ")"); break;
        case IN_COMMON: emit.emit("common"); break;
        case IN_MEMORY: emit.emit("memory"); break;
      }
      emit.endLine();
    }
  }
  
  // NONE     BYTE     SHORT     INT     LONG        FLT        DBL  ADDRESS   TEXT     LDBL
  private static final String[] directives = {
    "???", ".byte", ".short", ".int", ".quad", ".single", ".double", ".quad", "???", ".xxxx"};
  private static final int[]    dtsizes    = {
    0,       1,        2,      4,       8,         4,         8,       8,     0,       0};
  private static final int[]    dtalign    = {
    1,       1,        2,      4,       8,         4,         8,       8,     8,       8};

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

  public long assembleData(Emit emit, SpaceAllocation sa, long location)
  {
    return location;
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
    String label = getLabelString(lab);

    for (int i = 1; i <= reps; i++) {
      emit.emit(directives[dt]);
      emit.emit(' ');
      emit.emit(label);
      emit.endLine();
    }
    return dtsizes[dt] * reps;
  }
}
