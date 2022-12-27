+ Object {

	logist {
		arg k=1, l=1;
		var denominator=(1+exp(k*this.neg));
		var result=l/denominator;
		^result
	}

}