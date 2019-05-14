class OptimalAssignment {
	public static void main(String[] args) {
		Assign a; 
		a = new Assign();
		System.out.println(a.set(1));
	}
}

class Assign {

	int field;
	public int set(int arg) {
		int i;
		i = arg;					// left side should be loaded
		arg = field;				// right side should be loaded
		field = 5;				    // no (re)loads
		i = arg;                	// both sides should be loaded
		i = field;					// left side should be loaded

		// 'arg' and 'field' are both loaded but only 'field' is loop invariant
		while(i < 8){
			System.out.println(i);
			i = i + 1;				// 'i' should be loaded again
			arg = arg + field;		// arg should be loaded again but 'field' should not, 'field' is loop invariant, arg is not
		}
		return arg;//should be 15
	}
}
