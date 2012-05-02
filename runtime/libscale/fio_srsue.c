#include "f2c.h"

/**
 * fio_srsue
 *   start - read - sequential - unformatted - external
 */

/* PURE fio_srsue PUREGVA */
int fio_srsue( int cierr, int ciunit, int ciend, const char* cifmt, int cirec )
{
  static cilist params;

  params.cierr  = cierr;
  params.ciunit = ciunit;
  params.ciend  = ciend;
  params.cifmt  = cifmt;
  params.cirec  = cirec;

  return s_rsue( &params );
}
