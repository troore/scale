#include "f2c.h"

#ifdef KR_headers
void r_cnjg(r, z) complex *r, *z;
#else
void r_cnjg(complex *r, complex *z)
#endif
{
r->r = z->r;
r->i = - z->i;
}
