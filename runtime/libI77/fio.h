#include "stdio.h"
#include "errno.h"
#ifndef NULL
/* ANSI C */
#include "stddef.h"
#endif

#ifndef SEEK_SET
#define SEEK_SET 0
#define SEEK_CUR 1
#define SEEK_END 2
#endif

#ifdef UIOLEN_int
typedef int uiolen;
#else
typedef long uiolen;
#endif

/* units */
typedef struct
{
  FILE *ufd;	/* 0=unconnected */
  char *ufnm;
  long uinode;
  int udev;
  int  url;	/* 0=sequential */
  flag useek;	/* true=can backspace, use dir, ... */
  flag ufmt;
  flag uprnt;
  flag ublnk;
  flag uend;
  flag uwrt;	/* last io was write */
  flag uscrtch;
} unit;

extern flag f__init;
extern cilist *f__elist;	/* active external io list */
extern flag f__reading,f__external,f__sequential,f__formatted;
extern int  (*f__getn)(void),(*f__putn)(int);	/* for formatted io */
extern long f__inode(char*,int*);
extern void sig_die(const char*,int);
extern void f__fatal(int,char*);
extern int  t_runc(alist*);
extern int  f__nowreading(unit*), f__nowwriting(unit*);
extern int  fk_open(int,int,ftnint);
extern int  en_fio(void);
extern void f_init(void);
extern int  (*f__donewrec)(void), t_putc(int), x_wSL(void);
extern void b_char(char*,char*,ftnlen), g_char(char*,ftnlen,char*);
extern int  c_sfe(cilist*), z_rnew(void);
extern int  isatty(int);
extern int  err__fl(int,int,char*);
extern int  xrd_SL(void);

extern int (*f__doend)(void);
extern FILE *f__cf;	        /* current file */
extern unit *f__curunit;	/* current unit */
extern unit f__units[];
extern int err(int f, int m, char *s);
extern int err_fl(int f, int m, char *s);

/* Table sizes */
#define MXUNIT 100

extern int f__recpos;	/* position in current record */
extern int f__cursor;	/* offset to move to */
extern int f__hiwater;	/* so TL doesn't confuse us */

#define WRITE	1
#define READ	2
#define SEQ	3
#define DIR	4
#define FMT	5
#define UNF	6
#define EXT	7
#define INT	8

#define buf_end(x) (x->_flag & _IONBF ? x->_ptr : x->_base + BUFSIZ)
