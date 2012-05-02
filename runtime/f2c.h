/* f2c.h  --  Standard Fortran to C header file */
#ifndef F2C_INCLUDE
#define F2C_INCLUDE

/**
 *
 * This note is intended to document some of the interface to 
 * F2C's read/write I/O routines.
 *
 * The names of the f2c read/write routines follow this format:
 *
 *     a_bcde
 *
 * where
 *
 *     a: s=start, e=end 
 *     b: r=read, w=write 
 *     c: s=sequential, d=direct 
 *     d: u=unformatted,
 *        f=formatted,
 *        l=list directed,
 *        n=name list directed 
 *     e: i=internal, e=external 
 *
 * The name of the list directed I/O is unknown.
 *
 * We use the start and end versions of the following I/O routines:
 *
 *     For Reads:
 *
 *              rdfe 
 *              rdue 
 *              rsfe 
 *              rsfi 
 *              rsue 
 *              rsle 
 *              rsli 
 *
 *     For Writes:
 *              wdfe 
 *              wdue 
 *              wsfe 
 *              wsfi 
 *              wsue 
 *              wsle 
 *              wsli 
 *
 * The read/write routines process any IO data directly. 
 * Rather the data is passed by a separate call to data 
 * routines, which have the following format:
 *
 *     do_?io
 *
 * where the "?" is one of the following:
 *
 *     u: unformatted parameter 
 *     f: formatted parameter 
 *     l: list directed parameter 
 *
 * We use all of the parameter routines.
 */

typedef int integer;
typedef unsigned int uinteger;
typedef char *address;
typedef short int shortint;
typedef float real;
typedef double doublereal;
typedef struct { real r, i; } complex;
typedef struct { doublereal r, i; } doublecomplex;
typedef int logical;
typedef short int shortlogical;
typedef char logical1;
typedef char integer1;
typedef long long longint;		/* system-dependent */
typedef unsigned long long ulongint;	/* system-dependent */
#define qbit_clear(a,b)	((a) & ~((ulongint)1 << (b)))
#define qbit_set(a,b)	((a) |  ((ulongint)1 << (b)))

#define TRUE_ (1)
#define FALSE_ (0)
#define WANT_LEAD_0

/* Extern is for use with -E */
#ifndef Extern
#define Extern extern
#endif

/* I/O stuff */

typedef integer flag;
typedef integer ftnlen;
typedef integer ftnint;

/*external read, write*/
typedef struct {
  flag cierr;
  ftnint ciunit;
  flag ciend;
  const char *cifmt;
  ftnint cirec;
} cilist;

/*internal read, write*/
typedef struct {
  flag icierr;
  char *iciunit;
  flag iciend;
  const char *icifmt;
  ftnint icirlen;
  ftnint icirnum;
} icilist;

/*open*/
typedef struct {      
  flag oerr;
  ftnint ounit;
  char *ofnm;
  ftnlen ofnmlen;
  char *osta;
  char *oacc;
  char *ofm;
  ftnint orl;
  char *oblnk;
} olist;

/*close*/
typedef struct {
  flag cerr;
  ftnint cunit;
  char *csta;
} cllist;

/*rewind, backspace, endfile*/
typedef struct {
  flag aerr;
  ftnint aunit;
} alist;

/* inquire */
typedef struct {
  flag   inerr;
  ftnint inunit;
  char   *infile;
  ftnlen infilen;
  ftnint *inex;	/*parameters in standard's order*/
  ftnint *inopen;
  ftnint *innum;
  ftnint *innamed;
  char	 *inname;
  ftnlen innamlen;
  char	 *inacc;
  ftnlen inacclen;
  char	 *inseq;
  ftnlen inseqlen;
  char   *indir;
  ftnlen indirlen;
  char	 *infmt;
  ftnlen infmtlen;
  char	 *inform;
  ftnint informlen;
  char	 *inunf;
  ftnlen inunflen;
  ftnint *inrecl;
  ftnint *innrec;
  char	 *inblank;
  ftnlen inblanklen;
} inlist;

union Multitype {	/* for multiple entry points */
  integer1 g;
  shortint h;
  integer i;
  /* longint j; */
  real r;
  doublereal d;
  complex c;
  doublecomplex z;
};

typedef union Multitype Multitype;

struct Vardesc {	/* for Namelist */
  char *name;
  char *addr;
  ftnlen *dims;
  int  type;
};

typedef struct Vardesc Vardesc;

struct Namelist {
  char *name;
  Vardesc **vars;
  int nvars;
};
typedef struct Namelist Namelist;

#define abs(x) ((x) >= 0 ? (x) : -(x))
#define dabs(x) (doublereal)abs(x)
#define min(a,b) ((a) <= (b) ? (a) : (b))
#define max(a,b) ((a) >= (b) ? (a) : (b))
#define dmin(a,b) (doublereal)min(a,b)
#define dmax(a,b) (doublereal)max(a,b)
#define bit_test(a,b)	((a) >> (b) & 1)
#define bit_clear(a,b)	((a) & ~((uinteger)1 << (b)))
#define bit_set(a,b)	((a) |  ((uinteger)1 << (b)))

/* procedure parameter types for -A and -C++ */

#define F2C_proc_par_types 1
#ifdef __cplusplus
typedef int /* Unknown procedure type */ (*U_fp)(...);
typedef shortint (*J_fp)(...);
typedef integer (*I_fp)(...);
typedef real (*R_fp)(...);
typedef doublereal (*D_fp)(...), (*E_fp)(...);
typedef /* Complex */ void (*C_fp)(...);
typedef /* Double Complex */ void (*Z_fp)(...);
typedef logical (*L_fp)(...);
typedef shortlogical (*K_fp)(...);
typedef /* Character */ void (*H_fp)(...);
typedef /* Subroutine */ int (*S_fp)(...);
#else
typedef int /* Unknown procedure type */ (*U_fp)();
typedef shortint (*J_fp)();
typedef integer (*I_fp)();
typedef real (*R_fp)();
typedef doublereal (*D_fp)(), (*E_fp)();
typedef /* Complex */ void (*C_fp)();
typedef /* Double Complex */ void (*Z_fp)();
typedef logical (*L_fp)();
typedef shortlogical (*K_fp)();
typedef /* Character */ void (*H_fp)();
typedef /* Subroutine */ int (*S_fp)();
#endif
/* E_fp is for real functions when -R is not specified */
typedef void C_f;	/* complex function */
typedef void H_f;	/* character function */
typedef void Z_f;	/* double complex function */
typedef doublereal E_f;	/* real function with -R not specified */

extern int f_open(olist *);
extern int f_clos(cllist *);
extern int f_inqu(inlist *);
extern int f_back(alist *);
extern int f_end(alist *);
extern int f_rew(alist *);
extern int do_uio(ftnint *number, char *ptr, ftnlen len);
extern int do_fio(ftnint *number, char *ptr, ftnlen len);
extern int do_lio(ftnint *type, ftnint *number, char *ptr, ftnlen len);
extern int s_rdfe(cilist *params);
extern int e_rdfe(void);
extern int s_rdue(cilist *params);
extern int e_rdue(void);
extern int s_rsfe(cilist *params);
extern int e_rsfe(void);
extern int s_rsfi(icilist *params);
extern int e_rsfi(void);
extern int s_rsue(cilist *params);
extern int e_rsue(void);
extern int s_rsle(cilist *params);
extern int e_rsle(void);
extern int s_rsli(icilist *params);
extern int e_rsli(void);
extern int s_wdfe(cilist *params);
extern int e_wdfe(void);
extern int s_wdue(cilist *params);
extern int e_wdue(void);
extern int s_wsfe(cilist *params);
extern int e_wsfe(void);
extern int s_wsfi(icilist *params);
extern int e_wsfi(void);
extern int s_wsue(cilist *params);
extern int e_wsue(void);
extern int s_wsle(cilist *params);
extern int e_wsle(void);
extern int s_wsli(icilist *params);
extern int e_wsli(void);
extern int s_wsne(cilist *a);
extern void f_exit(void);
extern void c_sqrt(complex *r, complex *z);
extern void z_sqrt(doublecomplex *r, doublecomplex *z);
extern double f__cabs(double, double);
#endif
