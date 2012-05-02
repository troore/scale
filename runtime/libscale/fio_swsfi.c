#include "f2c.h"

/**
 * fio_swsfi
 *   start - write - sequential - formatted - internal
 */

/* PURE fio_swsfi PUREGVA */
int fio_swsfi( int icierr, char* iciunit, int iciend, const char* icifmt, int icirlen, int icirnum )
{
  static icilist params;

  params.icierr   = icierr;
  params.iciunit  = iciunit;
  params.iciend   = iciend;
  params.icifmt   = icifmt;
  params.icirlen  = icirlen;
  params.icirnum  = icirnum;

  return s_wsfi( &params );
}
