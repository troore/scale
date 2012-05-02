#include "f2c.h"

/**
 * fio_srdfe
 *   start - read - direct - formatted - external
 */

/* PURE fio_srdfe PUREGVA */
int fio_srdfe( int cierr, int ciunit, int ciend, const char* cifmt, int cirec )
{
  static cilist params;

  params.cierr  = cierr;
  params.ciunit = ciunit;
  params.ciend  = ciend;
  params.cifmt  = cifmt;
  params.cirec  = cirec;

  return s_rdfe( &params );
}
