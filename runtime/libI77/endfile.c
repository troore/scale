#include "f2c.h"
#include "fio.h"
#include "sys/types.h"
#include "rawio.h"
#undef abs
#undef min
#undef max
#include "stdlib.h"
#include "string.h"

integer
f_end(alist *a)
{
  unit *b;
  if ((a->aunit >= MXUNIT) || (a->aunit < 0))
    return err(a->aerr, 101, "endfile");

  b = &f__units[a->aunit];
  if (b->ufd == NULL) {
    char nbuf[10];
    (void) sprintf(nbuf, "fort.%ld", a->aunit);
    close(creat(nbuf, 0666));
    return 0;
  }

  b->uend = 1;

  return (b->useek ? t_runc(a) : 0);
}

static int
copy(char *from, register long len, char *to)
{
  int n;
  int k;
  int rc = 0;
  int tmp;
  char buf[BUFSIZ];

  if ((k = open(from, O_RDONLY)) < 0)
    return 1;
  if ((tmp = creat(to, 0666)) < 0)
    return 1;

  while((n = read(k, buf, len > BUFSIZ ? BUFSIZ : (int)len)) > 0) {
    if (write(tmp, buf, n) != n) {
      rc = 1;
      break;
    }
    if ((len -= n) <= 0)
      break;
  }

  close(k);
  close(tmp);

  return ((n < 0) ? 1 : rc);
}

#ifndef L_tmpnam
#define L_tmpnam 16
#endif

int
t_runc(alist *a)
{
  char nm[L_tmpnam+12];	/* extra space in case L_tmpnam is tiny */
  long loc;
  long len;
  unit *b;
  FILE *bf;
  int rc = 0;

  b = &f__units[a->aunit];
  if (b->url)
    return 0;	/*don't truncate direct files*/

  loc = ftell(bf = b->ufd);

  fseek(bf, 0L, SEEK_END);

  len = ftell(bf);
  if ((loc >= len) || (b->useek == 0) || (b->ufnm == NULL))
    return 0;

  rewind(b->ufd);	/* empty buffer */

  if (!loc) {
    if (close(creat(b->ufnm, 0666)))
      rc = 1;
    if (b->uwrt)
      b->uwrt = 1;
    goto done;
  }

  tmpnam(nm);
  if (copy(b->ufnm, loc, nm) || copy(nm, loc, b->ufnm))
    rc = 1;
  unlink(nm);
  fseek(b->ufd, loc, SEEK_SET);

done:
  if (rc)
    return err(a->aerr, 111, "endfile");
  return 0;
}
