#include "libscale.h"
/* PURE _scale_addzz PURE */
doublecomplex _scale_addzz(doublecomplex expr1, doublecomplex expr2) 
{
  doublecomplex result;
  result.r = expr1.r + expr2.r;
  result.i = expr1.i + expr2.i;
  return result;
}
