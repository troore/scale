int B[40] = {1}, C[40] = {1};

int reduce ()
{
	int i, j, sum = 0;

	for (i = 0; i < 4; ++i)
	{
		int local_sum = 0;

		for (j = i * 10; j < (i + 1) * 10; ++j)
		{
			local_sum += B[j] + C[j];
		}
		sum = sum + local_sum;
	}

	return sum;
}

void main ()
{
	int res = reduce ();
}
