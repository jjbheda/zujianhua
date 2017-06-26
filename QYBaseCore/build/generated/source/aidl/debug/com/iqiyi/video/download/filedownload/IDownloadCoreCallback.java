/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: D:\\github_prj\\liujiaqi2\\zujianhua\\QYBaseCore\\src\\com\\iqiyi\\video\\download\\filedownload\\IDownloadCoreCallback.aidl
 */
package com.iqiyi.video.download.filedownload;
public interface IDownloadCoreCallback extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.iqiyi.video.download.filedownload.IDownloadCoreCallback
{
private static final java.lang.String DESCRIPTOR = "com.iqiyi.video.download.filedownload.IDownloadCoreCallback";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.iqiyi.video.download.filedownload.IDownloadCoreCallback interface,
 * generating a proxy if needed.
 */
public static com.iqiyi.video.download.filedownload.IDownloadCoreCallback asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.iqiyi.video.download.filedownload.IDownloadCoreCallback))) {
return ((com.iqiyi.video.download.filedownload.IDownloadCoreCallback)iin);
}
return new com.iqiyi.video.download.filedownload.IDownloadCoreCallback.Stub.Proxy(obj);
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
case TRANSACTION_callback:
{
data.enforceInterface(DESCRIPTOR);
com.iqiyi.video.download.filedownload.FileDownloadExBean _arg0;
if ((0!=data.readInt())) {
_arg0 = com.iqiyi.video.download.filedownload.FileDownloadExBean.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
this.callback(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getMessage:
{
data.enforceInterface(DESCRIPTOR);
com.iqiyi.video.download.filedownload.FileDownloadExBean _arg0;
if ((0!=data.readInt())) {
_arg0 = com.iqiyi.video.download.filedownload.FileDownloadExBean.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
com.iqiyi.video.download.filedownload.FileDownloadExBean _result = this.getMessage(_arg0);
reply.writeNoException();
if ((_result!=null)) {
reply.writeInt(1);
_result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.iqiyi.video.download.filedownload.IDownloadCoreCallback
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
//下载器回调消息给UI层

@Override public void callback(com.iqiyi.video.download.filedownload.FileDownloadExBean msg) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((msg!=null)) {
_data.writeInt(1);
msg.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_callback, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
//下载器调用本地进程信息

@Override public com.iqiyi.video.download.filedownload.FileDownloadExBean getMessage(com.iqiyi.video.download.filedownload.FileDownloadExBean msg) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
com.iqiyi.video.download.filedownload.FileDownloadExBean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((msg!=null)) {
_data.writeInt(1);
msg.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_getMessage, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = com.iqiyi.video.download.filedownload.FileDownloadExBean.CREATOR.createFromParcel(_reply);
}
else {
_result = null;
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_callback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_getMessage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
}
//下载器回调消息给UI层

public void callback(com.iqiyi.video.download.filedownload.FileDownloadExBean msg) throws android.os.RemoteException;
//下载器调用本地进程信息

public com.iqiyi.video.download.filedownload.FileDownloadExBean getMessage(com.iqiyi.video.download.filedownload.FileDownloadExBean msg) throws android.os.RemoteException;
}
