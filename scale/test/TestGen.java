package scale.test;

import java.io.*;
import java.util.StringTokenizer;
import scale.common.*;

/**
 * This class generates makefiles to do regression testing.
 * <p>
 * $Id: TestGen.java,v 1.24 2007-10-04 19:58:40 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The following are the optional parameters for the program:
 * <dl>
 * <dt>t<dd>a list of tests to be performed/processed - see below.
 * <dt>m<dd>Select one of
 * <dl>
 * <dt>make<dd>generate a makefile
 * <dt>gather<dd>generate a script to gather the log files
 * <dt>check<dd>generate a script for checking the log files
 * </dl>
 * <dt>cmd<dd>specify the makefile rule to use
 * <dt>npfa<dd>specify to not use pure function annotations
 * </dl>
 * <h3>Tests</h3>
 * Each test is specified as a triple in the form
 * n/optimization/benchmark.  See below for the lists of allowed
 * optimizations and benchmarks.  <tt>n</tt> is the alias analysis
 * level and must be 1, 2, 3.1, 3.2, 3.4, 3.8, 4, 5, 6.1, 6.2, 6.4, or
 * 6.8.  If any part of the triple is missing or is '*', all
 * possibilities are selected.  If no item is specified, all 600 tests
 * are selected.
 * <h4>Allowed Optimizations</h4>
 * <ul>
 * <li>scnrvud
 * <li>scpenrvud
 * <li>scpervud
 * <li>Scnrvud
 * <li>Scpenrvud
 * <li>Scpervud
 * </ul>
 * <h4>Allowed Benchmarks</h4>
 * <table>
 * <tbody>
 * <tr><td>anagram</td>
 *     <td>applu</td>
 *     <td>apsi</td>
 *     <td>bc</td>
 * <tr><td>bisort</td>
 *     <td>compress95</td>
 *     <td>fpppp</td>
 *     <td>ft</td>
 * <tr><td>gcc</td>
 *     <td>go</td>
 *     <td>health</td>
 *     <td>hydro2d</td>
 * <tr><td>impulse</td>
 *     <td>jpeg</td>
 *     <td>ks</td>
 *     <td>m88ksim</td>
 * <tr><td>mgrid</td>
 *     <td>mst</td>
 *     <td>perimeter</td>
 *     <td>perl</td>
 * <tr><td>power</td>
 *     <td>su2cor</td>
 *     <td>swim</td>
 *     <td>tomcatv</td>
 *     <td>treeadd</td>
 * <tr><td>tsp</td>
 *     <td>turb3d</td>
 *     <td>vortex</td>
 *     <td>wave5</td>
 *     <td>xlisp</td>
 * <tr><td>yacr2</td>
 * </tbody>
 * </table>
 * <h3>Mode</h3>
 * When a test is performed, it generates a log file in the test
 * directory.  For example, the test 3/Scnruvd/bisort with the cmd parameter
 * set to "diff" generates a log file whose pathname is
 * <pre>
 * $SCALE/$SCALERELEASE/$SCALEHOST/scale/test/tests/bisort/a3Scnruvd/diff.log
 * </pre>
 * If the mode (-m) is set to gather, a script to collect the
 * generated log files is generated instead of the makefile that runs
 * the tests.  If the mode is set to check, a script is generated that
 * scans each log file and trys to determine if the test succeeded.
 * If the -npfa switch is specified, the pathname for the log file is
 * <pre>
 * $SCALE/$SCALERELEASE/$SCALEHOST/scale/test/tests/bisort/a3Scnruvd_npfa/diff.log
 * </pre>
 * and the generated makefile does not define the environment variable
 * that specifies that annotations are to be used.
 * <p>
 * All three modes are not placed into one makefile file as the resultant makefile file
 * could be quite large.
 */
public class TestGen
{
  private static final String helpM   = "make, gather, or check";
  private static final String helpCmd = "makefile rule to use - diff, scale, stimes, clean, clobber, etc.";
  private static final String helpRc  = "filename of configuration file";
  private static final String helpW   = "prepend string to 'gather' output file names";

  protected CmdParam   mode   = new CmdParam("m",    true,  CmdParam.STRING, "make", helpM);
  protected CmdParam   cmd    = new CmdParam("cmd",  true,  CmdParam.STRING, "diff", helpCmd);
  protected CmdParam   rc     = new CmdParam("rc",   false, CmdParam.STRING, null,   helpRc);
  protected CmdParam   what   = new CmdParam("w",    true,  CmdParam.STRING, null,   helpW);
  protected CmdParam[] params = {mode, cmd, what, rc};

  public static final int C    = 0;
  public static final int F77  = 1;
  public static final int JAVA = 2;

  protected boolean[][][] cube;     // True if a particular test should be done.
  protected boolean[][]   plane;    // True is any one test of a particular alias analysis and optimization level should be done.
  protected boolean[]     vect;     // True is any one test of a particular alias analysis level should be done.
  protected String        command    = "diff";
  protected String        type       = "make";
  protected int           commandCnt = 0;
  protected String[]      benchmarks = null;
  protected String[]      AAL        = null;
  protected String[]      opts       = null;
  protected String        backend    = "asm";
  protected String        nativetag  = null;
  protected boolean       doLte      = false;
  protected boolean       noPFA      = false;

  public TestGen()
  {
  }

  public static void main(String[] args)
  {
    TestGen me = new TestGen();
    Msg.setup(null);
    me.parseCmdLine(args, me.params);    // Parse the command line arguments
    if (me.type.equals("gather"))
      me.makeGather();
    else if (me.type.equals("check"))
      me.makeCheck();
    else
      me.makeMakefile();
  }

  private int getAA(String s)
  {
    int i = s.indexOf('.');
    if (i < 0)
      return Integer.parseInt(s);
    return Integer.parseInt(s.substring(0, i));    
  }

  private int getCat(String s)
  {
    int i = s.indexOf('.');
    if (i < 0)
      return 0;
    return Integer.parseInt(s.substring(i + 1));    
  }

  private void makeMakefile()
  {
    System.out.println("# generated file - do not edit");
    System.out.println("DEST    = $(SCALE)/$(SCALERELEASE)/$(SCALEHOST)/scale/test/tests");
    System.out.println("TEST    = $(SCALE)/$(SCALERELEASE)/scale/test/tests");
    System.out.println("SHELL   = /bin/csh -f");

    if (!noPFA)
      System.out.println("ANNOTATIONS = YES");
    System.out.println();

    System.out.print("all: ");
    for (int i = 0; i < AAL.length; i++) {
      int aa = getAA(AAL[i]);
      int cat = getCat(AAL[i]);
      if (vect[i]) {
        System.out.print(" ");
        System.out.print(genRuleName(null, aa, cat, ""));
      }
    }
    System.out.println();
    System.out.println();

    for (int i = 0; i < AAL.length; i++) {
      if (vect[i]) {
        int aa = getAA(AAL[i]);
        int cat = getCat(AAL[i]);
        System.out.print(genRuleName(null, aa, cat, ""));
        System.out.print(":");
        for (int k = 0; k < opts.length; k++) {
          if (plane[i][k]) {
            System.out.print(" ");
            System.out.print(genRuleName(null, aa, cat, opts[k]));
          }
        }
        System.out.println();
        System.out.println();
        for (int k = 0; k < opts.length; k++) {
          if (plane[i][k]) {
            System.out.print(genRuleName(null, aa, cat, opts[k]));
            System.out.print(": ");
            for (int l = 0; l < benchmarks.length; l++) {
              if (cube[i][k][l]) {
                System.out.println(" \\");
                System.out.print("\t");
                System.out.print(genRuleName(benchmarks[l], aa, cat, opts[k]));
              }
            }
            System.out.println();
            System.out.println();
            for (int l = 0; l < benchmarks.length; l++) {
              if (cube[i][k][l]) {
                System.out.print(genRuleName(benchmarks[l], aa, cat, opts[k]));
                System.out.println(": ");
                System.out.println(genEcho(benchmarks[l], aa, cat, opts[k]));
                System.out.println(genMkdir(benchmarks[l], aa, cat, opts[k]));
                System.out.println(genCommand(benchmarks[l], aa, cat, opts[k]));
              }
            }
          }
        }
        System.out.println();
      }
    }

    System.out.println("# Number rules = " + commandCnt);
  }

  private void makeGather()
  {
    System.out.println("#! /bin/csh -f");
    System.out.println("# generated file - do not edit");
    System.out.println("setenv DEST  $SCALE/$SCALERELEASE/$SCALEHOST/scale/test/tests");
    String wh = (String) what.getValue();

    for (int i = 0; i < AAL.length; i++) {
      if (vect[i]) {
        int aa = getAA(AAL[i]);
        int cat = getCat(AAL[i]);
        for (int k = 0; k < opts.length; k++) {
          if (plane[i][k]) {
            System.out.print("cp /dev/null ");
            System.out.println(genLogFileName(aa, cat, opts[k], wh));

            for (int l = 0; l < benchmarks.length; l++) {
              if (cube[i][k][l]) {
                System.out.println(genGrepCommand(benchmarks[l], aa, cat, opts[k], wh));
              }
            }
          }
          System.out.println();
        }
      }
    }
  }

  private void makeCheck()
  {
    System.out.println("#! /bin/csh -f");
    System.out.println("# generated file - do not edit");
    System.out.println("setenv DEST  $SCALE/$SCALERELEASE/$SCALEHOST/scale/test/tests");
    for (int i = 0; i < AAL.length; i++) {
      if (vect[i]) {
        int aa = getAA(AAL[i]);
        int cat = getCat(AAL[i]);
        for (int k = 0; k < opts.length; k++) {
          if (plane[i][k]) {
            for (int l = 0; l < benchmarks.length; l++) {
              if (cube[i][k][l]) {
                System.out.println(genCheckCommand(benchmarks[l], aa, cat, opts[k]));
              }
            }
          }
          System.out.println();
        }
      }
    }
  }

  private String genCheckCommand(String bench, int aal, int cat, String opt)
  {
    StringBuffer buf = new StringBuffer("echo -n \"");
    buf.append(genDirName(aal, cat, opt));
    buf.append('-');
    buf.append(command);
    buf.append(' ');
    String b = bench;
    if (b.length() < 10)
      b = b.concat("          ".substring(0, 10 - b.length()));
    buf.append(b);
    buf.append(" \"\n");
    buf.append("if (-e ");
    buf.append(genCPath(bench, aal, cat, opt));
    buf.append(command);
    if (nativetag != null) {
      buf.append('_');
      buf.append(nativetag);
    }
    buf.append(".log) then\n");
    buf.append("  echo -n \" exists \"\n");
    buf.append("endif\n");
    if (command.equals("scale")) {
      buf.append("if (-e ");
      buf.append(genCPath(bench, aal, cat, opt));
      buf.append(bench);
      buf.append(") then\n");
      buf.append("  echo -n \"succeeded\"\n");
      buf.append("endif\n");
    } else if (command.equals("native")) {
      buf.append("if (-e ");
      buf.append(genNPath(bench, nativetag));
      buf.append(bench);
      buf.append(") then\n");
      buf.append("  echo -n \"succeeded\"\n");
      buf.append("endif\n");
    } else {
      buf.append("grep \"^[*][*] Test.*succeeded\" ");
      buf.append(genCPath(bench, aal, cat, opt));
      buf.append(command);
      if (nativetag != null) {
        buf.append('_');
        buf.append(nativetag);
      }
      buf.append(".log >&/dev/null\n");
      buf.append("if ($status == 0) then\n");
      buf.append("  echo -n \"succeeded\"\n");
      buf.append("endif\n");
    }
    buf.append("echo \" \"");
    return buf.toString();
  }

  private String genLogFileName(int aal, int cat, String opt, String wh)
  {
    StringBuffer buf = new StringBuffer("");
    if (wh != null) {
      buf.append(wh);
      buf.append('-');
    }
    buf.append("a");
    buf.append(aal);
    buf.append('c');
    buf.append(cat);
    buf.append(opt);
    if (doLte)
      buf.append("_lte");
    if (noPFA)
      buf.append("_npfa");
    buf.append('-');
    buf.append(command);
    if (nativetag != null) {
      buf.append('_');
      buf.append(nativetag);
    }
    buf.append(".log");
    return buf.toString();
  }

  private String genGrepCommand(String bench, int aal, int cat, String opt, String wh)
  {
    String       log = genLogFileName(aal, cat, opt, wh);
    StringBuffer buf = new StringBuffer("if (-e ");
    buf.append(genCPath(bench, aal, cat, opt));
    buf.append(command);
    if (nativetag != null) {
      buf.append('_');
      buf.append(nativetag);
    }
    buf.append(".log) then\n  ");
    buf.append("echo \"***************************** ");
    buf.append(bench);
    buf.append("\" >>");
    buf.append(log);
    buf.append("\n  grep \"(stat\" ");
    buf.append(genCPath(bench, aal, cat, opt));
    buf.append(command);
    if (nativetag != null) {
      buf.append('_');
      buf.append(nativetag);
    }
    buf.append(".log >>");
    buf.append(log);
    buf.append("\nendif");
    return buf.toString();
  }

  private String genDirName(int aal, int cat, String opt)
  {
    StringBuffer buf = new StringBuffer("a");
    buf.append(aal);
    buf.append('c');
    buf.append(cat);
    buf.append(opt);
    if (doLte)
      buf.append("_lte");
    if (noPFA)
      buf.append("_npfa");
    return buf.toString();
  }

  private String genCPath(String bench, int aal, int cat, String opt)
  {
    StringBuffer buf = new StringBuffer("$DEST/");
    buf.append(bench);
    buf.append('/');
    buf.append(genDirName(aal, cat, opt));
    buf.append('/');
    return buf.toString();
  }

  private String genNPath(String bench, String nativetag)
  {
    if (nativetag == null)
      nativetag = "native";
    StringBuffer buf = new StringBuffer("$DEST/");
    buf.append(bench);
    buf.append('/');
    buf.append(nativetag);
    buf.append('/');
    return buf.toString();
  }

  private String genRuleName(String bench, int aal, int cat, String opt)
  {
    StringBuffer buf = new StringBuffer("");
    buf.append(genDirName(aal, cat, opt));
    if (bench != null) {
      buf.append('_');
      buf.append(bench);
    }
    return buf.toString();
  }

  private String genCommand(String bench, int aal, int cat, String opt)
  {
    commandCnt++;
    StringBuffer buf = new StringBuffer("\t@-(cd $(TEST)/");
    buf.append(bench);
    buf.append("; $(MAKE) makefile; \\\n\t$(MAKE) ");
    buf.append(command);
    buf.append(" SCALETAG=");
    buf.append(genDirName(aal, cat, opt));
    if (nativetag != null) {
      buf.append(" NATIVETAG=");
      buf.append(nativetag);
    }
    if (command.equals("scale")) {
      buf.append(" DSPLY=\"-stat 1\" BACKEND=");
      buf.append(backend);
      buf.append(" OPT=\"-O ");
      buf.append(opt);
      if (doLte)
        buf.append(" -lte");
      buf.append(" -AA ");
      buf.append(aal);
      buf.append(" -cat ");
      buf.append(cat);
    buf.append("\"");
    }
    buf.append(" >&");
    buf.append(genPath(bench, aal, cat, opt));
    buf.append(command);
    if (nativetag != null) {
      buf.append("_");
      buf.append(nativetag);
    }
    buf.append(".log;)\n");
    return buf.toString();
  }

  private String genEcho(String bench, int aal, int cat, String opt)
  {
    StringBuffer buf = new StringBuffer("\t@echo Doing ");
    buf.append(genRuleName(bench, aal, cat, opt));
    return buf.toString();
  }

  private String genMkdir(String bench, int aal, int cat, String opt)
  {
    StringBuffer buf = new StringBuffer("\t@mkdir -p ");
    buf.append(genPath(bench, aal, cat, opt));
    return buf.toString();
  }

  private String genPath(String bench, int aal, int cat, String opt)
  {
    StringBuffer buf = new StringBuffer("$(DEST)/");
    buf.append(bench);
    buf.append('/');
    buf.append(genDirName(aal, cat, opt));
    buf.append('/');
    return buf.toString();
  }

  private void initCube(boolean flag)
  {
    cube  = new boolean[AAL.length][opts.length][benchmarks.length];
    plane = new boolean[AAL.length][opts.length];
    vect  = new boolean[AAL.length];

    for (int i = 0; i < AAL.length; i++) {
      vect[i] = flag;
      for (int j = 0; j < opts.length; j++) {
        plane[i][j] = flag;
        for (int k = 0; k < benchmarks.length; k++)
          cube[i][j][k] = flag;
      }
    }
  }

  /**
   * Process the command line parameters.
   * @param args the array of command line parameters
   * @param params an array of allowed command line parameters
   */
  protected void parseCmdLine(String[] args, CmdParam[] params)
  {
    try {
      if (CmdParam.parse(this.getClass().getName(), args, params, null))
        System.exit(0);
    } catch(scale.common.InvalidKeyException ex) {
      System.out.println(ex.getMessage());
      CmdParam.usage(System.out, this.getClass().getName(), params);
      System.exit(1);
    }

    processConfigFile(rc.getStringValues());
    initCube(true);

    command  = cmd.getStringValue();
    type     = mode.getStringValue();
  }

  private void processConfigFile(Vector<String> files)
  {
    String          file = files.elementAt(0);
    String          line = "";
    boolean         ok   = true;
    Vector<String>  bmks = new Vector<String>();

    try {
      FileReader     fis   = new FileReader(file);
      BufferedReader br    = new BufferedReader(fis);
      Vector<String> toks  = new Vector<String>(10);

      while (ok) { // Read each line of each file.
        line = br.readLine();
        if (line == null)
          break;

        line = line.trim();

//      System.err.println("L " + line);

        if ((line.length() == 0) || (line.charAt(0) == '#'))
          continue;

        StringTokenizer tok = new StringTokenizer(line, "\n\r\t ,", false);

        if (!tok.hasMoreTokens())
          continue;

        String cmd = tok.nextToken();

        toks.clear();
        while (tok.hasMoreTokens())
          toks.addElement(tok.nextToken());

        int n = toks.size();

        if (cmd.equals("benchmarks")) {
          String f = toks.elementAt(0);
          for (int i = 0; i < n; i++) {
            String s = toks.elementAt(i);
            bmks.addElement(s);
          }
        } else if (cmd.equals("aa")) {
          String f = toks.elementAt(0);
          AAL = new String[n];
          for (int i = 0; i < n; i++) {
            String s = toks.elementAt(i);
            AAL[i] = s;
          }
        } else if (cmd.equals("opts")) {
          String f = toks.elementAt(0);
          opts = new String[n];
          for (int i = 0; i < n; i++) {
            String s = toks.elementAt(i);
            opts[i] = s;
          }
        } else if (cmd.equals("backend")) {
          backend = toks.elementAt(0);
        } else if (cmd.equals("lte")) {
          doLte = true;
        } else if (cmd.equals("native")) {
          nativetag = toks.elementAt(0);
        } else if (cmd.equals("npfa")) {
          noPFA = true;
        } else {
          ok = false;
          break;
        }
      }
      fis.close();
      br.close();
    } catch(IOException ex) {
      ex.printStackTrace();
    }

    if (!ok) {
      System.err.println("Invalid configuration file - " + line);
      System.exit(1);
    }

    benchmarks = new String[bmks.size()];
    for (int i = 0; i < bmks.size(); i++)
      benchmarks[i] = bmks.elementAt(i);
  }
}
