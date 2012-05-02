#include "libscale.h"
extern double f__cabs(double, double);
/* PURE _scale_cabs PURE */
/**
 * Used by Fortran.
 */
double _scale_dabs(doublecomplex arg)
{
  /* defined in libScaleF77 */
  return f__cabs(arg.r, arg.i);
}
