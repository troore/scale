package scale.clef;

/**
 * The predicate for the visit pattern on Clef AST nodes.
 * <p>
 * $Id: Predicate.java,v 1.36 2005-02-07 21:27:48 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * A traversal predicate has a method per instantiable subclass of
 * a Clef node.  Each method represents an action to be done when
 * visiting a node of that type during a traversal of a program
 * representation.  Predicates allow logically related code to be
 * grouped together (in an implementation of the predicate).
 * @see Node#visit
 * @see scale.clef.DeclPredicate
 * @see scale.clef.TypePredicate
 * @see scale.clef.StmtPredicate
 * @see scale.clef.ExprPredicate
 */

public interface Predicate extends
        scale.clef.DeclPredicate,
        scale.clef.TypePredicate,
        scale.clef.StmtPredicate,
        scale.clef.ExprPredicate
{
  public void visitNode(Node n);
}
