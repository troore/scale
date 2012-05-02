#include "libscale.h"
extern double f__cabs(double, double);
/* PURE _scale_absz PURE */
double _scale_absz(doublecomplex arg)
{
  /* defined in libScaleF77 */
  return f__cabs(arg.r, arg.i);
}
