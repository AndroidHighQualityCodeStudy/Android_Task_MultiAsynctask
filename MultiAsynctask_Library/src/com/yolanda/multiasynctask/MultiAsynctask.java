/**
 * Copyright © YOLANDA. All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yolanda.multiasynctask;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * 
 * Created in Aug 3, 2015 11:06:14 AM
 * 
 * @author YOLANDA
 * @param <Param> Execution parameters
 * @param <Update> Update parameters
 * @param <Result> Result parameter
 */
@SuppressWarnings("unchecked")
public abstract class MultiAsynctask<Param, Update, Result> {
	/**
	 * Mark update implementation process
	 */
	static final int MULTI_ASYNCTASK_UDPATE = 0x001;
	/**
	 * Mark sent execution results
	 */
	static final int MULTI_ASYNCTASK_RESULT = 0x002;
	/**
	 * To deal with the main thread and message between the child thread
	 */
	private static HandlerPoster sHandlerPoster = new HandlerPoster();

	/**
	 * 线程池
	 */
	private static ThreadPoolExecutor MAIN_THREAD_POOL_EXECUTOR;

	/**
	 * 默认5线程
	 */
	public MultiAsynctask() {
		this(5);
	}

	/**
	 * 初始化线程池
	 * 
	 * @param count
	 *            可自定义最大线程数量
	 */
	public MultiAsynctask(int count) {
		MAIN_THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(count, count, 0L,
				TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
	}

	/**
	 * 
	 * 获取单例handler
	 * 
	 * @return
	 */
	private final HandlerPoster getPoster() {
		return sHandlerPoster;
	}

	/**
	 * 
	 * @param params
	 */
	public final void execute(Param... params) {
		//
		onPreExecute();
		//
		TaskExecutor taskExecutor = new TaskExecutor(this, params);
		MAIN_THREAD_POOL_EXECUTOR.execute(taskExecutor);
	}

	/**
	 * 
	 * @param runnable
	 */
	public final void execute(Runnable runnable) {
		MAIN_THREAD_POOL_EXECUTOR.execute(runnable);
	}

	// ----------------------------------------
	/**
	 * 回调 OnPre
	 */
	public void onPreExecute() {
	}

	/**
	 * 耗时操作
	 * 
	 * @param params
	 * @return
	 */
	public abstract Result doInBackground(Param... params);

	/**
	 * 更新进度
	 * 
	 * @param update
	 */
	public void onUpdate(Update update) {
	}

	/**
	 * 返回结果
	 * 
	 * @param result
	 */
	public void onPostExecute(Result result) {
	}

	// ------------------------------------

	/**
	 * 更新进度的消息
	 * 
	 * @param update
	 */
	public final synchronized void postUpdate(Update update) {
		Message message = getPoster().obtainMessage(MULTI_ASYNCTASK_UDPATE,
				new Messenger<Param, Update, Result>(this, update, null));
		message.sendToTarget();
	}

	/**
	 * main handler
	 * 
	 * @author Administrator
	 *
	 */
	private static class HandlerPoster extends Handler {
		public HandlerPoster() {
			super(Looper.getMainLooper());
		}

		@Override
		public void handleMessage(Message msg) {
			Messenger<?, ?, ?> result = (Messenger<?, ?, ?>) msg.obj;
			switch (msg.what) {
			case MultiAsynctask.MULTI_ASYNCTASK_UDPATE:
				result.onUpdate();
				break;
			case MultiAsynctask.MULTI_ASYNCTASK_RESULT:
				result.onResult();
				break;
			}
		}
	}

	/**
	 * 消息
	 * 
	 * @author
	 *
	 * @param <Param>
	 * @param <Update>
	 * @param <Result>
	 */
	private static class Messenger<Param, Update, Result> {

		private MultiAsynctask<Param, Update, Result> mAsynctask;

		private Update mUpdate;

		private Result mResult;

		public Messenger(MultiAsynctask<Param, Update, Result> asynctask,
				Update update, Result result) {
			this.mAsynctask = asynctask;
			this.mUpdate = update;
			this.mResult = result;
		}

		/**
		 * 回调onUpdate
		 */
		public void onUpdate() {
			this.mAsynctask.onUpdate(mUpdate);
		}

		/**
		 * 回调onResult
		 */
		public void onResult() {
			this.mAsynctask.onPostExecute(mResult);
		}

	}

	/**
	 * 创建一个任务
	 * 
	 * @author
	 *
	 */
	private class TaskExecutor implements Runnable {

		/**
		 * This thread to run the parameters
		 */
		private Param[] mParams;

		/**
		 * Used to perform tasks
		 */
		private MultiAsynctask<Param, Update, Result> mTask;

		/**
		 * 
		 * @param mTask
		 * @param params
		 */
		public TaskExecutor(MultiAsynctask<Param, Update, Result> mTask,
				Param... params) {
			super();
			this.mTask = mTask;
			this.mParams = params;
		}

		@Override
		public void run() {
			// 执行耗时操作
			Result result = mTask.doInBackground(mParams);
			// 将结果post到主线程
			postReult(result);
		}

		/**
		 * 将结果post到主线程
		 * 
		 * @param result
		 */
		private void postReult(Result result) {
			//
			Message message = getPoster().obtainMessage(MULTI_ASYNCTASK_RESULT,
					new Messenger<Param, Update, Result>(mTask, null, result));
			//
			message.sendToTarget();
		}
	}
}
