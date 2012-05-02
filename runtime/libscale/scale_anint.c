#include <math.h>
/* PURE _scale_anint PURE */
double _scale_anint(double expr)
{
  /* round towards nearest whole number */
  return ((expr >= 0.0) ? floor(expr + 0.5) : ceil(expr - 0.5));
}
