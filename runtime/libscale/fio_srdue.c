#include "f2c.h"

/**
 * fio_srdue
 *   start - read - direct - unformatted - external
 */

/* PURE fio_srdue PUREGVA */
int fio_srdue( int cierr, int ciunit, int ciend, const char* cifmt, int cirec )
{
  static cilist params;

  params.cierr  = cierr;
  params.ciunit = ciunit;
  params.ciend  = ciend;
  params.cifmt  = cifmt;
  params.cirec  = cirec;

  return s_rdue( &params );
}
