/* PURE _scale_sindex PURE */
int _scale_sindex(char *s1, char *s2, int ls1, int ls2)
{
  int i;
  char *s1p, *s2p;
  int n = ls1 - ls2 + 1;
  char *s2end = s2 + ls2;
  int match = 1;

  /* start at first position in string s1 */
  for (i = 0; i < n; i++) {
    s1p = s1 + i;           /* point to next char in s1 */
    s2p = s2;               /* point to begin. of s2 */
    match = 1;              /* we assume we match */
    /* check if string s2 matches substring s1p */
    while (s2p < s2end) {
      if (*s1p++ != *s2p++) {
	match = 0;
	continue;
      }
    }
  }
  if (match) {
    return i;
  } else {
    return 0;
  }
}
