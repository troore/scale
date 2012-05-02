#include "f2c.h"
#include "fio.h"

static FILE *
unit_chk(integer Unit, char *who)
{
  if ((Unit >= MXUNIT) || (Unit < 0))
    f__fatal(101, who);
  return f__units[Unit].ufd;
}

integer
ftell_(integer *Unit)
{
  FILE *f;
  return (f = unit_chk(*Unit, "ftell")) ? ftell(f) : -1L;
}

int
fseek_(integer *Unit, integer *offset, integer *whence)
{
  FILE *f = unit_chk(*Unit, "fseek");
  return !f || (fseek(f, *offset, (int)*whence) ? 1 : 0);
}
