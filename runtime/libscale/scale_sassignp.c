/* Assign one string to another */
/* PURE _scale_sassign PUREGV */
void _scale_sassignp(char *destp, char *srcp, int destl, int srcl, char padding)
{
  while (destl-- > 0) {
    *destp++ = (srcl > 0) ? (srcl--, *srcp++) : padding;
  }
}
