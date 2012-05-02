#include "libscale.h"
/* PURE _scale_powcc PURE */
complex _scale_powcc(complex e1, complex e2) 
{
  doublecomplex tmp1;
  doublecomplex tmp2;
  doublecomplex result;
  tmp1.r = e1.r;
  tmp1.i = e1.i;
  tmp2.r = e2.r;
  tmp2.i = e2.i;
  _scale_pow_zz(&result, &tmp1, &tmp2);
  return _scale_createcomplex(result.r, result.r);
}
