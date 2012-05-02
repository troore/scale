#include "f2c.h"

/* PURE fio_inquire PUREGV */
int fio_inquire(int inerr,	    int     inunit,
	    char   *infile,	    int     infilen,
	    ftnint *inex,	    ftnint *inopen,
	    ftnint *innum,	    ftnint *innamed,
	    char   *inname,	    int     innamlen,
	    char   *inacc,	    int	    inacclen,
	    char   *inseq,	    int	    inseqlen,
	    char   *indir,	    int	    indirlen,
	    char   *infmt,	    int	    infmtlen,
	    char   *inform,	    int	    informlen,
	    char   *inunf,	    int	    inunflen,
	    ftnint *inrecl,	    ftnint *innrec,
	    char   *inblank,	    int     inblanklen)
{
  static inlist params;

  params.inerr      = inerr;
  params.inunit     = inunit;
  params.infile     = infile;
  params.infilen    = infilen;
  params.inex       = inex;
  params.inopen     = inopen;
  params.innum      = innum;
  params.innamed    = innamed;
  params.inname     = inname;
  params.innamlen   = innamlen;
  params.inacc      = inacc;
  params.inacclen   = inacclen;
  params.inseq      = inseq;
  params.inseqlen   = inseqlen;
  params.indir      = indir;
  params.indirlen   = indirlen;
  params.infmt      = infmt;
  params.infmtlen   = infmtlen;
  params.inform     = inform;
  params.informlen  = informlen;
  params.inunf      = inunf;
  params.inunflen   = inunflen;
  params.inrecl     = inrecl;
  params.innrec     = innrec;
  params.inblank    = inblank;
  params.inblanklen = inblanklen;
  
  return f_inqu( &params );
}
