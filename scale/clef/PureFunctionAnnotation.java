package scale.clef;

import java.util.Enumeration;
import scale.common.*;

import scale.annot.*;
import scale.clef.decl.RoutineDecl;

/**
 * This annotation is used to mark routines as being "pure functions".
 * <p>
 * $Id: PureFunctionAnnotation.java,v 1.19 2007-08-27 18:26:15 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * There are various types of "purity".
 * The function
 * <ul>
 * <li> may have side effects,
 * <li> may reference any global variables of this program, and
 * <li> may modify any locations referenced by an argument.
 * </ul>
 * The type of purity is specified as an integer where each bit
 * specifies a type of purity.
 * <p>
 * This class no longer creates an annotation.  It simply sets the
 * information in the declaration of the routine.
 * @see scale.clef.decl.RoutineDecl
 */
public class PureFunctionAnnotation extends Annotation 
{
  private static int[] pfaCount = {0, 0, 0, 0, 0, 0, 0, 0};

  private static final String[] stats = {
    "pfal0", "pfal1", "pfal2", "pfal3",
    "pfal4", "pfal5", "pfal6", "pfal7"};

  static
  {
    Statistics.register("scale.clef.PureFunctionAnnotation", stats);
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

  public static Annotation create(RoutineDecl declaration,
                                  Creator     creator,
                                  Support     support,
                                  String      slevel) throws scale.common.InvalidException
  {
    byte level = 0;
    if (slevel.equals("PURE"))
      level = RoutineDecl.PURE;
    else if (slevel.equals("PURESE"))
      level = RoutineDecl.PURESE;
    else if (slevel.equals("PURESGV"))
      level = RoutineDecl.PURESGV;
    else if (slevel.equals("PUREGVA"))
      level = RoutineDecl.PUREGVA;
    else if (slevel.equals("PUREGV"))
      level = RoutineDecl.PUREGV;
    else if (slevel.equals("PUREARGS"))
      level = RoutineDecl.PUREARGS;
    else if (slevel.equals("NOTPURE"))
      level = RoutineDecl.NOTPURE;
    else
      throw new scale.common.InvalidException("Incorrect level " + slevel);

    declaration.setPurityLevel(level);
    pfaCount[level]++;
    return null;
  }

  private PureFunctionAnnotation(Creator creator, Support support)
  {
    super(creator, support);
  }

  /**
   * Return true if the annotations are equivalent.
   */
  public boolean equivalent(Annotation da)
  {
    return false;
  }

  public boolean isUnique()
  {
    return false;
  }

  public static Object annotationKey()
  {
    return "scale.clef.PureFunctionAnnotation";
  }

  public Object getKey()
  {
    return PureFunctionAnnotation.annotationKey();
  }

  public String toStringSpecial()
  {
    return "";
  }
}
