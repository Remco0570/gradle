/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.collections;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.gradle.api.Action;
import org.gradle.api.internal.DefaultMutationGuard;
import org.gradle.api.internal.MutationGuard;
import org.gradle.api.internal.provider.ChangingValue;
import org.gradle.api.internal.provider.CollectionProviderInternal;
import org.gradle.api.internal.provider.Collectors;
import org.gradle.api.internal.provider.ProviderInternal;
import org.gradle.internal.Cast;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

public class SortedSetElementSource<T> implements ElementSource<T> {
    private final TreeSet<T> values;
    private final List<Collectors.TypedCollector<T>> pending = Lists.newArrayList();
    private Action<T> addRealizedAction;
    private final MutationGuard mutationGuard = new DefaultMutationGuard();

    public SortedSetElementSource(Comparator<T> comparator) {
        this.values = new TreeSet<T>(comparator);
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty() && pending.isEmpty();
    }

    @Override
    public boolean constantTimeIsEmpty() {
        return values.isEmpty() && pending.isEmpty();
    }

    @Override
    public int size() {
        int pendingSize = 0;
        for (Collectors.TypedCollector<T> collector : pending) {
            pendingSize += collector.size();
        }

        return values.size() + pendingSize;
    }

    @Override
    public int estimatedSize() {
        return size();
    }

    @Override
    public Iterator<T> iterator() {
        realizePending();
        return values.iterator();
    }

    @Override
    public Iterator<T> iteratorNoFlush() {
        return values.iterator();
    }

    @Override
    public boolean contains(Object element) {
        realizePending();
        return values.contains(element);
    }

    @Override
    public boolean containsAll(Collection<?> elements) {
        realizePending();
        return values.containsAll(elements);
    }

    @Override
    public boolean add(T element) {
        return values.add(element);
    }

    @Override
    public boolean addRealized(T element) {
        return values.add(element);
    }

    @Override
    public boolean remove(Object o) {
        return values.remove(o);
    }

    @Override
    public void clear() {
        pending.clear();
        values.clear();
    }

    @Override
    public void realizePending() {
        if (!pending.isEmpty()) {
            List<Collectors.TypedCollector<T>> copied = Lists.newArrayList(pending);
            realize(copied);
        }
    }

    @Override
    public void realizePending(Class<?> type) {
        if (!pending.isEmpty()) {
            List<Collectors.TypedCollector<T>> copied = Lists.newArrayList();
            for (Collectors.TypedCollector<T> collector : pending) {
                if (collector.getType() == null || type.isAssignableFrom(collector.getType())) {
                    copied.add(collector);
                }
            }
            realize(copied);
        }
    }

    private void realize(Iterable<Collectors.TypedCollector<T>> collectors) {
        for (Collectors.TypedCollector<T> collector : collectors) {
            pending.remove(collector);
            ImmutableList.Builder<T> builder = ImmutableList.builder();
            // Collect elements discarding potential side effects aggregated in the returned value
            collector.collectInto(builder);
            List<T> realized = builder.build();
            for (T element : realized) {
                doAddRealized(element);
            }
        }
    }

    private void doAddRealized(T value) {
        if (addRealized(value) && addRealizedAction != null) {
            addRealizedAction.execute(value);
        }
    }

    @Override
    public boolean addPending(final ProviderInternal<? extends T> provider) {
        if (provider instanceof ChangingValue) {
            Cast.<ChangingValue<T>>uncheckedNonnullCast(provider).onValueChange(previousValue -> {
                values.remove(previousValue);
                doAddPending(provider);
            });
        }
        return doAddPending(provider);
    }

    private boolean doAddPending(ProviderInternal<? extends T> provider) {
        return pending.add(new Collectors.TypedCollector<T>(provider.getType(), new Collectors.ElementFromProvider<T>(provider)));
    }

    @Override
    public boolean removePending(ProviderInternal<? extends T> provider) {
        return removeByProvider(provider);
    }

    private boolean removeByProvider(ProviderInternal<?> provider) {
        Iterator<Collectors.TypedCollector<T>> iterator = pending.iterator();
        while (iterator.hasNext()) {
            Collectors.TypedCollector<T> collector = iterator.next();
            if (collector.isProvidedBy(provider)) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean addPendingCollection(final CollectionProviderInternal<T, ? extends Iterable<T>> provider) {
        if (provider instanceof ChangingValue) {
            Cast.<ChangingValue<Iterable<T>>>uncheckedNonnullCast(provider).onValueChange(previousValues -> {
                for (T value : previousValues) {
                    values.remove(value);
                }
                doAddPendingCollection(provider);
            });
        }
        return doAddPendingCollection(provider);
    }

    private boolean doAddPendingCollection(CollectionProviderInternal<T, ? extends Iterable<T>> provider) {
        return pending.add(new Collectors.TypedCollector<T>(provider.getElementType(), new Collectors.ElementsFromCollectionProvider<T>(provider)));
    }

    @Override
    public boolean removePendingCollection(CollectionProviderInternal<T, ? extends Iterable<T>> provider) {
        return removeByProvider(provider);
    }

    @Override
    public void onPendingAdded(Action<T> action) {
        this.addRealizedAction = action;
    }

    @Override
    public void realizeExternal(ProviderInternal<? extends T> provider) {
        removePending(provider);
    }

    @Override
    public MutationGuard getMutationGuard() {
        return mutationGuard;
    }
}
