/* Assign one string to another */
/* PURE _scale_sassign PUREGV */
char *_scale_sassign(char *destp, char *srcp, int destl, int srcl)
{
  while ((destl-- > 0) && (srcl-- > 0))
    *destp++ = *srcp++;

  return destp;
}
