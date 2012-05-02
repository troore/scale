package scale.frontend.fortran;

import java.util.Iterator;

import scale.common.*;
import scale.clef.LiteralMap;
import scale.clef.expr.*;
import scale.clef.type.*;
import scale.clef.decl.*;
import scale.clef.stmt.*;

/** 
 * This class is used to process the Fortran implied-do constructs.
 * <p>
 * $Id: ImpliedDo.java,v 1.12 2007-10-04 19:58:13 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class ImpliedDo
{
  private F95          f95;
  private VariableDecl indexVar; // The iteration variable.
  private Expression   init;     // The initial value for the iteration variable.
  private Expression   limit;    // The end value for the iteration variable.
  private Expression   step;     // The increment.
  private Literal[]    data;     // The list of values to be used.
  private long[]       repeats;  // How many times an individual value is used.
  private int          dataPtr;  // How many elements in data.
  private int          lineno;   // Source line causing this implied do.

  private Vector<Object>        items;    // The list of items to be initialized.
  private HashSet<VariableDecl> vars;     // The set of initialized variables.

  /**
   * @param f95 is the parser
   * @param lineno is the source line number causing this implied do
   */
  public ImpliedDo(F95 f95, int lineno)
  {
    this.f95     = f95;
    this.items   = new Vector<Object>();
    this.dataPtr = 0;
    this.vars    = new HashSet<VariableDecl>();
    this.lineno  = lineno;
  }

  /**
   * Add an expression to be iterated over.  It is usually a {@link
   * scale.clef.expr.SubscriptValueOp SubscriptValueOp} instance.
   */
  public void add(Expression exp)
  {
    items.add(exp);
  }

  /**
   * Add another implied-do to be iterated over.  This occurs when two
   * index variables are required as in
   * <code>((A(I,J),1,10),1,5)</code>.
   */
  public void add(ImpliedDo id)
  {
    items.add(id);
  }

  /**
   * Specify the implied do loop.
   * @param indexVar is the iteration variable
   * @param init is the initial value for the iteration variable
   * @param limit is the end value for the iteration variable
   * @param step is the increment
   */
  public void set(VariableDecl indexVar,
                  Expression   init,
                  Expression   limit,
                  Expression   step) throws InvalidException
  {
    if ((indexVar != null) && !indexVar.getCoreType().isIntegerType())
      throw new InvalidException("do-i-variable must be integer");
    if (!init.getCoreType().isIntegerType())
      throw new InvalidException("do-i-variable must be integer");
    if (!limit.getCoreType().isIntegerType())
      throw new InvalidException("do-i-variable must be integer");
    if (!step.getCoreType().isIntegerType())
      throw new InvalidException("do-i-variable must be integer");

    this.indexVar = indexVar;
    this.init     = init;
    this.limit    = limit;
    this.step     = step;
  }

  /**
   * Record a data item for use in DATA statements.
   */
  public void addData(Literal value, long repeat)
  {
    if (data == null) {
      data    = new Literal[20];
      repeats = new long[20];
    } else if (dataPtr >= data.length) {
      Literal[] nd = new Literal[dataPtr * 2];
      System.arraycopy(data, 0, nd, 0, dataPtr);
      data = nd;
      long[] nr = new long[dataPtr * 2];
      System.arraycopy(repeats, 0, nr, 0, dataPtr);
      repeats = nr;
    }

    data[dataPtr] = value;
    repeats[dataPtr] = repeat;
    dataPtr++;
  }

  /**
   * Initialize the variables collected with the data collected.
   * @return false if an error was encountered
   */
  public boolean initVariables(boolean allowEqVars)
  {
    try {
      initVariables(data, repeats, 0, dataPtr, allowEqVars, 0);
      vars = null;
      return true;
    } catch (InvalidException ex) {
      assert display(true, 0, "*** EX " + ex.getMessage(), null);
      ex.printStackTrace();
      return false;
    }
  }

  private final boolean trace2 = false;

  private boolean display(boolean trace, int indent, String msg, Vector<Object> elements)
  {
    if (!trace)
      return true;

    for (int i = 0; i < indent; i++)
      System.out.print("  ");
    System.out.println(msg);

    if (elements == null)
      return true;

    int l = elements.size();
    for (int i = 0; i < l; i++)
      display(trace, indent + 1, " " + i + " " + elements.get(i), null);

    return true;
  }

  private int initVariables(Literal[] data,
                            long[]    repeats,
                            int       dindex,
                            int       dlimit,
                            boolean   allowEqVars,
                            int indent) throws InvalidException
  {
    int        start = 1;
    int        lim   = 1;
    int        inc   = 1;
    Type       ivt   = null;
    IntLiteral val   = null;
    Expression sv    = null;

    assert display(trace2,
                   indent,
                   "** DO i:" + dindex + " l:" + dlimit + " r:" + repeats[dindex] + " " + data[dindex],
                   null);

    if (indexVar != null) {
      start = getExpValue(init);
      lim   = getExpValue(limit);
      inc   = getExpValue(step);

      // We play games with this literal so that we don't create
      // thousands of them.

      val = new IntLiteral(indexVar.getCoreType(), 0);
      sv  = indexVar.getValue();

      indexVar.setValue(val);

      // The index variable must be seen as const for the
      // getConstantValue() methods to work as we want.

      if (!indexVar.isConst()) {
        ivt = indexVar.getType();
        indexVar.setType(RefType.create(ivt, RefAttr.Const));
      }
    }

    try {
      for (int i = start; i <= lim; i += inc) {
        if (val != null)
          val.setValue(i);
        assert display(trace2, indent, "      i:" + i, null);

        dindex = initVars(data, repeats, dindex, dlimit, allowEqVars, indent + 1);
      }

      return dindex;

    } finally {
      if (ivt != null)
        indexVar.setType(ivt);
      if (sv != null)
        indexVar.setValue(sv);
    }
  }

  private int doIdAddressOp(IdAddressOp op,
                            long        offset,
                            Literal[]   data,
                            long[]      repeats,
                            int         dindex,
                            int         dlimit, 
                            boolean     allowEqVars,
                            int         indent) throws InvalidException
  {

    VariableDecl vd = (VariableDecl) op.getDecl();

    if (vd.isEquivalenceDecl()) {
      if (!allowEqVars)
        throw new InvalidException("Can't init COMMON variables here.");
      EquivalenceDecl ed = (EquivalenceDecl) vd;
      if (F95.blankCommonName.equals(ed.getBaseVariable().getName()))
        throw new InvalidException("Can't init blank COMMON variables.");
    }

    Type type = vd.getType();
    long num  = type.getCoreType().numberOfElements();

    if (num <= 0)
      return dindex;

    int n = Integer.MAX_VALUE;
    if (num < n)
      n = (int) num;

    if (n == 1) {
      addElement(vd, offset, data[dindex], indent);
      repeats[dindex]--;
      if (repeats[dindex] <= 0)
        dindex++;
      return dindex;
    }

    Expression     values   = vd.getValue();
    Vector<Object> elements = null;

    if (values != null) {
      if (values instanceof AggregationElements) {
        elements = ((AggregationElements) values).getElementVector();
      } else {
        elements = new Vector<Object>(n  + 1);
        elements.add(fixData(type, values));
        values = new AggregationElements(type, elements);
        vd.setValue(values);
      }
    } else {
      elements = new Vector<Object>(n);
      values = new AggregationElements(type, elements);
      vd.setValue(values);
      vars.add(vd);
    }

    for (int k = 0; k < n; k++) {
      while ((repeats[dindex] > 1) && (repeats[dindex] <= (n - k)) && (k < n)) {
        if (dindex >= dlimit)
          throw new InvalidException("too few data values.");
        elements.add(new PositionRepeatOp((int) repeats[dindex]));
        elements.add(fixData(type, data[dindex]));
        k += repeats[dindex];
        repeats[dindex] = 0;
        dindex++;
      }

      if (k >= n)
        break;

      if (dindex >= dlimit)
        throw new InvalidException("too few data values.");

      elements.add(fixData(type, data[dindex]));
      repeats[dindex]--;
      if (repeats[dindex] <= 0)
        dindex++;
    }

    return dindex;
  }

  /**
   * Initialize the variables in the items list for one value of the
   * implied-do index.
   */
  private int initVars(Literal[] data,
                       long[]    repeats,
                       int       dindex,
                       int       dlimit,
                       boolean   allowEqVars,
                       int       indent) throws InvalidException
  {
    assert display(trace2, indent, "      iv " + indexVar, null);

    int l = items.size();
    for (int j = 0; j < l; j++) {
      if (dindex >= dlimit)
        throw new InvalidException("too few data values.");

      Object o = items.get(j);
      assert display(trace2, indent, "           it " + o, null);
      assert display(trace2, indent, "            i:" +
                     dindex +
                     " r:" +
                     repeats[dindex] +
                     " " +
                     data[dindex], null);

      if (o instanceof IdAddressOp) {
        dindex = doIdAddressOp((IdAddressOp) o,
                               0,
                               data,
                               repeats,
                               dindex,
                               dlimit,
                               allowEqVars,
                               indent);
        continue;
      }

      if (o instanceof SubscriptOp) {
        SubscriptOp subop  = (SubscriptOp) o;
        long        offset = subop.getConstantIndex();
        Expression  array  = subop.getArray();

        if ((offset < 0) || !(array instanceof IdAddressOp))
          throw new InvalidException("invalid array reference");

        VariableDecl vd = (VariableDecl) ((IdAddressOp) array).getDecl();
        ArrayType    at = vd.getCoreType().returnArrayType();
        if (at == null)
          throw new InvalidException("invalid array reference");
        Type et = at.getElementType().getCoreType();
        Type dt = data[dindex].getCoreType();

        boolean invalid = false;
        if (!et.equivalent(dt)) {
          invalid = true;
          FortranCharType fct1 = et.getCoreType().returnFortranCharType();
          FortranCharType fct2 = dt.getCoreType().returnFortranCharType();
          if (fct1 != null)
            invalid = (fct2 == null) || (fct1.getLength() < fct2.getLength());
          else if (et.isRealType())
            invalid = !dt.isRealType();
        }
        if (invalid)
          throw new InvalidException("invalid array reference " +
                                     et +
                                     "\n   " +
                                     data[dindex].getCoreType());

        if (dindex >= dlimit)
          throw new InvalidException("too few data values.");

        addElement(vd, offset, data[dindex], indent);

        repeats[dindex]--;
        if (repeats[dindex] <= 0)
          dindex++;

        continue;
      }

      if (o instanceof AdditionOp) {
        AdditionOp aop = (AdditionOp) o;
        Expression la = aop.getExpr1();
        Expression ra = aop.getExpr2().getConstantValue();

        if (la instanceof TypeConversionOp) {
          TypeConversionOp tc = (TypeConversionOp) la;
          if (tc.getConversion() == CastMode.CAST)
            la = tc.getExpr();
        }

        if ((la instanceof IdAddressOp) && (ra instanceof IntLiteral)) {
          dindex = doIdAddressOp((IdAddressOp) la,
                                 ((IntLiteral) ra).getLongValue(),
                                 data,
                                 repeats,
                                 dindex,
                                 dlimit,
                                 allowEqVars,
                                 indent);
          continue;
        }
      }

      if (o instanceof ImpliedDo) {
        ImpliedDo id = (ImpliedDo) o;
        dindex = id.initVariables(data, repeats, dindex, dlimit, allowEqVars, indent);
        continue;
      }

      throw new InvalidException("invalid data reference " + o);
    }

    return dindex;
  }

  private Literal fixData(Type type, Expression data) throws InvalidException
  {
    Literal lit = data.getConstantValue();
    if ((lit == Lattice.Bot) || (lit == Lattice.Top))
      throw new InvalidException("not a constant");

    if (type.getCoreType() == lit.getCoreType())
      return lit;

    if (lit instanceof FloatLiteral) {
      if (type.isIntegerType())
        lit = LiteralMap.put((long) ((FloatLiteral) lit).getDoubleValue(), type);
      else if (type.isFloatType())
        lit =  LiteralMap.put(((FloatLiteral) lit).getDoubleValue(), type);
      return lit;
    }

    if (lit instanceof IntLiteral) {
      if (type.isRealType())
        lit = LiteralMap.put((double) ((IntLiteral) lit).getLongValue(), type);
      return lit;
    }

    if (lit instanceof StringLiteral) {
      ArrayType at = type.getCoreType().returnArrayType();
      if (at != null)
        type = at.getElementType();

      int lt = f95.getStringLength(type.getCoreType());
      if (lt < 0)
        return lit;

      StringLiteral sl = (StringLiteral) lit;
      int           ls = sl.getString().length();

      if (lt < ls)
        lit = LiteralMap.put(sl.getString().substring(0, lt), FortranCharType.create(lt));
      else if (lt > ls) {
        String st = sl.getString();
        do {
          st = st + " ";
          ls++;
        } while (lt > ls);
        lit = LiteralMap.put(st, FortranCharType.create(lt));
      }

      return lit;
    }

    if (lit instanceof CharLiteral) {
      ArrayType at = type.getCoreType().returnArrayType();
      if (at != null)
        type = at.getElementType();

      int lt = f95.getStringLength(type.getCoreType());
      if (lt < 0)
        return lit;

      String st = String.valueOf(((CharLiteral) lit).getCharacterValue());
      int    ls = 1;

      if (lt < ls)
        lit = LiteralMap.put(st.substring(0, lt), FortranCharType.create(lt));
      else if (lt > ls) {
        do {
          st = st + " ";
          ls++;
        } while (lt > ls);
        lit = LiteralMap.put(st, FortranCharType.create(lt));
      }

      return lit;
    }

    if (lit instanceof ComplexLiteral) {
      ComplexLiteral cl = (ComplexLiteral) lit;
      if (type.isComplexType())
        return new ComplexLiteral(type, cl.getReal(), cl.getImaginary());
    }

    return lit;
  }

  private void addElement(VariableDecl vd, long offset, Literal data, int indent) throws InvalidException
  {
    Expression values = vd.getValue();
    Type       type   = vd.getCoreType();

    vars.add(vd);

    data = fixData(type, data);

    Vector<Object> elements = null;
    if (values == null) {
      if ((offset == 0) && !type.isArrayType()) {
        assert display(trace2, indent, "        6  " + data, null);
        vd.setValue(data);
        return;
      }

      elements = new Vector<Object>();
      values = new AggregationElements(vd.getType(), elements);
      vd.setValue(values);
    } else if (values instanceof AggregationElements)
      elements = ((AggregationElements) values).getElementVector();
    else {
      elements = new Vector<Object>();
      elements.add(values);
      values = new AggregationElements(vd.getType(), elements);
      vd.setValue(values);
    }

    int l = elements.size();
    if (l == 0) {
      elements.add(new PositionIndexOp(offset));
      elements.add(data);
      assert display(trace2, indent, "        5  ", elements);
      return;
    }

    long pos = 0;
    for (int i = 0; i < l; i++) {
      Object o = elements.get(i);
      if (o instanceof PositionIndexOp) {
        pos = ((PositionIndexOp) o).getIndex();
        if (pos == (offset + 1)) {
          ((PositionIndexOp) o).setIndex(offset);
          elements.insertElementAt(data, i + 1);
          assert display(trace2, indent, "     7  " + pos + " " + offset + " ", elements);
          cleanup(elements);
          return;
        } else if (pos > offset) {
          elements.insertElementAt(data, i);
          elements.insertElementAt(new PositionIndexOp(offset), i);
          assert display(trace2, indent, "        1  ", elements);
          cleanup(elements);
          return;
        }

        i++;
        continue;
      }
      if (pos == offset) {
        elements.insertElementAt(data, (int) pos);
        assert display(trace2, indent, "        2  ", elements);
        cleanup(elements);
        return;
      }
      pos++;
    }

    if (pos == offset) {
      elements.add(data);
      assert display(trace2, indent, "        3  ", elements);
      cleanup(elements);
      return;
    }

    elements.add(new PositionIndexOp(offset));
    elements.add(data);
    cleanup(elements);

    assert display(trace2, indent, "        4  ", elements);
  }

  private void cleanup(Vector<Object> elements) {
    long offset = 0;
    int  l      = elements.size();
    for (int i = 0; i < l; i++) {
      Object o = elements.get(i);
      if (o instanceof PositionIndexOp) {
        long pos = ((PositionIndexOp) o).getIndex();
        if (pos == offset) {
          elements.remove(i);
          i--;
          l--;
          continue;
        }
        offset = pos;
        continue;
      }
      offset++;
    }
  }

  /**
   * Generate the implied do loops and return the outer-most DO
   * statement.  This method does a call back to the F95 class for
   * each variable referenced.  
   * @see F95#callback
   */
  public DoLoopStmt genImpliedDoLoop(int callback, F95 f95) throws InvalidException
  {
    assert (indexVar != null) : "Not an implied do.";

    BlockStmt  doBlock = new BlockStmt();
    DoLoopStmt stmt    = new DoLoopStmt(new IdValueOp(indexVar), doBlock, init, limit, step);

    doBlock.setSourceLineNumber(lineno);
    stmt.setSourceLineNumber(lineno);

    int l = items.size();
    for (int i = 0; i < l; i++) {
      Object o = items.get(i);
      if (o instanceof Expression) {
        f95.callback(callback, (Expression) o, doBlock);
        continue;
      }
      ImpliedDo id = (ImpliedDo) o;
      doBlock.addStmt(id.genImpliedDoLoop(callback, f95));
    }
    return stmt;
  }

  private int getExpValue(Expression exp) throws InvalidException
  {
    Literal f = exp.getConstantValue();
    if (!(f instanceof IntLiteral))
      throw new InvalidException("must be scalar-int-expression");
    long fv = ((IntLiteral) f).getLongValue();
    int val = (int) fv;
    if (val != fv)
      throw new InvalidException("must be scalar-int-expression");
    return val;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("(IDO ");

    int l = items.size();
    for (int i = 0; i < l; i++) {
      buf.append(items.get(i));
      buf.append(',');
    }

    if (indexVar != null) {
      buf.append(indexVar.getName());
      buf.append('=');
      buf.append(init);
      buf.append(',');
      buf.append(limit);
      buf.append(',');
      buf.append(step);
    }

    buf.append(')');
    return buf.toString();
  }
}
