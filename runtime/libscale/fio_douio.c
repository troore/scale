#include "f2c.h"

/* PURE fio_douio PUREGV */
int fio_douio(int number, char* ptr, int len)
{
  int n = number;
  return do_uio( &n, ptr, len );
}
