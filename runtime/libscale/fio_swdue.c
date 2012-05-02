#include "f2c.h"

/**
 * fio_swdue
 *   start - write - direct - unformatted - external
 */

/* PURE fio_swdue PUREGVA */
int fio_swdue( int cierr, int ciunit, int ciend, const char* cifmt, int cirec )
{
  static cilist params;

  params.cierr  = cierr;
  params.ciunit = ciunit;
  params.ciend  = ciend;
  params.cifmt  = cifmt;
  params.cirec  = cirec;

  return s_wdue( &params );
}
