package scale.test;

import java.io.*;
import java.lang.Runtime;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import java.util.StringTokenizer;

import scale.annot.*;
import scale.common.*;
import scale.clef.*;
import scale.clef.symtab.*;
import scale.clef.decl.*;
import scale.clef.type.*;
import scale.callGraph.*;
import scale.alias.*;
import scale.alias.steensgaard.*;
import scale.alias.shapirohorowitz.*;
import scale.j2s.*;

import scale.visual.*;
import scale.score.*;
import scale.score.chords.Chord;
import scale.score.pred.*;
import scale.score.expr.*;
import scale.score.analyses.*;
import scale.score.trans.*;
import scale.score.dependence.DDGraph;
import scale.score.pp.PPCfg;
import scale.backend.Generator;
import scale.clef2scribble.Clef2Scribble;

import scale.frontend.*;

/**
 * This class provides the top-level control for the Scale compiler.
 * <p>
 * $Id: Scale.java,v 1.335 2007-10-04 19:53:33 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * <h2>Top Level</h2>
 * <img src="../../HighLevelDataFlow.jpg" alt="High-level Dataflow Diagram">
 * <p>
 * Like all compilers the Scale compiler follows the following high
 * level flow.  The front end of the compiler parses the source
 * program and converts it into the form upon which the compiler
 * performs optimizations.  Once the program has been transformed by
 * the various optimizations, it is converted to the machine language
 * of the target system.  Scale generates assembly language and calls
 * the native assembler to generate the actual object modules (i.e.,
 * <code>.o</code> files).
 * <p>
 * While not shown on this diagram or the more detailed diagrams of
 * the major parts of the compiler, it is possible to generate
 * <code>.c</code> files and/or graphical displays after any part of
 * the compiler.  These <code>.c</code> files and graphical displays
 * are used primarily as an aid to debugging the compiler.  Command
 * line switches are provided to control the generation of the files
 * and displays.
 * <p>
 * <h2>Front End</h2>
 * <p>
 * The front end parses the source program and converts it to a
 * high-level, program-language dependent form called the Abstract
 * Syntax Tree (AST) which we call Clef.  This
 * AST form is then converted to a machine-independent form called the
 * Control Flow Graph (CFG) which we call {@link scale.score.Scribble
 * Scribble}.
 * <p>
 * The parsers generates an AST file which Scale then
 * reads and converts to Clef.  The Clef AST is then converted to Scribble
 * by the {@link scale.clef2scribble.Clef2Scribble Clef2Scribble}
 * package.
 * <p>
 * <h2>Optimizations</h2>
 * <img src="../../Optimizations.jpg" alt="Optimization Flow Chart">
 * <p>
 * Scale provides many different optimizations.  (The optimizations
 * performed and the order in which they are performed is controllable
 * by command line switches.)  Most Scale optimizations are performed
 * when the CFG is in Static Single Assignment ({@link scale.score.SSA
 * SSA}) form.  The Scale compiler will converted the CFG to and from
 * this form as needed.
 * <p>
 * <h2>Back End</h2>
 * <p>
 * The Scale back end is very simple conceptually.  The {@link
 * scale.backend.Generator Generator} module simply converts from the
 * CFG to assembly language.  The back end converts the CFG into
 * machine specific instructions.  However, the {@link
 * scale.backend.Instruction instruction representation} allows the
 * instructions to be manipulated in a machine independent way.  (This
 * <i>object-oriented</i> approach is used by the {@link
 * scale.backend.RegisterAllocator register allocator}.)  Once the
 * instructions are generated and the registers are allocated the
 * sequence of instructions is converted to assembly language by the
 * {@link scale.backend.Assembler Assembler} module.
 * <p>
 *  Currently Scale can generate assembly language for four different
 * systems:
 * <ol>
 * <li>{@link scale.backend.alpha Alpha EV5}
 * <li>{@link scale.backend.sparc Sparc V8}
 * <li>{@link scale.backend.ppc PowerPC}
 * <li>{@link scale.backend.mips Mips M10000} (incomplete)
 * <li>{@link scale.backend.trips2 Trips}
 * </ol>
 * Others may be {@link scale.backend added}.
 * <p>
 * <h2>Command Line Switches</h2>
 * <p>
 * Execute
 * <pre>
 *  java scale.test.Scale -help
 * </pre>
 * to see the list of command line switches available.
 * <p>
 * <h2>Cross-compiling</h2>
 * <p>
 * Scale can be used as a cross compiler when the host machine
 * architecture is different from the target machine architecture.
 * The target machine architecture is specified using the
 * <tt>-arch</tt> <i>architecture</i> command line switch.  When
 * Scale is used as a cross compiler the include file directories must
 * be explicitly enumerated using the -I command line switch to
 * specify include files for the target system.  Also, since Scale
 * produces assembly language files, and calls the assembler to
 * convert them to <tt>.o</tt> files, the <tt>-oa</tt> switch cannot
 * be used; use only the <tt>-a</tt> or <tt>-c</tt> command line
 * switches.
 * <h1>Scale Data Flow</h1>
 * <p>
 * This page describes the data flow in the Scale compiler at a macro
 * level.  At this level the data flow is basically a pipeline so no
 * diagram is provided.  At a lower level, each component of the data
 * flow of the compiler is best described using control flow.
 * <p>
 * <h2>Data Flow Outline</h2>
 * <STYLE type="text/css">
 * OL.withnumber { list-style-type: arabic }
 * OL.withucl { list-style-type: upper-alpha }
 * OL.withlcl { list-style-type: lower-alpha }
 * </STYLE>
 * <ol class="withnumber">
 * <li> <a href="#Frontend">Front End</a>
 * <ol class="withucl">
 * <li> <a href="#C99">C99</a>
 * <li> <a href="#F95">F95</a>
 * </ol>
 * <li> <a href="#Clef2Scribble">Clef2Scribble</a>
 * <ol class="withucl">
 * <li> <a href="#ConversionASTCFG">Conversion of AST to CFG</a>
 * <li> <a href="#simpleifconversion">Simple if-conversion and
 * if-combining</a>
 * <li> <a href="#conversionirreducible">Conversion of irreducible
 * graphs to reducible graphs</a>
 * <li> <a href="#conversionimplicitloops">Conversion of implicit
 * loops to explicit loops</a>
 * <li> <a href="#insertionloopexits">Insertion of missing loop
 * exits</a>
 * </ol>
 * <li> <a href="#Inlining">Inlining</a>
 * <li> <a href="#AliasAnalysis">Alias Analysis</a>
 * <li> <a href="#ScalarOptimizations">Scalar Optimizations</a>
 * <li> <a href="#Hyperblocks">Hyper-Blocks</a>
 * <ol class="withucl">
 * <li> <a href="#CreateHyperblocks">Create Hyperblocks and Add Predication</a>
 * <li> <a href="#PredicationOptimizations">Predication Optimizations</a>
 * <li> <a href="#ReverseIf-conversion">Reverse If-conversion</a>
 * </ol>
 * <li> <a href="#Profiling">Profiling</a>
 * <li> <a href="#Backend">Backend</a>
 * <ol class="withucl">
 * <li> <a href="#Instructions">Instruction generation</a>
 * <li> <a href="#RegisterAllocation">Register Allocation</a>
 * <li> <a href="#TILGeneration">TIL Generation</a>
 * </ol>
 * <li> <a href="#PostCompiler">Post Compiler</a>
 * <ol class="withucl">
 * <li> <a href="#TripsScheduler">Trips Scheduler</a>
 * <li> TASL Generator
 * <li> TASL Assembler
 * <li> Trips Linker
 * <li> Trips Loader
 * </ol>
 * </ol>
 * <h2><a name="Frontend">Frontend</h2>
 * <p>
 * 
 * The front end converts from the source language program to an internal
 * representation called the <i>abstract syntax tree</i> (AST).  The AST
 * is <b>language dependent</b>.  Conceptually, this form is a tree but
 * for practical reasons a <i>directed acyclic graph</i> (DAG)
 * representation is used so that some leaf nodes, such as constants and
 * types, do not need to be duplicated.
 * 
 * <p>
 * 
 * The parsers convert the source program to the Scale form of the AST
 * which is called Clef (Common Language Encoding Form).  Currently,
 * two parsers are available.  Others may be {@link scale.frontend
 * added}.

 * <p>
 * <h3><a name="C99">C99</h3>
 * <p>
 * Scale uses the <a href="http://www.antlr.org">ANTLR</a> tool to
 * create the {@link scale.frontend.c C parser}.  This parser handles
 * standard C, K&amp;R C, and C99 along with some GNUisms.
 * <p>
 * <h3><a name="F95">F95</h3>
 * <p>
 *
 * The {@link scale.frontend.fortran Fortran parser} uses recursive
 * descent and handles Fortran 77 with Fortran F90 under development.
 * 
 * <p>
 * <h2><a name="Clef2Scribble">Clef2Scribble</h2>
 * <p>
 * 
 * {@link scale.clef2scribble Clef2Scribble} converts from the AST to
 * the <i>control flow graph</i> (CFG).  The CFG is called
 * <i>scribble</i> (for no currently valid reason) and is language
 * <b>independent</b> because <code>Clef2Scribble</code> performs
 * lowering.  (It is also supposed to be <b>architecture
 * independent</b> but is not completely due to lowering performed by
 * the front end parsers and the architecture-and-operating-system
 * dependent <code>include</code> files used by C programs.)
 * <p>
 * A basic block is defined as a sequence of CFG nodes such that:
 * <ul>
 * <li>A basic block begins with a
 * node that has more than one in-coming graph edge or whose parent has
 * more than one out-going graph edge.
 * <li>A basic block ends with a node
 * that has more than one out-going graph edge or whose successor node
 * has more than one in-coming graph edge.
 * </ul>
 * In most C programs basic blocks are on the order of 10 to 15
 * machine instructions.
 * <p>
 * <h3><a name="ConversionASTCFG">Conversion of AST to CFG</h3>
 * <p>
 * The lowering performed converts <code>do</code>-loops,
 * <code>for</code>-loops, etc to a common form where special nodes are
 * inserted into the CFG to mark the start of a loop and the exits from a
 * loop.
 * <p>
 * 
 * Lowering also converts complicated forms to simpler forms.  For example,
 * <pre>
 *    if (exp1 && exp2) {
 *      body;
 *    }
 * </pre>
 * is converted to
 * <pre>
 *    if (exp1) {
 *     if (exp2) {
 *       body;
 *     }
 *   }
 * </pre>
 * <p>
 * <h3><a name="simpleifconversion">Simple if-conversion and if-combining</h3>
 * <p>
 * Once the CFG is complete if-conversion and if-combining are
 * performed in order to create larger basic blocks.
 * <p>
 * Simple if-conversion is performed for target architectures that have a
 * conditional move capability.  A conditional move provides the
 * capability to select one out of a set of values without performing a
 * branch.  For example,
 * 
 * <pre>
 *    if (exp) {
 *      a = 1;
 *    } else
 *      a = 2;
 *    }
 * </pre>
 * is converted to
 * <pre>
 *    a = exp ? 1 : 2;
 * </pre>
 * 
 * In order for this conversion to take place, there can be no
 * side-effects from the right-hand-sides of the assignment statements
 * because the right-hand-side of the assignments are always evaluated.
 * <p>
 * To make the if-conversion more productive, if-combining is also performed.
 * For example,
 * <pre>
 *    if (A) {
 *      if (B) {
 *        body;
 *      }
 * }
 * </pre>
 * is converted to
 * <pre>
 *    if (A & B) {
 *      body;
 *    }
 * </pre>
 * 
 * (<b>Note</b> - the operation is simple "and" - not the C conditonal
 * "and".)  For this conversion to take place neither expression
 * <code>A</code> or <code>B</code> can cause a side-effect because both
 * are always evaluated.
 * 
 * <p>
 * Note - this if-combining is the opposite operation to the lowering of
 * complex if-forms performed earlier.  But, it does not reverse all such
 * conversions <b>and</b> it converts some original programmer written
 * forms.
 * <p>
 * <h3><a name="conversionirreducible">Conversion of irreducible graphs to reducible graphs</h3>
 * <p>
 * 
 * Programmers often write <i>spaghetti</i> code using <code>goto</code>
 * statements.  This can result in an <i>irreducible</i> graph.  An
 * irreducible graph is one with two nodes (<code>x</code> &amp;
 * <code>y</code>) that are each in the <i>dominance frontier</i> of the
 * other.
 * <p>
 * In order to have <i>well-delineated</i> loops in the CFG, Scale
 * converts irreducible</i> graphs to reducible graphs using <i>node
 * splitting</i>.  Node splitting duplicates parts of the graph so
 * that node <code>x</code> is in the dominance frontier of node
 * <code>y</code> and node <b
 * style="color:red"><code>y&acute;</code></b> is in the dominance
 * frontier of node x.
 * 
 * <p>
 * <h3><a name="conversionimplicitloops">Conversion of implicit loops to explicit loops</h3>
 * <p>
 * Programmers often write <i>spaghetti</i> code using <code>goto</code>
 * statements.  This can result in non-structured loops.  In order to
 * have <i>well-delineated</i> loops in the CFG, Scale converts
 * non-structured loops to structured loops by inserting the loop
 * markers.
 * <p>
 * <h3><a name="insertionloopexits">Insertion of missing loop exits</h3>
 * <p>
 * Programmers often write <i>spaghetti</i> code using <code>goto</code>
 * statements.  This can result in exits from loops that are not marked.
 * In order to have <i>well-delineated</i> loops in the CFG, Scale
 * detects such exits and marks them with loop exit nodes.
 * <p>
 * <h2><a name="Inlining">Inlining</h2>
 * <p>
 * Inlining inserts the body of a function in place of a call to that
 * function and renames the variables in the inlined body appropriately.
 * In order to inline a function the body (CFG) of that function must be
 * available.  Scale restricts inlining to non-recursive functions below
 * a user-selectable size.  And, Scale will not expand a CFG above a
 * certain user-selectable multiple.
 * <p>
 * <h2><a name="AliasAnalysis">Alias Analysis</h2>
 * <p>
 * Alias analysis detects aliases between variables and references using
 * C pointers.  When there is such an alias, performing optimizations
 * involving the variable may lead to incorrect code.
 * <p>
 * <h2><a name="ScalarOptimizations">Scalar Optimizations</h2>
 * <p>
 * 
 * Scalar optimizations are performed in the order specified by the user
 * of the compiler.  The CFG is converted, as needed, between the SSA
 * form of the CFG and the normal form.  The optimizations make use of
 * various information about the program.  Some of this information is
 * computed as needed.
 * 
 * <p>
 * On-demand services:
 * <ul>
 * <li> Computation of Pre- and Post-Dominators for every CFG Node
 * <li> Computation of the Dominance Frontier for every CFG Node
 * <li> Computation of the References for every variable by CFG Node
 * <br><b>Note:</b> This is <b>not</b> liveness information.
 * </ul>
 * Various optimizations invalidate this information.  Therefore, it
 * is re-computed when and as needed.
 * <p>
 * The scalar optimizations include:
 * <dl>
 * <dt><b>Array Access Strength Reduction</b><dd>Converts calculation
 * of array element address to simple adds
 * <dt><b>Sparse Conditional Constant Propagation</b><dd>Converts
 * constant expressions to constants and eliminates dead branches in
 * the CFG
 * <dt><b>Dead Variable Elimination</b><dd>Eliminates variables that
 * are not used
 * <dt><b>Partial Redundancy Elimination</b><dd>Eliminates common
 * sub-expressions under special circumstanes
 * <dt><b>Global Variable Replacement</b><dd>Converts references to
 * global variables to references to local copies that can be
 * optimized
 * <dt><b>Loop Flatten</b><dd>Replicate the loop body of loops with
 * known iteration counts and eliminates the looping
 * <dt><b>Loop Unrolling</b><dd>Replicates the loop body
 * <code>n</code> times to increase basic block size
 * <dt><b>Loop Invariant Code Motion</b><dd>Moves loop invariants
 * outside of the loop
 * <dt><b>Global Value Numbering</b><dd>Eliminates common
 * sub-expressions
 * <dt><b>Copy Propagation</b><dd>May eliminates un-necessary copy
 * operations
 * <dt><b>Loop Permutation</b><dd>Attempts to improve cache hits by
 * interchanging loops
 * <dt><b>Useless Copy Removal</b><dd>Eliminates an artifact created
 * by SSA form
 * <dt><b>Scalar Replacement</b><dd>Replaces references to array
 * elements by references to scalar variables
 * </dl>
 * <p>
 * 
 * SSA form is a significantly more complicated form of the CFG.  In this
 * form, it is very difficult to add new CFG paths.  SSA form provides
 * liveness information by converting to a form where
 * <i>local</i> variables are set only once and links are provided
 * between the set and each use of the variable.  There are often un-wanted
 * results when the SSA form is converted back to the normal CFG form.
 * For example, if an optimization moves a reference to a variable it may
 * result in an additional local variable and copy operation.
 * 
 * <p>
 * <h2><a name="Hyperblocks">Hyper-Blocks</h2>
 * <p>
 * On architectures that have a predication capability several small
 * <i>basic blocks</i> may be combined into a larger <i>hyper-block</i>
 * using predication.  For example, 
 * <pre>
 *    if (exp) {
 *      body1;
 *    } else {
 *      body2;
 *    }
 * </pre>
 * consists of 3 basic blocks.
 * 
 * <p>
 * Using predication, larger blocks can be constructed with the result
 * that there are fewer branch instructions and, as a consequence, fewer
 * branch mis-predictions.  In a predicated architecture, the above
 * example can be converted to 
 * <pre>
 *    p = exp != 0;
 *    if (p) body1;
 *    if (!p) body2;
 * </pre>
 * where the <code>if</code> is a special predicate operation and does
 * not result in a branch.  This differs from the above if-conversion
 * in that the <code>body</code> may cause side effects; the
 * <code>body</code> is not evaluated unless the predicate
 * (<code>p</code> or <code>!p</code>) is true.
 * <p>
 * <h3><a name="CreateHyperblocks">Create Hyperblocks and Add Predication</h3>
 * <p>
 * Scale creates the largest hyper-blocks possible under the following
 * constraints:
 * <ul>
 * 
 * <li> The resulting hyper-block will not result in more instructions
 * than a user-definable limit.  The number of instructions will be an
 * estimate based on a statistical analysis of the estimated number of
 * instructions that will be generated.  For Trips, since Scale generates
 * TIL, even an absolute count of the resulting TIL instructions is still
 * an estimate.
 * 
 * <li> A loop will always start a new hyper-block.
 * 
 * <li> Any available profiling information will be used to select the
 * basic blocks to be combined into a hyper-block.
 * </ul>
 * 
 * No special <code>Hyperblock</code> class or CFG markers are used to
 * delineate hyper-blocks.  A hyper-block is simply a normal Scale
 * basic block as defined above but which uses <i>predicated
 * stores</i>.  A predicated store is specified using the
 * <code>ExprChord</code> class and includes an additional in-coming
 * data-edge that is the predicate value.  For example,
 * 
 * <pre>
 *    if (pexp) {
 *      a = exp1;
 *      b = exp2;
 *    } else {
 *      c = exp3;
 *    }
 * </pre>
 * 
 * is converted to 
 * 
 * <pre>
 *    t = pexp;
 *    if (t) a = exp1;
 *    if (t) b = exp2;
 *    if (!t) c = exp3;
 * </pre>
 * where the <code>if</code> is the special predicated store
 * expression with three in-coming data edges (e.g., <code>t</code>,
 * <code>a</code>, and <code>exp1</code>).
 * 
 * <p>
 * Each predicated store is converted by the
 * architecture-specific backend to the appropriate code.  For example,
 * the backend that generates C code converts a
 * predicated store to
 * 
 * <pre>
 *     if (predicate) lhs = rhs;
 * </pre>
 * 
 * where <code>lhs</code> is the left-hand-side expression of the store
 * and <code>rhs> is the right-hand-side expression.
 * 
 * <p>
 * <h3><a name="PredicationOptimizations">Predication Optimizations</h3>
 * <p>
 * TBD
 * <p>
 * <h3><a name="ReverseIf-conversion">Reverse If-conversion</h3>
 * <p>
 * 
 * For systems that do not have predication the paper <cite>"A Framework
 * for Balancing Control Flow and Predication"</cite> by D. August, et al
 * indicates that some benefit may be obtained by forming hyper-blocks,
 * performing the predicated optimizations, and then converting back into
 * un-predicated code.  Therefore, if hyperblocks have been formed and
 * the target architetcure does not support predication, this pass of the
 * compiler will remove the predication.
 * 
 * <p>
 * <h2><a name="Profiling">Profiling</h2>
 * <p>
 * Profiling instruments the CFG by adding CFG nodes that generate
 * profiling information.  Block, edge, path, and loop profiling are
 * available profiling options.
 * <p>
 * <h2><a name="Backend">Backend</h2>
 * <p>
 * The backend converts the <b>architecture independent</b> CFG into
 * <b>architecture dependent</b> instructions.  For Scale, the backend
 * generates an assembly language file and expects that file to be
 * processed by a native assembler tool.
 * <p>
 * The definition of a basic block from this point on is slightly
 * different;  The new definition is that a basic block is defined as a
 * sequence of instructions such that:
 * <ul>
 * <li>A basic block begins swith a label that has more than
 * one in-coming graph edge or whose parent has more than one out-going
 * graph edge <i>or its predecessor is a call instruction</i>.
 * <li>A basic block ends with an instruction that has more than one
 * out-going graph edge or whose successor node has more than one
 * in-coming graph edge <i>or is a call instruction</i>.
 * </ul>
 * <p>
 * <h3><a name="Instructions">Instruction generation</h3>
 * <p>
 * Each CFG node is converted to a sequence of instructions which
 * reference <i>virtual</i> registers.  Using virtual registers provides
 * an infinite number of registers which results in much simpler
 * instruction generation.
 * <p>
 * For predicated store instances the appropriate
 * predicated instructions are generated.  Note, even though a complete
 * right-hand-side of an assigment is predicated, it is not necessary to
 * predicate every instruction that results from that right-hand-side.
 * Those instructions that do not cause side effects (e.g., a load or an
 * integer add) on the target architecture do not need to be predicated.
 * The fan-out of the predicated value is reduced by not predicating
 * these instructions.
 * <p>
 * <h3><a name="RegisterAllocation">Register Allocation</h3>
 * <p>
 * References to virtual registers are converted to references to
 * actual hardware registers on the target architecture.  An
 * <b>architecture independent</b> register allocated is used.  It
 * knows only whether an instructions defines or uses a specific
 * register (real or virtual).  The register allocator is able to
 * construct register live range information from this.
 * 
 * <p>
 * For Trips, only registers that are live at the beginning of a basic
 * block are allocated.  It is assumed that the remaining <i>virtual</i>
 * registers will become dataflow edges in the Trips grid.  It is
 * possible for the register allocator to annotate each basic block with
 * this liveness information.
 * <p>
 * <h3><a name="TILGeneration">TIL Generation</h3>
 * <p>
 * The instruction sequence is converted to the assembly language of the
 * target architecture.  For Trips this is the Trips Intermediate
 * Language which is intended to be human readable and writable.
 * <p>
 * <h2><a name="PostCompiler">Post Compiler</h2>
 * <p>
 * These steps occur after the compiler is used to generate an
 * assembly language file.
 * <p>
 * <h3><a name="TripsScheduler">Trips Scheduler</h3>
 * <p>
 * The Trips scheduler is responsible for converting the instructions of
 * Trips Intermediate Language, that are in <i>operand</i> form, to the
 * actual Trips instructions, that are in <i>target</i> form.  The
 * scheduler must then assign these instructions to the grid and handle
 * the fan-out issues, etc.  The scheduler must do this only on a basic
 * block by basic block basis.
 * <p>
 * It is possible that a basic block may be too large to schedule on the
 * grid.  In this case, either the estimates used by the compiler must be
 * adjusted so that this does not occur.  Or, the scheduler must be able
 * to split a basic block into two blocks and must be able to do a
 * reverse if-conversion (see above) to remove the predication as needed.
 * <p>
 * Splitting a basic block into two or more parts <i>does not
 * introduce any register allocation problems</i>.  Data edges between
 * the two parts must be passed in global registers.  If the scheduler
 * knows which global registers are live at the begining of the
 * original block, it need only use registers that are not live to
 * hold the values between the two parts.  No other register
 * allocation need be performed and no spilling results.
 * <p>
 * It is possible, <i>but unlikely</i> that there will be insufficient
 * registers available.  It should be possible to prevent this problem
 * by analysis during hyper-block formation.
 */

public class Scale
{
  /**
   * True if traces are to be performed.
   */
  public static boolean classTrace = false;

  private static int    globalVariablesCount = 0; // A count of all the defined global variables.
  private static String ddtests              = "";
  private static String optString            = "";

  private static final String[] stats = {
    "globalVariables",
    "dd",
    "opts",
    "version"};

  static
  {
    Statistics.register("scale.test.Scale", stats);
  }

  /**
   * Return the number of data dependence level selected.
   */
  public static String dd()
  {
    return ddtests;
  }

  /**
   * Return the optimizations in use.
   */
  public static String opts()
  {
    return optString;
  }

  /**
   * Return the number of global variables.
   */
  public static int globalVariables()
  {
    return globalVariablesCount;
  }

  /**
   * Return the compiler version.
   */
  public static String version()
  {
    return version;
  }

  /**
   * The machine the compiler is running on.
   */
  public final static String hostArch = System.getProperty("os.arch");
  /**
   * The operating the compiler is running on.
   */
  public final static String hostOS   = System.getProperty("os.name");
  /**
   * The version of the compiler.
   */
  public final static String version  = " Mon Feb 7 00:45:42 CST 2005 ";
  
  private final static String optimizationLetter = "abcdefgijlmnpstux";
  private final static String[] optimizationName = {
    "Array Access Strength Reduction",
    "Basic Block Optimizations",
    "Sparse Conditional Constant Propagation",
    "Dead Variable Elimination",
    "Partial Redundancy Elimination",
    "Structure Fields in Registers",
    "Global Variable Replacement",
    "Expression Tree Height Reduction",
    "Flatten, Unroll & Jam",
    "Loop Test at End",
    "Loop Invariant Code Motion",
    "Global Value Numbering",
    "Copy Propagation",
    "None",
    "Loop Permutation",
    "Useless Copy Removal",
    "Scalar Replacement"
   };

  private final static String[] optimizationClass = {
    "AASR",
    "BasicBlockOps",
    "SCC",
    "DeadVarElimination",
    "PRE",
    "SFIR",
    "GlobalVarReplacement",
    "TreeHeight",
    "URJ",
    "????",
    "LICM",
    "ValNum",
    "CP",
    "Noopt",
    "LoopPermute",
    "UselessCopy",
    "ScalarReplacement"
  };

  private static final String[] cannedOpts = {"",                    // -O0
                                              "cmnpud",              // -O1
                                              "gcamnpibudl",         // -O2
                                              "tfgjcamnpxmnpibudl",  // -O3
                                              "tfgjcamnpxmnpibudl"}; // -O4

  private final static int[] aliasAnalysisLevelMsg = {
    Msg.MSG_No_alias_analysis,                                         
    Msg.MSG_Simple_intra_procedural_alias_analysis,                                 
    Msg.MSG_Steensgard_intra_procedural_alias_analyses,
    Msg.MSG_Shapiro_intra_procedural_alias_analyses,              
    Msg.MSG_Simple_inter_procedural_alias_analysis,                   
    Msg.MSG_Steensgard_inter_procedural_alias_analysis,
    Msg.MSG_Shapiro_inter_procedural_alias_analysis,               
  };

  private final static boolean[] aliasAnalysisIsInter = {false, false, false, false, true,  true,  true};
  private final static boolean[] aliasAnalysisIsSimpl = {false, true,  false, false, true,  false, false};
  private final static boolean[] aliasAnalysisIsSteen = {false, false, true,  false, false, true,  false};
  private final static boolean[] aliasAnalysisIsShapi = {false, false, false, true,  false, false, true};

  private static long ct; // Current time in milliseconds.

  protected static final Integer int0 = new Integer(0);
  protected static final Integer int1 = new Integer(1);
  protected static final Integer int2 = new Integer(2);
  protected static final Integer int4 = new Integer(4);
  protected static final Boolean on   = Boolean.TRUE;
  protected static final Boolean off  = Boolean.FALSE;

  //                                                                 o
  //                                                                 p                      d
  //                                                                 t                      e
  //                                                                 i                      f
  //                                      n                          o     t                a         h
  //                                      a                          n     y                u         e
  //                                      m                          a     p                l         l
  //                                      e                          l     e                t         p
  public CmdParam cpInl    = new CmdParam("inl",                     true, CmdParam.INT,    int0,     Msg.HLP_inl);
  public CmdParam cpAA     = new CmdParam("Alias_Analysis",          true, CmdParam.INT,    int1,     Msg.HLP_AA);
  public CmdParam cpCat    = new CmdParam("cat",                     true, CmdParam.INT,    int2,     Msg.HLP_cat);
  public CmdParam cpDebug  = new CmdParam("d",                       true, CmdParam.INT,    int0,     Msg.HLP_d);
  public CmdParam cpStat   = new CmdParam("stat",                    true, CmdParam.INT,    int0,     Msg.HLP_stat);
  public CmdParam cpCdd    = new CmdParam("clef_display_depth",      true, CmdParam.INT,    int4,     Msg.HLP_cdd);
  public CmdParam cpSan    = new CmdParam("show_annotation_nodes",   true, CmdParam.INT,    int0,     Msg.HLP_san);
  public CmdParam cpFiles  = new CmdParam("files",                   true, CmdParam.LIST,   null,     Msg.HLP_files);
  public CmdParam cpIncl   = new CmdParam("I",                       true, CmdParam.LIST,   null,     Msg.HLP_I);
  public CmdParam cpIncls  = new CmdParam("I-",                      true, CmdParam.LIST,   null,     Msg.HLP_Is);
  public CmdParam cpAnnot  = new CmdParam("A",                       true, CmdParam.LIST,   null,     Msg.HLP_A);
  public CmdParam cpD      = new CmdParam("D",                       true, CmdParam.LIST,   null,     Msg.HLP_D);
  public CmdParam cpU      = new CmdParam("U",                       true, CmdParam.LIST,   null,     Msg.HLP_U);
  public CmdParam cpTcl    = new CmdParam("t",                       true, CmdParam.LIST,   null,     Msg.HLP_t);
  public CmdParam cpFcl    = new CmdParam("f",                       true, CmdParam.LIST,   null,     Msg.HLP_f);
  public CmdParam cpPp     = new CmdParam("profile_paths",           true, CmdParam.LIST,   null,     Msg.HLP_pp);
  public CmdParam cpNW     = new CmdParam("no_warn",                 true, CmdParam.LIST,   "",       Msg.HLP_nw);
  public CmdParam cpIcf    = new CmdParam("inline_complex_ftns",     true, CmdParam.SWITCH, off,      Msg.HLP_icf);
  public CmdParam cpCmi    = new CmdParam("cross_module_inlining",   true, CmdParam.SWITCH, on,       Msg.HLP_cmi);
  public CmdParam cpMulti  = new CmdParam("M",                       true, CmdParam.SWITCH, off,      Msg.HLP_M);
  public CmdParam cpPrePro = new CmdParam("E",                       true, CmdParam.SWITCH, off,      Msg.HLP_E);
  public CmdParam cpSla    = new CmdParam("L",                       true, CmdParam.SWITCH, off,      Msg.HLP_L);
  public CmdParam cpOfile  = new CmdParam("o",                       true, CmdParam.SWITCH, off,      Msg.HLP_o);
  public CmdParam cpOs     = new CmdParam("S",                       true, CmdParam.SWITCH, off,      Msg.HLP_S);
  public CmdParam cpOa     = new CmdParam("output_assembly",         true, CmdParam.SWITCH, off,      Msg.HLP_oa);
  public CmdParam cpOc     = new CmdParam("output_c",                true, CmdParam.SWITCH, off,      Msg.HLP_oc);
  public CmdParam cpCcb    = new CmdParam("clef_c_before",           true, CmdParam.SWITCH, off,      Msg.HLP_ccb);
  public CmdParam cpCca    = new CmdParam("clef_c_after",            true, CmdParam.SWITCH, off,      Msg.HLP_cca);
  public CmdParam cpCgb    = new CmdParam("clef_graph_before",       true, CmdParam.SWITCH, off,      Msg.HLP_cgb);
  public CmdParam cpCga    = new CmdParam("clef_graph_after",        true, CmdParam.SWITCH, off,      Msg.HLP_cga);
  public CmdParam cpDcg    = new CmdParam("display_call_graphs",     true, CmdParam.SWITCH, off,      Msg.HLP_dcg);
  public CmdParam cpC89    = new CmdParam("c89",                     true, CmdParam.SWITCH, off,      Msg.HLP_c89);
  public CmdParam cpC99    = new CmdParam("c99",                     true, CmdParam.SWITCH, off,      Msg.HLP_c99);
  public CmdParam cpGcc    = new CmdParam("gcc",                     true, CmdParam.SWITCH, off,      Msg.HLP_gcc);
  public CmdParam cpCkr    = new CmdParam("ckr",                     true, CmdParam.SWITCH, off,      Msg.HLP_ckr);
  public CmdParam cpAnsi   = new CmdParam("ansi",                    true, CmdParam.SWITCH, off,      Msg.HLP_ansi);
  public CmdParam cpUnsafe = new CmdParam("unsafe",                  true, CmdParam.SWITCH, off,      Msg.HLP_unsafe);
  public CmdParam cpVers   = new CmdParam("version",                 true, CmdParam.SWITCH, off,      Msg.HLP_version);
  public CmdParam cpSnap   = new CmdParam("snap",                    true, CmdParam.SWITCH, off,      Msg.HLP_snap);
  public CmdParam cpG      = new CmdParam("g",                       true, CmdParam.SWITCH, off,      Msg.HLP_gdb);
  public CmdParam cpNaln   = new CmdParam("naln",                    true, CmdParam.SWITCH, off,      Msg.HLP_naln);
  public CmdParam cpIs     = new CmdParam("instruction_scheduling",  true, CmdParam.SWITCH, on,       Msg.HLP_is);
  public CmdParam cpBi     = new CmdParam("builtin_inlining",        true, CmdParam.SWITCH, on,       Msg.HLP_bi);
  public CmdParam cpPh     = new CmdParam("peephole_hacker",         true, CmdParam.SWITCH, on,       Msg.HLP_ph);
  public CmdParam cpNp     = new CmdParam("new_parser",              true, CmdParam.SWITCH, off,      Msg.HLP_np);
  public CmdParam cpDm     = new CmdParam("display_macros",          true, CmdParam.SWITCH, off,      Msg.HLP_dm);
  public CmdParam cpQuiet  = new CmdParam("quiet",                   true, CmdParam.SWITCH, on,       Msg.HLP_quiet);
  public CmdParam cpNoWarn = new CmdParam("w",                       true, CmdParam.SWITCH, off,      Msg.HLP_w);
  public CmdParam cpDaVinci= new CmdParam("daVinci",                 true, CmdParam.SWITCH, off,      Msg.HLP_daVinci);
  public CmdParam cpVcg    = new CmdParam("vcg",                     true, CmdParam.SWITCH, off,      Msg.HLP_vcg);
  public CmdParam cpHda    = new CmdParam("dummy_aliases",           true, CmdParam.SWITCH, off,      Msg.HLP_hda);
  public CmdParam cpFpr    = new CmdParam("fp_reorder",              true, CmdParam.SWITCH, off,      Msg.HLP_fpr);
  public CmdParam cpSc     = new CmdParam("signed_chars",            true, CmdParam.SWITCH, on,       Msg.HLP_sc);
  public CmdParam cpUc     = new CmdParam("unsigned_chars",          true, CmdParam.SWITCH, off,      Msg.HLP_sc);
  public CmdParam cpSuspend= new CmdParam("suspend",                 true, CmdParam.SWITCH, off,      Msg.HLP_suspend);
  public CmdParam cpPg     = new CmdParam("profile_guided",          true, CmdParam.STRING, "",       Msg.HLP_pg);
  public CmdParam cpPi     = new CmdParam("profile_instrumentation", true, CmdParam.STRING, "",       Msg.HLP_pi);
  public CmdParam cpInls   = new CmdParam("inls",                    true, CmdParam.STRING, "",       Msg.HLP_inls);
  public CmdParam cpScb    = new CmdParam("scribble_c_before",       true, CmdParam.STRING, "",       Msg.HLP_scb);
  public CmdParam cpSca    = new CmdParam("scribble_c_after",        true, CmdParam.STRING, "",       Msg.HLP_sca);
  public CmdParam cpSgb    = new CmdParam("scribble_graph_before",   true, CmdParam.STRING, "",       Msg.HLP_sgb);
  public CmdParam cpSga    = new CmdParam("scribble_graph_after",    true, CmdParam.STRING, "",       Msg.HLP_sga);
  public CmdParam cpArch   = new CmdParam("arch",                    true, CmdParam.STRING, hostArch, Msg.HLP_arch);
  public CmdParam cpCc     = new CmdParam("cc",                      true, CmdParam.STRING, "cc",     Msg.HLP_cc);
  public CmdParam cpAsm    = new CmdParam("asm",                     true, CmdParam.STRING, "as",     Msg.HLP_asm);
  public CmdParam cpDir    = new CmdParam("dir",                     true, CmdParam.STRING, "." ,     Msg.HLP_dir);
  public CmdParam cpR      = new CmdParam("r",                       true, CmdParam.STRING, null,     Msg.HLP_r);
  public CmdParam cpWhich  = new CmdParam("for",                     true, CmdParam.STRING, "??",     Msg.HLP_for);
  public CmdParam cpGphType= new CmdParam("G",                       true, CmdParam.STRING, "e",      Msg.HLP_G);
  public CmdParam cpDd     = new CmdParam("data_dependence",         true, CmdParam.STRING, "tibBO",  Msg.HLP_dd);
  public CmdParam cpO      = new CmdParam("O",                       true, CmdParam.STRING, cannedOpts[3], Msg.HLP_O);
  public CmdParam cpIh     = new CmdParam("ignore_heuristics",       true, CmdParam.STRING, "",       Msg.HLP_ih);
  public CmdParam cpWrap   = new CmdParam("wrap_on_overflow",        true, CmdParam.STRING, "all",    Msg.HLP_wrap);
  public CmdParam cpHb     = new CmdParam("hb",                      true, CmdParam.STRING, "",       Msg.HLP_hb);
  public CmdParam cpSf     = new CmdParam("stat_file",               true, CmdParam.STRING, "",       Msg.HLP_sf);
  public CmdParam cpFf     = new CmdParam("flag_file",               true, CmdParam.STRING, "",       Msg.HLP_ff);

  protected CmdParam[] params = {
    cpOfile,   cpOa,      cpOc,      cpOs,      cpArch,
    cpIncl,    cpIncls,   cpPrePro,  cpO,       cpInl,
    cpInls,    cpDd,      cpCmi,     cpMulti,   cpAA,      cpCat,
    cpAnnot,   cpIcf,     cpSla,     cpR,       cpDir,
    cpD,       cpU,       cpG,       cpNaln,    cpUnsafe,
    cpFpr,     cpHb,      cpIs,      cpBi,      cpPh,
    cpNp,      cpDm,      cpC89,     cpC99,     cpGcc,
    cpCkr,     cpAnsi,    cpSc,      cpUc,      cpCcb,
    cpCca,     cpCgb,     cpCga,     cpScb,     cpSca,
    cpSgb,     cpSga,     cpDcg,     cpDaVinci, cpVcg,     cpGphType, 
    cpPi,      cpPg,      cpPp,
    cpCc,      cpAsm,     cpWhich,   cpWrap,
    cpDebug,   cpTcl,     cpFcl,     cpFf,      cpVers,    cpStat,
    cpSf,      cpCdd,     cpSan,     cpSnap,    cpQuiet,
    cpNoWarn,  cpNW,      cpHda,     cpIh,
    cpSuspend,
  };

  protected Vector<String> inputFiles;      // A vector of files (Strings) to be processed. 
  protected Vector<String> profilePaths;    // Locations to look in for generated profile information.
  protected Vector<String> warnings;        // Warnings to output in the generated code for every source file.

  protected boolean doSingle;        // True if separate compilation of the source files. 
  protected boolean doOfile;         // True if .o files are generated from .s files. 
  protected boolean doC;             // True if .c files are generated. 
  protected boolean doA;             // True if .s files are generated. 
  protected boolean debugging;       // True if -g specified.
  protected boolean all;             // True if no report name specified. 
  protected boolean doLines;         // True if line number information wanted.
  protected boolean readClassFiles;  // True if a Java class file was read. 
  protected boolean crossCompile;    // True if the target and host architectures differ.
  protected double  inllev;          // Inlining level requested. 
  protected int     aaLevel;         // The alias analysis level requested. 
  protected int     categories;      // The Shapiro alias analysis category. 
  protected int     backendFeatures; // The flags sent to the code generator;
  protected int     profInstOps;     // The flags specifying profiling instrumentation options.
  protected int     profGuidedOps;   // The flags specifying profiling guided options.
  protected String  architecture;    // The target architecture. 
  protected String  opts;            // Optimizations requested.
  protected String  targetArch;
  protected Aliases aliases;

  protected FileOutputStream inlfos;
  protected PrintWriter      inlStatusStream;

  protected Scale()
  {
    this.warnings = new Vector<String>(3);
    this.all      = true;
    this.inllev   = 1.0;
    this.aaLevel  = 4;
  }

  /**
   * Compile a C or Fortran program.
   * @param args the command line arguments
   */
  public static void main(String[] args) 
  {
    Scale me = new Scale();
    Msg.setup(null);
    me.compile(args);
  }

  /**
   * Compile a C or Fortran program.
   * @param args the command line arguments
   */
  public void compile(String[] args)
  {
    boolean aborted = false;

    parseCmdLine(args, params); // Parse the command line arguments

    // Issue command line to log file, etc.

    StringBuffer buf = new StringBuffer("Scale");
    for (int i = 0; i < args.length; i++) {
      buf.append(' ');
      buf.append(args[i]);
    }
    addWarning(Msg.MSG_s, buf.toString(), 0);

    try {
      if (cpPrePro.specified()) {
        runPreprocessor();
        return;
      }

      Statistics.reportStatus(Msg.MSG_Start);

      if (doSingle)
        separateCompilation();
      else
        multiCompilation();

      if (inlStatusStream != null)
        inlStatusStream.close();
    } catch(java.lang.Error er) {
      String msg = er.getMessage();
      if (msg == null)
        msg = er.getClass().getName();
      Msg.reportError(Msg.MSG_s, msg);
      er.printStackTrace();
      aborted = true;
    } catch(java.lang.RuntimeException err) {
      String msg = err.getMessage();
      if (msg == null)
        msg = err.getClass().getName();
      Msg.reportError(Msg.MSG_s, msg);
      err.printStackTrace();
      aborted = true;
    } catch(java.lang.Exception ex) {
      String msg = ex.getMessage();
      if (msg == null)
        msg = ex.getClass().getName();
      if (msg.length() > 0)
        Msg.reportError(Msg.MSG_s, msg);
      if (Debug.debug(1))
        ex.printStackTrace();
      aborted = true;
    }

    if (readClassFiles) {
      scale.jcr.ClassFile.closeZipFiles();
      Msg.reportInfo(Msg.MSG_There_were_s_classes_read,
                     Integer.toString(scale.jcr.ClassFile.classesRead));
    }

    DisplayGraph visualizer = DisplayGraph.getVisualizer();
    if (visualizer != null)
      visualizer.interact();

    Statistics.reportStatus(Msg.MSG_End);
    Statistics.reportStatistics(1, cpSf.getStringValue());

    if (cpSuspend.specified()) 
      try {
        // Clear out references and wait for the user to give the go
        // ahead.  We do this so that the user can interrogate any
        // tools, such as JMP, that rely on the JVM Profiling
        // Interface.

        visualizer             = null;
        aliases                = null;
        inlfos                 = null;
        inlStatusStream        = null;
        Machine.currentMachine = null;

        Annotation.removeAllAnnotationTables();
        Type.cleanup();
        WorkArea.cleanup();
        LoopTrans.cleanup();
        LiteralMap.clear();
        Noopt.cleanup();
        PPCfg.cleanup();
        DisplayGraph.setVisualizer(null);
        CallGraph.cleanup();
        Statistics.cleanup();
        Java2Scribble.cleanup();
        Scribble.cleanup();

        System.out.print(Msg.getMessage(Msg.MSG_Suspended_press_enter));
        System.in.read();
      } catch (java.lang.Exception ex) {
      }

    System.exit(aborted ? 1 : 0);
  }

  /**
   * Return true if cross-compiling.  We are cross-compiling if the
   * compiler host architecture is not the same as the target
   * architecture.
   */
  public boolean isCrossCompile()
  {
    return crossCompile;
  }

  /**
   * Add a warning message and display it if needed.
   */
  protected void addWarning(int msg, int level)
  {
    warnings.addElement(Msg.getMessage(msg));
    Statistics.reportStatus(msg, null, level);
  }

  /**
   * Add a warning message and display it if needed.
   */
  protected void addWarning(int msg, String text, int level)
  {
    warnings.addElement(Msg.insertText(msg, text));
    Statistics.reportStatus(msg, text, level);
  }

  /**
   * Add a warning message and display it if needed.
   */
  protected void addWarning(int msg, String text1, String text2)
  {
    warnings.addElement(Msg.insertText(msg, text1, text2));
    Statistics.reportStatus(msg, text1, text2, 1);
  }

  /**
   * Process the command line parameters.
   * @param args the array of command line parameters
   * @param params an array of allowed command line parameters
   */
  protected void parseCmdLine(String[] args, CmdParam[] params)
  {
    inputFiles = parseCmdLine(args, params, cpFiles);

    String forWhat = parseWhichParam();

    if (cpVers.specified()) {
      System.out.println(Msg.insertText(Msg.MSG_Scale_compiler_version_s, version));
      System.exit(0);
    }

    all     = parseReportParams(forWhat); // If there is no selected routine name.
    scale.backend.BBIS.all = all;
    doA     = false;
    doOfile = false;
    doC     = false;

    int x = 0;
    if (cpOa.specified() || cpOfile.specified()) {
      doA = true;
      doOfile = true;
      x++;
    }

    if (cpOc.specified()) {
      doC = true;
      doOfile = true;
      x++;
    }

    if (cpOs.specified()) {
      doA = true;
      x++;
    }

    if (x > 1) {
      Msg.reportError(Msg.MSG_Conflicting_parameters_choose_one_of_s, "-S, -o, -oa, -oc");
      System.exit(1);
    }

    debugging = cpG.specified();
    String opts = cpO.getStringValue();
    if (debugging) {
      if (!cpO.specified())
        opts = "";
      else if (!opts.equals("") && !opts.equals("0"))
        Msg.reportWarning(Msg.MSG_Use_of_Os_with_g_is_not_recommended, opts);
    }

    // Block and edge profiling use line numbers.

    doLines = (cpSla.specified() || debugging || cpPi.specified());

    if (doA) {
      int features = 0;
      if (debugging)
        features |= Generator.DEBUG;
      if (doLines)
        features |= Generator.LINENUM;
      if (cpNaln.specified())
        features |= Generator.NALN;
      if (!cpIs.specified())
        features |= Generator.NIS;
      if (!cpPh.specified())
        features |= Generator.NPH;
      if (cpAnsi.specified()) 
        features |= Generator.ANSIC;
      backendFeatures = features;
    }

    parseFlags(cpFcl.getStringValues());
    parseFlagFile(cpFf.getStringValue());

    doOfile &= parseArchParams();

    // Do multi-compilation if interprocedural analysis is requested OR
    //                      if multi-compilation is forced on OR
    //                      if profile reading or profile instrumentation
    //                         will occur.

    doSingle = parseAA() && !(cpMulti.specified() ||
                              cpPi.specified() ||
                              cpPg.specified());

    addWarning(doSingle ? Msg.MSG_Separate_compilation : Msg.MSG_Multi_compilation, 1);

    parseMiscellaneousParams(forWhat);
    parseOpts(opts, forWhat);
    parseOptHeuristics(cpIh.getStringValue(), forWhat);
    parseHyperblockParams();
    parseTraces(cpTcl.getStringValues());
  }

  protected Vector<String> parseCmdLine(String[] args, CmdParam[] params, CmdParam files)
  {
    try {
      if (CmdParam.parse(this.getClass().getName(), args, params, files))
        System.exit(0);
    } catch(java.lang.Exception ex) {
      if (classTrace)
        ex.printStackTrace();
      CmdParam.usage(System.out, this.getClass().getName(), params, files);
      System.exit(1);
    }

    return files.getStringValues();
  }

  protected String parseWhichParam()
  {
    if (cpWhich.specified())
      return cpWhich.getStringValue();
    return System.getProperty("user.dir");
  }

  /**
   * Return true if there is no routine name selected for special
   * debugging.
   */
  protected boolean parseReportParams(String forWhat)
  {
    Msg.ignoreAllWarnings = cpNoWarn.specified();

    if (cpNW.specified()) {
      Vector<String> v = cpNW.getStringValues();
      int    l = v.size();
      for (int i = 0; i < l; i++) {
        String n = v.get(i).toLowerCase();
        if ("all".equals(n)) {
          Msg.ignoreAllWarnings = true;
          continue;
        }
        try {
          int wn = Integer.parseInt(n);
          Msg.ignoreWarning(wn);
        } catch (java.lang.NumberFormatException ex) {
        }
      }
    }

    Msg.reportInfo = !cpQuiet.specified();

    Debug.setDebugLevel(cpDebug.getIntValue());
    Statistics.setStatusLevel(((Integer) cpStat.getIntValue()).intValue(), forWhat);

    int reportLevel = cpCdd.getIntValue();
    Node.setReportLevel(reportLevel); // Set Clef's level for displaying children.
    Note.setReportLevel(reportLevel); // Set Scribble's level for displaying children.

    int annotationLevel = cpSan.getIntValue();
    Note.setAnnotationLevel(annotationLevel);
    Node.setAnnotationLevel(annotationLevel);

    String rname = cpR.getStringValue();
    Debug.setReportName(rname); // The selected routine name if any.
    return (rname == null);
  }

  protected boolean parseArchParams()
  {
    architecture = Machine.setup(cpArch.getStringValue());
    
    if (architecture == null)
      System.exit(1);

    targetArch = Machine.currentMachine.getGenericArchitectureName();

    addWarning(Msg.MSG_Target_architecture_s, targetArch, 1);
    addWarning(Msg.MSG_Host_architecture_s, hostArch, 1);

    // This isn't right either.  What if you are cross compiling from
    // a Mac OS X system to a PPC Linux system.

    crossCompile = false;
    if (!targetArch.equals(hostArch)) {
      if (!hostArch.equals(cpArch.getStringValue()))
        crossCompile = true;
    }
    return !crossCompile;
  }

  private int getProfileOptions(String pf)
  {
    int po = 0;
    if (pf.indexOf('b') >= 0)
      po |= Scribble.PROFILE_BLOCKS;
    if (pf.indexOf('e') >= 0)
      po |= Scribble.PROFILE_EDGES;
    if (pf.indexOf('p') >= 0)
      po |= Scribble.PROFILE_PATHS;
    if (pf.indexOf('l') >= 0)
      po |= Scribble.PROFILE_LOOPS;
    if (pf.indexOf('c') >= 0)
      po |= Scribble.PROFILE_LICNT;
    return po;
  }

  protected void parseMiscellaneousParams(String forWhat)
  {
    profInstOps   = getProfileOptions(cpPi.getStringValue());
    profGuidedOps = getProfileOptions(cpPg.getStringValue());
    
    PPCfg.setOutputPath(cpDir.getStringValue());
    DisplayGraph.setOutputPath(cpDir.getStringValue());

    if (cpPi.specified() || cpPg.specified()) {
      profilePaths = cpPp.getStringValues();
      addWarning(Msg.MSG_Using_profiling_information, 1);
      if (profilePaths == null) {
        profilePaths = new Vector<String>();
        profilePaths.add(cpDir.getStringValue());
      }
    }

    if (cpAnnot.specified())
      addWarning(Msg.MSG_User_specified_annotations_added, 1);

    addWarning(Msg.MSG_Scale_compiler_version_s, version, 1);

    if (cpInl.specified()) {
      inllev = (100 + cpInl.getIntValue()) / 100.0;
      addWarning(Msg.MSG_Inlining_level_s, Double.toString(inllev), 1);
      String inlStatus = cpInls.getStringValue();
      if (!inlStatus.equals("")) {
        try {
          inlfos          = new FileOutputStream(genFileName(inlStatus, ""), true);
          inlStatusStream = new PrintWriter(inlfos, true);
        } catch(java.lang.Exception ex) {
          Msg.reportWarning(Msg.MSG_s, null, 0, 0, ex.getMessage());
        }
      }
      if (cpIcf.specified())
        Inlining.ignoreComplexityHeuristic = true;
    }

    int x = 0;
    if (cpC89.specified())
      x++;
    if (cpC99.specified())
      x++;
    if (cpCkr.specified())
      x++;
    if (x > 1) {
      Msg.reportError(Msg.MSG_Conflicting_parameters_choose_one_of_s,
                      null,
                      0,
                      0,
                      "-c89, -c99, -ckr");
      System.exit(1);
    }

    x = 0;
    if (cpGcc.specified())
      x++;
    if (cpAnsi.specified())
      x++;
    if (x > 1) {
      Msg.reportError(Msg.MSG_Conflicting_parameters_choose_one_of_s,
                      null,
                      0,
                      0,
                      "-gcc, -ansi");
      System.exit(1);
    }

    if (!cpBi.specified()) {
      Clef2Scribble.noBuiltins = true;
      addWarning(Msg.MSG_No_builtins, 1);
    }

    if (cpHda.specified()) {
      Clef2Scribble.hasDummyAliases = true;
      Optimization.hasDummyAliases = true;
      addWarning(Msg.MSG_Assume_dummy_aliases, 1);
    }

    if (cpUnsafe.specified()) {
      addWarning(Msg.MSG_Unsafe_optimizations_allowed, 1);
      Optimization.unsafe = true;
    }

    if (cpFpr.specified()) {
      addWarning(Msg.MSG_Floating_point_reordering_allowed, 1);
      Optimization.fpReorder = true;
      Expr.fpReorder = true;
    }
    
    String wrapStr = cpWrap.getStringValue();
    if (wrapStr.equals("all")) {
      Optimization.signedIntsWrapOnOverflow   = true;
      Optimization.unsignedIntsWrapOnOverflow = true;
    } else if (wrapStr.equals("unsigned")) {
      Optimization.signedIntsWrapOnOverflow   = false;
      Optimization.unsignedIntsWrapOnOverflow = true;
      addWarning(Msg.MSG_Signed_integer_overflows_might_not_wrap, 1);
    } else if (wrapStr.equals("none")) {
      Optimization.signedIntsWrapOnOverflow   = false;
      Optimization.unsignedIntsWrapOnOverflow = false;
      addWarning(Msg.MSG_All_integer_overflows_might_not_wrap, 1);
    } else {
      Msg.reportError(Msg.MSG_Invalid_parameter_s, null, 0, 0, wrapStr);
      System.exit(1);
    }
    
    ddtests = cpDd.getStringValue();
    addWarning(Msg.MSG_Data_dependence_testing_s, ddtests, 1);
    if (ddtests.indexOf('t') >= 0)
      scale.score.dependence.DDGraph.useTransClosure = true;
    if (ddtests.indexOf('i') >= 0)
      scale.score.trans.ScalarReplacement.innerLoopsOnly = true;
    if (ddtests.indexOf('b') >= 0)
      scale.score.dependence.DDGraph.useBasic = true;
    if (ddtests.indexOf('B') >= 0)
      scale.score.dependence.DDGraph.useBanerjee = true;
    if (ddtests.indexOf('O') >= 0)
      scale.score.dependence.DDGraph.useOmega = true;

    x = 0;
    if (cpSc.specified()) {
      x++;
      scale.clef.type.IntegerType.cCharsAreSigned = true;
    }
    if (cpUc.specified()) {
      x++;
      scale.clef.type.IntegerType.cCharsAreSigned = false;
    }

    if (x > 1) {
      Msg.reportError(Msg.MSG_Conflicting_parameters_choose_one_of_s,
                      null,
                      0,
                      0,
                      "-sc, -uc");
      System.exit(1);
    }

  }

  /**
   * Parse options for the hyperblock generator.
   */
  protected void parseHyperblockParams()
  {
    String hyper = cpHb.getStringValue();
    if (hyper.equals(""))
      return;
    else if (!hyper.equals("backend") || !architecture.equals("trips2")) {
      Msg.reportError(Msg.MSG_Invalid_parameter_s, null, 0, 0, hyper);
      System.exit(1);
    }
    addWarning(Msg.MSG_Hyperblock_policy_backend, 1);
    Scribble.doIfCombine = false;
    Scribble.doIfConversion = false;
    scale.backend.trips2.HyperblockFormation.enableHyperblockFormation = true;
  }
  
  /**
   * Parse the string of letters that specify the optimizations to be applied.
   * @param optLetters is the String specifying the optimizations
   * @param forWhat specifies the module for the statistics generated
   */
  protected void parseOpts(String optLetters, String forWhat)
  {
    boolean opt0 = false;

    opts = optLetters;
    int l = opts.length();
    if (l == 1) { // Check for -O0, -O1, -O2, etc.
      char optL  = opts.charAt(0);
      if (optL == '0')
        opt0 = true;

      if ((optL >= '0') && (optL <= '4')) {
        opts = cannedOpts[optL - '0'];
        l = opts.length();
      } else if (Character.isDigit(optL)) {
        Msg.reportError(Msg.MSG_Invalid_parameter_s, null, 0, 0, "-O" + opts);
        System.exit(1);
      }
    }

    if (opt0) {
      scale.score.Scribble.doIfConversion = false;
      scale.score.Scribble.doIfCombine = false;
      scale.callGraph.CallGraph.alphabeticalOrder = true;
    }

    StringBuffer buf = new StringBuffer("a");
    buf.append(aaLevel);
    buf.append('c');
    buf.append(categories);
    buf.append(opts);

    optString = buf.toString();

    addWarning(Msg.MSG_Optimizations_s, buf.toString(), 1);

    for (int i = 0; i < l; i++) {
      char optL  = opts.charAt(i);
      int  index = optimizationLetter.indexOf(optL);
      if (index < 0) {
        addWarning(Msg.MSG_Unknown_optimization_s, opts.substring(i, i + 1), 0);
        continue;
      }

      if (optL == 'j') {
        // Determine the unroll factor.
        String str = "0";
        while (i < l - 1) {
          char c = opts.charAt(i+1);
          if (!Character.isDigit(c))
            break;
          str += c;
          i++;
        }
        int unrollFactor = Integer.parseInt(str);
        if (unrollFactor >= 1) {
          PragmaStk.setDefaultValue(PragmaStk.UNROLL, unrollFactor);
          addWarning(Msg.MSG_Loop_unrolling_factor_s,
                     Integer.toString(unrollFactor),
                     1);
        } else
          PragmaStk.setDefaultValue(PragmaStk.UNROLL, 0); // Let URJ decide

      }

      String optName = optimizationName[index];
      addWarning(Msg.MSG_Performing_s, optName, 1);
    }

    int ll = opts.indexOf('l');
    if (0 <= ll) {
      PragmaStk.setDefaultFlag(PragmaStk.LOOP_TEST_AT_END, true);
      opts = opts.substring(0, ll) + opts.substring(ll + 1);
    }
  }

  /**
   * Parse the string of letters that specify the optimizations to be applied.
   * @param optLetters is the String specifying the optimizations
   * @param forWhat specifies the module for the statistics generated
   */
  protected void parseOptHeuristics(String optLetters, String forWhat)
  {
    String opts = optLetters;
    int    l    = opts.length();

    if (l == 1) { // Check for -O0, -O1, -O2, etc.
      char optL  = opts.charAt(0);

      if ((optL >= '0') && (optL <= '4')) {
        opts = cannedOpts[optL - '0'];
        l = opts.length();
      } else if (Character.isDigit(optL)) {
        Msg.reportError(Msg.MSG_Invalid_parameter_s, null, 0, 0, "-O" + opts);
        System.exit(1);
      }
    }

    for (int i = 0; i < l; i++) {
      char optL  = opts.charAt(i);
      if (optL == 'l')
        continue;

      int    index = optimizationLetter.indexOf(optL);
      if (index < 0)
        continue;

      String fc = "scale.score.trans." + optimizationClass[index];
      String ff = "useHeuristics";

      if (setFlag(fc, ff, "0", false)) {
        String msg = "-f " + fc + "." + ff + "=0";
        Msg.reportWarning(Msg.MSG_Invalid_parameter_s, null, 0, 0, msg);
      }
    }
  }

  /**
   * Process the alias analysis parameters.
   * @return true if this is a single compilation.
   */
  protected boolean parseAA()
  {
    boolean single = false;

    if (!cpAA.specified()) {
      aaLevel = 2;
      categories = 0;
      single = true;
    } else {
      categories = cpCat.getIntValue();
      aaLevel    = cpAA.getIntValue();
      if (aaLevel >= aliasAnalysisLevelMsg.length)
        aaLevel = aliasAnalysisLevelMsg.length - 1;
      if (!aliasAnalysisIsShapi[aaLevel]) {
        categories = 0;
      } else if (categories == 0) {
        Msg.reportError(Msg.MSG_SH_can_not_be_selected_with_0_categories,
                        null,
                        0,
                        0,
                        null);
        System.exit(1);
      }
      single = !aliasAnalysisIsInter[aaLevel];
    }

    addWarning(aliasAnalysisLevelMsg[aaLevel], 1);

    return single;
  }

  /**
   * Process the trace flag settings from the command line -t switch.
   */
  protected void parseTraces(Vector<String> trs)
  {
    if (trs == null)
      return;

    int l = trs.size();
    for (int i = 0; i < l; i++) {
      String tc = trs.elementAt(i);
      if (setFlag(tc, "classTrace", "1", true))
        Msg.reportWarning(Msg.MSG_Invalid_parameter_s,
                          null,
                          0,
                          0,
                          "-t " + tc);
    }
  }

  /**
   * Process the flag settings from the specified file.
   */
  protected void parseFlagFile(String filename)
  {
    if (filename.length() <= 0)
      return;

    try {
      FileReader     fis   = new FileReader(filename);
      BufferedReader br    = new BufferedReader(fis);
      Vector<String> flags = new Vector<String>(0);

      while (true) {
        String line = br.readLine();
        if (line == null)
          break;

        line = line.trim();
        if (line.length() <= 0)
          continue;

        flags.add(line);
      }

      parseFlags(flags);

      fis.close();
    } catch (IOException ioex) {
      Msg.reportError(Msg.MSG_Error_loading_flag_file_s, filename);
    }

  }

  /**
   * Process the flag settings from the command line -f switch.
   */
  protected void parseFlags(Vector<String> trs)
  {
    if (trs == null)
      return;

    int l = trs.size();
    for (int i = 0; i < l; i++) {
      String ap    = trs.elementAt(i);
      int    index = ap.indexOf('=');

      if (index < 1) {
        Msg.reportWarning(Msg.MSG_Invalid_parameter_s, null, 0, 0, ap);
        continue;
      }

      String fs = ap.substring(0, index).trim();
      int    li = fs.lastIndexOf('.');
      if (li < 1) {
        Msg.reportWarning(Msg.MSG_Invalid_parameter_s, null, 0, 0, ap);
        continue;
      }

      String fc  = fs.substring(0, li);
      String ff  = fs.substring(li + 1);
      String val = ap.substring(index + 1).trim();

      if (setFlag(fc, ff, val, false))
        Msg.reportWarning(Msg.MSG_Invalid_parameter_s, null, 0, 0, "-f " + ap);
    }
  }

  private boolean setFlag(String  className,
                          String  fieldName,
                          String  val,
                          boolean info)
  {
    try {
      java.lang.Class         cl    = java.lang.Class.forName(className);
      java.lang.reflect.Field field = cl.getField(fieldName);
      int                     mod   = field.getModifiers();

      if (!java.lang.reflect.Modifier.isStatic(mod))
        return true;

      java.lang.Class type = field.getType();

      StringBuffer buf = new StringBuffer("Set ");
      buf.append(className);
      buf.append(".");
      buf.append(fieldName);
      buf.append(" = ");

      if (type == className.getClass()) {
        field.set(null, val);
        buf.append('"');
        buf.append(val);
        buf.append('"');
      } else {
        long fv = Long.valueOf(val).intValue();

        if (type == Integer.TYPE)
          field.setInt(null, (int) fv);
        else if (type == Byte.TYPE)
          field.setByte(null, (byte) fv);
        else if (type == Short.TYPE)
          field.setShort(null, (short) fv);
        else if (type == Long.TYPE)
          field.setLong(null, fv);
        else if (type == Boolean.TYPE)
          field.setBoolean(null, fv != 0);
        else
          return true;

        buf.append( fv);
      }

      if (info)
        Msg.reportInfo(Msg.MSG_s, buf.toString());
      else
        addWarning(Msg.MSG_s, buf.toString(), 1);
      return false;
    } catch (java.lang.Exception ex) {
      if (Debug.debug(1))
        System.err.println(ex.getMessage());
      return true;
    }
  }

  /**
   * Compile all source files together.
   */
  protected void multiCompilation() throws java.lang.Exception
  {
    if (inputFiles == null)
      return;

    Suite suite = new Suite(true); // For collecting all of the call graphs

    LiteralMap.clear();

    // Read in and convert to Scribble each specified file.

    int l = inputFiles.size();
    for (int i = 0; i < l; i++)
      processFile(inputFiles.elementAt(i), suite);

    // Read in profiling information if specified.  We read in the
    // profiling data before doing profile instrumentation so that
    // previous profiling data can be used to guide path profiling
    // instrumentation.

    if (cpPg.specified())
      suite.readProfInfo(profilePaths, profGuidedOps);

    // Add profiling instrumentation if specified.

    if (cpPi.specified())
      suite.addProfiling(inputFiles, profInstOps);

    // Remove the annotation lookup tables that are no longer needed.
    // This frees up space by eliminating Clef nodes that are only
    // referenced by annotations.

    Annotation.removeAllAnnotationTables();

    processSuite(suite);

    // Perform optimizations.

    optimizeAllCFGs(suite);

    if (doC) {
      Enumeration<CallGraph> ecg = suite.getCallGraphs();
      while (ecg.hasMoreElements())
        genCfromCallGraph(ecg.nextElement());
    }

    if (doA) {
      Enumeration<CallGraph> ecg = suite.getCallGraphs();
      while (ecg.hasMoreElements()) 
        genAssemblyfromCallGraph(ecg.nextElement());
    }

    scale.clef.type.Type.cleanup();
    aliases = null;
  }

  private void runPreprocessor() throws java.io.IOException
  {
    int l = inputFiles.size();
    for (int i = 0; i < l; i++) {
      String fn = inputFiles.elementAt(i);
      Parser.runPreprocessor(System.out, fn, this);
    }
  }

  /**
   * Compile each source file separately.  Note that profiling is not
   * compatible with separate compilation.
   */
  protected void separateCompilation() throws java.lang.Exception
  {
    if (inputFiles == null)
      return;

    int l = inputFiles.size();
    for (int i = 0; i < l; i++) {
      String name  = inputFiles.elementAt(i);
      Suite  suite = new Suite(false); // For collecting the single call graph

      LiteralMap.clear();

      // Read in and convert to Scribble each specified file.

      processFile(name, suite);

      // Remove the annotation lookup tables that are no longer
      // needed.  This frees up space by eliminating Clef nodes that
      // are only referenced by annotations.

      Annotation.removeAllAnnotationTables();

      processSuite(suite);

      if (cpSnap.specified())
        Statistics.snapshot(name);

      optimizeAllCFGs(suite);

      if (doC) {
        Enumeration<CallGraph> ecg = suite.getCallGraphs();
        while (ecg.hasMoreElements())
          genCfromCallGraph(ecg.nextElement());
      }

      if (doA) {
        Enumeration<CallGraph> ecg = suite.getCallGraphs();
        while (ecg.hasMoreElements()) 
          genAssemblyfromCallGraph(ecg.nextElement());
      }

      scale.clef.type.Type.cleanup();
      aliases = null;
    }
  }

  private void processClassFile(String inputFile, Suite suite)
  {
    String         root = inputFile.substring(0, inputFile.length() - 6);
    Java2Scribble  j2s  = new Java2Scribble();
    ClassStuff     cs   = j2s.getClass(root);
    SourceLanguage lang = new scale.frontend.java.SourceJava();
    CallGraph      cg   = new CallGraph(cs.name, suite, lang);

    j2s.convertClass(cs, cg);

    readClassFiles = true;

    Enumeration<Declaration> evd = j2s.getTopDecls();
    while (evd.hasMoreElements()) {
      Declaration   d  = evd.nextElement();
      ProcedureDecl rd = d.returnProcedureDecl();
      if (rd != null)
        cg.recordRoutine(rd);
      else
        cg.addTopLevelDecl(d);
    }

    FileDecl fd = new FileDecl(root);
    cg.setAST(fd);

    suite.addCallGraph(cg);
  }

  private void processClef(String    inputFile,
                           Suite     suite,
                           CallGraph cg) throws java.lang.Exception
  {
    if (cpSnap.specified())
      Statistics.snapshot(inputFile);

    Statistics.reportStatus(Msg.MSG_Clef, inputFile, 2);

    if (cpCcb.specified())
      genCfromClef(cg, inputFile);

    if (cpCgb.specified())
      displayClef(cg, Debug.getReportName(), inputFile + " before optimizeClef");

    optimizeClef(cg);

    if (cpCga.specified())
      displayClef(cg, Debug.getReportName(), inputFile + " after optimizeClef");

    if (cpCca.specified())
      genCfromClef(cg, inputFile);

    cg.computeCallGraph();
    convertToScribble(cg);
    cg.removeClefAST();
  }

  /**
   * Convert the source file to Clef and add it to the set of Clef
   * ASTs.
   */
  protected void processFile(String firstInputFile,
                             Suite  suite) throws java.lang.Exception
  {
    Parser parser = Parser.getParser(firstInputFile, this);
    if (parser == null) {
      Msg.reportError(Msg.MSG_Unknown_file_type_s, null, 0, 0, firstInputFile);
      return;
    }

    CallGraph cg = parser.parse(firstInputFile,
                                suite,
                                warnings);
    if (cg == null) {
      Msg.reportError(Msg.MSG_Failed_to_compile_s, null, 0, 0, firstInputFile);
      throw new scale.common.Exception("");
    }

    processClef(firstInputFile, suite, cg);
    suite.addCallGraph(cg);
  }

  /**
   * Convert each RoutineDecl in a CallGraph to a CFG.
   * @param cg is the CallGraph
   */
  protected void convertToScribble(CallGraph cg)
  {
    SourceLanguage lang = cg.getSourceLanguage();

    Iterator<RoutineDecl> it = cg.allRoutines();
    while (it.hasNext()) {
      RoutineDecl rd   = it.next();
      String      name = rd.getName();

      if (rd.getBody() == null)
        continue;

      Statistics.reportStatus(Msg.MSG_Scribble_s, name, 2);
      new Clef2Scribble(rd, lang, cg);
      if (cpSnap.specified())
        Statistics.snapshot(name);
    }
  }

  /**
   * Once a Clef AST has been generated, this method is called to
   * perform any transformations on it.
   * @param cg is the Clef AST
   */
  protected void optimizeClef(CallGraph cg)
  {
    cg.optimizeAST();
  }

  private DisplayGraph getVisualizer()
  {
    DisplayGraph visualizer = DisplayGraph.getVisualizer();
    if (visualizer != null)
      return visualizer;

    if (cpDaVinci.specified())
      visualizer = new DaVinci();
    else if (cpVcg.specified())
      visualizer = new Vcg();
    else 
      visualizer = new SGD();

    DisplayGraph.setVisualizer(visualizer);

    return visualizer;
  }
  
  /**
   * This method is called to add user-specified annotations and
   * perform alias analysis.
   * @param suite is the suite of call graphs.
   * @see scale.callGraph.Suite
   */
  protected void processSuite(Suite suite)
  {
    if (Debug.debug(2))
      suite.printXRef();

    if (cpDcg.specified()) {
      DisplayGraph           visualizer = getVisualizer();
      Enumeration<CallGraph> es         = suite.getCallGraphs();
      while (es.hasMoreElements()) {
        CallGraph cg   = es.nextElement();
        String    name = cg.getName();
        visualizer.newGraph(name, false);
        cg.graphCallTree(visualizer);
        visualizer.openWindow(name, name, 0);
      }
    }

    if (cpAnnot.specified()) { // Add any user-specified annotations.
      boolean        dt = false; // true if Scale is to determine this for compiled functions.
      int            w  = Debug.debug(1) ? AnnotationFile.FOUND : AnnotationFile.NORMAL;
      AnnotationFile af = new AnnotationFile(w);
      Vector<String> v  = cpAnnot.getStringValues();
      int            l  = v.size();
      for (int i = 0; i < l; i++) {
        String aFile = v.elementAt(i);
        if (aFile.equals("DETECT"))
          dt = true;
        else
          af.processAnnotationFile(suite, aFile);
      }
      if (dt) // Create the annotations ourselves.
        new PureFunctionAnalyser(suite);
    }

    // Do inlining.

    if (cpInl.specified()) {
      Statistics.reportStatus(Msg.MSG_Inlining_Start, 2);
      Inlining inOpt = new Inlining(suite, cpCmi.specified());
      inOpt.optimize(inllev);
      if (inlStatusStream != null)
        inOpt.displayStatus(inlStatusStream);
      Statistics.reportStatus(Msg.MSG_Inlining_End, 2);
    }

    if (aaLevel > 0) { // compute aliases
      Statistics.reportStatus(Msg.MSG_Alias_Analysis_Start, 2);
      boolean isInter  = aliasAnalysisIsInter[aaLevel];
      boolean isSimple = aliasAnalysisIsSimpl[aaLevel];
      if (aliasAnalysisIsShapi[aaLevel]) {
        ShapiroHorowitz shapiro = new ShapiroHorowitz(isInter, categories);
        aliases = new CategoriesAliases(shapiro, suite, isSimple);
      } else {
        Steensgaard steensgaard = new Steensgaard(isInter);
        aliases = new Aliases(steensgaard, suite, isSimple);
      }

      aliases.computeAliases();

      if (Debug.debug(3))
        aliases.printAliasInfo();

      aliases.cleanup();
      Statistics.reportStatus(Msg.MSG_Alias_Analysis_End, 2);
    }

    // Count the number of global variables.

    Iterator<Declaration> tld = suite.topLevelDefDecls();
    while (tld.hasNext()) {
      Declaration d = tld.next();
      if (d.isVariableDecl()) {
        if (d.isGlobal())
          globalVariablesCount++;
      }
    }
  }

  /**
   * This method performs any optimizations/transformations on a CFG.
   * @param scribble the scribble graph
   * @param name is the name of the routine to be optimized
   * @see scale.score.Scribble
   */
  protected void optimizeScribble(Scribble scribble, String name)
  {
    ct = System.currentTimeMillis();
    Statistics.reportStatus(Msg.MSG_Optimizing_s, name, 2);

    String  bOpts = cpSgb.getStringValue();
    String  aOpts = cpSga.getStringValue();
    String  cOpts = cpScb.getStringValue();
    String  dOpts = cpSca.getStringValue();
    boolean disp  = all || Debug.trace(name, true, 10);
    int     cnt   = 1;

    if (disp && (0 <= bOpts.indexOf('0')))
      displayScribble(scribble, name + "_0_before_optimizeScribble");

    if (0 <= cOpts.indexOf('0'))
        genCfromScribble(scribble, name + "_0_before_optimizeScribble");

    PlaceIndirectOps pio = null;

    if (aaLevel > 0) {
      if (aliasAnalysisIsShapi[aaLevel])
        pio = new PlaceIndirectOpsSH(aliases);
      else
        pio = new PlaceIndirectOpsSteen(aliases);
    }

    Class[]  argNames = new Class[1];
    Object[] args     = new Object[1];
    int      l        = opts.length();
    for (int i = 0; i < l; i++) {
      char optL  = opts.charAt(i);
      int  index = optimizationLetter.indexOf(optL);
      if (index < 0)
        continue;

      String optName = optimizationClass[index];
      argNames[0] = scribble.getClass();
      args[0]     = scribble;

      Optimization opt = null;

      try {
        String                        clName  = "scale.score.trans." + optName;
        java.lang.Class<?>            opClass = Class.forName(clName);
        java.lang.reflect.Constructor cnstr   = opClass.getDeclaredConstructor(argNames);
        opt = (Optimization) cnstr.newInstance(args);
      } catch (java.lang.Exception ex) {
        if (Debug.debug(1))
          Msg.reportError(Msg.MSG_s, null, 0, 0, ex.getMessage());
        Msg.reportWarning(Msg.MSG_Optimization_s_not_performed_for_s,
                          null,
                          0,
                          0,
                          optName,
                          name);
        continue;
      }

      if ((scribble.inSSA() == Scribble.validSSA) &&
          (opt.requiresSSA() == Optimization.NO_SSA))
        scribble.removeDualExprs();

      if (disp && (0 <= cOpts.indexOf(optL)))
        genCfromScribble(scribble, name + "_" + cnt + "_before" + optName);

      if (disp && (0 <= bOpts.indexOf(optL)))
        displayScribble(scribble, name + "_" + cnt + "_before" + optName);

      if (!scribble.applyOptimization(opt, pio))
        scribble.addWarning(Msg.MSG_Optimization_s_not_performed_for_s,
                            optName,
                            name);

      if (disp && (0 <= aOpts.indexOf(optL)))
        displayScribble(scribble, name + "_" + cnt + "_after " + optName + " ");

      if (disp && (0 <= dOpts.indexOf(optL)))
        genCfromScribble(scribble, name + "_" + cnt + "_after" + optName);

      cnt++;
    }

    scribble.removeDualExprs();
    scribble.exitSSA();
    scribble.reduceMemory();

    if (disp && (0 <= aOpts.indexOf('0')))
      displayScribble(scribble, name + "_0_after_optimizeScribble");

    if (0 <= dOpts.indexOf('0'))
        genCfromScribble(scribble, name + "_0_after_optimizeScribble");
  }

  /**
   * Perform optimizations on all the routines that have a body.
   */
  private void optimizeAllCFGs(Suite suite) throws java.lang.Exception
  {
    // We use this enumeration instead of Suite.allRoutines() because
    // some optimizations may define new routines.

    Enumeration<CallGraph> ecg = suite.getCallGraphs();
    while (ecg.hasMoreElements()) {
      CallGraph cg = ecg.nextElement();

      RoutineDecl[] rds = cg.allRoutinesArray();
      for (int i = 0; i < rds.length; i++) {
        RoutineDecl rd       = rds[i];
        Scribble    scribble = rd.getScribbleCFG();

        if (scribble == null)
          continue;

        String name = rd.getName();
        if (all || Debug.trace(name, true, 10)) {
          optimizeScribble(scribble, name);

          if (cpSnap.specified())
            Statistics.snapshot(name);
        }
      }
    }
  }

  /**
   * Return a path name for the new file including the directory
   * specified by the "dir" switch.
   * @param name a file path from which to extract the file name
   * @param extension the extension to use for the new file
   */
  protected String genFileName(String name, String extension)
  {
    char slash = File.separatorChar;
    int  b     = name.lastIndexOf(slash);
    int  e     = name.lastIndexOf(".");
    if (b < 0)
      b = 0;
    else
      b++;
    if ((e < 0) || (e < b))
      e = name.length();

    StringBuffer fname = new StringBuffer((String) cpDir.getValue());
    fname.append(slash);
    fname.append(name.substring(b, e)); // the file name without the extension or path
    fname.append(extension);
    return fname.toString();
  }

  /**
   * Print out the C representation of the Clef AST.
   * @param cg is the Clef AST
   * @param name is is used to create the result file name
   */
  protected void genCfromClef(CallGraph cg, String name) throws java.lang.Exception
  {
    if (cg == null)
      return;

    String fname = genFileName(name, ".clef.c");

    Statistics.reportStatus(Msg.MSG_Generating_s, fname);

    FileOutputStream fos  = new FileOutputStream(fname);
    PrintWriter      cout = new PrintWriter(fos, true);
    Emit             em   = new EmitToFile(cout, 2);
    SourceLanguage   lang = cg.getSourceLanguage();

    Scribble2C.genIncludes(lang, em);
    Clef2C c2c = new Clef2C(em, cg.getSourceLanguage());
    c2c.codeGen(cg.getAST());

    fos.close();
  }

  /**
   * Print out the C representation of the Scribble form.
   * @param scribble is the CFG
   * @param name is is used to create the result file name
   */
  protected void genCfromScribble(Scribble scribble, String name)
  {
    if (scribble == null)
      return;

    String fname = genFileName(name, ".scribble.c");

    Statistics.reportStatus(Msg.MSG_Generating_s, fname);

    try {
      FileOutputStream fos  = new FileOutputStream(fname);
      PrintWriter      cout = new PrintWriter(fos, true);
      Emit             em   = new EmitToFile(cout, 2);
      Scribble2C       s2c  = new Scribble2C(Machine.currentMachine);

      s2c.generateC(scribble, em, true);
      fos.close();
    } catch (java.lang.Exception ex) {
      String msg = ex.getMessage();
      if (msg == null)
        msg = ex.getClass().getName();
      Msg.reportWarning(Msg.MSG_s, null, 0, 0, msg);
      ex.printStackTrace();
    }
  }

  private int displayFlags()
  {
    int flag = 0;

    String s = cpGphType.getStringValue();
    if (s.indexOf('u') >= 0)
      flag |= DisplayGraph.SHOW_DEFUSE;
    if (s.indexOf('m') >= 0)
      flag |= DisplayGraph.SHOW_MAYUSE;
    if (s.indexOf('c') >= 0)
      flag |= DisplayGraph.SHOW_CLEF;
    if (s.indexOf('t') >= 0)
      flag |= DisplayGraph.SHOW_TYPE;
    if (s.indexOf('a') >= 0)
      flag |= DisplayGraph.SHOW_ANNO;
    if (s.indexOf('e') >= 0)
      flag |= DisplayGraph.SHOW_EXPR;
    if (s.indexOf('l') >= 0)
      flag |= DisplayGraph.SHOW_LOW_EXPR;
    if (s.indexOf('h') >= 0)
      flag |= DisplayGraph.SHOW_HIGH_EXPR;
    if (s.indexOf('d') >= 0)
      flag |= DisplayGraph.SHOW_DD;
    if (s.indexOf('D') >= 0)
      flag |= DisplayGraph.SHOW_DOM;
    if (s.indexOf('P') >= 0)
      flag |= DisplayGraph.SHOW_PDOM;
    if (s.indexOf('C') >= 0)
      flag |= DisplayGraph.SHOW_CDG;

    return flag;
  }

  /**
   * Display a CFG.
   * @param scribble is the CFG
   * @param name is the display name
   */
  public void displayScribble(Scribble scribble, String name)
  {
    if (scribble == null)
      return;

    DisplayGraph visualizer = getVisualizer();
    visualizer.newGraph(name, false);

    int   flags = displayFlags();
    Chord graph = scribble.getBegin();
    if ((flags & DisplayGraph.SHOW_PDOM) != 0)
      graph = scribble.getEnd();

    ExportCFG exportCFG = new ExportCFG(visualizer, scribble, flags);

    exportCFG.traverse(graph);
    visualizer.openWindow(name, name, 0);
  }

  /**
   * Display a Clef AST.  If the specified routine name is not
   * <code>null</code> then only the AST for that routine is
   * displayed.  Otherwise, the AST for the entire call graph is
   * displayed.
   * @param cg is the call graph containing the Clef AST
   * @param rname is the name of the routine or null
   * @param name is the display name
   */
  protected void displayClef(CallGraph cg, String rname, String name)
  {
    if (cg == null)
      return;

    Node tree = cg.getAST();
    if (rname != null) { // Find the routine in the symbol table.
      Symtab symtab = cg.getSymbolTable();
      symtab.setCurrentScope(symtab.getRootScope());
      tree = symtab.findRoutine(rname);
      if (tree == null)
        return;
    }

    DisplayGraph visualizer = getVisualizer();
    visualizer.newGraph(name, true);

    Display displayTree = new Display(visualizer, displayFlags());

    tree.visit(displayTree);

    visualizer.openWindow(name, name, 0);
  }

  protected void splitString(String str, Vector<String> v)
  {
    StringTokenizer tok = new StringTokenizer(str);
    while (tok.hasMoreTokens()) {
      v.addElement(tok.nextToken());
    }
  }

  private static void processCommandOutput(Process p) throws java.lang.Exception
  {
    InputStreamReader is    = new InputStreamReader(p.getInputStream());
    InputStreamReader es    = new InputStreamReader(p.getErrorStream());
    boolean           ieof  = true;
    boolean           eeof  = true;
    char[]            buf   = new char[512];
    long              st    = System.currentTimeMillis();
    int               cnt   = 1;

    while (eeof || ieof) {
      // Sometimes the process never writes an eof but terminates anyway.

      if (is.ready() && ieof) {
        int l = is.read(buf, 0, 511);
        if (l < 0)
          ieof = false;
        else
          System.out.println(new String(buf, 0, l));
        cnt = 1;
      }

      if (es.ready() && eeof) {
        int l = es.read(buf, 0, 511);
        if (l < 0)
          eeof = false;
        else
          System.out.print(new String(buf, 0, l));
        cnt = 1;
      }

      cnt++;
      if (cnt >= 1000) {
        try {
          p.exitValue();
          return;
        } catch (java.lang.Exception Kex) { // process has not yet terminated.
          cnt = 1;
        }
      }
    }

    is.close();
    es.close();
  }

  /**
   * Execute an OS command represented by a string.  Display the
   * command output on System.out.
   * @param cmd is the command to execute
   * @return true if the command failed
   */
  public static boolean executeCommand(String cmd) throws java.lang.Exception
  {
    Statistics.reportStatus(Msg.MSG_s, cmd, 2);
    if (classTrace)
      System.out.println("** CMD: " + cmd);

    Runtime rt = Runtime.getRuntime();
    Process p  = rt.exec(cmd, null);

    processCommandOutput(p);

    int status = p.waitFor();
    if (status != 0) {
      String st  = Integer.toString(status);
      Msg.reportError(Msg.MSG_s_returned_status_s, null, 0, 0, cmd, st);
      return true;
    }
    return false;
  }

  /**
   * Execute an OS command represented by an array of strings.
   * Display the command output on System.out.
   * @param cmd is the command to execute
   * @return true if the command failed
   */
  protected boolean executeCommand(String[] cmd) throws java.lang.Exception
  {
    if (classTrace || Statistics.status(3)) {
      StringBuffer buf = new StringBuffer("  ");
      for (int j = 0; j < cmd.length; j++) {
        buf.append(cmd[j]);
        buf.append(" ");
      }

      if (classTrace)
        System.out.println("** CMD: " + buf.toString());

      if (Statistics.status(3))
        Statistics.reportStatus(Msg.MSG_s, buf.toString(), 3);
    }

    Runtime rt = Runtime.getRuntime();
    Process p  = rt.exec(cmd, null);

    processCommandOutput(p);

    int status = p.waitFor();
    if (status != 0) {
      StringBuffer buf = new StringBuffer("");
      for (int j = 0; j < cmd.length; j++) {
        buf.append(cmd[j]);
        buf.append(" ");
      }
      String st  = Integer.toString(status);
      Msg.reportError(Msg.MSG_s_returned_status_s,
                      null,
                      0,
                      0,
                      buf.toString(), st);
      return true;
    }
    return false;
  }

  /**
   * Execute an OS command represented by a list of strings.
   * @param command is the command to execute
   * @return true if the command failed
   */
  protected boolean executeCommand(Vector<String> command) throws java.lang.Exception
  {
    int    l     = command.size();
    String[] cmd = new String[l];
    for (int i = 0; i < l; i++)
      cmd[i] = command.elementAt(i);

    return executeCommand(cmd);
  }

 /**
   * Remove a file
   * @param file is the full pathname for the file
   */
  protected void removeFile(String file) throws java.lang.Exception
  {
    File tmpFile = new File(file);
    tmpFile.delete();
  }

  /**
   * Generate a .c file from a CallGraph using each RoutineDecl's CFG.
   * @param cg is the CallGraph
   */
  protected void genCfromCallGraph(CallGraph cg) throws java.lang.Exception
  {
    String         fname = genFileName(cg.getName(), ".scale.c");
    SourceLanguage lang  = cg.getSourceLanguage();

    Statistics.reportStatus(Msg.MSG_Generating_s, fname);

    FileOutputStream fos  = new FileOutputStream(fname);
    PrintWriter      cout = new PrintWriter(fos, true);
    Emit             em   = new EmitToFile(cout, 2);

    // Include any comments at the start of the file.

    int l = warnings.size();
    for (int i = 0; i < l; i++) {
      em.emit("/* ");
      em.emit(warnings.elementAt(i));
      em.emit(" */");
      em.endLine();
    }

    Scribble2C.genCFromCallGraph(cg, em);

    fos.close();

    if (doOfile) {
      String         cname  = cpCc.getStringValue();
      String         fnameo = genFileName(cg.getName(), ".o");
      Vector<String> ccmd   = new Vector<String>();

      splitString(cname, ccmd);

      if (debugging)
        ccmd.addElement("-g");

      if (cpGcc.specified() && !cname.startsWith("gcc"))
        ccmd.addElement("-gcc");

      if (cpIncl.specified()) {
        Vector<String> iv  = cpIncl.getStringValues();
        int    ivl = iv.size();
        for (int i = 0; i < ivl; i++) {
          ccmd.addElement("-I" + iv.elementAt(i));
        }
      }

      ccmd.addElement("-c");
      ccmd.addElement(fname);
      ccmd.addElement("-o");
      ccmd.addElement(fnameo);

      if (executeCommand(ccmd)) {
        Msg.reportError(Msg.MSG_Failed_to_compile_s, null, 0, 0, fname);
        throw new scale.common.Exception("");
      }
    }

    if (!doC)
      removeFile(fname);
  }

  /**
   * Generate an assembly file from a CallGraph using each
   * RoutineDecl's CFG.
   * @param cg is the CallGraph
   */
  protected void genAssemblyfromCallGraph(CallGraph cg) throws java.lang.Exception
  {
    String asm   = cpAsm.getStringValue();
    String as    = Machine.getAssemblerCommand(asm, backendFeatures);
    String ext   = Machine.currentMachine.getAsmFileExtension();
    String fname = genFileName(cg.getName(), ext);

    Statistics.reportStatus(Msg.MSG_Generating_s, fname);
    
    FileOutputStream fos  = new FileOutputStream(fname);
    PrintWriter      cout = new PrintWriter(fos, true);
    Emit             em   = new EmitToFile(cout, 2);
    Generator        gen  = Machine.getCodeGenerator(cg, backendFeatures);

    gen.generate();

    gen.assemble(em, cg.getName(), warnings.elements());

    fos.close();

    if (doOfile) {
      String          fnameo = genFileName(cg.getName(), ".o");
      Vector<String>  ccmd   = new Vector<String>();

      splitString(as, ccmd);

      ccmd.addElement(fname);
      ccmd.addElement("-o");
      ccmd.addElement(fnameo);

      if (executeCommand(ccmd))
        throw new scale.common.Error("Failed to assemble " + fname);
    }
  }
}
