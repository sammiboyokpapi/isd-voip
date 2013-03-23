package org.linphone.jortp;

class JitterBufferParamsImpl implements JitterBufferParams {
	boolean mAdaptive;
	int mNominalSize;
	
	JitterBufferParamsImpl(){
		mAdaptive=true;
		mNominalSize=60;
	}
	
	public int getNominalSize() {
		return mNominalSize;
	}

	public boolean isAdaptive() {
		return mAdaptive;
	}

	public void setAdaptive(boolean adaptive) {
		mAdaptive=adaptive;
	}

	public void setNominalSize(int milliseconds) {
		mNominalSize=milliseconds;
	}

}
