/* float ** integer */
/* PURE _scale_powfi PURE */
float _scale_powfi(float x, int n)
{
  double pow;
  unsigned long u;

  pow = 1.0;

  if (n != 0) {
    if (n < 0) {
      n = -n;
      x = 1 / x;
    }
    for (u = n; ; ) {
      if (u & 01)
	pow *= x;
      if (u >>= 1)
	x *= x;
      else
	break;
    }
  }
  return pow;
}
