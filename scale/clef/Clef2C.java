package scale.clef;

import java.io.*;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Arrays;

import scale.common.*;
import scale.frontend.SourceLanguage;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;
import scale.clef.type.*;
import scale.clef.symtab.*;

/**
 * A class to generate C code from a Clef AST.
 * <p>
 * $Id: Clef2C.java,v 1.4 2007-10-04 19:58:02 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class is used primarily to generate C for declarations.
 * However, it can also generate executable statements from a Clef
 * AST.
 * <p>
 * Note, that as a side effect, Clef2C may change the Clef tree (for
 * example, Clef2C changes the Clef tree to implement virtual
 * functions).
 * 
 * @see scale.clef
 */
public final class Clef2C implements scale.clef.Predicate
{
  /** 
   * The different passes made over types to generate
   * types/declarations.  We make two passes - a pass before the type
   * name is generated and a pass after the type name is generated.
   * In C, this is neccessary since the place where the name is placed
   * depends upon the type.
   *
   * We need to generate multiple passes for C types since C has
   * prefix and postfix type declaration attributes (such as "*"
   * (pointer), "[]" (array) ,"()" (function)
   */
  private static final int cNoPass   = 0; // Default value - means we aren't generating types.
  private static final int cPreName  = 1; // Generate type/declaration code before name
  private static final int cPostName = 2; // Generate type/declaration code after name

  private static final int    firstLabelNum = 1;     // The first label number that may be used.
  private static final String labelPrefix   = "_L_"; // Prefix characters to ensure we generate unique label names.
  private static final String idPrefix      = "__";  // Prefix characters to ensure we generate unique names.

  // If you change the position of any of these, then you
  // must also change the keyword string constants below.

  // If you add or delete keywords, then you must change the array
  // below.

  private final static String Keyword_AUTO     = "auto";
  private final static String Keyword_BREAK    = "break";
  private final static String Keyword_CASE     = "case";
  private final static String Keyword_CHAR     = "char";
  private final static String Keyword_CONST    = "const";
  private final static String Keyword_CONTINUE = "continue";
  private final static String Keyword_DEFAULT  = "default";
  private final static String Keyword_DO       = "do";
  private final static String Keyword_DOUBLE   = "double";
  private final static String Keyword_ELSE     = "else";
  private final static String Keyword_ENUM     = "enum";
  private final static String Keyword_EXTERN   = "extern";
  private final static String Keyword_FLOAT    = "float";
  private final static String Keyword_FOR      = "for";
  private final static String Keyword_GOTO     = "goto";
  private final static String Keyword_IF       = "if";
  private final static String Keyword_INT      = "int";
  private final static String Keyword_LONG     = "long";
  private final static String Keyword_REGISTER = "register";
  private final static String Keyword_RESTRICT = "restrict";
  private final static String Keyword_RETURN   = "return";
  private final static String Keyword_SHORT    = "short";
  private final static String Keyword_SIGNED   = "signed";
  private final static String Keyword_SIZEOF   = "sizeof";
  private final static String Keyword_STATIC   = "static";
  private final static String Keyword_STRUCT   = "struct";
  private final static String Keyword_SWITCH   = "switch";
  private final static String Keyword_TYPEDEF  = "typedef";
  private final static String Keyword_UNION    = "union";
  private final static String Keyword_UNSIGNED = "unsigned";
  private final static String Keyword_VOID     = "void";
  private final static String Keyword_VOLATILE = "volatile";
  private final static String Keyword_WHILE    = "while";

  private final static String[] keywords = {
    Keyword_AUTO,     Keyword_BREAK,    Keyword_CASE,     Keyword_CHAR,
    Keyword_CONTINUE, Keyword_CONST,    Keyword_DEFAULT,  Keyword_DO,
    Keyword_DOUBLE,   Keyword_ELSE,     Keyword_ENUM,     Keyword_EXTERN,
    Keyword_FLOAT,    Keyword_FOR,      Keyword_GOTO,     Keyword_IF,
    Keyword_INT,      Keyword_LONG,     Keyword_REGISTER, Keyword_RESTRICT,
    Keyword_RETURN,   Keyword_SHORT,    Keyword_SIGNED,   Keyword_SIZEOF,
    Keyword_STATIC,   Keyword_STRUCT,   Keyword_SWITCH,   Keyword_TYPEDEF,
    Keyword_UNION,    Keyword_UNSIGNED, Keyword_VOID,     Keyword_VOLATILE,
    Keyword_WHILE,
  };

  private Emit           emit;       // The Emit class used for generating output.
  private SourceLanguage lang;       // Information that is specific to the source language 
  private Declaration    myDecl;     // The declaration associated with a type.
  private Hashtable<String, Integer> labelToInt; // Map label strings to integers (needed for fortran)
  private int            labelNum;   // Current label number. We need to generate labels for certain Clef.
  private int            typePass;   // The current pass number for type declarations.  
  private int            tmpVarCntr; // For generated temporary variable names.

  private boolean genInitialValue;       // Do we want to generate the initializer for a declaration?
  private boolean inAggregationElements; // true if processing AggregationElements
  private boolean genForwardDecl;        // Generate a forward declaration or the complete function?
  private boolean genFullProcedureType;  // Do we want the formals and exceptions wanted?
  private boolean needsParens;           // Should the expression use parens?
  private boolean onLHS;                 // Is the expression on the left-hand-side?

  /**
   * A flag which indicates if we are generating an aggregate/enum
   * definition or reference.  That is,
   * <pre>
   *   struct A {int a;};
   * </pre>
   * versus
   * <pre>
   *  struct A;
   * </pre>
   * By default, we generate a type reference <code>(inTypeRef ==
   * true)</code>.  But, in declarations, we generate a type
   * definition <code>(inTypeRef == false)</code>.
   */ 
  private boolean inTypeRef;
  /**
   * A flag which indicates if we are generating a type for a cast
   * operation.  Special rules apply to this case in order to avoid
   * ambiguity.
   */
  private boolean castType;

  /**
   * Construct a C language code generator - output goes to a file.
   * @param emit is the Emit instance used to generate the C program.
   * @param lang the source language
   * @see scale.common.Emit
   */
  public Clef2C(Emit emit, SourceLanguage lang)
  {
    this.emit                  = emit;
    this.lang                  = lang;
    this.typePass              = cNoPass;
    this.labelToInt            = new Hashtable<String, Integer>();
    this.labelNum              = firstLabelNum;
    this.tmpVarCntr            = 0;
    this.genFullProcedureType  = false;
    this.genInitialValue       = true;
    this.genForwardDecl        = false;
    this.inAggregationElements = false;
    this.inTypeRef             = true; // By default, generate type references.
    this.castType              = false;
    this.needsParens           = false;
    this.onLHS                 = false;
  }

  /**
   * Return true if the identifier is a C keyword.
   * @param kw the identifier to check
   */
  private boolean isKeyword(String kw) 
  {
    for (int i = 0; i<keywords.length; i++) {
      if (kw.equals(keywords[i])) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return the string file name extension for C - <code>.c</code>.
   */
  public String fileExt()
  {
    return ".c";
  }

  /**
   * Generate code for Clef tree.
   * @param root root node of the Clef tree
   */
  public final void codeGen(Node root)
  {
    root.visit(this);
  }

  /**
   * We maintain a mapping between labels and integer values for
   * Fortran labels (i.e., for assign statements).  The label
   * index may be reset at procedure boundaries.
   */
  private int mapLabelToInt(String label) 
  {
    Integer num = labelToInt.get(label);

    if (num == null) {
      int retval = labelNum++;
      labelToInt.put(label, new Integer(retval));
      return retval;
    }

    return  num.intValue();
  }

  /**
   * Generate code for declarations.  We obtain the declarations from
   * the symbol table.  We don't generate declarations for:
   * <ul>
   * <li>Labels
   * <li>Field Declarations (this triggers an error)
   * <li>C <code>enum</code> elements
   * </ul>
   * The code in {@link #visitLabelDecl visitLabelDecl} should be
   * empty (so that we don't generate the declaration).  We should
   * never see a {@link scale.clef.decl.FieldDecl FieldDecl} node -
   * they always appear in the scope of the construct they are defined
   * in (e.g., struct, class, etc..). We need to make an explicit
   * check for {@link scale.clef.decl.EnumElementDecl
   * EnumElementDecl}.
   */
  private void genDecls(SymtabScope scope)
  {
    if (scope == null)
      return;

    Enumeration<SymtabEntry> e = scope.orderedElements();
    while (e.hasMoreElements()) {
      SymtabEntry s = e.nextElement();
      Declaration d = s.getDecl();
      // We shouldn't see a field decl here!
      assert !(d instanceof FieldDecl) : "FieldDecl found in list of declarations " + d;
      // Do not generate declarations for enum elements.
      if (d instanceof EnumElementDecl)
        continue;
      if (d.isRoutineDecl())
        continue;
      if (d instanceof FormalDecl)
        continue;

      d.visit(this);
    }
  }

  /**
   * Create a unique label name - we prepend the label with "_L_"
   * to ensure that label is legal for C/C++ and that it is unique.
   * For example, fortran labels are numbers which is illegal in C.
   */
  private String labelName(String n)
  {
    return labelPrefix + n;
  }

  /**
   * Generate the type for a cast.  Special rules apply to this case to
   * avoid ambiguity when programs write crap like
   * <pre>
   * Person *Person = (Person *) 0;
   * </pre>
   */
  public void genCastType(Type t)
  {
    boolean savect = castType;
    castType = true;
    genDeclarator(t, "");
    castType = savect;
  }
 
  /**
   * Convert the name of an EquivalenceDecl variable to a reference to
   * its base variable.
   * @param ed is the Declaration
   * @param address is true if an address form is needed
   */
  private String convertEquivalenceDeclName(EquivalenceDecl ed, boolean address)
  {
    String       name = ed.getName();
    StringBuffer buf  = new StringBuffer(address ? "" : "(*");
    Emit         se   = emit;
    VariableDecl base = ed.getBaseVariable();
    Type         edt  = ed.getType();
    Type         edct = edt.getCoreType();
    long         bo   = ed.getBaseOffset();

    emit = new EmitToString(buf, 0);

    ArrayType at = edct.getCoreType().returnArrayType();
    if (at != null)
      edt = at.getElementType();

    if (bo == 0) {
      buf.append("((");
      genCastType(PointerType.create(edt));
      buf.append(") ");
      if (!base.getCoreType().isArrayType())
        buf.append('&');
      buf.append(base.getName());
      buf.append(") /* ");
      buf.append(name);
      buf.append(" */");

      if (!address)
        buf.append(')');

      emit = se;
      return buf.toString();
    }

    buf.append("((");
    genCastType(PointerType.create(edt));
    buf.append(") (((char *)");
    if (!base.getCoreType().isArrayType())
      buf.append('&');
    buf.append(base.getName());
    buf.append(')');

    buf.append(" + ");
    buf.append(bo);
    buf.append(") /* ");
    buf.append(name);
    buf.append(" */)");

    if (!address)
      buf.append(')');

    emit = se;
    return buf.toString();
  }

  /**
   * Create a unique identifier name given a <i>Declaration</i> node.
   * We may have to add to the name to make it a legal name - we make
   * the following changes:
   * <ul>
   * <li>We mangle the name (if the source is C++).
   * <li> We prepend any identifier that is a keyword with two underscores 
   * so that we get a unique identifier.
   * <li> If name has local linkage we also prepend with two underscores
   * so that we get a unique identifier that doesn conflict with names
   * defined elsewhere (this can easily happen in C++ (ie., g vs. ::g)
   * <li> If the declaration is a formal parameter passed by reference, we
   * need to dereference the value.
   * </ul>
   *
   * @param d the declaration of the name
   * @return a legal identifier name that has possibly been altered
   */
  private String identifierName(Declaration d)
  {
    if (d.isEquivalenceDecl()) // Create reference to the base variable.
      return convertEquivalenceDeclName((EquivalenceDecl) d, false);

    String name = d.getName();

    if (isKeyword(name) || (name.equals("main") && !lang.mainFunction())) {
      // Check if we need to prepend the name with an underscore -
      // note a mangled name should never need to be prepended.
      name = idPrefix + name;
    }

    return name;
  }

  /**
   * Create a unique identifier name given a {@link
   * scale.clef.expr.IdReferenceOp IdReferenceOp} node.
   * <p>
   * There is a special case when the name is a class member.  We need
   * to determine the object class to determine the relationship between
   * the object class and the class that the member belongs too.  Either
   * the classes are the same or the class of the member is a super
   * class of the object class.  We represent the super class as a field
   * in the derived class.
   * @param id the <i>IdReferenceOp</i> node.
   */
  private String identifierName(IdReferenceOp id)
  {
    Declaration d    = id.getDecl();
    String      name = identifierName(d);
      
    // If its not a class member (or there is some error) then just try
    // to print out the name using the declaration.
    return name;
  }

  /**
   * Convert the name of the declaration for display.  If it is an
   * EquivalenceDecl, create a reference to the base variable.
   * @param decl is the Declaration
   * @param address is true if an address form of the name is needed
   */
  public String convertDeclName(Declaration decl, boolean address)
  {
    String name;

    if (decl.isEquivalenceDecl()) // Create reference to the base variable.
      name = convertEquivalenceDeclName((EquivalenceDecl) decl, address);
    else
      name = decl.getName();

    return name.replace('#', '_');
  }

  /** 
   * Generate code for a list of formal arguments.
   */
  private void genFormals(ProcedureType s)
  {
    int l = s.numFormals();

    emit.emit('(');    
   
    for (int i = 0; i < l; i++) {
      s.getFormal(i).visit(this);
      if (i < (l - 1)) 
        emit.emit(", ");
    }

    emit.emit(')');
  }

  /** 
   * Generate code for the actual arguments for a function call.
   * @param f the clef node for a regular function call
   */
  private void genActuals(CallFunctionOp f)
  {
    boolean savenp = needsParens;
    needsParens = false;
    emit.emit('(');
    genArgumentList(f, f.getProcedureInfo());
    emit.emit(')');
    needsParens = savenp;
  }

  /**
   * Generate code for the actuals arguments to a function.  We check
   * each actual against the formals to determine how the arguments
   * are passed (eg., by reference or by value).  If there are no formals
   * at all then the signature parameter is null.
   * 
   * @param c is the call operation
   * @param s the signature of the function (maybe null)
   */
  private void genArgumentList(CallOp c, ProcedureType s)
  {
    int l = c.getNumArgs();
    for (int i = 0; i < l; i++) {
      if (i > 0) 
        emit.emit(", ");
      c.getArg(i).visit(this);
    }
  }

  /** 
   * Generate the C code for a type reference.  We use this routine
   * for <code>structs</code>, <code>unions</code>,
   * <code>classes</code>, and <code>enums</code>.
   * <p>
   * We do not emit the name of the type we are currently generating a
   * type declaration (using TypeDecl or TypeName).  That is the
   * responsibility of {@link #genDeclarator
   * genDeclarator}.
   *
   * @param kind a string representing the C type (e.g., struct)
   */
  private void genTypeReference(String kind) 
  {
    if (myDecl == null) {
      emit.emit(kind);
      return;
    }

    if (myDecl instanceof TypeDecl) {
      emit.emit(kind);
      emit.emit(' ');
      emit.emit(identifierName(myDecl));
      return;
    }

    if (!castType && (myDecl instanceof TypeName)) {
      // Only generate the typedef for a type reference.
      emit.emit(identifierName(myDecl));  // ie., a typedef
      return;
    }

    // The name will be generated elsewhere (ie., genDeclarator).

    emit.emit(kind);
  }

  /** 
   * Generate the C code for a declarator.  This is difficult in C due to
   * pointers, arrays, and function types.  It takes two passes to
   * generate the declarator - this routine is a wrapper which makes
   * sure the two passes are made. <br> We generate type specifiers
   * here as well.  Except for pointers because the type specifier is
   * placed after the "*" symbol.
   * <p>
   * On the first pass we traverse the type to find the simple
   * type.  On the way back up the clef tree representing the type, we
   * generate pointer declarators.  Then, we make another traversal of
   * the type tree and generate arrays and functions.<br> The routine
   * generates superfluous parens - many could be removed by making
   * simple checks for precedence relations.
   *
   * @param t the clef type to generate in C
   * @param v the name of the defined object 
   */
  public void genDeclarator(Type t, String v) 
  {
    // Save the old value of typePass so that we can restore it at the
    // end.

    int saveTypePass = typePass;

    // First pass over type (before name).

    typePass = cPreName;
    t.visit(this);

    // Generate name.

    emit.emit(' ');
    emit.emit(v);

    // Second pass over type (after name).

    typePass = cPostName;
    t.visit(this);

    typePass = saveTypePass;
  }

  /** 
   * Generate the C code for a declarator.  This differs from {@link
   * #genDeclarator genDeclarator} in that the full procedure
   * declaration is generated.
   */
  public void genDeclaratorFull(Type t, String v) 
  {
    boolean savefpt = genFullProcedureType;
    genFullProcedureType = true;
    genDeclarator(t, v);
    genFullProcedureType = savefpt;
  }

  /**
   * Generate code for a list of statements.  If there if just one
   * statement then we surround the statement by braces.  We do that
   * in case we need to generate temporaries for the single statement
   * (which means there will be multiple statements).  If the
   * statement is a {@link scale.clef.stmt.BlockStmt BlockStmt} node
   * then the braces are generated in {@link
   * #visitBlockStmt visitBlockStmt()}.
   * 
   * @param s a Clef statement node
   */
  private void genStatements(Statement s)
  {
    if (!(s instanceof BlockStmt)) {
      emit.emit('{');
      emit.incIndLevel();
      emit.endLine();
    }
    s.visit(this);

    if (!(s instanceof BlockStmt)) {
      emit.decIndLevel();
      emit.emit('}');
      emit.endLine();
    }
  }

  public void visitNode(Node n) {}
  public void visitExceptionDecl(ExceptionDecl d) {}
  public void visitCaseLabelDecl(CaseLabelDecl d) {}
  public void visitDeclaration(Declaration n) {}
  public void visitRoutineDecl(RoutineDecl d) {}
  public void visitRealType(RealType d) {}
  public void visitRaiseWithType(RaiseWithType d) {}
  public void visitRaiseWithObject(RaiseWithObject d) {}
  public void visitRaise(Raise d) {}
  public void visitNumericType(NumericType d) {}
  public void visitCompositeType(CompositeType d) {}
  public void visitAtomicType(AtomicType d) {}
  public void visitAltCase(AltCase d) {}
  public void visitIfStmt(IfStmt d) {}
  public void visitTestLoopStmt(TestLoopStmt d) {}
  public void visitLoopStmt(LoopStmt d) {}
  public void visitStatement(Statement d) {}
  public void visitVarArgOp(VarArgOp d) {}
  public void visitTernaryOp(TernaryOp d) {}
  public void visitSubstringOp(SubstringOp d) {}
  public void visitIncrementOp(IncrementOp d) {}
  public void visitHeapOp(HeapOp d) {}
  public void visitDyadicOp(DyadicOp d) {}
  public void visitDeleteOp(DeleteOp d) {}
  public void visitDeleteArrayOp(DeleteArrayOp d) {}
  public void visitCompoundAssignmentOp(CompoundAssignmentOp d) {}
  public void visitCallOp(CallOp d) {}
  public void visitAssignmentOp(AssignmentOp d) {}
  public void visitMonadicOp(MonadicOp d) {}
  public void visitAllocateSettingFieldsOp(AllocateSettingFieldsOp d) {}
  public void visitAllocatePlacementOp(AllocatePlacementOp d) {}
  public void visitAggregateOp(AggregateOp d) {}

  /**
   * Generate code for declaring a type.  We assume that the name of
   * the type declaration is generated by the type.  That is, the type
   * looks at the parent (the {@link scale.clef.decl.TypeDecl
   * TypeDecl} node) to determine the name.  We need to do this
   * because different types place names in different places.
   */
  public void visitTypeDecl(TypeDecl td)
  {
    Type    t    = td.getType();
    boolean sitr = inTypeRef;

    inTypeRef = false;  // we want to generate a type decl.
    genDeclarator(t, "");
    inTypeRef = sitr;
    emit.emit(';');
    emit.endLine();
  }

  /**
   * Generate the C code for a {@link scale.clef.decl.TypeDecl TypeDecl}.
   */
  public void genTypeDecl(TypeDecl td)
  {
    boolean  sitr = inTypeRef;
    inTypeRef = true;
    genDeclarator(td.getType(), "");
    inTypeRef = sitr;
    emit.emit(';');
    emit.endLine();
  }

  public void visitTypeName(TypeName tn)
  {
    Type    t    = tn.getType();
    String  na   = identifierName(tn);
    boolean sitr = inTypeRef;

    if (na.equals("va_list"))
      return;

    if (na.equals("__builtin_va_list"))
      return;

    RefType rt = t.returnRefType();
    if (rt != null) { // Skip the reference back to this typedef.
      Declaration d  = rt.getDecl();
      if (d == tn)
        t = rt.getRefTo();
    }

    boolean gnudef = false;
    if (na.startsWith("__")) {
      if ("__PTRDIFF_TYPE__".equals(na)) {
        emit.emit("#ifndef __PTRDIFF_TYPE__");
        emit.endLine();
        gnudef = true;
      } else if ("__SIZE_TYPE__".equals(na)) {
        emit.emit("#ifndef __SIZE_TYPE__");
        emit.endLine();
        gnudef = true;
      } else if ("__WCHAR_TYPE__".equals(na)) {
        emit.emit("#ifndef __WCHAR_TYPE__");
        emit.endLine();
        gnudef = true;
      }
    }

    emit.emit(Keyword_TYPEDEF);
    emit.emit(' ');
    inTypeRef = true;  // we want to genereate a type name.
    genDeclarator(t, na);
    inTypeRef = sitr;
    emit.emit(';');
    emit.endLine();

    if (gnudef) {
      emit.emit("#endif");
      emit.endLine();
    }
  }

  public void visitValueDecl(ValueDecl v)
  {
    String name = identifierName(v).replace('#', '_');
 
    genDeclarator(v.getType(), name);

    Expression value = v.getValue();
    if (genInitialValue && (value != null)) {
      emit.emit(" = ");

      if ((value instanceof Literal)  && !(value instanceof AggregationElements)) {
        Literal lit = (Literal) value;

        if (v.getCoreType().isCompositeType()) {
          emit.emit('{');
          value.visit(this);
          emit.emit('}');
          return;
        }
      }

      value.visit(this);
    }
  }

  public void visitLabelDecl(LabelDecl n)
  {
    // We don't do anything for label declarations.  The real work
    // is done by other routines.
  }

  public void visitVariableDecl(VariableDecl d)
  {
    Visibility vis = d.visibility();
    if ((vis == Visibility.FILE) ||
        ((vis == Visibility.LOCAL) && (d.residency() == Residency.MEMORY))) {
      emit.emit(Keyword_STATIC); // Make sure static or extern is first.
      emit.emit(' ');
    } else if (vis == Visibility.EXTERN) {
      emit.emit(Keyword_EXTERN);
      emit.emit(' ');
    }

    visitValueDecl(d);

    emit.emit(';');
    emit.endLine();
  }

  public void visitRenamedVariableDecl(RenamedVariableDecl n)
  {
    visitVariableDecl(n);
  }

  /**
   * Generates the C code for Fortran <code>EQUIVALENCE</code>
   * relations.  The equivalenced array is converted to a C pointer
   * variable that is initialized to an address using the equivalence
   * relationship.
   */
  public void visitEquivalenceDecl(EquivalenceDecl ed)
  {
    emit.emit('/');
    emit.emit("* ");

    if ((ed.visibility() == Visibility.FILE) ||
        (ed.residency() == Residency.MEMORY)) {
      emit.emit(Keyword_STATIC); // Make sure static or extern is first.
      emit.emit(' ');
    } else if (ed.visibility() == Visibility.EXTERN) {
      emit.emit(Keyword_EXTERN);
      emit.emit(' ');
    }

    String name = ed.getName().replace('#', '_');

    genDeclarator(ed.getType(), name);

    emit.emit("; Ignore this equivalenced variable */");
    emit.endLine();
  }

  /**
   * Generate code for formal declarations.  In C, we do not generate
   * the default expression, since it isn't legal (so we can't call
   * {@link #visitValueDecl visitValueDecl} to generate the
   * declaration). The default value is handled by the caller.
   *
   * @param fd the FormalDecl node
   */
  public void visitFormalDecl(FormalDecl fd)
  {
    genDeclarator(fd.getType(), identifierName(fd));
  }
  
  public void visitUnknownFormals(UnknownFormals n)
  {
    emit.emit("...");
  }

  public void visitFieldDecl(FieldDecl fd)
  {
    visitValueDecl(fd);

    int bits = fd.getBits();
    if (bits > 0) {
      emit.emit(": ");
      emit.emit(bits);
    }
    emit.emit(';');
    emit.endLine();
  }

  /**
   * Generate the attributes (e.g., <code>static</code>) for a function.
   */
  public void genRoutineAttributes(RoutineDecl p)
  {
    Visibility visibility = p.visibility();
    if ((visibility == Visibility.FILE) ||
        (visibility == Visibility.LOCAL)) {
      emit.emit(Keyword_STATIC); // Make sure static or extern is first.
      emit.emit(' ');
    } else if (p.visibility() == Visibility.EXTERN) {
      emit.emit(Keyword_EXTERN);
      emit.emit(' ');
    }
  }

  public void visitProcedureDecl(ProcedureDecl p)
  {
    // Remember we generate functions using two passes.  So, if we
    // aren't generating forward declarations and the function is a
    // specification, then do not generate anything.

    if (p.isSpecification() && !genForwardDecl)
       return;

    genRoutineAttributes(p);
    genDeclaratorFull(p.getSignature(), identifierName(p));
    
    // Sometimes we call this routine from visitForwardProcedureDecl
    // or we just want to generate a forward declaration.

    if (!(p instanceof ForwardProcedureDecl) &&
        !p.isSpecification() && !genForwardDecl) {
      // We can initialize the label number stuff at proc. boundaries.
      labelNum = firstLabelNum;
      labelToInt.clear();
      emit.endLine();
      p.getBody().visit(this);

      if (p.isMain() && !lang.mainFunction()) {
        // This is the main program, so we create a C main routine which
        // just calls this routine for source languages that do not 
        // contain a main routine.

        emit.emit(Keyword_INT);
        emit.emit(' ');
        emit.emit("main(");
        emit.emit(Keyword_INT);
        emit.emit(" argc, ");
        emit.emit(Keyword_INT);
        emit.emit(" *argv[]) {");
        emit.endLine();
        emit.incIndLevel();
        emit.emit(identifierName(p));
        emit.emit("(argc, argv);");
        emit.endLine();
        emit.decIndLevel();
        emit.emit('}');
        emit.endLine();
      }
    } else {
      emit.emit(';');
      emit.endLine();
    }
  }

  /**
   * Generate the declaration for a routine that will be fully defined
   * later.
   */
  public void genForwardRoutineDecl(RoutineDecl p)
  {
    boolean sgfd = genForwardDecl;
    genForwardDecl = true;
    p.visit(this);
    genForwardDecl = sgfd;
  }

  public void visitForwardProcedureDecl(ForwardProcedureDecl n)
  {
    visitProcedureDecl(n);
  }

  public void visitFileDecl(FileDecl n)
  {
    int l = n.getNumDecls();

    // First, generate types.

    for (int i = 0; i < l; i++) {
      Declaration child = n.getDecl(i);
      if (child instanceof TypeDecl) {
        ((TypeDecl) child).visit(this);
      }
    }

    // Second, generate typedefs.
    for (int i = 0; i < l; i++) {
      Declaration child = n.getDecl(i);
      if (child instanceof TypeName) {
        ((TypeName) child).visit(this);
      }
    }

    // Generate declarations that have initializers - without
    // generating the actual initializers.  That will be done in the
    // next loop.  We need to do this to generate correct code for
    // recursive declarations/initializations (e.g., <code>void *x;
    // void *y; void *x = (void *)&y; void *y = (void *)&x</code>)

    genInitialValue = false;

    for (int i = 0; i < l; i++) {
      Declaration child = n.getDecl(i);
      if ((child instanceof ValueDecl) && (((ValueDecl) child).getValue() != null)) {
        // Okay, we've hit a declaration with an initialzer!
        ((ValueDecl) child).visit(this);
      }
    }

    genInitialValue = true;

    // We generate code for all declarations here.  However, we only
    // generate forward declarations for functions.  We will make a
    // separate pass to generate the whole definition after all the
    // declarations have been generated.  We need to do this because
    // we use a single Clef node to represent both a function
    // specification (ie., just the prototype) and a function
    // definition. Note, class type are responsible for generating
    // declaration in the correct order.

    genForwardDecl = true;

    int nrds = 0;
    for (int i = 0; i < l; i++) {
      Declaration child = n.getDecl(i);
      if (child == null)
        continue;

      if (!((child instanceof TypeName) || (child instanceof TypeDecl))) {
        child.visit(this);
      }

      if (child instanceof RoutineDecl)
        nrds++;
    }

    genForwardDecl = false;

    // Okay, we can generate the complete definition for all functions.

    RoutineDecl[] rds = new RoutineDecl[nrds];
    nrds = 0;

    for (int i = 0; i < l; i++) {
      Declaration child = n.getDecl(i);
      if (child instanceof RoutineDecl)
        rds[nrds++] = (RoutineDecl) child;
    } 

    Arrays.sort(rds);
    for (int i = 0; i < nrds; i++)
      rds[i].visit(this);
  }

  public void visitEnumElementDecl(EnumElementDecl d)
  {
    emit.emit(identifierName(d));
    emit.emit(" = ");
    d.getValue().visit(this);
  }

  public void visitType(Type n)
  {
  } 

  public void visitVoidType(VoidType n)
  {
    if (typePass != cPreName)
      return;

    emit.emit(Keyword_VOID);
  } 

  private void processTypeAttribute(RefAttr attribute, boolean pre)
  {
    if (typePass != cPreName)
      return;

    String str = "";

    switch (attribute) {    // Map the type attributes into Clef2C values.
    case None:
    case Traced:
    case Ordered:
    case Aligned:
      return;
    case VaList:
      str = "va_list";
      break;
    case Const:
      str = Keyword_CONST;
      break;
    case Volatile:
      str = Keyword_VOLATILE;
      break;
    case Restrict:
      str = Keyword_RESTRICT;
      break;
    default:
      throw new scale.common.InternalError("Type Attribute - Unknown Value " + attribute);
    }

    if (!pre)
      emit.emit(' ');

    emit.emit(str);

    if (pre)
      emit.emit(' ');
  }

  /**
   * Generate code for a {@link scale.clef.type.RefType RefType}.  No
   * type code is actually generated for a {@link
   * scale.clef.type.RefType RefType} node.  Instead, the {@link
   * scale.clef.type.RefType RefType} node may contain attributes
   * which we need to generate code for.  The order that the
   * attributes are generated depends upon the real type that the
   * {@link scale.clef.type.RefType RefType} contains.
   */
  public void visitRefType(RefType ref)
  {
    Type        ct         = ref.getCoreType();
    RefAttr     attr       = ref.getAttribute();
    Type        refTo      = ref.getRefTo();
    Declaration saveMyDecl = myDecl;
    boolean     ptype      = (ct.isPointerType() || ct.isArrayType());
    Declaration d          = ref.getDecl();

    if ((myDecl == null) || (castType && (d instanceof TypeDecl)))
      myDecl = d;

    if (!ptype)
      processTypeAttribute(attr, true);

    if (attr != RefAttr.VaList)
      refTo.visit(this);

    if (ptype)
      processTypeAttribute(attr, false);

    myDecl = saveMyDecl;
  } 

  /**
   * Generate code for a procedure type. Again, this is a two pass
   * algorithm when the type is used in a declarator.  On the first,
   * pass we generate the return type. On the second pass, we generate
   * a parameter list.  The function name is generate in between.
   * 
   * @param p is the clef node for a ProcedureType
   */
  public void visitProcedureType(ProcedureType p)
  {
    Type returnType = p.getReturnType();

    if (genFullProcedureType) {
      if (typePass == cPostName)
        genFormals(p);
      returnType.visit(this);
//       if (typePass == cPostName)
//         genExceptionList(p.getRaises());
      return;
    }

    boolean needsParens = returnType.precedence() > p.precedence();

    if (typePass == cPostName) {
      genFormals(p);
      if (needsParens)
        emit.emit(')');
    }

    Declaration saveMyDecl = myDecl;
    myDecl = null;

    returnType.visit(this);

    myDecl = saveMyDecl;

    if ((typePass == cPreName) && needsParens)
      emit.emit('(');
  } 

  public void visitIncompleteType(IncompleteType n)
  {
    Type ct = n.getCompleteType();
    assert (ct != null) : "Incomplete type not completed " + n;
    ct.visit(this);
  } 

  /**
   * Generate code for a pointer type.  If type attributes are specified
   * then they are placed after the pointer type (for all other types,
   * the attributes are placed before).  This enables us to generate
   * the correct code both a <i>constant pointer</i> and a <i>pointer to
   * constant data</i> which are defined as follows:
   * <pre>
   *    int * const const_pointer
   *    const int *pointer_to_const;
   * </pre>.
   * Note: we make multple passes over the type.  The generated code
   * depends upon which pass.
   * @param p the pointer type clef node
   */
  public void visitPointerType(PointerType p)
  {
    if (!castType && (myDecl instanceof TypeName)) {
      if (typePass != cPreName)
        return;
      // Only generate the typedef for a type reference.
      emit.emit(identifierName(myDecl));  // ie., a typedef
      return;
    }

    Type    pointedTo   = p.getPointedTo();
    boolean needsParens = pointedTo.precedence() > p.precedence();

    if ((typePass == cPostName) && needsParens)
      emit.emit(')');

    pointedTo.visit(this);

    if (typePass == cPreName) {
      if (needsParens)
        emit.emit(" (*");
      else 
        emit.emit(" *");
    }
  }

  /**
   * A generic array.  In most cases, we'll call one of the subclasses
   * of array type (ie., fixed array, open array).
   * <p>
   * To generate code for arrays, we may need to make two passes.
   * One the first pass, we generate the element type.  On the second
   * pass, we generate the array index expression.  In between, we
   * generate the name of the array.
   *
   * @param at is an array type
   */
  public void visitArrayType(ArrayType at)
  {
    if (!castType && (myDecl instanceof TypeName)) {
      if (typePass == cPreName) // Only generate the typedef for a type reference.
        emit.emit(identifierName(myDecl));  // ie., a typedef
      return;
    }

    Type    elementType = at.getElementType();
    boolean needsParens = elementType.precedence() > at.precedence();

    if (typePass == cPreName) {
      elementType.visit(this);
      if (needsParens)
        emit.emit(" (");
      return;
    }

    // Generate C code for the index part of an array declaration
    // (i.e., the []).  We only generate the array declarator on pass
    // 2.  A pointer declarator is generated on pass 1.

    int l = at.getRank();
    for (int i = 0; i < l; i++) {
      emit.emit('[');
      at.getIndex(i).visit(this);
      emit.emit(']');
    }

    if (needsParens)
      emit.emit(')');

    elementType.visit(this);
  } 

  /**
   * Generate the C code for an aggregate type.  Basically, just print
   * out the fields.
   * <p>
   * We assume that the aggregate head has already been generated via
   * a call to genTypeNameProlog() and that the enclosing braces are
   * generated.
   * @see scale.clef.type.RecordType
   * @see scale.clef.type.UnionType
   */
  public void visitAggregateType(AggregateType agg)
  {
    if (typePass != cPreName)
      return;

    if (!castType && (myDecl instanceof TypeName)) { // Only generate the typedef for a type reference.
      emit.emit(identifierName(myDecl));  // ie., a typedef
      return;
    }

    // We only want to print the names of the fields - not the entire
    // definition (eg., for a struct).  The code will still be correct
    // if a definition occurs within the fields though (via a typedecl
    // or typename).

    boolean sitr = inTypeRef;
    inTypeRef = true;

    Declaration saveMyDecl = myDecl;
    myDecl = null;

    // Generate the fields

    Vector<FieldDecl> av = agg.getAgFields();
    int               al = av.size();
    for (int i = 0; i < al; i++)
      av.elementAt(i).visit(this);

    inTypeRef = sitr;
    myDecl = saveMyDecl;
  } 

  public void visitComplexType(ComplexType type)
  {
    if (typePass != cPreName)
      return;

    if (!castType && (myDecl instanceof TypeName)) { // Only generate the typedef for a type reference.
      emit.emit(identifierName(myDecl));  // ie., a typedef
      return;
    }

    emit.emit(type.mapTypeToCString());
  } 

  public void visitBound(Bound b)
  {
    if (b == Bound.noBound)
      return;

    if (b == Bound.noValues) {
      emit.emit('0');
      return;
    }

    try {
      long sz = b.numberOfElements();
      emit.emit(sz);
      if (sz != (int) sz)
        emit.emit('L');
      return;
    } catch (scale.common.InvalidException ex) {
      emit.emit('1');
      return;
    }
  } 

  /**
   * Generate code for a fixed array (same as a regular array). 
   * To generate code for arrays, we may need to make two passes.
   * One the first pass, we generate the element type.  On the second
   * pass, we generate the array index expression.  In between, we
   * generate the name of the array.
   *
   * @param n a FixedArray Clef node
   */
  public void visitFixedArrayType(FixedArrayType n)
  {
    visitArrayType(n);
  } 

  /**
   * Generate code for a n allocatable array.  To generate code for
   * arrays, we need generate code for the structure that represents
   * the array at run time.
   */
  public void visitAllocArrayType(AllocArrayType n)
  {
    n.getStruct().visit(this);
  } 

  /**
   * Generate C code for a record type.  Similar code is generated for
   * records, unions, and classes.
   *
   * @see #visitAggregateType
   */
  public void visitRecordType(RecordType rt)
  {
    // Classes are only generated during the first pass.

    if (typePass != cPreName)
      return;

    // Only generate name if we want a type reference.
    // Also, we generate {} if the record has no name.
    // Also, we don't generate {} if the record has no fields.

    if (!inTypeRef || (myDecl == null)) {
      // Call the super class routine to generate the body.
        genTypeReference(Keyword_STRUCT);
      if  (rt.numFields() > 0) {
        emit.emit(" {");
        emit.endLine();
        emit.incIndLevel();
        visitAggregateType(rt);
        emit.decIndLevel();
        emit.emit('}');
      }
    } else
      genTypeReference(Keyword_STRUCT);
  }

  public void visitFloatType(FloatType type)
  {
    if (typePass != cPreName)
      return;

    if (!castType && (myDecl instanceof TypeName)) { // Only generate the typedef for a type reference.
      emit.emit(identifierName(myDecl));  // ie., a typedef
      return;
    }

    emit.emit(type.mapTypeToCString());
  } 

  public void visitIntegerType(IntegerType type)
  {
    if (typePass != cPreName)
      return;

    if (!castType && (myDecl instanceof TypeName)) {
      // only generate the typedef for a type reference
      emit.emit(identifierName(myDecl));  // ie., a typedef
      return;
    }

    // What we do here depends upon the size of the integer.

    emit.emit(type.mapTypeToCString());
  } 

  public void visitSignedIntegerType(SignedIntegerType type)
  {
    visitIntegerType(type);
  }

  public void visitUnsignedIntegerType(UnsignedIntegerType type)
  {
    visitIntegerType(type);
  }

  public void visitFortranCharType(FortranCharType type)
  {
    if (typePass == cPreName) {
      emit.emit("char");
      return;
    }

    int len = type.getLength();
    if (len <= 0)
      len = 1;

    emit.emit('[');
    emit.emit(len);
    emit.emit(']');
  }

  public void visitEnumerationType(EnumerationType et)
  {
    // Classes are only generated during the first pass.

    if (typePass != cPreName)
      return;

    if (!castType && (myDecl instanceof TypeName)) { // Only generate the typedef for a type reference.
      emit.emit(identifierName(myDecl));  // ie., a typedef
      return;
    }

    genTypeReference(Keyword_ENUM);

    // Check to see if we only want to generate the enum name.

    if (!inTypeRef || (myDecl == null)) {
      emit.emit(" {");
      emit.endLine();
      emit.incIndLevel();

      int l = et.getNumEnums();
      for (int i = 0; i < l; i++) {
        if (i > 0)
          emit.emit(", ");
        et.getEnum(i).visit(this);
      }

      emit.decIndLevel();
      emit.endLine();
      emit.emit('}');
    }
  } 

  public void visitUnionType(UnionType ut)
  {
    if (typePass != cPreName)
      return;

    genTypeReference(Keyword_UNION);

    // Only generate name if we want a type reference.
    // Also, we generate {} if the union has no name.
    // Also, we don't generate {} if the union has no fields.

    if ((!inTypeRef || (myDecl == null)) && (ut.numFields() > 0)) {
      emit.emit(" {");
      emit.endLine();
      emit.incIndLevel();
      visitAggregateType(ut);
      emit.decIndLevel();
      emit.emit('}');
    }
  }

  public void visitCharacterType(CharacterType ct)
  {
    if (typePass != cPreName)
      return;

    if (!castType && (myDecl instanceof TypeName)) { // Only generate the typedef for a type reference.
      emit.emit(identifierName(myDecl));  // ie., a typedef
      return;
    }

    emit.emit(ct.mapTypeToCString());
  } 

  public void visitBooleanType(BooleanType n)
  {
    if (typePass != cPreName)
       return;

    if (!castType && (myDecl instanceof TypeName)) { // Only generate the typedef for a type reference.
      emit.emit(identifierName(myDecl));  // ie., a typedef
      return;
    }

    emit.emit(Keyword_INT);
  } 

  /**
   * Generate code for a block of statements.  We emit braces around
   * the block of statements and we generate declarations (if we
   * generate C code) for all symbols declared in the block.  Also, we
   * advance the symbol table pointer to point to the current scope.
   */
  public void visitBlockStmt(BlockStmt stmt)
  {
    // The C version must generate both declarations and statements

    emit.emit('{');
    emit.endLine();
    emit.incIndLevel();

    // Generate declarations.

    genDecls(stmt.getScope());

    // Generate Statements

    int ls = stmt.numStmts();
    for (int i = 0; i < ls; i++)
      stmt.getStmt(i).visit(this);

    emit.decIndLevel();
    emit.emit('}');
    emit.endLine();
  }

  public void visitMultiBranchStmt(MultiBranchStmt s)
  {
    // This routine handles both AssignedGoto and ComputedGoto.

    needsParens = false;

    emit.emit(Keyword_SWITCH);
    emit.emit('(');
    s.getExpr().visit(this);
    emit.emit(") {");
    emit.endLine();
    emit.incIndLevel();

    int caseVal = 1;
    int l = s.numLabels();
    for (int i = 0; i < l; i++) {
      LabelDecl d = s.getLabel(i);

      if (s instanceof AssignedGotoStmt) {
        caseVal = mapLabelToInt(d.getName());
      } else if (s instanceof ComputedGotoStmt) {
        caseVal++;
      } else {
        throw new scale.common.InternalError("incorrect label type in multi-branch " + s);
      }

      emit.emit(Keyword_CASE);
      emit.emit(' ');
      emit.emit(String.valueOf(caseVal));
      emit.emit(": ");
      emit.emit(Keyword_GOTO);
      emit.emit(' ');
      emit.emit(labelName(d.getName()));
      emit.emit(';');
      emit.endLine();
    }

    emit.decIndLevel();
    emit.emit('}');
    emit.endLine();
  }

  /**
   * Generate code for a If-Then-Else statement.  Should be straightforward,
   * but, we need to be careful for nested Ifs.  If the If-statement or
   * Else-statement is another If then we need to generate brackets in
   * order to avoid the dangling-else problem.
   */
  public void visitIfThenElseStmt(IfThenElseStmt ifstmt)
  {
    needsParens = false;

    emit.emit(Keyword_IF);
    emit.emit(" (");
    ifstmt.getExpr().visit(this);
    emit.emit(')');

    genStatements(ifstmt.getThenStmt());

    Statement es = ifstmt.getElseStmt();
    if (!(es instanceof NullStmt)) {
      emit.emit(Keyword_ELSE);
      emit.emit(' ');
      genStatements(es);
    }
  }

  /**
   * Generate code for an arithmetic if statement (from Fortran 77).
   * The code generated is:
   * <pre>
   *   if (e == 0)
   *     goto L_Equal;
   *   else if (e < 0)
   *     goto L_Less; }
   *   else
   *     goto L_Greater;
   * <pre>
   */
  public void visitArithmeticIfStmt(ArithmeticIfStmt stmt)
  {
    needsParens = false;

    emit.emit("if (");
    stmt.getExpr().visit(this);
    emit.emit(" == 0) {");
    emit.endLine();

    emit.incIndLevel();
    emit.emit("goto ");
    emit.emit(labelName(stmt.getEqualLabel().getName()));
    emit.emit(';');
    emit.endLine();

    emit.decIndLevel();

    emit.emit("} else if (");
    stmt.getExpr().visit(this);
    emit.emit(" < 0) {");
    emit.endLine();

    emit.incIndLevel();

    emit.emit("goto ");
    emit.emit(labelName(stmt.getLessLabel().getName()));
    emit.emit(';');
    emit.endLine();

    emit.decIndLevel();
    emit.emit("} else {");
    emit.endLine();

    emit.incIndLevel();

    emit.emit("goto ");
    emit.emit(labelName(stmt.getMoreLabel().getName()));
    emit.emit(';');
    emit.endLine();

    emit.decIndLevel();

    emit.emit('}');
    emit.endLine();
  }

  public void visitComputedGotoStmt(ComputedGotoStmt n)
  {
    visitMultiBranchStmt(n);
  }

  public void visitAssignLabelStmt(AssignLabelStmt s)
  {
    // Get the label's integer value.

    int labelVal = mapLabelToInt(s.getLabel().getName());
    emit.emit(s.getValue().getName());
    emit.emit(" = ");
    emit.emit(String.valueOf(labelVal));
    emit.emit(';');
    emit.endLine();
  }

  public void visitAssignedGotoStmt(AssignedGotoStmt n)
  {
    visitMultiBranchStmt(n);
  }

  public void visitCaseStmt(CaseStmt stmt)
  {
    needsParens = false;

    emit.emit(Keyword_SWITCH);
    emit.emit(" (");
    stmt.getExpr().visit(this);
    emit.emit(") {"); 
    emit.endLine();
    
    // Get each of the case alternatives

    int l = stmt.numAlts();
    for (int i = 0; i < l; i++) {
      // Process a single case alternative - watch out for default case.
      AltCase caseAlt = stmt.getAlt(i);
      int     ll      = caseAlt.numKeys();

      if (ll == 0) {
        emit.emit(Keyword_DEFAULT);
        emit.emit(':');
        emit.endLine();
        genStatements(caseAlt.getStmt());
      } else {
        for (int j = 0; j < ll; j++) {
          Expression c = caseAlt.getKey(j);
          emit.emit(Keyword_CASE);
          emit.emit(' ');
          c.visit(this);
          emit.emit(':');
        }
        emit.endLine();
        genStatements(caseAlt.getStmt());
        emit.emit(Keyword_BREAK);
        emit.emit(';');
        emit.endLine();
        emit.decIndLevel();
      }
    }

    emit.emit("};");
    emit.endLine();
  }

  public void visitSwitchStmt(SwitchStmt stmt)
  {
    needsParens = false;

    emit.emit(Keyword_SWITCH);
    emit.emit( " (");
    stmt.getExpr().visit(this);
    emit.emit(')'); 
    genStatements(stmt.getStmt());
  }

  public void visitWhileLoopStmt(WhileLoopStmt stmt)
  {
    needsParens = false;

    emit.emit(Keyword_WHILE);
    emit.emit(" (");
    stmt.getExpr().visit(this);
    emit.emit(')');
    genStatements(stmt.getStmt());
  }

  public void visitRepeatWhileLoopStmt(RepeatWhileLoopStmt stmt)
  {
    needsParens = false;

    emit.emit(Keyword_DO);
    genStatements(stmt.getStmt());
    emit.emit(Keyword_WHILE);
    emit.emit(" (");
    stmt.getExpr().visit(this);
    emit.emit(");");
    emit.endLine();
  }

  public void visitRepeatUntilLoopStmt(RepeatUntilLoopStmt stmt)
  {
    // Repeat-Until is same as Repeat-While except that we check
    // if the expression is not true.

    needsParens = false;

    emit.emit(Keyword_DO);
    genStatements(stmt.getStmt());
    emit.emit(Keyword_WHILE);
    emit.emit(" (!(");
    stmt.getExpr().visit(this);
    emit.emit("));");
    emit.endLine();
  }

  public void visitDoLoopStmt(DoLoopStmt stmt)
  {
    Expression index = stmt.getIndex();
    Expression inc   = stmt.getExprInc();
    Expression term  = stmt.getExprTerm();

    needsParens = false;

    boolean incVarNeeded  = !(inc instanceof Literal);
    boolean termVarNeeded = !(term instanceof Literal);
    boolean needBlock     = (incVarNeeded || termVarNeeded);


    String incTempVar  = null;
    String termTempVar = null;
    if (needBlock) {
      emit.emit('{');
      emit.endLine();
      if (incVarNeeded) {
        incTempVar = "_T_inc" + tmpVarCntr++;
        emit.emit(incTempVar);
        emit.emit(" = ");
        inc.visit(this);
        emit.emit(';');
        emit.endLine();
      }
      if (termVarNeeded) {
        termTempVar = "_T_term" + tmpVarCntr++;
        emit.emit(termTempVar);
        emit.emit(" = ");
        term.visit(this);
        emit.emit(';');
        emit.endLine();
      }
    }

    emit.emit(Keyword_FOR);
    emit.emit(" (");
    index.visit(this);
    emit.emit(" = ");
    stmt.getExprInit().visit(this);
    emit.emit("; ");

    // If the step expression is a literal we know if the loop
    // increment is positive or negative - otherwise, we need to
    // check.

    if (incTempVar != null) {
      long val = ((IntLiteral) inc).getLongValue();
      index.visit(this);
      if (val >= 0) { // positive increment
        emit.emit(" <= ");
      } else { // negative increment
        emit.emit(" >= ");
      }
      if (termTempVar == null) {
        term.visit(this);
      } else {
        emit.emit(termTempVar);
      }
    } else {
      // Unknown increment expression.
      if (incTempVar == null) {
        inc.visit(this);
      } else {
        emit.emit(incTempVar);
      }

      emit.emit(" >= 0 ? ");
      index.visit(this);
      emit.emit(" <= ");

      if (termTempVar == null) {
        term.visit(this);
      } else {
        emit.emit(termTempVar);
      }

      emit.emit(" : ");
      index.visit(this);
      emit.emit(" >= ");

      if (termTempVar == null) {
        term.visit(this);
      } else {
        emit.emit(termTempVar);
      }
    }

    emit.emit("; ");
    index.visit(this);
    emit.emit(" += ");
    if (incTempVar == null) {
      inc.visit(this);
    } else {
      emit.emit(incTempVar);
    }

    emit.emit(')');

    genStatements(stmt.getStmt());

    if (needBlock) {
      emit.endLine();
      emit.emit('}');
    }
  }

  public void visitForLoopStmt(ForLoopStmt stmt)
  {
    Expression init = stmt.getExprInit();
    Expression test = stmt.getExprTest();
    Expression inc  = stmt.getExprInc();

    needsParens = false;

    emit.emit(Keyword_FOR);
    emit.emit(" (");
    if (init != null)
      init.visit(this);
    emit.emit("; ");
    if (test != null)
      test.visit(this);
    emit.emit("; ");
    if (inc != null)
      inc.visit(this);
    emit.emit(')');
    genStatements(stmt.getStmt());
  }
  
  public void visitBreakStmt(BreakStmt n)
  {
    emit.emit(Keyword_BREAK);
    emit.emit(';');
    emit.endLine();
  }

  public void visitContinueStmt(ContinueStmt n)
  {
    emit.emit(Keyword_CONTINUE);
    emit.emit(';');
    emit.endLine();
  }

  public void visitGotoStmt(GotoStmt n)
  {
    emit.emit(Keyword_GOTO);
    emit.emit(' ');
    emit.emit(labelName(n.getLabel().getName()));
    emit.emit(';');
    emit.endLine();
  }

  public void visitReturnStmt(ReturnStmt n)
  {
    Expression expr = n.getExpr();

    needsParens = false;

    if (expr == null) {
      emit.emit(Keyword_RETURN);
      emit.emit(';');
    } else {
      emit.emit(Keyword_RETURN);
      emit.emit(" (");
      expr.visit(this);
      emit.emit(");");
    }
    emit.endLine();
  }

  public void visitExitStmt(ExitStmt n)
  {
    Expression expr = n.getExpr();

    needsParens = false;

    if (expr == null) {
      emit.emit("exit(0);");
    } else if (expr.getCoreType().isIntegerType()) {
      emit.emit("exit(");
      expr.visit(this);
      emit.emit(");");
    } else {
      emit.emit("printf(\"%s\\n\",");
      expr.visit(this);
      emit.emit(");");
      emit.endLine();
      emit.emit("exit(0);");
    }

    emit.endLine();
  }

  public void visitEvalStmt(EvalStmt n)
  {
    needsParens = false;
    n.getExpr().visit(this);
    emit.emit(';');
    emit.endLine();
  }

  public void visitDeclStmt(DeclStmt n)
  {
    // Since we're generating C code, we can't emit declarations
    // anywhere - they must appear at the beginning of a block
  }

  public void visitNullStmt(NullStmt n)
  {
    emit.emit(';');
  }

  public void visitLabelStmt(LabelStmt s)
  {
    LabelDecl l = s.getLabel();

    if (l instanceof CaseLabelDecl) {
      CaseLabelDecl v = (CaseLabelDecl) l;
      Expression    e = v.getExpr();

      if (e != null) {
        emit.emit(Keyword_CASE);
        emit.emit(' ');
        e.visit(this);
      } else {
        emit.emit(Keyword_DEFAULT);
      }

    } else {
      emit.emit(labelName(l.getName()));
    }

    emit.emit(": ");
    s.getStmt().visit(this);
    emit.endLine();
  }

  public void visitExpression(Expression n) 
  {
    emit.emit("Missing");
  }

  /**
   * Generate code for a literal.  We need to ensure that the string
   * does not have invalid C characters.  We either convert invalid C
   * characters to escape code or just ignore the chracter.
   * @param l the literal node
   */
  public void visitLiteral(Literal l)
  {
    String  lit   = l.getGenericValue();
    Type    t     = l.getCoreType();
    boolean ptype = t.isPointerType();

    // getGenericValue returns a String already converted for display
    // using C syntax.  For another language, use getValue which
    // returns an object that can then be displayed using logic
    // specific to the target language.

    if (ptype) {
      emit.emit("((");
      genCastType(l.getType());
      emit.emit(')');
    }

    emit.emit(lit);
    if (ptype)
      emit.emit(')');
  }

  public void visitStringLiteral(StringLiteral n) 
  {
    visitLiteral(n);
  }

  public void visitBooleanLiteral(BooleanLiteral n) 
  {
    visitLiteral(n);
  }

  public void visitCharLiteral(CharLiteral n) 
  {
    visitLiteral(n);
  }

  public void visitIntLiteral(IntLiteral n) 
  {
    visitLiteral(n);
  }

  public void visitIntArrayLiteral(IntArrayLiteral l) 
  {
    Type    t      = l.getCoreType();
    int     len    = l.size();
    boolean signed = false;

    ArrayType at = t.getCoreType().returnArrayType();
    if (at != null) {
      Type et = at.getElementType();
      signed = et.isSigned();
    }

    // Do each element separately as getGenericValue() may return too
    // long of a string.

    for (int i = 0; i < len; i++) {
      if (i > 0) {
        emit.emit(',');
        if (emit.getCurrentColumn() > 60)
          emit.endLine();
      }

      long value = l.getValue(i);
      if (signed)
        emit.emit(Long.toString(value));
      else {
        emit.emit("0x");
        emit.emit(Long.toHexString(value));
        emit.emit('U');
      }
    }
  }

  public void visitFloatLiteral(FloatLiteral n) 
  {
    visitLiteral(n);
  }

  public void visitComplexLiteral(ComplexLiteral n) 
  {
    if (inAggregationElements) {
      emit.emit('{');
      emit.emit(Literal.formatRealValue(n.getReal()));
      emit.emit(',');
      emit.emit(Literal.formatRealValue(n.getImaginary()));
      emit.emit('}');
      return;
    }

    emit.emit("_scale_create");
    emit.emit(n.getGenericValue());
  }

  public void visitFloatArrayLiteral(FloatArrayLiteral l) 
  {
    int len = l.size();

    // Do each element separately as getGenericValue() may return too
    // long of a string.

    for (int i = 0; i < len; i++) {
      if (i > 0) {
        emit.emit(',');
        if ((i % 20) == 0)
          emit.endLine();
      }
      emit.emit(Double.toString(l.getValue(i)));
    }
  }

  public void visitSizeofLiteral(SizeofLiteral sl) 
  {
    Type v  = sl.getSizeofType();

    emit.emit("sizeof(");
    genDeclarator(v, "");
    emit.emit(')');
  }

  public void visitAddressLiteral(AddressLiteral sl) 
  {
    Declaration d      = sl.getDecl();
    long        offset = sl.getOffset();
    Type        t      = sl.getCoreType();

    ArrayType at = t.returnArrayType();
    if (at != null) {
      emit.emit("((void *)");
      if (offset != 0) {
        emit.emit(" (((char *) &");
        if (d != null)
          emit.emit(d.getName());
        else
          emit.emit(sl.getValue().getGenericValue());
        emit.emit("[0])+");
        emit.emit(offset);
        emit.emit("))");
        return;
      }

      emit.emit("&");
      if (d != null)
        emit.emit(d.getName());
      else
        emit.emit(sl.getValue().getGenericValue());
      emit.emit("[0])");

      return;
    }

    if (offset != 0) {
      emit.emit("((void *) (((char *) &");
      if (d != null)
        emit.emit(d.getName());
      else
        emit.emit(sl.getValue().getGenericValue());
      emit.emit(")+");
      emit.emit(offset);
      emit.emit("))");
      return;
    }

    if (d != null){
      emit.emit("((void *) &");
      emit.emit(d.getName());
      emit.emit(")");
      return;
    }

    emit.emit(sl.getValue().getGenericValue());
  }

  public void visitIdAddressOp(IdAddressOp id) 
  {
    emit.emit(identifierName(id));
  }

  public void visitIdValueOp(IdValueOp id) 
  {
    if (id.getDecl().getMode() == ParameterMode.REFERENCE)
      emit.emit('*');
    emit.emit(identifierName(id));
  }

  public void visitIdReferenceOp(IdReferenceOp id) 
  {
    emit.emit(identifierName(id));
  }

  public void visitSeriesOp(SeriesOp expr) 
  {
    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    expr.getExpr1().visit(this);
    emit.emit(", ");
    expr.getExpr2().visit(this);
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitParenthesesOp(ParenthesesOp expr) 
  {
    emit.emit(" (");
    expr.getExpr().visit(this);
    emit.emit(')');
  }

  /**
   * Generate code for aggregate values.  This routine is responsible
   * for ordering the AggregationElements to occur for user-specified
   * positions.
   * <p>
   * We use several stages to generate the C code.
   * <ol>
   * <li>Copy the AggreationElementOp nodes to a vector (we make a 
   * separate copy for each "bound" node).  We record the low and
   * high values for each one.
   * <li>Generate the code for each node in the vector in order.
   * </ol>
   */
  public void visitAggregationElements(AggregationElements agg) 
  {
    boolean saf = inAggregationElements;

    inAggregationElements = true;

    // Special case - check if the aggregation contains all zero values.

    if (agg.containsAllZeros()) {
      emit.emit("{0}");
      return;
    }

    // Generate the code

    emit.endLine();
    emit.incIndLevel();
    emit.emit('{');

    // Create a list of all the initializers.

    boolean         flg   = false;
    Vector<Object>  v     = agg.getElementVector();
    int             l     = v.size();
    long            index = 0;
    for (int i = 0; i < l; i++) {
      Object x = v.elementAt(i);

      if (x instanceof PositionRepeatOp) {
        int     reps = ((PositionRepeatOp) x).getCount();
        i++;
        Literal expr = (Literal) v.elementAt(i);
        for (int r = 0; r < reps; r++) {
          if (flg)
            emit.emit(", ");
          flg = true;
          if (emit.getCurrentColumn() > 80)
            emit.endLine();
          expr.visit(this);
          index++;
        }
        continue;
      }

      if (x instanceof PositionFieldOp) {
        FieldDecl fd   = ((PositionFieldOp) x).getField();
        i++;
        Expression expr = (Expression) v.elementAt(i);
        if (flg)
          emit.emit(", ");
        flg = true;
        if (emit.getCurrentColumn() > 80)
          emit.endLine();
        emit.emit('.');
        emit.emit(fd.getName());
        emit.emit('=');
        expr.visit(this);
        continue;
      }

      if (x instanceof PositionOffsetOp)
        continue;

      if (x instanceof PositionIndexOp) {
        long pos = ((PositionIndexOp) x).getIndex();

        while(index < pos) {
          if (flg)
            emit.emit(", ");
          flg = true;
          if (emit.getCurrentColumn() > 80)
            emit.endLine();
          emit.emit('0');
          index++;
        }
        continue;
      }

      if (x instanceof Expression)
        x = ((Expression) x).getConstantValue();

      if (x instanceof Literal) {
        Literal expr = (Literal) x;
        if (flg)
          emit.emit(", ");
        flg = true;
        if (emit.getCurrentColumn() > 80)
          emit.endLine();
        expr.visit(this);
        index++;
        continue;
      }

      throw new NotImplementedError("Can't generate constants for " + x);
    }

    emit.emit('}');
    emit.decIndLevel();

    inAggregationElements = saf;
  }

  private void doAssignment(String op, Expression lhs, Expression rhs)
  {
    boolean savenp    = needsParens;
    boolean saveOnLHS = onLHS;

    if (needsParens)
      emit.emit('(');

    needsParens = false;
    onLHS = true;

    lhs.visit(this);

    onLHS=false;
    emit.emit(op);

    rhs.visit(this);

    needsParens = savenp;
    onLHS = saveOnLHS;

    if (needsParens)
      emit.emit(')');
  }

  /**
   * Generate the C code for a regular assignment. 
   */
  public void visitAssignSimpleOp(AssignSimpleOp a) 
  {
    doAssignment(" = ", a.getLhs(), a.getRhs());
  }

  public void visitDefOp(DefOp expr) 
  {
    Expression exp  = expr.getExpr();

    exp.visit(this);
  }

  public void visitPositiveOp(PositiveOp expr) 
  {
    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    emit.emit(" + ");
    expr.getExpr().visit(this);
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }


  public static char simpleTypeName(String name)
  {
    if (name.equals("int"))
      return 'i';
    if (name.equals("long"))
      return 'l';
    if (name.equals("float"))
      return 'f';
    if (name.equals("double"))
      return 'd';
    if (name.equals("complex"))
      return 'c';
    if (name.equals("doublecomplex"))
      return 'z';
    if (name.equals("long long"))
      return 'L';
    if (name.equals("long double"))
      return 'D';
    return 'x';
  }

  /**
   * Generate a call to a routine that is part of the Scale runtime
   * library.  This is used mainly for operations on complex values.
   */
  public void genIntrinsicOp(String op,
                             Type t1,
                             Expression e1,
                             Type t2,
                             Expression e2)
  {
    String opn1 = t1.mapTypeToCString();
    String opn2 = t2.mapTypeToCString();

    boolean savenp = needsParens;
    needsParens = false;

    emit.emit("_scale_");
    emit.emit(op);
    emit.emit(simpleTypeName(opn1));
    emit.emit(simpleTypeName(opn2));
    emit.emit('(');

    e1.visit(this);

    if (e2 != null) {
      emit.emit(", ");
      e2.visit(this);
    }

    emit.emit(')');

    needsParens = savenp;
  }

  public void visitNegativeOp(NegativeOp expr)
  {   
    Expression e1 = expr.getExpr();
    Type       t1 = e1.getCoreType();

    if (t1.isComplexType()) {
      genIntrinsicOp("negate", t1, e1, null, null);
      return;
    }

    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;

    emit.emit(" -");
    e1.visit(this);

    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitTranscendentalOp(TranscendentalOp expr)
  {
    Expression e1   = expr.getExpr();
    Type       t1   = e1.getCoreType();

    if (t1.isComplexType()) {
      genIntrinsicOp(expr.getDisplayLabel(), t1, e1, null, null);
      return;
    }

    boolean savenp = needsParens;
    needsParens = false;


    emit.emit(expr.getDisplayLabel());
    emit.emit('(');
    e1.visit(this);
    emit.emit(')');

    needsParens = savenp;
  }

  public void visitTranscendental2Op(Transcendental2Op expr)
  {
    Expression e1 = expr.getExpr1();
    Expression e2 = expr.getExpr2();

    boolean savenp = needsParens;
    needsParens = false;

    emit.emit(expr.getDisplayLabel());
    emit.emit('(');
    e1.visit(this);
    emit.emit(',');
    e2.visit(this);
    emit.emit(')');

    needsParens = savenp;
  }

  public void visitAbsoluteValueOp(AbsoluteValueOp expr)
  {
    Expression e1 = expr.getExpr();
    Type       t1 = e1.getCoreType();

    if (t1.isComplexType()) {
      genIntrinsicOp("abs", t1, e1, null, null);
      return;
    }

    boolean savenp = needsParens;
    needsParens = false;

    IntegerType it = t1.returnIntegerType();
    if (it != null) {
      if (it.bitSize() > 32) {
        emit.emit('l');
        if (Machine.currentMachine.getIntegerCalcType().bitSize() <= 32)
          emit.emit('l');
      }
    } else
      emit.emit('f');

    emit.emit("abs(");

    e1.visit(this);
    emit.emit(')');

    needsParens = savenp;
  }

  public void visitMinimumOp(MinimumOp expr) 
  {
    Type t = expr.getCoreType();

    genIntrinsicOp("min", t, expr.getExpr1(), t, expr.getExpr2());
  }

  public void visitMaximumOp(MaximumOp expr) 
  {
    Type t = expr.getCoreType();

    genIntrinsicOp("max", t, expr.getExpr1(), t, expr.getExpr2());
  }

  public void visitAdditionOp(AdditionOp expr)
  {
    Type       t  = expr.getCoreType();
    Expression e1 = expr.getExpr1();
    Expression e2 = expr.getExpr2();
    Type       t1 = e1.getCoreType();
    Type       t2 = e2.getCoreType();

    if (t1.isComplexType()) {
      genIntrinsicOp("add", t1, e1, t2, e2);
      return;
    }

    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');

    needsParens = true;
    e1.visit(this);
    emit.emit(" + ");
    e2.visit(this);

    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitSubtractionOp(SubtractionOp expr)
  {
    Expression e1   = expr.getExpr1();
    Expression e2   = expr.getExpr2();
    Type       t1   = e1.getCoreType();
    Type       t2   = e2.getCoreType();

    if (t1.isComplexType()) {
      genIntrinsicOp("sub", t1, e1, t2, e2);
      return;
    }

    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;

    e1.visit(this);
    emit.emit(" - ");
    e2.visit(this);

    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitMultiplicationOp(MultiplicationOp expr)
  {
    Expression e1   = expr.getExpr1();
    Expression e2   = expr.getExpr2();
    Type       t1   = e1.getCoreType();
    Type       t2   = e2.getCoreType();

    if (t1.isComplexType()) {
      genIntrinsicOp("mult", t1, e1, t2, e2);
      return;
    }

    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    e1.visit(this);
    emit.emit(" * ");
    e2.visit(this);
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitDivisionOp(DivisionOp expr)
  {
    Expression e1 = expr.getExpr1();
    Expression e2 = expr.getExpr2();
    Type       t1 = e1.getCoreType();
    Type       t2 = e2.getCoreType();

    if (t1.isComplexType()) {
      genIntrinsicOp("div", t1, e1, t2, e2);
      return;
    }

    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    e1.visit(this);
    emit.emit(" / ");
    e2.visit(this);
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitModulusOp(ModulusOp expr) 
  {
    Type t = expr.getCoreType();

    genIntrinsicOp("mod", t, expr.getExpr1(), t, expr.getExpr2());
  }

  public void visitRemainderOp(RemainderOp expr) 
  {
    Expression e1 = expr.getExpr1();
    Expression e2 = expr.getExpr2();
    Type       t1 = expr.getCoreType();

    if (t1.isIntegerType()) {
      boolean savenp = needsParens;
      if (needsParens)
        emit.emit('(');
      needsParens = true;
      e1.visit(this);
      emit.emit(" % ");
      e2.visit(this);
      needsParens = savenp;
      if (needsParens)
        emit.emit(')');
      return;
    }

    if (t1.isRealType()) {
      emit.emit("fmod(");
      e1.visit(this);
      emit.emit(',');
      e2.visit(this);
      emit.emit(')');
      return;
    }

    throw new scale.common.InternalError("invalid operands for remainder operation " + expr);
  }

  public void visitExponentiationOp(ExponentiationOp expr)
  {
    Expression e1 = expr.getExpr1();
    Expression e2 = expr.getExpr2();
    Type       t1 = e1.getCoreType();
    Type       t2 = e2.getCoreType();

    genIntrinsicOp("pow", t1, e1, t2, e2);
  }

  public void visitPreDecrementOp(PreDecrementOp expr) 
  {
    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    emit.emit(" --");
    expr.getExpr().visit(this);
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitPreIncrementOp(PreIncrementOp expr) 
  {
    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    emit.emit(" ++");
    expr.getExpr().visit(this);
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitPostDecrementOp(PostDecrementOp expr) 
  {
    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    expr.getExpr().visit(this);
    emit.emit("-- ");
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitPostIncrementOp(PostIncrementOp expr) 
  {
    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    expr.getExpr().visit(this);
    emit.emit("++ ");
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitEqualityOp(EqualityOp expr) 
  {
    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');

    needsParens = true;
    if (expr.getCoreType().isComplexType()) { // e1.r == e2.r && e1.i == e2.i
      expr.getExpr1().visit(this);
      emit.emit(".r");
      emit.emit(" == ");
      expr.getExpr2().visit(this);
      emit.emit(".r");
      emit.emit(" && ");
      expr.getExpr1().visit(this);
      emit.emit(".i");
      emit.emit(" == ");
      expr.getExpr2().visit(this);
      emit.emit(".i");
    } else {
      expr.getExpr1().visit(this);
      emit.emit(" == ");
      expr.getExpr2().visit(this);
    }

    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitNotEqualOp(NotEqualOp expr) 
  {
    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;

    if (expr.getCoreType().isComplexType()) { // e1.r != e2.r || e1.i != e2.i 
      expr.getExpr1().visit(this);
      emit.emit(".r");
      emit.emit(" != ");
      expr.getExpr2().visit(this);
      emit.emit(".r");
      emit.emit(" || ");
      expr.getExpr1().visit(this);
      emit.emit(".i");
      emit.emit(" != ");
      expr.getExpr2().visit(this);
      emit.emit(".i");
    } else {
      expr.getExpr1().visit(this);
      emit.emit(" != ");
      expr.getExpr2().visit(this);
    }

    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitGreaterOp(GreaterOp expr) 
  {
    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    expr.getExpr1().visit(this);
    emit.emit(" > ");
    expr.getExpr2().visit(this);
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitGreaterEqualOp(GreaterEqualOp expr) 
  {
    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    expr.getExpr1().visit(this);
    emit.emit(" >= ");
    expr.getExpr2().visit(this);
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitLessOp(LessOp expr) 
  {
    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    expr.getExpr1().visit(this);
    emit.emit(" < ");
    expr.getExpr2().visit(this);
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitLessEqualOp(LessEqualOp expr) 
  {
    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    expr.getExpr1().visit(this);
    emit.emit(" <= ");
    expr.getExpr2().visit(this);
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }
  
  public void visitBitComplementOp(BitComplementOp expr) 
  {
    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    emit.emit(" ~");
    expr.getExpr().visit(this);
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitBitAndOp(BitAndOp expr) 
  {
    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    expr.getExpr1().visit(this);
    emit.emit(" & ");
    expr.getExpr2().visit(this);
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitBitXorOp(BitXorOp expr) 
  {
    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    expr.getExpr1().visit(this);
    emit.emit(" ^ ");
    expr.getExpr2().visit(this);
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitBitOrOp(BitOrOp expr) 
  {
    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    expr.getExpr1().visit(this);
    emit.emit(" | ");
    expr.getExpr2().visit(this);
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitBitShiftOp(BitShiftOp expr) 
  {
    ShiftMode mode = expr.getShiftMode();

    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    expr.getExpr1().visit(this);

    switch (mode) {
    case Left:
      emit.emit(" << ");
      break;
    case UnsignedRight:
    case SignedRight:
      emit.emit(" >> ");
      break;
    default:
      throw new scale.common.InternalError("Can't convert " +
                                           mode +
                                           " to C." +
                                           expr);
    }

    expr.getExpr2().visit(this);
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitMultiplicationAssignmentOp(MultiplicationAssignmentOp expr) 
  {
    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    expr.getLhs().visit(this);
    emit.emit(" *= ");
    expr.getRhs().visit(this);
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitDivisionAssignmentOp(DivisionAssignmentOp expr) 
  {
    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    expr.getLhs().visit(this);
    emit.emit(" /= ");
    expr.getRhs().visit(this);
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitRemainderAssignmentOp(RemainderAssignmentOp expr) 
  {
    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    expr.getLhs().visit(this);
    emit.emit(" %= ");
    expr.getRhs().visit(this);
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitAdditionAssignmentOp(AdditionAssignmentOp a) 
  {
    doAssignment(" += ", a.getLhs(), a.getRhs());
  }

  public void visitSubtractionAssignmentOp(SubtractionAssignmentOp a) 
  {
    doAssignment(" -= ", a.getLhs(), a.getRhs());
  }

  public void visitBitShiftAssignmentOp(BitShiftAssignmentOp a) 
  {
    String mode = null;

    switch (a.getShiftMode()) {
    case Left:
      mode = " <<= ";
      break;
    case SignedRight:
    case UnsignedRight:
      mode = " >>= ";
      break;
    default:
      throw new scale.common.InternalError("Can't convert " +
                                           a.getShiftMode() +
                                           " to C.");
    }

    doAssignment(mode, a.getLhs(), a.getRhs());
  }

  public void visitBitAndAssignmentOp(BitAndAssignmentOp a) 
  {
    doAssignment(" &= ", a.getLhs(), a.getRhs());
  }

  public void visitBitXorAssignmentOp(BitXorAssignmentOp a) 
  {
    doAssignment(" ^= ", a.getLhs(), a.getRhs());
  }

  public void visitBitOrAssignmentOp(BitOrAssignmentOp a) 
  {
    doAssignment(" |= ", a.getLhs(), a.getRhs());
  }

  public void visitNotOp(NotOp expr) 
  {
    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    emit.emit(" !");
    expr.getExpr().visit(this);
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitAndOp(AndOp expr) 
  {
    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    expr.getExpr1().visit(this);
    emit.emit(" && ");
    expr.getExpr2().visit(this);
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitOrOp(OrOp expr) 
  {
    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    expr.getExpr1().visit(this);
    emit.emit(" || ");
    expr.getExpr2().visit(this);
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitAndConditionalOp(AndConditionalOp expr) 
  {
    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    expr.getExpr1().visit(this);
    emit.emit(" && ");
    expr.getExpr2().visit(this);
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitOrConditionalOp(OrConditionalOp expr) 
  {
    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    expr.getExpr1().visit(this);
    emit.emit(" || ");
    expr.getExpr2().visit(this);
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitExpressionIfOp(ExpressionIfOp expr) 
  {
    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    expr.getExpr1().visit(this);
    emit.emit(" ? ");
    expr.getExpr2().visit(this);
    emit.emit(" : ");
    expr.getExpr3().visit(this);
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitAddressOp(AddressOp expr) 
  {
    Expression arg = expr.getExpr();
    Type       t   = arg.getCoreType();

    if (arg instanceof DereferenceOp) {
      ((DereferenceOp) arg).getExpr().visit(this);
      return;
    }

    boolean savenp = needsParens;

    if (needsParens)
      emit.emit('(');

    needsParens = true;
    emit.emit(" &");
    arg.visit(this);

    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitDereferenceOp(DereferenceOp expr) 
  {
    Expression arg = expr.getExpr();

    if (arg instanceof AddressOp) {
      ((AddressOp) arg).getExpr().visit(this);
      return;
    }

    if (arg instanceof SubscriptAddressOp) {
      visitSubscriptOp((SubscriptAddressOp) arg);
      return;
    }

    if (arg instanceof SelectIndirectOp) {
      SelectIndirectOp sop = (SelectIndirectOp) arg;
      doSelectOp(sop.getStruct(), sop.getField());
      return;
    }

    boolean savenp = needsParens;

    if (needsParens)
      emit.emit('(');

    needsParens = true;

    emit.emit(" *");
    arg.visit(this);
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitNilOp(NilOp n) 
  {
    emit.emit("(void *)0");
  }

  public void visitThisOp(ThisOp n) 
  {
    emit.emit("this");
  }

  private void doSelectOp(Expression struct, FieldDecl field)
  {
    struct.visit(this);
    if (struct instanceof IdAddressOp) {
      emit.emit('.');
    } else {
      emit.emit("->");
    }
    emit.emit(field.getName());
  }

  public void visitSelectOp(SelectOp expr) 
  {
    boolean savenp = needsParens;
    if (needsParens)
      emit.emit('(');
    needsParens = true;
    doSelectOp(expr.getStruct(), expr.getField());
    needsParens = savenp;
    if (needsParens)
      emit.emit(')');
  }

  public void visitSelectIndirectOp(SelectIndirectOp expr) 
  {
    if (!onLHS)
      emit.emit("(&");

    boolean saveOnLHS = onLHS;
    boolean savenp    = needsParens;
    onLHS = false;

    if (needsParens)
      emit.emit('(');

    needsParens = true;

    doSelectOp(expr.getStruct(), expr.getField());

    needsParens = savenp;
    onLHS = saveOnLHS;

    if (needsParens)
      emit.emit(')');
    if (!onLHS)
      emit.emit(')');
  }

  public void visitSubscriptValueOp(SubscriptValueOp n) 
  {
    visitSubscriptOp(n);
  }

  public void visitSubscriptAddressOp(SubscriptAddressOp n) 
  {
    if (!onLHS)
      emit.emit('&');

    boolean saveOnLHS = onLHS;
    onLHS = false;

    visitSubscriptOp(n);

    onLHS = saveOnLHS;
  }

  public void visitSubscriptOp(SubscriptOp expr) 
  {
    Expression  array = expr.getArray();
    PointerType t     = (PointerType) array.getCoreType();

    if (!(array instanceof IdAddressOp)) {
      emit.emit('(');
      array.visit(this); // Emit the array name (might be an expression)
      emit.emit(')');
    } else
      array.visit(this); // Emit the array name (might be an expression)

    emit.emit('['); 

    int l = expr.numSubscripts();
    for (int i = 0; i < l; i++) {
      if (i > 0)
        emit.emit("][");
      expr.getSubscript(i).visit(this);
    }

    emit.emit(']');
  }

  public void visitCallFunctionOp(CallFunctionOp fun) 
  {
    fun.getRoutine().visit(this);

    genActuals(fun);
  }

  /**
   * Implement the conversion operator.  The code generated for the
   * conversion depends upon the type of conversion.
   * <p>
   * We do not generate code for <i>cast</i> conversions to
   * <i>aggregate types</i> since these are illegal in C.  For an
   * {@link scale.clef.type.ArrayType ArrayType}, we generate a cast
   * to a pointer to the array's element type (we should only see this
   * at function calls).
   */
  public void visitTypeConversionOp(TypeConversionOp t)
  {
    // Either a user or language defined conversion routine is specified.
    
    Type       tt     = t.getType();
    Type       tct    = tt.getCoreType();
    Expression e      = t.getExpr();
    Type       et     = e.getCoreType();
    CastMode   r      = t.getConversion();
    boolean    savenp = needsParens;

    needsParens = false;

    switch (r) {
    case FLOOR:
      emit.emit("d_int("); // From the f2c libF77.
      e.visit(this);
      emit.emit(')');
      break;
    case CEILING:
      emit.emit("ceil(");
      e.visit(this);
      emit.emit(')');
      break;
    case ROUND:
      emit.emit("d_nint("); // From the f2c libF77.
      e.visit(this);
      emit.emit(')');
      break;
    case TRUNCATE:
    case REAL:
      emit.emit("((");
      genCastType(tt);
      emit.emit(')');
      if (et.isComplexType()) {
        if (e instanceof ComplexOp) {
          ((ComplexOp) e).getExpr1().visit(this);
        } else {
          e.visit(this);
          emit.emit(".r");
        }
      } else {
        e.visit(this);
      }
      emit.emit(')');
      break;
    case CAST:
      assert !tct.isAggregateType() : "Cannot cast to Aggregate Type " + t;

      emit.emit("((");
      genCastType(tt);
      emit.emit(')');
      e.visit(this);
      emit.emit(')');
      break;
    case IMAGINARY:
      emit.emit("((");
      genCastType(tt);
      emit.emit(')');
      if (et.isComplexType()) {
        if (e instanceof ComplexOp) {
          ((ComplexOp) e).getExpr2().visit(this);
        } else {
          e.visit(this);
          emit.emit(".i");
        }
      } else {
        emit.emit('0');
      }
      emit.emit(')');
      break;
    default:
      throw new scale.common.InternalError("unknown cast operation " + t);
    }

    needsParens = savenp;
  }

  /**
   * Generate C code to create a complex value.
   * @exception scale.common.InternalError for errors
   */
  public void visitComplexOp(ComplexOp t) 
  {
    boolean savenp = needsParens;
    String  opn    = ((ComplexType) t.getCoreType()).mapTypeToCString();

    needsParens = false;

    emit.emit("_scale_create");
    emit.emit(opn);
    emit.emit('(');
    t.getExpr1().visit(this);
    emit.emit(", ");
    t.getExpr2().visit(this);
    emit.emit(')');

    needsParens = savenp;
  }

  /**
   * Generate the C construct for the <code>va_start(va_list,
   * parmN)</code> construct.
   */
  public void visitVaStartOp(VaStartOp vas) 
  {
    boolean savenp = needsParens;
    needsParens = false;

    emit.emit("(void) va_start(");
    (vas.getVaList()).visit(this);
    emit.emit(", ");
    emit.emit(vas.getParmN().getName());
    emit.emit(')');

    needsParens = savenp;
  }

  /**
   * Generate the C construct for the <code>va_arg(va_list,
   * type)</code> construct.
   */
  public void visitVaArgOp(VaArgOp va) 
  {
    boolean savenp = needsParens;
    needsParens = false;

    emit.emit("va_arg(");
    (va.getVaList()).visit(this);
    emit.emit(", ");
    genDeclarator(va.getType(), "");
    emit.emit(')');

    needsParens = savenp;
  }

  /**
   *
   */
  public void visitVaCopyOp(VaCopyOp vac)
  {
    boolean savenp = needsParens;
    needsParens = false;

    emit.emit("va_copy(");
    vac.getExpr1().visit(this);
    emit.emit(", ");
    vac.getExpr2().visit(this);
    emit.emit(')');

    needsParens = savenp;
  }

  /**
   * Generate the C construct for the Gnu abomination called a
   * "statement expression".
   */
  public void visitStatementOp(StatementOp so)
  {
    Statement s = so.getStatement();

    emit.emit("({");

    if (s != null) {
      s.visit(this);
      emit.emit(';');
    }

    so.getExpr().visit(this);

    emit.emit(';');
    emit.emit("})");
  }

  /**
   * Generate the C construct for the <code>va_end(va_list)</code>
   * construct.
   */
  public void visitVaEndOp(VaEndOp vae) 
  {
    boolean savenp = needsParens;
    needsParens = false;

    emit.emit("(void) va_end(");
    (vae.getVaList()).visit(this);
    emit.emit(')');

    needsParens = savenp;
  }
}
