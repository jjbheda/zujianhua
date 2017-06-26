/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: D:\\github_prj\\liujiaqi2\\zujianhua\\QYBaseCore\\src\\org\\qiyi\\basecore\\filedownload\\FileDownloadRemoteServiceInterface.aidl
 */
package org.qiyi.basecore.filedownload;
public interface FileDownloadRemoteServiceInterface extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.qiyi.basecore.filedownload.FileDownloadRemoteServiceInterface
{
private static final java.lang.String DESCRIPTOR = "org.qiyi.basecore.filedownload.FileDownloadRemoteServiceInterface";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.qiyi.basecore.filedownload.FileDownloadRemoteServiceInterface interface,
 * generating a proxy if needed.
 */
public static org.qiyi.basecore.filedownload.FileDownloadRemoteServiceInterface asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.qiyi.basecore.filedownload.FileDownloadRemoteServiceInterface))) {
return ((org.qiyi.basecore.filedownload.FileDownloadRemoteServiceInterface)iin);
}
return new org.qiyi.basecore.filedownload.FileDownloadRemoteServiceInterface.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_registerCallback:
{
data.enforceInterface(DESCRIPTOR);
org.qiyi.basecore.filedownload.FileDownloadCallback _arg0;
_arg0 = org.qiyi.basecore.filedownload.FileDownloadCallback.Stub.asInterface(data.readStrongBinder());
java.lang.String _arg1;
_arg1 = data.readString();
this.registerCallback(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_unregisterCallback:
{
data.enforceInterface(DESCRIPTOR);
org.qiyi.basecore.filedownload.FileDownloadCallback _arg0;
_arg0 = org.qiyi.basecore.filedownload.FileDownloadCallback.Stub.asInterface(data.readStrongBinder());
java.lang.String _arg1;
_arg1 = data.readString();
this.unregisterCallback(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_addDownload:
{
data.enforceInterface(DESCRIPTOR);
java.util.List<org.qiyi.basecore.filedownload.FileDownloadStatus> _arg0;
_arg0 = data.createTypedArrayList(org.qiyi.basecore.filedownload.FileDownloadStatus.CREATOR);
this.addDownload(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_pauseDownload:
{
data.enforceInterface(DESCRIPTOR);
org.qiyi.basecore.filedownload.FileDownloadStatus _arg0;
if ((0!=data.readInt())) {
_arg0 = org.qiyi.basecore.filedownload.FileDownloadStatus.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
this.pauseDownload(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_resumeDownload:
{
data.enforceInterface(DESCRIPTOR);
org.qiyi.basecore.filedownload.FileDownloadStatus _arg0;
if ((0!=data.readInt())) {
_arg0 = org.qiyi.basecore.filedownload.FileDownloadStatus.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
this.resumeDownload(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_deleteDownloads:
{
data.enforceInterface(DESCRIPTOR);
java.util.List<org.qiyi.basecore.filedownload.FileDownloadStatus> _arg0;
_arg0 = data.createTypedArrayList(org.qiyi.basecore.filedownload.FileDownloadStatus.CREATOR);
this.deleteDownloads(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getDownloads:
{
data.enforceInterface(DESCRIPTOR);
java.util.List<org.qiyi.basecore.filedownload.FileDownloadStatus> _result = this.getDownloads();
reply.writeNoException();
reply.writeTypedList(_result);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.qiyi.basecore.filedownload.FileDownloadRemoteServiceInterface
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
/**
	 * 注册监听
     */
@Override public void registerCallback(org.qiyi.basecore.filedownload.FileDownloadCallback cb, java.lang.String type) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((cb!=null))?(cb.asBinder()):(null)));
_data.writeString(type);
mRemote.transact(Stub.TRANSACTION_registerCallback, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * 注销监听
     */
@Override public void unregisterCallback(org.qiyi.basecore.filedownload.FileDownloadCallback cb, java.lang.String type) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((cb!=null))?(cb.asBinder()):(null)));
_data.writeString(type);
mRemote.transact(Stub.TRANSACTION_unregisterCallback, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * 添加下载
     */
@Override public void addDownload(java.util.List<org.qiyi.basecore.filedownload.FileDownloadStatus> fileDownloadStatusList) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeTypedList(fileDownloadStatusList);
mRemote.transact(Stub.TRANSACTION_addDownload, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * 暂停下载
     */
@Override public void pauseDownload(org.qiyi.basecore.filedownload.FileDownloadStatus fileDownloadStatus) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((fileDownloadStatus!=null)) {
_data.writeInt(1);
fileDownloadStatus.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_pauseDownload, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * 继续下载
     */
@Override public void resumeDownload(org.qiyi.basecore.filedownload.FileDownloadStatus fileDownloadStatus) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((fileDownloadStatus!=null)) {
_data.writeInt(1);
fileDownloadStatus.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_resumeDownload, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * 删除下载
     */
@Override public void deleteDownloads(java.util.List<org.qiyi.basecore.filedownload.FileDownloadStatus> fileDownloadStatusList) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeTypedList(fileDownloadStatusList);
mRemote.transact(Stub.TRANSACTION_deleteDownloads, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * 取得下载集合
     */
@Override public java.util.List<org.qiyi.basecore.filedownload.FileDownloadStatus> getDownloads() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.List<org.qiyi.basecore.filedownload.FileDownloadStatus> _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getDownloads, _data, _reply, 0);
_reply.readException();
_result = _reply.createTypedArrayList(org.qiyi.basecore.filedownload.FileDownloadStatus.CREATOR);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_registerCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_unregisterCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_addDownload = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_pauseDownload = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_resumeDownload = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_deleteDownloads = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_getDownloads = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
}
/**
	 * 注册监听
     */
public void registerCallback(org.qiyi.basecore.filedownload.FileDownloadCallback cb, java.lang.String type) throws android.os.RemoteException;
/**
     * 注销监听
     */
public void unregisterCallback(org.qiyi.basecore.filedownload.FileDownloadCallback cb, java.lang.String type) throws android.os.RemoteException;
/**
     * 添加下载
     */
public void addDownload(java.util.List<org.qiyi.basecore.filedownload.FileDownloadStatus> fileDownloadStatusList) throws android.os.RemoteException;
/**
     * 暂停下载
     */
public void pauseDownload(org.qiyi.basecore.filedownload.FileDownloadStatus fileDownloadStatus) throws android.os.RemoteException;
/**
     * 继续下载
     */
public void resumeDownload(org.qiyi.basecore.filedownload.FileDownloadStatus fileDownloadStatus) throws android.os.RemoteException;
/**
     * 删除下载
     */
public void deleteDownloads(java.util.List<org.qiyi.basecore.filedownload.FileDownloadStatus> fileDownloadStatusList) throws android.os.RemoteException;
/**
     * 取得下载集合
     */
public java.util.List<org.qiyi.basecore.filedownload.FileDownloadStatus> getDownloads() throws android.os.RemoteException;
}
