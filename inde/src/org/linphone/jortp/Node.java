package org.linphone.jortp;

public class Node {
	protected Node mNext;
	protected Node mPrev;
	Object mlock;
	public Node(){
		mNext=mPrev=null;
	}
	protected Node(Object aLock) {
		this();
		mlock=aLock;
	}
	public Node getNext(){
		synchronized (mlock) {
			return mNext;
		}
	}
	public Node getPrev(){
		synchronized (mlock) {
			return mPrev;
		}
	}
	public void insertAfter(Node n){
		if (n.mlock == null) {
			n.mlock = mlock;
		}
		synchronized (mlock) {
			n.mNext=mNext;
			n.mPrev=this;
			mNext.mPrev=n;
			mNext=n;
		}
	}
	public void insertBefore(Node n){
		if (n.mlock == null) {
			n.mlock = mlock;
		}
		synchronized (mlock) {
			n.mPrev=mPrev;
			n.mNext=this;
			mPrev.mNext=n;
			mPrev=n;
		}
	}
	public void remove(){
		synchronized (mlock) {
			mPrev.mNext=mNext;
			mNext.mPrev=mPrev;
			mNext=mPrev=null;
			mlock=null;
		}
	}

}
