/* PURE _scale_memcpy PURE */
void *_scale_memcpy(void *dst, const void *src, unsigned long length)
{
  char *ptr = dst;
  const char *spt = src;
  while (length--) *ptr++ = *spt++;
  return dst;
}
