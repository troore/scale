package scale.backend.trips2;

import java.util.Iterator;

import scale.common.*;

import scale.backend.Instruction;
import scale.backend.RegisterSet;
import scale.backend.Domination;
import scale.backend.DominanceFrontier;
import scale.backend.Node;

/**
 * This class converts a PFG into the SSA form of the PFG.
 * <p>
 * $Id: SSA.java,v 1.56 2007-10-04 19:57:59 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The algorithm comes from <cite>Practical Improvements to the
 * Construction and Destruction of Static Single Assignment
 * Form</cite> by Briggs, et al, in Software - Practice and
 * Experience, Vol 1(1), 1-28, January 1988.
 * <p>
 * To enter SSA form, read/write instructions must already be inserted
 * for all global registers which are live-in/out of the hyperblock.
 */
public class SSA
{
  private static int deadInstructionCount = 0; // A count of the number of instructions removed.
  private static int copiesFoldedCount    = 0; // A count of the number of copies that were folded.
  private static int phisInsertedCount    = 0; // A count of the number of phi instructions inserted.
  
  private static final String[] stats = {"deadInstructions", "copiesFolded", "phisInserted"};

  static 
  {
    Statistics.register("scale.backend.trips2.SSA", stats);
  }

  /**
   * Return the number of instructions removed.
   */
  public static int deadInstructions()
  {
    return deadInstructionCount;
  }
  
  /**
   * Return the number of instructions removed.
   */
  public static int copiesFolded()
  {
    return copiesFoldedCount;
  }
  
  /**
   * Return the number of phi instructions inserted.
   */
  public static int phisInserted()
  {
    return phisInsertedCount;
  }
  
  private IntMap<HashSet<PredicateBlock>> defDecls;  // Maps a register to the PredicateBlock's it is defined in (int -> HashSet of PredicateBlock).
  private IntMap<Instruction>         useDef;        // Maps a register to the Instruction which defines it  (int -> Instruction).
  private IntMap<Vector<Instruction>> defUse;        // Maps a register to the Instruction's which use it (int -> vector of Instruction).
  private Vector<int[]>               predicatePhis; // The phis that define predicates.
  private IntMap<int[]>               stacks;        // The current register for v.
  private RegisterSet                 registers;     // The register set to use.
  private Hyperblock                  hb;            // The hyperblock to put into SSA form.
  private BitVect                     in;            // The registers live-in to this hyperblock.
  private BitVect                     out;           // The registers live-out of this hyperblock.     

  /**
   * The SSA constructor.
   */
  public SSA(RegisterSet registers, Hyperblock  hb)
  {
    this.registers = registers;
    this.hb        = hb;
    this.in        = hb.getLiveIn();
    this.out       = hb.getLiveOut();
  }  

  /**
   * Place phi functions and rename all variables.
   */
  public final void placePhis()
  {
    placePhiFunctions();
    rename();
  }
  
  /**
   * Return a list of variables used in more than one basic block.
   * Create a list of non-local variables for use in creating
   * semi-pruned SSA form.  This is the list of variables for which
   * phi functions must be inserted.
   */
  private BitVect getNonLocalVars()
  {
    Stack<Node>    wl           = WorkArea.<Node>getStack("getNonLocalVars");
    BitVect        killed       = new BitVect(); 
    BitVect        nonLocalVars = new BitVect();
    PredicateBlock start        = hb.getFirstBlock();
    
    start.nextVisit();
    start.setVisited();
    wl.add(start);
    
    while (!wl.isEmpty()) {
      PredicateBlock b = (PredicateBlock) wl.pop();
      b.pushOutEdges(wl);
      
      killed.reset();
      
      if (b.isPredicated()) {
      	nonLocalVars.set(b.getPredicate());
      }
        
      for (Instruction inst = b.getFirstInstruction();
           inst != null;
           inst = inst.getNext()) {
        int[] srcs = inst.getSrcRegisters();
        if (srcs != null) {
          for (int j = 0; j < srcs.length; j++) {
            int r = srcs[j];
            if (!killed.get(r)) {
              nonLocalVars.set(r);
            } 
          }
        }
          
        int dest = inst.getDestRegister();
        if (dest != -1)
          killed.set(dest);
      }
    }
    
    WorkArea.<Node>returnStack(wl);
    
    return nonLocalVars;
  }
  
  /**
   * Return a set of PredicateBlocks which define a register.
   */
  private HashSet<PredicateBlock> getDefBlockSet(int def)
  { 
    if (defDecls == null)
      computeDefBlockSet();
    
    HashSet<PredicateBlock> set = defDecls.get(def);
    if (set == null) {
      set = new HashSet<PredicateBlock>(1);
      defDecls.put(def, set);
    }
    
    return set;
  }
    
  /**
   * Compute the set of blocks that define a register.
   */
  private void computeDefBlockSet()
  {
    PredicateBlock start = hb.getFirstBlock();
    Stack<Node>    wl    = new Stack<Node>();   
    
    defDecls = new IntMap<HashSet<PredicateBlock>>(203);  // Clear previous.
    
    start.nextVisit();
    start.setVisited();
    wl.add(start);
    
    while (!wl.isEmpty()) {
      PredicateBlock b = (PredicateBlock) wl.pop();
      b.pushOutEdges(wl);
      
      for (Instruction inst = b.getFirstInstruction();
           inst != null;
           inst = inst.getNext()) {         
        int dest = inst.getDestRegister();
        if (dest == -1) 
          continue;
        
        HashSet<PredicateBlock> set = defDecls.get(dest);
        if (set == null) {
          set = new HashSet<PredicateBlock>(7);
          defDecls.put(dest, set);
        }
        set.add(b);
      }
    }
  }
  
  /**
   * Computes use/def and def/use chains.
   */
  private void computeUseDef()
  {
    PredicateBlock start = hb.getFirstBlock();
    Stack<Node>    wl    = WorkArea.<Node>getStack("computeUseDef");  
    
    defUse = new IntMap<Vector<Instruction>>(203);   // Clear previous.
    useDef = new IntMap<Instruction>(203);   // Clear previous.
    
    start.nextVisit();
    start.setVisited();
    wl.add(start);
    
    while (!wl.isEmpty()) {
      PredicateBlock b = (PredicateBlock) wl.pop();
      b.pushOutEdges(wl);
      
      for (Instruction inst = b.getFirstInstruction();
           inst != null;
           inst = inst.getNext()) {   
        if (inst.isMarker())
          continue;
       
        int[] srcs = inst.getSrcRegisters();
        if (srcs != null) {
          for (int i = 0; i < srcs.length; i++) {
            int                 src = srcs[i];
            Vector<Instruction> use = defUse.get(src);
            if (use == null) {
              use = new Vector<Instruction>();
              assert (defUse.get(src) == null) : "def-use";
              defUse.put(src, use);
            }
            
            if (!use.contains(inst))
              use.add(inst);
          }
        }
        
        if (inst.getOpcode() != Opcodes.WRITE) {
          int dest = inst.getDestRegister();
          if (dest != -1) {
            assert (useDef.get(dest) == null) : "use-def";
            useDef.put(dest, inst);
          }
        }
      }
    }
    
    WorkArea.<Node>returnStack(wl);
  }
  
  /**
   * Get a map of definitions to Instructions.
   */
  public final IntMap<Instruction> getUseDef()
  {
    if (useDef == null)
      computeUseDef();
    
    return useDef;
  }
  
  /**
   * Get a map of uses to Instructions.
   */
  public final IntMap<Vector<Instruction>> getDefUse()
  {
    if (defUse == null)
      computeUseDef();
    
    return defUse;
  }
  
  /**
   * Set the instruction which defines a register.
   */  
  public final void setDef(Instruction inst, int def)
  {
    if (useDef == null)
      return;

    useDef.put(def, inst);
  }
  
  /**
   * Remove an instruction from the set of uses.
   */  
  public final void removeUse(Instruction inst, int use)
  {
    if (defUse == null)
      return;
    
    Vector<Instruction> uses = defUse.get(use);
    uses.remove(inst);
  }
  
  /**
   * Record that an instruction uses a register.
   */  
  protected void addUse(Instruction inst, int use)
  {
    if (defUse == null)
      return;
    
    Vector<Instruction> uses = defUse.get(use);
    if (uses == null) {
      uses = new Vector<Instruction>();
      defUse.put(use, uses);
    }
     
    if (!uses.contains(inst)) 
      uses.add(inst);
  }
  
  /**
   * Remove all the use-def and def-use information for an
   * instruction.  This should be called whenever an instruction is
   * removed.
   */
  public final void clearDefUse(Instruction inst)
  {
    int ra = inst.getDestRegister();
    setDef(null, ra);
    
    int[] srcs = inst.getSrcRegisters();
    if (srcs == null)
      return;
    
    for (int i = 0; i < srcs.length; i++)
      removeUse(inst, srcs[i]);
  }
  
  /**
   * Place the Phi function nodes in the PFG.
   */
  private void placePhiFunctions()
  {
    PredicateBlock start = hb.getFirstBlock();
    if (start.numOutEdges() == 0) // If there is only one block, there cannot be any phis.
      return;
   
    DominanceFrontier       domf = hb.getDominanceFrontier();
    HashSet<PredicateBlock> aPhi = WorkArea.<PredicateBlock>getSet("trips placePhiFunctions");
    PredicateBlock          last = hb.getLastBlock();

    int[] ea = getNonLocalVars().getSetBits();
    for (int i = 0; i < ea.length; i++) {
      int     av    = ea[i];  
      HashSet<PredicateBlock> aOrig = getDefBlockSet(av);
      
      aOrig.add(start);  
      aPhi.clear();
      
      HashSet<PredicateBlock> wl = aOrig.clone();
      while (!wl.isEmpty()) {
        PredicateBlock block = wl.remove();
        Iterator<Node> domfn = domf.getDominanceFrontier(block);
        
        while (domfn.hasNext()) {
          PredicateBlock y = (PredicateBlock) domfn.next();
          
          // Since we do not use a pruned SSA form, we insert phi
          // instructions that are dead.  One easy case we can catch
          // is not to insert phi instructions for predicates in the
          // last block.
          
          if ((y == last) && hb.isPredicate(av))
            continue;
          
          if (aPhi.add(y)) { // Construct the phi node.
            PhiInstruction phi = new PhiInstruction(av, y.numInEdges());
            int            brp = y.getPredicate();
            
            if (hb.isPredicate(av))
              phi.setDefinesPredicate();

            y.insertInstructionAtHead(phi);
            phisInsertedCount++;
            
            if (!aOrig.contains(y))
              wl.add(y);
          }
        }
      }
    }

    WorkArea.<PredicateBlock>returnSet(aPhi);
  }
  
  /**
   * Rename using a pre-order walk over the dominator tree.
   */
  private void rename()
  {
    PredicateBlock start = hb.getFirstBlock();
    
    hb.clearPredicates();

    stacks = new IntMap<int[]>(203);
    search(start);    
    stacks = null;
  }
  
  /**
   * Return the stack for a key or a new stack if none exists.  The
   * stack is represented as an int array whose first element is the
   * number of items on the stack.
   */
  private int[] getStack(int key)
  {
    int[] s = stacks.get(key);
    if (s == null) {
      s = new int[4];
      s[0] = 0;
      stacks.put(key, s);
    }
    return s;
  }
  
  /**
   * Rename registers.
   */
  private void search(PredicateBlock block)
  {    
    int[] pushList = new int[100];
    int   npl      = 0;
    
    // Rename the current block.

    for (Instruction inst = block.getFirstInstruction();
         inst != null;
         inst = inst.getNext()) {
      if (inst.isMarker())
        continue;
   
      int opcode = inst.getOpcode();
      if (opcode == Opcodes._PHI) {
        int   v = inst.getDestRegister();
        int   i = registers.newTempRegister(RegisterSet.AFIREG); 
        int[] s = getStack(v);
        
        int n = s[0] + 1;
        if (n >= s.length) {
          int[] ns = new int[n + 4];
          System.arraycopy(s, 0, ns, 0, s.length);
          s = ns;
          stacks.put(v, s);
        }

        s[n] = i;
        s[0] = n;

        if (npl >= pushList.length) {
          int[] nv = new int[npl * 2];
          System.arraycopy(pushList, 0, nv, 0, npl);
          pushList = nv;
        }

        pushList[npl++] = v;  
        inst.remapDestRegister(v, i);   
      } else {
      	// Rename the source operands.
       
        if (opcode != Opcodes.READ) {
          int[] srcs = inst.getSrcRegisters();
          if (srcs != null) {
            for (int ui = 0; ui < srcs.length; ui++) {
              int   x = srcs[ui];
              int[] s = getStack(x);
              int   n = s[0];
              int   i = (n == 0) ? -1 : s[n];
              
              if (i < 0) // This is a use before a def and needs to be defined.
                throw new scale.common.InternalError(x + " is undefined in " + inst);
               
              inst.remapSrcRegister(x, i);  
            }
          }
        }
        
        // Rename the destination register.
        
        if (opcode != Opcodes.WRITE) {
          int v = inst.getDestRegister();
          if (v != -1) {
            int[] s = getStack(v);
            int   i = registers.newTempRegister(RegisterSet.AFIREG);
       
            inst.remapDestRegister(v, i);
          
            int n = s[0] + 1;
            if (n >= s.length) {
              int[] ns = new int[n + 4];
              System.arraycopy(s, 0, ns, 0, s.length);
              s = ns;
              stacks.put(v, s);
            }

            s[n] = i;
            s[0] = n;

            if (npl >= pushList.length) {
              int[] nv = new int[npl * 2];
              System.arraycopy(pushList, 0, nv, 0, npl);
              pushList = nv;
            }

            pushList[npl++] = v;  
          }
        }
      }
    }
  
    // Update the predicate for the block (it may have been renamed).
    // This keeps the PFG valid.
    
    if (block.isPredicated()) {
      int   pred        = block.getPredicate();
      int[] s           = getStack(pred);
      int   renamedPred = s[s[0]];

      assert (renamedPred != 0) : "Renaming with invalid predicate: " + pred;

      block.setPredicate(renamedPred);
      hb.setPredicate(renamedPred);
    }
     
    // Update the phi functions in the successor blocks. 
    
    for (int i = 0; i < block.numOutEdges(); i++) {
      PredicateBlock succ = (PredicateBlock) block.getOutEdge(i);
      
      if (succ.numInEdges() < 2) // not a join
        continue;
      
      int j = whichPred(succ, block);
      
      for (Instruction inst = succ.getFirstInstruction();
           inst != null;
           inst = inst.getNext()) {
        if (inst.getOpcode() == Opcodes._PHI) {
          PhiInstruction phi      = (PhiInstruction) inst;          
          int[]          operands = phi.getOperands();
          int            v        = operands[j];
          int[]          s        = stacks.get(v);
        
          // The stack will be empty when this phi is not used.
          // This check could be eliminated by using pruned SSA.
          
          if ((s == null) || (s[0] == 0))
            continue;
          
          int k = s[s[0]];
          phi.setOperand(k, j);
        }
      }
    }
    
    // Recurse on each child in the dominator tree.
    
    Domination dom = hb.getDomination();
    Node[]     dn  = dom.getDominatees(block);

    for (Node dd : dn)
      search((PredicateBlock) dd);
    
    // Pop the stack
    
    for (int j = 0; j < npl; j++) {
      int[] s = getStack(pushList[j]);
      s[0] = s[0] - 1;
    }   
  }
  
  /**
   * Returns which phi function in the successor corresponds to the current block.
   */
  private int whichPred(PredicateBlock succ, PredicateBlock b)
  {
    return succ.indexOfInEdge(b);
  }
    
  /**
   * Remove any phi instructions still in the PFG.
   * <br>
   * We use Cytron's algorithm of inserting copies into all predecessors of the phi.
   * This works because we don't have critical edges inside a hyperblock. 
   * <br>
   * If the instruction which defines a phi's operand can be renamed we do so and 
   * avoid inserting a copy in the predecessor predicate block.
   * <br>
   * There is a special case for handling phi's which define predicates. Instead of
   * replacing the phi with mov instructions we rename all the instructions which
   * reference one of the phi's operands to the same register.  If we were to insert
   * mov instructions, the mov's would define predicates and when the PFG was rebuilt
   * each mov would cause a split in the PFG.  
   */
  public final void removePhis()
  {    
    PredicateBlock start = hb.getFirstBlock();
    
    predicatePhis = new Vector<int[]>();

    removePhis(start);
    if (!predicatePhis.isEmpty())
      renamePredicatePhis();

    predicatePhis = null;
  }
  
  /**
   * Remove all phis which do not define predicates by inserting 
   * mov's into the blocks which are predecessors of the phi.
   */
  private void removePhis(PredicateBlock b)
  {    
    for (Instruction inst = b.getFirstInstruction(); inst != null; inst = inst.getNext()) {
      if (!inst.isPhi())
        break;
      
      PhiInstruction phi      = (PhiInstruction) inst;
      int            ra       = phi.getDestRegister();
      int[]          operands = phi.getOperands();
      
      // If the phi defines a predicate add it to the list of phis which must be renamed
      // Otherwise insert mov instructions in place of the phi.
      
      if (phi.definesPredicate()) {
        addPredicateReg(ra, operands);  
      } else {
        int ol = operands.length;
        for (int j = 0; j < ol; j++) {
          PredicateBlock pred = (PredicateBlock) b.getInEdge(j);
          int            rb   = operands[j];
          
          if (renamePhiOperand(ra, rb, pred))
            continue;
         
          int[]            preds = pred.getPredicates();
          boolean          sense = pred.isPredicatedOnTrue();
          TripsInstruction mov   = new GeneralInstruction(Opcodes.MOV, ra, rb);
          
          mov.setPredicates(preds, sense);
          pred.insertInstructionBeforeBranch(mov);
          addUse(mov, rb); // renamePhiOperand may need to rename the mov
        }
      }
          
      b.removeInstruction(null, inst);  // Remove the phi.
    }
    
    // Recurse on each child in the dominator tree.
    
    Domination dom = hb.getDomination();
    Node[]     dn  = dom.getDominatees(b);
    
    for (Node dd : dn)
      removePhis((PredicateBlock) dd);
  }
  
  /**
   * Try to rename the instruction which defines a phi's operand when leaving SSA form.
   * This avoids inserting a mov instruction for the operand when successful. 
   */
  private boolean renamePhiOperand(int dest, int operand, PredicateBlock pred)
  { 
    Instruction def = useDef.get(operand); 
    if (!def.isPredicated())
      return false;

    int[]   dpreds = def.getPredicates();
    int[]   bpreds = pred.getPredicates();
    boolean found  = false;
    
    for (int i = 0; i < dpreds.length; i++) {
      found = false;
      for (int j = 0; j < bpreds.length; j++) {
        if (dpreds[i] == bpreds[j]) {
          found = true;
          break;
        }
      }
      if (!found)
        return false;
    }

    int ra = def.getDestRegister();
    if (ra != operand)  // Already renamed.
      return true;
    
    Vector<Instruction> uses = defUse.get(ra);
    int    ul   = uses.size();
    for (int i = 0; i < ul; i++) {
      Instruction inst = uses.get(i);
      inst.remapSrcRegister(ra, dest);
    }
      
    def.remapDestRegister(ra, dest);
    return true;
  }
  
  /**
   * If a phi defines a predicate, rename any instructions which 
   * use the predicate to the same register. The register used can be
   * a new register or one of the phi's operands.  Here is an example
   * of renaming using one of the phi's operands -- p1.
   * 
   *      test_f<..> p1                  test_f<..> p1
   *      test_t<..> p2                  test_t<..> p1
   *      phi_t<..>  p3 = p1, p2   ==>   nop
   *      add_t<p3>                      add_t<p1>
   */      
  private void renamePredicatePhis()
  {
    PredicateBlock start = hb.getFirstBlock();
    Stack<Node>    wl    = WorkArea.<Node>getStack("renamePredicatePhis");
    
    start.nextVisit();
    start.setVisited();
    wl.add(start);
  
    while (!wl.isEmpty()) {
      PredicateBlock b = (PredicateBlock) wl.pop();
      b.pushOutEdges(wl);
      
      // Rename the predicates for the block. 
      
      if (b.isPredicated()) {
        int[] brp = b.getPredicates();
        for (int i = 0; i < brp.length; i++) {
          int pred = getRenamePredicateReg(brp[i]);
          if (pred > -1) 
            brp[i] = pred;
        }
        
        b.setPredicates(brp);
        
        assert (brp.length == 1);  // Multiple predicates is not finished.
        hb.setPredicate(brp[0]);   // Update the predicates used by the PFG.
      }
      
      // Rename the instructions in the block.
      
      for (Instruction inst = b.getFirstInstruction(); inst != null; inst = inst.getNext()) {
        if (inst.isMarker())
          continue;
        
        // Rename the destination register.
        
        int ra    = inst.getDestRegister();
        int newra = getRenamePredicateReg(ra);
        
        if (newra > -1)
          inst.remapDestRegister(ra, newra);
        
        // Rename the predicates used by the instruction.
        
        int pl = inst.numPredicates();
        for (int i = 0; i < pl; i++) {
          int pr      = inst.getPredicate(i);
          int newpred = getRenamePredicateReg(pr);
          if (newpred > -1) 
            inst.remapSrcRegister(pr, newpred);
        }
      }
    }
    
    WorkArea.<Node>returnStack(wl);
  }
  
  /**
   * Create a set of equivalent predicate registers from a phi instruction.
   */
  private void addPredicateReg(int ra, int[] operands)
  {
    int[] set = null; // First position of the array is the number of elements.
    
    // Find an existing set.
    
    int pl = predicatePhis.size();
    int ol = operands.length;
    int ix = 0;
    for (int i = 0; i < pl; i++) {
      int[] v = predicatePhis.get(i);
    lp:
      for (int j = 0; j < ol; j++) {
        int op = operands[j];
        int l  = v[0];
        for (int k = 1; k <= l; k++)
          if (v[k] == op) {
            set = v;
            ix = i;
            break lp;
          }
      }
    }
    
    if (set == null) {
      set = new int[ol + 4];
      predicatePhis.add(set);
      set[0] = ol + 1;
      set[1] = ra;
      System.arraycopy(operands, 0, set, 2, ol);
      return;
    }

    int n  = set[0] + 1;
    int nl = n + ol + 2;
    if (nl >= set.length) {
      int[] ns = new int[nl + 4];
      System.arraycopy(set, 0, ns, 0, set.length);
      predicatePhis.setElementAt(ns, ix);
      set = ns;
    }

    set[n] = ra;
    System.arraycopy(operands, 0, set, n + 1, ol);
    set[0] = n + ol;
  }
  
  /**
   * Return the register that phi removal should use to rename a predicate.
   */
  private int getRenamePredicateReg(int reg)
  {
    int pl = predicatePhis.size();
    for (int i = 0; i < pl; i++) {
      int[] v = predicatePhis.get(i);
      for (int j = 1; j <= v[0]; j++)
        if (v[j] == reg)
          return v[1];
    }
    return -1;
  }
  
  /**
   * Remove instructions which are dead.
   * <br>
   * This routine also maintains the predicates for the hyperblock.
   */
  public final void removeDeadCode()
  {
    PredicateBlock                       start  = hb.getFirstBlock();
    Stack<Node>                          wl     = WorkArea.<Node>getStack("removeDeadCode");    
    Stack<Instruction>                   useful = WorkArea.<Instruction>getStack("removeDeadCode");
    IntMap<Instruction>                  ud     = getUseDef();
    HashMap<Instruction, PredicateBlock> phiMap = new HashMap<Instruction, PredicateBlock>(17);
    
    hb.clearPredicates();  // Recompute the predicates below.

    start.nextVisit();
    start.setVisited();
    wl.push(start);
    
    // Tag all the useful instructions with a '1'.
    
    while (!wl.isEmpty()) {
      PredicateBlock b = (PredicateBlock) wl.pop();
      b.pushOutEdges(wl);
      
      if (b.isPredicated())
        hb.setPredicate(b.getPredicate());  // Update the set of predicates.
      
      for (Instruction inst = b.getFirstInstruction(); inst != null; inst = inst.getNext()) {
        int opcode = inst.getOpcode();
        if (inst.isStore() 
            || inst.isBranch() 
            || inst.isMarker() 
            || inst.isPrefetch()
            || (opcode == Opcodes.WRITE)) {
          useful.push(inst);   
          inst.setTag(1);  
        } else 
          inst.setTag(0); 
        
        if (opcode == Opcodes._PHI)
          phiMap.put(inst, b);
      }
    }
    
    while (!useful.isEmpty()) {
      Instruction inst   = useful.pop();
      int         opcode = inst.getOpcode();
      if (opcode == Opcodes.READ)
        continue;
          
      // Phi removal will insert mov's in the blocks which are predecessors 
      // of a phi. So do not kill the instructions which define the predicates
      // for the predecessor blocks. Doing so would cause the mov's to be 
      // predicated on undefined registers.
      
      if (opcode == Opcodes._PHI) {
        PredicateBlock b   = phiMap.get(inst);
        int            bin = b.numInEdges();
        
        for (int i = 0; i < bin; i++) {
          PredicateBlock pred = (PredicateBlock) b.getInEdge(i);
          if (!pred.isPredicated())
            continue;
         
          int[] preds = pred.getPredicates();
          int   pl    = preds.length;
          for (int j = 0; j < pl; j++) {
            int         rp  = preds[j];
            Instruction def = ud.get(rp);
        
            if (def.getTag() != 1) {
              useful.push(def);
              def.setTag(1);
            }
          }
        }
      }
      
      int[] operands = inst.getSrcRegisters();
      if (operands != null) {
        for (int i = 0; i < operands.length; i++) {
          int         t   = operands[i];
          Instruction def = ud.get(t);  
          if (def == null)
            continue;
          
          if (def.getTag() != 1) {
            useful.push(def);
            def.setTag(1);
          }        
        }
      }
    }
    
    // Eliminate dead code.  
    
    boolean deadPredicate = false;

    start.nextVisit();
    start.setVisited();
    wl.push(start);
    
    while (!wl.isEmpty()) {
      PredicateBlock b = (PredicateBlock) wl.pop();
      b.pushOutEdges(wl);

      Instruction prev = null;
      for (Instruction inst = b.getFirstInstruction();
           inst != null;
           inst = inst.getNext()) {
        if (inst.getTag() == 1) {
          prev = inst;
          continue;
        }
  
        if (((TripsInstruction) inst).definesPredicate())
          deadPredicate = true;
        b.removeInstruction(prev, inst); 
        clearDefUse(inst);          
        deadInstructionCount++;
      }
    }
    
    WorkArea.<Node>returnStack(wl);
    WorkArea.<Instruction>returnStack(useful);

    // Now clean up the PFG

    if (deadPredicate)
      hb.removeDeadPredicateBlocks();
  }
}
