/* Assign a char to a string */
/* PURE _scale_sassign PUREGV */
char *_scale_sassignc(char *destp, char src, int destl)
{
  if (destl > 0)
    *destp++ = src;
  return destp;
}
