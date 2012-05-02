#include "f2c.h"

double pow_di(doublereal *ap, integer *bp)
{
  double pow;
  double x;
  integer n;
  unsigned long u;

  pow = 1.0;
  x = *ap;
  n = *bp;

  if (n != 0) {
    if (n < 0) {
      n = -n;
      x = 1.0 / x;
    }
    for(u = n; ; ) {
      if (u & 01)
	pow *= x;
      if (u >>= 1)
	x *= x;
      else
	break;
    }
  }
  return pow;
}

double _pow_di(doublereal x, integer n)
{
  double pow;
  unsigned long u;

  pow = 1.0;

  if (n != 0) {
    if (n < 0) {
      n = -n;
      x = 1.0 / x;
    }
    for(u = n; ; ) {
      if (u & 01)
	pow *= x;
      if (u >>= 1)
	x *= x;
      else
	break;
    }
  }
  return pow;
}
