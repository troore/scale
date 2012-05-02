package scale.annot;

import java.util.Enumeration;
import scale.common.Vector;

/**
 * This class represents the support level for an annotation.
 * <p>
 * $Id: Support.java,v 1.24 2007-08-13 12:32:02 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * A particular support for annotations.
 * <P>
 * The belief values and combination rule are specified using
 * enumerations, which are as follows:
 * <UL>
 *   <LI><B>Belief:</B> This value represents the strength of
 *         belief.  Both user and system beliefs are specified with
 *         this enumeration. 
 *         <UL>
 *           <LI><CODE>false</CODE>
 *           <LI><CODE>improbable</CODE>
 *           <LI><CODE>unlikely</CODE>
 *           <LI><CODE>neutral</CODE>
 *           <LI><CODE>possible</CODE>
 *           <LI><CODE>probable</CODE>
 *           <LI><CODE>true</CODE>
 *         </UL>
 *         The value <CODE>neutral</CODE> expresses no belief,
 *         positive or negative, and is therefore identical to the
 *         annotation not existing.  For many annotations, the absence
 *         of an annotation implies a value; hence in these cases,
 *         <CODE>neutral</CODE> will be equivalent to one of false or
 *         true.  To make dealing with this unfortunate situation
 *         easier, I would like the elements of the enumeration to
 *         have a method that returns an integer value (e.g., value()
 *         or order()), so that we can do something like the
 *         following:
 *   <PRE>
 *     if (ann.belief().value() <= cNeutral)
 *         // annotation is false.
 *     else
 *         // annotation is true.
 *   </PRE>
 *         False is less than true, and the values are ordered
 *         as shown above.  Generally, <CODE>neutral</CODE> is used to
 *         indicate that an annotation is to be ignored.  
 *         <P>
 *         For some annotations, beliefs do not make sense.  For example,
 *         an annotation indicating originating source position is
 *         inherently true.  Nevertheless, they will be assigned beliefs.
 *         </P>
 *   <LI><B>Combining rule:</B>
 *         The combining rule specifies how the user and system belief
 *         values will be combined.  
 *         <UL>
 *           <LI><CODE>cStrongestSystem:</CODE> Use whichever value
 *           represents the strongest belief.  In the case of a tie,
 *           use the system's value.  Strength is the distance from
 *           <CODE>neutral</CODE>
 *           <LI><CODE>cStrongestUser:</CODE> Use whichever value
 *           represents the strongest belief.  In the case of a tie,
 *           use the user's value.
 *           <LI><CODE>cSystem:</CODE> Use the system's value.
 *           <LI><CODE>cUser:</CODE> Use the user's value
 *           <LI><CODE>cOptismistic:</CODE> Return whichever value
 *           reprsents the more optimistic belief.
 *           <LI><CODE>cPessimistic:</CODE> Return whichever value
 *           reprsents the more pessimistic belief.
 *         </UL>
 *         <CODE>cStrongestSystem</CODE> is the default rule, and is
 *         automatically set by the annotation constructor.  The
 *         combination rule field is mutable, and may be changed to 
 *         something other than the default by calling a method of the
 *         annotation class.
 * </UL>
 * This table lists some sample situations and the appropriate
 * settings for the belief and combination rule fields.  The
 * combination rule should always be the default, unless otherwise
 * specified.
 * 
 * <TABLE COLS=4 BORDER=3>
 * <THEAD>
 *   <TR>
 *     <TD ALIGN=center VALIGN=top>
 *       <B>Situation</B>
 *     </TD>
 *     <TD ALIGN=center VALIGN=top>
 *       <B>User belief</B>
 *     </TD>
 *     <TD ALIGN=center VALIGN=top>
 *       <B>System belief</B>
 *     </TD>
 *     <TD ALIGN=center VALIGN=top>
 *       <B>Combining rule</B>
 *     </TD>
 *   </TR>
 * </THEAD>
 * 
 * <TBODY>
 *   <TR>
 *     <TD ALIGN=center VALIGN=top>User creates annotation
 *     </TD>
 *     <TD ALIGN=center VALIGN=top>possible
 *     </TD>
 *     <TD ALIGN=center VALIGN=top>neutral
 *     </TD>
 *     <TD ALIGN=center VALIGN=top><I>default</I>
 *     </TD>
 *   </TR>
 * 
 *   <TR>
 *     <TD ALIGN=center VALIGN=top>Annotation derived from source
 *         language construct
 *     </TD>
 *     <TD ALIGN=center VALIGN=top>neutral
 *     </TD>
 *     <TD ALIGN=center VALIGN=top>true
 *     </TD>
 *     <TD ALIGN=center VALIGN=top><I>default</I>
 *     </TD>
 *   </TR>
 * 
 *   <TR>
 *     <TD ALIGN=center VALIGN=top>Annotation from an accurate analysis
 *     </TD>
 *     <TD ALIGN=center VALIGN=top>neutral
 *     </TD>
 *     <TD ALIGN=center VALIGN=top>true
 *     </TD>
 *     <TD ALIGN=center VALIGN=top><I>default</I>
 *     </TD>
 *   </TR>
 * 
 *   <TR>
 *     <TD ALIGN=center VALIGN=top>Annotation from a conservative analysis
 *     </TD>
 *     <TD ALIGN=center VALIGN=top>neutral
 *     </TD>
 *     <TD ALIGN=center VALIGN=top>probable
 *     </TD>
 *     <TD ALIGN=center VALIGN=top><I>default</I>
 *     </TD>
 *   </TR>
 * 
 *   <TR>
 *     <TD ALIGN=center VALIGN=top>User overrides annotation created by system
 *     </TD>
 *     <TD ALIGN=center VALIGN=top>neutral
 *     </TD>
 *     <TD ALIGN=center VALIGN=top><I>unchanged</I>
 *     </TD>
 *     <TD ALIGN=center VALIGN=top>user
 *     </TD>
 *   </TR>
 * 
 *   <TR>
 *     <TD ALIGN=center VALIGN=top>System choses to drop a user annotation
 *     </TD>
 *     <TD ALIGN=center VALIGN=top><I>unchanged</I>
 *     </TD>
 *     <TD ALIGN=center VALIGN=top>neutral
 *     </TD>
 *     <TD ALIGN=center VALIGN=top>system
 *     </TD>
 *   </TR>
 * </TBODY>
 * </TABLE>
 * @see scale.annot.Annotation
 */
public class Support
{
  /**
   * Annotations retain a measure of belief in the accuracy of the
   * annotation.  Annotations maintain separate values for the user's
   * belief and the system's (i.e., Scale's) belief, and an indication
   * of how to combine the beliefs into a single value.  The
   * representation of belief is intended to support two situations:
   * <ol>
   * <li> Verification of annotations
   * <li> Overriding of annotations (usually by the user)
   * </ol>
   * For example, an annotation which is known to be absolutely true
   * presumably would not have to be verified.  Also, if a user wants
   * to disable an annotation that the system created, the system's
   * belief may still be used to issue warnings.
   * <p>
   * We will not be overly sophisticated in our use of belief values.
   * If the combined belief value suggests any level of truth, the
   * annotation should be treated as true (except by annotation
   * verification components).
   * <p>
   * The belief values and combination rule are specified using
   * enumerations.
   * <p>
   * The value cNeutral expresses no belief, positive or negative, and
   * is therefore identical to the annotation not existing. For many
   * annotations, the absence of an annotation implies a value; hence
   * in these cases, neutral will be equivalent to one of false or
   * true. To make dealing with this unfortunate situation easier, the
   * elements of the enumeration to have a method that returns an
   * integer value so that we can do something like the following:
   * <pre>
   *         if (ann.belief().value() <= cNeutral)
   *             // annotation is false.
   *         else
   *             // annotation is true.
   * </pre>
   * False is less than true, and the values are ordered. Generally,
   * neutral is used to indicate that an annotation is to be ignored.
   * <p>
   * Even though we do not currently have any annotations which can
   * reasonably take a negative value, we have chosen to retain the
   * negative half of the belief scale, so that the system can
   * contradict the user (e.g., if the system disproves an annotation).
   */
  public enum Belief
  {
    /**
     * The annotation is believed to be false.
     */
    False     (-3),
    /**
     * The annotation is believed to be most probably false.
     */
    Improbable(-2),
    /**
     * The annotation is believed to be probably false.
     */
    Unlikely  (-1),
    /**
     * The value neutral expresses no belief, positive or negative, and
     * is therefore identical to the annotation not existing.
     */
    Neutral   (0),
    /**
     * The annotation is believed to be probably true.
     */
    Possible  (1),
    /**
     * The annotation is believed to be most probably true.
     */
    Probable  (2),
    /**
     * The annotation is believed to be true.
     */
    True      (3);

    private int value;

    private Belief(int value)
    {
      this.value = value;
    }

    public int value()
    {
      return value;
    }
  }

  /**
   * System belief is true.
   */
  public  static final Support systemTrue = Support.create(false, Belief.True);
  private static Vector<Support> supports;

  /**
   * The user belief.
   */
  private Belief user;
  /**
   * The system belief.
   */
  private Belief system;

  /**
   * The combining rule specifies how the user and system belief
   * values will be combined.
   */
  public enum Rule
  {
    /**
     * Use whichever value represents the strongest belief. In the case
     * of a tie, use the system's value. Strength is the distance from
     * neutral
     */
    StrongestSystem(),
    /**
     * Use whichever value represents the strongest belief. In the case
     * of a tie, use the user's value.
     */
    StrongestUser(),
    /**
     * Use the system's value. 
     */
    System(),
    /**
     * Use the user's value 
     */
    User(),
    /**
     * Use whichever value represents the more optimistic belief.
     */
    Optimistic(),
    /**
     * Use whichever value represents the more pessimistic belief. 
     */
    Pessimistic();
  }

  /**
   * The rule specifying how to combine user and system beliefs.
   */
  private Rule rule;

  private Support(Belief user, Belief system, Rule rule)
  {
    this.user = user;
    this.system = system;
    this.rule = rule;
    if (supports == null)
      supports = new Vector<Support>(4);
    supports.addElement(this);
  }

  /**
   * Re-use an existing instance of a particular support.
   * If no equivalent support exists, create a new one.
   */
  public static Support create(Belief user, Belief system, Rule rule)
  {
    if (supports != null) {
      Enumeration<Support> e = supports.elements();
      while (e.hasMoreElements()) {
        Support s = e.nextElement();
        if ((s.user == user) &&
            (s.system == system) &&
            (s.rule == rule))
          return s;
      }
    }
    return new Support(user, system, rule);
  }

  public static Support create(boolean isUser, Belief belief)
  {
    Support sup;
    if (isUser) {
      sup = Support.create(belief, Belief.Neutral, Rule.StrongestSystem);
    } else {
      sup = Support.create(Belief.Neutral, belief, Rule.StrongestSystem);
    }
    return sup;
  }

  /**
   * Combine two supports.
   * @return the new Support
   */
  public Support combine(Support a)
  {
    throw new scale.common.NotImplementedError("Combining of supports not yet implemented.");
  }

  /**
   * Return the rule used in the support.
   */
  public final Rule getRule()
  {
    return rule;
  }

  /**
   * Return the user belief used in the support.
   */
  public final Belief getUserBelief()
  {
    return user;
  }

  /**
   * Return the user system used in the support.
   */
  public final Belief getSystemBelief()
  {
    return system;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("(Support ");
    buf.append(user);
    buf.append(' ');
    buf.append(system);
    buf.append(' ');
    buf.append(rule);
    buf.append(")");
    return buf.toString();
  }
}
