#include "libscale.h"
/* PURE _scale_negatez PURE */
doublecomplex _scale_negatez(doublecomplex expr) 
{
  doublecomplex result;
  result.r = -expr.r;
  result.i = -expr.i;
  return result;
}
