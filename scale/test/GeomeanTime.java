package scale.test;

import java.io.*;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.GregorianCalendar;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;

import scale.common.*;

/**
 * This class calculates the geometric mean of the ratio of execution
 * times to a minimum time.
 * <p>
 * $Id: GeomeanTime.java,v 1.4 2007-10-04 19:58:39 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This program processes a data file and outputs another data file.
 * It acts as a filter and a combiner.  Each line in the input file
 * must be in the form:
 * <pre>
 *   yymmdd  benchmark  time ignored-characters\n
 * </pre>
 * where <code>time</code> is the number of seconds in floating point
 * format and <code>benchmark</code> is the benchmark name.  Only data
 * from recognized benchmarks is used.  The program recognizes 52
 * different benchmarks.
 * <p>
 * A table is formed with one unique row for each date and one column
 * for the execution time of each benchmark.
 * <p>
 * The geometric mean is calculated for each date.  Benchmarks whose
 * minimum time is 0 are not used.  Nor are times that are less than
 * 10% of the benchmark's minimum time.  The minimum time for each
 * benchmark for dodo.cs.umass.edu and star.cs.umass.edu are provided
 * as part of the program.
 * <p>
 * After the data is read in, the table formed, and the geometric mean
 * calculated, the selected information is written to separate files
 * whose names are the benchmark names.  This output is meant to be
 * used as input to a graphing tool.  The form of the output is:
 * <pre>
 * yymmdd time\n
 * </pre>
 */
public class GeomeanTime
{
  private static class Benchmark
  {
    public String benchmark;
    public double starMin;
    public double dodoMin;

    public Benchmark(String benchmark, String starMin, String dodoMin)
    {
      this.benchmark = benchmark;
      this.starMin   = Double.parseDouble(starMin);
      this.dodoMin   = Double.parseDouble(dodoMin);
    }

    public double getMin(String system)
    {
      if ("dodo".equals(system))
        return dodoMin;
      return starMin;
    }
  }

  private static final String helpLast = "only the last n";
  private static final String helpSys  = "star or dodo";
  private static final String helpDb   = "beginning date: -db yymmdd";
  private static final String helpDe   = "ending date: -de yymmdd";
  private static final String helpDir  = "directory to receive the generated files";
  private static final String helpFile = "the input data file";

  private CmdParam   last   = new CmdParam("last",   true,  CmdParam.INT,    null, helpLast);
  private CmdParam   sys    = new CmdParam("sys",    true,  CmdParam.STRING, null, helpSys);
  private CmdParam   db     = new CmdParam("db",     true,  CmdParam.STRING, null, helpDb);
  private CmdParam   de     = new CmdParam("de",     true,  CmdParam.STRING, null, helpDe);
  private CmdParam   dir    = new CmdParam("dir",    true,  CmdParam.STRING, ".",  helpDir);
  private CmdParam   file   = new CmdParam("i",      false, CmdParam.STRING, null, helpFile);
  private CmdParam[] params = {dir, db, de, sys, last};

  private Benchmark[] benchmarks; // The set of recognized benchamrks.

  private int numBenchmarks; // Number of benchmarks.
  private int numColumns;    // Number of columns of data.
  private int lastN;         // Number of data rows to save.

  private String inputFile;     // Input data file name.
  private String token;         // Result from nextToken().
  private String beginningDate; // Starting data to save.
  private String endingDate;    // Ending data to save.
  private String system;        // star or dodo
  private String directory;     // Result files directory.

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) 
  {
    GeomeanTime me = new GeomeanTime();

    Msg.setup(null);
    me.parseCmdLine(args, me.params); // Parse the command line arguments
    me.process();
  }

  /**
   * Do the processing.
   */
  private void process()
  {
    String   lastDate = "";
    String[] times    = new String[numColumns * 256];

    numBenchmarks = 0;
    benchmarks = new Benchmark[60];
    benchmarks[numBenchmarks++] = new Benchmark("099.go", "61.31", "82.52");
    benchmarks[numBenchmarks++] = new Benchmark("101.tomcatv", "05.47", "7.45");
    benchmarks[numBenchmarks++] = new Benchmark("102.swim", "02.40", "3.34");
    benchmarks[numBenchmarks++] = new Benchmark("103.su2cor", "02.72", "3.99");
    benchmarks[numBenchmarks++] = new Benchmark("104.hydro2d", "02.97", "4.87");
    benchmarks[numBenchmarks++] = new Benchmark("107.mgrid", "10.67", "18.23");
    benchmarks[numBenchmarks++] = new Benchmark("110.applu", "49.19", "81.77");
    benchmarks[numBenchmarks++] = new Benchmark("124.m88ksim", "0.00", "0.00");
    benchmarks[numBenchmarks++] = new Benchmark("125.turb3d", "40.70", "78.11");
    benchmarks[numBenchmarks++] = new Benchmark("129.compress", "207.84", "186.67");
    benchmarks[numBenchmarks++] = new Benchmark("130.li", "2.95", "4.41");
    benchmarks[numBenchmarks++] = new Benchmark("132.jpeg", "86.08", "88.44");
    benchmarks[numBenchmarks++] = new Benchmark("134.perl", "8.75", "10.18");
    benchmarks[numBenchmarks++] = new Benchmark("141.apsi", "29.87", "41.93");
    benchmarks[numBenchmarks++] = new Benchmark("145.fpppp", "05.11", "7.75");
    benchmarks[numBenchmarks++] = new Benchmark("146.wave5", "14.21", "21.43");
    benchmarks[numBenchmarks++] = new Benchmark("164.gzip", "11.90", "11.12");
    benchmarks[numBenchmarks++] = new Benchmark("168.wupwise", "38.47", "52.36");
    benchmarks[numBenchmarks++] = new Benchmark("171.swim", "03.58", "4.63");
    benchmarks[numBenchmarks++] = new Benchmark("172.mgrid", "123.31", "149.77");
    benchmarks[numBenchmarks++] = new Benchmark("173.applu", "01.19", "1.97");
    benchmarks[numBenchmarks++] = new Benchmark("175.vpr", "1.93", "2.27");
    benchmarks[numBenchmarks++] = new Benchmark("176.gcc", "7.41", "8.76");
    benchmarks[numBenchmarks++] = new Benchmark("177.mesa", "7.54", "10.80");
    benchmarks[numBenchmarks++] = new Benchmark("179.art", "15.89", "13.85");
    benchmarks[numBenchmarks++] = new Benchmark("181.mcf", "0.81", "1.11");
    benchmarks[numBenchmarks++] = new Benchmark("183.equake", "4.83", "5.91");
    benchmarks[numBenchmarks++] = new Benchmark("186.crafty", "24.17", "30.38");
    benchmarks[numBenchmarks++] = new Benchmark("188.ammp", "37.61", "38.99");
    benchmarks[numBenchmarks++] = new Benchmark("197.parser", "12.20", "15.74");
    benchmarks[numBenchmarks++] = new Benchmark("200.sixtrack", "44.09", "61.78");
    benchmarks[numBenchmarks++] = new Benchmark("253.perlbmk", "1.81", "1.92");
    benchmarks[numBenchmarks++] = new Benchmark("254.gap", "4.52", "5.70");
    benchmarks[numBenchmarks++] = new Benchmark("255.vortex", "0.00", "0.00");
    benchmarks[numBenchmarks++] = new Benchmark("256.bzip2", "33.61", "27.08");
    benchmarks[numBenchmarks++] = new Benchmark("300.twolf", "0.87", "1.13");
    benchmarks[numBenchmarks++] = new Benchmark("301.apsi", "29.81", "42.45");
    benchmarks[numBenchmarks++] = new Benchmark("aatest", "0.00", "0.00");
    benchmarks[numBenchmarks++] = new Benchmark("anagram", "0.05", "0.06");
    benchmarks[numBenchmarks++] = new Benchmark("bc", "0.04", "0.07");
    benchmarks[numBenchmarks++] = new Benchmark("bisort", "2.32", "5.03");
    benchmarks[numBenchmarks++] = new Benchmark("DIS_dm", "0.04", "0.06");
    benchmarks[numBenchmarks++] = new Benchmark("DIS_iu", "0.00", "5.47");
    benchmarks[numBenchmarks++] = new Benchmark("ft", "0.00", "0.00");
    benchmarks[numBenchmarks++] = new Benchmark("health", "35.47", "63.02");
    benchmarks[numBenchmarks++] = new Benchmark("ks", "0.00", "0.00");
    benchmarks[numBenchmarks++] = new Benchmark("mst", "4.99", "9.11");
    benchmarks[numBenchmarks++] = new Benchmark("perimeter", "0.67", "1.11");
    benchmarks[numBenchmarks++] = new Benchmark("power", "4.29", "6.36");
    benchmarks[numBenchmarks++] = new Benchmark("treeadd", "0.86", "1.87");
    benchmarks[numBenchmarks++] = new Benchmark("tsp", "0.32", "0.48");
    benchmarks[numBenchmarks++] = new Benchmark("yacr2", "2.14", "2.36");

    numColumns = numBenchmarks + 2; // Date plus geometric mean.

    int di = -1;

    try {
      FileReader      fr = new FileReader(inputFile);
      BufferedReader  br = new BufferedReader(fr);

      while (true) { // Read each line of each file.
        String line = br.readLine();

        if (line == null)
          break;

        line = line.trim();
        int l = line.length();
        int i = nextToken(line, 0);
        String date = token;
        i = nextToken(line, i);
        String bench = token;
        i = nextToken(line, i);
        String time = token;
        i = nextToken(line, i);
        String spills = token;

        if ((date.length() != 6) ||
            (bench.length() == 0) ||
            (time.length() == 0) ||
            (spills.length() == 0)) {
          //           System.out.print("date: " + date);
          //           System.out.print(" bench: " + bench);
          //           System.out.print(" time: " + time);
          //           System.out.println(" spill: " + spills);
          continue;
        }

        if (beginningDate != null) {
          if (date.compareTo(beginningDate) < 0)
            continue;
        }

        if (endingDate != null) {
          if (date.compareTo(endingDate) > 0)
            continue;
        }

        int sl = bench.indexOf('/');
        if (sl >= 0) {
          if (system == null)
            system = bench.substring(0, sl);
          bench = bench.substring(sl + 1);
        }

        int bi = lookup(bench);
        if (bi < 0)
          continue;

        if (!date.equals(lastDate)) {
          lastDate = date;
          di++;
        }

        int row   = di * numColumns;
        int index = row + bi + 2;
        if (index >= times.length) {
          String[] nt = new String[index * 2];
          System.arraycopy(times, 0, nt, 0, times.length);
          times = nt;
        }

        times[row + 0] = date;
        times[row + bi + 2] = time;
      }

      fr.close();
      br.close();
    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }

    int beg = 0;
    if (lastN > 0) {
      beg = di - lastN + 1;
      if (beg < 0)
        beg = 0;
    }

    // Get the minimum time to use in calculating the ratio.

    double[] mins = new double[numBenchmarks];
    for (int i = 0; i < numBenchmarks; i++)
      mins[i] = benchmarks[i].getMin(system);

    // Calculate the geometric mean of the ratios of the execution time
    // to the minimum execution time on each day and print them out.

    for (int i = beg; i <= di; i++) {
      int    num = 0;
      double sum = 0.0;
      for (int j = 0; j < numBenchmarks; j++) {
        double minTime = mins[j];
        if (minTime <= 0.0)
          continue;

        String t = times[i * numColumns + j + 2];
        if ((t == null) || (t.length() <= 0)) {
          // Find last good time.

          for (int k = i - 1; k >= 0; k--) {
            t = times[k * numColumns + j + 2];
            if (t != null)
              break;
          }
          if ((t == null) || (t.length() <= 0))
            continue;
        }
        double etime = 0.0;
        try {
          etime = Double.parseDouble(t);
        } catch (java.lang.NumberFormatException ex) {
          continue;  // An anomaly - like health exceeding available memory.
        }
        if (etime < minTime * 0.1)
          continue;  // An anomaly - like health exceeding available memory.

        double ratio = etime / minTime;
        if (ratio <= 0.0)
          continue;

        sum += java.lang.Math.log(ratio);
        num++;
      }

      times[i * numColumns + 1] = Double.toString(java.lang.Math.exp(sum / (double) num));
    }

    // Obtain the benchmark names to output.

    // Output the data.

    output("geomean", times, beg, di, 1);

    for (int i = 0; i < numBenchmarks; i++)
      output(benchmarks[i].benchmark, times, beg, di, 2 + i);
  }

  private void output(String name, String[] times, int beg, int end, int column)
  {
    String pathname = directory + '/' + name;
    try {
      FileOutputStream fos = new FileOutputStream(pathname);
      PrintStream      ps  = new PrintStream(fos);

      for (int i = beg; i <= end; i++) {
        ps.print(times[i * numColumns]);
        ps.print(" ");
        ps.println(times[i * numColumns + column]);
      }

      ps.close();
    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }
  }

  private int nextToken(String line, int i)
  {
    int l = line.length();
    while ((i < l) && (line.charAt(i) == ' ')) i++;
    int tokenb = i;
    while ((i < l) && (line.charAt(i) != ' ')) i++;
    int tokene = i;

    token = line.substring(tokenb, tokene);

    return i;
  }

  private int lookup(String bench)
  {
    for (int bi = 0; bi < numBenchmarks; bi++) {
      if (benchmarks[bi].benchmark.equals(bench))
        return bi;
    }
    return -1;
  }

  /**
   * Process the command line parameters.
   * @param args the array of command line parameters
   * @param params an array of allowed command line parameters
   */
  private void parseCmdLine(String[] args, CmdParam[] params)
  {
    try {
      if (CmdParam.parse(this.getClass().getName(), args, params, file))
        System.exit(0);
    } catch(scale.common.InvalidKeyException ex) {
      System.out.println(ex.getMessage());
      CmdParam.usage(System.out, this.getClass().getName(), params);
      System.exit(1);
    }

    Vector<String> v = file.getStringValues();
    if (v.size() != 1) {
      CmdParam.usage(System.out, this.getClass().getName(), params);
      System.exit(1);
    }

    inputFile = v.get(0);

    if (db.specified())
      beginningDate = db.getStringValue();
    if (de.specified())
      endingDate = (String) de.getValue();

    if (sys.specified())
      system = (String) sys.getValue();

    if (last.specified())
      lastN = ((Integer) last.getValue()).intValue();

    directory = (String) dir.getValue();
  }
}
