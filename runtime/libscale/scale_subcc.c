#include "libscale.h"
/* PURE _scale_subcc PURE */
complex _scale_subcc(complex expr1, complex expr2) 
{
  complex result;
  result.r = expr1.r - expr2.r;
  result.i = expr1.i - expr2.i;
  return result;
}
