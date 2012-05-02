#include "f2c.h"

/**
 * fio_srsli
 *   start - read - sequential - list directed - internal
 */

/* PURE fio_srsli PUREGVA */
int fio_srsli(int icierr, char* iciunit, int iciend, const char* icifmt, int icirlen, int icirnum )
{
  static icilist params;

  params.icierr   = icierr;
  params.iciunit  = iciunit;
  params.iciend   = iciend;
  params.icifmt   = icifmt;
  params.icirlen  = icirlen;
  params.icirnum  = icirnum;

  return s_rsli( &params );
}
