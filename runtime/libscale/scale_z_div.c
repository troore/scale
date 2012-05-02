#include "libscale.h"
void _scale_z_div(doublecomplex *c, doublecomplex *a, doublecomplex *b)
{
  extern void sig_die(char*, int);
  double ratio, den;
  double abr, abi;

  abr = b->r;
  if (abr < 0.0)
    abr = - abr;
  abi = b->i;
  if (abi < 0.0)
    abi = - abi;
  if (abr <= abi) {
    if (abi == 0)
      sig_die("complex division by zero", 1);

    ratio = b->r / b->i ;
    den = b->i * (1 + ratio * ratio);
    c->r = (a->r * ratio + a->i) / den;
    c->i = (a->i * ratio - a->r) / den;
  } else {
    ratio = b->i / b->r ;
    den = b->r * (1 + ratio * ratio);
    c->r = (a->r + a->i * ratio) / den;
    c->i = (a->i - a->r * ratio) / den;
  }
}
