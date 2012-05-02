package scale.clef.decl;

import java.util.StringTokenizer;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;

/** 
 * This class is used to represent Fortran statement functions.
 * <p> 
 * $Id: StmtFtnDecl.java,v 1.3 2007-10-04 19:58:04 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public final class StmtFtnDecl extends Declaration
{
  private static final String delims = ".()-+=[]*&^%$#@!~{};:'\",<>?/ \t\n\r\f";

  private String[] parameters; // The set of actual parameters.
  private String[] stmt;       // The statement function as a set of tokens.

  /**
   * Create a new StmtFtnDecl.
   * @param type is unused
   * @param name is the name of the statement function
   * @param parameters is the set of statement function parameter names
   * @param stmt is the text of the statement function
   */
  public StmtFtnDecl(Type type, String name, Vector<String> parameters, String stmt)
  {
    super(name, type);

    int lp = parameters.size();
    this.parameters = new String[lp];
    for (int i = 0; i < lp; i++)
      this.parameters[i] = parameters.get(i);

    StringTokenizer st = new StringTokenizer(stmt, delims, true);
    Vector<String>  v  = new Vector<String>();
    while (st.hasMoreTokens())
      v.add(st.nextToken());

    int lv = v.size();
    this.stmt = new String[lv];
    for (int i = 0; i < lv; i++)
      this.stmt[i] = v.get(i);
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("(StmtFtn: ");
    buf.append(getName());
    buf.append("(");
    for (int i = 0; i < parameters.length; i++) {
      if (i > 0)
        buf.append(',');
      buf.append(parameters[i]);
    }
    buf.append(")=(");
    for (int i = 0; i < stmt.length; i++)
      buf.append(stmt[i]);

    buf.append("))");
    return buf.toString();
  }

  /**
   * Return the statement function with the parameters replaced with
   * the actual parameters.
   * @param args is the set of actual parameters
   */
  public String substitute(Vector<String> args)
  {
    int lp = parameters.length;
    int la = args.size();
    if (lp != la)
      return null;

    String[] v   = stmt.clone();
    int      lv  = v.length;

    for (int i = 0; i < lp; i++) {
      String parm = parameters[i];
      String arg  = '(' + args.get(i) + ')';

      for (int j = 0; j < lv; j++) {
        if (v[j].equals(parm))
          v[j] = arg;
      }
    }

    StringBuffer buf = new StringBuffer("(");
    for (int i = 0; i < lv; i++)
      buf.append(v[i]);

    buf.append(')');
    buf.append('\0');

    return buf.toString();
  }

  /**
   * Return a copy of this Declaration but with a different name.
   * THis method causes an internal error if called.
   */
  public Declaration copy(String name)
  {
    throw new scale.common.InternalError("Can't copy a StmtFtnDecl.");
  }

  public final boolean isStmtFtnDecl()
  {
    return true;
  }

  public final StmtFtnDecl returnStmtFtnDecl()
  {
    return this;
  }
}
