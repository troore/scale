#include "f2c.h"

/* PURE fio_backspace PUREGVA */
int fio_backspace( int aerr, int aunit )
{
  static alist params;

  params.aerr      = aerr;
  params.aunit     = aunit;

  return f_back( &params );
}
