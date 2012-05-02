#include "libscale.h"
/* PURE _scale_powzz PURE */
extern double f__cabs(double, double);
doublecomplex _scale_powzz(doublecomplex e1, doublecomplex e2) 
{
  doublecomplex result;
  _scale_pow_zz(&result, &e1, &e2);
  return result;
}
