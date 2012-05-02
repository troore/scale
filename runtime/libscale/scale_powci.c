#include "libscale.h"
/* PURE _scale_powci PURE */

complex _scale_powci(complex a, int b) 
{
  doublecomplex p1, a1;

  a1.r = a.r;
  a1.i = a.i;

  _scale_pow_zi(&p1, &a1, b);

  return _scale_createcomplex(p1.r, p1.i);
}
