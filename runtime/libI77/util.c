#include "sys/types.h"
#include "sys/stat.h"
#include "f2c.h"
#include "fio.h"

void
g_char(char *a, ftnlen alen, char *b)
{
  char *x = a + alen;
  char *y = b + alen;

  for(;; y--) {
    if (x <= a) {
      *b = 0;
      return;
    }
    if (*--x != ' ')
      break;
  }
  *y-- = 0;
  do *y-- = *x;
  while (x-- > a);
}

void
b_char(char *a, char *b, ftnlen blen)
{
  int i;

  for (i = 0; (i < blen) && (*a != 0); i++)
    *b++= *a++;

  for(; i < blen; i++)
    *b++=' ';
}

long
f__inode(char *a, int *dev)
{
  struct stat x;

  if (stat(a, &x) < 0)
    return -1;

  *dev = x.st_dev;

  return x.st_ino;
}

