#include "libscale.h"
#include <math.h>
/* PURE _scale_sinz PURE */
doublecomplex _scale_sinz(doublecomplex expr)
{
  doublecomplex result;
  result.r = sin(expr.r) * cosh(expr.i);
  result.i = cos(expr.r) * sinh(expr.i);
  return result;
}
