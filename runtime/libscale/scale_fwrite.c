#include <stdio.h>
/* PURE _scale_fwrite PURE */
unsigned long _scale_fwrite(const void *ptr, unsigned long size, unsigned long nmemb, void *st)
{
  return fwrite(ptr, size, nmemb, (FILE *) st);
}
