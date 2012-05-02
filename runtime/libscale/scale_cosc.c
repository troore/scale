#include "libscale.h"
#include <math.h>
/* PURE _scale_cosc PURE */
complex _scale_cosc(complex expr)
{
  complex result;
  result.r =   cos(expr.r) * cosh(expr.i);
  result.i = - sin(expr.r) * sinh(expr.i);
  return result;
}
