#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#define I (32 * 1024)
#define P_LEN 128
#define T_LEN ((P_LEN - 1) + 4 * I)

char text[T_LEN];
char pattern[P_LEN];
int occurrences[T_LEN];
int counter = 0;

FILE *p_data;

void match()
{
	int i, j;

	for (i = 0; i < (T_LEN - P_LEN + 1); ++i)
	{
		int k = i;
		int flag = true;

		for (j = 0; j < P_LEN; j++)
		{
			if (text[k] != pattern[j]) flag = false;
			k++;
		}
		if (flag)
		{
			occurrences[counter++] = i;
		}
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
