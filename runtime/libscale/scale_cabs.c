#include "libscale.h"
extern double f__cabs(double, double);
/* PURE _scale_cabs PURE */
/**
 * Used by Fortran.
 */
float _scale_cabs(complex arg)
{
  return f__cabs((double) arg.r, (double) arg.i);
}
