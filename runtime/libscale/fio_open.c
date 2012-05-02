#include "f2c.h"

/* PURE fio_open PUREGV */
int fio_open(int oerr, int ounit, char *ofnm, int ofnmlen, char *osta, char *oacc, char *ofm, int orl, char *oblnk)
{
  static olist params;

  params.oerr    = oerr;
  params.ounit   = ounit;
  params.ofnm    = ofnm;
  params.ofnmlen = ofnmlen;
  params.osta    = osta;
  params.oacc    = oacc;
  params.ofm     = ofm;
  params.orl     = orl;
  params.oblnk   = oblnk;

  return f_open(&params);
}
