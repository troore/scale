#ifndef JAVART
#define JAVART

void *  __findIntMethod(int index, int hashcode, const struct VTABLE *vtable)
{
   const struct INTERFACEENTRY *ie = vtable->interfaces;
   while (ie->hash != hashcode) ie++;
   return *(ie->methods + index);
}
#endif
