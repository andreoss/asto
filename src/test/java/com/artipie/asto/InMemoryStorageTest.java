/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.asto;

import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link InMemoryStorage}.
 *
 * @since 0.14
 */
public final class InMemoryStorageTest {

    @Test
    void shouldListNoKeysWhenEmpty() {
        final BlockingStorage blocking = new BlockingStorage(this.storage());
        final Collection<String> keys = blocking.list(new Key.From("a", "b"))
            .stream()
            .map(Key::string)
            .collect(Collectors.toList());
        MatcherAssert.assertThat(keys, Matchers.empty());
    }

    @Test
    void shouldListKeysInOrder() {
        final byte[] data = "some data!".getBytes();
        final BlockingStorage blocking = new BlockingStorage(this.storage());
        blocking.save(new Key.From("1"), data);
        blocking.save(new Key.From("a", "b", "c", "1"), data);
        blocking.save(new Key.From("a", "b", "2"), data);
        blocking.save(new Key.From("a", "z"), data);
        blocking.save(new Key.From("z"), data);
        final Collection<String> keys = blocking.list(new Key.From("a", "b"))
            .stream()
            .map(Key::string)
            .collect(Collectors.toList());
        MatcherAssert.assertThat(
            keys,
            Matchers.equalTo(Arrays.asList("a/b/2", "a/b/c/1"))
        );
    }

    @Test
    void shouldSave() {
        final BlockingStorage storage = new BlockingStorage(this.storage());
        final byte[] data = "0".getBytes();
        final Key key = new Key.From("shouldSave");
        storage.save(key, data);
        MatcherAssert.assertThat(storage.value(key), Matchers.equalTo(data));
    }

    @Test
    void shouldSaveFromMultipleBuffers() throws Exception {
        final Storage storage = this.storage();
        final Key key = new Key.From("shouldSaveFromMultipleBuffers");
        storage.save(
            key,
            Flowable.fromArray(
                ByteBuffer.wrap("12".getBytes()),
                ByteBuffer.wrap("34".getBytes()),
                ByteBuffer.wrap("5".getBytes())
            )
        ).get();
        MatcherAssert.assertThat(
            new BlockingStorage(storage).value(key),
            Matchers.equalTo("12345".getBytes())
        );
    }

    @Test
    void shouldSaveEmpty() throws Exception {
        final Storage storage = this.storage();
        final Key key = new Key.From("shouldSaveEmpty");
        storage.save(key, Flowable.empty()).get();
        MatcherAssert.assertThat(
            new BlockingStorage(storage).value(key),
            Matchers.equalTo(new byte[0])
        );
    }

    @Test
    void shouldSaveWhenValueAlreadyExists() {
        final BlockingStorage storage = new BlockingStorage(this.storage());
        final byte[] original = "1".getBytes();
        final byte[] updated = "2".getBytes();
        final Key key = new Key.From("shouldSaveWhenValueAlreadyExists");
        storage.save(key, original);
        storage.save(key, updated);
        MatcherAssert.assertThat(storage.value(key), Matchers.equalTo(updated));
    }

    @Test
    void shouldFailToLoadAbsentValue() {
        final BlockingStorage storage = new BlockingStorage(this.storage());
        final Key key = new Key.From("shouldFailToLoadAbsentValue");
        Assertions.assertThrows(RuntimeException.class, () -> storage.value(key));
    }

    private InMemoryStorage storage() {
        return new InMemoryStorage();
    }
}
