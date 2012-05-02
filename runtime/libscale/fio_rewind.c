#include "f2c.h"

/* PURE fio_rewind PUREGVA */
int fio_rewind( int aerr, int aunit )
{
  static alist params;

  params.aerr      = aerr;
  params.aunit     = aunit;

  return f_rew( &params );
}
