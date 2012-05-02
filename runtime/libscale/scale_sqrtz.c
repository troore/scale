#include "libscale.h"
extern void z_sqrt(doublecomplex *r, doublecomplex *z);
/* PURE _scale_sqrtz PURE */
doublecomplex _scale_sqrtz(doublecomplex arg)
{
  /* make a call to the f2c sqrt function */
  doublecomplex result;
  z_sqrt(&result, &arg);
  return result;
}
