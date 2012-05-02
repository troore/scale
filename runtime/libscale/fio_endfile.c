#include "f2c.h"

/* PURE fio_endfile PUREGVA */
int fio_endfile( int aerr, int aunit )
{
  static alist params;

  params.aerr      = aerr;
  params.aunit     = aunit;

  return f_end( &params );
}
