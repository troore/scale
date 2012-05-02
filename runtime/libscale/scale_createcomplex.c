#include "libscale.h"
/* PURE _scale_createcomplex PURE */
complex _scale_createcomplex(float r, float i) 
{
  complex result;
  result.r = r;
  result.i = i;
  return result;
}
