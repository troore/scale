package scale.test;

import java.io.*;
import java.lang.Runtime;
import java.util.Enumeration;
import java.util.Iterator;

import java.util.StringTokenizer;

import scale.annot.*;
import scale.common.*;
import scale.clef.*;
import scale.clef.symtab.*;
import scale.clef.decl.*;
import scale.clef.type.*;
import scale.frontend.SourceLanguage;
import scale.callGraph.*;
import scale.alias.*;
import scale.alias.steensgaard.*;
import scale.alias.shapirohorowitz.*;
import scale.j2s.*;

import scale.visual.*;
import scale.score.*;
import scale.score.chords.Chord;
import scale.score.pred.*;
import scale.score.analyses.*;
import scale.score.trans.*;
import scale.score.dependence.DDGraph;
import scale.backend.Generator;
import scale.clef2scribble.Clef2Scribble;

/**
 * This class provides a "C compiler" using the common switches.
 * <p>
 * $Id: CC.java,v 1.39 2007-10-04 20:03:09 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class CC extends Scale
{
  public CmdParam cpVerbose = new CmdParam("v", true,  CmdParam.SWITCH, null, Msg.HLP_vcc);
  public CmdParam cpCcf     = new CmdParam("c", true,  CmdParam.SWITCH, null, Msg.HLP_ccc);
  public CmdParam cpAf      = new CmdParam("S", true,  CmdParam.SWITCH, null, Msg.HLP_S);
  public CmdParam cpCofile  = new CmdParam("o", false, CmdParam.STRING, null, Msg.HLP_occ);
  public CmdParam cpCO      = new CmdParam("O", true,  CmdParam.INT,    null, Msg.HLP_Occ);

  protected CmdParam[] paramsCC = {
    cpCcf,    cpAf,     cpCofile,  cpArch,    cpIncl,
    cpIncls,  cpPrePro, cpCO,      cpInl,     cpAnnot,
    cpSla,    cpR,      cpD,       cpG,       cpNaln,
    cpUnsafe, cpIs,     cpBi,      cpC89,     cpC99,
    cpGcc,    cpCkr,    cpAnsi,    cpCcb,     cpCca,
    cpCgb,    cpCga,    cpScb,     cpSca,     cpSgb,
    cpSga,    cpDcg,    cpGphType,
    cpPi,     cpPg,     cpPp,
    cpCc,     cpAsm,    cpWhich,
    cpDebug,  cpTcl,    cpFcl,     cpVers,    cpStat,
    cpSf,     cpCdd,    cpSan,     cpSnap,    cpVerbose
  };

  protected String dirPath = "";

  private static final String[] optstr = {"ud", "cud", "gcmnpudl", "fgcmnpxnmpudl", "fjgcamnpxnmpudl"};

  protected CC()
  {
    super();
  }

  /**
   * Compile a C or Fortran program.
   * @param args the command line arguments
   */
  public static void main(String[] args) 
  {
    CC me = new CC();
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

    parseCmdLine(args, paramsCC); // Parse the command line arguments

    Statistics.reportStatus(Msg.MSG_Start);

    try {
      separateCompilation();
    } catch(java.lang.Error er) {
      String msg = er.getMessage();
      if (msg == null)
        msg = er.getClass().getName();
      System.out.println("** " + msg);
      er.printStackTrace();
      aborted = true;
    } catch(java.lang.RuntimeException err) {
      String msg = err.getMessage();
      if (msg == null)
        msg = err.getClass().getName();
      System.out.println("** " + msg);
      err.printStackTrace();
      aborted = true;
    } catch(java.lang.Exception ex) {
      String msg = ex.getMessage();
      if (msg == null)
        msg = ex.getClass().getName();
      System.out.println("** " + msg);
      ex.printStackTrace();
      aborted = true;
    }

    DisplayGraph visualizer = DisplayGraph.getVisualizer();
    if (visualizer != null)
      visualizer.interact();

    Statistics.reportStatus(Msg.MSG_End);
    Statistics.reportStatistics(1, cpSf.getStringValue());

    System.exit(aborted ? 1 : 0);
  }

  /**
   * Process the command line parameters.
   * @param args the array of command line parameters
   * @param params an array of allowed command line parameters
   */
  protected void parseCmdLine(String[] args, CmdParam[] params)
  {
    inputFiles = parseCmdLine(args, params, cpFiles);
    if (inputFiles.size() == 0) {
      Msg.reportError(Msg.MSG_No_source_file_specified, null, 0, 0, null);
      System.exit(1);
    } else if (inputFiles.size() != 1) {
      Msg.reportError(Msg.MSG_More_than_one_source_file_specified, null, 0, 0, null);
      System.exit(1);
    }

    dirPath = getPath(inputFiles.elementAt(0));
    if (dirPath.equals(""))
      dirPath = ".";

    String forWhat = parseWhichParam();

    Debug.setDebugLevel(cpDebug.getIntValue());
    Statistics.setStatusLevel(cpStat.getIntValue(), forWhat);

    Msg.reportInfo = cpVerbose.specified();

    int reportLevel = cpCdd.getIntValue();
    Node.setReportLevel(reportLevel); // Set Clef's level for displaying children.
    Note.setReportLevel(reportLevel); // Set Scribble's level for displaying children.

    int annotationLevel = cpSan.getIntValue();
    Note.setAnnotationLevel(annotationLevel);
    Node.setAnnotationLevel(annotationLevel);

    String rname = cpR.getStringValue();
    Debug.setReportName(rname); // The selected routine name if any

    all = (rname == null);   // If there is no selected routine name

    doOfile = cpCofile.specified();
    doA     = cpCcf.specified();

    if (doA && doC) {
      Msg.reportError(Msg.MSG_Conflicting_parameters_choose_one_of_s, null, 0, 0, "-S, -c");
      System.exit(1);
    }

    doOfile  &= parseArchParams();

    doSingle = parseAA();

    int oLevel = 2;
    if (cpCO.specified())
      oLevel = cpCO.getIntValue();
    if ((oLevel < 0) || (oLevel > 4)) {
      Msg.reportError(Msg.MSG_Invalid_parameter_s, null, 0, 0, "Invalid -O level");
      System.exit(1);
    }

    parseMiscellaneousParams(forWhat);
    parseOpts(optstr[oLevel], forWhat);
    parseTraces(cpTcl.getStringValues());
    parseFlags(cpFcl.getStringValues());
  }

  /**
   * Generate a .c file from a CallGraph using each RoutineDecl's Scribble graph.
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
      String         fnameo = cpCofile.getStringValue();
      Vector<String> ccmd   = new Vector<String>();

      splitString(cname, ccmd);

      if (cpG.specified())
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

  private String getPath(String name)
  {
    int b = name.lastIndexOf(File.separatorChar);
    if (b < 0)
      return "";
    else
      b++;
    return name.substring(0, b);
  }

  /**
   * Return a path name for the new file including the directory specified by the "dir" switch.
   * @param name a file path from which to extract the file name
   * @param extension the extension to use for the new file
   */
  protected String genFileName(String name, String extension)
  {
    int b = name.lastIndexOf(File.separatorChar);
    int e = name.lastIndexOf(".");
    if (b < 0)
      b = 0;
    else
      b++;
    if ((e < 0) || (e < b))
      e = name.length();

    StringBuffer fname = new StringBuffer(dirPath);
    fname.append(File.separatorChar);
    fname.append(name.substring(b, e)); // the file name without the extension or path
    fname.append(extension);
    return fname.toString();
  }
  /**
   * Generate an assembly file from a CallGraph using each RoutineDecl's Scribble graph.
   * @param cg is the CallGraph
   */
  protected void genAssemblyfromCallGraph(CallGraph cg) throws java.lang.Exception
  {
    SourceLanguage lang  = cg.getSourceLanguage();
    String         as    = Machine.getAssemblerCommand(cpAsm.getStringValue(), backendFeatures);
    String         fname = genFileName(cg.getName(), Machine.currentMachine.getAsmFileExtension());

    Statistics.reportStatus(Msg.MSG_Generating_s, fname);

    FileOutputStream fos  = new FileOutputStream(fname);
    PrintWriter      cout = new PrintWriter(fos, true);
    Emit             em   = new EmitToFile(cout, 2);
    Generator        gen  = Machine.getCodeGenerator(cg, backendFeatures);

    gen.generate();

    gen.assemble(em, cg.getName(), warnings.elements());

    fos.close();

    if (doOfile) {
      String         fnameo = cpCofile.getStringValue();
      Vector<String> ccmd   = new Vector<String>();

      splitString(as, ccmd);

      if (cpG.specified()) {
        if (architecture.equals("alpha"))
          ccmd.addElement("-g");
      }

      ccmd.addElement(fname);
      ccmd.addElement("-o");
      ccmd.addElement(fnameo);

      if (executeCommand(ccmd))
        throw new scale.common.Error("Failed to assemble " + fname);
    }
  }
}
