#include "libscale.h"
#include <math.h>
/* PURE _scale_csin PURE */
/**
 * Used by Fortran.
 */
complex _scale_csin(complex expr)
{
  complex result;
  result.r = sin(expr.r) * cosh(expr.i);
  result.i = cos(expr.r) * sinh(expr.i);
  return result;
}
