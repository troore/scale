#include "libscale.h"
extern double f__cabs(double, double);
/* PURE _scale_absc PURE */
float _scale_absc(complex arg)
{
  /* defined in libScaleF77 */
  return f__cabs((double) arg.r, (double) arg.i);
}
