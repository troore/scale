#include "f2c.h"
#include "fio.h"
#include "lio.h"
extern char *f__icptr;
extern char *f__icend;
extern icilist *f__svic;
extern int f__icnum;
extern int z_putc(int);

static int
z_wSL(void)
{
  while(f__recpos < f__svic->icirlen)
    z_putc(' ');
  return z_rnew();
}

void
c_liw(icilist *a)
{
  f__reading   = 0;
  f__external  = 0;
  f__formatted = 1;
  f__putn      = z_putc;
  L_len        = a->icirlen;
  f__donewrec  = z_wSL;
  f__svic      = a;
  f__icnum     = 0;
  f__recpos    = 0;
  f__cursor    = 0;
  f__cf        = 0;
  f__curunit   = 0;
  f__icptr     = a->iciunit;
  f__icend     = f__icptr + a->icirlen * a->icirnum;
  f__elist     = (cilist *) a;
}

integer
s_wsni(icilist *a)
{
  cilist ca;

  c_liw(a);
  ca.cifmt = a->icifmt;
  x_wsne(&ca);
  z_wSL();
  return 0;
}

integer
s_wsli(icilist *a)
{
  f__lioproc = l_write;
  c_liw(a);
  return 0;
}

integer
e_wsli(void)
{
  z_wSL();
  return 0;
}
