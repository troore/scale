#include "f2c.h"
#include "fio.h"

integer
f_rew(alist *a)
{
  unit *b;

  if ((a->aunit >= MXUNIT) || (a->aunit < 0))
    return err(a->aerr, 101, "rewind");

  b = &f__units[a->aunit];

  if ((b->ufd == NULL) || (b->uwrt == 3))
    return(0);

  if (!b->useek)
    return err(a->aerr, 106, "rewind");
  if (b->uwrt) {
    (void) t_runc(a);
    b->uwrt = 3;
  }
  rewind(b->ufd);
  b->uend = 0;
  return 0;
}
