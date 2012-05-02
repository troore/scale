package scale.frontend.fortran;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Enumeration;

import scale.common.*;
import scale.frontend.*;
import scale.clef.*;
import scale.clef.expr.*;
import scale.clef.decl.*;
import scale.clef.type.*;
import scale.clef.stmt.*;
import scale.clef.symtab.*;
import scale.callGraph.*;
import scale.annot.*;

/** 
 * This is the parser for the F95 version of CFortran, including f77,
 * f90, and f95.
 * <p>
 * $Id: F95.java,v 1.80 2007-10-17 13:39:58 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * While the name of the class is F95, it currently only implements
 * the F77 standard.
 * <h3>Input Form</h3>
 * If the file extension of the source file pathname is
 * <code>.f90</code> or <code>.f95</code>, free-form input is assumed.
 * Otherwise, fixed-form input is assumed.  (Free-form input has not
 * been tested.)  In fixed-form input, all blanks are eliminated and
 * all alphabetical characters are converted to lower case, before
 * syntactical scanning is performed.
 * <p>
 * <h3>Scanning</h3>
 * In general, all scanning is accomplished by methods whose names
 * begin with "next".  The remainder of the method name specifies for
 * what the method is scanning.  Such a scanning method either
 * recognizes that thing or not.  If it does not recognize the thing,
 * it returns an indication (generally a <code>null</code>) that the
 * scan was not successful and resets the scan to the point at which
 * it started.  If the method has scanned far enough to know that it
 * is scanning the appropriate construct and encounters something that
 * does not conform, it throws an <code>InvalidException</code>.  For
 * example, consider the following statement:
 * <pre>
 *   COMMON/abcx,y,z
 * </pre>
 * The <code>nextCommonStmt</code> method knows that this is a COMMON
 * statement once the '/' has been encountered.  Thus, it throws an
 * exception when the ',' is encountered. For
 * <pre>
 *   DO10J=1,N
 * </pre>
 * the <code>nextAssignmentStmt</code> method returns
 * <code>null</code> when the comma is encountered.
 */

public class F95 extends Parser
{
  public static boolean classTrace = false;
  public static boolean fullError  = false;

  /**
   * Allow F77 features.
   */
  public static final int F77   = 1;
  /**
   * Allow F90 features.
   */
  public static final int F90   = 2;
  /**
   * Allow F96 features.
   */
  public static final int F95   = 4;
  /**
   * The name of blank common.
   */
  public static final String blankCommonName = "_BLNK__";

  private boolean allowF77Features; // True if Fortran 77 features allowed.
  private boolean allowF90Features; // True if Fortran 90 features allowed.
  private boolean allowF95Features; // True if Fortran 95 features allowed.
  private boolean eofCard;          // True if the end-of-file encountered when reading a card.
  private boolean eofStmt;          // True if the end-of-file encountered when reading a statement.
  private boolean fatalError;       // True if the parser finds an error.
  private boolean freeForm;         // True if the source files are in free form.
  private boolean cardValid;        // True if the card buffer holds a card.
  private boolean mainFound;        // True if the "MAIN" program found.
  private boolean topLevel;         // True if processing a top level declaration.
  private boolean inBlockData;      // True if processing a BLOCK DATA section.
  private boolean globalSaveFlag;   // True if the SAVE attribute should be applied to all decls in a routine.
  private boolean curFtnIsSub;      // True if the currentFunction is a SUBROUTINE.
  private boolean cmplxTypesDefed;  // True if C declarations for structs complex & doublecomplex have been generated.

  private HashMap<VariableDecl, Vector<EquivalenceDecl>> commonVars;       // Map from a base variable to a vector of its constituents.
  private HashMap<Declaration, FormalDecl> fctMap; // Map from a FormalDecl of type FortranCharType to the corresponding length FormalDecl.
  private HashMap<String, ProcedureDecl>   procMap;    // Map from pre-defined procedure name to procedure declaration.
  private HashMap<LabelDecl, VariableDecl> formatMap;  // Map from statement label to format variable.
  private HashMap<String, Object>          nameMap;    // Map from a name to the declaration to be used for it.
  private HashMap<ArrayType, Type>         allocTypes; // Used for ALLOCATABLE arrays.

  private Vector<Object>        entries;          // ProcedureDecl for each ENTRY.
  private Vector<FormalDecl>    actualFormals;    // The actual formals in use for this routine.
  private Vector<FormalDecl>    entryFormals;     // The set of formals used by each ENTRY statement.
  private Vector<Expression>    specList;         // The set of non-constant array size specifications.
  private Vector<String>        userDirs;         // Search directories for user include files.
  private Vector<VariableDecl>  arrayExprSubscripts; // The subscripts to use for arrays in array expressions.

  private Table<RoutineDecl, CallOp> fixupCalls;       // A table of routines and the calls to those routines.
  private Stack<FFile>               includeStack;     // The nested INCLUDE stack of FFile instances.
  private IntMap<LabelDecl>          labels;           // The set of labels encountered.

  private UniqueName  un;               // The name generator to use for any variables created.
  private FileReader  curReader;        // The current file reader.
  private String      filename;         // The current source program path name.
  private CallGraph   cg;               // The current CallGraph instance.
  private RoutineDecl currentFunction;  // The current routine under construction.
  private VariableDecl resultVar;       // Variable to hold the return value for a function.
  private BlockStmt   currentBlockStmt; // The current BlockStmt under construction.
  private EquivSet    currentEquivSet;  // See nextEquivalenceStmt.
  private char[]      buffer;           // Raw characters from the source file.
  private char[]      card;             // One "card" from the source file.
  private char[]      line;             // The current line.
  private char[]      statement;        // The current statement.
  private Literal     vzero;            // A ((void *) 0).
  private Literal     dzero;            // A ((double) 0.0).
  private Literal     zero;             // A ((int) 0).
  private Literal     one;              // A ((int) 1).
  private Literal     lzero;            // A ((char) 0).
  private Literal     lone;             // A ((char) 1).
  private Literal     five;             // A ((int) 5).
  private Literal     six;              // A ((int) 6).
  private LabelDecl   statementLabel;   // The user specified statement label.
  private LabelDecl   statementLabel2;  // The user specified statement label on an ENDIF.
  private Expression  errExp;           // Returned if error in generating an Expression instance.
  private Statement   errStmt;          // Returned if error in generating a Statement instance.
  private BlockStmt   errBlkStmt;       // Returned if error in generating a BlockStmt instance.
  private int         cardNumber;       // The current source program card number.
  private int         lineNumber;       // The current source program line number.
  private int         column;           // Current scan position in the statement.
  private int         bufPtr;           // The next unused character in the buffer.
  private int         bufLength;        // The number of characters in the buffer.
  private int         stmtPtr;          // The index to the next statement in the line.
  private int         stmtNestedLevel;  // The current compund statement nesting depth.
  private int         varCounter;       // For generating compiler-generated variable names.
  private int         traceIndent;      // For tracing.
  private int         errorCnt;         // Count of errors encountered.
  private int         allocTypesCnt;    // Used for ALLOCATABLE arrays.
  private Type        requiredType;     // Used for constant expressions.
  private Type        dimType;          // Used for ALLOCATABLE arrays.
  private Type[]      implicitTypes;    // Indexed by the first letter of an identifier.
  private int[]       validInVersion;   // Shows if the keyword is allowed in this version of Fortran.
  private ArrayType   arrayExprType;    // All arrays in an array expression must conform to this.

  /**
   * The types for the various pre-defined types.
   */
  private IntegerType size_t_type;
  private IntegerType ptrdiff_t_type;
  private IntegerType char_type;
  private IntegerType int_type;
  private IntegerType short_type;
  private int         int_type_size;
  private IntegerType long_type;
  private int         long_type_size;
  private FloatType   real_type;
  private FloatType   double_type;
  private FloatType   long_double_type;
  private ComplexType double_complex_type;
  private FloatType   double_imaginary_type;
  private ComplexType float_complex_type;
  private FloatType   float_imaginary_type;
  private IntegerType logical_type;
  private VoidType    void_type;
  private PointerType voidp_type;
  private PointerType charp_type;
  private PointerType ccharp_type;
  private PointerType intp_type;

  private Type[]      intTypeArray;
  private Type[]      floatTypeArray;
  private Type[]      complexTypeArray;
  private Type[]      logicalTypeArray;

  // Scalar dyadic operations.

  private static final int ADD_OP    = 0;
  private static final int SUB_OP    = 1;
  private static final int MUL_OP    = 2;
  private static final int DIV_OP    = 3;
  private static final int REM_OP    = 4;
  private static final int POW_OP    = 5;
  private static final int DIM_OP    = 6;
  private static final int SIGN_OP   = 7;
  private static final int ATAN2_OP  = 8;
  private static final int MAX_OP    = 9;
  private static final int MIN_OP    = 10;
  private static final int AND_OP    = 11;
  private static final int OR_OP     = 12;
  private static final int XOR_OP    = 13;
  private static final int SFTLFT_OP = 14;

  // True if arguments must be compatible types.

  private static final boolean[] dyadicOpChk = {
    true,  true,  true,  true,  true,
    false, false, false, true,  true,
    true,  true,  true,  true,  false,
  };

  // Scalar monadic operations.

  private static final int NOP_OP      = 0;
  private static final int ABS_OP      = 1;
  private static final int ACOS_OP     = 2;
  private static final int ASIN_OP     = 3;
  private static final int ATAN_OP     = 4;
  private static final int CEILING_OP  = 5;
  private static final int COMP_OP     = 6;
  private static final int CONJG_OP    = 7;
  private static final int COS_OP      = 8;
  private static final int COSH_OP     = 9;
  private static final int EXP_OP      = 10;
  private static final int FLOOR_OP    = 11;
  private static final int IMAG_OP     = 12;
  private static final int LOG10_OP    = 13;
  private static final int LOG_OP      = 14;
  private static final int REAL_OP     = 15;
  private static final int ROUND_OP    = 16;
  private static final int SIN_OP      = 17;
  private static final int SINH_OP     = 18;
  private static final int SQRT_OP     = 19;
  private static final int TAN_OP      = 20;
  private static final int TANH_OP     = 21;
  private static final int TRUNCATE_OP = 22;

  // True if the argument should be converted to the type of the
  // monadic operation result.

  private static final boolean[] convertArgOp = {
    true,  false, true,  true, true, //  0 -  4
    false, true,  true,  true, true, //  5 -  9
    false, true,  false, true, true, // 10 - 14
    false, false, true,  true, true, // 15 - 19
    true,  true,  false,             // 20 - 22
  };

  // The following specify what types of expressions are allowed.

  // NORMAL_EXPR - normal expressions do not have to have a
  // compile-time determined value. Any array reference is treated as
  // a reference to an element of that array.  The particular element
  // is specified by the subscripts specified in arrayExprSubscripts.
  // If arrayExprSubscripts is null, then the reference to the array
  // is treated as just the address to the array.
  private static final int NORMAL_EXPR       = 0;

  // SCALAR_EXPR - no references to an array slice are allowed.
  private static final int SCALAR_EXPR       = 1;

  // SCALAR_CONST_EXPR - no references to an array slice are
  // allowed and the expression must have a value that can be
  // determined at compile time.
  private static final int SCALAR_CONST_EXPR = 2;

  /**
   * Allowed Fortran IO list keyword flags.
   */
  private static final int FIO_UNIT        = 0x000001;
  private static final int FIO_IOSTAT      = 0x000002;
  private static final int FIO_ERR         = 0x000004;
  private static final int FIO_FMT         = 0x000008;
  private static final int FIO_REC         = 0x000010;
  private static final int FIO_END         = 0x000020;
  private static final int FIO_FILE        = 0x000040;
  private static final int FIO_STATUS      = 0x000080;
  private static final int FIO_ACCESS      = 0x000100;
  private static final int FIO_FORM        = 0x000200;
  private static final int FIO_RECL        = 0x000400;
  private static final int FIO_BLANK       = 0x000800;
  private static final int FIO_EXIST       = 0x001000;
  private static final int FIO_OPENED      = 0x002000;
  private static final int FIO_NUMBER      = 0x004000;
  private static final int FIO_NAMED       = 0x008000;
  private static final int FIO_NAME        = 0x010000;
  private static final int FIO_SEQUENTIAL  = 0x020000;
  private static final int FIO_DIRECT      = 0x040000;
  private static final int FIO_FORMATTED   = 0x080000;
  private static final int FIO_UNFORMATTED = 0x100000;
  private static final int FIO_NEXTREC     = 0x200000;
  private static final int FIO_RLIST      = 
    FIO_UNIT + FIO_FMT + FIO_REC + FIO_IOSTAT + FIO_ERR + FIO_END;
  private static final int FIO_WLIST      = 
    FIO_UNIT + FIO_FMT + FIO_REC + FIO_IOSTAT + FIO_ERR;
  private static final int FIO_OLIST       =
    FIO_UNIT + FIO_IOSTAT + FIO_ERR + FIO_FILE + FIO_STATUS +
    FIO_ACCESS + FIO_FORM + FIO_RECL + FIO_BLANK;
  private static final int FIO_CLLIST      =
    FIO_UNIT + FIO_IOSTAT + FIO_ERR + FIO_STATUS;
  private static final int FIO_INLIST     =
    FIO_UNIT + FIO_FILE + FIO_IOSTAT + FIO_ERR + FIO_EXIST +
    FIO_OPENED + FIO_NUMBER + FIO_NAMED +  FIO_NAME + FIO_ACCESS +
    FIO_SEQUENTIAL + FIO_DIRECT + FIO_FORM + FIO_FORMATTED +
    FIO_UNFORMATTED + FIO_RECL + FIO_NEXTREC + FIO_BLANK;
  private static final int FIO_ALIST =
    FIO_UNIT + FIO_IOSTAT + FIO_ERR;

  /**
   * The following are the types recognized by the f2c runtime
   * routines.  The values must match those in libI77/lio.h.
   */
  private static final int TYUNKNOWN  = 0;
  private static final int TYADDR     = 1;
  private static final int TYSHORT    = 2;
  private static final int TYLONG     = 3;
  private static final int TYREAL     = 4;
  private static final int TYDREAL    = 5;
  private static final int TYCOMPLEX  = 6;
  private static final int TYDCOMPLEX = 7;
  private static final int TYLOGICAL  = 8;
  private static final int TYCHAR     = 9;
  private static final int TYSUBR     = 10;
  private static final int TYINT1     = 11;
  private static final int TYLOGICAL1 = 12;
  private static final int TYLOGICAL2 = 13;
  private static final int TYQUAD     = 14;

  /**
   * A place to put the various values for Fortran IO functions.
   */
  private boolean      fioChkErr;      // True if the compiler should generate an error check.
  private Expression   fioUnit;        // The expression specifying the unit.
  private Expression   fioIostat;      // Variable to hold the Fortran IO function status.
  private LabelDecl    fioErr;         // The statement to branch to if an error occurs.
  private Expression   fioFmt;         // The variable holding the format or holing the FORMAT label.
  private Expression   fioRec;         // An integer expression.
  private LabelDecl    fioEnd;         // The statement to branch to at end of file.
  private Expression   fioFile;        // A character expression specifying the file.
  private Expression   fioStatus;      // A character expression specifying OLD, NEW, or SCRATCH.
  private Expression   fioAccess;      // A character expression specifying SEQUENTIAL or DIRECT.
  private Expression   fioForm;        // A character expression specifying FORMATTED or UNFORMATTED.
  private Expression   fioRecl;        // An integer expression specifying the record length.
  private Expression   fioBlank;       // A character expression specifying NULL or ZERO.
  private Expression   fioExist;       // A logical l-value expression.
  private Expression   fioOpened;      // A logical l-value expression.
  private Expression   fioNumber;      // An integer l-value expression.
  private Expression   fioNamed;       // A logical l-value expression.
  private Expression   fioName;        // A character l-value expression.
  private Expression   fioSequential;  // A character l-value expression.
  private Expression   fioDirect;      // A character l-value expression.
  private Expression   fioFormatted;   // A character l-value expression.
  private Expression   fioUnformatted; // A character l-value expression.
  private Expression   fioNextrec;     // An integer l-value expression.
  private VariableDecl fioTypeVar;     // Variable to hold type information for do_ call.
  private VariableDecl fioNumberVar;   // Variable to hold type information for do_ call.

  /**
   * Structure definitions for use with f2c Fortran IO library.
   */
  private RecordType cilist;  // external READ, WRITE
  private RecordType icilist; // internal READ, WRITE
  private RecordType olist;   // OPEN
  private RecordType cllist;  // CLOSE
  private RecordType alist;   // REWIND, BACKSPACE, ENDFILE
  private RecordType inlist;  // INQUIRE

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

  private static final String[] dotOps = {
    "??", "not", "and", "or", "eqv", "neqv",
    "gt", "ge",  "lt",  "le", "ne", "eq",
  };

  private static final int DOT_NA   = 0;
  private static final int DOT_NOT  = 1;
  private static final int DOT_AND  = 2;
  private static final int DOT_OR   = 3;
  private static final int DOT_EQV  = 4;
  private static final int DOT_NEQV = 5;
  private static final int DOT_GT   = 6;
  private static final int DOT_GE   = 7;
  private static final int DOT_LT   = 8;
  private static final int DOT_LE   = 9;
  private static final int DOT_NEQ  = 10;
  private static final int DOT_EQ   = 11;
  private static final String[] dotOpStrs  = {
    "??", ".not.", ".and.", ".or.", ".eqv.", ".neqv.",
    ".gt.", ".ge.", ".lt.", ".le.", ".ne.", ".eq.",
  };

  /**
   * The set of functions which are referenced in generated C code if
   * the source program uses the Fortran COMPLEX type.
   */
  private static final String[] complexFtns = {
    "_scale_createdoublecomplex", "xdd",
    "_scale_createcomplex",       "zff",
    "_scale_subcc",               "zzz",
    "_scale_addcc",               "zzz",
    "_scale_multcc",              "zzz",
    "_scale_divcc",               "zzz",
    "_scale_sqrtc",               "zz",
    "_scale_logc",                "zz",
    "_scale_expc",                "zz",
    "_scale_sinc",                "zz",
    "_scale_cosc",                "zz",
    "_scale_absc",                "dz",
    "_scale_conjgc",              "zz",
    "_scale_negatec",             "zz",
    "_scale_powci",               "zzi",
    "_scale_powcc",               "zzz",
    "_scale_subzz",               "xxx",
    "_scale_addzz",               "xxx",
    "_scale_multzz",              "xxx",
    "_scale_divzz",               "xxx",
    "_scale_sqrtz",               "xx",
    "_scale_logz",                "xx",
    "_scale_expz",                "xx",
    "_scale_sinz",                "xx",
    "_scale_cosz",                "xx",
    "_scale_CDABS",               "dX",
    "_scale_absz",                "dx",
    "_scale_conjgz",              "xx",
    "_scale_negatez",             "xx",
    "_scale_pow_zi",              "vXXi",
    "_scale_pow_zz",              "vXXX",
    "_scale_powzi",               "xxi",
    "_scale_powzz",               "xxx",
    "_scale_z_div",               "vXXX",
  };

  /**
   * @param top specifies the top level class of the compiler
   * @param extension specifies the file extension of the file to be parsed
   */
  public F95(scale.test.Scale top, String extension)
  {
    super(top, extension);

    this.userDirs   = top.cpIncl.getStringValues();

    this.allowF77Features = true;
    this.allowF90Features = "f90".equals(extension);
    this.allowF95Features = false;

    this.buffer    = null;
    this.card      = new char[133];
    this.line      = new char[1024];

    this.un = new UniqueName("_np");

    if (allowF95Features || allowF90Features)
      this.statement = new char[1024];
    else
      this.statement = line;

    this.size_t_type           = Machine.currentMachine.getSizetType();
    this.ptrdiff_t_type        = Machine.currentMachine.getPtrdifftType();

    this.char_type             = SignedIntegerType.create(8);
    this.short_type            = SignedIntegerType.create(16);
    this.int_type              = SignedIntegerType.create(32);
    this.long_type             = SignedIntegerType.create(64);

    this.intTypeArray          = new Type[5];
    this.intTypeArray[0]       = int_type;
    this.intTypeArray[1]       = char_type;
    this.intTypeArray[2]       = short_type;
    this.intTypeArray[3]       = int_type;
    this.intTypeArray[4]       = long_type;

    this.int_type_size         = 32;
    this.long_type_size        = 64;

    this.real_type             = Machine.currentMachine.getFloatType();
    this.double_type           = Machine.currentMachine.getDoubleType();
    this.long_double_type      = Machine.currentMachine.getLongDoubleType();

    this.floatTypeArray        = new Type[4];
    this.floatTypeArray[0]     = real_type;
    this.floatTypeArray[1]     = real_type;
    this.floatTypeArray[2]     = double_type;
    this.floatTypeArray[3]     = long_double_type;

    this.double_complex_type   = ComplexType.create(double_type.bitSize(), double_type.bitSize());
    this.double_imaginary_type = double_type;
    this.float_complex_type    = ComplexType.create(real_type.bitSize(), real_type.bitSize());
    this.float_imaginary_type  = real_type;

    this.complexTypeArray      = new Type[4];
    this.complexTypeArray[0]   = float_complex_type;
    this.complexTypeArray[1]   = float_complex_type;
    this.complexTypeArray[2]   = double_complex_type;

    // The Fortran LOGICAL type must be the same size as the INTEGER
    // type because of the f2c library.

    this.logical_type          = UnsignedIntegerType.create(32);
    this.logicalTypeArray      = new Type[5];
    this.logicalTypeArray[0]   = logical_type;
    this.logicalTypeArray[1]   = UnsignedIntegerType.create(8);
    this.logicalTypeArray[2]   = UnsignedIntegerType.create(16);
    this.logicalTypeArray[3]   = logical_type;
    this.logicalTypeArray[4]   = UnsignedIntegerType.create(64);

    this.void_type             = VoidType.type;
    this.voidp_type            = PointerType.create(void_type);
    this.charp_type            = PointerType.create(char_type);
    this.ccharp_type           = PointerType.create(RefType.create(char_type, RefAttr.Const));
    this.intp_type             = PointerType.create(int_type);

    this.vzero = LiteralMap.put(0, voidp_type);
    this.dzero = LiteralMap.put(0.0, double_type);
    this.zero  = LiteralMap.put(0, int_type);
    this.one   = LiteralMap.put(1, int_type);
    this.lzero = LiteralMap.put(0, logical_type);
    this.lone  = LiteralMap.put(1, logical_type);
    this.five  = LiteralMap.put(5, int_type);
    this.six   = LiteralMap.put(6, int_type);

    this.errExp     = new NilOp();
    this.errStmt    = new NullStmt();
    this.errBlkStmt = new BlockStmt();

    this.includeStack  = new Stack<FFile>();
    this.labels        = new IntMap<LabelDecl>(11);
    this.procMap       = new HashMap<String, ProcedureDecl>(61);
    this.formatMap     = new HashMap<LabelDecl, VariableDecl>(61);
    this.commonVars    = new HashMap<VariableDecl, Vector<EquivalenceDecl>>(13);
    this.nameMap       = new HashMap<String, Object>(13);
    this.entries       = new Vector<Object>();
    this.specList      = new Vector<Expression>();
    this.fctMap        = new HashMap<Declaration, FormalDecl>(13);
    this.fixupCalls    = new Table<RoutineDecl, CallOp>();

    this.arrayExprType       = null;
    this.arrayExprSubscripts = new Vector<VariableDecl>();

    int l = Keywords.kw_stmt.length;
    this.validInVersion = new int[l];
    for (int i = 0; i < l; i++) {
      validInVersion[i] = 0;
      if (allowF77Features)
        validInVersion[i] |= Keywords.kw_f77[i];
      if (allowF90Features)
        validInVersion[i] |= Keywords.kw_f90[i];
      if (allowF95Features)
        validInVersion[i] |= Keywords.kw_f95[i];
    }

    initImplicitTypes();
  }

  /**
   * Return the correct source langauge instance for this parser.
   */
  public SourceLanguage getSourceLanguage()
  {
    return new SourceFortran();
  }

  /**
   * Output a message in a method if
   * <code>classTrace</code> is <code>true</code>.
   * @return true
   */
  private boolean trace(String msg, Object o)
  {
    if (!classTrace)
      return true;

    for (int i = 0; i < traceIndent; i += 2)
      System.out.print("  ");

    if (msg != null) {
      System.out.print(msg);
      System.out.print(" ");
    }
    if (o != null)
      System.out.print(o);
    System.out.println("");

    return true;
  }

  /**
   * Output a message at entrance to a method if
   * <code>classTrace</code> is <code>true</code>.
   * @return true
   */
  private boolean traceIn(String where, Object o)
  {
    if (!classTrace)
      return true;

    for (int i = 0; i < traceIndent; i += 2)
      System.out.print("  ");
    traceIndent += 2;

    System.out.print("in  ");
    System.out.print(where);
    System.out.print(" <");
    System.out.print(column);
    System.out.print(":");
    System.out.print(statement[column]);
    System.out.print("> ");
    if (o != null)
      System.out.print(o);
    System.out.println("");

    return true;
  }

  /**
   * Output a message at exit from a method if
   * <code>classTrace</code> is <code>true</code>.
   * @return true
   */
  private boolean traceOut(String where, Object o)
  {
    if (!classTrace)
      return true;

    traceIndent -= 2;
    for (int i = 0; i < traceIndent; i += 2)
      System.out.print("  ");

    System.out.print("out ");
    System.out.print(where);
    System.out.print(" <");
    System.out.print(column);
    System.out.print(":");
    System.out.print(statement[column]);
    System.out.print("> ");
    if (o != null)
      System.out.print(o);
    System.out.println("");

    return true;
  }

  /**
   * Create a new temporary variable for use by the optimized code.
   */
  protected VariableDecl genTemp(Type t)
  {
    VariableDecl vd = new VariableDecl(un.genName(), t);
    vd.setTemporary();
    return vd;
  }

  /**
   * Parse the specified Fortran file.  If <code>macroText</code> is not
   * <code>null</code>, the set of defined macros is added to it.
   * @param name the name of the Clef AST (i.e., the file name)
   * @param suite is the collection of call graphs
   * @param macroText is <code>null</code> or has macro definitions as
   * text added
   * @return new CallGraph
   */
  public CallGraph parse(String name, Suite suite, Vector<String> macroText)
  {
    int extp = name.lastIndexOf('.');
    if (extp > 0) {
      String ext = name.substring(extp + 1);
      freeForm = ("f90".equals(ext) || "f95".equals(ext)) && (allowF95Features || allowF90Features);
    }

    cg = new CallGraph(name, suite, new SourceFortran());
    cg.getSymbolTable().beginScope();

    FileDecl            root   = new FileDecl(cg.getName());
    Vector<Declaration> rdecls = new Vector<Declaration>(23);

    root.setDecls(rdecls);
    cg.setAST(root);

    includeStack.clear();

    try {
      filename = name;
      curReader = newFile(name);

      readStatement();

      while (!eofStmt) {
        nextProgramUnit();
        if (fatalError) {
          while (curReader != null) {
            curReader.close();
            curReader = oldFile();
          }
          System.gc(); // To actually close files!
          return null;
        }
      }

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

        RoutineDecl rd = decl.returnRoutineDecl();
        if (rd != null)
          fixCalls(rd);

        rdecls.add(decl);
      }

      if (classTrace) {
        int ll = rdecls.size();
        for (int i = 0; i < ll; i++) {
          Declaration decl = rdecls.get(i);
          Type        ty   = decl.getCoreType();
          if (ty.isProcedureType())
            continue;

          System.out.println("  " + decl);
          long size = ty.memorySize(Machine.currentMachine);
          System.out.println("    " + size + " " + ty);
          AggregateType at = ty.getCoreType().returnAggregateType();
          if (at != null) {
            int lll = at.numFields();
            for (int ii = 0; ii < lll; ii++) {
              FieldDecl fd  = at.getField(ii);
              Type      ty2 = fd.getCoreType();
              System.out.println("       " + fd);
              if (ty2.isProcedureType())
                continue;
              long size2 = ty2.memorySize(Machine.currentMachine);
              System.out.println("        " + size2 + " " + ty2);
            }
          }
        }
      }

      if (curReader != null)
        curReader.close();
      System.gc(); // To actually close files!
      if (fatalError)
        return null;
      return cg;

    } catch(java.io.FileNotFoundException e) {
      Msg.reportError(Msg.MSG_Source_file_not_found_s, null, 0, 0, name);
      return null;
    } catch(java.lang.Exception e) {
      String msg = e.getMessage();
      if (true || (msg == null) || (msg.length() < 1))
        e.printStackTrace();
      return null;
    } catch(java.lang.Error e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * This routine fixes up calls to forward referenced routines so
   * that there are fewer warning messages produced from any generated
   * C code.
   */
  private void fixCalls(RoutineDecl rd)
  {
    if (rd.getBody() == null)
      return;

    Object[] v = fixupCalls.getRowArray(rd);
    if (v.length == 0)
      return;

    ProcedureType pt         = rd.getSignature();
    int           numFormals = pt.numFormals();

    for (int i = 0; i < v.length; i++) {
      CallOp call    = (CallOp) v[i];
      int    numArgs = call.getNumArgs();

      for (int j = 0; (j < numFormals) && (j < numArgs); j++) {
        FormalDecl fd = pt.getFormal(j);
        Type       ft = fd.getType();
        if (!ft.isPointerType())
          continue;

        Expression arg = call.getArg(j);
        Type       at  = arg.getType();

        if (at.equivalent(ft))
          continue;

        if (arg instanceof TypeConversionOp)
          arg = ((TypeConversionOp) arg).getExpr();

        call.setArg(new TypeConversionOp(ft, arg, CastMode.CAST), j);
      }
    }
  }

  /**
   *  Report the error message for the file at the line and column specified.
   */
  private void reportError(int errno, String text, String filename, int lineno, int column)
  {
    errorCnt++;
    if (errorCnt > 20)
      return;

    if (!freeForm) { // Account for continued lines.
      while (column >= 72) {
        lineno++;
        column -= 66;
      }
    }

    Msg.reportError(errno, filename, lineno, column, text);
    if (classTrace || fullError)
      Debug.printStackTrace();
  }

  /**
   *  Report the error message for the file at the line and column specified.
   */
  private void reportError(int errno, String text)
  {
    reportError(errno, text, filename, lineNumber, column);
  }

  /**
   *  Report the error message for the file at the line and column specified.
   */
  private void reportError(int    errno,
                           String text1,
                           String text2,
                           String filename,
                           int    lineno,
                           int    column)
  {
    errorCnt++;
    if (errorCnt > 20)
      return;

    Msg.reportError(errno, filename, lineno, column, text1, text2);
    if (classTrace || fullError)
      Debug.printStackTrace();
  }

  /**
   * Report an error message to the user.
   * Display the specified objects if tracing is requested.
   * The token provides the filename, line number and column.
   */
  private void userError(int    errno,
                         String text1,
                         String text2,
                         Object o1,
                         Object o2,
                         int    lineNumber,
                         int    column) throws InvalidException
  {
    if (classTrace || fullError) {
      if (o1 != null)
        System.out.println(" 1 " + o1);
      if (o2 != null)
        System.out.println(" 2 " + o2);
    }
    fatalError = true;
    reportError(errno, text1, text2, filename, lineNumber, column);
    throw new InvalidException("error");
  }

  /**
   * Report an error message to the user.
   * Display the specified objects if tracing is requested.
   * The token provides the filename, line number and column.
   */
  private void userError(int    errno,
                         String text1,
                         String text2,
                         int    lineNumber,
                         int    column) throws InvalidException
  {
    userError(errno, text1, text2, null, null, lineNumber, column);
  }

  /**
   * Report an error message to the user.
   * Display the specified objects if tracing is requested.
   * The token provides the filename, line number and column.
   */
  private void userError(int errno, String text1, int lineNumber, int column) throws InvalidException
  {
    userError(errno, text1, null, null, null, lineNumber, column);
  }

  /**
   * Report an error message to the user.
   * Display the specified objects if tracing is requested.
   * The token provides the filename, line number and column.
   */
  private void userError(int errno, String text) throws InvalidException
  {
    userError(errno, text, null, null, null, lineNumber, column);
  }

  /**
   * Report an error message to the user.
   * Display the specified objects if tracing is requested.
   * The token provides the filename, line number and column.
   */
  private void userError(int errno, String text1, String text2) throws InvalidException
  {
    userError(errno, text1, text2, null, null, lineNumber, column);
  }

  /**
   * Report a "not implemented" error message to the user.
   * The token provides the filename, line number and column.
   */
  private void notImplementedError(String msg, int lineNumber, int column) throws InvalidException
  {
    fatalError = true;
    Msg.reportError(Msg.MSG_Not_implemented_s, filename, lineNumber, column, msg);
    throw new InvalidException("not implemented");
  }

  /**
   * Report a "not implemented" error message to the user.
   * The token provides the filename, line number and column.
   */
  private void notImplementedError(String msg) throws InvalidException
  {
    fatalError = true;
    Msg.reportError(Msg.MSG_Not_implemented_s, filename, lineNumber, column, msg);
    throw new InvalidException("not implemented");
  }

  /**
   * Report a warning message to the user.
   * The token provides the filename, line number and column.
   */
  private void userWarning(int errno, String text, int lineNumber, int column)
  {
    Msg.reportWarning(errno, filename, lineNumber, column, text);
  }

  /**
   * This class is used to record the files that use the INCLUDE
   * statement.
   */
  private static class FFile
  {
    public FileReader reader;
    public String     filename;
    public char[]     buffer;     // Raw characters from the source file.
    public char[]     card;       // The next card.
    public int        cardNumber; // What card are we scanning.
    public int        bufPtr;     // Next raw character.
    public int        bufLength;  // How many raw characters.
    public boolean    cardValid;  // Is the next card valid.

    public FFile()
    {
    }
  }

  /**
   * Specify the next file to read.
   */
  private FileReader newFile(String filename) throws IOException
  {
    FileReader xcurReader = new FileReader(filename);
    if (classTrace)
      System.out.println("Reading: " + filename);

    FFile ffile = new FFile();

    ffile.reader     = this.curReader;
    ffile.filename   = this.filename; // The *this* is very important!
    ffile.cardNumber = this.cardNumber;
    ffile.cardNumber = this.cardNumber;
    ffile.cardValid  = this.cardValid;
    ffile.card       = this.card;
    ffile.buffer     = this.buffer;
    ffile.bufPtr     = this.bufPtr;
    ffile.bufLength  = this.bufLength;

    includeStack.push(ffile);

    this.cardNumber = 0;
    this.filename   = filename;
    this.bufPtr     = 0;
    this.bufLength  = 0;
    this.buffer     = new char[1024];
    this.card       = new char[1024];
    this.cardValid  = false;

    return xcurReader;
  }

  /**
   * Return from an INCLUDEd file.
   */
  private FileReader oldFile() throws IOException
  {
    if (includeStack.empty())
      return null;

    FFile      ffile      = includeStack.pop();
    FileReader xcurReader = ffile.reader;

    cardNumber = ffile.cardNumber;
    cardValid  = ffile.cardValid;
    card       = ffile.card;
    filename   = ffile.filename;
    bufPtr     = ffile.bufPtr;
    bufLength  = ffile.bufLength;
    buffer     = ffile.buffer;

    if (classTrace)
      System.out.println("Reading: " + filename);

    return xcurReader;
  }

  /**
   * Read one Fortran statment into the <code>line</code> buffer.
   */
  private void readStatement()
  {
    int start = freeForm ? 0 : 6;

    while (true) {
      if (freeForm)
        readFreeFormStmt();
      else
        readFixedFormStmt();

      if (eofStmt)
        return;

      if (statement[start + 0] != 'i')
        return;
      if (statement[start + 1] != 'n')
        return;
      if (statement[start + 2] != 'c')
        return;
      if (statement[start + 3] != 'l')
        return;
      if (statement[start + 4] != 'u')
        return;
      if (statement[start + 5] != 'd')
        return;
      if (statement[start + 6] != 'e')
        return;

      char delim = statement[start + 7];
      if ((delim != '\'') && (delim != '"'))
        return;

      // Process INCLUDE statement.

      int i = start + 8;
      while (true) {
        char c = statement[i];
        if (c == delim)
          break;
        if (c == 0)
          return;
        i++;
      }

      String fn = new String(statement, start + 8, i - (start + 8));
      if (findIncludeFile(fn, "."))
        continue;
      if (findIncludeFile(fn, userDirs))
        continue;

      reportError(Msg.MSG_Include_file_s_not_found, fn);
      fatalError = true;
    }
  }


  private boolean findIncludeFile(String filename, String directory)
  {
    File   c    = new File(directory, filename);
    String path = c.getAbsolutePath();

    if (path.equals(filename))
      return false; // Same file.

    try {
      FileReader xcurReader = newFile(path);
      if (xcurReader != null) {
        curReader = xcurReader;
        cardValid = false;
        return true;
      }
    } catch (java.lang.Throwable ex) {
    }

    return false;
  }

  private boolean findIncludeFile(String filename, Vector<String> dirs)
  {
    if (dirs == null)
      return false;

    int l = dirs.size();
    for (int j = 0; j < l; j++) {
      String dir  = dirs.elementAt(j);
      if (findIncludeFile(filename, dir))
        return true;
    }
    return false;
  }

  /**
   * Read one Fortran line in free-form into the <code>line</code>
   * buffer.
   */
  private void readFreeFormLine()
  {
    if (!cardValid) {
      while (true) {
        readCard();
        if (!eofCard)
          break;

        eofCard = false;

        try {
          curReader.close();
          curReader = null;
          curReader = oldFile();
        } catch (java.io.IOException ex) {
        }

        if (curReader != null)
          continue;

        eofStmt = true;
        return;
      }
    }

    // Transfer the initial card.

    System.arraycopy(card, 0, line, 0, 132);
    cardValid = false;
    lineNumber = cardNumber;

    // Append any continue cards.

    int linePtr = 131;
    while (true) {
      int i = linePtr;
      while (line[i] == ' ')
        i--;

      if (line[i] != '&')
        break;

      linePtr = i;
      readCard();
      if (eofCard)
        break;

      int ii;
      for (ii = 0; ii < 132; ii++)
        if (card[ii] != ' ')
          break;

      if((linePtr + (132 - ii)) >= line.length) {
        char[] nl = new char[line.length * 2];
        System.arraycopy(line, 0, nl, 0, linePtr);
        line = nl;
      }

      System.arraycopy(card, 0, line, linePtr, 132 - ii);
      cardValid = false;
      linePtr += 132 - ii;
    }

    line[linePtr] = 0;
  }

  /**
   * Read one Fortran statement in free-form into the <code>statement</code>
   * buffer.
   */
  private void readFreeFormStmt()
  {
    if (line[stmtPtr] == 0) {
      readFreeFormLine();
      stmtPtr = 0;
    }

    // Make it look like fixed format.

    while (line[stmtPtr] == ' ')
      stmtPtr++;

    int outPtr = 0;
    while (outPtr < 5) {
      char c = line[stmtPtr];
      if ((c < '0') || (c > '9'))
        break;
      stmtPtr++;
      statement[outPtr++] = c;
    }

    while (outPtr < 6)
      statement[outPtr++] = ' ';

    while (true) {
      char c = line[stmtPtr];

      if (c == 0)
        break;

      stmtPtr++;

      if (c == ';')
        break;

      statement[outPtr++] = c;
    }

    statement[outPtr] = 0;

    boolean inSingleQuote = false;
    boolean inDoubleQuote = false;

    // Eliminate the blanks.

    int inPtr = 6;
    outPtr = 6;
    while (true) {
      char c = statement[inPtr++];

      if (inSingleQuote) {
        if (c == '\'') {
          if(statement[inPtr] != '\'')
            inSingleQuote = false;
          else {
            statement[outPtr++] = c;
            inPtr++;
          }
        }
        statement[outPtr++] = c;
        continue;
      } else if (inDoubleQuote) {
        if (c == '\"') {
          if(statement[inPtr] != '"')
            inDoubleQuote = false;
          else {
            statement[outPtr++] = c;
            inPtr++;
          }
        }
        statement[outPtr++] = c;
        continue;
      }

      if (c == '\'')
        inSingleQuote = true;
      else if (c == '"')
        inDoubleQuote = true;
      else if (c == ' ')
        continue; // Remove the blank.

      if (c == '!') { // Remove ! comment.
        statement[outPtr] = 0;
        break;
      }

      // Convert to lower case.

      if ((c >= 'A') && (c <= 'Z'))
        c = (char) ('a' + (c - 'A'));

      if (outPtr >= statement.length)
        System.out.println("** rffs " + outPtr + " " + new String(statement, 0, outPtr));
      statement[outPtr++] = c;
      if (c == 0)
        break;
    }

    column = 6;

    if (classTrace) {
      System.out.print("** read(");
      System.out.print(lineNumber);
      System.out.print(")");
      System.out.println(new String(statement, 0, outPtr));
    }
  }

  /**
   * Read one "card".  A card is roughly one punched card; look in
   * your history books.  Comment cards are skipped.
   * @param fixedForm if true, specifies fixed-form input
   */
  private void readCard()
  {
    if (eofCard) {
      cardValid = false;
      card[0] = 0;
      return;
    }

    int size = freeForm ? 132 : 72;
    try {
      int     cardPtr = 0;
      boolean comment = false;
      do {
        boolean nb = false;
        cardPtr = 0;

        // Transfer one card's worth into the card buffer.  Skip any
        // characters past the max card size (i.e., skip the card punch
        // sequence in columns 72-80).

        while (true) {
          if (bufPtr >= bufLength) {
            if (eofStmt) {
              cardValid = false;
              card[0] = 0;
              return;
            }
            bufLength = curReader.read(buffer, 0, 1024);
            if (bufLength < 0) {
              eofCard = true;
              break;
            }
            bufPtr = 0;
          }

          char c = buffer[bufPtr++];
          if (c == '\n') {
            if (!nb) {
              card[0] = 'C';
              if (cardPtr <= 0)
                cardPtr = 1;
            }
            card[cardPtr] = 0;
            cardNumber++;
            break;
          } else if (c == '\t') {
            while (cardPtr < 5)
              card[cardPtr++] = ' ';
            c = ' ';
          }

          if (cardPtr >= size)
            continue;

          card[cardPtr++] = c;

          if (c == ' ')
            continue;

          if (!nb && (c == '!') && (freeForm || (cardPtr == 6)))
            card[0] = 'C';

          nb = true;
        }

        if ((cardPtr == 0) && eofCard) {
          eofCard = true;
          cardValid = false;
          return;
        }

        while (cardPtr <size)
          card[cardPtr++] = ' ';
        card[cardPtr] = 0;

        // Skip comment cards.

        char c = card[0];
        comment = (c == 'C') || (c == 'c') || (c == '*') || (c == '!');
      } while (comment);

      cardValid = true;
      return;
    } catch (java.io.IOException ex) {
      reportError(Msg.MSG_s, ex.getMessage(), null, lineNumber, 0);
      eofCard = true;
      cardValid = false;
      fatalError = true;
      card[0] = 0;
    }
  }

  /**
   * Read one Fortran statment in fixed-form into the
   * <code>statement</code> buffer.
   * Remove any ! comments.
   */
  private void readFixedFormStmt()
  {
    while (!cardValid) {
      readCard();
      if (!eofCard)
        break;

      eofCard = false;

      try {
        curReader.close();
        curReader = null;
        curReader = oldFile();
      } catch (java.io.IOException ex) {
      }

      if (curReader != null)
        continue;

      eofStmt = true;
      return;
    }

    // Transfer the initial card.

    System.arraycopy(card, 0, line, 0, 72);
    cardValid = false;
    lineNumber = cardNumber;

    // Append any continue cards.

    int linePtr = 72;
    while (true) {
      readCard();
      if (eofCard || eofStmt)
        break;

      if ((card[5] == ' ') || (card[5] == '0'))
        break;

      if((linePtr + 66) >= line.length) {
        char[] nl = new char[line.length * 2];
        System.arraycopy(line, 0, nl, 0, linePtr);
        line = nl;
        statement = nl;
      }

      System.arraycopy(card, 6, line, linePtr, 66);
      cardValid = false;
      linePtr += 66;
      int fnb = 0;
      while ((card[fnb] == ' ') && (fnb < 5))
        fnb++;
      if ((fnb < 5) && (card[fnb] != '\0') && (card[fnb] != '\t')) {
        System.out.println("** rfl " + fnb + " <" + card[fnb] + "> " + Integer.toHexString(card[fnb]));
        reportError(Msg.MSG_Improper_continuation_statement, "", null, lineNumber, column);
        fatalError = true;
      }
    }
    line[linePtr] = 0;

    boolean inSingleQuote = false;
    boolean inDoubleQuote = false;
    int     inPtr         = 6;
    int     outPtr        = 6;

    // Eliminate the blanks.

    while (true) {
      char c = line[inPtr++];

      if (inSingleQuote) {
        if (c == '\'') {
          if(line[inPtr] != '\'')
            inSingleQuote = false;
          else {
            line[outPtr++] = c;
            inPtr++;
          }
        }
        line[outPtr++] = c;
        if (c == 0)
          break;
        continue;
      } else if (inDoubleQuote) {
        if (c == '\"') {
          if(line[inPtr] != '"')
            inDoubleQuote = false;
          else {
            line[outPtr++] = c;
            inPtr++;
          }
        }
        line[outPtr++] = c;
        if (c == 0)
          break;
        continue;
      }

      if (c == '\'')
        inSingleQuote = true;
      else if (c == '"')
        inDoubleQuote = true;
      else if (c == ' ')
        continue; // Remove the blank.

      if (c == '!') { // Remove ! comment.
        int stop = (inPtr < 72) ? 72 : 72 + (66 * (1 + ((inPtr - 72) / 66)));
        inPtr = stop;
        continue;
      }

      // Convert to lower case.

      if ((c >= 'A') && (c <= 'Z'))
        c = (char) ('a' + (c - 'A'));

      line[outPtr++] = c;
      if (c == 0)
        break;

      // Check for FORMAT statement.

      if (outPtr != 13)
        continue;

      if (line[6] != 'f')
        continue;
      if (line[7] != 'o')
        continue;
      if (line[8] != 'r')
        continue;
      if (line[9] != 'm')
        continue;
      if (line[10] != 'a')
        continue;
      if (line[11] != 't')
        continue;
      if (line[12] != '(')
        continue;

      // Transfer remainder of FORMAT statement unmodified.

      while (true) {
        char cc = line[inPtr++];
        line[outPtr++] = cc;
        if (cc == 0)
          break;
      }
      break;
    }

    column = 6;

    if (classTrace) {
      System.out.print("** read(");
      System.out.print(lineNumber);
      System.out.print(")");
      System.out.println(new String(line, 0, outPtr));
    }
  }

  /**
   * Get the next source statement for processing.  Generate an error
   * message if the scan is not at the end of the current statement.
   */
  private void nextStatement()
  {
    if (eofStmt)
      return;

    if (!fatalError) { // Check for junk after the recognized statement.
      skipBlanks();

      if (statement[column] != 0) {
        System.out.print("** ns <");
        System.out.print(column);
        System.out.print(":");
        System.out.print(statement[column]);
        System.out.print("> eof:");
        System.out.print(eofStmt);
        System.out.print(" ");
        System.out.println(Integer.toHexString(statement[column]));
        reportError(Msg.MSG_Invalid_statement, null, filename, lineNumber, column);
        fatalError = true;
      }
    }

    // Get the next statement.

    readStatement();

    if (eofStmt)
      return;

    column = 6;

    skipBlanks();

    // Process any statement label.

    statementLabel = null;

    int label = 0;

    // Obtain the label if any.

    for (int i = 0; i < 5; i++) {
      char c = statement[i];
      if (c == ' ')
        continue;

      if ((c < '0') || (c > '9')) {
        reportError(Msg.MSG_Invalid_statement_label, null, filename, lineNumber, column);
        fatalError = true;
        return;
      }

      label = label * 10 + (c - '0');
    }

    if (label <= 0) // If no statement label.
      return;

    statementLabel = getLabelDecl(label);
    if (statementLabel.getTag() != 0) {
      reportError(Msg.MSG_Label_s_defined_twice, statementLabel.getName(), filename, lineNumber, column);
      fatalError = true;
    }

    statementLabel.setTag(1);
  }

  /**
   * Add the executable statement to the current block.  Add the label
   * if there is one.
   */
  private void addNewStatement(Statement stmt)
  {
    addNewStatement(stmt, lineNumber, column);
  }

  /**
   * Add the executable statement to the current block.  Add the label
   * if there is one.
   */
  private void addNewStatement(Statement stmt, int line, int col)
  {
    addStmtInfo(stmt, line, col);

    if (statementLabel != null) {
      stmt = new LabelStmt(statementLabel, stmt);
      addStmtInfo(stmt, lineNumber, column);
      statementLabel = null;
    }

    currentBlockStmt.addStmt(stmt);
    if (statementLabel2 != null) {
      Statement stmt2 = new LabelStmt(statementLabel2, new NullStmt());
      addStmtInfo(stmt2, lineNumber, column);
      currentBlockStmt.addStmt(stmt2);
      statementLabel2 = null;
    }
  }

  private void addAssignStmt(Expression lhs, Expression rhs)
  {
    AssignSimpleOp ass = new AssignSimpleOp(lhs, rhs);
    addNewStatement(new EvalStmt(ass));
  }

  /**
   * Add the source line number information to the AST node.
   */
  private void addStmtInfo(Node n, int lineNumber, int column)
  {
    if (n != null)
      n.setSourceLineNumber(lineNumber);
  }

  /**
   * Specify the storage class of a declaration.  Generate an error if
   * the storage class is not valid for the type of declaration.
   */
  private void specifyStorageClass(Declaration decl, int storageClass, int lineNumber, int column)
  {
    if (decl == null)
      return;

    if (decl.isRoutineDecl()) {
      switch (storageClass) {
      case cExtern:   decl.setVisibility(Visibility.EXTERN); break;
      case cStatic:   decl.setVisibility(Visibility.FILE);   break;
      case cGlobal:   decl.setVisibility(Visibility.GLOBAL); break;
      case cRegister:
        reportError(Msg.MSG_Invalid_storage_class, null, filename, lineNumber, column);
        fatalError = true;
        break;
      }
      return;
    }

    decl.setVisibility(Visibility.LOCAL);

    switch (storageClass) {
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
      break;
    case cAuto:
      break;
    case cRegister:
      decl.setResidency(Residency.REGISTER);
      break;
    }
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
   * <li> s - size_t
   * <li> S - size_t*
   * <li> c - char
   * <li> C - char*
   * <li> z - complex
   * <li> Z - complex*
   * <li> x - double complex
   * <li> X - double complex*
   */
  private  Type charToType(char c)
  {
    switch (c) {
    case 'i': return int_type;
    case 'I': return intp_type;
    case 'l': return long_type;
    case 'f': return real_type;
    case 'd': return double_type;
    case 'v': return void_type;
    case 'V': return voidp_type;
    case 's': return size_t_type;
    case 'c': return char_type;
    case 'C': return charp_type;
    case 'z': return float_complex_type;
    case 'Z': return PointerType.create(float_complex_type);
    case 'x': return double_complex_type;
    case 'X': return PointerType.create(double_complex_type);
    default: throw new scale.common.InternalError("Unknown type char(" + c + ").");
    }
  }

  /**
   * Return a FormaalDecl instance for a function parameter with the
   * specified name and type.  If the name is <code>null</code>,
   * generate the name from the vaalue of the <code>counter</code>
   * argument.
   */
  private FormalDecl createFormalDecl(String        name,
                                      Type          type,
                                      ParameterMode mode,
                                      int           counter)
  {
    if (type.isProcedureType())
      type = PointerType.create(type);

    ArrayType at = type.getCoreType().returnArrayType();
    if (type.isArrayType())
      type = PointerType.create(type);

    if (name == null)
      name = "_A" + counter;

    FormalDecl fd = new FormalDecl(name, type, mode);
    return fd;
  }

  /**
   * Return a FormalDecl instance of the type represented by the
   * single character representation of a type.
   */
  private FormalDecl charToFormalDecl(char c, int counter)
  {
    if (c == 'Z')
      return new UnknownFormals();

    return createFormalDecl(null, charToType(c), ParameterMode.REFERENCE, counter);
  }

  /**
   * Return the ProcedureDecl instance of a function with the
   * specified name and signature.  The signaature is a string of
   * characters.  The first character represents the function return
   * type.  Each additional character represnts the type of a
   * parameter.
   */
  private ProcedureDecl defPreKnownFtn(String name, String signature, int purity)
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
      rd.setReferenced();
      rd.setPurityLevel(purity);
      cg.addRootSymbol(rd);
      procMap.put(name, rd);
    }
    return rd;
  }

  private void addSymbol(Declaration decl)
  {
    cg.addSymbol(decl);
  }

  /**
   * If the address of a constant is required or the constant is not
   * an atomic value, it must be allocated to memory.
   */
  private VariableDecl createVarFromExp(Literal lit)
  {
    VariableDecl decl = new VariableDecl("_V" + varCounter++, lit.getType(), lit);
    decl.setResidency(Residency.MEMORY);
    decl.setVisibility(Visibility.FILE);
    decl.setAddressTaken();
    cg.addRootSymbol(decl);
    decl.setReferenced();
    cg.addTopLevelDecl(decl);
    return decl;
  }

  /**
   * If the address of a constant is required or the constant is not
   * an atomic value, it must be allocated to memory.
   */
  private VariableDecl createLocalVarFromExp(Literal lit)
  {
    VariableDecl decl = genTemp(lit.getType());
    decl.setValue(lit);
    decl.setResidency(Residency.MEMORY);
    decl.setAddressTaken();
    addSymbol(decl);
    decl.setReferenced();
    return decl;
  }

  private IdAddressOp genDeclAddress(Declaration decl)
  {
    return new IdAddressOp(PointerType.create(decl.getType()), decl);
  }

  /**
   * Convert non-atomic constant expressions to variable references.
   */
  private Expression convertLiterals(Expression expr)
  {
     if (fatalError)
       return errExp;

    if ((expr instanceof StringLiteral) ||
        (expr instanceof IntArrayLiteral) ||
        (expr instanceof FloatArrayLiteral)) {
      VariableDecl vd = createVarFromExp((Literal) expr);
      vd.setReferenced();
      Type type = vd.getType();

      FixedArrayType at = type.getCoreType().returnFixedArrayType();
      if (at != null) {
        vd.setAddressTaken();
        type = PointerType.create(at.getArraySubtype());
        Vector<Expression> subs = new Vector<Expression>(1);
        subs.add(zero);
        SubscriptAddressOp sop = new SubscriptAddressOp(type,
                                                        genDeclAddress(vd),
                                                        subs);
        sop.setFortranArray();
        return sop;
      }

      if (type.isProcedureType()) {
        vd.setAddressTaken();
        return genDeclAddress(vd);
      }

      return new IdValueOp(vd);
    }

    return expr;
  }

  private IdReferenceOp fixVariableRef(Declaration decl)
  {
    FormalDecl fd = decl.returnFormalDecl();
    if ((fd != null)  && (fd.getMode() == ParameterMode.REFERENCE))
      return new IdValueOp(fd);
    return genDeclAddress(decl);
  }

  /**
   * Generate a call to a routine.
   */
  private Expression genCall(ProcedureType      pt,
                             Expression         proc,
                             Vector<Expression> args,
                             int                lineNumber,
                             int                column) throws InvalidException
  {
    if (fatalError)
      return errExp;

    int          i  = 0;
    VariableDecl vd = null;

    if (pt.isFChar()) {
      // Function returns a CHARACTER value.  The first argument is the
      // address at which to place the returned value and the second
      // argument is the size of that area.

      if (args == null)
        args = new Vector<Expression>(2);

      FormalDecl fd = pt.getFormal(0);
      Type       at = fd.getType();
      int        cl = getStringLength(at);

      vd = genTemp(FortranCharType.create(cl));
      addSymbol(vd);
      vd.setReferenced();

      args.insertElementAt(LiteralMap.put(cl, int_type), 0);
      args.insertElementAt(genDeclAddress(vd), 0); // To be replaced later.
      i = 2;
    }

    int l = 0;
    if (args != null)
      l = args.size();

    int m = pt.numFormals();
    if ((l != m) && (m > 0)) {
      if (classTrace)
        System.out.println("   " + pt);
      userError(Msg.MSG_Call_does_not_match_routine_definition, "");
    }

    for (; i < l && i < m; i++) {
      Expression arg = convertLiterals(args.get(i));

      if (arg instanceof IdAddressOp)
        ((IdAddressOp) arg).getDecl().setAddressTaken();

      FormalDecl fd = pt.getFormal(i);
      if (fd instanceof UnknownFormals)
        break;

      Type type = calcArgType(fd.getType(), arg.getType());
      if (type == null)
        userError(Msg.MSG_Incompatible_function_argument_for_s, fd.getName(), lineNumber, column);

      args.set(i, cast(type, arg, lineNumber, column));
    }

    for (; i < l; i++) {
      Expression arg = convertLiterals(args.get(i));

      if (arg instanceof IdAddressOp)
        ((IdAddressOp) arg).getDecl().setAddressTaken();

      Type ty = arg.getCoreType();
      if (ty.isRealType())
        arg = cast(double_type, arg, lineNumber, column);
      else if (isIntegerType(ty)) {
        Type        aty = int_type;
        IntegerType ity = ty.returnIntegerType();
        if (ity != null) {
          if (ity.bitSize() <= int_type_size)
            aty =  int_type;
          else
            aty = long_type;
        }
        arg = cast(aty, arg, lineNumber, column);
      }
      args.set(i, arg);
    }

    CallOp call = new CallFunctionOp(pt.getReturnType(), proc, args);
    if (proc instanceof IdAddressOp) {
      RoutineDecl rd = (RoutineDecl) ((IdAddressOp) proc).getDecl();
      fixupCalls.add(rd, call);
    }

    if (vd == null)
      return call;

    return new SeriesOp(call, new IdValueOp(vd));
  }

  private Statement makeCallExitStmt(Expression arg) throws InvalidException
  {
    arg = getConstantValue(arg);
    if (getStringLength(arg.getCoreType()) >= 0) {
      ProcedureDecl      pd   = defPreKnownFtn("_scale_stop", "vCi", RoutineDecl.NOTPURE);
      Vector<Expression> args = new Vector<Expression>(2);
      args.add(makeLValue(arg));
      args.add(getStringLength(arg));
      Expression call = genCall(pd.getSignature(), genDeclAddress(pd), args, lineNumber, column);
      return new EvalStmt(call);
    }

    if (arg.getType().isIntegerType())
      return new ExitStmt(arg);

    userError(Msg.MSG_Invalid_expression, "");
    return errStmt;
  }

  /**
   * Return true if the type is used to represent the Fortran
   * CHARACTER type. The <code>char_type is used for CHARACTER*1 while
   * the FortranCharType is used for all other CHARACTER types.
   */
  private boolean isFCharType(Type type)
  {
    type = type.getPointedToCore();
    return ((type == char_type) || type.isFortranCharType());
  }

  /**
   * Return the Fortran CHARACTER type length or -1 if it is not a
   * Fortran CHARACTER type.
   */
  protected int getStringLength(Type type)
  {
    type = type.getPointedToCore();
    ArrayType at = type.returnArrayType();
    if (at != null)
      type = at.getElementType().getCoreType();

    if (type == char_type)
      return 1;

    FortranCharType fct = type.returnFortranCharType();
    if (fct != null)
      return fct.getLength();

    return -1;
  }

  /**
   * Return the Fortran CHARACTER length of the expression or 1 if it
   * is not a Fortran CHARACTER type.
   */
  private Expression getStringLength(Expression exp)
  {
    if (fatalError)
      return one;

    if (exp == null)
      return zero;

    Type type = exp.getPointedToCore();

    if (type == char_type)
      return one;

    FortranCharType fct = type.returnFortranCharType();
    if (fct != null) {
      int l = fct.getLength();
      if (l > 0)
        return LiteralMap.put(l, int_type);
    }

    if (exp instanceof DereferenceOp)
      exp = ((DereferenceOp) exp).getExpr();

    if (exp instanceof SubstringOp) {
      SubstringOp sop   = (SubstringOp) exp;
      Expression  first = sop.getFirst();
      Expression  last  = sop.getLast();
      try {
        Expression  sub = new SubtractionOp(int_type, cast(int_type, last), cast(int_type, first));
        return new AdditionOp(int_type, sub, one);
      } catch (InvalidException ex) {
        return null;
      }
    }

    if (exp instanceof IdReferenceOp) {
      Declaration decl = ((IdReferenceOp) exp).getDecl();
      if (decl.isVariableDecl()) {
        if (decl.isFormalDecl()) {
          FormalDecl fd = fctMap.get(decl);
          if (fd != null)
            return new IdValueOp(fd);
        }

        type = decl.getCoreType();

        ArrayType at = type.returnArrayType();
        if (at != null) {
          type = at.getElementType().getCoreType();
          FortranCharType fct2 = type.returnFortranCharType();
          if (fct2 != null) {
            int l = fct2.getLength();
            if (l > 0)
              return LiteralMap.put(l, int_type);
          } else if (type == char_type)
            return one;
        }
      }
    }

    return null;
  }

  private Expression getConstantValue(Expression exp)
  {
    if (exp == null)
      return null;

    Literal lit = exp.getConstantValue();
    if ((lit == Lattice.Bot) || (lit == Lattice.Top))
      return exp;
    return lit;
  }

  /**
   * Generate a cast expression.
   */
  private Expression cast(Type type, Expression exp, int lineNumber, int column) throws InvalidException
  {
    if (fatalError)
      return errExp;

    if (type == null)
      return exp;

    CastMode cr   = TypeConversionOp.determineCast(type, exp.getType());
    Type     from = exp.getCoreType();
    Type     to   = type.getCoreType();

    switch (cr) {
    case NONE:
      return exp;

    default:
      userError(Msg.MSG_Unknown_conversion, null);
      return errExp;

    case INVALID: {
      if (to.isComplexType()) {
        Type  ty = (to == float_complex_type) ? real_type : double_type;
        if (from.isComplexType()) {
          Expression r  = null;
          Expression i  = null;
          if (exp instanceof ComplexOp) {
            r =  cast(ty, ((ComplexOp) exp).getExpr1(), lineNumber, column);
            i =  cast(ty, ((ComplexOp) exp).getExpr2(), lineNumber, column);
          } else if (exp instanceof ComplexLiteral) {
            r =  cast(ty, ((ComplexLiteral) exp).getRealPart(), lineNumber, column);
            i =  cast(ty, ((ComplexLiteral) exp).getImaginaryPart(), lineNumber, column);
          } else {
            r = new TypeConversionOp(ty, exp, CastMode.REAL);
            i = new TypeConversionOp(ty, exp, CastMode.IMAGINARY);
          }
          return new ComplexOp((ComplexType) to, r, i);
        }

        return new ComplexOp((ComplexType) to, cast(ty, exp), cast(ty, dzero));
      } else if (from.isComplexType()) {
        return cast(to, new TypeConversionOp((from == float_complex_type) ? real_type : double_type,
                                             exp,
                                             CastMode.REAL));
      }

      if (fullError || classTrace) {
        System.out.println("** cast to " + type);
        System.out.println("     core  " + to);
        System.out.println("     from  " + from);
        System.out.println("     of    " + exp);
        Debug.printStackTrace();
      }

      userError(Msg.MSG_Unknown_conversion, null, lineNumber, column);
      return errExp;
    }
    case CAST:
      if (exp instanceof AddressLiteral)
        return ((AddressLiteral) exp).copy(type);
      if (exp instanceof IntLiteral)
        return LiteralMap.put(((IntLiteral) exp).getLongValue(), type);

      if (to.equivalent(from))
        return exp;

      if (type.isPointerType()) {
        ArrayType at = type.getPointedToCore().returnArrayType();
        if (at != null)
          type = PointerType.create(at.getElementType());
      }

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
        if (type.getCoreType() == real_type)
          return LiteralMap.put(il.convertToFloat(), type);
        return LiteralMap.put(il.convertToDouble(), type);
      }
      if (exp instanceof FloatLiteral)
        return LiteralMap.put(((FloatLiteral) exp).getDoubleValue(), type);
      return new TypeConversionOp(type, exp, CastMode.REAL);
    }
  }

  /**
   * Generate a cast expression.
   */
  private Expression cast(Type type, Expression exp) throws InvalidException
  {
    return cast(type, exp, lineNumber, column);
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


    if (t1.isIntegerType()) {
      if (t2.isIntegerType()) {
        if (((IntegerType) t1).bitSize() < ((IntegerType) t2).bitSize())
          return type2;
        return type1;
      } else if (t2.isComplexType()) {
        return t2;
      } else if (t2.isRealType()) {
        return type2;
      } else if (t2.isPointerType())
        return t2.getNonConstType();
    } else if (t1.isComplexType()) {
      if (t2.isComplexType() && (t2 == double_complex_type))
        return type2;
      return type1;
    } else if (t1.isRealType()) {
      if (isIntegerType(t2))
        return type1;

      if (t2.isComplexType())
        return type2;

      if (t2.isRealType()) {
        RealType it1 = (RealType) t1;
        RealType it2 = (RealType) t2;
        if (it1.bitSize() > it2.bitSize())
          return type1;
        return type2;
      }
    } else if (t1.isPointerType() && t2.isIntegerType())
      return t2.getNonConstType();
    else if (t1.isFortranCharType() || t2.isFortranCharType()) {
      if (getStringLength(t1) >= getStringLength(t2))
        return t1;
      return t2;
    }

    if (t1 == t2)
      return t1;

    if (classTrace) {
      System.out.println("** cbt " + type1);
      System.out.println("       " + t1);
      System.out.println("       " + type2);
      System.out.println("       " + t2);
      Debug.printStackTrace();
    }

    return null;
  }

  /**
   * Determine the type to use for the type of the right-hand-side of
   * an assignment statement.  Needs a lot of work.
   */
  private Type calcAssignType(Type type1, Type type2)
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
      else {
        ArrayType at = t2.returnArrayType();
        if (at != null)
          return calcAssignType(type1, at.getElementType());
      }
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
    } else if (t1.isArrayType() && t2.isPointerType()) {
      return type2;
    }

    return null;
  }

  /**
   * Determine the type to use for the argument to a function.  Needs
   * a lot of work.
   */
  private Type calcArgType(Type type1, Type type2) throws InvalidException
  {
    Type t1 = type1.getCoreType();
    Type t2 = type2.getCoreType();

    if (t1 == t2)
      return type1;

    if (t1.isPointerType() && t2.isPointerType())
      return type1;

    if (t2.isPointerType()) {
      Type t2p = t2.getPointedTo().getCoreType();
      ArrayType at = t2p.returnArrayType();
      if (at != null) {
        type2 = PointerType.create(at.getElementType());
        t2 = type2;
      }
    }

    ArrayType at = t1.returnArrayType();
    if (at != null) {
      type1 = PointerType.create(at.getElementType());
      t1 = type1;
    }

    IntegerType it1 = t1.returnIntegerType();
    if (it1 != null) {
      if (it1.bitSize() < int_type_size)
        type1 = int_type;
      if (t2.isIntegerType()) {
        return type1;
      } else if (t2.isPointerType())
        return type1;
      else if (t2.isEnumerationType())
        return type1;
      else if (t2.isBooleanType())
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

    if (t1.isUnionType())
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

  /**
   * Return the label declaration for the specified label.
   * Create it if it doesn't already exist.
   */
  private LabelDecl getLabelDecl(int label)
  {
    LabelDecl ld = labels.get(label);
    if (ld != null)
      return ld;
    ld = new LabelDecl("_L" + label);
    labels.put(label, ld);
    addSymbol(ld);
    return ld;
  }

  /**
   * Return the variable or function declaration for the name or
   * <code>null</code> if none.
   */
  private Declaration lookupDecl(String name)
  {
    Vector<SymtabEntry> ents = cg.getSymbolTable().lookupSymbol(name);
    if (ents == null)
      return null;

    Enumeration<SymtabEntry> en = ents.elements();
    while (en.hasMoreElements()) {
      SymtabEntry s    = en.nextElement();
      Declaration decl = s.getDecl();

      if (decl.isVariableDecl() && !decl.isCommonBaseVariable())
        return decl;
      if (decl instanceof EnumElementDecl)
        return decl;
      if (decl.isRoutineDecl())
        return decl;
      if (decl instanceof StmtFtnDecl)
        return decl;
    }

    return null;
  }

  /**
   * Return the variable or function declaration for the name in the
   * root symbol table or <code>null</code> if none.
   */
  private Declaration lookupRootDecl(String name)
  {
    Vector<SymtabEntry> ents = cg.getSymbolTable().getRootScope().lookup(name);
    if (ents == null)
      return null;

    Enumeration<SymtabEntry> en = ents.elements();
    while (en.hasMoreElements()) {
      SymtabEntry s    = en.nextElement();
      Declaration decl = s.getDecl();

      VariableDecl vd = decl.returnVariableDecl();
      if ((vd != null) && !vd.isCommonBaseVariable())
        return decl;
      if (decl instanceof EnumElementDecl)
        return decl;
      if (decl.isRoutineDecl())
        return decl;
      if (decl instanceof StmtFtnDecl)
        return decl;
    }

    return null;
  }

  /**
   * Advance the scan past any blanks.
   */
  private void skipBlanks()
  {
    while (statement[column] == ' ')
      column++;
  }

  /**
   * Generate an error if the next character is not the specified
   * character.  Advance the scan if it is.
   */
  private boolean nextCharMustBe(char c) throws InvalidException
  {
    if (statement[column] == c) {
      column++;
      return true;
    }

    userError(Msg.MSG_Expecting_s_found_s, "'" + c + "'", "'" + statement[column] + "'");
    return false;
  }

  /**
   * Generate an error if the next non-blank character is not the
   * specified character.  Advance the scan if it is.
   */
  private boolean nextNBCharMustBe(char c) throws InvalidException
  {
    while (statement[column] == ' ')
      column++;

    if (statement[column] == c) {
      column++;
      return true;
    }

    userError(Msg.MSG_Expecting_s_found_s, "'" + c + "'", "'" + statement[column] + "'");
    return false;
  }

  /**
   * Return true if the next character is the specified character.
   * Advance the scan if it is.
   */
  private boolean nextCharIs(char c)
  {
    if (statement[column] == c) {
      column++;
      return true;
    }

    return false;
  }

  /**
   * Return true if the next non-blank character is the specified
   * character.  Advance the scan if it is.
   */
  private boolean nextNBCharIs(char c)
  {
    while (statement[column] == ' ')
      column++;

    if (statement[column] == c) {
      column++;
      return true;
    }

    return false;
  }

  /**
   * Return true if the next non-blank character is end-of-line.
   * Advance the scan past the blanks.
   */
  private boolean nextNBCharIsEOL()
  {
    while (statement[column] == ' ')
      column++;

    if (statement[column] == 0)
      return true;

    return false;
  }

  /**
   * Return the next keyword.  Advance the scan past the keyword.
   */
  private int nextKeyword()
  {
    assert traceIn("nextKeyword", null);

    skipBlanks();
    int key = Keywords.lookup(this, statement, column);
    assert traceOut("nextKeyword", Keywords.keywords[key]);
    return key;
  }

  /**
   * Set the source line scan position.
   * @see Keywords
   */
  public final void setColumn(int column)
  {
    this.column = column;
  }

  /**
   * Return the next decimal integer.
   * @throws InvalidException if no decimal integer found
   */
  private long nextInteger() throws InvalidException
  {
    assert traceIn("nextInteger", null);
    long value = 0;
    try {
      skipBlanks();

      char c = statement[column];
      if ((c < '0') || (c > '9'))
        throw new InvalidException("Not a decimal integer");

      column++;

      value = c - '0';

      while (true) {
        c = statement[column];
        if (c == ' ') {
          column++;
          continue;
        }

        if (c < '0')
          break;
        if (c > '9')
          break;

        column++;
        value = value * 10 + (c - '0');
      }

      return value;
    } finally {
      assert traceOut("nextInteger", Long.toString(value));
    }
  }

  /**
   * Return the next binary integer.
   * @throws InvalidException if no binary integer found
   */
  private long nextBinaryValue() throws InvalidException
  {
    assert traceIn("nextBinaryValue", null);
    long s = 0;
    try {

      skipBlanks();

      char c = statement[column];
      if ((c < '0') || (c > '1'))
        throw new InvalidException("Not a binary integer");

      if (c == '1')
        s = 1;

      column++;

      int k = 0;
      while (k < 64) {
        c = statement[column];
        if (c == '1') {
          s = (s << 1) | 1;
          column++;
        } else if (c == '0') {
          column++;
        } else
          return s;

        k++;
      }

      throw new InvalidException("Not a binary integer");
    } finally {
      assert traceOut("nextBinaryValue", Long.toHexString(s));
    }
  }

  /**
   * Return the next octal integer.
   * @throws InvalidException if no octal integer found
   */
  private long nextOctalValue() throws InvalidException
  {
    assert traceIn("nextOctalValue", null);
    long s = 0;
    try {
      skipBlanks();

      char c = statement[column];
      if ((c < '0') || (c > '7'))
        throw new InvalidException("Not an octal integer");

      s = c - '0';

      column++;

      int k = 0;
      while (k < 11) {
        c = statement[column];
        if ((c >= '0') && (c <= '7')) {
          s = (s << 3) + (c - '0');
          column++;
        } else
          return s;

        k++;
      }

      throw new InvalidException("Not an octal integer");
    } finally {
      assert traceOut("nextOctalValue", Long.toOctalString(s));
    }
  }

  /**
   * Return the next hex integer.
   * @throws InvalidException if no hex integer found
   */
  private long nextHexValue() throws InvalidException
  {
    assert traceIn("nextHexValue", null);
    long s = 0;
    try {
      skipBlanks();

      char c = statement[column];
      if ((c >= '0') && (c <= '9'))
        s = c - '0';
      else if ((c >= 'a') && (c <= 'f'))
        s = 10 + c - 'a';
      else if ((c >= 'A') && (c <= 'F'))
        s = 10 + c - 'A';
      else
        throw new InvalidException("Not a hex integer");

      column++;

      int k = 0;
      while (k < 16) {
        c = statement[column];
        if ((c >= '0') && (c <= '9')) {
          s = (s << 4) + (c - '0');
          column++;
        } else if ((c >= 'a') && (c <= 'f')) {
          s = (s << 4) + 10 + (c - 'a');
          column++;
        } else if ((c >= 'A') && (c <= 'F')) {
          s = (s << 4) + 10 + (c - 'A');
          column++;
        } else
          return s;

        k++;
      }

      throw new InvalidException("Not a hex integer");
    } finally {
      assert traceOut("nextHexValue", Long.toHexString(s));
    }
  }

  /**
   * Return the label specified by the integer in the source
   * statement.
   */
  private LabelDecl nextLabel() throws InvalidException
  {
    assert traceIn("nextLabel", null);
    long label = 0;
    try {
      label = nextInteger();
      if ((label < 1) || (label > 99999))
        userError(Msg.MSG_Invalid_statement_label, "");
      return getLabelDecl((int) label);
    } catch (InvalidException ex) {
      userError(Msg.MSG_Invalid_statement_label, "");
      return null;
    } finally {
      assert traceOut("nextLabel", Long.toString(label));
    }
  }

  /**
   * Return the label specified by the integer in the source
   * statement or <code>null</code> if none found.
   */
  private LabelDecl nextLabelNoChk()
  {
    assert traceIn("nextLabelNoChk", null);
    long label = 0;
    try {
      label = nextInteger();
      if ((label < 1) || (label > 99999))
        return null;
      return getLabelDecl((int) label);
    } catch (InvalidException ex) {
      return null;
    } finally {
      assert traceOut("nextLabelNoChk", Long.toString(label));
    }
  }

  /**
   * Return true if the specified character could be the first
   * character of a name.
   */
  private boolean isValidIdFirstChar(char c)
  {
    return (((c >= 'a') && (c <= 'z')) || (c == '_'));
  }

  /**
   * Return true if the specified character could be a character of a
   * name.
   */
  private boolean isValidIdChar(char c)
  {
    return (((c >= 'a') && (c <= 'z')) || (c == '_') || ((c >= '0') && (c <= '9')));
  }

  /**
   * Return the next identifier or <code>null</code> if none found.
   */
  private String nextIdentifier()
  {
    assert traceIn("nextIdentifier", null);
    String id = null;
    try {
      skipBlanks();

      char c = Character.toLowerCase(statement[column]);
      if (isValidIdFirstChar(c)) {
        int start = column;
        column++;

        while (true) {
          c = Character.toLowerCase(statement[column]);
          if (isValidIdChar(c)) {
            column++;
            continue;
          }

          if (c == ' ') {
            column++;
            continue;
          }

          break;
        }

        char s = statement[column];
        statement[column] = '_';
        id = new String(statement, start, column - start + 1);
        statement[column] = s;

        return id;
      }

      return null;
    } finally {
      assert traceOut("nextIdentifier", id);
    }
  }

  private VariableDecl nextVariable() throws InvalidException
  {
    assert traceIn("nextVariable", null);
    VariableDecl vd = null;
    try {
      String name = nextIdentifier();
      if (name != null) {
        Declaration decl = lookupDecl(name);

        if (decl == null) {
          Object o = nameMap.get(name);
          if ((o != null) && !(o instanceof Declaration)) {
            userError(Msg.MSG_s_is_not_a_variable, name);
            return null;
          }

          decl = (Declaration) o;
          if (decl == null)
            decl = new VariableDecl(name, determineTypeFromName(name, void_type));
          addSymbol(decl);
        }

        vd = decl.returnVariableDecl();
        if (vd != null)
          return vd;
      }

      userError(Msg.MSG_s_is_not_a_variable, name);
      return null;
    } finally {
      assert traceOut("nextVariable", vd);
    }
  }

  private EquivalenceDecl nextEqVariable(VariableDecl base) throws InvalidException
  {
    assert traceIn("nextEqVariable", null);
    EquivalenceDecl vd = null;
    try {
      String name = nextIdentifier();
      if (name == null)
        return null;

      Declaration decl = lookupDecl(name);

      if (decl == null) {
        Object o = nameMap.get(name);
        if ((o != null) && !(o instanceof Declaration)) {
          userError(Msg.MSG_Variable_s_already_defined, name);
          return null;
        }
        decl = (Declaration) o;
      }

      if (decl == null) {
        Type ty  = determineTypeFromName(name, void_type);
        int  aln = ty.alignment(Machine.currentMachine);
        vd = new EquivalenceDecl(name, RefType.createAligned(ty, aln), base, -1);
        addSymbol(vd);
        return vd;
      }

      VariableDecl vd2 = decl.returnVariableDecl();
      if ((vd2 == null) || vd2.isEquivalenceDecl())
        userError(Msg.MSG_Variable_s_already_defined, name);
      else {
        Type            ty  = vd2.getType();
        int             aln = ty.alignment(Machine.currentMachine);
        EquivalenceDecl vde = new EquivalenceDecl(name, RefType.createAligned(ty, aln), base, -1);
        if (cg.getSymbolTable().replaceSymbol(vd2, vde) == null)
          addSymbol(vde);

        if (currentEquivSet != null)
          currentEquivSet.update(vd2, vde);

        vd = vde;
      }

      return vd;
    } finally {
      assert traceOut("nextEqVariable", vd);
    }
  }

  private int nextDotOp()
  {
    assert traceIn("nextDotOp", null);
    int x = 0;
    try {
      skipBlanks();

      char c1 = statement[column + 0];
      if (c1 == 0) {
        x = DOT_NA;
        return x;
      }
      char c2 = statement[column + 1];

      if (c2 == '=') {
        if (c1 == '=') {
          column += 2;
          x = DOT_EQ;
          return x;
        } else if (c1 == '/') {
          column += 2;
          x = DOT_NEQ;
          return x;
        } else if (c1 == '>') {
          column += 2;
          x =  DOT_GE;
          return x;
        } else if (c1 == '<') {
          column += 2;
          x = DOT_LE;
          return x;
        }
        x = DOT_NA;
        return x;
      } else if (c1 == '<') {
        column++;
        x = DOT_LT;
        return x;
      } else if (c1 == '>') {
        column++;
        x = DOT_GT;
        return x;
      } else if (c1 != '.') {
        x = DOT_NA;
        return x;
      }

      column++;
      skipBlanks();

      String op = null;
      char   c  = Character.toLowerCase(statement[column]);
      if (isValidIdFirstChar(c)) {
        int start = column;
        column++;

        while (true) {
          c = Character.toLowerCase(statement[column]);
          if (isValidIdChar(c)) {
            column++;
            continue;
          }
          break;
        }

        op = new String(statement, start, column - start);
      } else {
        x = DOT_NA;
        return x;
      }

      skipBlanks();
      if (statement[column] != '.') {
        x = DOT_NA;
        return x;
      }
      column++;

      for (int i = 1; i < dotOps.length; i++) {
        if (op.equals(dotOps[i])) {
          x = i;
          return x;
        }
      }

      x = DOT_NA;
      return x;
    } finally {
      assert traceOut("nextDotOp", dotOpStrs[x]);
    }
  }

  /**
   * Return a numeric literal or <code>null</code> if one is not
   * found.
   */
  private Literal nextNumericConstant()
  {
    assert traceIn("nextNumericConstant", null);
    Literal lit = null;
    try {
      int     sc  = column;
      boolean neg = false;

      if (nextNBCharIs('-'))
        neg = true;
      else 
        nextCharIs('+');

      skipBlanks();

      int k = column;

      while (true) {
        char c = statement[column];
        if ((c < '0') || (c > '9'))
          break;
        column++;
      }

      boolean dec = false;
      if (statement[column] == '.') {
        int scx = column;
        int op  = nextDotOp();
        column = scx;
        if (op == DOT_NA) {
          column++;
          dec = true;
          while (true) {
            char c = statement[column];
            if ((c < '0') || (c > '9'))
              break;
            column++;
          }
        }
      }

      boolean exp = false;
      boolean d   = false;
      char    ch  = statement[column];

      if (ch == 'd') {
        d = true;
        ch = 'e';
      }

      if (ch == 'e') {
        column++;
        if (nextNBCharIs('-'))
          ;
        else 
          nextCharIs('+');
        exp = true;
        while (true) {
          char c = statement[column];
          if ((c < '0') || (c > '9'))
            break;
          column++;
        }
      }

      int len = column - k;
      if (len <= 0) {
        column = sc;
        return null;
      }

      String str = new String(statement, k, len);

      if (d)
        str = str.replace('d', 'e');

      try {
        if (exp || dec) {
          double  value = Double.parseDouble(str);
          lit =  LiteralMap.put(neg ? -value : value, d ? double_type : real_type);
        } else {
          long    value = Long.parseLong(str);
          boolean isInt = ((int) value) == value;
          lit = LiteralMap.put(neg ? -value : value, isInt ? int_type : long_type);
        }
      } catch (java.lang.NumberFormatException ex) {
        if (classTrace) {
          System.out.println("   " + str);
          ex.printStackTrace();
        }
        column = sc;
      }

      return lit;
    } finally {
      assert traceOut("nextNumericConstant", lit);
    }
  }

  /**
   * Return a numeric array of values or <code>null</code> if one is
   * not found.
   */
  private Literal nextArrayConstant() throws InvalidException
  {
    if (!allowF90Features)
      return null;

    Literal lit = null;
    try {
      assert traceIn("nextArrayConstant", null);

      nextNBCharMustBe('/');

      ArrayType at  = requiredType.getCoreType().returnArrayType();
      if (at == null)
        userError(Msg.MSG_Invalid_type, "");

      Type    et  = at.getElementType();
      int     n   = (int) at.numberOfElements();
      boolean flt = false;

      IntArrayLiteral   liti = null;
      FloatArrayLiteral litf = null;

      if (et.isIntegerType()) {
        liti = new IntArrayLiteral(requiredType, n);
        lit = liti;
      } else if (et.isRealType()) {
        litf = new FloatArrayLiteral(requiredType, n);
        lit = litf;
        flt = true;
      } else
        userError(Msg.MSG_Invalid_type, "");

      do {
        long    repeat = 1;
        Literal value  = nextConstantPrimaryExpr();
        if (nextNBCharIs('*')) {
          if (value instanceof IntLiteral) {
            repeat = ((IntLiteral) value).getLongValue();
            if (repeat < 0)
              userError(Msg.MSG_Not_an_integer_constant, null);
            value = nextConstantPrimaryExpr();
          }
        }

        if (flt) {
          double val = 0;
          if (value instanceof IntLiteral)
            val = ((IntLiteral) value).getLongValue();
          else if (value instanceof FloatLiteral)
            val = ((FloatLiteral) value).getDoubleValue();
          for (int i = 0; i < repeat; i++)
            litf.addElement(val);
        } else {
          long val = 0;
          if (value instanceof IntLiteral)
            val = ((IntLiteral) value).getLongValue();
          else if (value instanceof FloatLiteral)
            val = (long) ((FloatLiteral) value).getDoubleValue();
          for (int i = 0; i < repeat; i++)
            liti.addElement(val);
        }
      } while (nextNBCharIs(','));

      nextNBCharMustBe('/');
      return lit;
    } finally {
      assert traceOut("nextArrayConstant", lit);
    }
  }

  private ProcedureDecl nextDefinedBinaryOp() throws InvalidException
  {
    return null;
  }

  private ProcedureDecl nextDefinedUnaryOp() throws InvalidException
  {
    return null;
  }

  private Expression nextArrayExpr() throws InvalidException
  {
    assert traceIn("nextArrayExpr", null);
    Expression expr = null;
    try {
      arrayExprType = null;
      arrayExprSubscripts.clear();
      expr = nextExpression(NORMAL_EXPR);
      if (arrayExprType == null)
        return expr;

      // Construct a loop to fill array with expression values.

      ArrayType      lt  = arrayExprType;
      FixedArrayType flt = lt.returnFixedArrayType();
      if (flt != null)
        expr = copyToFixedArray(null, expr, flt);
      else
        expr = copyToAllocArrayFromAlloc(null, expr, lt.returnAllocArrayType());

      return expr;
    } finally {
      arrayExprType = null;
      assert traceOut("nextArrayExpr", expr);
    }
  }

  private Expression copyToFixedArray(VariableDecl   dest,
                                      Expression     expr,
                                      FixedArrayType lt)
  {
    assert arrayExprType.isFixedArrayType();

    Type   et   = lt.getElementType();
    Type   pet  = PointerType.create(et);
    int    rank = lt.getRank();
    Vector<Expression> subs = new Vector<Expression>();
    for (int i = 0 ; i < rank; i++) {
      Expression ind = new IdValueOp(arrayExprSubscripts.get(i));
      Bound      bd  = lt.getIndex(i);
      Expression sub = new SubtractionOp(size_t_type, bd.getMin(), one);
      subs.add(new AdditionOp(size_t_type, ind, sub));
    }

    VariableDecl ad = dest;
    if (ad == null) {
      ad = genTemp(lt);
      addSymbol(ad);
      ad.setReferenced();
    }

    Expression         add  = genDeclAddress(ad);
    SubscriptAddressOp sub  = new SubscriptAddressOp(pet, add, subs);
    Statement          body = new EvalStmt(new AssignSimpleOp(sub, expr));
    sub.setFortranArray();
    body = buildDoLoops(lt, body, arrayExprSubscripts);
    expr = new StatementOp(PointerType.create(lt), add, body);
    return expr;
  }

  private Expression copyToAllocArrayFromFixed(VariableDecl   dest,
                                               Expression     expr,
                                               AllocArrayType aat)
  {
    assert (dest != null);
    assert arrayExprType.isFixedArrayType();

    Type   et   = aat.getElementType();
    Type   pet  = PointerType.create(et);
    int    rank = aat.getRank();

    RecordType rty    = (RecordType) aat.getStruct().getCoreType();
    RecordType tdim   = dimType.getCoreType().returnRecordType();
    Type       ptdim  = PointerType.create(dimType);
    FieldDecl  fsize  = tdim.findField("size");
    FieldDecl  ffirst = tdim.findField("first");
    FieldDecl  fptr   = rty.findField("ptr");
    FieldDecl  fdims  = rty.findField("dims");
    Type       tfdims = fdims.getType();

    Vector<Expression> subs = new Vector<Expression>();
    for (int i = 0 ; i < rank; i++) {
      Vector<Expression> inds1 = new Vector<Expression>(1);

      inds1.add(LiteralMap.put(i, int_type));

      Expression ind  = new IdValueOp(arrayExprSubscripts.get(i));
      Expression dims = new SelectIndirectOp(genDeclAddress(dest), fdims);
      Expression sub1 = new SubscriptAddressOp(ptdim, dims, inds1);
      Expression min  = new SelectOp(sub1, ffirst);
      Expression sub  = new SubtractionOp(size_t_type, min, one);

      subs.add(new AdditionOp(size_t_type, ind, sub));
    }

    BlockStmt    bs   = new BlockStmt();
    VariableDecl ad   = genTemp(PointerType.create(pet));
    Expression   add  = new IdValueOp(ad);
    Expression   padd = new SelectOp(genDeclAddress(dest), fptr);

    addSymbol(ad);
    ad.setReferenced();
    bs.addStmt(new EvalStmt(new AssignSimpleOp(genDeclAddress(ad), padd)));

    SubscriptAddressOp sub  = new SubscriptAddressOp(pet, add, subs);
    Statement          body = new EvalStmt(new AssignSimpleOp(sub, expr));
    sub.setFortranArray();
    bs.addStmt(buildDoLoops((FixedArrayType) arrayExprType, body, arrayExprSubscripts));
    expr = new StatementOp(pet, add, bs);
    return expr;
  }

  private Expression copyToAllocArrayFromAlloc(VariableDecl   dest,
                                               Expression     expr,
                                               AllocArrayType aat)
  {
    assert (arrayExprType instanceof AllocArrayType);

    Type   et   = aat.getElementType();
    Type   pet  = PointerType.create(et);
    int    rank = aat.getRank();

    RecordType rty    = (RecordType) aat.getStruct().getCoreType();
    RecordType tdim   = dimType.getCoreType().returnRecordType();
    Type       ptdim  = PointerType.create(dimType);
    FieldDecl  fsize  = tdim.findField("size");
    FieldDecl  ffirst = tdim.findField("first");
    FieldDecl  fptr   = rty.findField("ptr");
    FieldDecl  fdims  = rty.findField("dims");
    Type       tfdims = fdims.getType();

    Vector<Expression> subs = new Vector<Expression>();
    for (int i = 0 ; i < rank; i++) {
      Vector<Expression> inds1 = new Vector<Expression>(1);

      inds1.add(LiteralMap.put(i, int_type));

      Expression ind  = new IdValueOp(arrayExprSubscripts.get(i));
      Expression dims = new SelectIndirectOp(genDeclAddress(dest), fdims);
      Expression sub1 = new SubscriptAddressOp(ptdim, dims, inds1);
      Expression min  = new SelectOp(sub1, ffirst);
      Expression sub  = new SubtractionOp(size_t_type, min, one);

      subs.add(new AdditionOp(size_t_type, ind, sub));
    }

    BlockStmt    bs   = new BlockStmt();
    VariableDecl ad   = genTemp(PointerType.create(pet));
    Expression   add  = new IdValueOp(ad);
    Expression   padd = new SelectOp(genDeclAddress(dest), fptr);

    addSymbol(ad);
    ad.setReferenced();
    bs.addStmt(new EvalStmt(new AssignSimpleOp(genDeclAddress(ad), padd)));

    SubscriptAddressOp sub  = new SubscriptAddressOp(pet, add, subs);
    Statement          body = new EvalStmt(new AssignSimpleOp(sub, expr));
    sub.setFortranArray();
    for (int i = rank - 1; i >= 0; i--) {
      Vector<Expression> inds1 = new Vector<Expression>(1);

      inds1.add(LiteralMap.put(i, int_type));

      Expression dims  = new SelectIndirectOp(genDeclAddress(dest), fdims);
      Expression sub1  = new SubscriptAddressOp(ptdim, dims, inds1);
      Expression sz    = new SelectOp(sub1, fsize);
      Expression index = new IdValueOp(arrayExprSubscripts.get(i));
      body = new DoLoopStmt(index, body, one, sz, one);
    }

    bs.addStmt(body);
    expr = new StatementOp(pet, add, bs);

    return expr;
  }

  private Statement buildDoLoops(FixedArrayType at, Statement body, Vector<VariableDecl> subscripts)
  {
    int rank = at.getRank();
    for (int i = rank - 1; i >= 0; i--) {
      Bound      bd   = at.getIndex(i);
      Expression last = bd.getMax();
      Expression min  = bd.getMin();
      if (!min.getConstantValue().isOne()) {
        Expression sub = new SubtractionOp(size_t_type, last, min);
        last = new AdditionOp(size_t_type, sub, one);
      }
      Expression index = new IdValueOp(subscripts.get(i));
      body = new DoLoopStmt(index, body, one, last, one);
    }
    return body;
  }

  /**
   * Get the arguments to a procedure call.  This routine assumes the
   * opening left-paren has been eaten and it eats the closing
   * right-paren.
   */
  private Vector<Expression> nextArgumentList() throws InvalidException
  {
    assert traceIn("nextArgumentList", null);

    Vector<Expression> args = null;
    try {
      args = new Vector<Expression>();
      if (!nextNBCharIs(')')) {
        while (true) {
          args.add(nextArrayExpr());
          if (nextNBCharIs(')'))
            break;
          nextNBCharMustBe(',');
        }
      }

      return args;
    } finally {
      assert traceOut("nextArgumentList", args);
    }
  }

  /**
   * Get the subscripts to an array reference.  This routine assumes
   * the opening left-paren has been eaten and it eats the closing
   * right-paren.
   */
  private Vector<Expression> nextSubscriptList() throws InvalidException
  {
    assert traceIn("nextSubscriptList", null);

    Vector<Expression> args = null;
    try {
      args = new Vector<Expression>();
      if (!nextNBCharIs(')')) {
        while (true) {
          Expression index = nextExpression(SCALAR_EXPR);
          if (nextNBCharIs(':')) {
            Expression second = nextExpression(SCALAR_EXPR);
            index = new SeriesOp(int_type, index, second);
          }
          args.add(index);
          if (nextNBCharIs(')'))
            break;
          nextNBCharMustBe(',');
        }
      }

      return args;
    } finally {
      assert traceOut("nextSubscriptList", args);
    }
  }

  /**
   * Determine the type from the array and the subscripts.
   */
  private Type getArraySubtype(ArrayType at, Vector<Expression> args) throws InvalidException
  {
    int l    = args.size();
    int rank = at.getRank();

    if (l > rank)
      userError(Msg.MSG_Invalid_dimension, "");

    boolean slice = false;
    for (int i = 0; i < l; i++) {
      Expression sub = args.get(i);
      if (sub instanceof SeriesOp) {
        slice = true;
        break;
      }
    }

    if (!slice)
      return at.getArraySubtype(l);

    Vector<Bound> indecies = new Vector<Bound>(rank);
    int i;
    for (i = 0; i < l; i++) {
      Expression sub = args.get(i);
      if (sub instanceof SeriesOp) {
        SeriesOp so = (SeriesOp) sub;
        Bound    bd = Bound.create(so.getExpr1(), so.getExpr2());
        indecies.add(bd);
      } else
        indecies.add(Bound.create(one, one));
    }
    for (; i < rank; i++)
      indecies.add(Bound.create(one, one));

    return FixedArrayType.create(indecies, at.getElementType());
  }

  /**
   * If the function returns a CHARACTER value, add the two hidden
   * arguments for the return value.  The first hidden arguement has
   * the FUNCTION's name and is the address of the area at which the
   * result is to be stored.  The second hidden argument is the size
   * of the area.
  */
  private ProcedureType createFCharRet(String ftnName, Vector<FormalDecl> formals, Type returnType, boolean defArgs)
  {
    if (!returnType.isFortranCharType())
      return ProcedureType.create(returnType, formals, null);

    FormalDecl fd  = createFormalDecl(ftnName, PointerType.create(returnType), ParameterMode.REFERENCE, 0);
    FormalDecl fdl = createFormalDecl(ftnName + "len", int_type, ParameterMode.VALUE, 0);
    formals.insertElementAt(fdl, 0);
    formals.insertElementAt(fd, 0);
    fctMap.put(fd, fdl);
    if (defArgs) {
      addSymbol(fd);
      addSymbol(fdl);
    }

    returnType = void_type;

    ProcedureType pt = ProcedureType.create(returnType, formals, null);
    pt.markAsFChar();
    return pt;
  }

  /**
   * Create a procedure type from its arguments.
   */
  private ProcedureType createTypeFromArgs(String ftnName, Type returnType, Vector<Expression> args, String f)
  {
    int                l       = args.size();
    Vector<FormalDecl> formals = new Vector<FormalDecl>(l);
    for (int i = 0; i < l; i++) {
      Expression arg  = args.get(i);
      Type       type = arg.getType();
      Type       ty   = type.getCoreType();
      ArrayType  at   = ty.returnArrayType();
      if (at != null)
        type = PointerType.create(at.getElementType());

      FormalDecl fd  = createFormalDecl(f + i, type, ParameterMode.REFERENCE, i);
      formals.add(fd);
    }

    return createFCharRet(ftnName, formals, returnType, false);
  }

  /**
   * Define a new routine and return a call expression for it.
   */
  private Expression buildNewFtnCall(String name, Type returnType, Vector<Expression> args) throws InvalidException
  {
    Declaration decl = lookupDecl(name);
    if (decl == null) {
      ProcedureType pt = createTypeFromArgs(name, returnType, args, "A");
      ProcedureDecl rd = new ProcedureDecl(name, pt);
      cg.addRootSymbol(rd);
      cg.recordRoutine(rd);
      rd.setReferenced();
      rd.setVisibility(Visibility.EXTERN);
      rd.setSourceLineNumber(lineNumber);
      return genCall(rd.getSignature(), genDeclAddress(rd), args, lineNumber, column);
    }

    if (decl.isRoutineDecl()) {
      ProcedureDecl rd = (ProcedureDecl) decl;
      return genCall(rd.getSignature(), genDeclAddress(rd), args, lineNumber, column);
    }

    VariableDecl vd = decl.returnVariableDecl();
    if ((vd == null) || (vd.getValue() != null) || vd.getType().isArrayType()) {
      userError(Msg.MSG_Invalid_function_call, name);
      return null;
    }

    ProcedureType pt = createTypeFromArgs(name, returnType, args, "A");
    ProcedureDecl rd = new ProcedureDecl(name, pt);
    cg.addRootSymbol(rd);
    cg.recordRoutine(rd);
    cg.getSymbolTable().replaceSymbol(vd, rd);
    rd.setReferenced();
    rd.setVisibility(Visibility.EXTERN);
    rd.setSourceLineNumber(lineNumber);
    return genCall(rd.getSignature(), genDeclAddress(rd), args, lineNumber, column);
  }

  /**
   * Must be called with <code>statement[column]</code> containing a
   * delimiter.
   */
  private String nextString()
  {
    assert traceIn("nextString", null);
    String str = "";
    try {
      char delimiter = statement[column];
      int  start     = column + 1;
      int  k         = start;

      while (true) {
        char c = statement[k];
        if (c == delimiter)
          break;

        if (c == 0) {
          str = null;
          return null;
        }
        k++;
      }

      str = str + new String(statement, start, k - start);

      k++;

      while (statement[k] == delimiter) {
        start = k++;
        while (true) {
          char c = statement[k];
          if (c == delimiter)
            break;
          if (c == 0) {
            str = null;
            return null;
          }
          k++;
        }

        str = str + new String(statement, start, k - start);
        k++;
      }

      column = k;

      return str;
    } finally {
      assert traceOut("nextString", str);
    }
  }

  /**
   * Convert an l-value expression to it's r-value form.
   */
  private Expression makeRValue(Expression exp)
  {
    if (exp instanceof IdAddressOp) {
      Type type = exp.getCoreType().getPointedTo();
      if (type.isAtomicType())
        return new IdValueOp(((IdAddressOp) exp).getDecl());
      return new DereferenceOp(exp);
    }

    if (exp instanceof SubscriptAddressOp)
      return ((SubscriptAddressOp) exp).makeRValue();

    if (exp instanceof SelectIndirectOp) {
      SelectIndirectOp sel = (SelectIndirectOp) exp;
      return new SelectOp(sel.getStruct(), sel.getField());
    }

    return exp;
  }

  /**
   * Turn a multi-argument max() to a series of max().
   * max(a,b,c,d) => max(max(max(a,b),c),d)
   */
  private Expression genMax(int exprType, Type rtype, Type atype, Vector<Expression> args) throws InvalidException
  {
    Expression res = args.get(0);
    int        l   = args.size();
    for (int i = 1; i < l; i++) {
      Expression arg = args.get(i);
      res = cast(atype, res);
      arg = cast(atype, makeRValue(arg));
      res = buildScalarDyadicOp(exprType, MAX_OP, res, arg);
    }
    return cast(rtype, res);
  }

  /**
   * Turn a multi-argument min() to a series of min().
   * min(a,b,c,d) => min(min(min(a,b),c),d)
   */
  private Expression genMin(int exprType, Type rtype, Type atype, Vector<Expression> args) throws InvalidException
  {
    Expression res = args.get(0);
    int        l   = args.size();
    for (int i = 1; i < l; i++) {
      Expression arg = args.get(i);
      res = cast(atype, res);
      arg = cast(atype, makeRValue(arg));
      res = buildScalarDyadicOp(exprType, MIN_OP, res, arg);
    }
    return cast(rtype, res);
  }

  /**
   *
   */
  private Literal genSelIntKind(Vector<Expression> args) throws InvalidException
  {
    if (!allowF90Features)
      return null;

    if (args.size() != 1)
      userError(Msg.MSG_Improper_call_to_s, "select_int_kind");

    Literal lit = (args.get(0)).getConstantValue();
    if ((lit == Lattice.Top) || (lit == Lattice.Bot))
      userError(Msg.MSG_Improper_call_to_s, "select_int_kind");

    if (!(lit instanceof IntLiteral))
      userError(Msg.MSG_Improper_call_to_s, "select_int_kind");

    long kind = ((IntLiteral) lit).getLongValue();
    if (kind > 9)
      kind = 4;
    else if (kind > 5)
      kind = 3;
    else if (kind > 2)
      kind = 2;
    else if (kind > 0)
      kind = 1;
    else
      userError(Msg.MSG_Improper_call_to_s, "select_int_kind");

    return LiteralMap.put(kind, int_type);
  }

  private Expression genBTEST(Expression arg1, Expression arg2) throws InvalidException
  {
    Type type = arg1.getCoreType();
    if (!type.isIntegerType())
      userError(Msg.MSG_Not_an_integer_value, "BTEST");
    if (!arg2.getCoreType().isIntegerType())
      userError(Msg.MSG_Not_an_integer_value, "BTEST");

    Expression sub  = new SubtractionOp(type, arg2, one);
    Expression shft = new BitShiftOp(type, one, sub, ShiftMode.Left);
    Expression and  = new BitAndOp(type, arg1, shft);
    return new NotEqualOp(type, zero, and);
  }

  private Expression genISHFT(Expression arg1,
                              Expression arg2,
                              boolean    rotate) throws InvalidException
  {
    Type type = arg1.getCoreType();
    if (!type.isIntegerType())
      userError(Msg.MSG_Not_an_integer_value, "BTEST");
    if (!arg2.getCoreType().isIntegerType())
      userError(Msg.MSG_Not_an_integer_value, "BTEST");

    ShiftMode lop  = rotate ? ShiftMode.LeftRotate  : ShiftMode.Left;
    ShiftMode rop  = rotate ? ShiftMode.RightRotate : ShiftMode.UnsignedRight;
    Literal   shft = arg2.getConstantValue();

    if (shft instanceof IntLiteral) {
      long      shift = ((IntLiteral) shft).getLongValue();
      ShiftMode op    = lop;

      if (shift == 0)
        return arg1;

      if (shift < 0) {
        shft = LiteralMap.put(-shift, shft.getType());
        op = rop;
      }

      return new BitShiftOp(type, arg1, shft, op);
    }

    Expression test = new LessOp(logical_type, arg2, zero);
    Expression sl   = new BitShiftOp(type, arg1, shft, rop);
    Expression sr   = new BitShiftOp(type, arg1, shft, lop);
    return new ExpressionIfOp(type, test, sl, sr);
  }

  /**
   *
   */
  private Literal genSelRealKind(Vector<Expression> args) throws InvalidException
  {
    if (!allowF90Features)
      return null;

    if (args.size() != 2)
      userError(Msg.MSG_Improper_call_to_s, "select_int_kind");

    Literal lit1 = (args.get(0)).getConstantValue();
    if ((lit1 == Lattice.Top) || (lit1 == Lattice.Bot))
      userError(Msg.MSG_Improper_call_to_s, "select_int_kind");

    if (!(lit1 instanceof IntLiteral))
      userError(Msg.MSG_Improper_call_to_s, "select_int_kind");

    Literal lit2 = (args.get(1)).getConstantValue();
    if ((lit2 == Lattice.Top) || (lit2 == Lattice.Bot))
      userError(Msg.MSG_Improper_call_to_s, "select_int_kind");

    if (!(lit2 instanceof IntLiteral))
      userError(Msg.MSG_Improper_call_to_s, "select_int_kind");

    long kind = ((IntLiteral) lit1).getLongValue();
    if (kind > 16)
      kind = 3;
    else if (kind > 7)
      kind = 2;
    else if (kind > 0)
      kind = 1;
    else
      userError(Msg.MSG_Improper_call_to_s, "select_int_kind");

    return LiteralMap.put(kind, int_type);
  }

  private Expression genCMPLX(Expression  arg1,
                              Expression  arg2,
                              ComplexType rt) throws InvalidException
  {
    arg1 = makeRValue(arg1);
    arg2 = makeRValue(arg2);

    Type t1 = arg1.getCoreType();

    if (arg2 == null) {
      if (t1 ==  double_complex_type)
        return arg1;
      else if (t1 == float_complex_type) {
        if (rt == double_complex_type) {
          Expression a1 = new TypeConversionOp(real_type, arg1, CastMode.REAL);
          Expression a2 = new TypeConversionOp(real_type, arg1, CastMode.IMAGINARY);
          return new ComplexOp(rt, cast(double_type, a1), cast(double_type, a2));
        } else
          return arg1;
      } else
        arg2 = dzero;
    }

    Type at = real_type;
    if ((t1 == double_type) || (rt == double_complex_type)) {
      rt = double_complex_type;
      at = double_type;
    }
    return new ComplexOp(rt, cast(at, arg1), cast(at, arg2));
  }

  private Expression buildIndexCall(Expression arg1, Expression arg2) throws InvalidException
  {
    Vector<Expression> args = new Vector<Expression>(4);
    args.add(makeLValue(arg1));
    args.add(makeLValue(arg2));
    args.add(getStringLength(arg1));
    args.add(getStringLength(arg2));
    ProcedureDecl indexpd = defPreKnownFtn("_scale_sindex", "iCCii", RoutineDecl.PURE);
    return genCall(indexpd.getSignature(), genDeclAddress(indexpd), args, lineNumber, column);
  }

  private Expression nextIntrinsicCall(String name, int exprType) throws InvalidException
  {
    assert traceIn("nextIntrinsicCall", null);
    Expression call = null;
    try {
      int ftn = Intrinsics.lookup(name, name.length() - 1); // Drop the '_'.
      if (ftn == Intrinsics.NA)
        return null;

      Object o = nameMap.get(name);
      if ((o != null) && !(o instanceof Declaration))
        return null;

      Declaration decl = (Declaration) o;
      if ((decl != null) && decl.isRoutineDecl())
        return null; // See EXTERNAL statement.

      int    sc = column;
      Vector<Expression> args = nextArgumentList();
      int    l    = args.size();
      if ((l < Intrinsics.minArgs[ftn]) || (l > Intrinsics.maxArgs[ftn])) {
        column = sc;
        return null;
      }

      if (fatalError)
        return null;

      Expression arg1 = null;
      Expression arg2 = null;
      Type       t1   = null;
      Type       t2   = null;

      if (l > 0) {
        arg1 = args.get(0);
        t1 = arg1.getType();
        if (l > 1) {
          arg2 = args.get(1);
          t2 = arg2.getType();
        }
      }

      String signature = null;

      switch (ftn) {

      // Dyadic

      case Intrinsics.BTEST:        call = genBTEST(arg1, arg2); break;
      case Intrinsics.AMAX0:        call = genMax(exprType, real_type, int_type, args); break;
      case Intrinsics.AMAX1:        call = genMax(exprType, real_type, real_type, args); break;
      case Intrinsics.AMIN0:        call = genMin(exprType, int_type, real_type, args); break;
      case Intrinsics.AMIN1:        call = genMin(exprType, real_type, real_type, args); break;
      case Intrinsics.AMOD:
        arg1 = buildScalarMonadicOp(exprType, NOP_OP, real_type, makeRValue(arg1));
        arg2 = buildScalarMonadicOp(exprType, NOP_OP, real_type, makeRValue(arg2));
        call = buildScalarDyadicOp(exprType, REM_OP, arg1, arg2);
        break;
      case Intrinsics.CMPLX:        call = genCMPLX(arg1, arg2, float_complex_type); break;
      case Intrinsics.DCMPLX:       call = genCMPLX(arg1, arg2, double_complex_type); break;
      case Intrinsics.ATAN2:
        call = buildScalarDyadicOp(exprType, ATAN2_OP, arg1, arg2);
        break;
      case Intrinsics.DATAN2:
        arg1 = buildScalarMonadicOp(exprType, NOP_OP, double_type, makeRValue(arg1));
        arg2 = buildScalarMonadicOp(exprType, NOP_OP, double_type, makeRValue(arg2));
        call = buildScalarDyadicOp(exprType, ATAN2_OP, arg1, arg2);
        break;
      case Intrinsics.DDIM:
        arg1 = buildScalarMonadicOp(exprType, NOP_OP, double_type, makeRValue(arg1));
        arg2 = buildScalarMonadicOp(exprType, NOP_OP, double_type, makeRValue(arg2));
        call = buildScalarDyadicOp(exprType, DIM_OP, arg1, arg2);
        break;
      case Intrinsics.DIM:          call = buildScalarDyadicOp(exprType, DIM_OP, arg1, arg2); break;
      case Intrinsics.DMAX1:        call = genMax(exprType, double_type, double_type, args); break;
      case Intrinsics.DMIN1:        call = genMin(exprType, double_type, double_type, args); break;
      case Intrinsics.DMOD:         call = buildScalarDyadicOp(exprType, REM_OP, arg1, arg2); break;
      case Intrinsics.DPROD:
        arg1 = buildScalarMonadicOp(exprType, NOP_OP, double_type, makeRValue(arg1));
        arg2 = buildScalarMonadicOp(exprType, NOP_OP, double_type, makeRValue(arg2));
        call = buildScalarDyadicOp(exprType, MUL_OP, arg1, arg2);
        break;
      case Intrinsics.DSIGN:
        arg1 = buildScalarMonadicOp(exprType, NOP_OP, double_type, makeRValue(arg1));
        arg2 = buildScalarMonadicOp(exprType, NOP_OP, double_type, makeRValue(arg2));
        call = buildScalarDyadicOp(exprType, SIGN_OP, arg1, arg2);
        break;
      case Intrinsics.IAND:
        arg1 = buildScalarMonadicOp(exprType, NOP_OP, t1, makeRValue(arg1));
        arg2 = buildScalarMonadicOp(exprType, NOP_OP, t1, makeRValue(arg2));
        call = buildScalarDyadicOp(exprType, AND_OP, arg1, arg2);
        break;
      case Intrinsics.IBCLR:
        arg1 = buildScalarMonadicOp(exprType, NOP_OP, t1, makeRValue(arg1));
        arg2 = buildScalarMonadicOp(exprType, NOP_OP, t1, makeRValue(arg2));
        arg2 = buildScalarDyadicOp(exprType, SFTLFT_OP, one, arg2);
        arg2 = buildScalarMonadicOp(exprType, COMP_OP, t1, arg2);
        call = buildScalarDyadicOp(exprType, AND_OP, arg1, arg2);
        break;
      case Intrinsics.IBSET:
        arg1 = buildScalarMonadicOp(exprType, NOP_OP, t1, makeRValue(arg1));
        arg2 = buildScalarMonadicOp(exprType, NOP_OP, t1, makeRValue(arg2));
        arg2 = buildScalarDyadicOp(exprType, SFTLFT_OP, one, arg2);
        call = buildScalarDyadicOp(exprType, OR_OP, arg1, arg2);
        call = new BitOrOp(t1, arg1, arg2);
        break;
      case Intrinsics.IDIM:
        call = buildScalarDyadicOp(exprType, DIM_OP, arg1, arg2);
        break;
      case Intrinsics.IEOR:
        arg1 = buildScalarMonadicOp(exprType, NOP_OP, t1, makeRValue(arg1));
        arg2 = buildScalarMonadicOp(exprType, NOP_OP, t1, makeRValue(arg2));
        call = buildScalarDyadicOp(exprType, XOR_OP, arg1, arg2);
        break;
      case Intrinsics.INDEX:
        call = buildIndexCall(arg1, arg2);
        break;
      case Intrinsics.IOR:
        arg1 = buildScalarMonadicOp(exprType, NOP_OP, t1, makeRValue(arg1));
        arg2 = buildScalarMonadicOp(exprType, NOP_OP, t1, makeRValue(arg2));
        call = buildScalarDyadicOp(exprType, OR_OP, arg1, arg2);
        break;
      case Intrinsics.ISHFT:    call = genISHFT(arg1, arg2, false); break;
      case Intrinsics.ISHFTC:   call = genISHFT(arg1, arg2, true);  break;
      case Intrinsics.ISIGN:    call = buildScalarDyadicOp(exprType, SIGN_OP, arg1, arg2); break;
      case Intrinsics.MAX:      call = genMax(exprType, t1, t1, args); break;
      case Intrinsics.MAX0:     call = genMax(exprType, int_type, int_type, args); break;
      case Intrinsics.MAX1:     call = genMax(exprType, int_type, real_type, args); break;
      case Intrinsics.MIN:      call = genMin(exprType, t1, t1, args); break;
      case Intrinsics.MIN0:     call = genMin(exprType, int_type, int_type, args); break;
      case Intrinsics.MIN1:     call = genMin(exprType, int_type, real_type, args); break;
      case Intrinsics.MOD:      call = buildScalarDyadicOp(exprType, REM_OP, arg1, arg2); break;
      case Intrinsics.SIGN:     call = buildScalarDyadicOp(exprType, SIGN_OP, arg1, arg2); break;

      // Monadic

      case Intrinsics.ABS:
        ComplexType ct = t1.getCoreType().returnComplexType();
        if (ct != null)
          t1 = ct.getRealType();
        call = buildScalarMonadicOp(exprType, ABS_OP, t1, makeRValue(arg1));
        break;
      case Intrinsics.ACOS:     call = buildScalarMonadicOp(exprType, ACOS_OP,     t1,          arg1); break;
      case Intrinsics.AIMAG:    call = buildScalarMonadicOp(exprType, IMAG_OP,     real_type,   arg1); break;
      case Intrinsics.AINT:     call = buildScalarMonadicOp(exprType, FLOOR_OP,    t1,          arg1); break;
      case Intrinsics.ALOG10:   call = buildScalarMonadicOp(exprType, LOG10_OP,    real_type,   arg1); break;
      case Intrinsics.ALOG:     call = buildScalarMonadicOp(exprType, LOG_OP,      real_type,   arg1); break;
      case Intrinsics.ANINT:    call = buildScalarMonadicOp(exprType, ROUND_OP,    t1,          arg1); break;
      case Intrinsics.ASIN:     call = buildScalarMonadicOp(exprType, ASIN_OP,     t1,          arg1); break;
      case Intrinsics.ATAN:     call = buildScalarMonadicOp(exprType, ATAN_OP,     t1,          arg1); break;
      case Intrinsics.CABS:     call = buildScalarMonadicOp(exprType, ABS_OP,      real_type,   arg1); break;
      case Intrinsics.CCOS:     call = buildScalarMonadicOp(exprType, COS_OP,      t1,          arg1); break;
      case Intrinsics.CDABS:    call = buildScalarMonadicOp(exprType, ABS_OP,      double_type, arg1); break;
      case Intrinsics.CEILING:  call = buildScalarMonadicOp(exprType, CEILING_OP,  float_complex_type, arg1); break;
      case Intrinsics.CEXP:     call = buildScalarMonadicOp(exprType, EXP_OP,      t1,          arg1); break;
      case Intrinsics.CHAR:     call = buildScalarMonadicOp(exprType, TRUNCATE_OP, char_type,   arg1); break;
      case Intrinsics.CLOG:     call = buildScalarMonadicOp(exprType, LOG_OP,      t1,          arg1); break;
      case Intrinsics.CONJG:    call = buildScalarMonadicOp(exprType, CONJG_OP,    t1,          arg1); break;
      case Intrinsics.COS:      call = buildScalarMonadicOp(exprType, COS_OP,      t1,          arg1); break;
      case Intrinsics.COSH:     call = buildScalarMonadicOp(exprType, COSH_OP,     t1,          arg1); break;
      case Intrinsics.CSIN:     call = buildScalarMonadicOp(exprType, SIN_OP,      t1,          arg1); break;
      case Intrinsics.CSQRT:    call = buildScalarMonadicOp(exprType, SQRT_OP,     t1,          arg1); break;
      case Intrinsics.DABS:     call = buildScalarMonadicOp(exprType, ABS_OP,      double_type, arg1); break;
      case Intrinsics.DACOS:    call = buildScalarMonadicOp(exprType, ACOS_OP,     double_type, arg1); break;
      case Intrinsics.DASIN:    call = buildScalarMonadicOp(exprType, ASIN_OP,     double_type, arg1); break;
      case Intrinsics.DATAN:    call = buildScalarMonadicOp(exprType, ATAN_OP,     double_type, arg1); break;
      case Intrinsics.DBLE:     call = buildScalarMonadicOp(exprType, NOP_OP,      double_type, arg1); break;
      case Intrinsics.DCONJG:   call = buildScalarMonadicOp(exprType, CONJG_OP,    double_complex_type, arg1); break;
      case Intrinsics.DCOS:     call = buildScalarMonadicOp(exprType, COS_OP,      double_type, arg1); break;
      case Intrinsics.DCOSH:    call = buildScalarMonadicOp(exprType, COSH_OP,     double_type, arg1); break;
      case Intrinsics.DEXP:     call = buildScalarMonadicOp(exprType, EXP_OP,      double_type, arg1); break;
      case Intrinsics.DFLOAT:   call = buildScalarMonadicOp(exprType, NOP_OP,      double_type, arg1); break;
      case Intrinsics.DIMAG:    call = buildScalarMonadicOp(exprType, IMAG_OP,     double_type, arg1); break;
      case Intrinsics.DINT:     call = buildScalarMonadicOp(exprType, FLOOR_OP,    double_type, arg1); break;
      case Intrinsics.DLOG10:   call = buildScalarMonadicOp(exprType, LOG10_OP,    double_type, arg1); break;
      case Intrinsics.DLOG:     call = buildScalarMonadicOp(exprType, LOG_OP,      double_type, arg1); break;
      case Intrinsics.DNINT:    call = buildScalarMonadicOp(exprType, ROUND_OP,    double_type, arg1); break;
      case Intrinsics.DSIN:     call = buildScalarMonadicOp(exprType, SIN_OP,      double_type, arg1); break;
      case Intrinsics.DSINH:    call = buildScalarMonadicOp(exprType, SINH_OP,     double_type, arg1); break;
      case Intrinsics.DSQRT:    call = buildScalarMonadicOp(exprType, SQRT_OP,     double_type, arg1); break;
      case Intrinsics.DTAN:     call = buildScalarMonadicOp(exprType, TAN_OP,      double_type, arg1); break;
      case Intrinsics.DTANH:    call = buildScalarMonadicOp(exprType, TANH_OP,     double_type, arg1); break;
      case Intrinsics.EXP:      call = buildScalarMonadicOp(exprType, EXP_OP,      t1,          arg1); break;
      case Intrinsics.FLOAT:    call = buildScalarMonadicOp(exprType, NOP_OP,      real_type,   arg1); break;
      case Intrinsics.FLOOR:    call = buildScalarMonadicOp(exprType, FLOOR_OP,    t1,          arg1); break;
      case Intrinsics.IABS:     call = buildScalarMonadicOp(exprType, ABS_OP,      t1,          arg1); break;
      case Intrinsics.ICHAR:    call = buildScalarMonadicOp(exprType, TRUNCATE_OP, int_type,    arg1); break;
      case Intrinsics.IDFIX:    call = buildScalarMonadicOp(exprType, TRUNCATE_OP, int_type,    arg1); break;
      case Intrinsics.IDINT:    call = buildScalarMonadicOp(exprType, TRUNCATE_OP, int_type,    arg1); break;
      case Intrinsics.IDNINT:   call = buildScalarMonadicOp(exprType, ROUND_OP,    int_type,    arg1); break;
      case Intrinsics.IFIX:     call = buildScalarMonadicOp(exprType, TRUNCATE_OP, int_type,    arg1); break;
      case Intrinsics.INT:      call = buildScalarMonadicOp(exprType, TRUNCATE_OP, int_type,    arg1); break;
      case Intrinsics.LOG10:    call = buildScalarMonadicOp(exprType, LOG10_OP,    t1,          arg1); break;
      case Intrinsics.LOG:      call = buildScalarMonadicOp(exprType, LOG_OP,      t1,          arg1); break;
      case Intrinsics.NINT:     call = buildScalarMonadicOp(exprType, ROUND_OP,    int_type,    arg1); break;
      case Intrinsics.REAL:     call = buildScalarMonadicOp(exprType, NOP_OP,      real_type,   arg1); break;
      case Intrinsics.SIN:      call = buildScalarMonadicOp(exprType, SIN_OP,      t1,          arg1); break;
      case Intrinsics.SINH:     call = buildScalarMonadicOp(exprType, SINH_OP,     t1,          arg1); break;
      case Intrinsics.SNGL:     call = buildScalarMonadicOp(exprType, NOP_OP,      real_type,   arg1); break;
      case Intrinsics.SQRT:     call = buildScalarMonadicOp(exprType, SQRT_OP,     t1,          arg1); break;
      case Intrinsics.TAN:      call = buildScalarMonadicOp(exprType, TAN_OP,      t1,          arg1); break;
      case Intrinsics.TANH:     call = buildScalarMonadicOp(exprType, TANH_OP,     t1,          arg1); break;

        // Miscelanneous

      case Intrinsics.ACHAR:        break;
      case Intrinsics.ADJUSTL:      break;
      case Intrinsics.ADJUSTR:      break;
      case Intrinsics.ALL:          break;
      case Intrinsics.ALLOCATED:    break;
      case Intrinsics.ANY:          break;
      case Intrinsics.ASSOCIATED:   break;
      case Intrinsics.BIT_SIZE:     break;
      case Intrinsics.COUNT:        break;
      case Intrinsics.CSHIFT:       break;
      case Intrinsics.DIGITS:       break;
      case Intrinsics.DOT_PRODUCT:  break;
      case Intrinsics.EOSHIFT:      break;
      case Intrinsics.EPSILON:      break;
      case Intrinsics.EXPONENT:     break;
      case Intrinsics.FRACTION:     break;
      case Intrinsics.HUGE:         break;
      case Intrinsics.IACHAR:       break;
      case Intrinsics.IBITS:        break;
      case Intrinsics.KIND:         break;
      case Intrinsics.LBOUND:       break;
      case Intrinsics.LEN:          call = getStringLength(arg1); break;
      case Intrinsics.LEN_TRIM:     break;
      case Intrinsics.LGE:          break;
      case Intrinsics.LGT:          break;
      case Intrinsics.LLE:          break;
      case Intrinsics.LLT:          break;
      case Intrinsics.LOGICAL:      break;
      case Intrinsics.MATMUL:       break;
      case Intrinsics.MAXEXPONENT:  break;
      case Intrinsics.MAXLOC:       break;
      case Intrinsics.MAXVAL:       break;
      case Intrinsics.MERGE:        break;
      case Intrinsics.MINEXPONENT:  break;
      case Intrinsics.MINLOC:       break;
      case Intrinsics.MINVAL:       break;
      case Intrinsics.MODULO:       break;
      case Intrinsics.NEAREST:      break;
      case Intrinsics.NOT:          break;
      case Intrinsics.NULL:         break;
      case Intrinsics.PACK:         break;
      case Intrinsics.PRECISION:    break;
      case Intrinsics.PRESENT:      break;
      case Intrinsics.PRODUCT:      break;
      case Intrinsics.RADIX:        break;
      case Intrinsics.RANGE:        break;
      case Intrinsics.REPEAT:       break;
      case Intrinsics.RESHAPE:      break;
      case Intrinsics.RRSPACING:    break;
      case Intrinsics.SCALE:        break;
      case Intrinsics.SCAN:         break;
      case Intrinsics.SELECTED_INT_KIND:  call = genSelIntKind(args); break;
      case Intrinsics.SELECTED_REAL_KIND: call = genSelRealKind(args); break;
      case Intrinsics.SET_EXPONENT: break;
      case Intrinsics.SHAPE:        break;
      case Intrinsics.SIZE:         break;
      case Intrinsics.SPACING:      break;
      case Intrinsics.SPREAD:       break;
      case Intrinsics.SUM:          break;
      case Intrinsics.TINY:         break;
      case Intrinsics.TRANSFER:     break;
      case Intrinsics.TRANSPOSE:    break;
      case Intrinsics.UBOUND:       break;
      case Intrinsics.UNPACK:       break;
      case Intrinsics.VERIFY:       break;
      }

      if (exprType == SCALAR_CONST_EXPR) {
        if (call == null)
          return null;

        Literal lit = call.getConstantValue();
        if ((lit == Lattice.Top) || (lit == Lattice.Bot))
          return null;

        return lit;
      }

      if (call == null) {
        String fname = "_scale_" + name.substring(0, name.length() - 1);
        if (signature != null) {
          ProcedureDecl pd = defPreKnownFtn(fname, signature, RoutineDecl.NOTPURE);
          call = genCall(pd.getSignature(), genDeclAddress(pd), args, lineNumber, column);
        } else {
          System.out.println("** bic " + name);
          System.out.println("       " + args);
          call = buildNewFtnCall(fname, determineTypeFromName(name, null), args);
        }
      }

      return call;
    } finally {
      assert traceOut("nextIntrinsicCall", call);
    }
  }

  private ProcedureDecl defIntrinsicFtn(String name) throws InvalidException
  {
    int ftn = Intrinsics.lookup(name, name.length() - 1); // Drop the '_'.
    if (ftn == Intrinsics.NA)
      return null;

    String signature = null;
    String fname     = null;

    switch (ftn) {
    case Intrinsics.ABS:     fname = "r_abs";    signature = "df";    break;
    case Intrinsics.ACOS:    fname = "r_acos";   signature = "df";    break;
    case Intrinsics.AIMAG:   fname = "r_imag";   signature = "fz";    break;
    case Intrinsics.AINT:    fname = "r_int";    signature = "df";    break;
    case Intrinsics.ALOG10:  fname = "r_alog10"; signature = "df";    break;
    case Intrinsics.ALOG:    fname = "r_alog";   signature = "ff";    break;
    case Intrinsics.AMOD:    fname = "r_mod";    signature = "df";    break;
    case Intrinsics.ANINT:   fname = "r_nint";   signature = "df";    break;
    case Intrinsics.ASIN:    fname = "r_sin";    signature = "df";    break;
    case Intrinsics.ATAN2:   fname = "r_atan2";  signature = "dff";   break;
    case Intrinsics.ATAN:    fname = "r_atan";   signature = "df";    break;
    case Intrinsics.CABS:    fname = "c_abs";    signature = "dz";    break;
    case Intrinsics.CCOS:    fname = "c_cos";    signature = "vZZ";   break;
    case Intrinsics.CEXP:    fname = "c_exp";    signature = "vZZ";   break;
    case Intrinsics.CLOG:    fname = "c_log";    signature = "vzz";   break;
    case Intrinsics.CONJG:   fname = "r_cnjg";   signature = "ZZ";    break;
    case Intrinsics.COS:     fname = "r_cos";    signature = "df";    break;
    case Intrinsics.COSH:    fname = "r_cosh";   signature = "df";    break;
    case Intrinsics.CSIN:    fname = "c_sin";    signature = "vZZ";   break;
    case Intrinsics.CSQRT:   fname = "c_sqrt";   signature = "vZZ";   break;
    case Intrinsics.DABS:    fname = "d_abs";    signature = "dd";    break;
    case Intrinsics.DACOS:   fname = "d_acos";   signature = "dd";    break;
    case Intrinsics.DASIN:   fname = "d_asin";   signature = "dd";    break;
    case Intrinsics.DATAN2:  fname = "d_atan2";  signature = "ddd";   break;
    case Intrinsics.DATAN:   fname = "d_atan";   signature = "dd";    break;
    case Intrinsics.DCOS:    fname = "d_cos";    signature = "dd";    break;
    case Intrinsics.DCOSH:   fname = "d_cosh";   signature = "dd";    break;
    case Intrinsics.DDIM:    fname = "d_dim";    signature = "ddd";   break;
    case Intrinsics.DEXP:    fname = "d_exp";    signature = "dd";    break;
    case Intrinsics.DIM:     fname = "r_dim";    signature = "dff";   break;
    case Intrinsics.DIMAG:   fname = "d_imag";   signature = "dx";    break;
    case Intrinsics.DINT:    fname = "d_int";    signature = "dd";    break;
    case Intrinsics.DLOG10:  fname = "d_log10";  signature = "dd";    break;
    case Intrinsics.DLOG:    fname = "d_log";    signature = "ddd";   break;
    case Intrinsics.DMOD:    fname = "d_mod";    signature = "dd";    break;
    case Intrinsics.DNINT:   fname = "d_nint";   signature = "dd";    break;
    case Intrinsics.DPROD:   fname = "d_prod";   signature = "dd";    break;
    case Intrinsics.DSIGN:   fname = "d_sign";   signature = "dd";    break;
    case Intrinsics.DSIN:    fname = "d_sin";    signature = "dd";    break;
    case Intrinsics.DSINH:   fname = "d_sinh";   signature = "dd";    break;
    case Intrinsics.DSQRT:   fname = "d_sqrt";   signature = "dd";    break;
    case Intrinsics.DTAN:    fname = "d_tan";    signature = "dd";    break;
    case Intrinsics.DTANH:   fname = "d_tanh";   signature = "dd";    break;
    case Intrinsics.EXP:     fname = "r_exp";    signature = "df";    break;
    case Intrinsics.IABS:    fname = "i_abs";    signature = "ii";    break;
    case Intrinsics.IDIM:    fname = "i_dim";    signature = "ii";    break;
    case Intrinsics.IDNINT:  fname = "i_dnnt";   signature = "id";    break;
    case Intrinsics.INDEX:   fname = "i_indx";   signature = "iCCii"; break;
    case Intrinsics.ISIGN:   fname = "i_sign";   signature = "iii";   break;
    case Intrinsics.LEN:     fname = "i_len";    signature = "iVi";   break;
    case Intrinsics.MOD:     fname = "i_mod";    signature = "ii";    break;
    case Intrinsics.NINT:    fname = "i_nint";   signature = "if";    break;
    case Intrinsics.SIGN:    fname = "r_sign";   signature = "dff";   break;
    case Intrinsics.SIN:     fname = "r_sin";    signature = "df";    break;
    case Intrinsics.SINH:    fname = "r_sinh";   signature = "df";    break;
    case Intrinsics.SQRT:    fname = "r_sqrt";   signature = "df";    break;
    case Intrinsics.TAN:     fname = "r_tan";    signature = "df";    break;
    case Intrinsics.TANH:    fname = "r_tanh";   signature = "df";    break;
    default: return null;
    }

    return defPreKnownFtn(fname, signature, RoutineDecl.PURE);
  }

  /**
   * Return the l-value form of the expression.
   */
  private Expression makeLValue(Expression expr) throws scale.common.InvalidException
  {
    if (fatalError)
      return errExp;

    if (expr instanceof IdValueOp) {
      if (((IdValueOp) expr).getDecl().isFormalDecl())
        return expr;

      Type type = expr.getType();
      if (type.getPointedToCore().isProcedureType())
        return expr;
      return genDeclAddress(((IdValueOp) expr).getDecl());
    }

    if (expr instanceof SubscriptValueOp) {
      SubscriptValueOp svop = (SubscriptValueOp) expr;
      return svop.makeLValue();
    }

    if (expr instanceof SelectOp) {
      SelectOp sop = (SelectOp) expr;
      return new SelectIndirectOp(sop.getStruct(), sop.getField());
    }

    if (expr instanceof DereferenceOp)
      return ((DereferenceOp) expr).getExpr();

    if (expr instanceof IdAddressOp)
      return expr;

    if (expr instanceof SelectIndirectOp)
      return expr;

    if (expr instanceof SubscriptAddressOp)
      return expr;

    if (expr instanceof SubstringOp) {
      SubstringOp sop = (SubstringOp) expr;
      return new AdditionOp(charp_type,
                            cast(charp_type,
                                 makeLValue(sop.getStr()),
                                 lineNumber,
                                 column),
                            new SubtractionOp(int_type, sop.getFirst(), one));
    }

    if ((expr instanceof CallOp) && expr.getType().isPointerType())
      return expr;

    Type ptype = PointerType.create(expr.getType());
    return new AddressOp(ptype, expr);
  }

  /**
   * Turn a set of routine arguments to the proper form for Fortran.
   */
  private void makeByReference(Vector<Expression> args, ProcedureType pt) throws InvalidException
  {
    if (fatalError)
      return;

    int l = args.size();
    for (int i = 0; i < l; i++) {
      Expression arg = args.get(i);
      if (getStringLength(arg.getType()) >= 0) {
        Expression sl = getStringLength(arg);
        if (sl == null)
          sl = one;
        args.add(sl);
      }
    }

    int k = 0;
    int n = 0;
    if (pt != null) {
      n = pt.numFormals();
      if (pt.isFChar())
        k = 2;
    }

    for (int i = 0; i < l; i++) {
      Expression arg = args.get(i);
      Type       ft  = arg.getType().getCoreType();

      arg = makeLValue(arg);

      AllocArrayType aat = ft.returnAllocArrayType();
      if (aat != null) {
        RecordType rty  = (RecordType) aat.getStruct().getCoreType();
        FieldDecl  fptr = rty.findField("ptr");

        arg = new SelectOp(arg, fptr);
      }

      if (k < n) {
        FormalDecl fd = pt.getFormal(k++);
        arg = cast(fd.getType(), arg, lineNumber, column);
      }
      args.setElementAt(arg, i);
    }
  }

  private Expression nextIDExpr(int exprType) throws InvalidException
  {
    assert traceIn("nextIDExpr", null);
    Expression ref = null;
    try {
      int         sc     = column;
      String      name   = nextIdentifier();
      Declaration decl   = lookupDecl(name);
      boolean     postit = false;

      if (decl == null) {
        // We haven't defined this symbol yet but we may have
        // encountered its type definition already.
        Object o = nameMap.get(name);
        if (o instanceof Literal)
          return (Literal) o;

        decl = (Declaration) o;
        if (decl != null)
          postit = true;
      }

      if (exprType == SCALAR_CONST_EXPR) {
        if (!(decl instanceof FormalDecl) && nextNBCharIs('(')) {
          if ((decl == null) || !decl.getType().isArrayType()) {
            ref = nextIntrinsicCall(name, exprType);
            if (ref != null)
              return ref;
          }
        }

        if (!(decl instanceof VariableDecl)) {
          column = sc;
          return null;
        }

        VariableDecl vd = (VariableDecl) decl;
        if (vd.isConst()) {
          Literal lit = vd.getValue().getConstantValue();
          if ((lit != Lattice.Top) && (lit != Lattice.Bot))
            return lit;
        }

        column = sc;
        return null;
      }

      int sc2 = column;
      if (!(decl instanceof FormalDecl) && nextNBCharIs('(')) {
        if ((decl == null) ||
            (!decl.getType().isArrayType() &&
             !decl.getType().isFortranCharType())) {
          ref = nextIntrinsicCall(name, exprType);
          if (ref != null)
            return ref;
        }
        column = sc2;
      }

      if (decl == null) {
        if (nextNBCharIs('(')) {
          Vector<Expression> args = nextArgumentList();

          if (fatalError)
            return null;

          makeByReference(args, null);
          ref = buildNewFtnCall(name, determineTypeFromName(name, void_type), args);
          return ref;
        }

        // Must be a scalar variable that has not been previously
        // declared.

        decl = new VariableDecl(name, determineTypeFromName(name, null));
        addSymbol(decl);
        decl.setReferenced();
        ref = new IdValueOp(decl);
        return ref;
      }

      VariableDecl vd = decl.returnVariableDecl();
      if (vd != null) {
        Type type = vd.getType();
        assert trace(null, vd);

        if (type == void_type)
          reportError(Msg.MSG_Invalid_or_missing_type, "", filename, lineNumber, column);

        if (nextNBCharIs('(')) {
          if (isFCharType(type)) {
            SeriesOp exp = nextSubstringSpec();
            if (fatalError)
              return null;

            if (exp == null) {
              Vector<Expression> args = nextArgumentList();
              makeByReference(args, null);
              if (vd.isFormalDecl())
                type = type.getCoreType().getPointedTo();
              ref = buildNewFtnCall(vd.getName(), type, args);
              return ref;
            }

            if (postit)
              addSymbol(vd);

            vd.setReferenced();
            ref = new IdValueOp(vd);

            Expression first = exp.getExpr1();
            Expression last  = exp.getExpr2();
            if ((first != one) || !(last instanceof NilOp)) {
              if (last instanceof NilOp)
                last = LiteralMap.put(getStringLength(type), int_type);
              ref = new SubstringOp(ref, first, last);
              return ref;
            }

            return ref;
          }

          Type pet = type;
          if (pet.isPointerType())
            pet = type.getCoreType().getPointedTo();

          ArrayType at = pet.getCoreType().returnArrayType();
          if (at != null) {
            if (postit)
              addSymbol(vd);
            vd.setReferenced();

            Vector<Expression> args = nextSubscriptList();

            if (fatalError)
              return null;

            args.reverse();

            Type ty = getArraySubtype(at, args);
            if (ty == null)
              throw new InvalidException("array reference " + args.size() + " " + at);

            ref = buildArrayElementRef(at, ty, fixVariableRef(vd), args);
            return ref;
          }

          FormalDecl fd = vd.returnFormalDecl();
          if (fd != null) {
            Vector<Expression> args = nextArgumentList();

            if (fatalError)
              return null;

            makeByReference(args, null);

            ProcedureType pt  = createTypeFromArgs(fd.getName(), pet, args, "A");
            Type          ptt = PointerType.create(pt);

            fd.setType(ptt);
            fd.setMode(ParameterMode.VALUE);
            ref = genCall(pt, new IdValueOp(fd), args, lineNumber, column);
            return ref;
          }

          if ((at == null) && (vd.getValue() == null) && !vd.isCommonBaseVariable()) {
            Vector<Expression> args = nextArgumentList();

            if (fatalError)
              return null;

            makeByReference(args, null);
            ref = buildNewFtnCall(vd.getName(), type, args);
            return ref;
          }

          throw new InvalidException("invalid subscripting");
        }

        if (postit)
          addSymbol(vd);

        vd.setReferenced();

        FormalDecl fd = vd.returnFormalDecl();
        if ((fd != null)  && (fd.getMode() == ParameterMode.REFERENCE)) {
          type = type.getCoreType().getPointedTo();
          ref = new IdValueOp(fd);
          if (!type.isArrayType())
            ref = new DereferenceOp(ref);
          return ref;
        }

        if (type.isConst() && type.isAtomicType() && !type.isComplexType()) {
          ref = vd.getValue();
          return ref;
        }

        if (type.isArrayType())
          ref = genDeclAddress(vd);
        else
          ref = new IdValueOp(vd);

        return ref;
      }

      RoutineDecl rd = decl.returnRoutineDecl();
      if (rd != null) {
        if (rd.getSignature().getReturnType() == void_type)
          reportError(Msg.MSG_Invalid_or_missing_type, "", filename, lineNumber, column);

        if (nextNBCharIs('(')) {
          Vector<Expression> args = nextArgumentList();
          if (fatalError)
            return null;

          if (postit) {
            makeByReference(args, null);
            ref = buildNewFtnCall(name, rd.getSignature().getReturnType(), args);
            return ref;
          }
          makeByReference(args, rd.getSignature());
          ref = genCall(rd.getSignature(), genDeclAddress(rd), args, lineNumber, column);
          return ref;
        }

        rd.setReferenced();
        if (postit) {
          cg.addRootSymbol(rd);
          cg.recordRoutine(rd);
        }
        ref = genDeclAddress(rd);
        return ref;
      }

      if (decl instanceof StmtFtnDecl) {
        if (decl.getType() == void_type)
          reportError(Msg.MSG_Invalid_or_missing_type, "", filename, lineNumber, column);

        StmtFtnDecl    ftn  = (StmtFtnDecl) decl;
        Vector<String> args = nextList();
        if (args != null) {
          String ns = ftn.substitute(args);
          if (ns != null) {
            char[] ss = statement;
            statement = ns.toCharArray();
            sc = column;
            try {
              column = 0;
              ref = cast(decl.getType(), nextExpression(exprType));
              column = sc;
              statement = ss;
              return ref;
            } catch (InvalidException ex) {
              column = sc;
              statement = ss;
              throw ex;
            }
          }
        }
      }

      column = sc;
      return null;
    } finally {
      assert traceOut("nextIDExpr", ref);
    }
  }

  private Expression buildArrayElementRef(ArrayType  at,
                                          Type       ty,
                                          Expression array,
                                          Vector<Expression>     subs) throws InvalidException
  {
    if (at.isFixedArrayType()) {
      SubscriptValueOp sop = new SubscriptValueOp(ty, array, subs);
      sop.setFortranArray();
      return sop;
    }

    assert (dimType != null);

    AllocArrayType aat   = at.returnAllocArrayType();
    Type           et    = aat.getElementType();
    RecordType     rty   = (RecordType) aat.getStruct().getCoreType();
    int            rank  = aat.getRank();
    Type           ptst  = PointerType.create(size_t_type);
    Type           ptdim = PointerType.create(dimType);
    RecordType     tdim  = dimType.getCoreType().returnRecordType();
    FieldDecl      fsize = tdim.findField("size");
    int            l     = subs.size();

    if (l != rank)
      userError(Msg.MSG_Invalid_dimension, "");

    FieldDecl    fbase   = rty.findField("base");
    FieldDecl    fdims   = rty.findField("dims");
    Type         tfdims  = fdims.getType();
    VariableDecl dims    = genTemp(ptdim);

    addSymbol(dims);
    dims.setReferenced();

    Expression sel  = new SelectIndirectOp(array, fdims);
    addAssignStmt(genDeclAddress(dims), sel);

    Expression res = subs.get(0);
    for (int i = 1; i < l; i++) {
      Expression index  = subs.get(i);
      Expression offset = LiteralMap.put(i, int_type);
      Vector<Expression>     inds1  = new Vector<Expression>(1);

      inds1.add(offset);

      Expression sub1  = new SubscriptAddressOp(ptdim, new IdValueOp(dims), inds1);
      Expression dsize = new SelectOp(sub1, fsize);
      res = new AdditionOp(size_t_type, index, new MultiplicationOp(size_t_type, res, dsize));
    }

    Vector<Expression> nsubs = new Vector<Expression>(1);
    nsubs.add(res);

    array = new SelectOp(array, fbase);

    return new AdditionOp(size_t_type, new SubscriptValueOp(ty, array, nsubs), one);
  }

  private Expression getArrayDimensionSize(ArrayType at, int dimension, Expression array) throws InvalidException
  {
    if (at.isFixedArrayType()) {
      Bound bd = at.getIndex(dimension);
      try {
        return LiteralMap.put(bd.numberOfElements(), size_t_type);
      } catch (java.lang.Throwable ex) {
      }
      Expression min = cast(size_t_type, bd.getMin());
      Expression max = cast(size_t_type, bd.getMax());
      return new AdditionOp(size_t_type, new SubtractionOp(size_t_type, max, min), one);
    }

    AllocArrayType aat    = at.returnAllocArrayType();
    RecordType     rty    = (RecordType) aat.getStruct().getCoreType();
    RecordType     tdim   = dimType.getCoreType().returnRecordType();
    Type           ptdim  = PointerType.create(dimType);
    FieldDecl      fsize  = tdim.findField("size");
    FieldDecl      fdims  = rty.findField("dims");
    Type           tfdims = fdims.getType();
    Vector<Expression>         inds0  = new Vector<Expression>(1);

    inds0.add(zero);

    Expression dims = new SelectIndirectOp(array, fdims);
    Expression sub0 = new SubscriptAddressOp(ptdim, dims, inds0);

    Vector<Expression> inds1 = new Vector<Expression>(1);
    inds1.add(LiteralMap.put(dimension, int_type));
    Expression sub1 = new SubscriptAddressOp(ptdim, dims, inds1);
    return new SelectOp(sub1, fsize);
  }

  private Expression getArrayDimensionStart(ArrayType at, int dimension, Expression array)
  {
    if (at.isFixedArrayType()) {
      Bound bd = at.getIndex(dimension);
      return bd.getMin();
    }

    AllocArrayType aat    = at.returnAllocArrayType();
    RecordType     rty    = (RecordType) aat.getStruct().getCoreType();
    RecordType     tdim   = dimType.getCoreType().returnRecordType();
    Type           ptdim  = PointerType.create(dimType);
    FieldDecl      ffirst = tdim.findField("first");
    FieldDecl      fdims  = rty.findField("dims");
    Type           tfdims = fdims.getType();
    Vector<Expression>         inds0  = new Vector<Expression>(1);

    inds0.add(zero);

    Expression dims = new SelectIndirectOp(array, fdims);
    Expression sub0 = new SubscriptAddressOp(ptdim, dims, inds0);

    Vector<Expression> inds1 = new Vector<Expression>(1);
    inds1.add(LiteralMap.put(dimension, int_type));
    Expression sub1 = new SubscriptAddressOp(ptdim, dims, inds1);
    return new SelectOp(sub1, ffirst);
  }

  private Expression getArrayDimensionLast(ArrayType at, int dimension, Expression array)
  {
    if (at.isFixedArrayType()) {
      Bound bd = at.getIndex(dimension);
      return bd.getMax();
    }

    AllocArrayType aat    = at.returnAllocArrayType();
    RecordType     rty    = (RecordType) aat.getStruct().getCoreType();
    RecordType     tdim   = dimType.getCoreType().returnRecordType();
    Type           ptdim  = PointerType.create(dimType);
    FieldDecl      flast  = tdim.findField("last");
    FieldDecl      fdims  = rty.findField("dims");
    Type           tfdims = fdims.getType();
    Vector<Expression>         inds0  = new Vector<Expression>(1);

    inds0.add(zero);

    Expression dims = new SelectIndirectOp(array, fdims);
    Expression sub0 = new SubscriptAddressOp(ptdim, dims, inds0);

    Vector<Expression> inds1 = new Vector<Expression>(1);
    inds1.add(LiteralMap.put(dimension, int_type));
    Expression sub1 = new SubscriptAddressOp(ptdim, dims, inds1);
    return new SelectOp(sub1, flast);
  }

  /**
   * Return the type to use for a CHARACTER value of the specified
   * length.
   */
  private Type getFortranCharType(long length) throws InvalidException
  {
    if (((int) length) != length)
      throw new InvalidException("char array size too big");

    Type type = char_type;

    if (length != 1)
      type = FortranCharType.create((int) length);

    return type;
  }

  private Expression nextPrimaryExpr(int exprType) throws InvalidException
  {
    assert traceIn("nextPrimaryExpr", null);
    Expression primary = null;
    try {
      skipBlanks();

      int  sc = column;
      char c1 = statement[column];

      if (c1 == '(') {
        column++;

        primary = nextExpression(exprType);
        if (nextNBCharIs(')'))
          return primary; // Must be (exp).

        if (!nextNBCharIs(',')) {
          column = sc;
          primary = null;
          return null;
        }

        Expression imag = nextExpression(exprType);
        if (imag == null) {
          column = sc;
          return null;
        }

        if (!nextNBCharIs(')')) {
          column = sc;
          primary = null;
          return null;
        }

        // Must be a complex-constant-literal.

        primary = getConstantValue(primary);
        imag    = getConstantValue(imag);

        double r = 0.0;
        double i = 0.0;

        if (primary instanceof FloatLiteral)
          r = ((FloatLiteral) primary).getDoubleValue();
        else if (primary instanceof IntLiteral)
          r = ((IntLiteral) primary).getLongValue();
        else {
          column = sc;
          primary = null;
          return null;
        }

        if (imag instanceof FloatLiteral)
          i = ((FloatLiteral) imag).getDoubleValue();
        else if (imag instanceof IntLiteral)
          i = ((IntLiteral) imag).getLongValue();
        else {
          column = sc;
          primary = null;
          return null;
        }

        ComplexType rtype = float_complex_type;
        Type        at    = real_type;
        if (primary.getCoreType() == double_type) {
          rtype = double_complex_type;
          at = double_type;
        }
        primary = new ComplexLiteral(rtype, r, i);
        return primary;
      }

      if ((c1 == 'b') || (c1 == 'o') || (c1 == 'z') || (c1 == 'x')) {
        column++;
        skipBlanks();
        char c2 = statement[column];
        if ((c2 == '\'') || (c2 == '"')) {
          long value = 0;
          column += 2;

          if (c1 == 'b')
            value = nextBinaryValue();
          else if (c1 == 'o')
            value = nextOctalValue();
          else if ((c1 == 'z') || (c1 == 'x'))
            value = nextHexValue();

          if (!nextCharIs(c2))
            throw new InvalidException("primary expr");

          primary = LiteralMap.put(value, long_type);
          return primary;
        }
        column = sc;
      }

      if (isValidIdFirstChar(c1)) {
        primary = nextIDExpr(exprType);
        if (primary != null) {
          Type type = primary.getType();
          sc = column;
          if (isFCharType(type) && nextNBCharIs('(')) {
            SeriesOp exp = nextSubstringSpec();
            if (exp == null) {
              column = sc;
              return null;
            }
            Expression first = exp.getExpr1();
            Expression last  = exp.getExpr2();
            if ((first != one) || !(last instanceof NilOp)) {
              if (last instanceof NilOp)
                last = LiteralMap.put(getStringLength(type), int_type);
              primary = new SubstringOp(primary, first, last);
            }
          }

          ArrayType at = type.getCoreType().returnArrayType();
          if ((at != null) && (arrayExprType != null))
            primary = convertArrayRef(at, primary);

          return primary;
        }
        column = sc;
      }

      if ((c1 == '\'') || (c1 == '"')) {
        String str = nextString();
        if (str == null)
          throw new InvalidException("string");
        int length = str.length();
        Type type = getFortranCharType(length);
        if (length > 1)
          primary = LiteralMap.put(str, type);
        else
          primary = LiteralMap.put(str.charAt(0), type);
        return primary;
      }

      if (c1 == '/') {
        primary = nextArrayConstant();
        if (primary != null)
          return primary;
      }

      primary = nextNumericConstant();
      if (primary != null)
        return primary;

      if (c1 == '.') {
        column++;
        String b = nextIdentifier();
        if (nextCharIs('.')) {
          if ("true_".equals(b)) {
            primary = lone;
            return primary;
          }
          if ("false_".equals(b)) {
            primary = lzero;
            return primary;
          }
        }
      }

      column = sc;
      return null;
    } finally {
      assert traceOut("nextPrimaryExpr", primary);
    }
  }

  private Expression convertArrayRef(ArrayType at, Expression array) throws InvalidException
  {
    if (arrayExprType == null) {
      arrayExprType = at;
      int rank = at.getRank();
      for (int i = 0; i < rank; i++) {
        VariableDecl iVar = genTemp(int_type);
        addSymbol(iVar);
        iVar.setReferenced();
        arrayExprSubscripts.add(iVar);
      }
    } else
      checkArraysCompatible(at, arrayExprType);

    if (fatalError)
      return null;

    Type      et   = at.getElementType();
    int       rank = at.getRank();
    Vector<Expression>    subs = new Vector<Expression>(rank);
    for (int i = 0; i < rank; i++) {
      Expression sub = new IdValueOp(arrayExprSubscripts.get(i));
      Expression min = at.getIndex(i).getMin();
      if (!min.getConstantValue().isOne()) {
        Type       st     = sub.getType();
        Expression offset = new SubtractionOp(st, at.getIndex(i).getMin(), one);
        sub = new AdditionOp(st, sub, offset);
      }
      subs.add(sub);
    }

    SubscriptValueOp sop =  new SubscriptValueOp(et, makeLValue(array), subs);
    sop.setFortranArray();
    return sop;
  }

  private Expression buildScalarDyadicOp(int        exprType,
                                         int        op,
                                         Expression la,
                                         Expression ra) throws InvalidException
  {
    la = makeRValue(la);
    ra = makeRValue(ra);

    Type lt = la.getPointedToCore();
    Type rt = ra.getPointedToCore();

    if (exprType == NORMAL_EXPR) {
      ArrayType lat = lt.returnArrayType();
      if (lat != null) {
        la = convertArrayRef(lat, la);
        lt = lat.getElementType();
      } 

      ArrayType rat = rt.returnArrayType();
      if (rat != null) {
        ra = convertArrayRef(rat, ra);
        rt = rat.getElementType();
      }
    }

    Type type = lt;
    if (dyadicOpChk[op]) {
      type = calcBinaryType(lt, rt);
      if (type == null)
        return null;

      la = cast(type, la);
      ra = cast(type, ra);
    } else if (lt.isArrayType() || rt.isArrayType())
      return null;

    switch (op) {
    case ADD_OP:    return new AdditionOp(type, la, ra);
    case SUB_OP:    return new SubtractionOp(type, la, ra);
    case MUL_OP:    return new MultiplicationOp(type, la, ra);
    case DIV_OP:    return new DivisionOp(type, la, ra);
    case REM_OP:    return new RemainderOp(type, la, ra);
    case POW_OP:    return new ExponentiationOp(type, la, ra);
    case DIM_OP:    return new Transcendental2Op(type, la, ra, Transcendental2Op.cDim);
    case SIGN_OP:   return new Transcendental2Op(type, la, ra, Transcendental2Op.cSign);
    case ATAN2_OP:  return new Transcendental2Op(type, la, ra, Transcendental2Op.cAtan2);
    case MAX_OP:    return new MaximumOp(type, la, ra);
    case MIN_OP:    return new MinimumOp(type, la, ra);
    case AND_OP:    return new BitAndOp(type, la, ra);
    case OR_OP:     return new BitOrOp(type, la, ra);
    case XOR_OP:    return new BitXorOp(type, la, ra);
    case SFTLFT_OP: return new BitShiftOp(type, la, ra, ShiftMode.Left);
    default:
      throw new InvalidException("mult expr");
    }
  }

  private Expression buildScalarMonadicOp(int        exprType,
                                          int        op,
                                          Type       type,
                                          Expression arg) throws InvalidException
  {
    arg = makeRValue(arg);

    Type lt = arg.getCoreType();

    if (exprType == NORMAL_EXPR) {
      ArrayType at = lt.returnArrayType();
      if (at != null) {
        arg = convertArrayRef(at, arg);
        lt = at.getElementType();
      } 
    }

    if (lt.isArrayType())
      return null;

    ArrayType at = type.getCoreType().returnArrayType();
    if (at != null)
      type = at.getElementType();

    if (convertArgOp[op])
      arg = cast(type, arg);

    switch (op) {
    case NOP_OP:      return arg;
    case ABS_OP:      return new AbsoluteValueOp(type, arg);
    case CONJG_OP:    return new TranscendentalOp(type, arg, TransFtn.Conjg);
    case COS_OP:      return new TranscendentalOp(type, arg, TransFtn.Cos);
    case COSH_OP:     return new TranscendentalOp(type, arg, TransFtn.Cosh);
    case SIN_OP:      return new TranscendentalOp(type, arg, TransFtn.Sin);
    case SINH_OP:     return new TranscendentalOp(type, arg, TransFtn.Sinh);
    case TAN_OP:      return new TranscendentalOp(type, arg, TransFtn.Tan);
    case TANH_OP:     return new TranscendentalOp(type, arg, TransFtn.Tanh);
    case SQRT_OP:     return new TranscendentalOp(type, arg, TransFtn.Sqrt);
    case ACOS_OP:     return new TranscendentalOp(type, arg, TransFtn.Acos);
    case ASIN_OP:     return new TranscendentalOp(type, arg, TransFtn.Asin);
    case ATAN_OP:     return new TranscendentalOp(type, arg, TransFtn.Atan);
    case EXP_OP:      return new TranscendentalOp(type, arg, TransFtn.Exp);
    case LOG10_OP:    return new TranscendentalOp(type, arg, TransFtn.Log10);
    case LOG_OP:      return new TranscendentalOp(type, arg, TransFtn.Log);
    case FLOOR_OP:    return new TypeConversionOp(type, arg, CastMode.FLOOR);
    case CEILING_OP:  return new TypeConversionOp(type, arg, CastMode.CEILING);
    case ROUND_OP:    return new TypeConversionOp(type, arg, CastMode.ROUND);
    case IMAG_OP:     return new TypeConversionOp(type, arg, CastMode.IMAGINARY);
    case TRUNCATE_OP: return new TypeConversionOp(type, arg, CastMode.TRUNCATE);
    case REAL_OP:     return new TypeConversionOp(type, arg, CastMode.REAL);
    case COMP_OP:     return new BitComplementOp(type, arg);
    default:
      throw new InvalidException("mult expr");
    }
  }

  private void checkArraysCompatible(ArrayType lat, ArrayType rat) throws InvalidException
  {
    int lrank = lat.getRank();
    int rrank = rat.getRank();

    if (rrank != lrank)
      userError(Msg.MSG_The_shapes_of_the_array_expressions_do_not_conform, "");

    for (int j = 0; j < lrank; j++) {
      Bound lbd = lat.getIndex(j);
      if (!lbd.isConstantBounds())
        continue;

      Bound rbd = rat.getIndex(j);
      if (!rbd.isConstantBounds())
        continue;
      if (lbd.numberOfElements() != rbd.numberOfElements()) {
        System.out.println("** cn " + j + " " + lat.getIndex(j));
        System.out.println("      " + j + " " + rat.getIndex(j));
        userError(Msg.MSG_The_shapes_of_the_array_expressions_do_not_conform, "");
      }
    }
  }

  private Expression nextLevel1Expr(int exprType) throws InvalidException
  {
    assert traceIn("nextLevel1Expr", null);
    Expression exp = null;
    try {
      int           sc = column;
      ProcedureDecl rd = nextDefinedUnaryOp();

      if (rd == null) {
        column = sc;
        exp = nextPrimaryExpr(exprType);
        return exp;
      }

      Expression expr = nextPrimaryExpr(exprType);
      if (expr == null) {
        column = sc;
        return null;
      }

      Vector<Expression> args = new Vector<Expression>(1);
      args.add(expr);

      exp = genCall(rd.getSignature(), genDeclAddress(rd), args, lineNumber, column);
      return exp;
    } finally {
      assert traceOut("nextLevel1Expr", exp);
    }
  }

  private Expression nextPowerExpr(int exprType) throws InvalidException
  {
    assert traceIn("nextPowerExpr", null);
    Expression expl = null;
    try {

      expl = nextLevel1Expr(exprType);
      if (expl == null)
        return null;

      while (true) {
        skipBlanks();

        int  sc = column;
        if ((statement[column] != '*') || (statement[column + 1] != '*'))
          return expl;

        column += 2;

        Expression expr = nextPowerExpr(exprType);
        if (expr == null) {
          column = sc;
          return expl;
        }

        // We convert int**real to real**real.
        // But we don't convert real**int to real**real.

        Type type = calcBinaryType(expl.getType(), expr.getType());
        expl = buildScalarDyadicOp(exprType, POW_OP, cast(type, expl), expr);
      }
    } finally {
      assert traceOut("nextPowerExpr", expl);
    }
  }

  private Expression nextMultExpr(int exprType) throws InvalidException
  {
    assert traceIn("nextMultExpr", null);
    Expression expl = null;
    try {

      expl = nextPowerExpr(exprType);
      if (expl == null)
        return null;

      while (true) {
        skipBlanks();

        int  sc = column;
        char c1 = statement[column];
        int  op = 0;

        if ((c1 == '*') && (statement[column + 1] != '*')) {
          column++;
          op = MUL_OP;
        } else if ((c1 == '/') && (statement[column + 1] != '/')) {
          column++;
          op = DIV_OP;
        } else
          return expl;

        Expression expr = nextPowerExpr(exprType);
        if (expr == null) {
          column = sc;
          return expl;
        }

        expl = buildScalarDyadicOp(exprType, op, expl, expr);
        if (expl == null)
          return null;
      }
    } finally {
      assert traceOut("nextMultExpr", expl);
    }
  }

  private Expression nextLevel2Expr(int exprType) throws InvalidException
  {
    assert traceIn("nextLevel2Expr", null);
    Expression expl = null;
    try {
      int sc = column;

      if (nextNBCharIs('+')) {
        expl = nextMultExpr(exprType);
        if (expl == null) {
          column = sc;
          return null;
        }
      } else if (nextNBCharIs('-')) {
        expl = nextMultExpr(exprType);
        if (expl == null) {
          column = sc;
          return null;
        }
        expl = new NegativeOp(expl.getType(), expl);
      } else {
        expl = nextMultExpr(exprType);
        if (expl == null)
          return null;
      }

      while (true) {
        skipBlanks();

        sc = column;
        char c1 = statement[column];
        int  op = 0;

        if (c1 == '+') {
          column++;
          op = ADD_OP;
        } else if (c1 == '-') {
          column++;
          op = SUB_OP;
        } else
          return expl;

        Expression expr = nextMultExpr(exprType);
        if (expr == null) {
          column = sc;
          return expl;
        }

        expl = buildScalarDyadicOp(exprType, op, expl, expr);
        if (expl == null)
          return null;
      }
    } finally {
      assert traceOut("nextLevel2Expr", expl);
    }
  }

  private Expression nextLevel3Expr(int exprType) throws InvalidException
  {
    assert traceIn("nextLevel3Expr", null);
    Expression expl = null;
    try {
      int sc = column;

      expl = nextLevel2Expr(exprType);
      if (expl == null)
        return null;

      skipBlanks();

      if ((statement[column] != '/') || (statement[column + 1] != '/'))
        return expl;

      Expression[] v     = new Expression[10];
      int          index = 1;
      v[0] = expl;

      do {
        if ((statement[column] != '/') || (statement[column + 1] != '/'))
          break;

        column += 2;

        Expression expr = nextLevel2Expr(exprType);
        if (expr == null) {
          column = sc;
          expl = null;
          return null;
        }

        v[index++] = expr;
        skipBlanks();
      } while ((statement[column] == '/') && (statement[column + 1] == '/'));

      boolean sym    = false;
      int     cnt    = 0;
      int     length = 0;
      for (int i = 0; i < index; i++) {
        Expression exp  = v[i];
        Type       type = exp.getPointedToCore();

        v[i] = getConstantValue(exp);

        FortranCharType fct = type.returnFortranCharType();
        if (fct != null) {
          int len = fct.getLength();

          if (len <= 0) {
            if (exp instanceof SubstringOp) {
              len = getStringLength(((SubstringOp) exp).getStr().getType());
              sym = true;
            }

            if (len <= 0)
              userError(Msg.MSG_Concatenation_of_unknown_length_not_allowed, "");
          }

          length += len;

          if (exp instanceof StringLiteral)
            cnt++;

          continue;
        }

        if (type == char_type) {
          length++;
          if (exp instanceof IntLiteral)
            cnt++;
          continue;
        }

        column = sc;
        expl = null;
        return null;
      }

      Type type = getFortranCharType(length);
      if (cnt == index) {
        String str = "";
        for (int i = 0; i < index; i++) {
          Expression exp = v[i];
          if (exp instanceof StringLiteral) {
            StringLiteral sl = (StringLiteral) exp;
            str = str + sl.getString();
            continue;
          }

          str = str + (char) (((IntLiteral) exp).getLongValue());
        }
        expl = LiteralMap.put(str, type);
        return expl;
      }

      ProcedureDecl pdp = defPreKnownFtn("_scale_sassignp", "vCCiic", RoutineDecl.PURESGV);
      ProcedureType ptp = pdp.getSignature();
      Expression    pap = genDeclAddress(pdp);

      ProcedureDecl pdc = defPreKnownFtn("_scale_sassignc", "CCci", RoutineDecl.PURESGV);
      ProcedureType ptc = pdc.getSignature();
      Expression    pac = genDeclAddress(pdc);

      VariableDecl  vd  = genTemp(type);
      Expression    va  = cast(charp_type, genDeclAddress(vd), lineNumber, column);
      Expression    sop = null;
      Expression    blk = LiteralMap.put(' ', char_type);

      addSymbol(vd);
      vd.setReferenced();

      if (sym) {
        Expression off = zero;
        Expression len = LiteralMap.put(length, int_type);
        for (int i = 0; i < index; i++) {
          Expression exp  = v[i];
          Expression x    = getStringLength(exp);
          Expression call = null;
          Expression farg = (off == zero) ? va : new AdditionOp(charp_type, va, off);
          if (x == one) {
            Vector<Expression> args = new Vector<Expression>(4);
            args.add(farg);
            args.add(exp);
            args.add(new SubtractionOp(int_type, len, off));
            call = genCall(ptc, pac, args, lineNumber, column);
          } else {
            Vector<Expression> args = new Vector<Expression>(5);
            args.add(farg);
            args.add(makeLValue(exp));
            args.add(new SubtractionOp(int_type, len, off));
            args.add(getStringLength(exp));
            args.add(blk);
            call = genCall(ptp, pap, args, lineNumber, column);
          }
          off = getConstantValue(new AdditionOp(int_type, off, x));
          if (sop == null)
            sop = call;
          else
            sop = new SeriesOp(void_type, sop, call);
        }
      } else {
        int off = 0;
        for (int i = 0; i < index; i++) {
          Expression exp  = v[i];
          int        x    = getStringLength(exp.getType());
          Expression call = null;
          Expression farg = (off == 0) ? va : new AdditionOp(charp_type, va, LiteralMap.put(off, int_type));
          if (x == 1) {
            Vector<Expression> args = new Vector<Expression>(4);
            args.add(farg);
            args.add(exp);
            args.add(LiteralMap.put(length - off, int_type));
            call = genCall(ptc, pac, args, lineNumber, column);
          } else {
            Vector<Expression> args = new Vector<Expression>(5);
            args.add(farg);
            args.add(makeLValue(exp));
            args.add(LiteralMap.put(length - off, int_type));
            args.add(getStringLength(exp));
            args.add(blk);
            call = genCall(ptp, pap, args, lineNumber, column);
          }
          off += x;
          if (sop == null)
            sop = call;
          else
            sop = new SeriesOp(void_type, sop, call);
        }
      }

      expl = new SeriesOp(type, sop, new IdValueOp(vd));
      return expl;
    } finally {
      assert traceOut("nextLevel3Expr", expl);
    }
  }

  private Expression nextLevel4Expr(int exprType) throws InvalidException
  {
    assert traceIn("nextLevel4Expr", null);
    Expression expl = null;
    try {
      int sc = column;
      expl = nextLevel3Expr(exprType);
      if (expl == null)
        return null;

      while (true) {
        sc = column;
        int op = nextDotOp();
        switch (op) {
        case DOT_GT: 
        case DOT_GE: 
        case DOT_LT: 
        case DOT_LE: 
        case DOT_NEQ:
        case DOT_EQ:   break;
        default:
          column = sc;
          return expl;
        }

        Expression expr = nextLevel3Expr(exprType);
        if (expr == null) {
          column = sc;
          return expl;
        }

        Type    lt  = expl.getType();
        Type    rt  = expr.getType();
        Type    ltc = lt.getPointedToCore();
        Type    rtc = rt.getPointedToCore();
        boolean bl  = ltc.isFortranCharType();
        boolean br  = rtc.isFortranCharType();

        if (bl || br) {
          if (ltc == char_type) {
            Expression         lr   = getStringLength(expr);
            ProcedureDecl      pd   = defPreKnownFtn("_scale_ccmps", "icCi", RoutineDecl.PURE);
            Vector<Expression> args = new Vector<Expression>(3);
            args.add(expl);
            args.add(makeLValue(expr));
            args.add(lr);
            expl = genCall(pd.getSignature(), genDeclAddress(pd), args, lineNumber, column);
            expr = zero;
          } else if (rtc == char_type) {
            Expression         ll   = getStringLength(expl);
            ProcedureDecl      pd   = defPreKnownFtn("_scale_scmpc", "iCci", RoutineDecl.PURE);
            Vector<Expression> args = new Vector<Expression>(3);
            args.add(makeLValue(expl));
            args.add(expr);
            args.add(ll);
            expl = genCall(pd.getSignature(), genDeclAddress(pd), args, lineNumber, column);
            expr = zero;
          } else if (bl && br) {
            Expression         ll   = getStringLength(expl);
            Expression         lr   = getStringLength(expr);
            ProcedureDecl      pd   = defPreKnownFtn("_scale_scmp", "iCCii", RoutineDecl.PURE);
            Vector<Expression> args = new Vector<Expression>(4);

            args.add(makeLValue(expl));
            args.add(makeLValue(expr));
            args.add(ll);
            args.add(lr);
            expl = genCall(pd.getSignature(), genDeclAddress(pd), args, lineNumber, column);
            expr = zero;
          } else
            userError(Msg.MSG_Invalid_expression, "");
        } else {
          Type type = calcBinaryType(lt, rt);
          if (type == null) {
            userError(Msg.MSG_Invalid_expression, "");
            expl = null;
            return null;
          }
          expl = cast(type, expl);
          expr = cast(type, expr);
        }

        boolean cl = ltc.isComplexType();
        boolean cr = rtc.isComplexType();
        if (cl || cr) {
          Expression l1r = expl;
          Expression l1i = zero;
          Expression r1r = expr;
          Expression r1i = zero;
          if (cl) {
            Type l1t = (ltc.getCoreType() == float_complex_type) ? real_type : double_type;
            l1r = getConstantValue(new TypeConversionOp(l1t, expl, CastMode.REAL));
            l1i = getConstantValue(new TypeConversionOp(l1t, expl, CastMode.IMAGINARY));
          }
          if (cr) {
            Type r1t = (rtc.getCoreType() == float_complex_type) ? real_type : double_type;
            r1r = getConstantValue(new TypeConversionOp(r1t, expr, CastMode.REAL));
            r1i = getConstantValue(new TypeConversionOp(r1t, expr, CastMode.IMAGINARY));
          }
          switch (op) {
          default: userError(Msg.MSG_Invalid_expression, ""); break;
          case DOT_EQ:  expl = new AndConditionalOp(logical_type,
                                                    new EqualityOp(logical_type,
                                                                   l1r,
                                                                   r1r),
                                                    new EqualityOp(logical_type,
                                                                   l1i,
                                                                   r1i));
            continue;
          case DOT_NEQ: expl = new OrConditionalOp(logical_type,
                                                   new NotEqualOp(logical_type,
                                                                  l1r,
                                                                  r1r),
                                                   new NotEqualOp(logical_type,
                                                                  l1i,
                                                                  r1i));
            continue;
          }
        }

        switch (op) {
        case DOT_GT:  expl = new GreaterOp(logical_type, expl, expr);      break;
        case DOT_GE:  expl = new GreaterEqualOp(logical_type, expl, expr); break;
        case DOT_LT:  expl = new LessOp(logical_type, expl, expr);         break;
        case DOT_LE:  expl = new LessEqualOp(logical_type, expl, expr);    break;
        case DOT_NEQ: expl = new NotEqualOp(logical_type, expl, expr);     break;
        case DOT_EQ:  expl = new EqualityOp(logical_type, expl, expr);     break;
        }
      }
    } finally {
      assert traceOut("nextLevel4Expr", expl);
    }
  }

  private Expression nextAndOperandExpr(int exprType) throws InvalidException
  {
    assert traceIn("nextAndOperandExpr", null);
    Expression expl = null;
    try {
      int sc  = column;
      int dop = nextDotOp();
      if (dop == DOT_NOT) {
        expl = nextLevel4Expr(exprType);
        if (expl == null)
          throw new InvalidException("not not");
        if (expl instanceof EqualityOp) {
          EqualityOp eop = (EqualityOp) expl;
          expl = new NotEqualOp(logical_type, eop.getExpr1(), eop.getExpr2());
        } else if (expl instanceof NotEqualOp) {
          NotEqualOp eop = (NotEqualOp) expl;
          expl = new EqualityOp(logical_type, eop.getExpr1(), eop.getExpr2());
        } else
          expl = new NotOp(logical_type, expl);
        return expl;
      } else {
        column = sc;
        expl = nextLevel4Expr(exprType);
        return expl;
      }
    } finally {
      assert traceOut("nextAndOperandExpr", expl);
    }
  }

  private Expression nextOrOperandExpr(int exprType) throws InvalidException
  {
    assert traceIn("nextOrOperandExpr", null);
    Expression expl = null;
    try {
      int sc = column;
      expl = nextAndOperandExpr(exprType);
      if (expl == null)
        return null;

      while (true) {
        sc = column;
        int op = nextDotOp();
        if (op != DOT_AND) {
          column = sc;
          return expl;
        }

        Expression expr = nextAndOperandExpr(exprType);
        if (expr == null) {
          column = sc;
          return expl;
        }

        if (!expl.hasTrueFalseResult())
          expl = new NotEqualOp(logical_type, expl, expl.getType().isRealType() ? dzero : zero);

        if (!expr.hasTrueFalseResult())
          expr = new NotEqualOp(logical_type, expr, expr.getType().isRealType() ? dzero : zero);

        expl = new AndConditionalOp(logical_type, expl, expr);
      }
    } finally {
      assert traceOut("nextOrOperandExpr", expl);
    }
  }

  private Expression nextEqvOperandExpr(int exprType) throws InvalidException
  {
    assert traceIn("nextEqvOperandExpr", null);
    Expression expl = null;
    try {
      int sc = column;
      expl = nextOrOperandExpr(exprType);
      if (expl == null)
        return null;

      while (true) {
        sc = column;
        int op = nextDotOp();
        if (op != DOT_OR) {
          column = sc;
          return expl;
        }

        Expression expr = nextOrOperandExpr(exprType);
        if (expr == null) {
          column = sc;
          return expl;
        }

        if (!expl.hasTrueFalseResult())
          expl = new NotEqualOp(logical_type, expl, expl.getType().isRealType() ? dzero : zero);

        if (!expr.hasTrueFalseResult())
          expr = new NotEqualOp(logical_type, expr, expr.getType().isRealType() ? dzero : zero);

        expl = new OrConditionalOp(logical_type, expl, expr);
      }
    } finally {
      assert traceOut("nextEqvOperandExpr", expl);
    }
  }

  private Expression nextLevel5Expr(int exprType) throws InvalidException
  {
    assert traceIn("nextLevel5Expr", null);
    Expression expl = null;
    Expression expr = null;
    try {
      int sc = column;
      expl = nextEqvOperandExpr(exprType);
      if (expl == null)
        return null;

      while (true) {
        sc = column;
        int op = nextDotOp();
        switch (op) {
        case DOT_EQV:
          expr = nextEqvOperandExpr(exprType);
          if (expr == null) {
            column = sc;
            return expl;
          }

          expl = new EqualityOp(logical_type, expl, expr);
          break;

        case DOT_NEQV:
          expr = nextEqvOperandExpr(exprType);
          if (expr == null) {
            column = sc;
            return expl;
          }

          expl = new NotEqualOp(logical_type, expl, expr);
          break;

        default:
          column = sc;
          return expl;
        }
      }
    } finally {
      assert traceOut("nextLevel5Expr", expl);
    }
  }

  private Expression nextExpression(int exprType) throws InvalidException
  {
    assert traceIn("nextExpression", null);
    Expression expl = null;
    try {
      expl = nextLevel5Expr(exprType);
      if (expl == null)
        return null;

      if (!allowF90Features && !allowF95Features)
        return expl;

      while (true) {
        if (!nextNBCharIs('.'))
          return expl;

        int           sc = column;
        ProcedureDecl rd = nextDefinedBinaryOp();

        if (rd == null) {
          column = sc;
          return expl;
        }

        if (!nextNBCharIs('.')) {
          column = sc;
          return expl;
        }

        Expression expr = nextLevel5Expr(exprType);
        if (expr == null) {
          column = sc;
          return expl;
        }

        Vector<Expression> args = new Vector<Expression>(2);
        args.add(expl);
        args.add(expr);
        expl = genCall(rd.getSignature(), genDeclAddress(rd), args, lineNumber, column);
      }
    } finally {
      assert traceOut("nextExpression", expl);
    }
  }

  private Literal nextConstantExpression(Type type) throws InvalidException
  {
    assert traceIn("nextConstantExpression", null);
    Literal lit = null;
    Type    st = requiredType;

    requiredType = type;

    try {
      Expression exp = nextExpression(SCALAR_CONST_EXPR);
      if (exp == null)
        return null;

      lit = exp.getConstantValue();
      if (lit == null)
        return null;

      if ((lit == Lattice.Top) || (lit == Lattice.Bot)) {
        System.out.println("** " + lit);
        userError(Msg.MSG_Not_a_constant, null);
      }

      return lit;
    } finally {
      requiredType = st;
      assert traceOut("nextConstantExpression", lit);
    }
  }

  private Literal nextConstantPrimaryExpr() throws InvalidException
  {
    assert traceIn("nextConstantPrimaryExpr", null);
    Literal lit = null;
    try {
      Expression exp = nextPrimaryExpr(SCALAR_CONST_EXPR);
      if (exp == null)
        return null;

      lit = exp.getConstantValue();
      if ((lit == Lattice.Top) || (lit == Lattice.Bot))
        userError(Msg.MSG_Not_a_constant, null);

      return lit;
    } finally {
      assert traceOut("nextConstantPrimaryExpr", lit);
    }
  }

  private IntLiteral nextIntegerConstantExpression() throws InvalidException
  {
    assert traceIn("nextIntegerConstantExpression", null);
    IntLiteral lit = null;
    try {
      int        sc  = column;
      Expression exp = nextExpression(SCALAR_CONST_EXPR);
      if (exp == null)
        return null;
      Literal lit2 = exp.getConstantValue();
      if (!(lit2 instanceof IntLiteral)) {
        column = sc;
        return null;
      }
      lit = (IntLiteral) lit2;
      return lit;
    } finally {
      assert traceOut("nextIntegerConstantExpression", lit);
    }
  }

  private Expression nextScalarExpression() throws InvalidException
  {
    assert traceIn("nextScalarExpression", null);
    Expression exp = null;
    try {
      exp = nextExpression(SCALAR_EXPR);
      if (exp == null)
        return null;
      if (!exp.getCoreType().isAtomicType())
        userError(Msg.MSG_Not_a_scalar_value, "");
      return exp;
    } finally {
      assert traceOut("nextScalarExpression", exp);
    }
  }

  private Expression nextIntegerExpression() throws InvalidException
  {
    assert traceIn("nextIntegerExpression", null);
    Expression exp = null;
    try {
      exp = nextScalarExpression();
      if (exp == null)
        return null;
      if (!exp.getCoreType().isIntegerType())
        userError(Msg.MSG_Not_an_integer_value, "");
      return exp;
    } finally {
      assert traceOut("nextIntegerExpression", exp);
    }
  }

  private Expression nextSpecificationExpression() throws InvalidException
  {
    assert traceIn("nextSpecificationExpression", null);
    Expression exp = null;
    try {

      exp = nextIntegerExpression();
      Literal lit = exp.getConstantValue();
      if ((lit != Lattice.Bot) && (lit != Lattice.Top))
        exp = lit;
      else {
        int l = specList.size();
        for (int i = 0; i < l; i += 2) {
          Expression sp = specList.get(i);
          if (sp.equivalent(exp)) {
            exp = specList.get(i + 1);
            return exp;
          }
        }

        specList.add(exp);
        Type         type = exp.getType();
        VariableDecl vd   = genTemp(type);
        addSymbol(vd);
        vd.setReferenced();
        vd.setValue(exp);
        exp = new IdValueOp(vd);
        specList.add(exp);
      }

      return exp;
    } finally {
      assert traceOut("nextSpecificationExpression", exp);
    }
  }

  private Expression nextCharacterExpression() throws InvalidException
  {
    assert traceIn("nextCharacterExpression", null);
    Expression exp = null;
    try {
      exp = nextArrayExpr();
      if (exp == null)
        return null;

      Type      type = exp.getCoreType();
      ArrayType at   = type.returnArrayType();
      if (at != null)
        type = at.getElementType().getCoreType();
      if (type.isFortranCharType() || (type == char_type))
        return exp;
      userError(Msg.MSG_Not_a_CHARACTER_value, "");
      return null;
    } finally {
      assert traceOut("nextCharacterExpression", exp);
    }
  }

  private Expression nextLogicalExpression() throws InvalidException
  {
    assert traceIn("nextLogicalExpression", null);
    Expression exp = null;
    try {
      exp = nextArrayExpr();
      if (exp == null) {
        exp = zero;
        return exp;
      }

      if (exp.getCoreType() == logical_type)
        return exp;

      if (!exp.hasTrueFalseResult())
        userError(Msg.MSG_Not_a_LOGICAL_value, "");

      return exp;
    } finally {
      assert traceOut("nextLogicalExpression", exp);
    }
  }

  /**
   * Return a vector of parameter declarations.
   * The parameter types may be changed later.
   * @param subroutine true if called for subroutine
   */
  private Vector<FormalDecl> nextFormals(boolean subroutine) throws InvalidException
  {
    assert traceIn("nextFormals", null);
    Vector<FormalDecl> v       = null;
    int    counter = 0;

    try {
      if (!nextNBCharIs('('))
        return null;

      if (nextNBCharIs(')'))
        return null;

      boolean dummies = false;
      v = new Vector<FormalDecl>();
      while (true) {
        if (subroutine && nextNBCharIs('*')) {
          dummies = true;
        } else {
          String name = nextIdentifier();
          if (name == null) {
            userError(Msg.MSG_Invalid_statement, "arg");
            return null;
          }

          // For now, determine the type from the name.  A declaration
          // later on may change this. It is also possible that, for an
          // ENTRY statement, the types of an argument may already
          // have been specified.

          Type        type = null;
          Declaration decl = lookupDecl(name);
          if (decl == null) {
            Object o = nameMap.get(name);
            if ((o != null) && (o instanceof Declaration))
              decl = (Declaration) o;
          }

          if ((decl != null) && decl.isVariableDecl())
            type = decl.getType();
          else
            type = determineTypeFromName(name, void_type);
          if (!type.isArrayType())
            type = PointerType.create(type);

          int l = v.size();
          for (int i = 0; i < l; i++) {
            FormalDecl d = v.get(i);
            if (d.getName().equals(name))
              userError(Msg.MSG_Parameter_s_already_defined, name, lineNumber, column);
          }

          FormalDecl fd = null;
          if (entryFormals != null) {
            int ll = entryFormals.size();
            for (int i = 0; i < ll; i++) {
              FormalDecl d = entryFormals.get(i);
              if (d.getName().equals(name)) {
                fd = d;
                break;
              }
            }
          }

          if (fd == null) {
            fd = createFormalDecl(name, type, ParameterMode.REFERENCE, 0);
            if (entryFormals != null)
              entryFormals.add(fd);
          }

          v.add(fd);
        }

        if (nextNBCharIs(')'))
          break;

        nextCharMustBe(',');
      }

      if (dummies)
        v.insertElementAt(null, 0);
      return v;
    } finally {
      assert traceOut("nextFormals", v);
    }
  }

  /**
   * Return a vector of items separated by the comma character.
   */
  private Vector<String> nextList() throws InvalidException
  {
    assert traceIn("nextList", null);
    Vector<String> v = null;
    try {
      if (!nextNBCharIs('('))
        return null;

      v = new Vector<String>();
      if (nextNBCharIs(')'))
        return v;

      int pcnt = 0;
      while (true) {
        skipBlanks();

        int start = column;
        while (true) {
          char c = statement[column];
          if (c == 0)
            return null;

          if ((c == ',') && (pcnt == 0))
            break;
          else if (c == '(')
            pcnt++;
          else if (c == ')') {
            if (pcnt == 0)
              break;
            pcnt--;
          }
          column++;
        }

        column--;
        while ((column >= start) && (statement[column] == ' '))
          column--;
        column++;

        String arg = new String(statement, start, column - start);
        v.add(arg);

        if (nextNBCharIs(')'))
          return v;

        nextNBCharMustBe(',');
      }
    } finally {
      assert traceOut("nextList", v);
    }
  }

  /**
   * Make sure a local variable is initialized properly. Fortran
   * requires that it be completely initialized.
   */
  private void checkVariable(VariableDecl vd)
  {
    Expression exp = getConstantValue(vd.getValue());
    if (!(exp instanceof Literal))
      return;

    Literal lit  = (Literal) exp;
    Type    type = vd.getCoreType();
    long    noe  = type.numberOfElements();
    long    noa  = lit.getCount();

    if (type.isFortranCharType() && !vd.isEquivalenceDecl())
      vd.setResidency(Residency.MEMORY);

    if (noe == lit.getCount())
      return;

    if (noa > 1)
      return;

    if (lit instanceof IntLiteral) {
      Vector<Object>      v  = new Vector<Object>(3);
      AggregationElements ag = new AggregationElements(type, v);
      vd.setValue(ag);
      v.add(lit);
      v.add(new PositionRepeatOp((int) noe - 1));
      v.add(LiteralMap.put(0, lit.getType()));
      return;
    }

    if (lit instanceof FloatLiteral) {
      Vector<Object>      v  = new Vector<Object>(3);
      AggregationElements ag = new AggregationElements(type, v);
      vd.setValue(ag);
      v.add(lit);
      v.add(new PositionRepeatOp((int) noe - 1));
      v.add(LiteralMap.put(0.0, lit.getType()));
      return;
    }
  }

  /**
   * Create the declaration statements for the specified statement
   * block.  The current symbol table scope is accessed to obtain the
   * set of variables.
   */
  private void createDeclStatments(BlockStmt bs, int lineno, int column)
  {
    SymtabScope              ss = cg.getSymbolTable().getCurrentScope();
    Enumeration<SymtabEntry> es = ss.orderedElements();
    while (es.hasMoreElements()) {
      SymtabEntry se   = es.nextElement();
      Declaration decl = se.getDecl();

      if (decl.isGlobal() && !decl.isEquivalenceDecl())
        continue;

      if (decl instanceof FormalDecl)
        continue;

      if (decl.isRoutineDecl())
        continue;

      DeclStmt ds = new DeclStmt(decl);
      int      i  = bs.addDeclStmt(ds);
      addStmtInfo(ds, lineno, column);

      if (globalSaveFlag && decl.isVariableDecl() && !decl.isEquivalenceDecl() && !decl.isTemporary()) {
        decl.setResidency(Residency.MEMORY);
        decl.setVisibility(Visibility.LOCAL);
        decl.setReferenced();
      }

      VariableDecl vd = decl.returnVariableDecl();
      if (vd != null)
        checkVariable(vd);

      if (classTrace)
        System.out.println("  " + ds);
    }
  }

  /**
   * Finish up a statement block.
   */
  private void finishBlockStmt(BlockStmt bs, boolean ftn, SymtabScope cs, int lineNumber, int column)
  {
    if (ftn && (bs.numStmts() == 0)) {
      Statement stmti = new ReturnStmt(null);
      addStmtInfo(stmti, lineNumber, column);
      bs.addStmt(stmti);
    } else if (bs.numStmts() == 0)
      bs.addStmt(new NullStmt());

    addStmtInfo(bs, lineNumber, column);
    bs.setScope(cs);
  }

  /**
   * Create a procedure type from the specified formal parameters.
   */
  private ProcedureType makeProcedureType(String ftnName, Type returnType, Vector<FormalDecl> formals)
  {
    if (formals == null) {
      formals = new Vector<FormalDecl>(0);
    } else {
      int l = formals.size();

      if (l == 1) {
        FormalDecl fd = formals.get(0);
        if (fd.getCoreType().isVoidType())
          formals.remove(0);
        l = 0;
      }

      for (int i = 0; i < l; i++) {
        FormalDecl fd   = formals.get(i);

        if (getStringLength(fd.getType()) >= 0)
          formals.add(createFormalDecl(fd.getName() + "len", int_type, ParameterMode.VALUE, i));
      }
    }

    return createFCharRet(ftnName, formals, returnType, true);
  }

  /**
   * Create a procedure declaration from the specified formal
   * parameters.
   */
  private ProcedureDecl buildProcedureDecl(String             name,
                                           Type               returnType,
                                           Vector<FormalDecl> formals,
                                           int                lineNumber,
                                           int                column)
  {
    ProcedureType pt   = makeProcedureType(name, returnType, formals);
    Declaration   decl = lookupRootDecl(name);
    ProcedureDecl rd   = null;

    if (decl instanceof ProcedureDecl)
      rd = (ProcedureDecl) decl;

    if (rd != null) {
      ProcedureType opt  = rd.getSignature();
      int           optl = opt.numFormals();
      int           ptl  = pt.numFormals();
      if (optl != pt.numFormals()) {
        int cnt = 0;
        for (int i = opt.isFChar() ? 2 : 0; i < optl; i++) {
          FormalDecl fd = opt.getFormal(i);
          if (getStringLength(fd.getType()) >= 0)
            cnt++;
        }
        if (((optl - cnt) != ptl) && (optl != 0)) {
          if (classTrace)
            System.out.println("** bpd " + optl + " " + cnt + " " + ptl);
          userWarning(Msg.MSG_Function_s_definition_does_not_match_prototype, name, lineNumber, column);
        }
      }
      if (rd.getBody() == null)
        rd.setType(pt); // Use the current instances of the parameters.
    } else {
      rd = new ProcedureDecl(name, pt);
      cg.addRootSymbol(rd);
      cg.recordRoutine(rd);
    }

    rd.setVisibility(Visibility.GLOBAL);

    int l = pt.numFormals();
    for (int i = 0; i < l; i++) {
      FormalDecl fd = pt.getFormal(i);
      fd.setReferenced();
      addSymbol(fd);
    }

    rd.setSourceLineNumber(lineNumber);

    return rd;
  }

  /**
   * Compute the COMMON/EQUIVALENCE offsets and initializations.
   */
  private void doCommonEquivalence()
  {
    Machine machine = Machine.currentMachine;

    // Compute the offsets for common variables.

    Enumeration<VariableDecl> eb = commonVars.keys();
    while (eb.hasMoreElements()) {
      VariableDecl             base   = eb.nextElement();
      Vector<EquivalenceDecl>  vars   = commonVars.get(base);
      int          l      = vars.size();
      long         offset = 0;

      boolean trace2 = false && "scr_".equals(base.getName());
      if (trace2)
        System.out.println("** com0 " + base.getName());

      for (int i = 0; i < l; i++) {
        EquivalenceDecl vd  = vars.get(i);
        long            off = vd.getBaseOffset();

        if (off >= 0) { // Equivalenced variable.
          vd.setBaseOffset(offset + off);
          long ns = offset + vd.getCoreType().memorySize(machine) - off;
          if (ns > offset)
            offset = ns;
        } else {
          long off2 = Machine.alignTo(offset, vd.getType().alignment(machine));
          vd.setBaseOffset(off2);
          offset = off2 + vd.getCoreType().memorySize(machine);
        }

        if (trace2)
          System.out.println("        " + vd.getName() + " " +  vd.getBaseOffset());
      }
    }

    // Turn the variables in the equivalence set into EquivalenceDecl
    // instances.

    Symtab st   = cg.getSymbolTable();
    Type   saut = machine.getSmallestAddressableUnitType();

    for (EquivSet cur = currentEquivSet; cur != null; cur = cur.getPrevious()) {
      int n = cur.numInSet();
      if (n <= 0)
        continue;

      // Make all the variables in the equivalenced set relative to
      // the variable with the largest offset to the common point.

      int          lind = cur.getIndexLargestOffset();
      VariableDecl base = cur.getBaseVariable();
      long         off  = cur.getOffset(lind);

      if (base == null) {
        // Create a base variable large enough to hold all the
        // others and equivlence all the others to it.

        base = genTemp(int_type);
        base.setResidency(Residency.MEMORY);
        base.setHiddenAliases();
        base.setVisibility(Visibility.FILE);
        base.setReferenced();
        cg.addRootSymbol(base);
        cg.addTopLevelDecl(base);

        Vector<EquivalenceDecl> vars = new Vector<EquivalenceDecl>(n);

        commonVars.put(base, vars);

        for (int i = 0; i < n; i++) {
          VariableDecl    vd = cur.getDecl(i);
          long            vo = cur.getOffset(i);
          Type            vt = vd.getType();
          EquivalenceDecl ed = new EquivalenceDecl(vd.getName(), vt, base, off - vo);

          ed.setValue(vd.getValue());
          ed.setHiddenAliases();
          st.replaceSymbol(vd, ed);
          vars.add(ed);
        }

        continue;
      }

      boolean trace2 = false && "scr_".equals(base.getName());
      if (trace2)
        System.out.println("** eq " + cur);

      Declaration ld = cur.getDecl(lind);
      if (ld.isEquivalenceDecl())
        off += ((EquivalenceDecl) ld).getBaseOffset();

      if (trace2)
        System.out.println("   ld " + off + " " + ld);

      // Equivalence all the variables to the base variable.

      for (int i = 0; i < n; i++) {
        VariableDecl    vd = cur.getDecl(i);
        long            vo = cur.getOffset(i);
        EquivalenceDecl ed = null;

        if (!vd.isEquivalenceDecl()) { // Make it look like it is in common.
          ed = new EquivalenceDecl(vd.getName(), vd.getType(), base, 0);
          ed.setValue(vd.getValue());
          st.replaceSymbol(vd, ed);

          Vector<EquivalenceDecl> vars = commonVars.get(base);
          if (vars == null) {
            vars = new Vector<EquivalenceDecl>();
            commonVars.put(base, vars);
          }
          vars.add(ed);
        } else
          ed = (EquivalenceDecl) vd;

        if (trace2)
          System.out.println("** dce " + off + " " + vo + " " + ed);

        ed.setHiddenAliases();

        if (i != lind)
          ed.setBaseOffset(off - vo);
        if (trace2)
          System.out.println("        " + ed.getName() + " " + ed.getBaseOffset() + " " + ed.getValue());
      }
      if (trace2)
        System.out.println("      " + cur);
    }

    // Create the base variable type and initialization.

    int         galn = machine.generalAlignment();
    Enumeration<VariableDecl> eb2  = commonVars.keys();
    while (eb2.hasMoreElements()) {
      VariableDecl base   = eb2.nextElement();
      Vector<EquivalenceDecl> vars   = commonVars.get(base);
      int                     l      = vars.size();
      Vector<FieldDecl>       fields = new Vector<FieldDecl>(l);

      boolean trace2 = false && "_s1".equals(base.getName());
      if (trace2)
        System.out.println("** com " + base.getName());

      boolean changed = true;
      while (changed) {
        changed = false;
        for (int i = 0; i < l - 1; i++) {
          EquivalenceDecl vd1  = vars.get(i);
          EquivalenceDecl vd2  = vars.get(i + 1);
          long            b1   = vd1.getBaseOffset();
          long            b2   = vd2.getBaseOffset();
          boolean         swap = (b1 > b2);

          if (!swap && (b1 == b2))
            swap = (vd2.getValue() != null);

          if (swap) {
            vars.setElementAt(vd2, i);
            vars.setElementAt(vd1, i + 1);
            changed = true;
          }
        }
      }       

      long    position = 0;
      boolean hasInit  = false;
      int     k        = 0;
      long    max      = 0;
      for (int i = 0; i < l; i++) {
        EquivalenceDecl vd   = vars.get(i);
        long            off  = vd.getBaseOffset();
        Type            type = vd.getType();
        Expression      init = vd.getValue();
        long            sz   = type.memorySize(machine);
        long            n    = off - position;

        if (trace2) {
          System.out.print("     ");
          System.out.print(vd.getName());
          System.out.print("  p:");
          System.out.print(position);
          System.out.print(" off:");
          System.out.print(off);
          System.out.print(" n:");
          System.out.print(n);
          System.out.print(" sz:");
          System.out.print(type.memorySize(machine));
          System.out.print(" init:");
          System.out.println((init != null));
        }

        if (n >= 0) {
          if (init == null) {
            ArrayType at = FixedArrayType.create(0, n + sz - 1, saut);
            FieldDecl fd = new FieldDecl("e" + k++, at);
            fields.add(fd);
            if (trace2)
              System.out.println("       " + fd);
          } else {
            hasInit = true;
            if (n > 0) {
              ArrayType at = FixedArrayType.create(0, n - 1, saut);
              FieldDecl fd = new FieldDecl("e" + k++, at);
              fields.add(fd);
              if (trace2)
                System.out.println("       " + fd);
            }

            FieldDecl fd = new FieldDecl("e" + k++, type);
            fields.add(fd);
            if (trace2) {
              System.out.println("       " + fd);
              System.out.println("       " + fd.getCoreType());
            }
          }
          position = off + sz;
        }

        if ((off + sz) > max)
          max = off + sz;
      }

      if (max > position) {
        ArrayType at = FixedArrayType.create(0, max - position - 1, saut);
        FieldDecl fd = new FieldDecl("e" + k++, at);
        fields.add(fd);
      }

      if (hasInit) {
        position = 0;
        Vector<Object> agEls = new Vector<Object>(l);
        for (int i = 0; i < l; i++) {
          EquivalenceDecl vd   = vars.get(i);
          long            off  = vd.getBaseOffset();
          Type            type = vd.getCoreType();
          long            sz   = type.memorySize(machine);
          long            num  = type.numberOfElements();
          Expression      init = vd.getValue();

          long n = off - position;
          if (n >= 0) {
            if (init == null) {
              agEls.add(new PositionRepeatOp((int) (n + sz)));
              agEls.add(zero);
            } else {
              vd.setValue(null);
              if (n > 0) {
                agEls.add(new PositionRepeatOp((int) n));
                agEls.add(zero);
              }

              if (trace2)
                System.out.println("     " + vd.getName() + " " + init);

              agEls.add(init);
            }
          }
          position = off + sz;
        }

        base.setValue(new AggregationElements(int_type, agEls));
      }

      Type bt   = base.getCoreType();
      long size = bt.memorySize(machine);
      long nz   = position - size;
      if (hasInit || (bt == int_type)) {
        if ((nz > 0) && (bt != int_type)) {
          Type ty = FixedArrayType.create(0, nz - 1, saut);
          fields.add(new FieldDecl("e" + k++, ty));
        }
        RecordType at = RecordType.create(fields);
        base.setType(RefType.createAligned(at, galn));
      } else if (nz > 0) {
        Vector<FieldDecl>  nv = ((AggregateType) bt).getAgFields().clone();
        Type       ty = FixedArrayType.create(0, nz - 1, saut);
        FieldDecl  fd = nv.get(nv.size() - 1);
        int        kk = Integer.parseInt(fd.getName().substring(1));
        nv.add(new FieldDecl("e" + (kk + 1), ty));
        RecordType at = RecordType.create(nv);
        base.setType(RefType.createAligned(at, galn));
      }
      if (trace2) {
        System.out.println("    " + base);
        System.out.println("    " + base.getCoreType());
      }
    }

    currentEquivSet = null;
    commonVars.clear();
  }

  /**
   * Finish up a routine.
   */
  private void finishFunction(ProcedureDecl rd, BlockStmt body, int sc, int lineNumber, int column)
  {
    if (fatalError)
      return;

    String name = rd.getName();
    if (body != null) {
      if (rd.getBody() != null) {
        reportError(Msg.MSG_Function_s_is_already_defined, name, filename, lineNumber, column);
        fatalError = true;
      }
      needsReturn(name, (ProcedureType) rd.getType(), body, lineNumber, column);

      body = processEntryStmts(body, rd);

      rd.setBody(body);
      if (sc != cStatic)
        sc = cGlobal;
    }

    sc = (rd.getBody() == null) ? cExtern : sc;
    specifyStorageClass(rd, sc, lineNumber, column);

    fioTypeVar   = null;
    fioNumberVar = null;

    labels.clear();
    nameMap.clear();
    fctMap.clear();
    specList.clear();
  }

  /**
   * Convert a routine that has ENTRY statments.  Use the ENTRY
   * statement information to create the additional routines.
   * <p>
   * Assume that routine <code>proc</code> has two ENTRY statements
   * with names <code>ent1</code> and <code>ent2</code> and
   * <code>body</code> is the body of routine <code>proc</code>.  The
   * result is
   * <pre>
   *   SUBROUTINE __mproc(int __n, ...)
   *   goto(10, 20) __n
   *   body
   *   END
   *   SUBROUTINE proc(...)
   *   call __mproc(3, ...)
   *   END
   *   SUBROUTINE ent1(...)
   *   call __mproc(1, ...)
   *   END
   *   SUBROUTINE ent2(...)
   *   call __mproc(2, ...)
   *   END
   * </pre>
   * The labels used in the computed goto are created when the ENTRY
   * statement is first encountered.
   * @param body is the body of the main routine
   * @param rd is the main routine
   */
  private BlockStmt processEntryStmts(BlockStmt body, ProcedureDecl rd)
  {
    int l = entries.size();
    if (l == 0)
      return body;

    ProcedureType pt   = rd.getSignature();
    int           n    = pt.numFormals();
    Type          rt   = pt.getReturnType();
    String        name = rd.getName();
    LabelDecl     flab = new LabelDecl(name); // Main routines label for the goto.

    // Treat the main routine as just another ENTRY.

    entries.add(rd);
    entries.add(flab);
    l += 2;

    FormalDecl         vd   = createFormalDecl("__n", int_type, ParameterMode.VALUE, 0);
    Vector<LabelDecl>  labs = new Vector<LabelDecl>(l / 2); // For the computed goto.
    Vector<FormalDecl> cf   = new Vector<FormalDecl>();      // Formals for the new main routine.

    addSymbol(vd);
    vd.setReferenced();
    cf.add(vd); // The first formal is an integer value to select which entry point.

    // Collect and merge the entry point formals to create the
    // formals for the created new main routine.

    int cfl = 1;
    for (int i = 0; i < l; i += 2) {
      ProcedureDecl erd = (ProcedureDecl) entries.get(i);
      LabelDecl     lab = (LabelDecl) entries.get(i + 1);

      labs.add(lab);

      ProcedureType ept = erd.getSignature();
      int           nf  = ept.numFormals();
      for (int j = 0; j < nf; j++) {
        FormalDecl fd  = ept.getFormal(j);
        boolean    add = true;
        for (int k = 1; k < cfl; k++) {
          FormalDecl fd2 = cf.get(k);
          if (fd2.getName().equals(fd.getName())) {
            add = false;
            break;
          }
        }
        if (add) {
          cf.add(fd);
          cfl++;
          fd.setReferenced();
          addSymbol(fd);
        }
      }
    }

    int cfl2 = cfl;
    for (int i = 0; i < cfl2; i++) {
      FormalDecl fd  = cf.get(i);
      if (fd.getName().endsWith("_len")) {
        cf.add(fd);
        cf.removeElementAt(i);
        i--;
        cfl2--;
      }
    }


    // Create the new main routine.

    ProcedureType npt = ProcedureType.create(rt, cf, null);
    ProcedureDecl nrd = new ProcedureDecl("__m" + name, npt);
    cg.addRootSymbol(nrd);
    cg.recordRoutine(nrd);
    nrd.setVisibility(Visibility.GLOBAL);

    body.insertStmt(new LabelStmt(flab, new NullStmt()), 0);

    Expression       pred = new IdValueOp(vd);
    ComputedGotoStmt sws  = new ComputedGotoStmt(pred, labs);

    body.insertStmt(sws, 0);

    nrd.setBody(body);

    // Change the body of the routine for each entry point to be a
    // call to the main routine.

    for (int i = 0; i < l; i += 2) {
      ProcedureDecl erd = (ProcedureDecl) entries.get(i);
      ProcedureType ept = erd.getSignature();
      int           nf  = ept.numFormals();

      Vector<Expression> args = new Vector<Expression>(cfl + 1);
      args.add(LiteralMap.put(1 + (i / 2), int_type));
      for (int k = 1; k < cfl; k++) {
        FormalDecl fd2   = cf.get(k);
        String     fname = fd2.getName();

        // If the entry doesn't have that formal, use zero in the call
        // to the new main routine.

        Expression add = zero;
        for (int j = 0; j < nf; j++) {
          FormalDecl fd  = ept.getFormal(j);
          if (fname.equals(fd.getName())) {
            add = new IdValueOp(fd2);
            break;
          }
        }

        args.add(add);
      }

      CallFunctionOp call = new CallFunctionOp(rt, genDeclAddress(nrd), args);
      Statement      stmt = null;

      if (rt != void_type)
        stmt = new ReturnStmt(call);
      else
        stmt = new EvalStmt(call);

      Vector<Statement> stmts = new Vector<Statement>(1);
      stmts.add(stmt);
      erd.setBody(new BlockStmt(stmts));
    }

    entries.clear();

    return (BlockStmt) rd.getBody();
  }

  /**
   * Determine if the compound statement ends with a return
   * statement.  If not, add one.
   * @param name is the function name
   * @param pt is the procedure's signature
   * @param stmt is the compound statement
   */
  private void needsReturn(String name, ProcedureType pt, BlockStmt stmt, int lineNumber, int column)
  {
    Type rettype = pt.getReturnType();
    if (rettype == void_type)
      return;

    if (!needsReturn(stmt))
      return;

    Expression retexp  = null;

    if (resultVar != null) {
      if (resultVar.getCoreType().isFortranCharType())
        retexp = zero;
      else
        retexp = new IdValueOp(resultVar);
    } else if (rettype.isPointerType())
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

  /**
   * Return true if there is no return statement or call to
   * <code>exit()</code> terminating the specified statement.
   */
  private boolean needsReturn(Statement last)
  {
    if (last instanceof ReturnStmt)
      return false;

    if (last instanceof EvalStmt) {
      Expression exp = ((EvalStmt) last).getExpr();
      if (exp instanceof CallOp) {
        Expression ftn = ((CallOp) exp).getRoutine();
        if ((ftn instanceof IdReferenceOp) && (((IdReferenceOp) ftn).getDecl().getName().equals("exit")))
          return false;
      }
    }

    if (last instanceof IfThenElseStmt) {
      IfThenElseStmt ifs = (IfThenElseStmt) last;
      return needsReturn(ifs.getElseStmt()) || needsReturn(ifs.getThenStmt());
    }

    if (!(last instanceof BlockStmt))
      return true;

    BlockStmt bs = (BlockStmt) last;

    int ns = bs.numStmts();
    if (ns == 0)
      return true;

    return needsReturn(bs.getStmt(ns - 1));
  }

  /**
   * Initialize the implicit types for the next routine.
   */
  private void initImplicitTypes()
  {
    implicitTypes = new Type[26];
    for (int i = 'a'; i < 'i'; i++)
      implicitTypes[i - 'a'] = real_type;
    for (int i = 'i'; i < 'o'; i++)
      implicitTypes[i - 'a'] = int_type;
    for (int i = 'o'; i <= 'z'; i++)
      implicitTypes[i - 'a'] = real_type;
  }

  /**
   * Use the implicit types to determine the type to be used.
   */
  private Type determineTypeFromName(String name, Type defaultType)
  {
    Type type = null;
    if (implicitTypes != null) {
      char c = name.charAt(0);
      type = implicitTypes[c - 'a'];
    }

    if (type == null)
      type = defaultType;

    if (type == null) {
      reportError(Msg.MSG_Invalid_or_missing_type, "", filename, lineNumber, column);
      fatalError = true;
      type = int_type;
    }

    return type;
  }

  /**
   * Generate code to check the result of an IO operation.
   */
  private void buildFioErrCheck() throws InvalidException
  {
    Statement stmt = null;
    if (fioChkErr) { // Build inline error check.
      stmt = makeCallExitStmt(one);
    } else if (fioErr != null) { // Goto user specified label.
      stmt = new GotoStmt(fioErr);
    }

    if (stmt != null) {
      Expression chk = new GreaterOp(int_type, makeRValue(fioIostat), zero);
      addNewStatement(new IfThenElseStmt(chk, stmt, new NullStmt()));
    }

    // Generate code to branch to the user specified label on
    // end-of-file.

    if (fioEnd != null) {
      Expression chk = new LessOp(int_type, makeRValue(fioIostat), zero);
      Statement  gs  = new GotoStmt(fioEnd);
      addNewStatement(new IfThenElseStmt(chk, gs, new NullStmt()));
    }
  }

  /**
   * Generate code to set the specified field in the specified IO struct.
   */
  private void setStructField(VariableDecl list, FieldDecl fd, Expression value)
  {
    if (value == null)
      value = zero;

    Type       ft  = fd.getType();
    Expression lhs = new SelectIndirectOp(genDeclAddress(list), fd);

    if (ft.isPointerType() && !ft.equivalent(value.getType()))
      value = new TypeConversionOp(ft, value, CastMode.CAST);

    addNewStatement(new EvalStmt(new AssignSimpleOp(lhs, value)));
  }

  private Expression nextUnitSpecifier() throws InvalidException
  {
    assert traceIn("nextUnitSpecifier", null);
    Expression exp = null;
    try {
      if (nextCharIs('*'))
        exp = LiteralMap.put(6, int_type);
      else
        exp = nextExpression(SCALAR_EXPR);
      return exp;
    } finally {
      assert traceOut("nextUnitSpecifier", exp);
    }
  }

  private Expression nextFormatSpecifier() throws InvalidException
  {
    assert traceIn("nextFormatSpecifier", null);
    Expression fmt = null;
    try {
      // It can be a label, character string, integer variable holding
      // label, char array variable, or an asterisk.
      skipBlanks();

      char c = statement[column];

      if (c == '*') {
        nextCharIs('*');
        fmt = one;
        return fmt;
      }

      if ((c >= '0') && (c <= '9')) {
        LabelDecl    lab = nextLabel();
        VariableDecl vd  = formatMap.get(lab);
        if (vd == null) {
          vd = new VariableDecl(lab.getName(), void_type);
          vd.setResidency(Residency.MEMORY);
          addSymbol(vd);
          formatMap.put(lab, vd);
        }
        fmt = genDeclAddress(vd);
        return fmt;
      }

      fmt = nextArrayExpr();
      if (fmt == null)
        userError(Msg.MSG_Invalid_FORMAT_specifier, null);

      Type      type = fmt.getCoreType();
      ArrayType at   = type.returnArrayType();
      if (at != null)
        type = at.getElementType();

      if (fmt instanceof StringLiteral) {
        VariableDecl vd = createVarFromExp((Literal) fmt);
        fmt = genDeclAddress(vd);
      } else if (type.isFortranCharType())
        fmt = makeLValue(fmt);

      return fmt;
    } finally {
      assert traceOut("nextFormatSpecifier", fmt);
    }
  }

  /**
   * Prepare for the next Fortran IO statement.
   */
  private void resetFioListValues()
  {
    fioUnit = null;        // The expression specifying the unit.
    fioIostat = null;      // Variable to hold the Fortran IO function status.
    fioErr = null;         // The statement to branch to if an error occurs.
    fioFmt = null;         // The variable holding the format or holing the FORMAT label.
    fioRec = null;         // An integer expression.
    fioEnd = null;         // The statement to branch to at end of file.
    fioFile = null;        // A character expression specifying the file.
    fioStatus = null;      // A character expression specifying OLD, NEW, or SCRATCH.
    fioAccess = null;      // A character expression specifying SEQUENTIAL or DIRECT.
    fioForm = null;        // A character expression specifying FORMATTED or UNFORMATTED.
    fioRecl = null;        // An integer expression specifying the record length.
    fioBlank = null;       // A character expression specifying NULL or ZERO.
    fioExist = null;       // A logical l-value expression.
    fioOpened = null;      // A logical l-value expression.
    fioNumber = null;      // An integer l-value expression.
    fioNamed = null;       // A logical l-value expression.
    fioName = null;        // A character l-value expression.
    fioSequential = null;  // A character l-value expression.
    fioDirect = null;      // A character l-value expression.
    fioFormatted = null;   // A character l-value expression.
    fioUnformatted = null; // A character l-value expression.
    fioNextrec = null;     // An integer l-value expression.
  }

  private void nextAlistValues() throws InvalidException
  {
    assert traceIn("", null);
    resetFioListValues();
    try {
      if (nextNBCharIs('(')) {
        nextFioListValues(FIO_ALIST);
        nextNBCharMustBe(')');
      } else {
        fioUnit = nextUnitSpecifier();
        if (fioUnit == null)
          userError(Msg.MSG_Unit_not_specified, "");
      }

      if (fioIostat == null) {
        VariableDecl tv = genTemp(int_type);
        fioIostat = genDeclAddress(tv);
        addSymbol(tv);
      }
    } finally {
      assert traceOut("", null);
    }
  }

  private void nextCilistValues(int allowedKeywords) throws InvalidException
  {
    assert traceIn("nextCilistValues", null);
    resetFioListValues();
    try {
      if (nextNBCharIs('(')) {
        nextFioListValues(allowedKeywords);
        nextNBCharMustBe(')');
      } else {
        fioFmt = nextFormatSpecifier();
        nextNBCharMustBe(',');
      }

      if (fioIostat == null) {
        VariableDecl tv = genTemp(int_type);
        fioIostat = genDeclAddress(tv);
        addSymbol(tv);
      }
    } finally {
      assert traceOut("nextCilistValues", fioUnit);
    }
  }

  private void nextFioListValues(int allowedKeywords) throws InvalidException
  {
    assert traceIn("nextFioListValues", null);

    boolean inquire = allowedKeywords == FIO_INLIST;
    boolean open    = allowedKeywords == FIO_OLIST;
    boolean error   = false;

    try {
      int k = 0;
      do {
        k++;

        int sc      = column;
        int keyword = nextKeyword();

        if ((keyword != Keywords.NA) && nextNBCharIs('=')) {
          switch (keyword) {
          case Keywords.UNIT:
            if ((allowedKeywords & FIO_UNIT) != 0) {
              allowedKeywords &= ~FIO_UNIT;
              fioUnit = nextUnitSpecifier();
            } else
              error = true;
            break;
          case Keywords.FMT:
            if ((allowedKeywords & FIO_FMT) != 0) {
              allowedKeywords &= ~FIO_FMT;
              fioFmt = nextFormatSpecifier();
            } else
              error = true;
            break;
          case Keywords.REC:
            if ((allowedKeywords & FIO_REC) != 0) {
              allowedKeywords &= ~FIO_REC;
              fioRec = nextIntegerExpression();
            } else
              error = true;
            break;
          case Keywords.RECL:
            if ((allowedKeywords & FIO_RECL) != 0) {
              allowedKeywords &= ~FIO_RECL;
              fioRecl = nextIntegerExpression();
              if (inquire && (fioRecl != null))
                fioRecl = makeLValue(fioRecl);
            } else
              error = true;
            break;
          case Keywords.IOSTAT:
            if ((allowedKeywords & FIO_IOSTAT) != 0) {
              allowedKeywords &= ~FIO_IOSTAT;
              fioIostat = makeLValue(nextPrimaryExpr(SCALAR_EXPR));
            } else
              error = true;
            break;
          case Keywords.ERR:
            if ((allowedKeywords & FIO_ERR) != 0) {
              allowedKeywords &= ~FIO_ERR;
              fioErr = nextLabel();
            } else
              error = true;
            break;
          case Keywords.END:
            if ((allowedKeywords & FIO_END) != 0) {
              allowedKeywords &= ~FIO_END;
              fioEnd = nextLabel();
            } else
              error = true;
            break;
          case Keywords.FILE:
            if ((allowedKeywords & FIO_FILE) != 0) {
              allowedKeywords &= ~FIO_FILE;
              fioFile = makeLValue(nextCharacterExpression());
            } else
              error = true;
            break;
          case Keywords.STATUS:
            if ((allowedKeywords & FIO_STATUS) != 0) {
              allowedKeywords &= ~FIO_STATUS;
              fioStatus = makeLValue(nextCharacterExpression());
            } else
              error = true;
            break;
          case Keywords.ACCESS:
            if ((allowedKeywords & FIO_ACCESS) != 0) {
              allowedKeywords &= ~FIO_ACCESS;
              fioAccess = makeLValue(nextCharacterExpression());
            } else
              error = true;
            break;
          case Keywords.FORM:
            if ((allowedKeywords & FIO_FORM) != 0) {
              allowedKeywords &= ~FIO_FORM;
              fioForm = nextCharacterExpression();
              if (inquire || open)
                fioForm = makeLValue(fioForm);
            } else
              error = true;
            break;
          case Keywords.BLANK:
            if ((allowedKeywords & FIO_BLANK) != 0) {
              allowedKeywords &= ~FIO_BLANK;
              fioBlank = makeLValue(nextCharacterExpression());
            } else
              error = true;
            break;
          case Keywords.EXIST:
            if ((allowedKeywords & FIO_EXIST) != 0) {
              allowedKeywords &= ~FIO_EXIST;
              fioExist = makeLValue(nextLogicalExpression());
            } else
              error = true;
            break;
          case Keywords.OPENED:
            if ((allowedKeywords & FIO_OPENED) != 0) {
              allowedKeywords &= ~FIO_OPENED;
              fioOpened = makeLValue(nextLogicalExpression());
            } else
              error = true;
            break;
          case Keywords.NUMBER:
            if ((allowedKeywords & FIO_NUMBER) != 0) {
              allowedKeywords &= ~FIO_NUMBER;
              fioNumber = makeLValue(nextIntegerExpression());
            } else
              error = true;
            break;
          case Keywords.NAMED:
            if ((allowedKeywords & FIO_NAMED) != 0) {
              allowedKeywords &= ~FIO_NAMED;
              fioNamed = makeLValue(nextLogicalExpression());
            } else
              error = true;
            break;
          case Keywords.NAME:
            if ((allowedKeywords & FIO_NAME) != 0) {
              allowedKeywords &= ~FIO_NAME;
              fioName = makeLValue(nextCharacterExpression());
            } else
              error = true;
            break;
          case Keywords.SEQUENTIAL:
            if ((allowedKeywords & FIO_SEQUENTIAL) != 0) {
              allowedKeywords &= ~FIO_SEQUENTIAL;
              fioSequential = makeLValue(nextCharacterExpression());
            } else
              error = true;
            break;
          case Keywords.DIRECT:
            if ((allowedKeywords & FIO_DIRECT) != 0) {
              allowedKeywords &= ~FIO_DIRECT;
              fioDirect = makeLValue(nextCharacterExpression());
            } else
              error = true;
            break;
          case Keywords.FORMATTED:
            if ((allowedKeywords & FIO_FORMATTED) != 0) {
              allowedKeywords &= ~FIO_FORMATTED;
              fioFormatted = makeLValue(nextCharacterExpression());
            } else
              error = true;
            break;
          case Keywords.UNFORMATTED:
            if ((allowedKeywords & FIO_UNFORMATTED) != 0) {
              allowedKeywords &= ~FIO_UNFORMATTED;
              fioUnformatted = makeLValue(nextCharacterExpression());
            } else
              error = true;
            break;
          case Keywords.NEXTREC:
            if ((allowedKeywords & FIO_NEXTREC) != 0) {
              allowedKeywords &= ~FIO_NEXTREC;
              fioNextrec = makeLValue(nextIntegerExpression());
            } else
              error = true;
            break;
          default:
            error = true;
            break;
          }
        } else if (k <= 2) {
          column = sc;
          if ((k == 1) && ((allowedKeywords & FIO_UNIT) != 0)) {
            allowedKeywords &= ~FIO_UNIT;
            fioUnit = nextUnitSpecifier();
          } else if ((k == 2) && ((allowedKeywords & FIO_FMT) != 0)) {
            allowedKeywords &= ~FIO_FMT;
            fioFmt = nextFormatSpecifier();
          } else
            error = true;
        }

      } while (nextNBCharIs(','));

      if (error) {
        userError(Msg.MSG_Invalid_statement, null);
        return;
      }

      fioChkErr = (fioErr == null) && (fioIostat == null);

      if (inquire) {
        if (((fioUnit == null) && (fioFile == null)) ||
            ((fioUnit != null) && (fioFile != null)))
          userError(Msg.MSG_Unit_not_specified, "");
      } else if (fioUnit == null)
        userError(Msg.MSG_Unit_not_specified, "");
    } finally {
      assert traceOut("nextFioListValues", fioUnit);
    }
  }

  /**
   * Determine the f2c type.
   */
  private int determineFioType(Type type)
  {
    ArrayType at = type.getCoreType().returnArrayType();
    if (at != null)
      type = at.getElementType();

    type = type.getCoreType();
    int s = type.memorySizeAsInt(Machine.currentMachine);
    if (type.isIntegerType()) {
      if (!type.isSigned())
        return TYLOGICAL;

      switch (s) {
      case 1: return TYCHAR;
      case 2: return TYSHORT;
      case 4: return TYLONG;
      case 8: return TYQUAD;
      }
    } else if (type.isComplexType()) {
      switch (s) {
      case 8: return TYCOMPLEX;
      case 16: return TYDCOMPLEX;
      }
    } else if (type.isRealType()) {
      switch (s) {
      case 4: return TYREAL;
      case 8: return TYDREAL;
      }
    } else if (type.isFortranCharType() || (type == char_type)) {
      return TYCHAR;
    }
    return TYUNKNOWN;
  }

  private void nextIOList(int ioType) throws InvalidException
  {
    assert traceIn("nextIOList", null);
    try {
      do {
        int        sc  = column;
        Expression exp = nextArrayExpr();

        if (exp != null) {
          switch (ioType) {
          case DO_LIO: buildDolio(exp, currentBlockStmt); break;
          case DO_FIO: buildDofio(exp, currentBlockStmt); break;
          case DO_UIO: buildDouio(exp, currentBlockStmt); break;
          }
          continue;
        }

        column = sc;

        if (nextNBCharIs('(')) {
          try {
            ImpliedDo id = nextImpliedDo();
            addNewStatement(id.genImpliedDoLoop(ioType, this));
            nextNBCharMustBe(')');
            continue;
          } catch (InvalidException ex) {
            column = sc;
          }
        }

      } while (nextNBCharIs(','));
    } finally {
      assert traceOut("nextIOList", null);
    }
  }

  /**
   * Create the f2c cilist struct type.
   */
  private RecordType getCilistType()
  {
    if (cilist != null)
      return cilist;

    Vector<FieldDecl> fields = new Vector<FieldDecl>(5);
    fields.add(new FieldDecl("cierr",  int_type));
    fields.add(new FieldDecl("ciunit", int_type));
    fields.add(new FieldDecl("ciend",  int_type));
    fields.add(new FieldDecl("cifmt",  ccharp_type));
    fields.add(new FieldDecl("cirec",  int_type));
    cilist = RecordType.create(fields);
    cilist.memorySize(Machine.currentMachine);
    return cilist;
  }

  /**
   * Create the f2c cilist struct variable.
   */
  private VariableDecl getCilistVar()
  {
    String       name   = "_cilist";
    VariableDecl cilist = (VariableDecl) lookupDecl(name);
    if (cilist != null)
      return cilist;

    cilist = new VariableDecl(name, getCilistType());
    cilist.setAddressTaken();
    addSymbol(cilist);

    if (fioTypeVar == null) {
      fioTypeVar   = genTemp(int_type);
      fioNumberVar = genTemp(int_type);
      addSymbol(fioTypeVar);
      addSymbol(fioNumberVar);
    }

    return cilist;
  }

  /**
   * Generate code to call the f2c IO start function which uses the
   * cilist struct variable.
   */
  private Statement buildCilistStart(String       ftn,
                                     VariableDecl cilistv,
                                     Expression   unit,
                                     Expression   rec,
                                     Expression   fmt) throws InvalidException
  {
    RecordType cilist = getCilistType();

    setStructField(cilistv, cilist.findField("cierr"), (fioErr != null) ? one : zero);
    setStructField(cilistv, cilist.findField("ciunit"), unit);
    setStructField(cilistv, cilist.findField("ciend"), (fioEnd != null) ? one : zero);
    setStructField(cilistv, cilist.findField("cifmt"), fmt);
    setStructField(cilistv, cilist.findField("cirec"), (rec == null) ? zero : rec);

    ProcedureDecl      rd   = defPreKnownFtn(ftn, "iV", RoutineDecl.PUREGV);
    Expression         ftnr = genDeclAddress(rd);
    Vector<Expression> args = new Vector<Expression>(1);
    Expression         arg  = genDeclAddress(cilistv);

    rd.setReferenced();
    args.add(arg);

    Expression call   = genCall((ProcedureType) rd.getType(),
                                ftnr,
                                args,
                                lineNumber,
                                column);
    Expression assign = new AssignSimpleOp(fioIostat, call);
    return new EvalStmt(assign);
  }

  /**
   * Call the f2c IO start function.
   */
  private Statement buildIOListEnd(String ftn) throws InvalidException
  {
    ProcedureDecl rd   = defPreKnownFtn(ftn, "i", RoutineDecl.PUREGV);
    Expression    ftnr = genDeclAddress(rd);

    rd.setReferenced();

    Expression call   = genCall((ProcedureType) rd.getType(),
                                ftnr,
                                null,
                                lineNumber,
                                column);
    Expression assign = new AssignSimpleOp(fioIostat, call);
    return new EvalStmt(assign);
  }

  /**
   * Call the f2c IO start function which uses the icilist struct variable.
   */
  private Statement buildIcilistStart(String       ftn,
                                      VariableDecl cilistv,
                                      Expression   unit,
                                      Expression   fmt) throws InvalidException
  {
    RecordType cilist = getIcilistType();
    Expression len    = getStringLength(unit);
    Expression num    = one;
    Type       type   = unit.getPointedToCore();
    ArrayType  at     = type.returnArrayType();

    if (at != null)
      num = LiteralMap.put(at.numberOfElements(), int_type);

    setStructField(cilistv, cilist.findField("cierr"), (fioErr != null) ? one : zero);
    setStructField(cilistv, cilist.findField("iciunit"), makeLValue(unit));
    setStructField(cilistv, cilist.findField("ciend"), (fioEnd != null) ? one : zero);
    setStructField(cilistv, cilist.findField("cifmt"), fmt);
    setStructField(cilistv, cilist.findField("icirlen"), len);
    setStructField(cilistv, cilist.findField("icirnum"), num);

    ProcedureDecl      rd   = defPreKnownFtn(ftn, "iV", RoutineDecl.PUREGV);
    Expression         ftnr = genDeclAddress(rd);
    Vector<Expression> args = new Vector<Expression>(1);
    Expression         arg  = genDeclAddress(cilistv);

    rd.setReferenced();
    args.add(arg);

    Expression call   = genCall((ProcedureType) rd.getType(), ftnr, args, lineNumber, column);
    Expression assign = new AssignSimpleOp(fioIostat, call);
    return new EvalStmt(assign);
  }

  /**
   * Create the f2c icilist struct type.
   */
  private RecordType getIcilistType()
  {
    if (icilist != null)
      return icilist;

    Vector<FieldDecl> fields = new Vector<FieldDecl>(6);
    fields.add(new FieldDecl("cierr",   int_type));
    fields.add(new FieldDecl("iciunit", charp_type));
    fields.add(new FieldDecl("ciend",   int_type));
    fields.add(new FieldDecl("cifmt",   ccharp_type));
    fields.add(new FieldDecl("icirlen", int_type));
    fields.add(new FieldDecl("icirnum", int_type));
    icilist = RecordType.create(fields);
    icilist.memorySize(Machine.currentMachine);
    return icilist;
  }

  /**
   * Create the f2c cilist struct variable.
   */
  private VariableDecl getIcilistVar()
  {
    String       name    = "_icilist";
    VariableDecl icilist = (VariableDecl) lookupDecl(name);
    if (icilist != null)
      return icilist;

    icilist = new VariableDecl(name, getIcilistType());
    icilist.setAddressTaken();
    addSymbol(icilist);

    if (fioTypeVar == null) {
      fioTypeVar   = genTemp(int_type);
      fioNumberVar = genTemp(int_type);
      addSymbol(fioTypeVar);
      addSymbol(fioNumberVar);
    }

    return icilist;
  }

  /**
   * Create the f2c olist struct type.
   */
  private RecordType getOlistType()
  {
    if (olist != null)
      return olist;

    Vector<FieldDecl> fields = new Vector<FieldDecl>(9);
    fields.add(new FieldDecl("oerr",    int_type));
    fields.add(new FieldDecl("ounit",   int_type));
    fields.add(new FieldDecl("ofnm",    charp_type));
    fields.add(new FieldDecl("ofnmlen", int_type));
    fields.add(new FieldDecl("osta",    charp_type));
    fields.add(new FieldDecl("oacc",    charp_type));
    fields.add(new FieldDecl("ofm",     charp_type));
    fields.add(new FieldDecl("orl",     int_type));
    fields.add(new FieldDecl("oblnk",   charp_type));
    olist = RecordType.create(fields);
    olist.memorySize(Machine.currentMachine);
    return olist;
  }

  /**
   * Create the f2c olist struct variable.
   */
  private VariableDecl getOlistVar()
  {
    String       name  = "_olist";
    VariableDecl olist = (VariableDecl) lookupDecl(name);
    if (olist != null)
      return olist;

    olist = new VariableDecl(name, getOlistType());
    olist.setAddressTaken();
    addSymbol(olist);

    return olist;
  }

  /**
   * Create the f2c cllist struct type.
   */
  private RecordType getCllistType()
  {
    if (cllist != null)
      return cllist;

    Vector<FieldDecl> fields = new Vector<FieldDecl>(3);
    fields.add(new FieldDecl("cerr",  int_type));
    fields.add(new FieldDecl("cunit", int_type));
    fields.add(new FieldDecl("csta",  charp_type));
    cllist = RecordType.create(fields);
    cllist.memorySize(Machine.currentMachine);
    return cllist;
  }

  /**
   * Create the f2c cllist struct variable.
   */
  private VariableDecl getCllistVar()
  {
    String       name   = "_cllist";
    VariableDecl cllist = (VariableDecl) lookupDecl(name);
    if (cllist != null)
      return cllist;

    cllist = new VariableDecl(name, getCllistType());
    cllist.setAddressTaken();
    addSymbol(cllist);

    return cllist;
  }

  /**
   * Create the f2c alist struct type.
   */
  private RecordType getAlistType()
  {
    if (alist != null)
      return alist;

    Vector<FieldDecl> fields = new Vector<FieldDecl>(2);
    fields.add(new FieldDecl("aerr",  int_type));
    fields.add(new FieldDecl("aunit", int_type));
    alist = RecordType.create(fields);
    alist.memorySize(Machine.currentMachine);
    return alist;
  }

  /**
   * Create the f2c alist struct variable.
   */
  private VariableDecl getAlistVar()
  {
    String       name  = "_alist";
    VariableDecl alist = (VariableDecl) lookupDecl(name);
    if (alist != null)
      return alist;

    alist = new VariableDecl(name, getAlistType());
    alist.setAddressTaken();
    addSymbol(alist);

    return alist;
  }

  /**
   * Generate code to call the f2c IO alist function which uses the
   * alist struct variable.
   */
  private Statement buildAlistFtn(String ftn) throws InvalidException
  {
    assert traceIn("buildAlistFtn", null);
    Statement stmt = null;
    try {
      RecordType   alist  = getAlistType();
      VariableDecl alistv = getAlistVar();

      BlockStmt scbs = currentBlockStmt;
      currentBlockStmt = new BlockStmt();

      setStructField(alistv, alist.findField("aerr"), !fioChkErr ? one : zero);
      setStructField(alistv, alist.findField("aunit"), fioUnit);

      ProcedureDecl      rd   = defPreKnownFtn(ftn, "iV", RoutineDecl.PUREGV);
      Expression         ftnr = genDeclAddress(rd);
      Vector<Expression> args = new Vector<Expression>(1);
      Expression         arg  = genDeclAddress(alistv);

      rd.setReferenced();
      args.add(arg);

      Expression call   = genCall((ProcedureType) rd.getType(), ftnr, args, lineNumber, column);
      Expression assign = new AssignSimpleOp(fioIostat, call);

      addNewStatement(new EvalStmt(assign));

      buildFioErrCheck();
      stmt = currentBlockStmt;
      currentBlockStmt = scbs;
      return stmt;
    } finally {
      assert traceOut("buildAlistFtn", stmt);
    }
  }

  /**
   * Create the f2c inlist struct type used by the INQUIRE statement.
   */
  private RecordType getInlistType()
  {
    if (inlist != null)
      return inlist;

    Vector<FieldDecl> fields = new Vector<FieldDecl>(26);
    fields.add(new FieldDecl("inerr",      int_type));
    fields.add(new FieldDecl("inunit",     int_type));
    fields.add(new FieldDecl("infile",     charp_type));
    fields.add(new FieldDecl("infilen",    int_type));
    fields.add(new FieldDecl("inex",       intp_type));
    fields.add(new FieldDecl("inopen",     intp_type));
    fields.add(new FieldDecl("innum",      intp_type));
    fields.add(new FieldDecl("innamed",    intp_type));
    fields.add(new FieldDecl("inname",     charp_type));
    fields.add(new FieldDecl("innamlen",   int_type));
    fields.add(new FieldDecl("inacc",      charp_type));
    fields.add(new FieldDecl("inacclen",   int_type));
    fields.add(new FieldDecl("inseq",      charp_type));
    fields.add(new FieldDecl("inseqlen",   int_type));
    fields.add(new FieldDecl("indir",      charp_type));
    fields.add(new FieldDecl("indirlen",   int_type));
    fields.add(new FieldDecl("infmt",      charp_type));
    fields.add(new FieldDecl("infmtlen",   int_type));
    fields.add(new FieldDecl("inform",     charp_type));
    fields.add(new FieldDecl("informlen",  int_type));
    fields.add(new FieldDecl("inunf",      charp_type));
    fields.add(new FieldDecl("inunflen",   int_type));
    fields.add(new FieldDecl("inrecl",     intp_type));
    fields.add(new FieldDecl("innrec",     intp_type));
    fields.add(new FieldDecl("inblank",    charp_type));
    fields.add(new FieldDecl("inblanklen", int_type));
    inlist = RecordType.create(fields);
    inlist.memorySize(Machine.currentMachine);
    return inlist;
  }

  /**
   * Create the f2c inlist struct variable.
   */
  private VariableDecl getInlistVar()
  {
    String       name   = "_inlist";
    VariableDecl inlist = (VariableDecl) lookupDecl(name);
    if (inlist != null)
      return inlist;

    inlist = new VariableDecl(name, getInlistType());
    inlist.setAddressTaken();
    addSymbol(inlist);

    return inlist;
  }

  /**
   * Implied-do callbacks.
   */
  private static final int DO_LIO = 0;
  private static final int DO_FIO = 1;
  private static final int DO_UIO = 2;

  /**
   * Called by ImpliedDo to generate code for implied-do loops.
   */
  public void callback(int callback, Expression exp, BlockStmt bs) throws InvalidException
  {
    switch (callback) {
    case DO_LIO: buildDolio(exp, bs); break;
    case DO_FIO: buildDofio(exp, bs); break;
    case DO_UIO: buildDouio(exp, bs); break;
    default: throw new scale.common.InternalError("Unknown callback.");
    }

    return;
  }

  /**
   * Generate code to call the f2c do_lio function.
   */
  private void buildDolio(Expression exp, BlockStmt bs) throws InvalidException
  {
    BlockStmt scbs = currentBlockStmt;
    currentBlockStmt = bs;

    try {
      Type          type = exp.getPointedToCore();
      IdAddressOp   tref = genDeclAddress(fioTypeVar);
      IdAddressOp   nref = genDeclAddress(fioNumberVar);
      Vector<Expression> args = new Vector<Expression>(4);
      ProcedureDecl rd   = defPreKnownFtn("do_lio", "iIIVi", RoutineDecl.PUREGV);
      int           ft   = determineFioType(type);
      Literal       tlit = LiteralMap.put(ft, int_type);
      Expression    nlit = null;
      Expression    llit = null;

      if (isFCharType(type)) {
        llit = getStringLength(exp);
        nlit = one;
      } else {
        long nl = type.numberOfElements();
        long ll = type.elementSize(Machine.currentMachine);

        nlit = LiteralMap.put(nl, int_type);
        llit = LiteralMap.put(ll, int_type);
      }

      addNewStatement(new EvalStmt(new AssignSimpleOp(tref, tlit)));
      addNewStatement(new EvalStmt(new AssignSimpleOp(nref, nlit)));

      args.add(tref);
      args.add(nref);
      args.add(makeLValue(exp));
      args.add(llit);

      Expression call   = genCall((ProcedureType) rd.getType(), genDeclAddress(rd), args, lineNumber, column);
      Expression assign = new AssignSimpleOp(fioIostat, call);

      addNewStatement(new EvalStmt(assign));
      buildFioErrCheck();
    } finally {
      currentBlockStmt = scbs;
    }
  }

  /**
   * Generate code to call the f2c do_fio function.
   */
  private void buildDofio(Expression exp, BlockStmt bs) throws InvalidException
  {
    BlockStmt scbs = currentBlockStmt;
    currentBlockStmt = bs;

    try {
      Type          type = exp.getPointedToCore();
      IdAddressOp   nref = genDeclAddress(fioNumberVar);
      Vector<Expression> args = new Vector<Expression>(3);
      ProcedureDecl rd   = defPreKnownFtn("do_fio", "iIVi", RoutineDecl.PUREGV);
      Expression    nlit = null;
      Expression    llit = null;

      if (isFCharType(type)) {
        nlit = one;
        llit = getStringLength(exp);
      } else {
        long nl = type.numberOfElements();
        long ll = type.elementSize(Machine.currentMachine);

        if (type.isComplexType()) {
          nl <<= 1;
          ll >>= 1;
        }

        nlit = LiteralMap.put(nl, int_type);
        llit = LiteralMap.put(ll, int_type);
      }

      addNewStatement(new EvalStmt(new AssignSimpleOp(nref, nlit)));

      args.add(nref);
      args.add(makeLValue(exp));
      args.add(llit);

      Expression call   = genCall((ProcedureType) rd.getType(), genDeclAddress(rd), args, lineNumber, column);
      Expression assign = new AssignSimpleOp(fioIostat, call);

      addNewStatement(new EvalStmt(assign));
      buildFioErrCheck();
    } finally {
      currentBlockStmt = scbs;
    }
  }

  /**
   * Generate code to call the f2c do_uio function.
   */
  private void buildDouio(Expression exp, BlockStmt bs) throws InvalidException
  {
    BlockStmt scbs = currentBlockStmt;
    currentBlockStmt = bs;

    try {
      Type          type = exp.getPointedToCore();
      IdAddressOp   nref = genDeclAddress(fioNumberVar);
      Vector<Expression> args = new Vector<Expression>(3);
      ProcedureDecl rd   = defPreKnownFtn("do_uio", "iIVi", RoutineDecl.PUREGV);
      Expression    nlit = null;
      Expression    llit = null;

      if (isFCharType(type)) {
        llit = getStringLength(exp);
        nlit = one;
      } else {
        long nl = type.numberOfElements();
        long ll = type.elementSize(Machine.currentMachine);

        if (type.isComplexType()) {
          nl <<= 1;
          ll >>= 1;
        }

        nlit = LiteralMap.put(nl, int_type);
        llit = LiteralMap.put(ll, int_type);
      }

      addNewStatement(new EvalStmt(new AssignSimpleOp(nref, nlit)));

      args.add(nref);
      args.add(makeLValue(exp));
      args.add(llit);

      Expression call   = genCall((ProcedureType) rd.getType(), genDeclAddress(rd), args, lineNumber, column);
      Expression assign = new AssignSimpleOp(fioIostat, call);

      addNewStatement(new EvalStmt(assign));
      buildFioErrCheck();
    } finally {
      currentBlockStmt = scbs;
    }
  }

  private void nextProgramUnit()
  {
    assert traceIn("nextProgramUnit", null);
    try {
      labels.clear();
      commonVars.clear();

      currentEquivSet = null;

      int keyword = nextKeyword();
      if (keyword == Keywords.NA) {
        if (mainFound) {
          reportError(Msg.MSG_Invalid_statement, null, filename, lineNumber, column);
          fatalError = true;
        }

        column = 0;
        nextMainProgram("MAIN");
        return;
      }

      switch (keyword) {
      case Keywords.PROGRAM:    nextMainProgram(null);      break;
      case Keywords.SUBROUTINE: nextSubroutineSubprogram(); break;
      case Keywords.MODULE:     nextModule();               break;
      case Keywords.INTEGER:
      case Keywords.REAL:
      case Keywords.COMPLEX:
      case Keywords.DOUBLECOMPLEX:
      case Keywords.LOGICAL:
      case Keywords.CHARACTER:
      case Keywords.DOUBLEPRECISION: // fall through.
      case Keywords.FUNCTION:   nextFunctionSubprogram(keyword); break;
      case Keywords.BLOCK:
        keyword = nextKeyword();
        if (keyword != Keywords.DATA) {
          reportError(Msg.MSG_Invalid_statement, null, filename, lineNumber, column);
          fatalError = true;
          nextStatement();
          return;
        }
        nextBlockData();
        break;
      case Keywords.BLOCKDATA:  nextBlockData();            break;
      default:
        reportError(Msg.MSG_Invalid_statement, null, filename, lineNumber, column);
        fatalError = true;
        nextStatement();
        break;
      }
    } finally {
      assert traceOut("nextProgramUnit", null);
    }
  }

  private void nextMainProgram(String name)
  {
    assert traceIn("nextMainProgram", name);
    try {

      if (mainFound) {
        reportError(Msg.MSG_Invalid_statement, "only one main allowed", filename, lineNumber, column);
        fatalError = true;
      }
      mainFound = true;

      if (name == null) {
        name = nextIdentifier();
        column += name.length();
      }

      nextStatement();
      if (eofStmt)
        return;

      SymtabScope   cs  = cg.getSymbolTable().beginScope();
      ProcedureDecl rd  = buildProcedureDecl(name, void_type, null, lineNumber, column);
      BlockStmt     ss  = currentBlockStmt;
      int           sln = lineNumber;
      int           sc  = column;
      Type[]        sit = implicitTypes;

      rd.setMain();

      topLevel = false;
      currentBlockStmt = new BlockStmt();
      stmtNestedLevel++;
      currentFunction = rd;
      curFtnIsSub = true;
      initImplicitTypes();
      resultVar = null;
      globalSaveFlag = false;

      actualFormals = null;
      entryFormals = null;

      nextImplicitPart();
      nextSpecificationPart(Keywords.kw_main);
      nextExecutionPart(Keywords.kw_main, null);
      nextInternalSubprogramPart(Keywords.kw_main);
      nextEndStmt(Keywords.ENDPROGRAM);
      nextStatement();

      finishBlockStmt(currentBlockStmt, true, cs, sln, sc);
      finishFunction(rd, currentBlockStmt, sc, sln, sc);
      createDeclStatments(currentBlockStmt, lineNumber, column);

      currentBlockStmt = ss;
      cg.getSymbolTable().endScope();
      stmtNestedLevel--;
      implicitTypes = sit;
    } finally {
      assert traceOut("nextMainProgram", name);
    }
  }

  private void nextFunctionSubprogram(int keyword)
  {
    assert traceIn("nextFunctionSubprogram", null);
    String name = null;
    try {
      Type    returnType = null;
      boolean tdeclared  = false;
      if (keyword != Keywords.FUNCTION) {
        try {
          returnType = nextKindSelector(keyword);
          tdeclared  = true;
        } catch(InvalidException ex) {
        }
        keyword = nextKeyword();
        if (keyword != Keywords.FUNCTION) {
          reportError(Msg.MSG_Invalid_statement, null);
          fatalError = true;
          return;
        }
        name = nextIdentifier();
        if (name == null) {
          reportError(Msg.MSG_Invalid_statement, null);
          fatalError = true;
        }

      } else {
        name = nextIdentifier();
        if (name == null) {
          reportError(Msg.MSG_Invalid_statement, null);
          fatalError = true;
        }
      }

      SymtabScope cs = cg.getSymbolTable().beginScope();
      try {
        actualFormals = nextFormals(false);
      } catch (InvalidException ex) {
      }

      nextStatement();

      Type[] sit = implicitTypes;
      initImplicitTypes();
      nextImplicitPart();

      if (returnType == null)
        returnType = determineTypeFromName(name, void_type);

      ProcedureDecl rd  = buildProcedureDecl(name, returnType, actualFormals, lineNumber, column);
      BlockStmt     ss  = currentBlockStmt;
      int           sln = lineNumber;
      int           sc  = column;

      topLevel = false;
      currentBlockStmt = new BlockStmt();
      stmtNestedLevel++;
      currentFunction = rd;
      curFtnIsSub = false;
      globalSaveFlag = false;

      VariableDecl srv = resultVar;
      if (returnType.isFortranCharType())
        resultVar = rd.getSignature().getFormal(0);
      else {
        resultVar = new VariableDecl(name, returnType);
        addSymbol(resultVar);
      }

      if (actualFormals != null)
        entryFormals = actualFormals.clone();

      nextSpecificationPart(Keywords.kw_extsub);
      nextExecutionPart(Keywords.kw_extsub, null);
      nextInternalSubprogramPart(Keywords.kw_extsub);
      nextEndStmt(Keywords.ENDFUNCTION);
      nextStatement();

      finishBlockStmt(currentBlockStmt, true, cs, sln, sc);
      finishFunction(rd, currentBlockStmt, sc, sln, sc);
      createDeclStatments(currentBlockStmt, lineNumber, column);

      resultVar = srv;

      currentBlockStmt = ss;
      cg.getSymbolTable().endScope();
      stmtNestedLevel--;
      implicitTypes = sit;
      actualFormals = null;
      entryFormals = null;
    } finally {
      assert traceOut("nextFunctionSubprogram", name);
    }
  }

  private void nextSubroutineSubprogram()
  {
    assert traceIn("nextSubroutineSubprogram", null);
    String name = null;
    try {
      name = nextIdentifier();

      if (name == null) {
        reportError(Msg.MSG_Invalid_statement, null, filename, lineNumber, column);
        fatalError = true;
      }

      SymtabScope cs = cg.getSymbolTable().beginScope();
      try {
        actualFormals = nextFormals(true);
        nextStatement();
      } catch (InvalidException ex) {
        System.out.println("** ss " + ex.getMessage());
        reportError(Msg.MSG_Invalid_statement, "");
        fatalError = true;
        cg.getSymbolTable().endScope();
        return;
      }

      Type rt = void_type;
      if ((actualFormals != null) && (actualFormals.size() > 0) && (actualFormals.get(0) == null)) {
        actualFormals.remove(0);
        rt = int_type;
      }

      ProcedureDecl rd  = buildProcedureDecl(name, rt, actualFormals, lineNumber, column);
      BlockStmt     ss  = currentBlockStmt;
      int           sln = lineNumber;
      int           sc  = column;
      Type[]        sit = implicitTypes;

      topLevel = false;
      currentBlockStmt = new BlockStmt();
      stmtNestedLevel++;
      currentFunction = rd;
      curFtnIsSub = true;
      initImplicitTypes();
      resultVar = null;
      globalSaveFlag = false;

      if (actualFormals != null)
        entryFormals = actualFormals.clone();

      nextImplicitPart();
      nextSpecificationPart(Keywords.kw_extsub);
      nextExecutionPart(Keywords.kw_extsub, null);
      nextInternalSubprogramPart(Keywords.kw_extsub);
      nextEndStmt(Keywords.ENDSUBROUTINE);
      nextStatement();

      finishBlockStmt(currentBlockStmt, true, cs, sln, sc);
      finishFunction(rd, currentBlockStmt, sc, sln, sc);
      createDeclStatments(currentBlockStmt, lineNumber, column);

      currentBlockStmt = ss;
      cg.getSymbolTable().endScope();
      stmtNestedLevel--;
      implicitTypes = sit;
      actualFormals = null;
      entryFormals = null;
    } finally {
      assert traceOut("nextSubroutineSubprogram", name);
    }
  }

  private void nextModule()
  {
    assert traceIn("nextModule", null);
    String name = null;
    try {
      name = nextIdentifier();
      column += name.length();

      if (name == null) {
        reportError(Msg.MSG_Invalid_statement, null, filename, lineNumber, column);
        fatalError = true;
      }

      nextStatement();

      actualFormals = null;
      entryFormals = null;

      nextImplicitPart();
      nextSpecificationPart(Keywords.kw_module);
      nextModuleSubprogramPart(Keywords.kw_module);
      nextEndStmt(Keywords.ENDMODULE);
      nextStatement();

    } finally {
      assert traceOut("nextModule", name);
    }
  }

  private void nextBlockData()
  {
    assert traceIn("nextBlockData", null);
    String name = null;
    inBlockData = true;
    try {
      name = nextIdentifier();

      SymtabScope cs  = cg.getSymbolTable().beginScope();
      BlockStmt   ss  = currentBlockStmt;
      int         sln = lineNumber;
      int         sc  = column;
      Type[]      sit = implicitTypes;

      nextStatement();

      currentBlockStmt = new BlockStmt();
      stmtNestedLevel++;
      initImplicitTypes();

      actualFormals = null;
      entryFormals = null;

      nextImplicitPart();
      nextSpecificationPart(Keywords.kw_block);
      nextEndStmt(Keywords.ENDBLOCKDATA);
      nextStatement();

      createDeclStatments(currentBlockStmt, sln, sc);
      currentBlockStmt = ss;
      cg.getSymbolTable().endScope();
      stmtNestedLevel--;
      implicitTypes = sit;

      labels.clear();
      nameMap.clear();

    } finally {
      inBlockData = false;
      assert traceOut("nextBlockData", name);
    }
  }

  private void nextImplicitPart()
  {
    assert traceIn("nextImplicitPart", null);
    try {
      while (!eofStmt) {
        int sc      = column;
        int keyword = nextKeyword();
        if (keyword != Keywords.IMPLICIT) {
          column = sc;
          break;
        }

        nextImplicitStmt();
        nextStatement();
      }
    } finally {
      assert traceOut("nextImplicitPart", null);
    }
  }

  private void nextSpecificationPart(int[] allowed)
  {
    assert traceIn("nextSpecificationPart", null);
    try {
      boolean cont = true;
      while (cont) {
        int sc = column;

        if (nextStatementFunction()) {
          nextStatement();
          continue;
        } else
          column = sc;

        int keyword = nextKeyword();
        if (!Keywords.isSet(allowed, keyword) || !Keywords.isSet(validInVersion, keyword)) {
          column = sc;
          break;
        }

        Statement stmt = null;
        switch (keyword) {
        case Keywords.INTEGER:        stmt = nextTypeDeclStmt(keyword); break;
        case Keywords.REAL:           stmt = nextTypeDeclStmt(keyword); break;
        case Keywords.DOUBLE:         stmt = nextTypeDeclStmt(keyword); break;
        case Keywords.DOUBLEPRECISION:stmt = nextTypeDeclStmt(keyword); break;
        case Keywords.COMPLEX:        stmt = nextTypeDeclStmt(keyword); break;
        case Keywords.DOUBLECOMPLEX:  stmt = nextTypeDeclStmt(keyword); break;
        case Keywords.CHARACTER:      stmt = nextTypeDeclStmt(keyword); break;
        case Keywords.LOGICAL:        stmt = nextTypeDeclStmt(keyword); break;
        case Keywords.TYPE:           stmt = nextTypeDeclStmt(keyword); break;
        case Keywords.PARAMETER:      stmt = nextParameterStmt();       break;
        case Keywords.FORMAT:         stmt = nextFormatStmt();          break;
        case Keywords.ENTRY:          stmt = nextEntryStmt();           break;
        case Keywords.INTERFACE:      stmt = nextInterfaceBlock();      break;
        case Keywords.PUBLIC:         stmt = nextPublicStmt();          break;
        case Keywords.PRIVATE:        stmt = nextPrivateStmt();         break;
        case Keywords.ALLOCATABLE:    stmt = nextAllocatableStmt();     break;
        case Keywords.COMMON:         stmt = nextCommonStmt();          break;
        case Keywords.DATA:           stmt = nextDataStmt();            break;
        case Keywords.DIMENSION:      stmt = nextDimensionStmt();       break;
        case Keywords.EQUIVALENCE:    stmt = nextEquivalenceStmt();     break;
        case Keywords.EXTERNAL:       stmt = nextExternalStmt();        break;
        case Keywords.INTENT:         stmt = nextIntentStmt();          break;
        case Keywords.INTRINSIC:      stmt = nextIntrinsicStmt();       break;
        case Keywords.NAMELIST:       stmt = nextNamelistStmt();        break;
        case Keywords.OPTIONAL:       stmt = nextOptionalStmt();        break;
        case Keywords.POINTER:        stmt = nextPointerStmt();         break;
        case Keywords.SAVE:           stmt = nextSaveStmt();            break;
        case Keywords.TARGET:         stmt = nextTargetStmt();          break;
        default:
          column = sc;
          cont = false;
          break;
        }

        if (stmt != null)
          addNewStatement(stmt);

        nextStatement();
      }

      doCommonEquivalence();
    } finally {
      assert traceOut("nextSpecificationPart", null);
    }
  }

  private boolean nextStatementFunction()
  {
    assert traceIn("nextStatementFunction", null);
    StmtFtnDecl macro = null;
    try {
      boolean found = false;
      String  name  = nextIdentifier();
      if (name == null)
        return false;

      Declaration decl   = lookupDecl(name);
      boolean     postit = false;
      if (decl == null) {
        // We haven't defined this symbol yet but we may have
        // encountered its type definition already.
        Object o = nameMap.get(name);
        if (o instanceof Literal)
          return false;

        decl = (Declaration) o;
        postit = true;
      }

      Type type = null;
      if (decl != null) {
        VariableDecl vd = decl.returnVariableDecl();
        if ((vd == null) ||
            vd.isEquivalenceDecl() ||
            vd.isFormalDecl() ||
            (vd.getValue() != null) ||
            vd.getType().isArrayType())
          return false;
        type = vd.getType();
      }

      Vector<String> args = nextList();
      if (args == null)
        return false;

      if (!nextNBCharIs('='))
        return false;

      int start = column;
      while (statement[column] != 0)
        column++;

      if (type == null)
        type = determineTypeFromName(name, void_type);
      macro = new StmtFtnDecl(type, name, args, new String(statement, start, column - start));
      if (!postit && (decl != null))
        cg.getSymbolTable().replaceSymbol(decl, macro);
      else
        addSymbol(macro);

      return true;
    } catch (InvalidException ex) {
      return false;
    } finally {
      assert traceOut("nextStatementFunction", macro);
    }
  }

  private void nextEndStmt(int endType)
  {
    assert traceIn("nextEndStmt", Keywords.keywords[endType]);
    int keyword = 0;
    try {
      keyword = nextKeyword();

      if (keyword == Keywords.NA) {
        if (!fatalError)
          userError(Msg.MSG_Missing_END_statement, "");
        return;
      }

      if (keyword == endType)
        return;

      boolean endok = false;

      if (keyword == Keywords.END) {
        keyword = nextKeyword();

        if (keyword == Keywords.NA) {
          keyword = Keywords.END;
          switch (endType) {
          case Keywords.ENDPROGRAM:
          case Keywords.ENDFUNCTION:
          case Keywords.ENDBLOCKDATA:
          case Keywords.ENDSUBROUTINE: endok = true; break;
          default: break;
          }
        }

        if (keyword == Keywords.BLOCK) {
          keyword = nextKeyword();
          if (keyword == Keywords.DATA)
            keyword = Keywords.BLOCKDATA;
          else
            keyword = Keywords.NA;
        }


        switch (keyword) {
        case Keywords.PROGRAM:    keyword = Keywords.ENDPROGRAM;    break;
        case Keywords.FUNCTION:   keyword = Keywords.ENDFUNCTION;   break;
        case Keywords.BLOCKDATA:  keyword = Keywords.ENDBLOCKDATA;  break;
        case Keywords.INTERFACE:  keyword = Keywords.ENDINTERFACE;  break;
        case Keywords.SUBROUTINE: keyword = Keywords.ENDSUBROUTINE; break;
        case Keywords.MODULE:     keyword = Keywords.ENDMODULE;     break;
        case Keywords.TYPE:       keyword = Keywords.ENDTYPE;       break;
        case Keywords.WHERE:      keyword = Keywords.ENDWHERE;      break;
        case Keywords.SELECT:     keyword = Keywords.ENDSELECT;     break;
        case Keywords.IF:         keyword = Keywords.ENDIF;         break;
        case Keywords.FORALL:     keyword = Keywords.ENDFORALL;     break;
        case Keywords.DO:         keyword = Keywords.ENDDO;         break;
        }
      }

      if ((keyword == endType) || endok)
        return;

      userError(Msg.MSG_Missing_END_statement, "");
    } catch (InvalidException ex) {
    } finally {
      assert traceOut("nextEndStmt", Keywords.keywords[keyword]);
    }
  }

  /**
   * Process statements until one that is
   * <ul>
   * <li> not allowed,
   * <li> has the specified label, or
   * <li> is not recognized.
   * </ul>
   */
  private void nextExecutionPart(int[] allowed, LabelDecl stopLab)
  {
    assert traceIn("nextExecutionPart", null);
    try {
      while (!eofStmt) {
        Statement stmt = null;
        int       sc   = column;
        int       line = lineNumber;
        String    name = nextIdentifier();

        if ((name != null) && nextNBCharIs(':')) {
          int keyword = nextKeyword();
          if (Keywords.isConstruct(keyword)) {
            if (!Keywords.isSet(allowed, keyword) || !Keywords.isSet(validInVersion, keyword)) {
              return;
            }
            switch (keyword) {
            case Keywords.DO:     stmt = nextDoStmt(name);     break;
            case Keywords.FORALL: stmt = nextForallStmt(name); break;
            case Keywords.IF:     stmt = nextIfStmt(name);     break;
            case Keywords.SELECT: stmt = nextSelectStmt(name); break;
            case Keywords.WHERE:  stmt = nextWhereStmt(name);  break;
            }
          }

          if (stmt != null)
            addNewStatement(stmt);

          if ((stopLab != null) && (stopLab.getTag() == 1))
            return;

          nextStatement();
          continue;
        } else {
          column = sc;
          stmt = nextAssignmentStmt();
          if (stmt != null) {
            addNewStatement(stmt);
            if ((stopLab != null) && (stopLab.getTag() == 1))
              return;
            nextStatement();
            continue;
          }
        }

        column = sc;
        int keyword = nextKeyword();

        if (keyword == Keywords.NA) {
          reportError(Msg.MSG_Invalid_statement, "");
          fatalError = true;
          column = sc;
          return;
        }

        if (!Keywords.isExe(keyword) || !Keywords.isSet(validInVersion, keyword)) {
          column = sc;
          return;
        }

        switch (keyword) {
        case Keywords.FORMAT:     stmt = nextFormatStmt();     break;
        case Keywords.ENTRY:      stmt = nextEntryStmt();      break;
        case Keywords.DATA:       stmt = nextDataStmt();       break;
        case Keywords.CASE:       stmt = nextCaseStmt();       break;
        case Keywords.DO:         stmt = nextDoStmt(null);     break;
        case Keywords.FORALL:     stmt = nextForallStmt(null); break;
        case Keywords.IF:         stmt = nextIfStmt(null);     break;
        case Keywords.WHERE:      stmt = nextWhereStmt(null);  break;
        case Keywords.ALLOCATE:   stmt = nextAllocateStmt();   break;
        case Keywords.BACKSPACE:  stmt = nextBackspaceStmt();  break;
        case Keywords.CALL:       stmt = nextCallStmt();       break;
        case Keywords.CLOSE:      stmt = nextCloseStmt();      break;
        case Keywords.CONTINUE:   stmt = nextContinueStmt();   break;
        case Keywords.CYCLE:      stmt = nextCycleStmt();      break;
        case Keywords.DEALLOCATE: stmt = nextDeallocateStmt(); break;
        case Keywords.ENDFILE:    stmt = nextEndfileStmt();    break;
        case Keywords.EXIT:       stmt = nextExitStmt();       break;
        case Keywords.GOTO:       stmt = nextGotoStmt();       break;
        case Keywords.INQUIRE:    stmt = nextInquireStmt();    break;
        case Keywords.NULLIFY:    stmt = nextNullifyStmt();    break;
        case Keywords.OPEN:       stmt = nextOpenStmt();       break;
        case Keywords.PAUSE:      stmt = nextPauseStmt();      break;
        case Keywords.PRINT:      stmt = nextPrintStmt();      break;
        case Keywords.READ:       stmt = nextReadStmt();       break;
        case Keywords.WRITE:      stmt = nextWriteStmt();      break;
        case Keywords.RETURN:     stmt = nextReturnStmt();     break;
        case Keywords.REWIND:     stmt = nextRewindStmt();     break;
        case Keywords.STOP:       stmt = nextStopStmt();       break;
        case Keywords.ASSIGN:     stmt = nextAssignStmt();     break;
        default:
          column = sc;
          return;
        }

        if (stmt != null)
          addNewStatement(stmt, line, sc);

        if ((stopLab != null) && (stopLab.getTag() == 1))
          return;

        nextStatement();
      }
    } finally {
      assert traceOut("nextExecutionPart", null);
    }
  }

  private Statement nextActionStmt()
  {
    assert traceIn("nextActionStmt", null);
    try {
      int       sc   = column;
      Statement stmt = nextAssignmentStmt();
      if (stmt != null)
        return stmt;

      column = sc;

      int keyword = nextKeyword();
      switch (keyword) {
      case Keywords.ALLOCATE:   return nextAllocateStmt();
      case Keywords.BACKSPACE:  return nextBackspaceStmt();
      case Keywords.CALL:       return nextCallStmt();
      case Keywords.CLOSE:      return nextCloseStmt();
      case Keywords.CONTINUE:   return nextContinueStmt();
      case Keywords.CYCLE:      return nextCycleStmt();
      case Keywords.DEALLOCATE: return nextDeallocateStmt();
      case Keywords.DO:         return nextDoStmt(null);
      case Keywords.ENDFILE:    return nextEndfileStmt();
      case Keywords.EXIT:       return nextExitStmt();
      case Keywords.FORALL:     return nextForallStmt(null);
      case Keywords.GOTO:       return nextGotoStmt();
      case Keywords.INQUIRE:    return nextInquireStmt();
      case Keywords.NULLIFY:    return nextNullifyStmt();
      case Keywords.OPEN:       return nextOpenStmt();
      case Keywords.PAUSE:      return nextPauseStmt();
      case Keywords.PRINT:      return nextPrintStmt();
      case Keywords.READ:       return nextReadStmt();
      case Keywords.RETURN:     return nextReturnStmt();
      case Keywords.REWIND:     return nextRewindStmt();
      case Keywords.STOP:       return nextStopStmt();
      case Keywords.WHERE:      return nextWhereStmt(null);
      case Keywords.WRITE:      return nextWriteStmt();
      default:
        column = sc;
        return null;
      }
    } finally {
      assert traceOut("nextActionStmt", null);
    }
  }

  private void nextInternalSubprogramPart(int[] allowed)
  {
    assert traceIn("nextInternalSubprogramPart", null);
    try {
      int sc      = column;
      int keyword = nextKeyword();
      if ((keyword != Keywords.CONTAINS)  || !Keywords.isSet(validInVersion, keyword)) {
        column = sc;
        return;
      }

      nextContainsStmt();

      while (!eofStmt) {
        sc = column;
        keyword = nextKeyword();
        if (keyword == Keywords.FUNCTION)
          nextFunctionSubprogram(keyword);
        else if (keyword == Keywords.SUBROUTINE)
          nextSubroutineSubprogram();
        else {
          column = sc;
          break;
        }
      }
    } finally {
      assert traceOut("nextInternalSubprogramPart", null);
    }
  }

  private void nextModuleSubprogramPart(int[] allowed)
  {
    assert traceIn("nextModuleSubprogramPart", null);
    try {
      int keyword = nextKeyword();
      if ((keyword != Keywords.CONTAINS) || !Keywords.isSet(validInVersion, keyword))
        return;

      nextContainsStmt();

      while (!eofStmt) {
        keyword = nextKeyword();
        if (keyword == Keywords.FUNCTION)
          nextFunctionSubprogram(keyword);
        else if (keyword == Keywords.SUBROUTINE)
          nextSubroutineSubprogram();
        else
          break;
      }
    } finally {
      assert traceOut("nextModuleSubprogramPart", null);
    }
  }

  private void nextImplicitPartStmt(int[] allowed)
  {
    assert traceIn("nextImplicitPartStmt", null);
    try {
      int sc      = column;
      int keyword = nextKeyword();
      if ((!Keywords.isSet(allowed, keyword)) || !Keywords.isSet(validInVersion, keyword)) {
        column = sc;
        return;
      }

      Statement stmt = null;

      switch (keyword) {
      case Keywords.IMPLICIT:  nextImplicitStmt();     break;
      case Keywords.PARAMETER: nextParameterStmt();    break;
      case Keywords.FORMAT:    nextFormatStmt();       break;
      case Keywords.ENTRY:     nextEntryStmt();        break;
      default:
        column = sc;
        return;
      }

    } finally {
      assert traceOut("nextImplicitPartStmt", null);
    }
  }

  private SeriesOp nextSubstringSpec()
  {
    assert traceIn("nextSubstringSpec", null);
    SeriesOp exp = null;
    int      sc  = column;
    try {
      Expression lower = one;
      Expression upper = null;

      lower = nextExpression(SCALAR_EXPR);
      if (!nextNBCharIs(':')) {
        column = sc;
        return null;
      }

      if (lower == null)
        lower = one;

      if (!lower.getCoreType().isIntegerType())
        userError(Msg.MSG_Not_an_integer_value, "");

      upper = nextIntegerExpression();
      nextNBCharMustBe(')');

      if (upper == null)
        upper = new NilOp();

      exp = new SeriesOp(int_type, lower, upper);
      return exp;
    } catch (InvalidException ex) {
      column = sc;
      return null;
    } finally {
      assert traceOut("nextSubstringSpec", exp);
    }
  }

  private Bound nextRangeSpec() throws InvalidException
  {
    assert traceIn("nextRangeSpec", null);
    Bound bd = null;
    try {
      Expression lower = one;
      Expression upper = one;

      if (!nextNBCharIs(':') && !nextNBCharIs('*')) {
        upper = nextSpecificationExpression();
        upper = getConstantValue(upper);
        if (nextNBCharIs(':')) {
          lower = upper;
          if (nextNBCharIs('*')) {
            upper = lower;
          } else
            upper = nextSpecificationExpression();
        }
      }

      bd = Bound.create(lower, upper);

      return bd;
    } finally {
      assert traceOut("nextRangeSpec", bd);
    }
  }

  private ArrayType nextArraySpec(Type elementType) throws InvalidException
  {
    assert traceIn("nextArraySpec", elementType);
    ArrayType at = null;
    try {
      Vector<Bound> dims = new Vector<Bound>();
      ArrayType     at0  = elementType.getCoreType().returnArrayType();
      if (at0 != null) {
        elementType = at0.getElementType();
        int l = at.getRank();
        for (int i = 0; i < l; i++)
          dims.add(at0.getIndex(i));
      }
      do {
        dims.add(nextRangeSpec());
      } while (nextNBCharIs(','));

      dims.reverse();
      at = FixedArrayType.create(dims, elementType);
      return at;
    } finally {
      assert traceOut("nextArraySpec", at);
    }
  }

  /**
   * Create the type for the structure needed for an allocatable array
   * of the specified type.
   */
  private Type buildAllocType(Type type)
  {
    ArrayType at    = type.getCoreType().returnArrayType();
    int       rank  = at.getRank();
    Type      et    = at.getElementType();

    if (allocTypes == null)
      allocTypes = new HashMap<ArrayType, Type>(23);

    if (dimType == null) {
      Vector<FieldDecl> df = new Vector<FieldDecl>(3);

      df.add(new FieldDecl("size",  size_t_type));
      df.add(new FieldDecl("last",  size_t_type));
      df.add(new FieldDecl("first", size_t_type));

      Type     dim = RecordType.create(df);
      TypeName tm  = new TypeName("_tndim_", dim);
      cg.addRootSymbol(tm);
      dimType = RefType.create(dim, tm);
      tm.setType(dimType);
    }

    Type t = allocTypes.get(at);
    if (t == null) {
      ArrayType         dt     = FixedArrayType.create(0, rank - 1, dimType);
      Type              ptype  = PointerType.create(et);
      Vector<FieldDecl> fields = new Vector<FieldDecl>(11);

      fields.add(new FieldDecl("rank",   char_type));
      fields.add(new FieldDecl("alloc",  char_type));
      fields.add(new FieldDecl("type",   char_type));
      fields.add(new FieldDecl("status", char_type));
      fields.add(new FieldDecl("elsize", size_t_type));
      fields.add(new FieldDecl("ptr",    ptype));
      fields.add(new FieldDecl("unused", size_t_type));
      fields.add(new FieldDecl("base",   ptype));
      fields.add(new FieldDecl("dims",   dt));

      Type st = RecordType.create(fields);
      st.memorySize(Machine.currentMachine);

      StringBuffer buf = new StringBuffer("_st_");
      buf.append(rank);
      buf.append("_");
      buf.append(allocTypesCnt++);

      TypeName tn = new TypeName(buf.toString(), st);
      cg.addRootSymbol(tn);
      st = RefType.create(st, tn);
      tn.setType(st);

      t = AllocArrayType.create(rank, st, et);

      allocTypes.put(at, t);
    }

    return t;
  }

  /**
   *
   */
  private int determineAllocType(Type type)
  {
    ArrayType at = type.getCoreType().returnArrayType();
    if (at != null)
      type = at.getElementType();

    type = type.getCoreType();
    int s = type.memorySizeAsInt(Machine.currentMachine);
    if (type.isIntegerType()) {
      if (!type.isSigned())
        return TYLOGICAL;
      switch (s) {
      case 1: return TYCHAR;
      case 2: return TYSHORT;
      case 4: return TYINT1;
      case 8: return TYQUAD;
      }
    } else if (type.isComplexType()) {
      switch (s) {
      case 8: return TYCOMPLEX;
      case 16: return TYDCOMPLEX;
      }
    } else if (type.isRealType()) {
      switch (s) {
      case 4: return TYREAL;
      case 8: return TYDREAL;
      }
    } else if (type.isFortranCharType() || (type == char_type)) {
      return TYCHAR;
    }
    return TYUNKNOWN;
  }

  /**
   *
   */
  private void initAllocArray(VariableDecl vd, Type type)
  {
    ArrayType           at    = type.getCoreType().returnArrayType();
    Type                et    = at.getElementType();
    int                 rank  = at.getRank();
    Vector<Object>      v     = new Vector<Object>(3 * rank + 8);
    AggregationElements init  = new AggregationElements(vd.getCoreType(), v);
    int                 ty    = determineAllocType(et);

    v.add(LiteralMap.put(rank, char_type));
    v.add(LiteralMap.put(0,    char_type));
    v.add(LiteralMap.put(ty,   char_type));
    v.add(LiteralMap.put(0,    char_type));
    v.add(LiteralMap.put(0,    size_t_type));
    v.add(LiteralMap.put(0,    voidp_type));
    v.add(LiteralMap.put(0,    size_t_type));
    v.add(LiteralMap.put(0,    voidp_type));

    for (int i = 0; i < 3 * rank; i++)
      v.add(LiteralMap.put(0, size_t_type));

    vd.setValue(init);
  }

  private Declaration nextEntityDecl(Type    type,
                                     boolean isChar,
                                     boolean isConst,
                                     boolean isAllocatable) throws InvalidException
  {
    assert traceIn("nextEntityDecl", type);
    VariableDecl vd = null;
    try {
      String name = nextIdentifier();

      if (nextNBCharIs('(')) {
        type = nextArraySpec(type);
        nextNBCharMustBe(')');
      }

      if (isChar && nextNBCharIs('*')) {
        if (nextNBCharIs('(')) {
          Type et = nextCharLenParamValue();
          nextNBCharMustBe(')');
          FixedArrayType at = type.getCoreType().returnFixedArrayType();
          if (at != null)
            type = at.copy(et);
          else
            type = et;
        } else {
          IntLiteral lit = nextIntegerConstantExpression();
          if (lit == null) {
            userError(Msg.MSG_Invalid_statement, null);
            return vd;
          }

          Type           et = getFortranCharType(lit.getLongValue());
          FixedArrayType at = type.getCoreType().returnFixedArrayType();
          if (at != null) {
            type = at.copy(et);
          } else
            type = et;
        }
      }

      Literal init = null;
      if (!isAllocatable && nextNBCharIs('='))
        init = nextConstantExpression(type);

      Declaration decl = lookupDecl(name);
      if (decl == null) {
        Object o = nameMap.get(name);
        if ((o != null) && !(o instanceof Declaration)) {
          userError(Msg.MSG_s_is_not_a_variable, name);
          return null;
        }
        decl = (Declaration) o;
        if (decl == null) {
          Type at = type;
          if (isAllocatable)
            type = buildAllocType(type);

          if (isConst)
            type = RefType.create(type, RefAttr.Const);

          vd = new VariableDecl(name, type, init);
          nameMap.put(name, vd);
          if (type.isArrayType() && !vd.isFormalDecl())
            vd.setResidency(Residency.MEMORY);

          if (isAllocatable)
            initAllocArray(vd, at);

          return vd;
        }

        RoutineDecl rd = decl.returnRoutineDecl();
        if (rd != null) {
          vd = new VariableDecl(name, type);
          if (rd.visibility() == Visibility.EXTERN)
            vd.setVisibility(Visibility.EXTERN);
          addSymbol(vd);
          if (type.isArrayType() && !vd.isFormalDecl())
            vd.setResidency(Residency.MEMORY);
          return vd;
        }
      }

      vd = decl.returnVariableDecl();
      if (vd == null) {
        if (init != null)
          userError(Msg.MSG_s_is_not_a_variable, name);
        else if (decl instanceof ProcedureDecl) {
          ProcedureType pt = ((ProcedureDecl) decl).getSignature();
          if (!type.getCoreType().equivalent(pt.getReturnType().getCoreType()))
            userError(Msg.MSG_s_is_not_a_variable, name);
        }
        return decl;
      }

      if (vd.isFormalDecl()) {
        type = PointerType.create(type);
        if (getStringLength(type) >= 0) {
//           if (ty != char_type)
//             ((FormalDecl) vd).setMode(ParameterMode.VALUE);
          if (actualFormals != null) {
            FormalDecl fd = createFormalDecl(vd.getName() + "len", int_type, ParameterMode.VALUE, 0);
            fctMap.put(vd, fd);
            actualFormals.add(fd);
          }
        }
      }

      Type           vt  = vd.getCoreType();
      FixedArrayType at1 = vt.returnFixedArrayType();
      if (at1 != null) {
        Type et1 = at1.getElementType();

        if (et1.equivalent(type))
          return vd;

        ArrayType at2  = type.getCoreType().returnArrayType();
        if (at2 != null) {
          type = joinArrayTypes(at1, at2);
        } else
          type = at1.copy(type);
      }

      if ((vt.isArrayType() || vt.isFortranCharType()) && !vd.isFormalDecl())
        vd.setResidency(Residency.MEMORY);

      Type at = type;
      if (isAllocatable)
        type = buildAllocType(type);

      if (isConst)
        type = RefType.create(type, RefAttr.Const);

      vd.setType(type);
      if (vd == resultVar) {
        RoutineDecl rd = cg.getSymbolTable().findRoutine(vd.getName());
        if (rd != null)
          rd.setType(rd.getSignature().copy(vd.getType()));
      }

      if (isAllocatable)
        initAllocArray(vd, at);

      return vd;
    } finally {
      assert traceOut("nextEntityDecl", vd);
    }
  }

  /**
   * This routine is probably not effective anymore since we created
   * the FortranCharType.
   */
  private Type joinArrayTypes(ArrayType at1, ArrayType at2)
  {
    int           r1   = at1.getRank();
    int           r2   = at2.getRank();
    Vector<Bound> dims = new Vector<Bound>(r1 + r2);
    Type          et2  = at2.getElementType();

    for (int i = 0; i < r1; i++)
      dims.add(at1.getIndex(i));

    for (int i = 0; i < r2; i++)
      dims.add(at2.getIndex(i));

    return FixedArrayType.create(dims, et2);
  }

  private Statement nextTypeDeclStmt(int keyword)
  {
    assert traceIn("nextTypeDeclStmt", null);
    Statement stmt = null;
    try {
      Type type = nextKindSelector(keyword);

      int parameterFlg   = 0;
      int allocatableFlg = 0;
      int intrinsicFlg   = 0;
      int optionalFlg    = 0;
      int pointerFlg     = 0;
      int saveFlg        = 0;
      int targetFlg      = 0;
      int inout          = 0;
      int vis            = 0;
      int visFlg         = 0;
      int inoutFlg       = 0;

      boolean error  = false;
      boolean isChar = keyword == Keywords.CHARACTER;

      if (allowF90Features || allowF95Features) {
        if (nextNBCharIs(',')) {
          while (true) {
            keyword = nextKeyword();
            switch (keyword) {
            case Keywords.PARAMETER:   parameterFlg++;    break;
            case Keywords.PUBLIC:      visFlg++; vis = 1; break;
            case Keywords.PRIVATE:     visFlg++; vis = 2; break;
            case Keywords.EXTERNAL:    visFlg++; vis = 3; break;
            case Keywords.ALLOCATABLE: allocatableFlg++;  break;
            case Keywords.INTRINSIC:   intrinsicFlg++;    break;
            case Keywords.OPTIONAL:    optionalFlg++;     break;
            case Keywords.POINTER:     pointerFlg++;      break;
            case Keywords.SAVE:        saveFlg++;         break;
            case Keywords.TARGET:      targetFlg++;       break;
            case Keywords.INTENT:      inoutFlg++;
              nextNBCharMustBe('(');
              keyword = nextKeyword();
              if (keyword == Keywords.IN)
                inout = 1;
              else if (keyword == Keywords.OUT)
                inout = 2;
              else if (keyword == Keywords.INOUT)
                inout = 3;
              else
                userError(Msg.MSG_Invalid_statement, "INTENT error");
              nextNBCharMustBe(')');
              break;
            case Keywords.DIMENSION:
              nextNBCharMustBe('(');
              type = nextArraySpec(type);
              nextNBCharMustBe(')');
              break;
            default: error = true; break;
            }
            if (nextNBCharIs(':'))
              break;
            nextNBCharMustBe(',');
          }
          nextNBCharMustBe(':');
        } else {
          if (nextNBCharIs(':'))
            nextNBCharMustBe(':');
        }
      }

      error |= parameterFlg > 1;
      error |= allocatableFlg > 1;
      error |= intrinsicFlg > 1;
      error |= optionalFlg > 1;
      error |= pointerFlg > 1;
      error |= saveFlg > 1;
      error |= targetFlg > 1;
      error |= visFlg > 1;
      error |= inoutFlg > 1;

      if (error)
        userError(Msg.MSG_Invalid_statement, "");

      while (true) {
        Declaration decl = nextEntityDecl(type, isChar, parameterFlg > 0, allocatableFlg > 0);
        if (saveFlg > 0)
          decl.setResidency(Residency.MEMORY);
        if (vis == 3)
          decl.setVisibility(Visibility.EXTERN);

        assert trace(null, decl);

        // We don't actually *define* it until it is used.
        // For example does
        //   INTEGER X
        // define an integer variable or the return type of an
        // integer function?

        nameMap.put(decl.getName(), decl);

        if (!nextNBCharIs(','))
          break;
      }

      //      notImplementedError("type-decl-stmt");

      return null;
    } catch (InvalidException ex) {
      if (classTrace)
        ex.printStackTrace();
      return null;
    } finally {
      assert traceOut("nextTypeDeclStmt", stmt);
    }
  }

  private Statement nextContainsStmt()
  {
    assert traceIn("nextContainsStmt", null);
    Statement stmt = null;
    try {
      notImplementedError("contains stmt");
      return new NullStmt();
    } catch (InvalidException ex) {
      return new NullStmt();
    } finally {
      assert traceOut("nextContainsStmt", stmt);
    }
  }

  private Statement nextParameterStmt()
  {
    assert traceIn("nextParameterStmt", null);
    Statement stmt = null;
    try {
      nextNBCharMustBe('(');

      do {
        String      name = nextIdentifier();
        Declaration decl = lookupDecl(name);
        if (decl != null) {
          userError(Msg.MSG_Parameter_s_already_defined, name);
          return null;
        }

        Object o = nameMap.get(name);
        if (o != null) {
          if (!(o instanceof VariableDecl) || (((VariableDecl) o).getValue() != null)) {
            userError(Msg.MSG_Parameter_s_already_defined, name);
            return null;
          }
        }
        decl = (Declaration) o;

        Type type = null;
        if (decl != null)
          type = decl.getType();
        else
          type = determineTypeFromName(name, void_type);

        nextNBCharMustBe('=');

        Literal lit  = nextConstantExpression(type);
        if (lit != null)
          nameMap.put(name, lit);

        assert trace(name, lit);
      } while (nextNBCharIs(','));

      nextCharMustBe(')');
      return null;
    } catch (InvalidException ex) {
      return null;
    } finally {
      assert traceOut("nextParameterStmt", stmt);
    }
  }

  private Type nextKindSelector(int keyword) throws InvalidException
  {
    assert traceIn("nextKindSelector", null);
    Type type = null;
    try {
      switch (keyword) {
      case Keywords.CHARACTER:       type = nextCharKindSelector();    break;
      case Keywords.TYPE:            type = nextUserType();            break;
      case Keywords.INTEGER:         type = nextIntKindSelector();     break;
      case Keywords.REAL:            type = nextRealKindSelector();    break;
      case Keywords.DOUBLEPRECISION: type = double_type;               break;
      case Keywords.DOUBLE:          type = double_type;               break;
      case Keywords.COMPLEX:         type = nextComplexKindSelector(); break;
      case Keywords.DOUBLECOMPLEX:   type = double_complex_type;       break;
      case Keywords.LOGICAL:         type = nextLogicalKindSelector(); break;
      default:
        userError(Msg.MSG_Incorrect_type_specification, null);
      }
      return type;
    } finally {
      assert traceOut("nextKindSelector", type);
    }
  }

  private Type nextUserType()
  {
    return int_type;
  }

  private Type nextCharLenParamValue() throws InvalidException
  {
    assert traceIn("nextCharLenParamValue", null);
    Type at = null;
    try {
      if (nextNBCharIs('*')) {
        at = getFortranCharType(0);
        return at;
      }

      IntLiteral lit = nextIntegerConstantExpression();
      if (lit == null)
        return null;

      at = getFortranCharType(lit.getLongValue());
      return at;
    } finally {
      assert traceOut("nextCharLenParamValue", at);
    }
  }

  private Type nextCharKindSelector() throws InvalidException
  {
    assert traceIn("nextCharKindSelector", null);
    Type type = null;
    int  sc   = column;
    try {
      if (nextNBCharIs('*')) {
        if (nextNBCharIs('(')) {
          type = nextCharLenParamValue();
          nextNBCharMustBe(')');
          return type;
        }

        skipBlanks();
        char c = statement[column];
        if ((c < '0') || (c > '9'))
          userError(Msg.MSG_Not_an_integer_constant, null);

        long length = nextInteger();
        type = getFortranCharType(length);
        return type;
      }

      if (nextNBCharIs('(')) {
        Type et = null;

        do {
          int keyword = nextKeyword();
          if (keyword == Keywords.LEN) {
            if (type != null)
              userError(Msg.MSG_Invalid_KIND_selector, null);
            type = nextCharLenParamValue();
          } else if (keyword == Keywords.KIND) {
            if (type != null)
              userError(Msg.MSG_Invalid_KIND_selector, null);
            et = nextIntKindSelector();
          } else {
            if ((type != null) || (et != null))
              userError(Msg.MSG_Invalid_KIND_selector, null);
            type = nextCharLenParamValue();
          }
        } while (nextNBCharIs(','));

        nextNBCharMustBe(')');

        if (type == null)
          userError(Msg.MSG_Invalid_KIND_selector, null);

        if (et == null)
          return type;

        ArrayType at = type.getCoreType().returnArrayType();
        if (at.getElementType().equivalent(et))
          return type;

        type = at.copy(et);
        return type;
      }

      type = getFortranCharType(1);
      return type;
    } finally {
      assert traceOut("nextCharKindSelector", type);
    }
  }

  private Type nextIntKindSelector() throws InvalidException
  {
    assert traceIn("nextIntKindSelector", null);
    Type type = null;
    int  sc   = column;
    try {
      int x = nextKindInt();
      if (x < 0) {
        switch (-x) {
        case 1: type = intTypeArray[1]; return type;
        case 2: type = intTypeArray[2]; return type;
        case 4: type = intTypeArray[3]; return type;
        case 8: type = intTypeArray[4]; return type;
        }
        userError(Msg.MSG_Invalid_KIND_selector, null);
      }

      if (x >= intTypeArray.length)
        userError(Msg.MSG_Invalid_KIND_selector, null);

      type = intTypeArray[x];
      return type;
    } finally {
      assert traceOut("nextIntKindSelector", type);
    }
  }

  private Type nextLogicalKindSelector() throws InvalidException
  {
    assert traceIn("nextLogicalKindSelector", null);
    Type type = null;
    int  sc   = column;
    try {
      int x = nextKindInt();
      if (x < 0) {
        switch (-x) {
        case 1: type = logicalTypeArray[1]; return type;
        case 2: type = logicalTypeArray[2]; return type;
        case 4: type = logicalTypeArray[3]; return type;
        case 8: type = logicalTypeArray[4]; return type;
        }
        userError(Msg.MSG_Invalid_KIND_selector, null);
      }

      if (x >= logicalTypeArray.length)
        userError(Msg.MSG_Invalid_KIND_selector, null);

      type = logicalTypeArray[x];
      return type;
    } finally {
      assert traceOut("nextLogicalKindSelector", type);
    }
  }

  private Type nextRealKindSelector() throws InvalidException
  {
    assert traceIn("nextRealKindSelector", null);
    Type type = null;
    int  sc   = column;
    try {
      int x = nextKindInt();
      if (x < 0) {
        switch (-x) {
        case 4: type = real_type; return type;
        case 8: type = double_type; return type;
        }
        userError(Msg.MSG_Invalid_KIND_selector, null);
      }

      if (x >= floatTypeArray.length)
        userError(Msg.MSG_Invalid_KIND_selector, null);

      type = floatTypeArray[x];
      return type;
    } finally {
      assert traceOut("nextRealKindSelector", type);
    }
  }

  private Type nextComplexKindSelector() throws InvalidException
  {
    assert traceIn("nextComplexKindSelector", null);
    Type type = null;
    int  sc   = column;
    try {
      if (!cmplxTypesDefed)
        defComplexFtns();

      int x = nextKindInt();
      if (x < 0) {
        switch (-x) {
        case 8: type = float_complex_type; return type;
        case 16: type = double_complex_type; return type;
        }
        userError(Msg.MSG_Invalid_KIND_selector, null);
      }

      if (x >= logicalTypeArray.length)
        userError(Msg.MSG_Invalid_KIND_selector, null);

      type = complexTypeArray[x];
      return type;
    } finally {
      assert traceOut("nextComplexKindSelector", type);
    }
  }

  /**
   * Define those functions needed at runtime by generated C code.  We
   * do this just in case we generate C code as output.  Otherwise, it
   * is not needed.
   */
  private void defComplexFtns()
  {
    cmplxTypesDefed = true;
    Vector<FieldDecl> cfields = new Vector<FieldDecl>(2);
    cfields.add(new FieldDecl("r", real_type));
    cfields.add(new FieldDecl("i", real_type));
    RecordType ctc = RecordType.create(cfields);
    TypeName   tdc = new TypeName("complex", ctc);
    tdc.setType(RefType.create(ctc, tdc));
    cg.addRootSymbol(tdc);

    Vector<FieldDecl> dfields = new Vector<FieldDecl>(2);
    dfields.add(new FieldDecl("r", double_type));
    dfields.add(new FieldDecl("i", double_type));
    RecordType ctd = RecordType.create(dfields);
    TypeName   tdd = new TypeName("doublecomplex", ctd);
    tdd.setType(RefType.create(ctd, tdd));
    cg.addRootSymbol(tdd);

    for (int i = 0; i < complexFtns.length; i += 2)
      defPreKnownFtn(complexFtns[i], complexFtns[i + 1], RoutineDecl.PURE);
  }

  private int nextKindInt() throws InvalidException
  {
    assert traceIn("nextKindInt", null);
    long kind = 0;
    try {
      if (nextCharIs('*')) {
        skipBlanks();
        char c = statement[column];
        if ((c < '0') || (c > '9'))
          userError(Msg.MSG_Not_an_integer_constant, null);

        kind = -nextInteger();
        return (int) kind; //***** Forgot this, didn't you!
      }

      int sc = column;
      if (!nextNBCharIs('('))
        return 0;

      int        keyword = nextKeyword();
      IntLiteral lit     = null;
      if (keyword == Keywords.KIND) {
        nextNBCharMustBe('=');
        lit = nextIntegerConstantExpression();
        if (lit == null) {
          column = sc;
          return 0;
        }
        kind = lit.getLongValue();
      } else {
        if (keyword != Keywords.NA)
          userError(Msg.MSG_Invalid_KIND_selector, null);

        lit = nextIntegerConstantExpression();
        if (lit == null) {
          column = sc;
          return 0;
        }
        kind = lit.getLongValue();
      }

      nextNBCharMustBe(')');

      if ((kind < 0) || (kind > 5))
        userError(Msg.MSG_Invalid_KIND_selector, null);
      return (int) kind;
    } finally {
      assert traceOut("nextKindInt", Long.toString(kind));
    }
  }

  private Statement nextImplicitStmt()
  {
    assert traceIn("nextImplicitStmt", null);
    Statement stmt = null;
    try {
      int sc      = column;
      int keyword = nextKeyword();
      if (keyword == Keywords.NONE) {
        implicitTypes = null;
        return null;
      }

      column = sc;

      do {
        keyword = nextKeyword();
        if (keyword == Keywords.DOUBLE) {
          keyword = nextKeyword();
          if (keyword == Keywords.PRECISION)
            keyword = Keywords.DOUBLEPRECISION;
          else if (keyword == Keywords.COMPLEX)
            keyword = Keywords.DOUBLECOMPLEX;
        }

        Type type = nextKindSelector(keyword);
        nextLetterSpecList(type);
      } while (nextNBCharIs(','));

      if (actualFormals != null) {
        int l = actualFormals.size();
        for (int i = 0; i < l; i++) {
          Declaration decl = (Declaration) actualFormals.get(i);
          Type        type = PointerType.create(determineTypeFromName(decl.getName(), null));
          decl.setType(type);
        }
      }

      return null;
    } catch (InvalidException ex) {
      return null;
    } finally {
      assert traceOut("nextImplicitStmt", stmt);
    }
  }

  private void nextLetterSpecList(Type type) throws InvalidException
  {
    assert traceIn("nextLetterSpecList", null);
    try {
      nextNBCharMustBe('(');

      do {
        skipBlanks();
        char b = statement[column++];
        if ((b < 'a') || (b > 'z'))
          userError(Msg.MSG_Expecting_s_found_s, "letter", "'" + b + "'");

        skipBlanks();

        char e = b;
        if (nextCharIs('-')) {
          skipBlanks();
          e = statement[column++];
          if ((e < 'a') || (e > 'z'))
            userError(Msg.MSG_Expecting_s_found_s, "letter", "'" + e + "'");
        }

        if ((e < b) || (e > 'z'))
          userError(Msg.MSG_Expecting_s_found_s, "letter>'" + b + "'", "'" + e + "'");

        if (implicitTypes == null)
          initImplicitTypes();

        for (int i = b; i <= e; i++)
          implicitTypes[i - 'a'] = type;

      } while(nextNBCharIs(','));

      nextCharMustBe(')');
    } finally {
      assert traceOut("nextLetterSpecList", null);
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

  private Statement nextFormatStmt()
  {
    assert traceIn("nextFormatStmt", null);
    Statement stmt = null;
    try {
      nextNBCharMustBe('(');

      // Find enclosing parens.

      int leftParen  = column - 1;
      int rightparen = leftParen;
      while (true) {
        char c = statement[column];
        if (c == 0)
          break;

        if (c == ')')
          rightparen = column;

        column++;
      }

      column = rightparen + 1;

      if ((rightparen <= leftParen) || (statementLabel == null)) {
        userError(Msg.MSG_Invalid_statement, "nextFormatStmt");
        return null;
      }

      // The f2c expects the format string to include the enclosing
      // parens.

      String       text  = new String(statement, leftParen, rightparen - leftParen + 1);
      Type         type  = RefType.create(makeCharArrayType(text, char_type), RefAttr.Const);
      VariableDecl fdecl = formatMap.get(statementLabel);

      if (fdecl == null) {
        fdecl = new VariableDecl(statementLabel.getName(), type);
        fdecl.setResidency(Residency.MEMORY);
        addSymbol(fdecl);
        formatMap.put(statementLabel, fdecl);
      } else
        fdecl.setType(type);

      fdecl.setValue(LiteralMap.put(text, type));

      assert trace(null, fdecl);

      return null;
    } catch (InvalidException ex) {
      return null;
    } finally {
      assert traceOut("nextFormatStmt", stmt);
    }
  }

  private Statement nextEntryStmt()
  {
    assert traceIn("nextEntryStmt", null);
    Statement stmt = null;
    try {
      String ename = nextIdentifier();
      if (ename == null) {
        reportError(Msg.MSG_Invalid_statement, "");
        fatalError = true;
        return null;
      }

      try {
        actualFormals = nextFormals(true);
      } catch (InvalidException ex) {
        System.out.println("** ss " + ex.getMessage());
        fatalError = true;
        reportError(Msg.MSG_Invalid_statement, "");
        cg.getSymbolTable().endScope();
        return null;
      }

      Type    rt    = currentFunction.getSignature().getReturnType();
      boolean isFtn = true;
      Type    nrt   = curFtnIsSub ? void_type : rt;
      if (curFtnIsSub) {
        isFtn = false;
        if ((actualFormals != null) && (actualFormals.size() > 0) && (actualFormals.get(0) == null)) {
          actualFormals.remove(0);
          nrt = int_type;
          currentFunction.setType(currentFunction.getSignature().copy(int_type));
        }
      }

      RoutineDecl rd = buildProcedureDecl(ename, nrt, actualFormals, lineNumber, column);

      assert trace(null, rd);

      entries.add(rd);

      if (isFtn) {
        if (rt.isFortranCharType())
          resultVar = rd.getSignature().getFormal(0);
        else {
          resultVar = new VariableDecl(ename, rt);
          addSymbol(resultVar);
        }
      } else
        resultVar = null;


      LabelDecl lab = new LabelDecl(ename);
      entries.add(lab);
      addSymbol(lab);
      stmt = new LabelStmt(lab, new NullStmt());
      return stmt;
    } finally {
      assert traceOut("nextEntryStmt", stmt);
    }
  }

  private Statement nextInterfaceBlock()
  {
    assert traceIn("nextInterfaceBlock", null);
    Statement stmt = null;
    try {
      notImplementedError("interface-block");
      return null;
    } catch (InvalidException ex) {
      return null;
    } finally {
      assert traceOut("nextInterfaceBlock", stmt);
    }
  }

  private Statement nextPublicStmt()
  {
    assert traceIn("nextPublicStmt", null);
    Statement stmt = null;
    try {
      notImplementedError("nextPublicStmt");
      return null;
    } catch (InvalidException ex) {
      return null;
    } finally {
      assert traceOut("nextPublicStmt", stmt);
    }
  }

  private Statement nextPrivateStmt()
  {
    assert traceIn("Statement nextPrivateStmt", null);
    Statement stmt = null;
    try {
      notImplementedError("nextPrivateStmt");
      return null;
    } catch (InvalidException ex) {
      return null;
    } finally {
      assert traceOut("Statement nextPrivateStmt", stmt);
    }
  }

  private Statement nextAllocatableStmt()
  {
    assert traceIn("nextAllocatableStmt", null);
    Statement stmt = null;
    try {
      notImplementedError("nextAllocatableStmt");
      return null;
    } catch (InvalidException ex) {
      return null;
    } finally {
      assert traceOut("nextAllocatableStmt", stmt);
    }
  }

  private Statement nextCommonStmt()
  {
    assert traceIn("nextCommonStmt", null);
    Statement stmt = null;
    try {
      outer:
      while (true) {
        String name = blankCommonName;
        if (nextNBCharIs('/')) {
          String name2 = nextIdentifier();
          nextNBCharMustBe('/');
          if (name2 != null)
            name = name2;
        }

        VariableDecl        base = null;
        Vector<SymtabEntry> ents = cg.getSymbolTable().lookupSymbol(name);
        if (ents != null) {
          SymtabScope              root = cg.getSymbolTable().getRootScope();
          Enumeration<SymtabEntry> en   = ents.elements();
          while (en.hasMoreElements()) {
            SymtabEntry s    = en.nextElement();
            Declaration decl = s.getDecl();

            if (s.getScope() == root) {
              VariableDecl vd = decl.returnVariableDecl();
              if (vd != null) {
                base = vd;
                break;
              }
              reportError(Msg.MSG_Symbol_s_already_defined, name);
              fatalError = true;
              return null;
            }
          }
        }

        if (base == null) {
          base = new VariableDecl(name, int_type);
          base.setReferenced();
          base.setVisibility(Visibility.GLOBAL);
          base.setResidency(Residency.MEMORY);
          base.specifyCommonBaseVariable();
          cg.addRootSymbol(base);
          cg.addTopLevelDecl(base);
        }

        while (true) {
          EquivalenceDecl vd = nextEqVariable(base);
          if (vd == null)
            break;

          if (nextNBCharIs('(')) {
            Type type = nextArraySpec(vd.getType());
            nextNBCharMustBe(')');
            vd.setType(type);
            vd.setResidency(Residency.MEMORY);
          }

          Vector<EquivalenceDecl> vars = commonVars.get(base);
          if (vars == null) {
            vars = new Vector<EquivalenceDecl>();
            commonVars.put(base, vars);
          }
          vars.add(vd);

          skipBlanks();

          if (statement[column] == '/')
            continue outer;

          if (nextCharIs(',')) {
            skipBlanks();
            if (statement[column] == '/')
              continue outer;
            continue;
          }

          break;
        }

        break;
      }

      return null;
    } catch (InvalidException ex) {
      return null;
    } finally {
      assert traceOut("nextCommonStmt", stmt);
    }
  }

  private ImpliedDo nextImpliedDo() throws InvalidException
  {
    assert traceIn("nextImpliedDo", null);
    ImpliedDo did = new ImpliedDo(this, lineNumber);
    try {
      VariableDecl indexVar = null;
      do {
        if (nextNBCharIs('(')) {
          did.add(nextImpliedDo());
          nextNBCharMustBe(')');
        } else {
          Expression item = nextArrayExpr();
          if (nextNBCharIs('=')) {
            if ((item instanceof IdValueOp) && item.getType().isIntegerType()) {
              IdValueOp idvop = (IdValueOp) item;
              indexVar = (VariableDecl) idvop.getDecl();
              break;
            }
            userError(Msg.MSG_Not_an_integer_value, null);
          }
          did.add(item);
        }
      } while (nextNBCharIs(','));

      Expression init = nextExpression(SCALAR_EXPR);

      nextNBCharMustBe(',');

      Expression limit = nextExpression(SCALAR_EXPR);
      Expression step  = one;

      if (nextNBCharIs(','))
        step = nextExpression(SCALAR_EXPR);

      did.set(indexVar, init, limit, step);
      return did;
    } finally {
      assert traceOut("nextImpliedDo", did);
    }
  }

  private Statement nextDataStmt()
  {
    assert traceIn("nextDataStmt", null);
    Statement stmt = null;
    try {
      ImpliedDo did = new ImpliedDo(this, lineNumber);
      while (true) {
        do {
          if (nextNBCharIs('(')) {
            did.add(nextImpliedDo());
            nextNBCharMustBe(')');
          } else {
            did.add(makeLValue(nextPrimaryExpr(SCALAR_EXPR)));
          }

          skipBlanks();
          if (statement[column] == '/')
            break;
        } while (nextNBCharIs(','));

        did.set(null, one, one, one);

        nextNBCharMustBe('/');

        do {
          long    repeat = 1;
          Literal value  = nextConstantPrimaryExpr();
          if (nextNBCharIs('*')) {
            if (value instanceof IntLiteral) {
              repeat = ((IntLiteral) value).getLongValue();
              if (repeat < 0)
                userError(Msg.MSG_Not_an_integer_constant, null);
              value = nextConstantPrimaryExpr();
            }
          }
          did.addData(value, repeat);
        } while (nextNBCharIs(','));

        nextNBCharMustBe('/');

        if (statement[column] == 0)
          break;

        nextNBCharIs(',');
      }

      if (!fatalError)
        did.initVariables(inBlockData);
      return null;
    } catch (InvalidException ex) {
      return null;
    } finally {
      assert traceOut("nextDataStmt", stmt);
    }
  }

  private Statement nextDimensionStmt()
  {
    assert traceIn("nextDimensionStmt", null);
    Statement stmt = null;
    try {
      do {
        VariableDecl vd = nextVariable();

        nextNBCharMustBe('(');

        Type type = vd.getType();
        if (vd instanceof FormalDecl)
          type = type.getCoreType().getPointedTo();

        type = nextArraySpec(type);

        nextNBCharMustBe(')');

        if (vd instanceof FormalDecl)
          type = PointerType.create(type);

        vd.setType(type);
        if (!vd.isFormalDecl())
          vd.setResidency(Residency.MEMORY);

        assert trace(null, vd);

      } while (nextNBCharIs(','));

      return null;
    } catch (InvalidException ex) {
      return null;
    } finally {
      assert traceOut("nextDimensionStmt", stmt);
    }
  }

  /**
   * Return the offset in addressable memory uints specified for this
   * variable.  For
   * <pre>
   *  A(2,3)
   * </pre>
   * where A is a 3x3 array of floats, the result would be 44.
   */
  private long nextEqOffset(VariableDecl mv) throws InvalidException
  {
    assert traceIn("nextEqOffset", null);
    long eqOffset = 0;
    try {
      Type      mt = mv.getCoreType();
      ArrayType at = mt.returnArrayType();
      if ((at != null) && nextNBCharIs('(')) {
        int    k    = at.getRank();
        long[] subs = new long[k];

        do {
          if (k <= 0)
            userError(Msg.MSG_Too_many_subscripts, null);
          IntLiteral lit = nextIntegerConstantExpression();
          if (lit == null)
            userError(Msg.MSG_Not_an_integer_constant, null);
          subs[--k] = lit.getLongValue();
        } while (nextNBCharIs(','));

        nextNBCharMustBe(')');

        for (int i = 0; i < subs.length; i++) {
          Bound bd  = at.getIndex(i);
          long  min = bd.getConstMin();
          long  m   = bd.numberOfElements();
          eqOffset = (eqOffset * m) + (subs[i] - min);
        }
        eqOffset *= at.elementSize(Machine.currentMachine);
      }
      return eqOffset;
    } finally {
      assert traceOut("nextEqOffset", Long.toString(eqOffset));
    }
  }

  private Statement nextEquivalenceStmt()
  {
    assert traceIn("nextEquivalenceStmt", null);
    Statement stmt = null;
    try {
      do {
        nextNBCharMustBe('(');

        // Start a new equivalence set.

        currentEquivSet = new EquivSet(currentEquivSet);

        String mn = nextIdentifier();
        if (mn == null)
          userError(Msg.MSG_Invalid_statement, null);

        Declaration decl = lookupDecl(mn);
        if (decl == null) {
          Object o = nameMap.get(mn);
          if ((o != null) && !(o instanceof Declaration)) {
            userError(Msg.MSG_s_is_not_a_variable, mn);
            return null;
          }
          decl = (Declaration) o;
          if (decl != null) {
            if (decl.isVariableDecl())
              addSymbol(decl);
          }
        }

        VariableDecl mv = null;
        if (decl == null) {
          mv = new VariableDecl(mn, determineTypeFromName(mn, void_type));
          addSymbol(mv);
        } else {
          mv = decl.returnVariableDecl();
          if (mv == null) {
            userError(Msg.MSG_Variable_s_already_defined, mn);
            return null;
          }
        }

        long mo = nextEqOffset(mv);

        currentEquivSet.addEntry(mv, mo);

        nextNBCharMustBe(',');

        assert trace(null, mv);

        do  {
          String en = nextIdentifier();
          if (en == null)
            userError(Msg.MSG_Invalid_statement, null);
   
          decl = lookupDecl(en);
          if (decl == null) {
            Object o = nameMap.get(en);
            if ((o != null) && !(o instanceof Declaration))
              userError(Msg.MSG_s_is_not_a_variable, en);
            
            decl = (Declaration) o;
            if (decl != null) {
              if (decl.isVariableDecl())
                addSymbol(decl);
            }
          }

          VariableDecl ev = null;
          if (decl == null) {
            ev = new VariableDecl(en, determineTypeFromName(en, void_type));
            addSymbol(ev);
          } else {
            ev = decl.returnVariableDecl();
            if (ev == null) {
              userError(Msg.MSG_Variable_s_already_defined, en);
              return null;
            }
          }

          long eo = nextEqOffset(ev);

          currentEquivSet.addEntry(ev, eo);

          assert trace("  ", ev);
        } while (nextNBCharIs(','));

        nextNBCharMustBe(')');

      } while (nextNBCharIs(','));

      return null;
    } catch (InvalidException ex) {
      if (classTrace)
        ex.printStackTrace();
      return null;
    } finally {
      assert traceOut("nextEquivalenceStmt", stmt);
    }
  }

  private Statement nextExternalStmt()
  {
    assert traceIn("nextExternalStmt", null);
    Statement stmt = null;
    try {
      while (true) {
        String name = nextIdentifier();
        if (name == null)
          break;
        Declaration decl = lookupDecl(name);
        if (decl == null) {
          Object o = nameMap.get(name);
          if ((o != null) && !(o instanceof Declaration)) {
            reportError(Msg.MSG_s_is_not_a_variable, name);
            fatalError = true;
            return null;
          }
          decl = (Declaration) o;
          if (decl == null) {
            Type          rt = determineTypeFromName(name, void_type);
            ProcedureType pt = makeProcedureType(name, rt, null);
            decl = new ProcedureDecl(name, pt);
            nameMap.put(name, decl);
          }
        }

        VariableDecl vd = decl.returnVariableDecl();
        if ((vd != null) && !vd.isFormalDecl()) {
          Type ty = vd.getType();

          if (ty.isArrayType() || (vd.getValue() != null))
            userError(Msg.MSG_Symbol_s_already_defined, name);

          ProcedureType pt = makeProcedureType(name, ty, null);
          decl = new ProcedureDecl(name, pt);
          nameMap.put(name, decl);
          cg.getSymbolTable().replaceSymbol(vd, decl);
        }

        if (decl.isRoutineDecl())
          decl.setVisibility(Visibility.EXTERN);

        assert trace(null, decl);

        if (!nextNBCharIs(','))
          break;
      }
      return null;
    } catch (InvalidException ex) {
      return null;
    } finally {
      assert traceOut("nextExternalStmt", stmt);
    }
  }

  private Statement nextIntentStmt()
  {
    assert traceIn("nextIntentStmt", null);
    Statement stmt = null;
    try {
      notImplementedError("nextIntentStmt");
      return null;
    } catch (InvalidException ex) {
      return null;
    } finally {
      assert traceOut("nextIntentStmt", stmt);
    }
  }

  private Statement nextIntrinsicStmt()
  {
    assert traceIn("nextIntrinsicStmt", null);
    Statement stmt = null;
    try {
      while (true) {
        String name = nextIdentifier();
        if (name == null)
          break;

        Declaration decl = lookupDecl(name);

        boolean postit = false;
        if (decl != null)
          userError(Msg.MSG_Symbol_s_already_defined, name);

        ProcedureDecl pd = defIntrinsicFtn(name);
        if (pd == null)
          userError(Msg.MSG_Unknown_intrinsic_function_s, name);


        cg.addRootSymbol(pd);
        pd.setReferenced();

        assert trace(null, pd);

        Type pt = PointerType.create(pd.getSignature());
        VariableDecl vd = new VariableDecl(name, pt);
        vd.setValue(new AddressLiteral(pt, pd));
        addSymbol(vd);

        if (!nextNBCharIs(','))
          break;
      }
      return null;
    } catch (InvalidException ex) {
      return null;
    } finally {
      assert traceOut("nextIntrinsicStmt", stmt);
    }
  }

  private Statement nextNamelistStmt()
  {
    assert traceIn("nextNamelistStmt", null);
    Statement stmt = null;
    try {
      notImplementedError("nextNamelistStmt");
      return null;
    } catch (InvalidException ex) {
      return null;
    } finally {
      assert traceOut("nextNamelistStmt", stmt);
    }
  }

  private Statement nextOptionalStmt()
  {
    assert traceIn("nextOptionalStmt", null);
    Statement stmt = null;
    try {
      notImplementedError("nextOptionalStmt");
      return null;
    } catch (InvalidException ex) {
      return null;
    } finally {
      traceOut("nextOptionalStmt", stmt);
    }
  }

  private Statement nextPointerStmt()
  {
    assert traceIn("nextPointerStmt", null);
    Statement stmt = null;
    try {
      notImplementedError("nextPointerStmt");
      return null;
    } catch (InvalidException ex) {
      return null;
    } finally {
      assert traceOut("nextPointerStmt", stmt);
    }
  }

  private Statement nextSaveStmt()
  {
    assert traceIn("nextSaveStmt", null);
    Statement stmt = null;
    try {
      if (nextNBCharIs(':'))
        nextNBCharMustBe(':');

      do {
        if (nextNBCharIs('/')) {
          String name = nextIdentifier();
          nextNBCharMustBe('/');
        } else {
          String      name = nextIdentifier();
          if (name == null) {
            globalSaveFlag = true;
            return null;
          }

          Declaration decl = lookupDecl(name);
          if (decl == null) {
            Object o = nameMap.get(name);
            if ((o != null) && !(o instanceof Declaration)) {
              userError(Msg.MSG_s_is_not_a_variable, name);
              return null;
            }
            decl = (Declaration) o;
            if (decl == null) {
              decl = new VariableDecl(name, determineTypeFromName(name, void_type));
              nameMap.put(name, decl);
            }
          }
          decl.setResidency(Residency.MEMORY);
          decl.setVisibility(Visibility.LOCAL);
          decl.setReferenced();
          addSymbol(decl);

          assert trace(null, decl);
        }
      } while (nextNBCharIs(','));
      return null;
    } catch (InvalidException ex) {
      return null;
    } finally {
      assert traceOut("nextSaveStmt", stmt);
    }
  }

  private Statement nextTargetStmt()
  {
    assert traceIn("nextTargetStmt", null);
    Statement stmt = null;
    try {
      notImplementedError("nextTargetStmt");
      return null;
    } catch (InvalidException ex) {
      return null;
    } finally {
      assert traceOut("nextTargetStmt", stmt);
    }
  }

  private Statement nextAssignmentStmt()
  {
    assert traceIn("nextAssignmentStmt", null);
    Statement stmt = null;
    int       sc   = column;

    arrayExprType = null;    // All arrays in an array expression must conform to this.
    arrayExprSubscripts.clear(); // The subscripts to use for arrays in array expressions.

    try {
      Declaration decl = null;
      Expression  lhs  = null;
      Expression  rhs  = null;
      try {
        String name = nextIdentifier();
        if (name == null) {
          column = sc;
          return null;
        }

        boolean postit = false;
        decl = lookupDecl(name);
        if (decl == null) {
          // We haven't defined this symbol yet but we may have
          // encountered its type definition already.
          Object o = nameMap.get(name);
          if ((o != null) && !(o instanceof Declaration)) {
            column = sc;
            return null;
          }
          decl = (Declaration) o;
          postit = true;
        }

        if ((decl != null) && !decl.isVariableDecl()) {
          int sc2 = column;
          if ((decl.isRoutineDecl()) && nextNBCharIs('=')) {
            decl = null;
            column = sc2;
          } else {
            column = sc;
            return null;
          }
        }

        Type type = null;
        if (decl != null)
          type = decl.getCoreType().getPointedTo();

        if (nextNBCharIs('(')) {
          if (type == null) {
            column = sc;
            return null;
          }

          boolean   haveParen = true;
          ArrayType at        = type.getCoreType().returnArrayType();
          if (at != null) {
            Vector<Expression> args = nextSubscriptList();
            if (args == null) {
              column = sc;
              return null;
            }

            args.reverse();

            Type ty = getArraySubtype(at, args);
            if (ty == null) {
              column = sc;
              return null;
            }

            SubscriptAddressOp sop = new SubscriptAddressOp(PointerType.create(ty), fixVariableRef(decl), args);
            lhs = sop;
            sop.setFortranArray();
            type = ty;
            haveParen = nextNBCharIs('(');
          }

          if (haveParen && isFCharType(type)) {
            SeriesOp exp = nextSubstringSpec();
            if (exp == null) {
              column = sc;
              return null;
            }
            if (lhs == null)
              lhs = fixVariableRef(decl);
            Expression first = exp.getExpr1();
            Expression last  = exp.getExpr2();
            if ((first != one) || !(last instanceof NilOp)) {
              if (last instanceof NilOp)
                last = LiteralMap.put(getStringLength(type), int_type);
              lhs = new SubstringOp(lhs, first, last);
            }
            haveParen = false;
          }

          if (haveParen) {
            column = sc;
            return null;
          }
        }

        if (!nextCharIs('=')) {
          column = sc;
          return null;
        }


        if (type != null) {
          ArrayType at = type.getCoreType().returnArrayType();
          if (at != null) {
            int rank = at.getRank();
            for (int i = 0; i < rank; i++) {
              VariableDecl iVar = genTemp(int_type);
              addSymbol(iVar);
              iVar.setReferenced();
              arrayExprSubscripts.add(iVar);
            }
            arrayExprType = at;
          }
        }

        rhs = nextExpression(NORMAL_EXPR);
        if (rhs == null) {
          column = sc;
          return null;
        }

        if (!nextNBCharIsEOL()) {
          column = sc;
          return null;
        }

        if (lhs == null) {
          if (decl == null) {
            decl   = lookupDecl(name);
            postit = false;
            if (decl == null) {
              Object o = nameMap.get(name);
              if (o instanceof Literal) {
                System.out.println("*************** " + o);
                userError(Msg.MSG_Invalid_statement, "");
              }
              decl = (Declaration) o;
              if (decl == null)
                decl = new VariableDecl(name, determineTypeFromName(name, null));
              postit = true;
            }
          }
          if (decl instanceof ProcedureDecl) {
            decl = new VariableDecl(name, determineTypeFromName(name, null));
            postit = true;
          }

          lhs = fixVariableRef(decl);
        }

        if (postit)
          addSymbol(decl);
      } catch (InvalidException ex) {
        if (classTrace) {
          System.out.println("** nas " + ex.getMessage());
          ex.printStackTrace();
        }
        column = sc;
        return null;
      }

      // Definitely an assignment statement.

      decl.setReferenced();

      Type lt = lhs.getPointedToCore();
      Type rt = rhs.getPointedToCore();

      if (lt.isAtomicType() && rt.isAtomicType()) {
        if (rt.isPointerType() && !lt.isPointerType()) {
          rt = rt.getPointedTo();
          rhs = new DereferenceOp(rhs);
        }

        stmt = new EvalStmt(new AssignSimpleOp(makeLValue(lhs), cast(lt, rhs)));
        return stmt;
      }

      boolean bl = lt.isFortranCharType();
      boolean br = rt.isFortranCharType();

      if (bl || br) {
        stmt = nextFCAssignStmt(bl, lhs, br, rhs);
        if (stmt == null)
          column = sc;
        return stmt;
      }

      if (lt.isArrayType()) {
        stmt = nextArrayAssignStmt(lhs, rhs);
        if (stmt == null)
          column = sc;
        return stmt;
      }

      if (classTrace) {
        System.out.println("** nas " + lt);
        System.out.println("       " + rt);
        System.out.println("       " + lhs);
        System.out.println("       " + rhs);
        if (rhs instanceof IdReferenceOp)
          System.out.println("       " + ((IdReferenceOp) rhs).getDecl());
      }

      userError(Msg.MSG_Invalid_statement, "");
      return stmt;
    } catch (InvalidException ex) {
      if (classTrace)
        ex.printStackTrace();
      column = sc;
      return null;
    } finally {
      arrayExprType = null;
      assert traceOut("nextAssignmentStmt", stmt);
    }
  }

  private Statement nextArrayAssignStmt(Expression lhs,
                                        Expression rhs) throws InvalidException
  {
    assert traceIn("nextArrayAssignStmt", null);
    Statement stmt = null;
    try {
      ArrayType lat   = lhs.getPointedToCore().returnArrayType();
      Type      let   = lat.getElementType();
      Type      lpet  = PointerType.create(let);
      int       lrank = lat.getRank();
      long      lne   = lat.numberOfElements();

      Vector<Expression> subs = new Vector<Expression>();
      for (int i = 0 ; i < lrank; i++)
        subs.add(new IdValueOp(arrayExprSubscripts.get(i)));

      rhs = cast(let, rhs);

      if (lhs instanceof IdReferenceOp) {
        // X =

        if (lat.isFixedArrayType()) {
          // DIMENSION X(12)
          Expression         add  = lhs;
          SubscriptAddressOp sub  = new SubscriptAddressOp(lpet, add, subs);
          Statement          body = new EvalStmt(new AssignSimpleOp(sub, rhs));
          for (int i = lrank - 1; i >= 0; i--) {
            Expression last  = getArrayDimensionLast(lat, i, lhs);
            Expression first = getArrayDimensionStart(lat, i, lhs);
            Expression index = new IdValueOp(arrayExprSubscripts.get(i));
            body = new DoLoopStmt(subs.get(i), body, first, last, one);
          }
          sub.setFortranArray();
          return body;
        }

        // ALLOCATABLE :: X(:)

        AllocArrayType aat    = lat.returnAllocArrayType();
        RecordType     rty    = (RecordType) aat.getStruct().getCoreType();
        RecordType     tdim   = dimType.getCoreType().returnRecordType();
        Type           ptdim  = PointerType.create(dimType);
        FieldDecl      fsize  = tdim.findField("size");
        FieldDecl      fptr   = rty.findField("ptr");
        FieldDecl      fdims  = rty.findField("dims");
        Type           tfdims = fdims.getType();
        Vector<Expression> inds0  = new Vector<Expression>(1);
        FixedArrayType at     = buildFixedFromAlloc((AllocArrayType) arrayExprType, lhs);
        VariableDecl   ad     = genTemp(PointerType.create(at));
        Expression     padd   = cast(PointerType.create(at), new SelectOp(lhs, fptr), lineNumber, column);
        Expression     add    = new IdValueOp(ad);

        addSymbol(ad);
        ad.setReferenced();

        inds0.add(zero);

        Expression dims  = new SelectIndirectOp(lhs, fdims);
        Expression sub0  = new SubscriptAddressOp(ptdim, dims, inds0);
        Expression limit = new SelectOp(sub0, fsize);

        for (int i = 1; i < lrank; i++) {
          Vector<Expression> inds1 = new Vector<Expression>(1);
          inds1.add(LiteralMap.put(i, int_type));
          Expression sub1 = new SubscriptAddressOp(ptdim, dims, inds1);
          Expression sel1 = new SelectOp(sub1, fsize);
          limit = new MultiplicationOp(size_t_type, limit, sel1);
        }

        limit = new SubtractionOp(size_t_type, limit, one);

        BlockStmt bs = new BlockStmt();

        EvalStmt set = new EvalStmt(new AssignSimpleOp(genDeclAddress(ad), padd));
        bs.addStmt(set);//padd)));

        SubscriptAddressOp sub  = new SubscriptAddressOp(lpet, add, subs);
        sub.setFortranArray();
        Statement          body = new EvalStmt(new AssignSimpleOp(sub, rhs));
        for (int i = lrank - 1; i >= 0; i--) {
          Expression last  = getArrayDimensionLast(lat, i, lhs);
          Expression first = getArrayDimensionStart(lat, i, lhs);
          Expression index = new IdValueOp(arrayExprSubscripts.get(i));
          body = new DoLoopStmt(subs.get(i), body, first, last, one);
        }
        bs.addStmt(body);
        return bs;
      }

      if (lhs instanceof SubscriptAddressOp) {
        // X(:) =

        Expression subl = buildArrayElementRef(lat, let, makeLValue(lhs), subs);
        stmt  = new EvalStmt(new AssignSimpleOp(makeLValue(subl), rhs));

        for (int i = lrank - 1; i >= 0; i--) {
          Expression last  = getArrayDimensionLast(lat, i, lhs);
          Expression first = getArrayDimensionStart(lat, i, lhs);
          stmt = new DoLoopStmt(subs.get(i), stmt, first, last, one);
        }

        return stmt;
      }

      if (classTrace) {
        System.out.println("** naas " + lat);
        System.out.println("        " + lhs);
        System.out.println("        " + rhs);
        if (rhs instanceof IdReferenceOp)
          System.out.println("       " + ((IdReferenceOp) rhs).getDecl());
      }
      userError(Msg.MSG_Invalid_statement, "");
      return stmt;
    } finally {
      assert traceOut("nextArrayAssignStmt", stmt);
    }
  }

  private FixedArrayType buildFixedFromAlloc(AllocArrayType aat, Expression lhs)
  {
    int        rank   = aat.getRank();
    Vector<Bound> indx = new Vector<Bound>(rank);
    Type       et     = aat.getElementType();
    RecordType rty    = (RecordType) aat.getStruct().getCoreType();
    RecordType tdim   = dimType.getCoreType().returnRecordType();
    FieldDecl  fdims  = rty.findField("dims");
    Expression dims   = new SelectIndirectOp(lhs, fdims);
    FieldDecl  flast  = tdim.findField("last");
    FieldDecl  ffirst = tdim.findField("first");
    for (int i = 0; i < rank; i++) {
      Vector<Expression> ind = new Vector<Expression>(1);
      ind.add(LiteralMap.put(i, size_t_type));
      SubscriptAddressOp sub = new SubscriptAddressOp(PointerType.create(dimType), dims, ind);
      Expression         min = new SelectOp(sub, ffirst);
      Expression         max = new SelectOp(sub, flast);
      Bound              bd  = Bound.create(min, max);
      indx.add(bd);
    }
    return FixedArrayType.create(indx, et);
  }

  private Statement nextFCAssignStmt(boolean    bl,
                                     Expression lhs,
                                     boolean    br,
                                     Expression rhs) throws InvalidException
  {
    assert traceIn("nextFCAssignStmt", null);
    Type      lt   = lhs.getPointedToCore();
    Type      rt   = rhs.getPointedToCore();
    Statement stmt = null;

    try {
      if (rt == char_type) {
        FortranCharType atl = (FortranCharType) lt;
        Expression      ll  = getStringLength(lhs);

        Vector<Expression> args = new Vector<Expression>(4);
        args.add(makeLValue(lhs));
        args.add(rhs);
        args.add(ll);
        args.add(LiteralMap.put(' ', char_type));
        ProcedureDecl pd   = defPreKnownFtn("_scale_sassigncp", "vCcic", RoutineDecl.PURESGV);
        Expression    call = genCall(pd.getSignature(), genDeclAddress(pd), args, lineNumber, column);
        stmt = new EvalStmt(call);
        return stmt;
      }

      if (bl && br) {
        FortranCharType atl = (FortranCharType) lt;
        FortranCharType atr = (FortranCharType) rt;
        Expression      ll  = getStringLength(lhs);
        Expression      lr  = getStringLength(rhs);

        if (lr == null)
          return null;

        if (ll == null)
          ll = lr;

        Vector<Expression> args = new Vector<Expression>(5);
        args.add(makeLValue(lhs));
        args.add(makeLValue(rhs));
        args.add(ll);
        args.add(lr);
        args.add(LiteralMap.put(' ', char_type));
        ProcedureDecl pd   = defPreKnownFtn("_scale_sassignp", "vCCiic", RoutineDecl.PURESGV);
        Expression    call = genCall(pd.getSignature(), genDeclAddress(pd), args, lineNumber, column);
        stmt = new EvalStmt(call);
        return stmt;
      }

      if (classTrace) {
        System.out.println("** nas " + lt);
        System.out.println("       " + rt);
        System.out.println("       " + lhs);
        System.out.println("       " + rhs);
        if (rhs instanceof IdReferenceOp)
          System.out.println("       " + ((IdReferenceOp) rhs).getDecl());
      }
      userError(Msg.MSG_Not_a_CHARACTER_value, "");
      return null;
    } finally {
      assert traceOut("nextFCAssignStmt", stmt);
    }
  }

  private Statement nextCaseStmt()
  {
    assert traceIn("nextCaseStmt", null);
    Statement stmt = null;
    try {
      notImplementedError("nextCaseStmt");
      return new NullStmt();
    } catch (InvalidException ex) {
      return new NullStmt();
    } finally {
      assert traceOut("nextCaseStmt", stmt);
    }
  }

  private Statement nextDoStmt(String name)
  {
    assert traceIn("nextDoStmt", null);
    Statement stmt = null;
    LabelDecl ssl  = statementLabel;
    statementLabel = null;
    try {
      if (name != null)
        notImplementedError("doStmt");

      LabelDecl lab = nextLabelNoChk();

      nextNBCharIs(','); // Eat the comma if it is there.

      int sc      = column;
      int keyword = nextKeyword();
      if (keyword == Keywords.WHILE) { // DO WHILE.
        nextNBCharMustBe('(');

        Expression test = nextLogicalExpression();

        nextNBCharMustBe(')');
        nextStatement();

        BlockStmt scbs = currentBlockStmt;

        currentBlockStmt = new BlockStmt();
        nextExecutionPart(Keywords.kw_ifblock, lab);

        if (lab == null)
          nextEndStmt(Keywords.ENDDO);

        BlockStmt doBlock = currentBlockStmt;
        currentBlockStmt = scbs;

        stmt = new WhileLoopStmt(doBlock, test);
        return stmt;
      }

      column = sc;

      // Traditional DO.

      VariableDecl doVar = nextVariable();
      Type         dt    = doVar.getCoreType().getPointedTo();

      nextNBCharMustBe('=');

      Expression init = cast(dt, nextExpression(SCALAR_EXPR));

      nextNBCharMustBe(',');

      Expression limit = cast(dt, nextExpression(SCALAR_EXPR));

      Expression step = nextNBCharIs(',') ? cast(dt, nextExpression(SCALAR_EXPR)) : one;

      nextStatement();

      BlockStmt scbs = currentBlockStmt;
      currentBlockStmt = new BlockStmt();
      nextExecutionPart(Keywords.kw_ifblock, lab);

      if (lab == null)
        nextEndStmt(Keywords.ENDDO);

      BlockStmt doBlock = currentBlockStmt;
      currentBlockStmt = scbs;

      stmt = new DoLoopStmt(new IdValueOp(doVar), doBlock, init, limit, step);
      return stmt;
    } catch (InvalidException ex) {
      return new NullStmt();
    } finally {
      statementLabel = ssl;
      assert traceOut("nextDoStmt", stmt);
    }
  }

  private Statement nextForallStmt(String name)
  {
    assert traceIn("nextForallStmt", null);
    Statement stmt = null;
    try {
      notImplementedError("nextForallStmt");
      return new NullStmt();
    } catch (InvalidException ex) {
      return new NullStmt();
    } finally {
      assert traceOut("nextForallStmt", stmt);
    }
  }

  private Statement nextSelectStmt(String name)
  {
    assert traceIn("nextSelectStmt", null);
    Statement stmt = null;
    try {
      notImplementedError("nextSelectStmt");
      return new NullStmt();
    } catch (InvalidException ex) {
      return new NullStmt();
    } finally {
      assert traceOut("nextSelectStmt", stmt);
    }
  }

  private Statement nextArithmeticIfStmt(Expression pred)
  {
    assert traceIn("nextArithmeticIfStmt", null);
    Statement stmt = null;
    try {
      LabelDecl neg = nextLabel();
      nextCharMustBe(',');
      LabelDecl zer = nextLabel();
      nextCharMustBe(',');
      LabelDecl pos = nextLabel();
      if (!pred.getCoreType().isNumericType())
        userError(Msg.MSG_Invalid_statement, "must be numeric");
      stmt = new ArithmeticIfStmt(pred, neg, zer, pos);
      return stmt;
    } catch (InvalidException ex) {
      return null;
    } finally {
      assert traceOut("nextArithmeticIfStmt", stmt);
    }
  }

  private Statement nextIfStmt(String name)
  {
    assert traceIn("nextIfStmt", null);
    Statement stmt = null;
    LabelDecl ssl  = statementLabel;
    statementLabel = null;
    try {
      nextCharMustBe('(');
      Expression exp = nextScalarExpression();
      nextCharMustBe(')');

      if (allowF77Features && (name == null)) {
        skipBlanks();
        char c = statement[column];
        if ((c >= '0') && (c <= '9')) {
          stmt = nextArithmeticIfStmt(exp);
          return stmt;
        }
      }

      if (!exp.hasTrueFalseResult())
        exp = new NotEqualOp(int_type, exp, zero);

      int sc      = column;
      int keyword = nextKeyword();
      if ((name != null) && (keyword != Keywords.THEN))
        userError(Msg.MSG_Invalid_statement, "missing THEN");

      if (keyword != Keywords.THEN) {
        column = sc;
        Statement action = nextActionStmt();
        addStmtInfo(action, lineNumber, column);
        stmt   = new IfThenElseStmt(exp, action, new NullStmt());
        return stmt;
      }

      nextStatement();

      // Get the THEN-block.

      BlockStmt scbs = currentBlockStmt;
      currentBlockStmt = new BlockStmt();
      nextExecutionPart(Keywords.kw_ifblock, null);
      BlockStmt thenPart = currentBlockStmt;
      currentBlockStmt = scbs;

      keyword = nextKeyword();

      if (keyword == Keywords.END) {
        sc = column;
        int key2 = nextKeyword();
        if (key2 == Keywords.IF)
          keyword = Keywords.ENDIF;
        else
          column = sc;
      } else if (keyword == Keywords.ELSE) {
        sc = column;
        int key2 = nextKeyword();
        if (key2 == Keywords.IF)
          keyword = Keywords.ELSEIF;
        else
          column = sc;
      }

      if (name != null) {
        String id = nextIdentifier();
        if (!name.equals(id))
          userError(Msg.MSG_Invalid_statement, "labels don't match");
      }

      if (keyword == Keywords.ENDIF) {
        statementLabel2 = statementLabel;
        stmt = new IfThenElseStmt(exp, thenPart, new NullStmt());
        return stmt;
      }

      if (keyword == Keywords.ELSE) {
        nextStatement();
        currentBlockStmt = new BlockStmt();
        nextExecutionPart(Keywords.kw_ifblock, null);
        BlockStmt elsePart = currentBlockStmt;
        currentBlockStmt = scbs;

        keyword = nextKeyword();

        if (keyword == Keywords.END) {
          sc = column;
          int key2 = nextKeyword();
          if (key2 == Keywords.IF)
            keyword = Keywords.ENDIF;
          else
            column = sc;
        }

        if (keyword != Keywords.ENDIF)
          userError(Msg.MSG_Invalid_statement, "no ENDIF");

        if (name != null) {
          String id = nextIdentifier();
          if (!name.equals(id))
            userError(Msg.MSG_Invalid_statement, "labels don't match");

          stmt = new IfThenElseStmt(exp, thenPart, elsePart);
          return stmt;
        }

        statementLabel2 = statementLabel;
        stmt = new IfThenElseStmt(exp, thenPart, elsePart);
        return stmt;
      }

      if (keyword != Keywords.ELSEIF)
        userError(Msg.MSG_Invalid_statement, "missing END or ELSE");

      // Recurse to get ELSEIF.

      int       strtc    = column;
      int       strtl    = lineNumber;
      Statement elsePart = nextIfStmt(name);

      addStmtInfo(elsePart, strtl, strtc);

      currentBlockStmt = scbs;
      stmt = new IfThenElseStmt(exp, thenPart, elsePart);

      return stmt;
    } catch (InvalidException ex) {
      if (classTrace)
        ex.printStackTrace();
      return null;
    } finally {
      statementLabel = ssl;
      assert traceOut("nextIfStmt", stmt);
    }
  }

  private Statement nextWhereStmt(String name)
  {
    assert traceIn("nextWhereStmt", null);
    Statement stmt = null;
    try {
      notImplementedError("nextWhereStmt");
      return new NullStmt();
    } catch (InvalidException ex) {
      return new NullStmt();
    } finally {
      assert traceOut("nextWhereStmt", stmt);
    }
  }

  private Statement nextAllocateStmt()
  {
    assert traceIn("nextAllocateStmt", null);

    Statement  stmt   = null;
    BlockStmt  scbs   = currentBlockStmt;
    RecordType tdim   = dimType.getCoreType().returnRecordType();
    Type       ptdim  = PointerType.create(dimType);
    FieldDecl  fsize  = tdim.findField("size");
    FieldDecl  flast  = tdim.findField("last");
    FieldDecl  ffirst = tdim.findField("first");

    try {
      nextNBCharMustBe('(');

      ProcedureDecl malloc = defPreKnownFtn("malloc", "Vs", RoutineDecl.PURE);

      do {
        VariableDecl vd = nextVariable();
        if (vd instanceof FormalDecl)
          userError(Msg.MSG_Invalid_expression, "");

        nextNBCharMustBe('(');

        Type           type = vd.getCoreType();
        AllocArrayType aat  = type.returnAllocArrayType();
        if (aat == null)
          userError(Msg.MSG_Invalid_or_missing_type, "");

        Type           et   = aat.getElementType();
        Type           pet  = PointerType.create(et);
        long           es   = et.memorySize(Machine.currentMachine);
        Literal        sv   = LiteralMap.put(es, size_t_type);
        Expression     ty   = LiteralMap.put(determineFioType(et), char_type);
        ArrayType      at   = nextArraySpec(et);
        int            rank = at.getRank();

        if (at.getRank() != rank)
          userError(Msg.MSG_Invalid_dimension, "");

        nextNBCharMustBe(')');

        RecordType rty     = (RecordType) aat.getStruct().getCoreType();
        FieldDecl  falloc  = rty.findField("alloc");
        FieldDecl  ftype   = rty.findField("type");
        FieldDecl  felsize = rty.findField("elsize");
        FieldDecl  fptr    = rty.findField("ptr");
        FieldDecl  fbase   = rty.findField("base");
        FieldDecl  fdims   = rty.findField("dims");
        Type       tfdims  = fdims.getType();
        Type       ptfdims = PointerType.create(tfdims);
        BlockStmt  bs      = new BlockStmt();

        currentBlockStmt = bs;

        setStructField(vd, ftype, ty);
        setStructField(vd, felsize, sv);

        VariableDecl size = genTemp(size_t_type);
        VariableDecl dims = genTemp(ptdim);
        VariableDecl diff = genTemp(size_t_type);
        VariableDecl arr  = genTemp(pet);

        addSymbol(size);
        size.setReferenced();
        addSymbol(dims);
        dims.setReferenced();
        addSymbol(diff);
        diff.setReferenced();
        addSymbol(arr);
        arr.setReferenced();

        addAssignStmt(genDeclAddress(size), sv);

        Expression sel  = new SelectIndirectOp(genDeclAddress(vd), fdims);
        addAssignStmt(genDeclAddress(dims), sel);

        for (int i = 0; i < rank; i++) {
          Bound      bd   = at.getIndex(i);
          Expression max  = bd.getMax();
          Expression min  = bd.getMin();
          Expression liti = LiteralMap.put(i, int_type);

          Expression de = new AdditionOp(size_t_type, new SubtractionOp(size_t_type, max, min), one);
          Literal    ld = de.getConstantValue();
          if ((ld != Lattice.Top) && (ld != Lattice.Bot))
            de = ld;
          addAssignStmt(genDeclAddress(diff), de);

          if (!(ld instanceof IntLiteral)) {
            Expression pred = new LessEqualOp(size_t_type, new IdValueOp(diff), zero);
            Expression test = new ExpressionIfOp(size_t_type, pred, zero, new IdValueOp(diff));
            addAssignStmt(genDeclAddress(diff), test);
          }

          Vector<Expression> inds1 = new Vector<Expression>(1);
          inds1.add(liti);
          Expression sub1 = new SubscriptAddressOp(ptdim, new IdValueOp(dims), inds1);
          Expression sel1 = new SelectIndirectOp(sub1, fsize);
          addAssignStmt(sel1, new IdValueOp(diff));

          Vector<Expression> inds2 = new Vector<Expression>(1);
          inds2.add(liti);
          Expression sub2 = new SubscriptAddressOp(ptdim, new IdValueOp(dims), inds2);
          Expression sel2 = new SelectIndirectOp(sub2, flast);
          if (!max.getCoreType().equivalent(size_t_type))
            max = new TypeConversionOp(size_t_type, max, CastMode.TRUNCATE);
          addAssignStmt(sel2, max);

          Vector<Expression> inds3 = new Vector<Expression>(1);
          inds3.add(liti);
          Expression sub3 = new SubscriptAddressOp(ptdim, new IdValueOp(dims), inds3);
          Expression sel3 = new SelectIndirectOp(sub3, ffirst);
          if (!min.getCoreType().equivalent(size_t_type))
            min = new TypeConversionOp(size_t_type, min, CastMode.TRUNCATE);
          addAssignStmt(sel3, min);

          addAssignStmt(genDeclAddress(size),
                        new MultiplicationOp(size_t_type,
                                             new IdValueOp(size),
                                             new IdValueOp(diff)));
        }

        Vector<Expression> args = new Vector<Expression>(1);
        args.add(new IdValueOp(size));
        Expression call = genCall(malloc.getSignature(),
                                  genDeclAddress(malloc),
                                  args,
                                  lineNumber,
                                  column); 
        addAssignStmt(genDeclAddress(arr), call);
        setStructField(vd, fptr, new IdValueOp(arr));
        setStructField(vd, fbase, new SubtractionOp(pet,
                                                    new IdValueOp(arr),
                                                    new IdValueOp(size)));
        setStructField(vd, falloc, one);

        if (stmt == null)
          stmt = bs;
        else if (stmt instanceof BlockStmt)
          ((BlockStmt) stmt).addStmt(bs);
        else {
          stmt = new BlockStmt();
          ((BlockStmt) stmt).addStmt(bs);
        }

      } while (nextNBCharIs(','));

      nextNBCharMustBe(')');

      return stmt;
    } catch (InvalidException ex) {
      return new NullStmt();
    } finally {
      currentBlockStmt = scbs;
      assert traceOut("nextAllocateStmt", stmt);
    }
  }

  private Statement nextBackspaceStmt()
  {
    assert traceIn("nextBackspaceStmt", null);
    Statement stmt = null;
    try {
      nextAlistValues();

      stmt = buildAlistFtn("f_back");
      return stmt;
    } catch (InvalidException ex) {
      return new NullStmt();
    } finally {
      assert traceOut("nextBackspaceStmt", stmt);
    }
  }

  private Statement nextCloseStmt()
  {
    assert traceIn("nextCloseStmt", null);
    Statement stmt = null;
    try {
      nextCharMustBe('(');
      nextFioListValues(FIO_CLLIST);
      nextCharMustBe(')');

      if (fioIostat == null) {
        VariableDecl tv = genTemp(int_type);
        fioIostat = genDeclAddress(tv);
        addSymbol(tv);
      }

      RecordType   cllist  = getCllistType();
      VariableDecl cllistv = getCllistVar();

      BlockStmt scbs = currentBlockStmt;
      currentBlockStmt = new BlockStmt();

      setStructField(cllistv, cllist.findField("cerr"), !fioChkErr ? one : zero);
      setStructField(cllistv, cllist.findField("cunit"), fioUnit);
      setStructField(cllistv, cllist.findField("csta"), fioStatus);

      ProcedureDecl rd   = defPreKnownFtn("f_clos", "iV", RoutineDecl.PURESGV);
      Expression    ftnr = genDeclAddress(rd);
      Vector<Expression> args = new Vector<Expression>(1);
      Expression    arg  = genDeclAddress(cllistv);

      rd.setReferenced();
      args.add(arg);

      Expression call   = genCall((ProcedureType) rd.getType(), ftnr, args, lineNumber, column);
      Expression assign = new AssignSimpleOp(fioIostat, call);

      addNewStatement(new EvalStmt(assign));

      buildFioErrCheck();

      stmt = currentBlockStmt;
      currentBlockStmt = scbs;

      return stmt;
    } catch (InvalidException ex) {
      return new NullStmt();
    } finally {
      assert traceOut("nextCloseStmt", stmt);
    }
  }

  private Statement nextCallStmt()
  {
    assert traceIn("nextCallStmt", null);
    Statement stmt = null;
    try {
      int         sc     = column;
      String      name   = nextIdentifier();
      Declaration decl   = lookupDecl(name);

      if (decl == null) {
        // We haven't defined this symbol yet but we may have
        // encountered its type definition already.
        Object o = nameMap.get(name);
        if ((o != null) && !(o instanceof Declaration)) {
          column = sc;
          return null;
        }
        decl = (Declaration) o;
        if (decl instanceof RoutineDecl)
          cg.addRootSymbol(decl);
      }

      Expression    ftn = null;
      RoutineDecl   rd  = null;
      ProcedureType pt  = null;
      boolean       ind = false;

      if (decl != null) {
        rd = decl.returnRoutineDecl();
        if (rd != null) {
          pt = rd.getSignature();
          ftn = genDeclAddress(rd);
        } else {
          FormalDecl fd = decl.returnFormalDecl();
          if (fd != null) {
            ftn = new IdValueOp(fd);
            ind = true;
            fd.setMode(ParameterMode.VALUE);
          } else {
            column = sc;
            return null;
          }
        }
      }

      Vector<Expression> args = new Vector<Expression>();
      Vector<LabelDecl>  labs = null;
      if (nextNBCharIs('(')) {
        if (!nextNBCharIs(')')) {
          while (true) {
            if (nextNBCharIs('*')) {
              LabelDecl lab = nextLabel();
              if (labs == null)
                labs = new Vector<LabelDecl>();
              labs.add(lab);
            } else {
              Expression exp = nextArrayExpr();
              if (exp != null)
                args.add(exp);
            }

            if (nextNBCharIs(')'))
              break;
            nextNBCharMustBe(',');
          }
        }
      }

      if ((pt == null) || (pt.numFormals() == 0)) { // Define the routine.
        makeByReference(args, null);
        pt = createTypeFromArgs(name, (labs == null) ? (Type) void_type : (Type) int_type, args, "B");
        if (ftn == null) {
          rd = new ProcedureDecl(name, pt);
          rd.setVisibility(Visibility.EXTERN);
          cg.addRootSymbol(rd);
          cg.recordRoutine(rd);
          ftn = genDeclAddress(rd);
        } else if (ind) {
          Type ty = PointerType.create(pt);
          decl.setType(ty);
          ftn.setType(ty);
        }
      } else {
        makeByReference(args, null);
        if (args.size() != pt.numFormals()) {
          if (classTrace)
            System.out.println("** ncs " + args.size() + " " + pt.numFormals() + " " + pt);
          userError(Msg.MSG_Invalid_statement, "arg mismatch");
        }
      }

      int l = args.size();
      for (int i = 0; i < l; i++) {
        FormalDecl fd  = pt.getFormal(i);
        Expression arg = args.get(i);

        if (arg instanceof IdAddressOp)
          ((IdAddressOp) arg).getDecl().setAddressTaken();

        Type       ft  = fd.getType();
        Expression na  = cast(ft, arg, lineNumber, column);
        if (na != arg)
          args.setElementAt(na, i);
      }

      if (rd != null)
        rd.setReferenced();

      Type   rt   = pt.getReturnType();
      CallOp call = new CallFunctionOp(pt.getReturnType(), ftn, args);

      if (rd != null)
        fixupCalls.add(rd, call);

      if (labs != null) {
        if (rt != int_type) {
          System.out.println("** ncs " + ftn + "\n    " + pt);
          userError(Msg.MSG_Invalid_statement, "labels args");
        }
        int ll = labs.size();
        Vector<Statement> stmts = new Vector<Statement>(ll);
        for (int i = 0; i < ll; i++) {
          LabelDecl     ld  = labs.get(i);
          CaseLabelDecl cld = new CaseLabelDecl("_C" + i, LiteralMap.put(i + 1, int_type));
          Statement     ls  = new LabelStmt(cld, new GotoStmt(ld));
          stmts.addElement(ls);
        }
        stmt = new SwitchStmt(call, new BlockStmt(stmts));
      } else
        stmt = new EvalStmt(call);

      return stmt;
    } catch (InvalidException ex) {
      return new NullStmt();
    } finally {
      assert traceOut("nextCallStmt", stmt);
    }
  }

  private Statement nextContinueStmt()
  {
    assert traceIn("nextContinueStmt", null);
    Statement stmt = null;
    try {
      stmt = new NullStmt();
      return stmt;
    } finally {
      assert traceOut("nextContinueStmt", stmt);
    }
  }

  private Statement nextCycleStmt()
  {
    assert traceIn("nextCycleStmt", null);
    Statement stmt = null;
    try {
      String name = nextIdentifier();
      if (name != null)
        notImplementedError("labeled CYCLE");
      stmt = new ContinueStmt();
      return stmt;
    } catch (InvalidException ex) {
      return new NullStmt();
    } finally {
      assert traceOut("nextCycleStmt", stmt);
    }
  }

  private Statement nextDeallocateStmt()
  {
    assert traceIn("nextDeallocateStmt", null);
    Statement stmt = null;
    BlockStmt scbs = currentBlockStmt;
    try {
      nextNBCharMustBe('(');

      ProcedureDecl freeFtn = defPreKnownFtn("free", "vV", RoutineDecl.PURE);
      do {
        VariableDecl vd = nextVariable();
        if (vd instanceof FormalDecl)
          userError(Msg.MSG_Invalid_expression, "");

        Type           type = vd.getCoreType();
        AllocArrayType aat  = type.returnAllocArrayType();
        if (aat == null)
          userError(Msg.MSG_Invalid_or_missing_type, "");

        Type               et   = aat.getElementType();
        RecordType         rty  = (RecordType) aat.getStruct().getCoreType();
        BlockStmt          bs   = new BlockStmt();
        Vector<Expression> args = new Vector<Expression>(1);

        currentBlockStmt = bs;

        args.add(new SelectOp(genDeclAddress(vd), rty.findField("ptr")));
        Expression call = genCall(freeFtn.getSignature(),genDeclAddress(freeFtn), args, lineNumber, column); 
        addNewStatement(new EvalStmt(call));
        setStructField(vd, rty.findField("ptr"), zero);
        setStructField(vd, rty.findField("alloc"), zero);

        Expression sel  = new SelectOp(genDeclAddress(vd), rty.findField("alloc"));
        Expression pred = new EqualityOp(sel, one);
        IfThenElseStmt ifs = new IfThenElseStmt(pred, bs, new NullStmt());
        if (stmt == null)
          stmt = ifs;
        else if (stmt instanceof BlockStmt)
          ((BlockStmt) stmt).addStmt(ifs);
        else {
          stmt = new BlockStmt();
          ((BlockStmt) stmt).addStmt(ifs);
        }
      } while (nextNBCharIs(','));

      nextNBCharMustBe(')');

      return stmt;
    } catch (InvalidException ex) {
      return new NullStmt();
    } finally {
      currentBlockStmt = scbs;
      assert traceOut("nextDeallocateStmt", stmt);
    }
  }

  private Statement nextEndfileStmt()
  {
    assert traceIn("nextEndfileStmt", null);
    Statement stmt = null;
    try {
      nextAlistValues();

      stmt = buildAlistFtn("f_end");
      return stmt;
    } catch (InvalidException ex) {
      return new NullStmt();
    } finally {
      assert traceOut("nextEndfileStmt", stmt);
    }
  }

  private Statement nextExitStmt()
  {
    assert traceIn("nextExitStmt", null);
    Statement stmt = null;
    try {
      String name = nextIdentifier();
      if (name != null)
        notImplementedError("labeled EXIT");
      stmt = new BreakStmt();
      return stmt;
    } catch (InvalidException ex) {
      return new NullStmt();
    } finally {
      assert traceOut("nextExitStmt", stmt);
    }
  }

  private Statement nextGotoStmt()
  {
    assert traceIn("nextGotoStmt", null);
    Statement stmt = null;
    try {
      if (nextNBCharIs('(')) { // Computed goto.
        Vector<LabelDecl> labs = new Vector<LabelDecl>();
        do {
          labs.add(nextLabel());
        } while (nextNBCharIs(','));

        nextNBCharMustBe(')');
        nextNBCharIs(','); // Eat the comma if any.

        Expression exp  = nextExpression(SCALAR_EXPR);
        Type       type = exp.getCoreType().getPointedTo();
        if (!type.isIntegerType())
          userError(Msg.MSG_Not_an_integer_value, "");
        stmt = new ComputedGotoStmt(exp, labs);
        return stmt;
      }

      skipBlanks();

      if ((statement[column] >= '0') && (statement[column] <= '9')) { // goto  label
        LabelDecl ld   = nextLabel();
        stmt = new GotoStmt(ld);
        ld.setReferenced();
        return stmt;
      }

      if (!allowF77Features)
        userError(Msg.MSG_Invalid_goto_statement, "");

      // Assigned goto.

      VariableDecl vd = nextVariable();
      if (vd == null)
        userError(Msg.MSG_Invalid_goto_statement, "");

      Type        type = vd.getCoreType().getPointedTo();
      IntegerType it   = type.getCoreType().returnIntegerType();

      if (it == null)
        userError(Msg.MSG_s_is_not_an_integer_variable, vd.getName());

      // Kludge!  This logic is for machines where the size of an
      // INTEGER is 32-bits and the size of an address is greater than
      // 32 bits.

      IntegerType t  = Machine.currentMachine.getSizetType();
      if (t.bitSize() > it.bitSize())
        vd.setType(t);

      nextNBCharIs(',');
      nextNBCharMustBe('(');

      while (true) {
        LabelDecl    lab = nextLabel();
        VariableDecl fvd = formatMap.get(lab);
        if (fvd == null) {
          fvd = new VariableDecl(lab.getName(), void_type);
          fvd.setResidency(Residency.MEMORY);
          addSymbol(fvd);
          formatMap.put(lab, fvd);
        }

        GotoStmt   gos  = new GotoStmt(lab);
        Expression la   = new IdValueOp(vd);
        Expression ra   = genDeclAddress(fvd);
        Expression pred = new EqualityOp(logical_type, la, ra);
        stmt = new IfThenElseStmt(pred, gos, stmt);

        if (nextNBCharIs(')'))
          break;
        nextNBCharMustBe(',');
      }

      return stmt;
    } catch (InvalidException ex) {
      return new NullStmt();
    } finally {
      assert traceOut("nextGotoStmt", stmt);
    }
  }

  private Statement nextAssignStmt()
  {
    assert traceIn("nextAssignStmt", null);
    Statement stmt = null;
    try {
      LabelDecl lab = nextLabel();
      lab.setReferenced();
      int keyword = nextKeyword();
      if (keyword != Keywords.TO)
        userError(Msg.MSG_Invalid_statement, "");
      Expression var = nextIDExpr(SCALAR_EXPR);
      if (var == null)
        userError(Msg.MSG_Invalid_statement, "");

      VariableDecl vd = formatMap.get(lab);
      if (vd == null) {
        vd = new VariableDecl(lab.getName(), void_type);
        vd.setResidency(Residency.MEMORY);
        addSymbol(vd);
        formatMap.put(lab, vd);
      }

      // On 64-bit systems, a Fortran INTEGER type my not be big
      // enough for the address.  The following logic is a kludge
      // because it does not solve the problem when assigning to an
      // array element and because it changes the type of a
      // user-declared variable.  As ASSIGN statements are not allowed
      // by the Fortran 95 standard, we won't worry about doing a
      // correct (and hard) implementation.

      Expression lhs;
      if (var instanceof IdReferenceOp) {
        Declaration decl = ((IdReferenceOp) var).getDecl();
        decl.setType(ptrdiff_t_type);
        lhs = genDeclAddress(decl);
      } else
        lhs = makeLValue(var);

      // Assign the address of the label (FORMAT) to the variable.

      Expression rhs    = cast(lhs.getCoreType().getPointedTo(), genDeclAddress(vd));
      Expression assign = new AssignSimpleOp(lhs, rhs);
      stmt = new EvalStmt(assign);
      return stmt;
    } catch (InvalidException ex) {
      return new NullStmt();
    } finally {
      assert traceOut("nextAssignStmt", stmt);
    }
  }

  private Statement nextInquireStmt()
  {
    assert traceIn("nextInquireStmt", null);
    Statement stmt = null;
    try {
      resetFioListValues();
      nextCharMustBe('(');
      nextFioListValues(FIO_INLIST);
      nextCharMustBe(')');

      if (fioIostat == null) {
        VariableDecl tv = genTemp(int_type);
        addSymbol(tv);
        fioIostat = genDeclAddress(tv);
      }

      RecordType   inlist  = getInlistType();
      VariableDecl inlistv = getInlistVar();

      BlockStmt scbs = currentBlockStmt;
      currentBlockStmt = new BlockStmt();

      setStructField(inlistv, inlist.findField("inerr"),     !fioChkErr ? one : zero);
      setStructField(inlistv, inlist.findField("inunit"),     fioUnit);
      setStructField(inlistv, inlist.findField("infile"),     fioFile);
      setStructField(inlistv, inlist.findField("infilen"),    getStringLength(fioFile));
      setStructField(inlistv, inlist.findField("inex"),       fioExist);
      setStructField(inlistv, inlist.findField("inopen"),     fioOpened);
      setStructField(inlistv, inlist.findField("innum"),      fioNumber);
      setStructField(inlistv, inlist.findField("innamed"),    fioNamed);
      setStructField(inlistv, inlist.findField("inname"),     fioName);
      setStructField(inlistv, inlist.findField("innamlen"),   getStringLength(fioName));
      setStructField(inlistv, inlist.findField("inacc"),      fioAccess);
      setStructField(inlistv, inlist.findField("inacclen"),   getStringLength(fioAccess));
      setStructField(inlistv, inlist.findField("inseq"),      fioSequential);
      setStructField(inlistv, inlist.findField("inseqlen"),   getStringLength(fioSequential));
      setStructField(inlistv, inlist.findField("indir"),      fioDirect);
      setStructField(inlistv, inlist.findField("indirlen"),   getStringLength(fioDirect));
      setStructField(inlistv, inlist.findField("infmt"),      fioForm);
      setStructField(inlistv, inlist.findField("infmtlen"),   getStringLength(fioForm));
      setStructField(inlistv, inlist.findField("inform"),     fioFormatted);
      setStructField(inlistv, inlist.findField("informlen"),  getStringLength(fioFormatted));
      setStructField(inlistv, inlist.findField("inunf"),      fioUnformatted);
      setStructField(inlistv, inlist.findField("inunflen"),   getStringLength(fioUnformatted));
      setStructField(inlistv, inlist.findField("inrecl"),     fioRecl);
      setStructField(inlistv, inlist.findField("innrec"),     fioNextrec);
      setStructField(inlistv, inlist.findField("inblank"),    fioBlank);
      setStructField(inlistv, inlist.findField("inblanklen"), getStringLength(fioBlank));


      ProcedureDecl      rd   = defPreKnownFtn("f_inqu", "iV", RoutineDecl.PURESGV);
      Expression         ftnr = genDeclAddress(rd);
      Vector<Expression> args = new Vector<Expression>(1);
      Expression         arg  = genDeclAddress(inlistv);

      rd.setReferenced();
      args.add(arg);

      Expression call   = genCall((ProcedureType) rd.getType(), ftnr, args, lineNumber, column);
      Expression assign = new AssignSimpleOp(fioIostat, call);

      addNewStatement(new EvalStmt(assign));

      buildFioErrCheck();

      stmt = currentBlockStmt;
      currentBlockStmt = scbs;

      return stmt;
    } catch (InvalidException ex) {
      return new NullStmt();
    } finally {
      assert traceOut("nextInquireStmt", stmt);
    }
  }

  private Statement nextNullifyStmt()
  {
    assert traceIn("nextNullifyStmt", null);
    Statement stmt = null;
    try {
      notImplementedError("nextNullifyStmt");
      return new NullStmt();
    } catch (InvalidException ex) {
      return new NullStmt();
    } finally {
      assert traceOut("nextNullifyStmt", stmt);
    }
  }

  private Statement nextOpenStmt()
  {
    assert traceIn("nextOpenStmt", null);
    Statement stmt = null;
    try {
      resetFioListValues();
      nextCharMustBe('(');
      nextFioListValues(FIO_OLIST);
      nextCharMustBe(')');

      if (fioIostat == null) {
        VariableDecl tv = genTemp(int_type);
        fioIostat = genDeclAddress(tv);
        addSymbol(tv);
      }

      RecordType   olist  = getOlistType();
      VariableDecl olistv = getOlistVar();

      BlockStmt scbs = currentBlockStmt;
      currentBlockStmt = new BlockStmt();

      setStructField(olistv, olist.findField("oerr"),    (fioErr != null) ? one : zero);
      setStructField(olistv, olist.findField("ounit"),   fioUnit);
      setStructField(olistv, olist.findField("ofnm"),    fioFile);
      setStructField(olistv, olist.findField("ofnmlen"), getStringLength(fioFile));
      setStructField(olistv, olist.findField("osta"),    fioStatus);
      setStructField(olistv, olist.findField("oacc"),    fioAccess);
      setStructField(olistv, olist.findField("ofm"),     fioForm);
      setStructField(olistv, olist.findField("orl"),     fioRecl);
      setStructField(olistv, olist.findField("oblnk"),   fioBlank);

      ProcedureDecl      rd   = defPreKnownFtn("f_open", "iV", RoutineDecl.PURESGV);
      Expression         ftnr = genDeclAddress(rd);
      Vector<Expression> args = new Vector<Expression>(1);
      Expression         arg  = genDeclAddress(olistv);

      rd.setReferenced();
      args.add(arg);

      Expression call   = genCall((ProcedureType) rd.getType(), ftnr, args, lineNumber, column);
      Expression assign = new AssignSimpleOp(fioIostat, call);

      addNewStatement(new EvalStmt(assign));

      buildFioErrCheck();

      stmt = currentBlockStmt;
      currentBlockStmt = scbs;

      return stmt;
    } catch (InvalidException ex) {
      return new NullStmt();
    } finally {
      assert traceOut("nextOpenStmt", stmt);
    }
  }

  private Statement nextPauseStmt()
  {
    assert traceIn("nextPauseStmt", null);
    Statement stmt = null;
    try {
      Expression value = zero;
      try {
        value = nextExpression(SCALAR_EXPR);
      } catch (InvalidException ex) {
      }
      return new NullStmt();
    } finally {
      assert traceOut("nextPauseStmt", stmt);
    }
  }

  private Statement nextPrintStmt()
  {
    assert traceIn("nextPrintStmt", null);
    Statement stmt = null;
    try {
      Expression   fmt     = nextFormatSpecifier();
      VariableDecl cilistv = getCilistVar();
      VariableDecl tv      = genTemp(int_type);

      fioIostat = genDeclAddress(tv);
      addSymbol(tv);

      fioChkErr = true;

      BlockStmt scbs = currentBlockStmt;
      currentBlockStmt = new BlockStmt();

      Statement assign = null;
      int       ioType = 0;
      String    eftn   = "";
      if (fmt == null) {
        assign = buildCilistStart("s_wsue", cilistv, six, zero, zero);
        eftn = "e_wsue";
        ioType = DO_UIO;
      } else if (fmt == one) {
        assign = buildCilistStart("s_wsle", cilistv, six, zero, zero);
        eftn = "e_wsle";
        ioType = DO_LIO;
      } else {
        Expression fmte =  new TypeConversionOp(charp_type, fmt, CastMode.CAST);
        assign = buildCilistStart("s_wsfe", cilistv, six, zero, fmte);
        eftn = "e_wsfe";
        ioType = DO_FIO;
      }

      addNewStatement(assign);

      buildFioErrCheck();

      if (nextNBCharIs(',')) // Process list of items to print.
        nextIOList(ioType);

      assign = buildIOListEnd(eftn);

      addNewStatement(assign);

      buildFioErrCheck();

      stmt = currentBlockStmt;
      currentBlockStmt = scbs;

      return stmt;
    } catch (InvalidException ex) {
      return new NullStmt();
    } finally {
      assert traceOut("nextPrintStmt", stmt);
    }
  }

  private Statement nextReadStmt()
  {
    assert traceIn("nextReadStmt", null);
    Statement stmt = null;
    try {
      nextCilistValues(FIO_RLIST);
      if (fioUnit == null)
        fioUnit = five;

      Type ty = fioUnit.getCoreType();

      fioChkErr = true;

      BlockStmt scbs = currentBlockStmt;
      currentBlockStmt = new BlockStmt();

      Statement  assign = null;
      String     name   = "";
      Expression fmte   = null;
      int        ioType = 0;

      String eftn = "";
      if (!ty.isAtomicType() || ty.isPointerType()) {
        VariableDecl icilistv = getIcilistVar();
        if (fioFmt == null) {
          assign = buildIcilistStart("s_rsui", icilistv, fioUnit, zero);
          eftn = "e_rsui";
          ioType = DO_UIO;
        } else if (fioFmt == one) {
          assign = buildIcilistStart("s_rsli", icilistv, fioUnit, zero);
          eftn = "e_rsli";
          ioType = DO_LIO;
        } else {
          fmte =  new TypeConversionOp(charp_type, fioFmt, CastMode.CAST);
          assign = buildIcilistStart("s_rsfi", icilistv, fioUnit, fmte);
          eftn = "e_rsfi";
          ioType = DO_FIO;
        }
      } else {
        VariableDecl cilistv = getCilistVar();
        if (fioFmt == null) {
          assign = buildCilistStart((fioRec != null) ? "s_rdue" : "s_rsue", cilistv, fioUnit, fioRec, zero);
          eftn = (fioRec != null) ? "e_rdue" : "e_rsue";
          ioType = DO_UIO;
        } else if (fioFmt == one) {
          assign = buildCilistStart((fioRec != null) ? "s_rdle" : "s_rsle", cilistv, fioUnit, fioRec, zero);
          eftn = (fioRec != null) ? "e_rdle" : "e_rsle";
          ioType = DO_LIO;
        } else {
          fmte =  new TypeConversionOp(charp_type, fioFmt, CastMode.CAST);
          assign = buildCilistStart((fioRec != null) ? "s_rdfe" : "s_rsfe", cilistv, fioUnit, fioRec, fmte);
          eftn = (fioRec != null) ? "e_rdfe" : "e_rsfe";
          ioType = DO_FIO;
        }
      }

      addNewStatement(assign);

      buildFioErrCheck();

      // Process list of items to read.

      nextIOList(ioType);

      assign = buildIOListEnd(eftn);

      addNewStatement(assign);

      buildFioErrCheck();

      stmt = currentBlockStmt;
      currentBlockStmt = scbs;

      return stmt;

    } catch (InvalidException ex) {
      return new NullStmt();
    } finally {
      assert traceOut("nextReadStmt", stmt);
    }
  }

  private Statement nextWriteStmt()
  {
    assert traceIn("nextWriteStmt", null);
    Statement stmt = null;
    try {
      nextCilistValues(FIO_WLIST);
      if (fioUnit == null)
        fioUnit = six;

      Type ty = fioUnit.getCoreType();

      fioChkErr = true;

      BlockStmt scbs = currentBlockStmt;
      currentBlockStmt = new BlockStmt();

      Statement  assign = null;
      String     name   = "";
      Expression fmte   = null;
      int        ioType = 0;

      String eftn = "e_wsle";
      if (!ty.isAtomicType() || ty.isPointerType()) {
        VariableDecl icilistv = getIcilistVar();
        if (fioFmt == null) {
          assign = buildIcilistStart("s_wsui", icilistv, fioUnit, zero);
          eftn = "e_wsui";
          ioType = DO_UIO;
        } else if (fioFmt == one) {
          assign = buildIcilistStart("s_wsli", icilistv, fioUnit, zero);
          eftn = "e_wsli";
          ioType = DO_LIO;
        } else {
          fmte =  new TypeConversionOp(charp_type, fioFmt, CastMode.CAST);
          assign = buildIcilistStart("s_wsfi", icilistv, fioUnit, fmte);
          eftn = "e_wsfi";
          ioType = DO_FIO;
        }
      } else {
        VariableDecl cilistv = getCilistVar();
        if (fioFmt == null) {
          assign = buildCilistStart((fioRec != null) ? "s_wdue" : "s_wsue", cilistv, fioUnit, fioRec, zero);
          eftn = (fioRec != null) ? "e_wdue" : "e_wsue";
          ioType = DO_UIO;
        } else if (fioFmt == one) {
          assign = buildCilistStart((fioRec != null) ? "s_wdle" : "s_wsle", cilistv, fioUnit, fioRec, zero);
          eftn = (fioRec != null) ? "e_wdle" : "e_wsle";
          ioType = DO_LIO;
        } else {
          fmte =  new TypeConversionOp(charp_type, fioFmt, CastMode.CAST);
          assign = buildCilistStart((fioRec != null) ? "s_wdfe" : "s_wsfe", cilistv, fioUnit, fioRec, fmte);
          eftn = (fioRec != null) ? "e_wdfe" : "e_wsfe";
          ioType = DO_FIO;
        }
      }

      addNewStatement(assign);

      buildFioErrCheck();

      // Process list of items to write.

      nextIOList(ioType);

      assign = buildIOListEnd(eftn);

      addNewStatement(assign);

      buildFioErrCheck();

      stmt = currentBlockStmt;
      currentBlockStmt = scbs;

      return stmt;

    } catch (InvalidException ex) {
      return new NullStmt();
    } finally {
      assert traceOut("nextWriteStmt", stmt);
    }
  }

  private Statement nextReturnStmt()
  {
    assert traceIn("nextReturnStmt", null);
    Statement stmt = null;
    try {
      Expression exp = null;
      if (resultVar != null) {
        if (resultVar.getCoreType().isFortranCharType()) {
          ;
        } else
          exp = new IdValueOp(resultVar);
      } else
        exp = nextExpression(SCALAR_EXPR);

      stmt = new ReturnStmt(exp);
      return stmt;
    } catch (InvalidException ex) {
      return new NullStmt();
    } finally {
      assert traceOut("nextReturnStmt", stmt);
    }
  }

  private Statement nextRewindStmt()
  {
    assert traceIn("nextRewindStmt", null);
    Statement stmt = null;
    try {
      nextAlistValues();
      stmt = buildAlistFtn("f_rew");
      return stmt;
    } catch (InvalidException ex) {
      return new NullStmt();
    } finally {
      assert traceOut("nextRewindStmt", stmt);
    }
  }

  private Statement nextStopStmt()
  {
    assert traceIn("nextStopStmt", null);
    Statement stmt = null;
    try {
      Expression value = zero;
      try {
        value = nextExpression(SCALAR_EXPR);
        if (value == null)
          value = zero;
      } catch (InvalidException ex) {
      }
      stmt = makeCallExitStmt(value);
      return stmt;
    } catch (InvalidException ex) {
      return new NullStmt();
    } finally {
      assert traceOut("nextStopStmt", stmt);
    }
  }
}
