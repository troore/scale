/* double ** long */
/* PURE _scale_powdl PURE */
double _scale_powdl(double x, long n)
{
  double pow;
  unsigned long u;

  pow = 1.0;

  if (n != 0) {
    if (n < 0) {
      n = -n;
      x = 1.0 / x;
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
