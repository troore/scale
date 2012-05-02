#include <stdio.h>
#include <stdarg.h>
/* PURE _scale_printf PURE */
int _scale_printf(const char *fmt, ...)
{
  va_list ap;
  va_start(ap, fmt);
  return vfprintf(stdout, fmt, ap);
}
