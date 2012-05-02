package scale.backend.trips2;

import java.util.Iterator;

import scale.backend.*;
import scale.common.*;
import scale.callGraph.*;

/**
 * This class converts PTIL into TRIPS instructions.
 * <p>
 * $Id: TripsPGenerator.java,v 1.15 2006-11-16 17:49:42 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class TripsPGenerator extends Trips2Generator
{
  private Vector<String> passThroughItems;   // Lines in the original ptil file which need to be passed through.
  
  /**
   * @param cg is the call graph to be transformed
   * @param machine specifies machine details
   * @param features controls the instructions generated
   */
  public TripsPGenerator(CallGraph cg, Machine machine, int features)
  {
    super(cg, machine, features);
    
    this.passThroughItems = new Vector<String>(50);
  }

  /**
   * Generate the machine instructions for each routine in the call graph.
   */
  public void generate()
  {
    registers.initialize();
    
    generateScribble();
  }
  
  /**
   * Generate the machine instructions for a CFG.
   */
  public void generateScribble()
  { 
    TILReader reader = new TILReader(this);
    
    reader.readFile(cg.getName());
    
    Vector<Instruction> routines = reader.getRoutines();
    
    for (int i = 0; i < routines.size(); i++) {
      Instruction firstInstruction = routines.get(i);
      
      currentRoutine = ((BeginMarker) firstInstruction).getRoutine();
      
      processRoutineDecl(currentRoutine, true);
      
      hbStart = Hyperblock.enterHyperblockFlowGraph(firstInstruction, this);
      
      enterSSA();   
     
      if (!nph) 
        peepholeAfterRegisterAllocation(); 
      
      assignLoadStoreIds();
        
      leaveSSA();
      
      firstInstruction = Hyperblock.leaveHyperblockFlowGraph(hbStart, this);
        
      renameRegisters(firstInstruction);
      
      saveGeneratedCode(firstInstruction);
    }
  } 
  
  /**
   * Add a string which should be passed through to the TIL unchanged.
   */
  protected void addPassThroughItem(String item)
  {
    passThroughItems.add(item);
  }
  
  /**
   * Return the list of strings which should be passed through to the TIL.
   * Each string will be terminated by an end-of-line.
   */
  protected Vector<String> getPassThroughItems()
  {
    return passThroughItems;
  }
 }
