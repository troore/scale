#include "libscale.h"
/* PURE _scale_conjgz PURE */
doublecomplex _scale_conjgz(doublecomplex expr)
{
  doublecomplex result;
  result.r = expr.r;
  result.i = -expr.i;
  return result;
}
