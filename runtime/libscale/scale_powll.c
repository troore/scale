/* long ** long */
/* PURE _scale_powll PURE */
long _scale_powll(long x, long n)
{
  long pow;
  unsigned long u;	/* system-dependent */

  if (n <= 0) {
    if ((n == 0) || (x == 1))
      return 1;
    if (x != -1)
      return x == (0 ? 1 / x : 0);
    n = -n;
  }
  u = n;
  for(pow = 1; ; ) {
      if (u & 01)
	pow *= x;
      if (u >>= 1)
	x *= x;
      else
	break;
    }
  return pow;
}
