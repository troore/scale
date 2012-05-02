/**
 * Copyright 2005 by the <a href="http: *spa-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http: *www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http: *www.umass.edu/">University of Massachusetts</a.,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

#ifndef LIBSCALE_H
#define LIBSCALE_H

static char const Rcs_libscale_h[] = "$Id: libscale.h,v 1.1 2005-08-03 15:16:31 burrill Exp $";

#include "Clef2C.h"

/** f2c Fortran IO interface routines. **/

extern int fio_backspace(int aerr, int aunit);
extern int fio_close(int cerr, int cunit, char *csta);
extern int fio_dofio(int number, char* ptr, int len);
extern int fio_dolio(int type, int number, char* ptr, int len);
extern int fio_douio(int number, char* ptr, int len);
extern int fio_endfile(int aerr, int aunit);
extern int fio_erdfe(void);
extern int fio_erdue(void);
extern int fio_ersfe(void);
extern int fio_ersfi(void);
extern int fio_ersle(void);
extern int fio_ersli(void);
extern int fio_ersue(void);
extern int fio_ewdfe(void);
extern int fio_ewdue(void);
extern int fio_ewsfe(void);
extern int fio_ewsfi(void);
extern int fio_ewsle(void);
extern int fio_ewsli(void);
extern int fio_ewsue(void);
extern int fio_open(int oerr, int ounit, char *ofnm, int ofnmlen, char *osta, char *oacc, char *ofm, int orl, char *oblnk);
extern int fio_rewind(int aerr, int aunit);
extern int fio_srdfe(int cierr, int ciunit, int ciend, const char* cifmt, int cirec);
extern int fio_srdue(int cierr, int ciunit, int ciend, const char* cifmt, int cirec);
extern int fio_srsfe(int cierr, int ciunit, int ciend, const char* cifmt, int cirec);
extern int fio_srsfi(int icierr, char *iciunit, int iciend, const char* icifmt, int icirlen, int icirnum);
extern int fio_srsle(int cierr, int ciunit, int ciend, const char* cifmt, int cirec);
extern int fio_srsli(int icierr, char* iciunit, int iciend, const char* icifmt, int icirlen, int icirnum);
extern int fio_srsue(int cierr, int ciunit, int ciend, const char* cifmt, int cirec);
extern int fio_swdfe(int cierr, int ciunit, int ciend, const char* cifmt, int cirec);
extern int fio_swdue(int cierr, int ciunit, int ciend, const char* cifmt, int cirec);
extern int fio_swsfe(int cierr, int ciunit, int ciend, const char* cifmt, int cirec);
extern int fio_swsfi(int icierr, char* iciunit, int iciend, const char* icifmt, int icirlen, int icirnum);
extern int fio_swsle(int cierr, int ciunit, int ciend, const char* cifmt, int cirec);
extern int fio_swsli(int icierr, char* iciunit, int iciend, const char* icifmt, int icirlen, int icirnum);
extern int fio_swsue(int cierr, int ciunit, int ciend, const char* cifmt, int cirec);

#endif /* LIBSCALE_H */
