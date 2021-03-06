#include "f2c.h"

/**
 * fio_swdfe
 *   start - write - direct - formatted - external
 */

/* PURE fio_swdfe PUREGVA */
int fio_swdfe( int cierr, int ciunit, int ciend, const char* cifmt, int cirec )
{
  static cilist params;

  params.cierr  = cierr;
  params.ciunit = ciunit;
  params.ciend  = ciend;
  params.cifmt  = cifmt;
  params.cirec  = cirec;

  return s_wdfe( &params );
}
