#include "libscale.h"
#include <math.h>
extern double f__cabs(double, double);
/* PURE _scale_logz PURE */
doublecomplex _scale_logz(doublecomplex expr)
{
  doublecomplex result;

  result.i = atan2(expr.i, expr.r);
  result.r = log(f__cabs(expr.r, expr.i));
  return result;
}
