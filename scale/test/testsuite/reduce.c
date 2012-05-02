//#include <stdio.h>

#define ARRAY_LEN 40

typedef int ElementType;

ElementType B[ARRAY_LEN], C[ARRAY_LEN];
int thread_num = 4;

//extern int a;

void init_arrays ()
{
	int i;

	for (i = 0; i < ARRAY_LEN; ++i)
	{
		B[i] = 1;
		C[i] = 1;
	}
}

ElementType reduce ()
{
	int local_len = ARRAY_LEN / thread_num;
	
	int i, j, sum = 0;

	for (i = 0; i < thread_num; ++i)
	{
		int local_sum = 0;
		for (j = i * local_len; j < (i + 1) * local_len; ++j)
		{
			local_sum += B[j] + C[j];
		}

		sum = sum + local_sum;
	}

	return sum;
}

void main ()
{
	ElementType res;

	init_arrays ();
	res = reduce ();

	/* Do not forget to modify the formant parameter of printf when 'ElementType' changes */
	printf ("Sum of two arrays B and C is: %d\n", res);
}
