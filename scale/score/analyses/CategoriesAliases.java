package scale.score.analyses;

import java.io.*;
import scale.common.Stack;

import scale.alias.*;
import scale.alias.steensgaard.*;
import scale.alias.shapirohorowitz.*;
import scale.annot.*;
import scale.callGraph.*;
import scale.clef.decl.Declaration;
import scale.clef.decl.VariableDecl;
import scale.common.*;
import scale.score.*;
import scale.score.expr.LoadDeclAddressExpr;
import scale.score.pred.TraceChords;
import scale.score.chords.Chord;

/**
 * This class extends from the Aliases class.
 * <p>
 * $Id: CategoriesAliases.java,v 1.13 2005-02-07 21:28:33 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The only difference between this class and the super class is that
 * here there is an extra step of creating categories and assigning alias variables to them
 * Also, it overloads the method computeAliases
 */
public class CategoriesAliases extends Aliases
{
  /**
   * Create an object for computing alias categories.
   * @param analysis the alias analysis object
   * @param suite the collection of modules to process (may be just one procedure)
   * @param simple is true for simple alias analysis
   */
  public CategoriesAliases(AliasAnalysis analysis, Suite suite, boolean simple)
  {
    super(analysis, suite, simple);
  }

  /**
   * The main routine for computing aliases. Overloaded from the super
   * class Aliases.java First, we create alias variables for the
   * variables in the program then, we create categories based upon
   * the number provided Next, we assign variables to categories
   * Finally, we analyze the program to find the alias relationships
   * among the variables.
   */
  public void computeAliases()
  {
    createAliasVariables();
    findAliases();
    createVirtualVars();
  }

  /**
   * The routine that does a pre-pass over the points-to graph to determine the virtual variable
   * assignments.
   * Not sure if this method should be in the super class or not
   * Also, this pre-pass is somewhat expensive - try to refine later
   */
  public void createVirtualVars()
  {
    /* Within the main for loop - if super virtual variable already created
       find it using the subset, and then create the other subset vv's
       in the final for loop - create super vv's for the variables
       themselves if none found in hash map.
    */

    int l = aliasVars.size();
    for (int i = 0; i < l; i++) {
      SuperVirtualVar supvv    = null;
      boolean         need_sup = false;
      TypeVarCat      av       = (TypeVarCat) aliasVars.elementAt(i);
      Vector<ECR>     lv       = ((LocationTypeCat) av.getECR().getType()).getLocations();

      int llv = lv.size();
      for (int j = 0; j < llv; j++) {
        ECR ecr = lv.elementAt(j);
        if (ecr.getType() != AliasType.BOT) {
          SubVirtualVar subvar = (SubVirtualVar) getVirtualVariable(ecr);
          if (subvar != null)
            supvv = (SuperVirtualVar) subvar.getSuperset();
          else
            need_sup = true;
        }
      }
          
      if ((supvv == null) && need_sup)
        supvv = new SuperVirtualVar(nextVirtualName());

      for (int j = 0; j < llv; j++) {
        ECR ecr = lv.elementAt(j);
        if (ecr.getType() != AliasType.BOT) {
          SubVirtualVar subvar = (SubVirtualVar) getVirtualVariable(ecr);
          if (subvar == null) {
            subvar = supvv.createSubset(ecr);
            addVirtualVariable(ecr, subvar);
          }
        }
      }
    }
 
    for (int i = 0; i < l; i++) {
      TypeVar       av     = (TypeVar) aliasVars.elementAt(i);
      ECR           ecr    = av.getECR();
      SubVirtualVar subvar = (SubVirtualVar) getVirtualVariable(ecr);
      if (subvar == null) {
        SuperVirtualVar supvv = new SuperVirtualVar(nextVirtualName());
        subvar = supvv.createSubset(ecr);
        addVirtualVariable(ecr, subvar);
      }
    }
  }

  /**
   * provide a public static method to get the virtual variable that maps to a
   * given ecr. 
   */
  public VirtualVar getVirtualVar(ECR ecr)
  {
    VirtualVar vv = getVirtualVariable(ecr);
      
    if (vv != null)
      return vv;

    SuperVirtualVar supvv  = new SuperVirtualVar(nextVirtualName());
    SubVirtualVar   subvar = supvv.createSubset(ecr);

    addVirtualVariable(ecr, subvar);

    return subvar;
  }
}



