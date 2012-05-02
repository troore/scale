#include "libscale.h"
extern double f__cabs(double, double);
/* PURE CDABS PURE */
double _scale_CDABS(doublecomplex *expr)
{
  return f__cabs(expr->r, expr->i);
}
