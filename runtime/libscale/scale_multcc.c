#include "libscale.h"
/* PURE _multcc PURE */
complex _scale_multcc(complex expr1, complex expr2) 
{
  complex result;
  result.r = expr1.r * expr2.r - expr1.i * expr2.i;
  result.i = expr1.r * expr2.i + expr1.i * expr2.r;
  return result;
}
