/* PURE _scale_memset PURE */
void *_scale_memset(void *dst, int c, unsigned long length)
{
  char *ptr = dst;
  while (length--) *ptr++ = c;
  return ptr;
}
