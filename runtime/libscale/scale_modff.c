#include <math.h>
/* PURE _scale_modff PURE */
float _scale_modff(float expr1, float expr2)
{
  double xa, ya, z;
  if ((ya = expr2) < 0.)
    ya = -ya;
  xa = expr1;
  z = xa - floor(xa / ya) * ya;
  if (xa > 0) {
    if (z < 0)
      z += ya;
  } else if (z > 0)
    z -= ya;
  return z;
}
