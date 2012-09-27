/*
 * RHQ WebSphere Plug-in
 * Copyright (C) 2012 Crossroads Bank for Social Security
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package be.fgov.kszbcss.rhq.websphere.config.cache;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Blocking priority queue implementation similar to {@link PriorityBlockingQueue}, except that
 * priorities are evaluated when the queue is polled. This means that priorities are mutable.
 * 
 * @param <E>
 */
public class MutablePriorityQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition notEmpty = lock.newCondition();
    private final List<E> elements = new LinkedList<E>();

    public boolean add(E o) {
        return offer(o);
    }

    public boolean offer(E o, long timeout, TimeUnit unit) throws InterruptedException {
        return offer(o); // we never block
    }

    public void put(E o) throws InterruptedException {
        offer(o); // we never block
    }

    public boolean offer(E o) {
        lock.lock();
        try {
            boolean result = elements.add(o);
            notEmpty.signal();
            return result;
        } finally {
            lock.unlock();
        }
    }

    public E peek() {
        lock.lock();
        try {
            return getHead();
        } finally {
            lock.unlock();
        }
    }

    public E poll() {
        lock.lock();
        try {
            E head = getHead();
            elements.remove(head);
            return head;
        } finally {
            lock.unlock();
        }
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        lock.lockInterruptibly();
        try {
            while (true) {
                E head = getHead();
                if (head != null) {
                    elements.remove(head);
                    return head;
                }
                if (nanos <= 0) {
                    return null;
                }
                try {
                    nanos = notEmpty.awaitNanos(nanos);
                } catch (InterruptedException ex) {
                    notEmpty.signal(); // propagate to non-interrupted thread
                    throw ex;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public E take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            try {
                while (elements.size() == 0) {
                    notEmpty.await();
                }
            } catch (InterruptedException ie) {
                notEmpty.signal(); // propagate to non-interrupted thread
                throw ie;
            }
            E head = getHead();
            elements.remove(head);
            return head;
        } finally {
            lock.unlock();
        }
    }

    private E getHead() {
        E head = null;
        for (E o : elements) {
            if (head == null || ((Comparable<? super E>)head).compareTo(o) > 0) {
                head = o;
            }
        }
        return head;
    }
    
    public int size() {
        lock.lock();
        try {
            return elements.size();
        } finally {
            lock.unlock();
        }
    }
    
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    public Iterator<E> iterator() {
        lock.lock();
        try {
            return new ArrayList<E>(elements).iterator();
        } finally {
            lock.unlock();
        }
    }

    public int drainTo(Collection<? super E> c) {
        lock.lock();
        try {
            int n = elements.size();
            c.addAll(elements);
            elements.clear();
            return n;
        } finally {
            lock.unlock();
        }
    }

    public int drainTo(Collection<? super E> c, int maxElements) {
        throw new UnsupportedOperationException();
    }
}