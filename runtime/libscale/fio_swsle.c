#include "f2c.h"

/**
 * fio_swsle
 *   start - write - sequential - list directed - external
 */

/* PURE fio_swsle PUREGVA */
int fio_swsle( int cierr, int ciunit, int ciend, const char* cifmt, int cirec )
{
  cilist params;

  params.cierr  = cierr;
  params.ciunit = ciunit;
  params.ciend  = ciend;
  params.cifmt  = cifmt;
  params.cirec  = cirec;

  return s_wsle( &params );
}
