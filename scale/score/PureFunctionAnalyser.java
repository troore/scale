package scale.score;

import java.util.Iterator;

import scale.common.*;
import scale.annot.*;
import scale.clef.decl.*;
import scale.clef.type.*;
import scale.callGraph.*;

import scale.score.chords.*;
import scale.score.expr.*;

/**
 * This class adds purity level information to RoutineDecls.
 * <p>
 * $Id: PureFunctionAnalyser.java,v 1.31 2007-10-04 19:58:19 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This logic fails on the conservative side.  For example, if there
 * is an address argument such as <code>char *s</code> and
 * <code>s</code> is used only to read and not write, this logic will
 * still specify that the function changes values through its
 * arguments.  If no purity level information is found for a function
 * called by this function, no purity level information is added to
 * this function.
 * @see scale.clef.decl.RoutineDecl
 */
public class PureFunctionAnalyser
{
  /**
   * True if traces are to be performed.
   */
  public static boolean classTrace = false;

  private static final String[] stats = {
    "pfal0", "pfal1", "pfal2", "pfal3",
    "pfal4", "pfal5", "pfal6", "pfal7"};

  private static int[] pfaCount = {0, 0, 0, 0, 0, 0, 0, 0};

  static
  {
    Statistics.register("scale.score.PureFunctionAnalyser", stats);
  }

  /**
   * Return the count of functions marked as not pure.
   */
  public static int pfal0()
  {
    return pfaCount[0];
  }

  /**
   * Return the count of functions marked as PUREARGS.
   */
  public static int pfal1()
  {
    return pfaCount[1];
  }

  /**
   * Return the count of functions marked as PUREGV.
   */
  public static int pfal2()
  {
    return pfaCount[2];
  }

  /**
   * Return the count of functions marked as PUREGV & PUREARGS.
   */
  public static int pfal3()
  {
    return pfaCount[3];
  }

  /**
   * Return the count of functions marked as PURESE.
   */
  public static int pfal4()
  {
    return pfaCount[4];
  }

  /**
   * Return the count of functions marked as PURESE & PUREARGS.
   */
  public static int pfal5()
  {
    return pfaCount[5];
  }

  /**
   * Return the count of functions marked as PURESE & PUREGV.
   */
  public static int pfal6()
  {
    return pfaCount[6];
  }

  /**
   * Return the count of functions marked as PURE.
   */
  public static int pfal7()
  {
    return pfaCount[7];
  }

  private Suite                suite;
  private ProcedureType        pt;
  private Stack<ProcedureType> pts;
  private HashSet<RoutineDecl> done;
  private boolean              trace;

  /**
   * Mark the purity level of all the function specified by the {@link
   * scale.callGraph.Suite Suite}..
   */
  public PureFunctionAnalyser(Suite suite)
  {
    this.suite = suite;

    pts = WorkArea.<ProcedureType>getStack("PureFunctionAnalyser");
    done = WorkArea.<RoutineDecl>getSet("PureFunctionAnalyser");

    Iterator<RoutineDecl> efar = suite.allRoutines();
    while (efar.hasNext()) {
      analyseFunction(efar.next());
    }

    WorkArea.<ProcedureType>returnStack(pts);
    WorkArea.<RoutineDecl>returnSet(done);
  }

  /**
   * Determine the "purity" of a function by analysing its actions.
   * <pre>
   *   PURE             if the function is totally pure
   *   PURE & ~PUREGV   if the function references global variables
   *   PURE & ~PURESE   if the function changes global variables
   *   PURE & ~PUREARGS if the function changes values through it's arguments
   * </pre>
   * If the function calls another function and no purity level is known
   * for that function, do not add an annotation.
   * @param cn is the function to be analysed
   */
  private void analyseFunction(RoutineDecl cn)
  {
    if (!done.add(cn))
      return;

    Scribble scribble = cn.getScribbleCFG();
    if (scribble == null)
      return;

    assert setTrace(cn.getName());

    pts.push(pt);
    pt = cn.getSignature();

    if (trace)
      System.out.println("BEGIN " + cn.getName());

    Chord        begin = scribble.getBegin();
    byte         pure  = RoutineDecl.PURE;
    Stack<Chord> wl    = WorkArea.<Chord>getStack("analyseFunction");

    Chord.nextVisit();
    begin.setVisited();
    wl.push(begin);

    while (!wl.empty() && (pure > 0)) {
      Chord s = wl.pop();
      s.pushOutCfgEdges(wl);

      int l = s.numInDataEdges();
      for (int i = 0; i < l; i++)
        pure = analyseExpr(s.getInDataEdge(i), pure, false);
    }

    WorkArea.<Chord>returnStack(wl);

    pfaCount[pure]++;
    if (pure != 0) {
      cn.setPurityLevel(pure);
      if (Debug.debug(2)) {
        System.out.print("Adding purity level to ");
        System.out.print(cn.getName());
        System.out.print(" at level ");
        System.out.println(pure);
      }
    }

    pt = pts.pop();

    if (trace)
      System.out.println("END " + cn.getName());
  }

  private boolean setTrace(String name)
  {
    trace = Debug.trace(name, classTrace, 3);
    return true;
  }

  private byte analyseExpr(Expr exp, byte pure, boolean onLHS)
  {
    if (pure == 0)
      return 0;

    if (exp.isMemRefExpr()) {
      if (exp instanceof FieldExpr)
        return analyseExpr(((FieldExpr) exp).getStructure(), pure, onLHS);
      if (exp instanceof LoadValueIndirectExpr)
        return analyseExpr(exp.getOperand(0), pure, onLHS);

      Visibility visibility = Visibility.GLOBAL;
      if (exp instanceof LoadExpr) {

        Declaration d = ((LoadExpr) exp).getDecl();

        if (d != null) {
          visibility = d.visibility();

          if (((pure & RoutineDecl.PUREARGS) != 0)  &&
              (exp instanceof LoadDeclValueExpr)) {

            // Check if it's an argument to the function.  This logic
            // fails on the conservative side.  For example, if there
            // is an address argument such as char *s and s is used
            // only to read and not write, this logic will still
            // specify that it writes.

            if (onLHS || d.getType().isPointerType()) {
              int l = pt.numFormals();
              for (int i = 0; i < l; i++) {
                FormalDecl fd = pt.getFormal(i);
                if (fd == d) {
                  pure &= ~RoutineDecl.PUREARGS;
                  if (trace)
                    System.out.println("** PUREARGS " + exp);
                }
              }
            }
          }
        }
      }

      if (visibility != Visibility.LOCAL) {
        if (onLHS) {
          pure &= ~RoutineDecl.PURESE;

          if (trace)
            System.out.println("** PURESE " + exp);
        }

        pure &= ~RoutineDecl.PUREGV;
        if (trace) {
          System.out.print("** PUREGV ");
          System.out.print(exp);
          System.out.println(visibility);
        }
      }

      return pure;
    }

    if (exp instanceof CallExpr) {
      CallExpr               call = (CallExpr) exp;
      Expr                   ftn  = call.getFunction();
      int pfa  = 0;

      if (ftn instanceof LoadExpr) {
        Declaration d  = ((LoadExpr) ftn).getDecl();
        RoutineDecl rd = d.returnRoutineDecl();
        if (rd != null) {
          pfa = rd.getPurityLevel();
          if (pfa == 0) {
            CallGraph cg = suite.getCallGraph(rd);
            if (cg != null) {
	      cg.recordRoutine(rd);
              analyseFunction(rd);
              pfa = rd.getPurityLevel();
            }
          }
        }
      }

      if (pfa == 0) { // Fail on the conservative side.
        if (trace)
          System.out.println("** ALL " + exp);
        return 0;
      }

      pure &= pfa;
      if (trace)
        System.out.println("** CALL " + exp + " " + pure);

      int l = call.numArguments();
      for (int i = 0; i < l; i++)
        pure = analyseExpr(call.getArgument(i), pure, onLHS);

      return pure;
    }

    // Other expression types

    int l = exp.numInDataEdges();
    for (int i = 0; i < l; i++)
      pure = analyseExpr(exp.getInDataEdge(i), pure, onLHS);

    return pure;
  }
}
