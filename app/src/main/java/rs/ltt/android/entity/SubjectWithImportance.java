/*
 * Copyright 2019 Daniel Gultsch
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rs.ltt.android.entity;

public class SubjectWithImportance {

    public final String threadId;
    public final String subject;
    public final Boolean important;

    private SubjectWithImportance(String threadId, String subject, Boolean important) {
        this.threadId = threadId;
        this.subject = subject;
        this.important = important;
    }

    public static SubjectWithImportance of(ThreadHeader header, Boolean important) {
        final String threadId = header == null ? null : header.threadId;
        final String subject = header == null ? null : header.subject;
        return new SubjectWithImportance(threadId, subject, important);
    }

    public static SubjectWithImportance of(String threadId, String subject, boolean important) {
        return new SubjectWithImportance(threadId, subject, important);
    }
}
