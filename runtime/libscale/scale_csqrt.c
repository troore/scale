#include "libscale.h"
extern void c_sqrt(complex *r, complex *z);
/* PURE _scale_csqrt PURE */
/**
 * Used by Fortran.
 */
complex _scale_csqrt(complex arg)
{
  /* make a call to the f2c sqrt function */
  complex result;
  c_sqrt(&result, &arg);
  return result;
}
