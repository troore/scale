package scale.score.analyses;

import scale.annot.Annotation;
import scale.annot.Creator;
import scale.annot.Support;
import scale.common.Statistics;
import scale.clef.decl.Declaration;
import scale.score.expr.Expr;
import scale.alias.*;

/**
 * An annotation to represent alias variables.  The alias variables
 * <p>
 * $Id: AliasAnnote.java,v 1.19 2007-08-27 18:26:51 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The alias variables
 * are used by the alias analyzer as an abstraction.  We add the 
 * alias annotation to each of the declaration nodes in a program.
 */
public class AliasAnnote extends Annotation
{
  /**
   * The alias variable associated with a declaration.
   */
  private AliasVar aliasVar;

  /**
   * Create a alias annotation.
   * The annotation is added the node representing a declaration.
   * @param n is the node to which the annotation will be attached
   * @param c the tool which creates the annotation
   * @param s the support values for the annotation (belief/combination)
   * @param av the alias variable representing the declaration.
   * @return  the new annotation
   */
  public static Annotation create(Expr n, Creator c, Support s, AliasVar av)
  {
    Annotation a = new AliasAnnote(c, s, av);
    n.addAnnotation(a);
    return a;
  }

  /**
   * Create a alias annotation.  Used instead of a constructor.
   * The annotation is added the node representing a declaration.
   *
   * @param n is the node to which the annotation will be attached
   * @param c the tool which creates the annotation
   * @param s the support values for the annotation (belief/combination)
   * @param av the alias variable representing the declaration.
   * @return  the new annotation
   */
  public static Annotation create(Declaration n, Creator c, Support s, AliasVar av)
  {
    Annotation a = new AliasAnnote(c, s, av);
    n.addAnnotation(a);
    return a;
  }

  /**
   * Construct an alias annotation object.  It is placed on declaration
   * nodes.
   *
   * @param c the tool which creates the annotation
   * @param s the support values for the annotation (belief/combination)
   * @param av the alias variable representing the declaration.
   */
  private AliasAnnote(Creator c, Support s, AliasVar av)
  {
    super(c, s);
    aliasVar = av;
  }

  /**
   * @return true if the annotations are equivalent
   */
  public boolean equivalent(Annotation a)
  {
    if (a instanceof AliasAnnote)
      return aliasVar.equals(((AliasAnnote) a).aliasVar);
    return false;
  }

  /**
   * Get the alias variable.
   * @return the alias variable
   */
  public final AliasVar getAliasVar()
  {
    return aliasVar;
  }

  /**
   * Indicates that a node is only allowed one instance of this annotation.
   * @return true
   */
  public boolean isUnique()
  {
    return true;
  }

  /**
   * Define a unique key for the annotation which is used for comparisons.
   * @return the unique key.
   */
  public static Object annotationKey()
  {
    return "scale.alias.AliasAnnote";
  }

  /**
   * Return the key for the alias annotation.
   * @return the key for the alias annotation.
   */
  public Object getKey()
  {
    return AliasAnnote.annotationKey();
  }

  /**
   * Return a string representation of the alias annotation.
   */
  public String toStringSpecial()
  {
     return aliasVar.toString();
  }

  /**
   * Return a String suitable for labeling this node in a graphical
   * display.
   */
  public String getDisplayLabel()
  {
    StringBuffer buf = new StringBuffer(super.getDisplayLabel());
    buf.append(' ');
    buf.append(aliasVar.getDisplayLabel());
    return buf.toString();
  }
}
