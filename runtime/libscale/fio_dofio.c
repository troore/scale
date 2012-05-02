#include "f2c.h"

/* PURE fio_dofio PUREGV */
int fio_dofio(int number, char* ptr, int len)
{
  int n = number;
  return do_fio(&n, ptr, len );
}
