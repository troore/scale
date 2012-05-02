#include "libscale.h"
#include <math.h>
/* PURE _scale_expz PURE */
doublecomplex _scale_expz(doublecomplex expr)
{
  doublecomplex result;
  double tmp;

  tmp = exp(expr.r);
  result.r = tmp * cos(expr.i);
  result.i = tmp * sin(expr.i);
  return result;
}
