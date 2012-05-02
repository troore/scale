#include "f2c.h"

/* PURE fio_dolio PUREGV */
int fio_dolio(int type, int number, char* ptr, int len)
{
  int t = type;
  int n = number;
  return do_lio(&t, &n, ptr, len);
}
