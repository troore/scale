#include "f2c.h"

/**
 * fio_swsue
 *   start - write - sequential - unformatted - external
 */

/* PURE fio_swsue PUREGVA */
int fio_swsue( int cierr, int ciunit, int ciend, const char* cifmt, int cirec )
{
  cilist params;

  params.cierr  = cierr;
  params.ciunit = ciunit;
  params.ciend  = ciend;
  params.cifmt  = cifmt;
  params.cirec  = cirec;

  return s_wsue( &params );
}
