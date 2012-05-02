#include <stdio.h>
extern void exit(int);

void _scale_stop(char *msg, int len)
{
  int i;
  for (i = 0; i < len; i++)
    fputc(*(msg + i), stderr);
  fputc('\n', stderr);
  exit(0);
}
