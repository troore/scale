#include <stdio.h>
/* Implements the Fortran Pause statement */
void _scale_pause(char *n)
{
  fprintf(stderr, "Pause: %d\n", n);
  /* no actual pause is performed */
}
