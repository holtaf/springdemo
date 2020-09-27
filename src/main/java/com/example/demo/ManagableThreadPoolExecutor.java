package com.example.demo;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ManagableThreadPoolExecutor extends ThreadPoolExecutor {
	private final ConcurrentMap<String, Runnable> runnableMap;
	private final ConcurrentMap<Runnable, Thread> threadMap;

	public ManagableThreadPoolExecutor() {
		super(0, Integer.MAX_VALUE,
				60L, TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>());

		runnableMap = new ConcurrentHashMap<>();
		threadMap = new ConcurrentHashMap<>();
	}

	public String submitTask(String taskId, Runnable runnable) {
		runnableMap.put(taskId, runnable);

		execute(runnable);

		return taskId;
	}

	public Set<String> getTaskSet() {
		return runnableMap.keySet();
	}

	public void pause(String taskId) {
		Thread thread = findThreadByTaskId(taskId);
		if (thread != null) {
			thread.suspend();
		}
	}

	public void resume(String taskId) {
		Thread thread = findThreadByTaskId(taskId);
		if (thread != null) {
			thread.resume();
		}
	}

	public void cancel(String taskId) {
		Thread thread = findThreadByTaskId(taskId);
		if (thread != null) {
			thread.stop();

			removeTask(taskId);
		}
	}

	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);

		threadMap.put(r, t);
	}

	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);

		removeTask(findTaskIdByRunnable(r));
	}

	private void removeTask(String taskId) {
		if (taskId != null) {
			Runnable r = runnableMap.get(taskId);

			if (r != null) {
				threadMap.remove(r);
			}
		}
	}

	private String findTaskIdByRunnable(Runnable r) {
		for (Map.Entry<String, Runnable> entry : runnableMap.entrySet()) {
			if (entry.getValue().equals(r)) {
				return entry.getKey();
			}
		}

		return null;
	}

	private Thread findThreadByTaskId(String taskId) {
		Runnable r = runnableMap.get(taskId);
		if (r != null) {
			return threadMap.get(r);
		}

		return null;
	}
}