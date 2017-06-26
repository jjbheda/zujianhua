/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: D:\\github_prj\\liujiaqi2\\zujianhua\\QYBaseCore\\src\\org\\qiyi\\basecore\\filedownload\\FileDownloadCallback.aidl
 */
package org.qiyi.basecore.filedownload;
/**
 * @author kangle
 *  下载变动的回调
 */
public interface FileDownloadCallback extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.qiyi.basecore.filedownload.FileDownloadCallback
{
private static final java.lang.String DESCRIPTOR = "org.qiyi.basecore.filedownload.FileDownloadCallback";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.qiyi.basecore.filedownload.FileDownloadCallback interface,
 * generating a proxy if needed.
 */
public static org.qiyi.basecore.filedownload.FileDownloadCallback asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.qiyi.basecore.filedownload.FileDownloadCallback))) {
return ((org.qiyi.basecore.filedownload.FileDownloadCallback)iin);
}
return new org.qiyi.basecore.filedownload.FileDownloadCallback.Stub.Proxy(obj);
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
case TRANSACTION_onDownloadListChanged:
{
data.enforceInterface(DESCRIPTOR);
java.util.List<org.qiyi.basecore.filedownload.FileDownloadStatus> _arg0;
_arg0 = data.createTypedArrayList(org.qiyi.basecore.filedownload.FileDownloadStatus.CREATOR);
this.onDownloadListChanged(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_onDownloadProgress:
{
data.enforceInterface(DESCRIPTOR);
org.qiyi.basecore.filedownload.FileDownloadStatus _arg0;
if ((0!=data.readInt())) {
_arg0 = org.qiyi.basecore.filedownload.FileDownloadStatus.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
this.onDownloadProgress(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_onPaused:
{
data.enforceInterface(DESCRIPTOR);
org.qiyi.basecore.filedownload.FileDownloadStatus _arg0;
if ((0!=data.readInt())) {
_arg0 = org.qiyi.basecore.filedownload.FileDownloadStatus.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
this.onPaused(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_onFailed:
{
data.enforceInterface(DESCRIPTOR);
org.qiyi.basecore.filedownload.FileDownloadStatus _arg0;
if ((0!=data.readInt())) {
_arg0 = org.qiyi.basecore.filedownload.FileDownloadStatus.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
this.onFailed(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_onCompleted:
{
data.enforceInterface(DESCRIPTOR);
org.qiyi.basecore.filedownload.FileDownloadStatus _arg0;
if ((0!=data.readInt())) {
_arg0 = org.qiyi.basecore.filedownload.FileDownloadStatus.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
this.onCompleted(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.qiyi.basecore.filedownload.FileDownloadCallback
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
     *  下载队列发生变化，由初始化完毕（从数据库中恢复下载记录成功）、添加、删除引起
     * @param downloadStatusList
     */
@Override public void onDownloadListChanged(java.util.List<org.qiyi.basecore.filedownload.FileDownloadStatus> downloadStatusList) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeTypedList(downloadStatusList);
mRemote.transact(Stub.TRANSACTION_onDownloadListChanged, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * 进度发生变化
     * 
     * @param fileDownloadStatus
     */
@Override public void onDownloadProgress(org.qiyi.basecore.filedownload.FileDownloadStatus fileDownloadStatus) throws android.os.RemoteException
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
mRemote.transact(Stub.TRANSACTION_onDownloadProgress, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * 暂停
     * 
     * @param pausedReason 暂停原因 see PAUSED_XXX in DownloadManager
     */
@Override public void onPaused(org.qiyi.basecore.filedownload.FileDownloadStatus fileDownloadStatus) throws android.os.RemoteException
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
mRemote.transact(Stub.TRANSACTION_onPaused, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * 失败
     * 
     * @param failedReason 失败原因 see ERROR_XXX in DownloadManager
     */
@Override public void onFailed(org.qiyi.basecore.filedownload.FileDownloadStatus fileDownloadStatus) throws android.os.RemoteException
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
mRemote.transact(Stub.TRANSACTION_onFailed, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * 下载完成，该方法能保证 回调中的 downloaded File 的有效性（不需要额外判断null || file exist等）
     * 
     * @param finishedFile
     */
@Override public void onCompleted(org.qiyi.basecore.filedownload.FileDownloadStatus fileDownloadStatus) throws android.os.RemoteException
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
mRemote.transact(Stub.TRANSACTION_onCompleted, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_onDownloadListChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_onDownloadProgress = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_onPaused = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_onFailed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_onCompleted = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
}
/**
     *  下载队列发生变化，由初始化完毕（从数据库中恢复下载记录成功）、添加、删除引起
     * @param downloadStatusList
     */
public void onDownloadListChanged(java.util.List<org.qiyi.basecore.filedownload.FileDownloadStatus> downloadStatusList) throws android.os.RemoteException;
/**
     * 进度发生变化
     * 
     * @param fileDownloadStatus
     */
public void onDownloadProgress(org.qiyi.basecore.filedownload.FileDownloadStatus fileDownloadStatus) throws android.os.RemoteException;
/**
     * 暂停
     * 
     * @param pausedReason 暂停原因 see PAUSED_XXX in DownloadManager
     */
public void onPaused(org.qiyi.basecore.filedownload.FileDownloadStatus fileDownloadStatus) throws android.os.RemoteException;
/**
     * 失败
     * 
     * @param failedReason 失败原因 see ERROR_XXX in DownloadManager
     */
public void onFailed(org.qiyi.basecore.filedownload.FileDownloadStatus fileDownloadStatus) throws android.os.RemoteException;
/**
     * 下载完成，该方法能保证 回调中的 downloaded File 的有效性（不需要额外判断null || file exist等）
     * 
     * @param finishedFile
     */
public void onCompleted(org.qiyi.basecore.filedownload.FileDownloadStatus fileDownloadStatus) throws android.os.RemoteException;
}
