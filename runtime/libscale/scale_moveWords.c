void _scale_moveWords(int size, long *src, long *dest)
{
  int i;
  for (i = 0; i < size; i += 8) {
    *dest++ = *src++;
  }
}
