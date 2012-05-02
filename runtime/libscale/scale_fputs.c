#include <stdio.h>
/* PURE _scale_fputs PURE */
int _scale_fputs(const char *s, void *st)
{
  return fputs(s, (FILE *) st);
}
