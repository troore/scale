#include "libscale.h"
#include <math.h>
/* PURE _scale_ccos PURE */
/**
 * Used by Fortran.
 */
complex _scale_ccos(complex expr)
{
  complex result;
  result.r =   cos(expr.r) * cosh(expr.i);
  result.i = - sin(expr.r) * sinh(expr.i);
  return result;
}
