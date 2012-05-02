package scale.backend.trips2;

import scale.common.*;

import scale.backend.Node;
import scale.backend.Instruction;
import scale.backend.Label;
import scale.backend.Branch;
import scale.backend.RegisterSet;

/**
 * This class computes liveness on the Hyperblock Flow Graph.
 * <p>
 * $Id: DataflowAnalysis.java,v 1.31 2006-11-16 17:49:39 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <br>
 */
public class DataflowAnalysis
{
  private BitVect[]    hin;      // The registers live-in to a hyperblock.
  private BitVect[]    hout;     // The registers live-out of a hyperblock.
  private BitVect[]    hdef;     // The registers defined in a hyperblock.
  private BitVect[]    huse;     // The registers used in a hyperblock.
  private BitVect[]    hmod;     // The registers clobbered by a hyperblock.
  private BitVect[]    bin;      // The registers live-in to a predicate block.
  private BitVect[]    bdef;     // The registers defined in a predicate block.
  private BitVect[]    buse;     // The registers used in a predicate block.
  private BitVect[]    bout;     // The registers live-out of a predicate block.
  private Vector[]     edges;    // All exit predicate blocks that branch to a hyperblock.
  private Hyperblock[] hbs;      // The hyperblock containing a predicate block.
  private Hyperblock   hbStart;
  private RegisterSet  regs;
  private HashMap<Instruction, Hyperblock> entries;  
  
  /**
   * The default constructor.
   */
  public DataflowAnalysis(Hyperblock hbStart, RegisterSet regs)
  {
    this.hbStart = hbStart;
    this.regs    = regs;
  }
  
  /**
   * Return the live-in's for a hyperblock.
   */
  public BitVect getIn(int tag)
  {
    return hin[tag];
  }
  
  /**
   * Return the registers live-in.
   */
  public BitVect[] getIns()
  {
    return hin;
  }
  
  /**
   * Return the live-out's for a hyperblock.
   */
  public BitVect getOut(int tag)
  {
    return hout[tag];
  }
  
  /**
   * Return the registers live-out.
   */
  public BitVect[] getOuts()
  {
    return hout;
  }
  
  /**
   * Return the registers defined in a hyperblock.
   */
  public BitVect getDef(int tag)
  {
    return hdef[tag];
  }
  
  /**
   * Return the registers defined.
   */
  public BitVect[] getDefs()
  {
    return hdef;
  }
  
  /**
   * Return the registers used in a hyperblock.
   */
  public BitVect getUse(int tag)
  {
    return huse[tag];
  }
  
  /**
   * Return the registers used.
   */
  public BitVect[] getUses()
  {
    return huse;
  }
  
  /**
   * Return the registers clobered by a hyperblock.
   */
  public BitVect getMod(int tag)
  {
    return hmod[tag];
  }
  
  /**
   * Return the registers modified.
   */
  public BitVect[] getMods()
  {
    return hmod;
  }
  
  /**
   * Return the registers live-in to a predicate block.
   */
  public BitVect getBlockIn(int tag)
  {
    return bin[tag];
  }
  
  /**
   * Return the registers live-out of a predicate block.
   */
  public BitVect getBlockOut(int tag)
  {
    return bout[tag];
  }
  
 /**
  * Compute liveness -- all registers live for a hyperblock.
  */
  protected void computeLiveness()
  {
    doLiveness(true);
  }

  /**
   * Compute liveness -- all registers live for a hyperblock and its predicate blocks.
   */
  protected void computeLiveness3()
  {
    doLiveness(false);
  }
  
  /**
   * Main routine. 
   */
  private void doLiveness(boolean reduceMemory)
  {
    Stack<PredicateBlock> all = WorkArea.<PredicateBlock>getStack("computeLiveness df");
    
    init(all);
    computeRefs();
    computeInitialSets(all);
    iterate(all);
    computeFinalSets(); 
    
    WorkArea.<PredicateBlock>returnStack(all);
    
    if (reduceMemory) {
      bin  = null;
      bout = null;
      buse = null;
      bdef = null;
    }
    
    edges   = null;
    hbs     = null;    
    entries = null;
  }
    
  /**
   * Initialize.
   */
  private void init(Stack<PredicateBlock> all)
  {
    Stack<Node> wl  = WorkArea.<Node>getStack("computeLiveness df");
    Stack<Node> hwl = WorkArea.<Node>getStack("computeLiveness df");

    int   nb  = 0;
    int   nh  = 0;
    
    // Update the tags.
    
    hbStart.nextVisit();
    hbStart.setVisited();
    hwl.push(hbStart);
    
    while (!hwl.isEmpty()) {
      Hyperblock hb = (Hyperblock) hwl.pop();      
      hb.pushOutEdges(hwl);
      hb.setTag(nh);
      nh++;
      
      PredicateBlock start = hb.getFirstBlock();
      start.nextVisit();
      start.setVisited();
      wl.add(start);
            
      while (!wl.isEmpty()) {
        PredicateBlock block = (PredicateBlock) wl.pop();
        block.pushOutEdges(wl);        
        all.push(block);    
        block.setTag(nb);
        nb++;
      }
    }

    WorkArea.<Node>returnStack(wl);
    WorkArea.<Node>returnStack(hwl);
    
    // Allocate the bit vectors.
    
    hin   = new BitVect[nh];
    hout  = new BitVect[nh];
    huse  = new BitVect[nh];
    hdef  = new BitVect[nh];
    hmod  = new BitVect[nh];
    edges = new Vector[nh];
    
    for (int i = 0; i < nh; i++) {
      hout[i]  = new BitVect();
      huse[i]  = new BitVect();
      hdef[i]  = new BitVect();
      hmod[i]  = new BitVect();
      edges[i] = new Vector<PredicateBlock>();
    }
    
    hbs  = new Hyperblock[nb];
    bin  = new BitVect[nb];
    bout = new BitVect[nb];
    buse = new BitVect[nb];
    bdef = new BitVect[nb];
    
    for (int i = 0; i < nb; i++) {
      bin[i]  = new BitVect();
      bout[i] = new BitVect();
      buse[i] = new BitVect();
      bdef[i] = new BitVect();
    }
  }
  
  /**
   * Compute the refs needed for traversing the HFG.
   */
  private void computeRefs()
  {
    Stack<Node> wl  = WorkArea.<Node>getStack("computeLiveness df");
    Stack<Node> hwl = WorkArea.<Node>getStack("computeLiveness df");
    
    entries = new HashMap<Instruction, Hyperblock>();
    
    hbStart.nextVisit();
    hbStart.setVisited();
    hwl.push(hbStart);
    
    while (!hwl.isEmpty()) {
      Hyperblock hb = (Hyperblock) hwl.pop();      
      hb.pushOutEdges(hwl);
      
      PredicateBlock start = hb.getFirstBlock();
      start.nextVisit();
      start.setVisited();
      wl.add(start);
            
      entries.put(start.getFirstInstruction(), hb);  // The hyperblock for this entry.
                  
      while (!wl.isEmpty()) {
        PredicateBlock block = (PredicateBlock) wl.pop();
        block.pushOutEdges(wl);        
        hbs[block.getTag()] = hb;  // The hyperblock that contains the predicate block.
      }
    }

    WorkArea.<Node>returnStack(wl);
    WorkArea.<Node>returnStack(hwl);
  }

  /**
   * Compute the initial sets for each predicate block.
   */
  private void computeInitialSets(Stack<PredicateBlock> all)
  {
    int nrr = regs.numRealRegisters();
    int al  = all.size();
      
    for (int i = 0; i < al; i++) {
      PredicateBlock block = all.get(i);
      int            tag   = block.getTag();
      BitVect        in    = bin[tag];
      BitVect        out   = bout[tag];
      BitVect        uses  = buse[tag];
      BitVect        defs  = bdef[tag];
      for (Instruction inst = block.getFirstInstruction(); inst != null; inst = inst.getNext()) {
        int[] srcs = inst.getSrcRegisters();
        if (srcs != null) {
          for (int j = 0; j < srcs.length; j++) {
            int reg = srcs[j];
            if (!defs.get(reg))
              in.set(reg);
            uses.set(reg);
          }
        }
        
        if (inst.isBranch()) {
          TripsBranch br = (TripsBranch) inst;
          block.setHasBranch();  // Block splitting does not maintain this.
          
          // Determine the exits for the entry to a hyperblock.
          
          int bt = br.numTargets();
          for (int j = 0; j < bt; j++) {
            Label      lab  = br.getTarget(j);
            Hyperblock hb   = entries.get(lab);
            int        htag = hb.getTag();
            @SuppressWarnings("unchecked")
            Vector<PredicateBlock> v = edges[htag];
          
            v.add(block);
          }
            
          if (br.isCall() || (br.getOpcode() == Opcodes.RET)) {
          
            // The arguments are live out of a hyperblock.
          
            short[] used = br.uses();
            for (int j = 0; j < used.length; j++) {
              int ra = used[j];
              out.set(ra);
              uses.set(ra);
            }
          
            // Caller saved registers are clobbered by call's
        
            if (br.isCall()) {
              Hyperblock hb     = hbs[tag];
              BitVect    kills  = hmod[hb.getTag()]; 
              short[]    killed = br.getRegsKilled();
              for (int j = 0; j < killed.length; j++) {
                int ra = killed[j];
                kills.set(ra);
              }
            }
          }
        } else {
          int dest = inst.getDestRegister();
          if (dest > -1) {
            defs.set(dest);
            if (dest < nrr)
              out.set(dest);
          }
        }
      }
    }
  }
  
  /**
   * Compute liveness iteratively over all predicate blocks in the HFG.
   */
  private void iterate(Stack<PredicateBlock> all)
  {
    while (!all.isEmpty()) {
      PredicateBlock block = all.pop();
      int            tag   = block.getTag();
      BitVect        in    = bin[tag];
      BitVect        out   = bout[tag];
      BitVect        defs  = bdef[tag];
      BitVect        od    = out.clone();

      od.andNot(defs);
      in.or(od);   // in = in + (out - def)
      
      int ni = block.numInEdges();
      if (ni == 0) {
        Hyperblock hb = hbs[tag];
        @SuppressWarnings("unchecked")
        Vector<PredicateBlock> v  = edges[hb.getTag()];
        int        vl = v.size();
        
        for (int j = 0; j < vl; j++) { 
          PredicateBlock pred = v.get(j);
          int            ptag = pred.getTag();
          BitVect        pOut = bout[ptag];
        
          if (pOut.orAndTest(in)) // out(p) = out(p) + in(s)
            all.push(pred);
        }
      } else {
        for (int j = 0; j < ni; j++) {
          PredicateBlock pred = (PredicateBlock) block.getInEdge(j);
          int            ptag = pred.getTag();
          BitVect        pOut = bout[ptag];
        
          if (pOut.orAndTest(in)) // out(p) = out(p) + in(s)
            all.push(pred);
        }
      }
    }
  } 

  /**
   * Compute the final bit-vectors for each hyperblock. 
   */
  private void computeFinalSets()
  {
    Stack<Node> hwl = WorkArea.<Node>getStack("computeFinalSets df");
    Stack<Node> wl  = WorkArea.<Node>getStack("computeFinalSets df");
    
    hbStart.nextVisit();
    hbStart.setVisited();
    hwl.push(hbStart);
    
    while (!hwl.isEmpty()) {
      Hyperblock hb = (Hyperblock) hwl.pop();
      hb.pushOutEdges(hwl);
  
      int            htag  = hb.getTag();
      PredicateBlock start = hb.getFirstBlock();  
      BitVect        out   = hout[htag];
      BitVect        def   = hdef[htag];
      BitVect        use   = huse[htag];     

      start.nextVisit();
      start.setVisited();
      wl.push(start);
      
      while (!wl.isEmpty()) {
        PredicateBlock block = (PredicateBlock) wl.pop();
        int            btag  = block.getTag();
        
        block.pushOutEdges(wl);
        
        // The register allocator needs to know all the registers
        // defined and used by the hyperblock.
        
        def.or(bdef[btag]);
        use.or(buse[btag]);
        
        // The registers live-out of an exit are the 
        // registers live-out of the hyperblock.
                
        if (block.hasBranch())  
          out.or(bout[btag]);
      }
      
      // Registers live-in to the first predicate block 
      // are the registers live-in to the hyperblock.
          
      BitVect in = bin[start.getTag()];
      hin[htag] = in;
      
      hb.setLiveIn(in);
      hb.setLiveOut(out);
    }
    
    WorkArea.<Node>returnStack(wl);
    WorkArea.<Node>returnStack(hwl);
  }   
}
