#include "libscale.h"
#include <math.h>
/* PURE _scale_cosz PURE */
doublecomplex _scale_cosz(doublecomplex expr)
{
  doublecomplex result;
  result.r =   cos(expr.r) * cosh(expr.i);
  result.i = - sin(expr.r) * sinh(expr.i);
  return result;
}
