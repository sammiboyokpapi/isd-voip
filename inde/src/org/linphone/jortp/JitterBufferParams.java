package org.linphone.jortp;

public interface JitterBufferParams {
	public void setAdaptive(boolean adaptive);
	public boolean isAdaptive();
	public void setNominalSize(int milliseconds);
	public int getNominalSize();
}
