#include "f2c.h"

/**
 * fio_swsfe
 *   start - write - sequential - formatted - external
 */

/* PURE fio_swsfe PUREGVA */
int fio_swsfe(int cierr, int ciunit, int ciend, const char* cifmt, int cirec)
{
  static cilist params;

  params.cierr  = cierr;
  params.ciunit = ciunit;
  params.ciend  = ciend;
  params.cifmt  = cifmt;
  params.cirec  = cirec;

  return s_wsfe( &params );
}
