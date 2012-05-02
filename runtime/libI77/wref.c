#include "f2c.h"
#include "fio.h"
#include "fmt.h"
#include "fp.h"
#include "ctype.h"

#undef abs
#undef min
#undef max
#include "stdlib.h"
#include "string.h"

int wrt_DE(ufloat *p, int w, int d, int e, ftnlen len, int de);

int
wrt_E(ufloat *p, int w, int d, int e, ftnlen len)
{
  return wrt_DE(p, w, d, e, len, 0);
}

int
wrt_D(ufloat *p, int w, int d, int e, ftnlen len)
{
  return wrt_DE(p, w, d, e, len, 1);
}

int wrt_DE(ufloat *p, int w, int d, int e, ftnlen len, int useD)
{
  char buf[FMAX+EXPMAXDIGS+4];
  char *s;
  char *se;
  int d1;
  int delta;
  int e1;
  int i;
  int sign;
  int signspace;
  double dd, ddd;
#ifdef WANT_LEAD_0
  int insert0 = 0;
#endif
  int e0 = e;

  if (e <= 0)
    e = 2;

  if (f__scale) {
    if (f__scale >= d + 2 || f__scale <= -d)
      goto nogood;
  }

  if (f__scale <= 0)
    --d;
  if (len == sizeof(real))
    ddd = p->pf;
  else
    ddd = p->pd;

  /* This tricky logic is here just to avoid problems with -0.0 which
     can exist in IEEE floating point. Beware the compiler optimizations. */

  dd = 0.0;
  sign = 0;
  signspace = (int)f__cplus;
  if (ddd != 0.0) {
    if (ddd < 0.0) {
      signspace = sign = 1;
      dd = -ddd;
    } else {
      sign = 0;
      signspace = (int)f__cplus;
      dd = ddd;
    }
  }

  delta = w - (2 /* for the . and the d adjustment above */
	       + 2 /* for the E+ */ + signspace + d + e);

#ifdef WANT_LEAD_0
  if (f__scale <= 0 && delta > 0) {
    delta--;
    insert0 = 1;
  }
  else
#endif
    if (delta < 0) {
    nogood:
      while (--w >= 0)
	PUT('*');
      return 0;
    }

  if (f__scale < 0)
    d += f__scale;

  if (d > FMAX) {
    d1 = d - FMAX;
    d = FMAX;
  } else
    d1 = 0;
  sprintf(buf,"%#.*E", d, dd);

  if (useD) {
    char *ptr = strrchr(&buf[0], 'E');
    if (ptr != 0)
      *ptr = 'D';
  }

  /* check for NaN, Infinity */
  if (!isdigit(buf[0])) {
    switch(buf[0]) {
    case 'n':
    case 'N':
      signspace = 0;	/* no sign for NaNs */
    }
    delta = w - strlen(buf) - signspace;
    if (delta < 0)
      goto nogood;
    while(--delta >= 0)
      PUT(' ');
    if (signspace)
      PUT(sign ? '-' : '+');
    for(s = buf; *s; s++)
      PUT(*s);
    return 0;
  }

  se = buf + d + 3;
#ifdef GOOD_SPRINTF_EXPONENT /* When possible, exponent has 2 digits. */
  if (f__scale != 1 && dd)
    sprintf(se, "%+.2d", atoi(se) + 1 - f__scale);
#else
  if (dd)
    sprintf(se, "%+.2d", atoi(se) + 1 - f__scale);
  else
    strcpy(se, "+00");
#endif
  s = ++se;
  if (e < 2) {
    if (*s != '0')
      goto nogood;
  }

  /* accommodate 3 significant digits in exponent */
  if (s[2]) {
#ifdef Pedantic
    if (!e0 && !s[3])
      for(s -= 2, e1 = 2; s[0] = s[1]; s++);

    /* Pedantic gives the behavior that Fortran 77 specifies,	*/
    /* i.e., requires that E be specified for exponent fields	*/
    /* of more than 3 digits.  With Pedantic undefined, we get	*/
    /* the behavior that Cray displays -- you get a bigger		*/
    /* exponent field if it fits.	*/
#else
    if (!e0) {
      for(s -= 2, e1 = 2; s[0] = s[1]; s++)
#ifdef CRAY
	delta--;
      if ((delta += 4) < 0)
	goto nogood
#endif
	  ;
    } else if (e0 >= 0)
      goto shift;
    else
      e1 = e;
  } else
    shift:
#endif
  for(s += 2, e1 = 2; *s; ++e1, ++s)
    if (e1 >= e)
      goto nogood;

  while(--delta >= 0)
    PUT(' ');

  if (signspace)
    PUT(sign ? '-' : '+');

  s = buf;
  i = f__scale;
  if (f__scale <= 0) {
#ifdef WANT_LEAD_0
    if (insert0)
      PUT('0');
#endif
    PUT('.');
    for(; i < 0; ++i)
      PUT('0');
    PUT(*s);
    s += 2;
  }
  else if (f__scale > 1) {
    PUT(*s);
    s += 2;
    while (--i > 0)
      PUT(*s++);
    PUT('.');
  }
  if (d1) {
    se -= 2;
    while (s < se)
      PUT(*s++);
    se += 2;
    do
      PUT('0');
    while (--d1 > 0);
  }

  while (s < se)
    PUT(*s++);

  if (e < 2)
    PUT(s[1]);
  else {
    while (++e1 <= e)
      PUT('0');
    while (*s)
      PUT(*s++);
  }
  return 0;
}

int
wrt_F(ufloat *p, int w, int d, ftnlen len)
{
  int d1;
  int sign;
  int n;
  double x, y;
  char *b;
  char buf[MAXINTDIGS+MAXFRACDIGS+4];
  char *s;

  y = (len == sizeof(real) ? p->pf : p->pd);
  if (d < MAXFRACDIGS)
    d1 = 0;
  else {
    d1 = d - MAXFRACDIGS;
    d = MAXFRACDIGS;
  }

  /* This tricky logic is here just to avoid problems with -0.0 which
     can exist in IEEE floating point. Beware the compiler optimizations. */

  x = 0.0;
  sign = 0;
  if (y != 0.0) {
    if (y < 0.0) {
      x = -y;
      sign = 1;
    } else {
      sign = 0;
      x = y;
    }
  }

  if (n = f__scale)
    if (n > 0)
      do
	x *= 10.0;
      while (--n > 0);
    else
      do
	x *= 0.1;
      while (++n < 0);

#ifdef USE_STRLEN
  sprintf(b = buf, "%#.*f", d, x);
  n = strlen(b) + d1;
#else
  n = sprintf(b = buf, "%#.*f", d, x) + d1;
#endif

#ifndef WANT_LEAD_0
  if (buf[0] == '0' && d) {
    ++b;
    --n;
  }
#endif
  if (sign) {
    /* check for all zeros */
    for (s = b;;) {
      while (*s == '0') s++;
      switch(*s) {
      case '.':
	s++;
	continue;
      case 0:
	sign = 0;
      }
      break;
    }
  }
  if (sign || f__cplus)
    ++n;
  if (n > w) {
#ifdef WANT_LEAD_0
    if ((buf[0] == '0') && (--n == w))
      ++b;
    else
#endif
      {
	while(--w >= 0)
	  PUT('*');
	return 0;
      }
  }

  for (w -= n; --w >= 0; )
    PUT(' ');

  if (sign)
    PUT('-');
  else if (f__cplus)
    PUT('+');

  while (n = *b++)
    PUT(n);

  while (--d1 >= 0)
    PUT('0');

  return 0;
}
