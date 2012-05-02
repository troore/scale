#include "libscale.h"
#include <math.h>
extern double f__cabs(double, double);
/* PURE _scale_clog PURE */
/**
 * Used by Fortran.
 */
complex _scale_clog(complex expr)
{
  complex result;

  result.i = atan2(expr.i, expr.r);
  result.r = log(f__cabs(expr.r, expr.i));
  return result;
}
