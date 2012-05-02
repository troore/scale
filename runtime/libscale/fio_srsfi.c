#include "f2c.h"

/**
 * fio_srsfi
 *   start - read - sequential - formatted - internal
 */

/* PURE fio_srsfi PUREGVA */
int fio_srsfi(int icierr, char *iciunit, int iciend, const char* icifmt, int icirlen, int icirnum)
{
  static icilist params;

  params.icierr   = icierr;
  params.iciunit  = iciunit;
  params.iciend   = iciend;
  params.icifmt   = icifmt;
  params.icirlen  = icirlen;
  params.icirnum  = icirnum;

  return s_rsfi( &params );
}
