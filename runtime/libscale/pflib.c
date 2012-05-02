#include <stdio.h>
#include <alloca.h>
#include <string.h>
#include <math.h>
#include <assert.h>

/* A 64-bit integer is represented by long long on SPARC and by long
   on other stuff (well, Alpha at least). */
#ifdef __sparc
  typedef long long int64;
  #define INT64_FORMAT_STRING "%lld"
#else
  typedef long int64;
  #define INT64_FORMAT_STRING "%ld"
#endif

/* The number of slots for loop trip counts for each loop. */
#define LTC_TABLE_SIZE 100

extern void exit(int);

typedef struct {
  char  *fname;     /* Function name. */
  int    hash;      /* CFG characteristic value. */
  int    numBlocks; /* Number of basic blocks. */
  int   *lblks;     /* Line number of each basic block. */
  int   *blkcnts;   /* Execution count for each block. */
  int    numEdges;  /* Number of CFG edges. */
  int   *lsrc;      /* Edge source line number. */
  int   *ldst;      /* Edge destination line number. */
  int   *edgeCnts;  /* Edge execution counts. */
  int    pTblSize;  /* Number of paths. */
  int64 *pathNums;  /* Path number. */
  int64 *pathCnts;  /* Path execution counts. */
  int    numLoops;  /* Number of loops. */
  int   *lloops;    /* Line number of each loop. */
  int64 *ltchTable; /* Array of loop iteration count histograms. */
  int   *licnt;     /* Loop instruction count array. */
  /**
   * The loop instruction count array is made of of a 4-tuple for each loop:
   *  (ic, est. ic, unroll factor, estimated unroll factor)
   */
} FTN_ENTRY;


extern void **__profile_table[];
extern void *__profile_table_names[];

static void sort(int size, int linenos[], int sorted[])
{
  int i;
  int ul;
  int flag;
  int jumpSize = size;

  for (i = 0; i < size; i++)
    sorted[i] = i;

  do {
    flag = 0;
    jumpSize = (10 * jumpSize + 3) / 13;
    ul = size - jumpSize;
    for (i = 0; i < ul; i++) {
      int k  = i + jumpSize;
      int si = sorted[i];
      int sk = sorted[k];
      int swap = 0;

      if (linenos[si] > linenos[sk])
	swap = 1;

      if (swap) {
	sorted[i] = sk;
	sorted[k] = si;
	flag = 1;
      }
    }
  } while (flag || (jumpSize > 1));
}

static void function(FTN_ENTRY *ftab, FILE *pfFile)
{
  int    i, j;
  int    hash          = ftab->hash;
  int    numBlocks     = ftab->numBlocks;
  int   *lblks         = ftab->lblks;
  int   *blkcnts       = ftab->blkcnts;
  int    numEdges      = ftab->numEdges;
  int   *lsrc          = ftab->lsrc;
  int   *ldst          = ftab->ldst;
  int   *edgeCnts      = ftab->edgeCnts;
  int    pTblSize      = ftab->pTblSize;
  int64 *pathNums      = ftab->pathNums;
  int64 *pathCnts      = ftab->pathCnts;
  int    numLoops      = ftab->numLoops;
  int   *lloops        = ftab->lloops;
  int64 *ltchTable     = ftab->ltchTable;
  int   *licnt         = ftab->licnt;
  int   *sorted;

  if ((lblks == 0) && (lsrc == 0) && (pathNums == 0) && (ltchTable == 0))
    return;

  if ((numBlocks <= 0) && (numEdges <= 0) && (pTblSize <= 0) && (numLoops <= 0))
    return;

  fprintf(pfFile, "** Ftn: %s\n hash: %d\n\n", ftab->fname, hash);
  if ((lblks != 0) && (numBlocks > 0)) {
    sorted = (int *) alloca(numBlocks * sizeof(int));
    sort(numBlocks, lblks, sorted);
    fprintf(pfFile, " blocks: %d # line(block):count\n ", numBlocks);
    j = 0;
    for (i = 0; i < numBlocks; i++) {
      int k   = sorted[i];
      int cnt = blkcnts[k];
      if (cnt != 0) {
        fprintf(pfFile, " %d(%d):%d", lblks[k], k, cnt);
        if (++j == 10) {
          fprintf(pfFile, "\n ");
          j = 0;
        }
      }
    }
    if (j > 0)
      fprintf(pfFile, "\n");
    fprintf(pfFile, "\n");
  }

  if ((lsrc != 0) && (numEdges > 0)) {
    sorted = (int *) alloca(numEdges * sizeof(int));
    sort(numEdges, lsrc, sorted);
    fprintf(pfFile, " edges: %d # line->line(edge):count\n ", numEdges);
    j = 0;
    for (i = 0; i < numEdges; i++) {
      int k   = sorted[i];
      int cnt = edgeCnts[k];
      if (cnt != 0) {
        fprintf(pfFile, " %d->%d(%d):%d", lsrc[k], ldst[k], k, cnt);
        if (++j == 5) {
          fprintf(pfFile, "\n ");
          j = 0;
        }
      }
    }
    if (j > 0)
      fprintf(pfFile, "\n");
    fprintf(pfFile, "\n");
  }

  if ((pathCnts != 0) && (pTblSize > 0)) {
    int k = 0;
    for (i = 0; i < pTblSize; i++) {
      int64 cnt = pathCnts[i];
      if (cnt != 0)
        k++;
    }
    fprintf(pfFile, " paths: %d\n ", k);
    j = 0;
    for (i = 0; i < pTblSize; i++) {
      int64 pathNum = pathNums[i];
      int64 cnt     = pathCnts[i];
      if (cnt != 0) {
        fprintf(pfFile, " "INT64_FORMAT_STRING":"INT64_FORMAT_STRING, pathNum, cnt);
        if (++j == 10) {
          fprintf(pfFile, "\n ");
          j = 0;
        }
      }
    }
    if (j > 0)
      fprintf(pfFile, "\n");
    fprintf(pfFile, "\n");
  }

  if (numLoops > 0) {
    fprintf(pfFile, " loops: %d # loop:(line, ic, est. ic, unroll factor, est. unroll factor) histogram\n", numLoops);
    for (i = 0; i < numLoops; i++) {
      fprintf(pfFile, "   %d:", i);
      if (lloops != 0) {
        fprintf(pfFile, "(%d", lloops[i]);
        if (licnt != 0) {
          int k = i * 4;
          int j;
          for (j = k; j < k + 4; j++)
            fprintf(pfFile, ",%d", licnt[j]);
        }
        fprintf(pfFile, ")");
      }
      if (ltchTable != 0) {
        for (j = 0; j < LTC_TABLE_SIZE; j++) {
          int64 freq = ltchTable[i * LTC_TABLE_SIZE + j];
          if (freq > 0) {
            if (j > 64) {
              int64 k  = 1 + (1 << (j - 59));
              int64 k2 = (1 << (j - 58));
              fprintf(pfFile, " "INT64_FORMAT_STRING"-"INT64_FORMAT_STRING":"INT64_FORMAT_STRING, k, k2, freq);
            } else 
              fprintf(pfFile, " "INT64_FORMAT_STRING":"INT64_FORMAT_STRING, j, freq);
          }
        }
      }
      fprintf(pfFile, "\n");
    }
    fprintf(pfFile, "\n");
  }

  fprintf(pfFile, "\n");
}

static char *genPfFileName(char *moduleName)
{
  static char buf[512];
  strcpy(buf, moduleName);
  strcat(buf, ".pft");
  return &buf[0];
}

static void module(void **ftab, char *moduleName)
{
  int   i        = 0;
  char *fileName = genPfFileName(moduleName);
  FILE *pfFile   = fopen(fileName, "a");
  if (pfFile == 0) {
    fprintf(stderr, "Unable to open %s.\n", fileName);
    exit(1);
  }

  while (1) {
    void *ptr = ftab[i];
    if (ptr == 0)
      break;
    function((FTN_ENTRY *) ptr, pfFile);
    i++;
  }

  fclose(pfFile);
}

void __pf_profile_dump()
{
  void *ptr;
  int i = 0;
  while (1) {
    ptr = __profile_table[i];
    if (ptr == 0)
      break;
    module(ptr, (char *) __profile_table_names[i]);
    i++;
  }
}

/* See CLR for reasoning behind hashing algorithm. */
#define NUM_ADDITIONAL_HASH_ATTEMPTS 3
void __pf_hash_path(void *ftab, int64 pathNum)
{
  int primaryHash  = ((FTN_ENTRY*)ftab)->pTblSize - 1;
  int secondaryHash;
  int64 *pathNums = ((FTN_ENTRY*)ftab)->pathNums;
  int64 *pathCnts = ((FTN_ENTRY*)ftab)->pathCnts;
  int hashValue = pathNum % primaryHash;
  int i; /* Loop variable. */

  if (pathNums[hashValue] == pathNum) {
    /* This hash slot has been used already by the same path, so increment the count. */
    pathCnts[hashValue]++;
    return;
  }
  if (pathNums[hashValue] == -1) {
    /* This hash slot has not yet been used, so initialize it and increment the count. */
    pathNums[hashValue] = pathNum;
    pathCnts[hashValue]++;
    return;
  }

  /* Try other slots. */
  secondaryHash = primaryHash - 2;
  for (i = 1; i <= NUM_ADDITIONAL_HASH_ATTEMPTS; i++) {
    hashValue = (hashValue + (pathNum % secondaryHash) + 1) % primaryHash;
    if (pathNums[hashValue] == pathNum) {
      /* this hash slot has already been used by the same path */
      pathCnts[hashValue]++;
      return;
    }
    if (pathNums[hashValue] == -1) {
      /* this hash slot has not yet been used */
      pathNums[hashValue] = pathNum;
      pathCnts[hashValue]++;
      return;
    }
  }

  /* everything failed, so put it in the "lost" slot */
  pathCnts[primaryHash]++;
}

/* Take the floor of the log base 2 of n. */
int ilog2(int64 n) {
  int totalAmt;
  int shiftAmt;
  for (totalAmt = 0, shiftAmt = 32; shiftAmt; shiftAmt >>= 1) {
    int temp = n >> shiftAmt;
    if (temp) {
      totalAmt += shiftAmt;
      n = temp;
    }
  }
  return totalAmt;
}

/* Record a trip count for a particular loop. */
void __pf_record_ltc(void *ftab, int loopNum, int64 tripCount) {
  int slot;
  if ((ftab == 0) || (((FTN_ENTRY*)ftab)->ltchTable == 0))
    return;

  /* Trip counts in [1,64] use slots [1,64]. */
  if (tripCount <= 64) {
    slot = tripCount;
    /* Trip counts in [65,2^41] use slots [65,99]. */
  } else {
    slot = ilog2(tripCount - 1) + 59;
  }
  assert((loopNum > 0) && (slot >= 0) && (slot < LTC_TABLE_SIZE));
  ((FTN_ENTRY*)ftab)->ltchTable[(loopNum - 1) * LTC_TABLE_SIZE + slot]++;
}
