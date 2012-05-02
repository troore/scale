#include "f2c.h"

/**
 * fio_swsli
 *   start - write - sequential - list directed - internal
 */

/* PURE fio_swsli PUREGVA */
int fio_swsli( int icierr, char* iciunit, int iciend, const char* icifmt, int icirlen, int icirnum )
{
  icilist params;

  params.icierr   = icierr;
  params.iciunit  = iciunit;
  params.iciend   = iciend;
  params.icifmt   = icifmt;
  params.icirlen  = icirlen;
  params.icirnum  = icirnum;

  return s_wsli( &params );
}
