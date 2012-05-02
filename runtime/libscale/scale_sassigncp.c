/* Assign a char to a string */
/* PURE _scale_sassigncp PUREGV */
void _scale_sassigncp(char *destp, char src, int destl, char padding)
{
  if (destl > 0) {
    *destp++ = src;
    destl--;
  }

  while (destl-- > 0) {
    *destp++ = padding;
  }
}
