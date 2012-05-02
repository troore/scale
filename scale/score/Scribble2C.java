package scale.score;

import java.util.Enumeration;
import java.util.Iterator;

import scale.common.*;
 
import scale.frontend.SourceLanguage;

import scale.score.expr.*;
import scale.score.chords.*;
import scale.score.trans.*;
import scale.callGraph.*;
import scale.clef.*;
import scale.clef.type.*;
import scale.clef.decl.*;
import scale.clef.expr.*;

/**
 * A class to generate C code from a Scribble CFG.
 * <p>
 * $Id: Scribble2C.java,v 1.5 2007-10-04 19:58:19 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public final class Scribble2C implements scale.score.Predicate 
{
  private Machine  machine;           // Target architecture
  private Type     psaut;
  private Type     intCalcType;
  private Scribble scribble;          // CFG
  private Emit     emit;              // Where to send the generated C code.
  private Clef2C   clef2C;            // Used to generate code for decls.
  private Stack<Chord> wl;            // Chords to be processed are pushed onto this stack.
  private String   comment;           // Node by node comment.
  private Chord    lastProcessedNode; // Last node "visited".
  private Chord    gotoChord;         // Generate a goto to this node.
  private boolean  debug;

  /**
   * Construct an object to generate C code from a scribble tree.
   * This object is serially re-usable.
   * @param machine specifies the target architecture
   */
  public Scribble2C(Machine machine)
  {
    this.machine     = machine;
    this.psaut       = PointerType.create(machine.getSmallestAddressableUnitType());
    this.intCalcType = machine.getIntegerCalcType();
  }

  /**
   * Generate C code from the specified CFG.
   * @param scribble the Scribble graph (which represents a single routine).
   * @param emit the Emit class instance for generating the output
   * @param debug is true to select generation of additional comments
   */
  public void generateC(Scribble scribble, Emit emit, boolean debug)
  {
    this.scribble = scribble;
    this.emit     = emit;
    this.debug    = debug;

    // Include any comments at the start of the file.

    Enumeration<String> ev = scribble.getWarnings();
    while (ev.hasMoreElements()) {
      emit.emit("/* ");
      emit.emit(ev.nextElement());
      emit.emit(" */");
      emit.endLine();
    }

    // Create a Clef2C object to generate C code for declarations.

    clef2C = new Clef2C(emit, scribble.getSourceLanguage());
    wl     = WorkArea.<Chord>getStack("Scribble2C");

    RoutineDecl   rd  = scribble.getRoutineDecl();
    ProcedureType sig = rd.getSignature();

    /* Generate function header. */
     
    clef2C.genRoutineAttributes(rd);
    clef2C.genDeclaratorFull(sig, identifierName(rd, false));
    emit.endLine();

    emit.emit('{');
    emit.endLine();
    emit.incIndLevel();

    /* Generate local declarations. */

    int                    count = 0;
    HashSet<String>        names = WorkArea.<String>getSet("Scribble2C"); // For detection of duplicate names.
    CallGraph              cg    = scribble.getRoutineDecl().getCallGraph();
    Iterator<Declaration>  decls = cg.topLevelDecls();
    while (decls.hasNext()) {
      Declaration child = decls.next();
      if (child.isVariableDecl()) // Prohibit name conflicts with globals.
        names.add(child.getName());
    }

    // Get the names of the formal arguements.

    int l = sig.numFormals();
    for (int i = 0; i < l; i++) {
      FormalDecl fd = sig.getFormal(i);
      names.add(fd.getName());
    }
     
    int nd = scribble.numDecls();
    for (int i = 0; i < nd; i++) {
      Declaration child = scribble.getDecl(i);
      if (child instanceof TypeDecl) {
        TypeDecl  td = (TypeDecl) child;
        Type      t  = td.getCoreType();
        if (t.isAggregateType()) {
          clef2C.genTypeDecl(td);
        } else if (t.isEnumerationType()) {
          child.visit(clef2C);
        }
        if (this.debug)
         genNodeID(child, emit);
      }
    }

    for (int i = 0; i < nd; i++) {
      Declaration child = scribble.getDecl(i);
      if (child instanceof TypeName)
        child.visit(clef2C);
      if (this.debug)
        genNodeID(child, emit);
    }

    for (int i = 0; i < nd; i++) {
      Declaration child = scribble.getDecl(i);
      if (child instanceof TypeDecl) {
        TypeDecl  td = (TypeDecl) child;
        Type      t  = td.getCoreType();
        if (!t.isEnumerationType()) {
          child.visit(clef2C);
          if (this.debug)
            genNodeID(child, emit);
        }
      }
    }

    HashSet<Declaration> allowed = WorkArea.<Declaration>getSet("generateC");
    scribble.getAllDeclarations(allowed);

    Vector<ValueDecl> order = new Vector<ValueDecl>(200);
    for (int i = 0; i < nd; i++) {
      Declaration  child = scribble.getDecl(i);
      VariableDecl vd    = child.returnVariableDecl();
      if (vd != null) {
        Expression value = vd.getValue();
        if (value != null) {
          triage(vd, value, order, allowed);
          continue;
        }

        String name = child.getName();
        if (!names.add(name)) { // More than one variable has the same name.
          // NOTE - changing the name changes the hash code of the
          // Declaration instance -- see use of allowed.
          String newname = name + "_d" + count++;
          vd.setName(newname); // Change this variable's name
        }

        vd.visit(clef2C);
        if (this.debug)
          genNodeID(vd, emit);
        continue;
      }

      RoutineDecl rdc = child.returnRoutineDecl();
      if (rdc != null) {
        clef2C.genForwardRoutineDecl(rdc);
        if (this.debug)
          genNodeID(rdc, emit);
        continue;
      }
    }

    WorkArea.<Declaration>returnSet(allowed);

    int ll = order.size();
    for (int i = 0; i < ll; i++) {
      Declaration decl = order.elementAt(i);
      String      name = decl.getName();
      if (!names.add(name)) { // More than one variable has the same name.
        String newname = name + "_d" + count++;
        decl.setName(newname); // Change this variable's name
      }

      if (this.debug)
        genNodeID(decl, emit);
      decl.visit(clef2C);
    }

    WorkArea.<String>returnSet(names);

    emit.endLine();

    scribble.labelCfgForBackend(1); // Give labels to all CFG nodes

    // Visit and translate all CFG nodes.

    Chord.nextVisit();
    wl.push(scribble.getBegin());
    scribble.getBegin().setVisited();

    lastProcessedNode = null;

    int lsla = -1;
    while (!wl.empty()) {
      Chord ch    = wl.pop();
      int   sla   = ch.getSourceLineNumber();
      int   label = ch.getLabel();

      if (label > 0) {
        if ((ch.numInCfgEdges() != 1) ||
            (ch.getInCfgEdge() != lastProcessedNode)) {
          emit.decIndLevel();
          emit.endLine();
          emit.emit("__L_");
          emit.emit(String.valueOf(label));
          emit.emit(":  ");
          if (sla >= 0) {
            emit.emit(" /* line ");
            emit.emit(sla);
            emit.emit(" */");
            lsla = sla;
            sla = -1;
          }
          emit.endLine();
          emit.incIndLevel();
        }
      }

      comment = null;
      gotoChord = null;

      if (this.debug) {
        emit.emit("/* " + ch + " */");
        emit.endLine();
      }

      lastProcessedNode = ch;
      ch.visit(this);

      boolean doln = (sla >= 0) && (sla != lsla);
      if (doln || this.debug || (comment != null)) { // Add comment to aid in debugging.
        emit.emit("  /*");

        if (this.debug) { // Add node ID.
          emit.emit(" node ");
          emit.emit(ch.getNodeID());
        }

        if (doln) { // Add source line number.
          emit.emit(" line ");
          emit.emit(sla);
          lsla = sla;
        }

        if (comment != null) { // Add node specific comment.
          emit.emit(' ');
          emit.emit(comment);
        }

        emit.emit(" */");
      }

      emit.endLine();

      if (gotoChord != null) {
        emit.emit("goto __L_");
        emit.emit(String.valueOf(gotoChord.getLabel()));
        emit.emit(';');
        emit.endLine();
      }
    }

    emit.decIndLevel();
    emit.emit('}');
    emit.endLine();

    WorkArea.<Chord>returnStack(wl);

    this.emit              = null;
    this.clef2C            = null;
    this.wl                = null;
    this.comment           = null;
    this.lastProcessedNode = null;
    this.gotoChord         = null;
  }

  private static void genNodeID(Node n, Emit emit)
  {
    emit.emit("/* ");
    emit.emit(n.getNodeID());
    emit.emit(" */");
  }

  /**
   * Generate C source code from a collection of routines.
   * @param cg specifies the collection of routines
   * @param emit is the {@link scale.common.Emit Emit} object ued to
   * output the code
   */
  public static void genCFromCallGraph(CallGraph cg, Emit emit)
  {
    genGlobalDecls(cg, emit);

    Scribble2C            s2c = new Scribble2C(Machine.currentMachine);
    Iterator<RoutineDecl> it  = cg.allRoutines();
    while (it.hasNext()) {
      RoutineDecl rd       = it.next();
      Scribble    scribble = rd.getScribbleCFG();

      if (scribble == null)
        continue;

      if (rd.inlineSpecified() && (rd.visibility() == Visibility.EXTERN))
        continue;

      s2c.generateC(scribble, emit, false);
      emit.endLine();
    }

    SourceLanguage lang = cg.getSourceLanguage();
    RoutineDecl    main = cg.getMain();
    if ((main != null) && !lang.mainFunction()) {
      emit.emit("int main(int argc, char *argv[]) {");
      emit.endLine();
      emit.incIndLevel();
      if (lang instanceof scale.frontend.java.SourceJava) {
        emit.emit("extern ");
        emit.emit("struct ");
        emit.emit("_aXLjava_lang_String_ ");
        emit.emit("*__moung_args(int argc, char *argv[]);");
        emit.endLine();
      }
      emit.emit(main.getName());
      if (lang instanceof scale.frontend.java.SourceJava)
        emit.emit("(__moung_args(argc, argv));");
      else
        emit.emit("();");
      emit.endLine();
      emit.emit("return 0;");
      emit.endLine();
      emit.decIndLevel();
      emit.emit("}");
      emit.endLine();
    }
  }

  /**
   * Generate the include files needed for the generated C code.
   * @param lang specifies the original source language
   * @param emit is the {@link scale.common.Emit Emit} object ued to
   * output the code
   */
  public static void genIncludes(SourceLanguage lang, Emit emit)
  {
    if (lang.isFortran()) {
      emit.emit("#include <stdlib.h>");
      emit.endLine();
      emit.emit("#include <math.h>");
      emit.endLine();
    } else if (lang instanceof scale.frontend.java.SourceJava) {
      emit.emit("#include <stdlib.h>");
      emit.endLine();
      emit.emit("#include <stdio.h>");
      emit.endLine();
    } else {
      emit.emit("#include <stdarg.h>");
      emit.endLine();
    }
  }

  /** 
   * Generate global declarations
   * @param cg specifies the collection of routines
   * @param emit is the {@link scale.common.Emit Emit} object ued to
   * output the code
   */
  public static void genGlobalDecls(CallGraph cg, Emit emit)
  {
    SourceLanguage lang   = cg.getSourceLanguage();
    Clef2C         clef2C = new Clef2C(emit, lang);

    genIncludes(lang, emit);

    emit.endLine();

    HashSet<Type>      done = WorkArea.<Type>getSet("genGlobalDecls");
    HashSet<Type>      half = WorkArea.<Type>getSet("genGlobalDecls");
    Stack<Declaration> toDo = WorkArea.<Declaration>getStack("genGlobalDecls");

    // Generate the types needed for self reference.

    Iterator<Declaration> decls = cg.topLevelDecls();
    while (decls.hasNext()) { // Do "struct x;"
      Declaration child = decls.next();
      if (child instanceof TypeDecl) {
        TypeDecl td = (TypeDecl) child;
        Type     tt = td.getType();
        Type     t  = tt.getCoreType();
        if (t.isCompositeType()) {
          clef2C.genTypeDecl(td);
          half.add(tt); // It's only half done.
        }
      }
    }

    emit.endLine();

    // Do the types that don't reference other things.

    decls = cg.topLevelDecls();
    while (decls.hasNext()) { // Do "typedef struct x yyy;"
      Declaration td = decls.next();
      if ((td instanceof TypeName) || (td instanceof TypeDecl)) {
        Type t  = ((RefType) td.getType()).getRefTo();
        if (!half.contains(t) && !checkType(t, done, half)) {
          toDo.add(td);
          continue;
        }
        if (false)
          genNodeID(td, emit);
        td.visit(clef2C);
        done.add(td.getType());
      }
    }

    emit.endLine();

    // Do the remaining types in the proper order.

    int     ll      = toDo.size();
    boolean changed = true;
    while (changed) {
      changed = false;
      for (int i = 0; i < ll; i++) {
        Declaration td = toDo.get(i);
        if (td == null)
          continue;

        RefType tt = (RefType) td.getType();
        if (!checkType(tt, done, half)) {
          Type t = tt.getRefTo();
          if (!checkType(t, done, half))
            continue;

          done.add(tt);
        }

        td.visit(clef2C);
        toDo.setElementAt(null, i);
        changed = true;
      }
    }

    for (int i = 0; i < ll; i++) {
      Declaration td = toDo.get(i);
      if (td != null)
        throw new scale.common.InternalError("Not done " + td);
    }

    WorkArea.<Type>returnSet(done);
    WorkArea.<Type>returnSet(half);
    WorkArea.<Declaration>returnStack(toDo);

    emit.endLine();

    decls = cg.topLevelDecls();
    while (decls.hasNext()) { // Do "yyy variable;"
      Declaration child = decls.next();
      if (child instanceof ValueDecl) {
        if (((ValueDecl) child).getValue() == null) {
          if (false)
            genNodeID(child, emit);
          child.visit(clef2C);
        }
      }
    }

    emit.endLine();

    // Generate forward function declarations.

    Iterator<RoutineDecl> it = cg.allRoutines();
    while (it.hasNext()) {
      RoutineDecl rd = it.next();
      if (!rd.isReferenced())
        continue;

      if (false)
        genNodeID(rd, emit);
      clef2C.genForwardRoutineDecl(rd);
    }

    if (lang.isFortran()) {
      emit.emit("extern double _scale_powdd(double a, double b);");
      emit.endLine();
      emit.emit("extern double _scale_powdi(double a, int b);");
      emit.endLine();
      emit.emit("extern double _scale_powdl(double a, long b);");
      emit.endLine();
      emit.emit("extern float _scale_powff(float a, float b);");
      emit.endLine();
      emit.emit("extern float _scale_powfi(float a, int b);");
      emit.endLine();
      emit.emit("extern int _scale_powii(int a, int b);");
      emit.endLine();
      emit.emit("extern int _scale_powil(int a, long b);");
      emit.endLine();
      emit.emit("extern long _scale_powli(long a, int b);");
      emit.endLine();
      emit.emit("extern long _scale_powll(long a, long b);");
      emit.endLine();
    }

    emit.endLine();

    Vector<ValueDecl> order = new Vector<ValueDecl>(200);
    decls = cg.topLevelDecls();
    while (decls.hasNext()) {
      Declaration child = decls.next();
      if (child instanceof ValueDecl) {
        ValueDecl vd = (ValueDecl) child;
        if (vd.getValue() != null)
          triage(vd, vd.getValue(), order, null);
      }
    }

    int l = order.size();
    for (int i = 0; i < l; i++) {
      ValueDecl n = order.elementAt(i);
      if (false)
        genNodeID(n, emit);
      n.visit(clef2C);
    }

    emit.endLine();
  }

  private static boolean checkType(Type type, HashSet<Type> done, HashSet<Type> half)
  {
    if (done.contains(type))
      return true;

    if (type instanceof PointerType) {
      Type pt = ((PointerType) type).getPointedTo();
      while ((pt instanceof RefType) && !((RefType) pt).hasDecl())
        pt = ((RefType) pt).getRefTo();
      return half.contains(pt) || checkType(pt, done, half);
    } else if (type instanceof AggregateType) {
      AggregateType at = (AggregateType) type;
      int           nf = at.numFields();
      boolean       ok = true;
      for (int j = 0; j < nf; j++) {
        FieldDecl fd = at.getField(j);
        Type      ft = fd.getType();
        if (!checkType(ft, done, half))
          return false;
      }
      return true;
    } else if (type instanceof ProcedureType) {
      ProcedureType pt = (ProcedureType) type;
      int           nf = pt.numFormals();
      boolean       ok = true;
      for (int j = 0; j < nf; j++) {
        FormalDecl fd = pt.getFormal(j);
        Type       ft = fd.getType();
        if (!checkType(ft, done, half))
          return false;
      }
      return checkType(pt.getReturnType(), done, half);
    } else if (type instanceof RefType) {
      RefType rt = (RefType) type;
      if (rt.hasDecl())
        return false;
      return checkType(rt.getRefTo(), done, half);
    } else if (type instanceof IncompleteType) {
      return checkType(((IncompleteType) type).getCompleteType(), done, half);
    } else if (type instanceof ArrayType) {
      Type et = ((ArrayType) type).getElementType();
      return checkType(et, done, half);
    }
    return true;
  }

  /**
   * Put variables in the proper order to be compiled by a C compiler.
   * If the initializer for one contains a reference to another, then
   * it must be second.
   */
  private static void triage(ValueDecl         vd,
                             Node              value,
                             Vector<ValueDecl> order,
                             HashSet<Declaration>   allowed)
   {
     if (order.contains(vd) || ((allowed != null) && !allowed.contains(vd)))
      return;

    triagep(value, order, allowed);

    order.addElement(vd);
  }

  private static void triagep(Node value, Vector<ValueDecl> order, HashSet<Declaration> allowed)
  {
    if (value instanceof Literal) {
      if (value instanceof AddressLiteral) {
        AddressLiteral al = (AddressLiteral) value;
        Declaration    d  = al.getDecl();
        if (d instanceof ValueDecl) {
          ValueDecl  vd  = (ValueDecl) d;
          Expression val = vd.getValue();
          if (val != null)
            triage(vd, val, order, allowed);
        }
        return;
      }

      if (value instanceof AggregationElements) {
        AggregationElements ae = (AggregationElements) value;
        Vector<Object>      ea = ae.getElementVector();
        int                 l  = ea.size();
        for (int i = 0; i < l; i++) {
          Object x = ea.elementAt(i);
          if (x instanceof Expression) {
            Expression exp = (Expression) x;
            triagep(exp, order, allowed);
          }
        }
        return;
      }
      return;
    }

    if (value instanceof IdReferenceOp) {
      ValueDecl vd = ((IdReferenceOp) value).getDecl().returnValueDecl();
      if (vd == null)
        return;
      triage(vd, vd.getValue(), order, allowed);
      return;
    }

    if (value instanceof TypeConversionOp) {
      triagep(((TypeConversionOp) value).getExpr(), order, allowed);
      return;
    }

    throw new scale.common.InternalError("Unrecognized expression " + value);
  }

  private Chord findNextChord(Chord s)
  {
    if (debug)
      return s;

    while (s.isSpecial()) {
      if (s.numInCfgEdges() != 1)
        break;
      if (s.isPhiExpr())
        break;
      if (s.isLoopHeader())
        break;
      Chord nxt = s.getNextChord();
      if (nxt.getLabel() == 0)
        break;
      lastProcessedNode = null;
      s.setVisited();
      s = nxt;
    }
    return s;
  }

  private void processNextChord(Chord s)
  {
    s = findNextChord(s);

    if (!s.visited()) { // First time for this node - do it next.
      wl.push(s);
      s.setVisited();
      return;
    }

    if ((wl.size() > 0) && (wl.peek() == s))
      return; // It's next and already on the top of the stack.

    if (!wl.remove(s)) { // It's been processed already.
      gotoChord = s;
      return;
    }

    wl.push(s); // Do it next.
  }

  private String identifierName(Declaration d, boolean address)
  {
    return clef2C.convertDeclName(d, address);
  }

  private void emitGoto(Chord s)
  {
    s = findNextChord(s);

    emit.emit("goto __L_");
    emit.emit(String.valueOf(s.getLabel()));
    emit.emit(';');
  }

  /**
   * Generate a call to a routine that is part of the Scale runtime
   * library.  This is used mainly for operations on complex values.
   */
  private void genIntrinsicOp(String op, Type t1, Expr e1)
  {
    String opn1 = t1.mapTypeToCString();

    emit.emit("_scale_");
    emit.emit(op);
    emit.emit(Clef2C.simpleTypeName(opn1));
    emit.emit('(');
    e1.visit(this);
    emit.emit(')');
  }

  /**
   * Generate a call to a routine that is part of the Scale runtime
   * library.  This is used mainly for operations on complex values.
   */
  private void genIntrinsicOp(String op, Type t1, Expr e1, Type t2, Expr e2)
  {
    String opn1 = t1.mapTypeToCString();
    String opn2 = t2.mapTypeToCString();

    emit.emit("_scale_");
    emit.emit(op);
    emit.emit(Clef2C.simpleTypeName(opn1));
    emit.emit(Clef2C.simpleTypeName(opn2));
    emit.emit('(');
    e1.visit(this);
    emit.emit(", ");
    e2.visit(this);
    emit.emit(')');
  }

  public void visitNullChord(NullChord c)
  {
    if (debug)
      comment = "NOP";

    processNextChord(c.getNextChord());
  }

  public void visitLoopPreHeaderChord(LoopPreHeaderChord c)
  {
    if (debug)
      comment = "LPH (" + c.getLoopHeader().getNodeID() + ")";

    processNextChord(c.getNextChord());
  }

  public void visitLoopHeaderChord(LoopHeaderChord c)
  {
    if (debug)
      comment = "LH (" + c.getLoopHeader().getNodeID() + ")";

    processNextChord(c.getNextChord());
  }

  public void visitLoopTailChord(LoopTailChord c)
  {
    if (debug)
      comment = "LT (" + c.getLoopHeader().getNodeID() + ")";

    processNextChord(c.getNextChord());
  }

  public void visitLoopInitChord(LoopInitChord c)
  {
    if (debug)
      comment = "LI (" + c.getLoopHeader().getNodeID() + ")";

    processNextChord(c.getNextChord());
  }

  public void visitLoopExitChord(LoopExitChord c)
  {
    if (debug)
      comment = "LE (" + c.getLoopHeader().getNodeID() + ")";

    processNextChord(c.getNextChord());
  }

  public void visitExprChord(ExprChord c)
  {
    Expr lhs       = c.getLValue();
    Expr rhs       = c.getRValue();
    Expr predicate = c.getPredicate();

    if (predicate != null) {
      emit.emit(" if (");

      if (!c.predicatedOnTrue())
        emit.emit('!');

      predicate.visit(this);

      emit.emit(") ");
    }

    if (lhs == null)
      rhs.visit(this);
    else if (c.isVaCopy()) {
      emit.emit("va_copy(");
      lhs.visit(this);
      emit.emit(',');
      rhs.visit(this);
      emit.emit(')');
    } else
      doStore(lhs, rhs);

    emit.emit(';');

    processNextChord(c.getNextChord());
  }

  public void visitPhiExprChord(PhiExprChord c)
  {
    visitExprChord(c);
  }

  public void visitBeginChord(BeginChord c)
  {
    processNextChord(c.getNextChord());
  }

  public void visitEndChord(EndChord c)
  {
    emit.emit(';');
  } 

  public void visitSwitchChord(SwitchChord c)  
  { 
    Expr predicate = c.getPredicateExpr();
    emit.emit("switch (");
    if (predicate != null)
      predicate.visit(this);
    emit.emit(") {");
    emit.endLine(); 
    emit.incIndLevel();

    // Get each of the decision alternatives.

    long[]  keys     = c.getBranchEdgeKeyArray();
    Chord[] outEdges = c.getOutCfgEdgeArray(); 
    int     def      = c.getDefaultIndex();

    for (int i = 0; i < outEdges.length; i++) {
      // Process a single decision alternative
      Chord  caseChord = outEdges[i]; 
      if (i == def)
        emit.emit("default : ");
      else {
        emit.emit("case ");
        emit.emit(Long.toString(keys[i]));
        emit.emit(" : ");     
      } 

      caseChord = findNextChord(caseChord);
      emit.emit("goto __L_");
      emit.emit(String.valueOf(caseChord.getLabel()));
      emit.emit(';');
      emit.endLine(); 

      if (!caseChord.visited()) {
        wl.push(caseChord);
        caseChord.setVisited();
      }
    }

    lastProcessedNode = null;
    emit.decIndLevel();
    emit.emit('}');
  }

  /**
   * Skip nodes that don't result in generating instructions.  Special
   * CFG nodes do not generate code.  But, we must always generate the
   * label for the loop header.
   */
  private final Chord getBranchTarget(Chord s)
  {
    if (!s.isSpecial() || s.isLoopHeader())
      return s;

    if (s.isLoopTail())
      return s.getNextChord();

    return s;
  }

  public void visitIfThenElseChord(IfThenElseChord c)
  {
    Chord t    = getBranchTarget(c.getTrueCfgEdge());
    Chord f    = getBranchTarget(c.getFalseCfgEdge());
    Expr  pred = c.getPredicateExpr();

    Chord st = findNextChord(t);
    if (st.getLabel() != 0) {
      lastProcessedNode = null;
      t = st;
    }
 
    Chord sf = findNextChord(f);
    if (sf.getLabel() != 0) {
      lastProcessedNode = null;
      f = sf;
    }

    if ((t != null) && !t.visited()) {
      t.setVisited();
      if ((f != null) && !f.visited()) {
        f.setVisited();
        wl.push(f);    
      }
      wl.push(t);
      if (t.numInCfgEdges() == 1)
        t.setLabel(0);
      doReverseBranch(pred);
      emitGoto(f);
      return;
    }
   
    if ((f != null) && !f.visited()) {
      f.setVisited();
      wl.push(f);
      if (f.numInCfgEdges() == 1)
        f.setLabel(0);
      doRegularBranch(pred);
      emitGoto(t);
      return;
    }

    if (t != null) {
      if (!wl.empty() && (t == wl.peek())) {
        if (f != null) {
          doReverseBranch(pred);
          emitGoto(f);
        }
        return;
      }
      if (f != null) {
        doRegularBranch(pred);
        emitGoto(t);
        processNextChord(f);
        return;
      }
      emitGoto(t);
    }

    if (f != null)
      processNextChord(f);
  }

  private void doRegularBranch(Expr pred)
  {
    emit.emit("if ");
    if (!pred.isMatchExpr())
      emit.emit('(');
    pred.visit(this);
    if (!pred.isMatchExpr())
      emit.emit(')');
    emit.emit(' ');
  }

  private void doReverseBranch(Expr pred)
  {
    emit.emit("if ");
    if (pred.isMatchExpr()) {
      MatchExpr me = (MatchExpr) pred;
      Expr      la = me.getOperand(0);
      Expr      ra = me.getOperand(1);
      if (!la.getType().isRealType() && !ra.getType().isRealType()) {
        doMatchExpr(la, ra, me.getMatchOp().reverse());
        emit.emit(' ');
        return;
      }
      // Can't reverse floating point compare - e.g., NaN < 0.0
    }

    emit.emit("(!");
    pred.visit(this);
    emit.emit(") ");
  }

  public void visitMarkerChord(MarkerChord c)
  {
    /* Do Nothing */
  }

  public void visitGotoChord(GotoChord c)
  {
    Chord target = c.getTarget();

    processNextChord(target);
  }

  public void visitReturnChord(ReturnChord c)
  {
    if (c.numInDataEdges() > 0) {
      emit.emit("return (");
      c.getInDataEdge(0).visit(this);
      emit.emit(");");
    } else {
      emit.emit("return;");
    }
  } 

  public void visitExitChord(ExitChord c)
  {
    if (c.numInDataEdges() == 0) {
      emit.emit("exit(0);");
    } else {
      Expr ev = c.getInDataEdge(0);
      if (ev.getCoreType().isIntegerType()) {
        emit.emit("exit (");
        ev.visit(this);
        emit.emit(");");
      } else {
        emit.emit("printf(\"%s\\n\",");
        ev.visit(this);
        emit.emit(");");
        emit.endLine();
        emit.emit("exit(0);");
      }
    }
    processNextChord(c.getNextChord());
  } 

  public void visitDualExpr(DualExpr e)
  {
    e.getLow().visit(this);
  }

  private void addCast(Type type, Expr expr)
  {
    Type ct = type.getCoreType();
    if (ct.isPointerType()) {
      while ((expr instanceof ConversionExpr) &&
             ((ConversionExpr) expr).isCast())
        expr = ((ConversionExpr) expr).getArg();
    }

    if (ct.equivalent(expr.getCoreType())) {
      expr.visit(this);
      return;
    }

    emit.emit("((");
    clef2C.genCastType(type);
    emit.emit(')');

    expr.visit(this);

    emit.emit(')');
  }

  public void visitLiteralExpr(LiteralExpr e)
  {
    Literal l = e.getLiteral();

    if (l instanceof SizeofLiteral) {
      emit.emit("sizeof(");
      clef2C.genDeclarator(((SizeofLiteral) l).getSizeofType(), "");
      emit.emit(')');
      return;
    }

    if (l instanceof AggregationElements) {
      AggregationElements agg = (AggregationElements) e.getLiteral();
      agg.visit(clef2C);
      return;
    }

    if (l instanceof AddressLiteral) {
      AddressLiteral al = (AddressLiteral) e.getLiteral();
      al.visit(clef2C);
      return;
    }

    String lit = l.getGenericValue();
    if (l instanceof ComplexLiteral) {
      emit.emit("_scale_create");
      emit.emit(lit);
      return;
    }

    if (l.getCoreType().isPointerType()) {
        emit.emit("((");
        clef2C.genCastType(l.getType());
        emit.emit(')');
    }

    emit.emit(lit);

    if (l.getCoreType().isPointerType())
      emit.emit(')');
  }

  public void visitNilExpr(NilExpr e)
  {
    emit.emit("(void *)0");
  }

  public void visitVaStartExpr(VaStartExpr e)
  {
    Expr vaList = e.getVaList();
    emit.emit("va_start(*(");
    vaList.visit(this);
    emit.emit("), ");
    emit.emit(e.getParmN().getName());
    emit.emit(')');
  }

  public void visitVaEndExpr(VaEndExpr e)
  {
    Expr vaList = e.getVaList();
    emit.emit("va_end(*(");
    vaList.visit(this);
    emit.emit("))");
  }

  public void visitVaArgExpr(VaArgExpr e)
  {
    Expr vaList = e.getVaList();
    emit.emit("va_arg(*(");
    vaList.visit(this);
    emit.emit("), ");
    clef2C.genDeclarator(e.getType(), "");
    emit.emit(')');
  }

  public void visitAbsoluteValueExpr(AbsoluteValueExpr e)
  {
    Expr e1 = e.getOperand(0);
    Type t1 = e1.getCoreType();

    if (t1.isComplexType()) {
      genIntrinsicOp("abs", t1, e1);
      return;
    }

    IntegerType it = t1.returnIntegerType();
    if (it != null) {
      if (it.bitSize() > 32) {
        emit.emit('l');
        if (machine.getIntegerCalcType().bitSize() <= 32)
          emit.emit('l');
      }
    } else
      emit.emit('f');

    emit.emit("abs(");

    e1.visit(this);    
    emit.emit(')');
  }

  public void visitBitComplementExpr(BitComplementExpr e)
  {
    emit.emit("(~");
    e.getOperand(0).visit(this);
    emit.emit(')'); 
  }

  public void visitNegativeExpr(NegativeExpr e)
  {
    Expr e1 = e.getOperand(0);
    Type t1 = e1.getCoreType();
    
    if (t1.isComplexType()) {
      genIntrinsicOp("negate", t1, e1);
      return;
    }

    emit.emit("(- ");
    e1.visit(this);
    emit.emit(')');
  }

  public void visitTranscendentalExpr(TranscendentalExpr e)
  {
    Expr e1   = e.getOperand(0);
    Type et   = e1.getCoreType();

    if (et.isComplexType()) {
      genIntrinsicOp(e.getDisplayLabel(), et, e1);
      return;
    }

    emit.emit(e.getFtn().cName());
    emit.emit('(');
    e1.visit(this);
    emit.emit(')');
  }

  public void visitTranscendental2Expr(Transcendental2Expr e)
  {
    Expr e1   = e.getOperand(0);
    Expr e2   = e.getOperand(1);
    int  ftn  = e.getFtn();

    switch (ftn) {
    case Transcendental2Op.cAtan2:
      emit.emit(e.getDisplayLabel());
      emit.emit('(');
      e1.visit(this);
      emit.emit(',');
      e2.visit(this);
      emit.emit(')');
      return;
    case Transcendental2Op.cSign:
      Type t1 = e1.getCoreType();
      if (t1.isComplexType()) {
        emit.emit("((");
        e2.visit(this);
        emit.emit(" < 0.0) ? -");
        genIntrinsicOp("abs", t1, e1);
        emit.emit(" : ");
        genIntrinsicOp("abs", t1, e1);
        emit.emit(')');
        return;
      }

      if (t1.isRealType()) {
        emit.emit("((");
        e2.visit(this);
        emit.emit(" < 0.0) ? -fabs(");
        e1.visit(this);
        emit.emit(") : fabs(");
        e1.visit(this);
        emit.emit("))");
        return;
      }

        emit.emit("((");
        e2.visit(this);
        emit.emit(" < 0) ? -abs(");
        e1.visit(this);
        emit.emit(") : abs(");
        e1.visit(this);
        emit.emit("))");
        return;

    case Transcendental2Op.cDim:
      emit.emit("((");
      e1.visit(this);
      emit.emit(" > ");
      e2.visit(this);
      emit.emit(") ? (");
      e1.visit(this);
      emit.emit(" - ");
      e2.visit(this);
      emit.emit(") : 0)");
      return;
    default:
      throw new scale.common.InternalError("Uknown intrinsic " + ftn);
    }
  }

  public void visitNotExpr(NotExpr e)
  { 
    emit.emit("(!");
    e.getArg().visit(this);
    emit.emit(')');
  }

  public void visitAllocateExpr(AllocateExpr e)
  {
    emit.emit("((void *) malloc(");
    e.getArg().visit(this);
    emit.emit("))");
  }

  private void doCombinedBinaryExpr(Expr expr, String ops, String opc)
  {
    Expr e1 = expr.getOperand(0);
    Type t1 = e1.getCoreType();
    Expr e2 = expr.getOperand(1);

    if (t1.isComplexType()) {
      Type t2 = e2.getCoreType();
      genIntrinsicOp(opc, t1, e1, t2, e2);
      return;
    }

    emit.emit('(');
    e1.visit(this);
    emit.emit(ops);
    e2.visit(this);
    emit.emit(')');
  }

  private void doSimpleBinaryExpr(Expr expr, String op)
  {
    emit.emit('(');
    expr.getOperand(0).visit(this);
    emit.emit(op);
    expr.getOperand(1).visit(this);
    emit.emit(')');
  }
 
  public void visitAdditionExpr(AdditionExpr ae)
  {
    Expr la = ae.getLeftArg();
    Expr ra = ae.getRightArg();
    Type lt = la.getCoreType();
    Type rt = ra.getCoreType();

    if (!lt.isPointerType() && rt.isPointerType()) {
      Expr t = la;
      la = ra;
      ra = t;
      Type tt = lt;
      lt = rt;
      rt = tt;
    }

    if (!lt.isPointerType()) {
      doCombinedBinaryExpr(ae, " + ", "add");
      return;
    }

    if (ra.isLiteralExpr()) {
      Literal lito = ((LiteralExpr) ra).getLiteral();
      if (lito instanceof IntLiteral) {
        long ov  = ((IntLiteral) lito).getLongValue();
        if (ov == 0) {
          la.visit(this);
          return;
        }
      }
    }

    Type st = ae.getCoreType();
    emit.emit('(');
    if (psaut != st) {
      emit.emit('(');
      clef2C.genCastType(ae.getType());
      emit.emit(")(");
    }
    addCast(psaut, la);
    emit.emit(" + ");
    ra.visit(this);
    if (psaut != st)
      emit.emit(')');
    emit.emit(')');
  }
 
  public void visitAndExpr(AndExpr e)
  {
    doSimpleBinaryExpr(e, " && ");
  }
 
  public void visitBitAndExpr(BitAndExpr e)
  {     
    doSimpleBinaryExpr(e, " & ");
  }
 
  public void visitBitOrExpr(BitOrExpr e)
  {
    doSimpleBinaryExpr(e, " | ");
  }
 
  public void visitBitShiftExpr(BitShiftExpr e)
  { 
    ShiftMode mode = e.getShiftMode();
    String    op = "??";

    switch (mode) {
    case Left:
      op = "<<";
      break;
    case UnsignedRight:
    case SignedRight:
      op = ">>";
      break;
    default:
      throw new scale.common.InternalError("Can't convert " +
                                           mode +
                                           " to C.");
    }
    doSimpleBinaryExpr(e, op);
  }
 
  public void visitBitXorExpr(BitXorExpr e)
  {
    doSimpleBinaryExpr(e, " ^ ");
  }
 
  public void visitDivisionExpr(DivisionExpr e)
  {
    doCombinedBinaryExpr(e, " / ", "div");
  }

  public void visitCompareExpr(CompareExpr e)
  {
    int  mode = e.getMode();
    Expr la   = e.getOperand(0);
    Expr ra   = e.getOperand(1);

    switch (mode) {
    case CompareExpr.Normal:
      emit.emit("((");
      la.visit(this);
      emit.emit(" == ");
      ra.visit(this);
      emit.emit(") ? 0 : ((");
      la.visit(this);
      emit.emit(" < ");
      ra.visit(this);
      emit.emit(") ? -1 : 1))");
      break;
    case CompareExpr.FloatG:
    case CompareExpr.FloatL:
      emit.emit("compare");
      emit.emit(CompareExpr.modes[mode]);
      emit.emit(la.getCoreType().mapTypeToCString());
      emit.emit(ra.getCoreType().mapTypeToCString());
      emit.emit('(');
      la.visit(this);
      emit.emit(", ");
      ra.visit(this);
      emit.emit(')');
      break;
    default:
      throw new scale.common.InternalError("Invalid mode " + e);
    }
  }

  private void doMatchExpr(Expr la, Expr ra, CompareMode comp)
  {
    String op = comp.cName();

    if (la.getCoreType().isComplexType()) {
      assert ((comp == CompareMode.EQ) || (comp == CompareMode.NE)) :
             "The \"" + op + "\" operator is not allowed on complex values.";
      emit.emit("((");
      if (la instanceof ComplexValueExpr) {
        ((ComplexValueExpr) la).getReal().visit(this);
      } else {
        la.visit(this);
        emit.emit(".r");
      }

      emit.emit(' ');
      emit.emit(op);
      emit.emit(' ');

      if (ra instanceof ComplexValueExpr) {
        ((ComplexValueExpr) ra).getReal().visit(this);
      } else {
        ra.visit(this);
        emit.emit(".r");
      }

      if (comp == CompareMode.EQ)
        emit.emit(") && (");
      else
        emit.emit(") || (");

      if (la instanceof ComplexValueExpr) {
        ((ComplexValueExpr) la).getImaginary().visit(this);
      } else {
        la.visit(this);
        emit.emit(".i");
      }

      emit.emit(' ');
      emit.emit(op);
      emit.emit(' ');

      if (ra instanceof ComplexValueExpr) {
        ((ComplexValueExpr) ra).getImaginary().visit(this);
      } else {
        ra.visit(this);
        emit.emit(".i");
      }
      emit.emit("))");
    } else {
      emit.emit('(');
      la.visit(this);
      emit.emit(' ');
      emit.emit(op);
      emit.emit(' ');
      ra.visit(this);
      emit.emit(')');
    }
  }

  public void visitEqualityExpr(EqualityExpr e)
  {
    Expr la = e.getOperand(0);
    Expr ra = e.getOperand(1);

    doMatchExpr(la, ra, CompareMode.EQ);
  }

  public void visitGreaterEqualExpr(GreaterEqualExpr e)
  {
    Expr la   = e.getOperand(0);
    Expr ra   = e.getOperand(1);
    doMatchExpr(la, ra, CompareMode.GE);     
  }
 
  public void visitGreaterExpr(GreaterExpr e)
  { 
    Expr la = e.getOperand(0);
    Expr ra = e.getOperand(1);

    doMatchExpr(la, ra, CompareMode.GT);     
  }

  public void visitLessEqualExpr(LessEqualExpr e)
  {
    Expr la = e.getOperand(0);
    Expr ra = e.getOperand(1);

    doMatchExpr(la, ra, CompareMode.LE);     
  }
 
  public void visitLessExpr(LessExpr e)
  { 
    Expr la = e.getOperand(0);
    Expr ra = e.getOperand(1);

    doMatchExpr(la, ra, CompareMode.LT);     
  }

  public void visitExponentiationExpr(ExponentiationExpr e)
  {
    Expr e1 = e.getOperand(0);
    Expr e2 = e.getOperand(1);
    Type t1 = e1.getCoreType();
    Type t2 = e2.getCoreType();

    genIntrinsicOp("pow", t1, e1, t2, e2);
  }
 
  public void visitMultiplicationExpr(MultiplicationExpr e)
  {
    doCombinedBinaryExpr(e, " * ", "mult");
  }

 public void visitNotEqualExpr(NotEqualExpr e)
  {
    Expr la = e.getOperand(0);
    Expr ra = e.getOperand(1);

    doMatchExpr(la, ra, CompareMode.NE);
  }

  public void visitOrExpr(OrExpr e)
  {
    doSimpleBinaryExpr(e, " || ");
  }

  public void visitRemainderExpr(RemainderExpr e)
  {   
    Expr e1 = e.getOperand(0);
    Expr e2 = e.getOperand(1);
    Type t1 = e.getCoreType();

    if (t1.isIntegerType()) {
      emit.emit('(');
      e1.visit(this);     
      emit.emit(" % ");
      e2.visit(this);
      emit.emit(')');
      return;
    }

    if (t1.isFloatType()) {
      emit.emit("fmod(");
      e1.visit(this);
      emit.emit(',');
      e2.visit(this);
      emit.emit(')');
      return;
    }

    throw new scale.common.InternalError(" Modulo operands - " + e);
  }

  public void visitSubtractionExpr(SubtractionExpr se)
  {
    Expr la = se.getLeftArg();
    Expr ra = se.getRightArg();
    Type lt = la.getCoreType();
    Type rt = ra.getCoreType();

    if (!lt.isPointerType() || (psaut == lt)) {
      doCombinedBinaryExpr(se, " - ", "sub");
      return;
    }

    if (lt.isPointerType() && rt.isPointerType()) {
      // (((long) la) - ((long) ra))
      emit.emit('(');
      addCast(intCalcType, la);
      emit.emit(" - ");
      addCast(intCalcType, ra);
      emit.emit(')');
      return;
    }

    Type st = se.getCoreType();
    emit.emit('(');
    if (psaut != st) {
      emit.emit('(');
      clef2C.genCastType(se.getType());
      emit.emit(")(");
    }
    addCast(psaut, la);
    emit.emit(" - "); // The blank is important.
    ra.visit(this);
    if (psaut != st)
      emit.emit(')');
    emit.emit(')');
  }

  private void doFieldExpr(FieldExpr expr)
  {
    Expr      structure = expr.getStructure();
    FieldDecl field     = expr.getField();

    if (structure instanceof LoadDeclAddressExpr) {
      Declaration d = ((LoadDeclAddressExpr) structure).getDecl();
      Type dt = d.getCoreType();
      if (dt.isFixedArrayType()) {
        emit.emit("(&");
        emit.emit(identifierName(d, true));
        emit.emit("[0])->");
      } else {
        emit.emit(identifierName(d, true)); 
        emit.emit('.');
      }
    } else if (structure instanceof LoadDeclValueExpr) {
      Declaration d = ((LoadDeclValueExpr) structure).getDecl();
      emit.emit(identifierName(d, false)); 
      emit.emit("->");
    } else {
      emit.emit('(');
      structure.visit(this);     
      emit.emit(')');
      emit.emit("->");
    }
    emit.emit(identifierName(field, false));
  }

  public void visitLoadFieldValueExpr(LoadFieldValueExpr e)
  {
    doFieldExpr(e); 
  }

  public void visitLoadFieldAddressExpr(LoadFieldAddressExpr e)
  {
    FieldDecl field = e.getField();

    if (field.getCoreType().isArrayType()) {
//        emit.emit('&');
      doFieldExpr(e);
//        emit.emit("[0]");
    } else {
      emit.emit('&');
      doFieldExpr(e);
    }
  }

  public void visitConditionalExpr(ConditionalExpr ce)
  {
    Type    ty  = ce.getType();
    boolean flg = ty.isPointerType() && ty.getCoreType().getPointedTo().isArrayType();
    if (flg) {
      emit.emit("((");
      clef2C.genCastType(ty);
      emit.emit(')');
      ty = PointerType.create(VoidType.type);
    }
    emit.emit('(');
    ce.getTest().visit(this);
    emit.emit(" ? ");
    addCast(ty, ce.getTrueExpr());
    emit.emit(" : ");
    addCast(ty, ce.getFalseExpr());
    emit.emit(')');
    if (flg)
      emit.emit(')');
  }

  public void visitConversionExpr(ConversionExpr e)
  {
    Type tt = e.getType();
    Expr e2 = e.getOperand(0);
    Type et = e2.getType();

    // A language defined conversion routine.

    switch (e.getConversion()) {
    case FLOOR:
      emit.emit("floor(");
      e2.visit(this);
      emit.emit(')');
      break;
    case CEILING:
      emit.emit("ceil(");
      e2.visit(this);
      emit.emit(')');
      break;
    case ROUND:
      emit.emit('(');
      clef2C.genDeclarator(tt, "");
      emit.emit(") (");
      e2.visit(this);
      emit.emit(" + ((");
      e2.visit(this);
      emit.emit(" >= 0.0) ? 0.5 : -0.5");
      emit.emit("))");
      break;
    case TRUNCATE:
    case REAL:
      emit.emit("((");
      clef2C.genCastType(tt);
      emit.emit(')');
      if (et.isComplexType()) {
        if (e2 instanceof ComplexValueExpr) {
          ((ComplexValueExpr) e2).getReal().visit(this);
        } else {
          e2.visit(this);
          emit.emit(".r");
        }
      } else {
        e2.visit(this);
      }
      emit.emit(')');
      break;
    case CAST:
      assert !tt.isAggregateType() : "Cannot cast to Aggregate Type" + e;

      addCast(tt, e2);
      break;
    case IMAGINARY:
      emit.emit("((");
      clef2C.genCastType(tt);
      emit.emit(')');
      if (et.isComplexType()) {
        if (e2 instanceof ComplexValueExpr) {
          ((ComplexValueExpr) e2).getImaginary().visit(this);
        } else {
          e2.visit(this);
          emit.emit(".i");
        }
      } else
        emit.emit('0');

      emit.emit(')');
      break;
    default:
      throw new scale.common.InternalError("unknown cast operation" + e);
    }
  }

  public void visitComplexValueExpr(ComplexValueExpr e)
  {
    String opn = e.getCoreType().mapTypeToCString();
    emit.emit("_scale_create");
    emit.emit(opn);
    emit.emit('(');
    e.getReal().visit(this);
    emit.emit(", ");
    e.getImaginary().visit(this);
    emit.emit(')');
  }

  public void visitCallFunctionExpr(CallFunctionExpr e)
  {
    Expr func = e.getFunction();
    int  na   = e.numArguments();

    ProcedureType pt = (ProcedureType) func.getPointedToCore();
    int           nf = pt.numFormals();

    if (func instanceof LoadDeclAddressExpr)
      emit.emit(identifierName(((LoadDeclAddressExpr) func).getDecl(), false));
    else if (func instanceof LoadDeclValueExpr) {
      emit.emit("(*");
      emit.emit(identifierName(((LoadDeclValueExpr) func).getDecl(), false));
      emit.emit(')');
    } else if (func instanceof LoadValueIndirectExpr)
      visitLoadValueIndirectExpr((LoadValueIndirectExpr) func);
    else {
      emit.emit('(');
      func.visit(this);
      emit.emit(')');
    }

    if (nf < na) {
      if ((nf > 0) && !(pt.getFormal(nf - 1) instanceof UnknownFormals))
        na = nf;
    }

    emit.emit('(');
    int i = 0;
    for (; i < na; i++) {
      if (i > 0)
        emit.emit(", ");
      Expr arg = e.getArgument(i);
      arg.visit(this);
    }
    for (; i < nf; i++) {
      if (i > 0)
        emit.emit(", ");
      emit.emit('0');
    }
    emit.emit(')');
  }

  public void visitCallMethodExpr(CallMethodExpr e)
  {
    Expr func = e.getMethod();
    int  na   = e.numArguments();
     
    if (func instanceof LoadDeclAddressExpr)
      emit.emit(identifierName(((LoadDeclAddressExpr) func).getDecl(), false));
    else if (func instanceof LoadDeclValueExpr)
      emit.emit(identifierName(((LoadDeclValueExpr) func).getDecl(), false));   
    else if (func instanceof LoadValueIndirectExpr)
      visitLoadValueIndirectExpr((LoadValueIndirectExpr) func);
    else {
      emit.emit('(');
      func.visit(this);
      emit.emit(')');
    }

    emit.emit('(');
    e.getObjectClass().visit(this);
    if (na > 1) {
      for (int i = 1; i < na; i++) {
        emit.emit(", ");
        e.getArgument(i).visit(this);
      }
    }
    emit.emit(") /* met */"); 
  }

  public void visitLoadDeclAddressExpr(LoadDeclAddressExpr e)
  {
    Declaration decl = e.getDecl();
    Type        type = e.getType();
    Type        dt   = decl.getCoreType();

    if (dt.isFixedArrayType() || dt.isProcedureType()) {
      emit.emit(identifierName(decl, true));
    } else {
      emit.emit('&');
      emit.emit(identifierName(decl, false));
    }
  }

  public void visitLoadDeclValueExpr(LoadDeclValueExpr e)
  {
    Declaration d = e.getDecl();
    emit.emit(identifierName(d, false));
  }

  public void visitLoadValueIndirectExpr(LoadValueIndirectExpr e)
  {
    Expr addr = e.getArg();
   
    if (addr instanceof LoadDeclAddressExpr) {
      Declaration d = ((LoadDeclAddressExpr) addr).getDecl();
      if (!d.getCoreType().isArrayType()) {
        emit.emit(identifierName(d, false)); 
        return;
      }
    } else if (addr instanceof SubscriptExpr) {
      doSubscriptExpr((SubscriptExpr) addr);
      return;
    }

    emit.emit("(*");
    addr.visit(this);
    emit.emit(')');
  }

  private void doStore(Expr addr, Expr value)
  {
    Type      t      = value.getCoreType();
    boolean   memcpy = false;

    if (t.isArrayType()) {
      memcpy = true;
      emit.emit("memcpy((char *) ");
    }

    if (addr instanceof LoadDeclAddressExpr) {
      Declaration addrDecl = ((LoadDeclAddressExpr) addr).getDecl();
      if (memcpy)
        emit.emit('&');
      else if (addrDecl.getCoreType().isArrayType())
        emit.emit('*');
      emit.emit(identifierName(addrDecl, false)); 
    } else if (addr instanceof LoadDeclValueExpr) {
      Declaration addrDecl  = ((LoadDeclValueExpr) addr).getDecl();
      if (!memcpy)
        emit.emit('*');
      emit.emit(identifierName(addrDecl, false)); 
    } else if (addr instanceof LoadFieldValueExpr) {
      emit.emit("*(");
      doFieldExpr((FieldExpr) addr);
      emit.emit(')');
    } else if (addr instanceof LoadFieldAddressExpr) {
      doFieldExpr((FieldExpr) addr);
    } else if (addr instanceof SubscriptExpr) {
      doSubscriptExpr((SubscriptExpr) addr);
    } else if (addr instanceof LoadValueIndirectExpr) {
      emit.emit("*(");
      addr.visit(this);
      emit.emit(')');
    } else {
      emit.emit("*(");
      addr.visit(this);
      emit.emit(')');
    }
      
    if (memcpy) {
      emit.emit(", ");
      value.visit(this); 
      emit.emit(", sizeof(");
      clef2C.genDeclarator(t, "");
      emit.emit("))");
      return;
    }

    emit.emit(" = ");

    Type lt = addr.getCoreType().getPointedTo();
    addCast(lt, value); 
  }

  private void doSubscriptExpr(SubscriptExpr sub)
  {
    Expr arr = sub.getArray();
    
    arr.visit(this);

    int ns = sub.numSubscripts();
    for (int i = 0; i < ns; i++) {
      emit.emit('[');
      sub.getSubscript(i).visit(this);
      emit.emit(']');
    }
  }

  private void emitPlusInteger(long val)
  {
    if (val == 0)
      return;
    if (val >= 0)
      emit.emit(" + ");
    emit.emit(val);
  }

  private void emitPlusExpr(Expr val)
  {
    emit.emit(" + ");
    val.visit(this);
  }

  public void visitArrayIndexExpr(ArrayIndexExpr aie)
  {
    Type ait    = aie.getCoreType();
    Expr array  = aie.getArray();
    Type art    = array.getCoreType();
    Expr index  = aie.getIndex();
    Expr offset = aie.getOffset();

    emit.emit('(');

    addCast(ait, array);

    boolean doIndex = false;
    long    iv      = 0;
    if (index.isLiteralExpr()) {
      Literal liti = ((LiteralExpr) index).getLiteral();
      if (liti instanceof IntLiteral) {
        iv = ((IntLiteral) liti).getLongValue();
        doIndex = true;
      }
    }

    boolean doOffset = false;
    long    ov       = 0;
    if (offset.isLiteralExpr()) {
      Literal lito = ((LiteralExpr) offset).getLiteral();
      if (lito instanceof IntLiteral) {
        ov  = ((IntLiteral) lito).getLongValue();
        doOffset = true;
      }
    }

    if (doIndex && doOffset) {
      if ((iv + ov) != 0)
        emitPlusInteger(iv + ov);
    } else {
      if (doIndex) {
        if (iv != 0)
          emitPlusInteger(iv);
      } else
        emitPlusExpr(index);

      if (doOffset) {
        if (ov != 0)
          emitPlusInteger(ov);
      } else
        emitPlusExpr(offset);
    }

    emit.emit(')');
  }

  public void visitSubscriptExpr(SubscriptExpr e)
  {
    emit.emit("&(");
    doSubscriptExpr(e);
    emit.emit(')');    
  }

  public void visitPhiExpr(PhiExpr e)
  {
    emit.emit("Phi(");

    Expr[] array = e.getOperandArray();
    for (int i = 0; i < array.length; i++) {
      Expr arg = array[i];
      if (i > 0)
        emit.emit(", ");
      arg.visit(this);
    }
    emit.emit(')');    
  }

  public void visitExprPhiExpr(ExprPhiExpr e)
  {
    emit.emit("expPhi(");    
    Expr[] array = e.getOperandArray();
    for (int i = 0; i < array.length; i++) {
      ExprPhiExpr arg = (ExprPhiExpr) array[i];
      if (i > 0)
        emit.emit(", ");
      arg.visit(this);
    }
    emit.emit(')');
  }

  public void visitVectorExpr(VectorExpr e)
  {
    emit.emit("V(");    
    Expr[] array = e.getOperandArray();
    for (int i = 0; i < array.length; i++) {
      Expr arg = array[i];
      if (i > 0)
        emit.emit(", ");
      arg.visit(this);
    }
    emit.emit(')');    
  }

  public void visitMaxExpr(MaxExpr e)
  {
    emit.emit("max(");
    e.getOperand(0).visit(this);
    emit.emit(',');
    e.getOperand(1).visit(this);
    emit.emit(')');    
  }

  public void visitMinExpr(MinExpr e)
  {
    emit.emit("min(");
    e.getOperand(0).visit(this);
    emit.emit(',');
    e.getOperand(1).visit(this);
    emit.emit(')');    
  }
}
