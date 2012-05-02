#ifdef OPEN_DECL
extern int creat(const char*,int), open(const char*,int);
#endif
extern int close(int);
extern int read(int,void*,size_t), write(int,const void*,size_t);
extern int unlink(const char*);
#ifndef _POSIX_SOURCE
#ifndef NON_UNIX_STDIO
extern FILE *fdopen(int, const char*);
#endif
#endif

extern char *mktemp(char*);

#include "fcntl.h"

#ifndef O_WRONLY
#define O_RDONLY 0
#define O_WRONLY 1
#endif
