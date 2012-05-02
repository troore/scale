#include "libscale.h"
#include <math.h>
/* PURE _scale_cexp PURE */
/**
 * Used by Fortran.
 */
complex _scale_cexp(complex expr)
{
  complex result;
  double  tmp;

  tmp = exp(expr.r);
  result.r = tmp * cos(expr.i);
  result.i = tmp * sin(expr.i);
  return result;
}
