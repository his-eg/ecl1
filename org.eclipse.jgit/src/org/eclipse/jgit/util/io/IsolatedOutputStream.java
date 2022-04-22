/*
 * Copyright (C) 2016, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.util.io;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jgit.internal.JGitText;

/**
 * OutputStream isolated from interrupts.
 * <p>
 * Wraps an OutputStream to prevent interrupts during writes from being made
 * visible to that stream instance. This works around buggy or difficult
 * OutputStream implementations like JSch that cannot gracefully handle an
 * interrupt during write.
 * <p>
 * Every write (or flush) requires a context switch to another thread. Callers
 * should wrap this stream with {@code BufferedOutputStream} using a suitable
 * buffer size to amortize the cost of context switches.
 *
 * @since 4.6
 */
public class IsolatedOutputStream extends OutputStream {
	private final OutputStream dst;
	private final ExecutorService copier;
	private Future<Void> pending;

	/**
	 * Wraps an OutputStream.
	 *
	 * @param out
	 *            stream to send all writes to.
	 */
	public IsolatedOutputStream(OutputStream out) {
		dst = out;
		copier = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
				new ArrayBlockingQueue<>(1), new NamedThreadFactory());
	}

	/** {@inheritDoc} */
	@Override
	public void write(int ch) throws IOException {
		write(new byte[] { (byte) ch }, 0, 1);
	}

	/** {@inheritDoc} */
	@Override
	public void write(byte[] buf, int pos, int cnt)
			throws IOException {
		checkClosed();
		execute(() -> {
			dst.write(buf, pos, cnt);
			return null;
		});
	}

	/** {@inheritDoc} */
	@Override
	public void flush() throws IOException {
		checkClosed();
		execute(() -> {
			dst.flush();
			return null;
		});
	}

	/** {@inheritDoc} */
	@Override
	public void close() throws IOException {
		if (!copier.isShutdown()) {
			try {
				if (pending == null || tryCleanClose()) {
					cleanClose();
				} else {
					dirtyClose();
				}
			} finally {
				copier.shutdown();
			}
		}
	}

	private boolean tryCleanClose() {
		/*
		 * If the caller stopped waiting for a prior write or flush, they could
		 * be trying to close a stream that is still in-use. Check if the prior
		 * operation ended in a predictable way.
		 */
		try {
			pending.get(0, TimeUnit.MILLISECONDS);
			pending = null;
			return true;
		} catch (TimeoutException | InterruptedException e) {
			return false;
		} catch (ExecutionException e) {
			pending = null;
			return true;
		}
	}

	private void cleanClose() throws IOException {
		execute(() -> {
			dst.close();
			return null;
		});
	}

	private void dirtyClose() throws IOException {
		/*
		 * Interrupt any still pending write or flush operation. This may cause
		 * massive failures inside of the stream, but its going to be closed as
		 * the next step.
		 */
		pending.cancel(true);

		Future<Void> close;
		try {
			close = copier.submit(() -> {
				dst.close();
				return null;
			});
		} catch (RejectedExecutionException e) {
			throw new IOException(e);
		}
		try {
			close.get(200, TimeUnit.MILLISECONDS);
		} catch (InterruptedException | TimeoutException e) {
			close.cancel(true);
			throw new IOException(e);
		} catch (ExecutionException e) {
			throw new IOException(e.getCause());
		}
	}

	private void checkClosed() throws IOException {
		if (copier.isShutdown()) {
			throw new IOException(JGitText.get().closed);
		}
	}

	private void execute(Callable<Void> task) throws IOException {
		if (pending != null) {
			// Check (and rethrow) any prior failed operation.
			checkedGet(pending);
		}
		try {
			pending = copier.submit(task);
		} catch (RejectedExecutionException e) {
			throw new IOException(e);
		}
		checkedGet(pending);
		pending = null;
	}

	private static void checkedGet(Future<Void> future) throws IOException {
		try {
			future.get();
		} catch (InterruptedException e) {
			throw interrupted(e);
		} catch (ExecutionException e) {
			throw new IOException(e.getCause());
		}
	}

	private static InterruptedIOException interrupted(InterruptedException c) {
		InterruptedIOException e = new InterruptedIOException();
		e.initCause(c);
		return e;
	}

	private static class NamedThreadFactory implements ThreadFactory {
		private static final AtomicInteger cnt = new AtomicInteger();

		@Override
		public Thread newThread(Runnable r) {
			int n = cnt.incrementAndGet();
			String name = IsolatedOutputStream.class.getSimpleName() + '-' + n;
			return new Thread(r, name);
		}
	}
}
