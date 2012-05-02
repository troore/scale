#include "libscale.h"
/* PURE _scale_divcc PURE */
complex _scale_divcc(complex expr1, complex expr2)
{
  complex result;
  double abs_r, abs_i;
  double scale, div;
  double r1 = expr1.r;
  double i1 = expr1.i;
  double r2 = expr2.r;
  double i2 = expr2.i;
  double r2s, i2s;

  abs_r = r2;
  if (abs_r < 0.0)
    abs_r = -abs_r;

  abs_i = i2;
  if (abs_i < 0.0)
    abs_i = -abs_i;

  if (abs_r >= abs_i)
    scale = 1.0 / abs_r;
  else
    scale = 1.0 / abs_i;

  r2s = r2 * scale;
  i2s = i2 * scale;
  div = 1.0 / (r2s * r2 + i2s * i2);
  result.r = (r1 * r2s + i1 * i2s) * div;
  result.i = (i1 * r2s - r1 * i2s) * div;
  return result;
}
