#include "f2c.h"

/**
 * fio_srsfe
 *   start - read - sequential - formatted - external
 */

/* PURE fio_srsfe PUREGVA */
int fio_srsfe( int cierr, int ciunit, int ciend, const char* cifmt, int cirec )
{
  static cilist params;

  params.cierr  = cierr;
  params.ciunit = ciunit;
  params.ciend  = ciend;
  params.cifmt  = cifmt;
  params.cirec  = cirec;

  return s_rsfe( &params );
}
