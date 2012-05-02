/* 
** Character comparison - returns
**  -1 for less than
**  0 for equal
**  1 for greater than
*/
/* PURE _scale_scmp PURE */
int _scale_scmp(char *e1, char *e2, int e1l, int e2l) 
{
  int result = 0;
  char e1c, e2c;

  while (e1l > 0 || e2l > 0) {
    e1c = (e1l > 0) ? (e1l--, *e1++) : ' ';
    e2c = (e2l > 0) ? (e2l--, *e2++) : ' ';
    if (e1c < e2c) {
      result = -1; 
      break;
    } 
    if (e1c > e2c) {
      result = 1;
      break;
    }
  }
  return result;
}

/* PURE _scale_scmpc PURE */
int _scale_scmpc(char *e1, char e2, int e1l) 
{
  int result = 0;
  char e1c, e2c;

  while (e1l > 0) {
    e1c = (e1l > 0) ? (e1l--, *e1++) : ' ';
    e2c = e2; e2 = ' ';
    if (e1c < e2c) {
      result = -1; 
      break;
    } 
    if (e1c > e2c) {
      result = 1;
      break;
    }
  }
  return result;
}

/* PURE _scale_ccmps PURE */
int _scale_ccmps(char e1, char *e2, int e2l) 
{
  int result = 0;
  char e1c, e2c;

  while (e2l > 0) {
    e1c = e1; e1 = ' ';
    e2c = (e2l > 0) ? (e2l--, *e2++) : ' ';
    if (e1c < e2c) {
      result = -1; 
      break;
    } 
    if (e1c > e2c) {
      result = 1;
      break;
    }
  }
  return result;
}
