#include "f2c.h"
#include "fio.h"

int
c_due(cilist *a)
{
  if (!f__init)
    f_init();

  if ((a->ciunit >= MXUNIT) || (a->ciunit < 0))
    return err(a->cierr, 101, "startio");

  f__sequential = 0;
  f__formatted  = 0;
  f__recpos     = 0;
  f__external   = 1;
  f__curunit    = &f__units[a->ciunit];
  f__elist      = a;

  if ((f__curunit->ufd == NULL) && fk_open(DIR, UNF, a->ciunit))
    return err(a->cierr, 104, "due");
  f__cf=f__curunit->ufd;
  if (f__curunit->ufmt)
    return err(a->cierr, 102, "cdue");
  if (!f__curunit->useek)
    return err(a->cierr, 104, "cdue");
  if (f__curunit->ufd == NULL)
    return err(a->cierr, 114, "cdue");
  if (a->cirec <= 0)
    return err(a->cierr, 130, "due");

  fseek(f__cf, (long) (a->cirec - 1) * f__curunit->url, SEEK_SET);
  f__curunit->uend = 0;
  return 0;
}

integer
s_rdue(cilist *a)
{
  int n;

  f__reading = 1;
  if (n=c_due(a))
    return(n);
  if (f__curunit->uwrt && f__nowreading(f__curunit))
    return err(a->cierr, errno, "read start");

  return 0;
}

integer
s_wdue(cilist *a)
{
  int n;

  f__reading = 0;
  if (n = c_due(a))
    return n;
  if ((f__curunit->uwrt != 1) && f__nowwriting(f__curunit))
    return err(a->cierr, errno, "write start");
  return 0;
}

integer
e_rdue(void)
{
  if ((f__curunit->url == 1) || (f__recpos == f__curunit->url))
    return 0;

  fseek(f__cf, (long)(f__curunit->url-f__recpos), SEEK_CUR);

  if (ftell(f__cf) % f__curunit->url)
    return err(f__elist->cierr, 200, "syserr");

  return 0;
}

integer
e_wdue(void)
{
#ifdef ALWAYS_FLUSH
  if (fflush(f__cf))
    return err(f__elist->cierr, errno, "write end");
#endif
  return e_rdue();
}
