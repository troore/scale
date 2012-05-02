package scale.common;

/** 
 * This provides the basis for English messages issued by the Scale
 * compiler.
 * <p>
 * $Id: MsgEnglish.java,v 1.50 2007-10-04 19:53:36 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */
public class MsgEnglish extends Msg
{

  private static final String[] msgs = {
    "_alignof_ is non standard.",
    "Array of functions is not allowed.",
    "Assignment of integer to pointer without a cast.",
    "Broken pipe: %s.",
    "Configuration file %s not found.",
    "Deprecated: %s.",
    "Dereference of non address.",
    "Enum %s already defined.",
    "Error loading flag file: %s.",
    "Errors in compilation.",
    "Field %s already defined.",
    "Field %s not found.",
    "Function %s definition does not match prototype.",
    "Function %s is already defined.",
    "Ignored: %s.",
    "Improper call to %s.",
    "Improper comment termination.",
    "Improper string termination.",
    "Invalid initializer.",
    "Invalid procedure type.",
    "Incompatible function argument for: %s.",
    "Incorrect type specification.",
    "Invalid dimension.",
    "Invalid expression.",
    "Invalid function call.",
    "Invalid lvalue.",
    "Invalid or missing type.",
    "Invalid parameter: %s.",
    "Invalid storage class.",
    "Invalid subscript expression.",
    "Invalid type.",
    "Junk after directive.",
    "K R function definition: %s.",
    "Label %s defined twice.",
    "Language %s not supported, using default.",
    "Machine %s not found.",
    "Macro redefined: %s.",
    "Missing return statement: %s.",
    "More than one source file specified.",
    "No source file specified.",
    "Not a constant.",
    "Not a struct.",
    "Not a type: %s.",
    "Not defined: %s.",
    "Not implemented: %s.",
    "Operands to %s not compatible.",
    "Operands to %s not integer types.",
    "Parameter %s already defined.",
    "Profile CFG characteristic value differs for: %s.",
    "Routine %s can not be optimized.",
    "%s.",
    "Scale Compiler Version: %s",
    "%s contains gotos.",
    "%s ignored.",
    "Storage class specified twice.",
    "Symbol %s already defined.",
    "There were %s classes read.",
    "Too many subscripts.",
    "unused 58.",
    "Type %s is already defined.",
    "Unable to open window: %s.",
    "Unable to send to: %s.",
    "Unknown conversion.",
    "Unrecognized declaration attribute: %s.",
    "Unrecognized type attribute: %s.",
    "Unrecognized visulaizer response: %s.",
    "Variable %s already defined.",
    "Debug level is %s.",
    "Stat level is %s.",
    "Node report level is %s.",
    "Note report level is %s.",
    "Report name is %s.",
    "Invalid CFG before optimization: %s.",
    "Conflicting parameters, choose one of: %s.",
    "Shapiro-Horwitz can not be selected with 0 categories.",
    "Optimization %s not performed for %s.",
    "Separate compilation.",
    "Multi compilation.",
    "Target architecture: %s.",
    "Host architecture: %s.",
    "User specified annotations added.",
    "Inlining level: %s.",
    "No builtins.",
    "Assume dummy aliases.",
    "Unsafe optimizations allowed.",
    "Floating point reordering allowed.",
    "Signed integer overflows might not wrap",
    "All integer overflows might not wrap",
    "Data dependence testing: %s.",
    "Backend hyperblock formation enabled.",   
    "unused 90",   // unused
    "unused 91",   // unused
    "Optimizations: %s.",
    "Unknown optimization: %s.",
    "Loop unrolling factor: %s.",
    "Performing: %s",
    "No alias analysis.",
    "Simple intra-procedural alias analysis.",
    "Steensgard intra-procedural alias analyses.",
    "Shapiro intra-procedural alias analyses.",
    "Simple inter-procedural alias analysis.",
    "Steensgard inter-procedural alias analysis.",
    "Shapiro inter-procedural alias analysis.",
    "Parameter %s requires a value.",
    "Required parameter %s not specified.",
    "Usage: java %s",
    "default is",
    "%s returned status %s",
    "Include file %s not found.",
    "#endif without #if.",
    "Too many closing parens.",
    "Too few closing parens.",
    "Not an integer constant.",
    "Not a macro call.",
    "Too many macro arguments.",
    "Too few macro arguments.",
    "Invalid macro definition.",
    "Pre-defined macros can not be redefined.",
    "Pre-defined macros can not be undefined.",
    "Unknown file extension: %s.",
    "Failed to compile %s.",
    "Failed to generate IL file for multiple files.",
    "Failed to generate IL file for %s.",
    "Source file not found: %s.",
    "Unknown file type: %s.",
    "Using profiling information.",
    "Invalid statement.",
    "Expecting %s, found %s.",
    "Too many initializers.",
    "Generating %s",
    "Assembling %s",
    "Clef %s",
    "Scribble %s",
    "Optimizing %s",
    "Converting multiple files to Scribble.",
    "Converting to Scribble: %s",
    "Start",
    "End",
    "Use Profile Info Start",
    "Use Profile Info End",
    "Inlining Start",
    "Inlining End",
    "Alias Analysis Start",
    "Alias Analysis End",
    "Main already defined in %s",
    "%s is not a variable",
    "%s is not an integer variable",
    "Invalid goto statement",
    "Invalid statement label",
    "Unit not specified",
    "Not an integer value",
    "Not a scalar value",
    "Not a CHARACTER value",
    "Not a LOGICAL value",
    "Missing END statement",
    "Improper continuation statement",
    "Concatenation of unknown length not allowed",
    "Call does not match routine definition",
    "Unknown intrinsic function: %s",
    "Invalid KIND selector",
    "Invalid FORMAT specifier",
    "Invalid CFG after optimization: %s.",
    "Elapsed time: %s",
    "Parser %s not found.",
    "The shapes of the array expressions do not conform.",
    "Use of -O%s with -g is not recommended.",
    "Pragma not processed: %s",
    "Invalid type operand to %s.",
    "Unable to instruction schedule %s.",
    "Suspended, press enter: ",
    "Invalid profile data: %s.",
  };

  private static final String[] helpMsgs = {
    // HLP_none
    "No help provided.",
    // HLP_G
    "specify graphic display options\n" +
    "e - high & low expressions\n" +
    "l - low expressions only\n" +
    "h - high expressions only\n" +
    "a - annotations\n" +
    "m - may-use links\n" +
    "u - use-def links\n" +
    "t - types\n" +
    "d - data dependence\n" +
    "D - domination\n" +
    "C - control dependence\n" +
    "P - post domination\n" +
    "c - Clef",
    // HLP_da
    "use the daVinci graphing tool\n" +
    "The DaVinci graphics package must be installed on your system and the\n" +
    "executable must be on your path.  The default is to use the Scale\n" +
    "graphing tool.",
    // HLP_unused_3
    "unused 3",
    // HLP_d
    "set the debug level\n" +
    "0 - normal\n" +
    "1 - informative messages and validity checks\n" +
    "2 - more detailed information\n" +
    "3 - brain dump",
    // HLP_dd
    "specify the data dependence testing to use\n" +
    "t - use transitive closure\n" + 
    "i - scalar replacement within inner loops only\n" +
    "b - basic test\n" +
    "B - Banerjee test\n" +
    "O - Omega Library",
    // HLP_files
    "compile the source file(s) specified.  They can be a .c, .f, .ilf, or .ilc files.",
    // HLP_I
    "specify the user include file directory(s)",
    // HLP_Is
    "specify the system include file directory(s)",
    // HLP_A
    "specify annotation file(s)",
    // HLP_D
    "specify macro definitions for the pre-processor",
    // HLP_U
    "specify macros to be be un-defined by the pre-processor\n" +
    "Only pre-processor defined macros can be un-defined.",
    // HLP_t
    "set the classTrace field true for the specified classes - e.g., -t scale.backend.Generator",
    // HLP_f
    "set the specified static members for the specified classes to the\n" +
    "specified values - e.g.,\n" +
    "    -f class.member=3\n" +
    "See also the -flag_file switch.",
    // HLP_inl
    "specify the inlining code bloat limit\n" +
    "0 - none\n" +
    "1 - 1% code bloat\n" +
    "2 - 2% code bloat\n" +
    "3 - 5% code bloat\n" +
    "n - n% code bloat\n" +
    "The compilation time is a non-linear function of the bloat limit.\n" +
    "Use -f scale.score.trans.Inlining.ignoreHeuristics=1 to inhibit\n" +
    "the callee complexity filter.\n\n" +
    "If the -M switch is specified, inlining may be performed between source modules.",
    // HLP_inls
    "specify the file to hold inlining status information",
    // HLP_unused_16
    "unused 16",
    // HLP_pi
    "specify the type(s) profiling instrumentation to add to the program\n" +
    "  b - basic block frequencies\n" +
    "  c - loop instruction counts\n" +
    "  e - edge frequencies\n" +
    "  l - loop trip count histogram\n" +
    "  p - path frequencies\n" +
    "\n" +
    "At the end of program execution, the executing program dumps the profile\n" +
    "to disk in the form of .pft files -- one for each source file.  These\n" +
    "are text files that are pretty easy to read.  The files are placed in the\n" +
    "directory in which the program is executed.\n" +
    "\n" +
    "A second run of the compiler with the -profile_guided switch reads in the\n" +
    "profile from the .pft files and applies it to each routine.  Currently,\n" +
    "all profiling in Scale works only via the multi-compilation route\n" +
    "(i.e., using -pi or -pg automatically activates -M).\n" +
    "\n" +
    "Loop instruction counts statically determine the number of instructions\n" +
    "in a loop.  This is used to guide loop unrolling.  This currently is\n" +
    "implemented only for the Trips architecture.\n" +
    "\n" +
    "A loop histogram for a particular loop is a map from slots to\n" +
    "frequencies; slots with frequencies of zero are not present in the\n" +
    "map.  Each slot corresponds to a single trip count or to a range of trip\n" +
    "counts.  Currently, there are 100 slots, numbered 0 through 99. Slots 1 -\n" +
    "64 are used for trip counts 1 - 64.  (Note that a trip count of 0 is not\n" +
    "possible, since we define the trip count as the number of executions\n" +
    "of the loop header, not the loop back edge.  Slot 0 is not used.)\n" +
    "Slots 65 - 99 are used for trip counts 65 - 2^41 using the mapping function\n" +
    "   f(c) = floor(lg(c - 1)) + 59,\n" +
    "where c is the trip count, and f(c) is the corresponding slot.\n" +
    "\n" +
    "Basic block frequencies are used to rank call sites for inlining.\n" +
    "Loop histograms are used to select unroll factors for individual loops.\n" +
    "Edge frequencies are used for Trips in constructing hyperblocks.",
    // HLP_pp
    "specify the directory/ies to search for profile information\n" +
    "Use the -pg switch to use the profile information.\n" +
    "Defaults to the directory specified by the -dir switch.",
    // HLP_cmi
    "disable cross-module inlining (only applies if -M is selected but -me is not",
    // HLP_M
    "force multi-compilation\n" +
    "Scale supports three modes of compiling: single, batch, and\n" +
    "multi-compilation.  The compiler can be invoked on a single source file\n" +
    "or it can be invoked on multiple source files (batch).  The batch\n" +
    "method saves the overhead of establishing the Java environment\n" +
    "for each source file that is compiled.  If multiple source files\n" +
    "are specified with one invocation of the compiler, multi- (or cross)\n" +
    "compiling can be enabled.\n\n" +
    "Multi-compilation converts all the specified source files to AST form\n" +
    "before generating the CFG form of the programs.  Once all CFGs have\n" +
    "been generated, then the code generators are run.  This allows\n" +
    "inter-procedural optimizations to be performed.  Multi-compilation is\n" +
    "not recommended because of the very large amount of memory required\n" +
    "which results in excessive JVM GCs.\n\n" +
    "Currently, only function inlining can take advantage of multi-compilation\n" +
    "by inlining functions across modules.  To support this multi-compilation\n" +
    "renames all static functions and variables and makes them global.",
    // HLP_unused_21
    "unused 21.",
    // HLP_cat
    "specify the number of categories for Shapiro alias analysis",
    // HLP_stat
    "display status at level\n" +
    "0 - none\n" +
    "1 - normal statistics + compilation progress\n" +
    "2 - spawned processes\n" +
    "n - undefined.",
    // HLP_cdd
    "node display depth\n" +
    "The node display depth controls the debugging displays when print\n" + 
    "statements are added to the compiler that display AST or CFG nodes.",
    // HLP_san
    "show annotations in trace information to this level\n" +
    "This controls the depth to which debugging displays show annotations when\n" + 
    "print statements are added to the compiler that display AST or CFG nodes.",
    // HLP_E
    "run just the C pre-processor.",
    // HLP_sla
    "enable source line number information.",
    // HLP_o 
    "generate .o files",
    // HLP_S 
    "generate .s files only",
    // HLP_oa 
    "generate .o files from generated assembly language files",
    // HLP_oc 
    "generate .o files from generated C language files",
    // HLP_ccb 
    "generate C code from the Clef AST before optimizeClef has been called",
    // HLP_cca 
    "generate C code from the Clef AST after optimizeClef has been called",
    // HLP_cgb 
    "graphically display the Clef AST before the optimizeClef method is called",
    // HLP_cga 
    "graphically display the Clef AST after the optimizeClef method is called",
    // HLP_dcg 
    "graphically display the call graphs",
    // HLP_c89 
    "select the C89 dialect of C",
    // HLP_c99 
    "select the C99 dialect of C",
    // HLP_gcc 
    "select the Gnu dialect of C",
    // HLP_ckr 
    "select the K&R dialect of C",
    // HLP_ansi 
    "specify strict ANSI C mode",
    // HLP_unsafe 
    "allow unsafe optimizations",
    // HLP_scb 
    "generate C code from the CFG before the specified optimizations (see -O)\n" +
    "The generated C code can be limited to just one routine by using the -r switch.",
    // HLP_sca 
    "generate C code from the CFG after the specified optimizations (see -O)\n" +
    "The generated C code can be limited to just one routine by using the -r switch.",
    // HLP_sgb 
    "graphically display the CFG before the specified optimizations (see -O)\n" +
    "The CFG display can be limited to just one routine by using the -r switch.",
    // HLP_sga 
    "graphically display the CFG after the specified optimizations (see -O)\n" +
    "The CFG display can be limited to just one routine by using the -r switch.",
    // HLP_vers 
    "print compiler version",
    // HLP_snap 
    "deprecated - enable snapshot statistics",
    // HLP_gdb 
    "generate debug information",
    // HLP_naln 
    "assume non-ANSI C data alignment on indirect references",
    // HLP_is 
    "enable instruction scheduling",
    // HLP_bi 
    "inhibit inlining of certain standard C library routines",
    // HLP_ph 
    "perform peephole optimizations",
    // HLP_unused_54 
    "unused_54",
    // HLP_np 
    "deprecated.",
    // HLP_quiet 
    "inhibit informative messages",
    // HLP_hda 
    "Assumes that dummy (formal) arguments to procedures share memory\n" +
    "locations with other dummy arguments or with COMMON variables\n" +
    "that are assigned.  These program semantics slow performance and\n" +
    "do not strictly obey the FORTRAN-77 Standard.  The default is to\n" +
    "assume there are no dummy aliases.",

    // HLP_fpr 
    "allows floating point operations to be reordered during optimizations\n" +
    "which may affect the results",
    // HLP_hyper 
    "enable backend hyperblock formation",
    // HLP_wrap 
    "use the following integer overflow behavior\n" +
    "all      - all integers wrap on overflow (safest, slowest)\n" +
    "unsigned - unsigned integers wrap on overflow, signed may not\n" +
    "none     - all integers may not wrap on overflow (not valid for C)\n",

    // HLP_arch 
    "generate code for this architecture\n" +
    "alpha  - EV5 processor\n" +
    "sparc  - V8 or V9 processor using V8 ABI\n" +
    "mips   - incomplete implementation\n" +
    "ppc    - G4 processor - under development\n" + 
    "trips2 - UT TRIPS EDGE architecture - under development\n" +
    "Note - if this parameter is not specified the compiler obtains the\n" +
    "architecture from the os.arch property.  The architecture (e.g., xxx)\n" +
    "is used to find the code generator for that architecture.  There must\n" +
    "be a scale.backend.xxx directory and there must ba a xxxMachine class\n" +
    "in that directory.",

    // HLP_cc 
    "specify the native C compiler to use when the -oc switch is used",
    // HLP_asm 
    "specify the assmebler command to use when the -oa switch is specified",
    // HLP_unused_64 
    "unused 64",
    // HLP_unused_65 
    "unused 65",
    // HLP_dir 
    "specify the directory to place any generated files",
    // HLP_r 
    "limit optimizations, graphic displays, etc to only the source routine\n" +
    "specified by this name",
    // HLP_for 
    "a name to use to tag statistical output - see -stat",
    // HLP_ff
    "load a flag settings file\n" +
    "A flag settings file contains a list of compiler flags and values\n" +
    "with one flag and value per line of the file.  For example, the\n" +
    "following lines would set the indicated static members of the indicated\n" +
    "classes to the specified value:\n" +
    "   scale.score.trans.Inlining.ignoreComplexityHeuristic=0\n" +
    " scale.score.pp.PPCfg.pgp =1\n" +
    "   scale.score.Scribble.doIfConversion= 0\n" +
    "    scale.score.trans.URJ.defaultMinUnrollFactor = 6\n" +
    "The compiler reads the file and sets the specified flags to the specified\n" +
    "value.  Boolean true and false values are respresented by 1 and 0.\n" +
    "See also the -f switch.",
    // HLP_pg
    "Use profile information, previously generated, as a guide in compiling\n" +
    "See the -pi switch for a list of instrumentation information that can\n" +
    "be used.  Use the -profile_instrumentation switch to instrument\n" +
    "the program.",
    // HLP_AA 
    "specify alias analysis level\n" +
    "0 - No alias analysis,\n" +
    "1 - Simple intra-procedural alias analysis,\n" +
    "2 - Steensgard intra-procedural alias analyses,\n" +
    "3 - Shapiro intra-procedural alias analyses,\n" +
    "4 - Simple inter-procedural alias analysis,\n" +
    "5 - Steensgard inter-procedural alias analysis,\n" +
    "6 - Shapiro inter-procedural alias analysis",
    // HLP_O
    "specify optimizations\n" +
    "letter optimization\n" +
    "------ ---------------------------------------\n" +
    "0      No optimizations\n" +
    "1      Same as -O cmnpud\n" +
    "2      Same as -O gacmnpibudl\n" +
    "3      Same as -O fgjacmnpmxnpibudl\n" +
    "4      Same as -O fgjacmnpxmnpibudl\n" +
    "a      Array Access Strength Reduction\n" +
    "b      Basic Block Optimizations\n" +
    "c      Sparse Conditional Constant Propagation\n" +
    "d      Dead Variable Elimination\n" +
    "e      Partial Redundancy Elimination\n" +
    "f      Structure Fields In Registers\n" +
    "g      Global Variable Replacement\n" +
    "i      Expression Tree Height Reduction\n" + 
    "j      Loop Unrolling\n" +
    "l      Loop Test at End\n" +
    "m      Loop Invariant Code Motion\n" +
    "n      Global Value Numbering\n" +
    "p      Copy Propagation\n" +
    "s      None\n" +
    "t      Loop Permutation\n" +
    "u      Useless Copy Removal\n" +
    "x      Scalar Replacement",
    // HLP_help
    "display this help information",
    // HLP_vcc
    "generate status information",
    // HLP_ccc
    "specify .c file",
    // HLP_unused_76
    "unused 76",
    // HLP_occ
    "specify .o file",
    // HLP_Occ
    "specify optimization level - 0, 1, 2, 3, or 4.",
    // HLP_phelp
    "\n    Use\n" +
    "        java %s -help parameter\n" +
    "    to display help on the specified parameter.  Or, use\n" +
    "        java %s -help all\n" +
    "    to display help on the all the parameters.\n\n" +
    "    Parameters containing words separated by a '_' or '-'\n" +
    "    can be abbreviated using the first letter of each word.\n\n" +
    "    The '_' and '-' characters may be used interchangeably\n" +
    "    in a parameter name.\n\n" +
    "    Parameters that do not accept an argument may be prefaced\n" +
    "    with \"no_\" (long form) or \"n\" (abbreviated form)\n" +
    "    to specify turning off that switch.",
    // HLP_W
    "do not print warning messages",
    // HLP_vcg
    "output graphs to files in VCG format",
    // HLP_icf
    "inline complex functions",
    // HLP_sc
    "specifies whether or not the *char* type is signed\n" +
    "The -sc switch specifies that type *char* has the same range, representation,\n" +
    "and behavior as type *signed char*. The -uc switch specifies that type *char*\n" +
    "has the same range, representation, and behavior as type *unsigned char*.",
    // HLP_sf
    "specifies the file to receive the compiler statistics\n" +
    "The default is stdout.",
    // HLP_dm
    "display all macro definitions\n" +
    "If used with -E, the macro definitions are sent to stdout\n" +
    "without the preprocessed text of the program. Otherwise, the\n" +
    "macro definitions are placed as comments in the generated C or\n" +
    "assembly code.  This switch is ignored if the EDG parser is used.",
    // HLP_ih
    "ignore heuristics in the specified optimizations\n" +
    "Most optimizations increase register pressure which can result in register\n" +
    "spilling and degraded performance.  These optimizations employ heuristics\n" +
    "to minimize this degraded performance.",
    // HLP_phb
    "Use an edge profile to guide hyperblock formation",
    // HLP_nw
    "Tell the compiler to ignore specific warnings\n" +
    "   -nw 166       - disable warning number 166\n" +
    "   -nw 166 167   - disable warning numbers 166 167\n" +
    "   -nw all       - disable all warnings",
    // HLP_suspend
    "wait for the user to give the go ahead\n" +
    "Clears out any references and waits for the user before terminating\n" +
    "the compiler. This switch is provided so that the user can\n" +
    "interrogate any tools, such as JMP, that rely on the JVM\n" +
    "Profiling Interface.",
  };

  protected String[] getMessages()
  {
    return msgs;
  }

  protected String[] getHelpMessages()
  {
    return helpMsgs;
  }

  protected String getErrorString()
  {
    return "Error";
  }

  protected String getWarningString()
  {
    return "Warning";
  }

  protected String getInfoString()
  {
    return "Info";
  }
}
