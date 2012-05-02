#include "libscale.h"
/* PURE _scale_nint PURE */
int _scale_nint(double expr)
{
  /* round and produce an integer value */
  return (int)_scale_anint(expr);
}
