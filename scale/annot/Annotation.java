package scale.annot;

import java.util.Enumeration;
import java.util.WeakHashMap;

import scale.common.*;
import scale.annot.Support;
import scale.annot.Creator;

/**
 * Annotations are a flexible mechanism for attaching information to
 * certain class instances such as the nodes of a program
 * representation graph.
 * <p>
 * $Id: Annotation.java,v 1.53 2007-08-27 18:37:27 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * An annotation should be used to specify information whose lack will
 * not result in an invalid compilation.  If the information is needed
 * for validity, it should be represented as a member field of a class
 * instead.  <b style="color:red">It should not be possible to reach
 * from the annotation instance to the object that is annotated .</b>
 * <p>
 * The same annotation mechanism is used for both Clef and Score and
 * all classes derived from the {@link scale.common.Root Root} class.
 * <p>
 * Annotations are not an efficient (in either space or time)
 * mechanism for representing a given piece of information, but are
 * flexible enough to represent varied information.
 * <p>
 * Annotations represent varied information.  Some information will be
 * immutable, and so the annotation will not change (i.e., be deleted)
 * over time.  Other information is transitory and will need to be
 * deleted, perhaps even en masse.  The annotation handling
 * mechanism has the following capabilities:
 *   <UL>
 *   <LI> Associates annotations with nodes of a program representation. 
 *   <LI> Efficiently answers the question: Does a node have an
 *        annotation of a given type?
 *   <LI> Supports efficient removal of all annotations of a given
 *        type from the program representation.
 *   <LI> Annotations are unique.
 *   <LI> Allows nodes to have more than one instance of a single
 *        annotation type.
 *   <LI> Hides the implementation of annotations and their supporting
 *        structures enough to allow performance improvements later.
 *   <LI> Enables garbage collection of discarded annotation instances.
 *   </UL>
 * <p>
 * All annotations inherit from the abstract Annotation class.  Each
 * sub-class has a static method annotationKey that returns the key
 * for that class of annotation.  Each sub-class has a static create
 * method that is called to attach the annotation of that type to a
 * node.  This annotation instance may be a new instance or it may be
 * an already existing instance.  The constructor method in every
 * annoation sub-class is private in order to insure that the create
 * method is always used.
 * <p>
 * The create method takes a minimum of three arguments.
 * <ol>
 * <li>The first argument is the node to which the new annotation will
 * be attached.  It is important to use the most restrictive class
 * possible for the type of this argument as it is used by the
 * AnnotationTool to determine the which annotations can be attached
 * to which nodes.
 * <li>The second argument specifies who created the annotation.
 * Annotations carry an indication of where they were created.
 * This is used for debugging and error checking.  The creator is
 * represented by a Creator object which is sub-classed to specify
 * the type of creator.
 * Some sample sub-classes are as follows:
 *   <UL>
 *   <LI>CreatorSource
 *   <LI>CreatorAnnotationTool
 *   <LI>CreatorClef2score
 *   <LI>CreatorclefBuilder
 *   <LI>CreatorAliasAnalysis
 *   </UL>
 * The creator field is immutable.  <li>The third argument specifies
 * the belief in this annotation.  Annotations retain a measure of
 * belief in the accuracy of the annotation.  Annotations maintain
 * separate values for the user's belief and the system's (i.e.,
 * Scale's) belief, and an indication of how to combine the beliefs
 * into a single value.  The representation of belief is intended to
 * support two situations:
 * <ol>
 * <li> Verification of annotations
 * <li> Overriding of annotations (usually by the user) 
 * </ol>
 * For example, an annotation which is known to be absolutely true
 * presumably would not have to be verified.  Also, if a user wants to
 * disable an annotation that the system created, the system's belief
 * may still be used to issue warnings.
 * </ol>
 * Since Scale is not a truth system, it is not
 * overly sophisticated in the use of belief values.  If the combined
 * belief value suggests any level of truth, the annotation should be
 * treated as true (except by annotation verification components).  
 * @see scale.annot.Support
 * @see scale.annot.Creator
 */
public abstract class Annotation implements DisplayNode
{
  private static int createdCount = 0;

  /**
   * This is a list of all the annotations classes known by the Scale
   * system.
   */
  public static final String[] knownAnnotations =
  {
    "scale.clef.PureFunctionAnnotation",
    "scale.score.analyses.AliasAnnote",
  };

  /**
   * A collection of all annotations.  We map from the node to a set
   * of annotations attached to the node.
   */
  private static WeakHashMap<AnnotationInterface, Object> annotations;

  /**
   * The type and level of support for this instance of annotation.
   */
  private Support support;
  /**
   * The creator for this instance of this annotation.
   */
  private Creator creator;
  /**
   * The node id.
   */
  private int id;

  /**
   * All sub-classes should define annotationKey() to return a unique
   * object for the key.  There is no key for the abstract class
   * Annotation.  Currently, this object is the string representing
   * the full class name.
   */
  public static Object annotationKey()
  {
    throw new scale.common.InternalError("annotationKey() called on base Annotation class");
  }

  /**
   * Return the key for this annotation class.
   * Since 
   * <pre>
   *    this.annotationKey()
   * </pre>
   * returns the wrong key when used in a parent class method,
   * each sub-class must implement a method to return its key.
   * <pre>
   *   public Object getKey()
   *   {
   *     return ThisClass.annotationKey();
   *   }
   * </pre>
   */
  public abstract Object getKey();

  /**
   * For display and debugging.
   */
  public final String annotationName()
  {
    String s = getClass().getName();
    return s.substring(s.lastIndexOf('.'), s.length());
  }

  /**
   * Create an annotation.  This constructor should only be called by
   * a sub-class constructor.
   * @param creator indication of what Scale component created this
   * annotation
   * @param support the support for this annotation
   * @see scale.annot.Creator
   * @see scale.annot.Support
   */
  protected Annotation(Creator creator, Support support)
  {
    this.creator = creator;
    this.support = support;
    this.id      = createdCount++;
  }

  /**
   * Associates a node with this annotation.  
   * The caller of this method must always use the method as
   * <pre>
   *    na = annotation.addNode(this, na);
   * </pre>
   * @param n node being added
   */
  public final void addNode(AnnotationInterface n)
  {
    if (annotations == null) {
      annotations = new WeakHashMap<AnnotationInterface, Object>(203);
      annotations.put(n, this);
      return;
    }

    Object na = annotations.get(n);
    if (na == null) {
      na = this;
      annotations.put(n, na);
      return;
    } 

    if (na instanceof Annotation) {
      Annotation a = (Annotation) na;
      if (!a.equals(this)) {
        Vector<Annotation> list = new Vector<Annotation>(2);
        list.addElement(a);
        list.addElement(this);
        na = list;
        annotations.put(n, na);
      }
      return;
    }

    @SuppressWarnings("unchecked")
    Vector<Annotation> list = (Vector<Annotation>) na;
    int                ls   = list.size();
    for (int i = 0; i < ls; i++)
      if (list.elementAt(i).equals(this))
        return;
    list.addElement(this);
  }

  /**
   * Removes an association of a node with all annotations having this
   * key.
   * @param n node being disassociated
   * @param key is the annotation key
   * @exception NoSuchElementException tried to delete a node which is
   * not associated with this annotation.
   */
  public static final void removeNode(AnnotationInterface n, Object key) throws NoSuchElementException
  {
    if (annotations == null)
      return;

    Object na = annotations.get(n);
    if (na == null)
      return;

    if (na instanceof Annotation) {
      if (((Annotation) na).getKey() == key) {
        na = null;
        annotations.put(n, na);
      }
      return;
    }

    @SuppressWarnings("unchecked")
    Vector<Annotation> list = (Vector<Annotation>) na;
    int    ls   = list.size();
    for (int i = ls - 1; i >= 0; i--) {
      Annotation a = list.elementAt(i);
      if (a.getKey() == key)
        list.removeElementAt(i);
    }

    if (list.size() == 0) {
      na = null;
      annotations.put(n, na);
    }
  }

  /**
   * Removes an association of a node with this annotation.  
   * @param n node being disassociated
   * @exception NoSuchElementException tried to delete a node which is
   * not associated with this annotation.
   */
  public final void removeNode(AnnotationInterface n) throws NoSuchElementException
  {
    if (annotations == null)
      return;

    Object na = annotations.get(n);
    if (na == null)
      return;

    if (na instanceof Annotation) {
      if (na.equals(this)) {
        na = null;
        annotations.put(n, na);
      }
      return;
    }

    @SuppressWarnings("unchecked")
    Vector<Annotation> list = (Vector<Annotation>) na;
    int                ls   = list.size();
    for (int i = ls - 1; i >= 0; i--)
      if (list.elementAt(i).equals(this)) {
        list.removeElementAt(i);
        break;
      }

    if (list.size() == 0) {
      na = null;
      annotations.put(n, na);
    }
  }

  /**
   * Return the support of the annotation.
   */
  public final Support getSupport()
  {
    return support;
  }

  /**
   * Return the creator of the annotation.
   */
  public final Creator getCreator()
  {
    return creator;
  }

  /**
   * Returns a flag indicating if a node is permitted to have multiple
   * instances of this annotation type.
   * @return true when a node may not have multiple instances of this
   * annotation type
   */
  public abstract boolean isUnique();

  /**
   * Return true if the annotations are equivalent.  Note - the
   * Creator and the Support are not used in equivalence testing.
   * @param a the other annotation
   */
  public abstract boolean equivalent(Annotation a);

  /**
   * Return true if both annotations have the same key, creator,
   * support, etc.
   * @param o the other annotation
   */
  public final boolean equals(Object o)
  {
    if (o == this)
      return true;

    if (!(o instanceof Annotation))
      return false;

    Annotation a = (Annotation) o;
    if (!equivalent(a))
      return false;

    return (getKey().equals(a.getKey()) &&
            support.equals(a.support) &&
            creator.equals(a.creator));
  }

  public int hashCode()
  {
    return getKey().hashCode() ^ support.hashCode() ^ creator.hashCode();
  }

  /**
   * Return true if the annotation has this creator and this support.
   */
  public final boolean sameSupport(Creator c, Support sup)
  {
    return (c == creator) && (sup == support);
  }

  /**
   * Convert the class name of this node to a string representation.
   */
  public final String toStringClass()
  {
    String c = getClass().toString();
    return c.substring(c.lastIndexOf('.') + 1);
  }

  /**
   * Return a string representation of the annotation.
   */
  public final String toString()
  {
    StringBuffer buf = new StringBuffer("(");
    buf.append(toStringClass());
    buf.append(' ');
    buf.append(creator);
    buf.append(' ');
    buf.append(support);
    buf.append(' ');
    buf.append(this.toStringSpecial());
    buf.append(")");
    return buf.toString();
  }

  /**
   * Return a string representation of the additioanl internal state
   * of the annotation.
   */
  public abstract String toStringSpecial();

  /**
   * Returns a single instance of the annotation with the given key
   * from the annotation container.  This method may return
   * <code>null</code>.
   * @param akey the annotation key
   * @param n is the node with annotations
   * @return an arbitrary annotation of the indicated kind or
   * <code>null</code>
   */
  public final static Annotation getAnnotation(Object akey, AnnotationInterface n)
  {
    if (annotations == null)
      return null;

    Object na = annotations.get(n);
    if (na != null) {
      if (na instanceof Annotation) {
        Annotation a = (Annotation) na;
        if (a.getKey().equals(akey))
          return a;
      } else {
        @SuppressWarnings("unchecked")
        Vector<Annotation> list = (Vector<Annotation>) na;
        int                ls   = list.size();
        for (int i = 0; i < ls; i++) {
          Annotation a = list.elementAt(i);
          if (a.getKey().equals(akey))
            return a;
        }
      }
    }
    return null; 
  }

  /**
   * Test if the indicated annotation kind is in the container.
   * @param akey the annotation key
   * @param n is the node with annotations
   * @return true if the indicated kind of annotation is in the
   * annoation container
   */
  public final static boolean hasAnnotation(Object akey, AnnotationInterface n)
  {
    return (null != getAnnotation(akey, n));
  }

  /**
   * Test if the equivalent annotation is in the container.  Note -
   * the {@link Creator Creator} and {@link Support Support} are not
   * used in equivalence testing.
   * @param a is the annotation
   * @param n is the node with annotations
   * @return true if an equivalent annotation is in the annoation
   * container
   */
  public final static boolean hasEqualAnnotation(Annotation a, AnnotationInterface n)
  {
    if (annotations == null)
      return false;

    Object na = annotations.get(n);
    if (na != null) {
      if (na instanceof Annotation) {
        Annotation ta = (Annotation) na;
        return ta.equivalent(a);
      } else {
        @SuppressWarnings("unchecked")
          Vector<Annotation> list = (Vector<Annotation>) na;
        int                ls   = list.size();
        for (int i = 0; i < ls; i++) {
          Annotation ta = list.elementAt(i);
          if (ta.equivalent(a))
            return true;
        }
      }
    }
    return false;
  }

  /**
   * Return an enumeration of all annotations in the annotation
   * container.
   * @param n is the node with annotations
   * @return an enumeration of all the annotations in the annotation
   * container
   */
  public final static Enumeration<Annotation> allAnnotations(AnnotationInterface n)
  {
    if (annotations == null)
      return new EmptyEnumeration<Annotation>();

    Object na = annotations.get(n);
    if (na == null)
      return new EmptyEnumeration<Annotation>();
    if (na instanceof Annotation)
      return new SingleEnumeration<Annotation>((Annotation) na);
      @SuppressWarnings("unchecked")
      Vector<Annotation> list = (Vector<Annotation>) na;
    return list.elements();
  }

  /**
   * Return an enumeration of all annotations with the specified key
   * in the annotation container.
   * @param akey the annotation key
   * @param n is the node with annotations
   * @return an enumeration of all the annotations of the indicated
   * kind in the annotation container
   */
  public final static Enumeration<Annotation> allMatchingAnnotations(Object akey, AnnotationInterface n)
  {
    if (annotations == null)
      return new EmptyEnumeration<Annotation>();

    Object na = annotations.get(n);
    if (na != null) {
      if (na instanceof Annotation) {
        Annotation a = (Annotation) na;
        if (a.getKey().equals(akey))
          return new SingleEnumeration<Annotation>(a);
      } else {
        @SuppressWarnings("unchecked")
        Vector<Annotation> list  = (Vector<Annotation>) na;
        int    len   = list.size();
        Object o1    = null;
        Object o2    = null;
        int    count = 0;
        for (int i = 0; i < len; i++) { // In most cases there are no more than two annotations on a node.
          Annotation a = list.elementAt(i);
  
          if (a.getKey().equals(akey)) {
            o1 = o2;
            o2 = a;
            count++;
          }
        }
        switch (count) {
        case 0: return new EmptyEnumeration<Annotation>();
        case 1: return new SingleEnumeration<Annotation>((Annotation) o2);
        case 2: return new DoubleEnumeration<Annotation>((Annotation) o1, (Annotation) o2);
        default: break;
        }
        Vector<Annotation> nl = new Vector<Annotation>(count);
        for (int i = 0; i < len; i++) {
          Annotation a = list.elementAt(i);
          if (a.getKey().equals(akey))
            nl.addElement(a);
        }
        return nl.elements();
      }
    }
    return new EmptyEnumeration<Annotation>();
  }

  /**
   * Convert the container of annotations to a string representation.
   * @param n is the node with annotations
   */
  public final static String toStringAnnotations(AnnotationInterface n)
  {
    Object       na = annotations.get(n);
    StringBuffer s  = new StringBuffer();
    if (na != null) {
      s.append(" {");
      if (na instanceof Annotation) {
        s.append(na);
      } else {
        @SuppressWarnings("unchecked")
        Vector<Annotation> list = (Vector<Annotation>) na;
        int    ls   = list.size();
        for (int i = 0; i < ls; i++) {
          if (i > 0)
            s.append(' ');
          s.append(list.elementAt(i));
        }
      }
      s.append("}");
    }
    return s.toString();
  }

  /**
   * Return the node label.
   */
  public int getNodeID()
  {
    return hashCode();
  }

  /**
   * Return a unique name suitable for display.
   */
  public String getDisplayName()
  {
    return "A" + id;
  }

  /**
   * Return a String suitable for labeling this node in a graphical
   * display.
   */
  public String getDisplayLabel()
  {
    String mnemonic = toStringClass();
    if (mnemonic.endsWith("Annote"))
      mnemonic = mnemonic.substring(0, mnemonic.length() - 6);
    else if (mnemonic.endsWith("Annotation"))
      mnemonic = mnemonic.substring(0, mnemonic.length() - 10);
    return mnemonic;
  }

  /**
   * Return a String specifying the color to use for coloring this
   * node in a graphical display.
   */
  public DColor getDisplayColorHint()
  {
    return DColor.ORANGE;
  }

  /**
   * Return a String specifying a shape to use when drawing this node
   * in a graphical display.
   */
  public DShape getDisplayShapeHint()
  {
    return DShape.BOX;
  }

  /**
   * Remove all annotations from static lookup tables.
   */
  public static void removeAllAnnotationTables()
  {
    if (annotations != null)
      annotations.clear();
  }
}
