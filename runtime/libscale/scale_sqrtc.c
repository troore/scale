#include "libscale.h"
extern void c_sqrt(complex *r, complex *z);
/* PURE _scale_sqrtc PURE */
complex _scale_sqrtc(complex arg)
{
  /* make a call to the f2c sqrt function */
  complex result;
  c_sqrt(&result, &arg);
  return result;
}
