#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#define I (32 * 1024)
#define P_LEN 128
#define T_LEN ((P_LEN - 1) + 4 * I)

#define SLICE 4
#define slice_num (P_LEN / SLICE)

char text[T_LEN];
char pattern[P_LEN];
int occurrences[T_LEN];
int counter = 0;

FILE *p_data;

void match()
{
	int i, j, h;

	for (i = 0; i < (T_LEN - P_LEN + 1); i += 4)
	{
		int k0 = i;
		int k1 = i + 1;
		int k2 = i + 2;
		int k3 = i + 3;
		int flag0 = true;
		int flag1 = true;
		int flag2 = true;
		int flag3 = true;

		for (h = 0; h < slice_num; h++)
		{
			for (j = h * SLICE; j < (h + 1) * SLICE; j++)
			{
				if (text[k0] != pattern[j]) flag0 = false;
				if (text[k1] != pattern[j]) flag1 = false;
				if (text[k2] != pattern[j]) flag2 = false;
				if (text[k3] != pattern[j]) flag3 = false;
				k0++; k1++; k2++; k3++;
			}
		}
		if (flag0) occurrences[counter++] = i;
		if (flag1) occurrences[counter++] = i + 1;
		if (flag2) occurrences[counter++] = i + 2;
		if (flag3) occurrences[counter++] = i + 3;
	}
}

int main()
{
	int i;
#if RATIO == 0
	for (i = 0; i < P_LEN; ++i)
	{
		pattern[i] = 'a';
	}
	for (i = 0; i < T_LEN; ++i)
	{
		text[i] = 'b';
	}
#elif RATIO == 100
	for (i = 0; i < P_LEN; ++i)
	{
		pattern[i] = 'a';
	}
	for (i = 0; i < T_LEN; ++i)
	{
		text[i] = 'a';
	}
#elif RATIO == 25
	for (i = 0; i < P_LEN; ++i)
	{
		pattern[i] = 'a' + (i % 4);
	}
	for (i = 0; i < T_LEN; ++i)
	{
		text[i] = 'a' + (i % 4);
	}
#elif RATIO == 50
	for (i = 0; i < P_LEN; ++i)
	{
		pattern[i] = 'a' + (i % 2);
	}
	for (i = 0; i < T_LEN; ++i)
	{
		text[i] = 'a' + (i % 2);
	}

#endif


	match();

/*	for (i = 0; i < counter; ++i)
	{
		printf ("%d\n", occurrences[i]);
	}
	if (counter == 0)
	{
		printf ("NONONONO.\n");
	}
*/
	return 0;
}
