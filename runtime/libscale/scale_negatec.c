#include "libscale.h"
/* PURE _scale_negatec PURE */
complex _scale_negatec(complex expr) 
{
  complex result;
  result.r = -expr.r;
  result.i = -expr.i;
  return result;
}
