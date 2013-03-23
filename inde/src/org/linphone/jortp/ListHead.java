package org.linphone.jortp;

///Java lists SUCKS !
public class ListHead {
	private Node mRoot;
	private Node mEnd;
	public ListHead(){
		mRoot=new Node(this);
		mEnd= new Node(this);
		mRoot.mPrev=null;
		mRoot.mNext=mEnd;
		mEnd.mPrev=mRoot;
		mEnd.mNext=null;
	}
	public synchronized Node begin(){
		return mRoot.getNext();
	}
	public synchronized Node rbegin(){
		return mRoot;
	}
	public synchronized Node end(){
		return mEnd;
	}
	public synchronized Node rend(){
		return mEnd.mPrev;
	}
	public synchronized boolean empty(){
		return mRoot.mNext==mEnd;
	}
	public synchronized void pushBack(Node node){
		mEnd.insertBefore(node);
	}
	public synchronized Node popFront(){
		Node ret;
		if (empty()) return null;
		ret=begin();
		ret.remove();
		return ret;
	}
	public synchronized void clear(){
		while(popFront()!=null){
		}
	}
}
