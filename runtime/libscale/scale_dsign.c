#include <math.h>
/* PURE _scale_dsign PURE */
double _scale_dsign(double expr1, double expr2)
{
  return ((expr2 >= 0) ? fabs(expr1) : -fabs(expr1));
}
