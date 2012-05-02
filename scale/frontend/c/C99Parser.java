// $ANTLR 2.7.4: "c99.g" -> "C99Parser.java"$

package scale.frontend.c;

import antlr.TokenBuffer;
import antlr.TokenStreamException;
import antlr.TokenStreamIOException;
import antlr.ANTLRException;
import antlr.LLkParser;
import antlr.Token;
import antlr.TokenStream;
import antlr.RecognitionException;
import antlr.NoViableAltException;
import antlr.MismatchedTokenException;
import antlr.SemanticException;
import antlr.ParserSharedInputState;
import antlr.collections.impl.BitSet;

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

public class C99Parser extends antlr.LLkParser       implements C99ParserTokenTypes
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

protected C99Parser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public C99Parser(TokenBuffer tokenBuf) {
  this(tokenBuf,2);
}

protected C99Parser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public C99Parser(TokenStream lexer) {
  this(lexer,2);
}

public C99Parser(ParserSharedInputState state) {
  super(state,2);
  tokenNames = _tokenNames;
}

	protected final Expression  primaryExpression() throws RecognitionException, TokenStreamException {
		Expression exp=errExp;
		
		Token  n00 = null;
		Token  n01 = null;
		Token  n02 = null;
		Token  n03 = null;
		Token  n04 = null;
		Token  n05 = null;
		Token  n06 = null;
		Token  n07 = null;
		Token  n08 = null;
		Token  n09 = null;
		Token  n10 = null;
		Token  n11 = null;
		Token  n12 = null;
		Token  n13 = null;
		Token  n14 = null;
		Token  n15 = null;
		Token  n16 = null;
		Token  n17 = null;
		Token  n18 = null;
		Token  n19 = null;
		Token  n22 = null;
		Token  n20 = null;
		Token  n21 = null;
		Token  n23 = null;
		Token  n24 = null;
		
		BlockStmt s;
		String    t = null;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case Identifier:
			{
				n00 = LT(1);
				match(Identifier);
				if ( inputState.guessing==0 ) {
					exp = buildVariableRef(n00.getText(), n00);
				}
				break;
			}
			case HexDoubleValue:
			{
				n01 = LT(1);
				match(HexDoubleValue);
				if ( inputState.guessing==0 ) {
					exp = buildFloatLiteral(parseHexDouble(n01.getText()), double_type);
				}
				break;
			}
			case HexFloatValue:
			{
				n02 = LT(1);
				match(HexFloatValue);
				if ( inputState.guessing==0 ) {
					exp = buildFloatLiteral(parseHexDouble(n02.getText()), float_type);
				}
				break;
			}
			case HexLongDoubleValue:
			{
				n03 = LT(1);
				match(HexLongDoubleValue);
				if ( inputState.guessing==0 ) {
					exp = buildFloatLiteral(parseHexDouble(n03.getText()), long_double_type);
				}
				break;
			}
			case HexUnsignedIntValue:
			{
				n04 = LT(1);
				match(HexUnsignedIntValue);
				if ( inputState.guessing==0 ) {
					exp = buildIntLiteral(parseHex(n04.getText()), true, unsigned_int_type);
				}
				break;
			}
			case HexUnsignedLongIntValue:
			{
				n05 = LT(1);
				match(HexUnsignedLongIntValue);
				if ( inputState.guessing==0 ) {
					exp = buildIntLiteral(parseHex(n05.getText()), true, unsigned_long_type);
				}
				break;
			}
			case HexUnsignedLongLongIntValue:
			{
				n06 = LT(1);
				match(HexUnsignedLongLongIntValue);
				if ( inputState.guessing==0 ) {
					exp = buildIntLiteral(parseHex(n06.getText()), true, unsigned_long_long_type);
				}
				break;
			}
			case HexIntValue:
			{
				n07 = LT(1);
				match(HexIntValue);
				if ( inputState.guessing==0 ) {
					exp = buildIntLiteral(parseHex(n07.getText()), true, int_type);
				}
				break;
			}
			case HexLongIntValue:
			{
				n08 = LT(1);
				match(HexLongIntValue);
				if ( inputState.guessing==0 ) {
					exp = buildIntLiteral(parseHex(n08.getText()), true, long_type);
				}
				break;
			}
			case HexLongLongIntValue:
			{
				n09 = LT(1);
				match(HexLongLongIntValue);
				if ( inputState.guessing==0 ) {
					exp = buildIntLiteral(parseHex(n09.getText()), true, long_long_type);
				}
				break;
			}
			case DoubleValue:
			{
				n10 = LT(1);
				match(DoubleValue);
				if ( inputState.guessing==0 ) {
					exp = buildFloatLiteral(java.lang.Double.parseDouble(n10.getText()), double_type);
				}
				break;
			}
			case FloatValue:
			{
				n11 = LT(1);
				match(FloatValue);
				if ( inputState.guessing==0 ) {
					exp = buildFloatLiteral(java.lang.Double.parseDouble(n11.getText()), double_type);
				}
				break;
			}
			case LongDoubleValue:
			{
				n12 = LT(1);
				match(LongDoubleValue);
				if ( inputState.guessing==0 ) {
					exp = buildFloatLiteral(java.lang.Double.parseDouble(n12.getText()), long_double_type);
				}
				break;
			}
			case UnsignedIntValue:
			{
				n13 = LT(1);
				match(UnsignedIntValue);
				if ( inputState.guessing==0 ) {
					exp = buildIntLiteral(parseLong(n13.getText()), false, unsigned_int_type);
				}
				break;
			}
			case UnsignedLongIntValue:
			{
				n14 = LT(1);
				match(UnsignedLongIntValue);
				if ( inputState.guessing==0 ) {
					exp = buildIntLiteral(parseLong(n14.getText()), false, unsigned_long_type);
				}
				break;
			}
			case UnsignedLongLongIntValue:
			{
				n15 = LT(1);
				match(UnsignedLongLongIntValue);
				if ( inputState.guessing==0 ) {
					exp = buildIntLiteral(parseLong(n15.getText()), false, unsigned_long_long_type);
				}
				break;
			}
			case IntValue:
			{
				n16 = LT(1);
				match(IntValue);
				if ( inputState.guessing==0 ) {
					exp = buildIntLiteral(parseLong(n16.getText()), false, int_type);
				}
				break;
			}
			case LongIntValue:
			{
				n17 = LT(1);
				match(LongIntValue);
				if ( inputState.guessing==0 ) {
					exp = buildIntLiteral(parseLong(n17.getText()), false, long_type);
				}
				break;
			}
			case LongLongIntValue:
			{
				n18 = LT(1);
				match(LongLongIntValue);
				if ( inputState.guessing==0 ) {
					exp = buildIntLiteral(parseLong(n18.getText()), false, long_long_type);
				}
				break;
			}
			case CharacterConstant:
			{
				n19 = LT(1);
				match(CharacterConstant);
				if ( inputState.guessing==0 ) {
					exp = LiteralMap.put((int) n19.getText().charAt(0), char_type);
				}
				break;
			}
			case WideCharacterConstant:
			{
				n22 = LT(1);
				match(WideCharacterConstant);
				if ( inputState.guessing==0 ) {
					exp = LiteralMap.put(0xffff & n22.getText().charAt(0), wchar_type);
				}
				break;
			}
			case StringLit:
			{
				n20 = LT(1);
				match(StringLit);
				if ( inputState.guessing==0 ) {
					t = n20.getText();
				}
				{
				_loop3:
				do {
					if ((LA(1)==StringLit)) {
						n21 = LT(1);
						match(StringLit);
						if ( inputState.guessing==0 ) {
							t  = t.substring(0, t.length() - 1) + n21.getText();
						}
					}
					else {
						break _loop3;
					}
					
				} while (true);
				}
				if ( inputState.guessing==0 ) {
					exp = buildStringExpression(t, n20);
				}
				break;
			}
			case WideStringLiteral:
			{
				n23 = LT(1);
				match(WideStringLiteral);
				if ( inputState.guessing==0 ) {
					t = n23.getText();
				}
				{
				_loop5:
				do {
					if ((LA(1)==StringLit)) {
						n24 = LT(1);
						match(StringLit);
						if ( inputState.guessing==0 ) {
							t  = t.substring(0, t.length() - 1) + n24.getText();
						}
					}
					else {
						break _loop5;
					}
					
				} while (true);
				}
				if ( inputState.guessing==0 ) {
					exp = buildWStringExpression(t, n23);
				}
				break;
			}
			default:
				boolean synPredMatched7 = false;
				if ((((LA(1)==KEYWORD_Extension) && (LA(2)==LParen))&&(allowGNUExtensions))) {
					int _m7 = mark();
					synPredMatched7 = true;
					inputState.guessing++;
					try {
						{
						match(KEYWORD_Extension);
						match(LParen);
						match(LBrace);
						}
					}
					catch (RecognitionException pe) {
						synPredMatched7 = false;
					}
					rewind(_m7);
					inputState.guessing--;
				}
				if ( synPredMatched7 ) {
					match(KEYWORD_Extension);
					match(LParen);
					s=compoundStatement(false);
					match(RParen);
					if ( inputState.guessing==0 ) {
						exp = buildStmtExpression(s, LT(1));
					}
				}
				else {
					boolean synPredMatched9 = false;
					if ((((LA(1)==LParen) && (LA(2)==LBrace))&&(allowGNUExtensions))) {
						int _m9 = mark();
						synPredMatched9 = true;
						inputState.guessing++;
						try {
							{
							match(LParen);
							match(LBrace);
							}
						}
						catch (RecognitionException pe) {
							synPredMatched9 = false;
						}
						rewind(_m9);
						inputState.guessing--;
					}
					if ( synPredMatched9 ) {
						match(LParen);
						s=compoundStatement(false);
						match(RParen);
						if ( inputState.guessing==0 ) {
							exp = buildStmtExpression(s, LT(1));
						}
					}
					else if (((LA(1)==KEYWORD_Extension) && (LA(2)==LParen))&&(allowGNUExtensions)) {
						match(KEYWORD_Extension);
						match(LParen);
						exp=expression();
						match(RParen);
					}
					else if ((LA(1)==LParen) && (_tokenSet_0.member(LA(2)))) {
						match(LParen);
						exp=expression();
						match(RParen);
					}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				}}
			}
			catch (RecognitionException ex) {
				if (inputState.guessing==0) {
					reportError(ex);
					consume();
					consumeUntil(_tokenSet_1);
				} else {
				  throw ex;
				}
			}
			return exp;
		}
		
	protected final BlockStmt  compoundStatement(
		boolean ftn
	) throws RecognitionException, TokenStreamException {
		BlockStmt stmt=errBlkStmt;
		
		Token  s1 = null;
		Token  n0 = null;
		Token  p = null;
		
		Statement   stmti        = null;
		SymtabScope cs           = null;
		boolean     saveTopLevel = topLevel;
		boolean     eStmtFnd     = false;
		BlockStmt   saveStmts    = currentBlockStmt;
		
		
		try {      // for error handling
			if ( inputState.guessing==0 ) {
				
				saveTopLevel = topLevel;
				topLevel = false;
				stmt = new BlockStmt();
				currentBlockStmt = stmt;
				
			}
			s1 = LT(1);
			match(LBrace);
			if ( inputState.guessing==0 ) {
				
				cs = cg.getSymbolTable().beginScope();
				compoundStmtNestedLevel++;
				
			}
			{
			_loop245:
			do {
				switch ( LA(1)) {
				case KEYWORD_typedef:
				{
					typedef();
					break;
				}
				case Pragma:
				{
					p = LT(1);
					match(Pragma);
					if ( inputState.guessing==0 ) {
						processPragma(p.getText(), p);
					}
					break;
				}
				default:
					boolean synPredMatched242 = false;
					if ((((_tokenSet_2.member(LA(1))) && (_tokenSet_3.member(LA(2))))&&(!eStmtFnd || allowC99Extensions))) {
						int _m242 = mark();
						synPredMatched242 = true;
						inputState.guessing++;
						try {
							{
							declarationSpecifiersChk();
							}
						}
						catch (RecognitionException pe) {
							synPredMatched242 = false;
						}
						rewind(_m242);
						inputState.guessing--;
					}
					if ( synPredMatched242 ) {
						declaration(currentBlockStmt);
					}
					else {
						boolean synPredMatched244 = false;
						if (((LA(1)==KEYWORD_Extension) && (_tokenSet_4.member(LA(2))))) {
							int _m244 = mark();
							synPredMatched244 = true;
							inputState.guessing++;
							try {
								{
								match(KEYWORD_Extension);
								}
							}
							catch (RecognitionException pe) {
								synPredMatched244 = false;
							}
							rewind(_m244);
							inputState.guessing--;
						}
						if ( synPredMatched244 ) {
							n0 = LT(1);
							match(KEYWORD_Extension);
							if ( inputState.guessing==0 ) {
								
								if (!allowGNUExtensions)
								userWarning(Msg.MSG_Ignored_s, n0.getText(), n0);
								
							}
						}
						else if ((_tokenSet_5.member(LA(1))) && (_tokenSet_6.member(LA(2)))) {
							stmti=statement();
							if ( inputState.guessing==0 ) {
								
								if (stmti != null) {
								currentBlockStmt.addStmt(stmti);
								eStmtFnd = true;
								}
								
							}
						}
					else {
						break _loop245;
					}
					}}
				} while (true);
				}
				match(RBrace);
				{
				if (((LA(1)==Semi) && (_tokenSet_7.member(LA(2))))&&(allowGNUExtensions)) {
					match(Semi);
				}
				else if ((_tokenSet_7.member(LA(1))) && (_tokenSet_8.member(LA(2)))) {
				}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				
				}
				if ( inputState.guessing==0 ) {
					
					finishBlockStmt(currentBlockStmt, ftn, cs, s1);
					currentBlockStmt = saveStmts;
					cg.getSymbolTable().endScope();
					topLevel = saveTopLevel;
					compoundStmtNestedLevel--;
					
				}
			}
			catch (RecognitionException ex) {
				if (inputState.guessing==0) {
					
					reportError(ex);
					consumeUntil(RBrace);
					consume();
					topLevel = saveTopLevel;
					fatalError = true;
					compoundStmtNestedLevel--;
					currentBlockStmt = saveStmts;
					return null;
					
				} else {
					throw ex;
				}
			}
			return stmt;
		}
		
	protected final Expression  expression() throws RecognitionException, TokenStreamException {
		Expression exp=errExp;
		
		
		Expression expr;
		
		
		try {      // for error handling
			exp=assignmentExpression();
			{
			_loop85:
			do {
				if ((LA(1)==Comma)) {
					match(Comma);
					expr=assignmentExpression();
					if ( inputState.guessing==0 ) {
						exp = new SeriesOp(exp, expr);
					}
				}
				else {
					break _loop85;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_9);
			} else {
			  throw ex;
			}
		}
		return exp;
	}
	
	protected final Expression  postfixExpression() throws RecognitionException, TokenStreamException {
		Expression exp=errExp;
		
		Token  n0 = null;
		Token  n1 = null;
		Token  n2 = null;
		Token  n3 = null;
		
		Expression expr0 = null;
		Expression expr1 = null;
		Type       ty    = null;
		Vector<Expression> args  = null;
		
		
		try {      // for error handling
			{
			boolean synPredMatched13 = false;
			if (((LA(1)==LParen) && (_tokenSet_10.member(LA(2))))) {
				int _m13 = mark();
				synPredMatched13 = true;
				inputState.guessing++;
				try {
					{
					match(LParen);
					typeName();
					match(RParen);
					match(LBrace);
					}
				}
				catch (RecognitionException pe) {
					synPredMatched13 = false;
				}
				rewind(_m13);
				inputState.guessing--;
			}
			if ( synPredMatched13 ) {
				match(LParen);
				ty=typeName();
				match(RParen);
				exp=aggregationExpr(ty);
			}
			else {
				boolean synPredMatched15 = false;
				if (((LA(1)==Identifier) && (LA(2)==LParen))) {
					int _m15 = mark();
					synPredMatched15 = true;
					inputState.guessing++;
					try {
						{
						match(Identifier);
						match(LParen);
						}
					}
					catch (RecognitionException pe) {
						synPredMatched15 = false;
					}
					rewind(_m15);
					inputState.guessing--;
				}
				if ( synPredMatched15 ) {
					exp=ftnCall();
				}
				else {
					boolean synPredMatched18 = false;
					if (((LA(1)==KEYWORD_va_arg||LA(1)==KEYWORD_builtin_va_arg))) {
						int _m18 = mark();
						synPredMatched18 = true;
						inputState.guessing++;
						try {
							{
							{
							switch ( LA(1)) {
							case KEYWORD_va_arg:
							{
								match(KEYWORD_va_arg);
								break;
							}
							case KEYWORD_builtin_va_arg:
							{
								match(KEYWORD_builtin_va_arg);
								break;
							}
							default:
							{
								throw new NoViableAltException(LT(1), getFilename());
							}
							}
							}
							match(LParen);
							}
						}
						catch (RecognitionException pe) {
							synPredMatched18 = false;
						}
						rewind(_m18);
						inputState.guessing--;
					}
					if ( synPredMatched18 ) {
						exp=vaArg();
					}
					else if ((_tokenSet_11.member(LA(1))) && (_tokenSet_12.member(LA(2)))) {
						exp=primaryExpression();
					}
					else {
						throw new NoViableAltException(LT(1), getFilename());
					}
					}}
					}
					if ( inputState.guessing==0 ) {
						expr0 = exp;
					}
					{
					_loop21:
					do {
						switch ( LA(1)) {
						case Dot:
						{
							match(Dot);
							n0 = LT(1);
							match(Identifier);
							if ( inputState.guessing==0 ) {
								expr0 = buildFieldRef(false, n0.getText(), expr0, n0);
							}
							break;
						}
						case Select:
						{
							match(Select);
							n1 = LT(1);
							match(Identifier);
							if ( inputState.guessing==0 ) {
								expr0 = buildFieldRef(true, n1.getText(), expr0, n1);
							}
							break;
						}
						case LBracket:
						{
							n2 = LT(1);
							match(LBracket);
							expr1=expression();
							match(RBracket);
							if ( inputState.guessing==0 ) {
								expr0 = buildSubscriptValue(expr0, expr1, n2);
							}
							break;
						}
						case LParen:
						{
							n3 = LT(1);
							match(LParen);
							{
							switch ( LA(1)) {
							case KEYWORD_sizeof:
							case KEYWORD_Extension:
							case KEYWORD_alignof:
							case KEYWORD_va_arg:
							case KEYWORD_builtin_va_arg:
							case KEYWORD_builtin_offsetof:
							case Identifier:
							case HexDoubleValue:
							case HexFloatValue:
							case HexLongDoubleValue:
							case HexUnsignedIntValue:
							case HexUnsignedLongIntValue:
							case HexUnsignedLongLongIntValue:
							case HexIntValue:
							case HexLongIntValue:
							case HexLongLongIntValue:
							case DoubleValue:
							case FloatValue:
							case LongDoubleValue:
							case UnsignedIntValue:
							case UnsignedLongIntValue:
							case UnsignedLongLongIntValue:
							case IntValue:
							case LongIntValue:
							case LongLongIntValue:
							case CharacterConstant:
							case WideCharacterConstant:
							case StringLit:
							case WideStringLiteral:
							case LParen:
							case Dec:
							case Inc:
							case And:
							case Mult:
							case Plus:
							case Sub:
							case Comp:
							case Not:
							{
								args=argumentExpressionList();
								break;
							}
							case RParen:
							{
								break;
							}
							default:
							{
								throw new NoViableAltException(LT(1), getFilename());
							}
							}
							}
							match(RParen);
							if ( inputState.guessing==0 ) {
								expr0 = buildCall(expr0, args, n3);
							}
							break;
						}
						case Dec:
						{
							match(Dec);
							if ( inputState.guessing==0 ) {
								expr0 = new PostDecrementOp(expr0.getType(), makeLValue(expr0));
							}
							break;
						}
						case Inc:
						{
							match(Inc);
							if ( inputState.guessing==0 ) {
								expr0 = new PostIncrementOp(expr0.getType(), makeLValue(expr0));
							}
							break;
						}
						default:
						{
							break _loop21;
						}
						}
					} while (true);
					}
					if ( inputState.guessing==0 ) {
						exp = expr0;
					}
				}
				catch (RecognitionException ex) {
					if (inputState.guessing==0) {
						reportError(ex);
						consume();
						consumeUntil(_tokenSet_13);
					} else {
					  throw ex;
					}
				}
				return exp;
			}
			
	protected final Type  typeName() throws RecognitionException, TokenStreamException {
		Type type = void_type;
		
		
		try {      // for error handling
			type=specifierQualifierList();
			{
			switch ( LA(1)) {
			case LParen:
			case LBracket:
			case Mult:
			{
				type=abstractDeclarator(type);
				break;
			}
			case RParen:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_14);
			} else {
			  throw ex;
			}
		}
		return type;
	}
	
	protected final Expression  aggregationExpr(
		Type type
	) throws RecognitionException, TokenStreamException {
		Expression exp = errExp;
		
		
		Vector<Object> v = null;
		
		
		try {      // for error handling
			v=initializerList(type);
			if ( inputState.guessing==0 ) {
				
				if (allConstants(v)) {
				exp = buildAggregation(type, v, LT(1));
				} else {
				VariableDecl decl = genTemp(type.getNonConstType());
				exp = buildSeries(decl, v, LT(1));
				cg.addSymbol(decl);
				}
				
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_1);
			} else {
			  throw ex;
			}
		}
		return exp;
	}
	
	protected final Expression  ftnCall() throws RecognitionException, TokenStreamException {
		Expression exp=errExp;
		
		Token  n00 = null;
		
		Vector<Expression> args = null;
		Type       ty   = null;
		Expression call = null;
		
		
		try {      // for error handling
			n00 = LT(1);
			match(Identifier);
			match(LParen);
			{
			if (((_tokenSet_2.member(LA(1))) && (_tokenSet_15.member(LA(2))))&&(n00.getText().equals("__builtin_isfloat"))) {
				ty=singleType();
				match(RParen);
				if ( inputState.guessing==0 ) {
					call = ty.isRealType() ? one : zero;
				}
			}
			else if ((_tokenSet_16.member(LA(1))) && (_tokenSet_17.member(LA(2)))) {
				{
				switch ( LA(1)) {
				case KEYWORD_sizeof:
				case KEYWORD_Extension:
				case KEYWORD_alignof:
				case KEYWORD_va_arg:
				case KEYWORD_builtin_va_arg:
				case KEYWORD_builtin_offsetof:
				case Identifier:
				case HexDoubleValue:
				case HexFloatValue:
				case HexLongDoubleValue:
				case HexUnsignedIntValue:
				case HexUnsignedLongIntValue:
				case HexUnsignedLongLongIntValue:
				case HexIntValue:
				case HexLongIntValue:
				case HexLongLongIntValue:
				case DoubleValue:
				case FloatValue:
				case LongDoubleValue:
				case UnsignedIntValue:
				case UnsignedLongIntValue:
				case UnsignedLongLongIntValue:
				case IntValue:
				case LongIntValue:
				case LongLongIntValue:
				case CharacterConstant:
				case WideCharacterConstant:
				case StringLit:
				case WideStringLiteral:
				case LParen:
				case Dec:
				case Inc:
				case And:
				case Mult:
				case Plus:
				case Sub:
				case Comp:
				case Not:
				{
					args=argumentExpressionList();
					break;
				}
				case RParen:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				match(RParen);
				if ( inputState.guessing==0 ) {
					
					if (call == null)
					exp = buildCall(n00.getText(), args, n00);
					else
					exp = call;
					
				}
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_1);
			} else {
			  throw ex;
			}
		}
		return exp;
	}
	
	protected final Expression  vaArg() throws RecognitionException, TokenStreamException {
		Expression exp = errExp;
		
		
		Type ty;
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case KEYWORD_va_arg:
			{
				match(KEYWORD_va_arg);
				break;
			}
			case KEYWORD_builtin_va_arg:
			{
				match(KEYWORD_builtin_va_arg);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(LParen);
			exp=conditionalExpression();
			match(Comma);
			ty=singleType();
			if ( inputState.guessing==0 ) {
				
				if (exp instanceof IdReferenceOp)
				((IdReferenceOp) exp).getDecl().setAddressTaken();
				exp = new VaArgOp(ty, makeLValue(exp));
				
			}
			match(RParen);
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_1);
			} else {
			  throw ex;
			}
		}
		return exp;
	}
	
	protected final Vector<Expression>  argumentExpressionList() throws RecognitionException, TokenStreamException {
		Vector<Expression> v = null;
		
		
		Expression expr1, expr2;
		
		
		try {      // for error handling
			if ( inputState.guessing==0 ) {
				
				v = new Vector<Expression>(4);
				
			}
			expr1=assignmentExpression();
			if ( inputState.guessing==0 ) {
				
				if (expr1 != null)
				v.addElement(expr1);
				
			}
			{
			_loop29:
			do {
				if ((LA(1)==Comma)) {
					match(Comma);
					expr2=assignmentExpression();
					if ( inputState.guessing==0 ) {
						
						if (expr2 != null)
						v.addElement(expr2);
						
					}
				}
				else {
					break _loop29;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_14);
			} else {
			  throw ex;
			}
		}
		return v;
	}
	
	protected final Type  singleType() throws RecognitionException, TokenStreamException {
		Type type = void_type;
		
		
		String attributes        = null;
		int    saveStorageClass  = storageClass;
		
		
		try {      // for error handling
			type=declarationSpecifiers();
			if ( inputState.guessing==0 ) {
				
				storageClass  = saveStorageClass;
				
			}
			{
			switch ( LA(1)) {
			case LParen:
			case LBracket:
			case Mult:
			{
				type=abstractDeclarator(type);
				break;
			}
			case RParen:
			case Comma:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				
				storageClass  = saveStorageClass;
				reportError(ex);
				consume();
				fatalError = true;
				
			} else {
				throw ex;
			}
		}
		return type;
	}
	
	protected final Expression  conditionalExpression() throws RecognitionException, TokenStreamException {
		Expression exp=errExp;
		
		Token  n0 = null;
		
		Expression expr2;
		Expression expr3;
		
		
		try {      // for error handling
			exp=orConditionExpression();
			{
			switch ( LA(1)) {
			case QMark:
			{
				n0 = LT(1);
				match(QMark);
				expr2=expression();
				match(Colon);
				expr3=conditionalExpression();
				if ( inputState.guessing==0 ) {
					exp = buildExpressionIf(exp, expr2, expr3, n0);
				}
				break;
			}
			case RParen:
			case RBracket:
			case Comma:
			case Colon:
			case Assign:
			case MultAssign:
			case SlashAssign:
			case ModAssign:
			case PlusAssign:
			case SubAssign:
			case AndAssign:
			case OrAssign:
			case XorAssign:
			case LSAssign:
			case RSAssign:
			case Semi:
			case Attributes:
			case RBrace:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_18);
			} else {
			  throw ex;
			}
		}
		return exp;
	}
	
	protected final Expression  assignmentExpression() throws RecognitionException, TokenStreamException {
		Expression exp=errExp;
		
		
		int        op  = 0;
		Expression rhs = null;
		Token      t   = null;
		
		
		try {      // for error handling
			exp=conditionalExpression();
			{
			switch ( LA(1)) {
			case Assign:
			case MultAssign:
			case SlashAssign:
			case ModAssign:
			case PlusAssign:
			case SubAssign:
			case AndAssign:
			case OrAssign:
			case XorAssign:
			case LSAssign:
			case RSAssign:
			{
				if ( inputState.guessing==0 ) {
					t = LT(1);
				}
				op=assignmentOperator();
				rhs=assignmentExpression();
				if ( inputState.guessing==0 ) {
					exp = buildAssignment(op, exp, rhs, t);
				}
				break;
			}
			case RParen:
			case RBracket:
			case Comma:
			case Colon:
			case Semi:
			case RBrace:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_19);
			} else {
			  throw ex;
			}
		}
		return exp;
	}
	
	protected final Expression  unaryExpression() throws RecognitionException, TokenStreamException {
		Expression exp=errExp;
		
		Token  n0 = null;
		Token  n1 = null;
		Token  n2 = null;
		Token  n3 = null;
		Token  n4 = null;
		Token  n5 = null;
		Token  n6 = null;
		Token  n7 = null;
		Token  n8 = null;
		Token  n9 = null;
		Token  n10 = null;
		Token  l1 = null;
		
		Expression expr;
		Type       type;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case Inc:
			{
				n0 = LT(1);
				match(Inc);
				expr=unaryExpression();
				if ( inputState.guessing==0 ) {
					exp = new PreIncrementOp(expr.getType(), makeLValue(expr));
				}
				break;
			}
			case Dec:
			{
				n1 = LT(1);
				match(Dec);
				expr=unaryExpression();
				if ( inputState.guessing==0 ) {
					exp = new PreDecrementOp(expr.getType(), makeLValue(expr));
				}
				break;
			}
			case And:
			{
				n2 = LT(1);
				match(And);
				expr=castExpression();
				if ( inputState.guessing==0 ) {
					exp = buildAddressOf(expr, n2);
				}
				break;
			}
			case Mult:
			{
				n3 = LT(1);
				match(Mult);
				expr=castExpression();
				if ( inputState.guessing==0 ) {
					exp = buildDereferenceOp(expr, n3);
				}
				break;
			}
			case Plus:
			{
				n4 = LT(1);
				match(Plus);
				expr=castExpression();
				if ( inputState.guessing==0 ) {
					exp = expr;
				}
				break;
			}
			case Sub:
			{
				n5 = LT(1);
				match(Sub);
				expr=castExpression();
				if ( inputState.guessing==0 ) {
					exp = new NegativeOp(expr.getType(), expr);
				}
				break;
			}
			case Comp:
			{
				n6 = LT(1);
				match(Comp);
				expr=castExpression();
				if ( inputState.guessing==0 ) {
					exp = new BitComplementOp(expr.getType(), expr);
				}
				break;
			}
			case Not:
			{
				n7 = LT(1);
				match(Not);
				expr=castExpression();
				if ( inputState.guessing==0 ) {
					exp = equal0(expr, LT(1));
				}
				break;
			}
			case KEYWORD_sizeof:
			{
				n8 = LT(1);
				match(KEYWORD_sizeof);
				exp=sizeof();
				break;
			}
			case KEYWORD_builtin_offsetof:
			{
				n9 = LT(1);
				match(KEYWORD_builtin_offsetof);
				exp=offsetof();
				break;
			}
			case KEYWORD_alignof:
			{
				n10 = LT(1);
				match(KEYWORD_alignof);
				l1 = LT(1);
				match(LParen);
				type=singleType();
				match(RParen);
				if ( inputState.guessing==0 ) {
					
					if (strictANSIC)
					userWarning(Msg.MSG_alignof_is_non_standard, null, l1);
					if (!allowGNUExtensions)
					userWarning(Msg.MSG_alignof_is_non_standard, null, l1);
					
					int al = type.alignment(Machine.currentMachine);
					exp = LiteralMap.put(al, unsigned_int_type);
					
				}
				break;
			}
			case KEYWORD_Extension:
			case KEYWORD_va_arg:
			case KEYWORD_builtin_va_arg:
			case Identifier:
			case HexDoubleValue:
			case HexFloatValue:
			case HexLongDoubleValue:
			case HexUnsignedIntValue:
			case HexUnsignedLongIntValue:
			case HexUnsignedLongLongIntValue:
			case HexIntValue:
			case HexLongIntValue:
			case HexLongLongIntValue:
			case DoubleValue:
			case FloatValue:
			case LongDoubleValue:
			case UnsignedIntValue:
			case UnsignedLongIntValue:
			case UnsignedLongLongIntValue:
			case IntValue:
			case LongIntValue:
			case LongLongIntValue:
			case CharacterConstant:
			case WideCharacterConstant:
			case StringLit:
			case WideStringLiteral:
			case LParen:
			{
				exp=postfixExpression();
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_13);
			} else {
			  throw ex;
			}
		}
		return exp;
	}
	
	protected final Expression  castExpression() throws RecognitionException, TokenStreamException {
		Expression exp=errExp;
		
		Token  n0 = null;
		
		Type type = null;
		
		
		try {      // for error handling
			boolean synPredMatched42 = false;
			if (((LA(1)==LParen) && (_tokenSet_10.member(LA(2))))) {
				int _m42 = mark();
				synPredMatched42 = true;
				inputState.guessing++;
				try {
					{
					match(LParen);
					typeName();
					match(RParen);
					}
				}
				catch (RecognitionException pe) {
					synPredMatched42 = false;
				}
				rewind(_m42);
				inputState.guessing--;
			}
			if ( synPredMatched42 ) {
				match(LParen);
				type=typeName();
				n0 = LT(1);
				match(RParen);
				{
				switch ( LA(1)) {
				case LBrace:
				{
					exp=aggregationExpr(type);
					break;
				}
				case KEYWORD_sizeof:
				case KEYWORD_Extension:
				case KEYWORD_alignof:
				case KEYWORD_va_arg:
				case KEYWORD_builtin_va_arg:
				case KEYWORD_builtin_offsetof:
				case Identifier:
				case HexDoubleValue:
				case HexFloatValue:
				case HexLongDoubleValue:
				case HexUnsignedIntValue:
				case HexUnsignedLongIntValue:
				case HexUnsignedLongLongIntValue:
				case HexIntValue:
				case HexLongIntValue:
				case HexLongLongIntValue:
				case DoubleValue:
				case FloatValue:
				case LongDoubleValue:
				case UnsignedIntValue:
				case UnsignedLongIntValue:
				case UnsignedLongLongIntValue:
				case IntValue:
				case LongIntValue:
				case LongLongIntValue:
				case CharacterConstant:
				case WideCharacterConstant:
				case StringLit:
				case WideStringLiteral:
				case LParen:
				case Dec:
				case Inc:
				case And:
				case Mult:
				case Plus:
				case Sub:
				case Comp:
				case Not:
				{
					exp=castExpression();
					if ( inputState.guessing==0 ) {
						
						if (!fatalError) {
						if (type.isArrayType())
						type = PointerType.create(type);
						exp = cast(type, convertLiterals(exp), n0);
						}
						
					}
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
			}
			else if ((_tokenSet_0.member(LA(1))) && (_tokenSet_17.member(LA(2)))) {
				exp=unaryExpression();
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_13);
			} else {
			  throw ex;
			}
		}
		return exp;
	}
	
	protected final Expression  sizeof() throws RecognitionException, TokenStreamException {
		Expression exp = errExp;
		
		Token  n0 = null;
		
		Type       type;
		Expression expr;
		
		
		try {      // for error handling
			boolean synPredMatched33 = false;
			if (((LA(1)==LParen) && (_tokenSet_2.member(LA(2))))) {
				int _m33 = mark();
				synPredMatched33 = true;
				inputState.guessing++;
				try {
					{
					match(LParen);
					singleType();
					match(RParen);
					}
				}
				catch (RecognitionException pe) {
					synPredMatched33 = false;
				}
				rewind(_m33);
				inputState.guessing--;
			}
			if ( synPredMatched33 ) {
				n0 = LT(1);
				match(LParen);
				type=singleType();
				match(RParen);
				if ( inputState.guessing==0 ) {
					exp = buildSizeof(type, n0);
				}
			}
			else if ((_tokenSet_0.member(LA(1))) && (_tokenSet_17.member(LA(2)))) {
				expr=unaryExpression();
				if ( inputState.guessing==0 ) {
					exp = buildSizeof(expr, LT(0));
				}
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				
				reportError(ex);
				consume();
				fatalError = true;
				
			} else {
				throw ex;
			}
		}
		return exp;
	}
	
	protected final Expression  offsetof() throws RecognitionException, TokenStreamException {
		Expression exp = errExp;
		
		
		Type       type;
		
		
		try {      // for error handling
			match(LParen);
			type=singleType();
			match(Comma);
			exp=memberDesignator(type);
			match(RParen);
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				
				reportError(ex);
				consume();
				fatalError = true;
				
			} else {
				throw ex;
			}
		}
		return exp;
	}
	
	protected final Expression  memberDesignator(
		Type type
	) throws RecognitionException, TokenStreamException {
		Expression exp = null;
		
		Token  id = null;
		Token  id2 = null;
		Token  n = null;
		
		long       offset = 0;
		Expression exp2   = null;
		
		
		try {      // for error handling
			id = LT(1);
			match(Identifier);
			if ( inputState.guessing==0 ) {
				
				FieldDecl fd = getField(type, id.getText(), id);
				if (fd != null) {
				offset += fd.getFieldOffset();
				type = fd.getType();
				}
				
			}
			{
			_loop37:
			do {
				switch ( LA(1)) {
				case Dot:
				{
					match(Dot);
					id2 = LT(1);
					match(Identifier);
					if ( inputState.guessing==0 ) {
						
						FieldDecl fd = getField(type, id2.getText(), id2);
						if (fd != null) {
						offset += fd.getFieldOffset();
						type = fd.getType();
						}
						
					}
					break;
				}
				case LBracket:
				{
					n = LT(1);
					match(LBracket);
					exp2=expression();
					match(RBracket);
					if ( inputState.guessing==0 ) {
						
						exp = getArrayOffset(type, exp, exp2, n);
						
					}
					break;
				}
				default:
				{
					break _loop37;
				}
				}
			} while (true);
			}
			if ( inputState.guessing==0 ) {
				
				Expression off = LiteralMap.put(offset, size_t_type);
				if (exp == null)
				exp = off;
				else if (offset != 0)
				exp = new AdditionOp(size_t_type, off, exp);
				
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_14);
			} else {
			  throw ex;
			}
		}
		return exp;
	}
	
	protected final Type  declarationSpecifiers() throws RecognitionException, TokenStreamException {
		Type type = void_type;
		
		Token  n0 = null;
		
		int     saveBaseType      = baseType;
		int     saveTypeQualifier = typeQualifier;
		Type    saveBuildType     = buildType;
		
		
		try {      // for error handling
			if ( inputState.guessing==0 ) {
				
				baseType      = cStart;
				typeQualifier = 0;
				storageClass  = cAuto;
				
			}
			{
			int _cnt91=0;
			_loop91:
			do {
				switch ( LA(1)) {
				case KEYWORD_auto:
				case KEYWORD_extern:
				case KEYWORD_register:
				case KEYWORD_static:
				{
					storageClassSpecifier();
					break;
				}
				case KEYWORD_const:
				case KEYWORD___const:
				case KEYWORD_restrict:
				case KEYWORD_volatile:
				case KEYWORD__volatile:
				{
					typeQualifier();
					break;
				}
				case KEYWORD_inline:
				case KEYWORD__inline__:
				case KEYWORD__inline:
				{
					functionSpecifier();
					break;
				}
				case KEYWORD_char:
				case KEYWORD_double:
				case KEYWORD_enum:
				case KEYWORD_float:
				case KEYWORD_int:
				case KEYWORD_long:
				case KEYWORD_short:
				case KEYWORD_signed:
				case KEYWORD___signed__:
				case KEYWORD_struct:
				case KEYWORD_typeof:
				case KEYWORD_union:
				case KEYWORD_unsigned:
				case KEYWORD_void:
				case KEYWORD__Bool:
				case KEYWORD__Complex:
				case KEYWORD__Imaginary:
				{
					typeSpecifier();
					break;
				}
				default:
					if (((LA(1)==Identifier) && (_tokenSet_20.member(LA(2))))&&(isTypedef(LT(1).getText()) && (baseType == cStart))) {
						n0 = LT(1);
						match(Identifier);
						if ( inputState.guessing==0 ) {
							
							String   name = n0.getText();
							TypeName tn   = lookupTypedef(name);
							buildType = tn.getType();
							baseType = specifyBaseType(cOther, baseType);
							
						}
					}
				else {
					if ( _cnt91>=1 ) { break _loop91; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				}
				_cnt91++;
			} while (true);
			}
			if ( inputState.guessing==0 ) {
				
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
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				
				baseType      = saveBaseType;
				buildType     = saveBuildType;
				typeQualifier = saveTypeQualifier;
				reportError(ex);
				consume();
				fatalError = true;
				
			} else {
				throw ex;
			}
		}
		return type;
	}
	
	protected final Type  abstractDeclarator(
		Type type
	) throws RecognitionException, TokenStreamException {
		Type ntype=type;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case Mult:
			{
				ntype=pointer(type);
				{
				switch ( LA(1)) {
				case LParen:
				case LBracket:
				{
					ntype=directAbstractDeclarator(ntype);
					break;
				}
				case RParen:
				case Comma:
				case LITERAL___restrict:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				break;
			}
			case LParen:
			case LBracket:
			{
				ntype=directAbstractDeclarator(type);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_21);
			} else {
			  throw ex;
			}
		}
		return ntype;
	}
	
	protected final Vector<Object>  initializerList(
		Type type
	) throws RecognitionException, TokenStreamException {
		Vector<Object> v = null;
		
		
		int  npos  = 0;
		Type ntype = null;
		
		
		try {      // for error handling
			if ( inputState.guessing==0 ) {
				
				v = new Vector<Object>(20);
				
			}
			match(LBrace);
			{
			switch ( LA(1)) {
			case KEYWORD_sizeof:
			case KEYWORD_Extension:
			case KEYWORD_alignof:
			case KEYWORD_va_arg:
			case KEYWORD_builtin_va_arg:
			case KEYWORD_builtin_offsetof:
			case Identifier:
			case HexDoubleValue:
			case HexFloatValue:
			case HexLongDoubleValue:
			case HexUnsignedIntValue:
			case HexUnsignedLongIntValue:
			case HexUnsignedLongLongIntValue:
			case HexIntValue:
			case HexLongIntValue:
			case HexLongLongIntValue:
			case DoubleValue:
			case FloatValue:
			case LongDoubleValue:
			case UnsignedIntValue:
			case UnsignedLongIntValue:
			case UnsignedLongLongIntValue:
			case IntValue:
			case LongIntValue:
			case LongLongIntValue:
			case CharacterConstant:
			case WideCharacterConstant:
			case StringLit:
			case WideStringLiteral:
			case LParen:
			case LBrace:
			case Dot:
			case LBracket:
			case Dec:
			case Inc:
			case And:
			case Mult:
			case Plus:
			case Sub:
			case Comp:
			case Not:
			{
				npos=initializerListItem(v, npos, type);
				{
				_loop229:
				do {
					if ((LA(1)==Comma)) {
						match(Comma);
						{
						switch ( LA(1)) {
						case KEYWORD_sizeof:
						case KEYWORD_Extension:
						case KEYWORD_alignof:
						case KEYWORD_va_arg:
						case KEYWORD_builtin_va_arg:
						case KEYWORD_builtin_offsetof:
						case Identifier:
						case HexDoubleValue:
						case HexFloatValue:
						case HexLongDoubleValue:
						case HexUnsignedIntValue:
						case HexUnsignedLongIntValue:
						case HexUnsignedLongLongIntValue:
						case HexIntValue:
						case HexLongIntValue:
						case HexLongLongIntValue:
						case DoubleValue:
						case FloatValue:
						case LongDoubleValue:
						case UnsignedIntValue:
						case UnsignedLongIntValue:
						case UnsignedLongLongIntValue:
						case IntValue:
						case LongIntValue:
						case LongLongIntValue:
						case CharacterConstant:
						case WideCharacterConstant:
						case StringLit:
						case WideStringLiteral:
						case LParen:
						case LBrace:
						case Dot:
						case LBracket:
						case Dec:
						case Inc:
						case And:
						case Mult:
						case Plus:
						case Sub:
						case Comp:
						case Not:
						{
							npos=initializerListItem(v, npos, type);
							break;
						}
						case Comma:
						case RBrace:
						{
							break;
						}
						default:
						{
							throw new NoViableAltException(LT(1), getFilename());
						}
						}
						}
					}
					else {
						break _loop229;
					}
					
				} while (true);
				}
				break;
			}
			case RBrace:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(RBrace);
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				
				reportError(ex);
				consumeUntil(RBrace);
				consume();
				fatalError = true;
				return null;
				
			} else {
				throw ex;
			}
		}
		return v;
	}
	
	protected final Expression  multiplicativeExpression() throws RecognitionException, TokenStreamException {
		Expression exp=errExp;
		
		Token  n0 = null;
		Token  n1 = null;
		Token  n2 = null;
		
		Expression expr2;
		
		
		try {      // for error handling
			exp=castExpression();
			{
			_loop47:
			do {
				switch ( LA(1)) {
				case Mult:
				{
					n0 = LT(1);
					match(Mult);
					expr2=castExpression();
					if ( inputState.guessing==0 ) {
						exp = buildMultExpr(0, exp, expr2, n0);
					}
					break;
				}
				case Slash:
				{
					n1 = LT(1);
					match(Slash);
					expr2=castExpression();
					if ( inputState.guessing==0 ) {
						exp = buildMultExpr(1, exp, expr2, n1);
					}
					break;
				}
				case Mod:
				{
					n2 = LT(1);
					match(Mod);
					expr2=castExpression();
					if ( inputState.guessing==0 ) {
						exp = buildMultExpr(2, exp, expr2, n2);
					}
					break;
				}
				default:
				{
					break _loop47;
				}
				}
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_22);
			} else {
			  throw ex;
			}
		}
		return exp;
	}
	
	protected final Expression  additiveExpression() throws RecognitionException, TokenStreamException {
		Expression exp=errExp;
		
		Token  n0 = null;
		Token  n1 = null;
		
		Expression expr2;
		
		
		try {      // for error handling
			exp=multiplicativeExpression();
			{
			_loop50:
			do {
				switch ( LA(1)) {
				case Plus:
				{
					n0 = LT(1);
					match(Plus);
					expr2=multiplicativeExpression();
					if ( inputState.guessing==0 ) {
						exp = buildAddExpr(exp, expr2, n0);
					}
					break;
				}
				case Sub:
				{
					n1 = LT(1);
					match(Sub);
					expr2=multiplicativeExpression();
					if ( inputState.guessing==0 ) {
						exp = buildSubtractExpr(exp, expr2, n1);
					}
					break;
				}
				default:
				{
					break _loop50;
				}
				}
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_23);
			} else {
			  throw ex;
			}
		}
		return exp;
	}
	
	protected final Expression  shiftExpression() throws RecognitionException, TokenStreamException {
		Expression exp=errExp;
		
		Token  n0 = null;
		Token  n1 = null;
		
		Expression expr2;
		ShiftMode  shift = ShiftMode.Left;
		Token      t     = null;
		
		
		try {      // for error handling
			exp=additiveExpression();
			{
			_loop54:
			do {
				if ((LA(1)==LShift||LA(1)==RShift)) {
					{
					switch ( LA(1)) {
					case LShift:
					{
						n0 = LT(1);
						match(LShift);
						if ( inputState.guessing==0 ) {
							shift = ShiftMode.Left;        t = n0;
						}
						break;
					}
					case RShift:
					{
						n1 = LT(1);
						match(RShift);
						if ( inputState.guessing==0 ) {
							shift = ShiftMode.SignedRight; t = n1;
						}
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					expr2=additiveExpression();
					if ( inputState.guessing==0 ) {
						exp = buildShiftExpr(shift, exp, expr2, t);
					}
				}
				else {
					break _loop54;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_24);
			} else {
			  throw ex;
			}
		}
		return exp;
	}
	
	protected final Expression  relationalExpression() throws RecognitionException, TokenStreamException {
		Expression exp=errExp;
		
		Token  n0 = null;
		Token  n1 = null;
		Token  n2 = null;
		Token  n3 = null;
		
		Expression expr2 = null;
		int        sw    = 0;
		Token      t     = null;
		
		
		try {      // for error handling
			exp=shiftExpression();
			{
			_loop58:
			do {
				if (((LA(1) >= LAngle && LA(1) <= GEqual))) {
					{
					switch ( LA(1)) {
					case LAngle:
					{
						n0 = LT(1);
						match(LAngle);
						if ( inputState.guessing==0 ) {
							sw = 1; t = n0;
						}
						break;
					}
					case RAngle:
					{
						n1 = LT(1);
						match(RAngle);
						if ( inputState.guessing==0 ) {
							sw = 2; t = n1;
						}
						break;
					}
					case LEqual:
					{
						n2 = LT(1);
						match(LEqual);
						if ( inputState.guessing==0 ) {
							sw = 3; t = n2;
						}
						break;
					}
					case GEqual:
					{
						n3 = LT(1);
						match(GEqual);
						if ( inputState.guessing==0 ) {
							sw = 4; t = n3;
						}
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					expr2=shiftExpression();
					if ( inputState.guessing==0 ) {
						exp = buildCompare(sw, exp, expr2, t);
					}
				}
				else {
					break _loop58;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_25);
			} else {
			  throw ex;
			}
		}
		return exp;
	}
	
	protected final Expression  equalityExpression() throws RecognitionException, TokenStreamException {
		Expression exp=errExp;
		
		Token  n0 = null;
		Token  n1 = null;
		
		Expression expr2;
		boolean    equals = true;
		Token      t = null;
		
		
		try {      // for error handling
			exp=relationalExpression();
			{
			_loop62:
			do {
				if ((LA(1)==Equal||LA(1)==NEqual)) {
					{
					switch ( LA(1)) {
					case Equal:
					{
						n0 = LT(1);
						match(Equal);
						if ( inputState.guessing==0 ) {
							equals = true;  t = n0;
						}
						break;
					}
					case NEqual:
					{
						n1 = LT(1);
						match(NEqual);
						if ( inputState.guessing==0 ) {
							equals = false; t = n1;
						}
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					expr2=relationalExpression();
					if ( inputState.guessing==0 ) {
						exp = buildEqualCompare(equals, exp, expr2, t);
					}
				}
				else {
					break _loop62;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_26);
			} else {
			  throw ex;
			}
		}
		return exp;
	}
	
	protected final Expression  andExpression() throws RecognitionException, TokenStreamException {
		Expression exp=errExp;
		
		Token  n0 = null;
		
		Expression expr2;
		
		
		try {      // for error handling
			exp=equalityExpression();
			{
			_loop65:
			do {
				if ((LA(1)==And)) {
					n0 = LT(1);
					match(And);
					expr2=equalityExpression();
					if ( inputState.guessing==0 ) {
						exp = buildBitExpr(0, exp, expr2, n0);
					}
				}
				else {
					break _loop65;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_27);
			} else {
			  throw ex;
			}
		}
		return exp;
	}
	
	protected final Expression  xorExpression() throws RecognitionException, TokenStreamException {
		Expression exp=errExp;
		
		Token  n0 = null;
		
		Expression expr2;
		
		
		try {      // for error handling
			exp=andExpression();
			{
			_loop68:
			do {
				if ((LA(1)==Xor)) {
					n0 = LT(1);
					match(Xor);
					expr2=andExpression();
					if ( inputState.guessing==0 ) {
						exp = buildBitExpr(2, exp, expr2, n0);
					}
				}
				else {
					break _loop68;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_28);
			} else {
			  throw ex;
			}
		}
		return exp;
	}
	
	protected final Expression  orExpression() throws RecognitionException, TokenStreamException {
		Expression exp=errExp;
		
		Token  n0 = null;
		
		Expression expr2;
		
		
		try {      // for error handling
			exp=xorExpression();
			{
			_loop71:
			do {
				if ((LA(1)==Or)) {
					n0 = LT(1);
					match(Or);
					expr2=xorExpression();
					if ( inputState.guessing==0 ) {
						exp = buildBitExpr(1, exp, expr2, n0);
					}
				}
				else {
					break _loop71;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_29);
			} else {
			  throw ex;
			}
		}
		return exp;
	}
	
	protected final Expression  andConditionExpression() throws RecognitionException, TokenStreamException {
		Expression exp=errExp;
		
		Token  n0 = null;
		
		Expression expr2;
		
		
		try {      // for error handling
			exp=orExpression();
			{
			_loop74:
			do {
				if ((LA(1)==AndCond)) {
					n0 = LT(1);
					match(AndCond);
					expr2=orExpression();
					if ( inputState.guessing==0 ) {
						exp = buildConditional(0, exp, expr2, n0);
					}
				}
				else {
					break _loop74;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_30);
			} else {
			  throw ex;
			}
		}
		return exp;
	}
	
	protected final Expression  orConditionExpression() throws RecognitionException, TokenStreamException {
		Expression exp=errExp;
		
		Token  n0 = null;
		
		Expression expr2;
		
		
		try {      // for error handling
			exp=andConditionExpression();
			{
			_loop77:
			do {
				if ((LA(1)==OrCond)) {
					n0 = LT(1);
					match(OrCond);
					expr2=andConditionExpression();
					if ( inputState.guessing==0 ) {
						exp = buildConditional(1, exp, expr2, n0);
					}
				}
				else {
					break _loop77;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_31);
			} else {
			  throw ex;
			}
		}
		return exp;
	}
	
	protected final int  assignmentOperator() throws RecognitionException, TokenStreamException {
		int op = 0;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case Assign:
			{
				match(Assign);
				if ( inputState.guessing==0 ) {
					op =  1;
				}
				break;
			}
			case MultAssign:
			{
				match(MultAssign);
				if ( inputState.guessing==0 ) {
					op =  2;
				}
				break;
			}
			case SlashAssign:
			{
				match(SlashAssign);
				if ( inputState.guessing==0 ) {
					op =  3;
				}
				break;
			}
			case ModAssign:
			{
				match(ModAssign);
				if ( inputState.guessing==0 ) {
					op =  4;
				}
				break;
			}
			case PlusAssign:
			{
				match(PlusAssign);
				if ( inputState.guessing==0 ) {
					op =  5;
				}
				break;
			}
			case SubAssign:
			{
				match(SubAssign);
				if ( inputState.guessing==0 ) {
					op =  6;
				}
				break;
			}
			case AndAssign:
			{
				match(AndAssign);
				if ( inputState.guessing==0 ) {
					op =  7;
				}
				break;
			}
			case OrAssign:
			{
				match(OrAssign);
				if ( inputState.guessing==0 ) {
					op =  8;
				}
				break;
			}
			case XorAssign:
			{
				match(XorAssign);
				if ( inputState.guessing==0 ) {
					op =  9;
				}
				break;
			}
			case LSAssign:
			{
				match(LSAssign);
				if ( inputState.guessing==0 ) {
					op = 10;
				}
				break;
			}
			case RSAssign:
			{
				match(RSAssign);
				if ( inputState.guessing==0 ) {
					op = 11;
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_0);
			} else {
			  throw ex;
			}
		}
		return op;
	}
	
	protected final Expression  constantExpression() throws RecognitionException, TokenStreamException {
		Expression exp=errExp;
		
		
		boolean saveInConstant = inConstant;
		
		
		try {      // for error handling
			if ( inputState.guessing==0 ) {
				inConstant = true;
			}
			exp=conditionalExpression();
			if ( inputState.guessing==0 ) {
				
				inConstant = saveInConstant;
				if (!fatalError) {
				Literal lit = exp.getConstantValue();
				if ((lit == Lattice.Bot) || (lit == Lattice.Top))
				userError(Msg.MSG_Not_a_constant, null, LT(1));
				else
				exp = lit;
				}
				
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_32);
			} else {
			  throw ex;
			}
		}
		return exp;
	}
	
	protected final void declaration(
		BlockStmt block
	) throws RecognitionException, TokenStreamException {
		
		Token  s0 = null;
		
		Type   type             = null;
		Vector<Declaration> vars = null;
		String attributes       = null;
		int    saveStorageClass = storageClass;
		int    sc               = 0;
		
		
		try {      // for error handling
			if ( inputState.guessing==0 ) {
				
				vars = new Vector<Declaration>(10);
				
			}
			type=declarationSpecifiers();
			if ( inputState.guessing==0 ) {
				
				sc = storageClass;
				storageClass = saveStorageClass;
				
			}
			{
			switch ( LA(1)) {
			case Identifier:
			case LParen:
			case Mult:
			case LITERAL___restrict:
			case Attributes:
			{
				initDeclaratorList(vars, type, sc);
				break;
			}
			case Semi:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			s0 = LT(1);
			match(Semi);
			if ( inputState.guessing==0 ) {
				
				processDeclarations(vars, block, s0);
				
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_4);
			} else {
			  throw ex;
			}
		}
	}
	
	protected final void initDeclaratorList(
		Vector<Declaration> vars, Type type, int sc
	) throws RecognitionException, TokenStreamException {
		
		
		Declaration decl = null;
		
		
		try {      // for error handling
			decl=initDeclarator(type, sc);
			if ( inputState.guessing==0 ) {
				vars.addElement(decl);
			}
			{
			_loop97:
			do {
				if ((LA(1)==Comma)) {
					match(Comma);
					decl=initDeclarator(type, sc);
					if ( inputState.guessing==0 ) {
						vars.addElement(decl);
					}
				}
				else {
					break _loop97;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_33);
			} else {
			  throw ex;
			}
		}
	}
	
	protected final void storageClassSpecifier() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case KEYWORD_extern:
			{
				match(KEYWORD_extern);
				if ( inputState.guessing==0 ) {
					storageClass = specifyStorageClass(storageClass, cExtern, LT(1));
				}
				break;
			}
			case KEYWORD_static:
			{
				match(KEYWORD_static);
				if ( inputState.guessing==0 ) {
					storageClass = specifyStorageClass(storageClass, cStatic, LT(1));
				}
				break;
			}
			case KEYWORD_auto:
			{
				match(KEYWORD_auto);
				if ( inputState.guessing==0 ) {
					storageClass = specifyStorageClass(storageClass, cAuto, LT(1));
				}
				break;
			}
			case KEYWORD_register:
			{
				match(KEYWORD_register);
				if ( inputState.guessing==0 ) {
					storageClass = specifyStorageClass(storageClass, cRegister, LT(1));
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_34);
			} else {
			  throw ex;
			}
		}
	}
	
	protected final void typeQualifier() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case KEYWORD_const:
			{
				match(KEYWORD_const);
				if ( inputState.guessing==0 ) {
					specifyTypeQualifier(cConstantType);
				}
				break;
			}
			case KEYWORD_restrict:
			{
				match(KEYWORD_restrict);
				if ( inputState.guessing==0 ) {
					specifyTypeQualifier(cRestrict);
				}
				break;
			}
			case KEYWORD_volatile:
			{
				match(KEYWORD_volatile);
				if ( inputState.guessing==0 ) {
					specifyTypeQualifier(cVolatile);
				}
				break;
			}
			default:
				if (((LA(1)==KEYWORD___const))&&(allowGNUExtensions)) {
					match(KEYWORD___const);
					if ( inputState.guessing==0 ) {
						specifyTypeQualifier(cConstantType);
					}
				}
				else if (((LA(1)==KEYWORD__volatile))&&(allowGNUExtensions)) {
					match(KEYWORD__volatile);
					if ( inputState.guessing==0 ) {
						specifyTypeQualifier(cVolatile);
					}
				}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_35);
			} else {
			  throw ex;
			}
		}
	}
	
	protected final void functionSpecifier() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			{
			if ((LA(1)==KEYWORD_inline)) {
				match(KEYWORD_inline);
			}
			else if (((LA(1)==KEYWORD__inline))&&(allowGNUExtensions)) {
				match(KEYWORD__inline);
			}
			else if (((LA(1)==KEYWORD__inline__))&&(allowGNUExtensions)) {
				match(KEYWORD__inline__);
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			if ( inputState.guessing==0 ) {
				storageClass |= cInlined;
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_34);
			} else {
			  throw ex;
			}
		}
	}
	
	protected final void typeSpecifier() throws RecognitionException, TokenStreamException {
		
		
		Expression exp;
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case KEYWORD_void:
			{
				match(KEYWORD_void);
				if ( inputState.guessing==0 ) {
					baseType = specifyBaseType(cVoid, baseType);
				}
				break;
			}
			case KEYWORD_char:
			{
				match(KEYWORD_char);
				if ( inputState.guessing==0 ) {
					baseType = specifyBaseType(cChar, baseType);
				}
				break;
			}
			case KEYWORD_short:
			{
				match(KEYWORD_short);
				if ( inputState.guessing==0 ) {
					baseType = specifyBaseType(cShort, baseType);
				}
				break;
			}
			case KEYWORD_int:
			{
				match(KEYWORD_int);
				if ( inputState.guessing==0 ) {
					baseType = specifyBaseType(cInt, baseType);
				}
				break;
			}
			case KEYWORD_long:
			{
				match(KEYWORD_long);
				if ( inputState.guessing==0 ) {
					baseType = specifyBaseType(cLong, baseType);
				}
				break;
			}
			case KEYWORD_float:
			{
				match(KEYWORD_float);
				if ( inputState.guessing==0 ) {
					baseType = specifyBaseType(cFloat, baseType);
				}
				break;
			}
			case KEYWORD_double:
			{
				match(KEYWORD_double);
				if ( inputState.guessing==0 ) {
					baseType = specifyBaseType(cDouble, baseType);
				}
				break;
			}
			case KEYWORD_signed:
			{
				match(KEYWORD_signed);
				if ( inputState.guessing==0 ) {
					baseType = specifyBaseType(cSigned, baseType);
				}
				break;
			}
			case KEYWORD_unsigned:
			{
				match(KEYWORD_unsigned);
				if ( inputState.guessing==0 ) {
					baseType = specifyBaseType(cUnsigned, baseType);
				}
				break;
			}
			case KEYWORD__Bool:
			{
				match(KEYWORD__Bool);
				if ( inputState.guessing==0 ) {
					baseType = specifyBaseType(cBool, baseType);
				}
				break;
			}
			case KEYWORD__Complex:
			{
				match(KEYWORD__Complex);
				if ( inputState.guessing==0 ) {
					baseType = specifyBaseType(cComplex, baseType);
				}
				break;
			}
			case KEYWORD__Imaginary:
			{
				match(KEYWORD__Imaginary);
				if ( inputState.guessing==0 ) {
					baseType = specifyBaseType(cImaginary, baseType);
				}
				break;
			}
			case KEYWORD_struct:
			case KEYWORD_union:
			{
				buildType=structOrUnionSpecifier();
				if ( inputState.guessing==0 ) {
					baseType = specifyBaseType(cOther, baseType);
				}
				break;
			}
			case KEYWORD_enum:
			{
				buildType=enumSpecifier();
				if ( inputState.guessing==0 ) {
					baseType = specifyBaseType(cOther, baseType);
				}
				break;
			}
			default:
				if (((LA(1)==KEYWORD___signed__))&&(allowGNUExtensions)) {
					match(KEYWORD___signed__);
					if ( inputState.guessing==0 ) {
						baseType = specifyBaseType(cSigned, baseType);
					}
				}
				else if (((LA(1)==KEYWORD_typeof))&&(allowGNUExtensions)) {
					match(KEYWORD_typeof);
					match(LParen);
					exp=expression();
					match(RParen);
					if ( inputState.guessing==0 ) {
						
						buildType = exp.getType();
						baseType = specifyBaseType(cOther, baseType);
						
					}
				}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_36);
			} else {
			  throw ex;
			}
		}
	}
	
	protected final Type  declarationSpecifiersChk() throws RecognitionException, TokenStreamException {
		Type type = void_type;
		
		Token  n0 = null;
		
		int     saveBaseType      = baseType;
		int     saveTypeQualifier = typeQualifier;
		Type    saveBuildType     = buildType;
		
		
		try {      // for error handling
			if ( inputState.guessing==0 ) {
				
				baseType      = cStart;
				typeQualifier = 0;
				storageClass  = cAuto;
				
			}
			{
			int _cnt94=0;
			_loop94:
			do {
				switch ( LA(1)) {
				case KEYWORD_auto:
				case KEYWORD_extern:
				case KEYWORD_register:
				case KEYWORD_static:
				{
					storageClassSpecifier();
					break;
				}
				case KEYWORD_const:
				case KEYWORD___const:
				case KEYWORD_restrict:
				case KEYWORD_volatile:
				case KEYWORD__volatile:
				{
					typeQualifier();
					break;
				}
				case KEYWORD_inline:
				case KEYWORD__inline__:
				case KEYWORD__inline:
				{
					functionSpecifier();
					break;
				}
				case KEYWORD_char:
				case KEYWORD_double:
				case KEYWORD_enum:
				case KEYWORD_float:
				case KEYWORD_int:
				case KEYWORD_long:
				case KEYWORD_short:
				case KEYWORD_signed:
				case KEYWORD___signed__:
				case KEYWORD_struct:
				case KEYWORD_typeof:
				case KEYWORD_union:
				case KEYWORD_unsigned:
				case KEYWORD_void:
				case KEYWORD__Bool:
				case KEYWORD__Complex:
				case KEYWORD__Imaginary:
				{
					typeSpecifierChk();
					break;
				}
				default:
					if (((LA(1)==Identifier))&&(isTypedef(LT(1).getText()) && (baseType == cStart))) {
						n0 = LT(1);
						match(Identifier);
					}
				else {
					if ( _cnt94>=1 ) { break _loop94; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				}
				_cnt94++;
			} while (true);
			}
			if ( inputState.guessing==0 ) {
				
				baseType      = saveBaseType;
				buildType     = saveBuildType;
				typeQualifier = saveTypeQualifier;
				
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				
				baseType      = saveBaseType;
				buildType     = saveBuildType;
				typeQualifier = saveTypeQualifier;
				
			} else {
				throw ex;
			}
		}
		return type;
	}
	
	protected final void typeSpecifierChk() throws RecognitionException, TokenStreamException {
		
		
		Expression exp;
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case KEYWORD_void:
			{
				match(KEYWORD_void);
				break;
			}
			case KEYWORD_char:
			{
				match(KEYWORD_char);
				break;
			}
			case KEYWORD_short:
			{
				match(KEYWORD_short);
				break;
			}
			case KEYWORD_int:
			{
				match(KEYWORD_int);
				break;
			}
			case KEYWORD_long:
			{
				match(KEYWORD_long);
				break;
			}
			case KEYWORD_float:
			{
				match(KEYWORD_float);
				break;
			}
			case KEYWORD_double:
			{
				match(KEYWORD_double);
				break;
			}
			case KEYWORD_signed:
			{
				match(KEYWORD_signed);
				break;
			}
			case KEYWORD_unsigned:
			{
				match(KEYWORD_unsigned);
				break;
			}
			case KEYWORD__Bool:
			{
				match(KEYWORD__Bool);
				break;
			}
			case KEYWORD__Complex:
			{
				match(KEYWORD__Complex);
				break;
			}
			case KEYWORD__Imaginary:
			{
				match(KEYWORD__Imaginary);
				break;
			}
			case KEYWORD_struct:
			{
				match(KEYWORD_struct);
				break;
			}
			case KEYWORD_union:
			{
				match(KEYWORD_union);
				break;
			}
			case KEYWORD_enum:
			{
				match(KEYWORD_enum);
				break;
			}
			default:
				if (((LA(1)==KEYWORD___signed__))&&(allowGNUExtensions)) {
					match(KEYWORD___signed__);
				}
				else if (((LA(1)==KEYWORD_typeof))&&(allowGNUExtensions)) {
					match(KEYWORD_typeof);
					match(LParen);
				}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_37);
			} else {
			  throw ex;
			}
		}
	}
	
	protected final Declaration  initDeclarator(
		Type type, int sc
	) throws RecognitionException, TokenStreamException {
		Declaration decl = null;
		
		Token  n0 = null;
		
		Expression expr = null;
		
		
		try {      // for error handling
			decl=declarator(type, sc);
			if ( inputState.guessing==0 ) {
				
				if (decl != null)
				type = decl.getType();
				
			}
			{
			switch ( LA(1)) {
			case Assign:
			{
				n0 = LT(1);
				match(Assign);
				expr=initializer(type);
				break;
			}
			case Comma:
			case Semi:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			if ( inputState.guessing==0 ) {
				processDeclInitializer(decl, type, expr, n0);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_38);
			} else {
			  throw ex;
			}
		}
		return decl;
	}
	
	protected final Declaration  declarator(
		Type type, int sc
	) throws RecognitionException, TokenStreamException {
		Declaration decl = null;
		
		
		String saveIdentifier = declID;
		
		
		try {      // for error handling
			type=declarator2(type);
			if ( inputState.guessing==0 ) {
				
				decl = defineDecl(declID, type, sc, LT(1));
				processAttributes(declAtts, decl, LT(1));
				declAtts = "";
				declID = saveIdentifier;
				
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_39);
			} else {
			  throw ex;
			}
		}
		return decl;
	}
	
	protected final Expression  initializer(
		Type type
	) throws RecognitionException, TokenStreamException {
		Expression expr = errExp;
		
		
		Vector<Object>  v   = null;
		boolean         sic = inConstant;
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case LBrace:
			{
				if ( inputState.guessing==0 ) {
					inConstant = true;
				}
				v=initializerList(type);
				if ( inputState.guessing==0 ) {
					expr = buildAggregation(type, v, LT(1)); inConstant = sic;
				}
				break;
			}
			case KEYWORD_sizeof:
			case KEYWORD_Extension:
			case KEYWORD_alignof:
			case KEYWORD_va_arg:
			case KEYWORD_builtin_va_arg:
			case KEYWORD_builtin_offsetof:
			case Identifier:
			case HexDoubleValue:
			case HexFloatValue:
			case HexLongDoubleValue:
			case HexUnsignedIntValue:
			case HexUnsignedLongIntValue:
			case HexUnsignedLongLongIntValue:
			case HexIntValue:
			case HexLongIntValue:
			case HexLongLongIntValue:
			case DoubleValue:
			case FloatValue:
			case LongDoubleValue:
			case UnsignedIntValue:
			case UnsignedLongIntValue:
			case UnsignedLongLongIntValue:
			case IntValue:
			case LongIntValue:
			case LongLongIntValue:
			case CharacterConstant:
			case WideCharacterConstant:
			case StringLit:
			case WideStringLiteral:
			case LParen:
			case Dec:
			case Inc:
			case And:
			case Mult:
			case Plus:
			case Sub:
			case Comp:
			case Not:
			{
				expr=assignmentExpression();
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_40);
			} else {
			  throw ex;
			}
		}
		return expr;
	}
	
	protected final Type  declarator2(
		Type type
	) throws RecognitionException, TokenStreamException {
		Type ntype = type;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case Mult:
			{
				ntype=pointer(ntype);
				ntype=directDeclarator(ntype);
				break;
			}
			case Identifier:
			case LParen:
			case LITERAL___restrict:
			case Attributes:
			{
				ntype=directDeclarator(ntype);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_41);
			} else {
			  throw ex;
			}
		}
		return ntype;
	}
	
	protected final Type  pointer(
		Type type
	) throws RecognitionException, TokenStreamException {
		Type ntype = type;
		
		
		int saveTypeQualifier = typeQualifier;
		
		
		try {      // for error handling
			{
			int _cnt185=0;
			_loop185:
			do {
				if ((LA(1)==Mult)) {
					match(Mult);
					if ( inputState.guessing==0 ) {
						typeQualifier = 0;
					}
					{
					switch ( LA(1)) {
					case KEYWORD_const:
					case KEYWORD___const:
					case KEYWORD_restrict:
					case KEYWORD_volatile:
					case KEYWORD__volatile:
					{
						typeQualifierList();
						break;
					}
					case Identifier:
					case LParen:
					case RParen:
					case LBracket:
					case Comma:
					case Mult:
					case LITERAL___restrict:
					case Attributes:
					{
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					if ( inputState.guessing==0 ) {
						
						ntype = PointerType.create(ntype);
						if (typeQualifier != 0)
						ntype = addTypeQualifier(ntype, typeQualifier);
						
					}
				}
				else {
					if ( _cnt185>=1 ) { break _loop185; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt185++;
			} while (true);
			}
			if ( inputState.guessing==0 ) {
				typeQualifier = saveTypeQualifier;
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_42);
			} else {
			  throw ex;
			}
		}
		return ntype;
	}
	
	protected final Type  directDeclarator(
		Type type
	) throws RecognitionException, TokenStreamException {
		Type ntype=type;
		
		Token  att0 = null;
		Token  id = null;
		Token  att1 = null;
		
		IncompleteType ictype = null;
		Type           xtype  = null;
		String         name   = null;
		int            pos    = 0;
		String         atts   = "";
		declID = "";
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case Identifier:
			case LITERAL___restrict:
			case Attributes:
			{
				{
				if (((LA(1)==LITERAL___restrict))&&(allowGNUExtensions)) {
					match(LITERAL___restrict);
				}
				else if ((LA(1)==Identifier||LA(1)==Attributes)) {
				}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				
				}
				{
				_loop105:
				do {
					if ((LA(1)==Attributes)) {
						att0 = LT(1);
						match(Attributes);
						if ( inputState.guessing==0 ) {
							declAtts = declAtts + att0.getText();
						}
					}
					else {
						break _loop105;
					}
					
				} while (true);
				}
				id = LT(1);
				match(Identifier);
				{
				if ((((LA(1) >= LITERAL___asm__ && LA(1) <= LITERAL___asm)) && (_tokenSet_43.member(LA(2))))&&(allowGNUExtensions)) {
					{
					switch ( LA(1)) {
					case LITERAL___asm__:
					{
						match(LITERAL___asm__);
						break;
					}
					case LITERAL_asm:
					{
						match(LITERAL_asm);
						break;
					}
					case LITERAL___asm:
					{
						match(LITERAL___asm);
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
				}
				else if ((_tokenSet_43.member(LA(1))) && (_tokenSet_44.member(LA(2)))) {
				}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				
				}
				{
				_loop109:
				do {
					switch ( LA(1)) {
					case LParen:
					{
						ntype=parenGroup(ntype);
						if ( inputState.guessing==0 ) {
							pos = 0;
						}
						break;
					}
					case LBracket:
					{
						ntype=bracketGroup(ntype, pos++);
						break;
					}
					default:
					{
						break _loop109;
					}
					}
				} while (true);
				}
				if ( inputState.guessing==0 ) {
					declID = id.getText();
				}
				{
				if ((((LA(1) >= LITERAL___asm__ && LA(1) <= LITERAL___asm)))&&(allowGNUExtensions)) {
					{
					switch ( LA(1)) {
					case LITERAL___asm__:
					{
						match(LITERAL___asm__);
						break;
					}
					case LITERAL_asm:
					{
						match(LITERAL_asm);
						break;
					}
					case LITERAL___asm:
					{
						match(LITERAL___asm);
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					{
					switch ( LA(1)) {
					case LParen:
					{
						match(LParen);
						match(StringLit);
						{
						_loop114:
						do {
							if ((LA(1)==StringLit)) {
								match(StringLit);
							}
							else {
								break _loop114;
							}
							
						} while (true);
						}
						match(RParen);
						break;
					}
					case KEYWORD_auto:
					case KEYWORD_char:
					case KEYWORD_const:
					case KEYWORD___const:
					case KEYWORD_double:
					case KEYWORD_enum:
					case KEYWORD_extern:
					case KEYWORD_float:
					case KEYWORD_inline:
					case KEYWORD_int:
					case KEYWORD_long:
					case KEYWORD_register:
					case KEYWORD_restrict:
					case KEYWORD_short:
					case KEYWORD_signed:
					case KEYWORD___signed__:
					case KEYWORD_static:
					case KEYWORD_struct:
					case KEYWORD_typeof:
					case KEYWORD_union:
					case KEYWORD_unsigned:
					case KEYWORD_void:
					case KEYWORD_volatile:
					case KEYWORD__volatile:
					case KEYWORD__Bool:
					case KEYWORD__Complex:
					case KEYWORD__Imaginary:
					case KEYWORD__inline__:
					case KEYWORD__inline:
					case Identifier:
					case LBrace:
					case RParen:
					case Comma:
					case Colon:
					case Assign:
					case Semi:
					case Attributes:
					{
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
				}
				else if ((_tokenSet_41.member(LA(1)))) {
				}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				
				}
				{
				_loop116:
				do {
					if ((LA(1)==Attributes) && (_tokenSet_41.member(LA(2)))) {
						att1 = LT(1);
						match(Attributes);
						if ( inputState.guessing==0 ) {
							declAtts = declAtts + att1.getText();
						}
					}
					else {
						break _loop116;
					}
					
				} while (true);
				}
				break;
			}
			case LParen:
			{
				match(LParen);
				if ( inputState.guessing==0 ) {
					
					ictype = new IncompleteType();
					ntype = type;
					
				}
				xtype=declarator2(ictype);
				match(RParen);
				if ( inputState.guessing==0 ) {
					
					name = declID;
					
				}
				{
				_loop118:
				do {
					switch ( LA(1)) {
					case LParen:
					{
						ntype=parenGroup(ntype);
						if ( inputState.guessing==0 ) {
							pos = 0;
						}
						break;
					}
					case LBracket:
					{
						ntype=bracketGroup(ntype, pos++);
						break;
					}
					default:
					{
						break _loop118;
					}
					}
				} while (true);
				}
				if ( inputState.guessing==0 ) {
					
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
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_41);
			} else {
			  throw ex;
			}
		}
		return ntype;
	}
	
	protected final Type  parenGroup(
		Type type
	) throws RecognitionException, TokenStreamException {
		Type ntype=type;
		
		
		Vector<? extends Object> formals = null;
		
		
		try {      // for error handling
			match(LParen);
			{
			if ((LA(1)==RParen)) {
			}
			else {
				boolean synPredMatched179 = false;
				if (((LA(1)==KEYWORD_void) && (LA(2)==RParen))) {
					int _m179 = mark();
					synPredMatched179 = true;
					inputState.guessing++;
					try {
						{
						match(KEYWORD_void);
						match(RParen);
						}
					}
					catch (RecognitionException pe) {
						synPredMatched179 = false;
					}
					rewind(_m179);
					inputState.guessing--;
				}
				if ( synPredMatched179 ) {
					match(KEYWORD_void);
				}
				else {
					boolean synPredMatched181 = false;
					if ((((LA(1)==Identifier) && (LA(2)==RParen||LA(2)==Comma))&&(!isTypedef(LT(1).getText())))) {
						int _m181 = mark();
						synPredMatched181 = true;
						inputState.guessing++;
						try {
							{
							match(Identifier);
							}
						}
						catch (RecognitionException pe) {
							synPredMatched181 = false;
						}
						rewind(_m181);
						inputState.guessing--;
					}
					if ( synPredMatched181 ) {
						formals=identifierList();
					}
					else if ((_tokenSet_2.member(LA(1))) && (_tokenSet_45.member(LA(2)))) {
						formals=parameterTypeList();
					}
					else {
						throw new NoViableAltException(LT(1), getFilename());
					}
					}}
					}
					match(RParen);
					if ( inputState.guessing==0 ) {
						
						ntype = makeProcedureType(type, formals);
						
					}
				}
				catch (RecognitionException ex) {
					if (inputState.guessing==0) {
						reportError(ex);
						consume();
						consumeUntil(_tokenSet_43);
					} else {
					  throw ex;
					}
				}
				return ntype;
			}
			
	protected final Type  bracketGroup(
		Type type, int pos
	) throws RecognitionException, TokenStreamException {
		Type ntype=type;
		
		Token  s0 = null;
		Token  s1 = null;
		Token  s2 = null;
		
		Expression expr1 = null;
		
		
		try {      // for error handling
			s0 = LT(1);
			match(LBracket);
			{
			switch ( LA(1)) {
			case RBracket:
			{
				if ( inputState.guessing==0 ) {
					ntype = createArrayType(pos == 0, type, s0);
				}
				break;
			}
			case KEYWORD_static:
			{
				match(KEYWORD_static);
				{
				switch ( LA(1)) {
				case KEYWORD_const:
				case KEYWORD___const:
				case KEYWORD_restrict:
				case KEYWORD_volatile:
				case KEYWORD__volatile:
				{
					typeQualifierList();
					break;
				}
				case KEYWORD_sizeof:
				case KEYWORD_Extension:
				case KEYWORD_alignof:
				case KEYWORD_va_arg:
				case KEYWORD_builtin_va_arg:
				case KEYWORD_builtin_offsetof:
				case Identifier:
				case HexDoubleValue:
				case HexFloatValue:
				case HexLongDoubleValue:
				case HexUnsignedIntValue:
				case HexUnsignedLongIntValue:
				case HexUnsignedLongLongIntValue:
				case HexIntValue:
				case HexLongIntValue:
				case HexLongLongIntValue:
				case DoubleValue:
				case FloatValue:
				case LongDoubleValue:
				case UnsignedIntValue:
				case UnsignedLongIntValue:
				case UnsignedLongLongIntValue:
				case IntValue:
				case LongIntValue:
				case LongLongIntValue:
				case CharacterConstant:
				case WideCharacterConstant:
				case StringLit:
				case WideStringLiteral:
				case LParen:
				case Dec:
				case Inc:
				case And:
				case Mult:
				case Plus:
				case Sub:
				case Comp:
				case Not:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				expr1=assignmentExpression();
				break;
			}
			case KEYWORD_const:
			case KEYWORD___const:
			case KEYWORD_restrict:
			case KEYWORD_volatile:
			case KEYWORD__volatile:
			{
				typeQualifierList();
				{
				if ((LA(1)==KEYWORD_static)) {
					s1 = LT(1);
					match(KEYWORD_static);
					expr1=constantExpression();
					if ( inputState.guessing==0 ) {
						notImplementedError("static in array declaration ", s0);
					}
				}
				else {
					boolean synPredMatched173 = false;
					if (((LA(1)==Mult) && (LA(2)==RBracket))) {
						int _m173 = mark();
						synPredMatched173 = true;
						inputState.guessing++;
						try {
							{
							match(Mult);
							match(RBracket);
							}
						}
						catch (RecognitionException pe) {
							synPredMatched173 = false;
						}
						rewind(_m173);
						inputState.guessing--;
					}
					if ( synPredMatched173 ) {
						match(Mult);
					}
					else if ((_tokenSet_0.member(LA(1))) && (_tokenSet_46.member(LA(2)))) {
						expr1=assignmentExpression();
					}
					else {
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					if ( inputState.guessing==0 ) {
						notImplementedError("static in array declaration ", s1);
					}
					break;
				}
				default:
					boolean synPredMatched175 = false;
					if (((LA(1)==Mult) && (LA(2)==RBracket))) {
						int _m175 = mark();
						synPredMatched175 = true;
						inputState.guessing++;
						try {
							{
							match(Mult);
							match(RBracket);
							}
						}
						catch (RecognitionException pe) {
							synPredMatched175 = false;
						}
						rewind(_m175);
						inputState.guessing--;
					}
					if ( synPredMatched175 ) {
						s2 = LT(1);
						match(Mult);
						if ( inputState.guessing==0 ) {
							ntype = createArrayType(pos == 0, type, s0);
						}
					}
					else if ((_tokenSet_0.member(LA(1))) && (_tokenSet_47.member(LA(2)))) {
						expr1=constantExpression();
						if ( inputState.guessing==0 ) {
							ntype = createArrayType(pos == 0, type, expr1, s0);
						}
					}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				match(RBracket);
			}
			catch (RecognitionException ex) {
				if (inputState.guessing==0) {
					reportError(ex);
					consume();
					consumeUntil(_tokenSet_43);
				} else {
				  throw ex;
				}
			}
			return ntype;
		}
		
	protected final Type  structOrUnionSpecifier() throws RecognitionException, TokenStreamException {
		Type type = void_type;
		
		Token  att0 = null;
		Token  att1 = null;
		Token  n0 = null;
		Token  att2 = null;
		
		Vector<FieldDecl>   fields  = null;
		boolean  isUnion = false;
		TypeDecl decl    = null;
		String   atts    = "";
		
		
		try {      // for error handling
			if ( inputState.guessing==0 ) {
				
				fields = new Vector<FieldDecl>(8);
				
			}
			{
			switch ( LA(1)) {
			case KEYWORD_struct:
			{
				match(KEYWORD_struct);
				break;
			}
			case KEYWORD_union:
			{
				match(KEYWORD_union);
				if ( inputState.guessing==0 ) {
					isUnion = true;
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			_loop130:
			do {
				if ((LA(1)==Attributes)) {
					att0 = LT(1);
					match(Attributes);
					if ( inputState.guessing==0 ) {
						atts = atts + att0.getText();
					}
				}
				else {
					break _loop130;
				}
				
			} while (true);
			}
			{
			switch ( LA(1)) {
			case LBrace:
			{
				match(LBrace);
				structDeclarationList(fields);
				match(RBrace);
				{
				_loop133:
				do {
					if ((LA(1)==Attributes) && (_tokenSet_36.member(LA(2)))) {
						att1 = LT(1);
						match(Attributes);
						if ( inputState.guessing==0 ) {
							atts = atts + att1.getText();
						}
					}
					else {
						break _loop133;
					}
					
				} while (true);
				}
				if ( inputState.guessing==0 ) {
					
					AggregateType atype = isUnion ? UnionType.create(fields, false) : RecordType.create(fields);
					atype.memorySize(Machine.currentMachine);
					decl = createTypeDecl("_T" + typeCounter++, atype);
					type = decl.getType();
					
				}
				break;
			}
			case Identifier:
			{
				n0 = LT(1);
				match(Identifier);
				if ( inputState.guessing==0 ) {
					
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
				{
				switch ( LA(1)) {
				case LBrace:
				{
					match(LBrace);
					structDeclarationList(fields);
					match(RBrace);
					{
					_loop136:
					do {
						if ((LA(1)==Attributes) && (_tokenSet_36.member(LA(2)))) {
							att2 = LT(1);
							match(Attributes);
							if ( inputState.guessing==0 ) {
								atts = atts + att2.getText();
							}
						}
						else {
							break _loop136;
						}
						
					} while (true);
					}
					if ( inputState.guessing==0 ) {
						
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
					break;
				}
				case KEYWORD_auto:
				case KEYWORD_char:
				case KEYWORD_const:
				case KEYWORD___const:
				case KEYWORD_double:
				case KEYWORD_enum:
				case KEYWORD_extern:
				case KEYWORD_float:
				case KEYWORD_inline:
				case KEYWORD_int:
				case KEYWORD_long:
				case KEYWORD_register:
				case KEYWORD_restrict:
				case KEYWORD_short:
				case KEYWORD_signed:
				case KEYWORD___signed__:
				case KEYWORD_static:
				case KEYWORD_struct:
				case KEYWORD_typeof:
				case KEYWORD_union:
				case KEYWORD_unsigned:
				case KEYWORD_void:
				case KEYWORD_volatile:
				case KEYWORD__volatile:
				case KEYWORD__Bool:
				case KEYWORD__Complex:
				case KEYWORD__Imaginary:
				case KEYWORD__inline__:
				case KEYWORD__inline:
				case Identifier:
				case LParen:
				case RParen:
				case LBracket:
				case Comma:
				case Mult:
				case Colon:
				case Semi:
				case LITERAL___restrict:
				case Attributes:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			if ( inputState.guessing==0 ) {
				type = processAttributes(atts, type, LT(0));
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_36);
			} else {
			  throw ex;
			}
		}
		return type;
	}
	
	protected final Type  enumSpecifier() throws RecognitionException, TokenStreamException {
		Type type = void_type;
		
		Token  att0 = null;
		Token  n0 = null;
		Token  att1 = null;
		Token  n1 = null;
		Token  att2 = null;
		
		Vector<EnumElementDecl> ev   = null;
		String atts = "";
		
		
		try {      // for error handling
			match(KEYWORD_enum);
			{
			_loop155:
			do {
				if ((LA(1)==Attributes)) {
					att0 = LT(1);
					match(Attributes);
					if ( inputState.guessing==0 ) {
						atts = atts + att0.getText();
					}
				}
				else {
					break _loop155;
				}
				
			} while (true);
			}
			{
			switch ( LA(1)) {
			case LBrace:
			{
				n0 = LT(1);
				match(LBrace);
				ev=enumeratorList();
				match(RBrace);
				{
				_loop158:
				do {
					if ((LA(1)==Attributes) && (_tokenSet_36.member(LA(2)))) {
						att1 = LT(1);
						match(Attributes);
						if ( inputState.guessing==0 ) {
							atts = atts + att1.getText();
						}
					}
					else {
						break _loop158;
					}
					
				} while (true);
				}
				if ( inputState.guessing==0 ) {
					type = createEnumType(null, ev, n0);
				}
				break;
			}
			case Identifier:
			{
				n1 = LT(1);
				match(Identifier);
				{
				switch ( LA(1)) {
				case LBrace:
				{
					match(LBrace);
					ev=enumeratorList();
					match(RBrace);
					{
					_loop161:
					do {
						if ((LA(1)==Attributes) && (_tokenSet_36.member(LA(2)))) {
							att2 = LT(1);
							match(Attributes);
							if ( inputState.guessing==0 ) {
								atts = atts + att2.getText();
							}
						}
						else {
							break _loop161;
						}
						
					} while (true);
					}
					break;
				}
				case KEYWORD_auto:
				case KEYWORD_char:
				case KEYWORD_const:
				case KEYWORD___const:
				case KEYWORD_double:
				case KEYWORD_enum:
				case KEYWORD_extern:
				case KEYWORD_float:
				case KEYWORD_inline:
				case KEYWORD_int:
				case KEYWORD_long:
				case KEYWORD_register:
				case KEYWORD_restrict:
				case KEYWORD_short:
				case KEYWORD_signed:
				case KEYWORD___signed__:
				case KEYWORD_static:
				case KEYWORD_struct:
				case KEYWORD_typeof:
				case KEYWORD_union:
				case KEYWORD_unsigned:
				case KEYWORD_void:
				case KEYWORD_volatile:
				case KEYWORD__volatile:
				case KEYWORD__Bool:
				case KEYWORD__Complex:
				case KEYWORD__Imaginary:
				case KEYWORD__inline__:
				case KEYWORD__inline:
				case Identifier:
				case LParen:
				case RParen:
				case LBracket:
				case Comma:
				case Mult:
				case Colon:
				case Semi:
				case LITERAL___restrict:
				case Attributes:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				if ( inputState.guessing==0 ) {
					type = createEnumType(n1.getText(), ev, n1);
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			if ( inputState.guessing==0 ) {
				type = processAttributes(atts, type, LT(0));
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_36);
			} else {
			  throw ex;
			}
		}
		return type;
	}
	
	protected final void structDeclarationList(
		Vector<FieldDecl> fields
	) throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			{
			_loop139:
			do {
				if ((_tokenSet_48.member(LA(1)))) {
					structDeclaration(fields);
				}
				else {
					break _loop139;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_49);
			} else {
			  throw ex;
			}
		}
	}
	
	protected final void structDeclaration(
		Vector<FieldDecl> fields
	) throws RecognitionException, TokenStreamException {
		
		Token  n0 = null;
		
		Type      type = null;
		FieldDecl fd   = null; 
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case KEYWORD_Extension:
			{
				match(KEYWORD_Extension);
				break;
			}
			case KEYWORD_char:
			case KEYWORD_const:
			case KEYWORD___const:
			case KEYWORD_double:
			case KEYWORD_enum:
			case KEYWORD_float:
			case KEYWORD_int:
			case KEYWORD_long:
			case KEYWORD_restrict:
			case KEYWORD_short:
			case KEYWORD_signed:
			case KEYWORD___signed__:
			case KEYWORD_struct:
			case KEYWORD_typeof:
			case KEYWORD_union:
			case KEYWORD_unsigned:
			case KEYWORD_void:
			case KEYWORD_volatile:
			case KEYWORD__volatile:
			case KEYWORD__Bool:
			case KEYWORD__Complex:
			case KEYWORD__Imaginary:
			case Identifier:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			type=specifierQualifierList();
			{
			switch ( LA(1)) {
			case Identifier:
			case LParen:
			case Mult:
			case Colon:
			case LITERAL___restrict:
			case Attributes:
			{
				fd=structDeclarator(type);
				if ( inputState.guessing==0 ) {
					addField(fd, fields, LT(1));
				}
				{
				_loop144:
				do {
					if ((LA(1)==Comma)) {
						match(Comma);
						fd=structDeclarator(type);
						if ( inputState.guessing==0 ) {
							addField(fd, fields, LT(1));
						}
					}
					else {
						break _loop144;
					}
					
				} while (true);
				}
				break;
			}
			case Semi:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			n0 = LT(1);
			match(Semi);
			if ( inputState.guessing==0 ) {
				
				if (fd == null) { // Anonymous field.
				fd = createFieldDecl(null, type, 0, fieldCounter++, n0);
				addField(fd, fields, LT(1));
				}
				
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_50);
			} else {
			  throw ex;
			}
		}
	}
	
	protected final Type  specifierQualifierList() throws RecognitionException, TokenStreamException {
		Type type = void_type;
		
		Token  n0 = null;
		
		int     saveBaseType      = baseType;
		int     saveTypeQualifier = typeQualifier;
		Type    saveBuildType     = buildType;
		
		
		try {      // for error handling
			if ( inputState.guessing==0 ) {
				
				baseType      = cStart;
				typeQualifier = 0;
				
			}
			{
			int _cnt147=0;
			_loop147:
			do {
				switch ( LA(1)) {
				case KEYWORD_const:
				case KEYWORD___const:
				case KEYWORD_restrict:
				case KEYWORD_volatile:
				case KEYWORD__volatile:
				{
					typeQualifier();
					break;
				}
				case KEYWORD_char:
				case KEYWORD_double:
				case KEYWORD_enum:
				case KEYWORD_float:
				case KEYWORD_int:
				case KEYWORD_long:
				case KEYWORD_short:
				case KEYWORD_signed:
				case KEYWORD___signed__:
				case KEYWORD_struct:
				case KEYWORD_typeof:
				case KEYWORD_union:
				case KEYWORD_unsigned:
				case KEYWORD_void:
				case KEYWORD__Bool:
				case KEYWORD__Complex:
				case KEYWORD__Imaginary:
				{
					typeSpecifier();
					break;
				}
				default:
					if (((LA(1)==Identifier) && (_tokenSet_51.member(LA(2))))&&(isTypedef(LT(1).getText()) && (baseType == cStart))) {
						n0 = LT(1);
						match(Identifier);
						if ( inputState.guessing==0 ) {
							
							String   name = n0.getText();
							TypeName tn   = lookupTypedef(name);
							buildType = tn.getType();
							baseType = specifyBaseType(cOther, baseType);
							
						}
					}
				else {
					if ( _cnt147>=1 ) { break _loop147; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				}
				_cnt147++;
			} while (true);
			}
			if ( inputState.guessing==0 ) {
				
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
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				
				baseType      = saveBaseType;
				buildType     = saveBuildType;
				typeQualifier = saveTypeQualifier;
				reportError(ex);
				consume();
				fatalError = true;
				
			} else {
				throw ex;
			}
		}
		return type;
	}
	
	protected final FieldDecl  structDeclarator(
		Type type
	) throws RecognitionException, TokenStreamException {
		FieldDecl fd = null;
		
		Token  n0 = null;
		Token  n1 = null;
		
		Expression expr1  = null;
		FieldDecl  decl   = null;
		String     saveId = declID;
		String     name   = null;
		Token      t      = null;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case Colon:
			{
				n0 = LT(1);
				match(Colon);
				expr1=constantExpression();
				{
				switch ( LA(1)) {
				case Attributes:
				{
					type=getAttributes(type);
					break;
				}
				case Comma:
				case Semi:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				if ( inputState.guessing==0 ) {
					
					int bits = 0;
					if (expr1 != null)
					bits = getIntValue(expr1, n0);
					fd = createFieldDecl(null, type, bits, fieldCounter++, n0);
					
				}
				break;
			}
			case Identifier:
			case LParen:
			case Mult:
			case LITERAL___restrict:
			case Attributes:
			{
				if ( inputState.guessing==0 ) {
					saveId = declID; t = LT(1);
				}
				type=declarator2(type);
				if ( inputState.guessing==0 ) {
					name = declID; declID = saveId;
				}
				{
				switch ( LA(1)) {
				case Colon:
				{
					n1 = LT(1);
					match(Colon);
					expr1=constantExpression();
					break;
				}
				case Comma:
				case Semi:
				case Attributes:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				{
				switch ( LA(1)) {
				case Attributes:
				{
					type=getAttributes(type);
					break;
				}
				case Comma:
				case Semi:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				if ( inputState.guessing==0 ) {
					
					int bits = 0;
					if (expr1 != null)
					bits = getIntValue(expr1, n1);
					fd = createFieldDecl(name, type, bits, 0, t);
					
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_38);
			} else {
			  throw ex;
			}
		}
		return fd;
	}
	
	protected final Type  getAttributes(
		Type type
	) throws RecognitionException, TokenStreamException {
		Type ntype = type;
		
		Token  att = null;
		
		try {      // for error handling
			att = LT(1);
			match(Attributes);
			if ( inputState.guessing==0 ) {
				ntype = processAttributes(att.getText(), type, att);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_38);
			} else {
			  throw ex;
			}
		}
		return ntype;
	}
	
	protected final Vector<EnumElementDecl>  enumeratorList() throws RecognitionException, TokenStreamException {
		Vector<EnumElementDecl> ev = new Vector<EnumElementDecl>(10);
		
		
		EnumElementDecl ed = null;
		
		
		try {      // for error handling
			ed=enumerator(ed);
			if ( inputState.guessing==0 ) {
				ev.addElement(ed);
			}
			{
			_loop165:
			do {
				if ((LA(1)==Comma)) {
					match(Comma);
					{
					switch ( LA(1)) {
					case Identifier:
					{
						ed=enumerator(ed);
						if ( inputState.guessing==0 ) {
							ev.addElement(ed);
						}
						break;
					}
					case Comma:
					case RBrace:
					{
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
				}
				else {
					break _loop165;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_49);
			} else {
			  throw ex;
			}
		}
		return ev;
	}
	
	protected final EnumElementDecl  enumerator(
		EnumElementDecl last
	) throws RecognitionException, TokenStreamException {
		EnumElementDecl ed = null;
		
		Token  n0 = null;
		
		Expression expr1 = null;
		
		
		try {      // for error handling
			n0 = LT(1);
			match(Identifier);
			{
			switch ( LA(1)) {
			case Assign:
			{
				match(Assign);
				expr1=constantExpression();
				break;
			}
			case Comma:
			case RBrace:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			if ( inputState.guessing==0 ) {
				ed = buildEnumElementDecl(n0.getText(), last, expr1, n0);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_52);
			} else {
			  throw ex;
			}
		}
		return ed;
	}
	
	protected final void typeQualifierList() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			if ( inputState.guessing==0 ) {
				
				typeQualifier = 0;
				
			}
			{
			int _cnt188=0;
			_loop188:
			do {
				if ((_tokenSet_53.member(LA(1)))) {
					typeQualifier();
				}
				else {
					if ( _cnt188>=1 ) { break _loop188; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt188++;
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_54);
			} else {
			  throw ex;
			}
		}
	}
	
	protected final Vector<String>  identifierList() throws RecognitionException, TokenStreamException {
		Vector<String> ids = new Vector<String>(6);
		
		Token  n0 = null;
		Token  n1 = null;
		
		try {      // for error handling
			n0 = LT(1);
			match(Identifier);
			if ( inputState.guessing==0 ) {
				ids.addElement(n0.getText());
			}
			{
			_loop206:
			do {
				if ((LA(1)==Comma)) {
					match(Comma);
					n1 = LT(1);
					match(Identifier);
					if ( inputState.guessing==0 ) {
						ids.addElement(n1.getText());
					}
				}
				else {
					break _loop206;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_14);
			} else {
			  throw ex;
			}
		}
		return ids;
	}
	
	protected final Vector<FormalDecl>  parameterTypeList() throws RecognitionException, TokenStreamException {
		Vector<FormalDecl> p = null;;
		
		
		try {      // for error handling
			if ( inputState.guessing==0 ) {
				
				p = new Vector<FormalDecl>(8);
				
			}
			parameterList(p);
			{
			switch ( LA(1)) {
			case Comma:
			{
				match(Comma);
				match(Varargs);
				if ( inputState.guessing==0 ) {
					p.addElement(new UnknownFormals());
				}
				break;
			}
			case RParen:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_14);
			} else {
			  throw ex;
			}
		}
		return p;
	}
	
	protected final void parameterList(
		Vector<FormalDecl> v
	) throws RecognitionException, TokenStreamException {
		
		
		FormalDecl fd      = null;
		int        counter = 0;
		
		
		try {      // for error handling
			fd=parameterDeclaration(counter++);
			if ( inputState.guessing==0 ) {
				addParameter(fd, v, LT(0));
			}
			{
			_loop197:
			do {
				if ((LA(1)==Comma) && (_tokenSet_2.member(LA(2)))) {
					match(Comma);
					fd=parameterDeclaration(counter++);
					if ( inputState.guessing==0 ) {
						addParameter(fd, v, LT(0));
					}
				}
				else {
					break _loop197;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_55);
			} else {
			  throw ex;
			}
		}
	}
	
	protected final FormalDecl  parameterDeclaration(
		int counter
	) throws RecognitionException, TokenStreamException {
		FormalDecl fd=null;
		
		
		Type   type             = null;
		int    saveStorageClass = storageClass;
		String atts             = "";
		
		
		try {      // for error handling
			type=declarationSpecifiers();
			if ( inputState.guessing==0 ) {
				
				storageClass  = saveStorageClass;
				
			}
			{
			boolean synPredMatched201 = false;
			if (((_tokenSet_56.member(LA(1))) && (_tokenSet_57.member(LA(2))))) {
				int _m201 = mark();
				synPredMatched201 = true;
				inputState.guessing++;
				try {
					{
					formalDeclarator(type);
					}
				}
				catch (RecognitionException pe) {
					synPredMatched201 = false;
				}
				rewind(_m201);
				inputState.guessing--;
			}
			if ( synPredMatched201 ) {
				fd=formalDeclarator(type);
			}
			else if ((LA(1)==LParen||LA(1)==LBracket||LA(1)==Mult) && (_tokenSet_58.member(LA(2)))) {
				type=abstractDeclarator(type);
				{
				switch ( LA(1)) {
				case LITERAL___restrict:
				{
					match(LITERAL___restrict);
					break;
				}
				case RParen:
				case Comma:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				if ( inputState.guessing==0 ) {
					fd = createFormalDecl(null, type, counter);
				}
			}
			else if ((LA(1)==LITERAL___restrict) && (LA(2)==RParen||LA(2)==Comma)) {
				match(LITERAL___restrict);
				if ( inputState.guessing==0 ) {
					fd = createFormalDecl(null, type, counter);
				}
			}
			else if ((LA(1)==RParen||LA(1)==Comma)) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			if ( inputState.guessing==0 ) {
				
				if ((fd == null) && (type != void_type))
				fd = createFormalDecl(null, type, counter);
				
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_55);
			} else {
			  throw ex;
			}
		}
		return fd;
	}
	
	protected final FormalDecl  formalDeclarator(
		Type type
	) throws RecognitionException, TokenStreamException {
		FormalDecl decl = null;
		
		
		String saveIdentifier = declID;
		
		
		try {      // for error handling
			type=declarator2(type);
			if ( inputState.guessing==0 ) {
				
				decl = createFormalDecl(declID, type, 0);
				declID = saveIdentifier;
				
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_59);
			} else {
			  throw ex;
			}
		}
		return decl;
	}
	
	protected final Type  directAbstractDeclarator(
		Type type
	) throws RecognitionException, TokenStreamException {
		Type ntype=type;
		
		
		int pos = 0;
		
		
		try {      // for error handling
			{
			int _cnt217=0;
			_loop217:
			do {
				switch ( LA(1)) {
				case LParen:
				{
					ntype=directFunction(ntype);
					if ( inputState.guessing==0 ) {
						pos = 0;
					}
					break;
				}
				case LBracket:
				{
					ntype=directArray(ntype, pos++);
					break;
				}
				default:
				{
					if ( _cnt217>=1 ) { break _loop217; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				}
				_cnt217++;
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_21);
			} else {
			  throw ex;
			}
		}
		return ntype;
	}
	
	protected final Type  directFunction(
		Type type
	) throws RecognitionException, TokenStreamException {
		Type pt = type;
		
		Token  l0 = null;
		Token  l1 = null;
		Token  l2 = null;
		
		Vector<FormalDecl> v      = null;
		IncompleteType     ictype = null;
		
		
		try {      // for error handling
			if ((LA(1)==LParen) && (LA(2)==RParen)) {
				l0 = LT(1);
				match(LParen);
				match(RParen);
				if ( inputState.guessing==0 ) {
					pt = createProcedureType(type, new Vector<FormalDecl>(0), l0);
				}
			}
			else if ((LA(1)==LParen) && (LA(2)==LParen||LA(2)==LBracket||LA(2)==Mult)) {
				l1 = LT(1);
				match(LParen);
				if ( inputState.guessing==0 ) {
					
					ictype = new IncompleteType();
					
				}
				pt=abstractDeclarator(ictype);
				match(RParen);
				if ( inputState.guessing==0 ) {
					
					Type rt = ProcedureType.create(type, new Vector<FormalDecl>(0), null);
					ictype.setCompleteType(rt);
					
				}
			}
			else if ((LA(1)==LParen) && (_tokenSet_2.member(LA(2)))) {
				l2 = LT(1);
				match(LParen);
				v=parameterTypeList();
				match(RParen);
				if ( inputState.guessing==0 ) {
					pt = createProcedureType(type, v, l2);
				}
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_60);
			} else {
			  throw ex;
			}
		}
		return pt;
	}
	
	protected final Type  directArray(
		Type type, int pos
	) throws RecognitionException, TokenStreamException {
		Type ntype = type;
		
		Token  s0 = null;
		
		Expression exp = null;
		
		
		try {      // for error handling
			s0 = LT(1);
			match(LBracket);
			{
			if ((LA(1)==RBracket)) {
				match(RBracket);
				if ( inputState.guessing==0 ) {
					ntype = createArrayType(pos == 0, type, s0);
				}
			}
			else {
				boolean synPredMatched222 = false;
				if (((LA(1)==Mult) && (LA(2)==RBracket))) {
					int _m222 = mark();
					synPredMatched222 = true;
					inputState.guessing++;
					try {
						{
						match(Mult);
						match(RBracket);
						}
					}
					catch (RecognitionException pe) {
						synPredMatched222 = false;
					}
					rewind(_m222);
					inputState.guessing--;
				}
				if ( synPredMatched222 ) {
					match(Mult);
					match(RBracket);
					if ( inputState.guessing==0 ) {
						ntype = createArrayType(pos == 0, type, s0);
					}
				}
				else if ((_tokenSet_0.member(LA(1))) && (_tokenSet_47.member(LA(2)))) {
					exp=constantExpression();
					match(RBracket);
					if ( inputState.guessing==0 ) {
						ntype = createArrayType(pos == 0, 0, getLongValue(exp, s0), ntype, s0);
					}
				}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
			}
			catch (RecognitionException ex) {
				if (inputState.guessing==0) {
					reportError(ex);
					consume();
					consumeUntil(_tokenSet_60);
				} else {
				  throw ex;
				}
			}
			return ntype;
		}
		
	protected final int  initializerListItem(
		Vector<Object> v, int nposi, Type type
	) throws RecognitionException, TokenStreamException {
		int npos = 0;
		
		Token  n1 = null;
		
		Vector<Object> positions = null;
		Expression     expr      = null;
		Type           ntype     = null;
		
		
		try {      // for error handling
			if ((_tokenSet_61.member(LA(1))) && (_tokenSet_62.member(LA(2)))) {
				if ( inputState.guessing==0 ) {
					
					npos = nposi;
					
				}
				{
				switch ( LA(1)) {
				case Dot:
				case LBracket:
				{
					positions=designation();
					break;
				}
				case KEYWORD_sizeof:
				case KEYWORD_Extension:
				case KEYWORD_alignof:
				case KEYWORD_va_arg:
				case KEYWORD_builtin_va_arg:
				case KEYWORD_builtin_offsetof:
				case Identifier:
				case HexDoubleValue:
				case HexFloatValue:
				case HexLongDoubleValue:
				case HexUnsignedIntValue:
				case HexUnsignedLongIntValue:
				case HexUnsignedLongLongIntValue:
				case HexIntValue:
				case HexLongIntValue:
				case HexLongLongIntValue:
				case DoubleValue:
				case FloatValue:
				case LongDoubleValue:
				case UnsignedIntValue:
				case UnsignedLongIntValue:
				case UnsignedLongLongIntValue:
				case IntValue:
				case LongIntValue:
				case LongLongIntValue:
				case CharacterConstant:
				case WideCharacterConstant:
				case StringLit:
				case WideStringLiteral:
				case LParen:
				case LBrace:
				case Dec:
				case Inc:
				case And:
				case Mult:
				case Plus:
				case Sub:
				case Comp:
				case Not:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				if ( inputState.guessing==0 ) {
					
					ntype = findType(type, npos, positions, LT(1));
					
				}
				expr=initializer(ntype);
				if ( inputState.guessing==0 ) {
					
					npos = processInitializers(npos, type, v, expr, positions, LT(1));
					positions = null;
					
				}
			}
			else if (((LA(1)==Identifier) && (LA(2)==Colon))&&(allowGNUExtensions)) {
				n1 = LT(1);
				match(Identifier);
				match(Colon);
				if ( inputState.guessing==0 ) {
					
					positions = new Vector<Object>(1);
					positions.addElement(n1.getText());
					ntype = findType(type, npos, positions, LT(1));
					
				}
				expr=initializer(ntype);
				if ( inputState.guessing==0 ) {
					
					npos = processInitializers(npos, type, v, expr, positions, LT(1));
					positions = null;
					
				}
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_52);
			} else {
			  throw ex;
			}
		}
		return npos;
	}
	
	protected final Vector<Object>  designation() throws RecognitionException, TokenStreamException {
		Vector<Object> positions = null;
		
		
		try {      // for error handling
			if ( inputState.guessing==0 ) {
				
				positions = new Vector<Object>(10);
				
			}
			{
			int _cnt234=0;
			_loop234:
			do {
				if ((LA(1)==Dot||LA(1)==LBracket)) {
					designator(positions);
				}
				else {
					if ( _cnt234>=1 ) { break _loop234; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt234++;
			} while (true);
			}
			match(Assign);
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_63);
			} else {
			  throw ex;
			}
		}
		return positions;
	}
	
	protected final void designator(
		Vector<Object> v
	) throws RecognitionException, TokenStreamException {
		
		Token  n0 = null;
		
		Expression expr = null;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case LBracket:
			{
				match(LBracket);
				expr=constantExpression();
				match(RBracket);
				if ( inputState.guessing==0 ) {
					v.addElement(expr);
				}
				break;
			}
			case Dot:
			{
				match(Dot);
				n0 = LT(1);
				match(Identifier);
				if ( inputState.guessing==0 ) {
					v.addElement(n0.getText());
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_64);
			} else {
			  throw ex;
			}
		}
	}
	
	protected final Statement  statement() throws RecognitionException, TokenStreamException {
		Statement stmt=errStmt;
		
		Token  s1 = null;
		Token  s2 = null;
		Token  l = null;
		Token  lab = null;
		
		Expression expr1 = null;
		Statement  stmt1 = null;
		Token      t     = null;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case KEYWORD_case:
			{
				s1 = LT(1);
				match(KEYWORD_case);
				expr1=constantExpression();
				match(Colon);
				stmt1=statement();
				if ( inputState.guessing==0 ) {
					stmt = buildCaseStmt(expr1, stmt1, s1);
				}
				break;
			}
			case KEYWORD_default:
			{
				s2 = LT(1);
				match(KEYWORD_default);
				match(Colon);
				stmt1=statement();
				if ( inputState.guessing==0 ) {
					stmt = buildCaseStmt(null, stmt1, s2);
				}
				break;
			}
			case LBrace:
			{
				stmt=compoundStatement(false);
				break;
			}
			case KEYWORD_if:
			case KEYWORD_switch:
			{
				stmt=selectionStatement();
				break;
			}
			case KEYWORD_do:
			case KEYWORD_for:
			case KEYWORD_while:
			{
				stmt=iterationStatement();
				break;
			}
			case KEYWORD_break:
			case KEYWORD_continue:
			case KEYWORD_goto:
			case KEYWORD_return:
			{
				stmt=jumpStatement();
				break;
			}
			default:
				boolean synPredMatched238 = false;
				if (((LA(1)==Identifier) && (LA(2)==Colon))) {
					int _m238 = mark();
					synPredMatched238 = true;
					inputState.guessing++;
					try {
						{
						match(Identifier);
						match(Colon);
						}
					}
					catch (RecognitionException pe) {
						synPredMatched238 = false;
					}
					rewind(_m238);
					inputState.guessing--;
				}
				if ( synPredMatched238 ) {
					l = LT(1);
					match(Identifier);
					match(Colon);
					if ( inputState.guessing==0 ) {
						t = LT(1);
					}
					stmt1=statement();
					if ( inputState.guessing==0 ) {
						stmt = buildLabelStmt(l.getText(), stmt1, t);
					}
				}
				else if (((LA(1)==KEYWORD_label))&&(allowGNUExtensions)) {
					match(KEYWORD_label);
					lab = LT(1);
					match(Identifier);
					match(Semi);
					if ( inputState.guessing==0 ) {
						getLabelDecl(lab.getText()); stmt = new NullStmt();
					}
				}
				else if ((_tokenSet_65.member(LA(1))) && (_tokenSet_66.member(LA(2)))) {
					stmt=expressionStatement();
				}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_67);
			} else {
			  throw ex;
			}
		}
		return stmt;
	}
	
	protected final Statement  expressionStatement() throws RecognitionException, TokenStreamException {
		Statement stmt=errStmt;
		
		Token  n0 = null;
		
		Expression expr   = null;
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case KEYWORD_sizeof:
			case KEYWORD_Extension:
			case KEYWORD_alignof:
			case KEYWORD_va_arg:
			case KEYWORD_builtin_va_arg:
			case KEYWORD_builtin_offsetof:
			case Identifier:
			case HexDoubleValue:
			case HexFloatValue:
			case HexLongDoubleValue:
			case HexUnsignedIntValue:
			case HexUnsignedLongIntValue:
			case HexUnsignedLongLongIntValue:
			case HexIntValue:
			case HexLongIntValue:
			case HexLongLongIntValue:
			case DoubleValue:
			case FloatValue:
			case LongDoubleValue:
			case UnsignedIntValue:
			case UnsignedLongIntValue:
			case UnsignedLongLongIntValue:
			case IntValue:
			case LongIntValue:
			case LongLongIntValue:
			case CharacterConstant:
			case WideCharacterConstant:
			case StringLit:
			case WideStringLiteral:
			case LParen:
			case Dec:
			case Inc:
			case And:
			case Mult:
			case Plus:
			case Sub:
			case Comp:
			case Not:
			{
				expr=expression();
				break;
			}
			case Semi:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			n0 = LT(1);
			match(Semi);
			if ( inputState.guessing==0 ) {
				stmt = buildExpressionStmt(expr, n0);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				
				reportError(ex);
				consumeUntil(Semi);
				consume();
				fatalError = true;
				return null;
				
			} else {
				throw ex;
			}
		}
		return stmt;
	}
	
	protected final Statement  selectionStatement() throws RecognitionException, TokenStreamException {
		Statement stmt=errStmt;
		
		Token  i = null;
		Token  s = null;
		
		Expression expr  = null;
		Statement  stmtt = null;
		Statement  stmtf = null;
		Token      token = null;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case KEYWORD_if:
			{
				i = LT(1);
				match(KEYWORD_if);
				expr=enclosedExpression();
				stmtt=statement();
				{
				boolean synPredMatched252 = false;
				if (((LA(1)==KEYWORD_else) && (_tokenSet_5.member(LA(2))))) {
					int _m252 = mark();
					synPredMatched252 = true;
					inputState.guessing++;
					try {
						{
						match(KEYWORD_else);
						}
					}
					catch (RecognitionException pe) {
						synPredMatched252 = false;
					}
					rewind(_m252);
					inputState.guessing--;
				}
				if ( synPredMatched252 ) {
					match(KEYWORD_else);
					stmtf=statement();
				}
				else if ((_tokenSet_67.member(LA(1))) && (_tokenSet_68.member(LA(2)))) {
				}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				
				}
				if ( inputState.guessing==0 ) {
					stmt = buildIfStmt(expr, stmtt, stmtf, i);
				}
				break;
			}
			case KEYWORD_switch:
			{
				s = LT(1);
				match(KEYWORD_switch);
				expr=enclosedExpression();
				stmtt=statement();
				if ( inputState.guessing==0 ) {
					stmt = buildSwitchStmt(expr, stmtt, s);
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_67);
			} else {
			  throw ex;
			}
		}
		return stmt;
	}
	
	protected final Statement  iterationStatement() throws RecognitionException, TokenStreamException {
		Statement stmt=errStmt;
		
		Token  w = null;
		Token  d = null;
		Token  f = null;
		
		Declaration decl  = null;
		Type        type  = null;
		Expression  expr1 = null;
		Expression  expr2 = null;
		Expression  expr3 = null;
		
		Expression  exprThdNum = null;
		
		Statement   istmt = null;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case KEYWORD_while:
			{
				w = LT(1);
				match(KEYWORD_while);
				expr1=enclosedExpression();
				istmt=statement();
				if ( inputState.guessing==0 ) {
					stmt = buildWhileStmt(expr1, istmt, w);
				}
				break;
			}
			case KEYWORD_do:
			{
				d = LT(1);
				match(KEYWORD_do);
				istmt=statement();
				match(KEYWORD_while);
				expr1=enclosedExpression();
				match(Semi);
				if ( inputState.guessing==0 ) {
					stmt = buildDoWhileStmt(expr1, istmt, d);
				}
				break;
			}
			case KEYWORD_for:
			{
				f = LT(1);
				match(KEYWORD_for);
				match(LParen);
				{
				if ((LA(1)==Semi)) {
					match(Semi);
				}
				else {
					boolean synPredMatched256 = false;
					if ((((_tokenSet_2.member(LA(1))) && (_tokenSet_69.member(LA(2))))&&(allowC99Extensions))) {
						int _m256 = mark();
						synPredMatched256 = true;
						inputState.guessing++;
						try {
							{
							declarationSpecifiersChk();
							}
						}
						catch (RecognitionException pe) {
							synPredMatched256 = false;
						}
						rewind(_m256);
						inputState.guessing--;
					}
					if ( synPredMatched256 ) {
						type=declarationSpecifiers();
						decl=declarator(type, 0);
						match(Assign);
						expr1=initializer(type);
						match(Semi);
						if ( inputState.guessing==0 ) {
							expr1 = new AssignSimpleOp(type, genDeclAddress(decl), expr1);
						}
					}
					else if ((_tokenSet_0.member(LA(1))) && (_tokenSet_70.member(LA(2)))) {
						expr1=expression();
						match(Semi);
					}
					else {
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					{
					switch ( LA(1)) {
					case KEYWORD_sizeof:
					case KEYWORD_Extension:
					case KEYWORD_alignof:
					case KEYWORD_va_arg:
					case KEYWORD_builtin_va_arg:
					case KEYWORD_builtin_offsetof:
					case Identifier:
					case HexDoubleValue:
					case HexFloatValue:
					case HexLongDoubleValue:
					case HexUnsignedIntValue:
					case HexUnsignedLongIntValue:
					case HexUnsignedLongLongIntValue:
					case HexIntValue:
					case HexLongIntValue:
					case HexLongLongIntValue:
					case DoubleValue:
					case FloatValue:
					case LongDoubleValue:
					case UnsignedIntValue:
					case UnsignedLongIntValue:
					case UnsignedLongLongIntValue:
					case IntValue:
					case LongIntValue:
					case LongLongIntValue:
					case CharacterConstant:
					case WideCharacterConstant:
					case StringLit:
					case WideStringLiteral:
					case LParen:
					case Dec:
					case Inc:
					case And:
					case Mult:
					case Plus:
					case Sub:
					case Comp:
					case Not:
					{
						expr2=expression();
						break;
					}
					case Semi:
					{
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					match(Semi);
					{
					switch ( LA(1)) {
					case KEYWORD_sizeof:
					case KEYWORD_Extension:
					case KEYWORD_alignof:
					case KEYWORD_va_arg:
					case KEYWORD_builtin_va_arg:
					case KEYWORD_builtin_offsetof:
					case Identifier:
					case HexDoubleValue:
					case HexFloatValue:
					case HexLongDoubleValue:
					case HexUnsignedIntValue:
					case HexUnsignedLongIntValue:
					case HexUnsignedLongLongIntValue:
					case HexIntValue:
					case HexLongIntValue:
					case HexLongLongIntValue:
					case DoubleValue:
					case FloatValue:
					case LongDoubleValue:
					case UnsignedIntValue:
					case UnsignedLongIntValue:
					case UnsignedLongLongIntValue:
					case IntValue:
					case LongIntValue:
					case LongLongIntValue:
					case CharacterConstant:
					case WideCharacterConstant:
					case StringLit:
					case WideStringLiteral:
					case LParen:
					case Dec:
					case Inc:
					case And:
					case Mult:
					case Plus:
					case Sub:
					case Comp:
					case Not:
					{
						expr3=expression();
						break;
					}
					case RParen:
					{
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					match(RParen);
					{
					switch ( LA(1)) {
					case KEYWORD_clone:
					{
						match(KEYWORD_clone);
						match(LParen);
						exprThdNum=expression();
						match(RParen);
						break;
					}
					case KEYWORD_break:
					case KEYWORD_case:
					case KEYWORD_continue:
					case KEYWORD_default:
					case KEYWORD_do:
					case KEYWORD_for:
					case KEYWORD_goto:
					case KEYWORD_if:
					case KEYWORD_return:
					case KEYWORD_sizeof:
					case KEYWORD_switch:
					case KEYWORD_label:
					case KEYWORD_while:
					case KEYWORD_Extension:
					case KEYWORD_alignof:
					case KEYWORD_va_arg:
					case KEYWORD_builtin_va_arg:
					case KEYWORD_builtin_offsetof:
					case Identifier:
					case HexDoubleValue:
					case HexFloatValue:
					case HexLongDoubleValue:
					case HexUnsignedIntValue:
					case HexUnsignedLongIntValue:
					case HexUnsignedLongLongIntValue:
					case HexIntValue:
					case HexLongIntValue:
					case HexLongLongIntValue:
					case DoubleValue:
					case FloatValue:
					case LongDoubleValue:
					case UnsignedIntValue:
					case UnsignedLongIntValue:
					case UnsignedLongLongIntValue:
					case IntValue:
					case LongIntValue:
					case LongLongIntValue:
					case CharacterConstant:
					case WideCharacterConstant:
					case StringLit:
					case WideStringLiteral:
					case LParen:
					case LBrace:
					case Dec:
					case Inc:
					case And:
					case Mult:
					case Plus:
					case Sub:
					case Comp:
					case Not:
					case Semi:
					{
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					istmt=statement();
					if ( inputState.guessing==0 ) {
						
								 stmt = exprThdNum == null ? buildForStmt (expr1, expr2, expr3, istmt, f) : buildCloneForStmt (expr1, expr2, expr3, istmt, f, exprThdNum);
						
					}
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
			}
			catch (RecognitionException ex) {
				if (inputState.guessing==0) {
					reportError(ex);
					consume();
					consumeUntil(_tokenSet_67);
				} else {
				  throw ex;
				}
			}
			return stmt;
		}
		
	protected final Statement  jumpStatement() throws RecognitionException, TokenStreamException {
		Statement stmt=errStmt;
		
		Token  g = null;
		Token  l = null;
		Token  c = null;
		Token  b = null;
		Token  r = null;
		
		Expression expr = null;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case KEYWORD_goto:
			{
				g = LT(1);
				match(KEYWORD_goto);
				l = LT(1);
				match(Identifier);
				match(Semi);
				if ( inputState.guessing==0 ) {
					stmt = buildGotoStmt(l.getText(), l);
				}
				break;
			}
			case KEYWORD_continue:
			{
				c = LT(1);
				match(KEYWORD_continue);
				match(Semi);
				if ( inputState.guessing==0 ) {
					stmt = buildContinueStmt(c);
				}
				break;
			}
			case KEYWORD_break:
			{
				b = LT(1);
				match(KEYWORD_break);
				match(Semi);
				if ( inputState.guessing==0 ) {
					stmt = buildBreakStmt(b);
				}
				break;
			}
			case KEYWORD_return:
			{
				r = LT(1);
				match(KEYWORD_return);
				{
				switch ( LA(1)) {
				case KEYWORD_sizeof:
				case KEYWORD_Extension:
				case KEYWORD_alignof:
				case KEYWORD_va_arg:
				case KEYWORD_builtin_va_arg:
				case KEYWORD_builtin_offsetof:
				case Identifier:
				case HexDoubleValue:
				case HexFloatValue:
				case HexLongDoubleValue:
				case HexUnsignedIntValue:
				case HexUnsignedLongIntValue:
				case HexUnsignedLongLongIntValue:
				case HexIntValue:
				case HexLongIntValue:
				case HexLongLongIntValue:
				case DoubleValue:
				case FloatValue:
				case LongDoubleValue:
				case UnsignedIntValue:
				case UnsignedLongIntValue:
				case UnsignedLongLongIntValue:
				case IntValue:
				case LongIntValue:
				case LongLongIntValue:
				case CharacterConstant:
				case WideCharacterConstant:
				case StringLit:
				case WideStringLiteral:
				case LParen:
				case Dec:
				case Inc:
				case And:
				case Mult:
				case Plus:
				case Sub:
				case Comp:
				case Not:
				{
					expr=expression();
					break;
				}
				case Semi:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				match(Semi);
				if ( inputState.guessing==0 ) {
					stmt = buildReturnStmt(expr, r);
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				
				reportError(ex);
				consumeUntil(Semi);
				consume();
				fatalError = true;
				return null;
				
			} else {
				throw ex;
			}
		}
		return stmt;
	}
	
	protected final void typedef() throws RecognitionException, TokenStreamException {
		
		
		Type        type = null;
		Declaration decl = null;
		
		
		try {      // for error handling
			match(KEYWORD_typedef);
			type=declarationSpecifiers();
			if ( inputState.guessing==0 ) {
				
				if (type == null) {
				type = void_type;
				userError(Msg.MSG_Invalid_type, null, LT(1));
				}
				
			}
			typeDeclarator(type);
			{
			_loop269:
			do {
				if ((LA(1)==Comma)) {
					match(Comma);
					typeDeclarator(type);
				}
				else {
					break _loop269;
				}
				
			} while (true);
			}
			match(Semi);
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				
				reportError(ex);
				consumeUntil(Semi);
				consume();
				fatalError = true;
				
			} else {
				throw ex;
			}
		}
	}
	
	protected final Expression  enclosedExpression() throws RecognitionException, TokenStreamException {
		Expression expr = errExp;
		
		Token  n0 = null;
		
		try {      // for error handling
			n0 = LT(1);
			match(LParen);
			expr=expression();
			match(RParen);
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				
				reportError(ex);
				consumeUntil(RParen);
				consume();
				fatalError = true;
				return null;
				
			} else {
				throw ex;
			}
		}
		return expr;
	}
	
	protected final void typeDeclarator(
		Type type
	) throws RecognitionException, TokenStreamException {
		
		
		String saveIdentifier = declID;
		
		
		try {      // for error handling
			type=declarator2(type);
			{
			switch ( LA(1)) {
			case Attributes:
			{
				type=getAttributes(type);
				break;
			}
			case Comma:
			case Semi:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			if ( inputState.guessing==0 ) {
				
				processTypeDeclarator(declID, type, LT(1));
				declID = saveIdentifier;
				
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_38);
			} else {
			  throw ex;
			}
		}
	}
	
	protected final void translationUnit() throws RecognitionException, TokenStreamException {
		
		Token  l = null;
		Token  p = null;
		Token  er = null;
		Token  wn = null;
		Token  n0 = null;
		
		FileDecl            root   = null;
		Vector<Declaration> rdecls = null;
		Type                type   = null;
		
		
		try {      // for error handling
			if ( inputState.guessing==0 ) {
				
				cg.getSymbolTable().beginScope();
				initialize();
				root = new FileDecl(cg.getName());
				rdecls = new Vector<Declaration>(23);
				root.setDecls(rdecls);
				cg.setAST(root);
				
			}
			{
			_loop274:
			do {
				switch ( LA(1)) {
				case PPLine:
				{
					l = LT(1);
					match(PPLine);
					if ( inputState.guessing==0 ) {
						processLine(l.getText(), l);
					}
					break;
				}
				case StdArg:
				{
					match(StdArg);
					if ( inputState.guessing==0 ) {
						
						if (!vaListDefined) {
						vaListDefined = true;
						cg.addRootSymbol(defineTypeName("va_list", RefType.create(Machine.currentMachine.getVaListType(), RefAttr.VaList)));
						cg.addRootSymbol(defineTypeName("__va_list", RefType.create(Machine.currentMachine.getVaListType(), RefAttr.VaList)));
						if (allowGNUExtensions) {
						cg.addRootSymbol(defineTypeName("__gnuc_va_list", RefType.create(Machine.currentMachine.getVaListType(), RefAttr.VaList)));
						}
						}
						
					}
					break;
				}
				case Pragma:
				{
					p = LT(1);
					match(Pragma);
					if ( inputState.guessing==0 ) {
						processPragma(p.getText(), p);
					}
					break;
				}
				case PPError:
				{
					er = LT(1);
					match(PPError);
					if ( inputState.guessing==0 ) {
						userError(Msg.MSG_s, er.getText(), er);
					}
					break;
				}
				case PPWarning:
				{
					wn = LT(1);
					match(PPWarning);
					if ( inputState.guessing==0 ) {
						userWarning(Msg.MSG_s, wn.getText(), wn);
					}
					break;
				}
				case KEYWORD_Extension:
				{
					n0 = LT(1);
					match(KEYWORD_Extension);
					if ( inputState.guessing==0 ) {
						
						if (!allowGNUExtensions)
						userWarning(Msg.MSG_Ignored_s, n0.getText(), n0);
						
					}
					break;
				}
				case KEYWORD_typedef:
				{
					typedef();
					break;
				}
				case KEYWORD_auto:
				case KEYWORD_char:
				case KEYWORD_const:
				case KEYWORD___const:
				case KEYWORD_double:
				case KEYWORD_enum:
				case KEYWORD_extern:
				case KEYWORD_float:
				case KEYWORD_inline:
				case KEYWORD_int:
				case KEYWORD_long:
				case KEYWORD_register:
				case KEYWORD_restrict:
				case KEYWORD_short:
				case KEYWORD_signed:
				case KEYWORD___signed__:
				case KEYWORD_static:
				case KEYWORD_struct:
				case KEYWORD_typeof:
				case KEYWORD_union:
				case KEYWORD_unsigned:
				case KEYWORD_void:
				case KEYWORD_volatile:
				case KEYWORD__volatile:
				case KEYWORD__Bool:
				case KEYWORD__Complex:
				case KEYWORD__Imaginary:
				case KEYWORD__inline__:
				case KEYWORD__inline:
				case Identifier:
				case LParen:
				case Mult:
				case Semi:
				case LITERAL___restrict:
				case Attributes:
				{
					ftnOrDecl();
					break;
				}
				default:
				{
					break _loop274;
				}
				}
			} while (true);
			}
			match(Token.EOF_TYPE);
			if ( inputState.guessing==0 ) {
				
				finishTranslationUnit(root, rdecls, LT(0));
				
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				
				reportError(ex);
				if (classTrace)
				ex.printStackTrace();
				consumeUntil(EOF);
				throw ex;
				
			} else {
				throw ex;
			}
		}
		catch (antlr.TokenStreamRecognitionException ex) {
			if (inputState.guessing==0) {
				
				reportError(ex);
				if (classTrace)
				ex.printStackTrace();
				consumeUntil(EOF);
				throw ex;
				
			} else {
				throw ex;
			}
		}
	}
	
	protected final void ftnOrDecl() throws RecognitionException, TokenStreamException {
		
		Token  att0 = null;
		Token  n1 = null;
		Token  n2 = null;
		
		Type          type  = int_type;
		Type          dtype = int_type;
		ProcedureType pt    = null;
		Declaration   decl  = null;
		Vector<Declaration> vars  = null;
		Expression    expr  = null;
		int           sc    = 0;
		String        attrs = "";
		Token         t     = null;
		
		
		try {      // for error handling
			if ( inputState.guessing==0 ) {
				
				declAtts     = "";
				storageClass = cAuto;
				
			}
			{
			boolean synPredMatched278 = false;
			if (((_tokenSet_2.member(LA(1))) && (_tokenSet_3.member(LA(2))))) {
				int _m278 = mark();
				synPredMatched278 = true;
				inputState.guessing++;
				try {
					{
					declarationSpecifiersChk();
					}
				}
				catch (RecognitionException pe) {
					synPredMatched278 = false;
				}
				rewind(_m278);
				inputState.guessing--;
			}
			if ( synPredMatched278 ) {
				type=declarationSpecifiers();
				if ( inputState.guessing==0 ) {
					
					sc = storageClass;
					if (sc == cAuto)
					sc = cGlobal;
					
				}
			}
			else if ((_tokenSet_71.member(LA(1))) && (_tokenSet_72.member(LA(2)))) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			{
			_loop280:
			do {
				if ((LA(1)==Attributes) && (_tokenSet_71.member(LA(2)))) {
					att0 = LT(1);
					match(Attributes);
					if ( inputState.guessing==0 ) {
						declAtts = declAtts + att0.getText();
					}
				}
				else {
					break _loop280;
				}
				
			} while (true);
			}
			{
			switch ( LA(1)) {
			case Semi:
			{
				match(Semi);
				if ( inputState.guessing==0 ) {
					
					declAtts = "";
					
				}
				break;
			}
			case Identifier:
			case LParen:
			case Mult:
			case LITERAL___restrict:
			case Attributes:
			{
				dtype=declarator2(type);
				if ( inputState.guessing==0 ) {
					
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
				{
				switch ( LA(1)) {
				case Semi:
				{
					match(Semi);
					if ( inputState.guessing==0 ) {
						
						decl = defineDecl(declID, dtype, sc, t);
						processAttributes(attrs, decl, t);
						vars = new Vector<Declaration>(1);
						vars.add(decl);
						processTopDecls(vars, sc, t);
						
					}
					break;
				}
				case Comma:
				{
					match(Comma);
					if ( inputState.guessing==0 ) {
						
						decl = defineDecl(declID, dtype, sc, t);
						processAttributes(attrs, decl, t);
						vars = new Vector<Declaration>(4);
						vars.add(decl);
						
					}
					initDeclaratorList(vars, type, sc);
					n1 = LT(1);
					match(Semi);
					if ( inputState.guessing==0 ) {
						
						processTopDecls(vars, sc, n1);
						
					}
					break;
				}
				case Assign:
				{
					match(Assign);
					if ( inputState.guessing==0 ) {
						
						decl = defineDecl(declID, dtype, sc, t);
						processAttributes(attrs, decl, t);
						
					}
					expr=initializer(dtype);
					if ( inputState.guessing==0 ) {
						
						processDeclInitializer(decl, dtype, expr, t);
						vars = new Vector<Declaration>(4);
						vars.add(decl);
						
					}
					{
					switch ( LA(1)) {
					case Comma:
					{
						match(Comma);
						initDeclaratorList(vars, type, sc);
						break;
					}
					case Semi:
					{
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					n2 = LT(1);
					match(Semi);
					if ( inputState.guessing==0 ) {
						
						processTopDecls(vars, sc, n2);
						
					}
					break;
				}
				default:
					if (((_tokenSet_73.member(LA(1))))&&(pt != null)) {
						ftnDef(pt, declID, attrs, sc);
					}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_74);
			} else {
			  throw ex;
			}
		}
	}
	
	protected final void ftnDef(
		ProcedureType pt, String name, String attrs, int sc
	) throws RecognitionException, TokenStreamException {
		
		
		Token       t    = LT(1);
		BlockStmt   stmt = null;
		RoutineDecl rd   = null;
		
		
		try {      // for error handling
			{
			_loop286:
			do {
				if ((_tokenSet_2.member(LA(1)))) {
					krArg(pt);
				}
				else {
					break _loop286;
				}
				
			} while (true);
			}
			if ( inputState.guessing==0 ) {
				
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
			stmt=compoundStatement(true);
			if ( inputState.guessing==0 ) {
				
				finishFunction(rd, stmt, sc, t);
				cg.getSymbolTable().endScope();
				labels.clear();
				
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_74);
			} else {
			  throw ex;
			}
		}
	}
	
	protected final void krArg(
		ProcedureType pt
	) throws RecognitionException, TokenStreamException {
		
		
		Type       type = null;
		FormalDecl fd   = null;
		
		
		try {      // for error handling
			type=declarationSpecifiers();
			fd=formalDeclarator(type);
			if ( inputState.guessing==0 ) {
				
				if (!fatalError) {
				String     name = fd.getName();
				FormalDecl fd2  = pt.getFormal(name);
				if (fd2 == null)
				userError(Msg.MSG_Invalid_parameter_s, name, LT(0));
				fd2.setType(fd.getType());
				}
				
			}
			{
			_loop289:
			do {
				if ((LA(1)==Comma)) {
					match(Comma);
					fd=formalDeclarator(type);
					if ( inputState.guessing==0 ) {
						
						if (!fatalError) {
						String     name = fd.getName();
						FormalDecl fd2  = pt.getFormal(name);
						if (fd2 == null)
						userError(Msg.MSG_Invalid_parameter_s, name, LT(0));
						fd2.setType(fd.getType());
						}
						
					}
				}
				else {
					break _loop289;
				}
				
			} while (true);
			}
			match(Semi);
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_73);
			} else {
			  throw ex;
			}
		}
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"\"auto\"",
		"\"break\"",
		"\"case\"",
		"\"char\"",
		"\"const\"",
		"\"__const\"",
		"\"continue\"",
		"\"default\"",
		"\"do\"",
		"\"double\"",
		"\"else\"",
		"\"enum\"",
		"\"extern\"",
		"\"float\"",
		"\"for\"",
		"\"goto\"",
		"\"if\"",
		"\"inline\"",
		"\"int\"",
		"\"long\"",
		"\"register\"",
		"\"restrict\"",
		"\"return\"",
		"\"short\"",
		"\"signed\"",
		"\"__signed__\"",
		"\"sizeof\"",
		"\"static\"",
		"\"struct\"",
		"\"switch\"",
		"\"typedef\"",
		"\"__label__\"",
		"\"typeof\"",
		"\"union\"",
		"\"unsigned\"",
		"\"void\"",
		"\"volatile\"",
		"\"__volatile\"",
		"\"while\"",
		"\"_Bool\"",
		"\"_Complex\"",
		"\"_Imaginary\"",
		"\"__extension__\"",
		"\"__alignof__\"",
		"\"__inline__\"",
		"\"__inline\"",
		"\"va_arg\"",
		"\"__builtin_va_arg\"",
		"\"__builtin_offsetof\"",
		"\"__CLONE_beta\"",
		"an identifier",
		"HexDoubleValue",
		"HexFloatValue",
		"HexLongDoubleValue",
		"HexUnsignedIntValue",
		"HexUnsignedLongIntValue",
		"HexUnsignedLongLongIntValue",
		"HexIntValue",
		"HexLongIntValue",
		"HexLongLongIntValue",
		"DoubleValue",
		"FloatValue",
		"LongDoubleValue",
		"UnsignedIntValue",
		"UnsignedLongIntValue",
		"UnsignedLongLongIntValue",
		"IntValue",
		"LongIntValue",
		"LongLongIntValue",
		"CharacterConstant",
		"WideCharacterConstant",
		"a string literal",
		"a wide string literal",
		"open '('",
		"open '{'",
		"close ')'",
		"Dot",
		"Select",
		"open '['",
		"close ']'",
		"Dec",
		"Inc",
		"Comma",
		"And",
		"Mult",
		"Plus",
		"Sub",
		"Comp",
		"Not",
		"Slash",
		"Mod",
		"LShift",
		"RShift",
		"LAngle",
		"RAngle",
		"LEqual",
		"GEqual",
		"Equal",
		"NEqual",
		"Xor",
		"Or",
		"AndCond",
		"OrCond",
		"QMark",
		"Colon",
		"Assign",
		"MultAssign",
		"SlashAssign",
		"ModAssign",
		"PlusAssign",
		"SubAssign",
		"AndAssign",
		"OrAssign",
		"XorAssign",
		"LSAssign",
		"RSAssign",
		"semi-colon",
		"\"__restrict\"",
		"Attributes",
		"\"__asm__\"",
		"\"asm\"",
		"\"__asm\"",
		"close '}'",
		"Varargs",
		"Pragma",
		"PPLine",
		"StdArg",
		"PPError",
		"PPWarning",
		"WS",
		"OctalDigit",
		"OctalConstant",
		"NonzeroDigit",
		"HexadecimalDigit",
		"IntegerSuffix",
		"UnsignedSuffix",
		"UnsignedLongSuffix",
		"UnsignedLongLongSuffix",
		"LongUnsignedSuffix",
		"LongLongUnsignedSuffix",
		"LongSuffix",
		"LongLongSuffix",
		"ExponentPart",
		"Exponent",
		"DigitSequence",
		"BinaryExponentPart",
		"FloatingSuffix",
		"a constant",
		"CCharSequence",
		"CChar",
		"a character escape",
		"a string character",
		"HexPrefix",
		"Hatch",
		"DHatch",
		"LColon",
		"RColon",
		"LMod",
		"RMod",
		"MColon",
		"MCMColon",
		"LineNumber",
		"Text1",
		"Text2",
		"an attribute",
		"IdentifierNondigit",
		"Nondigit",
		"Digit",
		"UniversalCharacterName",
		"HexQuad"
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { -9921991855308800L, 531644415L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 0L, 5044031582252277760L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 18924732865029008L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = { 18924732865029008L, 504403158282297344L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = { -9007199254757392L, 4683743612996976639L, 1L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
		long[] data = { -9917550790173600L, 72057594569588735L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	private static final long[] mk_tokenSet_6() {
		long[] data = { -9007199254757392L, 4755801206502686719L, 1L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_6 = new BitSet(mk_tokenSet_6());
	private static final long[] mk_tokenSet_7() {
		long[] data = { -9007199254741006L, 5116089177224577023L, 31L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_7 = new BitSet(mk_tokenSet_7());
	private static final long[] mk_tokenSet_8() {
		long[] data = { -9007199254741006L, 9223372036854775807L, 31L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_8 = new BitSet(mk_tokenSet_8());
	private static final long[] mk_tokenSet_9() {
		long[] data = { 0L, 72075186224529408L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_9 = new BitSet(mk_tokenSet_9());
	private static final long[] mk_tokenSet_10() {
		long[] data = { 18080305768473472L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_10 = new BitSet(mk_tokenSet_10());
	private static final long[] mk_tokenSet_11() {
		long[] data = { -17944029765304320L, 16383L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_11 = new BitSet(mk_tokenSet_11());
	private static final long[] mk_tokenSet_12() {
		long[] data = { -9921991855308800L, 5044031582654955519L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_12 = new BitSet(mk_tokenSet_12());
	private static final long[] mk_tokenSet_13() {
		long[] data = { 0L, 5044031582248665088L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_13 = new BitSet(mk_tokenSet_13());
	private static final long[] mk_tokenSet_14() {
		long[] data = { 0L, 32768L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_14 = new BitSet(mk_tokenSet_14());
	private static final long[] mk_tokenSet_15() {
		long[] data = { 18924732865029008L, 288230376168808448L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_15 = new BitSet(mk_tokenSet_15());
	private static final long[] mk_tokenSet_16() {
		long[] data = { -9921991855308800L, 531677183L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_16 = new BitSet(mk_tokenSet_16());
	private static final long[] mk_tokenSet_17() {
		long[] data = { -9856084596317312L, 5044031582654955519L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_17 = new BitSet(mk_tokenSet_17());
	private static final long[] mk_tokenSet_18() {
		long[] data = { 0L, 5044013990473662464L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_18 = new BitSet(mk_tokenSet_18());
	private static final long[] mk_tokenSet_19() {
		long[] data = { 0L, 4683761204656111616L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_19 = new BitSet(mk_tokenSet_19());
	private static final long[] mk_tokenSet_20() {
		long[] data = { 18924732865029008L, 504403158286770176L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_20 = new BitSet(mk_tokenSet_20());
	private static final long[] mk_tokenSet_21() {
		long[] data = { 0L, 144115188080082944L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_21 = new BitSet(mk_tokenSet_21());
	private static final long[] mk_tokenSet_22() {
		long[] data = { 0L, 5044031580621275136L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_22 = new BitSet(mk_tokenSet_22());
	private static final long[] mk_tokenSet_23() {
		long[] data = { 0L, 5044031580520611840L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_23 = new BitSet(mk_tokenSet_23());
	private static final long[] mk_tokenSet_24() {
		long[] data = { 0L, 5044031574078160896L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_24 = new BitSet(mk_tokenSet_24());
	private static final long[] mk_tokenSet_25() {
		long[] data = { 0L, 5044031445229142016L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_25 = new BitSet(mk_tokenSet_25());
	private static final long[] mk_tokenSet_26() {
		long[] data = { 0L, 5044031032912281600L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_26 = new BitSet(mk_tokenSet_26());
	private static final long[] mk_tokenSet_27() {
		long[] data = { 0L, 5044031032903892992L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_27 = new BitSet(mk_tokenSet_27());
	private static final long[] mk_tokenSet_28() {
		long[] data = { 0L, 5044030483148079104L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_28 = new BitSet(mk_tokenSet_28());
	private static final long[] mk_tokenSet_29() {
		long[] data = { 0L, 5044029383636451328L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_29 = new BitSet(mk_tokenSet_29());
	private static final long[] mk_tokenSet_30() {
		long[] data = { 0L, 5044027184613195776L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_30 = new BitSet(mk_tokenSet_30());
	private static final long[] mk_tokenSet_31() {
		long[] data = { 0L, 5044022786566684672L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_31 = new BitSet(mk_tokenSet_31());
	private static final long[] mk_tokenSet_32() {
		long[] data = { 0L, 4971991580807790592L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_32 = new BitSet(mk_tokenSet_32());
	private static final long[] mk_tokenSet_33() {
		long[] data = { 0L, 72057594037927936L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_33 = new BitSet(mk_tokenSet_33());
	private static final long[] mk_tokenSet_34() {
		long[] data = { 18924732865029010L, 504403158286770176L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_34 = new BitSet(mk_tokenSet_34());
	private static final long[] mk_tokenSet_35() {
		long[] data = { -9011657499761774L, 504420750987673599L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_35 = new BitSet(mk_tokenSet_35());
	private static final long[] mk_tokenSet_36() {
		long[] data = { 18924732865029008L, 504420750472814592L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_36 = new BitSet(mk_tokenSet_36());
	private static final long[] mk_tokenSet_37() {
		long[] data = { 18924732865029010L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_37 = new BitSet(mk_tokenSet_37());
	private static final long[] mk_tokenSet_38() {
		long[] data = { 0L, 72057594042122240L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_38 = new BitSet(mk_tokenSet_38());
	private static final long[] mk_tokenSet_39() {
		long[] data = { 0L, 72092778414211072L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_39 = new BitSet(mk_tokenSet_39());
	private static final long[] mk_tokenSet_40() {
		long[] data = { 0L, 4683743612469510144L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_40 = new BitSet(mk_tokenSet_40());
	private static final long[] mk_tokenSet_41() {
		long[] data = { 18924732865029008L, 360340746752016384L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_41 = new BitSet(mk_tokenSet_41());
	private static final long[] mk_tokenSet_42() {
		long[] data = { 18014398509481984L, 432345564232065024L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_42 = new BitSet(mk_tokenSet_42());
	private static final long[] mk_tokenSet_43() {
		long[] data = { 18924732865029008L, 4395566012876251136L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_43 = new BitSet(mk_tokenSet_43());
	private static final long[] mk_tokenSet_44() {
		long[] data = { -9007199254757390L, -72004816943120385L, 31L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_44 = new BitSet(mk_tokenSet_44());
	private static final long[] mk_tokenSet_45() {
		long[] data = { 18924732865029008L, 432345564248858624L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_45 = new BitSet(mk_tokenSet_45());
	private static final long[] mk_tokenSet_46() {
		long[] data = { -9856084596317312L, 72040001847656447L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_46 = new BitSet(mk_tokenSet_46());
	private static final long[] mk_tokenSet_47() {
		long[] data = { -9856084596317312L, 17592181817343L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_47 = new BitSet(mk_tokenSet_47());
	private static final long[] mk_tokenSet_48() {
		long[] data = { 18150674512651136L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_48 = new BitSet(mk_tokenSet_48());
	private static final long[] mk_tokenSet_49() {
		long[] data = { 0L, 4611686018427387904L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_49 = new BitSet(mk_tokenSet_49());
	private static final long[] mk_tokenSet_50() {
		long[] data = { 18150674512651136L, 4611686018427387904L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_50 = new BitSet(mk_tokenSet_50());
	private static final long[] mk_tokenSet_51() {
		long[] data = { 18080305768473472L, 504420750468620288L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_51 = new BitSet(mk_tokenSet_51());
	private static final long[] mk_tokenSet_52() {
		long[] data = { 0L, 4611686018431582208L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_52 = new BitSet(mk_tokenSet_52());
	private static final long[] mk_tokenSet_53() {
		long[] data = { 3298568438528L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_53 = new BitSet(mk_tokenSet_53());
	private static final long[] mk_tokenSet_54() {
		long[] data = { -9921989707825152L, 432345564763701247L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_54 = new BitSet(mk_tokenSet_54());
	private static final long[] mk_tokenSet_55() {
		long[] data = { 0L, 4227072L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_55 = new BitSet(mk_tokenSet_55());
	private static final long[] mk_tokenSet_56() {
		long[] data = { 18014398509481984L, 432345564244353024L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_56 = new BitSet(mk_tokenSet_56());
	private static final long[] mk_tokenSet_57() {
		long[] data = { 18017697077920512L, 4467570830372806656L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_57 = new BitSet(mk_tokenSet_57());
	private static final long[] mk_tokenSet_58() {
		long[] data = { -9011657499761776L, 144115188612513791L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_58 = new BitSet(mk_tokenSet_58());
	private static final long[] mk_tokenSet_59() {
		long[] data = { 0L, 72057594042155008L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_59 = new BitSet(mk_tokenSet_59());
	private static final long[] mk_tokenSet_60() {
		long[] data = { 0L, 144115188080353280L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_60 = new BitSet(mk_tokenSet_60());
	private static final long[] mk_tokenSet_61() {
		long[] data = { -9921991855308800L, 531988479L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_61 = new BitSet(mk_tokenSet_61());
	private static final long[] mk_tokenSet_62() {
		long[] data = { -9856084596317312L, 4683726020278714367L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_62 = new BitSet(mk_tokenSet_62());
	private static final long[] mk_tokenSet_63() {
		long[] data = { -9921991855308800L, 531660799L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_63 = new BitSet(mk_tokenSet_63());
	private static final long[] mk_tokenSet_64() {
		long[] data = { 0L, 35184372416512L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_64 = new BitSet(mk_tokenSet_64());
	private static final long[] mk_tokenSet_65() {
		long[] data = { -9921991855308800L, 72057594569572351L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_65 = new BitSet(mk_tokenSet_65());
	private static final long[] mk_tokenSet_66() {
		long[] data = { -9007199254741008L, 4755783614316642303L, 1L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_66 = new BitSet(mk_tokenSet_66());
	private static final long[] mk_tokenSet_67() {
		long[] data = { -9007199254741008L, 4683743612996976639L, 1L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_67 = new BitSet(mk_tokenSet_67());
	private static final long[] mk_tokenSet_68() {
		long[] data = { -9007199254741006L, 5188146770730287103L, 31L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_68 = new BitSet(mk_tokenSet_68());
	private static final long[] mk_tokenSet_69() {
		long[] data = { 18924732865029008L, 432345564244369408L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_69 = new BitSet(mk_tokenSet_69());
	private static final long[] mk_tokenSet_70() {
		long[] data = { -9856084596317312L, 144097595889254399L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_70 = new BitSet(mk_tokenSet_70());
	private static final long[] mk_tokenSet_71() {
		long[] data = { 18014398509481984L, 504403158282280960L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_71 = new BitSet(mk_tokenSet_71());
	private static final long[] mk_tokenSet_72() {
		long[] data = { 18995118789075858L, 4539663608782807040L, 31L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_72 = new BitSet(mk_tokenSet_72());
	private static final long[] mk_tokenSet_73() {
		long[] data = { 18924732865029008L, 16384L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_73 = new BitSet(mk_tokenSet_73());
	private static final long[] mk_tokenSet_74() {
		long[] data = { 18995118789075858L, 504403158282280960L, 31L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_74 = new BitSet(mk_tokenSet_74());
	
	}
