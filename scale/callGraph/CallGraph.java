package scale.callGraph;

import java.util.Iterator;
import java.util.Map;
import java.util.Arrays;
import java.util.List;

import java.io.*;

import scale.common.*;
import scale.frontend.*;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.type.*;
import scale.clef.expr.*;
import scale.clef.symtab.*;
import scale.score.Scribble;
import scale.visual.*;
import scale.score.pp.PPCfg;

/**
 * This class holds all of the RoutineDecl instances for the routines
 * in a single compilation unit.
 * <p>
 * $Id: CallGraph.java,v 1.126 2007-10-29 13:46:16 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * A class that represents the call graph of a program.  This class is used
 * to build the call graph and maintain a pointer to the root node (the
 * main routine) of the program.
 * <p>
 * The class also maintains information about the routines and global
 * variables in a program.
 * @see RoutineDecl
 */
public final class CallGraph
{
  /**
   * If true, return {@link scale.clef.decl.RoutineDecl routines} in
   * alphabetical order.
   */
  public static boolean alphabeticalOrder = false;

  private static int    createdCount = 0; // A count of all the created instances of this class.

  private HashSet<RoutineDecl> pointerFuncs;     // Keep track all functions with address taken.
  private HashSet<RoutineDecl> allRoutines;      // Record all routines.
  private Vector<Declaration>  topLevelDecls;    // Keep track of all top level and static declarations except routines.
  private Vector<Declaration>  topLevelDefDecls; // Keep track of all defining top level and static declarations (ie., no extern keyword).

  private RoutineDecl    rootRoutine;       // The main procedure if one is found.
  private String         name;              // Name of call graph (i.e., the file name of the originating Clef AST.
  private SourceLanguage sourceLang;        // Source language used to generate Scribble.
  private Suite          suite;             // The Suite of callGraphs for this program.
  private Node           ast;               // Root node of the Clef AST.
  private Symtab         symtab;            // Clef AST symbol table.
  private int            id;                // A unique integer representing this instance.

  /**
   * Create a call graph of call nodes.
   * @param name the name of the Clef AST (i.e., the file name)
   * @param suite is the collection of call graphs
   * @param sourceLang specifies the source language used
   */
  public CallGraph(String name, Suite suite, SourceLanguage sourceLang)
  {
    this.sourceLang = sourceLang;
    this.name       = name;
    this.suite      = suite;
    this.ast        = null;

    this.symtab           = new Symtab();
    this.pointerFuncs     = new HashSet<RoutineDecl>(11);
    this.allRoutines      = new HashSet<RoutineDecl>(11);
    this.topLevelDecls    = new Vector<Declaration>(11);
    this.topLevelDefDecls = new Vector<Declaration>(11);
    this.id = createdCount++;
  }

  /**
   * Use the file name of the source file.
   */
  public int hashCode()
  {
    return name.hashCode();
  }

  /**
   * Apply optimizations to the AST.
   */
  public final void optimizeAST()
  {
    if (ast == null)
      return;
  }

  /**
   * Return the {@link scale.callGraph.Suite Suite} to which this
   * CallGraph belongs.
   */
  public final Suite getSuite()
  {
    return suite;
  }

  /**
   * Return the name associated with this call graph.
   */
  public final String getName()
  {
    return name;
  }

  /**
   * Return the source language of the call graph.
   */
  public final SourceLanguage getSourceLanguage()
  {
    return sourceLang;
  }

  /**
   * Return the root of the Clef abstract syntax tree (AST).
   */
  public final Node getAST()
  {
    return ast;
  }

  /**
   * Specify the root node of the AST.
   */
  public final void setAST(Node ast)
  {
    assert (this.ast == null) : "Invalid Semantics: root already specified";

    this.ast = ast;
  }

  /**
   * Compute the call graph.
   */
  public final void computeCallGraph()
  {
    new ClefCalls(this);
  }

  /**
   * Remove the Clef AST to save space.
   */
  public final void removeClefAST()
  {
    ast = null;
    symtab = null;
  }

  /**
   * Return the symbol table for the clef tree.
   */
  public final Symtab getSymbolTable()
  {
    return symtab;
  }

  /**
   * Add the routine to the list of {@link scale.clef.decl.RoutineDecl
   * routines} that are referenced.
   */
  public void addFunction(RoutineDecl p)
  {
    pointerFuncs.add(p);   
  }

  /**
   * Add a call to the call graph.  If a call node hasn't already been
   * created, we create one.  Also, we all the callee to the list of
   * routines in the program (if it hasn't been added already).
   *
   * @param caller the call node representing the caller
   * @param rd the declaration node representig the callee
   */
  public void addCallee(RoutineDecl caller, RoutineDecl rd)
  {
    recordRoutine(rd);
    caller.addCallee(rd);
  }

  /**
   * Add the profiling instrumentation to every call node in the call graph.
   * @param moduleNames is the list of all source modules in the program 
   * and must include the module containing "main"
   * @param profileOptions specifies which profiling instrumentation to insert
   */
  public void addProfiling(Vector<String> moduleNames, int profileOptions)
  {
    Vector<Object> fns = new Vector<Object>(); // An entry for each routine in the call graph.
    Type           pvt = PointerType.create(VoidType.type);
    int            ii  = 0;

    // Process each routine.

    Iterator<RoutineDecl> it  = allRoutines.iterator();
    Vector<RoutineDecl>   cns = new Vector<RoutineDecl>(10); // Necessary since we add routine __pf_profile_dump.
    while (it.hasNext())
      cns.addElement(it.next());

    boolean hasMain = false;
    int     lcns    = cns.size();
    for (int i = 0; i < lcns; i++) {
      RoutineDecl cn = cns.elementAt(i);

      hasMain |= cn.isMain() && (cn.getScribbleCFG() != null);

      VariableDecl vd = cn.addProfiling(profileOptions);
      if (vd == null)
        continue;

      // Create a link to the routine's profiling data.

      AddressLiteral al = new AddressLiteral(pvt, vd);

      fns.addElement(al);
    }

    Literal lit0 = LiteralMap.put(0, pvt);
    fns.addElement(lit0);

    // Create a variable containing the links to the tables of each
    // function's profiling data.

    String              mname   = extractModuleName(name).replace('-', '_');
    FixedArrayType      cgtype  = FixedArrayType.create(0, fns.size() - 1, pvt);
    Type                vt      = RefType.create(cgtype, RefAttr.Const);
    AggregationElements ae      = new AggregationElements(vt, fns);
    VariableDecl        cgTable = new VariableDecl("__pf_" + mname, vt, ae);

    cgTable.setVisibility(Visibility.GLOBAL);
    cgTable.setResidency(Residency.MEMORY);
    topLevelDefDecls.add(cgTable);
    topLevelDecls.add(cgTable);

    // If this is the call graph containing the "main" entry, create
    // the table of links to each call graph's table.

    if (hasMain) {
      Type           pfct     = SignedIntegerType.create(8);
      int            l        = moduleNames.size();
      Vector<Object> modules  = new Vector<Object>(l);
      Vector<Object> nmodules = new Vector<Object>(l);
      Type           ppvt     = PointerType.create(pvt);

      for (int i = 0; i < l; i++) {
        String       mod      = moduleNames.elementAt(i);
        String       mnme     = extractModuleName(mod);
        String       oname    = mnme.replace('-', '_');
        VariableDecl ocgTable = cgTable;

        if (!oname.equals(mname)) {
          ocgTable = new VariableDecl("__pf_" + oname, ppvt);
          ocgTable.setVisibility(Visibility.EXTERN);
          topLevelDecls.add(ocgTable);
          topLevelDefDecls.add(ocgTable);
        }

        FixedArrayType onamet = FixedArrayType.create(0, mnme.length(), pfct);
        StringLiteral  osl    = new StringLiteral(onamet, mnme + "\0");
        VariableDecl   osln   = new VariableDecl("__pf_n_" + oname, onamet, osl);

        osln.setResidency(Residency.MEMORY);
        topLevelDefDecls.add(osln);
        topLevelDecls.add(osln);

        AddressLiteral aosll = new AddressLiteral(ppvt, osln);
        AddressLiteral al    = new AddressLiteral(ppvt, ocgTable);

        nmodules.addElement(aosll);
        modules.addElement(al);
      }

      nmodules.addElement(lit0);
      modules.addElement(lit0);

      FixedArrayType      ntype     = FixedArrayType.create(0, l, pvt);
      AggregationElements noae      = new AggregationElements(ntype, nmodules);
      VariableDecl        nameTable = new VariableDecl("__profile_table_names", ntype, noae);

      nameTable.setVisibility(Visibility.GLOBAL);
      nameTable.setResidency(Residency.MEMORY);
      topLevelDefDecls.add(nameTable);
      topLevelDecls.add(nameTable);

      Type                at        = FixedArrayType.create(0, l, ppvt);
      Type                atype     = RefType.create(at, RefAttr.Const);
      AggregationElements oae       = new AggregationElements(atype, modules);
      VariableDecl        mainTable = new VariableDecl("__profile_table", atype, oae);

      mainTable.setVisibility(Visibility.GLOBAL);
      mainTable.setResidency(Residency.MEMORY);
      topLevelDefDecls.add(mainTable);
      topLevelDecls.add(mainTable);
    }
  }

  /**
   * Generate a message for a profile data problem and abort.
   */
  public static void reportProfileProblem(String text)
  {
    Msg.reportError(Msg.MSG_Invalid_profile_data_s, text);
    throw new scale.common.RuntimeException(text);
  }

  /**
   * Read in the profiling information for every call node in the call
   * graph.
   * @param profilePaths is the list of directories to search for
   * profile information and must include the module containing "main"
   * @param profileOptions specifies which profiling instrumentation to insert
   */
  public void readProfInfo(Vector<String> profilePaths, int profileOptions)
  {
    // Get the file name of the profile information for this module.

    String modn = extractModuleName(name).replace('-', '_');

    // Read the profile information from the generated file.

    boolean succeeded = false;

    StringBuffer buf = new StringBuffer();
    int          fl  = profilePaths.size();
    for (int fi = 0; fi < fl; fi++) {
      buf.setLength(0);
      buf.append(profilePaths.get(fi));
      if (buf.charAt(buf.length() - 1) != '/')
        buf.append('/');
      buf.append(modn);
      buf.append(".pft");

      String fileName = buf.toString();
      try {
        FileReader     fis      = new FileReader(fileName);
        BufferedReader br       = new BufferedReader(fis);
        HashMap<String, ProfileInfo> pfTable  = new HashMap<String, ProfileInfo>(23); // Map from routine name to the edge counts.

        String line = br.readLine();
        while (line != null) { // Read each line of each file.
          if (!line.startsWith("** Ftn: ")) {
            line = br.readLine();
            continue;
          }

          String fname = line.substring(8).trim();

          line = br.readLine();
          while (line != null) {
            if (line.startsWith(" hash: ")) {
              int         hash = Integer.parseInt(line.substring(7).trim());
              ProfileInfo pf   = new ProfileInfo(hash);

              // Read one routine's profile info. 

              line = readProfileFtnInfo(br, pf);
        
              // Check if there is already profile info for the routine.
              // If so, combine the latest info with the old info.

              ProfileInfo oldpf = pfTable.get(fname);
              if (oldpf != null)
                combineProfileInfo(pf, oldpf);

              // Store the combined profile.
              pfTable.put(fname, pf);
              break;
            }

            line = br.readLine();
          }
        }

        fis.close();

        // Apply profile information to each routine.

        Iterator<RoutineDecl> it = allRoutines.iterator();
        while (it.hasNext()) {
          RoutineDecl cn       = it.next();
          Scribble    scribble = cn.getScribbleCFG();

          if (scribble == null)
            continue;

          ProfileInfo pf = pfTable.get(cn.getRoutineName());
          if (pf == null)
            continue;

          // Apply edge, path, loop trip count, loop instruction count profiles.

          if (profileOptions != 0)
            scribble.applyProfInfo(pf, profileOptions);
        }

        succeeded = true;

      } catch(IOException ex) {
        System.out.println(ex.getMessage());
      }
    }

    if (!succeeded) {
      Msg.reportError(Msg.MSG_s, getName(), 0, 0, "Failed to find profiling information file.");
      
      // If specified, halt the compiler so we don't waste anymore
      // time trying to compile.
      if (PPCfg.getFailWithoutProfile())
        reportProfileProblem("Cannot continue without profile information file");
    }
  }

  /**
   * Add two profiles together.  Store the combined profile in the
   * first profile info variable.
   * @param dest is the first profile.  This profile is overwritten
   * with the combined profile.
   * @param src is the second profile.  It is not modified.
   */
  private void combineProfileInfo(ProfileInfo dest, ProfileInfo src)
  {
    // Check that the hash values are the same.  If they are not,
    // set the combined hash to an invalid value, so we can detect it later.
    if (dest.hash != src.hash) {
      dest.hash = -1;
      return;
    }

    // Combine the block profiles.

    if (dest.blockArray == null) {
      dest.blockArray = src.blockArray;
    } else if (src.blockArray != null) {
      for (int i = 0; i < src.blockArray.length; i++) {
        dest.blockArray[i] += src.blockArray[i];
      }
    }
    
    // Combine the edge profiles.

    if (dest.edgeArray == null) {
      dest.edgeArray = src.edgeArray;
    } else if (src.edgeArray != null) {
      for (int i = 0; i < src.edgeArray.length; i++) {
        dest.edgeArray[i] += src.edgeArray[i];
      }
    }
    
    // Combine the path profiles.

    if (dest.pathMap == null) {
      dest.pathMap = src.pathMap;
    } else if (src.pathMap != null) {
      Iterator<Long> iter = src.pathMap.keySet().iterator();
      while (iter.hasNext()) {
        Long pathNum  = iter.next();
        long srcFreq  = src.pathMap.get(pathNum).longValue();
        long destFreq = 0;
        if (dest.pathMap.containsKey(pathNum))
          destFreq = dest.pathMap.get(pathNum).longValue();

        dest.pathMap.put(pathNum, new Long(srcFreq + destFreq));
      }
    }
    
    // Combine the loop trip count profiles.

    if (dest.loopHistMap == null) {
      dest.loopHistMap = src.loopHistMap;
    } else if (src.loopHistMap != null) {
      for (int loopNum = 0; loopNum < src.numLoops; loopNum++) {
        long[] srcHistogram  = src.loopHistMap.get(loopNum);
        long[] destHistogram = dest.loopHistMap.get(loopNum);
        for (int si = 0; si < srcHistogram.length; si++)
          destHistogram[si] += srcHistogram[si];
      }
    }
  }
  
  /**
   *  Read the edge profile info for a function and add it to the
   *  table.
   */
  private String readProfileFtnInfo(BufferedReader br, ProfileInfo pf) throws IOException
  {
    while (true) { // Read each line of each file.
      String line = br.readLine();
      if (line == null)
        return null;

      int ind = line.indexOf('#');
      if (ind >= 0)
        line = line.substring(0, ind);

      line = line.trim();
      if (line.length() <= 0)
        continue;

      if (line.startsWith("** Ftn: "))
        return line;

      if (line.startsWith("edges: ")) {
        int numEdges = Integer.parseInt(line.substring(7));
        readProfEdgeInfo(pf, br, numEdges);
        continue;
      }

      if (line.startsWith("blocks: ")) {
        int numBlocks= Integer.parseInt(line.substring(8));
        readProfBlockInfo(pf, br, numBlocks);
        continue;
      }

      if (line.startsWith("paths: ")) {
        int numPaths = Integer.parseInt(line.substring(7));
        readProfPathInfo(pf, br, numPaths);
        continue;
      }
      
      if (line.startsWith("loops: ")) {
        int numLoops = Integer.parseInt(line.substring(7));
        pf.numLoops = numLoops;
        readProfLoopInfo(pf, br, numLoops);
        continue;
      }
    }
  }

  /**
   * Return the array of edge profile counts indexed by the edge
   * number.
   */
  private void readProfBlockInfo(ProfileInfo    pf,
                                 BufferedReader br,
                                 int            numBlocks) throws IOException
  {
    int[] pfArray = new int[numBlocks];
    pf.blockArray = pfArray;

    while (true) { // Read each line of each file.
      String line = br.readLine();

      if (line == null)
        return;

      if (line.trim().length() <= 0)
        return;

      int l = line.length();
      for (int i = 0; i < l; i++) {
        if (line.charAt(i) == '(')
          i = extractProfileInfo(line, i, pfArray);
      }
    }
  }

  /**
   * Return the array of edge profile counts indexed by the edge
   * number.
   */
  private void readProfEdgeInfo(ProfileInfo    pf,
                                BufferedReader br,
                                int            numEdges) throws IOException
  {
    int[] pfArray = new int[numEdges];

    pf.edgeArray = pfArray;

    while (true) { // Read each line of each file.
      String line = br.readLine();

      if (line == null)
        return;

      if (line.trim().length() <= 0)
        return;

      int l = line.length();
      for (int i = 0; i < l; i++) {
        if (line.charAt(i) == '(')
          i = extractProfileInfo(line, i, pfArray);
      }
    }
  }

  /**
   * Return the map of path numbers to path frequencies (Integer,
   * Integer).
   */
  private void readProfPathInfo(ProfileInfo    pf,
                                BufferedReader br,
                                int            numPaths) throws IOException
  {
    HashMap<Long, Long> pfMap = new HashMap<Long, Long>();

    pf.pathMap = pfMap;

    int numPathsRead = 0;
    while (true) { // Read each line of each file.
      String line = br.readLine();
      if (line == null ||
          line.trim().length() <= 0) {
        break;
      }

      // Split along whitespace boundaries (each pairs[i] has the form
      // "pathNum:count").

      String[] pairs = line.split("\\s+");
      for (int i = 0; i < pairs.length; i++) {
        if (pairs[i].trim().length() > 0) {
          extractProfilePath(pairs[i], pfMap);
          numPathsRead++;
        }
      }
    }

    if (numPathsRead != numPaths)
      reportProfileProblem("Expected different number of path pairs");
  }

  /**
   * Return the map of loop numbers to loop histograms.
   */
  private void readProfLoopInfo(ProfileInfo    pf,
                                BufferedReader br,
                                int            numLoops) throws IOException
  {
    IntMap<long[]> loopHistMap = null;
    int[]          icArray     = null;
    int[]          ucArray     = null;

    while (true) { // Read each line of each file.
      String line = br.readLine();
      if (line == null)
        break;

      line = line.trim();
      if (line.length() <= 0)
        break;

      int ic = line.indexOf(':');
      if (ic <= 0)
        continue;

      int loopNum = Integer.parseInt(line.substring(0, ic));

      line = line.substring(ic + 1); // Histogram values.

      if (line.charAt(0) == '(') { // Grab line number, etc.
        int ind = line.indexOf(')');
        if (ind <= 0)
          continue;

        String stats = line.substring(1, ind - 1);
        line = line.substring(ind + 1);
        String[] st = stats.split(",");
        if (st.length == 5) {
          if (icArray == null)
            icArray = new int[numLoops];
          if (ucArray == null)
            ucArray = new int[numLoops];
          icArray[loopNum] = Integer.parseInt(st[1]);
          ucArray[loopNum] = Integer.parseInt(st[3]);
        }
      }

      if (line.length() <= 0)
        continue;

      // Read in a new loop histogram for this loop.

      // Split along whitespace boundaries. Each pairs[i] has the form
      // "tripCount:freq" or "tripcount1-tripcount2:freq".

      String[] pairs  = line.split("\\s+");
      long[]   values = new long[Scribble.LTC_TABLE_SIZE];

      if (loopHistMap == null)
        loopHistMap = new IntMap<long[]>(11);

      loopHistMap.put(loopNum, values);

      for (int i = 0; i < pairs.length; i++) {
        String pair = pairs[i].trim();
        if (pair.length() > 0) {
          String[] vals = pair.split(":");

          if (vals.length != 2)
            reportProfileProblem("Expected colon between trip count and frequency.");  
 
          String tripstr = vals[0];
          int    ir      = tripstr.indexOf('-');
          if (ir > 0) // Range specified - use first value.
            tripstr = tripstr.substring(0, ir);

          int  tripCount = Integer.parseInt(tripstr);
          long freq      = Long.parseLong(vals[1]);

          int slot = tripCount;
          if (tripCount > 64) { // Slot index is power of two.
            int x = 31;
            tripCount--;
            if ((tripCount & 0xffff0000) == 0) {
              tripCount <<= 16;
              x -= 16;
            }
            if ((tripCount & 0xff000000) == 0) {
              tripCount <<= 8;
              x -= 8;
            }
            if ((tripCount & 0xf0000000) == 0) {
              tripCount <<= 4;
              x -= 4;
            }
            if ((tripCount & 0xc0000000) == 0) {
              tripCount <<= 2;
              x -= 2;
            }
            if ((tripCount & 0x80000000) == 0) {
              tripCount <<= 1;
              x -= 1;
            }
            slot = 59 + x;
          }
          values[slot] = freq;
        }
      }
    }

    pf.loopHistMap = loopHistMap;
    pf.ucArray     = ucArray;
    pf.icArray     = icArray;
  }
  
  /**
   * Process each set of profile information.
   */
  private int extractProfileInfo(String line, int index, int[] pfArray)
  {
    int ix = line.indexOf(')', index);
    if (ix < 0)
      return index;

    int edgeNumber = Integer.parseInt(line.substring(index + 1, ix));
    int iy         = line.indexOf(' ', ix + 2);
    if (iy < 0)
      iy = line.length();

    int count = Integer.parseInt(line.substring(ix + 2, iy));
    pfArray[edgeNumber] = count;
    return iy;
  }

  /**
   * Process each line of path profile information.
   */
  private void extractProfilePath(String pair, Map<Long, Long> pfMap)
  {
    String[] vals = pair.split(":");
    if (vals.length != 2)
      reportProfileProblem("Expected colon between numPaths and count.");

    Long pathNumber = Long.valueOf(vals[0]);
    Long count      = Long.valueOf(vals[1]);
    pfMap.put(pathNumber, count);
  }

  private String extractModuleName(String name)
  {
    int f  = name.lastIndexOf('/');
    int ln = name.lastIndexOf('.');
    if (ln < 0)
      ln = name.length();

    String str = name.substring(f + 1, ln);
    return str;
  }

  /**
   * Print to out the {@link scale.clef.decl.RoutineDecl routines} in
   * this call graph.
   */
  public void printAllRoutines()
  {
    Iterator<RoutineDecl> it = allRoutines.iterator();
    while (it.hasNext()) {
      RoutineDecl n    = it.next();
      String      name = n.getName();

      System.out.print("// " + name + " calls: ");
      n.printCallees();

      int l = n.numCallers();
      if (l > 0) {
        System.out.print("// " + name + " called by: ");

      for (int i = 0; i < l; i++) {
          RoutineDecl caller = n.getCaller(i);
          if (i > 0)
            System.out.print(", ");
          System.out.print(caller.getName());
        }
        System.out.println();
      }
    }
  }

  /**
   * Associate indirect calls with their targets.
   */
  public void processFunctionPointers()
  {
    Iterator<RoutineDecl> ep = pointerFuncs.iterator();
    while (ep.hasNext()) {
      RoutineDecl pd    = ep.next();
      Type        t     = pd.getSignature().getReturnType();

      Iterator<RoutineDecl> it = allRoutines.iterator();
      while (it.hasNext()) {
        RoutineDecl n = it.next();
        int         l = n.numCalleeCandidates();

        for (int i = 0; i < l; i++) {
          Type t1 = n.getCalleeCandidate(i);
          if (t == t1)
            n.addCallee(pd);
        }
      } 
      // If we don't actually make a call via a function pointer we still
      // need to add it to the list of routines.  So we need to create a 
      // dummy call node.  Otherwise, the routine name may not be added to
      // the Suite.

      recordRoutine(pd);
    }
  }

  /**
   * Record the {@link scale.clef.decl.RoutineDecl RoutineDecl}.
   */
  public void recordRoutine(RoutineDecl rd)
  {
    allRoutines.add(rd);
    rd.specifyCallGraph(this);
  } 

  /**
   * Return the main procedure if any.
   */
  public RoutineDecl getMain()
  {
    return rootRoutine;
  }

  /**
   * Specify the main procedure (if any) in the CallGraph.
   */
  public void setMain(RoutineDecl main)
  {
    assert ((rootRoutine == null) || (rootRoutine == main)) :
      "Main routine specified twice " + rootRoutine + " " + main;
    rootRoutine = main;
  }

  /**
   * Return an <code>Iterator</code> of all the {@link
   * scale.clef.decl.RoutineDecl routines}.
   */
  public Iterator<RoutineDecl> allRoutines()
  {
    if (!alphabeticalOrder)
      return allRoutines.iterator();

    RoutineDecl[] arr = new RoutineDecl[allRoutines.size()];
    arr = allRoutines.<RoutineDecl>toArray(arr);
    Arrays.sort(arr);
    List<RoutineDecl> l = Arrays.<RoutineDecl>asList(arr);
    return l.iterator();
  }

  /**
   * Return an array of all the {@link scale.clef.decl.RoutineDecl
   * routines}.
   */
  public RoutineDecl[] allRoutinesArray()
  {
    RoutineDecl[] arr = new RoutineDecl[allRoutines.size()];
    arr = allRoutines.<RoutineDecl>toArray(arr);
    return arr;
  }

  /**
   * Return the number of {@link scale.clef.decl.RoutineDecl
   * routines} in this call graph.
   */
  public int numRoutines()
  {
    return allRoutines.size();
  }

  /**
   * Add a top level {@link scale.clef.decl.Declaration declaration}.
   */
  public void addTopLevelDecl(Declaration decl)
  {
    if (!decl.isReferenced())
      return;

    RoutineDecl rd = decl.returnRoutineDecl();
    if (rd != null) {
      recordRoutine(rd);
    } else {
      if (topLevelDecls.contains(decl))
        return;

      if ((decl.visibility() == Visibility.LOCAL) && decl.isVariableDecl())
        return;

      topLevelDecls.add(decl);
    }

    if (decl.visibility() != Visibility.EXTERN)
      topLevelDefDecls.add(decl);
  }

  /**
   * Return an <code>Iterator</code> of all the top level {@link
   * scale.clef.decl.Declaration declarations} except routines.
   */
  public Iterator<Declaration> topLevelDecls()
  {
    return topLevelDecls.iterator();
  }

  /**
   * Return an <code>Iterator</code> of all the top level {@link
   * scale.clef.decl.Declaration declarations} (except routines) that
   * are defining definitions.  We use this to distinguish between
   * defining and references declarations (ie., those with the extern
   * keyword).
   */
  public Iterator<Declaration> topLevelDefDecls()
  {
    return topLevelDefDecls.iterator();
  }

  /**
   * Create a graphic display of the call graph.
   * @param da is the graph display
   */
  public void graphCallTree(DisplayGraph da)
  {
    Iterator<RoutineDecl> it = allRoutines.iterator();
    while (it.hasNext()) {
      RoutineDecl caller = it.next();
      int         l      = caller.numCallees();
      if (l > 0) {
        for (int i = 0; i < l; i++) {
          RoutineDecl callee = caller.getCallee(i);
          da.addEdge(caller, callee, DColor.GREEN, DEdge.DOTTED, callee.toString());
        }
      } else {
        da.addNode(caller);
      }
    }
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("(CG-");
    buf.append(id);
    buf.append(' ');
    buf.append(name);
    buf.append(')');
    return buf.toString();
  }

  /**
   * Add the {@link scale.clef.decl.Declaration declaration} to the
   * current symbol table scope.
   * @return the symbol table entry
   */
  public SymtabEntry addSymbol(Declaration decl)
  {
    Symtab      st = getSymbolTable();
    SymtabEntry se = st.lookupSymbol(decl);
    if ((se != null) && (se.getScope() == st.getCurrentScope()))
      return se;

    return st.addSymbol(decl);
  }

  /**
   * Add the {@link scale.clef.decl.Declaration declaration} to the
   * root symbol table scope.
   * @return the symbol table entry
   */
  public SymtabEntry addRootSymbol(Declaration decl)
  {
    Symtab      st = getSymbolTable();
    SymtabScope sc = st.getRootScope();
    SymtabEntry se = sc.lookupSymbol(decl);

    if ((se != null) && (se.getScope() == sc))
      return se;

    return st.addRootSymbol(decl);
  }

  /**
   * Clean up for profiling statistics.
   */
  public static void cleanup()
  {
  }
}
