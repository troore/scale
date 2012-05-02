#include <math.h>
#define abs(x) ((x) >= 0 ? (x) : -(x))

/* PURE _scale_isign PURE */
int _scale_isign(int expr1, int expr2)
{
  return ((expr2 >= 0) ? abs(expr1) : -abs(expr1));
}
