header {
package scale.frontend.c;
}

{
import java.util.Enumeration;
import scale.common.*;
import scale.clef.*;
import scale.clef.expr.*;
import scale.clef.decl.*;
import scale.clef.type.*;
import scale.clef.stmt.*;
import scale.clef.symtab.*;
import scale.callGraph.*;
import scale.annot.*;

  /** 
   * This class performs parsing of C99 programs..
   * <p>
   * $Id: c99.g,v 1.144 2007-09-20 18:49:43 burrill Exp $
   * <p>
   * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
   * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
   * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
   * Amherst MA. 01003, USA<br>
   * All Rights Reserved.<br>
   * <p>
   * The grammer was developed from the grammar in Annex A of the 
   * INTERNATIONAL STANDARD ISO/IEC 9899 Second edition 1999-12-01 Programming languages - C
   * <p>
   */
}
class C99Parser extends Parser;
options {
  k = 2;
}
tokens {
KEYWORD_auto = "auto";
KEYWORD_break = "break";
KEYWORD_case = "case";
KEYWORD_char = "char";
KEYWORD_const = "const";
KEYWORD___const = "__const";
KEYWORD_continue = "continue";
KEYWORD_default = "default";
KEYWORD_do = "do";
KEYWORD_double = "double";
KEYWORD_else = "else";
KEYWORD_enum = "enum";
KEYWORD_extern = "extern";
KEYWORD_float = "float";
KEYWORD_for = "for";
KEYWORD_goto = "goto";
KEYWORD_if = "if"; 
KEYWORD_inline = "inline";
KEYWORD_int = "int";
KEYWORD_long = "long";
KEYWORD_register = "register";
KEYWORD_restrict = "restrict";
KEYWORD_return = "return";
KEYWORD_short = "short";
KEYWORD_signed = "signed";
KEYWORD___signed__ = "__signed__";
KEYWORD_sizeof = "sizeof"; 
KEYWORD_static = "static";
KEYWORD_struct = "struct";
KEYWORD_switch = "switch";
KEYWORD_typedef = "typedef";
KEYWORD_label = "__label__";
KEYWORD_typeof = "typeof";
KEYWORD_union = "union";
KEYWORD_unsigned = "unsigned";
KEYWORD_void = "void";
KEYWORD_volatile = "volatile";
KEYWORD__volatile = "__volatile";
KEYWORD_while = "while";
KEYWORD__Bool = "_Bool";
KEYWORD__Complex = "_Complex";
KEYWORD__Imaginary = "_Imaginary";
KEYWORD_Extension = "__extension__";
KEYWORD_alignof = "__alignof__";
KEYWORD__inline__ = "__inline__";
KEYWORD__inline = "__inline";
KEYWORD_va_arg  = "va_arg";
KEYWORD_builtin_va_arg = "__builtin_va_arg";
KEYWORD_builtin_offsetof = "__builtin_offsetof";

KEYWORD_clone = "__CLONE_beta";
}
{
  private static final CreatorSource creator = new CreatorSource("C99Parser");

  public static boolean classTrace = false;
  public static boolean fullError  = false;

  /**
   * Set true if GNU extensions are allowed.
   */
  protected boolean allowGNUExtensions = false;
  /**
   * Set true if C99 extensions are allowed.
   */
  protected boolean allowC99Extensions = false;
  /**
   * Set true if strict ANSI C is required
   */
  protected boolean strictANSIC = false;

  private HashMap<SymtabScope, HashMap<Expression,VariableDecl>> varlitMap = null;  // Map from literals to the global const variable defining the value.
  private HashMap<String, LabelDecl>      labels        = null;  // The set of labels encountered.
  private HashMap<String, ProcedureDecl>  procMap       = null;  // Map from pre-defined procedure name to procedure declaration.
  private Vector<String>                  warnings      = null;  // A vector in which any warning messages are added.
  private HashSet<String>                 badAttributes = null;  // The set of unrecognized attributes.

  private boolean         vaListDefined           = false; // True if va_list has been defined.
  private boolean         fatalError              = false; // True if the parser finds an error.
  private boolean         inConstant              = false; // True if the parser is parsing a constant expression.
  private boolean         topLevel                = true;  // True if processing a top level declaration.
  private CallGraph       cg                      = null;  // The CallGraph instance in which to place the AST.
  private CPreprocessor   cpp                     = null;  // The C pre-processor in use.
  private RoutineDecl     currentFunction         = null;  // The current routine under construction.
  private BlockStmt       currentBlockStmt        = null;  // The current BlockStmt under construction.
  private int             typeCounter             = 0;     // For generating implied type names.
  private int             caseCounter             = 0;     // For generating case label names.
  private int             fieldCounter            = 0;     // For generating anonymous struct field names.
  private int             varCounter              = 0;     // For generating compiler-generated variable names.
  private int             labelCounter            = 0;     // For generating compiler-generated label names.
  private int             compoundStmtNestedLevel = 0;     // The current compund statement nesting depth.
  private int             storageClass            = 0;     // The current storage class for the declaration under construction.
  private int             baseType                = 0;     // The current base type for the declaration under construction.
  private int             typeQualifier           = 0;     // The current type qualifier for the declaration under construction.
  private String          declID                  = null;  // The identifier for the declaration under construction.
  private Type            buildType               = null;  // For the construction of types.
  private Literal         dzero                   = null;  // A ((double) 0.0).
  private Literal         zero                    = null;  // A ((int) 0).
  private Literal         one                     = null;  // A ((int) 1).
  private Expression      errExp                  = null;  // Returned if error in generating an Expression instance.
  private Statement       errStmt                 = null;  // Returned if error in generating a Statement instance.
  private BlockStmt       errBlkStmt              = null;  // Returned if error in generating a BlockStmt instance.
  private RecordType      noRecordType            = null;  // Terminates incomplete type chain.
  private UnionType       noUnionType             = null;  // Terminates incomplete type chain.
  private EnumerationType noEnumType              = null;  // Terminates incomplete type chain.
  private FieldDecl[]     fieldSequence           = null;  // For finding fields in anonyous structs.
  private String          declAtts                = null;  // For collecting attributes for declarations.
  private UniqueName      un                      = null;  // The name generator to use for any variables created.
  private PragmaStk       pragmaStk               = null;

  /**
   * The types for the various C pre-defined types.
   */
  private IntegerType     ptrdiff_t_type;
  private IntegerType     size_t_type;
  private IntegerType     signed_char_type;
  private IntegerType     unsigned_char_type;
  private IntegerType     unsigned_short_type;
  private IntegerType     unsigned_int_type;
  private int             unsigned_int_type_size;
  private IntegerType     unsigned_long_type;
  private int             unsigned_long_type_size;
  private IntegerType     unsigned_long_long_type;
  private int             unsigned_long_long_type_size;
  private IntegerType     char_type;
  private IntegerType     wchar_type;
  private IntegerType     short_type;
  private IntegerType     int_type;
  private int             int_type_size;
  private IntegerType     long_type;
  private int             long_type_size;
  private IntegerType     long_long_type;
  private int             long_long_type_size;
  private FloatType       float_type;
  private FloatType       double_type;
  private FloatType       long_double_type;
  private ComplexType     double_complex_type;
  private FloatType       double_imaginary_type;
  private ComplexType     float_complex_type;
  private FloatType       float_imaginary_type;
  private IntegerType     bool_type;
  private VoidType        void_type;
  private PointerType     voidp_type;
  private PointerType     charp_type;

  /**
   * The different visibilities for the declarations.
   */
  private static final int cNone     = 0;
  private static final int cAuto     = 1;
  private static final int cExtern   = 2;
  private static final int cStatic   = 3;
  private static final int cRegister = 4;
  private static final int cGlobal   = 5;
  private static final int cSCERR    = 6;
  private static final int cInlined  = 8;

  private static final int SC_MASK   = 7;


  /**
   * Mapping from visibility to visibility.
   */
  private static final int[][] scTrans = {
    /* from/to      none       auto       extern   static   register   global   cSCERR*/
    /* none     */ {cSCERR,    cAuto,     cExtern, cStatic, cRegister, cGlobal, cSCERR},
    /* auto     */ {cAuto,     cAuto,     cExtern, cStatic, cRegister, cGlobal, cSCERR},
    /* extern   */ {cExtern,   cExtern,   cExtern, cSCERR,  cSCERR,    cSCERR,  cSCERR},
    /* static   */ {cStatic,   cStatic,   cSCERR,  cStatic, cSCERR,    cSCERR,  cSCERR},
    /* register */ {cRegister, cRegister, cSCERR,  cSCERR,  cRegister, cSCERR,  cSCERR},
    /* global   */ {cGlobal,   cGlobal,   cExtern, cStatic, cSCERR,    cGlobal, cSCERR},
    /* cSCERR   */ {cSCERR,    cSCERR,    cSCERR,  cSCERR,  cSCERR,    cSCERR,  cSCERR},
  };

  /**
   * The different type qualifers.
   */
  private static final int cConstantType = 1;
  private static final int cVolatile     = 2;
  private static final int cRestrict     = 4;

  /**
   * The set of integer indexes for the pre-defined types.
   */
  private static final byte cStart            = 0;
  private static final byte cChar             = 1;
  private static final byte cShort            = 2;
  private static final byte cInt              = 3;
  private static final byte cLong             = 4;
  private static final byte cLongLong         = 5;
  private static final byte cSignedChar       = 6;
  private static final byte cUnsignedChar     = 7;
  private static final byte cUnsignedShort    = 8;
  private static final byte cUnsignedInt      = 9;
  private static final byte cUnsignedLong     = 10;
  private static final byte cUnsignedLongLong = 11;
  private static final byte cSigned           = 12;
  private static final byte cUnsigned         = 13;
  private static final byte cFloat            = 14;
  private static final byte cDouble           = 15;
  private static final byte cLongDouble       = 16;
  private static final byte cBool             = 17;
  private static final byte cComplex          = 18;
  private static final byte cImaginary        = 19;
  private static final byte cFloatComplex     = 20;
  private static final byte cFloatImaginary   = 21;
  private static final byte cDoubleComplex    = 22;
  private static final byte cDoubleImaginary  = 23;
  private static final byte cVoid             = 24;
  private static final byte cOther            = 25; // struct, union, enum
  private static final byte cError            = 26;

  private static final String[] typeNames = {
    "cStart",
    "cChar",
    "cShort",
    "cInt",
    "cLong",
    "cLongLong",
    "cSignedChar",
    "cUnsignedChar",
    "cUnsignedShort",
    "cUnsignedInt",
    "cUnsignedLong",
    "cUnsignedLongLong",
    "cSigned",
    "cUnsigned",
    "cFloat",
    "cDouble",
    "cLongDouble",
    "cBool",
    "cComplex",
    "cImaginary",
    "cFloatComplex",
    "cFloatImaginary",
    "cDoubleComplex",
    "cDoubleImaginary",
    "cVoid",
    "cOther",
    "cError"
  };

  /**
   * For conversion from type specifier to type specifier.
   */
  private static final byte[][] typeFSA = {
    // cStart
    {cError, cChar, cShort, cInt, cLong,
    cLongLong, cSignedChar, cUnsignedChar, cUnsignedShort, cUnsignedInt, cUnsignedLong,
    cUnsignedLongLong, cSigned, cUnsigned, cFloat, cDouble,
    cLongDouble, cBool, cComplex, cImaginary, cError,
    cError, cError, cError, cVoid, cOther},
    // cChar
    {cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError, cError,
    cError, cChar, cUnsignedChar, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError},
    // cShort    
    {cError, cError, cError, cShort, cError,
    cError, cError, cError, cError, cError, cError,
    cError, cShort, cUnsignedShort, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError},
    // cInt
    {cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError, cError,
    cError, cInt, cUnsignedInt, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError},
    // cLong
    {cError, cError, cError, cLong, cLongLong,
    cError, cError, cError, cError, cError, cError,
    cError, cLong, cUnsignedLong, cError, cLongDouble,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError},
    // cLongLong
    {cError, cError, cError, cLongLong, cError,
    cError, cError, cError, cError, cError, cError,
    cError, cLongLong, cUnsignedLongLong, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError},
    // cSignedChar
    {cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError},
    // cUnsignedChar
    {cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError},
    // cUnsignedShort    
    {cError, cError, cError, cUnsignedShort, cError,
    cError, cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError},
    // cUnsignedInt
    {cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError},
    // cUnsignedLong
    {cError, cError, cError,cUnsignedLong, cUnsignedLongLong,
    cError, cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError},
    // cUnsignedLongLong
    {cError, cError, cError,cUnsignedLongLong, cError,
    cError, cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError},
    // cSigned
    {cError, cSignedChar, cShort, cInt, cLong,
    cLongLong, cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError},
    // cUnsigned
    {cError, cUnsignedChar, cUnsignedShort, cUnsignedInt, cUnsignedLong,
    cUnsignedLongLong, cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError},
    // cFloat
    {cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cFloatComplex, cFloatImaginary, cError,
    cError, cError, cError, cError, cError},
    // cDouble
    {cError, cError, cError, cError, cLongDouble,
    cError, cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cDoubleComplex, cDoubleImaginary, cError,
    cError, cError, cError, cError, cError},
    // cLongDouble
    {cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError},
    // cBool
    {cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError},
    // cComplex
    {cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError, cError,
    cError, cError, cError, cFloatComplex, cDoubleComplex,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError},
    // cImaginary
    {cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError, cError,
    cError, cError, cError, cFloatImaginary, cDoubleImaginary,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError},
    // CFloatComplex
    {cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError},
    // cFloatImaginary
    {cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError},
    // cDoubleComplex
    {cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError},
    // cDoubleImaaginary
    {cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError},
    // cVoid
    {cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError},
    // cOther
    {cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError,
    cError, cError, cError, cError, cError},
  };

  /**
   * The following is a table of function name to function type for
   * pre-defined functions.  These functions are defined if the user
   * source program references them before defining them.  The function
   * type is represented by a string where each letter corresponds to
   * a type.  The first leter is the return type.
   * <ul>
   * <li> v - void
   * <li> V - void*
   * <li> d - double
   * <li> D - double*
   * <li> f - float
   * <li> F - Float*
   * <li> i - int
   * <li> I - int*
   * <li> l - long
   * <li> L - long*
   * <li> m - unsigned long
   * <li> M - unsigned long*
   * <li> s - size_t
   * <li> S - size_t*
   * <li> c - char
   * <li> C - char*
   * <li> H - const char *
   * <li> Z - variable args
   * If this table gets big enough, use a binary search.
   * For now we are just lazy.
   */
  private static final String[] preKnownFtns = {
    "abort",  "v",
    "exit",   "vi", 
    "malloc", "Vs",
    "memcpy", "VVVs",
    "memset", "VVis",
    "printf", "iHZ", 
    "strcat", "CCC",
    "strcpy", "CCC",
    "strlen", "sC",
  };

  /**
   * This is the main routine for the C parser.
   * @param cg is the {@link scale.callGraph.CallGraph CallGraph} to be populated from the source code
   * @param cpp is the {@link scale.frontend.c.CPreprocessor C pre-processor} in use
   * @param warnings is a vector in which any warning messages are added
   */
  public CallGraph parse(CallGraph cg, CPreprocessor cpp, Vector<String> warnings)
  {
    this.cg  = cg;
    this.cpp = cpp;
    this.warnings = warnings;
    this.un  = new UniqueName("_np");
    this.pragmaStk = new PragmaStk(getFilename());

    try {
      translationUnit(); // The main syntactical unit.

      if (fatalError) {
        reportError(Msg.MSG_Errors_in_compilation, null, getFilename(), LT(0).getLine(), LT(0).getColumn());
        return null;
      }

      return cg;
    } catch (RecognitionException ex) {
      reportError(ex);
      return null;
    } catch (antlr.TokenStreamRecognitionException ex) {
      reportError(ex);
      return null;
    } catch(java.lang.Exception e) {
      String msg = e.getMessage();
      System.out.println("***** " + e.getClass().getName());
      e.printStackTrace();
      return null;
    } catch(java.lang.Error e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Over-ride the default traceIn() method so that the parser can be
   * generated with tracing enabled but tracing only occurs if the
   * user requests it.
   */
  public void traceIn(String msg) throws antlr.TokenStreamException
   {
     if (classTrace)
       super.traceIn(msg);
   }

  /**
   * Over-ride the default traceOut() method so that the parser can be
   * generated with tracing enabled but tracing only occurs if the
   * user requests it.
   */
  public void traceOut(String msg) throws antlr.TokenStreamException
   {
     if (classTrace)
       super.traceOut(msg);
   }

  private boolean assertTrace(String msg, Object o)
  {
    if (!classTrace)
      return true;

    for (int i = 0; i <= traceDepth; i++)
      System.out.print(" ");

    System.out.print(msg);
    if (o == null)
      o = "";
    System.out.println(o);

    return true;
  }

  /**
   *  Report the error message for the file at the line and column specified.
   */
  private void reportError(int errno, String text, String filename, int lineno, int column)
  {
    Msg.reportError(errno, filename, lineno, column, text);
    if (classTrace || fullError)
      Debug.printStackTrace();
  }

  /**
   * Over-ride the default ANTLR report error method so that a "fatal
   * error" flag may be set and so that we can format the error report
   * to our taste.
   */
  public void reportError(antlr.TokenStreamRecognitionException tex)
  {
    RecognitionException ex = tex.recog;
    fatalError = true;
    reportError(Msg.MSG_s, ex.getMessage(), getFilename(), ex.getLine(), ex.getColumn());
  }

  /**
   * Over-ride the default ANTLR report error method so that a "fatal
   * error" flag may be set and so that we can format the error report
   * to our taste.
   */
  public void reportError(RecognitionException ex)
  {
    fatalError = true;
    reportError(Msg.MSG_s, ex.getMessage(), getFilename(), ex.getLine(), ex.getColumn());
  }

  /**
   * Report an error message to the user.
   */
  private void userError(int errno, String text, Token t)
  {
    userError(errno, text, null, null, t);
  }

  /**
   * Report an error message to the user.
   * Display the specified object if tracing is requested.
   * The token provides the filename, line number and column.
   */
  private void userError(int errno, String text, Object o, Token t)
  {
    userError(errno, text, o, null, t);
  }

  /**
   * Report an error message to the user.
   * Display the specified objects if tracing is requested.
   * The token provides the filename, line number and column.
   */
  private void userError(int errno, String text, Object o1, Object o2, Token t)
  {
    if (classTrace || fullError) {
      if (o1 != null)
        System.out.println(" 1 " + o1);
      if (o2 != null)
        System.out.println(" 2 " + o2);
    }
    fatalError = true;
    reportError(errno, text, getFilename(), t.getLine(), t.getColumn());
  }

  /**
   * Report an error message for an expression to the user.
   * Display the specified object if tracing is requested.
   * The token provides the filename, line number and column.
   * @return the error expression
   */
  private Expression userExprError(int errno, String text, Object expr, Token t)
  {
    userError(errno, text, expr, null, t);
    return errExp;
  }

  /**
   * Report an error message for an expression to the user.
   * Display the specified objects if tracing is requested.
   * The token provides the filename, line number and column.
   * @return the error expression
   */
  private Expression userExprError(int errno, String text, Object expr1, Object expr2, Token t)
  {
    userError(errno, text, expr1, expr2, t);
    return errExp;
  }

  /**
   * Report a "not implemented" error message to the user.
   * The token provides the filename, line number and column.
   */
  private void notImplementedError(String msg, Token t)
  {
    fatalError = true;
    Msg.reportError(Msg.MSG_Not_implemented_s, getFilename(), t.getLine(), t.getColumn(), msg);
  }

  /**
   * Report a warning message to the user.
   * The token provides the filename, line number and column.
   */
  private void userWarning(int errno, String text, Token t)
  {
    Msg.reportWarning(errno, getFilename(), t.getLine(), t.getColumn(), text);
  }

  /**
   * Specify the type qualifier and report and error if it was already specified.
   */
  private void specifyTypeQualifier(int tq)
  {
    typeQualifier |= tq;
  }

  /**
   * Convert from the index of the type to the type and add the type
   * qualifier.
   * @return the type or <code>null</code> if it is not valid.
   */
  private Type buildType(int baseType, Type buildType)
  {
    Type bt = null;

    switch (baseType) {
    case cChar:             bt = char_type;               break;
    case cShort:            bt = short_type;              break;
    case cSigned:           bt = int_type;                break;
    case cInt:              bt = int_type;                break;
    case cLong:             bt = long_type;               break;
    case cLongLong:         bt = long_long_type;          break;
    case cSignedChar:       bt = signed_char_type;        break;
    case cUnsignedChar:     bt = unsigned_char_type;      break;
    case cUnsignedShort:    bt = unsigned_short_type;     break;
    case cUnsigned:         bt = unsigned_int_type;       break;
    case cUnsignedInt:      bt = unsigned_int_type;       break;
    case cUnsignedLong:     bt = unsigned_long_type;      break;
    case cUnsignedLongLong: bt = unsigned_long_long_type; break;
    case cFloat:            bt = float_type;              break;
    case cDouble:           bt = double_type;             break;
    case cLongDouble:       bt = long_double_type;        break;
    case cBool:             bt = bool_type;               break;
    case cComplex:          bt = double_complex_type;     break;
    case cImaginary:        bt = double_imaginary_type;   break;
    case cFloatComplex:     bt = float_complex_type;      break;
    case cFloatImaginary:   bt = float_imaginary_type;    break;
    case cDoubleComplex:    bt = double_complex_type;     break;
    case cDoubleImaginary:  bt = double_imaginary_type;   break;
    case cVoid:             bt = void_type;               break;
    case cOther:            bt = buildType;               break;
    default:                return null;
    }

    bt = addTypeQualifier(bt, typeQualifier);

    return bt;
  }

  private Type addTypeQualifier(Type type, int typeQualifier)
  {
    if (typeQualifier == 0)
      return type;

    if (0 != (typeQualifier & cRestrict)) {
      if (!type.isPointerType())
        return null;
      type = RefType.create(type, RefAttr.Restrict);
    }

    if (0 != (typeQualifier & cConstantType))
      type = RefType.create(type, RefAttr.Const);

    if (0 != (typeQualifier & cVolatile))
      type = RefType.create(type, RefAttr.Volatile);

    return type;
  }

  private int setStorageClass(int sc, int sc2)
  {
    return (sc & ~SC_MASK) + sc2;
  }

  private boolean isStorageClass(int sc, int sc2)
  {
    return (sc & SC_MASK) == sc2;
  }

  /**
   * Specify the storage class of a declaration.  Generate an error if
   * the storage class is not valid for the type of declaration.
   */
  private void specifyStorageClass(Declaration decl, int storageClass, Token t)
  {
    if (decl == null)
      return;

    RoutineDecl rd = decl.returnRoutineDecl();
    if (rd != null) {
      if (0 != (storageClass & cInlined))
        rd.setInlineSpecified();

      switch (storageClass & SC_MASK) {
      case cNone:   return;
      case cExtern: rd.setVisibility(Visibility.EXTERN); return;
      case cStatic: rd.setVisibility(Visibility.FILE);   return;
      case cGlobal: rd.setVisibility(Visibility.GLOBAL); return;
      }
      assert assertTrace("** ssc ", Integer.toHexString(storageClass));
      userError(Msg.MSG_Invalid_storage_class, null, rd, t);
      return;
    }

    if (0 != (storageClass & cInlined))
      userError(Msg.MSG_Invalid_storage_class, null, decl, t);

    decl.setVisibility(Visibility.LOCAL);

    switch (storageClass & SC_MASK) {
    case cExtern:
      decl.setVisibility(Visibility.EXTERN);
      decl.setResidency(Residency.MEMORY);
      break;
    case cStatic:
      if (topLevel)
        decl.setVisibility(Visibility.FILE);
      decl.setResidency(Residency.MEMORY);
      break;
    case cGlobal:
      decl.setVisibility(Visibility.GLOBAL);
      decl.setResidency(Residency.MEMORY);
      decl.setReferenced();
      break;
    case cAuto:
      break;
    case cRegister:
      decl.setResidency(Residency.REGISTER);
      break;
    }
  }

  /**
   * Convert from current type to new type.
   * For example, from "long" to "long int".
   * @return the new type
   */
  private int specifyBaseType(int type, int baseType)
  {
//    System.out.println("** sbt " + typeNames[baseType] + " " + typeNames[type] + " -> " + typeNames[typeFSA[baseType][type]]);
    return typeFSA[baseType][type];
  }

  /**
   * Convert from the current storage class to a new storage class.
   * Report an error if the conversion is not valid.
   * @return the new storage class.
   */
  private int specifyStorageClass(int storageClass, int sc, Token t)
  {
    int osc = scTrans[storageClass & SC_MASK][sc & SC_MASK];
    if (osc == cSCERR)
      userError(Msg.MSG_Storage_class_specified_twice, null, t);
    return osc | (storageClass & ~SC_MASK);
  }

  /**
   * Initialize all the member fields.
   */
  private void initialize()
  {
    assert ((typeFSA.length == cError) && (typeFSA[0].length == cError)) : "Invalid typesNFA.";

    labels        = new HashMap<String, LabelDecl>(11);
    fieldSequence = new FieldDecl[4];
    procMap       = new HashMap<String, ProcedureDecl>(61);
    varlitMap     = new HashMap<SymtabScope, HashMap<Expression,VariableDecl>>(7);

    ptrdiff_t_type          = Machine.currentMachine.getPtrdifftType();
    size_t_type             = Machine.currentMachine.getSizetType();

    signed_char_type        = Machine.currentMachine.getSignedCharType();
    unsigned_char_type      = Machine.currentMachine.getUnsignedCharType();
    unsigned_short_type     = Machine.currentMachine.getUnsignedShortType();
    unsigned_int_type       = Machine.currentMachine.getUnsignedIntType();
    unsigned_long_type      = Machine.currentMachine.getUnsignedLongType();
    unsigned_long_long_type = Machine.currentMachine.getUnsignedLongLongType();

    unsigned_int_type_size       = unsigned_int_type.bitSize();
    unsigned_long_type_size      = unsigned_long_type.bitSize();
    unsigned_long_long_type_size = unsigned_long_long_type.bitSize();

    char_type               = scale.clef.type.IntegerType.cCharsAreSigned ? signed_char_type : unsigned_char_type;
    wchar_type              = Machine.currentMachine.getWchartType();
    short_type              = Machine.currentMachine.getSignedShortType();
    int_type                = Machine.currentMachine.getSignedIntType();
    long_type               = Machine.currentMachine.getSignedLongType();
    long_long_type          = Machine.currentMachine.getSignedLongLongType();

    int_type_size           = int_type.bitSize();
    long_type_size          = long_type.bitSize();
    long_long_type_size     = long_long_type.bitSize();

    float_type              = Machine.currentMachine.getFloatType();
    double_type             = Machine.currentMachine.getDoubleType();
    long_double_type        = Machine.currentMachine.getLongDoubleType();

    double_complex_type     = ComplexType.create(double_type.bitSize(), double_type.bitSize());
    double_imaginary_type   = double_type;
    float_complex_type      = ComplexType.create(float_type.bitSize(), float_type.bitSize());
    float_imaginary_type    = float_type;
    bool_type               = int_type;
    void_type               = VoidType.type;
    voidp_type              = PointerType.create(void_type);
    charp_type              = PointerType.create(char_type);

    dzero = LiteralMap.put(0.0, double_type);
    zero  = LiteralMap.put(0, int_type);
    one   = LiteralMap.put(1, int_type);

    errExp     = new NilOp();
    errStmt    = new NullStmt();
    errBlkStmt = new BlockStmt();

    if (allowGNUExtensions) {
      defineTypeName("ptrdiff_t", ptrdiff_t_type);
      defineTypeName("size_t", size_t_type);
      defineTypeName("wchar_t", wchar_type);
      defineTypeName("__builtin_va_list", RefType.create(Machine.currentMachine.getVaListType(), RefAttr.VaList));
    }
  }

  /**
   * Returns the last incomplete type or <code>null</code> if the 
   * incomplete type was completed.
   */
  private IncompleteType lastIncompleteType(Type t)
  {
    while (t instanceof RefType)
      t = ((RefType) t).getRefTo();

    if (t instanceof IncompleteType) {
      IncompleteType incType = (IncompleteType) t;
      Type tt = incType.getCompleteType();
      if ((tt == null) || (tt == noRecordType) || (tt == noUnionType) || (tt == noEnumType))
        return incType;
      return lastIncompleteType(tt);
    }

    return null;
  }

  /**
   * Return the label declaration for the specified name.
   * Create it if it doesn't already exist.
   */
  private LabelDecl getLabelDecl(String name)
  {
    LabelDecl ld = labels.get(name);
    if (ld != null)
      return ld;
    ld = new LabelDecl(name);
    labels.put(name, ld);
    cg.addSymbol(ld);
    return ld;
  }

  /**
   * Create a new temporary variable for use by the optimized code.
   */
  private VariableDecl genTemp(Type t)
  {
    VariableDecl vd = new VariableDecl(un.genName(), t);
    vd.setTemporary();
    return vd;
  }

  /**
   * Return the variable or function declaration for the name or
   * <code>null</code> if none.
   */
  private Declaration lookupVarDecl(String name)
  {
    Vector<SymtabEntry> ents = cg.getSymbolTable().lookupSymbol(name);
    if (ents == null)
      return null;

    Enumeration<SymtabEntry> en = ents.elements();
    while (en.hasMoreElements()) {
      SymtabEntry s    = en.nextElement();
      Declaration decl = s.getDecl();

      if (decl instanceof VariableDecl)
        return decl;
      if (decl instanceof EnumElementDecl)
        return decl;
      if (decl instanceof RoutineDecl)
        return decl;
    }

    return null;
  }

  /**
   * Create a declaration for the specified name and type.  Check if a
   * conflcting declaration already exists.  Return the declaration.
   * Note, in C, a variable can be defined as <code>extern</code> and
   * then redefined with an initializer.  Similar for functions.
   */
  private Declaration defineDecl(String name, Type type, int sc, Token t)
  {
    if (fatalError)
      return null;

    Symtab      st    = cg.getSymbolTable();
    SymtabScope cp    = (sc == cExtern) ? st.getRootScope() : st.getCurrentScope();
    Vector<SymtabEntry> ents  = st.lookupSymbol(name);
    boolean     isFtn = type.getCoreType().isProcedureType();

    // Check to see if it is already defined.

    if (ents != null) {
      int l = ents.size();
      for (int i = 0; i < l; i++) {
        SymtabEntry s  = ents.get(i);
        Declaration d  = s.getDecl();
        SymtabScope ds = s.getScope();

        RoutineDecl   rd  = d.returnRoutineDecl();
        if (isFtn && (rd != null)) {
          ProcedureType pt1 = (ProcedureType) type.getCoreType();
          ProcedureType pt2 = (ProcedureType) rd.getSignature().getCoreType();

          if (!pt2.equivalent(pt1)) {
            Type rt1 = pt1.getReturnType().getCoreType();
            Type rt2 = pt2.getReturnType().getCoreType();
            if (!rt2.equivalent(rt1)) {
              Type rtx1 = rt1.getPointedTo().getCoreType();
              Type rtx2 = rt2.getPointedTo().getCoreType();
              assert assertTrace("** proto " + pt2.numFormals() + " ", pt2);
              assert assertTrace("     def " + pt1.numFormals() + " ", pt1);
              assert assertTrace("     rt1 " + pt1.numFormals() + " ", rt1);
              assert assertTrace("     rt2 " + pt1.numFormals() + " ", rt2);
              assert assertTrace("     rt1 " + pt1.numFormals() + " ", rtx1);
              assert assertTrace("     rt2 " + pt1.numFormals() + " ", rtx2);
              userError(Msg.MSG_Function_s_definition_does_not_match_prototype, name, t);
              return rd;
            }
            int n1 = pt1.numFormals();
            int n2 = pt2.numFormals();
            if ((n1 != n2) && ((n1 * n2) != 0)) {
              assert assertTrace("** proto " + pt2.numFormals() + " ", pt2);
              assert assertTrace("     def " + pt1.numFormals() + " ", pt1);
              userError(Msg.MSG_Function_s_definition_does_not_match_prototype, name, t);
              return rd;
            }
            if (strictANSIC)
              userWarning(Msg.MSG_Function_s_definition_does_not_match_prototype, name, t);
          }

          if ((rd.getBody() == null) && (pt1.numFormals() >= pt2.numFormals()))
            rd.setType(pt1);

          cg.addRootSymbol(rd);
          cg.recordRoutine(rd);
          rd.setSourceLineNumber(t.getLine());

          if (name.equals("main"))
            rd.setMain();

          assert assertTrace("  1 ", d);

          return rd;
        }

        if (ds != cp)
          continue;

        if (!isFtn && d.isVariableDecl()) {
          if (d.getType().equivalent(type)) {
            if (ds != st.getCurrentScope())
              cg.addSymbol(d);
            ArrayType at = d.getCoreType().returnArrayType();
            if (at != null) {
              if (!at.isSizeKnown())
                d.setType(type);
            }
            assert assertTrace("  ", d);
            return d;
          }

          Type tt = type.getCoreType();
          Type dt = d.getCoreType();
          if (dt.equivalent(type.getCoreType())) {
            if (d.visibility() == Visibility.EXTERN)
              specifyStorageClass(d, sc, t);
            assert assertTrace("  ", d);
            return d;
          }

          ArrayType dat = dt.returnArrayType();
          ArrayType tat = tt.returnArrayType();
          if ((dat != null) && (tat != null)) {
            if (!dat.isSizeKnown()) {
              if (d.visibility() == Visibility.EXTERN)
                specifyStorageClass(d, sc, t);
              d.setType(type);
              assert assertTrace("  ", d);
              return d;
            }
            if (!tat.isSizeKnown()) {
              if (d.visibility() == Visibility.EXTERN)
                specifyStorageClass(d, sc, t);
              assert assertTrace("  ", d);
              return d;
            }
          }

          if (classTrace || fullError) {
            System.out.println("** dd " + d);
            System.out.println("      " + d.getType());
            System.out.println("      " + type);
            System.out.println("      " + dt);
            System.out.println("      " + tt);
          }

          userError(Msg.MSG_Variable_s_already_defined, name, d, t);
          break;
        }
      }
    }

    // Define the declaration.

    if (isFtn) {
      ProcedureDecl rd = new ProcedureDecl(name, (ProcedureType) type.getCoreType());
      specifyStorageClass(rd, setStorageClass(sc, cExtern), t);
      cg.addRootSymbol(rd);
      cg.recordRoutine(rd);
      if (name.equals("main"))
        rd.setMain();
      rd.setSourceLineNumber(t.getLine());
      assert assertTrace("  2 ", rd);
      return rd;
    }

    Declaration decl = new VariableDecl(name, type);
    specifyStorageClass(decl, sc, t);
    cp.addEntry(decl);
    assert assertTrace("  ", decl);

    return decl;
  }

  /**
   * Find the specified field in the specified <code>struct</code> or
   * <code>union</code>.  This routine is able to find fields in
   * nested (anonymous) structures.  This routine is recursive and is
   * not meant to be called directly.
   * @see #lookupField
   * @param name is the field name
   * @param at is the type of the structure
   * @param index is the value to return if the field is not found
   * @return the field's index into the <code>fieldSequence</code> array
   */
  private int findField(String name, AggregateType at, int index)
  {
    if (index >= fieldSequence.length) {
      FieldDecl[] nf = new FieldDecl[index * 2];
      System.arraycopy(fieldSequence, 0, nf, 0, fieldSequence.length);
      fieldSequence = nf;
    }

    int n = at.numFields();

    for (int i = 0; i < n; i++) {
      FieldDecl fd = at.getField(i);
      if (name.equals(fd.getName())) {
        fieldSequence[index++] = fd;
        return index;
      }
    }

    // Check anonymous sub-structs.

    for (int i = 0; i < n; i++) {
      FieldDecl fd = at.getField(i);
      if (fd.getName().startsWith("_F") && fd.getCoreType().isAggregateType()) {
        fieldSequence[index] = fd;
        int i2 = findField(name, (AggregateType) fd.getCoreType(), index + 1);
        if (i2 > index + 1)
          return i2;
      }
    }

    return index;
  }

  /**
   * Find the specified field in the specifiedtype.  This routine is
   * able to find fields in nested (anonymous) structures.  The type
   * must be a <code>struct</code>, <code>union</code>, or array of
   * <code>struct</code> or <code>union</code> instances.
   * @param name is the field name
   * @param at is the type of the structure
   * @return the field's index into the <code>fieldSequence</code> 
   * array or 0 if not found
   */
  private int lookupField(String name, Type type)
  {
    Type st = type;
    type = type.getPointedToCore();

    ArrayType at = type.returnArrayType();
    if (at != null)
      type = at.getElementType().getCoreType();

    AggregateType agt = type.returnAggregateType();
    if (agt != null)
      return findField(name, agt, 0);

    return 0;
  }

  /**
   * Add the specified field to the vector.  Report an error if a
   * field by that name is already in the vector.
   */
  private void addField(FieldDecl fd, Vector<FieldDecl> fields, Token t)
  {
    if (fd == null)
      return;

     String fdn = fd.getName();
     int    l   = fields.size();
     for (int i = 0; i < l; i++) {
       FieldDecl d = fields.get(i);
       if (d.getName().equals(fdn)) {
         userError(Msg.MSG_Field_s_already_defined, fdn, fd, t);
         return;
       }
     }
     fields.add(fd);
  }

  /**
   * Add the specified parameter to the vector.  Report an error if a
   * parameter by that name is already in the vector.
   */
  private void addParameter(FormalDecl fd, Vector<FormalDecl> params, Token t)
  {
    if (fd == null)
      return;

    String fdn = fd.getName();
    int    l   = params.size();
    for (int i = 0; i < l; i++) {
      FormalDecl d = params.get(i);
      if (d.getName().equals(fdn)) {
        userError(Msg.MSG_Parameter_s_already_defined, fdn, d, t);
        return;
      }
    }
    params.add(fd);
  }

  /**
   * Return a new {@link scale.clef.decl.TypeName TypeName} instance.
   * A {@link scale.clef.decl.TypeName TypeName} instance is used to
   * represent a user defined named type (e.g., <code>typedef</code>).
   */
  private TypeName defineTypeName(String name, Type type)
  {
    TypeName nametype = new TypeName(name, type);
    cg.addSymbol(nametype);
    type = RefType.create(type, nametype);
    nametype.setType(type);
    return nametype;
  }

  /**
   * Return a new {@link scale.clef.decl.TypeDecl TypeDecl} instance.
   * A {@link scale.clef.decl.TypeDel TypeDecl} instance is used to
   * represent a a named <code>struct</code>, <code>union</code>, or
   * <code>enum</code>.  For example, <pre>struct x {};</pre>
   */
  private TypeDecl createTypeDecl(String name, Type type)
  {
    TypeDecl decl = new TypeDecl(name, type);
    cg.addSymbol(decl);
    type = RefType.create(type, decl);
    decl.setType(type);
    return decl;
  }

  /**
   * Return the specified {@link scale.clef.decl.TypeDecl TypeDecl}
   * instance or <code>null</code> if none.
   */
  private TypeDecl lookupTypeDecl(String name)
  {
    Vector<SymtabEntry> ents = cg.getSymbolTable().lookupSymbol(name);
    if (ents == null)
      return null;

    int l = ents.size();
    if (l < 1)
      return null;

    for (int i = 0; i < l; i++) {
      SymtabEntry s    = ents.get(i);
      Declaration decl = s.getDecl();
      if (decl instanceof TypeDecl) {
        return (TypeDecl) decl;
      }
    }
    return null;
  }

  /**
   * Return true if the specified type is really an integer type.
   */
  private boolean isIntegerType(Type type)
  {
    Type t = type.getCoreType();
    return t.isIntegerType() || t.isEnumerationType() || t.isBooleanType();
  }

  /**
   * Determine the type to use for the type of the right-hand-side of
   * an assignment statement.  Needs a lot of work.
   */
  private Type calcAssignType(Type type1, Type type2, Token t)
  {
    Type t1 = type1.getCoreType();
    Type t2 = type2.getCoreType();

    if (t1 == t2)
      return type1;

    if (isIntegerType(t1)) {
      if (isIntegerType(t2)) {
        return type1;
      } else if (t2.isRealType()) {
        return type1;
      } else if (t2.isPointerType())
        return type1;
    } else if (t1.isRealType()) {
      if (isIntegerType(t2)) {
        return type1;
      } else if (t2.isRealType()) {
       return type1;
      }
    } else if (t1.isPointerType()) {
      if (t2.isPointerType()) {
        Type t1p = t1.getPointedTo().getCoreType();
        Type t2p = t2.getPointedTo().getCoreType();
        if (t1p.equivalent(t2p))
          return type1.getNonConstType();
        if ((t1p == void_type) || (t2p == void_type))
          return voidp_type;
        ArrayType at = t2p.returnArrayType();
        if (at != null) {
          if (t1p.equivalent(at.getElementType().getCoreType()))
            return type1.getNonConstType();
        }
        return type1;
      } else if (isIntegerType(t2)) {
        return type1.getNonConstType();
      }
    } else if (t1.isArrayType() && t2.isArrayType()) {
      return type2;
    }

    if (fullError || classTrace) {
      System.out.println("** cat " + type1);
      System.out.println("       " + t1);
      System.out.println("       " + type2);
      System.out.println("       " + t2);
      Debug.printStackTrace();
    }

    return null;
  }

  /**
   * Determine the type to use for the argument to a function.  Needs
   * a lot of work.
   */
  private Type calcArgType(Type type1, Type type2)
  {
    Type t1 = type1.getCoreType();
    Type t2 = type2.getCoreType();

    if (t1 == t2)
      return type1;

    if (t2.isPointerType()) {
      ArrayType t2p = t2.getPointedTo().getCoreType().returnArrayType();
      if (t2p != null) {
        type2 = PointerType.create(t2p.getElementType());
        t2 = type2;
      }
    }

    ArrayType at = t1.returnArrayType();
    if (at != null) {
      type1 = PointerType.create(at.getElementType());
      t1 = type1;
    }

    IntegerType it = t1.returnIntegerType();
    if (it != null) {
      if (it.bitSize() < int_type_size)
        type1 = int_type;
      if (t2.isIntegerType()) {
        return type1;
      } else if (t2.isPointerType())
        return type1;
      else if (t2.isEnumerationType())
        return type1;
      else if (t2.isBooleanType())
        return type1;
      else if (t2.isRealType())
        return type1;
    } else if (t1.isRealType()) {
      return type1;
    } else if (t1.isPointerType()) {
      Type t1p = t1.getPointedTo().getCoreType();
      Type t2p = t2.getPointedTo().getCoreType();

      if (t1p.equivalent(t2p))
        return type2.getNonConstType();
      if (t2.isPointerType()) {
        return type1.getNonConstType();
      } else if (isIntegerType(t2)) {
        return type1.getNonConstType();
      }
    } else if (t1.isEnumerationType()) {
      if (isIntegerType(t2))
        return int_type;
    } else if (t1.isArrayType()) {
      if (t2.getPointedTo().isArrayType())
        return type2;
    }

    if (allowGNUExtensions && t1.isUnionType())
      return t1;

    if (fullError || classTrace) {
      System.out.println("** carg " + type1);
      System.out.println("        " + t1);
      System.out.println("        " + type2);
      System.out.println("        " + t2);
      Debug.printStackTrace();
    }

    return null;
  }

  private IntegerType bestIntType(IntegerType it1, IntegerType it2)
  {
    IntegerType type = null;
    if (it1.bitSize() > it2.bitSize())
      type = it1;
    else if (it1.bitSize() < it2.bitSize())
      type = it2;
    else if (it1.isSigned())
      type = it2;
    else
      type = it1;

    if (type.bitSize() <= int_type.bitSize())
      type = type.isSigned() ? int_type : unsigned_int_type;
    else if (type.bitSize() <= long_type.bitSize())
      type = type.isSigned() ? long_type : unsigned_long_type;
    else
      type = type.isSigned() ? long_long_type : unsigned_long_long_type;

    return type;
  }

  /**
   * Determine the type to use for the arguments to binary (dyadic)
   * operator.  Needs a lot of work.
   */
  private Type calcBinaryType(Type type1, Type type2)
  {

    Type t1 = type1.getCoreType();
    Type t2 = type2.getCoreType();

    if (t1.isEnumerationType() || t1.isBooleanType())
      t1 = int_type;

    if (t2.isEnumerationType() || t2.isBooleanType())
      t2 = int_type;

    IntegerType it1 = t1.returnIntegerType();
    if (it1 != null) {
      IntegerType it2 = t2.returnIntegerType();
      if (it2 != null) {
        return bestIntType(it1, it2);
      } else if (t2.isComplexType()) {
        ;
      } else if (t2.isRealType()) {
        return type2;
      } else if (t2.isPointerType())
        return t2.getNonConstType();
    } else if (t1.isComplexType()) {
      if (t2.isComplexType())
        return type1;
    } else if (t1.isRealType()) {
      if (isIntegerType(t2))
        return type1;
      else if (t2.isRealType()) {
        RealType rt1 = (RealType) t1;
        RealType rt2 = (RealType) t2;
        if (rt1.bitSize() > rt2.bitSize())
          return type1;
        return type2;
      }
    } else if (t1.isPointerType() && t2.isIntegerType())
      return ptrdiff_t_type;

    if (t1 == t2)
      return t1;

    if (true || classTrace) {
      System.out.println("** cbt " + type1);
      System.out.println("       " + t1);
      System.out.println("       " + type2);
      System.out.println("       " + t2);
      Debug.printStackTrace();
    }

    return null;
  }

  /**
   * Determine the type to use for the arguments to expression-if (?:)
   * operator.  Needs a lot of work.
   */
  private Type calcCommonType(Type type1, Type type2, Token t)
  {
    Type t1 = type1.getCoreType();
    Type t2 = type2.getCoreType();

    if (t1 == t2)
      return type1;

    IntegerType it1 = t1.returnIntegerType();
    if (it1 != null) {
      IntegerType it2 = t2.returnIntegerType();
      if (it2 != null) {
        return bestIntType(it1, it2);
      } else if (t2.isRealType()) {
        return type2;
      } else if (t2.isPointerType())
        return t2.getNonConstType();
      else if (t2.isEnumerationType())
        return type1;
      else if (t2.isBooleanType())
        return type1;
    } else if (t1.isRealType()) {
      if (isIntegerType(t2)) {
        return type1;
      } else if (t2.isRealType()) {
        RealType rt1 = (RealType) t1;
        RealType rt2 = (RealType) t2;
        if (rt1.bitSize() > rt2.bitSize())
          return type1;
        else
          return type2;
      }
    } else if (t1.isPointerType()) {
      Type t1p = t1.getCoreType().getPointedTo();
      if (t1p.isProcedureType())
        return type1;

      ArrayType at = t1p.returnArrayType();
      if ((at != null)  && (at.getRank() == 1))
        t1p = at.getElementType();

      if (t2.isPointerType()) {
        Type t2p = t2.getCoreType().getPointedTo(); 

        if (t1p == VoidType.type)
          return t1;

        if (t2p == VoidType.type)
          return t2;

        ArrayType at2 = t2p.getCoreType().returnArrayType();
        if ((at2 != null) && (at2.getRank() == 1))
          t2p = at2.getElementType();

        Type t3 = calcCommonType(t1p, t2p, t);

        if (t3 != null)
          return PointerType.create(t3);
      } else if (isIntegerType(t2))
        return type1;
    } else if (t1.isEnumerationType() || t1.isBooleanType()) {
      if (t2.isIntegerType())
        return type2;
      if (t2.isEnumerationType() || t2.isBooleanType())
        return int_type;
    }

    if (true || classTrace) {
      System.out.println("** cct " + t.getLine());
      System.out.println("       " + type1);
      System.out.println("       " + t1);
      System.out.println("       " + type2);
      System.out.println("       " + t2);
      Debug.printStackTrace();
    }

    return null;
  }

  /**
   * If the address of a constant is required or the constant is not
   * an atomic value, it must be allocated to memory.  In this case we
   * know that the value of the created variable will not be modified.
   * This allows us to re-use the constant.
   */
  private VariableDecl createConstVarFromExp(Expression lit)
  {
    Symtab       symtab = cg.getSymbolTable();
    SymtabScope  scope  = symtab.getCurrentScope();
    HashMap<Expression,VariableDecl> map = varlitMap.get(scope);
    VariableDecl decl   = null;

    if (map != null) { // Maybe it has already been created.
      decl = map.get(lit);
      if (decl != null)
        return decl;
    }

    Type           type = lit.getType();
    FixedArrayType at   = type.getCoreType().returnFixedArrayType();
    if (at != null)
      type = at.copy(RefType.create(at.getElementType(), RefAttr.Const));
    else
      type = RefType.create(type, RefAttr.Const);

    decl = new VariableDecl("_V" + varCounter++, type, lit);

    decl.setResidency(Residency.MEMORY);
    decl.setAddressTaken();
    decl.setReferenced();

    cg.addSymbol(decl);
    if (symtab.isAtRoot()) {
      decl.setVisibility(Visibility.FILE);
      cg.addTopLevelDecl(decl);
    }

    if (map == null) {
      map = new HashMap<Expression,VariableDecl>(11);
      varlitMap.put(scope, map);
    }

    map.put(lit, decl);

    return decl;
  }

  /**
   * If the address of a constant is required or the constant is not
   * an atomic value, it must be allocated to memory.
   */
  private VariableDecl createVarFromExp(Expression lit)
  {
    VariableDecl decl = new VariableDecl("_V" + varCounter++, lit.getType(), lit);

    decl.setResidency(Residency.MEMORY);
    decl.setAddressTaken();
    decl.setReferenced();

    cg.addSymbol(decl);
    if (cg.getSymbolTable().isAtRoot()) {
      decl.setVisibility(Visibility.FILE);
      cg.addTopLevelDecl(decl);
    }

    return decl;
  }

  private IdAddressOp genDeclAddress(Declaration decl)
  {
    Type      type = decl.getType();
    ArrayType at   = type.getCoreType().returnArrayType();
    if (at != null)
      type = at.getArraySubtype();
    return new IdAddressOp(PointerType.create(type), decl);
  }

  /**
   * Return the l-value form of the expression.
   */
  private Expression makeLValue(Expression expr0)
  {
    if (fatalError)
      return errExp;

    if (expr0 instanceof IdValueOp)
      return genDeclAddress(((IdValueOp) expr0).getDecl());

    if (expr0 instanceof SubscriptValueOp) {
      SubscriptValueOp svop = (SubscriptValueOp) expr0;
      return svop.makeLValue();
    }

    if (expr0 instanceof SelectOp) {
      SelectOp   sop    = (SelectOp) expr0;
      Expression struct = sop.getStruct();
      FieldDecl  field  = sop.getField();

      return new SelectIndirectOp(struct, field);
    }

    if (expr0 instanceof DereferenceOp)
      return ((DereferenceOp) expr0).getExpr();

    if (expr0 instanceof IdAddressOp)
      return expr0;
    if (expr0 instanceof SelectIndirectOp)
      return expr0;
    if (expr0 instanceof SubscriptAddressOp)
      return expr0;
    if (expr0 instanceof Literal) {
      VariableDecl decl = createVarFromExp(expr0);
      return buildVariableAddressRef(decl);
    }

    if (expr0 instanceof TypeConversionOp) {
      TypeConversionOp top = (TypeConversionOp) expr0;
      if (top.getConversion() == CastMode.CAST) {
        Type       ty  = PointerType.create(top.getType());
        Expression arg = makeLValue(top.getExpr());
        return new TypeConversionOp(ty, arg, CastMode.CAST);
      }
      Expression arg = top.getExpr();
      if (arg instanceof SelectOp)
        return makeLValue(arg);
    }

    Type ptype = PointerType.create(expr0.getType());
    return new AddressOp(ptype, expr0);
  }

  /**
   * Generate a cast expression.
   */
  private Expression cast(Type type, Expression exp, Token t)
  {
    if (fatalError)
      return errExp;

    if (type == null)
      return exp;

    CastMode cr = TypeConversionOp.determineCast(type, exp.getType());

    switch (cr) {
    case NONE:
      return exp;

    default:
      return userExprError(Msg.MSG_Unknown_conversion, null, exp, t);

    case INVALID: {
      Type from = exp.getCoreType();
      Type to   = type.getCoreType();

      if (allowGNUExtensions && (to.isUnionType())) {
        UnionType ut = (UnionType) to;
        int       l  = ut.numFields();
        for (int i = 0; i < l; i++) {
          FieldDecl fd = ut.getField(i);
          Type      ft = fd.getCoreType();
          if (ft.equivalent(from)) {
            VariableDecl vd     = genTemp(type);
            Expression   lhs    = new SelectIndirectOp(genDeclAddress(vd), fd);
            Expression   assign = new AssignSimpleOp(lhs, cast(fd.getType(), exp, t));
            cg.addSymbol(vd);
            return new SeriesOp(assign, new IdValueOp(vd));
          }
        }
      }


      AggregateType at = to.returnAggregateType();;
      if (at != null) {
        FieldDecl     fd = at.getField(0);
        Type          ft = fd.getCoreType();
        if (ft.equivalent(from)) {
          VariableDecl vd     = genTemp(type);
          Expression   lhs    = new SelectIndirectOp(genDeclAddress(vd), fd);
          Expression   assign = new AssignSimpleOp(lhs, cast(fd.getType(), exp, t));
          cg.addSymbol(vd);
          return new SeriesOp(assign, new IdValueOp(vd));
        }
      }

      if (true || classTrace) {
        System.out.println("** cast to " + type);
        System.out.println("     core  " + to);
        System.out.println("     from  " + from);
        System.out.println("     of    " + exp);
        Debug.printStackTrace();
      }

      return userExprError(Msg.MSG_Unknown_conversion, null, exp, t);
    }
    case CAST:
      if (exp instanceof AddressLiteral)
        return ((AddressLiteral) exp).copy(type); 
      if (exp instanceof IntLiteral)
        return LiteralMap.put(((IntLiteral) exp).getLongValue(), type);
      return new TypeConversionOp(type, exp, CastMode.CAST);
    case TRUNCATE:
      if (exp instanceof IntLiteral) {
        Expression il = LiteralMap.put(((IntLiteral) exp).getLongValue(), type);
        return il;
      }
      if (exp instanceof FloatLiteral) {
        Expression il = LiteralMap.put((long) ((FloatLiteral) exp).getDoubleValue(), type);
        return il;
      }
      return new TypeConversionOp(type, exp, CastMode.TRUNCATE);
    case REAL:
      if (exp instanceof IntLiteral) {
        IntLiteral il = (IntLiteral) exp;
        if (type.getCoreType() == float_type)
          return LiteralMap.put(il.convertToFloat(), type);
        return LiteralMap.put(il.convertToDouble(), type);
      }
      if (exp instanceof FloatLiteral) {
        if (type.getCoreType() == float_type)
          return LiteralMap.put((float)((FloatLiteral) exp).getDoubleValue(), type);
        return LiteralMap.put(((FloatLiteral) exp).getDoubleValue(), type);
      }
      return new TypeConversionOp(type, exp, CastMode.REAL);
    }
  }

  /**
   * Return the array type required for a string.
   */
  private Type makeCharArrayType(String s, Type type)
  {
    int n = s.length();
    ArrayType at = FixedArrayType.create(0, n - 1, type);
    return at;
  }

  /**
   * Return the long value that the expression represents.
   */
  private long getLongValue(Expression exp, Token t)
  {
    Object o = exp;
    if (!(o instanceof IntLiteral))
      o = exp.getConstantValue();

    if (o instanceof IntLiteral) {
      IntLiteral il = (IntLiteral) o;
      return il.getLongValue();
    }
    userError(Msg.MSG_Not_a_constant, null, exp, t);
    return 0;
  }

  /**
   * Return the int value that the expression represents.
   */
  private int getIntValue(Expression exp, Token t)
  {
    Object o = exp;
    if (!(o instanceof IntLiteral))
      o = exp.getConstantValue();

    if (o instanceof IntLiteral) {
      IntLiteral il = (IntLiteral) o;
      return (int) il.getLongValue();
    }

    userError(Msg.MSG_Not_a_constant, null, exp, t);
    return 0;
  }

  private void processPragma(String prag, Token t)
  {
    if (prag.startsWith("ident ")) {
      warnings.add(prag);
      return;
    }

    if (pragmaStk.newPragma(prag, t.getLine()))
      userWarning(Msg.MSG_Pragma_not_processed_s, prag, t);
  }

  private int processInitializers(int            pos,
                                  Type           type,
                                  Vector<Object> initializers,
                                  Expression     exp,
                                  Vector<Object> positions,
                                  Token          t)
  {
    if (positions == null) {
      initializers.addElement(exp);
      return pos + 1;
    }

    int l = positions.size();

    type = type.getCoreType().getPointedTo();

    if (type.isArrayType()) {
      for (int i = 0; i < l; i++) {
        IntLiteral p   = (IntLiteral) positions.elementAt(i);
        pos = (int) p.getLongValue();
        initializers.addElement(new PositionIndexOp(pos));
        initializers.addElement(exp);
      }
      return pos + 1;
    }

    AggregateType agt = type.returnAggregateType();
    if (agt != null) {
      int n  = agt.numFields();
      for (int i = 0; i < l; i++) {
        String name   = (String) positions.elementAt(i);
        for (int j = 0; j < n; j++) {
          FieldDecl fd = agt.getField(j);
          if (name.equals(fd.getName())) {
            initializers.addElement(new PositionFieldOp(fd));
            initializers.addElement(exp);
          }
        }
      }
      return pos + 1;
    }

    userError(Msg.MSG_Invalid_initializer, null, t);
    return pos;
  }

  private void processDeclInitializer(Declaration decl, Type type, Expression expr, Token t)
  {
    if (fatalError)
      return;

    if (decl == null)
      return;

    if (expr == null)
    return;

    if (decl.visibility() == Visibility.EXTERN) {
      decl.setVisibility(Visibility.GLOBAL);
      decl.setReferenced();
    }

    // If possiible, use a simple constant value as the initializer 

    Literal lit = expr.getConstantValue();
    if ((lit != Lattice.Bot) && (lit != Lattice.Top)) {
      if (!(lit instanceof AddressLiteral) ||
          (decl.residency() == Residency.MEMORY)) {
        // The original expression is preferred over an AddressLiteral
        // because an IdAddressOp is handled better.
        expr = lit;
      }
    }

    ValueDecl d  = (ValueDecl) decl;
    Type      dt = d.getType();

    if (dt.isPointerType() && expr.getType().isArrayType()) {
      VariableDecl vd = createConstVarFromExp(expr);
      expr = buildVariableRef(vd);
    } else {
      ArrayType at = dt.getCoreType().returnArrayType();
      if ((at != null)  && at.isSizeKnown() && (expr instanceof StringLiteral)) {
        expr = LiteralMap.put(((StringLiteral) expr).getString(), dt);
      }
    }

    Type ty = calcAssignType(type, expr.getType(), t);
    if (ty == null) {
      userError(Msg.MSG_Operands_to_s_not_compatible, "=", type, expr.getType(), t);
      return;
    }

    expr = cast(ty, expr, t);

    d.setValue(expr);

    ArrayType at = dt.getCoreType().returnArrayType();
    if (at != null) { // Fix up arrays whose size is defined by their initializer.
      if (!at.isSizeKnown())
        decl.setType(ty);
    }
  }

  /**
   * Return the type associated with the field at the specified position.
   */
  private Type findType(Type type, int position, Vector<Object> positions, Token t)
  {
    if (fatalError)
      return null;

    if (positions == null) {
      type = type.getPointedToCore();
      ArrayType at = type.returnArrayType();
      if (at != null)
        return at.getArraySubtype();

      AggregateType agt = type.returnAggregateType();
      if (agt != null) {
        int n  = agt.numFields();
        if (position < n)
          return agt.getField(position).getType();
      }
      return null;
    }

    if (type.isAggregateType()) {
      int l = positions.size();
      if (l > 0) {
        String  fn = (String) positions.get(0);
        int     fd = lookupField(fn, type);
        if (fd == 0) {
          userError(Msg.MSG_Field_s_not_found, fn, type, t);
          return int_type;
        }
        return fieldSequence[fd - 1].getType().getNonConstType();
      }
    }

    notImplementedError("designator " + positions.getClass().getName(), t);

    return null;
  }

  private Type createEnumType(String name, Vector<EnumElementDecl> ev, Token t)
  {
    if (name == null) {
      EnumerationType etype = EnumerationType.create(ev);
      TypeDecl decl = createTypeDecl("_T" + typeCounter++, etype);
      cg.addSymbol(decl);
      return decl.getType();
    }

    TypeDecl decl = lookupTypeDecl(name);
    if (decl == null) {
      if (ev == null) {
        IncompleteType ictype = new IncompleteType();
        if (noEnumType == null)
          noEnumType = EnumerationType.create(new Vector<EnumElementDecl>(0));
        ictype.setCompleteType(noEnumType);
        decl = createTypeDecl(name, ictype);
        cg.addSymbol(decl);
        return decl.getType();
      }

      EnumerationType etype = EnumerationType.create(ev);
      decl = createTypeDecl(name, etype);
      cg.addSymbol(decl);
      return decl.getType();
    }

    IncompleteType ictype = lastIncompleteType(decl.getType());
    if (ev != null) {
      if (ictype == null)
        userError(Msg.MSG_Enum_s_already_defined, name, t);
      EnumerationType etype = EnumerationType.create(ev);
      ictype.setCompleteType(etype);
      return decl.getType();
    }

    if (!decl.getCoreType().isEnumerationType()) {
      userError(Msg.MSG_Variable_s_already_defined, name, t);
      return void_type;
    }

    return decl.getType();
  }

  private Bound createBound(long min, long max)
  {
    if (max == 0) {
      return Bound.noValues;
    }

    return Bound.create(min, max - 1);
  }

  private ArrayType createArrayType(boolean noAdd, long min, long max, Type elementType, Token t)
  {
    if (elementType.isProcedureType())
      userError(Msg.MSG_Array_of_functions_is_not_allowed, null, t);

    Type           tt = elementType.getCoreType();
    FixedArrayType at = null;
    if (tt != null) // Not an incomplete type.
      at = tt.returnFixedArrayType();

    if (noAdd || (at == null)) {
      if (max == 0) {
        Vector<Bound> indicies = new Vector<Bound>(1);
        Bound         bd       = Bound.noValues;
        indicies.add(bd);
        return FixedArrayType.create(indicies, elementType);
      }
      return FixedArrayType.create(min, max - 1, elementType);
    }

    int           rank     = at.getRank();
    Vector<Bound> indicies = new Vector<Bound>(rank + 1);
    Bound         bd       = Bound.noValues;
    if (max > 0)
      bd = createBound(min, max);
    else
      Debug.printStackTrace();

    indicies.add(bd);

    for (int i = 0; i < rank; i++)
      indicies.add(at.getIndex(i));

    return FixedArrayType.create(indicies, at.getElementType());
  }

  private ArrayType createArrayType(boolean noAdd, Vector<Bound> indicies, Type elementType, Token t)
  {
    if (elementType.isProcedureType())
      userError(Msg.MSG_Array_of_functions_is_not_allowed, null, t);

    FixedArrayType at = elementType.getCoreType().returnFixedArrayType();
    if (noAdd || (at == null))
      return FixedArrayType.create(indicies, elementType);

    int rank = at.getRank();

    for (int i = 0; i < rank; i++)
      indicies.add(at.getIndex(i));

    return FixedArrayType.create(indicies, at.getElementType());
  }

  private ArrayType createArrayType(boolean noAdd, Type type, Expression expr1, Token t)
  {
    Object         o  = expr1.getConstantValue();
    Type           tt = type.getCoreType();
    FixedArrayType at = null;

    if (tt != null) // Not an incomplete type.
      at = tt.returnFixedArrayType();

    if (o instanceof IntLiteral) {
      long max = ((IntLiteral) o).getLongValue();
      if (noAdd || (at == null))
        return createArrayType(noAdd, 0, max, type, t);

      Bound bd = createBound(0, max);
      return at.addIndex(bd);
    }

    Expression max = expr1;
    Bound      bd  = Bound.create(zero, new SubtractionOp(max.getType(), max, one));
    if (allowGNUExtensions) {
      if (!noAdd && (at != null))
        return at.addIndex(bd);

      Vector<Bound> v = new Vector<Bound>(1);
      v.add(bd);
      return createArrayType(true, v, type, t);
    }

    userError(Msg.MSG_Invalid_dimension, null, expr1, t);
    Vector<Bound> v = new Vector<Bound>(1);
    v.add(bd);
    return createArrayType(noAdd, v, int_type, t);
  }

  private ArrayType createArrayType(boolean noAdd, Type type, Token t)
  {
    Bound          bd = Bound.noBound;
    FixedArrayType at = type.getCoreType().returnFixedArrayType();
    if (!noAdd && (at != null))
      return at.addIndex(bd);

    Vector<Bound> bounds = new Vector<Bound>(1);
    bounds.addElement(bd);
    return createArrayType(noAdd, bounds, type, t);
  }

  private Type createProcedureType(Type type, Vector<FormalDecl> params, Token t)
  {
    if (fatalError)
      return null;

    int d = 0;
    while (type.isPointerType()) {
      type = type.getCoreType().getPointedTo();
      d++;
    }

    ProcedureType pt = type.getCoreType().returnProcedureType();
    if (pt == null) {
      userError(Msg.MSG_Invalid_procedure_type, null, type, t);
      return type;
    }

    Type rt = pt.getReturnType();

    type = makeProcedureType(rt, params);

    for (int i = 0; i < d; i++)
      type = PointerType.create(type);

    return type;
  }

  /**
   * Add the source line number information to the AST node.
   */
  private void addStmtInfo(Statement n, int lineNumber, int column)
  {
    if (n != null) {
      n.setSourceLineNumber(lineNumber);
      n.setPragma(pragmaStk.getTop());
    }
  }

  /**
   * Return an expression whose result is true (1) or false (0).
   */
  private Expression trueOrFalse(Expression exp, Token t)
   {
     if (fatalError)
       return errExp;

     if ((exp == null) || exp.hasTrueFalseResult())
       return exp;

     Type type = exp.getCoreType();
     if (isIntegerType(type) || type.isPointerType())
       return new NotEqualOp(bool_type, exp, zero);

     if (type.isRealType())
       return new NotEqualOp(bool_type, exp, dzero);

     return userExprError(Msg.MSG_Invalid_expression, null, exp, t);
   }

  /**
   * Return an expression whose result is true (1) or false (0).
   */
  private Expression equal0(Expression exp, Token t)
   {
     if (fatalError)
       return errExp;

     if (exp.hasTrueFalseResult())
       return new NotOp(bool_type, exp);
     Type type = exp.getCoreType();
     if (isIntegerType(type) || type.isPointerType())
       return new EqualityOp(bool_type, exp, zero);
     if (type.isRealType())
       return new EqualityOp(bool_type, exp, dzero);
     return userExprError(Msg.MSG_Invalid_expression, null, exp, t);
   }

  /**
   * Return a reference expression to the string.
   */
  private Expression stringConstant(StringLiteral sl)
  {
    VariableDecl decl = createVarFromExp(sl);
    return buildVariableRef(decl);
  }

  /**
   * Return the <code>long</code> value represented by the string of
   * hex digits.  We use this method because
   * java.lang.Long.parseLong(String, 16) complains about
   * "8000000000000000".
   */
  private long parseHex(String digits)
  {
    int  l = digits.length();
    long s = 0;

    for (int i = 0; i < l; i++) {
      char c = digits.charAt(i);
      int n = 0;
      if ((c >= '0') && (c <= '9'))
        n = c - '0';
      else if ((c >= 'a') && (c <= 'f'))
        n = c - 'a' + 10;
      else if ((c >= 'A') && (c <= 'F'))
        n = c - 'A' + 10;
      s = (s << 4) | n;
    }

    return s;
  }

  /**
   * Return the <code>long</code> value represented by the string of
   * decimal digits.  We use this method because
   * java.lang.Long.parseLong(String, 10) complains about
   * "18446744073709551615".
   */
  private long parseLong(String digits)
  {
    int  l = digits.length();
    long s = 0;

    if (digits.charAt(0) == '0') {
      boolean ok = true;
      for (int i = 0; i < l; i++) {
        char c = digits.charAt(i);
        int  n = c - '0';
        if (n > 7) {
          ok = false;
          break;
        }
        s = (s << 3) + n;
      }
      if (ok)
        return s;
    }

    for (int i = 0; i < l; i++) {
      char c = digits.charAt(i);
      int  n = c - '0';
      s = (s << 3) + ((s << 1) + n);
    }

    return s;
  }

  /**
   * Return the <code>double</code> value represented by the string of
   * hex digits.
   */
  private double parseHexDouble(String digits)
  {
    int  l = digits.length();
    long s = 0;
    long p = 0;

    int i = 0;
    int k = 0;
    int m = 0;
    char c = 0;
    while (i < l) {
      c = digits.charAt(i++);
      int  n = 0;
      if ((c >= '0') && (c <= '9'))
        n = c - '0';
      else if ((c >= 'a') && (c <= 'f'))
        n = c - 'a' + 10;
      else if ((c >= 'A') && (c <= 'F'))
        n = c - 'A' + 10;
      else
        break;
      s = (s << 4) + n;
      k++;
      m++;
    }

    if (i < l) {
      if (c == '.') {
        while (i < l) {
          c = digits.charAt(i++);
          int  n = 0;
          if ((c >= '0') && (c <= '9'))
            n = c - '0';
          else if ((c >= 'a') && (c <= 'f'))
            n = c - 'a' + 10;
          else if ((c >= 'A') && (c <= 'F'))
            n = c - 'A' + 10;
          else
            break;
          s = (s << 4) + n;
          k++;
        }
      }

      if (c == 'p') {
        while (i < l) {
          c = digits.charAt(i++);
          int  n = c - '0';
          p = (p * 10) + n;
        }
      }
    }

    if (s == 0)
      return 0.0;

    p += m * 4 - 2;

    if (k < 16)
      s <<= ((16 - k) * 4);

    while ((s & 0x8000000000000000L) == 0) {
      s <<= 1;
      p--;
    }
    s = (s >> 11) & 0xfffffffffffffL;

    p = (p + 0x400) & 0x7ff;

    return Double.longBitsToDouble((p << 52) + s);
  }

  /**
   * Return the type represented by the single character
   * representation of a type.
   * <ul>
   * <li> v - void
   * <li> V - void*
   * <li> d - double
   * <li> D - double*
   * <li> f - float
   * <li> F - Float*
   * <li> i - int
   * <li> I - int*
   * <li> l - long
   * <li> L - long*
   * <li> m - unsigned long
   * <li> M - unsigned long*
   * <li> s - size_t
   * <li> S - size_t*
   * <li> c - char
   * <li> C - char*
   * <li> H - const char *
   * <li> Z - variable args
   */
  private  Type charToType(char c)
  {
    switch (c) {
    case 'i': return int_type;   
    case 'l': return long_type;  
    case 'm': return unsigned_long_type;  
    case 'f': return float_type; 
    case 'd': return double_type;
    case 'v': return void_type;  
    case 'V': return voidp_type; 
    case 's': return size_t_type;
    case 'c': return char_type;  
    case 'C': return charp_type; 
    case 'H': return PointerType.create(RefType.create(char_type, RefAttr.Const));
    default: throw new scale.common.InternalError("Unknown type char(" + c + ").");
    }
  }

  /**
   * Return a FormalDecl instance of the type represented by the
   * single character representation of a type.
   */
  private FormalDecl charToFormalDecl(char c, int counter)
  {
    if (c == 'Z')
      return new UnknownFormals();

    return createFormalDecl(null, charToType(c), counter);
  }

  /**
   * Return the ProcedureDecl instance of a function with the
   * specified name and signature.  The signaature is a string of
   * characters.  The first character represents the function return
   * type.  Each additional character represnts the type of a
   * parameter.
   */
  private ProcedureDecl defPreKnownFtn(String name, String signature)
  {
    ProcedureDecl rd = procMap.get(name);
    if (rd == null) {
      int                l       = signature.length();
      Type               rt      = charToType(signature.charAt(0));
      Vector<FormalDecl> formals = new Vector<FormalDecl>(l - 1);
      for (int i = 1; i < l; i++)
        formals.add(charToFormalDecl(signature.charAt(i), i));
      ProcedureType pt = ProcedureType.create(rt, formals, null);
      rd = new ProcedureDecl(name, pt);
      rd.setVisibility(Visibility.EXTERN);
      cg.addRootSymbol(rd);
      procMap.put(name, rd);
    }
    return rd;
  }

  /**
   * Return the expression equivalent to a call to the specified
   * function or <code>null</code> if none.
   */
  private Expression builtin(String name, Vector<Expression> args)
  {
     if (fatalError)
       return errExp;

    int l = (args == null) ? 0 : args.size();
    switch (l) {
    case 0:
      if (name.equals("__builtin_trap"))
        defPreKnownFtn("_scale" + name.substring(9), "v");
      else if (name.equals("__builtin_huge_val"))
        return LiteralMap.put(Double.MAX_VALUE, float_type);
      else if (name.equals("__builtin_huge_valf"))
        return LiteralMap.put(Float.MAX_VALUE, double_type);
      break;
    case 1: {
      Expression arg0 = args.get(0);
      if (name.equals("va_end") || name.equals("__builtin_va_end")) {
        if (arg0 instanceof IdReferenceOp)
          ((IdReferenceOp) arg0).getDecl().setAddressTaken();
        return new VaEndOp(void_type, makeLValue(arg0));
      }
      if (name.equals("__builtin_alloca"))
        return new TranscendentalOp(voidp_type, arg0, TransFtn.Alloca);
      if (name.equals("__builtin_abs") ||
          name.equals("__builtin_labs") ||
          name.equals("__builtin_llabs") ||
          name.equals("__builtin_imaxabs") ||
          name.equals("__builtin_fabs") ||
          name.equals("__builtin_fabsf") ||
          name.equals("__builtin_fabsl"))
        return new AbsoluteValueOp(arg0.getType().getNonConstType(), arg0);
      if (name.equals("__builtin_sqrt") || name.equals("__builtin_sqrtl") || name.equals("__builtin_sqrtf"))
        return new TranscendentalOp(arg0.getType().getNonConstType(), arg0, TransFtn.Sqrt);
      if (name.equals("__builtin_sin") || name.equals("__builtin_sinl") || name.equals("__builtin_sinf"))
        return new TranscendentalOp(arg0.getType().getNonConstType(), arg0, TransFtn.Sin);
      if (name.equals("__builtin_cos") || name.equals("__builtin_cosl") || name.equals("__builtin_cosf"))
        return new TranscendentalOp(arg0.getType().getNonConstType(), arg0, TransFtn.Cos);
      if (name.equals("__builtin_exp")  || name.equals("__builtin_expl") || name.equals("__builtin_expf"))
        return new TranscendentalOp(arg0.getType().getNonConstType(), arg0, TransFtn.Exp);
      if (name.equals("__builtin_log") || name.equals("__builtin_logl") || name.equals("__builtin_logf"))
        return new TranscendentalOp(arg0.getType().getNonConstType(), arg0, TransFtn.Log);
      if (name.equals("__builtin_conj") || name.equals("__builtin_conjl") || name.equals("__builtin_conjf"))
        return new TranscendentalOp(arg0.getType().getNonConstType(), arg0, TransFtn.Conjg);
      if (name.equals("__builtin_real") || name.equals("__builtin_reall") || name.equals("__builtin_realf"))
        return new TypeConversionOp(double_type, arg0, CastMode.REAL);
      if (name.equals("__builtin_imag") || name.equals("__builtin_imagl") || name.equals("__builtin_imagf"))
        return new TypeConversionOp(double_type, arg0, CastMode.IMAGINARY);
      if (name.equals("__builtin_frame_address"))
        return new TranscendentalOp(voidp_type, arg0, TransFtn.FrameAddress);
      if (name.equals("__builtin_return_address"))
        return new TranscendentalOp(voidp_type, arg0, TransFtn.ReturnAddress);
      if (name.equals("__builtin_constant_p")) {
        Literal lit = arg0.getConstantValue();
        return ((lit != Lattice.Bot) && (lit != Lattice.Top) && !(lit instanceof AddressLiteral)) ? one : zero;
      }
      if (name.equals("__builtin_setjmp"))
        defPreKnownFtn("_scale" + name.substring(9), "iV");
      else if (name.equals("__builtin_ffs") || name.equals("__builtin_putchar"))
        defPreKnownFtn("_scale" + name.substring(9), "ii");
      else if (name.equals("__builtin_puts"))
        defPreKnownFtn("_scale" + name.substring(9), "iC");
      }
      break;
    case 2: {
      Expression argl = args.get(0);
      Expression argr = args.get(1);
      if (name.equals("va_start") || name.equals("__builtin_stdarg_start") || name.equals("__builtin_va_start")) {
        if (argl instanceof IdReferenceOp)
          ((IdReferenceOp) argl).getDecl().setAddressTaken();
        return new VaStartOp(void_type, makeLValue(argl), (FormalDecl) argr.getDecl());
      }
      if (name.equals("va_copy") || name.equals("__builtin_va_copy") || name.equals("__va_copy")) {
        if (argr instanceof IdReferenceOp)
          ((IdReferenceOp) argr).getDecl().setAddressTaken();
        if (argl instanceof IdReferenceOp)
          ((IdReferenceOp) argl).getDecl().setAddressTaken();
        return new VaCopyOp(void_type, makeLValue(argl), makeLValue(argr));
      }
      if (name.equals("__builtin_isgreater"))
        return new GreaterOp(bool_type, argl, argr);
      if (name.equals("__builtin_isgreaterequal"))
        return new GreaterEqualOp(bool_type, argl, argr);
      if (name.equals("__builtin_isless"))
        return new LessOp(bool_type, argl, argr);
      if (name.equals("__builtin_islessequal"))
        return new LessEqualOp(bool_type, argl, argr);
      if (name.equals("__builtin_isequal"))
        return new EqualityOp(bool_type, argl, argr);
      if (name.equals("__builtin_islessgreater"))
        return new NotEqualOp(bool_type, argl, argr);
      if (name.equals("__builtin_expect"))
        return argl;
      if (name.equals("__builtin_longcmp"))
        defPreKnownFtn("_scale" + name.substring(9), "vVi");
      else if (name.equals("__builtin_fputs"))
        defPreKnownFtn("_scale" + name.substring(9), "iCV");
      else if (name.equals("__builtin_fputc"))
        defPreKnownFtn("_scale" + name.substring(9), "iiV");
      else if (name.equals("__builtin_strspn"))
        defPreKnownFtn("_scale" + name.substring(9), "sCC");
      else if (name.equals("__builtin_strpbrk") ||
               name.equals("__builtin_strcat") ||
               name.equals("__builtin_strstr") ||
               name.equals("__builtin_strcpy"))
        defPreKnownFtn("_scale" + name.substring(9), "CCC");
      else if (name.equals("__builtin_strchr") ||
               name.equals("__builtin_strrchr") ||
               name.equals("__builtin_index") ||
               name.equals("__builtin_rindex"))
        defPreKnownFtn("_scale" + name.substring(9), "CCi");
      }
      break;
    case 3:
      Expression arg0 = args.get(0);
      Expression arg1 = args.get(1);
      Expression arg2 = args.get(1);
      if (name.equals("va_start") || name.equals("__builtin_stdarg_start") || name.equals("__builtin_va_start")) {
        if (arg0 instanceof IdReferenceOp)
          ((IdReferenceOp) arg0).getDecl().setAddressTaken();
        return new VaStartOp(void_type, makeLValue(arg0), (FormalDecl) arg1.getDecl());
      }
      if (name.equals("__builtin_memset"))
        defPreKnownFtn("_scale" + name.substring(9), "VVis");
      else if (name.equals("__builtin_memcpy"))
        defPreKnownFtn("_scale" + name.substring(9), "VVVs");
      else if (name.equals("__builtin_strncat"))
        defPreKnownFtn("_scale" + name.substring(9), "CCCs");
      else if (name.equals("__builtin_strncpy"))
        defPreKnownFtn("_scale" + name.substring(9), "CCCs");
      else if (name.equals("__builtin_strncmp"))
        defPreKnownFtn("_scale" + name.substring(9), "iCCs");
      break;
    case 4:
      if (name.equals("__builtin_fwrite"))
        defPreKnownFtn("_scale" + name.substring(9), "mVmmV");
      break;
    }
    return null;
  }

  /**
   * Conform the elements to the type to produce the aggregation.
   * ***** This routine needs a lot more work. ***
   */
  private Expression buildAggregation(Type type, Vector<? extends Object> elements, Token t)
  {
     if (fatalError)
       return errExp;

    Vector<Object> v = new Vector<Object>();

    if (elements != null) {
      int ll = elements.size();
      if ((ll == 1) && !type.isCompositeType())
        return (Expression) elements.get(0);
      buildAggregation(type, v, elements, 0, ll, t);
      ArrayType at = type.getCoreType().returnArrayType();
      if (at != null) {
        if (!at.isSizeKnown()) {
          int l = v.size();
          if (l == 1) {
            Literal lit = ((Expression) v.get(0)).getConstantValue();
            if ((lit != Lattice.Bot) && (lit != Lattice.Top))
              l = lit.getCount();
          }
          type = createArrayType(true, 0, l, at.getArraySubtype(), t);
        }
      }
      for (int i = 0; i < ll; i++) {
        if (elements.get(i) != null) {
          userError(Msg.MSG_Too_many_initializers, null, t);
          break;
        }
      }
    }

    AggregationElements ag = new AggregationElements(type, v);
    return ag;
  }

  /**
   * @param type is the type of structure to be initialized
   * @param result is a vector to be filled with initialization elements
   * @param elements is the vector of items supplied by the user
   * @param index is the current posit+ion in elements
   * @param last is the index of the last element in elements to use
   * @param t specifies the current scan position
   */
  private int buildAggregation(Type           type,
                               Vector<Object> result,
                               Vector<? extends Object> elements,
                               int            index,
                               long           last,
                               Token          t)
  {
    Type          tt  = type.getCoreType();
    AggregateType agt = tt.returnAggregateType();
    if (agt != null)
      return buildStructAggregation(agt, result, elements, index, last, t);

    ArrayType at = tt.returnArrayType();
    if (at != null)
      return buildArrayAggregation(at, result, elements, index, last, t);

    if ((last - index) == 1) {
      result.add(elements.get(0));
      index++;
      return index;
    }

    if ((last - index) > 1) {
      assert assertTrace("** ba2 ", tt);
      assert assertTrace("       ", elements.get(0));
      userError(Msg.MSG_Invalid_initializer, null, tt, elements.get(0), t);
    }

    return index;
  }

  /**
   * @param at is the type of struct to be initialized
   * @param result is a vector to be filled with initialization elements
   * @param elements is the vector of items supplied by the user
   * @param index is the current position in elements
   * @param last is the index of the last element in elements to use
   * @param t specifies the current scan position
   */
  private int buildStructAggregation(AggregateType  at,
                                     Vector<Object> result,
                                     Vector<? extends Object> elements,
                                     int            index,
                                     long           last,
                                     Token          t)
  {
    int n = at.numFields();
    int k = 0;
    int i = 0;

    while ((index < last) && (i < n)) {
      int        from = index;
      Object     o    = elements.get(index++);
      FieldDecl  fd   = null;
      Expression exp  = null;

      if (o instanceof Expression) {
        exp = (Expression) o;
        fd = at.getField(i);
        i++;
      } else if (o instanceof PositionFieldOp) {
        result.add(o);
        elements.set(from, null);
        from = index;
        exp = (Expression) elements.get(index++);
        fd = ((PositionFieldOp) o).getField();
        i = at.getFieldIndex(fd) + 1;
      } else
        throw new scale.common.InternalError("Unknown initializer " + o);

      Literal lit = exp.getConstantValue();
      if ((lit != Lattice.Bot) && (lit != Lattice.Top))
        exp = lit;

      if ((exp instanceof AdditionOp) || (exp instanceof SubtractionOp)) {
        // Simplify if it's something like (a + 1), etc.
        DyadicOp   aop  = (DyadicOp) exp;
        Expression la   = aop.getExpr1();
        Expression ra   = aop.getExpr2();
        long       mult = (exp instanceof AdditionOp) ? 1 : -1;
        Literal    ll   = la.getConstantValue();
        if ((ll != Lattice.Top) && (ll != Lattice.Bot))
          la = ll;

        Literal    rl  = ra.getConstantValue();
        if ((rl != Lattice.Top) && (rl != Lattice.Bot))
          ra = rl;

        if ((mult == 1) && (la instanceof IntLiteral)) {
          Expression swap = la;
          la = ra;
          ra = swap;
        }

        if ((ra instanceof IntLiteral) && (la instanceof AddressLiteral)) {
          IntLiteral     il     = (IntLiteral) ra;
          AddressLiteral al     = (AddressLiteral) la;
          Declaration    decl   = al.getDecl();
          Type           ty     = exp.getType();
          long           tys    = ty.memorySize(Machine.currentMachine);
          long           offset = al.getOffset() + ((mult * tys) * il.getLongValue());
          exp = new AddressLiteral(ty, decl, offset);
        }
      }

      Type ft = fd.getType();
      Type et = exp.getCoreType();
      if (!ft.getCoreType().equivalent(et)) {
        if (isIntegerType(ft) && (exp instanceof IntLiteral)) {
          exp = LiteralMap.put(((IntLiteral) exp).getLongValue(), ft);
        } else if (ft.isPointerType() && !et.isPointerType()) {
          if (exp instanceof IntLiteral)
            exp = cast(ft, exp, t);
          else {
            VariableDecl decl = createConstVarFromExp(exp);
            exp = buildVariableRef(decl);
          }
        } else if (ft.isAggregateType()) {
          if (exp instanceof AggregationElements) {
            ;
          } else {
            Vector<Object> vv = new Vector<Object>();
            index = buildAggregation(ft, vv, elements, index - 1, last, t);
            exp = new AggregationElements(ft, vv);
          }
        } else if (ft.isArrayType()) {
          ArrayType art = (ArrayType) ft.getCoreType();

          if (exp instanceof AggregationElements)
            ;
          else if ((exp instanceof Literal) && (((Literal) exp).getCount() > 1)) {
            if (exp instanceof StringLiteral) {
              StringLiteral sl  = (StringLiteral) exp;
              String        str = sl.getStringValue();
              int           len = str.length() - 1;
              if ((len == ft.numberOfElements()) && (str.charAt(len) == 0)) {
                str = str.substring(0, len);
                exp = LiteralMap.put(str, makeCharArrayType(str, art.getElementType()));
              }
            }
          } else {
            Vector<Object>    vv  = new Vector<Object>();
            index = buildAggregation(ft, vv, elements, index - 1, last, t);
            if (!art.isSizeKnown())
              ft = createArrayType(true, 0, vv.size(), art.getElementType(), t);
            exp = new AggregationElements(ft, vv);
          }
        } else if (ft.isAtomicType())
          exp = cast(ft, exp, t);
      }

      result.add(exp);
      elements.set(from, null);
    }

    return index;
  }

  /**
   * @param at is the type of array to be initialized
   * @param result is a vector to be filled with initialization elements
   * @param elements is the vector of items supplied by the user
   * @param index is the current position in elements
   * @param last is the index of the last element in elements to use
   * @param t specifies the current scan position
   */
  private int buildArrayAggregation(ArrayType      at,
                                    Vector<Object> result,
                                    Vector<? extends Object> elements,
                                    int            index,
                                    long           last,
                                    Token          t)
  {
    Type ft  = at.getArraySubtype();
    Type tt  = ft.getCoreType();
    long num = 0;
    long noe = at.isSizeKnown() ? at.numberOfElements() : Integer.MAX_VALUE;

    // Try to consolidate memory by using an IntArrayLiteral or
    // FloatArrayLiteral.

    if (inConstant && (noe == (int) noe)) {
      int number = (int) ((noe < (last - index)) ? noe : (last - index));

      try {
        Type et = at.getElementType();
        if (ft.isRealType()) {
          Type              ct  = createArrayType(true, 0, number, et, t);
          FloatArrayLiteral fal = new FloatArrayLiteral(ct, number);
     
          for (int i = 0; i < number; i++) {
            Expression exp = (Expression) elements.get(index + i);
            Literal    lit = exp.getConstantValue();
            fal.addElement(Lattice.convertToDoubleValue(lit));
            elements.set(index + i, null);
          }
          result.add(fal);
          return index + number;
        }

        if (tt.isIntegerType()) {
          Type            ct  = createArrayType(true, 0, number, et, t);
          IntArrayLiteral fal = new IntArrayLiteral(ct, number);
          
          for (int i = 0; i < number; i++) {
            Expression exp = (Expression) elements.get(index + i);
            Literal    lit = exp.getConstantValue();
            fal.addElement(Lattice.convertToLongValue(lit));
            elements.set(index + i, null);
          }

          result.add(fal);
          return index + number;
        }
      } catch (java.lang.Throwable ex) {
      }
    }

    // Do it the large way.

    if (tt.isAtomicType()) {
      while ((index < last) && (num < noe)) {
        int        from = index;
        Expression exp  = (Expression) elements.get(index++);
        Literal    lit  = exp.getConstantValue();

        if ((lit != Lattice.Bot) && (lit != Lattice.Top))
          exp = lit;

        Type et = exp.getCoreType();

        if (isIntegerType(tt) && (exp instanceof IntLiteral) &&
            !tt.equivalent(exp.getCoreType())) {
          long value = ((IntLiteral) exp).getLongValue();
          exp = inConstant ? new IntLiteral(ft, value) : LiteralMap.put(value, ft);
        } else if (tt.isPointerType() && et.isArrayType()) {
          Type et2 = ((ArrayType) et.getCoreType()).getElementType();
          exp = new AddressLiteral(PointerType.create(et2), (Literal) exp, 0);
        } else if (et.isArrayType()) {
          num = noe;
        } else if (tt.isAtomicType())
          exp = cast(ft, exp, t);

        num++;

        result.add(exp);
        elements.set(from, null);
      }
      return index;
    }

    ArrayType ftc = tt.returnArrayType();
    if (ftc != null) {
      long      noe2 = ftc.isSizeKnown() ? ftc.numberOfElements() : Integer.MAX_VALUE;
      while ((index < last) && (num < noe)) {
        int        from = index;
        Expression exp  = (Expression) elements.get(index++);
        Literal    lit  = exp.getConstantValue();

        if ((lit != Lattice.Bot) && (lit != Lattice.Top))
          exp = lit;

        if (exp instanceof AggregationElements)
          ;
        else if (exp instanceof StringLiteral) {
          long ne = lit.getCount();
          if (ne != noe2) {
            StringBuffer buf = new StringBuffer(((StringLiteral) exp).getString());
            if ((ne > noe2) && (ne != (noe2 + 1)))
              userError(Msg.MSG_Invalid_initializer, null, at, elements.get(0), t);

            buf.setLength((int) noe2);
            exp = LiteralMap.put(buf.toString(), ftc);
          }
          num += noe2 - 1;
        } else if ((exp instanceof Literal) && (lit.getCount() > 1)) {
          num += lit.getCount() - 1;
        } else {
          Vector<Object> vv = new Vector<Object>();
          long           li = index - 1 + ftc.numberOfElements();
          index = buildAggregation(ft, vv, elements, index - 1, (li <= last) ? li : last, t);
          if (!ftc.isSizeKnown())
            ft = createArrayType(true, 0, vv.size(), ftc.getElementType(), t);
          exp = new AggregationElements(ft, vv);
        }

        num++;

        result.add(exp);
        elements.set(from, null);
      }
      return index;
    }

    AggregateType fta = tt.returnAggregateType();
    if (fta != null) {
      while ((index < last) && (num < noe)) {
        int        from = index;
        Expression exp  = (Expression) elements.get(index++);
        Literal    lit  = exp.getConstantValue();

        if ((lit != Lattice.Bot) && (lit != Lattice.Top))
          exp = lit;

        if (exp instanceof AggregationElements) {
          ;
        } else {
          Vector<Object> vv = new Vector<Object>();
          index = buildAggregation(ft, vv, elements, index - 1, last, t);
          exp = new AggregationElements(ft, vv);
        }

        num++;

        result.add(exp);
        elements.set(from, null);
      }
      return index;
    }

    assert assertTrace("** ba1 ", tt);
    assert assertTrace("       ", elements.get(0));
    userError(Msg.MSG_Invalid_initializer, null, at, elements.get(0), t);

    return index;
  }

  /**
   * Return true if all the elements of the vector are constants.
   */
  private boolean allConstants(Vector<Object> v)
  {
    int l = v.size();
    for (int i = 0; i < l; i++) {
      Object o = v.get(i);
      if (o instanceof Expression) {
        Expression exp = (Expression) o;
        if (exp instanceof AggregationElements) {
          if (!((AggregationElements) exp).containsAllLiterals())
            return false;
          continue;
        }
        if (exp instanceof AddressLiteral)
          return false;
        Literal lit = exp.getConstantValue();
        if (lit == Lattice.Bot)
          return false;
        if (lit == Lattice.Top)
          return false;
      }
    }
    return true;
  }

  private EnumElementDecl buildEnumElementDecl(String name, EnumElementDecl last, Expression expr, Token t)
  {
    if (expr == null) {
      int number = 0;
      if (last != null) {
        Expression val = last.getValue();
        if (val != null) {
          val = val.getConstantValue();
          if (val instanceof IntLiteral)
            number = (int) ((IntLiteral) val).getLongValue() + 1;
        }
      }
      expr = LiteralMap.put(number, int_type);
    }

     Symtab              st   = cg.getSymbolTable();
     SymtabScope         cp   = st.getCurrentScope();
     Vector<SymtabEntry> ents = st.lookupSymbol(name);

     // Check to see if it is already defined.

     if (ents != null) {
       int l = ents.size();
       for (int i = 0; i < l; i++) {
         SymtabEntry s  = ents.get(i);
         Declaration d  = s.getDecl();
         SymtabScope ds = s.getScope();
         if (d instanceof TypeName)
           continue;
         if (d instanceof TypeDecl)
           continue;

         if (ds == cp) {
           userError(Msg.MSG_Symbol_s_already_defined, name, expr, t);
           break;
         }
       }
     }

     // Define the declaration.

     EnumElementDecl ed = new EnumElementDecl(name, RefType.create(int_type, RefAttr.Const));
     cg.addSymbol(ed);
     ed.setValue(expr);
     return ed;
  }

  /**
   * Store the elements of an aggregation one at a time into the
   * variable.  Return a series of expressions of the store operations
   * with the last one a reference to the variable.
   */
  private Expression buildSeries(VariableDecl decl, Vector<Object> elements, Token t)
  {
     if (fatalError)
       return errExp;

    Expression load = new IdValueOp(decl);

    if ((elements == null) || (elements.size() == 0))
      return load;

    Vector<Expression> v = new Vector<Expression>();
    buildSeries(genDeclAddress(decl), decl.getCoreType(), v, elements, 0, 0, t);

    int        l   = v.size();
    Expression exp = v.get(0);
    for (int i = 1; i < l; i++)
      exp = new SeriesOp(exp, v.get(i));
    return new SeriesOp(exp, load);
  }
    
  private int buildSeries(Expression         var,
                          Type               type,
                          Vector<Expression> v,
                          Vector<Object>     elements,
                          int                index,
                          int                depth,
                          Token              t)
    {
    Type tt = type.getCoreType();
    int  l  = elements.size();

    AggregateType agt = tt.returnAggregateType();
    if (agt != null) {
      int           n  = agt.numFields();
      long          fieldOffset = 0;
      int           k  = 0;
      int           i  = 0;
      while (index < l) {
        Object     o   = elements.get(index++);
        FieldDecl  fd  = null;
        Expression exp = null;

        if (o instanceof Expression) {
          exp = (Expression) o;
          fd = agt.getField(i);
          i++;
        } else if (o instanceof PositionFieldOp) {
          exp = (Expression) elements.get(index++);
          fd = ((PositionFieldOp) o).getField();
          i = agt.getFieldIndex(fd) + 1;
        } else
          throw new scale.common.InternalError("Unknown initializer " + o);

        Type       ft  = fd.getType();
        Type       et  = exp.getCoreType();
        Expression fde = new SelectIndirectOp(var, fd);

        if (isIntegerType(ft) && (exp instanceof IntLiteral)) {
          exp = LiteralMap.put(((IntLiteral) exp).getLongValue(), ft);
          v.add(new AssignSimpleOp(fde, exp));
        } else if (ft.isPointerType() && !et.isPointerType()) {
          VariableDecl decl = createVarFromExp(exp);
          exp = buildVariableRef(decl);
          v.add(new AssignSimpleOp(fde, exp));
        } else if (ft.isAggregateType()) {
          if (exp instanceof AggregationElements)
            buildSeries(fde, ft, v, ((AggregationElements) exp).getElementVector(), 0, depth + 1, t);
          else
            index = buildSeries(fde, ft, v, elements, index - 1, depth + 1, t);
        } else if (ft.isArrayType()) {
          if (exp instanceof AggregationElements)
            buildSeries(fde, ft, v, ((AggregationElements) exp).getElementVector(), 0, depth + 1, t);
          else
            index = buildSeries(fde, ft, v, elements, index - 1, depth + 1, t);
        } else {
          exp = cast(ft, exp, t);
          v.add(new AssignSimpleOp(fde, exp));
        }

        int  fa = fd.isPackedField(Machine.currentMachine) ? 1 : ft.alignment(Machine.currentMachine);
        fieldOffset = Machine.alignTo(fieldOffset, fa);
        fd.setFieldOffset(fieldOffset);
        fieldOffset += ft.getCoreType().memorySize(Machine.currentMachine);
      }
      return index;
    }

    ArrayType at = tt.returnArrayType();
    if (at != null) {
      Type ft  = at.getArraySubtype();
      Type ftp = PointerType.create(ft);
      int  num = 0;
      if (ft.isArrayType()) {
        while (index < l) {
          Expression         exp = (Expression) elements.get(index++);
          Type               et  = exp.getCoreType();
          Vector<Expression> s   = new Vector<Expression>(1);
          s.add(LiteralMap.put(num++, int_type));
          Expression fde = new SubscriptAddressOp(ftp, var, s);
          if (exp instanceof AggregationElements)
            buildSeries(fde, ft, v, ((AggregationElements) exp).getElementVector(), 0, depth + 1, t);
          else
            index = buildSeries(fde, ft, v, elements, index - 1, depth + 1, t);
        }
      } else if (ft.isAggregateType()) {
        while (index < l) {
          Expression         exp = (Expression) elements.get(index++);
          Type               et  = exp.getCoreType();
          Vector<Expression> s   = new Vector<Expression>(1);
          s.add(LiteralMap.put(num++, int_type));
          Expression fde = new SubscriptAddressOp(ftp, var, s);
          if (exp instanceof AggregationElements)
            buildSeries(fde, ft, v, ((AggregationElements) exp).getElementVector(), 0, depth + 1, t);
          else
            index = buildSeries(fde, ft, v, elements, index - 1, depth + 1, t);
        }
      } else {
        while (index < l) {
          Expression         exp = (Expression) elements.get(index++);
          Type               et  = exp.getCoreType();
          Vector<Expression> s   = new Vector<Expression>(1);
          s.add(LiteralMap.put(num++, int_type));
          Expression fde = new SubscriptAddressOp(ftp, var, s);

          exp = cast(ft, exp, t);
          v.add(new AssignSimpleOp(fde, exp));
        }
      }
    }

    return index;
  }

  /**
   * Return a vector of the attributes specified by the string.
   */
  private Vector<String> parseAttributes(String attributes)
  {
    StringBuffer   att  = new StringBuffer(attributes);
    Vector<String> args = new Vector<String>();
    int            l    = att.length();
    int            i    = 0;
    while (i < l) {
      while ((i < l) && (att.charAt(i) == ' ')) i++;
      if ((i + 13) >= l)
        break;
      if (!"__attribute__".equals(att.substring(i, i + 13)))
        break;
      i += 13;
      while ((i < l) && (att.charAt(i) == ' ')) i++;
      if ((i >= l) || (att.charAt(i++) != '('))
        break;
      while ((i < l) && (att.charAt(i) == ' ')) i++;
      if ((i >= l) || (att.charAt(i++) != '('))
        break;

      int pcnt = 0;
      int lnb  = i;
      int s    = i;
      while (i < l) {
        char c = att.charAt(i++);
        if (c == ' ')
          continue;

        if ((pcnt == 0) && (c == ',')) {
          while (att.charAt(s) == '_')
            s++;
          while (att.charAt(lnb - 1) == '_')
            lnb--;
          if (s <= lnb)
            args.add(att.substring(s, lnb));
          s = i;
        } else if (c == '(') {
          pcnt++;
          lnb = i;
        } else if (c == ')') {
          if (pcnt == 0) {
            while (att.charAt(s) == '_')
              s++;
            while (att.charAt(lnb - 1) == '_')
              lnb--;
            if (s <= lnb)
              args.add(att.substring(s, lnb));
            break;
          }
          pcnt--;
          lnb = i;
        } else {
          lnb = i;
        }
      }
      if ((i >= l) || (att.charAt(i++) != ')'))
        break;
    }
    return args;
  }

  /**
   * Return the type modified by the attributes specified by the string.
   */
  private Type processAttributes(String attributes, Type type, Token t)
  {
    if (fatalError)
      return type;

    if (attributes == null)
      return type;

    Vector<String> att = parseAttributes(attributes);
    int    l   = att.size();
    for (int i = 0; i < l; i++) {
      String at = att.get(i);

      if (at.startsWith("aligned")) {
        int start = at.indexOf('(') + 1;
        int aln   = -1;
        if (start > 0) {
          aln = 0;
          for (int j = start; j < at.length(); j++) {
            char c = at.charAt(j);
            if (c == ')')
              break;

            if ((c < '0') && (c > '9')) {
              aln = -1;
              break;
            }

            aln = aln * 10 + (c - '0');
          }
        }
        if (aln > 0) {
          type = RefType.createAligned(type, aln);
        } else
          badAttribute(at, Msg.MSG_Unrecognized_type_attribute_s, t);
        continue;
      }

      if (at.startsWith("packed"))
        continue;

      badAttribute(at, Msg.MSG_Unrecognized_type_attribute_s, t);
    }

    return type;
  }

  private void badAttribute(String attribute, int error, Token t)
  {
    if (badAttributes == null)
      badAttributes = new HashSet<String>();

    int index = attribute.indexOf(' ');
    if (index > 0)
      attribute = attribute.substring(0, index);

    if (badAttributes.add(attribute))
      userWarning(error, attribute, t);
  }

  /**
   * Modify the declaration by the attributes specified by the string.
   */
  private void processAttributes(String attributes, Declaration decl, Token t)
  {
    if (fatalError)
      return;

    if (attributes == null)
      return;

    if (attributes.length() == 0)
      return;

    RoutineDecl    rd  = decl.returnRoutineDecl();
    Vector<String> att = parseAttributes(attributes);
    int            l   = att.size();
    for (int i = 0; i < l; i++) {
      String at = att.get(i);

      if (rd != null) {
        if (at.equals("pure")) {
          rd.setPurityLevel(RoutineDecl.PURESE + RoutineDecl.PUREARGS);
          continue;
        }

        if (at.equals("const")) {
          rd.setPurityLevel(RoutineDecl.PURE);
          continue;
        }

        if (at.startsWith("noinline")) {
          rd.setNoinlineSpecified();
          continue;
        }
      }

      if (at.equals("weak")) {
        decl.setWeak(true);
        continue;
      }

      if (at.startsWith("alias") && setAlias(at, decl))
        continue;

      if (at.startsWith("aligned")) {
        int start = at.indexOf('(') + 1;
        int aln   = -1;
        if (start > 0) {
          aln = 0;
          for (int j = start; j < at.length(); j++) {
            char c = at.charAt(j);
            if (c == ')')
              break;

            if ((c < '0') && (c > '9')) {
              aln = -1;
              break;
            }

            aln = aln * 10 + (c - '0');
          }
        }
        if (aln > 0) {
          Type type = decl.getType();
          type = RefType.createAligned(type, aln);
          decl.setType(type);
          continue;
        }
      }

      badAttribute(at, Msg.MSG_Unrecognized_declaration_attribute_s, t);
    }
  }

  private boolean setAlias(String attribute, Declaration decl)
  {
    int i = 5;
    int l = attribute.length();

    while ((i < l) && Character.isWhitespace(attribute.charAt(i))) i++;
    if ((i >= l) || (attribute.charAt(i++) != '('))
      return false;

    while ((i < l) && Character.isWhitespace(attribute.charAt(i))) i++;
    if ((i >= l) || (attribute.charAt(i++) != '"'))
      return false;

    int s = i;
    while ((i < l) && (attribute.charAt(i) != '"')) i++;
    if (i >= l)
      return false;

    int e = i++;
    while ((i < l) && Character.isWhitespace(attribute.charAt(i))) i++;
    if ((i >= l) || (attribute.charAt(i) != ')'))
      return false;

    decl.setAlias(attribute.substring(s, e));

    return true;
  }

  /**
   * Return a FormaalDecl instance for a function parameter with the
   * specified name and type.  If the name is <code>null</code>,
   * generate the name from the vaalue of the <code>counter</code>
   * argument.
   */
  private FormalDecl createFormalDecl(String name, Type type, int counter)
  {
    if (fatalError)
      return null;

    if (type.isProcedureType())
      type = PointerType.create(type);

    ArrayType at = type.getCoreType().returnArrayType();
    if (at != null)
      type = PointerType.create(at.getArraySubtype());

    if (name == null)
      name = "_A" + counter;

    FormalDecl fd = new FormalDecl(name, type);
    return fd;
  }

  /**
   * 
   */
  private FieldDecl createFieldDecl(String name, Type type, int bits, int counter, Token t)
  {
    if (name == null)
      name = "_F" + counter;

    if (bits > 0) {
      Type        ty = type.getCoreType();
      IntegerType it = ty.returnIntegerType();
      if (it == null) {
        if (!ty.isEnumerationType() && !ty.isBooleanType())
          userError(Msg.MSG_Invalid_or_missing_type, null, type, ty, t);
        type = unsigned_int_type;
        it   = unsigned_int_type;
      }

      int bs = it.bitSize();
      if (bits > bs)
        userError(Msg.MSG_Invalid_or_missing_type, null, type, ty, t);
    }

    FieldDecl fd = new FieldDecl(name, type);
    if (bits > 0)
      fd.setBits(bits, 0);
    return fd;
  }

  /**
   * Because of the change to process declaaration initializers as the
   * declaration is defined, this routine may be superfluous.  See
   * processDeclarations().
   */
  private void createDeclStatments(BlockStmt bs, Token t)
  {
    int lineno = t.getLine();
    int column = t.getColumn();

    SymtabScope              ss = cg.getSymbolTable().getCurrentScope();
    Enumeration<SymtabEntry> es = ss.orderedElements();
    while (es.hasMoreElements()) {
      SymtabEntry se   = es.nextElement();
      Declaration decl = se.getDecl();

      if (decl.isGlobal())
        continue;

      if (decl instanceof FormalDecl)
        continue;

      if (decl instanceof EnumElementDecl)
        continue;

      if (decl instanceof RoutineDecl)
        continue;

      DeclStmt ds = new DeclStmt(decl);
      int      i  = bs.addDeclStmt(ds);
      addStmtInfo(ds, lineno, column);

      VariableDecl vd = decl.returnVariableDecl();
      if ((vd != null) && (vd.residency() != Residency.MEMORY)) {
        Expression   init = vd.getInitialValue();
        Type         vt   = vd.getCoreType();
        if (vd.isConst())
          vd.setType(vd.getType().getNonConstType());
        if (init != null) {
          vd.setInitialValue(null);
          Type rt = init.getCoreType();
          ArrayType at = rt.returnArrayType();
          if (!rt.isAggregateType() && (at == null)) {
            Type type = calcAssignType(vt, rt, t);
            assert (type != null) : "This should have been caught earlier." + vd;
            init = cast(type, init, t);
          } else if (at != null) {
            if (vt.isPointerType())
              init = new AddressOp(PointerType.create(at.getElementType()), init);
          }
          if (init != null) {
            Expression expr = new AssignSimpleOp(vd.getType(), genDeclAddress(vd), cast(vd.getType(), init, t));
            Statement  set  = new EvalStmt(expr);
            bs.insertStmt(set, i);
            addStmtInfo(set, lineno, 1);
          }
        }
      }

      if (classTrace)
        System.out.println("  " + ds);
    }
  }

  /**
   * Process each symbol defined inside a block (e.g., a function).
   * Add each declaration to the symbol table.  If a variable
   * declaration has an initializer, set up an assignement statement
   * to initialize it.
   */
  private void processDeclarations(Vector<Declaration> vars, BlockStmt block, Token t)
  {
    for (int i = 0; i < vars.size(); i++) {
      Declaration decl = vars.elementAt(i);
      if (decl == null)
        continue;

      cg.addSymbol(decl);

      if (decl.isGlobal())
        continue;

      if (decl.residency() == Residency.MEMORY)
        continue;

      if (!decl.isVariableDecl())
        continue;

      VariableDecl vd   = (VariableDecl) decl;
      Expression   init = vd.getValue();
      if (init == null)
        continue;

      vd.setValue(null);

      Type vt = vd.getCoreType();
      Type rt = init.getCoreType();

      ArrayType at = rt.returnArrayType();
      if (!rt.isAggregateType() && (at == null)) {
        Type type = calcAssignType(vt, rt, t);
        assert (type != null) : "This should have been caught earlier." + decl;
        init = cast(type, init, t);
      } else if (at != null) {
        if (vt.isPointerType())
          init = new AddressOp(PointerType.create(at.getElementType()), init);
      }

      if (init == null)
        continue;

      Expression expr = new AssignSimpleOp(decl.getType(), genDeclAddress(decl), cast(decl.getType(), init, t));
      Statement  set  = new EvalStmt(expr);

      block.addStmt(set);
      addStmtInfo(set, t.getLine(), 1);
    }
  }

  /**
   * Return the IntLiteral instance required to represent the value.
   * If the user specifies <code>0x89abcdef</code>, is it an unsigned
   * 32-bit value or a signed 32-bit value?  Other ambiguities exist
   * as well.  For example, did the user want <code>0x987654321</code>
   * to be of type <code>int</code> or of type <code>long long</code>?
   * See Section 6.4.4.1, paragraph 5 of the ISO/IEC 9899-1999 (E) C
   * standard.
   * @param value is always a positive value
   * @param hex is true if '0x' notation was used.
   * @param type is the type specified by the constant suffix (e.g., UL, LL, etc.)
   */
  private Literal buildIntLiteral(long value, boolean hex, IntegerType type)
  {
    if (type == int_type) {
      if (hex) {
        if ((value >>> (int_type_size - 1)) == 0)
          type = int_type;

        else if ((value >>> int_type_size) == 0)
          type = unsigned_int_type;

        else if ((value >>> (long_type_size - 1)) == 0)
          type = long_type;

        else if ((long_type_size < 64) && (value >>> long_type_size) == 0)
          type = unsigned_long_type;

        else if ((value >>> (long_long_type_size - 1)) == 0)
          type = long_long_type;
        else
          type = unsigned_long_long_type;
      } else if ((value >>> (int_type_size - 1)) == 0)
        type = int_type;

      else if ((value >>> (long_type_size - 1)) == 0)
        type = long_type;
      else
        type = long_long_type;
    } else if (type == unsigned_int_type) {
      if ((value >>> unsigned_int_type_size) == 0)
        type = unsigned_int_type;

      else if ((unsigned_long_type_size < 64) && (value >>> unsigned_long_type_size) == 0)
        type = unsigned_long_type;
      else
        type = unsigned_long_long_type;
    } else if (type == long_type) {
      if (hex) {
        if ((value >>> (long_type_size - 1)) == 0)
          type = long_type;

        else if ((long_type_size < 64) && (value >>> long_type_size) == 0)
          type = unsigned_long_type;

        else if ((value >>> (long_long_type_size - 1)) == 0)
          type = long_long_type;
        else
          type = unsigned_long_long_type;
      } else if ((value >>> (long_type_size - 1)) == 0)
        type = long_type;
      else
        type = long_long_type;
    } else if (type == unsigned_long_type) {
      if ((unsigned_long_type_size < 64) && (value >>> unsigned_long_type_size) == 0)
        type = unsigned_long_type;
      else
        type = unsigned_long_long_type;
    } else if (type == long_long_type) {
      if (hex) {
        if ((value >>> (long_long_type_size - 1)) == 0)
          type = long_long_type;
        else
          type = unsigned_long_long_type;
      } else
        type = long_long_type;
    } else
      type = unsigned_long_long_type;
    return inConstant ? new IntLiteral(type, value) : LiteralMap.put(value, type);
  }

  private Literal buildFloatLiteral(double value, RealType type)
  {
    return inConstant ? new FloatLiteral(type, value) : LiteralMap.put(value, type);
  }

  private Expression convertLiterals(Expression expr)
  {
     if (fatalError)
       return errExp;

    if ((expr instanceof StringLiteral) ||
        (expr instanceof IntArrayLiteral) ||
        (expr instanceof FloatArrayLiteral)) {
      VariableDecl vd = createVarFromExp(expr);
      expr = buildVariableRef(vd);
    }

    return expr;
  }

  private Expression genCall(String             name,
                             ProcedureType      pt,
                             Expression         proc,
                             Vector<Expression> args,
                             Token              t)
  {
    if (fatalError)
      return errExp;

    int l = 0;
    int m = pt.numFormals();
    if (args != null)
      l = args.size();

    if ((l != m) &&
        (m > 0) &&
        ((l < (m - 1)) || !(pt.getFormal(m - 1) instanceof UnknownFormals)))
      userWarning(Msg.MSG_Improper_call_to_s, name, t);

    int i = 0;
    for (; i < l && i < m; i++) {
      Expression arg = convertLiterals(args.get(i));
      FormalDecl fd  = pt.getFormal(i);
      if (fd instanceof UnknownFormals)
        break;

      Type type = calcArgType(fd.getType(), arg.getType());
      if (type == null)
        return userExprError(Msg.MSG_Incompatible_function_argument_for_s, fd.getName(), arg, t);

      args.set(i, cast(type, arg, t));
    }

    for (; i < l; i++) {
      Expression arg = convertLiterals(args.get(i));
      Type       ty  = arg.getCoreType();
      if (ty.isRealType() && !ty.isComplexType())
        arg = cast(double_type, arg, t);
      else if (isIntegerType(ty)) {
        Type        aty = ty.isSigned() ? int_type : unsigned_int_type;
        IntegerType ity = ty.returnIntegerType();
        if (ity != null) {
          if (ity.bitSize() <= int_type_size)
            aty = ty.isSigned() ? int_type : unsigned_int_type;
          else if (ity.bitSize() <= long_type_size)
            aty = ty.isSigned() ? long_type : unsigned_long_type;
          else
            aty = ty.isSigned() ? long_long_type : unsigned_long_long_type;
        }
        arg = cast(aty, arg, t);
      }
      args.set(i, arg);
    }

    return new CallFunctionOp(pt.getReturnType(), proc, args);
  }

  /**
   * Determine if the compound statement ends with a return
   * statement.  If not, add one.
   * @param name is the function name
   * @param pt is the procedure's signature
   * @param stmt is the compound statement
   */
  private void needsReturn(String name, ProcedureType pt, BlockStmt stmt, Token t)
  {
    Type rettype = pt.getReturnType().getCoreType();
    if (rettype == void_type)
      return;

    if (stmt.hasReturnStmt())
      return;

    userWarning(Msg.MSG_Missing_return_statement_s, name, t);

    Expression retexp  = null;

    if (rettype.isPointerType())
      retexp = LiteralMap.put(0, rettype);
    else if (isIntegerType(rettype))
      retexp = LiteralMap.put(0, rettype);
    else if (rettype.isRealType())
      retexp = LiteralMap.put(0.0, rettype);
    else if (rettype != void_type) {
      VariableDecl decl = genTemp(rettype);
      stmt.addDeclStmt(new DeclStmt(decl));
      retexp = new IdValueOp(decl);
    }
    stmt.addStmt(new ReturnStmt(retexp));
  }

  private Expression buildCall(String name, Vector<Expression> args, Token t)
  {
     if (fatalError)
       return errExp;

    Expression call = null;
    if (name.startsWith("va_") || name.startsWith("__va_"))
      call = builtin(name, args);
    else if (name.startsWith("__builtin")) {
      call = builtin(name, args);
      if (call == null)
        name = "_scale" + name.substring(9);
    }

    if ("setjmp".equals(name) || "_scale_setjmp".equals(name))
      currentFunction.setUsesSetjmp();

    if (call != null)
      return call;

    Declaration decl = lookupVarDecl(name);
    if (decl == null) {
      int    n   = preKnownFtns.length;
      String sig = "i";
      for (int i = 0; i < n; i += 2) {
        if (name.equals(preKnownFtns[i])) {
          sig = preKnownFtns[i + 1];
          break;
        }
      }
      decl = defPreKnownFtn(name, sig);
      Expression expr0 = genDeclAddress(decl);
      decl.setReferenced();
      return genCall(name, (ProcedureType) decl.getType(), expr0, args, t);
    }

    RoutineDecl rd = decl.returnRoutineDecl();
    if (rd != null) {
      Expression    expr0 = genDeclAddress(rd);
      ProcedureType pt    = rd.getSignature();
      rd.setReferenced();
      return genCall(name, pt, expr0, args, t);
    }

    VariableDecl vd = decl.returnVariableDecl();
    if (vd != null) {
      Type type = vd.getCoreType();
      if (type.isPointerType()) {
        ProcedureType pt = type.getPointedToCore().returnProcedureType();
        if (pt != null) {
          Expression expr0 = new IdValueOp(vd);
          vd.setReferenced();
          return genCall(name, pt, expr0, args, t);
        }
      }
    }

    return userExprError(Msg.MSG_Improper_call_to_s, name, null, t);
  }

  private Expression buildCall(Expression ftn, Vector<Expression> args, Token t)
  {
    if (fatalError)
      return errExp;

    Type type = ftn.getPointedToCore();

    if (!type.isProcedureType())
      return userExprError(Msg.MSG_Invalid_function_call, null, ftn, t);

    Type rt = ((ProcedureType) type).getReturnType();

    return genCall("??", (ProcedureType) type, ftn, args, t);
  }

  private Expression buildFieldRef(boolean addr, String fieldName, Expression struct, Token t)
  {
     if (fatalError)
       return errExp;

    if (!addr)
      struct = makeLValue(struct);

    // If an array of structs is used, the first element of the array is
    // referenced.

    Type pst = struct.getCoreType();
    if (!pst.isPointerType())
      return userExprError(Msg.MSG_Invalid_type_operand_to_s, (addr ? "->" : "."), struct, t);

    Type      st = struct.getType().getCoreType().getPointedTo();
    ArrayType at = st.getCoreType().returnArrayType();
    if (at != null)
      st = at.getElementType();

    if (!st.isAggregateType())
      return userExprError(Msg.MSG_Not_a_struct, null, struct, st.getCoreType(), t);

    int fdi = lookupField(fieldName, st);
    if (fdi == 0)
      return userExprError(Msg.MSG_Field_s_not_found, fieldName, struct, t);

    int i = 0;
    while (i < fdi - 1) {
      FieldDecl fd = fieldSequence[i++];
      struct = new SelectIndirectOp(struct, fd);
    }

    FieldDecl fd   = fieldSequence[i];
    Type      ft   = fd.getType().getNonConstType();
    Type      tt   = ft.getCoreType();
    int       bits = fd.getBits();

    if (tt.isEnumerationType() || tt.isBooleanType())
      tt = int_type;

    if (tt.isArrayType()) {
      Expression exp = new SelectIndirectOp(struct, fd);
      return exp;
    }

    if (tt.isProcedureType())
      return new SelectIndirectOp(struct, fd);

    Expression sel  = new SelectOp(struct, fd);
    Type       type = ft;
    if (allowGNUExtensions && (bits != 0)) {
      if (bits < int_type_size)
        type = int_type;
      else if (bits == int_type_size)
        type = type.isSigned() ? int_type : unsigned_int_type;
      else if (bits < long_type_size)
        type = long_type;
      else if (bits == long_type_size)
        type = type.isSigned() ? long_type : unsigned_long_type;
      sel = cast(type, sel, t);
    }

    return sel;
  }

  private Expression buildAddressOf(Expression ra, Token t)
  {
    if (fatalError)
      return errExp;

    if (ra instanceof IdValueOp) {
      Declaration decl = ((IdValueOp) ra).getDecl();
      decl.setReferenced();
      decl.setAddressTaken();
      return genDeclAddress(decl);
    }

    if (ra instanceof SubscriptValueOp) {
      Expression exp = ((SubscriptValueOp) ra).makeLValue();
      return exp;
    }

    if (ra instanceof DereferenceOp)
      return ((DereferenceOp) ra).getExpr();

    if (ra instanceof SelectOp) {
      SelectOp   sop = (SelectOp) ra;
      Expression fld = sop.getStruct();

      // If the address is taken of a field of a struct variable, it
      // is taken of the entire variable.

      if (fld instanceof IdReferenceOp)
        ((IdReferenceOp) fld).getDecl().setAddressTaken();

      return makeLValue(ra);
    }

    if (ra instanceof IdAddressOp)
      return ra;

    if (ra instanceof Literal) {
      if (strictANSIC && !ra.getType().isArrayType())
        return userExprError(Msg.MSG_Invalid_lvalue, null, ra, t);

      VariableDecl decl = new VariableDecl("_V" + varCounter++, ra.getType().getNonConstType(), ra);
      decl.setAddressTaken();
      decl.setReferenced();
      cg.addSymbol(decl);
      return buildVariableAddressRef(decl);
    }

    if (ra instanceof AddressOp)
      return ra;

    if (ra instanceof SubscriptAddressOp)
      return ra;

    if (ra instanceof SelectIndirectOp)
      return ra;

    if (ra instanceof TypeConversionOp) {
      TypeConversionOp top = (TypeConversionOp) ra;
      if (top.getConversion() == CastMode.CAST) {
        Type       ty  = PointerType.create(top.getType());
        Expression arg = buildAddressOf(top.getExpr(), t);
        return new TypeConversionOp(ty, arg, CastMode.CAST);
      }
    }

    return new AddressOp(PointerType.create(ra.getType()), ra);
  }

  private Expression buildDereferenceOp(Expression ra, Token t)
  {
     if (fatalError)
       return errExp;

    Type tt = ra.getCoreType();
    if (!tt.isPointerType())
      return userExprError(Msg.MSG_Dereference_of_non_address, null, ra, t);

    Type ptype = tt.getPointedTo();
    if (ptype.isProcedureType())
      return ra;

    ArrayType pat = ptype.getCoreType().returnArrayType();
    if (pat != null)
      return new TypeConversionOp(PointerType.create(pat.getArraySubtype()), ra, CastMode.CAST);

    if (ra instanceof SubscriptAddressOp) {
      SubscriptAddressOp sop  = (SubscriptAddressOp) ra;
      int                n    = sop.numSubscripts();
      Vector<Expression> subs = new Vector<Expression>(n);

      for (int i = 0; i < n; i++)
        subs.add(sop.getSubscript(i));

      ArrayType at = ptype.getCoreType().returnArrayType();
      if (at != null) {
        int       m    = n + at.getRank();
        ptype = at.getElementType();
        for (int i = n; i < m; i++)
          subs.add(zero);
      }

      Expression exp = new SubscriptValueOp(ptype, sop.getArray(), subs);
      return exp;
    }

    if (ra instanceof IdAddressOp) {
      IdAddressOp ia   = (IdAddressOp) ra;
      Declaration decl = ia.getDecl();

      decl.setReferenced();

      ArrayType at = decl.getType().getCoreType().returnArrayType();
      if (at == null)
        return new IdValueOp(decl);

      int                n    = at.getRank();
      Vector<Expression> subs = new Vector<Expression>(n);

      for (int i = 0; i < n; i++)
        subs.add(zero);

      ptype = at.getElementType();

      Expression exp = new SubscriptValueOp(ptype, ra, subs);
      return exp;
    }

    if (ra instanceof SelectIndirectOp) {
      SelectIndirectOp sio    = (SelectIndirectOp) ra;
      Expression       struct = sio.getStruct();
      FieldDecl        field  = sio.getField();

      ArrayType at = field.getCoreType().returnArrayType();
      if (at == null)
        return new SelectOp(struct, field);

      int                n    = at.getRank();
      Vector<Expression> subs = new Vector<Expression>(n);

      for (int i = 0; i < n; i++)
        subs.add(zero);

      return new SubscriptValueOp(at.getElementType(), sio, subs);
    }

    if (ra instanceof AddressOp)
      return ((AddressOp) ra).getExpr();

    return new DereferenceOp(ra);
  }

  private static final int RIGHT = 0;
  private static final int LEFT  = 1;
  private static final int BOTH  = 2;

  // ??    =      *=     /=     %=    +=      -=     &=     |=     ^=     <<=    >>=
  private static final boolean[] addressAllowedInAssignment = {
    false, true,  false, false, false, true,  true,  false, false, false, false, false};
  private static final boolean[] integerOnly = {
    false, false, false, false, true,  false, false, true,  true,  true,  true,  true};
  private static final byte[] calcRType = {
    RIGHT, RIGHT, BOTH,  BOTH,  BOTH,  BOTH,  BOTH,  BOTH,  BOTH,  BOTH,  LEFT,  LEFT};

  private Expression buildAssignment(int op, Expression la, Expression ra, Token t)
  {
    assert ((addressAllowedInAssignment.length == 12) &&
            (integerOnly.length == 12) &&
            (calcRType.length == 12));

    if (fatalError)
      return errExp;

    Expression lhs = makeLValue(la);

    assert lhs.getCoreType().isPointerType() : "This should have been checked before now.";

    Type ratype = ra.getType();
    Type latype = la.getType();

    if (ratype.isPointerType()) {
      Type      ty = ratype.getPointedToCore();
      ArrayType at = ty.returnArrayType();
      if (at != null)
        ratype = PointerType.create(at.getElementType());
    }

    ArrayType lat = latype.getCoreType().returnArrayType();
    if (lat != null)
      latype = PointerType.create(lat.getElementType());

    ArrayType rat = ratype.getCoreType().returnArrayType();
    if (rat != null)
      ratype = PointerType.create(rat.getElementType());

    if (!lhs.getCoreType().isPointerType())
      return userExprError(Msg.MSG_Invalid_lvalue, null, la, t);

    boolean intOnly        = integerOnly[op];
    boolean addressAllowed = addressAllowedInAssignment[op];

    if (intOnly && (!isIntegerType(latype) || !isIntegerType(ratype)))
      return userExprError(Msg.MSG_Operands_to_s_not_compatible, t.getText(), la, ra, t);

    if (!addressAllowed && (latype.isPointerType() || ratype.isPointerType()))
      return userExprError(Msg.MSG_Operands_to_s_not_compatible, t.getText(), la, ra, t);

    Type rtype = null; // Type of value of the assignment expression.
    switch (calcRType[op]) {
    case RIGHT: rtype = ratype; break;
    case LEFT:  rtype = latype; break;
    case BOTH:  rtype = calcBinaryType(latype, ratype); break;
    }

    if ((rtype == null) || latype.isVoidType())
      return userExprError(Msg.MSG_Operands_to_s_not_compatible, t.getText(), la, ra, t);

    Expression rhs = convertLiterals(ra);

    Type t1 = lhs.getCoreType().getPointedTo();
    Type t2 = ratype.getCoreType();

    boolean good = true;

    if (t1.isPointerType()) {
      if (op == 1) {
        if (!t2.isPointerType()) {
          if (isIntegerType(t2)) {
            boolean w = !((rhs instanceof IntLiteral) && ((IntLiteral) rhs).isZero());
            rhs = cast(t1, rhs, t);
            if (w)
              userWarning(Msg.MSG_Assignment_of_integer_to_pointer_without_a_cast, null, t);
          } else
            good = false;
        }
      } else if (!isIntegerType(t2) && !t2.isPointerType())
        good = false;
    } else if (t2.isPointerType()) {
      if (t1.isProcedureType() && t2.getPointedTo().isProcedureType())
        t1 = voidp_type;
      else
        good = false;
    }

    if (t1.isAggregateType()) {
      if (!t2.isAggregateType() || !t1.getCoreType().equivalent(t2.getCoreType()) || (op != 1))
        good = false;
    }

    if (t1.isNumericType() && t2.isNumericType()) {
      Type type = calcAssignType(t1, t2, t);
      if (type == null)
        good = false;
    }

    if (!good) {
      if (fullError) {
        System.out.println("** t1 " + t1);
        System.out.println("   t2 " + t2);
      }
      return userExprError(Msg.MSG_Operands_to_s_not_compatible, t.getText(), lhs, rhs, t);
    }

    switch (op) {
    case  1: return new AssignSimpleOp(latype, lhs, rhs);
    case  2: return new MultiplicationAssignmentOp(latype, rtype, lhs, rhs);
    case  3: return new DivisionAssignmentOp(latype, rtype, lhs, rhs);
    case  4: return new RemainderAssignmentOp(latype, rtype, lhs, rhs);
    case  5: return new AdditionAssignmentOp(latype, rtype, lhs, rhs);
    case  6: return new SubtractionAssignmentOp(latype, rtype, lhs, rhs);
    case  7: return new BitAndAssignmentOp(latype, rtype, lhs, rhs);
    case  8: return new BitOrAssignmentOp(latype, rtype, lhs, rhs);
    case  9: return new BitXorAssignmentOp(latype, rtype, lhs, rhs);
    case 10: return new BitShiftAssignmentOp(latype, rtype, lhs, rhs, ShiftMode.Left);
    case 11: return new BitShiftAssignmentOp(latype, rtype, lhs, rhs, t1.isSigned() ? ShiftMode.SignedRight : ShiftMode.UnsignedRight);
    }

    return errExp;
  }

  /**
   * Return a new string with escape character sequences replaced by
   * the actual character.
   */
  private String processString(String src, int mask)
  {
    char[] array = src.toCharArray();
    int k = 0;
    int l = array.length;
    for (int i = 0; i < l; i++) {
      char c = (char) (array[i] & mask);
      if ((c == '\\') && (i < l - 1)) {
        char c2 = array[i + 1];
        switch (c2) {
        case '\'':
        case '?':
        case '\\':
        case '"':  c = c2;     i++; break;
        case 'a':  c = '\007'; i++; break;
        case 'b':  c = '\b';   i++; break;
        case 'e':  c = '\033'; i++; break;
        case 'f':  c = '\f';   i++; break;
        case 'n':  c = '\n';   i++; break;
        case 'r':  c = '\r';   i++; break;
        case 't':  c = '\t';   i++; break;
        case 'v':  c = '\013'; i++; break;
        }
      }
      array[k++] = c;
    }
    return new String(array, 0, k);
  }

  private boolean isArrayVariable(Expression array)
  {
    if (array instanceof IdAddressOp)
      return true;
    if (array instanceof IdValueOp)
      return false;
    if (array instanceof SubscriptOp)
      return false;
    if (array instanceof ExpressionIfOp)
      return isArrayVariable(((ExpressionIfOp) array).getExpr2());
    if (array instanceof SelectIndirectOp)
      return ((SelectIndirectOp) array).getField().getType().isArrayType();
    if ((array instanceof DereferenceOp) && array.getType().isArrayType())
      return true;
    return false;
  }

  private Expression buildSubscriptAddress(Expression array, Expression index, Token t)
  {
    if (fatalError)
      return errExp;

    Type pt = array.getCoreType();
    if (!pt.isPointerType() && index.getCoreType().isPointerType()) {
      Expression tmp = array;
      array = index;
      index = tmp;
      pt = array.getCoreType();
    }

    if ((array instanceof Literal) && !(array instanceof AddressLiteral)) {
      VariableDecl decl = createVarFromExp(array);
      array = buildVariableRef(decl);
      pt = array.getCoreType();
    }

    if (!pt.isPointerType())
      return userExprError(Msg.MSG_Invalid_subscript_expression, null, array, t);

    if (!isIntegerType(index.getCoreType()))
      return userExprError(Msg.MSG_Invalid_type_operand_to_s, "[]", index, t);

    boolean addressNeeded = false;
    Vector<Expression> subs          = null;
    Type    et            = pt.getPointedTo();
    Type    etc           = et.getCoreType();

    if (array instanceof SubscriptAddressOp) {
      SubscriptAddressOp sop  = (SubscriptAddressOp) array;
      int                n    = sop.numSubscripts();
      Expression         sarr = sop.getArray();
      ArrayType          at2  = sarr.getPointedToCore().returnArrayType();

      if ((at2 != null) && (sop.numSubscripts() <= at2.getRank())) {
        array = sarr;

        subs = new Vector<Expression>(n + 1);

        for (int i = 0; i < n; i++)
          subs.add(sop.getSubscript(i));

        subs.add(index);
      } else {
        subs = new Vector<Expression>(n);
        for (int i = 0; i < n - 1; i++)
          subs.add(sop.getSubscript(i));
        Expression last = sop.getSubscript(n - 1);
        subs.add(new AdditionOp(last.getType(), last, index));
        array = sop.getArray();
      }
    } else {
      subs = new Vector<Expression>(1);
      subs.add(index);
    }

    array = new SubscriptAddressOp(PointerType.create(et), array, subs);

    return array;
  }

  private Expression buildSubscriptValue(Expression array, Expression index, Token t)
  {
    if (fatalError)
      return errExp;

    Type pt = array.getCoreType();
    if (!pt.isPointerType() && index.getCoreType().isPointerType()) {
      Expression tmp = array;
      array = index;
      index = tmp;
      pt = array.getCoreType();
    }

    if ((array instanceof Literal) && !(array instanceof AddressLiteral)) {
      VariableDecl decl = createVarFromExp(array);
      array = buildVariableRef(decl);
      pt = array.getCoreType();
    }

    if (!pt.isPointerType())
      return userExprError(Msg.MSG_Invalid_subscript_expression, null, array, t);

    if (!isIntegerType(index.getCoreType()))
      return userExprError(Msg.MSG_Invalid_type_operand_to_s, "[]", index, t);

    boolean addressNeeded = false;
    Vector<Expression> subs          = null;
    Type    et            = pt.getPointedTo();
    Type    etc           = et.getCoreType();
    if (!etc.isAtomicType() && !etc.isCompositeType())
      return userExprError(Msg.MSG_Invalid_type_operand_to_s, "[]", array, t);

    ArrayType at = etc.returnArrayType();
    if (at != null) {
      et = at.getArraySubtype();
      et = PointerType.create(et);
      addressNeeded = true;
    }

    if (array instanceof SubscriptAddressOp) {
      SubscriptAddressOp sop  = (SubscriptAddressOp) array;
      int                n    = sop.numSubscripts();
      Expression         sarr = sop.getArray();
      ArrayType          at2  = sarr.getPointedToCore().returnArrayType();

      if ((at2 != null) && (sop.numSubscripts() <= at2.getRank())) {
        array = sarr;

        subs = new Vector<Expression>(n + 1);

        for (int i = 0; i < n; i++)
          subs.add(sop.getSubscript(i));

        subs.add(index);
      } else {
        subs = new Vector<Expression>(n);
        for (int i = 0; i < n - 1; i++)
          subs.add(sop.getSubscript(i));
        Expression last = sop.getSubscript(n - 1);
        subs.add(new AdditionOp(last.getType(), last, index));
        array = sop.getArray();
      }
    } else {
      subs = new Vector<Expression>(1);
      subs.add(index);
    }

    boolean dereference = true;
    if (et.isArrayType())
      dereference = false;

    if (!dereference)
      array = new SubscriptAddressOp(PointerType.create(et), array, subs);
    else if (addressNeeded) {
      assert et.isPointerType();
      array = new SubscriptAddressOp(et, array, subs);
    } else
      array = new SubscriptValueOp(et, array, subs);

    return array;
  }

  private ProcedureType makeProcedureType(Type returnType, Vector<? extends Object> f)
  {
    if (fatalError)
      return null;

    int nf = 0;
    if (f != null)
      nf = f.size();

    Vector<FormalDecl> formals = new Vector<FormalDecl>(nf);

    if (nf > 0) {
      for (int i = 0; i < nf; i++) {
        Object o = f.get(i);
        if (o instanceof String) {
          FormalDecl fd = createFormalDecl((String) o, int_type, 0);
          formals.add(fd);
        } else
          formals.add((FormalDecl) o);
      }

      FormalDecl fd = formals.get(0);
      if (fd.getCoreType().isVoidType())
        formals.remove(0);
    }

    return ProcedureType.create(returnType, formals, null);
  }

  private void processTopDecls(Vector<Declaration> v, int sc, Token t)
  {
    if (fatalError)
      return;

    for (int i = 0; i < v.size(); i++) {
      Declaration decl = v.elementAt(i);
      if (decl == null)
        continue;

      cg.addSymbol(decl);

      VariableDecl vd = decl.returnVariableDecl();
      if (vd == null)
        continue;

      if (sc == cStatic)
        vd.setVisibility(Visibility.FILE);

      else if (sc != cExtern) {
        vd.setVisibility(Visibility.GLOBAL);
        vd.setReferenced();
      }

      Expression   init = vd.getInitialValue();
      Type         vt   = vd.getCoreType();
      if (init != null) {
        Expression rhs = init;
        Type       rt  = rhs.getCoreType();
        if ((rhs instanceof StringLiteral) && (!vt.isArrayType())) {
          vd.setInitialValue(stringConstant((StringLiteral) rhs));
        } else if (!rt.isAggregateType() && !rt.isArrayType()) {
          Type ty = calcAssignType(vt, rt, t);
          assert (ty != null) : "This should have been caught earlier.";
          rhs = cast(ty, rhs, t);
          vd.setInitialValue(rhs);
        }
      }
      if (classTrace) {
        System.out.println("  " + vd);
      }
    }
  }

  /**
   * Return true if the specified name represents a user-defined type
   * in the current scope.
   */
  private boolean isTypedef(String name)
  {
    return lookupTypedef(name) != null;
  }

  /**
   * Return the user-defined type with the specified name, or
   * <code>null</code> in the current scope.
   */
  private TypeName lookupTypedef(String name)
  {
    Vector<SymtabEntry> ents = cg.getSymbolTable().lookupSymbol(name);
    if (ents == null)
      return null;
    int l = ents.size();
    if (l < 1)
      return null;

    int i = 0;
    while (i < l) {
      SymtabEntry s    = ents.get(i++);
      Declaration decl = s.getDecl();
      if (decl instanceof TypeName)
        return (TypeName) decl;

      if (decl instanceof TypeDecl)
        continue;
      break;
    }
    return null;
  }

  private Expression buildAddExpr(Expression la, Expression ra, Token t)
  {
    if (fatalError)
      return errExp;

    la = convertLiterals(la);
    ra = convertLiterals(ra);

    Type t1 = la.getType();
    Type t2 = ra.getType();

    if (t1.isPointerType() && isIntegerType(t2))
      return buildSubscriptAddress(la, ra, t);

    if (t2.isPointerType() && isIntegerType(t1))
      return buildSubscriptAddress(ra, la, t);

    Type type = calcBinaryType(t1, t2);
    if (type != null)
      return new AdditionOp(type, cast(type, la, t), cast(type, ra, t));

    if (classTrace) {
      System.out.println("** ad " + la);
      System.out.println("      " + t1.getCoreType());
      System.out.println("      " + ra);
      System.out.println("      " + t2.getCoreType());
    }

    return userExprError(Msg.MSG_Operands_to_s_not_compatible, t.getText(), la, ra, t);
  }

  private Expression buildSubtractExpr(Expression la, Expression ra, Token t)
  {
    if (fatalError)
      return errExp;

    la = convertLiterals(la);
    ra = convertLiterals(ra);
    Type t1 = la.getType();
    Type t2 = ra.getType();

    if (t1.isPointerType()) {
      if (t2.isPointerType())
        return new SubtractionOp(ptrdiff_t_type, la, ra);
      if (isIntegerType(t2))
        return buildSubscriptAddress(la, new NegativeOp(ptrdiff_t_type,ra), t);
    } else if (!t2.isPointerType()) {
      Type type = calcBinaryType(t1, t2);
      if (type != null) {
        la   = cast(type, la, t);
        ra = cast(type, ra, t);
        return new SubtractionOp(type, la, ra);
      }
    }

    return userExprError(Msg.MSG_Operands_to_s_not_compatible, t.getText(), la, ra, t);
  }

  private Expression buildShiftExpr(ShiftMode shift, Expression la, Expression ra, Token t)
  {
    if (fatalError)
      return errExp;

    Type t1 = la.getType();
    Type t2 = ra.getType();
    if (!isIntegerType(t1) || !isIntegerType(t2))
      return userExprError(Msg.MSG_Operands_to_s_not_integer_types, t.getText(), la, ra, t);

    IntegerType type = t1.getCoreType().returnIntegerType();
    if (type == null)
      t1 = int_type;
    else {
       if (type.bitSize() < int_type_size)
        t1 = int_type;
    }

    if (!t1.isSigned() && (shift == ShiftMode.SignedRight))
      shift = ShiftMode.UnsignedRight;

    return new BitShiftOp(t1, la, ra, shift);
  }

  private Expression buildCompare(int sw, Expression la, Expression ra, Token t)
  {
    if (fatalError)
      return errExp;

    Type t1  = la.getCoreType();
    Type t2  = ra.getCoreType();
    if (t1.isPointerType()) {
      if (t2.isPointerType())
        ;
      else if (!isIntegerType(t2))
        sw = 0;
    } else {
      Type type = calcBinaryType(t1, t2);
      if (type != null) {
        la   = cast(type, la, t);
        ra = cast(type, ra, t);
      } else
        sw = 0;
    }
    switch (sw) {
    case 1: return new LessOp(bool_type, la, ra);
    case 2: return new GreaterOp(bool_type, la, ra);
    case 3: return new LessEqualOp(bool_type, la, ra);
    case 4: return new GreaterEqualOp(bool_type, la, ra);
    }
    return userExprError(Msg.MSG_Operands_to_s_not_compatible, t.getText(), la, ra, t);
  }

  private Expression buildEqualCompare(boolean equals, Expression la, Expression ra, Token t)
  {
    if (fatalError)
      return errExp;

    boolean bad = true;
    Type    t1  = la.getType();
    Type    t2  = ra.getType();
    if (t1.isPointerType()) {
      if (t2.isPointerType()) {
        bad = false;
      } else if (isIntegerType(t2)) {
        bad = false;
        ra = cast(t1, ra, t);
      }
    } else if (t2.isPointerType()) {
      if (isIntegerType(t1)) {
        bad = false;
        ra = cast(t2, ra, t);
      }
    } else {
      Type type = calcBinaryType(t1, t2);
      bad = (type == null);
      if (!bad) {
        la = cast(type, la, t);
        ra = cast(type, ra, t);
      }
    }

    if (bad) {
      if (classTrace || fullError) {
        System.out.println("** eq " + la);
        System.out.println("      " + la.getCoreType());
        System.out.println("      " + ra);
        System.out.println("      " + ra.getCoreType());
      }
      return userExprError(Msg.MSG_Operands_to_s_not_compatible, t.getText(), la, ra, t);
    }

    if (equals)
      return new EqualityOp(bool_type, la, ra);

    return new NotEqualOp(bool_type, la, ra);
  }

  private Expression buildBitExpr(int sw, Expression la, Expression ra, Token t)
  {
    if (fatalError)
      return errExp;

    Type t1  = la.getCoreType();
    Type t2  = ra.getCoreType();
    if (!((t1.isPointerType() && isIntegerType(t2)) ||
          (isIntegerType(t1) && t2.isPointerType())) &&
        (!isIntegerType(t1) || !isIntegerType(t2)))
      return userExprError(Msg.MSG_Operands_to_s_not_integer_types, t.getText(), la, ra, t);

    Type type = calcBinaryType(t1, t2);
    if (type == null)
      return userExprError(Msg.MSG_Operands_to_s_not_compatible, t.getText(), la, ra, t);

    if (sw == 0)
      return new BitAndOp(type, cast(type, la, t), cast(type, ra, t));
    if (sw == 1)
      return new BitOrOp(type, cast(type, la, t), cast(type, ra, t));
    if (sw == 2)
      return new BitXorOp(type, cast(type, la, t), cast(type, ra, t));

    throw new scale.common.InternalError("Invalid bit operation.");
  }

  private Expression buildConditional(int sw, Expression la, Expression ra, Token t)
  {
    if (fatalError)
      return errExp;

    la   = trueOrFalse(la, t);
    ra = trueOrFalse(ra, t);

    if (sw == 0)
      return new AndConditionalOp(bool_type, la, ra);
    if (sw == 1)
      return new OrConditionalOp(bool_type, la, ra);
    throw new scale.common.InternalError("Invalid conditional operation.");
  }

  private Expression buildExpressionIf(Expression la, Expression ma, Expression ra, Token t)
  {
    if (fatalError)
      return errExp;

    Literal lit = la.getConstantValue();

    if (lit.isZero())
      return ra;
    else if (lit.isOne())
      return ma;

    ma = convertLiterals(ma);
    ra = convertLiterals(ra);
    Type type2 = ma.getType();
    Type type3 = ra.getType();
    Type type  = calcCommonType(type2, type3, t);
    if (type == null)
      return userExprError(Msg.MSG_Operands_to_s_not_compatible, "?:", ma, ra, t);
    return new ExpressionIfOp(type, trueOrFalse(la, t), cast(type, ma, t), cast(type, ra, t));
  }

  private Expression buildVariableRef(String name, Token t)
  {
    if (fatalError)
      return errExp;

    Declaration decl = lookupVarDecl(name);
    if (decl == null) {
      if ("__func__".equals(name)) { // Section 6.4.2.2 of ISO/IEC 9899-1999 (E)
        String  ftn   = currentFunction.getName() + "\0";
        Type    type  = RefType.create(makeCharArrayType(ftn, char_type), RefAttr.Const);
        Literal value = LiteralMap.put(ftn, type);
        decl = new VariableDecl("__func__", type, value);
        cg.addSymbol(decl);
        decl.setResidency(Residency.MEMORY);
        decl.setReferenced();
      } else
        return userExprError(Msg.MSG_Not_defined_s, name, null, t);
    }

    return buildVariableRef(decl);
  }

  private Expression buildVariableRef(Declaration decl)
  {
    if (fatalError)
      return errExp;

    decl.setReferenced();
    Type type = decl.getType();

    ArrayType at = type.getCoreType().returnArrayType();
    if (at != null) {
      int        rank = at.getRank();
      Expression load = genDeclAddress(decl);

      decl.setAddressTaken();

      return load;
    } 

    if (type.isProcedureType()) {
      decl.setAddressTaken();
      return genDeclAddress(decl);
    }

    if (decl instanceof EnumElementDecl)
      return ((EnumElementDecl) decl).getValue();

    return new IdValueOp(decl);
  }

  private Expression buildVariableAddressRef(Declaration decl)
  {
    if (fatalError)
      return errExp;

    decl.setReferenced();
    Type type = decl.getType();

    ArrayType at = type.getCoreType().returnArrayType();
    if (at != null) {
      decl.setAddressTaken();
      type = PointerType.create(at.getElementType());
      return new TypeConversionOp(type, genDeclAddress(decl), CastMode.CAST);
    } 

    if (type.isProcedureType())
      decl.setAddressTaken();

    return genDeclAddress(decl);
  }

  private Expression buildStmtExpression(Statement s, Token t)
  {
    if (fatalError)
      return errExp;

    Statement ss = s;
    BlockStmt p  = null;

    while (ss instanceof BlockStmt) {
      BlockStmt bs = (BlockStmt) ss;
      int       n = bs.numStmts();
      if (n == 0)
        break;
      p = bs;
      ss = bs.getStmt(n - 1);
      continue;
    }

    if ((s == p) && (p.numStmts() == 0))
      s = null;

    Expression exp1 = null;
    Type       type = VoidType.type;
    if (!(ss instanceof EvalStmt)) {
      exp1 = new NilOp();
    } else {
      exp1 = ((EvalStmt) ss).getExpr();
      type = exp1.getType();
      if (p != null)
        p.removeLastStatement();
    }

    return new StatementOp(type, exp1, s);
  }

  private Expression buildSizeof(Type type, Token t)
  {
    if (fatalError)
      return errExp;

    if (type.isProcedureType())
      type = PointerType.create(type);

    return new SizeofLiteral(size_t_type, type);
  }

  private Expression buildSizeof(Expression expr, Token t)
  {
    if (fatalError)
      return errExp;

    Type ty = expr.getType();
    if (expr instanceof IdAddressOp) {
      IdAddressOp sop = (IdAddressOp) expr;
      ty = sop.getDecl().getType();
    } else if (expr instanceof SelectIndirectOp) {
      SelectIndirectOp sop   = (SelectIndirectOp) expr;
      FieldDecl        field = sop.getField();
      if (field.getType().isArrayType())
        ty = field.getType();
    } else if (expr instanceof SubscriptAddressOp) {
      ty = ((SubscriptAddressOp) expr).getArray().getCoreType().getPointedTo();
    } else if (expr instanceof AddressLiteral) {
      ty = ((AddressLiteral) expr).getDecl().getType();
    } else if ((expr instanceof TypeConversionOp) &&
               (((TypeConversionOp) expr).getConversion() == CastMode.CAST)) {
      Expression arg = ((TypeConversionOp) expr).getExpr();
      Type       ta  = arg.getCoreType();
      if (ta.isPointerType() && ta.getPointedTo().isArrayType())
        ty = ta.getPointedTo();
      else
        return buildSizeof(arg, t);
    }

    return buildSizeof(ty, t);
  }

  private FieldDecl getField(Type type, String fname, Token t)
  {
    if (fatalError)
      return null;

    AggregateType at = type.getCoreType().returnAggregateType();
    if (at == null) {
      userError(Msg.MSG_Incorrect_type_specification, null, t);
      return null;
    }

    FieldDecl fd = at.findField(fname);
    if (fd == null)
      userError(Msg.MSG_Field_s_not_found, fname, t);

    long size = at.memorySize(Machine.currentMachine); // Initialize offsets.

    return fd;
  }

  private Expression getArrayOffset(Type       type,
                                    Expression exp,
                                    Expression exp2,
                                    Token      t)
  {
    if (fatalError)
      return errExp;

    ArrayType at = type.getCoreType().returnArrayType();
    if (at == null) {
      userError(Msg.MSG_Incorrect_type_specification, null, t);
      return errExp;
    }

    Type       et   = at.getElementType();
    Expression mult = new SizeofLiteral(size_t_type, et);

    exp2 = new MultiplicationOp(size_t_type, mult, exp2);

    if (exp == null)
      return  exp2;

    return new AdditionOp(size_t_type, exp, exp2);
  }

  private Expression buildMultExpr(int sw, Expression la, Expression ra, Token t)
  {
    if (fatalError)
      return errExp;

    Type type = calcBinaryType(la.getType(), ra.getType());
    if (type == null)
      return userExprError(Msg.MSG_Operands_to_s_not_compatible, t.getText(), la, ra, t);

    la = cast(type, la, t);
    ra = cast(type, ra, t);

    if (sw == 0)
      return new MultiplicationOp(type, la, ra);

    if (sw == 1)
      return new DivisionOp(type, la, ra);

    Type t1 = la.getType();
    Type t2 = ra.getType();
    if (!isIntegerType(t1) || !isIntegerType(t2))
      return userExprError(Msg.MSG_Operands_to_s_not_integer_types, t.getText(), la, ra, t);

    return new RemainderOp(type, la, ra);
  }

  private Expression buildStringExpression(String str, Token t)
  {
    if (fatalError)
      return errExp;

    str = processString(str, 0xff);
    Type at = makeCharArrayType(str, char_type);
    return LiteralMap.put(str, at);
  }

  private Expression buildWStringExpression(String str, Token t)
  {
    if (fatalError)
      return errExp;

    str = processString(str, 0xffff);
    Type            at = makeCharArrayType(str, wchar_type);
    int             l  = str.length();
    IntArrayLiteral sl = new IntArrayLiteral(at, l);
    for (int i = 0; i < l; i++)
      sl.addElement(0xffff & str.charAt(i));
    return sl;
  }

  private Statement buildCaseStmt(Expression expr, Statement stmt1, Token t)
  {
    if (fatalError)
      return errStmt;

    addStmtInfo(stmt1, t.getLine(), t.getColumn());
    if (expr instanceof CharLiteral)
      expr = LiteralMap.put(((CharLiteral) expr).getCharacterValue(), int_type);

    CaseLabelDecl ld   = new CaseLabelDecl("_C" + caseCounter++, expr);
    Statement     stmt = new LabelStmt(ld, stmt1);
    addStmtInfo(stmt, t.getLine(), t.getColumn());
    return stmt;
  }

  private Statement buildLabelStmt(String name, Statement stmt1, Token t)
  {
    if (fatalError)
      return errStmt;

    if (stmt1 == null)
      stmt1 = new NullStmt();

    addStmtInfo(stmt1, t.getLine(), t.getColumn());

    LabelDecl ld   = getLabelDecl(name);
    Statement stmt = new LabelStmt(ld, stmt1);

    if (ld.getTag() != 0)
      userError(Msg.MSG_Label_s_defined_twice, name, t);
    ld.setTag(1);

    addStmtInfo(stmt, t.getLine(), t.getColumn());
    return stmt;
  }

  private void finishBlockStmt(BlockStmt bs, boolean ftn, SymtabScope cs, Token t)
  {
    createDeclStatments(bs, t);

    if (ftn && (bs.numStmts() == 0)) {
      Statement stmti = new ReturnStmt(null);
      addStmtInfo(stmti, t.getLine(), t.getColumn());
      bs.addStmt(stmti);
    } else if (bs.numStmts() == 0)
      bs.addStmt(new NullStmt());

    addStmtInfo(bs, t.getLine(), t.getColumn());
    bs.setScope(cs);
  }

  private Statement buildExpressionStmt(Expression expr, Token t)
  {
    if (fatalError)
      return errStmt;

    Statement stmt;
    if (expr != null)
      stmt = new EvalStmt(expr);
    else 
      stmt = new NullStmt();

    addStmtInfo(stmt, t.getLine(), t.getColumn());
    return stmt;
  }

  private Statement buildIfStmt(Expression expr, Statement stmtt, Statement stmtf, Token t)
  {
    if (fatalError)
      return errStmt;

    if (stmtf == null)
      stmtf = new NullStmt();

    expr = trueOrFalse(expr, t);
    Statement stmt = new IfThenElseStmt(expr, stmtt, stmtf);
    addStmtInfo(stmt, t.getLine(), t.getColumn());
    return stmt;
  }

  private Statement buildSwitchStmt(Expression expr, Statement stmtt, Token t)
  {
    if (fatalError)
      return errStmt;

    Statement stmt = new SwitchStmt(expr, stmtt);

    addStmtInfo(stmt, t.getLine(), t.getColumn());

    if (!(stmtt instanceof BlockStmt))
      return stmt;

    // Check for "fall through" cases and replace by explicit gotos.

    BlockStmt bs   = (BlockStmt) stmtt;
    int       ns   = bs.numStmts();
    Statement last = null;
    int       cnt  = 0;
    Statement st   = null;
    for (int ii = 1; ii < ns; ii++, last = st) {
      st = bs.getStmt(ii);
      if (!(st instanceof LabelStmt))
        continue;

      cnt++;
      if (cnt <= 1)
        continue;

      if (last instanceof BreakStmt)
        continue;

      LabelStmt ls = (LabelStmt) st;
      Statement s1 = ls.getStmt();
      ls.setStmt(new NullStmt());
      bs.insertStmt(s1, ii + 1);

      LabelDecl ld = getLabelDecl("_L" + labelCounter++);
      bs.insertStmt(new GotoStmt(ld), ii);
      bs.insertStmt(new LabelStmt(ld, new NullStmt()), ii + 2);
      ii += 3;
      ns += 3;
    }

    return stmt;
  }

  private Statement buildWhileStmt(Expression expr1, Statement istmt, Token t)
  {
    if (fatalError)
      return errStmt;

    expr1 = trueOrFalse(expr1, t);
    Statement stmt = new WhileLoopStmt(istmt, expr1);
    addStmtInfo(stmt, t.getLine(), t.getColumn());
    return stmt;
  }

  private Statement buildDoWhileStmt(Expression expr1, Statement istmt, Token t)
  {
    if (fatalError)
      return errStmt;

    expr1 = trueOrFalse(expr1, t);
    Statement stmt = new RepeatWhileLoopStmt(istmt, expr1);
    addStmtInfo(stmt, t.getLine(), t.getColumn());
    return stmt;
  }

  private Statement buildForStmt(Expression expr1, Expression expr2, Expression expr3, Statement istmt, Token t)
  {
    if (fatalError)
      return errStmt;

    if (istmt == null)
      istmt = new NullStmt();

    expr2 = trueOrFalse(expr2, t);
    Statement stmt = new ForLoopStmt(istmt, expr1, expr2, expr3);
    addStmtInfo(stmt, t.getLine(), t.getColumn());
    return stmt;
  }

  private Statement buildCloneForStmt(Expression expr1, Expression expr2, Expression expr3, Statement istmt, Token t, Expression exprThdNum)
  {
    if (fatalError)
      return errStmt;

    if (istmt == null)
      istmt = new NullStmt();

    expr2 = trueOrFalse(expr2, t);
    Statement stmt = new ForCloneLoopStmt(istmt, expr1, expr2, expr3, exprThdNum);
    addStmtInfo(stmt, t.getLine(), t.getColumn());
    return stmt;
  }

  private Statement buildGotoStmt(String label, Token t)
  {
    LabelDecl ld   = getLabelDecl(label);
    Statement stmt = new GotoStmt(ld);
    addStmtInfo(stmt, t.getLine(), t.getColumn());
    return stmt;
  }

  private Statement buildContinueStmt(Token t)
  {
    Statement stmt = new ContinueStmt();
    addStmtInfo(stmt, t.getLine(), t.getColumn());
    return stmt;
  }

  private Statement buildBreakStmt(Token t)
  {
    Statement stmt = new BreakStmt();
    addStmtInfo(stmt, t.getLine(), t.getColumn());
    return stmt;
  }

  private Statement buildReturnStmt(Expression expr, Token t)
  {
    if (fatalError)
      return errStmt;

    if (expr != null) {
      expr = convertLiterals(expr);
      ProcedureType pt = currentFunction.getSignature();
      Type          rt = pt.getReturnType();
      if (!rt.isAggregateType()) {
        expr = cast(rt, expr, t);
      }
    }

    Statement stmt = new ReturnStmt(expr);
    addStmtInfo(stmt, t.getLine(), t.getColumn());
    return stmt;
  }

  private void processTypeDeclarator(String name, Type type, Token t)
  {
    if (fatalError)
      return;

    Declaration         td   = null;
    Vector<SymtabEntry> ents = cg.getSymbolTable().lookupSymbol(name);
    if (ents != null) {
      int l = ents.size();
      if (l > 0) {
        for (int i = 0; i < l; i++) {
          SymtabEntry s    = ents.get(i);
          Declaration decl = s.getDecl();
          if (decl instanceof TypeName) {
            td = decl;
            break;
          }
        }
      }
    }
    if (td == null) {
      TypeName tdecl = defineTypeName(name, type);
      assert assertTrace("  ", tdecl);
      assert assertTrace("  ", type.getCoreType());
      cg.addSymbol(tdecl);
      return;
    }

    if (!td.getCoreType().equivalent(type.getCoreType()))
      userError(Msg.MSG_Type_s_is_already_defined, name, type.getCoreType(), td.getCoreType(), t);
  }

  private void processLine(String text, Token t)
  {
    int fi = text.indexOf('"');
    if (fi >= 0) {
      int li = text.indexOf('"', fi + 1);
      if (li <= 0)
        li = text.length();
      setFilename(text.substring(fi + 1, li));
    }
  }

  private void finishFunction(RoutineDecl rd, BlockStmt body, int sc, Token t)
  {
    if (fatalError)
      return;

    String name = rd.getName();
    if (body != null) {
      if (rd.getBody() != null)
        userError(Msg.MSG_Function_s_is_already_defined, name, rd, t);

      needsReturn(name, (ProcedureType) rd.getType(), body, t);

      rd.setBody(body);

      if (!isStorageClass(sc, cStatic)) {
        if (!allowGNUExtensions || !isStorageClass(sc, cExtern))
        sc = setStorageClass(sc, cGlobal);
      }
    }
    sc = (rd.getBody() == null) ? setStorageClass(sc, cExtern) : sc;
    specifyStorageClass(rd, sc, t);
    labels.clear();
  }

  private void finishTranslationUnit(FileDecl root, Vector<Declaration> rdecls, Token t)
  {
    SymtabScope              ss = cg.getSymbolTable().getRootScope();
    Enumeration<SymtabEntry> es = ss.orderedElements();
    while (es.hasMoreElements()) {
      SymtabEntry se   = es.nextElement();
      Declaration decl = se.getDecl();
      if (decl instanceof EnumElementDecl)
        continue;
      if (decl instanceof FormalDecl)
        continue;
      if (!decl.isReferenced())
        continue;
      rdecls.add(decl);
    }

    if (!classTrace)
      return;

    int ll = rdecls.size();
    for (int i = 0; i < ll; i++) {
      Declaration decl = rdecls.get(i);
      Type        ty   = decl.getCoreType();
      if (ty.isProcedureType())
        continue;

      System.out.println("  " + decl);
      long size = ty.memorySize(Machine.currentMachine);
      System.out.println("    " + size + " " + ty);

      AggregateType at = ty.returnAggregateType();
      if (at != null) {
        int lll = at.numFields();
        for (int ii = 0; ii < lll; ii++) {
          FieldDecl fd = at.getField(ii);
          System.out.println("       " + fd);
          Type        ty2   = fd.getCoreType();
          if (ty2.isProcedureType())
            continue;
          long size2 = ty2.memorySize(Machine.currentMachine);
          System.out.println("        " + size2 + " " + ty2);
        }
      }
    }
  }

  // ******* methods go here.
}

protected primaryExpression returns [Expression exp=errExp]
{
  BlockStmt s;
  String    t = null;
} :
   n00:Identifier                  { exp = buildVariableRef(n00.getText(), n00); }
 | n01:HexDoubleValue              { exp = buildFloatLiteral(parseHexDouble(n01.getText()), double_type); }
 | n02:HexFloatValue               { exp = buildFloatLiteral(parseHexDouble(n02.getText()), float_type); }
 | n03:HexLongDoubleValue          { exp = buildFloatLiteral(parseHexDouble(n03.getText()), long_double_type); }
 | n04:HexUnsignedIntValue         { exp = buildIntLiteral(parseHex(n04.getText()), true, unsigned_int_type); }
 | n05:HexUnsignedLongIntValue     { exp = buildIntLiteral(parseHex(n05.getText()), true, unsigned_long_type); }
 | n06:HexUnsignedLongLongIntValue { exp = buildIntLiteral(parseHex(n06.getText()), true, unsigned_long_long_type); }
 | n07:HexIntValue                 { exp = buildIntLiteral(parseHex(n07.getText()), true, int_type); }
 | n08:HexLongIntValue             { exp = buildIntLiteral(parseHex(n08.getText()), true, long_type); }
 | n09:HexLongLongIntValue         { exp = buildIntLiteral(parseHex(n09.getText()), true, long_long_type); }
 | n10:DoubleValue                 { exp = buildFloatLiteral(java.lang.Double.parseDouble(n10.getText()), double_type); }
 | n11:FloatValue                  { exp = buildFloatLiteral(java.lang.Double.parseDouble(n11.getText()), double_type); }
 | n12:LongDoubleValue             { exp = buildFloatLiteral(java.lang.Double.parseDouble(n12.getText()), long_double_type); }
 | n13:UnsignedIntValue            { exp = buildIntLiteral(parseLong(n13.getText()), false, unsigned_int_type); }
 | n14:UnsignedLongIntValue        { exp = buildIntLiteral(parseLong(n14.getText()), false, unsigned_long_type); }
 | n15:UnsignedLongLongIntValue    { exp = buildIntLiteral(parseLong(n15.getText()), false, unsigned_long_long_type); }
 | n16:IntValue                    { exp = buildIntLiteral(parseLong(n16.getText()), false, int_type); }
 | n17:LongIntValue                { exp = buildIntLiteral(parseLong(n17.getText()), false, long_type); }
 | n18:LongLongIntValue            { exp = buildIntLiteral(parseLong(n18.getText()), false, long_long_type); }
 | n19:CharacterConstant           { exp = LiteralMap.put((int) n19.getText().charAt(0), char_type); }
 | n22:WideCharacterConstant       { exp = LiteralMap.put(0xffff & n22.getText().charAt(0), wchar_type); }
 | n20:StringLit  { t = n20.getText(); }
   (n21:StringLit { t  = t.substring(0, t.length() - 1) + n21.getText(); } )*
                                   { exp = buildStringExpression(t, n20); }
 | n23:WideStringLiteral  { t = n23.getText(); }
   (n24:StringLit { t  = t.substring(0, t.length() - 1) + n24.getText(); } )*
                                   {exp = buildWStringExpression(t, n23); }
 | {allowGNUExtensions}? (KEYWORD_Extension LParen LBrace)=> KEYWORD_Extension LParen s  =compoundStatement[false] RParen { exp = buildStmtExpression(s, LT(1)); }
 | {allowGNUExtensions}? (                  LParen LBrace)=>                   LParen s  =compoundStatement[false] RParen { exp = buildStmtExpression(s, LT(1)); }
 | {allowGNUExtensions}? KEYWORD_Extension LParen exp=expression RParen
 |                                         LParen exp=expression RParen
 ;

protected postfixExpression returns [Expression exp=errExp]
{
  Expression expr0 = null;
  Expression expr1 = null;
  Type       ty    = null;
  Vector<Expression> args  = null;
} :
  (
    (LParen typeName RParen LBrace)=> LParen ty=typeName RParen exp = aggregationExpr[ty]
  | (Identifier LParen)=> exp=ftnCall
  | ((KEYWORD_va_arg | KEYWORD_builtin_va_arg)  LParen)=> exp=vaArg
  | exp=primaryExpression
  )
    {expr0 = exp; }
  (
     Dot n0:Identifier                                     { expr0 = buildFieldRef(false, n0.getText(), expr0, n0); }
   | Select n1:Identifier                                  { expr0 = buildFieldRef(true, n1.getText(), expr0, n1); }
   | n2:LBracket expr1=expression RBracket                 { expr0 = buildSubscriptValue(expr0, expr1, n2); }
   | n3:LParen   (args = argumentExpressionList)? RParen   { expr0 = buildCall(expr0, args, n3); }
   | Dec { expr0 = new PostDecrementOp(expr0.getType(), makeLValue(expr0)); }
   | Inc { expr0 = new PostIncrementOp(expr0.getType(), makeLValue(expr0)); }
   )*
    { exp = expr0; }
  ;

protected ftnCall returns [Expression exp=errExp]
{
  Vector<Expression> args = null;
  Type       ty   = null;
  Expression call = null;
} :
  n00:Identifier LParen
   (
     {n00.getText().equals("__builtin_isfloat")}? ty = singleType RParen
     { call = ty.isRealType() ? one : zero; }
   | (args = argumentExpressionList)? RParen
   {
     if (call == null)
       exp = buildCall(n00.getText(), args, n00);
     else
       exp = call;
   }
  )
;

protected vaArg returns[Expression exp = errExp]
{
  Type ty;
} :
  (KEYWORD_va_arg | KEYWORD_builtin_va_arg) LParen exp = conditionalExpression Comma ty = singleType
    {
      if (exp instanceof IdReferenceOp)
        ((IdReferenceOp) exp).getDecl().setAddressTaken();
      exp = new VaArgOp(ty, makeLValue(exp));
    }
  RParen
  ;

protected argumentExpressionList returns [Vector<Expression> v = null]
{
  Expression expr1, expr2;
} :
{
  v = new Vector<Expression>(4);
}
  expr1=assignmentExpression
   {
     if (expr1 != null)
       v.addElement(expr1);
   }
  (Comma expr2=assignmentExpression
   {
    if (expr2 != null)
      v.addElement(expr2);
   }
  )*
  ;

protected unaryExpression returns [Expression exp=errExp]
{
  Expression expr;
  Type       type;
} :
      n0:Inc  expr=unaryExpression { exp = new PreIncrementOp(expr.getType(), makeLValue(expr)); }
    | n1:Dec  expr=unaryExpression { exp = new PreDecrementOp(expr.getType(), makeLValue(expr)); }
    | n2:And  expr=castExpression  { exp = buildAddressOf(expr, n2); }
    | n3:Mult expr=castExpression  { exp = buildDereferenceOp(expr, n3); }
    | n4:Plus expr=castExpression  { exp = expr; }
    | n5:Sub  expr=castExpression  { exp = new NegativeOp(expr.getType(), expr); }
    | n6:Comp expr=castExpression  { exp = new BitComplementOp(expr.getType(), expr); }
    | n7:Not  expr=castExpression  { exp = equal0(expr, LT(1)); }
    | n8:KEYWORD_sizeof exp=sizeof
    | n9:KEYWORD_builtin_offsetof exp=offsetof
    | n10:KEYWORD_alignof l1:LParen type = singleType RParen
   {
     if (strictANSIC)
       userWarning(Msg.MSG_alignof_is_non_standard, null, l1);
     if (!allowGNUExtensions)
       userWarning(Msg.MSG_alignof_is_non_standard, null, l1);

      int al = type.alignment(Machine.currentMachine);
      exp = LiteralMap.put(al, unsigned_int_type);
   }
 | exp=postfixExpression
 ;

protected sizeof returns[Expression exp = errExp]
{
  Type       type;
  Expression expr;
} :
  (LParen singleType RParen)=> n0:LParen type = singleType RParen
    { exp = buildSizeof(type, n0); }
  | expr = unaryExpression
    { exp = buildSizeof(expr, LT(0)); }
  ;
exception
catch [RecognitionException ex]
{
  reportError(ex);
  consume();
  fatalError = true;
}

protected offsetof returns[Expression exp = errExp]
{
  Type       type;
} :
  LParen type = singleType Comma exp=memberDesignator[type] RParen
  ;
exception
catch [RecognitionException ex]
{
  reportError(ex);
  consume();
  fatalError = true;
}

protected memberDesignator[Type type] returns[Expression exp = null]
{
  long       offset = 0;
  Expression exp2   = null;
} :
  id:Identifier
  {
    FieldDecl fd = getField(type, id.getText(), id);
    if (fd != null) {
      offset += fd.getFieldOffset();
      type = fd.getType();
    }
  }
  (
    Dot id2:Identifier
    {
      FieldDecl fd = getField(type, id2.getText(), id2);
      if (fd != null) {
        offset += fd.getFieldOffset();
        type = fd.getType();
      }
    }
  | n:LBracket exp2=expression RBracket
    {
      exp = getArrayOffset(type, exp, exp2, n);
    }
  )*
  {
    Expression off = LiteralMap.put(offset, size_t_type);
    if (exp == null)
      exp = off;
    else if (offset != 0)
      exp = new AdditionOp(size_t_type, off, exp);
  }
;

protected singleType returns [Type type = void_type]
{
  String attributes        = null;
  int    saveStorageClass  = storageClass;
} :
   type = declarationSpecifiers
   {
      storageClass  = saveStorageClass;
   }
   (type=abstractDeclarator[type])? 
 ;
exception
catch [RecognitionException ex]
{
  storageClass  = saveStorageClass;
  reportError(ex);
  consume();
  fatalError = true;
}

protected castExpression returns [Expression exp=errExp]
{
  Type type = null;
} :
  (LParen typeName RParen)=> LParen type=typeName n0:RParen
    (
       exp = aggregationExpr[type]
     | exp = castExpression
       {
         if (!fatalError) {
           if (type.isArrayType())
           type = PointerType.create(type);
           exp = cast(type, convertLiterals(exp), n0);
         }
       }
    )
 | exp=unaryExpression
 ;

protected aggregationExpr[Type type] returns[Expression exp = errExp]
{
  Vector<Object> v = null;
} :
    v=initializerList[type]
    {
      if (allConstants(v)) {
        exp = buildAggregation(type, v, LT(1));
      } else {
        VariableDecl decl = genTemp(type.getNonConstType());
        exp = buildSeries(decl, v, LT(1));
        cg.addSymbol(decl);
      }
    }
;

protected multiplicativeExpression returns [Expression exp=errExp]
{
  Expression expr2;
} :
  exp = castExpression
  (
      n0:Mult  expr2=castExpression { exp = buildMultExpr(0, exp, expr2, n0); }
    | n1:Slash expr2=castExpression { exp = buildMultExpr(1, exp, expr2, n1); }
    | n2:Mod   expr2=castExpression { exp = buildMultExpr(2, exp, expr2, n2); }
 )* ;

protected additiveExpression returns [Expression exp=errExp]
{
  Expression expr2;
} :
  exp=multiplicativeExpression
  (
    n0:Plus expr2=multiplicativeExpression { exp = buildAddExpr(exp, expr2, n0); }
  | n1:Sub expr2=multiplicativeExpression { exp = buildSubtractExpr(exp, expr2, n1); }
  )* ;

protected shiftExpression returns [Expression exp=errExp]
{
  Expression expr2;
  ShiftMode  shift = ShiftMode.Left;
  Token      t     = null;
} :
  exp=additiveExpression
  (
   (
      n0:LShift { shift = ShiftMode.Left;        t = n0; }
    | n1:RShift { shift = ShiftMode.SignedRight; t = n1; }
   )
   expr2=additiveExpression { exp = buildShiftExpr(shift, exp, expr2, t); }
 )* ;

protected relationalExpression returns [Expression exp=errExp]
{
  Expression expr2 = null;
  int        sw    = 0;
  Token      t     = null;
} :
  exp = shiftExpression
 (
  (
    n0:LAngle { sw = 1; t = n0; }
  | n1:RAngle { sw = 2; t = n1; }
  | n2:LEqual { sw = 3; t = n2; }
  | n3:GEqual { sw = 4; t = n3; }
  )
  expr2=shiftExpression { exp = buildCompare(sw, exp, expr2, t); }
 )* ;

protected equalityExpression returns [Expression exp=errExp]
{
  Expression expr2;
  boolean    equals = true;
  Token      t = null;
} :
 exp = relationalExpression
 (
  (
     n0:Equal  { equals = true;  t = n0; }
   | n1:NEqual { equals = false; t = n1; }
  )
  expr2=relationalExpression { exp = buildEqualCompare(equals, exp, expr2, t); }
 )* ;

protected andExpression returns [Expression exp=errExp]
{
  Expression expr2;
} :
  exp = equalityExpression
   (
     n0:And expr2=equalityExpression { exp = buildBitExpr(0, exp, expr2, n0); }
   )* ;

protected xorExpression returns [Expression exp=errExp]
{
  Expression expr2;
} :
  exp = andExpression
   (
     n0:Xor expr2=andExpression { exp = buildBitExpr(2, exp, expr2, n0); }
   )* ;

protected orExpression returns [Expression exp=errExp]
{
  Expression expr2;
} :
  exp = xorExpression
   (
     n0:Or expr2=xorExpression { exp = buildBitExpr(1, exp, expr2, n0); }
   )* ;

protected andConditionExpression returns [Expression exp=errExp]
{
  Expression expr2;
} :
  exp = orExpression
  (
   n0:AndCond expr2=orExpression { exp = buildConditional(0, exp, expr2, n0); }
  )*
 ;

protected orConditionExpression returns [Expression exp=errExp]
{
  Expression expr2;
} :
  exp=andConditionExpression
  (
   n0:OrCond expr2=andConditionExpression { exp = buildConditional(1, exp, expr2, n0); }
  )*
 ;

protected conditionalExpression returns [Expression exp=errExp]
{
  Expression expr2;
  Expression expr3;
} :
  exp = orConditionExpression
  (
   n0:QMark expr2=expression Colon expr3=conditionalExpression { exp = buildExpressionIf(exp, expr2, expr3, n0); }
  )?
 ;

protected assignmentExpression returns [Expression exp=errExp]
{
  int        op  = 0;
  Expression rhs = null;
  Token      t   = null;
} :
    exp=conditionalExpression
    ( { t = LT(1); } 
     op = assignmentOperator rhs = assignmentExpression
      { exp = buildAssignment(op, exp, rhs, t); }
    )?
 ;

protected assignmentOperator returns[int op = 0] :
  Assign       { op =  1; }
| MultAssign   { op =  2; }
| SlashAssign  { op =  3; }
| ModAssign    { op =  4; }
| PlusAssign   { op =  5; }
| SubAssign    { op =  6; }
| AndAssign    { op =  7; }
| OrAssign     { op =  8; }
| XorAssign    { op =  9; }
| LSAssign     { op = 10; }
| RSAssign     { op = 11; }
;

protected expression returns [Expression exp=errExp]
{
  Expression expr;
} : 
          exp = assignmentExpression 
  (Comma expr = assignmentExpression { exp = new SeriesOp(exp, expr); } )*
  ;

protected constantExpression returns [Expression exp=errExp]
{
  boolean saveInConstant = inConstant;
} : 
    { inConstant = true; }
    exp=conditionalExpression
    {
      inConstant = saveInConstant;
      if (!fatalError) {
        Literal lit = exp.getConstantValue();
        if ((lit == Lattice.Bot) || (lit == Lattice.Top))
          userError(Msg.MSG_Not_a_constant, null, LT(1));
        else
          exp = lit;
      }
    }
 ;

protected declaration[BlockStmt block]
{
  Type   type             = null;
  Vector<Declaration> vars = null;
  String attributes       = null;
  int    saveStorageClass = storageClass;
  int    sc               = 0;
} :
{
  vars = new Vector<Declaration>(10);
}
  type = declarationSpecifiers
    {
      sc = storageClass;
      storageClass = saveStorageClass;
    }
  (initDeclaratorList[vars, type, sc])? s0:Semi
    {
      processDeclarations(vars, block, s0);
    }
  ;

protected declarationSpecifiers returns[Type type = void_type]
{
  int     saveBaseType      = baseType;
  int     saveTypeQualifier = typeQualifier;
  Type    saveBuildType     = buildType;
} :
{
  baseType      = cStart;
  typeQualifier = 0;
  storageClass  = cAuto;
}
(options { warnWhenFollowAmbig=false;} :
   storageClassSpecifier
 | typeQualifier
 | functionSpecifier
 | typeSpecifier
 | {isTypedef(LT(1).getText()) && (baseType == cStart)}? n0:Identifier
   {
    String   name = n0.getText();
    TypeName tn   = lookupTypedef(name);
    buildType = tn.getType();
    baseType = specifyBaseType(cOther, baseType);
   }
 )+
   {
     if (baseType == cStart)
       baseType = cInt;

      buildType = buildType(baseType, buildType);
      if (buildType == null) {
        userError(Msg.MSG_Invalid_type, null, LT(1));
        buildType = void_type;
      }

      type          = buildType;
      baseType      = saveBaseType;
      buildType     = saveBuildType;
      typeQualifier = saveTypeQualifier;
    }
 ;
exception
catch [RecognitionException ex]
{
  baseType      = saveBaseType;
  buildType     = saveBuildType;
  typeQualifier = saveTypeQualifier;
  reportError(ex);
  consume();
  fatalError = true;
}

protected declarationSpecifiersChk returns[Type type = void_type]
{
  int     saveBaseType      = baseType;
  int     saveTypeQualifier = typeQualifier;
  Type    saveBuildType     = buildType;
} :
{
  baseType      = cStart;
  typeQualifier = 0;
  storageClass  = cAuto;
}
(options { warnWhenFollowAmbig=false;} :
   storageClassSpecifier
 | typeQualifier
 | functionSpecifier
 | typeSpecifierChk
 | {isTypedef(LT(1).getText()) && (baseType == cStart)}? n0:Identifier
 )+
   {
      baseType      = saveBaseType;
      buildType     = saveBuildType;
      typeQualifier = saveTypeQualifier;
    }
 ;
exception
catch [RecognitionException ex]
{
  baseType      = saveBaseType;
  buildType     = saveBuildType;
  typeQualifier = saveTypeQualifier;
}

protected initDeclaratorList [Vector<Declaration> vars, Type type, int sc]
{
  Declaration decl = null;
} :
          decl = initDeclarator[type, sc] { vars.addElement(decl);}
   (Comma decl = initDeclarator[type, sc] { vars.addElement(decl);})* ;

protected initDeclarator[Type type, int sc] returns [Declaration decl = null]
{
  Expression expr = null;
} : decl = declarator[type, sc]
     {
       if (decl != null)
         type = decl.getType();
     }
    (n0:Assign expr = initializer[type])?
     { processDeclInitializer(decl, type, expr, n0); }
 ;

protected declarator[Type type, int sc] returns [Declaration decl = null]
{
  String saveIdentifier = declID;
} :
  type=declarator2[type]
   { 
     decl = defineDecl(declID, type, sc, LT(1));
     processAttributes(declAtts, decl, LT(1));
     declAtts = "";
     declID = saveIdentifier;
   }
 ;

protected declarator2[Type type] returns [Type ntype = type] :
   ntype = pointer[ntype] ntype=directDeclarator[ntype]
 | ntype = directDeclarator[ntype]
 ;

protected directDeclarator[Type type] returns [Type ntype=type]
{
  IncompleteType ictype = null;
  Type           xtype  = null;
  String         name   = null;
  int            pos    = 0;
  String         atts   = "";
  declID = "";
} :
   ({allowGNUExtensions}? "__restrict")? (att0:Attributes { declAtts = declAtts + att0.getText(); })*
   id:Identifier
   ({allowGNUExtensions}? ("__asm__" | "asm" | "__asm"))?
   (ntype = parenGroup[ntype] { pos = 0; } | ntype = bracketGroup[ntype, pos++])*
     { declID = id.getText(); } 
   ({allowGNUExtensions}? ("__asm__" | "asm" | "__asm") (LParen StringLit (StringLit)* RParen)?)?
   (options { warnWhenFollowAmbig=false;} : att1:Attributes { declAtts = declAtts + att1.getText(); })*
 | LParen
     {
       ictype = new IncompleteType();
       ntype = type;
     }
   xtype=declarator2[ictype] RParen
     {
       name = declID;
     }
   ( ntype = parenGroup[ntype] { pos = 0; } | ntype = bracketGroup[ntype, pos++] )*
     {
       if (!fatalError) {
         ictype.setCompleteType(ntype);
         ntype = xtype;
         ArrayType at = ntype.getCoreType().returnArrayType();
         if (at != null) {
           if (at.getElementType().isProcedureType())
             userError(Msg.MSG_Array_of_functions_is_not_allowed, null, LT(0));
         }
         declID = name;
       }
     }
;

protected storageClassSpecifier : 
   KEYWORD_extern   { storageClass = specifyStorageClass(storageClass, cExtern, LT(1)); }
 | KEYWORD_static   { storageClass = specifyStorageClass(storageClass, cStatic, LT(1)); }
 | KEYWORD_auto     { storageClass = specifyStorageClass(storageClass, cAuto, LT(1)); }
 | KEYWORD_register { storageClass = specifyStorageClass(storageClass, cRegister, LT(1)); }
 ;

protected functionSpecifier :
 (
   KEYWORD_inline        
 | {allowGNUExtensions}? KEYWORD__inline  
 | {allowGNUExtensions}? KEYWORD__inline__
 )
  { storageClass |= cInlined; }
;

protected typeSpecifier
{
  Expression exp;
} :
 ( KEYWORD_void       { baseType = specifyBaseType(cVoid, baseType);}
 | KEYWORD_char       { baseType = specifyBaseType(cChar, baseType);}
 | KEYWORD_short      { baseType = specifyBaseType(cShort, baseType);}
 | KEYWORD_int        { baseType = specifyBaseType(cInt, baseType);}
 | KEYWORD_long       { baseType = specifyBaseType(cLong, baseType);}
 | KEYWORD_float      { baseType = specifyBaseType(cFloat, baseType);}
 | KEYWORD_double     { baseType = specifyBaseType(cDouble, baseType);}
 | KEYWORD_signed     { baseType = specifyBaseType(cSigned, baseType);}
 | {allowGNUExtensions}? KEYWORD___signed__     { baseType = specifyBaseType(cSigned, baseType);}
 | KEYWORD_unsigned   { baseType = specifyBaseType(cUnsigned, baseType);}
 | KEYWORD__Bool      { baseType = specifyBaseType(cBool, baseType);}
 | KEYWORD__Complex   { baseType = specifyBaseType(cComplex, baseType);}
 | KEYWORD__Imaginary { baseType = specifyBaseType(cImaginary, baseType);}
 | {allowGNUExtensions}? KEYWORD_typeof LParen exp=expression RParen
     {
        buildType = exp.getType();
        baseType = specifyBaseType(cOther, baseType);
     }
 | buildType=structOrUnionSpecifier { baseType = specifyBaseType(cOther, baseType);}
 | buildType=enumSpecifier          { baseType = specifyBaseType(cOther, baseType); }
 )
 ;

protected typeSpecifierChk
{
  Expression exp;
} :
 ( KEYWORD_void
 | KEYWORD_char
 | KEYWORD_short
 | KEYWORD_int
 | KEYWORD_long
 | KEYWORD_float
 | KEYWORD_double
 | KEYWORD_signed
 | {allowGNUExtensions}? KEYWORD___signed__
 | KEYWORD_unsigned
 | KEYWORD__Bool
 | KEYWORD__Complex
 | KEYWORD__Imaginary
 | {allowGNUExtensions}? KEYWORD_typeof LParen
 | KEYWORD_struct
 | KEYWORD_union
 | KEYWORD_enum
 )
 ;

protected typeQualifier :
   KEYWORD_const    { specifyTypeQualifier(cConstantType); }
 | {allowGNUExtensions}? KEYWORD___const    { specifyTypeQualifier(cConstantType); }
 | KEYWORD_restrict { specifyTypeQualifier(cRestrict); }
 | KEYWORD_volatile { specifyTypeQualifier(cVolatile); }
 | {allowGNUExtensions}? KEYWORD__volatile { specifyTypeQualifier(cVolatile); }
 ;

protected structOrUnionSpecifier returns [Type type = void_type]
{
  Vector<FieldDecl>   fields  = null;
  boolean  isUnion = false;
  TypeDecl decl    = null;
  String   atts    = "";
} :
{
  fields = new Vector<FieldDecl>(8);
}
  (KEYWORD_struct | KEYWORD_union {isUnion = true;}) (att0:Attributes { atts = atts + att0.getText(); })*
   (
     LBrace structDeclarationList[fields] RBrace
     (options { warnWhenFollowAmbig=false;} : att1:Attributes { atts = atts + att1.getText(); })* 
       {
         AggregateType atype = isUnion ? UnionType.create(fields, false) : RecordType.create(fields);
         atype.memorySize(Machine.currentMachine);
         decl = createTypeDecl("_T" + typeCounter++, atype);
         type = decl.getType();
       }
     | n0:Identifier
        {
         decl = lookupTypeDecl(n0.getText());
         if (decl == null) {
           IncompleteType ictype = new IncompleteType();
           if (noRecordType == null)
             noRecordType = RecordType.create(new Vector<FieldDecl>(0));
           if (noUnionType == null)
             noUnionType = UnionType.create(new Vector<FieldDecl>(0), false);
           decl = createTypeDecl(n0.getText(), ictype);
           cg.addSymbol(decl);
           ictype.setCompleteType(isUnion ? noUnionType : noRecordType);
         }
         type = decl.getType();
        }
        (
         LBrace structDeclarationList[fields] RBrace
         (options { warnWhenFollowAmbig=false;} : att2:Attributes { atts = atts + att2.getText(); })*
         {
           if (!fatalError) {
             Type           stype  = type;
             AggregateType  atype  = isUnion ? UnionType.create(fields, false) : RecordType.create(fields);
             IncompleteType ictype = lastIncompleteType(stype);

             atype.memorySize(Machine.currentMachine);

             if (ictype == null)
               userError(Msg.MSG_Type_s_is_already_defined, n0.getText(), n0);
             else {
               ictype.setCompleteType(atype);

               Symtab      st = cg.getSymbolTable();
               SymtabScope sc = st.getCurrentScope();
               SymtabEntry se = sc.lookupSymbol(decl);
               if (se == null)
                 cg.addSymbol(decl);
               else
                 sc.reorder(se);
             }
           }
         }
        )?
    )
    { type = processAttributes(atts, type, LT(0)); }
  ;

protected structDeclarationList[Vector<FieldDecl> fields] :
 (structDeclaration[fields])*
 ;

protected structDeclaration[Vector<FieldDecl> fields]
{
  Type      type = null;
  FieldDecl fd   = null; 
} :
  (KEYWORD_Extension)? type = specifierQualifierList
  (
          fd = structDeclarator[type] { addField(fd, fields, LT(1)); }
   (Comma fd = structDeclarator[type] { addField(fd, fields, LT(1)); })*
  )?
  n0:Semi
   {
     if (fd == null) { // Anonymous field.
       fd = createFieldDecl(null, type, 0, fieldCounter++, n0);
       addField(fd, fields, LT(1));
     }
   }
 ;

protected specifierQualifierList returns [Type type = void_type]
{
  int     saveBaseType      = baseType;
  int     saveTypeQualifier = typeQualifier;
  Type    saveBuildType     = buildType;
} :
{
   baseType      = cStart;
   typeQualifier = 0;
}
   (
    options { warnWhenFollowAmbig=false; }:
     typeQualifier
   | typeSpecifier
   | {isTypedef(LT(1).getText()) && (baseType == cStart)}? n0:Identifier
     {
       String   name = n0.getText();
       TypeName tn   = lookupTypedef(name);
       buildType = tn.getType();
       baseType = specifyBaseType(cOther, baseType);
      }
   )+
    {
      buildType = buildType(baseType, buildType); 
      if (buildType == null) {
        userError(Msg.MSG_Invalid_type, null, LT(1));
        buildType = void_type;
      }
      type          = buildType;
      baseType      = saveBaseType;
      buildType     = saveBuildType;
      typeQualifier = saveTypeQualifier;
    }
 ;
exception
catch [RecognitionException ex]
{
  baseType      = saveBaseType;
  buildType     = saveBuildType;
  typeQualifier = saveTypeQualifier;
  reportError(ex);
  consume();
  fatalError = true;
}

protected structDeclarator[Type type] returns [FieldDecl fd = null]
{
  Expression expr1  = null;
  FieldDecl  decl   = null;
  String     saveId = declID;
  String     name   = null;
  Token      t      = null;
} : 
   n0:Colon expr1=constantExpression ( type = getAttributes[type] )?
   {
     int bits = 0;
     if (expr1 != null)
       bits = getIntValue(expr1, n0);
     fd = createFieldDecl(null, type, bits, fieldCounter++, n0);
   }
 |  {saveId = declID; t = LT(1); }
   type=declarator2[type]
    { name = declID; declID = saveId; }
   (n1:Colon expr1=constantExpression)? ( type = getAttributes[type])?
   {
     int bits = 0;
     if (expr1 != null)
       bits = getIntValue(expr1, n1);
     fd = createFieldDecl(name, type, bits, 0, t);
   }
  ;

protected getAttributes[Type type] returns[Type ntype = type] :
  att:Attributes { ntype = processAttributes(att.getText(), type, att); }
;

protected enumSpecifier returns [Type type = void_type]
{
  Vector<EnumElementDecl> ev   = null;
  String atts = "";
} : 
 KEYWORD_enum (att0:Attributes { atts = atts + att0.getText(); })*
 (
   n0:LBrace              ev = enumeratorList RBrace
   (options { warnWhenFollowAmbig=false;} :att1:Attributes { atts = atts + att1.getText(); })*
    { type = createEnumType(null, ev, n0); }
 | n1:Identifier ( LBrace ev = enumeratorList RBrace
   (options { warnWhenFollowAmbig=false;} :att2:Attributes { atts = atts + att2.getText(); })*)?
    { type = createEnumType(n1.getText(), ev, n1); }
 )
 { type = processAttributes(atts, type, LT(0)); }
;

protected enumeratorList returns [Vector<EnumElementDecl> ev = new Vector<EnumElementDecl>(10)]
{
  EnumElementDecl ed = null;
} :
           ed = enumerator[ed] {ev.addElement(ed); }
   (Comma (ed = enumerator[ed] {ev.addElement(ed); })? )*
 ;

protected enumerator[EnumElementDecl last] returns [EnumElementDecl ed = null]
{
  Expression expr1 = null;
} : n0:Identifier (Assign expr1=constantExpression)? { ed = buildEnumElementDecl(n0.getText(), last, expr1, n0); } ;

protected bracketGroup[Type type, int pos] returns [Type ntype=type]
{
  Expression expr1 = null;
} :
   s0:LBracket 
    (
        { ntype = createArrayType(pos == 0, type, s0); }
      | KEYWORD_static (typeQualifierList)? expr1=assignmentExpression
      | typeQualifierList
        (
          s1:KEYWORD_static expr1=constantExpression { notImplementedError("static in array declaration ", s0); }
        | (Mult RBracket)=> Mult
        | expr1=assignmentExpression
        ) 
         { notImplementedError("static in array declaration ", s1); }
      | (Mult RBracket)=> s2:Mult { ntype = createArrayType(pos == 0, type, s0); }
      | expr1=constantExpression { ntype = createArrayType(pos == 0, type, expr1, s0); }
    )
  RBracket
 ;

protected parenGroup[Type type] returns [Type ntype=type]
{
  Vector<? extends Object> formals = null;
} :
  LParen
      (
      | (KEYWORD_void  RParen)=> KEYWORD_void
      | {!isTypedef(LT(1).getText())}? (Identifier)=> formals=identifierList
      | formals=parameterTypeList
      )
 RParen
      {
        ntype = makeProcedureType(type, formals);
      }
  ;

protected pointer[Type type] returns [Type ntype = type]
{
  int saveTypeQualifier = typeQualifier;
} :
  (Mult
    { typeQualifier = 0; } 
   (typeQualifierList)?
     {
      ntype = PointerType.create(ntype);
      if (typeQualifier != 0)
        ntype = addTypeQualifier(ntype, typeQualifier);
     }
   )+
   { typeQualifier = saveTypeQualifier; }
   ;

protected typeQualifierList :
{
  typeQualifier = 0;
}
  (typeQualifier)+
 ;

protected parameterTypeList returns [Vector<FormalDecl> p = null;] :
{
  p = new Vector<FormalDecl>(8);
}
   parameterList[p]
   ((Comma Varargs)=> Comma Varargs (RParen)=> {p.addElement(new UnknownFormals());})?
;

protected parameterList[Vector<FormalDecl> v]
{
  FormalDecl fd      = null;
  int        counter = 0;
} :
         fd=parameterDeclaration[counter++] { addParameter(fd, v, LT(0)); } 
  (options { warnWhenFollowAmbig=false;} :
   Comma fd=parameterDeclaration[counter++] { addParameter(fd, v, LT(0)); } )*
 ;

protected parameterDeclaration[int counter] returns [FormalDecl fd=null]
{
  Type   type             = null;
  int    saveStorageClass = storageClass;
  String atts             = "";
} : 
  type = declarationSpecifiers
    {
      storageClass  = saveStorageClass;
    }
  (
    (formalDeclarator[type])=> fd=formalDeclarator[type]
  | type=abstractDeclarator[type] ("__restrict")? { fd = createFormalDecl(null, type, counter); }
  | "__restrict"                                  { fd = createFormalDecl(null, type, counter); }
  )?
    {
      if ((fd == null) && (type != void_type))
        fd = createFormalDecl(null, type, counter);
    }
 ;

protected formalDeclarator[Type type] returns [FormalDecl decl = null]
{
  String saveIdentifier = declID;
} :
  type=declarator2[type]
   { 
     decl = createFormalDecl(declID, type, 0);
     declID = saveIdentifier;
   }
 ;

protected identifierList returns [Vector<String> ids = new Vector<String>(6)] :
               n0:Identifier { ids.addElement(n0.getText()); }
        (Comma n1:Identifier { ids.addElement(n1.getText()); })* ;

protected typeName returns [Type type = void_type] :
   type=specifierQualifierList (type=abstractDeclarator[type])? ;

protected abstractDeclarator[Type type] returns [Type ntype=type] : 
   ntype = pointer[type] (ntype=directAbstractDeclarator[ntype])?
 | ntype=directAbstractDeclarator[type]
  ;

protected directAbstractDeclarator[Type type] returns [Type ntype=type]
{
  int pos = 0;
} :
  ( (LParen)=>   ntype = directFunction[ntype] { pos = 0; }
  | (LBracket)=> ntype = directArray[ntype, pos++]
  )+
  ;

protected directFunction[Type type] returns[Type pt = type]
{
  Vector<FormalDecl> v      = null;
  IncompleteType     ictype = null;
} :
    l0:LParen RParen { pt = createProcedureType(type, new Vector<FormalDecl>(0), l0); }
  | l1:LParen
    {
      ictype = new IncompleteType();
    }
    pt = abstractDeclarator[ictype] RParen
    {
      Type rt = ProcedureType.create(type, new Vector<FormalDecl>(0), null);
      ictype.setCompleteType(rt);
    }
  | l2:LParen v=parameterTypeList RParen { pt = createProcedureType(type, v, l2); }
 ;

protected directArray[Type type, int pos] returns[Type ntype = type]
{
  Expression exp = null;
} :
 s0:LBracket
   (
                            RBracket { ntype = createArrayType(pos == 0, type, s0); }
   | (Mult RBracket)=> Mult RBracket { ntype = createArrayType(pos == 0, type, s0); }
   | exp=constantExpression RBracket { ntype = createArrayType(pos == 0, 0, getLongValue(exp, s0), ntype, s0); }
   )
 ;

protected initializer[Type type] returns [Expression expr = errExp]
{
  Vector<Object>  v   = null;
  boolean         sic = inConstant;
} :
  (
    { inConstant = true; } v=initializerList[type] { expr = buildAggregation(type, v, LT(1)); inConstant = sic; }
  | expr=assignmentExpression
  )
 ;

protected initializerList[Type type] returns [Vector<Object> v = null]
{
  int  npos  = 0;
  Type ntype = null;
} :
{
  v = new Vector<Object>(20);
}
 LBrace
  (
    npos = initializerListItem[v, npos, type]
    (
     Comma
     (npos=initializerListItem[v, npos, type])?
    )*
  )?
 RBrace
 ;
exception
catch [RecognitionException ex]
{
  reportError(ex);
  consumeUntil(RBrace);
  consume();
  fatalError = true;
  return null;
}

protected initializerListItem[Vector<Object> v, int nposi, Type type] returns[int npos = 0]
{
  Vector<Object> positions = null;
  Expression     expr      = null;
  Type           ntype     = null;
} :
{
  npos = nposi;
}
   (positions=designation)?
    {
      ntype = findType(type, npos, positions, LT(1));
    }
   expr=initializer[ntype]
    {
      npos = processInitializers(npos, type, v, expr, positions, LT(1));
      positions = null;
    }
 | {allowGNUExtensions}? n1:Identifier Colon
    {
      positions = new Vector<Object>(1);
      positions.addElement(n1.getText());
      ntype = findType(type, npos, positions, LT(1));
    }
   expr=initializer[ntype]
    {
      npos = processInitializers(npos, type, v, expr, positions, LT(1));
      positions = null;
    }
;

protected designation returns [Vector<Object> positions = null] : 
{
  positions = new Vector<Object>(10);
}
   (designator[positions])+ Assign
 ;

protected designator [Vector<Object> v]
{
  Expression expr = null;
} :
   LBracket expr = constantExpression RBracket { v.addElement(expr); }
 | Dot n0:Identifier { v.addElement(n0.getText()); }
 ;

protected statement returns [Statement stmt=errStmt]
{
  Expression expr1 = null;
  Statement  stmt1 = null;
  Token      t     = null;
} :
   s1:KEYWORD_case expr1=constantExpression Colon stmt1=statement
  { stmt = buildCaseStmt(expr1, stmt1, s1); }
 | s2:KEYWORD_default Colon stmt1=statement
  { stmt = buildCaseStmt(null, stmt1, s2); }
 | (Identifier Colon)=> l:Identifier Colon { t = LT(1); } stmt1=statement
    { stmt = buildLabelStmt(l.getText(), stmt1, t); }
 | {allowGNUExtensions}? KEYWORD_label lab:Identifier Semi
    { getLabelDecl(lab.getText()); stmt = new NullStmt(); }
 | stmt=compoundStatement[false]
 | stmt=expressionStatement
 | stmt=selectionStatement
 | stmt=iterationStatement
 | stmt=jumpStatement
  ;

protected compoundStatement[boolean ftn] returns [BlockStmt stmt=errBlkStmt]
{
  Statement   stmti        = null;
  SymtabScope cs           = null;
  boolean     saveTopLevel = topLevel;
  boolean     eStmtFnd     = false;
  BlockStmt   saveStmts    = currentBlockStmt;
} :
{
 saveTopLevel = topLevel;
 topLevel = false;
 stmt = new BlockStmt();
 currentBlockStmt = stmt;
}
  s1:LBrace
    {
      cs = cg.getSymbolTable().beginScope();
      compoundStmtNestedLevel++;
    }
  (
    typedef
  | {!eStmtFnd || allowC99Extensions}? (declarationSpecifiersChk)=> declaration[currentBlockStmt]
  | (KEYWORD_Extension)=> n0:KEYWORD_Extension 
      {
        if (!allowGNUExtensions)
          userWarning(Msg.MSG_Ignored_s, n0.getText(), n0);
      }
  | stmti=statement
      {
        if (stmti != null) {
          currentBlockStmt.addStmt(stmti);
          eStmtFnd = true;
        }
      }
  | p:Pragma { processPragma(p.getText(), p); }
  )*
  RBrace ({allowGNUExtensions}? Semi)?
      {
        finishBlockStmt(currentBlockStmt, ftn, cs, s1);
        currentBlockStmt = saveStmts;
        cg.getSymbolTable().endScope();
        topLevel = saveTopLevel;
        compoundStmtNestedLevel--;
      }
  ;
exception
catch [RecognitionException ex]
{
  reportError(ex);
  consumeUntil(RBrace);
  consume();
  topLevel = saveTopLevel;
  fatalError = true;
  compoundStmtNestedLevel--;
  currentBlockStmt = saveStmts;
  return null;
}

protected expressionStatement returns [Statement stmt=errStmt]
{
  Expression expr   = null;
} :
   (expr = expression)? n0:Semi { stmt = buildExpressionStmt(expr, n0); }
;
exception
catch [RecognitionException ex]
{
  reportError(ex);
  consumeUntil(Semi);
  consume();
  fatalError = true;
  return null;
}

protected selectionStatement returns [Statement stmt=errStmt]
{ 
  Expression expr  = null;
  Statement  stmtt = null;
  Statement  stmtf = null;
  Token      token = null;
} :
   i:KEYWORD_if expr=enclosedExpression
   stmtt=statement ((KEYWORD_else)=> KEYWORD_else stmtf=statement)?
    { stmt = buildIfStmt(expr, stmtt, stmtf, i); }
 | s:KEYWORD_switch expr=enclosedExpression
   stmtt=statement { stmt = buildSwitchStmt(expr, stmtt, s); }
 ;

protected iterationStatement returns [Statement stmt=errStmt]
{ 
  Declaration decl  = null;
  Type        type  = null;
  Expression  expr1 = null;
  Expression  expr2 = null;
  Expression  expr3 = null;
  
  Statement   istmt = null;
} :
    w:KEYWORD_while expr1=enclosedExpression istmt=statement
    { stmt = buildWhileStmt(expr1, istmt, w); }
  | d:KEYWORD_do istmt=statement KEYWORD_while expr1=enclosedExpression Semi
    { stmt = buildDoWhileStmt(expr1, istmt, d); }
  | f:KEYWORD_for LParen
   (
     Semi
   | {allowC99Extensions}? (declarationSpecifiersChk)=> type = declarationSpecifiers decl = declarator[type, 0] Assign expr1 = initializer[type] Semi
     { expr1 = new AssignSimpleOp(type, genDeclAddress(decl), expr1); }
   | expr1=expression Semi
   ) (expr2=expression)? Semi (expr3=expression)? RParen istmt=statement  { stmt = buildForStmt (expr1, expr2, expr3, istmt, f); }
 ;

protected enclosedExpression returns [Expression expr = errExp] :
  n0:LParen expr = expression RParen
;
exception
catch [RecognitionException ex]
{
  reportError(ex);
  consumeUntil(RParen);
  consume();
  fatalError = true;
  return null;
}

protected jumpStatement returns [Statement stmt=errStmt]
{ 
  Expression expr = null;
} :
   g:KEYWORD_goto l:Identifier         Semi { stmt = buildGotoStmt(l.getText(), l); }
 | c:KEYWORD_continue                  Semi { stmt = buildContinueStmt(c); }
 | b:KEYWORD_break                     Semi { stmt = buildBreakStmt(b);    }
 | r:KEYWORD_return (expr=expression)? Semi { stmt = buildReturnStmt(expr, r); }
 ;
exception
catch [RecognitionException ex]
{
  reportError(ex);
  consumeUntil(Semi);
  consume();
  fatalError = true;
  return null;
}

protected typeDeclarator[Type type]
{
  String saveIdentifier = declID;
} :
   type=declarator2[type] ( type = getAttributes[type])?
   { 
     processTypeDeclarator(declID, type, LT(1));
     declID = saveIdentifier;
   }
 ;

protected typedef
{
  Type        type = null;
  Declaration decl = null;
} : 
  KEYWORD_typedef
  type = declarationSpecifiers 
    {
      if (type == null) {
        type = void_type;
        userError(Msg.MSG_Invalid_type, null, LT(1));
      }
    }
   typeDeclarator[type] (Comma typeDeclarator[type])*  Semi
 ;
exception
catch [RecognitionException ex]
{
  reportError(ex);
  consumeUntil(Semi);
  consume();
  fatalError = true;
}

// This rule is needed because the calls to traceIN() and traceOut are
// performed outside of the try clause.

protected translationUnit
{
  FileDecl            root   = null;
  Vector<Declaration> rdecls = null;
  Type                type   = null;
} :
{
  cg.getSymbolTable().beginScope();
  initialize();
  root = new FileDecl(cg.getName());
  rdecls = new Vector<Declaration>(23);
  root.setDecls(rdecls);
  cg.setAST(root);
}
 (
   l:PPLine { processLine(l.getText(), l); }
 | StdArg
 {
   if (!vaListDefined) {
     vaListDefined = true;
     cg.addRootSymbol(defineTypeName("va_list", RefType.create(Machine.currentMachine.getVaListType(), RefAttr.VaList)));
     cg.addRootSymbol(defineTypeName("__va_list", RefType.create(Machine.currentMachine.getVaListType(), RefAttr.VaList)));
     if (allowGNUExtensions) {
       cg.addRootSymbol(defineTypeName("__gnuc_va_list", RefType.create(Machine.currentMachine.getVaListType(), RefAttr.VaList)));
     }
   }
 }
 | p:Pragma { processPragma(p.getText(), p); }
 | er:PPError { userError(Msg.MSG_s, er.getText(), er); }
 | wn:PPWarning { userWarning(Msg.MSG_s, wn.getText(), wn); }
 | (KEYWORD_Extension)=> n0:KEYWORD_Extension 
      {
        if (!allowGNUExtensions)
          userWarning(Msg.MSG_Ignored_s, n0.getText(), n0);
      }
 | typedef
 | ftnOrDecl
 )*
 EOF
 {
   finishTranslationUnit(root, rdecls, LT(0));
 }
 ;
exception
catch [RecognitionException ex]
{
  reportError(ex);
  if (classTrace)
    ex.printStackTrace();
  consumeUntil(EOF);
  throw ex;
}
catch [antlr.TokenStreamRecognitionException ex]
{
  reportError(ex);
  if (classTrace)
    ex.printStackTrace();
  consumeUntil(EOF);
  throw ex;
}

protected ftnOrDecl
{
  Type          type  = int_type;
  Type          dtype = int_type;
  ProcedureType pt    = null;
  Declaration   decl  = null;
  Vector<Declaration> vars  = null;
  Expression    expr  = null;
  int           sc    = 0;
  String        attrs = "";
  Token         t     = null;
} :
{
  declAtts     = "";
  storageClass = cAuto;
}
  (options { warnWhenFollowAmbig=false;} : 
    (declarationSpecifiersChk)=> type = declarationSpecifiers 
      {
        sc = storageClass;
        if (sc == cAuto)
          sc = cGlobal;
      }
    )?
   (options { warnWhenFollowAmbig=false;} : att0:Attributes { declAtts = declAtts + att0.getText(); })*
   (
     Semi
       {
         declAtts = "";
       }         
   | dtype = declarator2[type]

       {
         t = LT(1);
         attrs = declAtts;
         declAtts = "";
         pt = null;
         if (dtype != null) {
           Type ty = dtype.getCoreType();
           if (ty.isProcedureType())
             pt = (ProcedureType) ty;
         }
       }
     (
      Semi
        {
          decl = defineDecl(declID, dtype, sc, t);
          processAttributes(attrs, decl, t);
          vars = new Vector<Declaration>(1);
          vars.add(decl);
          processTopDecls(vars, sc, t);
        }
     | Comma
        {
          decl = defineDecl(declID, dtype, sc, t);
          processAttributes(attrs, decl, t);
          vars = new Vector<Declaration>(4);
          vars.add(decl);
        }
       initDeclaratorList[vars, type, sc] n1:Semi
        {
           processTopDecls(vars, sc, n1);
        }
     | Assign 
        {
          decl = defineDecl(declID, dtype, sc, t);
          processAttributes(attrs, decl, t);
        }
       expr = initializer[dtype]
        {
          processDeclInitializer(decl, dtype, expr, t);
          vars = new Vector<Declaration>(4);
          vars.add(decl);
        }
       (Comma initDeclaratorList[vars, type, sc])? n2:Semi
         {
           processTopDecls(vars, sc, n2);
         }
     | {pt != null}? ftnDef[pt, declID, attrs, sc]
     )
   )
 ;

protected ftnDef[ProcedureType pt, String name, String attrs, int sc]
{
  Token       t    = LT(1);
  BlockStmt   stmt = null;
  RoutineDecl rd   = null;
} :
   (krArg[pt])*
     {
       rd = (RoutineDecl) defineDecl(name, pt, sc, t);
       processAttributes(attrs, rd, t);
       cg.getSymbolTable().beginScope();
       if (!fatalError) {
         int l = pt.numFormals();
         for (int i = 0; i < l; i++)
           cg.addSymbol(pt.getFormal(i));

         rd.setSourceLineNumber(t.getLine());
         currentFunction = rd;
         cpp.setCurrentFtn(rd.getName());
       }
     }
   stmt = compoundStatement[true]
     {
       finishFunction(rd, stmt, sc, t);
       cg.getSymbolTable().endScope();
       labels.clear();
     }
;

protected krArg[ProcedureType pt]
{
  Type       type = null;
  FormalDecl fd   = null;
} :
  type = declarationSpecifiers fd = formalDeclarator[type]
    {
      if (!fatalError) {
        String     name = fd.getName();
        FormalDecl fd2  = pt.getFormal(name);
        if (fd2 == null)
          userError(Msg.MSG_Invalid_parameter_s, name, LT(0));
        fd2.setType(fd.getType());
      }
    }
  (Comma fd = formalDeclarator[type]
    {
      if (!fatalError) {
        String     name = fd.getName();
        FormalDecl fd2  = pt.getFormal(name);
        if (fd2 == null)
          userError(Msg.MSG_Invalid_parameter_s, name, LT(0));
        fd2.setType(fd.getType());
      }
    }
  )*
  Semi
  ;

{
import java.util.Enumeration;
import scale.common.*;
import scale.clef.LiteralMap;
import scale.clef.expr.Literal;
import scale.clef.expr.IntLiteral;
import scale.clef.expr.FloatLiteral;
import scale.clef.expr.StringLiteral;
import scale.clef.type.*;
import scale.clef.decl.*;
import scale.clef.symtab.*;
import scale.callGraph.*;

/** 
 * This class reads in a chacater stream and outputs tokens in the C99 language.
 * <p>
 * $Id: c99.g,v 1.144 2007-09-20 18:49:43 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */
}
class C99Lexer extends Lexer;
options
{
  k              = 3;
  testLiterals   = false;
  charVocabulary = '\u0000'..'\ufffe';
}

{
  private CallGraph cg;
  private CPreprocessor reader;
  private C99Parser parser;
  private boolean   longLongSubtype = false;
  private boolean   longSubtype     = false;
  private boolean   unsignedSubtype = false;
  private boolean   floatSubtype    = false;
  private boolean   intType         = false;
  private boolean   realType        = true;

  public void setCallGraph(CallGraph cg)
  {
    this.cg = cg;
  }

  public void setReader(CPreprocessor reader)
  {
    this.reader = reader;
  }

  public void setParser(C99Parser parser)
  {
    this.parser = parser;
  }

  public void newline()
  {
    setLine(reader.getLineNumber() + 1);
    setColumn(1);
  }

  private void resetSubtypes()
  {
    longSubtype = false;
    longLongSubtype = false;
    unsignedSubtype = false;
    floatSubtype = false;
    intType = false;
    realType = true;
  }

  private int specifyHexFloat()
  {
    if (longSubtype)
      return (HexLongDoubleValue);
    if (floatSubtype)
      return (HexFloatValue);
    return (HexDoubleValue);
  }

  private int specifyHexInt()
  {
    if (unsignedSubtype) {
      if (longLongSubtype)
        return (HexUnsignedLongLongIntValue);
      if (longSubtype)
        return (HexUnsignedLongIntValue);
      return (HexUnsignedIntValue);
    }

    if (longLongSubtype)
      return (HexLongLongIntValue);
    if (longSubtype)
      return (HexLongIntValue);
    return (HexIntValue);
  }

  private int specifyFloat()
  {
    if (longSubtype)
      return (LongDoubleValue);
    if (floatSubtype)
      return (FloatValue);
    return (DoubleValue);
  }

  private int specifyInt()
  {
    if (unsignedSubtype) {
      if (longLongSubtype)
        return (UnsignedLongLongIntValue);
      if (longSubtype)
        return (UnsignedLongIntValue);
      return
        (UnsignedIntValue);
    }

    if (longLongSubtype)
      return (LongLongIntValue);
    if (longSubtype)
      return (LongIntValue);
    return (IntValue);
  }
}

WS   : ('\t' | '\r' { newline(); } | ' ' | '\n' { newline(); }) { $setType(Token.SKIP); };

protected OctalDigit : '0'..'7' ;

protected OctalConstant : '0' (OctalDigit)* ;

protected NonzeroDigit : '1'..'9' ;

protected HexadecimalDigit :
   '0'..'9'
 | 'a'..'f'
 | 'A'..'F'
 ;

protected IntegerSuffix : 
   LongLongSuffix         { longLongSubtype = true; }
 | LongLongUnsignedSuffix { unsignedSubtype = true; longLongSubtype = true; }
 | LongSuffix             { longSubtype     = true; }
 | LongUnsignedSuffix     { unsignedSubtype = true; longSubtype = true; }
 | UnsignedLongLongSuffix { unsignedSubtype = true; longLongSubtype = true; }
 | UnsignedLongSuffix     { unsignedSubtype = true; longSubtype = true; }
 | UnsignedSuffix         { unsignedSubtype = true; }
 ;

protected UnsignedSuffix : "u" | "U" ;

protected UnsignedLongSuffix : "ul" | "Ul" | "uL" | "UL" ;

protected UnsignedLongLongSuffix : "ull" | "Ull" | "uLL" | "ULL" ;

protected LongUnsignedSuffix : "lu" | "lU" | "Lu" | "LU" ;

protected LongLongUnsignedSuffix : "llu" | "llU" | "LLu" | "LLU" ;

protected LongSuffix : "l" | "L"  ;

protected LongLongSuffix : "ll" | "lL" | "Ll" | "LL" ;

protected ExponentPart :
   'e' Exponent
 | 'E' Exponent
 ;

protected Exponent : ('+' | '-')? DigitSequence ;

protected DigitSequence : (Digit)+ ;

protected BinaryExponentPart :
   'P' Exponent
 | 'p' Exponent
 ;

protected FloatingSuffix :
   'f' { floatSubtype = true; }
 | 'F' { floatSubtype = true; }
 | 'l' { longSubtype = true; }
 | 'L' { longSubtype = true; }
 ;

protected HexDoubleValue : ;
protected HexFloatValue : ;
protected HexLongDoubleValue : ;
protected HexUnsignedIntValue : ;
protected HexUnsignedLongIntValue : ;
protected HexUnsignedLongLongIntValue : ;
protected HexIntValue : ;
protected HexLongIntValue : ;
protected HexLongLongIntValue : ;
protected DoubleValue : ;
protected FloatValue : ;
protected LongDoubleValue : ;
protected UnsignedIntValue : ;
protected UnsignedLongIntValue : ;
protected UnsignedLongLongIntValue : ;
protected IntValue : ;
protected LongIntValue : ;
protected LongLongIntValue : ;

Constant  
options
{
  paraphrase = "a constant";
}  :
   (HexPrefix! {resetSubtypes(); }
     (
       '.' (HexadecimalDigit)+ BinaryExponentPart (FloatingSuffix!)?
             {realType = true; $setType(specifyHexFloat()); }
     | (HexadecimalDigit)+
         (
           '.' (HexadecimalDigit)* BinaryExponentPart (FloatingSuffix!)?
             { realType = true; $setType(specifyHexFloat());}
         | BinaryExponentPart (FloatingSuffix!)?
             { realType = true; $setType(specifyHexFloat());}
         | (IntegerSuffix!)?
             { intType = true; $setType(specifyHexInt());}
         )
    )
   )
 | (
     ('.' Digit)=> '.' (Digit)+ (ExponentPart)? {resetSubtypes(); } (FloatingSuffix!)?
      { realType = true; $setType(specifyFloat()); }
   | ((Digit)+ '.')=> (Digit)+ '.' (Digit)* (ExponentPart)? {resetSubtypes(); } (FloatingSuffix!)?
      { realType = true; $setType(specifyFloat()); }
   | ((Digit)+ ('e' | 'E'))=> (Digit)+ ExponentPart {resetSubtypes(); } (FloatingSuffix!)?
      { realType = true; $setType(specifyFloat()); }
   | NonzeroDigit (Digit)* {resetSubtypes(); } (IntegerSuffix!)?
      { intType = true; $setType(specifyInt()); }
   | '0' (OctalDigit)* {resetSubtypes(); } (IntegerSuffix!)?
      { intType = true; $setType(specifyInt()); }
   | ("...")=> "..." {$setType(Varargs);}
   | ('.')=> '.' {$setType(Dot);}
   )
 ;

protected CCharSequence : (CChar)+ ;

protected CChar : 
   EscapeSequence
 | ~('\n' | '\'' | '\\')
 ;

protected EscapeSequence 
options
{
  paraphrase = "a character escape";
} 
{
  char x;
} : '\\'
  (  '\'' { $setText('\''); }
   | '"'  { $setText('"'); }
   | '?' 
   | '\\' { $setText('\\'); }
   | 'a'  { $setText('\007'); }
   | 'b'  { $setText('\b'); }
   | 'e'  { $setText('\033'); }
   | 'f'  { $setText('\f'); }
   | 'n'  { $setText('\n'); }
   | 'r'  { $setText('\r'); }
   | 't'  { $setText('\t'); }
   | 'v'  { $setText('\013'); }
   | 'x' HexadecimalDigit ((HexadecimalDigit)=> HexadecimalDigit)?
     {
       String str = $getText;
       x=((char)Integer.parseInt(str.substring(2), 16)); $setText(x);
     }
   | 'u' HexQuad ((HexQuad)=> (HexQuad))?
   | OctalDigit ((OctalDigit)=> OctalDigit ((OctalDigit)=> OctalDigit)?)?
     {
       String str = $getText;
       x=(char)Integer.parseInt(str.substring(1), 8); $setText(x);
     }
  )
 ;

CharacterConstant : '\''! CCharSequence '\''! ;
WideCharacterConstant : 'L'! '\''! CCharSequence '\''! ;

StringLit 
options
{
  paraphrase = "a string literal";
} : '"'! (SChar)* '"'!
 {
   String str = $getText;
   $setText(str + "\0");
 }
 ;

WideStringLiteral 
options
{
  paraphrase = "a wide string literal";
} :
 'L'! '"'! (SChar)* '"'!
 {
   String str = $getText;
   $setText(str + "\0");
 }
 ;

protected SChar 
options
{
  paraphrase = "a string character";
} :
   EscapeSequence
 | ~('"' | '\\')
 ;

protected HexPrefix : "0x" | "0X" ;

LBrace
options
{
  paraphrase = "open '{'";
} : '{' ;
RBrace
options
{
  paraphrase = "close '}'";
}    : '}' ;
LBracket 
options
{
  paraphrase = "open '['";
} : '[' ;
RBracket 
options
{
  paraphrase = "close ']'";
}    : ']' ;
LParen   
options
{
  paraphrase = "open '('";
} : '(' ;
RParen    
options
{
  paraphrase = "close ')'";
}    : ')' ;
Select   : "->" ;
Inc      : "++" ;
Dec      : "--" ;
And      : '&' ;
Mult     : '*' ;
Plus     : '+' ;
Sub      : '-' ;
Comp     : '~' ;
Not      : '!' ;
Slash    : '/' ;
Mod      : '%' ;
LShift   : "<<" ;
RShift   : ">>" ;
LAngle   : '<' ;
RAngle   : '>' ;
LEqual   : "<=" ;
GEqual   : ">=" ;
Equal    : "==" ;
NEqual   : "!=" ;
Xor      : '^' ;
AndCond  : "&&" ;
OrCond   : "||" ;
Or       : '|' ;
QMark    : '?' ;
Colon    : ':' ;
Semi      
options
{
  paraphrase = "semi-colon";
}    : ';' ;
Assign   : '=' ;
MultAssign : "*=" ;
SlashAssign  : "/=" ;
ModAssign  : "%=" ;
PlusAssign : "+=" ;
SubAssign  : "-=" ;
LSAssign   : "<<=" ;
RSAssign   : ">>=" ;
AndAssign  : "&=" ;
XorAssign  : "^=" ;
OrAssign   : "|=" ;
Comma      : ',' ;
Hatch      : '#' ;
DHatch     : "##" ;
LColon     : "<:" ;
RColon     : ":>" ;
LMod       : "<%" ;
RMod       : ">%" ;
MColon     : "%:" ;
MCMColon   : "%:%:" ;

PPLine :
   "#line" (' ')+ l:LineNumber (' ')+ '"' f:Text1 '"' '\n' { newline(); }
  {
    setLine(Integer.parseInt(l.getText()) + 1);
    parser.setFilename(f.getText());
    setText(" ");
    $setType(Token.SKIP);
  }
  ;

StdArg :
  "#include" (' ')*  '<' (' ')* ("stdarg.h" | "va_list.h") (' ')* '>' (' ')* '\n'
   { newline(); }
 ;


PPError :
 "#error"
 (
    '\n' { newline(); setText(""); }
 |  ' ' f:Text2 '\n' { newline(); setText(f.getText()); }
 )
;

PPWarning :
 "#warning"
 (
    '\n' { newline(); setText(""); }
 |  ' ' f:Text2 '\n' { newline(); setText(f.getText()); }
 )
;

Pragma :
   "#pragma"
  (
   '\n' { newline(); setText(""); }
  | ' ' f:Text2 '\n' { newline(); setText(f.getText()); }
  )
;

protected LineNumber : (Digit)+ ;
protected Text1 : (~('\n' | '"'))* ;
protected Text2 : (~('\n'))* ;
protected Dot     : ;
protected Varargs : ;
protected Attributes : ;

Identifier
options {
  paraphrase = "an identifier";
  testLiterals=true;
} :
  ("__attribute__")=> "__attribute__" (WS)* "((" Att "))"
    {
      $setType(Attributes);
    }
  | IdentifierNondigit (IdentifierNondigit | Digit)*
  ;

protected Att
options {
  paraphrase = "an attribute";
} 
{
  int cnt = 0;
} : 
  (
    '(' {cnt++;}
   | {cnt > 0}? ')' {cnt--;}
   | ~('(' | ')')
  )*
  ;

protected IdentifierNondigit :
   Nondigit
 | UniversalCharacterName
 ;

protected Nondigit :
   '_'
 | 'a'..'z'
 | 'A'..'Z'
 ;

protected Digit : '0'..'9' ;

protected UniversalCharacterName : "\\u" HexQuad ((HexQuad)=> (HexQuad))? ;

protected HexQuad : HexadecimalDigit HexadecimalDigit HexadecimalDigit HexadecimalDigit ;
