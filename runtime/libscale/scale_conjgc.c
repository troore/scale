#include "libscale.h"
/* PURE _scale_conjgc PURE */
complex _scale_conjgc(complex expr)
{
  complex result;
  result.r = expr.r;
  result.i = -expr.i;
  return result;
}
