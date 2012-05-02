package scale.annot;

import scale.annot.Annotation;
import java.util.Enumeration;

/**
 * Defins the operations required of a class in order to use annotations.
 * <p>
 * $Id: AnnotationInterface.java,v 1.19 2007-08-13 12:32:02 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Each node contains a container of annotations that have been
 * associated with it.  For the key to retrieve annotations, we use
 * the value returned by the static method {@link
 * Annotation#annotationKey Annotation.annotationKey()}.
 * <p>
 * The goal of this interface is to make sure that details about how
 * annotations are actually managed stay hidden.  The interface
 * provides only very general container operations, plus a few
 * specialized routines unique to annotations.
 * @see scale.annot.Annotation
 */

public interface AnnotationInterface
{
  /**
   * Adds an annotation to this node's annotation list.
   * Redundant annotations are permitted.
   */
  public void addAnnotation(Annotation a);
  /**
   * Delete all annotations which match the key of the given
   * Annotation.
   */
  public void removeAnnotation(Annotation annotation);
  /**
   * Returns a single instance of the annotation with the given key.
   * This method may return null.
   * @param annotationKey the annotation key
   * @return an arbitrary annotation of the indicated kind or null.
   */
  public Annotation getAnnotation(Object annotationKey);
  /**
   * @param annotationKey the annotation key
   * @return true if the indicated kind of annotation is associated
   * with the node.
   */
  public boolean hasAnnotation(Object annotationKey);
  /**
   * Return true if an equivalent annotation is associated with the
   * node.  Note - the Creator, Beliefs, and the Combining Rule are
   * not used in equivalence testing.
   * @param annotationKey the annotation key
   */
  public boolean hasEqualAnnotation(Annotation annotationKey);
  /**
   * Return an enumeration of all the annotations associated with this
   * node.
   */
  public Enumeration<Annotation> allAnnotations();
  /**
   * Return an enumeration of all the annotations of the given kind
   * associated with this node.
   */
  public Enumeration<Annotation> allMatchingAnnotations(Object annotationKey);
}
