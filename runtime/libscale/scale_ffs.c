/* PURE _scale_ffs PURE */
int _scale_ffs(int arg)
{
  int shift;
  int bit;

  if (arg == 0)
    return 0;

  shift = 0;
  for (shift = 0; shift < 4; shift++) {
    if ((arg & 0xff) != 0)
      break;
    arg >>= 8;
  }

  bit = 1;
  for (bit = 1; bit <= 8; bit++) {
    if ((arg & 1) != 0)
      return bit + shift * 8;
    arg >>= 1;
  }

  return 0;
}
