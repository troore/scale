#include <stdio.h>
/* PURE _scale_fputc PURE */
int _scale_fputc(int c, void *st)
{
  return fputc(c, (FILE *) st);
}
