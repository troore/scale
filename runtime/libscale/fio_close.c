#include "f2c.h"

/* PURE fio_close PUREGV */
int fio_close(int cerr, int cunit, char *csta)
{	 
  static cllist params;

  params.cerr = cerr;
  params.cunit = cunit;
  params.csta = csta;

  return f_clos(&params);
}
