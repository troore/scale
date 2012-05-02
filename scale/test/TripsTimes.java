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
 * This class calculates the geometric mean of the Trips benchmark data.
 * <p>
 * $Id: TripsTimes.java,v 1.3 2007-10-04 19:58:40 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This program processes a data file and outputs another data file.
 * It acts as a filter and a combiner.
 * </pre>
 */
public class TripsTimes
{
  private static class Benchmark
  {
    public String benchmark;
    public long   minInsts;
    public long   minBlocks;
    public long   minCycles;
    public long   maxInsts;
    public long   maxBlocks;
    public long   maxCycles;

    public Benchmark(String benchmark)
    {
      this.benchmark = benchmark;
      this.minInsts  = Long.MAX_VALUE;
      this.minBlocks = Long.MAX_VALUE;
      this.minCycles = Long.MAX_VALUE;
    }
  }

  private static final String helpLast   = "only the last n";
  private static final String helpDb     = "beginning date: -db yymmdd";
  private static final String helpDe     = "ending date: -de yymmdd";
  private static final String helpDir    = "directory to receive the generated files";
  private static final String helpFile   = "the input data file";
  private static final String helpMicro  = "input is for micro bechmarks";

  private CmdParam   last   = new CmdParam("last",   true,  CmdParam.INT,    null, helpLast);
  private CmdParam   db     = new CmdParam("db",     true,  CmdParam.STRING, null, helpDb);
  private CmdParam   de     = new CmdParam("de",     true,  CmdParam.STRING, null, helpDe);
  private CmdParam   dir    = new CmdParam("dir",    true,  CmdParam.STRING, ".",  helpDir);
  private CmdParam   file   = new CmdParam("i",      false, CmdParam.STRING, null, helpFile);
  private CmdParam   micro  = new CmdParam("micro",  true,  CmdParam.SWITCH, null, helpMicro);
  private CmdParam[] params = {dir, db, de, last, micro};

  private Benchmark[] benchmarks; // The set of recognized benchamrks.
  private String[]    dates;
  private long[]      insts;
  private long[]      blocks;
  private long[]      cycles;

  private static final String[] valid = {
    "2005-08-02", "2005-08-30", "2005-09-30", "2005-10-31",
    "2005-11-26", "2005-12-20", "2006-01-30", "2006-02-27",
    "2006-03-27", "2006-04-24", "2006-05-29", "2006-06-26",
    "2006-07-31", "2006-08-28", "2006-09-27", "2006-10-30",
    "2006-11-30", "2006-12-30", "2007-01-27", "2007-02-12",
    "2007-03-10", "2007-04-10", "2007-05-19", "2007-06-23", "2007-07-07"
  };

  private static final String[] months = {
   "Jan", "Feb", "Mar", "Apr", "May", "Jun",
   "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
  };

  private static final String[] monthi = {
    "01", "02", "03", "04", "05", "06",
    "07", "08", "09", "10", "11", "12"
  };

  private int numBenchmarks; // Number of benchmarks.
  private int numColumns;    // Number of columns of data.
  private int lastN;         // Number of data rows to save.

  private String inputFile;     // Input data file name.
  private String token;         // Result from nextToken().
  private String beginningDate; // Starting data to save.
  private String endingDate;    // Ending data to save.
  private String directory;     // Result files directory.

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) 
  {
    TripsTimes me = new TripsTimes();

    Msg.setup(null);
    me.parseCmdLine(args, me.params); // Parse the command line arguments
    me.process();
  }

  /**
   * Do the processing.
   */
  private void process()
  {
    int di = micro.specified() ? processMicroData() : processSpecData();

    int beg = 0;
    if (lastN > 0) {
      beg = di - lastN + 1;
      if (beg < 0)
        beg = 0;
    }

    // Calculate the geometric mean of the ratios of the execution time
    // to the minimum execution time on each day and print them out.

    calcGeoMean(beg, di, insts, 0);
    calcGeoMean(beg, di, blocks, 1);
    calcGeoMean(beg, di, cycles, 2);

    // Obtain the benchmark names to output.

    // Output the data.

    outputGeomean("geomean-inst", insts, beg, di);
    outputGeomean("geomean-block", blocks, beg, di);
    outputGeomean("geomean-cycle", cycles, beg, di);

    outputData("blocks", blocks, beg, di);
    outputData("insts", insts, beg, di);
    outputData("cycles", cycles, beg, di);

    for (int i = 0; i < numBenchmarks; i++)
      outputBenchmark(i, beg, di);
  }

  private int processSpecData()
  {
    String   lastDate = "";

    numBenchmarks = 0;
    benchmarks = new Benchmark[60];
    benchmarks[numBenchmarks++] = new Benchmark("164.gzip");
    benchmarks[numBenchmarks++] = new Benchmark("168.wupwise");
    benchmarks[numBenchmarks++] = new Benchmark("171.swim");
    benchmarks[numBenchmarks++] = new Benchmark("172.mgrid");
    benchmarks[numBenchmarks++] = new Benchmark("173.applu");
    benchmarks[numBenchmarks++] = new Benchmark("175.vpr");
    benchmarks[numBenchmarks++] = new Benchmark("176.gcc");
    benchmarks[numBenchmarks++] = new Benchmark("177.mesa");
    benchmarks[numBenchmarks++] = new Benchmark("179.art");
    benchmarks[numBenchmarks++] = new Benchmark("181.mcf");
    benchmarks[numBenchmarks++] = new Benchmark("183.equake");
    benchmarks[numBenchmarks++] = new Benchmark("186.crafty");
    benchmarks[numBenchmarks++] = new Benchmark("188.ammp");
    benchmarks[numBenchmarks++] = new Benchmark("197.parser");
    benchmarks[numBenchmarks++] = new Benchmark("200.sixtrack");
    benchmarks[numBenchmarks++] = new Benchmark("253.perlbmk");
    benchmarks[numBenchmarks++] = new Benchmark("254.gap");
    benchmarks[numBenchmarks++] = new Benchmark("255.vortex");
    benchmarks[numBenchmarks++] = new Benchmark("256.bzip2");
    benchmarks[numBenchmarks++] = new Benchmark("300.twolf");
    benchmarks[numBenchmarks++] = new Benchmark("301.apsi");

    numColumns = numBenchmarks + 1; // Date plus geometric mean.
    dates      = new String[numColumns * 256];
    insts      = new long[numColumns * 256];
    blocks     = new long[numColumns * 256];
    cycles     = new long[numColumns * 256];


    int di = -1;

    try {
      FileReader      fr = new FileReader(inputFile);
      BufferedReader  br = new BufferedReader(fr);

      while (true) { // Read each line of each file.
        String line = br.readLine();

        if (line == null)
          break;

        line = line.trim();
        int i = nextToken(line, 0);
        String date = token;
        i = nextToken(line, i);
        String bench = token;
        i = nextToken(line, i); // Skip flag
        if (!"P".equals(token))
          continue;

        i = nextToken(line, i); // Skip
        i = nextToken(line, i);
        long instcnt = Long.parseLong(token);
        i = nextToken(line, i);
        long blockcnt = Long.parseLong(token);
        i = nextToken(line, i); // Skip
        i = nextToken(line, i); // Skip
        i = nextToken(line, i);
        long cyclecnt = Long.parseLong(token);

        if ((date.length() != 10) ||
            (bench.length() == 0) ||
            (instcnt == 0) ||
            (blockcnt == 0)) {
          //           System.out.print("date: " + date);
          //           System.out.print(" bench: " + bench);
          //           System.out.print(" insts: " + instcnt);
          //           System.out.println(" blocks: " + blockcnt);
          continue;
        }

        if (beginningDate != null) {
          if (date.compareTo(beginningDate) < 0)
            continue;
        }

        if (!validDate(date))
          continue;

        if (endingDate != null) {
          if (date.compareTo(endingDate) > 0)
            continue;
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
        if (di >= dates.length) {
          String[] nd = new String[di * 2];
          System.arraycopy(dates, 0, nd, 0, dates.length);
          dates = nd;
          long[] nt = new long[index * 2];
          System.arraycopy(insts, 0, nt, 0, dates.length);
          insts = nt;
          long[] nb = new long[index * 2];
          System.arraycopy(blocks, 0, nb, 0, dates.length);
          blocks = nb;
          long[] nc = new long[index * 2];
          System.arraycopy(cycles, 0, nc, 0, dates.length);
          cycles = nc;
        }

        dates[di] = date;
        insts[row + bi + 1] = instcnt;
        blocks[row + bi + 1] = blockcnt;
        cycles[row + bi + 1] = cyclecnt;

        if ((instcnt > 0) && (instcnt < benchmarks[bi].minInsts))
          benchmarks[bi].minInsts = instcnt;

        if ((blockcnt > 0) && (blockcnt < benchmarks[bi].minBlocks))
          benchmarks[bi].minBlocks = blockcnt;

        if ((cyclecnt > 0) && (cyclecnt < benchmarks[bi].minCycles))
          benchmarks[bi].minCycles = cyclecnt;

        if ((instcnt > 0) && (instcnt > benchmarks[bi].maxInsts))
          benchmarks[bi].maxInsts = instcnt;

        if ((blockcnt > 0) && (blockcnt > benchmarks[bi].maxBlocks))
          benchmarks[bi].maxBlocks = blockcnt;

        if ((cyclecnt > 0) && (cyclecnt > benchmarks[bi].maxCycles))
          benchmarks[bi].maxCycles = cyclecnt;
      }

      fr.close();
      br.close();

    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }

    return di;
  }

  private int processMicroData()
  {
    String   lastDate = "";

    numBenchmarks = 0;
    benchmarks = new Benchmark[60];

    int di = -1;

    try {
      String blockname = inputFile + "_blocks.txt";
      String insnname  = inputFile + "_insns.txt";
      String cyclename = inputFile + "_cycles.txt";
      FileReader      frb = new FileReader(blockname);
      BufferedReader  brb = new BufferedReader(frb);
      FileReader      frc = new FileReader(cyclename);
      BufferedReader  brc = new BufferedReader(frc);
      FileReader      fri = new FileReader(insnname);
      BufferedReader  bri = new BufferedReader(fri);
      String lineb = brb.readLine();
      String linec = brc.readLine();
      String linei = bri.readLine();

      int jb = nextToken(lineb, 0);
      int jc = nextToken(linec, 0);
      int ji = nextToken(linei, 0);
      while (true) {
        jb = nextToken(lineb, jb);
        if (jb < 0)
          break;

        String benchb = token;
        jc = nextToken(lineb, jc);
        String benchc = token;
        ji = nextToken(lineb, ji);
        String benchi = token;

        if (!benchb.equals(benchc) || !benchb.equals(benchi)) {
          System.err.println("Micro-benchmark files don't match.");
          System.exit(1);
        }

        benchmarks[numBenchmarks++] = new Benchmark(benchb);
      }

      numColumns = numBenchmarks + 1; // Date plus geometric mean.
      dates      = new String[numColumns * 256];
      insts      = new long[numColumns * 256];
      blocks     = new long[numColumns * 256];
      cycles     = new long[numColumns * 256];

      while (true) { // Read each line of each file.
        lineb = brb.readLine();
        linec = brc.readLine();
        linei = bri.readLine();

        if ((lineb == null) || (linec == null) || (linei == null))
          break;

        lineb = lineb.trim();
        linec = linec.trim();
        linei = linei.trim();

        int    ib    = nextToken(lineb, 0);
        String dateb = token;
        int    ic    = nextToken(linec, 0);
        String datec = token;
        int    ii    = nextToken(linei, 0);
        String datei = token;

        if (!dateb.equals(datec) || !dateb.equals(datei)) {
          System.err.print("Micro-benchmark files don't match.");
          System.out.print(dateb);
          System.out.print(" ");
          System.out.print(datec);
          System.out.print(" ");
          System.out.println(datei);
          System.exit(1);
        }

        if (beginningDate != null) {
          if (dateb.compareTo(beginningDate) < 0)
            continue;
        }

        if (endingDate != null) {
          if (dateb.compareTo(endingDate) > 0)
            continue;
        }

        di++;
        int row   = di * numColumns;
        int index = row + 2;
        if (di >= dates.length) {
          String[] nd = new String[di * 2];
          System.arraycopy(dates, 0, nd, 0, dates.length);
          dates = nd;
          long[] nt = new long[index * 2];
          System.arraycopy(insts, 0, nt, 0, dates.length);
          insts = nt;
          long[] nb = new long[index * 2];
          System.arraycopy(blocks, 0, nb, 0, dates.length);
          blocks = nb;
          long[] nc = new long[index * 2];
          System.arraycopy(cycles, 0, nc, 0, dates.length);
          cycles = nc;
        }

        if (dateb.length() < 11)
          dateb = dateb.substring(0, 4) + "0" + dateb.substring(4);
        String month = dateb.substring(0,3);
        for (int mi = 0; mi < months.length; mi++)
          if (months[mi].equals(month)) {
            dateb = dateb.substring(7, 11) + "-" + monthi[mi] + "-" + dateb.substring(4,6);
            break;
          }

        dates[di] = dateb;
        for (int bi = 0; bi < numBenchmarks; bi++) {
          ib = nextToken(lineb, ib);
          long blockcnt = Long.parseLong(token);
          blocks[row + bi + 1] = blockcnt;

          if ((blockcnt > 0) && (blockcnt < benchmarks[bi].minBlocks))
            benchmarks[bi].minBlocks = blockcnt;

          if ((blockcnt > 0) && (blockcnt > benchmarks[bi].maxBlocks))
            benchmarks[bi].maxBlocks = blockcnt;

          ic = nextToken(linec, ic);
          long cyclecnt = Long.parseLong(token);
          cycles[row + bi + 1] = cyclecnt;

          if ((cyclecnt > 0) && (cyclecnt < benchmarks[bi].minCycles))
            benchmarks[bi].minCycles = cyclecnt;

          if ((cyclecnt > 0) && (cyclecnt > benchmarks[bi].maxCycles))
            benchmarks[bi].maxCycles = cyclecnt;

          ii = nextToken(linei, ii);
          long instcnt = Long.parseLong(token);
          insts[row + bi + 1] = instcnt;

          if ((instcnt > 0) && (instcnt < benchmarks[bi].minInsts))
            benchmarks[bi].minInsts = instcnt;

          if ((instcnt > 0) && (instcnt > benchmarks[bi].maxInsts))
            benchmarks[bi].maxInsts = instcnt;
        }
      }

      frb.close();
      brb.close();

      frc.close();
      brc.close();

      fri.close();
      bri.close();
    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }

    return di;
  }

  private void calcGeoMean(int beg, int di, long[] insts, int which)
  {
    for (int i = beg; i <= di; i++) {
      int    num = 0;
      double sum = 0.0;
      for (int j = 0; j < numBenchmarks; j++) {
        double min = 0.0;
        double max = 0.0;
        switch (which) {
        case 0: min = benchmarks[j].minInsts;  max = benchmarks[j].maxInsts;  break;
        case 1: min = benchmarks[j].minBlocks; max = benchmarks[j].maxBlocks; break;
        case 2: min = benchmarks[j].minCycles; max = benchmarks[j].maxCycles; break;
        }
        if ((min <= 0.0) || (min == Long.MAX_VALUE))
          continue;

        double t = insts[i * numColumns + j + 1];

        if ((t == 0.0) || (t < min * 0.1))
          continue;  // An anomaly - like health exceeding available memory.

        double x = t;
        double ratio = x / max;
        if (ratio <= 0.01)
          continue;

        sum += java.lang.Math.log(ratio);
        num++;
      }

      double mean = 1.0;
      if (num > 0)
        mean = java.lang.Math.exp(sum / (double) num);
      insts[i * numColumns] = (long)(0.5 + 100.0 * mean);
    }
  }

  private void outputData(String name, long[] data, int beg, int di)
  {
    String        ms       = (micro.specified() ? "micro-" : "spec-");
    String        pathname = directory + '/' + ms + name + ".txt";
    DecimalFormat df       = new DecimalFormat("0.00");
    try {
      FileOutputStream fos = new FileOutputStream(pathname);
      PrintStream      ps  = new PrintStream(fos);

      ps.print("date, geomean");
      for (int j = 0; j < numBenchmarks; j++) {
        ps.print(", ");
        ps.print(benchmarks[j].benchmark);
      }
      ps.println("");

      for (int i = beg; i <= di; i++) {
        ps.print(formatDate(dates[i]));
        for (int j = 0; j < numColumns; j++) {
          long x = data[i * numColumns + j];
          ps.print(", ");

          if (j == 0)
            ps.print(df.format(x / 100.0));
          else 
            ps.print(x);
        }
        ps.println("");
      }
      ps.close();
    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }
  }

  private void outputBenchmark(int bn, int beg, int di)
  {
    String pathname  = directory + '/' + benchmarks[bn].benchmark + ".txt";
    double maxInsts  = benchmarks[bn].maxInsts;
    double maxBlocks = benchmarks[bn].maxBlocks;
    double maxCycles = benchmarks[bn].maxCycles;
    DecimalFormat df = new DecimalFormat("0.00");
    try {
      FileOutputStream fos = new FileOutputStream(pathname);
      PrintStream      ps  = new PrintStream(fos);
      ps.println("date, insts, blocks, cycles");
      for (int i = beg; i <= di; i++) {
        ps.print(formatDate(dates[i]));
        ps.print(", ");        
        ps.print(df.format(insts[i * numColumns + bn + 1] / maxInsts));
        ps.print(", ");
        ps.print(df.format(blocks[i * numColumns + bn + 1] / maxBlocks));
        ps.print(", ");
        ps.print(df.format(cycles[i * numColumns + bn + 1] / maxCycles));
        ps.println("");
      }

      ps.close();
    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }
  }

  private void outputGeomean(String name, long[] values, int beg, int end)
  {
    String        ms       = (micro.specified() ? "micro-" : "spec-");
    String        pathname = directory + '/' + ms + name + ".txt";
    DecimalFormat df       = new DecimalFormat("0.00");
    try {
      FileOutputStream fos = new FileOutputStream(pathname);
      PrintStream      ps  = new PrintStream(fos);

      ps.println("date, geomean");
      for (int i = beg; i <= end; i++) {
        ps.print(formatDate(dates[i]));
        ps.print(", ");
        ps.println(df.format(values[i * numColumns] / 100.0));
      }

      ps.close();
    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }
  }

  private String formatDate(String date)
  {
    return date.substring(5, 7) + "/" + date.substring(8, 10) + "/" + date.substring(2, 4);
  }

  private int nextToken(String line, int i)
  {
    int l = line.length();
    if (i >= l)
      return -1;

    if (line.charAt(i) == ',')
        i++;

    while ((i < l) && (line.charAt(i) == ' ')) i++;
    int tokenb = i;
    while ((i < l) && (line.charAt(i) != ' ') && (line.charAt(i) != ',')) i++;
    int tokene = i;

    if (tokenb == tokene)
      token = "0";
    else
      token = line.substring(tokenb, tokene);

    return i;
  }

  private int lookup(String bench)
  {
    for (int bi = 0; bi < numBenchmarks; bi++) {
      if (benchmarks[bi].benchmark.equals(bench))
        return bi;
      if (benchmarks[bi].benchmark.endsWith(bench))
        return bi;
    }
    return -1;
  }

  private boolean validDate(String date)
  {
    for (int i = 0; i < valid.length; i++)
      if (date.equals(valid[i]))
        return true;
    return false;
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

    if (last.specified())
      lastN = ((Integer) last.getValue()).intValue();

    directory = (String) dir.getValue();
  }
}
