#define FMAX 40
#define EXPMAXDIGS 8
#define EXPMAX 99999999
/* FMAX = max number of nonzero digits passed to atof() */
/* EXPMAX = 10^EXPMAXDIGS - 1 = largest allowed exponent absolute value */

/* MAXFRACDIGS and MAXINTDIGS are for wrt_F -- bounds (not necessarily
   tight) on the maximum number of digits to the right and left of
 * the decimal point.
 */

/* values that suffice for IEEE double */
#define MAXFRACDIGS 344
#define MAXINTDIGS 308
