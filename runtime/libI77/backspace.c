#include "f2c.h"
#include "fio.h"

integer f_back(alist *a)
{
  unit *b;
  int i;
  int n;
  int ndec;
  long x;
  long y;
  char buf[32];

  if ((a->aunit >= MXUNIT) || (a->aunit < 0))
    return err(a->aerr, 101, "backspace");

  b = &f__units[a->aunit];

  if (b->useek == 0)
    return err(a->aerr, 106, "backspace");

  if (b->ufd == NULL) {
     fk_open(1, 1, a->aunit);
     return 0;
  }
  if (b->uend == 1) {
    b->uend = 0;
    return 0;
  }
  if (b->uwrt) {
    (void) t_runc(a);
    if (f__nowreading(b))
      return err(a->aerr, errno, "backspace");

  }
  if (b->url > 0) {
    x = ftell(b->ufd);
    y = x % b->url;
    if (y == 0)
      x--;
    x /= b->url;
    x *= b->url;
    (void) fseek(b->ufd, x, SEEK_SET);
    return 0;
  }

  if (b->ufmt == 0) {
    (void) fseek(b->ufd, -(long)sizeof(int), SEEK_CUR);
    (void) fread((char *)&n, sizeof(int), 1, b->ufd);
    (void) fseek(b->ufd, -(long)n-2*sizeof(int), SEEK_CUR);
    return 0;
  }

  for (ndec = 1;; ndec = 0) {
    x = ftell(b->ufd);
    y = x;

    if (x < sizeof(buf))
      x = 0;
    else
      x -= sizeof(buf);

    (void) fseek(b->ufd, x, SEEK_SET);
    n = fread(buf, 1, (int)(y - x), b->ufd);

    for (i = n - ndec; --i >= 0; ) {
      if (buf[i] != '\n')
        continue;
      fseek(b->ufd, (long)(i + 1 - n), SEEK_CUR);
      return 0;
    }

    if (x == 0) {
      (void) fseek(b->ufd, 0L, SEEK_SET);
      return 0;
    } else if (n <= 0)
      return err(a->aerr, (EOF), "backspace");

    (void) fseek(b->ufd, x, SEEK_SET);
  }
}
