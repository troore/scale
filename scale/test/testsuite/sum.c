int result = 0;

void sum ()
{
	int i;

	for (i = 1; i <= 10; ++i)
	{
		result = result + i;
	}
}

int main ()
{
	result = sum ();

	return 0;
}
