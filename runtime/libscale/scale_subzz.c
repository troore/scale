#include "libscale.h"
/* PURE _scale_subzz PURE */
doublecomplex _scale_subzz(doublecomplex expr1, doublecomplex expr2) 
{
  doublecomplex result;
  result.r = expr1.r - expr2.r;
  result.i = expr1.i - expr2.i;
  return result;
}
