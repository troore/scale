#include <math.h>
/* PURE _scale_aint PURE */
double _scale_aint(double expr)
{
  /* truncate toward zero */
  return ((expr >= 0.0) ? floor(expr) : ceil(expr));
}
