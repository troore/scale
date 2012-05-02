package scale.backend.trips2;

import java.util.Iterator;

import scale.common.*;

import scale.backend.Instruction;
import scale.backend.Domination;
import scale.backend.Node;

/**
 * This is the Peephole optimizer for TRIPS. 
 * <p>
 * $Id: Peepholer.java,v 1.51 2007-10-04 19:57:59 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p> 
 */

public class Peepholer
{
  private static int addiLoadStoreCount = 0; // A count of the number of times the addiLoadStore peephole optimization was used.
  private static int movMovCount        = 0; // A count of the number of times the movMov peephole optimization was used.
  private static int enter0Count        = 0; // A count of the number of times the movi0 peephole optimization was used.
  private static int extExtCount        = 0; // A count of the number of times the extExt peephole optimization was used.
  private static int addiAddiCount      = 0; // A count of the number of times the addiAddi peephole optimization was used.
  private static int readWriteCount     = 0; // A count of the number of times the readWrite peephole optimization was used.
  private static int immediate0or1Count = 0; // A count of the number of times the immediate peephole optimzation was used.
  private static int extStoreCount      = 0; // A count of the number of times the extStore peephole optimzation was used.
  private static int testTnei0Count     = 0; // A count of the number of times the testTnei0 peephole optimization was used.
  private static int phiCount           = 0; // A count of the number of times the phi peephole optimization was used.
  
  private static final String[] stats = {
    "peepAddiLoadStore", "peepMovMov", "peepEnter0", "peepExtExt",
    "peepAddiAddi", "peepReadWrite", "peepImmediate0or1", "peepExtStore",
    "peepTestTnei0" };

  static 
  {
    Statistics.register("scale.backend.trips2.Peepholer", stats);
  }

  /**
   * Return the number of times the addiLoadStore pattern was used.
   */
  public static int peepAddiLoadStore()
  {
    return addiLoadStoreCount;
  }

  /**
   * Return the number of times the movMov pattern was used.
   */
  public static int peepMovMov()
  {
    return movMovCount;
  }

  /**
   * Return the number of times the mov0 pattern was used.
   */
  public static int peepEnter0()
  {
    return enter0Count;
  }
  
  /**
   * Return the number of times the extExt pattern was used.
   */
  public static int peepExtExt()
  {
    return extExtCount;
  }
  
  /**
   * Return the number of times the addiAddi pattern was used.
   */
  public static int peepAddiAddi()
  {
    return addiAddiCount;
  }
  
  /**
   * Return the number of times the readWrite pattern was used.
   */
  public static int peepReadWrite()
  {
    return readWriteCount;
  }
  
  /**
   * Return the number of times an instruction with an immediate of 0
   * or 1 was removed.
   */
  public static int peepImmediate0or1()
  {
    return immediate0or1Count;
  }
  
  /**
   * Return the number of times the extStore pattern was used.
   */
  public static int peepExtStore()
  {
    return extStoreCount;
  }

  /**
   * Return the number of times the testTnei0 pattern was used.
   */
  public static int peepTestTest()
  {
    return testTnei0Count;
  }
  
  private IntMap<Instruction>         useDef; // The use/def map for the current hyperblock.
  private IntMap<Vector<Instruction>> defUse; // The def/use map for the current hyperblock.

  private Hyperblock hbStart;    // The hyperblock to start from.
  private SSA        ssa;        // The SSA instance for the current hyperblock.
  private Hyperblock chb;        // The PFG instance for the current hyperblock.
  private boolean    afterAlloc; // True if called before register allocation.
  private boolean    changed;    // If a peephole pattern was applied.
 
  /** 
   * The constructor.
   */
  public Peepholer(Hyperblock hbStart, boolean afterAlloc)
  {
    this.hbStart    = hbStart;
    this.ssa        = null; 
    this.changed    = false;
    this.useDef     = null;
    this.defUse     = null;
    this.afterAlloc = afterAlloc;
  }
  
  /**
   * Run the peepholer.
   */
  protected void peephole()
  {
    if (Hyperblock.intraBlockDefault == Hyperblock.PREDICATE_TOP)
      return;  
    
    Stack<Node> wl = WorkArea.<Node>getStack("peephole");
   
    hbStart.nextVisit();
    hbStart.setVisited();
    wl.push(hbStart);
    
    while (!wl.isEmpty()) {
      Hyperblock hb = (Hyperblock) wl.pop(); 
      hb.pushOutEdges(wl);
      peephole(hb);
    }
   
    WorkArea.<Node>returnStack(wl);
  }
  
  /**
   * Peephole a hyperblock.
   */
  public final void peephole(Hyperblock hb)
  {
    ssa     = hb.getSSA();
    chb     = hb;
    useDef  = ssa.getUseDef();
    defUse  = ssa.getDefUse();
    changed = false;
   
    // TODO: should this run until no more patterns are applied
    
    peepholeBlock(chb.getFirstBlock());
    
    if (changed)
      ssa.removeDeadCode();
  }
  
  /**
   * Apply a peephole pattern.
   */
  private boolean applyPattern(Instruction pred, Instruction inst)
  {
    boolean remove  = false;
    int     opcode  = inst.getOpcode();
    int     pattern = (pred.getOpcode() << 8) + opcode;

    switch (pattern) {
    // ADDI + ADDI
      case (Opcodes.ADDI << 8) + Opcodes.ADDI:
        remove = addiAddiPattern(pred, inst);
        break;
        
    // ADDI + load/store
      case (Opcodes.ADDI << 8) + Opcodes.LB:
      case (Opcodes.ADDI << 8) + Opcodes.LH:
      case (Opcodes.ADDI << 8) + Opcodes.LW:
      case (Opcodes.ADDI << 8) + Opcodes.LD:
      case (Opcodes.ADDI << 8) + Opcodes.LBS:
      case (Opcodes.ADDI << 8) + Opcodes.LHS:
      case (Opcodes.ADDI << 8) + Opcodes.LWS:
      case (Opcodes.ADDI << 8) + Opcodes.SB:
      case (Opcodes.ADDI << 8) + Opcodes.SH:
      case (Opcodes.ADDI << 8) + Opcodes.SW:
      case (Opcodes.ADDI << 8) + Opcodes.SD:
        remove = addiLoadStorePattern((ImmediateInstruction) pred, inst);
        break;

    // ENTER0 + conversion  
      case (Opcodes._ENTER << 8) + Opcodes.FITOD:
      case (Opcodes._ENTER << 8) + Opcodes.FDTOI:
      case (Opcodes._ENTER << 8) + Opcodes.FSTOD:
      case (Opcodes._ENTER << 8) + Opcodes.FDTOS:
      case (Opcodes._ENTER << 8) + Opcodes.EXTSB:
      case (Opcodes._ENTER << 8) + Opcodes.EXTSH:
      case (Opcodes._ENTER << 8) + Opcodes.EXTSW:
      case (Opcodes._ENTER << 8) + Opcodes.EXTUB:
      case (Opcodes._ENTER << 8) + Opcodes.EXTUH:
      case (Opcodes._ENTER << 8) + Opcodes.EXTUW:
        remove = enter0Pattern(pred, inst);
        break;
      
    // EXTxy + EXTxy (matching: signed-signed/unsigned-unsigned)
      case (Opcodes.EXTSW << 8) + Opcodes.EXTSW:
      case (Opcodes.EXTSH << 8) + Opcodes.EXTSH:
      case (Opcodes.EXTSH << 8) + Opcodes.EXTSW:
      case (Opcodes.EXTSB << 8) + Opcodes.EXTSB:
      case (Opcodes.EXTSB << 8) + Opcodes.EXTSH:
      case (Opcodes.EXTSB << 8) + Opcodes.EXTSW:
      case (Opcodes.EXTUW << 8) + Opcodes.EXTUW:
      case (Opcodes.EXTUH << 8) + Opcodes.EXTUH:
      case (Opcodes.EXTUH << 8) + Opcodes.EXTUW:
      case (Opcodes.EXTUB << 8) + Opcodes.EXTUB:
      case (Opcodes.EXTUB << 8) + Opcodes.EXTUH:
      case (Opcodes.EXTUB << 8) + Opcodes.EXTUW:
        remove = extExt1Pattern(pred, inst);
        break;
       
    // EXTxy + EXTzy (not matching: signed-unsigned/unsigned-signed)
      case (Opcodes.EXTUW << 8) + Opcodes.EXTSW:
      case (Opcodes.EXTUW << 8) + Opcodes.EXTSH:
      case (Opcodes.EXTUW << 8) + Opcodes.EXTSB:
      case (Opcodes.EXTUH << 8) + Opcodes.EXTSH:
      case (Opcodes.EXTUH << 8) + Opcodes.EXTSB:
      case (Opcodes.EXTUB << 8) + Opcodes.EXTSB:
      case (Opcodes.EXTSW << 8) + Opcodes.EXTUW:
      case (Opcodes.EXTSW << 8) + Opcodes.EXTUH:
      case (Opcodes.EXTSW << 8) + Opcodes.EXTUB:
      case (Opcodes.EXTSH << 8) + Opcodes.EXTUH:
      case (Opcodes.EXTSH << 8) + Opcodes.EXTUB:
      case (Opcodes.EXTSB << 8) + Opcodes.EXTUB:
    // successor has a smaller type than the predecessor
      case (Opcodes.EXTUW << 8) + Opcodes.EXTUH:
      case (Opcodes.EXTUW << 8) + Opcodes.EXTUB:
      case (Opcodes.EXTUH << 8) + Opcodes.EXTUB:
        remove = extExt2Pattern(pred, inst);
        break;
           
    // MOV + MOV
      case (Opcodes.MOV << 8) + Opcodes.MOV:
        remove = movMovPattern(pred, inst);
        break; 
     
    // EXTxy + Sy
      case (Opcodes.EXTUW << 8) + Opcodes.SW:
      case (Opcodes.EXTUW << 8) + Opcodes.SH:
      case (Opcodes.EXTUW << 8) + Opcodes.SB:
      case (Opcodes.EXTSW << 8) + Opcodes.SW:
      case (Opcodes.EXTSW << 8) + Opcodes.SH:
      case (Opcodes.EXTSW << 8) + Opcodes.SB:
      case (Opcodes.EXTUH << 8) + Opcodes.SH:
      case (Opcodes.EXTUH << 8) + Opcodes.SB:
      case (Opcodes.EXTSH << 8) + Opcodes.SH:
      case (Opcodes.EXTSH << 8) + Opcodes.SB:
      case (Opcodes.EXTUB << 8) + Opcodes.SB:
      case (Opcodes.EXTSB << 8) + Opcodes.SB:
        remove = extStorePattern(pred, inst);
        break;
     
    // TEST + TNEI
      case (Opcodes.TLT  << 8) + Opcodes.TNEI:
      case (Opcodes.TLTU << 8) + Opcodes.TNEI:
      case (Opcodes.TLTI << 8) + Opcodes.TNEI:
      case (Opcodes.TLTUI<< 8) + Opcodes.TNEI:

      case (Opcodes.TLE  << 8) + Opcodes.TNEI:
      case (Opcodes.TLEU << 8) + Opcodes.TNEI:
      case (Opcodes.TLEI << 8) + Opcodes.TNEI:
      case (Opcodes.TLEUI<< 8) + Opcodes.TNEI:

      case (Opcodes.TGT  << 8) + Opcodes.TNEI:
      case (Opcodes.TGTU << 8) + Opcodes.TNEI:
      case (Opcodes.TGTI << 8) + Opcodes.TNEI:
      case (Opcodes.TGTUI<< 8) + Opcodes.TNEI:

      case (Opcodes.TGE  << 8) + Opcodes.TNEI:
      case (Opcodes.TGEU << 8) + Opcodes.TNEI:
      case (Opcodes.TGEI << 8) + Opcodes.TNEI:
      case (Opcodes.TGEUI<< 8) + Opcodes.TNEI:

      case (Opcodes.TNE  << 8) + Opcodes.TNEI:
      case (Opcodes.TNEI << 8) + Opcodes.TNEI:

      case (Opcodes.TEQ  << 8) + Opcodes.TNEI:
      case (Opcodes.TEQI << 8) + Opcodes.TNEI:

      case (Opcodes.FLT  << 8) + Opcodes.TNEI:
      case (Opcodes.FLE  << 8) + Opcodes.TNEI:
      case (Opcodes.FGT  << 8) + Opcodes.TNEI:
      case (Opcodes.FGE  << 8) + Opcodes.TNEI:
      case (Opcodes.FNE  << 8) + Opcodes.TNEI:
      case (Opcodes.FEQ  << 8) + Opcodes.TNEI:
        remove = testTnei0Pattern(pred, (ImmediateInstruction) inst);
        break;
        
    // READ + WRITE     
      case (Opcodes.READ << 8) + Opcodes.WRITE:
        remove = readWritePattern(pred, inst);
        break;
      
      default:
        if (Opcodes.getFormat(opcode) == Opcodes.I1)
          remove = immediatePattern((ImmediateInstruction) inst);
        else if (opcode == Opcodes._PHI)
          remove = phiPattern((PhiInstruction) inst);
        break;
    }  
    
    return remove;
  }
  
  /**
   * Peephole a block.
   */
  private void peepholeBlock(PredicateBlock block)
  {
    Instruction prev = null;
    for (Instruction inst = block.getFirstInstruction(); inst != null; inst = inst.getNext()) {
      boolean remove = false;
      int[]   srcs   = inst.getSrcRegisters();
      
      if (srcs != null) {
        for (int i = 0; i < srcs.length; i++) {
          int         reg  = srcs[i];          
          Instruction pred = useDef.get(reg);
          if (pred == null)
            continue;
          if (((TripsInstruction) pred).definesPredicate())
            continue;
      
          remove = applyPattern(pred, inst);
          if (remove)
            break;
        }
      }
      
      if (remove)
        block.removeInstruction(prev, inst);
      else
        prev = inst;
    }

    // Recurse on each child in the dominator tree.
    
    Domination dom = chb.getDomination();
    Node[]     dn  = dom.getDominatees(block);
    for (Node pb : dn)
      peepholeBlock((PredicateBlock) pb);
  }
  
  /**
   * If the operands are all the same the phi can be removed.
   */
  private boolean phiPattern(PhiInstruction phi)
  {
    int[]   operands = phi.getOperands();
    int     ol       = operands.length;
    int     reg      = operands[0];
  
    for (int i = 1; i < ol; i++) {
      if (reg != operands[i])
        return false;
    }
    
    int                 ra    = phi.getRa();
    Vector<Instruction> uses  = defUse.get(ra);
    int[]               preds = phi.getPredicates(); 
    
    for (Iterator<Instruction> iter = uses.iterator(); iter.hasNext(); ) {
      Instruction use = iter.next();
      use.remapSrcRegister(ra, reg);
      iter.remove();
      ssa.addUse(use, reg);
    }
    
    if (preds != null) {
      for (int i = 0; i < preds.length; i++)
        ssa.removeUse(phi, preds[i]);
    }
      
    ssa.removeUse(phi, reg);
    ssa.setDef(null, ra);
      
    phiCount++;
    changed = true;
    
    return true;
  }
  
  /**
   * This pattern arises a lot as an artifact of code generation.
   * <br>
   * TEST       rx, ..., ...     TEST       ry, ..., ...
   * TNEI       ry, rx, #0   ->  NOP
   * INST_b<ry>                  INST_b<ry>
   * <br>
   * This patern must preserve the predicate flow graph.  If we were to rename
   * all the instructions which used ry, to rx, we would also have to change
   * all the predicate blocks to be predicated on rx.  
   */
  private boolean testTnei0Pattern(Instruction pred, ImmediateInstruction inst)
  { 
    long imm = inst.getImm();
    if (imm != 0)
      return false;

    if (pred.isPredicated() || inst.isPredicated()) {
      if (pred.numPredicates() != inst.numPredicates())
        return false;
      if (pred.isPredicatedOnTrue() != inst.isPredicatedOnTrue())
        return false;
      if (pred.getPredicate(0) != inst.getPredicate(0)) 
        return false;
    } 
    
    int                 ry   = inst.getRa();    
    int                 rx   = inst.getRb();
    Vector<Instruction> uses = defUse.get(rx);
    if (uses.size() > 1)  // There is a use of the TEST which is not the TNEI.
      return false;
    
    pred.remapDestRegister(rx, ry);
    
    if (inst.definesPredicate())
      ((TripsInstruction) pred).setDefinesPredicate();
    
    ssa.clearDefUse(inst);
    ssa.setDef(null, rx);
    ssa.setDef(pred, ry);
    
    testTnei0Count++;
    changed = true;
    
    return true;
  } 
  
  /**
   * ADDI/SUBI/SLLI    Ra, Rb, 0  ->  nop
   * MULI/DIVUI/DIVSI  Ra, Rb, 1  ->  nop
   */
  private boolean immediatePattern(ImmediateInstruction inst)
  {
    if (inst.getDisp() != null)
      return false;
    
    long imm = inst.getImm();
    int  op  = inst.getOpcode();
    
    if (((imm == 0) &&
         ((op == Opcodes.ADDI) || (op == Opcodes.SUBI) || (op == Opcodes.SLLI))) || 
        ((imm == 1) &&
         ((op == Opcodes.MULI) || (op == Opcodes.DIVUI) || (op == Opcodes.DIVSI))) ) {

      int                 ra    = inst.getRa();
      int                 rb    = inst.getRb();
      int[]               preds = inst.getPredicates();  
      Vector<Instruction> uses  = defUse.get(ra);
        
      for (Iterator<Instruction> iter = uses.iterator(); iter.hasNext(); ) {
        Instruction use = iter.next();
        use.remapSrcRegister(ra, rb);
        iter.remove();  // Remove the use of ra.
        ssa.addUse(use, rb);
      }
        
      if (preds != null) {
        for (int i = 0; i < preds.length; i++)
          ssa.removeUse(inst, preds[i]);
      }
        
      ssa.removeUse(inst, rb);
      ssa.setDef(null, ra);
        
      immediate0or1Count++;
      changed = true;
        
      return true;
    }
    
    return false;
  }
  
  /**
   * ADDI  Ra, Rb, Imm       ADDI  Ra, Rb, Imm
   * SD    (Ra), Rc     ->   SD    Imm(Rb), Rc
   */
  private boolean addiLoadStorePattern(ImmediateInstruction pred, Instruction inst)
  {
    long lsImm   = 0;
    int  lsSrc   = -1;
    
    if (pred.getDisp() != null)
      return false;
    
    if (inst.isLoad()) {
      if (((LoadInstruction) inst).getDisp() != null)
        return false;
      lsImm = ((LoadInstruction) inst).getImm();
      lsSrc = ((LoadInstruction) inst).getRb();
    } else if (inst.isStore()) {
      if (((StoreInstruction) inst).getDisp() != null)
        return false;
      lsImm = ((StoreInstruction) inst).getImm();
      lsSrc = ((StoreInstruction) inst).getRb();
    }
    
    if (pred.getRa() != lsSrc)
      return false;
    
    long imm = pred.getImm() + lsImm;  
    if (!Trips2Machine.isImmediate(imm))
      return false;
    
    // If SD is the only use of ADDI, then the fanout for
    // ADDI.Rb stays the same. Otherwise, we have to check
    // the number of targets for ADDI.Rb.
    
    Vector<Instruction> usesRa = defUse.get(pred.getRa());
    int                 addiSrc = pred.getRb();
    
    if (usesRa.size() > 1) {
      Instruction         def    = useDef.get(addiSrc);
      Vector<Instruction> usesRb = defUse.get(addiSrc);
      if (usesRb.size() >= Opcodes.getNumTargets(def))
        return false;
    }
    
    if (inst.isLoad())
      ((LoadInstruction) inst).setImm(imm);
    else if (inst.isStore())
      ((StoreInstruction) inst).setImm(imm);

    inst.remapSrcRegister(lsSrc, addiSrc);
    ssa.removeUse(inst, lsSrc);
    ssa.addUse(inst, addiSrc);
    
    addiLoadStoreCount++;
    changed = true;
    
    return false;
  }

 /**
   * ENTER Rb, 0       ENTER Ra, 0
   * INST  Ra, Rb  ->  NOP
   */
  private boolean enter0Pattern(Instruction pred, Instruction inst)
  {
    EnterInstruction   ei = (EnterInstruction) pred;
    GeneralInstruction gi = (GeneralInstruction) inst;
    
    if (!(ei.disp.isNumeric() && ei.disp.isZero()))
      return false;

    int    ra   = ei.getDestRegister();
    int    rb   = gi.getDestRegister();
    Vector<Instruction> uses = defUse.get(rb);
    
    for (Iterator<Instruction> iter = uses.iterator(); iter.hasNext(); ) {
      Instruction use = iter.next();
      use.remapSrcRegister(rb, ra);
      iter.remove();  // Remove the use of rb.
      ssa.addUse(use, ra);
    }
    
    enter0Count++;
    changed = true;
    
    return true;
  }
  
  /**
   * Matching extensions (signed-signed/unsigned-unsigned)
   * <br>
   * EXTxx Ra, Rb       EXTxx Ra, Rb
   * EXTxx Rc, Ra  ->   EXTxx Rc, Rb
   *                    Rename all uses of Rc to Ra.
   */
  private boolean extExt1Pattern(Instruction pred, Instruction inst)
  {
    int ra = pred.getDestRegister();
    int rb = ((GeneralInstruction) pred).getRb();
    int rc = inst.getDestRegister();
    
    inst.remapSrcRegister(ra, rb);
    ssa.addUse(inst, rb);
    ssa.removeUse(inst, ra);
 
    Vector<Instruction> uses = defUse.get(rc);
    
    for (Iterator<Instruction> iter = uses.iterator(); iter.hasNext(); ) {
      Instruction use = iter.next();
      use.remapSrcRegister(rc, ra);
      iter.remove();  // Removes the use of rc.
      ssa.addUse(use, ra);
    }

    extExtCount++;
    changed = true;
    
    return false;
  }
  
   /**
   * Non-matching extensions (signed-unsigned/unsigned-signed)
   * <br>
   * EXTxx Ra, Rb       EXTxx Ra, Rb
   * EXTxx Rc, Ra  ->   EXTxx Rc, Rb
   * <br>
   * This may increase the fanout of Rb.
   * <br>
   * This does not remove an instruction but it reduces tree height
   * and may allow instructions to become dead code.
   */
  private boolean extExt2Pattern(Instruction pred, Instruction inst)
  {
    int ra = pred.getDestRegister();
    int rb = ((GeneralInstruction) pred).getRb();
 
    Instruction         def  = useDef.get(rb);
    Vector<Instruction> uses = defUse.get(rb);
    if (uses.size() >= Opcodes.getNumTargets(def))
      return false;
      
    inst.remapSrcRegister(ra, rb);
    ssa.addUse(inst, rb);
    ssa.removeUse(inst, ra);
 
    extExtCount++;
    changed = true;
    
    return false;
  }
  
  /**
   * MOV Ra, Rb      MOV Ra, Rb
   * MOV Rc, Ra  ->  MOV Rc, Rb
   */
  private boolean movMovPattern(Instruction pred, Instruction inst)
  {
    if (!afterAlloc)
	    return false;
	
    int rb = ((GeneralInstruction) pred).getRb();
    int ra = ((GeneralInstruction) inst).getRb();
    
    inst.remapSrcRegister(ra, rb);
    ssa.removeUse(inst, ra);
    ssa.addUse(inst, rb);
    
    movMovCount++;
    changed = true;
    
    return false;
  }
  
  /**
   * EXTxy Rp,  Rq      EXTxy Rp,  Rq
   * Sy   (Rr), Rp  ->  Sy   (Rr), Rq
   */
  private boolean extStorePattern(Instruction pred, Instruction inst)
  {
    int extRq = ((GeneralInstruction) pred).getRb();
    int  stRp = ((  StoreInstruction) inst).getRc();

    inst.remapSrcRegister(stRp, extRq);
    ssa.removeUse(inst, stRp);
    ssa.addUse(inst, extRq);

    extStoreCount++;
    changed = true;
 
    return false;
  }
  
  /**
   * READ  Ra, Rb      READ Ra, Rb
   * WRITE Rc, Ra  ->  NOP
   */
  private boolean readWritePattern(Instruction pred, Instruction inst)
  {
    int rb = ((GeneralInstruction) pred).getRb();
    int ra = ((GeneralInstruction) pred).getRa();
    int rc = ((GeneralInstruction) inst).getRa();

    if (rb != rc)
      return false;
    
    ssa.removeUse(inst, ra);
    ssa.setDef(null, rc);
    
    readWriteCount++;
    changed = true;
    
    return true;
  }
  
  /**
   * ADDI Ra, Rb, Imm1      ADDI Ra, Rb, Imm1
   * ADDI Rc, Ra, Imm2  ->  ADDI Rc, Rb, Imm1 + Imm2
   * <br>
   * If Imm1 + Imm2 = 0, all the uses of Rc can be renamed to use Rb.
   */
  private boolean addiAddiPattern(Instruction pred, Instruction inst) 
  {
    ImmediateInstruction pi   = (ImmediateInstruction) pred;
    ImmediateInstruction ii   = (ImmediateInstruction) inst;
    int                  rb   = pi.getRb();
    long                 imm1 = pi.getImm();
    long                 imm2 = ii.getImm();
    long                 imm3 = imm1 + imm2;
    
    if (!Trips2Machine.isImmediate(imm3))
      return false;
    
    if ((pi.getDisp() != null) && (ii.getDisp() == null) && (imm2 == 0))
      return immediatePattern((ImmediateInstruction) inst);
    
    if ((pi.getDisp() != null) || (ii.getDisp() != null))
      return false;

    if (imm3 == 0) {  // If the combined immediate is 0, rename the uses.
      int                 ra   = ii.getRa();
      Vector<Instruction> uses = defUse.get(ra);
      int                 ul   = uses.size();
      
      for (int i = 0; i < ul; i++) {
        Instruction use = uses.remove(0);   // Remove the use
        use.remapSrcRegister(ra, rb);
        ssa.addUse(use, rb);
      }
    } else {
      int                 rc = ii.getRb();
      int                 ra = pi.getRa();
      Vector<Instruction> uses = defUse.get(ra);
      
      // We can only apply this pattern when the only use for
      // pred is inst, because we are going to increase the
      // fanout for rb by one, and we need to remove pred to
      // stay under the block size limit.  
      
      if (uses.size() != 1)  
        return false;
    
      ii.remapSrcRegister(rc, rb);
      ii.setImm(imm3);
      ssa.removeUse(ii, rc);
      ssa.addUse(ii, rb);
    }
    
    addiAddiCount++;
    changed = true;
    
    return false;
  }
}
