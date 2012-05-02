#include "libscale.h"
/* PURE _multzz PURE */
doublecomplex _scale_multzz(doublecomplex expr1, doublecomplex expr2) 
{
  doublecomplex result;
  result.r = expr1.r * expr2.r - expr1.i * expr2.i;
  result.i = expr1.r * expr2.i + expr1.i * expr2.r;
  return result;
}
