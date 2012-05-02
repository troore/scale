#include "sys/types.h"
#include "sys/stat.h"
#include "f2c.h"
#include "fio.h"
#include "string.h"
#include "rawio.h"

#undef abs
#undef min
#undef max
#include "stdlib.h"
extern int f__canseek(FILE*);
extern integer f_clos(cllist*);

char *f__r_mode[2] = {"rb", "r"};
char *f__w_mode[4] = {"wb", "w", "r+b", "r+"};

int
f__isdev(char *s)
{
  struct stat x;

  if (stat(s, &x) == -1)
    return 0;

#ifdef S_IFMT
  switch(x.st_mode & S_IFMT) {
  case S_IFREG:
  case S_IFDIR:
    return 0;
  }
#else
#ifdef S_ISREG
  /* POSIX version */
  if (S_ISREG(x.st_mode) || S_ISDIR(x.st_mode))
    return 0;
  else
#else
    Help! How does stat work on this system?
#endif
#endif
  return 1;
}

integer
f_open(olist *a)
{
  unit *b;
  integer rv;
  char buf[256];
  char *s;
  cllist x;
  int ufmt;
  int n;
  struct stat stb;

  if ((a->ounit >= MXUNIT) || (a->ounit < 0))
    return err(a->oerr, 101, "open");

  if (!f__init)
    f_init();

  b = &f__units[a->ounit];
  f__curunit = b;

  if (b->ufd) {
    if (a->ofnm == 0) {
    same:
      if (a->oblnk)
	b->ublnk = (*a->oblnk == 'z') || (*a->oblnk == 'Z');
      return 0;
    }
    g_char(a->ofnm, a->ofnmlen, buf);
    if ((f__inode(buf, &n) == b->uinode) && (n == b->udev))
      goto same;

    x.cunit = a->ounit;
    x.csta  = 0;
    x.cerr  = a->oerr;

    if ((rv = f_clos(&x)) != 0)
      return rv;
  }

  b->url = (int) a->orl;
  b->ublnk = a->oblnk && ((*a->oblnk == 'z') || (*a->oblnk == 'Z'));

  if (a->ofm == 0) {
    if (b->url > 0)
      b->ufmt = 0;
    else
      b->ufmt = 1;
  }
  else if ((*a->ofm == 'f') || (*a->ofm == 'F'))
    b->ufmt = 1;
  else
    b->ufmt = 0;

  ufmt = b->ufmt;
#ifdef url_Adjust
  if (b->url && !ufmt)
    url_Adjust(b->url);
#endif
  if (a->ofnm) {
    g_char(a->ofnm, a->ofnmlen, buf);
    if (!buf[0])
      return err(a->oerr, 107, "open");
  }
  else
    sprintf(buf, "fort.%ld", a->ounit);
  b->uscrtch = 0;
  switch (a->osta ? *a->osta : 'u') {
  case 'o':
  case 'O':
    if (stat(buf, &stb))
      return err(a->oerr, errno, "open");
    break;
  case 's':
  case 'S':
    b->uscrtch = 1;
    tmpnam(buf);
    goto replace;
  case 'n':
  case 'N':
    if (!stat(buf, &stb))
      return err(a->oerr, 128, "open");
    /* no break */
  case 'r':	/* Fortran 90 replace option */
  case 'R':
  replace:
  (void) close(creat(buf, 0666));
  }

  b->ufnm = (char *) malloc((unsigned int)(strlen(buf) + 1));
  if (b->ufnm == NULL)
    return err(a->oerr, 113, "no space");

  (void) strcpy(b->ufnm, buf);
  b->uend = 0;
  b->uwrt = 0;

  if (f__isdev(buf)) {
    b->ufd = fopen(buf, f__r_mode[ufmt]);
    if (b->ufd == NULL)
      return err(a->oerr, errno, buf);
  } else {
    if (!(b->ufd = fopen(buf, f__r_mode[ufmt]))) {
      if ((n = open(buf, O_WRONLY)) >= 0)
	b->uwrt = 2;
      else {
	n = creat(buf, 0666);
	b->uwrt = 1;
      }
      if ((n < 0) || ((b->ufd = fdopen(n, f__w_mode[ufmt])) == NULL))
	return err(a->oerr, errno, "open");
    }
  }

  b->useek=f__canseek(b->ufd);
  if (b->useek)
    if (a->orl)
      rewind(b->ufd);
    else if ((s = a->oacc) && ((*s == 'a') || (*s == 'A'))
	     && fseek(b->ufd, 0L, SEEK_END))
      return err(a->oerr, 129, "open");

  b->uinode = f__inode(buf, &b->udev);

  return 0;
}

int
fk_open(int seq, int fmt, ftnint n)
{
  char nbuf[10];
  olist a;

  (void) sprintf(nbuf, "fort.%ld", n);

  a.oerr    = 1;
  a.ounit   = n;
  a.ofnm    = nbuf;
  a.ofnmlen = strlen(nbuf);
  a.osta    = NULL;
  a.oacc    = ((seq == SEQ) ? "s" : "d");
  a.ofm     = ((fmt == FMT) ? "f" : "u");
  a.orl     = ((seq == DIR) ? 1 : 0);
  a.oblnk   = NULL;
  return f_open(&a);
}
