#include "libscale.h"
#include <math.h>
/* PURE _scale_sinc PURE */
complex _scale_sinc(complex expr)
{
  complex result;
  result.r = sin(expr.r) * cosh(expr.i);
  result.i = cos(expr.r) * sinh(expr.i);
  return result;
}
