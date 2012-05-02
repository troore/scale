/* integer ** integer */
/* PURE _scale_powii PURE */
int _scale_powii(int x, int n)
{
  int pow;
  unsigned long u;

  if (n <= 0) {
    if ((n == 0) || (x == 1))
      return 1;
    if (x != -1)
      return (x == 0 ? 1 / x : 0);
    n = -n;
  }
  u = n;
  for (pow = 1; ; ) {
    if (u & 01)
      pow *= x;
    if (u >>= 1)
      x *= x;
    else
      break;
  }
  return pow;
}
