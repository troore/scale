#include "libscale.h"
/* PURE _scale_powzi PURE */
doublecomplex _scale_powzi(doublecomplex e1, int n)
{
  doublecomplex result;
  _scale_pow_zi(&result, &e1, n);
  return result;
}
