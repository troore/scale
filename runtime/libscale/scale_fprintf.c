#include <stdio.h>
#include <stdarg.h>
/* PURE _scale_fprintf PURE */
int _scale_fprintf(FILE *file, const char *fmt, ...)
{
  va_list ap;
  va_start(ap, fmt);
  return vfprintf(file, fmt, ap);
}
